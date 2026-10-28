package com.schedule.app.work

import android.content.Context
import com.schedule.app.data.AppDatabase
import com.schedule.app.util.DAY_NAMES
import com.schedule.app.util.dayIndex
import com.schedule.app.util.formatMinutes
import java.time.LocalDate
import java.time.LocalDateTime

/** نصوص الملخصات اليومية. تُرجع (عنوان، نص) أو null عند عدم وجود شيء يستحق التنبيه. */
object Summaries {
    private fun lines(items: List<DatedLecture>) = items.filter { !it.occ.cancelled }.map {
        buildString {
            append(formatMinutes(it.occ.start)).append(" ").append(it.slot.title)
            if (it.room.isNotBlank()) append(" (").append(it.room).append(")")
            if (it.slot.bring.isNotBlank()) append(" - أحضر: ").append(it.slot.bring)
        }
    }

    suspend fun morning(ctx: Context): Pair<String, String>? {
        val today = LocalDate.now()
        val dao = AppDatabase.get(ctx).dao()
        val lec = lines(lecturesOn(ctx, today))
        val tasks = dao.allTasks().filter { !it.done && it.dueEpoch != null && it.dueEpoch <= today.toEpochDay() + 1 && it.dueEpoch >= today.toEpochDay() }
        val exams = dao.allExams().filter { it.dateEpoch in today.toEpochDay()..today.toEpochDay() + 7 }
        if (lec.isEmpty() && tasks.isEmpty() && exams.isEmpty()) return null
        val text = buildString {
            if (lec.isEmpty()) append("لا توجد محاضرات اليوم.") else append(lec.joinToString("\n"))
            tasks.forEach { append("\nواجب: ").append(it.title).append(if (it.dueEpoch == today.toEpochDay()) " (اليوم)" else " (غدا)") }
            exams.forEach { append("\nامتحان: ").append(it.title).append(" بعد ").append(it.dateEpoch - today.toEpochDay()).append(" يوم") }
        }
        return "صباح الخير - ${DAY_NAMES[dayIndex(today)]}" to text
    }

    suspend fun evening(ctx: Context): Pair<String, String>? {
        val tomorrow = LocalDate.now().plusDays(1)
        val dao = AppDatabase.get(ctx).dao()
        val lec = lines(lecturesOn(ctx, tomorrow))
        val tasks = dao.allTasks().filter { !it.done && it.dueEpoch == tomorrow.toEpochDay() }
        if (lec.isEmpty() && tasks.isEmpty()) return null
        val text = buildString {
            if (lec.isEmpty()) append("لا توجد محاضرات غدا.") else append(lec.joinToString("\n"))
            tasks.forEach { append("\nتسليم غدا: ").append(it.title) }
        }
        return "جدول الغد - ${DAY_NAMES[dayIndex(tomorrow)]}" to text
    }

    /** محاضرات اليوم المنتهية التي لم يُسجَّل حضورها. */
    suspend fun forgotAttendance(ctx: Context): List<String> {
        val today = LocalDate.now(); val now = LocalDateTime.now()
        val att = AppDatabase.get(ctx).dao().allAttendance().filter { it.dateEpoch == today.toEpochDay() }.map { it.slotId }.toSet()
        return lecturesOn(ctx, today).filter { !it.occ.cancelled && it.end.isBefore(now) && it.slot.id !in att }.map { it.slot.title }
    }
}
