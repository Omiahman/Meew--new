package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt

class LiveAudioMixer(private val context: Context) {
    private val TAG = "LiveAudioMixer"
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    private val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRunning = false
    private var workerThread: Thread? = null

    // Mixer Controls
    private val _isMixingActive = MutableStateFlow(false)
    val isMixingActive: StateFlow<Boolean> = _isMixingActive.asStateFlow()

    private val _micVolume = MutableStateFlow(1.0f)
    val micVolume: StateFlow<Float> = _micVolume.asStateFlow()

    private val _memeVolume = MutableStateFlow(1.0f)
    val memeVolume: StateFlow<Float> = _memeVolume.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isSoundOnlyMode = MutableStateFlow(false)
    val isSoundOnlyMode: StateFlow<Boolean> = _isSoundOnlyMode.asStateFlow()

    // Real-time audio VU meter (0.0 to 1.0)
    private val _micLevel = MutableStateFlow(0.0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    private val _outputLevel = MutableStateFlow(0.0f)
    val outputLevel: StateFlow<Float> = _outputLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Overlay / sound playback meme queue
    @Volatile
    private var activeMemePcm: ShortArray? = null
    @Volatile
    private var memeReadPos = 0

    fun setMicVolume(volume: Float) {
        _micVolume.value = volume.coerceIn(0.0f, 2.0f)
    }

    fun setMemeVolume(volume: Float) {
        _memeVolume.value = volume.coerceIn(0.0f, 2.0f)
    }

    fun setMicMuted(muted: Boolean) {
        _isMicMuted.value = muted
    }

    fun toggleMicMute(): Boolean {
        _isMicMuted.value = !_isMicMuted.value
        return _isMicMuted.value
    }

    fun setSoundOnlyMode(soundOnly: Boolean) {
        _isSoundOnlyMode.value = soundOnly
    }

    fun queueMemeSound(pcmSamples: ShortArray) {
        activeMemePcm = pcmSamples
        memeReadPos = 0
    }

    fun loadAndQueueMemeFile(file: File) {
        try {
            if (!file.exists() || file.length() < 44) return
            val pcmBytes = file.readBytes()
            // Skip 44 bytes wav header
            val dataLen = pcmBytes.size - 44
            if (dataLen <= 0) return
            val shortCount = dataLen / 2
            val shorts = ShortArray(shortCount)
            val buffer = ByteBuffer.wrap(pcmBytes, 44, dataLen).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until shortCount) {
                shorts[i] = buffer.short
            }
            queueMemeSound(shorts)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading meme file into mixer", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startMixer(): Boolean {
        if (isRunning) return true

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _errorMessage.value = "Microphone permission required for real-time audio mixing."
            return false
        }

        try {
            val minRecordBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT
            ).coerceAtLeast(2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                minRecordBufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _errorMessage.value = "Microphone hardware is currently occupied or unavailable."
                audioRecord?.release()
                audioRecord = null
                return false
            }

            val minTrackBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_OUT,
                AUDIO_FORMAT
            ).coerceAtLeast(2048)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .build()
                )
                .setBufferSizeInBytes(minTrackBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            audioRecord?.startRecording()
            audioTrack?.play()

            isRunning = true
            _isMixingActive.value = true
            _errorMessage.value = null

            workerThread = Thread({ runMixingLoop(minRecordBufferSize) }, "LiveAudioMixerThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio mixer", e)
            _errorMessage.value = "Audio engine initialisation failed: ${e.message}"
            stopMixer()
            return false
        }
    }

    private fun runMixingLoop(bufferSize: Int) {
        val micBuffer = ShortArray(bufferSize)
        val outBuffer = ShortArray(bufferSize)

        while (isRunning) {
            val record = audioRecord ?: break
            val track = audioTrack ?: break

            val readSamples = record.read(micBuffer, 0, bufferSize)
            if (readSamples <= 0) continue

            val isMuted = _isMicMuted.value || _isSoundOnlyMode.value
            val mVol = if (isMuted) 0.0f else _micVolume.value
            val sVol = _memeVolume.value

            val currentMeme = activeMemePcm
            var memePos = memeReadPos

            var sumMicSq = 0.0
            var sumOutSq = 0.0

            for (i in 0 until readSamples) {
                val micSample = micBuffer[i]
                sumMicSq += micSample * micSample

                val processedMic = micSample * mVol

                var memeSample = 0.0f
                if (currentMeme != null && memePos < currentMeme.size) {
                    memeSample = currentMeme[memePos] * sVol
                    memePos++
                }

                val mixed = (processedMic + memeSample).coerceIn(-32768.0f, 32767.0f)
                outBuffer[i] = mixed.toInt().toShort()
                sumOutSq += mixed * mixed
            }

            if (currentMeme != null) {
                memeReadPos = memePos
                if (memePos >= currentMeme.size) {
                    activeMemePcm = null
                    memeReadPos = 0
                }
            }

            // Write mixed audio to low latency output
            track.write(outBuffer, 0, readSamples)

            // Calculate RMS Decibel values for UI VU Meter
            val micRms = sqrt(sumMicSq / readSamples)
            val outRms = sqrt(sumOutSq / readSamples)

            // Normalize 0.0 to 1.0 with smooth decay
            val targetMicNorm = (micRms / 15000.0).coerceIn(0.0, 1.0).toFloat()
            val targetOutNorm = (outRms / 15000.0).coerceIn(0.0, 1.0).toFloat()

            _micLevel.value = _micLevel.value * 0.7f + targetMicNorm * 0.3f
            _outputLevel.value = _outputLevel.value * 0.7f + targetOutNorm * 0.3f
        }
    }

    fun stopMixer() {
        isRunning = false
        _isMixingActive.value = false
        _micLevel.value = 0.0f
        _outputLevel.value = 0.0f

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
        audioTrack = null

        workerThread = null
    }
}
