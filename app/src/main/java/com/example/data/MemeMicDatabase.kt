package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MemeSoundEntity::class,
        UserSettingsEntity::class,
        LocalMemeAudioEntity::class,
        SoundCategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MemeMicDatabase : RoomDatabase() {
    abstract fun memeSoundDao(): MemeSoundDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun localMemeAudioDao(): LocalMemeAudioDao
    abstract fun soundCategoryDao(): SoundCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemeMicDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MemeMicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemeMicDatabase::class.java,
                    "mememic_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(MemeMicDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class MemeMicDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(
                            database.memeSoundDao(),
                            database.userSettingsDao(),
                            database.soundCategoryDao()
                        )
                    }
                }
            }

            suspend fun populateDatabase(
                soundDao: MemeSoundDao,
                settingsDao: UserSettingsDao,
                categoryDao: SoundCategoryDao
            ) {
                if (soundDao.getSoundCount() == 0) {
                    soundDao.insertSounds(DefaultSoundsData.getInitialSounds())
                }
                if (settingsDao.getSettingsOnce() == null) {
                    settingsDao.insertOrUpdate(UserSettingsEntity())
                }
                if (categoryDao.getCategoryCount() == 0) {
                    val defaultCategories = listOf(
                        SoundCategoryEntity(name = "Gaming", iconName = "sports_esports", colorHex = "#00FF66", displayOrder = 1, isDefault = true),
                        SoundCategoryEntity(name = "Troll", iconName = "sentiment_very_dissatisfied", colorHex = "#FF0055", displayOrder = 2, isDefault = true),
                        SoundCategoryEntity(name = "Funny", iconName = "mood", colorHex = "#FFB800", displayOrder = 3, isDefault = true),
                        SoundCategoryEntity(name = "Reaction", iconName = "flash_on", colorHex = "#00F0FF", displayOrder = 4, isDefault = true),
                        SoundCategoryEntity(name = "Laugh", iconName = "sentiment_very_satisfied", colorHex = "#FFD700", displayOrder = 5, isDefault = true),
                        SoundCategoryEntity(name = "Voice", iconName = "mic", colorHex = "#B388FF", displayOrder = 6, isDefault = true),
                        SoundCategoryEntity(name = "Effects", iconName = "graphic_eq", colorHex = "#00E5FF", displayOrder = 7, isDefault = true),
                        SoundCategoryEntity(name = "Custom", iconName = "folder", colorHex = "#76FF03", displayOrder = 8, isDefault = true)
                    )
                    categoryDao.insertCategories(defaultCategories)
                }
            }
        }
    }
}

