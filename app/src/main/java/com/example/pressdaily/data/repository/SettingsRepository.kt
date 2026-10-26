package com.example.pressdaily.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.pressdaily.data.model.AccentTheme
import com.example.pressdaily.data.model.AlarmSoundOption
import com.example.pressdaily.data.model.AppSettings
import com.example.pressdaily.data.model.NotificationStyle
import com.example.pressdaily.data.model.ThemeMode
import com.example.pressdaily.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pressdaily_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val accentStr = prefs.getString("accent_theme", AccentTheme.TEAL.name) ?: AccentTheme.TEAL.name
        val soundStr = prefs.getString("alarm_sound", AlarmSoundOption.GENTLE_CHIME.name) ?: AlarmSoundOption.GENTLE_CHIME.name
        val styleStr = prefs.getString("notification_style", NotificationStyle.BOTH.name) ?: NotificationStyle.BOTH.name

        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs.getBoolean("dynamic_color", false),
            accentTheme = runCatching { AccentTheme.valueOf(accentStr) }.getOrDefault(AccentTheme.TEAL),
            highLegibility = prefs.getBoolean("high_legibility", false),
            alarmsEnabled = prefs.getBoolean("alarms_enabled", true),
            notificationStyle = runCatching { NotificationStyle.valueOf(styleStr) }.getOrDefault(NotificationStyle.BOTH),
            alarmSound = runCatching { AlarmSoundOption.valueOf(soundStr) }.getOrDefault(AlarmSoundOption.GENTLE_CHIME),
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
            .apply()
    }

    fun playSoundPreview(sound: AlarmSoundOption) {
        if (sound.toneId >= 0) {
            try {
                toneGenerator?.startTone(sound.toneId, 400)
            } catch (_: Exception) {}
        }
        if (_settings.value.vibrateOnAlarm || sound == AlarmSoundOption.VIBRATE_ONLY) {
            vibrateDevice()
        }
    }

    fun triggerLiveDoseAlert(
        medicationName: String = "Lisinopril",
        dosage: String = "10 mg",
        time: String = "08:00",
        instructions: String = "Take with food and a full glass of water",
        remainingSupply: Int = 22
    ) {
        val style = _settings.value.notificationStyle
        if (style == NotificationStyle.BOTH || style == NotificationStyle.HEADS_UP) {
            NotificationHelper.showDoseNotification(
                context = context,
                medicationId = "med_lisinopril",
                medicationName = medicationName,
                dosage = dosage,
                scheduledTime = time,
                instructions = instructions,
                remainingSupply = remainingSupply
            )
        }
        playSoundPreview(_settings.value.alarmSound)
    }

    private fun vibrateDevice() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(250)
            }
        } catch (_: Exception) {}
    }
}
