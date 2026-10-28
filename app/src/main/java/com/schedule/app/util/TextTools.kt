package com.schedule.app.util

import kotlin.math.ln
import kotlin.math.sqrt

private val STOP = setOf("في", "من", "علي", "الي", "عن", "ان", "هو", "هي", "ما", "هذا", "هذه", "ذلك", "التي", "الذي", "او", "ثم", "كان", "كل", "مع", "هل", "قد", "لا", "لم", "بين", "كما", "عند", "اذا", "ذات", "تم", "و", "ب", "ل", "اي", "فقط", "the", "of", "and", "to", "a", "is", "in")

fun tokenize(s: String): List<String> =
    Regex("[\\p{L}\\p{N}]+").findAll(normalizeArabic(s)).map { it.value }.map { w ->
        when {
            w.length > 5 && w.startsWith("وال") -> w.substring(3)
            w.length > 4 && (w.startsWith("بال") || w.startsWith("لل") || w.startsWith("كال")) -> w.substring(2)
            w.length > 4 && w.startsWith("ال") -> w.substring(2)
            else -> w
        }
    }.filter { it !in STOP && it.length > 1 }.toList()

fun sentences(text: String): List<String> = text.split(Regex("[.!?؟\\n]+")).map { it.trim() }.filter { it.length > 3 }

/** تلخيص استخلاصي: أهم الجمل بحسب تكرار الكلمات (بدون نموذج لغوي). */
fun summarize(text: String, maxSentences: Int = 3): List<String> {
    val ss = sentences(text)
    if (ss.size <= maxSentences) return ss
    val freq = HashMap<String, Int>()
    ss.forEach { s -> tokenize(s).forEach { freq[it] = (freq[it] ?: 0) + 1 } }
    val scored = ss.mapIndexed { i, s ->
        val t = tokenize(s)
        i to (if (t.isEmpty()) 0.0 else t.sumOf { (freq[it] ?: 0).toDouble() } / sqrt(t.size.toDouble()))
    }
    return scored.sortedByDescending { it.second }.take(maxSentences).map { it.first }.sorted().map { ss[it] }
}

/** أسئلة مراجعة بحذف أهم كلمة في كل جملة: (السؤال، الإجابة). */
fun makeQuestions(text: String, max: Int = 5): List<Pair<String, String>> {
    val freq = HashMap<String, Int>()
    sentences(text).forEach { s -> tokenize(s).forEach { freq[it] = (freq[it] ?: 0) + 1 } }
    return sentences(text).mapNotNull { s ->
        val words = s.split(Regex("\\s+"))
        if (words.size < 5) return@mapNotNull null
        val key = words.filter { w -> tokenize(w).firstOrNull()?.let { it.length >= 4 } == true }
            .maxByOrNull { w -> (freq[tokenize(w).first()] ?: 0) * 10 + w.length } ?: return@mapNotNull null
        s.replaceFirst(key, "..........") to key.trim('،', ',', ':', '(', ')')
    }.take(max)
}

/** ترتيب مستندات بتشابه TF-IDF مع الاستعلام (بحث تقريبي بالمعنى اللفظي، وليس تضمينات حقيقية). */
fun tfidfRank(query: String, docs: List<Pair<Long, String>>, top: Int = 8): List<Pair<Long, Double>> {
    val q = tokenize(query); if (q.isEmpty() || docs.isEmpty()) return emptyList()
    val toks = docs.map { it.first to tokenize(it.second) }
    val df = HashMap<String, Int>()
    toks.forEach { (_, t) -> t.toSet().forEach { df[it] = (df[it] ?: 0) + 1 } }
    val n = docs.size.toDouble()
    fun vec(t: List<String>): Map<String, Double> = t.groupingBy { it }.eachCount().mapValues { (w, c) -> c * (ln((n + 1) / ((df[w] ?: 0) + 1)) + 1.0) }
    val qv = vec(q); val qn = sqrt(qv.values.sumOf { it * it })
    return toks.mapNotNull { (id, t) ->
        val dv = vec(t); val dn = sqrt(dv.values.sumOf { it * it }); if (dn == 0.0) return@mapNotNull null
        // تطابق بالجذر: بادئة مشتركة تزيد التطابق
        var dot = 0.0
        qv.forEach { (w, x) ->
            dv[w]?.let { dot += x * it } ?: dv.entries.firstOrNull { it.key.length > 3 && w.length > 3 && (it.key.startsWith(w) || w.startsWith(it.key)) }?.let { dot += x * it.value * 0.6 }
        }
        val score = dot / (qn * dn)
        if (score > 0.01) id to score else null
    }.sortedByDescending { it.second }.take(top)
}

// ---------- نص منسق خفيف ----------
data class RichSpan(val text: String, val bold: Boolean)
data class RichLine(val kind: Int, val spans: List<RichSpan>) // 0 عادي، 1 عنوان، 2 نقطة، 3 مهمة، 4 مهمة منجزة

fun parseRich(text: String): List<RichLine> = text.lines().map { raw ->
    var kind = 0; var line = raw
    when {
        raw.startsWith("# ") -> { kind = 1; line = raw.substring(2) }
        raw.startsWith("- [x] ") -> { kind = 4; line = raw.substring(6) }
        raw.startsWith("- [ ] ") -> { kind = 3; line = raw.substring(6) }
        raw.startsWith("- ") -> { kind = 2; line = raw.substring(2) }
    }
    val spans = ArrayList<RichSpan>()
    val parts = line.split("**")
    parts.forEachIndexed { i, p -> if (p.isNotEmpty()) spans += RichSpan(p, i % 2 == 1 && parts.size % 2 == 1) else if (false) Unit }
    RichLine(kind, if (spans.isEmpty()) listOf(RichSpan("", false)) else spans)
}

fun parseTags(s: String): List<String> = s.split(',', '،', '#', ' ', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
