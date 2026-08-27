package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.AwayTimeApp
import com.example.widget.AwayTimeWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScreenStateReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Received system broadcast action: $action")

        val app = context.applicationContext as? AwayTimeApp ?: return
        val repository = app.repository

        when (action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen went OFF -> Starting Away Session automatically")
                TrackingDiagnostics.recordEvent("ACTION_SCREEN_OFF", isScreenOn = false)
                scope.launch {
                    try {
                        repository.startAwaySession()
                        AwayTimeWidgetManager.updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting away session on screen off", e)
                    }
                }
            }

            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen turned ON -> Ending Away Session automatically")
                TrackingDiagnostics.recordEvent("ACTION_SCREEN_ON", isScreenOn = true)
                scope.launch {
                    try {
                        repository.endAwaySession()
                        AwayTimeWidgetManager.updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error ending away session on screen on", e)
                    }
                }
            }

            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "User unlocked device (USER_PRESENT)")
                TrackingDiagnostics.recordEvent("ACTION_USER_PRESENT", isScreenOn = true)
                scope.launch {
                    try {
                        // Ensure session is closed if screen on wasn't caught
                        repository.endAwaySession()
                        AwayTimeWidgetManager.updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking away session on user present", e)
                    }
                }
            }

            Intent.ACTION_SHUTDOWN -> {
                Log.d(TAG, "Device shutdown -> Ending active Away Session")
                TrackingDiagnostics.recordEvent("ACTION_SHUTDOWN", isScreenOn = false)
                scope.launch {
                    try {
                        repository.endAwaySession()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving session on shutdown", e)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }
}
