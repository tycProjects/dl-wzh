package com.schedule.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "slots")
data class Slot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val instructor: String = "",
    val room: String = "",
    /** 0 = السبت ... 6 = الجمعة */
    val day: Int,
    @ColumnInfo(name = "start_min") val startMin: Int,
    @ColumnInfo(name = "end_min") val endMin: Int,
    /** فهرس اللون في لوحة الألوان */
    @ColumnInfo(defaultValue = "0") val color: Int = 0,
    /** 0 كل أسبوع، 1 فردي، 2 زوجي */
    @ColumnInfo(name = "week_mode", defaultValue = "0") val weekMode: Int = 0,
    /** 0 نظري، 1 عملي، 2 سكشن */
    @ColumnInfo(defaultValue = "0") val type: Int = 0,
    /** معرّف محاضرة مرتبطة (مثلا العملي المرتبط بالنظري)، 0 = لا شيء */
    @ColumnInfo(name = "linked_id", defaultValue = "0") val linkedId: Long = 0,
    /** ما يجب إحضاره (كتاب، حاسبة...) */
    @ColumnInfo(defaultValue = "''") val bring: String = "",
    /** نغمة إشعار خاصة بالمادة (Uri نصي)، فارغ = الافتراضية */
    @ColumnInfo(defaultValue = "''") val sound: String = "",
    /** تواريخ محددة (أيام عصر يونكس مفصولة بفاصلة) عندما weekMode = 5 */
    @ColumnInfo(defaultValue = "''") val dates: String = "",
    @ColumnInfo(name = "schedule_id", defaultValue = "1") val scheduleId: Long = 1,
) {
    fun dateSet(): Set<Long> = dates.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
}

val SLOT_TYPE_LABELS = listOf("نظري", "عملي", "سكشن")

/** ملاحظة لمحاضرة (slotId > 0) أو ليوم كامل (slotId = 0 و day = 0..6). dateEpoch: تاريخ الملاحظة لتذكير قبله بيوم. */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long = 0,
    val day: Int = -1,
    val text: String,
    @ColumnInfo(name = "date_epoch") val dateEpoch: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    /** وسوم مفصولة بفاصلة */
    @ColumnInfo(defaultValue = "''") val tags: String = "",
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
)

/** kind: 0 ملف مرفق، 1 تسجيل صوتي، 2 رسم، 3 صورة ممسوحة */
@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long,
    val name: String,
    val path: String,
    val mime: String,
    val size: Long,
    val kind: Int = 0,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis(),
)

/** تعديل لمرة واحدة على موعد في تاريخ محدد (إلغاء أو تغيير الوقت/القاعة). */
@Entity(tableName = "exceptions")
data class SlotException(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long,
    @ColumnInfo(name = "date_epoch") val dateEpoch: Long,
    val cancelled: Boolean = false,
    @ColumnInfo(name = "new_start") val newStart: Int? = null,
    @ColumnInfo(name = "new_end") val newEnd: Int? = null,
    @ColumnInfo(name = "new_room") val newRoom: String? = null,
)

/** status: 0 حاضر، 1 غائب، 2 غياب بعذر */
@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long,
    @ColumnInfo(name = "date_epoch") val dateEpoch: Long,
    val status: Int,
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long,
    val title: String,
    @ColumnInfo(name = "due_epoch") val dueEpoch: Long? = null,
    val done: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    /** مرفق مرتبط بالواجب (0 = لا شيء) */
    @ColumnInfo(name = "attachment_id", defaultValue = "0") val attachmentId: Long = 0,
)

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "start_epoch") val startEpoch: Long,
    @ColumnInfo(name = "end_epoch") val endEpoch: Long,
)

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "date_epoch") val dateEpoch: Long,
)

/** جدول/فصل دراسي. المعرّف 1 هو الافتراضي. */
@Entity(tableName = "schedules")
data class ScheduleInfo(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

/** درجة في مادة: score من max، وزنها weight (نسبة من 100 من درجة المادة). isFinal للامتحان النهائي. */
@Entity(tableName = "grades")
data class Grade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long,
    val name: String,
    val score: Double,
    val max: Double,
    val weight: Double,
    @ColumnInfo(name = "is_final", defaultValue = "0") val isFinal: Boolean = false,
)

/** عدد الساعات المعتمدة للمادة (لحساب المعدل). */
@Entity(tableName = "credits")
data class Credit(@PrimaryKey @ColumnInfo(name = "slot_id") val slotId: Long, val hours: Int)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    val minutes: Int,
)

/** بطاقة مراجعة بنظام التكرار المتباعد (SM-2). */
@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long,
    val front: String,
    val back: String,
    @ColumnInfo(name = "due_epoch") val dueEpoch: Long = 0,
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 0,
    val ease: Double = 2.5,
    val reps: Int = 0,
)

@Entity(tableName = "refs")
data class Reference(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long,
    val title: String,
    val author: String = "",
    val note: String = "",
)

/** معلومات قاعة: وصف الوصول وإحداثيات اختيارية. */
@Entity(tableName = "rooms")
data class RoomInfo(
    @PrimaryKey val name: String,
    val description: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)

@Dao
interface ExtraDao {
    @Query("SELECT * FROM schedules ORDER BY id") fun observeSchedules(): Flow<List<ScheduleInfo>>
    @Query("SELECT * FROM schedules ORDER BY id") suspend fun allSchedules(): List<ScheduleInfo>
    @Insert suspend fun insertSchedule(s: ScheduleInfo): Long
    @Update suspend fun updateSchedule(s: ScheduleInfo)
    @Query("DELETE FROM schedules WHERE id = :id") suspend fun deleteSchedule(id: Long)

    @Query("SELECT * FROM grades") fun observeGrades(): Flow<List<Grade>>
    @Query("SELECT * FROM grades") suspend fun allGrades(): List<Grade>
    @Insert suspend fun insertGrade(g: Grade): Long
    @Update suspend fun updateGrade(g: Grade)
    @Query("DELETE FROM grades WHERE id = :id") suspend fun deleteGrade(id: Long)
    @Query("DELETE FROM grades WHERE slot_id = :slotId") suspend fun deleteGradesOfSlot(slotId: Long)
    @Query("DELETE FROM grades") suspend fun clearGrades()

    @Query("SELECT * FROM credits") fun observeCredits(): Flow<List<Credit>>
    @Query("SELECT * FROM credits") suspend fun allCredits(): List<Credit>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCredit(c: Credit)
    @Query("DELETE FROM credits WHERE slot_id = :slotId") suspend fun deleteCreditOfSlot(slotId: Long)
    @Query("DELETE FROM credits") suspend fun clearCredits()

    @Query("SELECT * FROM study_sessions ORDER BY started_at DESC") fun observeSessions(): Flow<List<StudySession>>
    @Query("SELECT * FROM study_sessions") suspend fun allSessions(): List<StudySession>
    @Insert suspend fun insertSession(s: StudySession): Long
    @Query("DELETE FROM study_sessions WHERE slot_id = :slotId") suspend fun deleteSessionsOfSlot(slotId: Long)
    @Query("DELETE FROM study_sessions") suspend fun clearSessions()

    @Query("SELECT * FROM cards ORDER BY id") fun observeCards(): Flow<List<Card>>
    @Query("SELECT * FROM cards") suspend fun allCards(): List<Card>
    @Insert suspend fun insertCard(c: Card): Long
    @Update suspend fun updateCard(c: Card)
    @Query("DELETE FROM cards WHERE id = :id") suspend fun deleteCard(id: Long)
    @Query("DELETE FROM cards WHERE slot_id = :slotId") suspend fun deleteCardsOfSlot(slotId: Long)
    @Query("DELETE FROM cards") suspend fun clearCards()

    @Query("SELECT * FROM refs ORDER BY id") fun observeRefs(): Flow<List<Reference>>
    @Query("SELECT * FROM refs") suspend fun allRefs(): List<Reference>
    @Insert suspend fun insertRef(r: Reference): Long
    @Update suspend fun updateRef(r: Reference)
    @Query("DELETE FROM refs WHERE id = :id") suspend fun deleteRef(id: Long)
    @Query("DELETE FROM refs WHERE slot_id = :slotId") suspend fun deleteRefsOfSlot(slotId: Long)
    @Query("DELETE FROM refs") suspend fun clearRefs()

    @Query("SELECT * FROM rooms") fun observeRooms(): Flow<List<RoomInfo>>
    @Query("SELECT * FROM rooms") suspend fun allRooms(): List<RoomInfo>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRoom(r: RoomInfo)
    @Query("DELETE FROM rooms WHERE name = :name") suspend fun deleteRoom(name: String)
    @Query("DELETE FROM rooms") suspend fun clearRooms()
}

@Dao
interface AppDao {
    @Query("SELECT * FROM slots WHERE schedule_id = :sid ORDER BY day, start_min") suspend fun slotsOf(sid: Long): List<Slot>
    @Query("UPDATE slots SET schedule_id = :sid WHERE id = :id") suspend fun moveSlot(id: Long, sid: Long)
    @Query("DELETE FROM slots WHERE schedule_id = :sid") suspend fun deleteSlotsOfSchedule(sid: Long)
    @Query("SELECT id FROM slots WHERE schedule_id = :sid") suspend fun slotIdsOfSchedule(sid: Long): List<Long>

    @Query("SELECT * FROM slots ORDER BY day, start_min") fun observeSlots(): Flow<List<Slot>>
    @Query("SELECT * FROM slots ORDER BY day, start_min") suspend fun allSlots(): List<Slot>
    @Query("SELECT * FROM slots WHERE id = :id") suspend fun slot(id: Long): Slot?
    @Insert suspend fun insertSlot(s: Slot): Long
    @Update suspend fun updateSlot(s: Slot)
    @Query("DELETE FROM slots WHERE id = :id") suspend fun deleteSlot(id: Long)

    @Query("SELECT * FROM notes ORDER BY created_at DESC") fun observeNotes(): Flow<List<Note>>
    @Query("SELECT * FROM notes") suspend fun allNotes(): List<Note>
    @Query("SELECT * FROM notes WHERE id = :id") suspend fun note(id: Long): Note?
    @Insert suspend fun insertNote(n: Note): Long
    @Update suspend fun updateNote(n: Note)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: Long)
    @Query("DELETE FROM notes WHERE slot_id = :slotId") suspend fun deleteNotesOfSlot(slotId: Long)

    @Query("SELECT * FROM attachments ORDER BY added_at DESC") fun observeAttachments(): Flow<List<Attachment>>
    @Query("SELECT * FROM attachments") suspend fun allAttachments(): List<Attachment>
    @Query("SELECT * FROM attachments WHERE id = :id") suspend fun attachment(id: Long): Attachment?
    @Insert suspend fun insertAttachment(a: Attachment): Long
    @Query("DELETE FROM attachments WHERE id = :id") suspend fun deleteAttachment(id: Long)
    @Query("DELETE FROM attachments WHERE slot_id = :slotId") suspend fun deleteAttachmentsOfSlot(slotId: Long)

    @Query("SELECT * FROM exams ORDER BY date_epoch") fun observeExams(): Flow<List<Exam>>
    @Query("SELECT * FROM exams") suspend fun allExams(): List<Exam>
    @Insert suspend fun insertExam(e: Exam): Long
    @Update suspend fun updateExam(e: Exam)
    @Query("DELETE FROM exams WHERE id = :id") suspend fun deleteExam(id: Long)

    @Query("SELECT * FROM exceptions") fun observeExceptions(): Flow<List<SlotException>>
    @Query("SELECT * FROM exceptions") suspend fun allExceptions(): List<SlotException>
    @Query("SELECT * FROM exceptions WHERE slot_id = :slotId AND date_epoch = :date LIMIT 1") suspend fun exceptionOn(slotId: Long, date: Long): SlotException?
    @Insert suspend fun insertException(e: SlotException): Long
    @Update suspend fun updateException(e: SlotException)
    @Query("DELETE FROM exceptions WHERE id = :id") suspend fun deleteException(id: Long)
    @Query("DELETE FROM exceptions WHERE slot_id = :slotId") suspend fun deleteExceptionsOfSlot(slotId: Long)
    @Query("DELETE FROM exceptions") suspend fun clearExceptions()

    @Query("SELECT * FROM attendance") fun observeAttendance(): Flow<List<Attendance>>
    @Query("SELECT * FROM attendance") suspend fun allAttendance(): List<Attendance>
    @Query("SELECT * FROM attendance WHERE slot_id = :slotId AND date_epoch = :date LIMIT 1") suspend fun attendanceOn(slotId: Long, date: Long): Attendance?
    @Insert suspend fun insertAttendance(a: Attendance): Long
    @Update suspend fun updateAttendance(a: Attendance)
    @Query("DELETE FROM attendance WHERE id = :id") suspend fun deleteAttendance(id: Long)
    @Query("DELETE FROM attendance WHERE slot_id = :slotId") suspend fun deleteAttendanceOfSlot(slotId: Long)
    @Query("DELETE FROM attendance") suspend fun clearAttendance()

    @Query("SELECT * FROM tasks ORDER BY done, due_epoch IS NULL, due_epoch, created_at") fun observeTasks(): Flow<List<Task>>
    @Query("SELECT * FROM tasks") suspend fun allTasks(): List<Task>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun task(id: Long): Task?
    @Insert suspend fun insertTask(t: Task): Long
    @Update suspend fun updateTask(t: Task)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)
    @Query("DELETE FROM tasks WHERE slot_id = :slotId") suspend fun deleteTasksOfSlot(slotId: Long)
    @Query("DELETE FROM tasks") suspend fun clearTasks()

    @Query("SELECT * FROM holidays ORDER BY start_epoch") fun observeHolidays(): Flow<List<Holiday>>
    @Query("SELECT * FROM holidays") suspend fun allHolidays(): List<Holiday>
    @Insert suspend fun insertHoliday(h: Holiday): Long
    @Update suspend fun updateHoliday(h: Holiday)
    @Query("DELETE FROM holidays WHERE id = :id") suspend fun deleteHoliday(id: Long)
    @Query("DELETE FROM holidays") suspend fun clearHolidays()

    @Query("DELETE FROM slots") suspend fun clearSlots()
    @Query("DELETE FROM notes") suspend fun clearNotes()
    @Query("DELETE FROM attachments") suspend fun clearAttachments()
    @Query("DELETE FROM exams") suspend fun clearExams()
}

@Database(
    entities = [Slot::class, Note::class, Attachment::class, Exam::class, SlotException::class, Attendance::class, Task::class, Holiday::class,
        ScheduleInfo::class, Grade::class, Credit::class, StudySession::class, Card::class, Reference::class, RoomInfo::class],
    version = 3, exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
    abstract fun extra(): ExtraDao

    companion object {
        @Volatile private var inst: AppDatabase? = null
        fun get(ctx: android.content.Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "schedule.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("INSERT INTO schedules (id, name) VALUES (1, 'الجدول الأساسي')")
                    }
                }).build().also { inst = it }
        }
    }
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE slots ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE slots ADD COLUMN week_mode INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS exceptions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, slot_id INTEGER NOT NULL, date_epoch INTEGER NOT NULL, cancelled INTEGER NOT NULL, new_start INTEGER, new_end INTEGER, new_room TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS attendance (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, slot_id INTEGER NOT NULL, date_epoch INTEGER NOT NULL, status INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, slot_id INTEGER NOT NULL, title TEXT NOT NULL, due_epoch INTEGER, done INTEGER NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS holidays (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, start_epoch INTEGER NOT NULL, end_epoch INTEGER NOT NULL)")
    }
}

val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE slots ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE slots ADD COLUMN linked_id INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE slots ADD COLUMN bring TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE slots ADD COLUMN sound TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE slots ADD COLUMN dates TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE slots ADD COLUMN schedule_id INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN attachment_id INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS schedules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
        db.execSQL("INSERT INTO schedules (id, name) VALUES (1, 'الجدول الأساسي')")
        db.execSQL("CREATE TABLE IF NOT EXISTS grades (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, slot_id INTEGER NOT NULL, name TEXT NOT NULL, score REAL NOT NULL, max REAL NOT NULL, weight REAL NOT NULL, is_final INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS credits (slot_id INTEGER PRIMARY KEY NOT NULL, hours INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS study_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, slot_id INTEGER NOT NULL, started_at INTEGER NOT NULL, minutes INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, slot_id INTEGER NOT NULL, front TEXT NOT NULL, back TEXT NOT NULL, due_epoch INTEGER NOT NULL, interval_days INTEGER NOT NULL, ease REAL NOT NULL, reps INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS refs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, slot_id INTEGER NOT NULL, title TEXT NOT NULL, author TEXT NOT NULL, note TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS rooms (name TEXT PRIMARY KEY NOT NULL, description TEXT NOT NULL, lat REAL, lng REAL)")
    }
}
