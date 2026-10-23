package com.lecturemanager.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val code: String = "",
    val instructor: String = "",
    @ColumnInfo(name = "theme_color") val themeColor: Long = 0xFF1A2542,
    val icon: String = "📘",
    /** موعد الامتحان (بداية اليوم بالميلي ثانية، أو null). */
    @ColumnInfo(name = "exam_date") val examDate: Long? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)

/**
 * الأسبوع الدراسي يُنشأ تلقائياً: يكفي وجود محاضرة تحمل رقم الأسبوع،
 * ويتم تجميع المحاضرات حسب [weekNumber] عند العرض.
 */
@Entity(
    tableName = "lectures",
    foreignKeys = [ForeignKey(
        entity = Subject::class,
        parentColumns = ["id"],
        childColumns = ["subject_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("subject_id")],
)
data class Lecture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "week_number") val weekNumber: Int = 1,
    val title: String,
    val duration: Int = 0,
    @ColumnInfo(name = "pdf_path") val pdfPath: String? = null,
    @ColumnInfo(name = "file_size") val fileSize: Long = 0,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    val notes: String = "",
    val tag: String = "",
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    /** آخر صفحة (يبدأ العدّ من 0) وصل لها الطالب. */
    @ColumnInfo(name = "last_page") val lastPage: Int = 0,
    @ColumnInfo(name = "page_count") val pageCount: Int = 0,
    /** هل تمت فهرسة نص الملف للبحث. */
    val indexed: Boolean = false,
)

// الجداول التالية بلا مفاتيح خارجية عمداً؛ الحذف المتسلسل يتم يدوياً في Repository.

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lecture_id") val lectureId: Long,
    val page: Int,
    val label: String = "",
)

/** رسمة على صفحة PDF. الإحداثيات نسبية (0..1) من أبعاد الصفحة، والعرض نسبة من عرض الصفحة. */
@Entity(tableName = "annotations")
data class Annotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lecture_id") val lectureId: Long,
    val page: Int,
    /** 0 = قلم، 1 = تظليل */
    val tool: Int,
    val color: Long,
    val width: Float,
    /** "x,y;x,y;..." */
    val points: String,
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "lecture_id") val lectureId: Long = 0,
    val front: String,
    val back: String,
    /** صندوق لايتنر 0..4 */
    val box: Int = 0,
    /** اليوم (epoch day) الذي تستحق فيه المراجعة */
    @ColumnInfo(name = "due_day") val dueDay: Long = 0,
)

@Entity(tableName = "study_log")
data class StudyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "subject_id") val subjectId: Long,
    @ColumnInfo(name = "lecture_id") val lectureId: Long,
    val day: Long,
    val seconds: Long,
)

/** نص صفحة (مطبَّع) لأغراض البحث داخل الملفات. */
@Entity(tableName = "page_text")
data class PageText(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lecture_id") val lectureId: Long,
    val page: Int,
    val text: String,
)

data class SubjectTotal(val subjectId: Long, val total: Long)
data class DayTotal(val day: Long, val total: Long)
data class PageHit(val lectureId: Long, val page: Int, val text: String)
