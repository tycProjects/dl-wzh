package com.livevideowallpaper.app

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Waits for the screen to turn on and, if the phone is locked, opens LockVideoActivity above the lock screen.
 * Works on phones (like Honor) where the system lock screen cannot show a third-party live wallpaper.
 */
class LockVideoService : Service() {

    companion object {
        private const val CHANNEL = "lockvideo"
        private const val ID = 4712

        fun start(c: Context) {
            ContextCompat.startForegroundService(c, Intent(c, LockVideoService::class.java))
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, LockVideoService::class.java))
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            showLockVideo()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Video lock screen", NotificationManager.IMPORTANCE_LOW))
        val tap = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Video lock screen is on")
            .setContentText("Tap to open the app")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tap)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, n)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun showLockVideo() {
        if (!Prefs.lockEnabled(this) || !Settings.canDrawOverlays(this)) return
        if (CallHub.calls.isNotEmpty()) return
        val km = getSystemService(KeyguardManager::class.java)
        if (!km.isKeyguardLocked) return
        val path = Prefs.bgPath(this, Prefs.SLOT_LOCK) ?: Prefs.selectedPath(this)
        if (path == null || !File(path).isFile) return
        try {
            startActivity(
                Intent(this, LockVideoActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            )
        } catch (_: Exception) {}
    }
}
