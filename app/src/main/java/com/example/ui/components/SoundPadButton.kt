package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemeSoundEntity
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBackgroundElevated
import com.example.ui.theme.CardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SoundPadButton(
    sound: MemeSoundEntity,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onQuickAccessClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isPlaying -> NeonCyan
            sound.isQuickAccess -> NeonOrange
            sound.isFavorite -> NeonGreen
            else -> CardBorder
        },
        label = "borderColor"
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = if (isPlaying) {
            listOf(Color(0xFF13364D), Color(0xFF0F2033))
        } else {
            listOf(CardBackgroundElevated, CardBackground)
        }
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isPlaying) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onPlayClick)
            .testTag("sound_pad_${sound.id}"),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isPlaying) 8.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(backgroundGradient)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top row: Category tag & Action icons (Quick Access + Favorite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (sound.category) {
                                    "Funny" -> Color(0x33FFB300)
                                    "Troll" -> Color(0x337C4DFF)
                                    "Reaction" -> Color(0x3300E5FF)
                                    "Laugh" -> Color(0x3300E676)
                                    "Gaming" -> Color(0x33FF1744)
                                    "Voice" -> Color(0x332979FF)
                                    else -> Color(0x3364748B)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = sound.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = when (sound.category) {
                                    "Funny" -> Color(0xFFFFCA28)
                                    "Troll" -> Color(0xFFB388FF)
                                    "Reaction" -> NeonCyan
                                    "Laugh" -> NeonGreen
                                    "Gaming" -> Color(0xFFFF5252)
                                    "Voice" -> Color(0xFF82B1FF)
                                    else -> TextSecondary
                                }
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Access Pin
                        IconButton(
                            onClick = onQuickAccessClick,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("quick_access_${sound.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = if (sound.isQuickAccess) "In Overlay Quick Access" else "Add to Overlay",
                                tint = if (sound.isQuickAccess) NeonOrange else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Favorite Pin
                        IconButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("favorite_${sound.id}")
                        ) {
                            Icon(
                                imageVector = if (sound.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (sound.isFavorite) "Favorited" else "Add Favorite",
                                tint = if (sound.isFavorite) NeonGreen else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Middle: Play/Waveform Indicator + Sound Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(if (isPlaying) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(if (isPlaying) NeonCyan else Color(0xFF283654)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = if (isPlaying) Color.Black else NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = sound.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isPlaying) NeonCyan else TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (isPlaying) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = "Playing",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PLAYING NOW",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            } else {
                                Text(
                                    text = "${sound.durationMs / 1000.0}s",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                )
                                if (sound.playCount > 0) {
                                    Text(
                                        text = " • ${sound.playCount} plays",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
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
