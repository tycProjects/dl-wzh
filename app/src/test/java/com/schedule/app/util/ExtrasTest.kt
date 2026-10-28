package com.schedule.app.util

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExtrasTest {
    // الأربعاء 2026-10-28
    private val today = LocalDate.of(2026, 10, 28)

    @Test fun addLecture() {
        val c = parseCommand("ضيف محاضرة فيزياء يوم الأحد من 10 الى 12 قاعة 5", today) as Cmd.AddLecture
        assertEquals("فيزياء", c.title); assertEquals(1, c.day); assertEquals(600, c.start); assertEquals(720, c.end); assertEquals("قاعه 5", c.room)
    }
    @Test fun afternoonDefault() {
        val c = parseCommand("ضيف محاضرة كيمياء يوم الاثنين من 1 الى 3", today) as Cmd.AddLecture
        assertEquals(13 * 60, c.start); assertEquals(15 * 60, c.end)
    }
    @Test fun addTask() {
        val c = parseCommand("ضيف واجب حل التمارين لمادة الفيزياء بكرة", today) as Cmd.AddTask
        assertEquals("حل التمارين", c.title); assertEquals("الفيزياء", c.subject); assertEquals(today.plusDays(1).toEpochDay(), c.due)
    }
    @Test fun addExam() {
        val c = parseCommand("ضيف امتحان الرياضيات 15/12", today) as Cmd.AddExam
        assertEquals("الرياضيات", c.title); assertEquals(LocalDate.of(2026, 12, 15).toEpochDay(), c.date)
    }
    @Test fun cancelAndAgenda() {
        val c = parseCommand("الغي محاضرة الفيزياء بكرة", today) as Cmd.Cancel
        assertEquals("الفيزياء", c.subject); assertEquals(today.plusDays(1).toEpochDay(), c.date)
        val a = parseCommand("ايه محاضراتي بكرة؟", today) as Cmd.Agenda
        assertEquals(today.plusDays(1).toEpochDay(), a.date)
        assertTrue(parseCommand("المحاضرة الجاية امتى", today) is Cmd.Next)
        assertTrue(parseCommand("كلام عشوائي", today) is Cmd.Unknown)
    }
    @Test fun noteAndMatch() {
        val c = parseCommand("ضيف ملاحظة لمادة الكيمياء: احضر الكتاب", today) as Cmd.AddNote
        assertEquals("الكيمياء", c.subject); assertEquals("احضر الكتاب", c.text)
        assertEquals("الكيمياء التحليلية", matchSubject("كيمياء", listOf("الرياضيات", "الكيمياء التحليلية")))
    }
    @Test fun gaps() {
        assertEquals(listOf(8 * 60 to 9 * 60, 12 * 60 to 18 * 60), freeGaps(listOf(9 * 60 to 12 * 60), minLen = 30))
        assertEquals(listOf(10 * 60 to 18 * 60), freeGaps(listOf(8 * 60 to 9 * 60, 8 * 60 + 30 to 10 * 60)))
        val c = commonFree(listOf(DaySpan(0, 8 * 60, 12 * 60)), listOf(DaySpan(0, 11 * 60, 14 * 60)))
        assertEquals(listOf(14 * 60 to 18 * 60), c[0])
    }
    @Test fun travel() {
        val t = travelTight(listOf(TravelItem(1, 540, 600, "أ"), TravelItem(2, 605, 660, "ب"), TravelItem(3, 700, 760, "ج")), 10)
        assertEquals(setOf(2L), t)
        assertTrue(travelTight(listOf(TravelItem(1, 540, 600, "أ"), TravelItem(2, 605, 660, "أ")), 10).isEmpty())
    }
    @Test fun ics() {
        val sem = LocalDate.of(2026, 10, 24) // سبت
        val s = IcsSlot(1, "فيزياء, 1", "د. س", "5", 0, 540, 630, 0, emptySet())
        val ics = buildIcs(listOf(s), listOf(IcsExam("ميد", LocalDate.of(2026, 12, 1).toEpochDay())), emptyList(), sem, emptyList(), weeks = 3)
        assertEquals(3, Regex("DTSTART:20261\\d{3}T090000").findAll(ics).count())
        assertTrue(ics.contains("SUMMARY:فيزياء\\, 1")); assertTrue(ics.contains("DTSTART;VALUE=DATE:20261201"))
        val hol = buildIcs(listOf(s), emptyList(), emptyList(), sem, listOf(HolidayRange(sem.plusWeeks(1).toEpochDay(), sem.plusWeeks(1).toEpochDay())), weeks = 3)
        assertEquals(2, Regex("BEGIN:VEVENT").findAll(hol).count())
        val dates = IcsSlot(2, "س", "", "", 0, 600, 660, 5, setOf(sem.plusDays(3).toEpochDay()))
        assertEquals(1, Regex("BEGIN:VEVENT").findAll(buildIcs(listOf(dates), emptyList(), emptyList(), sem, emptyList(), weeks = 3)).count())
    }
    @Test fun plan() {
        val p = studyPlan(today, listOf(PlanExam("فيزياء", today.plusDays(10).toEpochDay(), 6)), { listOf(9 * 60 to 12 * 60) })
        assertEquals(4, p.size)
        assertTrue(p.all { it.dateEpoch < today.plusDays(10).toEpochDay() && it.start >= 12 * 60 })
        assertTrue(p.map { it.dateEpoch }.distinct().size >= 3)
    }
    @Test fun grades() {
        val g = listOf(GradeIn(18.0, 20.0, 20.0), GradeIn(8.0, 10.0, 10.0))
        assertEquals(26.0, earnedPoints(g), 1e-9)
        assertEquals(86.666, courseAverage(g)!!, 0.01)
        assertEquals(68.571, neededOnFinal(g, 70.0, 74.0) ?: 0.0, 0.01) // (74-26)/70*100
        assertEquals("A", percentToLetter(95.0)); assertEquals(3.7, percentToPoints(91.0), 1e-9)
        assertEquals(3.88, gpa(listOf(95.0 to 3, 91.0 to 2, 82.0 to 0)) ?: 0.0, 0.01)
    }
    @Test fun spaced() {
        var s = SrState(0, 2.5, 0)
        s = sm2(s, 4); assertEquals(1, s.intervalDays)
        s = sm2(s, 4); assertEquals(6, s.intervalDays)
        s = sm2(s, 5); assertTrue(s.intervalDays > 6)
        s = sm2(s, 1); assertEquals(1, s.intervalDays); assertEquals(0, s.reps)
    }
    @Test fun text() {
        val t = "الخلية هي وحدة البناء الاساسية للكائن الحي. تحتوي الخلية على نواة تحمل المادة الوراثية. الميتوكوندريا تنتج الطاقة للخلية. الطقس جميل اليوم."
        val sm = summarize(t, 2); assertEquals(2, sm.size)
        assertTrue(makeQuestions(t, 3).isNotEmpty())
        val r = tfidfRank("الطاقة", listOf(1L to t.substringAfter("الميتوكوندريا"), 2L to "الرياضيات والجبر"), 3)
        assertEquals(1L, r.first().first)
        val rich = parseRich("# عنوان\n- نقطة **مهم** نص\n- [x] تم")
        assertEquals(1, rich[0].kind); assertEquals(2, rich[1].kind); assertTrue(rich[1].spans.any { it.bold && it.text == "مهم" }); assertEquals(4, rich[2].kind)
        assertEquals(listOf("أ", "ب", "ج"), parseTags("أ, ب،#ج"))
    }
    @Test fun recurrence() {
        assertTrue(weekActive(3, 1)); assertFalse(weekActive(3, 2)); assertTrue(weekActive(3, 4)); assertTrue(weekActive(4, 5)); assertFalse(weekActive(4, 4))
        assertTrue(examAlertDue(10, 3)); assertFalse(examAlertDue(10, 4)); assertTrue(isNight(22)); assertFalse(isNight(12))
    }
}
