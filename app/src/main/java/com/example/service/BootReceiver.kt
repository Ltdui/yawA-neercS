package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.AwayTimeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? AwayTimeApp ?: return
            val repository = app.repository
            val preferences = app.preferencesRepository

            scope.launch {
                try {
                    // Safe recovery of any active session before reboot
                    repository.recoverFromReboot()

                    // Update all widgets after recovery
                    com.example.widget.AwayTimeWidgetUpdater.updateAllWidgets(context)

                    // Check if tracking is enabled in user preferences
                    val userPrefs = preferences.userPreferencesFlow.first()
                    if (userPrefs.backgroundTrackingEnabled) {
                        AwayTrackingService.start(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in BootReceiver execution", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
