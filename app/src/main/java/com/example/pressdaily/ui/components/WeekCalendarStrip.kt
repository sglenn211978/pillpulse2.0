package com.example.pressdaily.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pressdaily.ui.theme.PulseTeal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WeekCalendarStrip(
    selectedDateKey: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())
    val todayKey = dateFormat.format(Date())

    // Generate 7 days centered on today
    val days = ( -3..3 ).map { offset ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offset)
        val key = dateFormat.format(cal.time)
        val dayName = dayNameFormat.format(cal.time)
        val dayNumber = dayNumberFormat.format(cal.time)
        Triple(key, dayName, dayNumber)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { (key, dayName, dayNum) ->
            val isSelected = key == selectedDateKey
            val isToday = key == todayKey

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDateSelected(key) }
                    .background(
                        when {
                            isSelected -> PulseTeal
                            isToday -> PulseTeal.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("calendar_day_$key"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayName.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isSelected -> Color.White.copy(alpha = 0.8f)
                        isToday -> PulseTeal
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayNum,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSelected -> Color.White
                        isToday -> PulseTeal
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
