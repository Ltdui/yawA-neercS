package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.AwayTimeApp
import com.example.MainActivity
import com.example.R
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AwayTimeSmallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AwayTimeWidgetManager.updateSmallWidgets(context, appWidgetManager, appWidgetIds)
    }
}

class AwayTimeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AwayTimeWidgetManager.updateLargeWidgets(context, appWidgetManager, appWidgetIds)
    }
}

object AwayTimeWidgetManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val smallIds = appWidgetManager.getAppWidgetIds(ComponentName(context, AwayTimeSmallWidgetProvider::class.java))
        val largeIds = appWidgetManager.getAppWidgetIds(ComponentName(context, AwayTimeWidgetProvider::class.java))

        if (smallIds.isNotEmpty()) {
            updateSmallWidgets(context, appWidgetManager, smallIds)
        }
        if (largeIds.isNotEmpty()) {
            updateLargeWidgets(context, appWidgetManager, largeIds)
        }
    }

    fun updateSmallWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scope.launch {
            try {
                val app = context.applicationContext as? AwayTimeApp ?: return@launch
                val repository = app.repository
                val active = repository.getActiveSessionSync()
                val todayTotal = repository.getTodayTotalAwayMillisSync()

                val currentAwayText = if (active != null) {
                    val duration = System.currentTimeMillis() - active.startTime
                    TimeUtils.formatDuration(duration)
                } else {
                    "0m"
                }

                val todayText = "Today: " + TimeUtils.formatDuration(todayTotal)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_small)
                    views.setTextViewText(R.id.tv_widget_small_time, currentAwayText)
                    views.setTextViewText(R.id.tv_widget_small_sub, todayText)

                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_small_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                // Fail gracefully
            }
        }
    }

    fun updateLargeWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scope.launch {
            try {
                val app = context.applicationContext as? AwayTimeApp ?: return@launch
                val repository = app.repository
                val active = repository.getActiveSessionSync()
                val todayTotal = repository.getTodayTotalAwayMillisSync()
                val longestBreak = repository.getTodayLongestBreakMillisSync()

                val todayKey = TimeUtils.getCurrentDateKey()
                val todaySessions = app.database.awaySessionDao().getSessionsForDateSync(todayKey)
                val sessionCount = todaySessions.size + if (active != null) 1 else 0

                val isAway = active != null
                val currentAwayDuration = if (active != null) System.currentTimeMillis() - active.startTime else 0L

                val currentAwayText = if (isAway) TimeUtils.formatDuration(currentAwayDuration) else "0m"
                val statusText = if (isAway) {
                    "Away since " + TimeUtils.formatTime(active!!.startTime)
                } else {
                    "Phone in use"
                }
                val dotColor = if (isAway) "#34C759" else "#8E8E93"

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_large)
                    views.setTextViewText(R.id.tv_widget_large_time, currentAwayText)
                    views.setTextViewText(R.id.tv_widget_large_status, statusText)
                    views.setTextViewText(R.id.tv_widget_today_total, TimeUtils.formatDuration(todayTotal))
                    views.setTextViewText(R.id.tv_widget_longest_break, TimeUtils.formatDuration(longestBreak))
                    views.setTextViewText(R.id.tv_widget_sessions_count, sessionCount.toString())

                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_large_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                // Fail gracefully
            }
        }
    }
}
