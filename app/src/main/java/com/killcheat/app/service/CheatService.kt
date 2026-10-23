package com.killcheat.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.killcheat.app.R
import com.killcheat.app.state.CheatState

class CheatService : Service() {

    companion object {
        private const val CHANNEL_ID = "killcheat_core"
        private const val NOTIF_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("target")?.let { CheatState.targetGame = it }
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        startService(Intent(this, VpnCoreService::class.java))
        return START_STICKY
    }

    private fun createChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "KillCheat Core", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification {
        val game = if (CheatState.targetGame == "ff") "Free Fire" else "FF MAX"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KillCheat Running")
            .setContentText("Target: $game")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
