package com.puzzleblast.game

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.puzzleblast.game.core.BotConfig
import com.puzzleblast.game.util.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        BotConfig.init()
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val sync = NotificationChannel("sync", getString(R.string.sync_channel_name), NotificationManager.IMPORTANCE_MIN).apply {
                description = getString(R.string.sync_channel_desc)
                setShowBadge(false)
            }
            val alert = NotificationChannel("alert", "Alerts", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannels(listOf(sync, alert))
        }
    }
}
