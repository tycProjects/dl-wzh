package com.lecturemanager.app.util

import java.text.Normalizer

/** توحيد النص العربي للبحث: يزيل التشكيل والتطويل ويوحّد الألف والياء والتاء المربوطة والأرقام. */
fun normalizeArabic(input: String): String {
    val nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC)
    val sb = StringBuilder(nfkc.length)
    for (ch in nfkc) {
        val c = when (ch) {
            in 'ً'..'ٟ', 'ٰ', in 'ۖ'..'ۭ', 'ـ' -> continue
            'أ', 'إ', 'آ', 'ٱ' -> 'ا'
            'ى' -> 'ي'
            'ة' -> 'ه'
            in '٠'..'٩' -> '0' + (ch - '٠')
            in '۰'..'۹' -> '0' + (ch - '۰')
            else -> ch.lowercaseChar()
        }
        sb.append(c)
    }
    return sb.toString().replace(Regex("\\s+"), " ").trim()
}

/** مقتطف حول أول ظهور للكلمة (النص والكلمة مطبَّعان مسبقاً). */
fun snippet(text: String, query: String, radius: Int = 40): String {
    val i = text.indexOf(query)
    if (i < 0) return text.take(radius * 2)
    val start = (i - radius).coerceAtLeast(0)
    val end = (i + query.length + radius).coerceAtMost(text.length)
    return (if (start > 0) "…" else "") + text.substring(start, end) + (if (end < text.length) "…" else "")
}

private val Q_LINE = Regex("^[\\-*•\\d.)\\s]*(?:س|سؤال)\\s*[:：]\\s*(.+)\$")
private val A_LINE = Regex("^[\\-*•\\s]*(?:ج|جواب|الجواب|الإجابة|الاجابة)\\s*[:：]\\s*(.+)\$")
private val INLINE_QA = Regex("^(.*?)\\s+(?:ج|جواب|الجواب|الإجابة|الاجابة)\\s*[:：]\\s*(.+)\$")

/**
 * يستخرج بطاقات (سؤال، جواب) من الملاحظات:
 *  - `مصطلح :: تعريف`
 *  - `س: السؤال` ثم `ج: الجواب` في السطر التالي (أو `س: ... ج: ...` في سطر واحد)
 */
fun parseFlashcards(notes: String): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    val lines = notes.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        val q = Q_LINE.matchEntire(line)
        if (q != null) {
            val rest = q.groupValues[1].trim()
            val inline = INLINE_QA.matchEntire(rest)
            if (inline != null) {
                val f = inline.groupValues[1].trim(); val b = inline.groupValues[2].trim()
                if (f.isNotEmpty() && b.isNotEmpty()) out += f to b
                i++; continue
            }
            // الجواب في الأسطر التالية
            var j = i + 1
            while (j < lines.size && lines[j].isBlank()) j++
            val a = if (j < lines.size) A_LINE.matchEntire(lines[j].trim()) else null
            if (a != null) {
                val sb = StringBuilder(a.groupValues[1].trim())
                var k = j + 1
                while (k < lines.size && lines[k].isNotBlank() && Q_LINE.matchEntire(lines[k].trim()) == null && !lines[k].contains("::")) {
                    sb.append('\n').append(lines[k].trim()); k++
                }
                out += rest to sb.toString()
                i = k; continue
            }
            i++; continue
        }
        val idx = line.indexOf("::")
        if (idx > 0) {
            val f = line.substring(0, idx).trim().trimStart('-', '*', '•', ' ')
            val b = line.substring(idx + 2).trim()
            if (f.isNotEmpty() && b.isNotEmpty()) out += f to b
        }
        i++
    }
    return out
}

/** صناديق لايتنر: الفاصل (بالأيام) قبل المراجعة التالية لكل صندوق. */
val LEITNER_INTERVALS = intArrayOf(0, 1, 3, 7, 14)
const val LEITNER_MAX_BOX = 4

fun nextBox(box: Int, correct: Boolean): Int = if (correct) (box + 1).coerceAtMost(LEITNER_MAX_BOX) else 0
fun dueDayAfter(box: Int, today: Long): Long = today + LEITNER_INTERVALS[box.coerceIn(0, LEITNER_MAX_BOX)]

/** عدد الأيام من اليوم حتى الامتحان. الموعد مخزَّن كمنتصف ليل UTC لليوم المختار (كما يعيده DatePicker). */
fun daysUntilExam(examMillis: Long, todayEpochDay: Long): Long =
    Math.floorDiv(examMillis, 86_400_000L) - todayEpochDay

fun formatSeconds(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return when {
        h > 0 -> "$h س ${m} د"
        m > 0 -> "$m د"
        else -> "${totalSeconds} ث"
    }
}

/** ترميز/فك نقاط الرسم "x,y;x,y". */
fun encodePoints(xs: FloatArray, ys: FloatArray): String {
    val sb = StringBuilder()
    for (i in xs.indices) { if (i > 0) sb.append(';'); sb.append(xs[i]).append(',').append(ys[i]) }
    return sb.toString()
}

fun decodePoints(s: String): List<Pair<Float, Float>> =
    if (s.isEmpty()) emptyList() else s.split(';').mapNotNull { p ->
        val t = p.split(','); if (t.size == 2) (t[0].toFloatOrNull() ?: return@mapNotNull null) to (t[1].toFloatOrNull() ?: return@mapNotNull null) else null
    }
