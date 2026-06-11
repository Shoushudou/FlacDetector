package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.db.HistoryDatabase
import com.example.data.repository.HistoryRepository

class FlacDetectiveApplication : Application() {

    // Dependency container
    lateinit var historyRepository: HistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Room Database carefully
        val database = Room.databaseBuilder(
            applicationContext,
            HistoryDatabase::class.java,
            "flac_detective_database"
        )
        .fallbackToDestructiveMigration() // safe upgrade protocol for database schema changes
        .build()

        historyRepository = HistoryRepository(database.scanDao())
    }
}
