package com.example.data.model

data class DaySummary(
    val dateKey: String,
    val dayLabel: String, // e.g. "Mon"
    val displayDate: String, // e.g. "Today", "Yesterday", "Monday, Aug 24"
    val totalAwayMillis: Long,
    val longestBreakMillis: Long,
    val sessionCount: Int,
    val sessions: List<AwaySession> = emptyList(),
    val isToday: Boolean = false
)

data class WeeklyStats(
    val days: List<DaySummary>,
    val total7DayMillis: Long,
    val dailyAverageMillis: Long,
    val bestDay: DaySummary?,
    val comparisonWithYesterdayMillis: Long, // Positive if today > yesterday
    val totalSessionsCount: Int
)

data class AllTimeStats(
    val totalAwayMillis: Long,
    val totalDaysTracked: Int,
    val dailyAverageMillis: Long,
    val longestSingleBreakMillis: Long,
    val totalSessionsCount: Int,
    val morningAwayMillis: Long, // 5 AM - 12 PM
    val afternoonAwayMillis: Long, // 12 PM - 5 PM
    val eveningNightAwayMillis: Long // 5 PM - 5 AM
)
