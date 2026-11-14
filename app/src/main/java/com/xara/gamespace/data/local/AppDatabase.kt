package com.xara.gamespace.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameProfileEntity::class,
        BoostHistoryEntity::class,
        GameSessionEntity::class,
        TouchProfileEntity::class,
        MacroConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun xaraDao(): XaraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xara_gamespace.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
