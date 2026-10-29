package com.example.pressdaily.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.pressdaily.data.model.AlarmSoundOption
import com.example.pressdaily.data.model.SoundType

object RingtonePreferences {
    private const val PREFS_NAME = "pressdaily_ringtone_prefs"
    private const val KEY_SELECTED_RINGTONE = "selected_notification_ringtone"
    private const val KEY_CURRENT_CHANNEL_ID = "current_notification_channel_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retrieve the persisted ringtone choice from SharedPreferences.
     * Guaranteed to persist across application restarts and device reboots.
     */
    fun getSavedRingtone(context: Context): AlarmSoundOption {
        val soundName = getPrefs(context).getString(KEY_SELECTED_RINGTONE, null)
            ?: context.getSharedPreferences("pressdaily_settings", Context.MODE_PRIVATE)
                .getString("alarm_sound", AlarmSoundOption.SYSTEM_ALARM.name)
            ?: AlarmSoundOption.SYSTEM_ALARM.name

        return runCatching { AlarmSoundOption.valueOf(soundName) }.getOrDefault(AlarmSoundOption.SYSTEM_ALARM)
    }

    /**
     * Save the user's selected notification ringtone choice to SharedPreferences synchronously
     * and immediately apply it to the Android system NotificationChannel.
     */
    fun saveAndApplyRingtone(context: Context, sound: AlarmSoundOption) {
        getPrefs(context).edit()
            .putString(KEY_SELECTED_RINGTONE, sound.name)
            .commit()

        applyRingtoneToNotificationChannel(context, sound)
    }

    /**
     * Apply the ringtone to the system notification channel.
     * In Android 8.0+ (Oreo+), NotificationChannels are immutable once created with a sound.
     * We create/register the channel with the selected sound and sound URI, and cleanly remove
     * obsolete channels so system settings stay tidy.
     */
    fun applyRingtoneToNotificationChannel(context: Context, sound: AlarmSoundOption = getSavedRingtone(context)): String {
        val channelId = "pressdaily_ringtone_${sound.name.lowercase()}"
        val channelName = "PressDaily Reminders (${sound.label})"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val oldChannelId = getPrefs(context).getString(KEY_CURRENT_CHANNEL_ID, null)
            if (oldChannelId != null && oldChannelId != channelId) {
                try {
                    notificationManager.deleteNotificationChannel(oldChannelId)
                } catch (_: Exception) {}
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val soundUri: Uri? = when (sound.soundType) {
                SoundType.SYSTEM_ALARM -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                SoundType.SYSTEM_RINGTONE -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                SoundType.SYSTEM_NOTIFICATION,
                SoundType.MELODY_ESPRESSO,
                SoundType.MELODY_SUNRISE,
                SoundType.MELODY_ZEN,
                SoundType.MELODY_HARP,
                SoundType.MELODY_ALARM -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                SoundType.VIBRATE,
                SoundType.SILENT -> null
            }

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent dose reminders and alarm alerts with selected ringtone: ${sound.label}"
                enableLights(true)
                enableVibration(sound != AlarmSoundOption.SILENT)
                if (soundUri != null && sound != AlarmSoundOption.VIBRATE_ONLY && sound != AlarmSoundOption.SILENT) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channel)

            getPrefs(context).edit()
                .putString(KEY_CURRENT_CHANNEL_ID, channelId)
                .commit()
        }

        return channelId
    }
}
