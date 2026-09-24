package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun AudioLevelMeter(
    label: String,
    level: Float, // 0.0f to 1.0f
    isMuted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = if (isMuted) 0.0f else level.coerceIn(0.0f, 1.0f),
        animationSpec = tween(durationMillis = 80),
        label = "vuMeter"
    )

    val totalBars = 16
    val activeBars = (animatedLevel * totalBars).toInt().coerceIn(0, totalBars)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )

            Text(
                text = when {
                    isMuted -> "MUTED"
                    animatedLevel > 0.85f -> "PEAK"
                    animatedLevel > 0.02f -> "${(animatedLevel * 100).toInt()}%"
                    else -> "IDLE"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    color = when {
                        isMuted -> NeonRed
                        animatedLevel > 0.85f -> NeonRed
                        animatedLevel > 0.5f -> NeonOrange
                        animatedLevel > 0.02f -> NeonGreen
                        else -> TextMuted
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Segmented LED Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0F1522))
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalBars) {
                val isActive = i < activeBars && !isMuted
                val segmentColor = when {
                    i >= 13 -> NeonRed
                    i >= 9 -> NeonOrange
                    else -> NeonGreen
                }

                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isActive) segmentColor else segmentColor.copy(alpha = 0.15f)
                        )
                )
            }
        }
    }
}
