package com.example.pressdaily.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.pressdaily.data.model.NotificationStyle
import com.example.pressdaily.data.repository.SettingsRepository
import com.example.pressdaily.ui.alarm.FullScreenAlarmActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Acquire WakeLock to keep CPU alive through deep Doze mode
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "PressDaily:DoseAlarmReceiver"
        )
        try {
            wakeLock?.acquire(45_000L) // 45 seconds timeout
        } catch (_: Exception) {}

        val medicationId = intent.getStringExtra(FullScreenAlarmActivity.EXTRA_MED_ID) ?: return
        val medicationName = intent.getStringExtra(FullScreenAlarmActivity.EXTRA_MED_NAME) ?: "Medication"
        val dosage = intent.getStringExtra(FullScreenAlarmActivity.EXTRA_DOSAGE) ?: ""
        val scheduledTime = intent.getStringExtra(FullScreenAlarmActivity.EXTRA_TIME) ?: ""
        val instructions = intent.getStringExtra(FullScreenAlarmActivity.EXTRA_INSTRUCTIONS) ?: ""
        val remainingSupply = intent.getIntExtra(FullScreenAlarmActivity.EXTRA_REMAINING_SUPPLY, 0)
        val isSnooze = intent.getBooleanExtra(MedicationScheduler.EXTRA_IS_SNOOZE, false)

        Log.d(TAG, "Dose reminder alarm triggered for $medicationName ($dosage) at $scheduledTime (isSnooze=$isSnooze)")

        val settingsRepo = SettingsRepository(context.applicationContext)
        val settings = settingsRepo.settings.value

        // Check Quiet Hours
        val isQuietHour = if (settings.quietHoursEnabled) {
            isInQuietHours(settings.quietHoursStart, settings.quietHoursEnd)
        } else {
            false
        }

        val soundToUse = if (isQuietHour) {
            com.example.pressdaily.data.model.AlarmSoundOption.SILENT
        } else {
            settings.alarmSound
        }

        // 1. Post Notification with Activity-based Full Screen Intent
        NotificationHelper.showDoseNotification(
            context = context,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            instructions = instructions,
            remainingSupply = remainingSupply,
            alarmSound = soundToUse
        )

        // 2. Launch FullScreenAlarmActivity directly if style includes Full-Screen
        if (settings.notificationStyle == NotificationStyle.BOTH || settings.notificationStyle == NotificationStyle.FULL_SCREEN) {
            val alarmIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(FullScreenAlarmActivity.EXTRA_MED_ID, medicationId)
                putExtra(FullScreenAlarmActivity.EXTRA_MED_NAME, medicationName)
                putExtra(FullScreenAlarmActivity.EXTRA_DOSAGE, dosage)
                putExtra(FullScreenAlarmActivity.EXTRA_TIME, scheduledTime)
                putExtra(FullScreenAlarmActivity.EXTRA_INSTRUCTIONS, instructions)
                putExtra(FullScreenAlarmActivity.EXTRA_REMAINING_SUPPLY, remainingSupply)
                putExtra(FullScreenAlarmActivity.EXTRA_SOUND_NAME, soundToUse.name)
            }
            runCatching {
                context.startActivity(alarmIntent)
            }.onFailure { e ->
                Log.w(TAG, "Direct activity start restricted by system, falling back to full-screen notification intent", e)
            }
        }

        // 3. If this was a regular daily alarm (not a snooze), queue tomorrow's recurrence
        if (!isSnooze) {
            MedicationScheduler.scheduleNextRecurringAlarm(context, medicationId, scheduledTime)
        }
    }

    private fun isInQuietHours(startStr: String, endStr: String): Boolean {
        try {
            val nowFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentStr = nowFormat.format(Date())

            val currentMinutes = toMinutes(currentStr)
            val startMinutes = toMinutes(startStr)
            val endMinutes = toMinutes(endStr)

            return if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Crosses midnight (e.g. 22:00 to 07:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (_: Exception) {
            return false
        }
    }

    private fun toMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (h * 60) + m
    }
}
