package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemeSoundDao {
    @Query("SELECT * FROM meme_sounds ORDER BY isQuickAccess DESC, playCount DESC, name ASC")
    fun getAllSounds(): Flow<List<MemeSoundEntity>>

    @Query("SELECT * FROM meme_sounds WHERE isFavorite = 1 ORDER BY playCount DESC, name ASC")
    fun getFavoriteSounds(): Flow<List<MemeSoundEntity>>

    @Query("SELECT * FROM meme_sounds WHERE isQuickAccess = 1 ORDER BY quickAccessOrder ASC, playCount DESC")
    fun getQuickAccessSounds(): Flow<List<MemeSoundEntity>>

    @Query("SELECT * FROM meme_sounds WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentlyPlayedSounds(limit: Int = 12): Flow<List<MemeSoundEntity>>

    @Query("SELECT * FROM meme_sounds WHERE isCustom = 1 ORDER BY id DESC")
    fun getCustomSounds(): Flow<List<MemeSoundEntity>>

    @Query("SELECT * FROM meme_sounds WHERE category = :category ORDER BY playCount DESC, name ASC")
    fun getSoundsByCategory(category: String): Flow<List<MemeSoundEntity>>

    @Query("SELECT * FROM meme_sounds WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchSounds(query: String): Flow<List<MemeSoundEntity>>

    @Query("SELECT * FROM meme_sounds WHERE soundKey = :soundKey LIMIT 1")
    suspend fun getSoundByKey(soundKey: String): MemeSoundEntity?

    @Query("SELECT * FROM meme_sounds WHERE id = :id LIMIT 1")
    suspend fun getSoundById(id: Long): MemeSoundEntity?

    @Query("SELECT COUNT(*) FROM meme_sounds")
    suspend fun getSoundCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSound(sound: MemeSoundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSounds(sounds: List<MemeSoundEntity>)

    @Update
    suspend fun updateSound(sound: MemeSoundEntity)

    @Delete
    suspend fun deleteSound(sound: MemeSoundEntity)

    @Query("UPDATE meme_sounds SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE meme_sounds SET isQuickAccess = :isQuick, quickAccessOrder = :order WHERE id = :id")
    suspend fun updateQuickAccess(id: Long, isQuick: Boolean, order: Int)

    @Query("UPDATE meme_sounds SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :id")
    suspend fun recordSoundPlay(id: Long, timestamp: Long)

    @Query("UPDATE meme_sounds SET isQuickAccess = 0")
    suspend fun resetAllQuickAccess()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET gamingModeActive = :active WHERE id = 1")
    suspend fun updateGamingMode(active: Boolean)

    @Query("UPDATE user_settings SET isMicMuted = :muted WHERE id = 1")
    suspend fun updateMicMuted(muted: Boolean)

    @Query("UPDATE user_settings SET micVolume = :micVol, memeVolume = :memeVol WHERE id = 1")
    suspend fun updateVolumes(micVol: Float, memeVol: Float)

    @Query("UPDATE user_settings SET overlaySize = :size, overlayOpacity = :opacity, overlaySnapToEdge = :snap WHERE id = 1")
    suspend fun updateOverlaySettings(size: String, opacity: Float, snap: Boolean)

    @Query("UPDATE user_settings SET overlayLastX = :x, overlayLastY = :y WHERE id = 1")
    suspend fun updateOverlayPosition(x: Int, y: Int)
}
