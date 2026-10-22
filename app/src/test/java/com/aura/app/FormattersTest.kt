package com.aura.app

import com.aura.app.core.common.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun testFormatTimer_zeroSeconds() {
        assertEquals("00:00", Formatters.formatTimer(0))
    }

    @Test
    fun testFormatTimer_standardSeconds() {
        assertEquals("01:05", Formatters.formatTimer(65))
        assertEquals("25:00", Formatters.formatTimer(1500))
        assertEquals("59:59", Formatters.formatTimer(3599))
    }

    @Test
    fun testFormatDurationMinutes() {
        assertEquals("1 min", Formatters.formatDurationMinutes(30000L))
        assertEquals("5 mins", Formatters.formatDurationMinutes(300000L))
        assertEquals("25 mins", Formatters.formatDurationMinutes(1500000L))
    }
}
