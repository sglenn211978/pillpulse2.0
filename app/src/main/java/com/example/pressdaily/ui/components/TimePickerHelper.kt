package com.example.pressdaily.ui.components

import android.app.TimePickerDialog
import android.content.Context
import java.util.Locale

object TimePickerHelper {
    /**
     * Opens the native Android circular clock dial time picker dialog.
     * Supports both 12-hour clock face with AM/PM and keyboard mode.
     */
    fun showClockTimePicker(
        context: Context,
        initialTime: String = "08:00",
        onTimeSelected: (String) -> Unit
    ) {
        val parts = initialTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formatted)
            },
            hour,
            minute,
            false // 12-hour AM/PM clock
        ).show()
    }

    /**
     * Formats "08:00" to "8:00 AM", "19:30" to "7:30 PM", etc.
     */
    fun formatDisplayTime(time24: String): String {
        return try {
            val parts = time24.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val amPm = if (h >= 12) "PM" else "AM"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            String.format(Locale.getDefault(), "%d:%02d %s", h12, m, amPm)
        } catch (_: Exception) {
            time24
        }
    }
}
