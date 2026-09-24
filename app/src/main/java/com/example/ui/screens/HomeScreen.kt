package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemeSoundEntity
import com.example.ui.components.AudioLevelMeter
import com.example.ui.components.GamingHeader
import com.example.ui.components.HardwareRoutingGuideDialog
import com.example.ui.components.OverlayPreviewSimulator
import com.example.ui.components.SoundPadButton
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
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSoundboard: () -> Unit,
    onNavigateToRouting: () -> Unit,
    onPermissionRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val favoriteSounds = viewModel.favoriteSounds.value
    val quickSounds = viewModel.quickAccessSounds.value
    val recentSounds = viewModel.recentSounds.value
    val currentlyPlayingId = viewModel.currentlyPlayingId.value
    val routingStatus = viewModel.routingStatus.value
    val isGamingModeActive = viewModel.isGamingModeActive.value
    val isMicMuted = viewModel.isMicMuted.value
    val micLevel = viewModel.micAudioLevel.value
    val outputLevel = viewModel.outputAudioLevel.value
    val settings = viewModel.userSettings.value

    var showGuideDialog by remember { mutableStateOf(false) }

    if (showGuideDialog) {
        HardwareRoutingGuideDialog(onDismiss = { showGuideDialog = false })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Header
        item {
            GamingHeader(
                isGamingModeActive = isGamingModeActive,
                isMicMuted = isMicMuted,
                onGamingModeClick = {
                    viewModel.toggleGamingMode(context, onPermissionRequired)
                },
                onMicMuteClick = {
                    viewModel.toggleMicMute()
                },
                routingSummary = routingStatus.primaryInputDevice
            )
        }

        // Hero Gaming Mode Banner
        item {
            Spacer(modifier = Modifier.height(10.dp))
            GamingModeHeroBanner(
                isActive = isGamingModeActive,
                onToggle = { viewModel.toggleGamingMode(context, onPermissionRequired) },
                onOpenGuide = { showGuideDialog = true }
            )
        }

        // Live Audio VU Meter Card
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Audio & Mic Monitor",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        // Toggle Live Mixer
                        Text(
                            text = if (viewModel.isMixingActive.value) "MIXER ACTIVE" else "TAP TO START",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (viewModel.isMixingActive.value) NeonGreen else NeonCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF131D2D))
                                .clickable { viewModel.toggleLiveMixer() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AudioLevelMeter(
                        label = "Microphone Input (Voice)",
                        level = micLevel,
                        isMuted = isMicMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AudioLevelMeter(
                        label = "Mixed Game Stream (Voice + Memes)",
                        level = outputLevel,
                        isMuted = false
                    )
                }
            }
        }

        // Overlay Simulator / Quick Launch Preview
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Floating Overlay Preview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = if (isGamingModeActive) "OVERLAY LIVE" else "TEST IN SIMULATOR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isGamingModeActive) NeonGreen else NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OverlayPreviewSimulator(
                quickSounds = if (quickSounds.isNotEmpty()) quickSounds else favoriteSounds,
                overlaySize = settings.overlaySize,
                overlayOpacity = settings.overlayOpacity,
                isMicMuted = isMicMuted,
                onSoundClick = { sound -> viewModel.playSound(sound) },
                onToggleMicMute = { viewModel.toggleMicMute() }
            )
        }

        // Quick Trigger Favorites
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = NeonOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Instant Meme Buttons",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = "View All (${viewModel.allSounds.value.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable(onClick = onNavigateToSoundboard)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Top 6 Quick Sounds Grid
        val displayQuickList = if (quickSounds.isNotEmpty()) quickSounds else favoriteSounds.take(6)
        if (displayQuickList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No favorite sounds yet. Pin your top memes from the library!",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            val chunks = displayQuickList.take(6).chunked(2)
            chunks.forEach { rowSounds ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (sound in rowSounds) {
                            SoundPadButton(
                                sound = sound,
                                isPlaying = currentlyPlayingId == sound.id,
                                onPlayClick = {
                                    if (currentlyPlayingId == sound.id) {
                                        viewModel.stopAllSounds()
                                    } else {
                                        viewModel.playSound(sound)
                                    }
                                },
                                onFavoriteClick = { viewModel.toggleFavorite(sound) },
                                onQuickAccessClick = { viewModel.toggleQuickAccess(sound) },
                                modifier = Modifier.weight(1.0f)
                            )
                        }
                        if (rowSounds.size == 1) {
                            Spacer(modifier = Modifier.weight(1.0f))
                        }
                    }
                }
            }
        }

        // Recently Played Carousel
        if (recentSounds.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recently Used",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(recentSounds) { sound ->
                        RecentSoundChip(
                            sound = sound,
                            isPlaying = currentlyPlayingId == sound.id,
                            onClick = { viewModel.playSound(sound) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GamingModeHeroBanner(
    isActive: Boolean,
    onToggle: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = if (isActive) {
            listOf(Color(0xFF0F323D), Color(0xFF102131))
        } else {
            listOf(Color(0xFF1B2438), Color(0xFF121927))
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isActive) NeonGreen else CardBorder
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) NeonGreen else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isActive) "GAMING MODE ACTIVE" else "GAMING MODE STANDBY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isActive) NeonGreen else TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isActive) "Floating overlay & low-latency sound triggers are active above your games." else "Start Gaming Mode to launch floating meme buttons over Free Fire.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) NeonRed else NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("toggle_gaming_hero")
                    ) {
                        Text(
                            text = if (isActive) "Stop" else "Enable",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Sub-10ms trigger latency • TRRS mixing safe",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )

                    Text(
                        text = "Hardware Guide ➔",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.clickable(onClick = onOpenGuide)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSoundChip(
    sound: MemeSoundEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPlaying) Color(0xFF13364D) else Color(0xFF182236))
            .border(
                1.dp,
                if (isPlaying) NeonCyan else Color(0xFF283654),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (isPlaying) NeonCyan else TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = sound.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) NeonCyan else TextPrimary
                ),
                maxLines = 1
            )
        }
    }
}
