package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

class AwayTimeSmallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} small widget(s)")
        AwayTimeWidgetUpdater.updateSmallWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d(TAG, "onAppWidgetOptionsChanged for small widget id: $appWidgetId")
        AwayTimeWidgetUpdater.updateSmallWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "onEnabled: First small widget added to home screen")
        AwayTimeWidgetUpdater.updateAllWidgets(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "onDisabled: Last small widget removed from home screen")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive intent in small widget provider: ${intent.action}")
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "Refresh button tapped on small widget -> updating all widgets")
            AwayTimeWidgetUpdater.updateAllWidgets(context)
        }
    }

    companion object {
        private const val TAG = "AwayTimeWidget"
        const val ACTION_REFRESH_WIDGET = "com.example.action.REFRESH_SMALL_WIDGET"
    }
}

class AwayTimeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} large widget(s)")
        AwayTimeWidgetUpdater.updateLargeWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.d(TAG, "onAppWidgetOptionsChanged for large widget id: $appWidgetId")
        AwayTimeWidgetUpdater.updateLargeWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "onEnabled: First large widget added to home screen")
        AwayTimeWidgetUpdater.updateAllWidgets(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "onDisabled: Last large widget removed from home screen")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive intent in large widget provider: ${intent.action}")
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            Log.d(TAG, "Refresh button tapped on large widget -> updating all widgets")
            AwayTimeWidgetUpdater.updateAllWidgets(context)
        }
    }

    companion object {
        private const val TAG = "AwayTimeWidget"
        const val ACTION_REFRESH_WIDGET = "com.example.action.REFRESH_LARGE_WIDGET"
    }
}

/**
 * Legacy compatibility facade for AwayTimeWidgetManager.
 */
object AwayTimeWidgetManager {
    fun updateAllWidgets(context: Context) {
        AwayTimeWidgetUpdater.updateAllWidgets(context)
    }

    fun updateSmallWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AwayTimeWidgetUpdater.updateSmallWidgets(context, appWidgetManager, appWidgetIds)
    }

    fun updateLargeWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AwayTimeWidgetUpdater.updateLargeWidgets(context, appWidgetManager, appWidgetIds)
    }
}
