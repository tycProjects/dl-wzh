package com.lecturemanager.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lecturemanager.app.data.Annotation
import com.lecturemanager.app.data.Bookmark
import com.lecturemanager.app.data.Flashcard
import com.lecturemanager.app.data.Lecture
import com.lecturemanager.app.data.PageHit
import com.lecturemanager.app.data.Prefs
import com.lecturemanager.app.data.Repository
import com.lecturemanager.app.data.SettingsState
import com.lecturemanager.app.data.StorageInfo
import com.lecturemanager.app.data.Subject
import com.lecturemanager.app.data.todayEpochDay
import com.lecturemanager.app.export.buildNotesText
import com.lecturemanager.app.export.exportNotesPdf
import com.lecturemanager.app.widget.WidgetUpdater
import com.lecturemanager.app.work.BackupWorker
import com.lecturemanager.app.work.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** حالة مؤقّت المذاكرة (بومودورو أو مؤقّت حر). */
data class TimerUi(
    val running: Boolean = false,
    val pomodoro: Boolean = true,
    val work: Boolean = true,
    /** بومودورو: الثواني المتبقية في المرحلة، حر: الثواني المنقضية. */
    val seconds: Int = 0,
    val lectureId: Long = 0,
    val subjectId: Long = 0,
)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val prefs = Prefs(app)

    // ---------- البيانات ----------
    val subjects: StateFlow<List<Subject>> =
        repo.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lectures: StateFlow<List<Lecture>> =
        repo.lectures.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val flashcards: StateFlow<List<Flashcard>> =
        repo.flashcards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val dayTotals = repo.dayTotals(todayEpochDay() - 6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val subjectTotals = repo.subjectTotals(todayEpochDay() - 29)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storage = MutableStateFlow(StorageInfo(0, 0, 0))
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)

    // ---------- الإعدادات والقفل ----------
    val settings = MutableStateFlow(prefs.read())
    val locked = MutableStateFlow(settings.value.lockEnabled)

    init {
        // تحديث الويدجت بعد أي تغيير في البيانات (مع تأخير حتى لا نحدّثه مع كل صفحة تُقرأ)
        viewModelScope.launch {
            repo.lectures.collectLatest { delay(1500); runCatching { WidgetUpdater.update(app) } }
        }
    }

    fun updateSettings(block: SettingsState.() -> SettingsState) {
        val n = prefs.read().block()
        prefs.write(n)
        settings.value = n
        Scheduler.applyReminder(app, n)
        Scheduler.applyBackup(app, n)
    }

    fun lock() { if (settings.value.lockEnabled) locked.value = true }
    fun unlock() { locked.value = false }
    fun tryUnlock(pin: String): Boolean = prefs.checkPin(pin).also { if (it) locked.value = false }
    fun enableLock(pin: String) { prefs.setPin(pin); updateSettings { copy(lockEnabled = true) } }
    fun disableLock() { prefs.clearPin(); updateSettings { copy(lockEnabled = false, biometric = false) }; locked.value = false }
    fun checkPin(pin: String) = prefs.checkPin(pin)

    // ---------- المواد والمحاضرات ----------
    fun lecture(id: Long) = repo.lecture(id)
    fun bookmarks(id: Long) = repo.bookmarks(id)
    fun annotations(id: Long) = repo.annotations(id)

    fun addSubject(s: Subject) = viewModelScope.launch { repo.addSubject(s) }
    data class ScheduleItem(val subject: String, val instructor: String, val title: String, val week: Int, val notes: String, val pdf: Uri?)

    /** استقبال عدة محاضرات دفعة واحدة من تطبيق الجدول. */
    fun importBatchFromSchedule(items: List<ScheduleItem>) = viewModelScope.launch {
        var ok = 0
        var anyPdf = false
        items.forEach { it0 ->
            try { repo.importFromSchedule(it0.subject, it0.instructor, it0.week, it0.title, it0.notes, it0.pdf); ok++; if (it0.pdf != null) anyPdf = true } catch (_: Exception) {}
        }
        if (anyPdf) Scheduler.enqueueIndex(app)
        message.value = if (ok == items.size) "تم استلام $ok محاضرة من تطبيق الجدول" else "تم استلام $ok من ${items.size} محاضرة من تطبيق الجدول"
    }

    fun importFromSchedule(subject: String, instructor: String, week: Int, title: String, notes: String, pdf: Uri?) =
        viewModelScope.launch {
            try {
                repo.importFromSchedule(subject, instructor, week, title, notes, pdf)
                if (pdf != null) Scheduler.enqueueIndex(app)
                message.value = "تم استلام المحاضرة من تطبيق الجدول: $title"
            } catch (e: Exception) { message.value = "تعذّر استلام المحاضرة من تطبيق الجدول" }
        }

    fun updateSubject(s: Subject) = viewModelScope.launch { repo.updateSubject(s) }
    fun updateLecture(l: Lecture) = viewModelScope.launch { repo.updateLecture(l) }

    fun addLecture(l: Lecture, pdf: Uri?, onDone: () -> Unit) = viewModelScope.launch {
        busy.value = true
        try {
            val (path, size) = pdf?.let { repo.importPdf(it) } ?: (null to 0L)
            repo.addLecture(l.copy(pdfPath = path, fileSize = size))
            if (path != null) Scheduler.enqueueIndex(app)
            onDone()
        } catch (e: Exception) {
            message.value = "تعذّر إضافة المحاضرة: ${e.message}"
        } finally { busy.value = false }
    }

    fun toggle(l: Lecture) = viewModelScope.launch { repo.setCompleted(l.id, !l.isCompleted) }
    fun setCompleted(id: Long, done: Boolean) = viewModelScope.launch { repo.setCompleted(id, done) }
    fun saveNotes(id: Long, notes: String, tag: String) = viewModelScope.launch { repo.setNotes(id, notes, tag) }
    fun deleteLecture(l: Lecture) = viewModelScope.launch { repo.deleteLecture(l) }
    fun deleteSubject(s: Subject) = viewModelScope.launch { repo.deleteSubject(s) }
    fun moveSubject(id: Long, delta: Int) = viewModelScope.launch { repo.moveSubject(id, delta) }
    fun moveLecture(id: Long, delta: Int) = viewModelScope.launch { repo.moveLecture(id, delta) }
    fun saveProgress(id: Long, page: Int, count: Int) = viewModelScope.launch { repo.setProgress(id, page, count) }

    fun attachPdf(id: Long, uri: Uri) = viewModelScope.launch {
        busy.value = true
        try { repo.attachPdf(id, uri); Scheduler.enqueueIndex(app) }
        catch (e: Exception) { message.value = "تعذّر إرفاق الملف" }
        finally { busy.value = false }
    }

    // ---------- الإشارات والرسم ----------
    fun addBookmark(lectureId: Long, page: Int) = viewModelScope.launch { repo.addBookmark(lectureId, page) }
    fun deleteBookmark(b: Bookmark) = viewModelScope.launch { repo.deleteBookmark(b.id) }
    fun addAnnotation(a: Annotation) = viewModelScope.launch { repo.addAnnotation(a) }
    fun deleteAnnotation(id: Long) = viewModelScope.launch { repo.deleteAnnotation(id) }

    // ---------- البحث في نص الملفات ----------
    val pdfHits = MutableStateFlow<List<PageHit>>(emptyList())
    private var searchJob: Job? = null

    fun searchPdfText(q: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(250); pdfHits.value = repo.searchPages(q) }
    }

    suspend fun searchInLecture(lectureId: Long, q: String): List<PageHit> = repo.searchPagesIn(lectureId, q)

    fun reindexAll() { Scheduler.enqueueIndex(app) }

    // ---------- بطاقات المراجعة ----------
    fun generateCards(l: Lecture, notes: String) = viewModelScope.launch {
        val n = repo.generateCards(l, notes)
        message.value = if (n > 0) "تم إنشاء $n بطاقة جديدة" else "لم أجد صيغة بطاقات في الملاحظات (مصطلح :: تعريف، أو س: ... ج: ...)"
    }
    fun addCard(subjectId: Long, front: String, back: String) = viewModelScope.launch { repo.addCard(subjectId, front, back) }
    fun updateCard(c: Flashcard) = viewModelScope.launch { repo.dao.updateFlashcard(c) }
    fun deleteCard(id: Long) = viewModelScope.launch { repo.deleteCard(id) }
    fun reviewCard(c: Flashcard, correct: Boolean) = viewModelScope.launch { repo.reviewCard(c, correct) }

    // ---------- مؤقّت المذاكرة ----------
    val timer = MutableStateFlow(TimerUi())
    private var timerJob: Job? = null
    private var phaseStart = 0L
    private var phaseWork = true
    private var timerPomodoro = true
    private var timerLecture = 0L
    private var timerSubject = 0L

    fun startTimer(lectureId: Long, subjectId: Long, pomodoro: Boolean) {
        stopTimer()
        val cfg = settings.value
        timerLecture = lectureId; timerSubject = subjectId; timerPomodoro = pomodoro; phaseWork = true
        phaseStart = SystemClock.elapsedRealtime()
        timer.value = TimerUi(true, pomodoro, true, if (pomodoro) cfg.pomodoroWork * 60 else 0, lectureId, subjectId)
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(500)
                val elapsed = (SystemClock.elapsedRealtime() - phaseStart) / 1000
                if (!timerPomodoro) {
                    timer.value = timer.value.copy(seconds = elapsed.toInt())
                } else {
                    val total = (if (phaseWork) cfg.pomodoroWork else cfg.pomodoroBreak) * 60L
                    if (elapsed >= total) {
                        if (phaseWork) repo.logStudy(timerSubject, timerLecture, total)
                        notifyPhaseEnd(phaseWork)
                        phaseWork = !phaseWork
                        phaseStart = SystemClock.elapsedRealtime()
                        val next = (if (phaseWork) cfg.pomodoroWork else cfg.pomodoroBreak) * 60
                        timer.value = timer.value.copy(work = phaseWork, seconds = next)
                    } else timer.value = timer.value.copy(seconds = (total - elapsed).toInt())
                }
            }
        }
    }

    /** يوقف المؤقّت ويسجّل الوقت المنقضي في مرحلة المذاكرة الحالية. */
    fun stopTimer() {
        timerJob?.cancel(); timerJob = null
        if (timer.value.running && phaseWork) {
            val elapsed = (SystemClock.elapsedRealtime() - phaseStart) / 1000
            if (elapsed >= 30) viewModelScope.launch { repo.logStudy(timerSubject, timerLecture, elapsed) }
        }
        timer.value = TimerUi()
    }

    private fun notifyPhaseEnd(wasWork: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(app, LectureApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (wasWork) "انتهت فترة المذاكرة ☕" else "انتهت الاستراحة 📚")
            .setContentText(if (wasWork) "خذ استراحة قصيرة" else "ارجع للمذاكرة")
            .setAutoCancel(true).build()
        try { NotificationManagerCompat.from(app).notify(1002, n) } catch (_: SecurityException) {}
    }

    override fun onCleared() { stopTimer(); super.onCleared() }

    // ---------- التخزين والنسخ الاحتياطي والتصدير ----------
    fun refreshStorage() = viewModelScope.launch { storage.value = repo.storageInfo() }

    fun clearCache() = viewModelScope.launch {
        repo.clearCache(); refreshStorage(); message.value = "تم مسح الذاكرة المؤقتة"
    }

    fun export(uri: Uri) = viewModelScope.launch {
        busy.value = true
        try { repo.exportBackup(uri); message.value = "تم تصدير النسخة الاحتياطية" }
        catch (e: Exception) { message.value = "فشل التصدير: ${e.message}" }
        finally { busy.value = false }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        busy.value = true
        try { repo.importBackup(uri); refreshStorage(); Scheduler.enqueueIndex(app); message.value = "تم استيراد النسخة الاحتياطية" }
        catch (e: Exception) { message.value = "فشل الاستيراد: ${e.message}" }
        finally { busy.value = false }
    }

    fun backupNow() = viewModelScope.launch {
        busy.value = true
        try {
            val dir = BackupWorker.backupDir(app)
            val name = "backup-" + java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(java.util.Date()) + ".zip"
            repo.exportBackupToFile(File(dir, name))
            updateSettings { copy(lastBackup = System.currentTimeMillis()) }
            message.value = "تم حفظ نسخة في: ${dir.name}/$name"
        } catch (e: Exception) { message.value = "فشل النسخ: ${e.message}" }
        finally { busy.value = false }
    }

    /** تصدير ملاحظات مادة إلى ملف (PDF أو نص) في الموضع الذي اختاره المستخدم. */
    fun exportNotes(subject: Subject, pdf: Boolean, target: Uri) = viewModelScope.launch {
        busy.value = true
        try {
            val ls = lectures.value.filter { it.subjectId == subject.id }
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(target)!!.use { out ->
                    if (pdf) exportNotesPdf(app, subject, ls).inputStream().use { it.copyTo(out) }
                    else out.write(buildNotesText(subject, ls).toByteArray())
                }
            }
            message.value = "تم تصدير الملاحظات"
        } catch (e: Exception) { message.value = "فشل التصدير: ${e.message}" }
        finally { busy.value = false }
    }

    fun consumeMessage() { message.value = null }
}
