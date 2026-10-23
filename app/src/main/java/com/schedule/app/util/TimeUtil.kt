package com.schedule.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** الأيام تبدأ من السبت: 0 = السبت ... 6 = الجمعة. */
val DAY_NAMES = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

fun dayIndex(d: LocalDate): Int = (d.dayOfWeek.value + 1) % 7

fun dayIndexToDow(idx: Int): DayOfWeek {
    val v = (idx + 6) % 7
    return DayOfWeek.of(if (v == 0) 7 else v)
}

fun formatMinutes(min: Int): String {
    val h = (min / 60) % 24
    val m = min % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "ص" else "م")
}

fun formatRange(start: Int, end: Int) = "${formatMinutes(start)} - ${formatMinutes(end)}"

/**
 * أقرب موعد أسبوعي للمحاضرة (يوم + دقيقة من اليوم) يأتي بعد [from] تماماً.
 * يُستخدم لجدولة المنبهات.
 */
fun nextOccurrence(day: Int, minuteOfDay: Int, from: LocalDateTime): LocalDateTime {
    var d = from.toLocalDate()
    repeat(8) {
        if (dayIndex(d) == day) {
            val t = d.atTime(minuteOfDay / 60, minuteOfDay % 60)
            if (t.isAfter(from)) return t
        }
        d = d.plusDays(1)
    }
    error("unreachable")
}

/** مدة المحاضرة صالحة إذا كانت النهاية بعد البداية. */
fun isValidRange(start: Int, end: Int) = end > start

fun daysUntil(epochDay: Long, today: Long): Long = epochDay - today
