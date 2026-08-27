package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import com.example.AwayTimeApp
import com.example.MainActivity
import com.example.R
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AwayTimeWidgetUpdater {

    private const val TAG_WIDGET = "AwayTimeWidget"
    private const val TAG_TRACKING = "AwayTimeTracking"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Updates every installed Away Time widget (small 2x2 and large 4x2).
     */
    fun updateAllWidgets(context: Context) {
        Log.d(TAG_WIDGET, "Widget update requested for all widgets")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetManager == null) {
            Log.w(TAG_WIDGET, "AppWidgetManager instance is null, aborting update")
            return
        }

        val smallIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, AwayTimeSmallWidgetProvider::class.java)
        )
        val largeIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, AwayTimeWidgetProvider::class.java)
        )

        Log.d(
            TAG_WIDGET,
            "Number of widget instances found: ${smallIds.size} small (2x2), ${largeIds.size} large (4x2)"
        )

        if (smallIds.isNotEmpty()) {
            updateSmallWidgets(context, appWidgetManager, smallIds)
        }
        if (largeIds.isNotEmpty()) {
            updateLargeWidgets(context, appWidgetManager, largeIds)
        }
    }

    /**
     * Updates all small 2x2 widget instances.
     */
    fun updateSmallWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        scope.launch {
            try {
                val app = context.applicationContext as? AwayTimeApp ?: return@launch
                val repository = app.repository

                val active = repository.getActiveSessionSync()
                val isAway = active != null && active.isActive
                val now = System.currentTimeMillis()

                val currentAwayDuration = if (isAway && active != null) {
                    val duration = maxOf(0L, now - active.startTime)
                    Log.d(TAG_TRACKING, "Active session found. Start: ${active.startTime}, elapsed: ${duration}ms")
                    duration
                } else {
                    Log.d(TAG_TRACKING, "No active session found (Screen ON / Phone in use)")
                    0L
                }

                val lastBreak = repository.getLastCompletedSessionSync()
                val todayTotal = repository.getTodayTotalAwayMillisSync()

                val timeDisplay = if (isAway) {
                    TimeUtils.formatDuration(currentAwayDuration)
                } else {
                    if (lastBreak != null && lastBreak.durationMillis > 0) {
                        TimeUtils.formatDuration(lastBreak.durationMillis)
                    } else if (todayTotal > 0) {
                        TimeUtils.formatDuration(todayTotal)
                    } else {
                        "0m"
                    }
                }

                val labelDisplay = if (isAway) "AWAY ACTIVE" else "AWAY TIME"
                val subDisplay = if (isAway) {
                    "Today: " + TimeUtils.formatDuration(todayTotal)
                } else {
                    "Today: " + TimeUtils.formatDuration(todayTotal)
                }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_small)
                    views.setTextViewText(R.id.tv_widget_small_label, labelDisplay)
                    views.setTextViewText(R.id.tv_widget_small_time, timeDisplay)
                    views.setTextViewText(R.id.tv_widget_small_sub, subDisplay)

                    // 1. Root card click -> Open Main App
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    val appPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_small_root, appPendingIntent)

                    // 2. Corner Refresh button click -> Trigger refresh
                    val refreshIntent = Intent(context, AwayTimeSmallWidgetProvider::class.java).apply {
                        action = AwayTimeSmallWidgetProvider.ACTION_REFRESH_WIDGET
                    }
                    val refreshPendingIntent = PendingIntent.getBroadcast(
                        context,
                        widgetId + 10000,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }

                Log.d(TAG_WIDGET, "Small 2x2 widget update completed for ${appWidgetIds.size} instances. Display: $timeDisplay")
            } catch (e: Exception) {
                Log.e(TAG_WIDGET, "Error updating small widgets", e)
            }
        }
    }

    /**
     * Updates all large 4x2 summary widget instances.
     */
    fun updateLargeWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        scope.launch {
            try {
                val app = context.applicationContext as? AwayTimeApp ?: return@launch
                val repository = app.repository

                val active = repository.getActiveSessionSync()
                val isAway = active != null && active.isActive
                val now = System.currentTimeMillis()

                val currentAwayDuration = if (isAway && active != null) {
                    val duration = maxOf(0L, now - active.startTime)
                    Log.d(TAG_TRACKING, "Active session found. Start: ${active.startTime}, elapsed: ${duration}ms")
                    duration
                } else {
                    Log.d(TAG_TRACKING, "No active session found (Screen ON / Phone in use)")
                    0L
                }

                val lastBreak = repository.getLastCompletedSessionSync()
                val todayTotal = repository.getTodayTotalAwayMillisSync()
                val longestBreak = repository.getTodayLongestBreakMillisSync()

                val todayKey = TimeUtils.getCurrentDateKey()
                val todaySessions = app.database.awaySessionDao().getSessionsForDateSync(todayKey)
                val sessionCount = todaySessions.size + if (isAway) 1 else 0

                val timeDisplay = if (isAway) {
                    TimeUtils.formatDuration(currentAwayDuration)
                } else {
                    if (lastBreak != null && lastBreak.durationMillis > 0) {
                        TimeUtils.formatDuration(lastBreak.durationMillis)
                    } else if (todayTotal > 0) {
                        TimeUtils.formatDuration(todayTotal)
                    } else {
                        "0m"
                    }
                }

                val statusDisplay = if (isAway && active != null) {
                    "Away since " + TimeUtils.formatTime(active.startTime)
                } else {
                    if (lastBreak != null && lastBreak.durationMillis > 0) {
                        "Last break: " + TimeUtils.formatDuration(lastBreak.durationMillis)
                    } else {
                        "Phone in use"
                    }
                }

                val dotColor = if (isAway) Color.parseColor("#34C759") else Color.parseColor("#8E8E93")

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_large)
                    views.setTextViewText(R.id.tv_widget_large_time, timeDisplay)
                    views.setTextViewText(R.id.tv_widget_large_status, statusDisplay)
                    views.setTextColor(R.id.tv_widget_status_dot, dotColor)
                    views.setTextViewText(R.id.tv_widget_today_total, TimeUtils.formatDuration(todayTotal))
                    views.setTextViewText(R.id.tv_widget_longest_break, TimeUtils.formatDuration(longestBreak))
                    views.setTextViewText(R.id.tv_widget_sessions_count, sessionCount.toString())

                    // 1. Root card click -> Open Main App
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    val appPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_large_root, appPendingIntent)

                    // 2. Corner Refresh button click -> Trigger refresh
                    val refreshIntent = Intent(context, AwayTimeWidgetProvider::class.java).apply {
                        action = AwayTimeWidgetProvider.ACTION_REFRESH_WIDGET
                    }
                    val refreshPendingIntent = PendingIntent.getBroadcast(
                        context,
                        widgetId + 20000,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_large_refresh, refreshPendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }

                Log.d(TAG_WIDGET, "Large widget update completed for ${appWidgetIds.size} instances. Display: $timeDisplay")
            } catch (e: Exception) {
                Log.e(TAG_WIDGET, "Error updating large widgets", e)
            }
        }
    }
}
