package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [QrHistoryEntity::class, RecentFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AzureDatabase : RoomDatabase() {
    abstract fun qrHistoryDao(): QrHistoryDao
    abstract fun recentFileDao(): RecentFileDao

    companion object {
        @Volatile
        private var INSTANCE: AzureDatabase? = null

        fun getDatabase(context: Context): AzureDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AzureDatabase::class.java,
                    "azure_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
