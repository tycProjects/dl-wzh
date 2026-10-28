package com.schedule.app.util

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleTest {
    private val sem = LocalDate.of(2026, 10, 17) // سبت = بداية الأسبوع 1

    @Test fun weekNumbers() {
        assertEquals(1, weekNumber(LocalDate.of(2026, 10, 17), sem))
        assertEquals(1, weekNumber(LocalDate.of(2026, 10, 23), sem))
        assertEquals(2, weekNumber(LocalDate.of(2026, 10, 24), sem))
        assertEquals(0, weekNumber(LocalDate.of(2026, 10, 16), sem))
        assertTrue(weekActive(1, 1)); assertFalse(weekActive(1, 2)); assertTrue(weekActive(2, 2)); assertTrue(weekActive(2, 0))
    }

    @Test fun oddEvenSkipsInactiveWeeks() {
        // الأحد (1) في الأسابيع الزوجية: أول أحد في الأسبوع 1 غير مطابق، فيأتي التالي (أسبوع 2)
        val t = nextTrigger(1, 2, 540, 630, 1, 0, LocalDateTime.of(2026, 10, 17, 8, 0), sem, emptyList(), emptyList())
        assertEquals(LocalDateTime.of(2026, 10, 25, 9, 0), t)
    }

    @Test fun cancelledAndHolidaySkipped() {
        val sun = LocalDate.of(2026, 10, 18).toEpochDay()
        val t = nextTrigger(1, 0, 540, 630, 1, 0, LocalDateTime.of(2026, 10, 17, 8, 0), sem, listOf(ExcInfo(sun, true)), emptyList())
        assertEquals(LocalDateTime.of(2026, 10, 25, 9, 0), t)
        val h = nextTrigger(1, 0, 540, 630, 1, 0, LocalDateTime.of(2026, 10, 17, 8, 0), sem, emptyList(), listOf(HolidayRange(sun, sun + 7)))
        assertEquals(LocalDateTime.of(2026, 11, 1, 9, 0), h)
    }

    @Test fun oneTimeChangeAffectsTrigger() {
        val sun = LocalDate.of(2026, 10, 18).toEpochDay()
        val t = nextTrigger(1, 0, 540, 630, 1, 0, LocalDateTime.of(2026, 10, 17, 8, 0), sem, listOf(ExcInfo(sun, false, newStart = 660, newEnd = 720)), emptyList())
        assertEquals(LocalDateTime.of(2026, 10, 18, 11, 0), t)
    }

    @Test fun leadAndEndKinds() {
        val now = LocalDateTime.of(2026, 10, 18, 8, 0)
        assertEquals(LocalDateTime.of(2026, 10, 18, 8, 50), nextTrigger(1, 0, 540, 630, 0, 10, now, sem, emptyList(), emptyList()))
        assertEquals(LocalDateTime.of(2026, 10, 18, 10, 30), nextTrigger(1, 0, 540, 630, 2, 0, now, sem, emptyList(), emptyList()))
        // بعد تجاوز وقت التنبيه المسبق يُرجع الأسبوع التالي
        assertEquals(LocalDateTime.of(2026, 10, 25, 8, 50), nextTrigger(1, 0, 540, 630, 0, 10, LocalDateTime.of(2026, 10, 18, 8, 55), sem, emptyList(), emptyList()))
    }

    @Test fun conflictRules() {
        assertTrue(conflicts(1, 0, 540, 600, 1, 0, 570, 630))
        assertFalse(conflicts(1, 0, 540, 600, 1, 0, 600, 660)) // تلامس بدون تداخل
        assertFalse(conflicts(1, 1, 540, 600, 1, 2, 540, 600)) // فردي مقابل زوجي
        assertFalse(conflicts(1, 0, 540, 600, 2, 0, 540, 600))
    }

    @Test fun absenceWarnings() { assertEquals(0, absenceWarning(2, 4)); assertEquals(1, absenceWarning(3, 4)); assertEquals(2, absenceWarning(4, 4)); assertEquals(0, absenceWarning(9, 0)) }

    @Test fun csvParsing() {
        val r = parseScheduleCsv("المادة,اليوم,من,إلى,المحاضر,القاعة\nبرمجة,الأحد,9:00 ص,10:30 ص,د. أحمد,قاعة 3\n\"فيزياء, عملي\";الثلاثاء;13:00;15:00;;معمل;فردي\nخطأ,يوم,1,2")
        assertEquals(2, r.slots.size)
        assertEquals(1, r.slots[0].day); assertEquals(540, r.slots[0].startMin); assertEquals(630, r.slots[0].endMin)
        assertEquals("فيزياء, عملي", r.slots[1].title)
        assertEquals(1, r.slots[1].weekMode)
        assertEquals(1, r.errors.size)
    }

    @Test fun timeAndDayParsing() {
        assertEquals(14 * 60 + 30, parseTime("2:30 م")); assertEquals(0, parseTime("12:00 ص")); assertEquals(12 * 60, parseTime("12 م"))
        assertEquals(15 * 60, parseTime("15:00")); assertNull(parseTime("25:00"))
        assertEquals(0, parseDay("السبت")); assertEquals(1, parseDay("الأحد")); assertEquals(1, parseDay("الاحد")); assertEquals(6, parseDay("6")); assertEquals(4, parseDay("Wed"))
    }

    @Test fun csvRoundTrip() {
        val rows = listOf(CsvSlot("مادة, ما", 2, 540, 600, "د. س", "قاعة", 2))
        val back = parseScheduleCsv(slotsToCsv(rows))
        assertEquals(rows, back.slots)
    }
}

class TextNormTest {
    @Test fun normalizesArabic() {
        assertEquals(normalizeArabic("أَحْمَد"), normalizeArabic("احمد"))
        assertEquals("مدرسه", normalizeArabic("مدرسة"))
        assertEquals("علي", normalizeArabic("على"))
        assertEquals("12", normalizeArabic("١٢"))
    }
}
