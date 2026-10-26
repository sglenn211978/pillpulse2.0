package com.example.pressdaily.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pressdaily.data.model.Medication
import com.example.pressdaily.ui.theme.PulseAmber

@Composable
fun RefillAlertBanner(
    lowStockMeds: List<Medication>,
    onViewCabinet: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lowStockMeds.isEmpty()) return

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PulseAmber.copy(alpha = 0.12f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewCabinet() }
            .testTag("refill_alert_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PulseAmber.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Refill Alert",
                    tint = PulseAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Refill Alert: ${lowStockMeds.size} medication${if (lowStockMeds.size > 1) "s" else ""} low",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = lowStockMeds.joinToString(", ") { "${it.name} (${it.remainingQuantity} left)" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Text(
                text = "Refill ›",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PulseAmber,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
