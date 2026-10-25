package com.example.pillpulse.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.pillpulse.data.model.DoseStatus
import com.example.pillpulse.data.repository.MedicationRepository

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra(NotificationHelper.EXTRA_MED_ID) ?: return
        val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MED_NAME) ?: "Medication"
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_TIME) ?: "Scheduled"

        val repository = MedicationRepository(context.applicationContext)

        when (intent.action) {
            NotificationHelper.ACTION_TAKE_DOSE -> {
                repository.logDose(
                    medicationId = medicationId,
                    scheduledTime = scheduledTime,
                    status = DoseStatus.TAKEN,
                    note = "Logged from system notification"
                )
                NotificationHelper.cancelNotification(context)
                Toast.makeText(
                    context,
                    "✅ $medicationName ($scheduledTime) marked as taken!",
                    Toast.LENGTH_LONG
                ).show()
            }

            NotificationHelper.ACTION_SNOOZE -> {
                NotificationHelper.cancelNotification(context)
                Toast.makeText(
                    context,
                    "⏳ Snoozed $medicationName for 10 minutes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
