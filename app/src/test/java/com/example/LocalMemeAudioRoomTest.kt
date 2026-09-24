package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CategoryWithCount
import com.example.data.LocalMemeAudioDao
import com.example.data.LocalMemeAudioEntity
import com.example.data.LocalMemeAudioRepository
import com.example.data.MemeMicDatabase
import com.example.data.SoundCategoryDao
import com.example.data.SoundCategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalMemeAudioRoomTest {

    private lateinit var database: MemeMicDatabase
    private lateinit var localMemeAudioDao: LocalMemeAudioDao
    private lateinit var soundCategoryDao: SoundCategoryDao
    private lateinit var repository: LocalMemeAudioRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MemeMicDatabase::class.java
        ).allowMainThreadQueries().build()

        localMemeAudioDao = database.localMemeAudioDao()
        soundCategoryDao = database.soundCategoryDao()
        repository = LocalMemeAudioRepository(localMemeAudioDao, soundCategoryDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveLocalMemeAudio() = runBlocking {
        val audio = LocalMemeAudioEntity(
            title = "Victory Screech",
            filePath = "/data/user/0/com.example/files/victory.wav",
            category = "Gaming",
            durationMs = 2500L,
            fileSizeBytes = 102400L,
            mimeType = "audio/wav",
            isFavorite = true
        )

        val insertedId = repository.insertAudioFile(audio)
        assertTrue(insertedId > 0)

        val retrieved = repository.getAudioFileById(insertedId)
        assertNotNull(retrieved)
        assertEquals("Victory Screech", retrieved?.title)
        assertEquals("Gaming", retrieved?.category)
        assertTrue(retrieved?.isFavorite == true)
        assertEquals(2500L, retrieved?.durationMs)
    }

    @Test
    fun testCategorizeAndFilterSounds() = runBlocking {
        val audio1 = LocalMemeAudioEntity(
            title = "Booyah",
            filePath = "/path/booyah.wav",
            category = "Gaming"
        )
        val audio2 = LocalMemeAudioEntity(
            title = "Bruh",
            filePath = "/path/bruh.wav",
            category = "Funny"
        )
        val audio3 = LocalMemeAudioEntity(
            title = "Airhorn",
            filePath = "/path/airhorn.wav",
            category = "Troll"
        )
        val audio4 = LocalMemeAudioEntity(
            title = "Headshot",
            filePath = "/path/headshot.wav",
            category = "Gaming"
        )

        repository.insertAudioFiles(listOf(audio1, audio2, audio3, audio4))

        val gamingSounds = repository.getAudioFilesByCategory("Gaming").first()
        assertEquals(2, gamingSounds.size)
        assertTrue(gamingSounds.any { it.title == "Booyah" })
        assertTrue(gamingSounds.any { it.title == "Headshot" })

        val funnySounds = repository.getAudioFilesByCategory("Funny").first()
        assertEquals(1, funnySounds.size)
        assertEquals("Bruh", funnySounds[0].title)

        val allCategories = repository.categories.first()
        assertTrue(allCategories.contains("Gaming"))
        assertTrue(allCategories.contains("Funny"))
        assertTrue(allCategories.contains("Troll"))
    }

    @Test
    fun testUpdateSoundCategory() = runBlocking {
        val audio = LocalMemeAudioEntity(
            title = "Troll Laugh",
            filePath = "/path/laugh.wav",
            category = "Funny"
        )

        val id = repository.insertAudioFile(audio)

        // Move to Troll category
        repository.updateCategory(id, "Troll")

        val updated = repository.getAudioFileById(id)
        assertEquals("Troll", updated?.category)

        val trollSounds = repository.getAudioFilesByCategory("Troll").first()
        assertEquals(1, trollSounds.size)
        assertEquals("Troll Laugh", trollSounds[0].title)
    }

    @Test
    fun testDeleteLocalMemeAudio() = runBlocking {
        val testFile = File(context.filesDir, "test_meme_delete.wav")
        testFile.writeText("dummy wav content")

        val audio = LocalMemeAudioEntity(
            title = "Temporary Meme",
            filePath = testFile.absolutePath,
            category = "Custom"
        )

        val id = repository.insertAudioFile(audio)
        assertNotNull(repository.getAudioFileById(id))
        assertTrue(testFile.exists())

        repository.deleteAudioFileById(id, deletePhysicalFile = true)

        assertNull(repository.getAudioFileById(id))
        assertTrue(!testFile.exists())
    }

    @Test
    fun testCategoryWithCounts() = runBlocking {
        repository.insertAudioFiles(
            listOf(
                LocalMemeAudioEntity(title = "S1", filePath = "/p1", category = "Gaming"),
                LocalMemeAudioEntity(title = "S2", filePath = "/p2", category = "Gaming"),
                LocalMemeAudioEntity(title = "S3", filePath = "/p3", category = "Troll")
            )
        )

        val counts = repository.categoriesWithCounts.first()
        val gamingCount = counts.find { it.category == "Gaming" }?.count
        val trollCount = counts.find { it.category == "Troll" }?.count

        assertEquals(2, gamingCount)
        assertEquals(1, trollCount)
    }

    @Test
    fun testSearchLocalAudioFiles() = runBlocking {
        repository.insertAudioFiles(
            listOf(
                LocalMemeAudioEntity(title = "Dramatic Vine Boom", filePath = "/p1", category = "Reaction"),
                LocalMemeAudioEntity(title = "Metal Pipe Falling", filePath = "/p2", category = "Effects")
            )
        )

        val searchResult = repository.searchAudioFiles("Boom").first()
        assertEquals(1, searchResult.size)
        assertEquals("Dramatic Vine Boom", searchResult[0].title)
    }
}
