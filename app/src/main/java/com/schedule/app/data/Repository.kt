package com.schedule.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.schedule.app.work.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class Repository(private val ctx: Context) {
    private val dao = AppDatabase.get(ctx).dao()
    val prefs = Prefs(ctx)
    val slots = dao.observeSlots()
    val notes = dao.observeNotes()
    val attachments = dao.observeAttachments()
    val exams = dao.observeExams()
    val exceptions = dao.observeExceptions()
    val attendance = dao.observeAttendance()
    val tasks = dao.observeTasks()
    val holidays = dao.observeHolidays()
    private val dir get() = File(ctx.filesDir, "attachments").also { it.mkdirs() }

    // ---- المحاضرات
    suspend fun saveSlot(s: Slot): Long {
        val id = if (s.id == 0L) dao.insertSlot(s) else { dao.updateSlot(s); s.id }
        Scheduler.scheduleSlot(ctx, s.copy(id = id))
        Scheduler.refreshUi(ctx)
        return id
    }

    /** يضيف محاضرات مستوردة من CSV بدون حذف الموجود. */
    suspend fun addSlots(list: List<Slot>): Int { list.forEach { saveSlot(it) }; return list.size }

    suspend fun allSlotsOnce() = dao.allSlots()

    // ---- استثناءات لمرة واحدة
    suspend fun saveException(e: SlotException) {
        val old = dao.exceptionOn(e.slotId, e.dateEpoch)
        if (old == null) dao.insertException(e) else dao.updateException(e.copy(id = old.id))
        dao.slot(e.slotId)?.let { Scheduler.scheduleSlot(ctx, it) }
        Scheduler.refreshUi(ctx)
    }
    suspend fun deleteException(e: SlotException) {
        dao.deleteException(e.id)
        dao.slot(e.slotId)?.let { Scheduler.scheduleSlot(ctx, it) }
        Scheduler.refreshUi(ctx)
    }

    // ---- الحضور
    suspend fun setAttendance(slotId: Long, dateEpoch: Long, status: Int?) {
        val old = dao.attendanceOn(slotId, dateEpoch)
        when {
            status == null -> old?.let { dao.deleteAttendance(it.id) }
            old == null -> dao.insertAttendance(Attendance(slotId = slotId, dateEpoch = dateEpoch, status = status))
            else -> dao.updateAttendance(old.copy(status = status))
        }
    }

    // ---- الواجبات
    suspend fun saveTask(t: Task) {
        val id = if (t.id == 0L) dao.insertTask(t) else { dao.updateTask(t); t.id }
        Scheduler.scheduleTask(ctx, t.copy(id = id))
    }
    suspend fun deleteTask(id: Long) { Scheduler.cancelTask(ctx, id); dao.deleteTask(id) }

    // ---- الإجازات
    suspend fun saveHoliday(h: Holiday) {
        if (h.id == 0L) dao.insertHoliday(h) else dao.updateHoliday(h)
        Scheduler.rescheduleAll(ctx)
    }
    suspend fun deleteHoliday(id: Long) { dao.deleteHoliday(id); Scheduler.rescheduleAll(ctx) }

    suspend fun deleteSlot(id: Long) {
        Scheduler.cancelSlot(ctx, id)
        dao.allNotes().filter { it.slotId == id }.forEach { Scheduler.cancelNote(ctx, it.id) }
        dao.allAttachments().filter { it.slotId == id }.forEach { File(it.path).delete() }
        dao.allTasks().filter { it.slotId == id }.forEach { Scheduler.cancelTask(ctx, it.id) }
        dao.deleteNotesOfSlot(id); dao.deleteAttachmentsOfSlot(id); dao.deleteExceptionsOfSlot(id)
        dao.deleteAttendanceOfSlot(id); dao.deleteTasksOfSlot(id); dao.deleteSlot(id)
        Scheduler.refreshUi(ctx)
    }

    // ---- الملاحظات
    suspend fun saveNote(n: Note) {
        val id = if (n.id == 0L) dao.insertNote(n) else { dao.updateNote(n); n.id }
        Scheduler.scheduleNote(ctx, n.copy(id = id))
    }

    suspend fun deleteNote(id: Long) { Scheduler.cancelNote(ctx, id); dao.deleteNote(id) }

    // ---- المرفقات
    private fun displayName(uri: Uri): String =
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) c.getString(i) else null
        } ?: "ملف"

    suspend fun addAttachment(slotId: Long, uri: Uri): Long = withContext(Dispatchers.IO) {
        val name = displayName(uri)
        val dest = File(dir, "${UUID.randomUUID()}_${name.replace('/', '_')}")
        ctx.contentResolver.openInputStream(uri)!!.use { i -> dest.outputStream().use { o -> i.copyTo(o, 64 * 1024) } }
        val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
        dao.insertAttachment(Attachment(slotId = slotId, name = name, path = dest.absolutePath, mime = mime, size = dest.length()))
    }

    suspend fun deleteAttachment(a: Attachment) = withContext(Dispatchers.IO) { File(a.path).delete(); dao.deleteAttachment(a.id) }

    suspend fun copyAttachmentTo(a: Attachment, target: Uri) = withContext(Dispatchers.IO) {
        ctx.contentResolver.openOutputStream(target)!!.use { o -> File(a.path).inputStream().use { it.copyTo(o, 64 * 1024) } }
    }

    // ---- الامتحانات والملف الشخصي
    suspend fun saveExam(e: Exam) { if (e.id == 0L) dao.insertExam(e) else dao.updateExam(e) }
    suspend fun deleteExam(id: Long) = dao.deleteExam(id)

    suspend fun setPhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }!!
        val f = File(ctx.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        prefs.read().photoPath.takeIf { it.isNotEmpty() }?.let { File(it).delete() }
        f.absolutePath
    }

    // ---- نسخة احتياطية: data.json + attachments/ + photo.jpg
    suspend fun export(os: OutputStream) = withContext(Dispatchers.IO) {
        val s = prefs.read()
        val json = JSONObject().apply {
            put("version", 1)
            put("profileName", s.profileName)
            put("slots", JSONArray().apply { dao.allSlots().forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("instructor", it.instructor).put("room", it.room).put("day", it.day).put("start", it.startMin).put("end", it.endMin).put("color", it.color).put("weekMode", it.weekMode)) } })
            put("notes", JSONArray().apply { dao.allNotes().forEach { put(JSONObject().put("slotId", it.slotId).put("day", it.day).put("text", it.text).put("date", it.dateEpoch ?: JSONObject.NULL)) } })
            put("attachments", JSONArray().apply { dao.allAttachments().forEach { put(JSONObject().put("slotId", it.slotId).put("name", it.name).put("file", File(it.path).name).put("mime", it.mime).put("kind", it.kind)) } })
            put("exams", JSONArray().apply { dao.allExams().forEach { put(JSONObject().put("title", it.title).put("date", it.dateEpoch)) } })
            put("exceptions", JSONArray().apply { dao.allExceptions().forEach { put(JSONObject().put("slotId", it.slotId).put("date", it.dateEpoch).put("cancelled", it.cancelled)
                .put("newStart", it.newStart ?: JSONObject.NULL).put("newEnd", it.newEnd ?: JSONObject.NULL).put("newRoom", it.newRoom ?: JSONObject.NULL)) } })
            put("attendance", JSONArray().apply { dao.allAttendance().forEach { put(JSONObject().put("slotId", it.slotId).put("date", it.dateEpoch).put("status", it.status)) } })
            put("tasks", JSONArray().apply { dao.allTasks().forEach { put(JSONObject().put("slotId", it.slotId).put("title", it.title).put("due", it.dueEpoch ?: JSONObject.NULL).put("done", it.done)) } })
            put("holidays", JSONArray().apply { dao.allHolidays().forEach { put(JSONObject().put("title", it.title).put("start", it.startEpoch).put("end", it.endEpoch)) } })
            put("semesterStart", s.semesterStart); put("absenceLimit", s.absenceLimit)
        }
        ZipOutputStream(os.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("data.json")); zip.write(json.toString().toByteArray()); zip.closeEntry()
            dao.allAttachments().map { File(it.path) }.filter { it.exists() }.distinct().forEach { f ->
                zip.putNextEntry(ZipEntry("attachments/${f.name}")); f.inputStream().use { it.copyTo(zip, 64 * 1024) }; zip.closeEntry()
            }
            s.photoPath.takeIf { it.isNotEmpty() && File(it).exists() }?.let { p ->
                zip.putNextEntry(ZipEntry("photo.jpg")); File(p).inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
    }

    suspend fun import(ins: InputStream) = withContext(Dispatchers.IO) {
        var jsonText: String? = null
        val restored = mutableSetOf<String>()
        var photo: File? = null
        ZipInputStream(ins.buffered()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                when {
                    e.name == "data.json" -> jsonText = zip.readBytes().toString(Charsets.UTF_8)
                    e.name == "photo.jpg" -> { photo = File(ctx.filesDir, "profile_${System.currentTimeMillis()}.jpg"); photo!!.outputStream().use { zip.copyTo(it) } }
                    e.name.startsWith("attachments/") && !e.isDirectory -> {
                        val n = File(e.name).name // يمنع اجتياز المسارات
                        File(dir, n).outputStream().use { zip.copyTo(it, 64 * 1024) }; restored += n
                    }
                }
                e = zip.nextEntry
            }
        }
        val root = JSONObject(requireNotNull(jsonText) { "ملف النسخة الاحتياطية غير صالح" })
        // حذف الحالي
        dao.allSlots().forEach { Scheduler.cancelSlot(ctx, it.id) }
        dao.allNotes().forEach { Scheduler.cancelNote(ctx, it.id) }
        dao.allTasks().forEach { Scheduler.cancelTask(ctx, it.id) }
        dao.allAttachments().forEach { if (File(it.path).name !in restored) File(it.path).delete() }
        dao.clearAttachments(); dao.clearNotes(); dao.clearSlots(); dao.clearExams()
        dao.clearExceptions(); dao.clearAttendance(); dao.clearTasks(); dao.clearHolidays()

        val map = mutableMapOf<Long, Long>()
        root.getJSONArray("slots").let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
            map[o.getLong("id")] = dao.insertSlot(Slot(title = o.getString("title"), instructor = o.optString("instructor"), room = o.optString("room"),
                day = o.getInt("day"), startMin = o.getInt("start"), endMin = o.getInt("end"),
                color = o.optInt("color"), weekMode = o.optInt("weekMode"))) } }
        root.optJSONArray("notes")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
            val sid = o.optLong("slotId"); if (sid > 0 && sid !in map) continue
            dao.insertNote(Note(slotId = if (sid > 0) map.getValue(sid) else 0, day = o.optInt("day", -1), text = o.getString("text"),
                dateEpoch = if (o.isNull("date")) null else o.getLong("date"))) } }
        root.optJSONArray("attachments")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
            val sid = map[o.getLong("slotId")] ?: continue
            val f = File(dir, File(o.getString("file")).name); if (!f.exists()) continue
            dao.insertAttachment(Attachment(slotId = sid, name = o.getString("name"), path = f.absolutePath, mime = o.optString("mime", "application/octet-stream"), size = f.length(), kind = o.optInt("kind"))) } }
        root.optJSONArray("exams")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i); dao.insertExam(Exam(title = o.getString("title"), dateEpoch = o.getLong("date"))) } }
        root.optJSONArray("exceptions")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
            val sid = map[o.getLong("slotId")] ?: continue
            dao.insertException(SlotException(slotId = sid, dateEpoch = o.getLong("date"), cancelled = o.optBoolean("cancelled"),
                newStart = if (o.isNull("newStart")) null else o.getInt("newStart"), newEnd = if (o.isNull("newEnd")) null else o.getInt("newEnd"),
                newRoom = if (o.isNull("newRoom")) null else o.getString("newRoom"))) } }
        root.optJSONArray("attendance")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
            val sid = map[o.getLong("slotId")] ?: continue
            dao.insertAttendance(Attendance(slotId = sid, dateEpoch = o.getLong("date"), status = o.getInt("status"))) } }
        root.optJSONArray("tasks")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
            val sid = o.optLong("slotId"); if (sid > 0 && sid !in map) continue
            dao.insertTask(Task(slotId = if (sid > 0) map.getValue(sid) else 0, title = o.getString("title"), dueEpoch = if (o.isNull("due")) null else o.getLong("due"), done = o.optBoolean("done"))) } }
        root.optJSONArray("holidays")?.let { a -> for (i in 0 until a.length()) { val o = a.getJSONObject(i)
            dao.insertHoliday(Holiday(title = o.getString("title"), startEpoch = o.getLong("start"), endEpoch = o.getLong("end"))) } }
        var restoredSettings = prefs.read().copy(profileName = root.optString("profileName"), photoPath = photo?.absolutePath ?: "")
        if (root.has("semesterStart")) restoredSettings = restoredSettings.copy(semesterStart = root.getLong("semesterStart"), absenceLimit = root.optInt("absenceLimit", 4))
        prefs.write(restoredSettings)
        Scheduler.rescheduleAll(ctx)
    }
}
