package com.example.pillpulse.data.model

enum class DoseStatus {
    TAKEN,
    SKIPPED,
    SNOOZED
}

data class DoseLog(
    val id: String,
    val medicationId: String,
    val medicationName: String,
    val scheduledTime: String,
    val loggedAtTimestamp: Long = System.currentTimeMillis(),
    val status: DoseStatus = DoseStatus.TAKEN,
    val note: String = "",
    val dayKey: String // Format: YYYY-MM-DD
)

data class ScheduledDoseItem(
    val medication: Medication,
    val scheduledTime: String,
    val status: DoseStatus?, // null if pending
    val logId: String? = null
)

data class AdherenceStats(
    val todayTaken: Int,
    val todayTotal: Int,
    val todayPercentage: Int,
    val currentStreakDays: Int,
    val weeklyPercentage: Int,
    val totalDosesLogged: Int
)
