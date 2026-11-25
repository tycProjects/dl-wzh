package com.example.service

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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BackgroundModeState {
    STOPPED,
    ACTIVE_LISTENING,
    PAUSED
}

object BackgroundAssistantRegistry {
    private val _state = MutableStateFlow(BackgroundModeState.STOPPED)
    val state: StateFlow<BackgroundModeState> = _state.asStateFlow()

    private val _batterySaverActive = MutableStateFlow(false)
    val batterySaverActive: StateFlow<Boolean> = _batterySaverActive.asStateFlow()

    internal fun updateState(newState: BackgroundModeState, batterySaver: Boolean) {
        _state.value = newState
        _batterySaverActive.value = batterySaver
    }
}

class MayaBackgroundService : Service() {

    companion object {
        const val CHANNEL_ID = "maya_bg_assistant_channel"
        const val NOTIFICATION_ID = 4101

        const val ACTION_START = "com.example.maya.BG_START"
        const val ACTION_PAUSE = "com.example.maya.BG_PAUSE"
        const val ACTION_RESUME = "com.example.maya.BG_RESUME"
        const val ACTION_STOP = "com.example.maya.BG_STOP"
        const val EXTRA_START_VOICE = "extra_start_voice"

        fun start(context: Context) {
            val intent = Intent(context, MayaBackgroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, MayaBackgroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, MayaBackgroundService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MayaBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentState = BackgroundModeState.STOPPED

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isBatterySaver = powerManager.isPowerSaveMode

        when (intent?.action) {
            ACTION_STOP -> {
                currentState = BackgroundModeState.STOPPED
                BackgroundAssistantRegistry.updateState(currentState, isBatterySaver)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                currentState = BackgroundModeState.PAUSED
                BackgroundAssistantRegistry.updateState(currentState, isBatterySaver)
                updateNotification(isBatterySaver)
            }
            ACTION_RESUME, ACTION_START, null -> {
                currentState = BackgroundModeState.ACTIVE_LISTENING
                BackgroundAssistantRegistry.updateState(currentState, isBatterySaver)
                val notification = buildNotification(isBatterySaver)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        BackgroundAssistantRegistry.updateState(BackgroundModeState.STOPPED, false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(isBatterySaver: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(isBatterySaver))
    }

    private fun buildNotification(isBatterySaver: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_VOICE, currentState == BackgroundModeState.ACTIVE_LISTENING)
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionIntent = Intent(this, MayaBackgroundService::class.java).apply {
            action = if (currentState == BackgroundModeState.PAUSED) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePending = PendingIntent.getService(
            this,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MayaBackgroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when {
            currentState == BackgroundModeState.PAUSED -> "লিসেনিং বিরতিতে আছে • চালু করতে ট্যাপ করো"
            isBatterySaver -> "ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট চালু (ব্যাটারি সাশ্রয় মোড)"
            else -> "মায়া প্রস্তুত • কথা বলতে ট্যাপ করো"
        }

        val toggleLabel = if (currentState == BackgroundModeState.PAUSED) "চালু করো" else "বিরতি দাও"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MAYA অ্যাসিস্ট্যান্ট")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, toggleLabel, togglePending)
            .addAction(0, "বন্ধ করো", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.bg_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MAYA transparent background assistant status"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
