package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AwaySession
import com.example.data.model.DaySummary
import com.example.data.repository.AwayTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val daySummaries: List<DaySummary> = emptyList(),
    val expandedDateKeys: Set<String> = emptySet(),
    val totalRecordedDays: Int = 0,
    val totalAwayMillis: Long = 0L,
    val totalSessionsCount: Int = 0
)

class HistoryViewModel(
    private val repository: AwayTimeRepository
) : ViewModel() {

    private val _expandedDateKeys = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.historyByDayFlow,
        _expandedDateKeys
    ) { days, expanded ->
        val totalDays = days.size
        val totalMillis = days.sumOf { it.totalAwayMillis }
        val totalSessions = days.sumOf { it.sessionCount }

        // Expand the first day by default if nothing expanded yet
        val finalExpanded = if (expanded.isEmpty() && days.isNotEmpty()) {
            setOf(days.first().dateKey)
        } else {
            expanded
        }

        HistoryUiState(
            daySummaries = days,
            expandedDateKeys = finalExpanded,
            totalRecordedDays = totalDays,
            totalAwayMillis = totalMillis,
            totalSessionsCount = totalSessions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun toggleDayExpanded(dateKey: String) {
        val current = _expandedDateKeys.value
        _expandedDateKeys.value = if (current.contains(dateKey)) {
            current - dateKey
        } else {
            current + dateKey
        }
    }

    fun deleteSession(session: AwaySession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    class Factory(private val repository: AwayTimeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
