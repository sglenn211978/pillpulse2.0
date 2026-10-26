package com.example.pressdaily.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pressdaily.data.model.FrequencyType
import com.example.pressdaily.data.model.Medication
import com.example.pressdaily.ui.theme.PulseAmber
import com.example.pressdaily.ui.theme.PulseCoral
import com.example.pressdaily.ui.theme.PulseEmerald
import com.example.pressdaily.ui.theme.PulseTeal
import com.example.pressdaily.ui.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CabinetScreen(
    viewModel: MedicationViewModel,
    onAddMedication: () -> Unit,
    onEditMedication: (Medication) -> Unit,
    modifier: Modifier = Modifier
) {
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val searchQuery by viewModel.cabinetSearchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.cabinetFilter.collectAsStateWithLifecycle()

    var medToDelete by remember { mutableStateOf<Medication?>(null) }
    var medToRefill by remember { mutableStateOf<Medication?>(null) }
    var refillAmountText by remember { mutableStateOf("30") }

    val filteredMeds = medications.filter { med ->
        val matchesQuery = med.name.contains(searchQuery, ignoreCase = true) ||
                med.instructions.contains(searchQuery, ignoreCase = true) ||
                med.rxNumber.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "LOW_STOCK" -> med.remainingQuantity <= med.refillThreshold
            "DAILY" -> med.frequency != FrequencyType.AS_NEEDED
            "PRN" -> med.frequency == FrequencyType.AS_NEEDED
            else -> true
        }

        matchesQuery && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedication,
                containerColor = PulseTeal,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_medication_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Medication")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Medicine Cabinet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${medications.size} active prescriptions and supplements",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setCabinetSearchQuery(it) },
                    placeholder = { Text("Search by drug name or Rx number...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_medications_input")
                )
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All (${medications.size})",
                        "LOW_STOCK" to "Low Stock (${medications.count { it.remainingQuantity <= it.refillThreshold }})",
                        "DAILY" to "Daily Scheduled",
                        "PRN" to "As-Needed (PRN)"
                    )
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { viewModel.setCabinetFilter(key) },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Medications List
            if (filteredMeds.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No medications found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your search query or filter",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredMeds, key = { it.id }) { med ->
                    val isLowStock = med.remainingQuantity <= med.refillThreshold
                    val pillColor = Color(med.colorHex)
                    val inventoryFraction = if (med.totalQuantity > 0) {
                        (med.remainingQuantity.toFloat() / med.totalQuantity.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cabinet_item_${med.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(pillColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = med.name,
                                        tint = pillColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = med.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${med.dosage} • ${med.form.label}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Edit & Delete actions
                                IconButton(onClick = { onEditMedication(med) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit ${med.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { medToDelete = med }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete ${med.name}",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Inventory Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLowStock) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Low Stock",
                                            tint = PulseAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Low Stock (${med.remainingQuantity} left)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PulseAmber
                                        )
                                    } else {
                                        Text(
                                            text = "${med.remainingQuantity} of ${med.totalQuantity} remaining",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        medToRefill = med
                                        refillAmountText = "30"
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalPharmacy,
                                        contentDescription = null,
                                        tint = PulseTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refill", fontSize = 12.sp, color = PulseTeal, fontWeight = FontWeight.Bold)
                                }
                            }

                            LinearProgressIndicator(
                                progress = { inventoryFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = if (isLowStock) PulseAmber else PulseTeal,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Additional details
                            if (med.instructions.isNotEmpty() || med.rxNumber.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                if (med.rxNumber.isNotEmpty()) {
                                    Text(
                                        text = "Rx#: ${med.rxNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (med.instructions.isNotEmpty()) {
                                    Text(
                                        text = med.instructions,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Refill Dialog
    medToRefill?.let { med ->
        AlertDialog(
            onDismissRequest = { medToRefill = null },
            title = { Text("Refill ${med.name}") },
            text = {
                Column {
                    Text("Enter the number of pills or units to add to your supply:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = refillAmountText,
                        onValueChange = { refillAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Units to add") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = refillAmountText.toIntOrNull() ?: 30
                        viewModel.refillMedication(med.id, amount)
                        medToRefill = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTeal)
                ) {
                    Text("Confirm Refill")
                }
            },
            dismissButton = {
                TextButton(onClick = { medToRefill = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    medToDelete?.let { med ->
        AlertDialog(
            onDismissRequest = { medToDelete = null },
            title = { Text("Delete Medication") },
            text = {
                Text("Are you sure you want to remove ${med.name} (${med.dosage}) from your medicine cabinet?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedication(med.id)
                        medToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { medToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
