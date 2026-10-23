package com.lecturemanager.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilsTest {
    @Test fun normalizeRemovesDiacriticsAndUnifiesLetters() {
        assertEquals("الاسلام", normalizeArabic("الإسْلام"))
        assertEquals("مدرسه", normalizeArabic("مَدْرَسَة"))
        assertEquals("علي", normalizeArabic("على"))
        assertEquals("كتاب 12", normalizeArabic("كِتــاب ١٢"))
    }

    @Test fun normalizeHandlesPresentationForms() {
        // "لا" بصيغة العرض U+FEFB تتحول إلى حرفين لا
        assertEquals("لا", normalizeArabic("ﻻ"))
    }

    @Test fun snippetWindows() {
        val t = "a".repeat(100) + "هدف" + "b".repeat(100)
        val s = snippet(t, "هدف", 5)
        assertTrue(s.startsWith("…") && s.endsWith("…") && s.contains("هدف"))
    }

    @Test fun parsesDoubleColon() {
        val r = parseFlashcards("- الخلية :: الوحدة الأساسية للحياة\nنص عادي")
        assertEquals(listOf("الخلية" to "الوحدة الأساسية للحياة"), r)
    }

    @Test fun parsesQuestionAnswerLines() {
        val r = parseFlashcards("س: ما هو الميتوكوندريا؟\nج: مصنع الطاقة\nفي الخلية\n\nس: ما النواة؟ ج: مركز التحكم")
        assertEquals(2, r.size)
        assertEquals("ما هو الميتوكوندريا؟" to "مصنع الطاقة\nفي الخلية", r[0])
        assertEquals("ما النواة؟" to "مركز التحكم", r[1])
    }

    @Test fun ignoresIncomplete() {
        assertTrue(parseFlashcards("س: سؤال بدون جواب\n\nكلام").isEmpty())
        assertTrue(parseFlashcards(":: بدون مصطلح").isEmpty())
    }

    @Test fun leitner() {
        assertEquals(1, nextBox(0, true)); assertEquals(4, nextBox(4, true)); assertEquals(0, nextBox(3, false))
        assertEquals(110L, dueDayAfter(2, 107))
        assertEquals(100L, dueDayAfter(0, 100))
    }

    @Test fun examCountdown() {
        val day = 20000L
        assertEquals(5L, daysUntilExam(day * 86_400_000L + 3_600_000L, day - 5))
        assertEquals(0L, daysUntilExam(day * 86_400_000L, day))
        assertEquals(-2L, daysUntilExam(day * 86_400_000L, day + 2))
    }

    @Test fun formatsDuration() {
        assertEquals("45 ث", formatSeconds(45)); assertEquals("5 د", formatSeconds(300)); assertEquals("1 س 5 د", formatSeconds(3900))
    }

    @Test fun pointsRoundTrip() {
        val enc = encodePoints(floatArrayOf(0.1f, 0.5f), floatArrayOf(0.2f, 0.9f))
        assertEquals(listOf(0.1f to 0.2f, 0.5f to 0.9f), decodePoints(enc))
        assertTrue(decodePoints("").isEmpty())
    }
}
