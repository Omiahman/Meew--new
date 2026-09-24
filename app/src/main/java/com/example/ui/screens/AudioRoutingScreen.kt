package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AudioLevelMeter
import com.example.ui.components.HardwareRoutingGuideDialog
import com.example.ui.theme.CardBackground
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
fun AudioRoutingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val routingStatus = viewModel.routingStatus.value
    val isMixingActive = viewModel.isMixingActive.value
    val micLevel = viewModel.micAudioLevel.value
    val outputLevel = viewModel.outputAudioLevel.value
    val isMicMuted = viewModel.isMicMuted.value
    val userSettings = viewModel.userSettings.value
    val errorMessage = viewModel.mixerErrorMessage.value

    var showGuideDialog by remember { mutableStateOf(false) }

    if (showGuideDialog) {
        HardwareRoutingGuideDialog(onDismiss = { showGuideDialog = false })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Audio Routing & Mixer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Mic passthrough, mixing console, and hardware status",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                IconButton(onClick = { viewModel.refreshAudioDevices() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh Devices",
                        tint = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Truthful Virtual Mic Injection Status (Strict prompt requirement)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E151F)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Direct game microphone injection is not supported on this device.",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonRed
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Android OS security architecture prohibits third-party applications from injecting audio into another application's microphone input. MemeMic uses legitimate AudioRecord/AudioTrack mixing and provides verified external hardware routing.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showGuideDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C223A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Cable, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Supported TRRS & USB Hardware Setup",
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Mixer Control Console
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Audio Mixer Console",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleLiveMixer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMixingActive) NeonRed else NeonGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("toggle_mixer_button")
                        ) {
                            Text(
                                text = if (isMixingActive) "Stop Mixer" else "Start Mixer",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = NeonRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live VU Meters
                    AudioLevelMeter(
                        label = "Microphone Live Input (Voice)",
                        level = micLevel,
                        isMuted = isMicMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AudioLevelMeter(
                        label = "Combined Mixed Output (Mic + Meme)",
                        level = outputLevel,
                        isMuted = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mic Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Microphone Gain / Volume",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${(userSettings.micVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Black)
                        )
                    }

                    Slider(
                        value = userSettings.micVolume,
                        onValueChange = { viewModel.updateVolumes(it, userSettings.memeVolume) },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0xFF283654)
                        ),
                        modifier = Modifier.testTag("mic_volume_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Meme Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Meme Sounds Output Volume",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${(userSettings.memeVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(color = NeonOrange, fontWeight = FontWeight.Black)
                        )
                    }

                    Slider(
                        value = userSettings.memeVolume,
                        onValueChange = { viewModel.updateVolumes(userSettings.micVolume, it) },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonOrange,
                            activeTrackColor = NeonOrange,
                            inactiveTrackColor = Color(0xFF283654)
                        ),
                        modifier = Modifier.testTag("meme_volume_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mic Mute & Sound-Only Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Mute Toggle
                        Button(
                            onClick = { viewModel.toggleMicMute() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMicMuted) NeonRed else Color(0xFF1E283E),
                                contentColor = if (isMicMuted) Color.White else TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("mic_mute_action")
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isMicMuted) "Microphone Muted" else "Mute Mic")
                        }

                        // Sound-Only Mode Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Sound-Only Mode",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Zero mic passthrough",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = userSettings.isSoundOnlyMode,
                                onCheckedChange = { checked ->
                                    viewModel.updateVolumes(
                                        if (checked) 0f else 1.0f,
                                        userSettings.memeVolume
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonCyan,
                                    checkedTrackColor = Color(0xFF0F323D)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Detected Audio Devices List
        item {
            Text(
                text = "Detected Physical Audio Hardware",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(routingStatus.activeDevices) { device ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141D2D))
                    .border(1.dp, Color(0xFF223048), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (device.isSource) Icons.Filled.Mic else Icons.Filled.Headset,
                            contentDescription = null,
                            tint = if (device.isSource) NeonGreen else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = device.typeName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E283E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (device.isSource) "INPUT (MIC)" else "OUTPUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (device.isSource) NeonGreen else NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
