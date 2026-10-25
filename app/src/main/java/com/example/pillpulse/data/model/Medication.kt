package com.example.pillpulse.data.model

enum class MedicationForm(val label: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    LIQUID("Liquid / Syrup"),
    INJECTION("Injection"),
    INHALER("Inhaler"),
    DROPS("Drops"),
    TOPICAL("Topical Cream / Patch")
}

enum class FrequencyType(val label: String) {
    ONCE_DAILY("Once Daily"),
    TWICE_DAILY("Twice Daily"),
    THREE_TIMES_DAILY("3 Times Daily"),
    AS_NEEDED("As Needed (PRN)"),
    WEEKLY("Weekly")
}

enum class FoodInstruction(val label: String) {
    NO_RESTRICTION("Any time"),
    WITH_FOOD("With food"),
    BEFORE_FOOD("Before meals"),
    AFTER_FOOD("After meals")
}

data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val form: MedicationForm = MedicationForm.TABLET,
    val frequency: FrequencyType = FrequencyType.ONCE_DAILY,
    val scheduledTimes: List<String> = listOf("08:00"),
    val foodInstruction: FoodInstruction = FoodInstruction.NO_RESTRICTION,
    val colorHex: Long = 0xFF0D9488, // Default teal
    val totalQuantity: Int = 30,
    val remainingQuantity: Int = 30,
    val refillThreshold: Int = 7,
    val rxNumber: String = "",
    val instructions: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
