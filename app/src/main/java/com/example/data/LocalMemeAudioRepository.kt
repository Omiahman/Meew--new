package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LocalMemeAudioRepository(
    private val localMemeAudioDao: LocalMemeAudioDao,
    private val soundCategoryDao: SoundCategoryDao? = null
) {
    val allAudioFiles: Flow<List<LocalMemeAudioEntity>> = localMemeAudioDao.getAllAudioFiles()
    val favoriteAudioFiles: Flow<List<LocalMemeAudioEntity>> = localMemeAudioDao.getFavoriteAudioFiles()
    val categories: Flow<List<String>> = localMemeAudioDao.getAllCategories()
    val categoriesWithCounts: Flow<List<CategoryWithCount>> = localMemeAudioDao.getCategoriesWithCount()
    val allCategoryEntities: Flow<List<SoundCategoryEntity>>? = soundCategoryDao?.getAllCategories()

    fun getAudioFilesByCategory(category: String): Flow<List<LocalMemeAudioEntity>> {
        return if (category == "All" || category.isBlank()) {
            localMemeAudioDao.getAllAudioFiles()
        } else {
            localMemeAudioDao.getAudioFilesByCategory(category)
        }
    }

    fun searchAudioFiles(query: String): Flow<List<LocalMemeAudioEntity>> =
        localMemeAudioDao.searchAudioFiles(query)

    suspend fun getAudioFileById(id: Long): LocalMemeAudioEntity? =
        localMemeAudioDao.getAudioFileById(id)

    suspend fun getAudioFileByPath(filePath: String): LocalMemeAudioEntity? =
        localMemeAudioDao.getAudioFileByPath(filePath)

    suspend fun insertAudioFile(audio: LocalMemeAudioEntity): Long =
        localMemeAudioDao.insertAudioFile(audio)

    suspend fun insertAudioFiles(audioList: List<LocalMemeAudioEntity>): List<Long> =
        localMemeAudioDao.insertAudioFiles(audioList)

    suspend fun updateAudioFile(audio: LocalMemeAudioEntity) =
        localMemeAudioDao.updateAudioFile(audio)

    suspend fun updateCategory(id: Long, newCategory: String) =
        localMemeAudioDao.updateCategory(id, newCategory)

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        localMemeAudioDao.updateFavorite(id, isFavorite)

    suspend fun recordAudioPlayed(id: Long) =
        localMemeAudioDao.recordPlay(id, System.currentTimeMillis())

    suspend fun updateQuickAccess(id: Long, isQuick: Boolean, order: Int = 0) =
        localMemeAudioDao.updateQuickAccess(id, isQuick, order)

    suspend fun deleteAudioFile(audio: LocalMemeAudioEntity, deletePhysicalFile: Boolean = true) {
        if (deletePhysicalFile && audio.filePath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(audio.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        localMemeAudioDao.deleteAudioFile(audio)
    }

    suspend fun deleteAudioFileById(id: Long, deletePhysicalFile: Boolean = true) {
        val audio = localMemeAudioDao.getAudioFileById(id)
        if (audio != null) {
            deleteAudioFile(audio, deletePhysicalFile)
        } else {
            localMemeAudioDao.deleteAudioFileById(id)
        }
    }

    suspend fun importAudioFile(
        sourceUri: Uri,
        title: String,
        category: String,
        context: Context,
        mimeType: String = "audio/wav",
        tags: String = ""
    ): LocalMemeAudioEntity = withContext(Dispatchers.IO) {
        val targetDir = File(context.filesDir, "local_meme_audio").apply { if (!exists()) mkdirs() }
        val extension = if (mimeType.contains("mp3", ignoreCase = true)) "mp3" else "wav"
        val fileName = "meme_audio_${System.currentTimeMillis()}.$extension"
        val targetFile = File(targetDir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        val fileSize = targetFile.length()
        val entity = LocalMemeAudioEntity(
            title = title.ifBlank { targetFile.nameWithoutExtension },
            filePath = targetFile.absolutePath,
            fileUri = sourceUri.toString(),
            category = category.ifBlank { "Custom" },
            durationMs = 2000L,
            fileSizeBytes = fileSize,
            mimeType = mimeType,
            iconName = "library_music",
            isFavorite = false,
            tags = tags
        )
        val insertedId = localMemeAudioDao.insertAudioFile(entity)
        entity.copy(id = insertedId)
    }

    suspend fun saveVoiceRecording(
        pcmSamples: ShortArray,
        sampleRate: Int,
        title: String,
        category: String,
        context: Context,
        tags: String = ""
    ): LocalMemeAudioEntity = withContext(Dispatchers.IO) {
        val targetDir = File(context.filesDir, "local_meme_audio").apply { if (!exists()) mkdirs() }
        val fileName = "voice_recording_${System.currentTimeMillis()}.wav"
        val targetFile = File(targetDir, fileName)

        writeWav(targetFile, pcmSamples, sampleRate)

        val durationMs = ((pcmSamples.size.toDouble() / sampleRate.toDouble()) * 1000).toLong()
        val entity = LocalMemeAudioEntity(
            title = title.ifBlank { "Recorded Voice #${System.currentTimeMillis() % 1000}" },
            filePath = targetFile.absolutePath,
            fileUri = null,
            category = category.ifBlank { "Voice" },
            durationMs = durationMs,
            fileSizeBytes = targetFile.length(),
            mimeType = "audio/wav",
            iconName = "mic",
            isFavorite = true,
            tags = tags
        )
        val id = localMemeAudioDao.insertAudioFile(entity)
        entity.copy(id = id)
    }

    private fun writeWav(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44).apply {
            this[0] = 'R'.code.toByte(); this[1] = 'I'.code.toByte(); this[2] = 'F'.code.toByte(); this[3] = 'F'.code.toByte()
            this[4] = (totalDataLen and 0xff).toByte()
            this[5] = ((totalDataLen shr 8) and 0xff).toByte()
            this[6] = ((totalDataLen shr 16) and 0xff).toByte()
            this[7] = ((totalDataLen shr 24) and 0xff).toByte()
            this[8] = 'W'.code.toByte(); this[9] = 'A'.code.toByte(); this[10] = 'V'.code.toByte(); this[11] = 'E'.code.toByte()
            this[12] = 'f'.code.toByte(); this[13] = 'm'.code.toByte(); this[14] = 't'.code.toByte(); this[15] = ' '.code.toByte()
            this[16] = 16; this[17] = 0; this[18] = 0; this[19] = 0
            this[20] = 1; this[21] = 0
            this[22] = channels.toByte(); this[23] = 0
            this[24] = (sampleRate and 0xff).toByte()
            this[25] = ((sampleRate shr 8) and 0xff).toByte()
            this[26] = ((sampleRate shr 16) and 0xff).toByte()
            this[27] = ((sampleRate shr 24) and 0xff).toByte()
            this[28] = (byteRate and 0xff).toByte()
            this[29] = ((byteRate shr 8) and 0xff).toByte()
            this[30] = ((byteRate shr 16) and 0xff).toByte()
            this[31] = ((byteRate shr 24) and 0xff).toByte()
            this[32] = (channels * 2).toByte(); this[33] = 0
            this[34] = 16; this[35] = 0
            this[36] = 'd'.code.toByte(); this[37] = 'a'.code.toByte(); this[38] = 't'.code.toByte(); this[39] = 'a'.code.toByte()
            this[40] = (totalAudioLen and 0xff).toByte()
            this[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            this[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            this[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        }

        val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcmData) {
            byteBuffer.putShort(sample)
        }

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(byteBuffer.array())
        }
    }

    suspend fun addCategory(name: String, iconName: String = "category", colorHex: String = "#00F0FF") {
        soundCategoryDao?.insertCategory(
            SoundCategoryEntity(name = name, iconName = iconName, colorHex = colorHex)
        )
    }

    suspend fun deleteCategory(categoryName: String) {
        soundCategoryDao?.deleteCategoryByName(categoryName)
    }

    suspend fun ensureDefaultCategories() {
        val count = soundCategoryDao?.getCategoryCount() ?: 0
        if (count == 0 && soundCategoryDao != null) {
            val defaults = listOf(
                SoundCategoryEntity(name = "Gaming", iconName = "sports_esports", colorHex = "#00FF66", displayOrder = 1, isDefault = true),
                SoundCategoryEntity(name = "Troll", iconName = "sentiment_very_dissatisfied", colorHex = "#FF0055", displayOrder = 2, isDefault = true),
                SoundCategoryEntity(name = "Funny", iconName = "mood", colorHex = "#FFB800", displayOrder = 3, isDefault = true),
                SoundCategoryEntity(name = "Reaction", iconName = "flash_on", colorHex = "#00F0FF", displayOrder = 4, isDefault = true),
                SoundCategoryEntity(name = "Laugh", iconName = "sentiment_very_satisfied", colorHex = "#FFD700", displayOrder = 5, isDefault = true),
                SoundCategoryEntity(name = "Voice", iconName = "mic", colorHex = "#B388FF", displayOrder = 6, isDefault = true),
                SoundCategoryEntity(name = "Effects", iconName = "graphic_eq", colorHex = "#00E5FF", displayOrder = 7, isDefault = true),
                SoundCategoryEntity(name = "Custom", iconName = "folder", colorHex = "#76FF03", displayOrder = 8, isDefault = true)
            )
            soundCategoryDao.insertCategories(defaults)
        }
    }
}

fun LocalMemeAudioEntity.toMemeSoundEntity(): MemeSoundEntity = MemeSoundEntity(
    id = id,
    soundKey = "local_$id",
    name = title,
    category = category,
    iconName = iconName,
    durationMs = durationMs.toInt(),
    isFavorite = isFavorite,
    isCustom = true,
    filePath = filePath,
    lastPlayedTimestamp = lastPlayedTimestamp,
    playCount = playCount,
    isQuickAccess = isQuickAccess,
    quickAccessOrder = quickAccessOrder
)

fun MemeSoundEntity.toLocalMemeAudioEntity(): LocalMemeAudioEntity = LocalMemeAudioEntity(
    id = id,
    title = name,
    filePath = filePath ?: "",
    category = category,
    durationMs = durationMs.toLong(),
    iconName = iconName,
    isFavorite = isFavorite,
    isQuickAccess = isQuickAccess,
    quickAccessOrder = quickAccessOrder,
    playCount = playCount,
    lastPlayedTimestamp = lastPlayedTimestamp
)
