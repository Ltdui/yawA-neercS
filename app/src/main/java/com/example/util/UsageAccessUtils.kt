package com.example.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object UsageAccessUtils {

    private const val TAG = "UsageAccessUtils"

    /**
     * Checks whether the user has granted the PACKAGE_USAGE_STATS permission
     * to the app using AppOpsManager.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage access permission", e)
            false
        }
    }

    /**
     * Opens the system settings screen for Usage Access so the user can grant permission.
     */
    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching usage access settings", e)
            // Fallback to main security/settings if specific intent fails
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Error launching fallback settings", fallbackEx)
            }
        }
    }

    /**
     * Queries whether the screen is currently interactive (turned ON) via PowerManager.
     */
    fun isScreenInteractive(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isInteractive ?: true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking screen interactive state", e)
            true
        }
    }
}
