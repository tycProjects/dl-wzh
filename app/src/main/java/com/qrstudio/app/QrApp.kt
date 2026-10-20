package com.qrstudio.app

import android.app.Application
import androidx.room.Room
import com.qrstudio.app.data.AppDatabase
import com.qrstudio.app.data.ScanHistoryRepository
import com.qrstudio.app.data.SettingsRepository

class QrApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var historyRepository: ScanHistoryRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "qr_studio.db").build()
        historyRepository = ScanHistoryRepository(database.scanHistoryDao())
        settingsRepository = SettingsRepository(getSharedPreferences("qr_studio_settings", MODE_PRIVATE))
    }
}
