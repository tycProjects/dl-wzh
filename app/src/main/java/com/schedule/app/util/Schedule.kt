package com.schedule.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** استثناء لموعد واحد بتاريخ محدد: إلغاء، أو تغيير الوقت/القاعة لمرة واحدة. */
data class ExcInfo(val dateEpoch: Long, val cancelled: Boolean, val newStart: Int? = null, val newEnd: Int? = null, val newRoom: String? = null)

/** فترة إجازة شاملة للطرفين. */
data class HolidayRange(val start: Long, val end: Long, val title: String = "")

/** موعد فعلي لمحاضرة في تاريخ معين. */
data class Occurrence(
    val start: Int, val end: Int, val cancelled: Boolean,
    val holiday: String? = null, val changed: Boolean = false, val room: String? = null,
)

/** رقم أسبوع الفصل (1 = أسبوع بداية الفصل؛ قد يكون <= 0 قبله). */
fun weekNumber(date: LocalDate, semesterStart: LocalDate): Int =
    Math.floorDiv(ChronoUnit.DAYS.between(semesterStart, date), 7L).toInt() + 1

/** 0 كل أسبوع، 1 الأسابيع الفردية، 2 الأسابيع الزوجية. */
fun weekActive(mode: Int, week: Int): Boolean = when (mode) {
    1 -> Math.floorMod(week, 2) == 1
    2 -> Math.floorMod(week, 2) == 0
    else -> true
}

val WEEK_MODE_LABELS = listOf("كل أسبوع", "الأسابيع الفردية", "الأسابيع الزوجية")

/** بداية الأسبوع (السبت) الذي يحتوي التاريخ. */
fun weekStart(date: LocalDate): LocalDate = date.minusDays(dayIndex(date).toLong())

/**
 * موعد المحاضرة في [date]؛ null إذا لم تكن مجدولة في ذلك اليوم (يوم مختلف أو أسبوع غير مطابق).
 * الإجازة والإلغاء يُرجعان موعداً ملغى حتى يظهر في الجدول بعلامة.
 */
fun occurrence(
    day: Int, weekMode: Int, startMin: Int, endMin: Int, date: LocalDate, semesterStart: LocalDate,
    excs: List<ExcInfo>, holidays: List<HolidayRange>,
): Occurrence? {
    if (dayIndex(date) != day) return null
    if (!weekActive(weekMode, weekNumber(date, semesterStart))) return null
    val e = date.toEpochDay()
    holidays.firstOrNull { e in it.start..it.end }?.let { return Occurrence(startMin, endMin, true, holiday = it.title.ifBlank { "إجازة" }) }
    val x = excs.firstOrNull { it.dateEpoch == e } ?: return Occurrence(startMin, endMin, false)
    if (x.cancelled) return Occurrence(startMin, endMin, true)
    val s = x.newStart ?: startMin
    val en = x.newEnd ?: endMin
    return Occurrence(s, en, false, changed = x.newStart != null || x.newEnd != null || x.newRoom != null, room = x.newRoom)
}

/**
 * وقت التنبيه القادم بعد [now]. kind: 0 قبل البداية بـ[lead] دقيقة، 1 البداية، 2 النهاية.
 * يتخطى الأسابيع غير المطابقة والإجازات والمواعيد الملغاة.
 */
fun nextTrigger(
    day: Int, weekMode: Int, startMin: Int, endMin: Int, kind: Int, lead: Int, now: LocalDateTime,
    semesterStart: LocalDate, excs: List<ExcInfo>, holidays: List<HolidayRange>,
): LocalDateTime? {
    var d = now.toLocalDate()
    repeat(150) {
        val occ = occurrence(day, weekMode, startMin, endMin, d, semesterStart, excs, holidays)
        if (occ != null && !occ.cancelled) {
            val base = if (kind == 2) occ.end else occ.start
            val t = d.atTime(base / 60, base % 60).minusMinutes(if (kind == 0) lead.toLong() else 0)
            if (t.isAfter(now)) return t
        }
        d = d.plusDays(1)
    }
    return null
}

/** هل يتعارض موعدان في نفس اليوم؟ (الأسبوعان الفردي والزوجي لا يتعارضان). */
fun conflicts(aDay: Int, aMode: Int, aStart: Int, aEnd: Int, bDay: Int, bMode: Int, bStart: Int, bEnd: Int): Boolean {
    if (aDay != bDay) return false
    if (aMode != 0 && bMode != 0 && aMode != bMode) return false
    return aStart < bEnd && bStart < aEnd
}

/** نسبة الغياب: يُحذَّر عند الاقتراب من الحد ثم عند بلوغه. 0 لا تحذير، 1 اقتراب، 2 بلغ الحد. */
fun absenceWarning(absences: Int, limit: Int): Int = when {
    limit <= 0 -> 0
    absences >= limit -> 2
    absences >= limit - 1 -> 1
    else -> 0
}
