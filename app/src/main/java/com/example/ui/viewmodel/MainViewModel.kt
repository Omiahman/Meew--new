package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MemeMicApp
import com.example.audio.AudioHardwareManager
import com.example.audio.AudioRoutingStatus
import com.example.audio.AudioSynthesisEngine
import com.example.audio.LiveAudioMixer
import com.example.audio.LowLatencySoundPlayer
import com.example.data.LocalMemeAudioEntity
import com.example.data.LocalMemeAudioRepository
import com.example.data.MemeSoundEntity
import com.example.data.MemeSoundRepository
import com.example.data.UserSettingsEntity
import com.example.data.toLocalMemeAudioEntity
import com.example.service.OverlayGamingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class InstalledGameInfo(
    val packageName: String,
    val appName: String,
    val isGameCategory: Boolean,
    val isSelected: Boolean
)

class MainViewModel(
    private val repository: MemeSoundRepository,
    private val soundPlayer: LowLatencySoundPlayer,
    private val audioHardwareManager: AudioHardwareManager,
    private val liveAudioMixer: LiveAudioMixer,
    private val appContext: Context,
    val localAudioRepository: LocalMemeAudioRepository = (appContext.applicationContext as? MemeMicApp)?.localMemeAudioRepository
        ?: LocalMemeAudioRepository(
            (appContext.applicationContext as MemeMicApp).database.localMemeAudioDao(),
            (appContext.applicationContext as MemeMicApp).database.soundCategoryDao()
        )
) : ViewModel() {

    val allSounds: StateFlow<List<MemeSoundEntity>> = repository.allSounds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSounds: StateFlow<List<MemeSoundEntity>> = repository.favoriteSounds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickAccessSounds: StateFlow<List<MemeSoundEntity>> = repository.quickAccessSounds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSounds: StateFlow<List<MemeSoundEntity>> = repository.recentSounds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customSounds: StateFlow<List<MemeSoundEntity>> = repository.customSounds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localAudioFiles: StateFlow<List<LocalMemeAudioEntity>> = localAudioRepository.allAudioFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val soundCategories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettingsEntity> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettingsEntity())

    val currentlyPlayingId: StateFlow<Long?> = soundPlayer.currentlyPlayingId
    val routingStatus: StateFlow<AudioRoutingStatus> = audioHardwareManager.routingStatus
    val isMixingActive: StateFlow<Boolean> = liveAudioMixer.isMixingActive
    val micAudioLevel: StateFlow<Float> = liveAudioMixer.micLevel
    val outputAudioLevel: StateFlow<Float> = liveAudioMixer.outputLevel
    val isMicMuted: StateFlow<Boolean> = liveAudioMixer.isMicMuted
    val mixerErrorMessage: StateFlow<String?> = liveAudioMixer.errorMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isGamingModeActive = MutableStateFlow(OverlayGamingService.isServiceRunning)
    val isGamingModeActive: StateFlow<Boolean> = _isGamingModeActive.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _installedGames = MutableStateFlow<List<InstalledGameInfo>>(emptyList())
    val installedGames: StateFlow<List<InstalledGameInfo>> = _installedGames.asStateFlow()

    val filteredSounds: StateFlow<List<MemeSoundEntity>> = combine(
        allSounds,
        _searchQuery,
        _selectedCategory
    ) { sounds, query, category ->
        sounds.filter { sound ->
            val matchesCategory = category == "All" || sound.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() || sound.name.contains(query, ignoreCase = true) || sound.category.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Sync volume settings with players
        viewModelScope.launch {
            userSettings.collect { settings ->
                soundPlayer.setVolume(settings.memeVolume)
                soundPlayer.setMultiPlay(settings.multiPlayEnabled)
                liveAudioMixer.setMicVolume(settings.micVolume)
                liveAudioMixer.setMemeVolume(settings.memeVolume)
                liveAudioMixer.setMicMuted(settings.isMicMuted)
                liveAudioMixer.setSoundOnlyMode(settings.isSoundOnlyMode)
                _isGamingModeActive.value = OverlayGamingService.isServiceRunning
            }
        }
        loadInstalledGames()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun playSound(sound: MemeSoundEntity) {
        soundPlayer.playSound(sound)
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordSoundPlayed(sound.id)
        }
    }

    fun stopAllSounds() {
        soundPlayer.stopAll()
    }

    fun toggleFavorite(sound: MemeSoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(sound.id, sound.isFavorite)
        }
    }

    fun toggleQuickAccess(sound: MemeSoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentQuickList = quickAccessSounds.value
            val newOrder = if (!sound.isQuickAccess) currentQuickList.size + 1 else 0
            repository.toggleQuickAccess(sound.id, sound.isQuickAccess, newOrder)
        }
    }

    fun deleteCustomSound(sound: MemeSoundEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            sound.filePath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
            repository.deleteSound(sound)
        }
    }

    fun toggleGamingMode(context: Context, onPermissionRequired: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            onPermissionRequired()
            return
        }

        val willActivate = !_isGamingModeActive.value
        _isGamingModeActive.value = willActivate

        val serviceIntent = Intent(context, OverlayGamingService::class.java).apply {
            action = if (willActivate) OverlayGamingService.ACTION_START else OverlayGamingService.ACTION_STOP
        }

        if (willActivate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            // Auto start live audio mixer for game pass-through
            liveAudioMixer.startMixer()
        } else {
            context.stopService(serviceIntent)
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.setGamingMode(willActivate)
        }
    }

    fun updateOverlayPermissionStatus(context: Context) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        _hasOverlayPermission.value = granted
        _isGamingModeActive.value = OverlayGamingService.isServiceRunning
        if (granted && OverlayGamingService.isServiceRunning) {
            val reloadIntent = Intent(context, OverlayGamingService::class.java).apply {
                action = OverlayGamingService.ACTION_RELOAD_OVERLAY
            }
            context.startService(reloadIntent)
        }
    }

    fun toggleMicMute() {
        val newMute = liveAudioMixer.toggleMicMute()
        viewModelScope.launch(Dispatchers.IO) {
            repository.setMicMuted(newMute)
        }
    }

    fun toggleLiveMixer() {
        if (liveAudioMixer.isMixingActive.value) {
            liveAudioMixer.stopMixer()
        } else {
            liveAudioMixer.startMixer()
        }
    }

    fun updateVolumes(micVol: Float, memeVol: Float) {
        soundPlayer.setVolume(memeVol)
        liveAudioMixer.setMicVolume(micVol)
        liveAudioMixer.setMemeVolume(memeVol)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateVolumes(micVol, memeVol)
        }
    }

    fun updateOverlaySettings(size: String, opacity: Float, snap: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOverlayConfig(size, opacity, snap)
        }
    }

    fun refreshAudioDevices() {
        audioHardwareManager.refresh()
    }

    fun saveRecordedVoiceClip(name: String, category: String, pcmSamples: ShortArray) {
        viewModelScope.launch(Dispatchers.IO) {
            val customDir = File(appContext.filesDir, "custom_meme_sounds").apply { if (!exists()) mkdirs() }
            val fileName = "custom_${System.currentTimeMillis()}.wav"
            val file = File(customDir, fileName)

            writeWavFile(file, pcmSamples, 44100)

            val durationMs = ((pcmSamples.size.toDouble() / 44100.0) * 1000).toInt()
            val entity = MemeSoundEntity(
                soundKey = "custom_${System.currentTimeMillis()}",
                name = name.ifBlank { "Custom Voice #1" },
                category = category,
                iconName = "mic",
                durationMs = durationMs,
                isFavorite = true,
                isCustom = true,
                filePath = file.absolutePath
            )
            repository.saveCustomSound(entity)
            localAudioRepository.insertAudioFile(entity.toLocalMemeAudioEntity())
        }
    }

    fun importAudioFile(uri: Uri, name: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val customDir = File(appContext.filesDir, "custom_meme_sounds").apply { if (!exists()) mkdirs() }
                val targetFile = File(customDir, "imported_${System.currentTimeMillis()}.wav")

                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val entity = MemeSoundEntity(
                    soundKey = "imported_${System.currentTimeMillis()}",
                    name = name.ifBlank { "Imported Audio" },
                    category = category,
                    iconName = "library_music",
                    durationMs = 2000,
                    isFavorite = true,
                    isCustom = true,
                    filePath = targetFile.absolutePath
                )
                repository.saveCustomSound(entity)
                localAudioRepository.insertAudioFile(entity.toLocalMemeAudioEntity())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSoundCategory(sound: MemeSoundEntity, newCategory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSoundCategory(sound.id, newCategory)
            localAudioRepository.updateCategory(sound.id, newCategory)
        }
    }

    fun updateLocalAudioCategory(audioId: Long, newCategory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            localAudioRepository.updateCategory(audioId, newCategory)
        }
    }

    fun deleteLocalAudio(audio: LocalMemeAudioEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            localAudioRepository.deleteAudioFile(audio, deletePhysicalFile = true)
        }
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put('R'.code.toByte()); put('I'.code.toByte()); put('F'.code.toByte()); put('F'.code.toByte())
            putInt(totalDataLen)
            put('W'.code.toByte()); put('A'.code.toByte()); put('V'.code.toByte()); put('E'.code.toByte())
            put('f'.code.toByte()); put('m'.code.toByte()); put('t'.code.toByte()); put(' '.code.toByte())
            putInt(16)
            putShort(1.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * 2).toShort())
            putShort(16.toShort())
            put('d'.code.toByte()); put('a'.code.toByte()); put('t'.code.toByte()); put('a'.code.toByte())
            putInt(totalAudioLen)
        }.array()

        val pcmBytes = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            for (sample in pcmData) putShort(sample)
        }.array()

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmBytes)
            out.flush()
        }
    }

    fun loadInstalledGames() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = appContext.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = pm.queryIntentActivities(intent, 0)

            val currentSelected = userSettings.value.selectedGamingApps.split(",").toSet()

            val gamesList = mutableListOf<InstalledGameInfo>()
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg == appContext.packageName) continue

                val appName = info.loadLabel(pm).toString()
                val isGame = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appInfo.category == ApplicationInfo.CATEGORY_GAME
                    } else {
                        false
                    }
                } catch (_: Exception) { false }

                // Check common multiplayer gaming packages (Free Fire, PUBG, etc.) or games category
                val isTargetGame = isGame || pkg.contains("freefire", ignoreCase = true) ||
                        pkg.contains("pubg", ignoreCase = true) ||
                        pkg.contains("dts", ignoreCase = true) ||
                        pkg.contains("cod", ignoreCase = true) ||
                        pkg.contains("roblox", ignoreCase = true)

                if (isTargetGame || gamesList.size < 12) {
                    gamesList.add(
                        InstalledGameInfo(
                            packageName = pkg,
                            appName = appName,
                            isGameCategory = isTargetGame,
                            isSelected = currentSelected.contains(pkg) || pkg.contains("freefire", ignoreCase = true)
                        )
                    )
                }
            }

            // If empty (e.g. fresh emulator), add popular supported games presets
            if (gamesList.isEmpty()) {
                gamesList.addAll(
                    listOf(
                        InstalledGameInfo("com.dts.freefireth", "Free Fire", true, true),
                        InstalledGameInfo("com.dts.freefiremax", "Free Fire MAX", true, true),
                        InstalledGameInfo("com.pubg.imobile", "BGMI / PUBG Mobile", true, false),
                        InstalledGameInfo("com.activision.callofduty.shooter", "Call of Duty Mobile", true, false),
                        InstalledGameInfo("com.supercell.brawlstars", "Brawl Stars", true, false)
                    )
                )
            }

            _installedGames.value = gamesList.sortedByDescending { it.isSelected }
        }
    }

    fun toggleGameSelection(packageName: String) {
        val current = _installedGames.value.toMutableList()
        val index = current.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            val item = current[index]
            val updated = item.copy(isSelected = !item.isSelected)
            current[index] = updated
            _installedGames.value = current

            val selectedPkgs = current.filter { it.isSelected }.joinToString(",") { it.packageName }
            viewModelScope.launch(Dispatchers.IO) {
                val settings = userSettings.value.copy(selectedGamingApps = selectedPkgs)
                repository.updateSettings(settings)
            }
        }
    }

    class Factory(private val app: MemeMicApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                app.repository,
                app.soundPlayer,
                app.audioHardwareManager,
                app.liveAudioMixer,
                app.applicationContext,
                app.localMemeAudioRepository
            ) as T
        }
    }
}
