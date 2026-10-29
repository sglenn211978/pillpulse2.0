package com.example.pressdaily.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.pressdaily.data.model.AccentTheme
import com.example.pressdaily.data.model.AlarmSoundOption
import com.example.pressdaily.data.model.AppSettings
import com.example.pressdaily.data.model.NotificationStyle
import com.example.pressdaily.data.model.ThemeMode
import com.example.pressdaily.notification.MedicationScheduler
import com.example.pressdaily.notification.NotificationHelper
import com.example.pressdaily.notification.PressDailySoundManager
import com.example.pressdaily.notification.RingtonePreferences
import com.example.pressdaily.ui.alarm.FullScreenAlarmActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pressdaily_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        // Ensure the saved ringtone is applied to the system NotificationChannel on initialization
        RingtonePreferences.applyRingtoneToNotificationChannel(context, _settings.value.alarmSound)
    }

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val accentStr = prefs.getString("accent_theme", AccentTheme.TEAL.name) ?: AccentTheme.TEAL.name
        val sound = RingtonePreferences.getSavedRingtone(context)
        val styleStr = prefs.getString("notification_style", NotificationStyle.BOTH.name) ?: NotificationStyle.BOTH.name

        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs.getBoolean("dynamic_color", false),
            accentTheme = runCatching { AccentTheme.valueOf(accentStr) }.getOrDefault(AccentTheme.TEAL),
            highLegibility = prefs.getBoolean("high_legibility", false),
            alarmsEnabled = prefs.getBoolean("alarms_enabled", true),
            notificationStyle = runCatching { NotificationStyle.valueOf(styleStr) }.getOrDefault(NotificationStyle.BOTH),
            alarmSound = sound,
            vibrateOnAlarm = prefs.getBoolean("vibrate_on_alarm", true),
            advanceNoticeMinutes = prefs.getInt("advance_notice_min", 0),
            snoozeDurationMinutes = prefs.getInt("snooze_duration_min", 10),
            persistentReminders = prefs.getBoolean("persistent_reminders", true),
            quietHoursEnabled = prefs.getBoolean("quiet_hours_enabled", false),
            quietHoursStart = prefs.getString("quiet_hours_start", "22:00") ?: "22:00",
            quietHoursEnd = prefs.getString("quiet_hours_end", "07:00") ?: "07:00"
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings

        // Persist ringtone choice in SharedPreferences and apply immediately to system notification channel
        RingtonePreferences.saveAndApplyRingtone(context, newSettings.alarmSound)

        // Use commit() to guarantee synchronous persistence to disk across restarts
        prefs.edit()
            .putString("theme_mode", newSettings.themeMode.name)
            .putBoolean("dynamic_color", newSettings.dynamicColor)
            .putString("accent_theme", newSettings.accentTheme.name)
            .putBoolean("high_legibility", newSettings.highLegibility)
            .putBoolean("alarms_enabled", newSettings.alarmsEnabled)
            .putString("notification_style", newSettings.notificationStyle.name)
            .putString("alarm_sound", newSettings.alarmSound.name)
            .putBoolean("vibrate_on_alarm", newSettings.vibrateOnAlarm)
            .putInt("advance_notice_min", newSettings.advanceNoticeMinutes)
            .putInt("snooze_duration_min", newSettings.snoozeDurationMinutes)
            .putBoolean("persistent_reminders", newSettings.persistentReminders)
            .putBoolean("quiet_hours_enabled", newSettings.quietHoursEnabled)
            .putString("quiet_hours_start", newSettings.quietHoursStart)
            .putString("quiet_hours_end", newSettings.quietHoursEnd)
            .commit()

        // Reschedule alarms with updated settings (e.g. advance notice, quiet hours, alarms enabled)
        MedicationScheduler.scheduleAllAlarms(context)
    }

    fun playSoundPreview(sound: AlarmSoundOption) {
        PressDailySoundManager.playPreview(context, sound)
    }

    fun stopSoundPreview() {
        PressDailySoundManager.stopSound()
    }

    fun triggerLiveDoseAlert(
        medicationName: String = "Lisinopril",
        dosage: String = "10 mg",
        time: String = "08:00",
        instructions: String = "Take with food and a full glass of water",
        remainingSupply: Int = 22
    ) {
        val current = _settings.value

        // Post notification with Activity-based full screen intent
        NotificationHelper.showDoseNotification(
            context = context,
            medicationId = "med_lisinopril",
            medicationName = medicationName,
            dosage = dosage,
            scheduledTime = time,
            instructions = instructions,
            remainingSupply = remainingSupply,
            alarmSound = current.alarmSound
        )

        // Launch full-screen alarm activity directly
        val alarmIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(FullScreenAlarmActivity.EXTRA_MED_ID, "med_lisinopril")
            putExtra(FullScreenAlarmActivity.EXTRA_MED_NAME, medicationName)
            putExtra(FullScreenAlarmActivity.EXTRA_DOSAGE, dosage)
            putExtra(FullScreenAlarmActivity.EXTRA_TIME, time)
            putExtra(FullScreenAlarmActivity.EXTRA_INSTRUCTIONS, instructions)
            putExtra(FullScreenAlarmActivity.EXTRA_REMAINING_SUPPLY, remainingSupply)
            putExtra(FullScreenAlarmActivity.EXTRA_SOUND_NAME, current.alarmSound.name)
        }
        context.startActivity(alarmIntent)
    }
}
