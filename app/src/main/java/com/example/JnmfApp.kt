package com.example

import android.app.Application
import com.example.audio.AudioEngine
import com.example.data.db.AppDatabase
import com.example.data.repository.MusicRepository

class JnmfApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: MusicRepository by lazy {
        MusicRepository(this, database)
    }

    val audioEngine: AudioEngine by lazy {
        AudioEngine(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
