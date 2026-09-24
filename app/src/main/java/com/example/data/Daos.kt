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

    @Query("SELECT DISTINCT category FROM meme_sounds ORDER BY category ASC")
    fun getAllSoundCategories(): Flow<List<String>>

    @Query("UPDATE meme_sounds SET category = :category WHERE id = :id")
    suspend fun updateSoundCategory(id: Long, category: String)

    @Query("SELECT category, COUNT(*) as count FROM meme_sounds GROUP BY category ORDER BY count DESC")
    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>>
}

@Dao
interface LocalMemeAudioDao {
    @Query("SELECT * FROM local_meme_audio ORDER BY createdAt DESC")
    fun getAllAudioFiles(): Flow<List<LocalMemeAudioEntity>>

    @Query("SELECT * FROM local_meme_audio WHERE category = :category ORDER BY title ASC")
    fun getAudioFilesByCategory(category: String): Flow<List<LocalMemeAudioEntity>>

    @Query("SELECT DISTINCT category FROM local_meme_audio ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT category, COUNT(*) as count FROM local_meme_audio GROUP BY category ORDER BY count DESC")
    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM local_meme_audio WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteAudioFiles(): Flow<List<LocalMemeAudioEntity>>

    @Query("SELECT * FROM local_meme_audio WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchAudioFiles(query: String): Flow<List<LocalMemeAudioEntity>>

    @Query("SELECT * FROM local_meme_audio WHERE id = :id LIMIT 1")
    suspend fun getAudioFileById(id: Long): LocalMemeAudioEntity?

    @Query("SELECT * FROM local_meme_audio WHERE filePath = :filePath LIMIT 1")
    suspend fun getAudioFileByPath(filePath: String): LocalMemeAudioEntity?

    @Query("SELECT COUNT(*) FROM local_meme_audio")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioFile(audioFile: LocalMemeAudioEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioFiles(audioFiles: List<LocalMemeAudioEntity>): List<Long>

    @Update
    suspend fun updateAudioFile(audioFile: LocalMemeAudioEntity)

    @Delete
    suspend fun deleteAudioFile(audioFile: LocalMemeAudioEntity)

    @Query("DELETE FROM local_meme_audio WHERE id = :id")
    suspend fun deleteAudioFileById(id: Long)

    @Query("UPDATE local_meme_audio SET category = :newCategory WHERE id = :id")
    suspend fun updateCategory(id: Long, newCategory: String)

    @Query("UPDATE local_meme_audio SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE local_meme_audio SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :id")
    suspend fun recordPlay(id: Long, timestamp: Long)

    @Query("UPDATE local_meme_audio SET isQuickAccess = :isQuick, quickAccessOrder = :order WHERE id = :id")
    suspend fun updateQuickAccess(id: Long, isQuick: Boolean, order: Int)

    @Query("DELETE FROM local_meme_audio WHERE category = :category")
    suspend fun deleteAudioFilesByCategory(category: String)
}

@Dao
interface SoundCategoryDao {
    @Query("SELECT * FROM sound_categories ORDER BY displayOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<SoundCategoryEntity>>

    @Query("SELECT * FROM sound_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): SoundCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: SoundCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<SoundCategoryEntity>)

    @Update
    suspend fun updateCategory(category: SoundCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: SoundCategoryEntity)

    @Query("DELETE FROM sound_categories WHERE name = :name AND isDefault = 0")
    suspend fun deleteCategoryByName(name: String)

    @Query("SELECT COUNT(*) FROM sound_categories")
    suspend fun getCategoryCount(): Int
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
