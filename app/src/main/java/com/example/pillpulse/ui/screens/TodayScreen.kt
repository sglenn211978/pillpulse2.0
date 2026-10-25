package com.example.pillpulse.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.pillpulse.data.model.Medication
import com.example.pillpulse.data.model.ScheduledDoseItem
import com.example.pillpulse.ui.components.DoseCard
import com.example.pillpulse.ui.components.PulseHeroCard
import com.example.pillpulse.ui.components.RefillAlertBanner
import com.example.pillpulse.ui.components.WeekCalendarStrip
import com.example.pillpulse.ui.theme.PulseTeal
import com.example.pillpulse.ui.viewmodel.MedicationViewModel

import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip

@Composable
fun TodayScreen(
    viewModel: MedicationViewModel,
    onNavigateToCabinet: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onAddNewMedication: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val adherenceStats by viewModel.adherenceStats.collectAsStateWithLifecycle()
    val scheduledDoses by viewModel.scheduledDoses.collectAsStateWithLifecycle()
    val asNeededMeds by viewModel.asNeededMedications.collectAsStateWithLifecycle()
    val lowStockMeds by viewModel.lowStockMedications.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    var skippingItem by remember { mutableStateOf<ScheduledDoseItem?>(null) }
    var takingPrnMed by remember { mutableStateOf<Medication?>(null) }

    val todayKey = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }
    val isToday = selectedDateKey == todayKey

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Week Calendar Strip
        item {
            Spacer(modifier = Modifier.height(4.dp))
            WeekCalendarStrip(
                selectedDateKey = selectedDateKey,
                onDateSelected = { viewModel.selectDate(it) }
            )
        }

        // Pulse Adherence Hero Meter
        item {
            PulseHeroCard(
                stats = adherenceStats,
                isToday = isToday
            )
        }

        // Alarm & Reminder Quick Status Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (appSettings.alarmsEnabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToSettings() }
                    .testTag("alarm_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (appSettings.alarmsEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (appSettings.alarmsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Alarm status",
                            tint = if (appSettings.alarmsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (appSettings.alarmsEnabled) "Alarms Active: ${appSettings.alarmSound.label}" else "Alarms Muted",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (appSettings.alarmsEnabled) {
                                if (appSettings.advanceNoticeMinutes > 0) "${appSettings.advanceNoticeMinutes}m early • Snooze ${appSettings.snoozeDurationMinutes}m"
                                else "At exact dose times • Tap to customize"
                            } else {
                                "Tap to enable scheduled dose chimes"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Settings ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Low stock banner
        if (lowStockMeds.isNotEmpty()) {
            item {
                RefillAlertBanner(
                    lowStockMeds = lowStockMeds,
                    onViewCabinet = onNavigateToCabinet
                )
            }
        }

        // Scheduled Timeline Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled Doses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${scheduledDoses.count { it.status != null }} of ${scheduledDoses.size} taken",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scheduled Doses List
        if (scheduledDoses.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = PulseTeal,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No scheduled doses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add regular daily medications to track adherence here.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddNewMedication,
                            colors = ButtonDefaults.buttonColors(containerColor = PulseTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Medication")
                        }
                    }
                }
            }
        } else {
            items(scheduledDoses, key = { "${it.medication.id}_${it.scheduledTime}" }) { item ->
                DoseCard(
                    item = item,
                    onTake = { viewModel.takeDose(item.medication.id, item.scheduledTime) },
                    onSkip = { skippingItem = item },
                    onUndo = { logId -> viewModel.undoDoseLog(logId) }
                )
            }
        }

        // As Needed (PRN) Section
        if (asNeededMeds.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "As-Needed (PRN) Medications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(asNeededMeds, key = { it.id }) { med ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(med.colorHex).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = med.name,
                                tint = Color(med.colorHex),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = med.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${med.dosage} • ${med.remainingQuantity} doses left",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { takingPrnMed = med },
                            colors = ButtonDefaults.buttonColors(containerColor = PulseTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("prn_take_${med.id}")
                        ) {
                            Text("Log Dose", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Skip Dose Reason Dialog
    skippingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { skippingItem = null },
            title = { Text("Skip Dose") },
            text = {
                Text("Are you sure you want to skip ${item.medication.name} (${item.medication.dosage}) scheduled for ${item.scheduledTime}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.skipDose(item.medication.id, item.scheduledTime, "Patient skipped")
                        skippingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Skip")
                }
            },
            dismissButton = {
                TextButton(onClick = { skippingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Log PRN Dose Dialog
    takingPrnMed?.let { med ->
        AlertDialog(
            onDismissRequest = { takingPrnMed = null },
            title = { Text("Log PRN Dose") },
            text = {
                Text("Record taking 1 dose of ${med.name} (${med.dosage}) now?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logAsNeeded(med.id)
                        takingPrnMed = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTeal)
                ) {
                    Text("Log Taken")
                }
            },
            dismissButton = {
                TextButton(onClick = { takingPrnMed = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
