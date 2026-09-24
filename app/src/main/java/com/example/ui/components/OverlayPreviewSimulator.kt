package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemeSoundEntity
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun OverlayPreviewSimulator(
    quickSounds: List<MemeSoundEntity>,
    overlaySize: String, // "Small", "Medium", "Large"
    overlayOpacity: Float,
    isMicMuted: Boolean,
    onSoundClick: (MemeSoundEntity) -> Unit,
    onToggleMicMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset(20f, 20f)) }

    val density = LocalDensity.current

    val scaleFactor = when (overlaySize) {
        "Small" -> 0.85f
        "Large" -> 1.15f
        else -> 1.0f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070A10))
            .border(1.dp, Color(0xFF1E283E), RoundedCornerShape(16.dp))
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // Background game simulation watermark
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.SportsEsports,
                    contentDescription = null,
                    tint = Color(0x1A00E5FF),
                    modifier = Modifier.size(70.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Interactive Overlay Simulation (Free Fire / Game Preview)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0x33FFFFFF),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "Drag badge anywhere • Tap to expand • Instant meme trigger",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0x2200E5FF),
                        fontSize = 10.sp
                    )
                )
            }
        }

        // Draggable floating widget
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (offset.x + dragAmount.x).coerceIn(0f, (maxWidthPx - 180f).coerceAtLeast(0f))
                        val newY = (offset.y + dragAmount.y).coerceIn(0f, (maxHeightPx - 120f).coerceAtLeast(0f))
                        offset = Offset(newX, newY)
                    }
                }
        ) {
            if (!isExpanded) {
                // Minimized floating bubble
                val bubbleSize = (52 * scaleFactor).dp
                Box(
                    modifier = Modifier
                        .size(bubbleSize)
                        .clip(CircleShape)
                        .background(Color(0xE6101624).copy(alpha = overlayOpacity))
                        .border(2.dp, NeonCyan, CircleShape)
                        .clickable { isExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SportsEsports,
                        contentDescription = "Expand Overlay",
                        tint = NeonCyan,
                        modifier = Modifier.size((26 * scaleFactor).dp)
                    )

                    if (isMicMuted) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(NeonRed)
                        )
                    }
                }
            } else {
                // Expanded floating deck
                val cardWidth = (230 * scaleFactor).dp
                Card(
                    modifier = Modifier.width(cardWidth),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xF20F1522).copy(alpha = overlayOpacity)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
                ) {
                    Column(
                        modifier = Modifier.padding((10 * scaleFactor).dp)
                    ) {
                        // Title Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ MemeMic",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = NeonCyan,
                                    fontSize = (12 * scaleFactor).sp
                                )
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isMicMuted) Color(0x33FF1744) else Color(0x3300E676))
                                        .clickable(onClick = onToggleMicMute)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                        contentDescription = null,
                                        tint = if (isMicMuted) NeonRed else NeonGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { isExpanded = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Collapse",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick buttons (up to 4 in mini preview)
                        val displaySounds = quickSounds.take(4)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            displaySounds.forEach { sound ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1B2438))
                                        .border(1.dp, Color(0xFF283654), RoundedCornerShape(8.dp))
                                        .clickable { onSoundClick(sound) }
                                        .padding(horizontal = 8.dp, vertical = (6 * scaleFactor).dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(NeonCyan)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sound.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextPrimary,
                                                fontSize = (11 * scaleFactor).sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
}
