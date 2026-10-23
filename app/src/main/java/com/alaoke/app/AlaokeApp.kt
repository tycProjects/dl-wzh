package com.alaoke.app

import android.app.Application
import com.alaoke.app.data.AlaokeDatabase
import com.alaoke.app.data.AlaokeRepository
import com.alaoke.app.youtube.YouTubeService

class AlaokeApp : Application() {

    lateinit var repository: AlaokeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AlaokeDatabase.getInstance(this)
        val youTubeService = YouTubeService()
        repository = AlaokeRepository(db, youTubeService)
    }
}
