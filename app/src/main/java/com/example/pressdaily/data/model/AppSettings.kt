package com.example.pressdaily.data.model

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Pure Black")
}

enum class AccentTheme(val label: String, val primaryColorHex: Long, val secondaryColorHex: Long) {
    TEAL("Pulse Teal", 0xFF0D9488, 0xFF06B6D4),
    CYAN("Ocean Cyan", 0xFF06B6D4, 0xFF0284C7),
    CORAL("Coral Rose", 0xFFF43F5E, 0xFFFB7185),
    VIOLET("Royal Violet", 0xFF8B5CF6, 0xFFA78BFA),
    EMERALD("Emerald Green", 0xFF10B981, 0xFF34D399),
    AMBER("Sunset Amber", 0xFFF59E0B, 0xFFFBBF24),
    BLUE("Cobalt Blue", 0xFF2563EB, 0xFF60A5FA)
}

enum class SoundType {
    SYSTEM_ALARM,
    SYSTEM_RINGTONE,
    SYSTEM_NOTIFICATION,
    MELODY_ESPRESSO,
    MELODY_SUNRISE,
    MELODY_ZEN,
    MELODY_HARP,
    MELODY_ALARM,
    VIBRATE,
    SILENT
}

enum class AlarmSoundOption(
    val label: String,
    val soundType: SoundType,
    val description: String
) {
    SYSTEM_ALARM("System Alarm Ringtone", SoundType.SYSTEM_ALARM, "Full device alarm ringtone (urgent waking sound)"),
    SYSTEM_RINGTONE("Phone Call Ringtone", SoundType.SYSTEM_RINGTONE, "Standard phone call ringtone melody"),
    SYSTEM_NOTIFICATION("System Notification Chime", SoundType.SYSTEM_NOTIFICATION, "Quick default device notification alert"),
    WARM_ESPRESSO("Warm Espresso Melody", SoundType.MELODY_ESPRESSO, "Acoustic ascending marimba chords"),
    MORNING_SUNRISE("Morning Sunrise Chime", SoundType.MELODY_SUNRISE, "Bright acoustic arpeggio bell tones"),
    ZEN_TEMPLE("Zen Temple Bell", SoundType.MELODY_ZEN, "Deep resonant Tibetan singing bowl chime"),
    VIBRANT_HARP("Vibrant Harp Chords", SoundType.MELODY_HARP, "Gentle multi-chord soothing harp melody"),
    CLASSIC_ALARM("Classic Urgent Alarm", SoundType.MELODY_ALARM, "High-attention pulsating dual chime"),
    VIBRATE_ONLY("Vibrate Only", SoundType.VIBRATE, "Continuous haptic vibration pattern with no audio"),
    SILENT("Silent", SoundType.SILENT, "Visual notification only (no sound or vibration)")
}

enum class NotificationStyle(val label: String, val description: String) {
    BOTH("Both (Heads-Up & Full-Screen)", "Top status banner with action buttons plus prominent full-screen alarm modal"),
    HEADS_UP("Status Bar Heads-Up Only", "Top drop-down notification banner with quick Take Dose and Snooze buttons"),
    FULL_SCREEN("Full-Screen Alarm Only", "Urgent full-screen alert like an alarm clock with large action buttons")
}

data class AppSettings(
    // Theming options
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val accentTheme: AccentTheme = AccentTheme.TEAL,
    val highLegibility: Boolean = false,

    // Alarm & Reminder settings
    val alarmsEnabled: Boolean = true,
    val notificationStyle: NotificationStyle = NotificationStyle.BOTH,
    val alarmSound: AlarmSoundOption = AlarmSoundOption.SYSTEM_ALARM,
    val vibrateOnAlarm: Boolean = true,
    val advanceNoticeMinutes: Int = 0,
    val snoozeDurationMinutes: Int = 10,
    val persistentReminders: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)
