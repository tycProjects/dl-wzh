package com.schedule.app.util

import kotlin.math.max
import kotlin.math.roundToInt

// ---------- الدرجات والمعدل ----------
data class GradeIn(val score: Double, val max: Double, val weight: Double, val isFinal: Boolean = false)

/** النقاط المحصَّلة من الوزن الكلي (الوزن بالنسبة من 100). */
fun earnedPoints(g: List<GradeIn>, includeFinal: Boolean = true): Double =
    g.filter { includeFinal || !it.isFinal }.sumOf { if (it.max > 0) it.score / it.max * it.weight else 0.0 }

fun completedWeight(g: List<GradeIn>): Double = g.sumOf { it.weight }

/** نسبة المادة حتى الآن من الأعمال المسجلة (تُقسم على الوزن المنجز)؛ null إن لم توجد درجات. */
fun courseAverage(g: List<GradeIn>): Double? {
    val w = completedWeight(g); if (w <= 0) return null
    return earnedPoints(g) / w * 100.0
}

/** الدرجة المطلوبة (كنسبة مئوية) في النهائي ليبلغ مجموع المادة [targetPercent] من 100. */
fun neededOnFinal(g: List<GradeIn>, finalWeight: Double, targetPercent: Double): Double? {
    if (finalWeight <= 0) return null
    return (targetPercent - earnedPoints(g, includeFinal = false)) / finalWeight * 100.0
}

/** مقياس 4.0 شائع (تقريبي؛ قد تختلف كليتك). */
fun percentToPoints(p: Double): Double = when {
    p >= 97 -> 4.0; p >= 93 -> 4.0; p >= 90 -> 3.7; p >= 87 -> 3.3; p >= 83 -> 3.0; p >= 80 -> 2.7
    p >= 77 -> 2.3; p >= 73 -> 2.0; p >= 70 -> 1.7; p >= 67 -> 1.3; p >= 63 -> 1.0; p >= 60 -> 0.7; else -> 0.0
}

fun percentToLetter(p: Double): String = when {
    p >= 97 -> "A+"; p >= 93 -> "A"; p >= 90 -> "A-"; p >= 87 -> "B+"; p >= 83 -> "B"; p >= 80 -> "B-"
    p >= 77 -> "C+"; p >= 73 -> "C"; p >= 70 -> "C-"; p >= 67 -> "D+"; p >= 63 -> "D"; p >= 60 -> "D-"; else -> "F"
}

/** المعدل التراكمي مرجّحا بالساعات المعتمدة. */
fun gpa(courses: List<Pair<Double, Int>>): Double? {
    val hours = courses.sumOf { it.second }; if (hours <= 0) return null
    return courses.sumOf { percentToPoints(it.first) * it.second } / hours
}

// ---------- التكرار المتباعد SM-2 ----------
data class SrState(val intervalDays: Int, val ease: Double, val reps: Int)

/** quality: 1 لم أتذكر، 3 صعب، 4 جيد، 5 سهل. */
fun sm2(s: SrState, quality: Int): SrState {
    if (quality < 3) return SrState(1, max(1.3, s.ease - 0.2), 0)
    val reps = s.reps + 1
    val interval = when (reps) { 1 -> 1; 2 -> 6; else -> (s.intervalDays * s.ease).roundToInt().coerceAtLeast(s.intervalDays + 1) }
    val q = 5 - quality
    val ease = max(1.3, s.ease + 0.1 - q * (0.08 + q * 0.02))
    return SrState(interval, ease, reps)
}

// ---------- إحصائيات المذاكرة ----------
data class SessionIn(val slotId: Long, val startedAtMs: Long, val minutes: Int)

fun minutesBySlot(sessions: List<SessionIn>, fromMs: Long = 0, toMs: Long = Long.MAX_VALUE): Map<Long, Int> =
    sessions.filter { it.startedAtMs in fromMs until toMs }.groupBy { it.slotId }.mapValues { e -> e.value.sumOf { it.minutes } }

fun goalProgress(minutes: Int, goal: Int): Float = if (goal <= 0) 0f else (minutes.toFloat() / goal).coerceIn(0f, 1f)

fun completionRate(done: Int, total: Int): Int = if (total == 0) 0 else (done * 100.0 / total).roundToInt()

fun formatMinutesLong(min: Int): String = if (min >= 60) "${min / 60} س ${min % 60} د" else "$min د"
