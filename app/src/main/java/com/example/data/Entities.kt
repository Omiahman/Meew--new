package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meme_sounds")
data class MemeSoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val soundKey: String,
    val name: String,
    val category: String, // "Funny", "Troll", "Reaction", "Laugh", "Surprise", "Gaming", "Voice", "Effects"
    val iconName: String,
    val durationMs: Int,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val filePath: String? = null,
    val waveformType: String = "",
    val lastPlayedTimestamp: Long = 0L,
    val playCount: Int = 0,
    val isQuickAccess: Boolean = false,
    val quickAccessOrder: Int = 0
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val micVolume: Float = 1.0f,
    val memeVolume: Float = 1.0f,
    val isMicMuted: Boolean = false,
    val isSoundOnlyMode: Boolean = false,
    val overlayEnabled: Boolean = true,
    val overlaySize: String = "Medium", // Small, Medium, Large
    val overlayOpacity: Float = 0.90f,
    val overlaySnapToEdge: Boolean = true,
    val overlayLastX: Int = -1,
    val overlayLastY: Int = -1,
    val multiPlayEnabled: Boolean = false,
    val gamingModeActive: Boolean = false,
    val selectedGamingApps: String = "com.dts.freefireth,com.dts.freefiremax,com.pubg.imobile,com.tencent.ig"
)

@Entity(tableName = "local_meme_audio")
data class LocalMemeAudioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val fileUri: String? = null,
    val category: String = "Custom", // "Funny", "Troll", "Reaction", "Laugh", "Surprise", "Gaming", "Voice", "Effects", "Custom"
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "audio/wav",
    val iconName: String = "mic",
    val isFavorite: Boolean = false,
    val isQuickAccess: Boolean = false,
    val quickAccessOrder: Int = 0,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: String = ""
)

@Entity(tableName = "sound_categories")
data class SoundCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "category",
    val colorHex: String = "#00F0FF",
    val displayOrder: Int = 0,
    val isDefault: Boolean = false
)

data class CategoryWithCount(
    val category: String,
    val count: Int
)

