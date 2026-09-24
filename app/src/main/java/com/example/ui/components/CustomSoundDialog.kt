package com.example.ui.components

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DefaultSoundsData
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CustomSoundDialog(
    onDismiss: () -> Unit,
    onSaveRecorded: (name: String, category: String, pcmData: ShortArray) -> Unit,
    onImportFile: (uri: Uri, name: String, category: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var soundName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Voice") }

    // Recording State
    var isRecording by remember { mutableStateOf(false) }
    val recordedSamples = remember { mutableStateListOf<Short>() }
    var recordingRecord: AudioRecord? by remember { mutableStateOf(null) }

    // Trimming State (0f to 1f)
    var trimRange by remember { mutableStateOf(0f..1f) }

    // File Picker State
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = uri.lastPathSegment ?: "Imported File"
            if (soundName.isBlank()) {
                soundName = selectedFileName?.substringBeforeLast(".") ?: "Imported Sound"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recordingRecord?.stop()
                recordingRecord?.release()
            } catch (_: Exception) {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Custom Meme Sound",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF101624),
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Record Mic Clip",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 0) NeonCyan else TextSecondary
                                )
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Import Audio File",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) NeonCyan else TextSecondary
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Record with Mic
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) NeonRed else Color(0xFF1E283E))
                                .border(
                                    2.dp,
                                    if (isRecording) NeonRed else NeonCyan,
                                    CircleShape
                                )
                                .clickable {
                                    if (!isRecording) {
                                        // Start recording
                                        isRecording = true
                                        recordedSamples.clear()
                                        scope.launch(Dispatchers.IO) {
                                            startLiveRecording { sample ->
                                                recordedSamples.add(sample)
                                            }
                                        }
                                    } else {
                                        // Stop recording
                                        isRecording = false
                                        try {
                                            recordingRecord?.stop()
                                            recordingRecord?.release()
                                        } catch (_: Exception) {}
                                        recordingRecord = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = if (isRecording) "Stop Recording" else "Record Mic",
                                tint = if (isRecording) Color.White else NeonCyan,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isRecording) "RECORDING... (Tap to stop)" else if (recordedSamples.isNotEmpty()) "Clip Captured: ${recordedSamples.size / 44100}s" else "Tap Mic to record voice prank",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isRecording) NeonRed else NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    // File Importer
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { filePicker.launch("audio/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.AudioFile, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedFileName ?: "Select Audio File (.mp3, .wav, .m4a)",
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trimming Slider
                Text(
                    text = "Trim Audio Range: ${(trimRange.start * 100).toInt()}% - ${(trimRange.endInclusive * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )

                RangeSlider(
                    value = trimRange,
                    onValueChange = { trimRange = it },
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color(0xFF283654)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Name Input
                OutlinedTextField(
                    value = soundName,
                    onValueChange = { soundName = it },
                    label = { Text("Sound Name") },
                    placeholder = { Text("e.g. My Free Fire Clutch Voice") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_sound_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf("Voice", "Funny", "Troll", "Gaming")
                    categories.forEach { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonCyan else Color(0xFF1E283E))
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else TextPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Action
                Button(
                    onClick = {
                        val name = soundName.ifBlank { "Custom Sound" }
                        if (selectedTab == 0 && recordedSamples.isNotEmpty()) {
                            // Apply trim
                            val startIdx = (recordedSamples.size * trimRange.start).toInt()
                            val endIdx = (recordedSamples.size * trimRange.endInclusive).toInt().coerceAtLeast(startIdx + 1000)
                            val trimmed = recordedSamples.subList(startIdx, endIdx.coerceAtMost(recordedSamples.size)).toShortArray()
                            onSaveRecorded(name, selectedCategory, trimmed)
                            onDismiss()
                        } else if (selectedTab == 1 && selectedUri != null) {
                            onImportFile(selectedUri!!, name, selectedCategory)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_custom_sound_button"),
                    enabled = (selectedTab == 0 && recordedSamples.isNotEmpty()) || (selectedTab == 1 && selectedUri != null),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save to Soundboard",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun startLiveRecording(onSample: (Short) -> Unit) {
    val sampleRate = 44100
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

    val record = try {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize * 2
        )
    } catch (_: Exception) { null } ?: return

    try {
        record.startRecording()
        val buffer = ShortArray(minBufferSize)
        while (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val read = record.read(buffer, 0, minBufferSize)
            if (read > 0) {
                for (i in 0 until read) {
                    onSample(buffer[i])
                }
            }
        }
    } catch (_: Exception) {
    } finally {
        try {
            record.stop()
            record.release()
        } catch (_: Exception) {}
    }
}
