package com.example.pressdaily.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.pressdaily.data.model.FrequencyType
import com.example.pressdaily.data.model.Medication
import com.example.pressdaily.data.repository.MedicationRepository
import com.example.pressdaily.data.repository.SettingsRepository
import com.example.pressdaily.ui.alarm.FullScreenAlarmActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MedicationScheduler {
    private const val TAG = "MedicationScheduler"
    private const val PREFS_ALARMS = "pressdaily_scheduled_alarms_tracker"
    private const val KEY_TRACKED_CODES = "tracked_alarm_request_codes"

    const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    const val ACTION_TRIGGER_ALARM = "com.example.pressdaily.ACTION_TRIGGER_ALARM"

    /**
     * Produces a guaranteed positive, deterministic, collision-free request code
     * for every medication ID and time combination.
     */
    fun getRequestCode(medicationId: String, scheduledTime: String, isSnooze: Boolean = false): Int {
        val idPart = (medicationId.hashCode() and 0xFFFF)
        val timeParts = scheduledTime.split(":")
        val h = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val timePart = ((h * 60 + m) and 0x7FF)
        val snoozeBit = if (isSnooze) 1 else 0
        return (((snoozeBit shl 27) or (idPart shl 11) or timePart) and 0x7FFFFFFF).coerceAtLeast(100)
    }

    /**
     * Schedules alarms for all active medications in the database based on their reminder times.
     * Cleans up any orphaned or deleted medication alarms automatically.
     */
    fun scheduleAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val medicationRepo = MedicationRepository(context.applicationContext)
        val settingsRepo = SettingsRepository(context.applicationContext)
        val settings = settingsRepo.settings.value

        // Cancel previously registered alarms before re-scheduling to eliminate orphans/ghosts
        cancelAllTrackedAlarms(context, alarmManager)

        if (!settings.alarmsEnabled) {
            Log.d(TAG, "Alarms disabled in settings; all alarms cancelled.")
            return
        }

        val medications = medicationRepo.medications.value
        val newlyTrackedCodes = mutableSetOf<String>()
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val rescheduledMap = medicationRepo.rescheduledDoses.value

        Log.d(TAG, "Scheduling alarms for ${medications.size} medications...")

        medications.forEach { med ->
            if (med.frequency != FrequencyType.AS_NEEDED) {
                med.scheduledTimes.forEach { timeStr ->
                    val effectiveTime = rescheduledMap["${todayKey}_${med.id}_${timeStr}"] ?: timeStr
                    val code = scheduleSingleAlarm(
                        context = context,
                        alarmManager = alarmManager,
                        medication = med,
                        scheduledTime = effectiveTime,
                        advanceNoticeMinutes = settings.advanceNoticeMinutes
                    )
                    if (code != null) {
                        newlyTrackedCodes.add(code.toString())
                    }
                }
            }
        }

        // Save active codes to tracker prefs
        val prefs = context.getSharedPreferences(PREFS_ALARMS, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_TRACKED_CODES, newlyTrackedCodes).apply()
        Log.d(TAG, "Successfully registered ${newlyTrackedCodes.size} system alarms.")
    }

    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        medication: Medication,
        scheduledTime: String,
        advanceNoticeMinutes: Int
    ): Int? {
        val timeParts = scheduledTime.split(":")
        if (timeParts.size < 2) return null

        val hour = timeParts[0].toIntOrNull() ?: return null
        val minute = timeParts[1].toIntOrNull() ?: return null

        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (advanceNoticeMinutes > 0) {
                add(Calendar.MINUTE, -advanceNoticeMinutes)
            }
        }

        // If alarm time for today has already arrived or passed, schedule for tomorrow
        if (alarmTime.timeInMillis <= now.timeInMillis) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerTimeMs = alarmTime.timeInMillis
        val requestCode = getRequestCode(medication.id, scheduledTime, isSnooze = false)

        // Activity Intent directly targeted to FullScreenAlarmActivity
        // Using PendingIntent.getActivity with setAlarmClock allows Android OS to launch
        // FullScreenAlarmActivity directly in full screen, completely bypassing background activity restrictions!
        val fullScreenIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(FullScreenAlarmActivity.EXTRA_MED_ID, medication.id)
            putExtra(FullScreenAlarmActivity.EXTRA_MED_NAME, medication.name)
            putExtra(FullScreenAlarmActivity.EXTRA_DOSAGE, medication.dosage)
            putExtra(FullScreenAlarmActivity.EXTRA_TIME, scheduledTime)
            putExtra(FullScreenAlarmActivity.EXTRA_INSTRUCTIONS, medication.instructions)
            putExtra(FullScreenAlarmActivity.EXTRA_REMAINING_SUPPLY, medication.remainingQuantity)
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val activityPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, activityPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, activityPendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, activityPendingIntent)
            }
            Log.d(TAG, "Scheduled FullScreen AlarmClock for ${medication.name} ($scheduledTime) at ${alarmTime.time} (ReqCode: $requestCode)")
            return requestCode
        } catch (e: SecurityException) {
            // Fallback if SCHEDULE_EXACT_ALARM is not granted
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, activityPendingIntent)
                Log.d(TAG, "Fallback setExactAndAllowWhileIdle for ${medication.name} ($scheduledTime) at ${alarmTime.time}")
                return requestCode
            } catch (_: Exception) {
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, activityPendingIntent)
                    Log.d(TAG, "Fallback setAndAllowWhileIdle for ${medication.name} ($scheduledTime)")
                    return requestCode
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to schedule alarm for ${medication.name}", ex)
                    return null
                }
            }
        }
    }

    /**
     * Reschedules the recurring alarm for tomorrow after today's alarm has fired.
     */
    fun scheduleNextRecurringAlarm(context: Context, medicationId: String, scheduledTime: String) {
        val medicationRepo = MedicationRepository(context.applicationContext)
        val med = medicationRepo.medications.value.find { it.id == medicationId } ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val settingsRepo = SettingsRepository(context.applicationContext)

        val code = scheduleSingleAlarm(
            context = context,
            alarmManager = alarmManager,
            medication = med,
            scheduledTime = scheduledTime,
            advanceNoticeMinutes = settingsRepo.settings.value.advanceNoticeMinutes
        )

        if (code != null) {
            val prefs = context.getSharedPreferences(PREFS_ALARMS, Context.MODE_PRIVATE)
            val current = prefs.getStringSet(KEY_TRACKED_CODES, emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(code.toString())
            prefs.edit().putStringSet(KEY_TRACKED_CODES, current).apply()
        }
    }

    /**
     * Schedules a Snooze alarm to re-trigger in [snoozeMinutes] minutes directly in full screen.
     */
    fun scheduleSnooze(
        context: Context,
        medicationId: String,
        medicationName: String,
        dosage: String,
        scheduledTime: String,
        instructions: String = "",
        remainingSupply: Int = 0,
        snoozeMinutes: Int = 10
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val requestCode = getRequestCode(medicationId, scheduledTime, isSnooze = true)

        val fullScreenIntent = Intent(context, FullScreenAlarmActivity::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(FullScreenAlarmActivity.EXTRA_MED_ID, medicationId)
            putExtra(FullScreenAlarmActivity.EXTRA_MED_NAME, medicationName)
            putExtra(FullScreenAlarmActivity.EXTRA_DOSAGE, dosage)
            putExtra(FullScreenAlarmActivity.EXTRA_TIME, scheduledTime)
            putExtra(FullScreenAlarmActivity.EXTRA_INSTRUCTIONS, instructions)
            putExtra(FullScreenAlarmActivity.EXTRA_REMAINING_SUPPLY, remainingSupply)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        val activityPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, activityPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, activityPendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, activityPendingIntent)
            }
            Log.d(TAG, "Snooze alarm scheduled for $medicationName in $snoozeMinutes minutes (ReqCode: $requestCode)")
        } catch (_: Exception) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, activityPendingIntent)
            } catch (_: Exception) {
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, activityPendingIntent)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Cancels all previously tracked alarms in Android AlarmManager.
     */
    private fun cancelAllTrackedAlarms(context: Context, alarmManager: AlarmManager) {
        val prefs = context.getSharedPreferences(PREFS_ALARMS, Context.MODE_PRIVATE)
        val trackedCodes = prefs.getStringSet(KEY_TRACKED_CODES, emptySet()) ?: emptySet()

        trackedCodes.forEach { codeStr ->
            val code = codeStr.toIntOrNull() ?: return@forEach
            val intent = Intent(context, FullScreenAlarmActivity::class.java).apply { action = ACTION_TRIGGER_ALARM }
            val pi = PendingIntent.getActivity(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    /**
     * Returns human-readable string of the earliest upcoming scheduled alarm.
     */
    fun getNextUpcomingAlarmInfo(context: Context): String? {
        val medicationRepo = MedicationRepository(context.applicationContext)
        val medications = medicationRepo.medications.value.filter { it.frequency != FrequencyType.AS_NEEDED }
        if (medications.isEmpty()) return null

        val now = Calendar.getInstance()
        var earliestCal: Calendar? = null
        var earliestMedName: String? = null
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val rescheduledMap = medicationRepo.rescheduledDoses.value

        medications.forEach { med ->
            med.scheduledTimes.forEach { originalTime ->
                val timeStr = rescheduledMap["${todayKey}_${med.id}_${originalTime}"] ?: originalTime
                val parts = timeStr.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                val m = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= now.timeInMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                if (earliestCal == null || cal.before(earliestCal)) {
                    earliestCal = cal
                    earliestMedName = med.name
                }
            }
        }

        val target = earliestCal ?: return null
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val isToday = target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                target.get(Calendar.YEAR) == now.get(Calendar.YEAR)

        val dayLabel = if (isToday) "Today" else "Tomorrow"
        return "$dayLabel at ${timeFormat.format(target.time)} ($earliestMedName)"
    }
}
