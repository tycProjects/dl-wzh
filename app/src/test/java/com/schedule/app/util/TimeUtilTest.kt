package com.schedule.app.util

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class TimeUtilTest {
    @Test fun dayIndexStartsFromSaturday() {
        assertEquals(0, dayIndex(LocalDate.of(2026, 10, 17))) // السبت
        assertEquals(1, dayIndex(LocalDate.of(2026, 10, 18))) // الأحد
        assertEquals(6, dayIndex(LocalDate.of(2026, 10, 23))) // الجمعة
        for (i in 0..6) assertEquals(i, dayIndex(LocalDate.of(2026, 10, 17).plusDays(i.toLong())))
        assertEquals(DayOfWeek.SATURDAY, dayIndexToDow(0)); assertEquals(DayOfWeek.FRIDAY, dayIndexToDow(6))
    }

    @Test fun nextOccurrenceSameDayLaterAndNextWeek() {
        val sunday = LocalDateTime.of(2026, 10, 18, 8, 0)
        assertEquals(LocalDateTime.of(2026, 10, 18, 9, 0), nextOccurrence(1, 9 * 60, sunday))
        assertEquals(LocalDateTime.of(2026, 10, 25, 9, 0), nextOccurrence(1, 9 * 60, LocalDateTime.of(2026, 10, 18, 9, 0)))
        assertEquals(LocalDateTime.of(2026, 10, 21, 10, 30), nextOccurrence(4, 10 * 60 + 30, sunday)) // الأربعاء
    }

    @Test fun formatsArabicTime() {
        assertEquals("12:05 ص", formatMinutes(5)); assertEquals("1:30 م", formatMinutes(13 * 60 + 30)); assertEquals("12:00 م", formatMinutes(12 * 60))
    }

    @Test fun rangeValidation() { assertTrue(isValidRange(540, 600)); assertFalse(isValidRange(600, 600)) }
}
