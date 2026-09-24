package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.data.DefaultSoundsData
import com.example.ui.components.CustomSoundDialog
import com.example.ui.components.SoundPadButton
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SoundboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val filteredSounds = viewModel.filteredSounds.value
    val currentlyPlayingId = viewModel.currentlyPlayingId.value
    val selectedCategory = viewModel.selectedCategory.value
    val searchQuery = viewModel.searchQuery.value
    val userSettings = viewModel.userSettings.value

    var showCustomSoundDialog by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    if (showCustomSoundDialog) {
        CustomSoundDialog(
            onDismiss = { showCustomSoundDialog = false },
            onSaveRecorded = { name, cat, samples ->
                viewModel.saveRecordedVoiceClip(name, cat, samples)
            },
            onImportFile = { uri, name, cat ->
                viewModel.importAudioFile(uri, name, cat)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCustomSoundDialog = true },
                containerColor = NeonCyan,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("add_custom_sound_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Custom Sound", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Volume Button & Stop All
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search meme sounds...",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    ),
                    modifier = Modifier
                        .weight(1.0f)
                        .testTag("search_sound_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Volume slider toggle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showVolumeSlider) NeonCyan else CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { showVolumeSlider = !showVolumeSlider },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = if (showVolumeSlider) Color.Black else NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Stop All Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentlyPlayingId != null) NeonRed else CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { viewModel.stopAllSounds() }
                        .testTag("stop_all_sounds_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop All",
                        tint = if (currentlyPlayingId != null) Color.White else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Volume Adjustment Slider Dropdown
            AnimatedVisibility(visible = showVolumeSlider) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Meme Master Playback Volume",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${(userSettings.memeVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Black)
                        )
                    }

                    Slider(
                        value = userSettings.memeVolume,
                        onValueChange = { viewModel.updateVolumes(userSettings.micVolume, it) },
                        valueRange = 0.0f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0xFF283654)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(DefaultSoundsData.CATEGORIES) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) NeonCyan else Color(0xFF141C2C))
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else CardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.setSelectedCategory(category) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("category_chip_$category")
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sound Count Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredSounds.size} Meme Sounds Available",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "+ Custom Sounds (${viewModel.customSounds.value.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of Sound Pads
            if (filteredSounds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No meme sounds found for '$searchQuery'",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.setSelectedCategory("All")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E))
                        ) {
                            Text("Reset Filters", color = NeonCyan)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSounds, key = { it.id }) { sound ->
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
                            onQuickAccessClick = { viewModel.toggleQuickAccess(sound) }
                        )
                    }
                }
            }
        }
    }
}
