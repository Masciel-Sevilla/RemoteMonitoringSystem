package com.example.remotemonitoringsystem.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SensorData::class, Credentials::class],
    version = 2, // <-- PASO 1: Aumenta la versión
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "monitoring_database"
                )
                    .fallbackToDestructiveMigration() // <-- PASO 2: Añade esta línea
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}