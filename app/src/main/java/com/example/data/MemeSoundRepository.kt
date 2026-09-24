package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemeSoundRepository(
    private val memeSoundDao: MemeSoundDao,
    private val userSettingsDao: UserSettingsDao
) {
    val allSounds: Flow<List<MemeSoundEntity>> = memeSoundDao.getAllSounds()
    val favoriteSounds: Flow<List<MemeSoundEntity>> = memeSoundDao.getFavoriteSounds()
    val quickAccessSounds: Flow<List<MemeSoundEntity>> = memeSoundDao.getQuickAccessSounds()
    val recentSounds: Flow<List<MemeSoundEntity>> = memeSoundDao.getRecentlyPlayedSounds()
    val customSounds: Flow<List<MemeSoundEntity>> = memeSoundDao.getCustomSounds()
    val userSettings: Flow<UserSettingsEntity> = userSettingsDao.getSettingsFlow()
        .map { it ?: UserSettingsEntity() }
    val allCategories: Flow<List<String>> = memeSoundDao.getAllSoundCategories()
    val categoriesWithCount: Flow<List<CategoryWithCount>> = memeSoundDao.getCategoriesWithCount()

    fun getSoundsByCategory(category: String): Flow<List<MemeSoundEntity>> {
        return if (category == "All") {
            memeSoundDao.getAllSounds()
        } else {
            memeSoundDao.getSoundsByCategory(category)
        }
    }

    suspend fun updateSoundCategory(soundId: Long, category: String) {
        memeSoundDao.updateSoundCategory(soundId, category)
    }

    fun searchSounds(query: String): Flow<List<MemeSoundEntity>> =
        memeSoundDao.searchSounds(query)

    suspend fun toggleFavorite(soundId: Long, currentFavorite: Boolean) {
        memeSoundDao.updateFavorite(soundId, !currentFavorite)
    }

    suspend fun toggleQuickAccess(soundId: Long, currentQuickAccess: Boolean, currentOrder: Int = 0) {
        memeSoundDao.updateQuickAccess(soundId, !currentQuickAccess, if (!currentQuickAccess) currentOrder else 0)
    }

    suspend fun recordSoundPlayed(soundId: Long) {
        memeSoundDao.recordSoundPlay(soundId, System.currentTimeMillis())
    }

    suspend fun saveCustomSound(sound: MemeSoundEntity): Long {
        return memeSoundDao.insertSound(sound)
    }

    suspend fun deleteSound(sound: MemeSoundEntity) {
        memeSoundDao.deleteSound(sound)
    }

    suspend fun updateSettings(settings: UserSettingsEntity) {
        userSettingsDao.insertOrUpdate(settings)
    }

    suspend fun setGamingMode(active: Boolean) {
        userSettingsDao.updateGamingMode(active)
    }

    suspend fun setMicMuted(muted: Boolean) {
        userSettingsDao.updateMicMuted(muted)
    }

    suspend fun updateVolumes(micVol: Float, memeVol: Float) {
        userSettingsDao.updateVolumes(micVol, memeVol)
    }

    suspend fun updateOverlayConfig(size: String, opacity: Float, snap: Boolean) {
        userSettingsDao.updateOverlaySettings(size, opacity, snap)
    }

    suspend fun updateOverlayPosition(x: Int, y: Int) {
        userSettingsDao.updateOverlayPosition(x, y)
    }

    suspend fun ensureDefaultDataPopulated() {
        if (memeSoundDao.getSoundCount() == 0) {
            memeSoundDao.insertSounds(DefaultSoundsData.getInitialSounds())
        }
        if (userSettingsDao.getSettingsOnce() == null) {
            userSettingsDao.insertOrUpdate(UserSettingsEntity())
        }
    }
}
