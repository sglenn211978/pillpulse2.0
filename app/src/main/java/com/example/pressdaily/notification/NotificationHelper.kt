package com.example.pressdaily.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pressdaily.MainActivity
import com.example.pressdaily.R
import com.example.pressdaily.data.model.AlarmSoundOption
import com.example.pressdaily.data.model.SoundType
import com.example.pressdaily.ui.alarm.FullScreenAlarmActivity

object NotificationHelper {
    const val DEFAULT_NOTIFICATION_ID = 1001

    const val ACTION_TAKE_NOW = "com.example.pressdaily.ACTION_TAKE_NOW"
    const val ACTION_TAKEN = "com.example.pressdaily.ACTION_TAKEN"
    const val ACTION_TAKE_DOSE = "com.example.pressdaily.ACTION_TAKE_DOSE"
    const val ACTION_SNOOZE = "com.example.pressdaily.ACTION_SNOOZE"
    const val EXTRA_MED_ID = "extra_med_id"
    const val EXTRA_MED_NAME = "extra_med_name"
    const val EXTRA_TIME = "extra_time"

    fun getNotificationId(medicationId: String, scheduledTime: String): Int {
        val timeParts = scheduledTime.split(":")
        val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val idHash = (medicationId.hashCode() and 0x3FFF)
        return 10000 + (idHash * 10) + ((h * 60 + m) % 10)
    }

    fun getChannelId(sound: AlarmSoundOption): String {
        return "pressdaily_reminder_${sound.name.lowercase()}"
    }

    fun createNotificationChannel(context: Context, sound: AlarmSoundOption = RingtonePreferences.getSavedRingtone(context)) {
        RingtonePreferences.applyRingtoneToNotificationChannel(context, sound)
    }

    fun showDoseNotification(
        context: Context,
        medicationId: String,
        medicationName: String,
        dosage: String,
        scheduledTime: String,
        instructions: String = "",
        remainingSupply: Int = 0,
        alarmSound: AlarmSoundOption = RingtonePreferences.getSavedRingtone(context)
    ) {
        val channelId = RingtonePreferences.applyRingtoneToNotificationChannel(context, alarmSound)
        val notificationId = getNotificationId(medicationId, scheduledTime)

        // Activity-based Full Screen Intent pointing to FullScreenAlarmActivity
        val fullScreenIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(FullScreenAlarmActivity.EXTRA_MED_ID, medicationId)
            putExtra(FullScreenAlarmActivity.EXTRA_MED_NAME, medicationName)
            putExtra(FullScreenAlarmActivity.EXTRA_DOSAGE, dosage)
            putExtra(FullScreenAlarmActivity.EXTRA_TIME, scheduledTime)
            putExtra(FullScreenAlarmActivity.EXTRA_INSTRUCTIONS, instructions)
            putExtra(FullScreenAlarmActivity.EXTRA_REMAINING_SUPPLY, remainingSupply)
            putExtra(FullScreenAlarmActivity.EXTRA_SOUND_NAME, alarmSound.name)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 4,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open app when tapping notification body
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MED_ID, medicationId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 10 + 5,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Take Now" action button
        val takeNowIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TAKE_NOW
            putExtra(EXTRA_MED_ID, medicationId)
            putExtra(EXTRA_MED_NAME, medicationName)
            putExtra(EXTRA_TIME, scheduledTime)
        }
        val takeNowPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takeNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Taken" action button
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_MED_ID, medicationId)
            putExtra(EXTRA_MED_NAME, medicationName)
            putExtra(EXTRA_TIME, scheduledTime)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Snooze" action button
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_MED_ID, medicationId)
            putExtra(EXTRA_MED_NAME, medicationName)
            putExtra(EXTRA_TIME, scheduledTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            append("Scheduled for $scheduledTime • $dosage")
            if (instructions.isNotEmpty()) append(" • $instructions")
            if (remainingSupply > 0) append(" ($remainingSupply doses left)")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Time for $medicationName ($dosage)")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true) // Keeps notification visible until Taken, Snoozed, or Cancelled
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "💊 Take Now",
                takeNowPendingIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✅ Taken",
                takenPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_reminder,
                "⏳ Snooze 10m",
                snoozePendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun cancelNotification(context: Context, medicationId: String? = null, scheduledTime: String? = null) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (medicationId != null && scheduledTime != null) {
                notificationManager.cancel(getNotificationId(medicationId, scheduledTime))
            } else {
                notificationManager.cancel(DEFAULT_NOTIFICATION_ID)
                notificationManager.cancelAll()
            }
        } catch (_: Exception) {}
    }
}
