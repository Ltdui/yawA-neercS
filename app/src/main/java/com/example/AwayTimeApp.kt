package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AwayTimeRepository
import com.example.service.AwayTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AwayTimeApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val repository: AwayTimeRepository by lazy {
        AwayTimeRepository(database.awaySessionDao(), preferencesRepository, this)
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("AwayTimeApp", "AwayTimeApp initialized")

        com.example.widget.AwayTimeWidgetUpdater.updateAllWidgets(this)

        applicationScope.launch {
            try {
                // Clean invalid/corrupted records if any
                database.awaySessionDao().cleanInvalidRecords()

                // Check preferences and start tracking service
                val prefs = preferencesRepository.userPreferencesFlow.first()
                if (prefs.backgroundTrackingEnabled) {
                    AwayTrackingService.start(this@AwayTimeApp)
                }
            } catch (e: Exception) {
                Log.e("AwayTimeApp", "Error during app startup initialization", e)
            }
        }
    }
}
