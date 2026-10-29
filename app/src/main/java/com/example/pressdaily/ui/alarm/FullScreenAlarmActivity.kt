package com.example.pressdaily.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pressdaily.R
import com.example.pressdaily.data.model.AlarmSoundOption
import com.example.pressdaily.data.model.DoseStatus
import com.example.pressdaily.data.repository.MedicationRepository
import com.example.pressdaily.notification.MedicationScheduler
import com.example.pressdaily.notification.NotificationHelper
import com.example.pressdaily.notification.PressDailySoundManager
import com.example.pressdaily.notification.RingtonePreferences
import com.example.pressdaily.ui.theme.PressDailyTheme
import com.example.pressdaily.ui.theme.PulseTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FullScreenAlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_INSTRUCTIONS = "extra_instructions"
        const val EXTRA_REMAINING_SUPPLY = "extra_remaining_supply"
        const val EXTRA_SOUND_NAME = "extra_sound_name"
    }

    private var medicationId = "med_lisinopril"
    private var medicationName = "Lisinopril"
    private var dosage = "10 mg"
    private var scheduledTime = "08:00"
    private var instructions = "Take with food and a full glass of water"
    private var remainingSupply = 22
    private var soundOption = AlarmSoundOption.SYSTEM_ALARM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on, keep screen on, and display over keyguard/lockscreen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Read extras
        medicationId = intent.getStringExtra(EXTRA_MED_ID) ?: "med_lisinopril"
        medicationName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Lisinopril"
        dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: "10 mg"
        scheduledTime = intent.getStringExtra(EXTRA_TIME) ?: "08:00"
        instructions = intent.getStringExtra(EXTRA_INSTRUCTIONS) ?: ""
        remainingSupply = intent.getIntExtra(EXTRA_REMAINING_SUPPLY, 22)
        val isSnooze = intent.getBooleanExtra(MedicationScheduler.EXTRA_IS_SNOOZE, false)

        val savedSound = RingtonePreferences.getSavedRingtone(this)
        val soundName = intent.getStringExtra(EXTRA_SOUND_NAME) ?: savedSound.name
        soundOption = runCatching { AlarmSoundOption.valueOf(soundName) }.getOrDefault(savedSound)

        // Post status bar notification as ongoing so user can also interact from drawer if minimized
        NotificationHelper.showDoseNotification(
            context = this,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            instructions = instructions,
            remainingSupply = remainingSupply,
            alarmSound = soundOption
        )

        // Queue tomorrow's recurrence if this is a regular scheduled alarm
        if (!isSnooze) {
            MedicationScheduler.scheduleNextRecurringAlarm(applicationContext, medicationId, scheduledTime)
        }

        // Start continuous looping alarm sound & vibration
        PressDailySoundManager.startAlarmLoop(this, soundOption)

        setContent {
            PressDailyTheme {
                FullScreenAlarmContent(
                    medicationName = medicationName,
                    dosage = dosage,
                    scheduledTime = scheduledTime,
                    instructions = instructions,
                    remainingSupply = remainingSupply,
                    soundLabel = soundOption.label,
                    onTakeNow = {
                        PressDailySoundManager.stopAlarm()
                        NotificationHelper.cancelNotification(this, medicationId, scheduledTime)
                        val repository = MedicationRepository(applicationContext)
                        repository.logDose(
                            medicationId = medicationId,
                            scheduledTime = scheduledTime,
                            status = DoseStatus.TAKEN,
                            note = "Taking dose right now"
                        )
                        Toast.makeText(this, "💊 Taking $medicationName ($dosage) now — recorded as Taken!", Toast.LENGTH_LONG).show()
                        finish()
                    },
                    onTaken = {
                        PressDailySoundManager.stopAlarm()
                        NotificationHelper.cancelNotification(this, medicationId, scheduledTime)
                        val repository = MedicationRepository(applicationContext)
                        repository.logDose(
                            medicationId = medicationId,
                            scheduledTime = scheduledTime,
                            status = DoseStatus.TAKEN,
                            note = "Confirmed as Taken"
                        )
                        Toast.makeText(this, "✅ $medicationName ($dosage) confirmed as Taken!", Toast.LENGTH_LONG).show()
                        finish()
                    },
                    onSnooze = {
                        PressDailySoundManager.stopAlarm()
                        NotificationHelper.cancelNotification(this, medicationId, scheduledTime)
                        MedicationScheduler.scheduleSnooze(
                            context = applicationContext,
                            medicationId = medicationId,
                            medicationName = medicationName,
                            dosage = dosage,
                            scheduledTime = scheduledTime,
                            instructions = instructions,
                            remainingSupply = remainingSupply,
                            snoozeMinutes = 10
                        )
                        Toast.makeText(this, "⏳ Snoozed for 10 minutes — will alarm in full screen", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onSkip = {
                        PressDailySoundManager.stopAlarm()
                        NotificationHelper.cancelNotification(this, medicationId, scheduledTime)
                        val repository = MedicationRepository(applicationContext)
                        repository.logDose(
                            medicationId = medicationId,
                            scheduledTime = scheduledTime,
                            status = DoseStatus.SKIPPED,
                            note = "Skipped from alarm screen"
                        )
                        Toast.makeText(this, "⚠️ $medicationName marked as Skipped", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PressDailySoundManager.stopAlarm()
    }
}

@Composable
fun FullScreenAlarmContent(
    medicationName: String,
    dosage: String,
    scheduledTime: String,
    instructions: String,
    remainingSupply: Int,
    soundLabel: String,
    onTakeNow: () -> Unit,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    // Pulsating animation for alarm visual
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Rich deep midnight background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Alarm Header Tag
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRESSDAILY DOSE ALERT",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Giant Clock
            Text(
                text = currentTime,
                fontSize = 50.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Scheduled for $scheduledTime",
                fontSize = 15.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Pulsating Icon Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(PulseTeal.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(3.dp, PulseTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pressdaily_logo),
                        contentDescription = "PressDaily Logo",
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Medication Details Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = medicationName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dosage,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PulseTeal
                    )

                    if (instructions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF334155))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = instructions,
                                fontSize = 12.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    if (remainingSupply > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "📦 $remainingSupply doses remaining in cabinet",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ringtone Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = PulseTeal,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Playing: $soundLabel",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ACTION BUTTONS SECTION (Clear 'Take Now' and 'Taken' buttons)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 'TAKE NOW' BUTTON
                Button(
                    onClick = onTakeNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981) // High-contrast Emerald Green
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("full_screen_take_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalPharmacy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "TAKE NOW",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Taking scheduled dose right now",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // 2. 'TAKEN' BUTTON
                Button(
                    onClick = onTaken,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseTeal // Rich Pulse Teal
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("full_screen_taken_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "TAKEN",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Already took this dose earlier",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECONDARY ACTIONS: SNOOZE 10M & SKIP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("full_screen_snooze_button")
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Snooze 10m", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onSkip,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF87171)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("full_screen_skip_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Skip", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
