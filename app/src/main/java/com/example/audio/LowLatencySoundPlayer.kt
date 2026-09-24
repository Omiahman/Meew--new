package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.util.Log
import com.example.data.MemeSoundEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class LowLatencySoundPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "MemeMicPlayer"
    private var soundPool: SoundPool? = null
    private val soundIdMap = HashMap<String, Int>()
    private val activeStreams = HashSet<Int>()
    private var activeMediaPlayer: MediaPlayer? = null

    private val _currentlyPlayingId = MutableStateFlow<Long?>(null)
    val currentlyPlayingId: StateFlow<Long?> = _currentlyPlayingId.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _multiPlayEnabled = MutableStateFlow(false)
    val multiPlayEnabled: StateFlow<Boolean> = _multiPlayEnabled.asStateFlow()

    // Listener for live audio mixer feeding
    var onSoundPlaybackStarted: ((soundKey: String, volume: Float) -> Unit)? = null

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0.0f, 2.0f)
        val adjustedVol = _volume.value.coerceIn(0.0f, 1.0f)
        activeStreams.forEach { streamId ->
            soundPool?.setVolume(streamId, adjustedVol, adjustedVol)
        }
        activeMediaPlayer?.setVolume(adjustedVol, adjustedVol)
    }

    fun setMultiPlay(enabled: Boolean) {
        _multiPlayEnabled.value = enabled
    }

    fun playSound(sound: MemeSoundEntity, onComplete: (() -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                if (!_multiPlayEnabled.value) {
                    stopAll()
                }

                _currentlyPlayingId.value = sound.id
                val vol = _volume.value.coerceIn(0.0f, 1.0f)

                // Get file (either custom audio path or synthesized wav)
                val soundFile: File = if (sound.isCustom && !sound.filePath.isNullOrBlank()) {
                    File(sound.filePath)
                } else {
                    AudioSynthesisEngine.getOrGenerateSoundFile(
                        context,
                        sound.soundKey,
                        sound.waveformType
                    )
                }

                if (!soundFile.exists() || soundFile.length() < 44) {
                    Log.e(TAG, "Audio file not found: ${soundFile.absolutePath}")
                    _currentlyPlayingId.value = null
                    return@launch
                }

                // Notify live mixer that sound triggered
                onSoundPlaybackStarted?.invoke(sound.soundKey, _volume.value)

                // For short clips, try SoundPool for minimal latency
                val cachedId = soundIdMap[sound.soundKey]
                if (cachedId != null && cachedId != 0) {
                    val streamId = soundPool?.play(cachedId, vol, vol, 1, 0, 1.0f) ?: 0
                    if (streamId != 0) {
                        activeStreams.add(streamId)
                        // Auto clear state after sound duration
                        scope.launch(Dispatchers.Default) {
                            kotlinx.coroutines.delay(sound.durationMs.toLong() + 100)
                            activeStreams.remove(streamId)
                            if (activeStreams.isEmpty() && activeMediaPlayer == null) {
                                _currentlyPlayingId.value = null
                            }
                            onComplete?.invoke()
                        }
                        return@launch
                    }
                }

                // If not in SoundPool or stream failed, play via low-latency MediaPlayer
                playViaMediaPlayer(soundFile, sound.id, vol, onComplete)

                // Pre-cache into SoundPool for subsequent taps
                if (!soundIdMap.containsKey(sound.soundKey) && soundFile.length() < 1024 * 1024) {
                    soundPool?.load(soundFile.absolutePath, 1)?.let { loadedId ->
                        soundIdMap[sound.soundKey] = loadedId
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing sound: ${sound.name}", e)
                _currentlyPlayingId.value = null
            }
        }
    }

    private fun playViaMediaPlayer(file: File, soundId: Long, vol: Float, onComplete: (() -> Unit)?) {
        scope.launch(Dispatchers.Main) {
            try {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, Uri.fromFile(file))
                    setVolume(vol, vol)
                    setOnCompletionListener {
                        _currentlyPlayingId.value = null
                        it.release()
                        if (activeMediaPlayer == it) activeMediaPlayer = null
                        onComplete?.invoke()
                    }
                    setOnErrorListener { mp, _, _ ->
                        _currentlyPlayingId.value = null
                        mp.release()
                        if (activeMediaPlayer == mp) activeMediaPlayer = null
                        false
                    }
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer error", e)
                _currentlyPlayingId.value = null
            }
        }
    }

    fun stopAll() {
        activeStreams.forEach { streamId ->
            soundPool?.stop(streamId)
        }
        activeStreams.clear()
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            activeMediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }
        _currentlyPlayingId.value = null
    }

    fun release() {
        stopAll()
        soundPool?.release()
        soundPool = null
        soundIdMap.clear()
    }
}
