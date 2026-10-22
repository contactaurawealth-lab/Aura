package com.aura.app.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun formatTimer(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun formatDurationMinutes(durationMs: Long): String {
        val minutes = (durationMs / 1000) / 60
        return if (minutes <= 1) "1 min" else "$minutes mins"
    }

    fun formatDate(timestampMs: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatTimeOfDay(timestampMs: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
