package com.schedule.app.util

import java.time.LocalDate

/** أوامر المساعد المفهومة بالقواعد (بدون نموذج لغوي). */
sealed class Cmd {
    data class AddLecture(val title: String, val day: Int, val start: Int, val end: Int, val room: String) : Cmd()
    data class AddTask(val title: String, val subject: String?, val due: Long?) : Cmd()
    data class AddNote(val text: String, val subject: String?) : Cmd()
    data class AddExam(val title: String, val date: Long) : Cmd()
    data class Cancel(val subject: String, val date: Long) : Cmd()
    data class Agenda(val date: Long) : Cmd()
    data object Next : Cmd()
    data class Search(val query: String) : Cmd()
    data class Summarize(val subject: String?) : Cmd()
    data class Quiz(val subject: String?) : Cmd()
    data class Unknown(val text: String) : Cmd()
}

private val DAY_KEYS = listOf("السبت", "الاحد", "الاثنين", "الثلاثاء", "الاربعاء", "الخميس", "الجمعه").map { normalizeArabic(it) }

/** فهرس اليوم (0 = السبت) إن وُجد اسم يوم في النص، مع موضعه. */
private fun findDay(n: String): Pair<Int, IntRange>? {
    var best: Pair<Int, IntRange>? = null
    DAY_KEYS.forEachIndexed { i, k ->
        val m = Regex("(?<![\\p{L}])" + k + "(?![\\p{L}])").find(n) ?: return@forEachIndexed
        if (best == null || m.range.first < best!!.second.first) best = i to m.range
    }
    // "الاثنين" قد تُكتب "الاتنين" و"الجمعة"... نكتفي بالأشكال الشائعة
    if (best == null) {
        val alt = mapOf("الاتنين" to 2, "التلات" to 3, "الاربع" to 4, "الخميس" to 5)
        alt.forEach { (k, i) -> Regex(k).find(n)?.let { if (best == null) best = i to it.range } }
    }
    return best
}

/** يستخرج تاريخا من النص (بكرة، النهارده، يوم الأحد، 15/12) ويرجع التاريخ مع النص بعد حذف كلمات التاريخ. */
fun extractDate(n: String, today: LocalDate, inclusiveToday: Boolean): Pair<Long?, String> {
    var t = n
    fun cut(r: Regex): Boolean { val m = r.find(t) ?: return false; t = t.removeRange(m.range).replace(Regex("\\s+"), " ").trim(); return true }
    if (cut(Regex("(?<![\\p{L}])بعد (بكره|غدا)(?![\\p{L}])"))) return today.plusDays(2).toEpochDay() to t
    if (cut(Regex("(?<![\\p{L}])(بكره|غدا|بكرا)(?![\\p{L}])"))) return today.plusDays(1).toEpochDay() to t
    if (cut(Regex("(?<![\\p{L}])(النهارده|انهارده|اليوم|النهاردا)(?![\\p{L}])"))) return today.toEpochDay() to t
    Regex("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?").find(t)?.let { m ->
        val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt()
        var y = m.groupValues[3].toIntOrNull() ?: today.year
        if (y < 100) y += 2000
        val date = runCatching { LocalDate.of(y, mo, d) }.getOrNull()
        if (date != null) {
            val fixed = if (m.groupValues[3].isEmpty() && date.isBefore(today)) date.plusYears(1) else date
            t = t.removeRange(m.range).replace(Regex("\\s+"), " ").trim()
            return fixed.toEpochDay() to t
        }
    }
    findDay(t)?.let { (idx, r) ->
        var start = r.first
        // نحذف كلمة "يوم" قبل اسم اليوم
        val before = t.substring(0, start).trimEnd()
        if (before.endsWith("يوم")) start = before.length - 3
        t = (t.substring(0, start) + " " + t.substring(r.last + 1)).replace(Regex("\\s+"), " ").trim()
        var d = if (inclusiveToday) today else today.plusDays(1)
        while (dayIndex(d) != idx) d = d.plusDays(1)
        return d.toEpochDay() to t
    }
    return null to t
}

private fun toMinutes(h: Int, m: Int, pm: Boolean?, morningDefault: Boolean): Int {
    var hh = h
    when (pm) {
        true -> if (hh < 12) hh += 12
        false -> if (hh == 12) hh = 0
        null -> if (!morningDefault && hh in 1..6) hh += 12
    }
    return hh * 60 + m
}

/** "من 10 الى 12" أو "من 1:30 الى 3 م". يرجع (بداية، نهاية) بالدقائق. */
fun extractTimeRange(n: String): Triple<Int, Int, String>? {
    val re = Regex("(?:من|الساعه)?\\s*(\\d{1,2})(?:[:.](\\d{2}))?\\s*(صباحا|مساء|ص|م)?\\s*(?:الي|حتي|-|لحد|لغايه)\\s*(\\d{1,2})(?:[:.](\\d{2}))?\\s*(صباحا|مساء|ص|م)?")
    val m = re.find(n) ?: return null
    val h1 = m.groupValues[1].toInt(); val m1 = m.groupValues[2].ifEmpty { "0" }.toInt()
    val h2 = m.groupValues[4].toInt(); val m2 = m.groupValues[5].ifEmpty { "0" }.toInt()
    if (h1 > 23 || h2 > 23 || m1 > 59 || m2 > 59) return null
    fun pmOf(s: String) = when (s) { "م", "مساء" -> true; "ص", "صباحا" -> false; else -> null }
    val pm1 = pmOf(m.groupValues[3]); val pm2 = pmOf(m.groupValues[6])
    var s = toMinutes(h1, m1, pm1 ?: if (pm2 == true && h1 < h2) true else null, false)
    var e = toMinutes(h2, m2, pm2, false)
    if (e <= s && e < 12 * 60) e += 12 * 60
    if (e <= s) return null
    val rest = n.removeRange(m.range).replace(Regex("\\s+"), " ").trim()
    return Triple(s, e, rest)
}

private val ADD = Regex("^(?:من فضلك |لو سمحت )?(?:ضيف|اضف|اضافه|سجل|زود|عايز اضيف|عاوز اضيف)\\s*")

private fun subjectOf(t: String): Pair<String?, String> {
    val re = Regex("(?:لماده|في ماده|علي ماده|ماده)\\s+([^:]+?)(?=\\s*:|\\s+(?:بكره|غدا|يوم|النهارده|اليوم|بعد|ان|انه|\\d)|\$)")
    val m = re.find(t) ?: return null to t
    val sub = m.groupValues[1].trim()
    return sub to t.removeRange(m.range).replace(Regex("\\s+"), " ").trim()
}

fun parseCommand(raw: String, today: LocalDate): Cmd {
    val n = normalizeArabic(raw).replace("؟", " ").replace("?", " ").replace(Regex("\\s+"), " ").trim()
    if (n.isEmpty()) return Cmd.Unknown(raw)

    // إلغاء محاضرة
    Regex("^(?:الغي|الغ|الغاء|الغيلي)\\s*(?:محاضره|محاضرة)?\\s*(.*)\$").find(n)?.let { m ->
        val (d, rest) = extractDate(m.groupValues[1], today, true)
        val subj = rest.removePrefix("ماده").trim()
        if (subj.isNotEmpty()) return Cmd.Cancel(subj, d ?: today.toEpochDay())
    }
    Regex("^(?:لخص|لخصلي|تلخيص)\\s*(?:ملاحظات|ملاحظاتي)?\\s*(?:ماده)?\\s*(.*)\$").find(n)?.let { return Cmd.Summarize(it.groupValues[1].trim().ifEmpty { null }) }
    if (Regex("^(?:اسئله|اسئلة|اختبرني|امتحني|مراجعه)").containsMatchIn(n)) {
        val subj = n.replace(Regex("^(?:اسئله|اسئلة|اختبرني|امتحني|مراجعه)\\s*(?:مراجعه)?\\s*(?:في|علي|ل)?\\s*(?:ماده)?"), "").trim()
        return Cmd.Quiz(subj.ifEmpty { null })
    }
    Regex("^(?:ابحث|دور|دورلي|فين)\\s*(?:عن|علي|في)?\\s*(.+)\$").find(n)?.let { return Cmd.Search(it.groupValues[1].trim()) }

    val add = ADD.find(n)
    if (add != null) {
        val body = n.substring(add.range.last + 1).trim()
        if (Regex("^(?:امتحان|اختبار|ميد|فاينل)").containsMatchIn(body)) {
            val (d, rest) = extractDate(body.replace(Regex("^(?:امتحان|اختبار)\\s*"), ""), today, false)
            if (d != null && rest.isNotBlank()) return Cmd.AddExam(rest.trim(), d)
            return Cmd.Unknown(raw)
        }
        if (Regex("^(?:واجب|تاسك|مهمه|تكليف)").containsMatchIn(body)) {
            var t = body.replace(Regex("^(?:واجب|تاسك|مهمه|تكليف)\\s*(?:جديد)?\\s*"), "")
            val (subj, afterSubj) = subjectOf(t); t = afterSubj
            val (d, rest) = extractDate(t, today, false)
            val title = rest.removePrefix("ان").trim()
            return if (title.isNotBlank()) Cmd.AddTask(title, subj, d) else Cmd.Unknown(raw)
        }
        if (Regex("^(?:ملاحظه|ملاحظة|نوت)").containsMatchIn(body)) {
            var t = body.replace(Regex("^(?:ملاحظه|ملاحظة|نوت)\\s*"), "")
            val (subj, after) = subjectOf(t); t = after.removePrefix(":").trim()
            return if (t.isNotBlank()) Cmd.AddNote(t, subj) else Cmd.Unknown(raw)
        }
        if (Regex("^(?:محاضره|محاضرة|ماده|سكشن|معمل)").containsMatchIn(body)) {
            val noPrefix = body.replace(Regex("^(?:محاضره|محاضرة|ماده|سكشن|معمل)\\s*"), "")
            val time = extractTimeRange(noPrefix)
            val dayHit = findDay(noPrefix)
            if (time == null || dayHit == null) return Cmd.Unknown(raw)
            val rest0 = time.third
            val (_, afterDay) = extractDate(rest0, today, true)
            var roomText = ""
            var title = afterDay
            Regex("(?:في |ب)?(قاعه|معمل|مدرج|فصل)\\s+(\\S+)").find(title)?.let { m ->
                roomText = m.groupValues[1] + " " + m.groupValues[2]; title = title.removeRange(m.range).trim()
            }
            title = title.replace(Regex("(?<![\\p{L}])(?:يوم|من|في)(?![\\p{L}])"), " ").replace(Regex("\\s+"), " ").trim()
            return if (title.isNotBlank()) Cmd.AddLecture(title, dayHit.first, time.first, time.second, roomText) else Cmd.Unknown(raw)
        }
    }
    if (Regex("(?:الجايه|القادمه|التاليه|الجاي|القادم)").containsMatchIn(n) && n.contains("محاضر")) return Cmd.Next
    if (Regex("(?:ايه|اي|ايش|ما هي|عندي)").containsMatchIn(n) && Regex("(?:محاضر|جدول|عندي)").containsMatchIn(n)) {
        val (d, _) = extractDate(n, today, true)
        return Cmd.Agenda(d ?: today.toEpochDay())
    }
    return Cmd.Unknown(raw)
}

/** أقرب مادة بالاسم (تطابق جزئي بعد التوحيد). */
fun matchSubject(query: String, titles: List<String>): String? {
    val q = normalizeArabic(query).removePrefix("ماده").trim().removePrefix("ال")
    if (q.isEmpty()) return null
    titles.firstOrNull { normalizeArabic(it) == normalizeArabic(query) }?.let { return it }
    return titles.firstOrNull { val t = normalizeArabic(it).removePrefix("ال"); t.contains(q) || q.contains(t) }
}
