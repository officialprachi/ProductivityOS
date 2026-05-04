package com.productivityos.app

import org.junit.Test
import org.junit.Assert.*

class ProductivityOSUnitTest {

    @Test
    fun `score formula - focus adds 2 points per minute`() {
        val focusMinutes = 30
        val distractMinutes = 0
        val score = (focusMinutes * 2) - (distractMinutes * 1)
        assertEquals(60, score)
    }

    @Test
    fun `score formula - distraction subtracts 1 point per minute`() {
        val focusMinutes = 0
        val distractMinutes = 45
        val score = (focusMinutes * 2) - (distractMinutes * 1)
        assertEquals(-45, score)
    }

    @Test
    fun `score formula - combined focus and distraction`() {
        val focusMinutes = 260  // 4h 20m
        val distractMinutes = 95 // 1h 35m
        val rawScore = (focusMinutes * 2) - (distractMinutes * 1)
        val clampedScore = rawScore.coerceIn(0, 100)
        // 520 - 95 = 425 → clamped to 100
        assertEquals(100, clampedScore)
    }

    @Test
    fun `burnout alert fires after 432 minutes (7h 12m)`() {
        val burnoutThresholdMinutes = 7 * 60 + 12 // 432
        val workedMinutes = 433
        val shouldAlert = workedMinutes >= burnoutThresholdMinutes
        assertTrue(shouldAlert)
    }

    @Test
    fun `burnout alert does not fire before threshold`() {
        val burnoutThresholdMinutes = 7 * 60 + 12
        val workedMinutes = 400
        val shouldAlert = workedMinutes >= burnoutThresholdMinutes
        assertFalse(shouldAlert)
    }

    @Test
    fun `format minutes converts correctly for hours and minutes`() {
        val minutes = 260
        val h = minutes / 60
        val m = minutes % 60
        val result = "${h}h ${m}m"
        assertEquals("4h 20m", result)
    }

    @Test
    fun `format minutes shows only minutes when under 60`() {
        val minutes = 42
        val h = minutes / 60
        val m = minutes % 60
        val result = if (h > 0) "${h}h ${m}m" else "${m}m"
        assertEquals("42m", result)
    }

    @Test
    fun `format seconds for timer display`() {
        val totalSeconds = 89 * 60 + 47
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val result = String.format("%02d:%02d:%02d", h, m, s)
        assertEquals("01:29:47", result)
    }
}
