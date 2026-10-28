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

    val settings = MutableStateFlow(repo.prefs.read())
    /** محاضرات الجدول النشط فقط */
    val slots = combine(repo.slots, settings) { l, st -> l.filter { it.scheduleId == st.activeSchedule } }.state(emptyList())
    val allSlots = repo.slots.state(emptyList())
    val schedules = repo.schedules.state(emptyList())
    val grades = repo.grades.state(emptyList())
    val credits = repo.credits.state(emptyList())
    val sessions = repo.sessions.state(emptyList())
    val cards = repo.cards.state(emptyList())
    val refs = repo.refs.state(emptyList())
    val rooms = repo.rooms.state(emptyList())
    val notes = repo.notes.state(emptyList())
    val attachments = repo.attachments.state(emptyList())
    val exams = repo.exams.state(emptyList())
    val exceptions = repo.exceptions.state(emptyList())
    val attendance = repo.attendance.state(emptyList())
    val tasks = repo.tasks.state(emptyList())
    val holidays = repo.holidays.state(emptyList())
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
            old.noteReminder != new.noteReminder || old.morningSummary != new.morningSummary || old.eveningSummary != new.eveningSummary ||
            old.morningHour != new.morningHour || old.eveningHour != new.eveningHour || old.forgotAttendance != new.forgotAttendance || old.examAlerts != new.examAlerts || old.semesterStart != new.semesterStart || old.silentMode != new.silentMode || old.askRecord != new.askRecord
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
        val ext = if (settings.value.extTree.isNotBlank()) withContext(Dispatchers.IO) { runCatching { com.schedule.app.work.ExternalBackup.copyTo(app, f) }.getOrDefault(false) } else null
        message.value = when (ext) { true -> "تم حفظ نسخة في مجلد التطبيق وفي المجلد الخارجي"; false -> "حُفظت النسخة محليا لكن تعذّر النسخ للمجلد الخارجي"; null -> "تم حفظ نسخة احتياطية في مجلد التطبيق" }
    }
    fun rescheduleAll() = viewModelScope.launch { Scheduler.rescheduleAll(app) }

    // ================= ميزات إضافية =================
    /** وجهة تنقل قادمة من اختصارات التطبيق */
    val route = MutableStateFlow<String?>(null)

    fun copySlot(s: Slot, day: Int) = launchSafe("تعذّر النسخ") { repo.copySlot(s, day); message.value = "تم نسخ المحاضرة إلى ${DAY_NAMES[day]}" }
    fun addSchedule(name: String) = launchSafe("تعذّر الإنشاء") { val id = repo.addSchedule(name); repo.switchSchedule(id); settings.value = repo.prefs.read() }
    fun renameSchedule(s: ScheduleInfo) = launchSafe("تعذّر الحفظ") { repo.renameSchedule(s) }
    fun deleteSchedule(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteSchedule(id); settings.value = repo.prefs.read() }
    fun switchSchedule(id: Long) = launchSafe("تعذّر التبديل") { repo.switchSchedule(id); settings.value = repo.prefs.read() }
    fun saveGrade(g: Grade) = launchSafe("تعذّر الحفظ") { repo.saveGrade(g) }
    fun deleteGrade(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteGrade(id) }
    fun setCredit(slotId: Long, h: Int) = launchSafe("تعذّر الحفظ") { repo.setCredit(slotId, h) }
    fun saveCard(c: Card) = launchSafe("تعذّر الحفظ") { repo.saveCard(c) }
    fun deleteCard(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteCard(id) }
    fun reviewCard(c: Card, quality: Int) = launchSafe("تعذّر الحفظ") {
        val n = sm2(SrState(c.intervalDays, c.ease, c.reps), quality)
        repo.saveCard(c.copy(intervalDays = n.intervalDays, ease = n.ease, reps = n.reps, dueEpoch = java.time.LocalDate.now().toEpochDay() + n.intervalDays))
    }
    fun saveRef(r: Reference) = launchSafe("تعذّر الحفظ") { repo.saveRef(r) }
    fun deleteRef(id: Long) = launchSafe("تعذّر الحذف") { repo.deleteRef(id) }
    fun saveRoom(r: RoomInfo) = launchSafe("تعذّر الحفظ") { repo.saveRoom(r) }
    fun deleteRoom(name: String) = launchSafe("تعذّر الحذف") { repo.deleteRoom(name) }
    fun linkTaskAttachment(t: Task, attachmentId: Long) = saveTask(t.copy(attachmentId = attachmentId))
    fun addTemplate(t: ScheduleTemplate) = launchSafe("تعذّر إضافة القالب") {
        val r = parseScheduleCsv(t.csv)
        val n = repo.addSlots(r.slots.map { Slot(title = it.title, instructor = it.instructor, room = it.room, day = it.day, startMin = it.startMin, endMin = it.endMin, weekMode = it.weekMode) })
        message.value = "تمت إضافة $n محاضرة من القالب"
    }
    fun addGeneratedFile(slotId: Long, file: File, name: String, mime: String, kind: Int) = launchSafe("تعذّر الحفظ") {
        repo.addAttachmentFile(slotId, file, name, mime, kind); message.value = "تم الحفظ في مرفقات المادة"
    }

    // ---- المؤقت (بومودورو) مرتبط بمادة
    data class Pomo(val running: Boolean = false, val breakPhase: Boolean = false, val slotId: Long = 0, val endAt: Long = 0, val totalMin: Int = 25, val lastSavedMin: Int = 0)
    val pomo = MutableStateFlow(Pomo())
    fun pomoStart(slotId: Long, work: Boolean) {
        val m = if (work) settings.value.pomoWork else settings.value.pomoBreak
        val end = System.currentTimeMillis() + m * 60_000L
        pomo.value = Pomo(true, !work, slotId, end, m)
        Scheduler.schedulePomo(app, end, !work)
    }
    /** يوقف المؤقت؛ في وضع المذاكرة يسجل الدقائق المنقضية. */
    fun pomoStop() {
        val p = pomo.value
        Scheduler.cancelPomo(app)
        if (p.running && !p.breakPhase) {
            val elapsed = ((System.currentTimeMillis() - (p.endAt - p.totalMin * 60_000L)) / 60_000L).toInt().coerceIn(0, p.totalMin)
            if (elapsed >= 1) launchSafe("تعذّر الحفظ") { repo.addSession(p.slotId, elapsed); message.value = "سُجّلت $elapsed دقيقة مذاكرة" }
        }
        pomo.value = Pomo()
    }
    /** يُستدعى كل ثانية من الشاشة؛ ينهي الجلسة عند الوقت. */
    fun pomoTick() {
        val p = pomo.value
        if (p.running && System.currentTimeMillis() >= p.endAt) {
            if (!p.breakPhase) launchSafe("تعذّر الحفظ") { repo.addSession(p.slotId, p.totalMin) }
            pomo.value = Pomo(running = false, breakPhase = p.breakPhase, slotId = p.slotId, totalMin = p.totalMin, lastSavedMin = if (p.breakPhase) 0 else p.totalMin)
        }
    }

    // ---- تصدير التقويم (ICS)
    suspend fun icsText(): String = withContext(Dispatchers.IO) {
        val st = settings.value
        val sl = repo.allSlotsOnce().filter { it.scheduleId == st.activeSchedule }
        val excs = exceptions.value
        buildIcs(
            sl.map { s -> IcsSlot(s.id, s.title, s.instructor, s.room, s.day, s.startMin, s.endMin, s.weekMode, s.dateSet(),
                excs.filter { it.slotId == s.id }.map { ExcInfo(it.dateEpoch, it.cancelled, it.newStart, it.newEnd, it.newRoom) }) },
            exams.value.map { IcsExam(it.title, it.dateEpoch) },
            tasks.value.filter { it.dueEpoch != null }.map { IcsTask(it.id, it.title, it.dueEpoch!!, it.done) },
            java.time.LocalDate.ofEpochDay(st.semesterStart), holidays.value.map { HolidayRange(it.startEpoch, it.endEpoch, it.title) },
        )
    }

    // ---- استيراد أحداث تقويم الجهاز (يشمل حساب جوجل المزامن) كواجبات
    fun importDeviceCalendar(days: Int = 60) = launchSafe("تعذّر قراءة التقويم") {
        val added = withContext(Dispatchers.IO) {
            val begin = System.currentTimeMillis(); val end = begin + days * 86_400_000L
            val uri = android.provider.CalendarContract.Instances.CONTENT_URI.buildUpon().also { android.content.ContentUris.appendId(it, begin); android.content.ContentUris.appendId(it, end) }.build()
            val existing = tasks.value.map { it.title to it.dueEpoch }.toSet()
            var n = 0
            app.contentResolver.query(uri, arrayOf(android.provider.CalendarContract.Instances.TITLE, android.provider.CalendarContract.Instances.BEGIN, android.provider.CalendarContract.Instances.ALL_DAY), null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0)?.trim().orEmpty(); if (title.isEmpty()) continue
                    val allDay = c.getInt(2) == 1
                    val zone = if (allDay) java.time.ZoneOffset.UTC else java.time.ZoneId.systemDefault()
                    val date = java.time.Instant.ofEpochMilli(c.getLong(1)).atZone(zone).toLocalDate().toEpochDay()
                    if ((title to date) in existing) continue
                    repo.saveTask(Task(slotId = 0, title = title, dueEpoch = date)); n++
                }
            }
            n
        }
        message.value = if (added > 0) "تم استيراد $added حدث كواجبات" else "لا توجد أحداث جديدة"
    }

    // ---- خطة المذاكرة والفراغ
    private fun busyOn(date: java.time.LocalDate): List<Pair<Int, Int>> =
        com.schedule.app.ui.itemsOn(date, slots.value, exceptions.value, holidays.value, java.time.LocalDate.ofEpochDay(settings.value.semesterStart))
            .filter { !it.occ.cancelled }.map { it.occ.start to it.occ.end }
    fun studyPlan(hoursPerExam: Int): List<PlanItem> {
        val today = java.time.LocalDate.now()
        return studyPlan(today, exams.value.map { PlanExam(it.title, it.dateEpoch, hoursPerExam) }, ::busyOn)
    }
    fun busyByDayIndex(): Map<Int, List<Pair<Int, Int>>> = slots.value.groupBy { it.day }.mapValues { e -> e.value.map { it.startMin to it.endMin } }
    fun mySpans(): List<DaySpan> = slots.value.map { DaySpan(it.day, it.startMin, it.endMin) }

    // ---- المساعد (قواعد، بدون نموذج لغوي)
    suspend fun assistant(text: String): String {
        val today = java.time.LocalDate.now()
        val sl = slots.value
        val titles = sl.map { it.title }.distinct()
        fun d(e: Long) = formatDateShort(e)
        return when (val c = parseCommand(text, today)) {
            is Cmd.AddLecture -> {
                val s = Slot(title = c.title, day = c.day, startMin = c.start, endMin = c.end, room = c.room)
                val clash = sl.filter { conflicts(s.day, s.weekMode, s.startMin, s.endMin, it.day, it.weekMode, it.startMin, it.endMin) }
                repo.saveSlot(s)
                "تمت إضافة ${c.title} يوم ${DAY_NAMES[c.day]} ${formatRange(c.start, c.end)}" + (if (c.room.isNotBlank()) " - ${c.room}" else "") +
                    if (clash.isNotEmpty()) "\nتنبيه: تتعارض مع ${clash.joinToString(" و ") { it.title }}." else ""
            }
            is Cmd.AddTask -> {
                val subj = c.subject?.let { matchSubject(it, titles) }
                val slot = sl.firstOrNull { it.title == subj }
                if (c.subject != null && slot == null) "لم أجد مادة باسم \"${c.subject}\". الواجب لم يُضف."
                else { repo.saveTask(Task(slotId = slot?.id ?: 0, title = c.title, dueEpoch = c.due)); "تمت إضافة الواجب \"${c.title}\"" + (subj?.let { " لمادة $it" } ?: "") + (c.due?.let { " - التسليم ${d(it)}" } ?: "") }
            }
            is Cmd.AddNote -> {
                val subj = c.subject?.let { matchSubject(it, titles) }
                val slot = sl.firstOrNull { it.title == subj }
                if (c.subject != null && slot == null) "لم أجد مادة باسم \"${c.subject}\". الملاحظة لم تُضف."
                else { repo.saveNote(Note(slotId = slot?.id ?: 0, day = if (slot == null) dayIndex(today) else -1, text = c.text)); "تمت إضافة الملاحظة" + (subj?.let { " لمادة $it" } ?: " ليوم اليوم") }
            }
            is Cmd.AddExam -> { repo.saveExam(Exam(title = c.title, dateEpoch = c.date)); "تمت إضافة الامتحان \"${c.title}\" بتاريخ ${d(c.date)}" }
            is Cmd.Cancel -> {
                val subj = matchSubject(c.subject, titles)
                val day = dayIndex(java.time.LocalDate.ofEpochDay(c.date))
                val slot = sl.firstOrNull { it.title == subj && it.day == day }
                when {
                    subj == null -> "لم أجد مادة باسم \"${c.subject}\"."
                    slot == null -> "$subj ليس لها محاضرة يوم ${DAY_NAMES[day]} (${d(c.date)})."
                    else -> { repo.saveException(SlotException(slotId = slot.id, dateEpoch = c.date, cancelled = true)); "أُلغيت محاضرة $subj بتاريخ ${d(c.date)} (لمرة واحدة)" }
                }
            }
            is Cmd.Agenda -> {
                val date = java.time.LocalDate.ofEpochDay(c.date)
                val items = com.schedule.app.ui.itemsOn(date, sl, exceptions.value, holidays.value, java.time.LocalDate.ofEpochDay(settings.value.semesterStart))
                val ex = exams.value.filter { it.dateEpoch == c.date }
                val ts = tasks.value.filter { !it.done && it.dueEpoch == c.date }
                if (items.isEmpty() && ex.isEmpty() && ts.isEmpty()) "لا يوجد شيء يوم ${DAY_NAMES[dayIndex(date)]} (${d(c.date)})."
                else buildString {
                    append("يوم ${DAY_NAMES[dayIndex(date)]} ${d(c.date)}:")
                    items.forEach { append("\n- ${formatMinutes(it.occ.start)} ${it.slot.title}" + (it.occ.room ?: it.slot.room).takeIf { r -> r.isNotBlank() }?.let { r -> " ($r)" }.orEmpty() + if (it.occ.cancelled) " [ملغاة]" else "") }
                    ex.forEach { append("\n- امتحان: ${it.title}") }
                    ts.forEach { append("\n- تسليم: ${it.title}") }
                }
            }
            Cmd.Next -> {
                val now = java.time.LocalDateTime.now(); var found: String? = null
                for (off in 0L..14L) {
                    val date = today.plusDays(off)
                    val it = com.schedule.app.ui.itemsOn(date, sl, exceptions.value, holidays.value, java.time.LocalDate.ofEpochDay(settings.value.semesterStart))
                        .firstOrNull { x -> !x.occ.cancelled && date.atTime(x.occ.start / 60, x.occ.start % 60).isAfter(now) }
                    if (it != null) { found = "المحاضرة القادمة: ${it.slot.title} يوم ${DAY_NAMES[dayIndex(date)]} ${formatMinutes(it.occ.start)}" + (it.occ.room ?: it.slot.room).takeIf { r -> r.isNotBlank() }?.let { r -> " - $r" }.orEmpty(); break }
                }
                found ?: "لا توجد محاضرات قادمة خلال أسبوعين."
            }
            is Cmd.Search -> {
                val docs = buildList<Pair<Long, String>> {
                    sl.forEach { add(1_000_000 + it.id to "${it.title} ${it.instructor} ${it.room}") }
                    notes.value.forEach { add(2_000_000 + it.id to it.text) }
                    tasks.value.forEach { add(3_000_000 + it.id to it.title) }
                    attachments.value.forEach { add(4_000_000 + it.id to it.name) }
                    cards.value.forEach { add(5_000_000 + it.id to "${it.front} ${it.back}") }
                }
                val r = tfidfRank(c.query, docs, 6)
                if (r.isEmpty()) "لم أجد نتائج قريبة من \"${c.query}\"." else buildString {
                    append("أقرب النتائج (بحث لفظي تقريبي):")
                    r.forEach { (id, _) ->
                        val kind = id / 1_000_000; val rid = id % 1_000_000
                        append("\n- " + when (kind) {
                            1L -> "مادة: " + (sl.firstOrNull { it.id == rid }?.title ?: "")
                            2L -> "ملاحظة: " + (notes.value.firstOrNull { it.id == rid }?.text?.take(70) ?: "")
                            3L -> "واجب: " + (tasks.value.firstOrNull { it.id == rid }?.title ?: "")
                            4L -> "ملف: " + (attachments.value.firstOrNull { it.id == rid }?.name ?: "")
                            else -> "بطاقة: " + (cards.value.firstOrNull { it.id == rid }?.front ?: "")
                        })
                    }
                }
            }
            is Cmd.Summarize -> {
                val subj = c.subject?.let { matchSubject(it, titles) }
                val ids = sl.filter { subj == null || it.title == subj }.map { it.id }.toSet()
                val text = notes.value.filter { it.slotId in ids }.joinToString(". ") { it.text.replace(Regex("[#*]|- \\[.\\] |^- "), "") }
                if (text.isBlank()) "لا توجد ملاحظات" + (subj?.let { " لمادة $it" } ?: "") + " لتلخيصها."
                else "ملخص" + (subj?.let { " $it" } ?: "") + " (تلخيص استخلاصي من جملك الأهم، وليس إعادة صياغة):\n" + summarize(text, 4).joinToString("\n") { "- $it" }
            }
            is Cmd.Quiz -> {
                val subj = c.subject?.let { matchSubject(it, titles) }
                val ids = sl.filter { subj == null || it.title == subj }.map { it.id }.toSet()
                val fromCards = cards.value.filter { it.slotId in ids }.map { it.front to it.back }
                val fromNotes = makeQuestions(notes.value.filter { it.slotId in ids }.joinToString(". ") { it.text }, 5)
                val qs = (fromCards + fromNotes).shuffled().take(6)
                if (qs.isEmpty()) "لا توجد ملاحظات أو بطاقات كافية لتوليد أسئلة." else
                    "أسئلة مراجعة" + (subj?.let { " - $it" } ?: "") + ":\n" + qs.mapIndexed { i, q -> "${i + 1}. ${q.first}" }.joinToString("\n") + "\n\nالإجابات:\n" + qs.mapIndexed { i, q -> "${i + 1}. ${q.second}" }.joinToString("\n")
            }
            is Cmd.Unknown -> "لم أفهم الأمر. أمثلة:\n- ضيف محاضرة فيزياء يوم الأحد من 10 الى 12 قاعة 5\n- ضيف واجب حل التمارين لمادة الفيزياء بكرة\n- ضيف امتحان الرياضيات 15/12\n- الغي محاضرة الفيزياء بكرة\n- ايه محاضراتي بكرة\n- المحاضرة الجاية\n- ابحث عن الطاقة\n- لخص ملاحظات الكيمياء\n- اسئلة مراجعة الفيزياء"
        }
    }
    fun consumeMessage() { message.value = null }
}
