package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AwayTimeRepository
import com.example.service.AwayTrackingService
import com.example.service.DiagnosticsData
import com.example.service.TrackingDiagnostics
import com.example.util.UsageAccessUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val awayTimeRepository: AwayTimeRepository
) : ViewModel() {

    val preferencesState: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val diagnosticsState: StateFlow<DiagnosticsData> = TrackingDiagnostics.state

    fun refreshDiagnostics(context: Context) {
        val hasPermission = UsageAccessUtils.hasUsageAccessPermission(context)
        val isInteractive = UsageAccessUtils.isScreenInteractive(context)
        TrackingDiagnostics.updateUsageAccess(hasPermission)
        TrackingDiagnostics.updateScreenState(isInteractive)
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setMinDuration(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setMinSessionDuration(seconds)
        }
    }

    fun setIncludeShortBreaks(include: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setIncludeShortBreaks(include)
        }
    }

    fun setFirstDayOfWeek(day: String) {
        viewModelScope.launch {
            preferencesRepository.setFirstDayOfWeek(day)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setBackgroundTrackingEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundTrackingEnabled(enabled)
            if (enabled) {
                AwayTrackingService.start(context)
            } else {
                AwayTrackingService.stop(context)
            }
        }
    }

    fun addTestSession(durationMinutes: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val start = now - (durationMinutes * 60 * 1000L)
            awayTimeRepository.insertTestSession(start, now)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            awayTimeRepository.clearAllData()
        }
    }

    class Factory(
        private val preferencesRepository: UserPreferencesRepository,
        private val awayTimeRepository: AwayTimeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferencesRepository, awayTimeRepository) as T
        }
    }
}
