package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val fullDayFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getCurrentDateKey(): String {
        return dateKeyFormat.format(Date())
    }

    fun getDateKey(timestamp: Long): String {
        return dateKeyFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDayOfWeek(dateKey: String): String {
        return try {
            val date = dateKeyFormat.parse(dateKey) ?: return dateKey
            dayOfWeekFormat.format(date)
        } catch (e: Exception) {
            dateKey
        }
    }

    fun formatDisplayDate(dateKey: String): String {
        val today = getCurrentDateKey()
        val yesterday = getYesterdayDateKey()

        return when (dateKey) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                try {
                    val date = dateKeyFormat.parse(dateKey) ?: return dateKey
                    fullDayFormat.format(date)
                } catch (e: Exception) {
                    dateKey
                }
            }
        }
    }

    fun getYesterdayDateKey(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateKeyFormat.format(calendar.time)
    }

    fun getLastNDaysDateKeys(days: Int): List<String> {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        // End with today
        for (i in (days - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            list.add(dateKeyFormat.format(cal.time))
        }
        return list
    }

    fun formatDuration(durationMillis: Long): String {
        if (durationMillis <= 0) return "0m"

        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

        return when {
            hours > 0 -> {
                if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            }
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    fun formatDurationExact(durationMillis: Long): String {
        if (durationMillis <= 0) return "0m"

        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    fun formatDurationHoursDecimal(durationMillis: Long): String {
        val hours = durationMillis.toDouble() / (1000.0 * 60.0 * 60.0)
        return String.format(Locale.getDefault(), "%.1fh", hours)
    }
}
