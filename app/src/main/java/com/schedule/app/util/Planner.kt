package com.schedule.app.util

import java.time.LocalDate
import kotlin.math.ceil

// ---------- تصدير ICS ----------
data class IcsSlot(
    val id: Long, val title: String, val instructor: String, val room: String, val day: Int, val start: Int, val end: Int,
    val weekMode: Int, val dates: Set<Long>, val excs: List<ExcInfo> = emptyList(),
)
data class IcsExam(val title: String, val dateEpoch: Long)
data class IcsTask(val id: Long, val title: String, val dueEpoch: Long, val done: Boolean)

private fun esc(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r", "").replace("\n", "\\n")

private fun fold(line: String): String {
    if (line.length <= 73) return line
    val sb = StringBuilder(); var i = 0
    while (i < line.length) {
        val end = minOf(line.length, i + (if (i == 0) 73 else 72))
        if (i > 0) sb.append("\r\n ")
        sb.append(line, i, end); i = end
    }
    return sb.toString()
}

private fun ymd(d: LocalDate) = "%04d%02d%02d".format(d.year, d.monthValue, d.dayOfMonth)
private fun hms(min: Int) = "%02d%02d00".format(min / 60, min % 60)

/** يبني ملف iCalendar: كل موعد فعلي (بعد الأسابيع والإجازات والاستثناءات) حدث مستقل، + الامتحانات + الواجبات. */
fun buildIcs(
    slots: List<IcsSlot>, exams: List<IcsExam>, tasks: List<IcsTask>, semesterStart: LocalDate,
    holidays: List<HolidayRange>, weeks: Int = 18, calName: String = "منظم الجدول",
): String {
    val out = ArrayList<String>()
    out += "BEGIN:VCALENDAR"; out += "VERSION:2.0"; out += "PRODID:-//ScheduleApp//AR"; out += "CALSCALE:GREGORIAN"
    out += "X-WR-CALNAME:" + esc(calName)
    val from = semesterStart; val to = semesterStart.plusWeeks(weeks.toLong())
    slots.forEach { s ->
        var d = from
        while (d.isBefore(to)) {
            val o = occurrence(s.day, s.weekMode, s.start, s.end, d, semesterStart, s.excs, holidays, s.dates)
            if (o != null && !o.cancelled) {
                out += "BEGIN:VEVENT"
                out += "UID:slot${s.id}-${ymd(d)}@schedule.app"
                out += "DTSTAMP:${ymd(from)}T000000"
                out += "DTSTART:${ymd(d)}T${hms(o.start)}"
                out += "DTEND:${ymd(d)}T${hms(o.end)}"
                out += fold("SUMMARY:" + esc(s.title))
                val room = o.room ?: s.room
                if (room.isNotBlank()) out += fold("LOCATION:" + esc(room))
                if (s.instructor.isNotBlank()) out += fold("DESCRIPTION:" + esc("المحاضر: " + s.instructor))
                out += "END:VEVENT"
            }
            d = d.plusDays(1)
        }
    }
    exams.forEach { e ->
        val d = LocalDate.ofEpochDay(e.dateEpoch)
        out += "BEGIN:VEVENT"; out += "UID:exam${e.title.hashCode()}-${ymd(d)}@schedule.app"; out += "DTSTAMP:${ymd(from)}T000000"
        out += "DTSTART;VALUE=DATE:${ymd(d)}"; out += "DTEND;VALUE=DATE:${ymd(d.plusDays(1))}"
        out += fold("SUMMARY:" + esc("امتحان: " + e.title)); out += "END:VEVENT"
    }
    tasks.filter { !it.done }.forEach { t ->
        val d = LocalDate.ofEpochDay(t.dueEpoch)
        out += "BEGIN:VEVENT"; out += "UID:task${t.id}-${ymd(d)}@schedule.app"; out += "DTSTAMP:${ymd(from)}T000000"
        out += "DTSTART;VALUE=DATE:${ymd(d)}"; out += "DTEND;VALUE=DATE:${ymd(d.plusDays(1))}"
        out += fold("SUMMARY:" + esc("تسليم: " + t.title)); out += "END:VEVENT"
    }
    out += "END:VCALENDAR"
    return out.joinToString("\r\n") + "\r\n"
}

// ---------- الفراغات ----------
/** فترات الفراغ داخل [from, to] بعد حذف المشغول، بطول لا يقل عن [minLen] دقيقة. */
fun freeGaps(busy: List<Pair<Int, Int>>, from: Int = 8 * 60, to: Int = 18 * 60, minLen: Int = 30): List<Pair<Int, Int>> {
    val merged = ArrayList<Pair<Int, Int>>()
    busy.filter { it.second > it.first }.sortedBy { it.first }.forEach { b ->
        val last = merged.lastOrNull()
        if (last != null && b.first <= last.second) merged[merged.lastIndex] = last.first to maxOf(last.second, b.second) else merged += b
    }
    val gaps = ArrayList<Pair<Int, Int>>()
    var cur = from
    for ((s, e) in merged) {
        if (e <= cur) continue
        if (s > cur) gaps += cur to minOf(s, to)
        cur = maxOf(cur, e)
        if (cur >= to) break
    }
    if (cur < to) gaps += cur to to
    return gaps.filter { it.second - it.first >= minLen }
}

data class DaySpan(val day: Int, val start: Int, val end: Int)

/** الفراغ المشترك بين جدولين لكل يوم (0..6). */
fun commonFree(a: List<DaySpan>, b: List<DaySpan>, from: Int = 8 * 60, to: Int = 18 * 60, minLen: Int = 45): Map<Int, List<Pair<Int, Int>>> =
    (0..6).associateWith { d -> freeGaps((a + b).filter { it.day == d }.map { it.start to it.end }, from, to, minLen) }.filterValues { it.isNotEmpty() }

/** أفضل فترة فراغ لكل يوم للمذاكرة (الأطول، مع تفضيل ما بعد الظهر عند التساوي). */
fun suggestStudyTimes(busyByDay: Map<Int, List<Pair<Int, Int>>>, minLen: Int = 60): List<DaySpan> =
    (0..6).mapNotNull { d ->
        freeGaps(busyByDay[d].orEmpty(), 9 * 60, 22 * 60, minLen)
            .maxWithOrNull(compareBy({ minOf(it.second - it.first, 180) }, { it.first }))
            ?.let { g -> DaySpan(d, g.first, minOf(g.second, g.first + 180)) }
    }

// ---------- خطة المذاكرة قبل الامتحانات ----------
data class PlanExam(val title: String, val dateEpoch: Long, val hoursNeeded: Int)
data class PlanItem(val dateEpoch: Long, val start: Int, val end: Int, val title: String)

/**
 * يوزّع جلسات مذاكرة (افتراضيا 90 دقيقة) على الأيام التي تسبق كل امتحان في فترات فراغ، بدون تداخل مع المحاضرات أو الجلسات الأخرى.
 * [busy] يرجع مشغول كل تاريخ (محاضرات).
 */
fun studyPlan(today: LocalDate, exams: List<PlanExam>, busy: (LocalDate) -> List<Pair<Int, Int>>, sessionMin: Int = 90): List<PlanItem> {
    val placed = HashMap<Long, MutableList<Pair<Int, Int>>>()
    val result = ArrayList<PlanItem>()
    for (ex in exams.sortedBy { it.dateEpoch }) {
        val last = ex.dateEpoch - 1
        if (last < today.toEpochDay()) continue
        val days = (today.toEpochDay()..last).toList()
        val sessions = ceil(ex.hoursNeeded * 60.0 / sessionMin).toInt().coerceAtLeast(1)
        var made = 0
        for (i in 0 until sessions) {
            val pref = (i * days.size.toDouble() / sessions).toInt().coerceIn(0, days.size - 1)
            // نجرب اليوم المفضل ثم الأيام التالية ثم السابقة
            val order = (pref until days.size) + (pref - 1 downTo 0)
            for (k in order) {
                val e = days[k]
                val b = busy(LocalDate.ofEpochDay(e)) + placed[e].orEmpty()
                val gap = freeGaps(b, 9 * 60, 22 * 60, sessionMin).firstOrNull() ?: continue
                val item = PlanItem(e, gap.first, gap.first + sessionMin, ex.title)
                placed.getOrPut(e) { mutableListOf() } += item.start to item.end
                result += item; made++
                break
            }
        }
    }
    return result.sortedWith(compareBy({ it.dateEpoch }, { it.start }))
}

// ---------- زمن التنقل ----------
data class TravelItem(val id: Long, val start: Int, val end: Int, val room: String)

/** معرّفات المحاضرات التي لا يكفي الوقت للوصول إليها من القاعة السابقة (قاعتان مختلفتان وفاصل أقل من [travel] دقيقة). */
fun travelTight(items: List<TravelItem>, travel: Int): Set<Long> {
    if (travel <= 0) return emptySet()
    val sorted = items.sortedBy { it.start }
    val out = HashSet<Long>()
    for (i in 1 until sorted.size) {
        val a = sorted[i - 1]; val b = sorted[i]
        if (a.room.isBlank() || b.room.isBlank() || normalizeArabic(a.room) == normalizeArabic(b.room)) continue
        val gap = b.start - a.end
        if (gap in 0 until travel) out += b.id
    }
    return out
}

// ---------- تنبيهات الامتحانات ----------
val EXAM_ALERT_DAYS = listOf(7, 3, 1)
fun examAlertDue(examEpoch: Long, todayEpoch: Long): Boolean = (examEpoch - todayEpoch).toInt() in EXAM_ALERT_DAYS

// ---------- أوقات اليوم ----------
fun isNight(hour: Int, from: Int = 18, to: Int = 6): Boolean = hour >= from || hour < to
