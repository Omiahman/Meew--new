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
    entities = [MemeSoundEntity::class, UserSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MemeMicDatabase : RoomDatabase() {
    abstract fun memeSoundDao(): MemeSoundDao
    abstract fun userSettingsDao(): UserSettingsDao

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
                        populateDatabase(database.memeSoundDao(), database.userSettingsDao())
                    }
                }
            }

            suspend fun populateDatabase(soundDao: MemeSoundDao, settingsDao: UserSettingsDao) {
                if (soundDao.getSoundCount() == 0) {
                    soundDao.insertSounds(DefaultSoundsData.getInitialSounds())
                }
                if (settingsDao.getSettingsOnce() == null) {
                    settingsDao.insertOrUpdate(UserSettingsEntity())
                }
            }
        }
    }
}
