package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AwaySession
import com.example.data.model.WeeklyStats
import com.example.data.repository.AwayTimeRepository
import com.example.util.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val isAway: Boolean = false,
    val activeSession: AwaySession? = null,
    val liveDurationMillis: Long = 0L,
    val todayTotalMillis: Long = 0L,
    val longestBreakMillis: Long = 0L,
    val todaySessionsCount: Int = 0,
    val weeklyStats: WeeklyStats? = null,
    val recentSessions: List<AwaySession> = emptyList(),
    val insightText: String = "",
    val lastBreakDurationMillis: Long? = null,
    val isSimulatingAway: Boolean = false
)

class HomeViewModel(
    private val repository: AwayTimeRepository
) : ViewModel() {

    private val _simulatingAway = MutableStateFlow(false)
    private val _liveActiveDuration = MutableStateFlow(0L)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.activeSessionFlow,
        repository.todaySessionsFlow,
        repository.weeklyStatsFlow,
        repository.recentSessionsFlow,
        _simulatingAway,
        _liveActiveDuration
    ) { args: Array<Any?> ->
        val activeSession = args[0] as? AwaySession
        @Suppress("UNCHECKED_CAST")
        val todaySessions = (args[1] as? List<AwaySession>) ?: emptyList()
        val weekly = args[2] as? WeeklyStats
        @Suppress("UNCHECKED_CAST")
        val recent = (args[3] as? List<AwaySession>) ?: emptyList()
        val simulating = (args[4] as? Boolean) ?: false
        val liveDuration = (args[5] as? Long) ?: 0L

        val isAway = activeSession != null || simulating
        val currentLive = if (activeSession != null) {
            maxOf(0L, System.currentTimeMillis() - activeSession.startTime)
        } else if (simulating) {
            liveDuration
        } else {
            0L
        }

        val completedTodaySum = todaySessions.sumOf { it.durationMillis }
        val todayTotal = completedTodaySum + currentLive

        val maxCompleted = todaySessions.maxOfOrNull { it.durationMillis } ?: 0L
        val longestBreak = maxOf(maxCompleted, currentLive)
        val sessionsCount = todaySessions.size + (if (isAway) 1 else 0)

        val lastBreak = recent.firstOrNull()?.durationMillis

        val insight = generateInsight(todayTotal, longestBreak, sessionsCount, weekly)

        HomeUiState(
            isAway = isAway,
            activeSession = activeSession,
            liveDurationMillis = currentLive,
            todayTotalMillis = todayTotal,
            longestBreakMillis = longestBreak,
            todaySessionsCount = sessionsCount,
            weeklyStats = weekly,
            recentSessions = recent,
            insightText = insight,
            lastBreakDurationMillis = lastBreak,
            isSimulatingAway = simulating
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        startLiveTimer()
    }

    private fun startLiveTimer() {
        viewModelScope.launch {
            while (isActive) {
                val active = repository.getActiveSessionSync()
                if (active != null) {
                    _liveActiveDuration.value = maxOf(0L, System.currentTimeMillis() - active.startTime)
                } else if (_simulatingAway.value) {
                    _liveActiveDuration.value += 1000L
                }
                delay(1000L)
            }
        }
    }

    fun toggleSimulateAway() {
        viewModelScope.launch {
            if (_simulatingAway.value) {
                // End simulated break and store as test session
                val now = System.currentTimeMillis()
                val duration = _liveActiveDuration.value
                val start = now - duration
                if (duration >= 3000L) {
                    repository.insertTestSession(start, now)
                }
                _simulatingAway.value = false
                _liveActiveDuration.value = 0L
            } else {
                // Start simulated break
                _liveActiveDuration.value = 0L
                _simulatingAway.value = true
            }
        }
    }

    fun deleteSession(session: AwaySession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    private fun generateInsight(
        todayTotal: Long,
        longestBreak: Long,
        sessionsCount: Int,
        weekly: WeeklyStats?
    ): String {
        if (todayTotal <= 0L && sessionsCount == 0) {
            return "Put your phone down to start your first away session."
        }

        if (longestBreak > 0L && longestBreak >= todayTotal * 0.7f && sessionsCount > 1) {
            return "Your longest break today was ${TimeUtils.formatDuration(longestBreak)}."
        }

        if (weekly != null && weekly.comparisonWithYesterdayMillis > 15 * 60 * 1000L) {
            val diff = TimeUtils.formatDuration(weekly.comparisonWithYesterdayMillis)
            return "You've spent $diff more away time today than yesterday."
        }

        if (longestBreak > 0L) {
            return "Your longest break today was ${TimeUtils.formatDuration(longestBreak)} across $sessionsCount ${if (sessionsCount == 1) "break" else "breaks"}."
        }

        return "You've spent ${TimeUtils.formatDuration(todayTotal)} away from your screen today."
    }

    class Factory(private val repository: AwayTimeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
