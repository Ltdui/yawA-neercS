package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
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
        Log.d(TAG, "Received system action: $action")

        val app = context.applicationContext as? AwayTimeApp ?: return
        val repository = app.repository

        when (action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen went OFF -> Starting Away Session")
                scope.launch {
                    try {
                        repository.startAwaySession()
                        AwayTimeWidgetManager.updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting away session on screen off", e)
                    }
                }
            }

            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen ON / User Present -> Ending Away Session")
                scope.launch {
                    try {
                        repository.endAwaySession()
                        AwayTimeWidgetManager.updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error ending away session on screen on", e)
                    }
                }
            }

            Intent.ACTION_SHUTDOWN -> {
                Log.d(TAG, "Device shutdown -> Ending Away Session")
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
