package com.example.pillpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pillpulse.data.model.Medication
import com.example.pillpulse.ui.screens.AddEditMedicationScreen
import com.example.pillpulse.ui.screens.CabinetScreen
import com.example.pillpulse.ui.screens.PulseHistoryScreen
import com.example.pillpulse.ui.screens.SettingsScreen
import com.example.pillpulse.ui.screens.TodayScreen
import com.example.pillpulse.ui.theme.PillPulseTheme
import com.example.pillpulse.ui.theme.PulseCoral
import com.example.pillpulse.ui.viewmodel.MedicationViewModel

enum class AppScreen {
    TODAY,
    CABINET,
    PULSE_LOG,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private val viewModel: MedicationViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

            PillPulseTheme(settings = appSettings) {
                var currentScreen by remember { mutableStateOf(AppScreen.TODAY) }
                var editingMedication by remember { mutableStateOf<Medication?>(null) }
                var isAddingOrEditing by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAddingOrEditing) {
                        AddEditMedicationScreen(
                            initialMedication = editingMedication,
                            onSave = { med ->
                                viewModel.saveMedication(med)
                                isAddingOrEditing = false
                                editingMedication = null
                            },
                            onNavigateBack = {
                                isAddingOrEditing = false
                                editingMedication = null
                            }
                        )
                    } else if (currentScreen == AppScreen.SETTINGS) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = AppScreen.TODAY }
                        )
                    } else {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF24314B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_pressdaily_crop),
                                                    contentDescription = "PressDaily Logo",
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "PressDaily",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Daily",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { currentScreen = AppScreen.SETTINGS },
                                            modifier = Modifier.testTag("top_bar_settings_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Alarm and Theme Settings",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp,
                                    modifier = Modifier.testTag("bottom_nav_bar")
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.TODAY,
                                        onClick = { currentScreen = AppScreen.TODAY },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == AppScreen.TODAY) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
                                                contentDescription = "Today"
                                            )
                                        },
                                        label = { Text("Today") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_today")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.CABINET,
                                        onClick = { currentScreen = AppScreen.CABINET },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == AppScreen.CABINET) Icons.Filled.LocalPharmacy else Icons.Outlined.LocalPharmacy,
                                                contentDescription = "Cabinet"
                                            )
                                        },
                                        label = { Text("Cabinet") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_cabinet")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.PULSE_LOG,
                                        onClick = { currentScreen = AppScreen.PULSE_LOG },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == AppScreen.PULSE_LOG) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = "Pulse Log"
                                            )
                                        },
                                        label = { Text("Pulse Log") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_pulse_log")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.SETTINGS,
                                        onClick = { currentScreen = AppScreen.SETTINGS },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == AppScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Settings"
                                            )
                                        },
                                        label = { Text("Settings") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_settings")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            when (currentScreen) {
                                AppScreen.TODAY -> {
                                    TodayScreen(
                                        viewModel = viewModel,
                                        onNavigateToCabinet = { currentScreen = AppScreen.CABINET },
                                        onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                                        onAddNewMedication = {
                                            editingMedication = null
                                            isAddingOrEditing = true
                                        },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                AppScreen.CABINET -> {
                                    CabinetScreen(
                                        viewModel = viewModel,
                                        onAddMedication = {
                                            editingMedication = null
                                            isAddingOrEditing = true
                                        },
                                        onEditMedication = { med ->
                                            editingMedication = med
                                            isAddingOrEditing = true
                                        },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                AppScreen.PULSE_LOG -> {
                                    PulseHistoryScreen(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                AppScreen.SETTINGS -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = AppScreen.TODAY },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
