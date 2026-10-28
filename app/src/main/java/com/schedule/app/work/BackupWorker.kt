package com.schedule.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.schedule.app.data.Prefs
import com.schedule.app.data.Repository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** نسخة احتياطية تلقائية في مجلد التطبيق (آخر 3 نسخ). */
class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        val dir = backupDir(applicationContext)
        val f = File(dir, "schedule-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".zip")
        f.outputStream().use { Repository(applicationContext).export(it) }
        dir.listFiles { x -> x.name.startsWith("schedule-") }?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach { it.delete() }
        runCatching { ExternalBackup.copyTo(applicationContext, f) }
        val p = Prefs(applicationContext); p.write(p.read().copy(lastBackup = System.currentTimeMillis()))
        Result.success()
    } catch (e: Exception) { Result.retry() }

    companion object {
        fun backupDir(ctx: Context): File = (ctx.getExternalFilesDir("backups") ?: File(ctx.filesDir, "backups")).also { it.mkdirs() }
    }
}
