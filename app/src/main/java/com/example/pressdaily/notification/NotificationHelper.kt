package com.example.pressdaily.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pressdaily.MainActivity
import com.example.pressdaily.R

object NotificationHelper {
    const val CHANNEL_ID = "pressdaily_dose_reminders"
    const val CHANNEL_NAME = "PressDaily Dose Reminders"
    const val NOTIFICATION_ID = 1001

    const val ACTION_TAKE_DOSE = "com.example.pressdaily.ACTION_TAKE_DOSE"
    const val ACTION_SNOOZE = "com.example.pressdaily.ACTION_SNOOZE"
    const val EXTRA_MED_ID = "extra_med_id"
    const val EXTRA_MED_NAME = "extra_med_name"
    const val EXTRA_TIME = "extra_time"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent dose reminders, alarms, and scheduled medication alerts"
                enableLights(true)
                enableVibration(true)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showDoseNotification(
        context: Context,
        medicationId: String,
        medicationName: String,
        dosage: String,
        scheduledTime: String,
        instructions: String = "",
        remainingSupply: Int = 0
    ) {
        createNotificationChannel(context)

        // Intent to open app when tapping notification body
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MED_ID, medicationId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Take Dose directly from notification shade
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TAKE_DOSE
            putExtra(EXTRA_MED_ID, medicationId)
            putExtra(EXTRA_MED_NAME, medicationName)
            putExtra(EXTRA_TIME, scheduledTime)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 10 minutes
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_MED_ID, medicationId)
            putExtra(EXTRA_MED_NAME, medicationName)
            putExtra(EXTRA_TIME, scheduledTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            append("Scheduled for $scheduledTime • $dosage")
            if (instructions.isNotEmpty()) append(" • $instructions")
            if (remainingSupply > 0) append(" ($remainingSupply doses left)")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Time for $medicationName ($dosage)")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "✅ Take Dose",
                takePendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_reminder,
                "⏳ Snooze 10m",
                snoozePendingIntent
            )

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Handled when notification permission is denied
        }
    }

    fun cancelNotification(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(NOTIFICATION_ID)
    }
}
