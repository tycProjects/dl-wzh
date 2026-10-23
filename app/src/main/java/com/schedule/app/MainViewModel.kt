package com.schedule.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.app.data.*
import com.schedule.app.util.*
import com.schedule.app.work.BackupWorker
import com.schedule.app.work.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private fun <T> Flow<T>.state(init: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), init)

    val slots = repo.slots.state(emptyList())
    val notes = repo.notes.state(emptyList())
    val attachments = repo.attachments.state(emptyList())
    val exams = repo.exams.state(emptyList())
    val exceptions = repo.exceptions.state(emptyList())
    val attendance = repo.attendance.state(emptyList())
    val tasks = repo.tasks.state(emptyList())
    val holidays = repo.holidays.state(emptyList())
    val settings = MutableStateFlow(repo.prefs.read())
    val message = MutableStateFlow<String?>(null)

    /** طلب تنقل قادم من إشعار: 1 = فتح محاضرة، 2 = بدء تسجيل */
    data class NavRequest(val kind: Int, val slotId: Long)
    val navRequest = MutableStateFlow<NavRequest?>(null)

    // ---- قفل التطبيق
    val locked = MutableStateFlow(settings.value.lockEnabled)
    fun lock() { if (settings.value.lockEnabled) locked.value = true }
    fun unlock() { locked.value = false }
    fun checkPin(pin: String) = repo.prefs.checkPin(pin)
    fun tryUnlock(pin: String): Boolean = checkPin(pin).also { if (it) locked.value = false }
    fun enableLock(pin: String) { repo.prefs.setPin(pin); updateSettings { copy(lockEnabled = true) } }
    fun disableLock() { repo.prefs.clearPin(); updateSettings { copy(lockEnabled = false, biometric = false) }; locked.value = false }

    private fun launchSafe(err: String, block: suspend () -> Unit) = viewModelScope.launch {
        try { block() } catch (e: Exception) { message.value = err }
    }

    fun updateSettings(block: Settings.() -> Settings) {
        val old = settings.value
        val new = old.block()
        settings.value = new
        repo.prefs.write(new)
        val resched = old.leadMinutes != new.leadMinutes || old.notifyStart != new.notifyStart || old.notifyEnd != new.notifyEnd ||
            old.noteReminder != new.noteReminder || old.semesterStart != new.semesterStart || old.silentMode != new.silentMode || old.askRecord != new.askRecord
        if (old.autoBackup != new.autoBackup) Scheduler.applyBackup(app, new.autoBackup)
        viewModelScope.launch { if (resched) Scheduler.rescheduleAll(app) else if (old.ongoing != new.ongoing) Scheduler.refreshUi(app) }
    }

    fun saveSlot(s: Slot) = launchSafe("تعذّر حفظ المحاضرة") { repo.saveSlot(s) }
    fun deleteSlot(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteSlot(id) }
    fun saveNote(n: Note) = launchSafe("تعذّر حفظ الملاحظة") { repo.saveNote(n) }
    fun deleteNote(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteNote(id) }
    fun addAttachment(slotId: Long, uri: Uri) = launchSafe("تعذّر إرفاق الملف") { repo.addAttachment(slotId, uri); message.value = "تم إرفاق الملف" }
    fun deleteAttachment(a: Attachment) = launchSafe("تعذّر الحذف") { repo.deleteAttachment(a) }
    fun saveAttachmentTo(a: Attachment, uri: Uri) = launchSafe("تعذّر حفظ الملف") { repo.copyAttachmentTo(a, uri); message.value = "تم حفظ الملف" }
    fun saveExam(e: Exam) = launchSafe("تعذّر الحفظ") { repo.saveExam(e) }
    fun deleteExam(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteExam(id) }
    fun setName(n: String) = updateSettings { copy(profileName = n.trim()) }
    fun setPhoto(uri: Uri) = launchSafe("تعذّر تحميل الصورة") { val p = repo.setPhoto(uri); updateSettings { copy(photoPath = p) } }

    fun saveException(e: SlotException) = launchSafe("تعذّر الحفظ") { repo.saveException(e) }
    fun deleteException(e: SlotException) = launchSafe("تعذّر الحذف") { repo.deleteException(e) }
    fun setAttendance(slotId: Long, date: Long, status: Int?) = launchSafe("تعذّر تسجيل الحضور") { repo.setAttendance(slotId, date, status) }
    fun saveTask(t: Task) = launchSafe("تعذّر حفظ الواجب") { repo.saveTask(t) }
    fun toggleTask(t: Task) = saveTask(t.copy(done = !t.done))
    fun deleteTask(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteTask(id) }
    fun saveHoliday(h: Holiday) = launchSafe("تعذّر الحفظ") { repo.saveHoliday(h) }
    fun deleteHoliday(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteHoliday(id) }

    // ---- استيراد/تصدير الجدول بصيغة CSV
    val csvPreview = MutableStateFlow<CsvResult?>(null)
    fun previewCsv(uri: Uri) = launchSafe("تعذّرت قراءة الملف") {
        val text = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }
        csvPreview.value = parseScheduleCsv(text)
    }
    fun confirmCsv() = launchSafe("تعذّر الاستيراد") {
        val r = csvPreview.value ?: return@launchSafe
        csvPreview.value = null
        val n = repo.addSlots(r.slots.map { Slot(title = it.title, instructor = it.instructor, room = it.room, day = it.day, startMin = it.startMin, endMin = it.endMin, weekMode = it.weekMode) })
        message.value = "تمت إضافة $n محاضرة"
    }
    fun dismissCsv() { csvPreview.value = null }
    suspend fun scheduleCsv(): String = slotsToCsv(repo.allSlotsOnce().map { CsvSlot(it.title, it.day, it.startMin, it.endMin, it.instructor, it.room, it.weekMode) })

    fun exportTo(uri: Uri) = launchSafe("تعذّر التصدير") {
        app.contentResolver.openOutputStream(uri)!!.use { repo.export(it) }; message.value = "تم تصدير النسخة الاحتياطية"
    }
    fun importFrom(uri: Uri) = launchSafe("ملف النسخة الاحتياطية غير صالح") {
        app.contentResolver.openInputStream(uri)!!.use { repo.import(it) }
        settings.value = repo.prefs.read(); message.value = "تم استيراد النسخة الاحتياطية"
    }
    fun backupNow() = launchSafe("تعذّر النسخ الاحتياطي") {
        val f = File(BackupWorker.backupDir(app), "schedule-manual-${System.currentTimeMillis() / 1000}.zip")
        withContext(Dispatchers.IO) { f.outputStream().use { repo.export(it) } }
        updateSettings { copy(lastBackup = System.currentTimeMillis()) }
        message.value = "تم حفظ نسخة احتياطية في مجلد التطبيق"
    }
    fun rescheduleAll() = viewModelScope.launch { Scheduler.rescheduleAll(app) }
    fun consumeMessage() { message.value = null }
}
