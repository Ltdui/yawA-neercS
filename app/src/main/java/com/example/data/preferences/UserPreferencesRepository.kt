package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "away_time_preferences")

data class UserPreferences(
    val themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val minSessionDurationSeconds: Int = 60,
    val includeShortBreaks: Boolean = true,
    val firstDayOfWeek: String = "MONDAY", // "MONDAY", "SUNDAY", "SYSTEM"
    val notificationsEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val backgroundTrackingEnabled: Boolean = true
) {
    val hasCompletedOnboarding: Boolean get() = onboardingCompleted
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MIN_SESSION_DURATION = intPreferencesKey("min_session_duration")
        val INCLUDE_SHORT_BREAKS = booleanPreferencesKey("include_short_breaks")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val BACKGROUND_TRACKING_ENABLED = booleanPreferencesKey("background_tracking_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            themeMode = preferences[Keys.THEME_MODE] ?: "DARK",
            minSessionDurationSeconds = preferences[Keys.MIN_SESSION_DURATION] ?: 60,
            includeShortBreaks = preferences[Keys.INCLUDE_SHORT_BREAKS] ?: true,
            firstDayOfWeek = preferences[Keys.FIRST_DAY_OF_WEEK] ?: "MONDAY",
            notificationsEnabled = preferences[Keys.NOTIFICATIONS_ENABLED] ?: false,
            onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
            backgroundTrackingEnabled = preferences[Keys.BACKGROUND_TRACKING_ENABLED] ?: true
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setMinSessionDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MIN_SESSION_DURATION] = seconds
        }
    }

    suspend fun setIncludeShortBreaks(include: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.INCLUDE_SHORT_BREAKS] = include
        }
    }

    suspend fun setFirstDayOfWeek(firstDay: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.FIRST_DAY_OF_WEEK] = firstDay
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setBackgroundTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BACKGROUND_TRACKING_ENABLED] = enabled
        }
    }
}
