package dev.tajim.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.tajim.jarvis.JarvisApp
import dev.tajim.jarvis.MainActivity
import dev.tajim.jarvis.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the assistant alive in the background. A persistent notification is always shown while it runs
 * (Android requires it for microphone use) with Sleep/Wake and Stop buttons.
 */
class AssistantService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller get() = (application as JarvisApp).container.assistant

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { controller.stop(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
            ACTION_TOGGLE -> {
                if (controller.state.value.mode == AssistantMode.ASLEEP) controller.wakeNow() else controller.sleepNow()
                return START_STICKY
            }
        }
        ensureChannel()
        try {
            val notification = build(controller.state.value.mode)
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTIF_ID, notification)
        } catch (e: SecurityException) {
            // Microphone permission missing: Android refuses a microphone service. Stop cleanly instead of crashing.
            stopSelf()
            return START_NOT_STICKY
        }
        controller.start()
        scope.launch {
            controller.state.map { it.mode }.distinctUntilChanged().collect { mode ->
                getSystemService(NotificationManager::class.java).notify(NOTIF_ID, build(mode))
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        controller.stop()
        super.onDestroy()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "JARVIS assistant", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun build(mode: AssistantMode): Notification {
        fun action(a: String) = PendingIntent.getService(
            this, a.hashCode(), Intent(this, AssistantService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val awake = mode == AssistantMode.AWAKE
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (awake) "Awake: listening for commands. Say \"sleep\" to rest." else "Sleeping: say \"Jarvis\" to wake me.")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, if (awake) "Sleep" else "Wake", action(ACTION_TOGGLE))
            .addAction(0, "Stop", action(ACTION_STOP))
            .build()
    }

    companion object {
        private const val CHANNEL = "assistant"
        private const val NOTIF_ID = 42
        const val ACTION_STOP = "dev.tajim.jarvis.STOP"
        const val ACTION_TOGGLE = "dev.tajim.jarvis.TOGGLE"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, AssistantService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AssistantService::class.java))
        }
    }
}
