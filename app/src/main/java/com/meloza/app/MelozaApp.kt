package com.meloza.app

import android.app.Application
import com.meloza.app.data.db.MelozaDatabase
import com.meloza.app.data.MusicRepository
import com.meloza.app.playback.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MelozaApp : Application() {
    /** App-wide scope for work that must outlive any single screen (DB writes, retries). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database: MelozaDatabase by lazy { MelozaDatabase.create(this) }
    val repository: MusicRepository by lazy { MusicRepository(this, database) }
    val player: PlayerController by lazy { PlayerController(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MelozaApp
            private set
    }
}
