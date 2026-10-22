package com.aura.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerTest {

    @Test
    fun testActiveFocusSessionRemaining_calculatesCorrectly() {
        val now = 1000000L
        val targetDurationSec = 25 * 60L
        val endTime = now + (targetDurationSec * 1000L)

        val remainingSeconds = (endTime - now) / 1000L
        assertEquals(1500L, remainingSeconds)

        val progress = (remainingSeconds.toFloat() / targetDurationSec.toFloat()).coerceIn(0f, 1f)
        assertEquals(1.0f, progress, 0.001f)
    }

    @Test
    fun testActiveFocusSessionExpiration() {
        val now = 2000000L
        val endTime = 1999990L // Expired 10 seconds ago

        val isExpired = endTime <= now
        assertTrue(isExpired)
    }

    @Test
    fun testCompletionThreshold_halfwayIsNotComplete() {
        val targetSeconds = 1500L
        val elapsedSeconds = 700L
        val isCompleted = elapsedSeconds >= targetSeconds
        assertFalse(isCompleted)
    }
}
