// app/src/main/java/com/hyperboost/engine/services/FreeFireWatcherService.kt
package com.hyperboost.engine.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hyperboost.engine.MainActivity
import com.hyperboost.engine.core.FreeFire
import com.hyperboost.engine.core.FreeFireBoost
import com.hyperboost.engine.telemetry.TelemetryHub
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watches the foreground app. When Free Fire / Free Fire MAX comes to the front it applies the
 * saved Game Turbo options and shows the sidebar; a few seconds after the game is left it puts the
 * phone back. With "auto start" off it quits right after that restore.
 */
class FreeFireWatcherService : Service() {

    companion object {
        const val ACTION_STOP = "com.lsupra.gameturbo.STOP_WATCHER"
        private const val CHANNEL = "lsupra_ff_watcher"
        private const val NOTIF_ID = 9102
        private const val AWAY_MS = 5_000L
        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, FreeFireWatcherService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, FreeFireWatcherService::class.java).setAction(ACTION_STOP))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val hub by lazy { TelemetryHub.get(applicationContext) }
    private val boost by lazy { FreeFireBoost(applicationContext) }
    private var job: Job? = null
    private var awayJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CHANNEL, "Free Fire Turbo", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIF_ID, notification())
        _running.value = true
        hub.acquire("ffwatch", 2000L)
        if (job == null) job = scope.launch { watch() }
        return START_STICKY
    }

    private suspend fun watch() {
        hub.foregroundPackage.collect { fg ->
            if (FreeFire.isFreeFire(fg)) {
                awayJob?.cancel()
                if (!boost.isActive()) {
                    val o = boost.loadOptions()
                    boost.apply(fg!!, o)
                    if (o.sidebar) GameSidebarService.start(applicationContext, fg, FreeFire.PACKAGES[fg] ?: "Free Fire")
                }
            } else if (boost.isActive() && fg != null && fg != packageName && awayJob?.isActive != true) {
                awayJob = scope.launch {
                    delay(AWAY_MS)
                    val now = hub.foregroundPackage.value
                    if (!FreeFire.isFreeFire(now) && now != packageName) {
                        boost.restore()
                        if (!boost.loadOptions().autoStart) stopSelf()
                    }
                }
            }
        }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 2, Intent(this, FreeFireWatcherService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Free Fire Turbo đang theo dõi")
            .setContentText("Tự tăng tốc khi bạn vào game")
            .setContentIntent(open).addAction(0, "Tắt", stop).setOngoing(true).build()
    }

    override fun onDestroy() {
        job?.cancel(); awayJob?.cancel(); scope.cancel()
        hub.release("ffwatch")
        _running.value = false
        // Never leave the phone boosted when the watcher goes away.
        if (boost.isActive()) CoroutineScope(Dispatchers.IO).launch { boost.restore() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
