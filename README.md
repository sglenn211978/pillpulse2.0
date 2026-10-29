# PressDaily ☕💊

PressDaily is a daily habit, routine, and medication tracking application built for Android with Jetpack Compose and Material Design 3.

## Features

- **Daily Medication Pulse & Adherence**:
  - Real-time animated circular pulse meter reflecting daily adherence.
  - Interactive 7-day horizontal calendar strip for reviewing scheduled doses across any date.
  - Visual completion status and real-time adherence progress.

- **Intelligent Dose Scheduling & Action Logging**:
  - One-tap dose logging (`Take Dose`, `Skip Dose` with reason recording, and `Undo`).
  - Doses organized by time slot: Morning, Afternoon, Evening, Bedtime.
  - Clear medication badges indicating strength, pharmaceutical form (tablet, capsule, liquid, inhaler, drops, etc.), and food instructions (with food, before meals, after meals).
  - Quick-log section for As-Needed (PRN) medications (e.g. rescue inhalers, analgesics).

- **Medicine Cabinet & Inventory Tracking**:
  - Complete digital medicine cabinet with live stock counting.
  - Visual supply indicator bars and automated refill alert threshold warnings.
  - 1-tap fast refill or custom refill unit addition.
  - Full CRUD: Add, edit, search, filter, and delete medications.

- **Alarm & Medication Reminders**:
  - Master toggle for dose alarms and notification chimes.
  - 16 custom ringtones with live preview (*Gentle Chime*, *Pulse Bell*, *Warm Espresso*, *Digital Beep*, *Radiant Alarm*, *Soft Melodic*, *Quick Chirp*, *Morning Sunrise*, *Zen Bell*, *Echo Pulse*, *Subtle Ping*, *Deep Resonant*, *System Notification*, *System Alarm*, *Vibrate Only*, *Silent*).
  - Configurable advance notice (at exact time, 5m, 10m, 15m, 30m early).
  - Customizable snooze durations (5m, 10m, 15m, 20m, 30m).
  - Persistent escalation (repeat chime every 5 minutes until dose confirmation).
  - Quiet Hours / Sleep Mode schedule to mute non-urgent chimes during night hours.
  - Live dose alarm simulation tool.

- **Personalization & Theming Options**:
  - Theme mode: System Default, Light, Dark (Midnight Slate), and AMOLED Pure Black (OLED battery saving).
  - 7 custom accent color themes: *Pulse Teal*, *Ocean Cyan*, *Coral Rose*, *Royal Violet*, *Emerald Green*, *Sunset Amber*, and *Cobalt Blue*.
  - Android 12+ Material You Dynamic Color integration (wallpaper-matched palette).
  - High-Legibility Mode with enlarged typography and enhanced contrast for easy prescription reading.

- **Pulse Log & Clinical Adherence Report**:
  - Chronological activity log of all taken and skipped medications with notes.
  - Weekly and all-time adherence performance scores.
  - Formatted Clinical / Doctor Summary Report with 1-tap clipboard export for healthcare visits and caregiver coordination.

## Technical Architecture

- **Platform**: Android SDK 36, Kotlin 2.2, Android Gradle Plugin 9.1.1
- **UI Framework**: Jetpack Compose with Material 3 Design & dynamic theming
- **Architecture**: MVVM with Android ViewModel, Kotlin Coroutines, and StateFlow
- **Data Persistence**: SharedPreferences with robust structured JSON serialization
- **Iconography & Visuals**: Custom adaptive launcher icon with 66dp safe zone and multi-density mipmaps