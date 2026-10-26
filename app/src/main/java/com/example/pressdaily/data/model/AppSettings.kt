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

enum class AlarmSoundOption(val label: String, val toneId: Int) {
    GENTLE_CHIME("Gentle Chime", 24),
    PULSE_BELL("Pulse Bell", 28),
    DIGITAL_BEEP("Digital Beep", 2),
    ALARM_BUZZ("Radiant Alarm", 96),
    VIBRATE_ONLY("Vibrate Only", -1),
    SILENT("Silent", -2)
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
    val alarmSound: AlarmSoundOption = AlarmSoundOption.GENTLE_CHIME,
    val vibrateOnAlarm: Boolean = true,
    val advanceNoticeMinutes: Int = 0,
    val snoozeDurationMinutes: Int = 10,
    val persistentReminders: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)
