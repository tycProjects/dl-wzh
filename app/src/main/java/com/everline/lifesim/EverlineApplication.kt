package com.everline.lifesim

import android.app.Application
import androidx.room.Room
import com.everline.lifesim.data.EverlineDatabase
import com.everline.lifesim.data.GameRepository

class EverlineApplication : Application() {
    val database: EverlineDatabase by lazy {
        Room.databaseBuilder(applicationContext, EverlineDatabase::class.java, "everline.db")
            // No destructive migration: schema changes must have explicit migrations.
            .build()
    }
    val repository: GameRepository by lazy { GameRepository(database) }
}
