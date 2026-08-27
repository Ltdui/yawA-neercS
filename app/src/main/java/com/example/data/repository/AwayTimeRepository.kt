package com.example.data.repository

import android.content.Context
import com.example.data.local.AwaySessionDao
import com.example.data.model.AllTimeStats
import com.example.data.model.AwaySession
import com.example.data.model.DaySummary
import com.example.data.model.WeeklyStats
import com.example.data.preferences.UserPreferencesRepository
import com.example.util.TimeUtils
import com.example.widget.AwayTimeWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class AwayTimeRepository(
    private val awaySessionDao: AwaySessionDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val context: Context? = null
) {

    val activeSessionFlow: Flow<AwaySession?> = awaySessionDao.observeActiveSession()

    val allSessionsFlow: Flow<List<AwaySession>> = combine(
        awaySessionDao.getAllSessions(),
        preferencesRepository.userPreferencesFlow
    ) { sessions, prefs ->
        filterSessions(sessions, prefs.includeShortBreaks, prefs.minSessionDurationSeconds)
    }.flowOn(Dispatchers.Default)

    val todaySessionsFlow: Flow<List<AwaySession>> = combine(
        awaySessionDao.getAllSessions(),
        preferencesRepository.userPreferencesFlow
    ) { sessions, prefs ->
        val todayKey = TimeUtils.getCurrentDateKey()
        val todaySessions = sessions.filter { it.dateKey == todayKey && !it.isActive }
        filterSessions(todaySessions, prefs.includeShortBreaks, prefs.minSessionDurationSeconds)
    }.flowOn(Dispatchers.Default)

    val recentSessionsFlow: Flow<List<AwaySession>> = combine(
        awaySessionDao.getAllSessions(),
        preferencesRepository.userPreferencesFlow
    ) { sessions, prefs ->
        val filtered = filterSessions(sessions, prefs.includeShortBreaks, prefs.minSessionDurationSeconds)
        filtered.take(10)
    }.flowOn(Dispatchers.Default)

    val weeklyStatsFlow: Flow<WeeklyStats> = combine(
        awaySessionDao.getAllSessions(),
        preferencesRepository.userPreferencesFlow
    ) { sessions, prefs ->
        calculateWeeklyStats(sessions, prefs.includeShortBreaks, prefs.minSessionDurationSeconds)
    }.flowOn(Dispatchers.Default)

    val allTimeStatsFlow: Flow<AllTimeStats> = combine(
        awaySessionDao.getAllSessions(),
        preferencesRepository.userPreferencesFlow
    ) { sessions, prefs ->
        calculateAllTimeStats(sessions, prefs.includeShortBreaks, prefs.minSessionDurationSeconds)
    }.flowOn(Dispatchers.Default)

    val historyByDayFlow: Flow<List<DaySummary>> = combine(
        awaySessionDao.getAllSessions(),
        preferencesRepository.userPreferencesFlow
    ) { sessions, prefs ->
        val filtered = filterSessions(sessions, prefs.includeShortBreaks, prefs.minSessionDurationSeconds)
        val grouped = filtered.groupBy { it.dateKey }
        val todayKey = TimeUtils.getCurrentDateKey()

        grouped.map { (dateKey, daySessions) ->
            val totalMillis = daySessions.sumOf { it.durationMillis }
            val longestBreak = daySessions.maxOfOrNull { it.durationMillis } ?: 0L
            DaySummary(
                dateKey = dateKey,
                dayLabel = TimeUtils.formatDayOfWeek(dateKey),
                displayDate = TimeUtils.formatDisplayDate(dateKey),
                totalAwayMillis = totalMillis,
                longestBreakMillis = longestBreak,
                sessionCount = daySessions.size,
                sessions = daySessions.sortedByDescending { it.startTime },
                isToday = dateKey == todayKey
            )
        }.sortedByDescending { it.dateKey }
    }.flowOn(Dispatchers.Default)

    suspend fun startAwaySession(timestamp: Long = System.currentTimeMillis()): Long = withContext(Dispatchers.IO) {
        val currentActive = awaySessionDao.getActiveSession()
        if (currentActive != null) {
            context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
            return@withContext currentActive.id
        }
        val dateKey = TimeUtils.getDateKey(timestamp)
        val session = AwaySession(
            startTime = timestamp,
            endTime = null,
            durationMillis = 0L,
            dateKey = dateKey,
            isActive = true,
            createdAt = timestamp
        )
        val id = awaySessionDao.insertSession(session)
        context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
        id
    }

    suspend fun endAwaySession(timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val active = awaySessionDao.getActiveSession() ?: run {
            context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
            return@withContext
        }
        val start = active.startTime
        val end = maxOf(start, timestamp)
        val totalDuration = end - start

        if (totalDuration < 1000L) {
            // Less than 1 second, discard
            awaySessionDao.deleteSession(active)
            context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
            return@withContext
        }

        val startDateKey = active.dateKey
        val endDateKey = TimeUtils.getDateKey(end)

        if (startDateKey == endDateKey) {
            // Same day session
            val updated = active.copy(
                endTime = end,
                durationMillis = totalDuration,
                isActive = false
            )
            awaySessionDao.updateSession(updated)
        } else {
            // Crossed midnight: split across days for clean statistics
            handleCrossMidnightSession(active, start, end)
        }
        context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
    }

    private suspend fun handleCrossMidnightSession(active: AwaySession, start: Long, end: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = start
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val midnightMillis = calendar.timeInMillis + 1 // Start of next day

        if (end <= midnightMillis) {
            val updated = active.copy(
                endTime = end,
                durationMillis = end - start,
                isActive = false
            )
            awaySessionDao.updateSession(updated)
        } else {
            // First part up to midnight
            val durationFirst = midnightMillis - start
            val firstSession = active.copy(
                endTime = midnightMillis,
                durationMillis = maxOf(0L, durationFirst),
                isActive = false
            )
            awaySessionDao.updateSession(firstSession)

            // Second part from midnight to end
            val durationSecond = end - midnightMillis
            if (durationSecond > 1000L) {
                val secondSession = AwaySession(
                    startTime = midnightMillis,
                    endTime = end,
                    durationMillis = durationSecond,
                    dateKey = TimeUtils.getDateKey(midnightMillis),
                    isActive = false,
                    createdAt = end
                )
                awaySessionDao.insertSession(secondSession)
            }
        }
    }

    suspend fun recoverFromReboot(bootTimestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val active = awaySessionDao.getActiveSession() ?: run {
            context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
            return@withContext
        }
        // If active session was left open before reboot, close it at reboot time
        val duration = maxOf(0L, bootTimestamp - active.startTime)
        // If it was abnormally long (e.g. days), limit reasonably
        val maxReasonable = 24L * 60 * 60 * 1000 // 24 hours
        val finalEnd = if (duration > maxReasonable) active.startTime + maxReasonable else bootTimestamp
        endAwaySession(finalEnd)
        context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
    }

    suspend fun deleteSession(session: AwaySession) = withContext(Dispatchers.IO) {
        awaySessionDao.deleteSession(session)
        context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
    }

    suspend fun deleteSessionById(id: Long) = withContext(Dispatchers.IO) {
        awaySessionDao.deleteSessionById(id)
        context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        awaySessionDao.deleteAllSessions()
        context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
    }

    suspend fun insertTestSession(startTime: Long, endTime: Long) = withContext(Dispatchers.IO) {
        val duration = maxOf(0L, endTime - startTime)
        val dateKey = TimeUtils.getDateKey(startTime)
        val session = AwaySession(
            startTime = startTime,
            endTime = endTime,
            durationMillis = duration,
            dateKey = dateKey,
            isActive = false,
            createdAt = System.currentTimeMillis()
        )
        val id = awaySessionDao.insertSession(session)
        context?.let { AwayTimeWidgetUpdater.updateAllWidgets(it) }
        id
    }

    suspend fun getActiveSessionSync(): AwaySession? = withContext(Dispatchers.IO) {
        awaySessionDao.getActiveSession()
    }

    suspend fun getLastCompletedSessionSync(): AwaySession? = withContext(Dispatchers.IO) {
        val todayKey = TimeUtils.getCurrentDateKey()
        val sessions = awaySessionDao.getSessionsForDateSync(todayKey)
        sessions.firstOrNull { !it.isActive } ?: awaySessionDao.getAllSessionsSync().firstOrNull { !it.isActive }
    }

    suspend fun getTodayTotalAwayMillisSync(): Long = withContext(Dispatchers.IO) {
        val todayKey = TimeUtils.getCurrentDateKey()
        val sessions = awaySessionDao.getSessionsForDateSync(todayKey)
        val completedSum = sessions.sumOf { it.durationMillis }
        val active = awaySessionDao.getActiveSession()
        val activeSum = if (active != null) {
            val now = System.currentTimeMillis()
            val startOfToday = TimeUtils.getStartOfTodayMillis()
            val effectiveStart = maxOf(active.startTime, startOfToday)
            maxOf(0L, now - effectiveStart)
        } else 0L
        completedSum + activeSum
    }

    suspend fun getTodayLongestBreakMillisSync(): Long = withContext(Dispatchers.IO) {
        val todayKey = TimeUtils.getCurrentDateKey()
        val sessions = awaySessionDao.getSessionsForDateSync(todayKey)
        val maxCompleted = sessions.maxOfOrNull { it.durationMillis } ?: 0L
        val active = awaySessionDao.getActiveSession()
        val activeDuration = if (active != null) maxOf(0L, System.currentTimeMillis() - active.startTime) else 0L
        maxOf(maxCompleted, activeDuration)
    }

    private fun filterSessions(
        sessions: List<AwaySession>,
        includeShortBreaks: Boolean,
        minDurationSeconds: Int
    ): List<AwaySession> {
        val minMillis = minDurationSeconds * 1000L
        return if (includeShortBreaks) {
            sessions.filter { it.durationMillis > 0 }
        } else {
            sessions.filter { it.durationMillis >= minMillis }
        }
    }

    private fun calculateWeeklyStats(
        allSessions: List<AwaySession>,
        includeShortBreaks: Boolean,
        minDurationSeconds: Int
    ): WeeklyStats {
        val filtered = filterSessions(allSessions, includeShortBreaks, minDurationSeconds)
        val last7DaysKeys = TimeUtils.getLastNDaysDateKeys(7)
        val todayKey = TimeUtils.getCurrentDateKey()
        val yesterdayKey = TimeUtils.getYesterdayDateKey()

        val grouped = filtered.groupBy { it.dateKey }

        val daySummaries = last7DaysKeys.map { dateKey ->
            val daySessions = grouped[dateKey] ?: emptyList()
            val totalMillis = daySessions.sumOf { it.durationMillis }
            val longest = daySessions.maxOfOrNull { it.durationMillis } ?: 0L
            DaySummary(
                dateKey = dateKey,
                dayLabel = TimeUtils.formatDayOfWeek(dateKey),
                displayDate = TimeUtils.formatDisplayDate(dateKey),
                totalAwayMillis = totalMillis,
                longestBreakMillis = longest,
                sessionCount = daySessions.size,
                sessions = daySessions.sortedByDescending { it.startTime },
                isToday = dateKey == todayKey
            )
        }

        val total7Day = daySummaries.sumOf { it.totalAwayMillis }
        val daysWithData = daySummaries.count { it.totalAwayMillis > 0 }
        val divisor = if (daysWithData > 0) daysWithData else 1
        val dailyAverage = total7Day / divisor

        val bestDay = daySummaries.filter { it.totalAwayMillis > 0 }.maxByOrNull { it.totalAwayMillis }

        val todayTotal = daySummaries.find { it.dateKey == todayKey }?.totalAwayMillis ?: 0L
        val yesterdayTotal = daySummaries.find { it.dateKey == yesterdayKey }?.totalAwayMillis ?: 0L
        val comparison = todayTotal - yesterdayTotal

        val totalSessions = daySummaries.sumOf { it.sessionCount }

        return WeeklyStats(
            days = daySummaries,
            total7DayMillis = total7Day,
            dailyAverageMillis = dailyAverage,
            bestDay = bestDay,
            comparisonWithYesterdayMillis = comparison,
            totalSessionsCount = totalSessions
        )
    }

    private fun calculateAllTimeStats(
        allSessions: List<AwaySession>,
        includeShortBreaks: Boolean,
        minDurationSeconds: Int
    ): AllTimeStats {
        val filtered = filterSessions(allSessions, includeShortBreaks, minDurationSeconds)
        val totalMillis = filtered.sumOf { it.durationMillis }
        val uniqueDays = filtered.map { it.dateKey }.distinct().size
        val divisor = if (uniqueDays > 0) uniqueDays else 1
        val dailyAverage = totalMillis / divisor
        val longestBreak = filtered.maxOfOrNull { it.durationMillis } ?: 0L
        val totalSessions = filtered.size

        var morning = 0L
        var afternoon = 0L
        var eveningNight = 0L

        val calendar = Calendar.getInstance()
        for (session in filtered) {
            calendar.timeInMillis = session.startTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..11 -> morning += session.durationMillis
                in 12..16 -> afternoon += session.durationMillis
                else -> eveningNight += session.durationMillis
            }
        }

        return AllTimeStats(
            totalAwayMillis = totalMillis,
            totalDaysTracked = uniqueDays,
            dailyAverageMillis = dailyAverage,
            longestSingleBreakMillis = longestBreak,
            totalSessionsCount = totalSessions,
            morningAwayMillis = morning,
            afternoonAwayMillis = afternoon,
            eveningNightAwayMillis = eveningNight
        )
    }
}
