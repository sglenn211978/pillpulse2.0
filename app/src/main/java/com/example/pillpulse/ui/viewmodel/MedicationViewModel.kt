package com.example.pillpulse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillpulse.data.model.AdherenceStats
import com.example.pillpulse.data.model.DoseLog
import com.example.pillpulse.data.model.DoseStatus
import com.example.pillpulse.data.model.FrequencyType
import com.example.pillpulse.data.model.Medication
import com.example.pillpulse.data.model.ScheduledDoseItem
import com.example.pillpulse.data.model.AlarmSoundOption
import com.example.pillpulse.data.model.AppSettings
import com.example.pillpulse.data.repository.MedicationRepository
import com.example.pillpulse.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MedicationRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    val appSettings: StateFlow<AppSettings> = settingsRepository.settings
    val medications: StateFlow<List<Medication>> = repository.medications
    val doseLogs: StateFlow<List<DoseLog>> = repository.doseLogs

    private val _selectedDateKey = MutableStateFlow(repository.getTodayKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _cabinetSearchQuery = MutableStateFlow("")
    val cabinetSearchQuery: StateFlow<String> = _cabinetSearchQuery.asStateFlow()

    private val _cabinetFilter = MutableStateFlow("ALL") // "ALL", "LOW_STOCK", "DAILY", "PRN"
    val cabinetFilter: StateFlow<String> = _cabinetFilter.asStateFlow()

    val adherenceStats: StateFlow<AdherenceStats> = combine(
        medications,
        doseLogs,
        _selectedDateKey
    ) { _, _, dateKey ->
        repository.calculateAdherence(dateKey)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.calculateAdherence()
    )

    val scheduledDoses: StateFlow<List<ScheduledDoseItem>> = combine(
        medications,
        doseLogs,
        _selectedDateKey
    ) { meds, logs, dateKey ->
        val result = mutableListOf<ScheduledDoseItem>()
        val scheduledMeds = meds.filter { it.isActive && it.frequency != FrequencyType.AS_NEEDED }

        scheduledMeds.forEach { med ->
            med.scheduledTimes.forEach { time ->
                val matchingLog = logs.find {
                    it.medicationId == med.id && it.scheduledTime == time && it.dayKey == dateKey
                }
                result.add(
                    ScheduledDoseItem(
                        medication = med,
                        scheduledTime = time,
                        status = matchingLog?.status,
                        logId = matchingLog?.id
                    )
                )
            }
        }
        // Sort by scheduled time string (e.g., "08:00", "12:30", "19:00", "22:00")
        result.sortedBy { it.scheduledTime }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val asNeededMedications: StateFlow<List<Medication>> = medications.combine(
        MutableStateFlow(Unit)
    ) { meds, _ ->
        meds.filter { it.isActive && it.frequency == FrequencyType.AS_NEEDED }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val lowStockMedications: StateFlow<List<Medication>> = medications.combine(
        MutableStateFlow(Unit)
    ) { meds, _ ->
        meds.filter { it.isActive && it.remainingQuantity <= it.refillThreshold }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun setCabinetSearchQuery(query: String) {
        _cabinetSearchQuery.value = query
    }

    fun setCabinetFilter(filter: String) {
        _cabinetFilter.value = filter
    }

    fun takeDose(medicationId: String, scheduledTime: String, note: String = "") {
        viewModelScope.launch {
            repository.logDose(
                medicationId = medicationId,
                scheduledTime = scheduledTime,
                status = DoseStatus.TAKEN,
                dayKey = _selectedDateKey.value,
                note = note
            )
        }
    }

    fun skipDose(medicationId: String, scheduledTime: String, reason: String = "") {
        viewModelScope.launch {
            repository.logDose(
                medicationId = medicationId,
                scheduledTime = scheduledTime,
                status = DoseStatus.SKIPPED,
                dayKey = _selectedDateKey.value,
                note = reason
            )
        }
    }

    fun logAsNeeded(medicationId: String, note: String = "As-needed dose") {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            repository.logDose(
                medicationId = medicationId,
                scheduledTime = currentTime,
                status = DoseStatus.TAKEN,
                dayKey = repository.getTodayKey(),
                note = note
            )
        }
    }

    fun undoDoseLog(logId: String) {
        viewModelScope.launch {
            repository.undoDoseLog(logId)
        }
    }

    fun saveMedication(med: Medication) {
        viewModelScope.launch {
            repository.addMedication(med)
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            repository.deleteMedication(id)
        }
    }

    fun refillMedication(id: String, units: Int) {
        viewModelScope.launch {
            repository.refillMedication(id, units)
        }
    }

    fun generateDoctorSummaryReport(): String {
        val stats = adherenceStats.value
        val meds = medications.value
        val logs = doseLogs.value.take(20)

        val sb = StringBuilder()
        sb.append("📋 PRESSDAILY - MEDICATION & ADHERENCE REPORT\n")
        sb.append("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
        sb.append("═══════════════════════════════════════════\n\n")

        sb.append("📊 ADHERENCE OVERVIEW\n")
        sb.append("• 7-Day Adherence Score: ${stats.weeklyPercentage}%\n")
        sb.append("• Current Adherence Streak: ${stats.currentStreakDays} days\n")
        sb.append("• Total Doses Logged: ${stats.totalDosesLogged}\n\n")

        sb.append("💊 ACTIVE PRESCRIPTION & MEDICINE CABINET (${meds.size} total)\n")
        meds.forEachIndexed { idx, med ->
            sb.append("${idx + 1}. ${med.name} (${med.dosage})\n")
            sb.append("   • Form: ${med.form.label} | Frequency: ${med.frequency.label}\n")
            if (med.scheduledTimes.isNotEmpty()) {
                sb.append("   • Times: ${med.scheduledTimes.joinToString(", ")}\n")
            }
            sb.append("   • Food Relation: ${med.foodInstruction.label}\n")
            sb.append("   • Remaining Supply: ${med.remainingQuantity} / ${med.totalQuantity} doses")
            if (med.remainingQuantity <= med.refillThreshold) {
                sb.append(" [REFILL NEEDED]")
            }
            sb.append("\n")
            if (med.rxNumber.isNotEmpty()) sb.append("   • Rx#: ${med.rxNumber}\n")
            if (med.instructions.isNotEmpty()) sb.append("   • Notes: ${med.instructions}\n")
            sb.append("\n")
        }

        sb.append("🕒 RECENT DOSE HISTORY\n")
        if (logs.isEmpty()) {
            sb.append("No recorded doses yet.\n")
        } else {
            logs.take(10).forEach { log ->
                val statusEmoji = when (log.status) {
                    DoseStatus.TAKEN -> "✅ Taken"
                    DoseStatus.SKIPPED -> "⚠️ Skipped"
                    DoseStatus.SNOOZED -> "⏳ Snoozed"
                }
                sb.append("• ${log.dayKey} ${log.scheduledTime} - ${log.medicationName}: $statusEmoji")
                if (log.note.isNotEmpty()) sb.append(" (${log.note})")
                sb.append("\n")
            }
        }

        return sb.toString()
    }

    fun updateSettings(settings: AppSettings) {
        settingsRepository.updateSettings(settings)
    }

    fun playSoundPreview(sound: AlarmSoundOption) {
        settingsRepository.playSoundPreview(sound)
    }

    fun triggerLiveDoseAlert(
        medicationName: String = "Lisinopril",
        dosage: String = "10 mg",
        time: String = "08:00",
        instructions: String = "Take with food and a full glass of water",
        remainingSupply: Int = 22
    ) {
        settingsRepository.triggerLiveDoseAlert(
            medicationName = medicationName,
            dosage = dosage,
            time = time,
            instructions = instructions,
            remainingSupply = remainingSupply
        )
    }
}
