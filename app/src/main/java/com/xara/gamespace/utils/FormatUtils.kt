package com.xara.gamespace.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatMemoryMb(mb: Long): String {
        return if (mb >= 1024L) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            "$mb MB"
        }
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return "Never"
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
