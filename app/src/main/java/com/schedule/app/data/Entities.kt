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
)

/** ملاحظة لمحاضرة (slotId > 0) أو ليوم كامل (slotId = 0 و day = 0..6). dateEpoch: تاريخ الملاحظة لتذكير قبله بيوم. */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "slot_id") val slotId: Long = 0,
    val day: Int = -1,
    val text: String,
    @ColumnInfo(name = "date_epoch") val dateEpoch: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

/** kind: 0 ملف مرفق، 1 تسجيل صوتي */
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

@Dao
interface AppDao {
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

@Database(entities = [Slot::class, Note::class, Attachment::class, Exam::class, SlotException::class, Attendance::class, Task::class, Holiday::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var inst: AppDatabase? = null
        fun get(ctx: android.content.Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "schedule.db")
                .addMigrations(MIGRATION_1_2).build().also { inst = it }
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
