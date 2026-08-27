package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.AwayTimeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AwayTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var screenStateReceiver: ScreenStateReceiver? = null
    private var isReceiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AwayTrackingService created")
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AwayTrackingService onStartCommand")
        checkInitialScreenState()
        return START_STICKY
    }

    private fun registerScreenStateReceiver() {
        if (isReceiverRegistered) return
        try {
            screenStateReceiver = ScreenStateReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SHUTDOWN)
            }
            registerReceiver(screenStateReceiver, filter)
            isReceiverRegistered = true
            Log.d(TAG, "ScreenStateReceiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register ScreenStateReceiver", e)
        }
    }

    private fun checkInitialScreenState() {
        serviceScope.launch {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isInteractive = powerManager?.isInteractive ?: true
                TrackingDiagnostics.updateScreenState(isInteractive)
                val app = applicationContext as? AwayTimeApp ?: return@launch
                val repository = app.repository

                if (!isInteractive) {
                    Log.d(TAG, "Initial check: Device screen is OFF -> Start session")
                    repository.startAwaySession()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during initial screen state check", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AwayTrackingService onDestroy")
        if (isReceiverRegistered && screenStateReceiver != null) {
            try {
                unregisterReceiver(screenStateReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering ScreenStateReceiver", e)
            }
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AwayTrackingService"

        fun start(context: Context) {
            try {
                val intent = Intent(context, AwayTrackingService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to start AwayTrackingService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AwayTrackingService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to stop AwayTrackingService", e)
            }
        }
    }
}
