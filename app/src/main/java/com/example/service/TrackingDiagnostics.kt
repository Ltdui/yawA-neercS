package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiagnosticsData(
    val isScreenOn: Boolean = true,
    val isUsageAccessGranted: Boolean = false,
    val hasActiveSession: Boolean = false,
    val activeSessionStartTime: Long? = null,
    val lastCompletedSessionTime: Long? = null,
    val lastCompletedSessionDuration: Long? = null,
    val lastReceivedEvent: String = "None (App Launched)",
    val lastEventTimestamp: Long = System.currentTimeMillis()
)

object TrackingDiagnostics {

    private val _state = MutableStateFlow(DiagnosticsData())
    val state: StateFlow<DiagnosticsData> = _state.asStateFlow()

    fun recordEvent(
        eventName: String,
        isScreenOn: Boolean? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        _state.value = _state.value.copy(
            lastReceivedEvent = eventName,
            lastEventTimestamp = timestamp,
            isScreenOn = isScreenOn ?: _state.value.isScreenOn
        )
    }

    fun updateUsageAccess(granted: Boolean) {
        _state.value = _state.value.copy(isUsageAccessGranted = granted)
    }

    fun updateActiveSession(hasActive: Boolean, startTime: Long?) {
        _state.value = _state.value.copy(
            hasActiveSession = hasActive,
            activeSessionStartTime = startTime
        )
    }

    fun updateLastCompleted(endTime: Long, durationMillis: Long) {
        _state.value = _state.value.copy(
            lastCompletedSessionTime = endTime,
            lastCompletedSessionDuration = durationMillis
        )
    }

    fun updateScreenState(isOn: Boolean) {
        _state.value = _state.value.copy(isScreenOn = isOn)
    }
}
