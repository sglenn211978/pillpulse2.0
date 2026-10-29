package com.example.pressdaily.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.pressdaily.data.model.DoseStatus
import com.example.pressdaily.data.repository.MedicationRepository
import com.example.pressdaily.data.repository.SettingsRepository

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra(NotificationHelper.EXTRA_MED_ID) ?: return
        val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MED_NAME) ?: "Medication"
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_TIME) ?: "Scheduled"

        val repository = MedicationRepository(context.applicationContext)
        val settingsRepo = SettingsRepository(context.applicationContext)
        val snoozeMins = settingsRepo.settings.value.snoozeDurationMinutes

        PressDailySoundManager.stopAlarm()

        when (intent.action) {
            NotificationHelper.ACTION_TAKE_NOW -> {
                repository.logDose(
                    medicationId = medicationId,
                    scheduledTime = scheduledTime,
                    status = DoseStatus.TAKEN,
                    note = "Taking dose right now"
                )
                NotificationHelper.cancelNotification(context)
                Toast.makeText(
                    context,
                    "💊 Taking $medicationName ($scheduledTime) right now — recorded!",
                    Toast.LENGTH_LONG
                ).show()
            }

            NotificationHelper.ACTION_TAKEN, NotificationHelper.ACTION_TAKE_DOSE -> {
                repository.logDose(
                    medicationId = medicationId,
                    scheduledTime = scheduledTime,
                    status = DoseStatus.TAKEN,
                    note = "Confirmed as Taken"
                )
                NotificationHelper.cancelNotification(context)
                Toast.makeText(
                    context,
                    "✅ $medicationName ($scheduledTime) marked as Taken!",
                    Toast.LENGTH_LONG
                ).show()
            }

            NotificationHelper.ACTION_SNOOZE -> {
                NotificationHelper.cancelNotification(context)
                // Schedule actual AlarmManager alarm in snoozeMins minutes
                MedicationScheduler.scheduleSnooze(
                    context = context,
                    medicationId = medicationId,
                    medicationName = medicationName,
                    dosage = "",
                    scheduledTime = scheduledTime,
                    snoozeMinutes = snoozeMins
                )
                Toast.makeText(
                    context,
                    "⏳ Snoozed $medicationName for $snoozeMins minutes — will alarm again",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
