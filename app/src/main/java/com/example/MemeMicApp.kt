package com.example

import android.app.Application
import com.example.audio.AudioHardwareManager
import com.example.audio.AudioSynthesisEngine
import com.example.audio.LiveAudioMixer
import com.example.audio.LowLatencySoundPlayer
import com.example.data.MemeMicDatabase
import com.example.data.MemeSoundRepository
import com.example.data.LocalMemeAudioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MemeMicApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { MemeMicDatabase.getDatabase(this, applicationScope) }
    val repository by lazy {
        MemeSoundRepository(
            database.memeSoundDao(),
            database.userSettingsDao()
        )
    }
    val localMemeAudioRepository by lazy {
        LocalMemeAudioRepository(
            database.localMemeAudioDao(),
            database.soundCategoryDao()
        )
    }

    val soundPlayer by lazy { LowLatencySoundPlayer(this, applicationScope) }
    val audioHardwareManager by lazy { AudioHardwareManager(this) }
    val liveAudioMixer by lazy { LiveAudioMixer(this) }

    override fun onCreate() {
        super.onCreate()

        // Preload database and synthesize default sound cache
        applicationScope.launch(Dispatchers.IO) {
            repository.ensureDefaultDataPopulated()

            // Pre-synthesize core meme sounds into cache so taps are zero-latency
            val coreSounds = listOf("bruh", "airhorn", "vine_boom", "booyah", "coffin_dance", "metal_pipe")
            for (key in coreSounds) {
                AudioSynthesisEngine.getOrGenerateSoundFile(this@MemeMicApp, key, key)
            }
        }

        // Bridge sound player with live mixer
        soundPlayer.onSoundPlaybackStarted = { soundKey, volume ->
            if (liveAudioMixer.isMixingActive.value) {
                applicationScope.launch(Dispatchers.IO) {
                    val file = AudioSynthesisEngine.getOrGenerateSoundFile(this@MemeMicApp, soundKey, soundKey)
                    liveAudioMixer.loadAndQueueMemeFile(file)
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        soundPlayer.release()
        audioHardwareManager.unregister()
        liveAudioMixer.stopMixer()
    }
}
