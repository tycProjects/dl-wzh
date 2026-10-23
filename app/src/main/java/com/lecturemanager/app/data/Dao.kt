package com.lecturemanager.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // ---------- المواد والمحاضرات ----------
    @Query("SELECT * FROM subjects ORDER BY sort_order, id")
    fun observeSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM lectures ORDER BY week_number, sort_order, id")
    fun observeLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE id = :id")
    fun observeLecture(id: Long): Flow<Lecture?>

    @Query("SELECT * FROM lectures WHERE id = :id")
    suspend fun getLecture(id: Long): Lecture?

    @Query("SELECT * FROM subjects ORDER BY sort_order, id")
    suspend fun allSubjects(): List<Subject>

    @Query("SELECT * FROM lectures ORDER BY id")
    suspend fun allLectures(): List<Lecture>

    @Insert suspend fun insertSubject(s: Subject): Long
    @Insert suspend fun insertLecture(l: Lecture): Long
    @Update suspend fun updateSubject(s: Subject)
    @Update suspend fun updateLecture(l: Lecture)
    @Delete suspend fun deleteSubject(s: Subject)
    @Delete suspend fun deleteLecture(l: Lecture)

    @Query("UPDATE lectures SET is_completed = :done WHERE id = :id")
    suspend fun setCompleted(id: Long, done: Boolean)

    @Query("UPDATE lectures SET notes = :notes, tag = :tag WHERE id = :id")
    suspend fun setNotes(id: Long, notes: String, tag: String)

    @Query("UPDATE lectures SET pdf_path = :path, file_size = :size, indexed = 0, last_page = 0, page_count = 0 WHERE id = :id")
    suspend fun setPdf(id: Long, path: String?, size: Long)

    @Query("UPDATE lectures SET last_page = :page, page_count = :count WHERE id = :id")
    suspend fun setProgress(id: Long, page: Int, count: Int)

    @Query("UPDATE lectures SET indexed = :v WHERE id = :id")
    suspend fun setIndexed(id: Long, v: Boolean)

    @Query("UPDATE subjects SET sort_order = :o WHERE id = :id")
    suspend fun setSubjectOrder(id: Long, o: Int)

    @Query("UPDATE lectures SET sort_order = :o WHERE id = :id")
    suspend fun setLectureOrder(id: Long, o: Int)

    @Query("SELECT * FROM lectures WHERE pdf_path IS NOT NULL AND indexed = 0 ORDER BY id")
    suspend fun lecturesToIndex(): List<Lecture>

    @Query("DELETE FROM lectures") suspend fun clearLectures()
    @Query("DELETE FROM subjects") suspend fun clearSubjects()

    // ---------- الإشارات المرجعية ----------
    @Query("SELECT * FROM bookmarks WHERE lecture_id = :lectureId ORDER BY page")
    fun observeBookmarks(lectureId: Long): Flow<List<Bookmark>>
    @Query("SELECT * FROM bookmarks WHERE lecture_id = :lectureId ORDER BY page")
    suspend fun observeBookmarksOnce(lectureId: Long): List<Bookmark>
    @Insert suspend fun insertBookmark(b: Bookmark): Long
    @Query("DELETE FROM bookmarks WHERE id = :id") suspend fun deleteBookmark(id: Long)
    @Query("DELETE FROM bookmarks WHERE lecture_id = :lectureId") suspend fun deleteBookmarksOf(lectureId: Long)

    // ---------- الرسم على الـ PDF ----------
    @Query("SELECT * FROM annotations WHERE lecture_id = :lectureId ORDER BY id")
    fun observeAnnotations(lectureId: Long): Flow<List<Annotation>>
    @Query("SELECT * FROM annotations WHERE lecture_id = :lectureId ORDER BY id")
    suspend fun annotationsOnce(lectureId: Long): List<Annotation>
    @Insert suspend fun insertAnnotation(a: Annotation): Long
    @Query("DELETE FROM annotations WHERE id = :id") suspend fun deleteAnnotation(id: Long)
    @Query("DELETE FROM annotations WHERE lecture_id = :lectureId") suspend fun deleteAnnotationsOf(lectureId: Long)

    // ---------- بطاقات المراجعة ----------
    @Query("SELECT * FROM flashcards ORDER BY id")
    fun observeFlashcards(): Flow<List<Flashcard>>
    @Query("SELECT * FROM flashcards ORDER BY id") suspend fun allFlashcards(): List<Flashcard>
    @Insert suspend fun insertFlashcard(f: Flashcard): Long
    @Update suspend fun updateFlashcard(f: Flashcard)
    @Query("DELETE FROM flashcards WHERE id = :id") suspend fun deleteFlashcard(id: Long)
    @Query("DELETE FROM flashcards WHERE lecture_id = :lectureId") suspend fun deleteFlashcardsOfLecture(lectureId: Long)
    @Query("DELETE FROM flashcards WHERE subject_id = :subjectId") suspend fun deleteFlashcardsOfSubject(subjectId: Long)
    @Query("SELECT COUNT(*) FROM flashcards WHERE lecture_id = :lectureId AND front = :front")
    suspend fun countCard(lectureId: Long, front: String): Int
    @Query("SELECT COUNT(*) FROM flashcards WHERE due_day <= :day")
    suspend fun dueCount(day: Long): Int

    // ---------- سجل المذاكرة ----------
    @Insert suspend fun insertStudyLog(s: StudyLog): Long
    @Query("SELECT subject_id AS subjectId, SUM(seconds) AS total FROM study_log WHERE day >= :fromDay GROUP BY subject_id")
    fun observeSubjectTotals(fromDay: Long): Flow<List<SubjectTotal>>
    @Query("SELECT day AS day, SUM(seconds) AS total FROM study_log WHERE day >= :fromDay GROUP BY day ORDER BY day")
    fun observeDayTotals(fromDay: Long): Flow<List<DayTotal>>
    @Query("DELETE FROM study_log WHERE lecture_id = :lectureId") suspend fun deleteLogsOf(lectureId: Long)

    // ---------- فهرس نص الملفات ----------
    @Insert suspend fun insertPageText(p: PageText): Long
    @Query("SELECT page FROM page_text WHERE lecture_id = :lectureId")
    suspend fun indexedPages(lectureId: Long): List<Int>
    @Query("DELETE FROM page_text WHERE lecture_id = :lectureId") suspend fun deletePageTextOf(lectureId: Long)
    @Query("SELECT lecture_id AS lectureId, page AS page, text AS text FROM page_text WHERE text LIKE '%' || :q || '%' ORDER BY lecture_id, page LIMIT 100")
    suspend fun searchPages(q: String): List<PageHit>
    @Query("SELECT lecture_id AS lectureId, page AS page, text AS text FROM page_text WHERE lecture_id = :lectureId AND text LIKE '%' || :q || '%' ORDER BY page LIMIT 200")
    suspend fun searchPagesIn(lectureId: Long, q: String): List<PageHit>
    @Query("SELECT COUNT(DISTINCT lecture_id) FROM page_text") suspend fun indexedLectureCount(): Int
}

@Database(
    entities = [Subject::class, Lecture::class, Bookmark::class, Annotation::class,
        Flashcard::class, StudyLog::class, PageText::class],
    version = 2, exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN exam_date INTEGER")
                db.execSQL("ALTER TABLE subjects ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE lectures ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE lectures ADD COLUMN last_page INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE lectures ADD COLUMN page_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE lectures ADD COLUMN indexed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lecture_id INTEGER NOT NULL, page INTEGER NOT NULL, label TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS annotations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lecture_id INTEGER NOT NULL, page INTEGER NOT NULL, tool INTEGER NOT NULL, color INTEGER NOT NULL, width REAL NOT NULL, points TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcards (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subject_id INTEGER NOT NULL, lecture_id INTEGER NOT NULL, front TEXT NOT NULL, back TEXT NOT NULL, box INTEGER NOT NULL, due_day INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS study_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subject_id INTEGER NOT NULL, lecture_id INTEGER NOT NULL, day INTEGER NOT NULL, seconds INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS page_text (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lecture_id INTEGER NOT NULL, page INTEGER NOT NULL, text TEXT NOT NULL)")
                // ترتيب ابتدائي يحفظ الترتيب الحالي (حسب id)
                db.execSQL("UPDATE subjects SET sort_order = id")
                db.execSQL("UPDATE lectures SET sort_order = id")
            }
        }

        fun get(context: android.content.Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "lectures.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
