package com.schedule.app.util

data class CsvSlot(val title: String, val day: Int, val startMin: Int, val endMin: Int, val instructor: String, val room: String, val weekMode: Int)
data class CsvResult(val slots: List<CsvSlot>, val errors: List<String>)

private fun splitLine(line: String, sep: Char): List<String> {
    val out = mutableListOf<String>(); val sb = StringBuilder(); var q = false
    for (c in line) when {
        c == '"' -> q = !q
        c == sep && !q -> { out += sb.toString().trim(); sb.setLength(0) }
        else -> sb.append(c)
    }
    out += sb.toString().trim()
    return out
}

fun parseDay(raw: String): Int? {
    val t = normalizeArabic(raw)
    t.toIntOrNull()?.let { return if (it in 0..6) it else null }
    return DAY_NAMES.indexOfFirst { normalizeArabic(it) == t }.takeIf { it >= 0 }
        ?: listOf("السبت", "الاحد", "الاثنين", "الثلاثاء", "الاربعاء", "الخميس", "الجمعه").indexOfFirst { normalizeArabic(it) == t }.takeIf { it >= 0 }
        ?: listOf("sat", "sun", "mon", "tue", "wed", "thu", "fri").indexOfFirst { t.startsWith(it) }.takeIf { it >= 0 }
}

/** يقبل 14:30 أو 2:30 م أو 2:30 pm أو 9 ص. */
fun parseTime(raw: String): Int? {
    val t = raw.trim().lowercase().replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
        .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
    val m = Regex("^(\\d{1,2})(?:[:.](\\d{2}))?\\s*(ص|م|am|pm)?\$").find(t) ?: return null
    var h = m.groupValues[1].toInt(); val min = m.groupValues[2].ifEmpty { "0" }.toInt()
    when (m.groupValues[3]) {
        "م", "pm" -> if (h < 12) h += 12
        "ص", "am" -> if (h == 12) h = 0
    }
    return if (h in 0..23 && min in 0..59) h * 60 + min else null
}

/**
 * الصيغة: المادة, اليوم, من, إلى, المحاضر, القاعة, الأسابيع (اختياري: كل/فردي/زوجي).
 * الفاصل فاصلة أو فاصلة منقوطة أو Tab. السطر الأول يُتجاهل إن كان عنواناً.
 */
fun parseScheduleCsv(text: String): CsvResult {
    val slots = mutableListOf<CsvSlot>(); val errors = mutableListOf<String>()
    text.removePrefix("﻿").lineSequence().forEachIndexed { idx, raw ->
        val line = raw.trim(); if (line.isEmpty()) return@forEachIndexed
        val sep = when { '\t' in line -> '\t'; ';' in line -> ';'; else -> ',' }
        val f = splitLine(line, sep)
        val n = idx + 1
        if (f.size < 4) { if (idx > 0) errors += "سطر $n: عدد الأعمدة أقل من 4"; return@forEachIndexed }
        val day = parseDay(f[1]); val s = parseTime(f[2]); val e = parseTime(f[3])
        if (day == null || s == null || e == null) { if (idx > 0) errors += "سطر $n: يوم أو وقت غير مفهوم"; return@forEachIndexed } // الأول غالبا عنوان
        if (f[0].isBlank()) { errors += "سطر $n: اسم المادة فارغ"; return@forEachIndexed }
        if (e <= s) { errors += "سطر $n: النهاية قبل البداية"; return@forEachIndexed }
        val mode = f.getOrNull(6)?.let { m -> val k = normalizeArabic(m); when { k.startsWith("فرد") || k == "1" -> 1; k.startsWith("زوج") || k == "2" -> 2; else -> 0 } } ?: 0
        slots += CsvSlot(f[0], day, s, e, f.getOrNull(4).orEmpty(), f.getOrNull(5).orEmpty(), mode)
    }
    return CsvResult(slots, errors)
}

fun slotsToCsv(rows: List<CsvSlot>): String = buildString {
    append("﻿المادة,اليوم,من,إلى,المحاضر,القاعة,الأسابيع\n")
    fun q(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    fun t(m: Int) = "%02d:%02d".format(m / 60, m % 60)
    rows.forEach { append(q(it.title)).append(',').append(DAY_NAMES[it.day]).append(',').append(t(it.startMin)).append(',').append(t(it.endMin)).append(',')
        .append(q(it.instructor)).append(',').append(q(it.room)).append(',').append(listOf("كل", "فردي", "زوجي")[it.weekMode]).append('\n') }
}
