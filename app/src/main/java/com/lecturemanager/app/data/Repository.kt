package com.lecturemanager.app.data

import android.content.Context
import android.net.Uri
import com.lecturemanager.app.util.dueDayAfter
import com.lecturemanager.app.util.nextBox
import com.lecturemanager.app.util.normalizeArabic
import com.lecturemanager.app.util.parseFlashcards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class StorageInfo(val pdfBytes: Long, val cacheBytes: Long, val dbBytes: Long) {
    val total get() = pdfBytes + cacheBytes + dbBytes
}

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

class Repository(private val context: Context) {
    val dao = AppDatabase.get(context).dao()
    private val pdfDir get() = File(context.filesDir, "pdfs").also { it.mkdirs() }

    val subjects = dao.observeSubjects()
    val lectures = dao.observeLectures()
    val flashcards = dao.observeFlashcards()
    fun lecture(id: Long) = dao.observeLecture(id)
    fun bookmarks(lectureId: Long) = dao.observeBookmarks(lectureId)
    fun annotations(lectureId: Long) = dao.observeAnnotations(lectureId)
    fun subjectTotals(fromDay: Long) = dao.observeSubjectTotals(fromDay)
    fun dayTotals(fromDay: Long) = dao.observeDayTotals(fromDay)

    // ---------- المواد والمحاضرات ----------
    suspend fun addSubject(s: Subject): Long {
        val next = (dao.allSubjects().maxOfOrNull { it.sortOrder } ?: 0) + 1
        return dao.insertSubject(s.copy(sortOrder = next))
    }

    suspend fun addLecture(l: Lecture): Long {
        val next = (dao.allLectures().maxOfOrNull { it.sortOrder } ?: 0) + 1
        return dao.insertLecture(l.copy(sortOrder = next))
    }

    /** استقبال محاضرة من تطبيق الجدول: يبحث عن المادة بالاسم أو ينشئها، ثم يضيف المحاضرة بملاحظاتها وملف PDF إن وُجد. */
    suspend fun importFromSchedule(
        subjectTitle: String, instructor: String, week: Int, lectureTitle: String, notes: String, pdf: Uri?,
    ): Long {
        val key = com.lecturemanager.app.util.normalizeArabic(subjectTitle)
        val existing = dao.allSubjects().firstOrNull { com.lecturemanager.app.util.normalizeArabic(it.title) == key }
        val subjectId = existing?.id ?: addSubject(Subject(title = subjectTitle.trim(), instructor = instructor.trim()))
        val (path, size) = pdf?.let { importPdf(it) } ?: (null to 0L)
        return addLecture(Lecture(subjectId = subjectId, weekNumber = week.coerceAtLeast(1), title = lectureTitle.trim(),
            notes = notes, pdfPath = path, fileSize = size))
    }

    suspend fun updateSubject(s: Subject) = dao.updateSubject(s)
    suspend fun updateLecture(l: Lecture) = dao.updateLecture(l)
    suspend fun setCompleted(id: Long, done: Boolean) = dao.setCompleted(id, done)
    suspend fun setNotes(id: Long, notes: String, tag: String) = dao.setNotes(id, notes, tag)
    suspend fun getLecture(id: Long) = dao.getLecture(id)
    suspend fun setProgress(id: Long, page: Int, count: Int) = dao.setProgress(id, page, count)

    /** تحريك مادة لأعلى (-1) أو لأسفل (+1) في الترتيب. */
    suspend fun moveSubject(id: Long, delta: Int) {
        val list = dao.allSubjects().toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        list[i] = list[j].also { list[j] = list[i] }
        list.forEachIndexed { idx, s -> dao.setSubjectOrder(s.id, idx) }
    }

    /** تحريك محاضرة داخل نفس الأسبوع في نفس المادة. */
    suspend fun moveLecture(id: Long, delta: Int) {
        val me = dao.getLecture(id) ?: return
        val group = dao.allLectures()
            .filter { it.subjectId == me.subjectId && it.weekNumber == me.weekNumber }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .toMutableList()
        val i = group.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in group.indices) return
        group[i] = group[j].also { group[j] = group[i] }
        group.forEachIndexed { idx, l -> dao.setLectureOrder(l.id, idx) }
    }

    private suspend fun cleanupLecture(l: Lecture) {
        l.pdfPath?.let { File(it).delete() }
        dao.deleteBookmarksOf(l.id); dao.deleteAnnotationsOf(l.id)
        dao.deleteFlashcardsOfLecture(l.id); dao.deleteLogsOf(l.id); dao.deletePageTextOf(l.id)
    }

    suspend fun deleteLecture(l: Lecture) = withContext(Dispatchers.IO) {
        cleanupLecture(l); dao.deleteLecture(l)
    }

    suspend fun deleteSubject(s: Subject) = withContext(Dispatchers.IO) {
        dao.allLectures().filter { it.subjectId == s.id }.forEach { cleanupLecture(it) }
        dao.deleteFlashcardsOfSubject(s.id)
        dao.deleteSubject(s)
    }

    /** ينسخ الملف إلى مجلد التطبيق الآمن على خيط خلفي (بدون تجميد الواجهة) ويعيد المسار والحجم. */
    suspend fun importPdf(uri: Uri): Pair<String, Long> = withContext(Dispatchers.IO) {
        val dest = File(pdfDir, "${UUID.randomUUID()}.pdf")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            dest.outputStream().use { out -> input.copyTo(out, 64 * 1024) }
        }
        dest.absolutePath to dest.length()
    }

    suspend fun attachPdf(lectureId: Long, uri: Uri) {
        val old = dao.getLecture(lectureId)?.pdfPath
        val (path, size) = importPdf(uri)
        dao.setPdf(lectureId, path, size)
        // أرقام الصفحات القديمة لا تنطبق على الملف الجديد
        dao.deleteBookmarksOf(lectureId); dao.deleteAnnotationsOf(lectureId); dao.deletePageTextOf(lectureId)
        withContext(Dispatchers.IO) { old?.let { File(it).delete() } }
    }

    // ---------- الإشارات والرسم ----------
    suspend fun addBookmark(lectureId: Long, page: Int, label: String = "") = dao.insertBookmark(Bookmark(lectureId = lectureId, page = page, label = label))
    suspend fun deleteBookmark(id: Long) = dao.deleteBookmark(id)
    suspend fun addAnnotation(a: Annotation) = dao.insertAnnotation(a)
    suspend fun deleteAnnotation(id: Long) = dao.deleteAnnotation(id)

    // ---------- بطاقات المراجعة ----------
    /** يولّد بطاقات من ملاحظات المحاضرة ويتجنب التكرار. يعيد عدد البطاقات الجديدة. */
    suspend fun generateCards(lecture: Lecture, notes: String): Int {
        var added = 0
        for ((front, back) in parseFlashcards(notes)) {
            if (dao.countCard(lecture.id, front) == 0) {
                dao.insertFlashcard(Flashcard(subjectId = lecture.subjectId, lectureId = lecture.id, front = front, back = back, dueDay = todayEpochDay()))
                added++
            }
        }
        return added
    }

    suspend fun addCard(subjectId: Long, front: String, back: String) =
        dao.insertFlashcard(Flashcard(subjectId = subjectId, front = front, back = back, dueDay = todayEpochDay()))

    suspend fun reviewCard(card: Flashcard, correct: Boolean) {
        val box = nextBox(card.box, correct)
        val today = todayEpochDay()
        dao.updateFlashcard(card.copy(box = box, dueDay = if (correct) dueDayAfter(box, today) else today))
    }

    suspend fun deleteCard(id: Long) = dao.deleteFlashcard(id)

    // ---------- المذاكرة ----------
    suspend fun logStudy(subjectId: Long, lectureId: Long, seconds: Long) {
        if (seconds <= 0) return
        dao.insertStudyLog(StudyLog(subjectId = subjectId, lectureId = lectureId, day = todayEpochDay(), seconds = seconds))
    }

    // ---------- البحث داخل الملفات ----------
    suspend fun searchPages(q: String): List<PageHit> {
        val n = normalizeArabic(q)
        if (n.length < 2) return emptyList()
        return dao.searchPages(n.replace("%", "").replace("_", ""))
    }

    suspend fun searchPagesIn(lectureId: Long, q: String): List<PageHit> {
        val n = normalizeArabic(q)
        if (n.length < 2) return emptyList()
        return dao.searchPagesIn(lectureId, n.replace("%", "").replace("_", ""))
    }

    // ---------- التخزين ----------
    suspend fun storageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        StorageInfo(
            pdfBytes = pdfDir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
            cacheBytes = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() },
            dbBytes = context.getDatabasePath("lectures.db").parentFile
                ?.listFiles { f -> f.name.startsWith("lectures.db") }?.sumOf { it.length() } ?: 0L,
        )
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    // ---------- النسخ الاحتياطي (ZIP: data.json + pdfs/) ----------
    suspend fun exportBackup(target: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(target)!!.use { writeBackup(it) }
    }

    suspend fun exportBackupToFile(file: File) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.outputStream().use { writeBackup(it) }
    }

    private suspend fun writeBackup(os: OutputStream) {
        val subjects = dao.allSubjects()
        val lectures = dao.allLectures()
        val json = JSONObject().apply {
            put("version", 2)
            put("subjects", JSONArray().apply {
                subjects.forEach { s ->
                    put(JSONObject().put("id", s.id).put("title", s.title).put("code", s.code)
                        .put("instructor", s.instructor).put("themeColor", s.themeColor).put("icon", s.icon)
                        .put("examDate", s.examDate ?: JSONObject.NULL).put("sortOrder", s.sortOrder))
                }
            })
            put("lectures", JSONArray().apply {
                lectures.forEach { l ->
                    put(JSONObject().put("id", l.id).put("subjectId", l.subjectId)
                        .put("weekNumber", l.weekNumber).put("title", l.title).put("duration", l.duration)
                        .put("pdf", l.pdfPath?.let { File(it).name } ?: JSONObject.NULL)
                        .put("completed", l.isCompleted).put("notes", l.notes).put("tag", l.tag)
                        .put("sortOrder", l.sortOrder).put("lastPage", l.lastPage).put("pageCount", l.pageCount))
                }
            })
            put("bookmarks", JSONArray().apply {
                lectures.forEach { l -> dao.observeBookmarksOnce(l.id).forEach { b ->
                    put(JSONObject().put("lectureId", b.lectureId).put("page", b.page).put("label", b.label)) } }
            })
            put("annotations", JSONArray().apply {
                lectures.forEach { l -> dao.annotationsOnce(l.id).forEach { a ->
                    put(JSONObject().put("lectureId", a.lectureId).put("page", a.page).put("tool", a.tool)
                        .put("color", a.color).put("width", a.width.toDouble()).put("points", a.points)) } }
            })
            put("flashcards", JSONArray().apply {
                dao.allFlashcards().forEach { f ->
                    put(JSONObject().put("subjectId", f.subjectId).put("lectureId", f.lectureId)
                        .put("front", f.front).put("back", f.back).put("box", f.box).put("dueDay", f.dueDay))
                }
            })
        }
        ZipOutputStream(os.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(json.toString().toByteArray())
            zip.closeEntry()
            lectures.mapNotNull { it.pdfPath }.distinct().map(::File).filter { it.exists() }.forEach { f ->
                zip.putNextEntry(ZipEntry("pdfs/${f.name}"))
                f.inputStream().use { it.copyTo(zip, 64 * 1024) }
                zip.closeEntry()
            }
        }
    }

    /** يستبدل كل البيانات الحالية بمحتوى النسخة الاحتياطية. */
    suspend fun importBackup(source: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(source)!!.use { readBackup(it) }
    }

    private suspend fun readBackup(ins: InputStream) {
        var jsonText: String? = null
        val restored = mutableSetOf<String>()
        ZipInputStream(ins.buffered()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                when {
                    e.name == "data.json" -> jsonText = zip.readBytes().toString(Charsets.UTF_8)
                    e.name.startsWith("pdfs/") && !e.isDirectory -> {
                        val name = File(e.name).name // يمنع اجتياز المسارات
                        File(pdfDir, name).outputStream().use { zip.copyTo(it, 64 * 1024) }
                        restored += name
                    }
                }
                e = zip.nextEntry
            }
        }
        val root = JSONObject(requireNotNull(jsonText) { "ملف النسخة الاحتياطية غير صالح" })
        // حذف الحالي
        dao.allLectures().forEach { l ->
            l.pdfPath?.let { p -> if (File(p).name !in restored) File(p).delete() }
            dao.deleteBookmarksOf(l.id); dao.deleteAnnotationsOf(l.id); dao.deletePageTextOf(l.id); dao.deleteLogsOf(l.id)
        }
        dao.allFlashcards().forEach { dao.deleteFlashcard(it.id) }
        dao.clearLectures(); dao.clearSubjects()

        val subjectMap = mutableMapOf<Long, Long>()
        val ss = root.getJSONArray("subjects")
        for (i in 0 until ss.length()) {
            val o = ss.getJSONObject(i)
            subjectMap[o.getLong("id")] = dao.insertSubject(
                Subject(title = o.getString("title"), code = o.optString("code"),
                    instructor = o.optString("instructor"), themeColor = o.getLong("themeColor"),
                    icon = o.optString("icon", "📘"),
                    examDate = if (o.isNull("examDate")) null else o.optLong("examDate"),
                    sortOrder = o.optInt("sortOrder", i))
            )
        }
        val lectureMap = mutableMapOf<Long, Long>()
        val ls = root.getJSONArray("lectures")
        for (i in 0 until ls.length()) {
            val o = ls.getJSONObject(i)
            val pdfName = if (o.isNull("pdf")) null else File(o.getString("pdf")).name
            val f = pdfName?.let { File(pdfDir, it) }?.takeIf { it.exists() }
            lectureMap[o.getLong("id")] = dao.insertLecture(
                Lecture(subjectId = subjectMap[o.getLong("subjectId")] ?: continue,
                    weekNumber = o.getInt("weekNumber"), title = o.getString("title"),
                    duration = o.optInt("duration"), pdfPath = f?.absolutePath, fileSize = f?.length() ?: 0,
                    isCompleted = o.optBoolean("completed"), notes = o.optString("notes"), tag = o.optString("tag"),
                    sortOrder = o.optInt("sortOrder", i), lastPage = o.optInt("lastPage"), pageCount = o.optInt("pageCount"))
            )
        }
        root.optJSONArray("bookmarks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val lid = lectureMap[o.getLong("lectureId")] ?: continue
                dao.insertBookmark(Bookmark(lectureId = lid, page = o.getInt("page"), label = o.optString("label")))
            }
        }
        root.optJSONArray("annotations")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val lid = lectureMap[o.getLong("lectureId")] ?: continue
                dao.insertAnnotation(Annotation(lectureId = lid, page = o.getInt("page"), tool = o.getInt("tool"),
                    color = o.getLong("color"), width = o.getDouble("width").toFloat(), points = o.getString("points")))
            }
        }
        root.optJSONArray("flashcards")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val sid = subjectMap[o.getLong("subjectId")] ?: continue
                dao.insertFlashcard(Flashcard(subjectId = sid, lectureId = lectureMap[o.optLong("lectureId")] ?: 0L,
                    front = o.getString("front"), back = o.getString("back"), box = o.optInt("box"), dueDay = o.optLong("dueDay")))
            }
        }
    }
}
