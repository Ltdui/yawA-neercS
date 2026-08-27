package com.example

import com.example.data.model.AwaySession
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AwayTimeLogicTest {

    @Test
    fun testDurationFormatting_basic() {
        assertEquals("0m", TimeUtils.formatDuration(0L))
        assertEquals("45s", TimeUtils.formatDuration(45_000L)) // 45 sec -> 45s
        assertEquals("1m", TimeUtils.formatDuration(60_000L))
        assertEquals("15m", TimeUtils.formatDuration(15 * 60_000L))
        assertEquals("1h", TimeUtils.formatDuration(60 * 60_000L))
        assertEquals("2h 37m", TimeUtils.formatDuration((2 * 60 + 37) * 60_000L))
    }

    @Test
    fun testDurationFormatting_negativeOrZero() {
        assertEquals("0m", TimeUtils.formatDuration(-5000L))
    }

    @Test
    fun testDurationHoursDecimal() {
        assertEquals("0.5h", TimeUtils.formatDurationHoursDecimal(30 * 60_000L))
        assertEquals("1.0h", TimeUtils.formatDurationHoursDecimal(60 * 60_000L))
        assertEquals("2.5h", TimeUtils.formatDurationHoursDecimal(150 * 60_000L))
    }

    @Test
    fun testDateKeyGeneration() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 24, 14, 30, 0)
        }
        val dateKey = TimeUtils.getDateKey(calendar.timeInMillis)
        assertEquals("2026-08-24", dateKey)
    }

    @Test
    fun testLast7DaysKeysCount() {
        val keys = TimeUtils.getLastNDaysDateKeys(7)
        assertEquals(7, keys.size)
        // Ensure today is the last item
        assertEquals(TimeUtils.getCurrentDateKey(), keys.last())
    }

    @Test
    fun testSessionFiltering_shortBreaksThreshold() {
        val sessions = listOf(
            AwaySession(id = 1, startTime = 1000, endTime = 31000, durationMillis = 30_000, dateKey = "2026-08-24", isActive = false),
            AwaySession(id = 2, startTime = 40000, endTime = 100000, durationMillis = 60_000, dateKey = "2026-08-24", isActive = false),
            AwaySession(id = 3, startTime = 200000, endTime = 500000, durationMillis = 300_000, dateKey = "2026-08-24", isActive = false)
        )

        val minDurationSeconds = 60
        val filteredWithoutShort = sessions.filter { it.durationMillis >= minDurationSeconds * 1000L }
        assertEquals(2, filteredWithoutShort.size)
        assertFalse(filteredWithoutShort.any { it.id == 1L })

        val filteredWithShort = sessions.filter { it.durationMillis > 0 }
        assertEquals(3, filteredWithShort.size)
    }

    @Test
    fun testMidnightCrossingLogic() {
        // Starts at 11:45 PM on Day 1, ends at 12:15 AM on Day 2 (30 min total)
        val calStart = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 24, 23, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 25, 0, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val start = calStart.timeInMillis
        val end = calEnd.timeInMillis

        val startDateKey = TimeUtils.getDateKey(start)
        val endDateKey = TimeUtils.getDateKey(end)

        assertEquals("2026-08-24", startDateKey)
        assertEquals("2026-08-25", endDateKey)

        // Midnight calculation
        val calMidnight = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val midnightMillis = calMidnight.timeInMillis + 1

        val firstPartDuration = midnightMillis - start
        val secondPartDuration = end - midnightMillis

        // 15 min on day 1 (15 * 60 * 1000 = 900,000)
        assertEquals(15 * 60 * 1000L, firstPartDuration)
        // 15 min on day 2 (15 * 60 * 1000 = 900,000)
        assertEquals(15 * 60 * 1000L, secondPartDuration)
        assertEquals(30 * 60 * 1000L, firstPartDuration + secondPartDuration)
    }

    @Test
    fun testRebootRecoveryClamp() {
        val start = System.currentTimeMillis() - (48L * 60 * 60 * 1000) // 48 hours ago
        val now = System.currentTimeMillis()
        val duration = now - start
        val maxReasonable = 24L * 60 * 60 * 1000 // 24 hours
        val recoveredEnd = if (duration > maxReasonable) start + maxReasonable else now

        assertEquals(24L * 60 * 60 * 1000, recoveredEnd - start)
    }
}
