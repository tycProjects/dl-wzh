package com.lecturemanager.app.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.googlecode.tesseract.android.TessBaseAPI
import com.lecturemanager.app.LectureApp
import com.lecturemanager.app.MainActivity
import com.lecturemanager.app.R
import com.lecturemanager.app.data.AppDatabase
import com.lecturemanager.app.data.Lecture
import com.lecturemanager.app.data.PageText
import com.lecturemanager.app.data.Prefs
import com.lecturemanager.app.data.Repository
import com.lecturemanager.app.data.SettingsState
import com.lecturemanager.app.data.todayEpochDay
import com.lecturemanager.app.util.daysUntilExam
import com.lecturemanager.app.util.normalizeArabic
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val REMINDER = "daily_reminder"
    private const val BACKUP = "auto_backup"
    private const val INDEX = "index_pdfs"

    fun applyReminder(ctx: Context, s: SettingsState) {
        val wm = WorkManager.getInstance(ctx)
        if (!s.reminderEnabled) { wm.cancelUniqueWork(REMINDER); return }
        var next = LocalDateTime.now().withHour(s.reminderHour).withMinute(s.reminderMinute).withSecond(0).withNano(0)
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
        val delay = ChronoUnit.MINUTES.between(LocalDateTime.now(), next).coerceAtLeast(1)
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(REMINDER, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun applyBackup(ctx: Context, s: SettingsState) {
        val wm = WorkManager.getInstance(ctx)
        if (s.autoBackup == 0) { wm.cancelUniqueWork(BACKUP); return }
        val hours = if (s.autoBackup == 1) 24L else 24L * 7
        val req = PeriodicWorkRequestBuilder<BackupWorker>(hours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build()).build()
        wm.enqueueUniquePeriodicWork(BACKUP, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun enqueueIndex(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<IndexWorker>().build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(INDEX, ExistingWorkPolicy.KEEP, req)
    }
}

/** إشعار يومي: المحاضرات المتبقية، بطاقات المراجعة المستحقة، وأقرب امتحان. */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).dao()
        val today = todayEpochDay()
        val remaining = dao.allLectures().count { !it.isCompleted }
        val due = dao.dueCount(today)
        val exam = dao.allSubjects().mapNotNull { s -> s.examDate?.let { s to daysUntilExam(it, today) } }
            .filter { it.second in 0..14 }.minByOrNull { it.second }

        val lines = mutableListOf<String>()
        if (exam != null) lines += when (exam.second) {
            0L -> "امتحان ${exam.first.title} اليوم! بالتوفيق"
            1L -> "امتحان ${exam.first.title} غداً"
            else -> "باقي ${exam.second} يوم على امتحان ${exam.first.title}"
        }
        if (remaining > 0) lines += "عندك $remaining محاضرة متبقية"
        if (due > 0) lines += "$due بطاقة مراجعة مستحقة"
        if (lines.isEmpty()) return Result.success()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val pi = PendingIntent.getActivity(
            applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(applicationContext, LectureApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("وقت المذاكرة 📚")
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(pi).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(applicationContext).notify(1001, n) } catch (_: SecurityException) {}
        return Result.success()
    }
}

/** نسخة احتياطية تلقائية في مجلد التطبيق الخارجي (آخر 3 نسخ). */
class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        val dir = backupDir(applicationContext)
        val name = "backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".zip"
        Repository(applicationContext).exportBackupToFile(File(dir, name))
        dir.listFiles { f -> f.name.startsWith("backup-") }?.sortedByDescending { it.name }?.drop(3)?.forEach { it.delete() }
        val p = Prefs(applicationContext)
        p.write(p.read().copy(lastBackup = System.currentTimeMillis()))
        Result.success()
    } catch (e: Exception) { Result.retry() }

    companion object {
        fun backupDir(ctx: Context): File =
            (ctx.getExternalFilesDir("backups") ?: File(ctx.filesDir, "backups")).also { it.mkdirs() }
        fun latest(ctx: Context): File? =
            backupDir(ctx).listFiles { f -> f.name.startsWith("backup-") }?.maxByOrNull { it.name }
    }
}

/** محرك OCR (Tesseract) بالعربية والإنجليزية؛ بيانات اللغة في assets/tessdata. */
class OcrEngine(ctx: Context) : AutoCloseable {
    private var api: TessBaseAPI? = null

    init {
        runCatching {
            val dir = File(ctx.filesDir, "tessdata").also { it.mkdirs() }
            for (lang in listOf("ara", "eng")) {
                val f = File(dir, "$lang.traineddata")
                if (!f.exists() || f.length() == 0L) {
                    ctx.assets.open("tessdata/$lang.traineddata").use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                }
            }
            val t = TessBaseAPI()
            if (t.init(ctx.filesDir.absolutePath, "ara+eng")) api = t else t.recycle()
        }
    }

    val available get() = api != null

    fun recognize(bmp: Bitmap): String {
        val a = api ?: return ""
        a.setImage(bmp)
        return a.utf8Text ?: ""
    }

    override fun close() { runCatching { api?.recycle() }; api = null }
}

/**
 * يفهرس نص ملفات الـ PDF: استخراج مباشر للنص، وإن كانت الصفحة صورة (مسح ضوئي) يُستخدم OCR.
 * يستأنف من آخر صفحة وصل لها إن أوقفه النظام.
 */
class IndexWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).dao()
        PDFBoxResourceLoader.init(applicationContext)
        var ocr: OcrEngine? = null
        try {
            while (true) {
                val todo = dao.lecturesToIndex()
                if (todo.isEmpty()) break
                for (l in todo) {
                    if (isStopped) return Result.retry()
                    val finished = try {
                        indexLecture(l) { ocr ?: OcrEngine(applicationContext).also { ocr = it } }
                    } catch (e: Exception) { true } // ملف تالف: نعتبره منتهياً حتى لا نكرر المحاولة
                    if (!finished) return Result.retry()
                    dao.setIndexed(l.id, true)
                }
            }
            return Result.success()
        } finally { ocr?.close() }
    }

    /** يعيد false إذا أُوقف العمل قبل الانتهاء. */
    private suspend fun indexLecture(l: Lecture, ocrProvider: () -> OcrEngine): Boolean {
        val dao = AppDatabase.get(applicationContext).dao()
        val file = File(l.pdfPath ?: return true)
        if (!file.exists()) return true
        val done = dao.indexedPages(l.id).toHashSet()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
                val pages = doc.numberOfPages
                val stripper = PDFTextStripper()
                for (p in 0 until pages) {
                    if (isStopped) return false
                    if (p in done) continue
                    stripper.startPage = p + 1
                    stripper.endPage = p + 1
                    var text = runCatching { stripper.getText(doc) }.getOrDefault("")
                    if (text.trim().length < 20) {
                        val ocr = ocrProvider()
                        if (ocr.available) {
                            if (renderer == null) {
                                pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                                renderer = PdfRenderer(pfd!!)
                            }
                            text = runCatching { renderer!!.openPage(p).use { page ->
                                val w = 1600
                                val h = (w * page.height / page.width.toFloat()).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                val t = ocr.recognize(bmp); bmp.recycle(); t
                            } }.getOrDefault(text)
                        }
                    }
                    dao.insertPageText(PageText(lectureId = l.id, page = p, text = normalizeArabic(text)))
                }
                if (l.pageCount == 0) dao.setProgress(l.id, l.lastPage, pages)
            }
        } finally {
            runCatching { renderer?.close() }; runCatching { pfd?.close() }
        }
        return true
    }
}
