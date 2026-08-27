package com.example.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AllTimeStats
import com.example.data.model.DaySummary
import com.example.data.model.WeeklyStats
import com.example.data.repository.AwayTimeRepository
import com.example.util.TimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val todayMillis: Long = 0L,
    val yesterdayMillis: Long = 0L,
    val weeklyStats: WeeklyStats? = null,
    val allTimeStats: AllTimeStats? = null,
    val hasEnoughData: Boolean = false,
    val selectedDay: DaySummary? = null
)

class StatsViewModel(
    private val repository: AwayTimeRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.weeklyStatsFlow,
        repository.allTimeStatsFlow
    ) { weekly, allTime ->
        val todayKey = TimeUtils.getCurrentDateKey()
        val yesterdayKey = TimeUtils.getYesterdayDateKey()

        val todayTotal = weekly.days.find { it.dateKey == todayKey }?.totalAwayMillis ?: 0L
        val yesterdayTotal = weekly.days.find { it.dateKey == yesterdayKey }?.totalAwayMillis ?: 0L
        val hasData = allTime.totalSessionsCount > 0

        StatsUiState(
            todayMillis = todayTotal,
            yesterdayMillis = yesterdayTotal,
            weeklyStats = weekly,
            allTimeStats = allTime,
            hasEnoughData = hasData,
            selectedDay = weekly.days.find { it.isToday }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )

    class Factory(private val repository: AwayTimeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(repository) as T
        }
    }
}
