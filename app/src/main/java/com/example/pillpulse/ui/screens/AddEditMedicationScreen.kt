package com.example.pillpulse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillpulse.data.model.FoodInstruction
import com.example.pillpulse.data.model.FrequencyType
import com.example.pillpulse.data.model.Medication
import com.example.pillpulse.data.model.MedicationForm
import com.example.pillpulse.ui.theme.PulseTeal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    initialMedication: Medication?,
    onSave: (Medication) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var name by remember { mutableStateOf(initialMedication?.name ?: "") }
    var dosage by remember { mutableStateOf(initialMedication?.dosage ?: "") }
    var selectedForm by remember { mutableStateOf(initialMedication?.form ?: MedicationForm.TABLET) }
    var selectedFrequency by remember { mutableStateOf(initialMedication?.frequency ?: FrequencyType.ONCE_DAILY) }
    var scheduledTimes by remember {
        mutableStateOf(
            initialMedication?.scheduledTimes ?: listOf("08:00")
        )
    }
    var foodInstruction by remember { mutableStateOf(initialMedication?.foodInstruction ?: FoodInstruction.NO_RESTRICTION) }
    var totalQuantity by remember { mutableStateOf(initialMedication?.totalQuantity?.toString() ?: "30") }
    var remainingQuantity by remember { mutableStateOf(initialMedication?.remainingQuantity?.toString() ?: "30") }
    var refillThreshold by remember { mutableStateOf(initialMedication?.refillThreshold?.toString() ?: "7") }
    var rxNumber by remember { mutableStateOf(initialMedication?.rxNumber ?: "") }
    var instructions by remember { mutableStateOf(initialMedication?.instructions ?: "") }
    var selectedColorHex by remember { mutableStateOf(initialMedication?.colorHex ?: 0xFF0D9488) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val colorOptions = listOf(
        0xFF0D9488, // Pulse Teal
        0xFF06B6D4, // Cyan
        0xFF6366F1, // Indigo
        0xFF8B5CF6, // Purple
        0xFFF43F5E, // Coral
        0xFFF59E0B, // Amber
        0xFF10B981  // Emerald
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initialMedication == null) "Add Medication" else "Edit Medication",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info
            item {
                Text("Basic Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Medication Name *") },
                    placeholder = { Text("e.g. Amoxicillin, Lisinopril") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null && name.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medication_name_input")
                )
            }

            item {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Strength / Dosage *") },
                    placeholder = { Text("e.g. 500 mg, 10 ml, 2 puffs") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medication_dosage_input")
                )
            }

            // Form Selection Chips
            item {
                Text("Form", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MedicationForm.entries) { form ->
                        FilterChip(
                            selected = selectedForm == form,
                            onClick = { selectedForm = form },
                            label = { Text(form.label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Frequency
            item {
                Text("Frequency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FrequencyType.entries) { freq ->
                        FilterChip(
                            selected = selectedFrequency == freq,
                            onClick = {
                                selectedFrequency = freq
                                scheduledTimes = when (freq) {
                                    FrequencyType.ONCE_DAILY -> listOf("08:00")
                                    FrequencyType.TWICE_DAILY -> listOf("08:00", "19:00")
                                    FrequencyType.THREE_TIMES_DAILY -> listOf("08:00", "13:00", "19:00")
                                    FrequencyType.AS_NEEDED -> emptyList()
                                    FrequencyType.WEEKLY -> listOf("09:00")
                                }
                            },
                            label = { Text(freq.label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Scheduled Times (if not PRN)
            if (selectedFrequency != FrequencyType.AS_NEEDED) {
                item {
                    Text("Scheduled Times", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scheduledTimes.forEachIndexed { index, time ->
                            Box(
                                modifier = Modifier
                                    .background(PulseTeal.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = time,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTeal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Food Instruction
            item {
                Text("Food Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FoodInstruction.entries) { food ->
                        FilterChip(
                            selected = foodInstruction == food,
                            onClick = { foodInstruction = food },
                            label = { Text(food.label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Color Selection
            item {
                Text("Color Tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .clickable { selectedColorHex = hex }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Inventory & Refill
            item {
                Text("Inventory & Refill Alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = remainingQuantity,
                        onValueChange = { remainingQuantity = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Remaining") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalQuantity,
                        onValueChange = { totalQuantity = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total Pack") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = refillThreshold,
                        onValueChange = { refillThreshold = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Alert When <") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Prescription info
            item {
                OutlinedTextField(
                    value = rxNumber,
                    onValueChange = { rxNumber = it },
                    label = { Text("Rx / Prescription Number (Optional)") },
                    placeholder = { Text("e.g. RX-948210") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Special Instructions / Warning (Optional)") },
                    placeholder = { Text("e.g. Take with a full glass of water. Avoid sunlight.") },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Error display
            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Please enter medication name"
                            return@Button
                        }
                        if (dosage.isBlank()) {
                            errorMessage = "Please enter dosage/strength"
                            return@Button
                        }

                        val med = Medication(
                            id = initialMedication?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            dosage = dosage.trim(),
                            form = selectedForm,
                            frequency = selectedFrequency,
                            scheduledTimes = if (selectedFrequency == FrequencyType.AS_NEEDED) emptyList() else scheduledTimes,
                            foodInstruction = foodInstruction,
                            colorHex = selectedColorHex,
                            totalQuantity = totalQuantity.toIntOrNull() ?: 30,
                            remainingQuantity = remainingQuantity.toIntOrNull() ?: 30,
                            refillThreshold = refillThreshold.toIntOrNull() ?: 7,
                            rxNumber = rxNumber.trim(),
                            instructions = instructions.trim(),
                            isActive = true,
                            createdAt = initialMedication?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(med)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTeal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_medication_button")
                ) {
                    Text(
                        text = if (initialMedication == null) "Add to Cabinet" else "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
