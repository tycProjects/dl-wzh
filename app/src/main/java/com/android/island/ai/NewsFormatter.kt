package com.android.island.ai

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Deterministic cleanup for RSS news results shown directly to the user (no model rewrite, so
 * nothing can be invented):
 *  - GMT/UTC publisher times become WIB (GMT+7, Asia/Jakarta).
 *  - Residual raw HTML tags/entities are stripped from every field.
 *  - Duplicate headlines are removed: same normalized title, a trailing "- Media" copy of the
 *    publisher name, or a near-identical syndicated headline of the same story.
 *  - Long URLs are never printed visibly; each item ends with a short "[Baca Sumber](url)" link
 *    the chat renderer turns into a tappable label.
 * Pure Kotlin without Android types so the behaviour is unit tested.
 */
internal object NewsFormatter {

    val WIB: ZoneId = ZoneId.of("Asia/Jakarta")
    private val DISPLAY = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("id"))

    /** One cleaned item for the numbered user-facing list. */
    data class Item(
        val title: String,
        val media: String,
        val url: String,
        val epochMs: Long
    )

    /** "20 Nov 2026, 15:13 WIB" from an epoch millisecond value. */
    fun wibLabel(epochMs: Long): String =
        "${Instant.ofEpochMilli(epochMs).atZone(WIB).format(DISPLAY)} WIB"

    /** Maps a publisher RSS article onto the cleaned user-facing item. */
    fun from(a: NoozraNews.Article): Item = Item(
        title = clean(a.title),
        media = clean(a.source),
        url = a.url.trim(),
        epochMs = a.publishedEpoch
    )

    /**
     * The exact numbered format requested for the news panel:
     * `1. Judul — Media (20 Nov 2026, 15:13 WIB) | Baca Sumber`
     * Newest first, duplicates collapsed, no raw HTML, no visible long URL.
     */
    fun formatUserList(items: List<Item>, maxItems: Int = 10): String {
        if (items.isEmpty()) return ""
        val seen = LinkedHashSet<String>()
        val out = mutableListOf<String>()
        for (raw in items.sortedByDescending { it.epochMs }) {
            if (out.size >= maxItems.coerceAtLeast(1)) break
            val title = clean(raw.title)
            if (title.isBlank()) continue
            val key = dedupKey(title)
            if (key.isBlank() || key in seen) continue
            if (seen.any { nearDuplicate(key, it) }) continue
            seen.add(key)
            val media = clean(raw.media)
            val mediaText = if (media.isBlank()) "" else " — $media"
            val whenText = if (raw.epochMs > 0L) " (${wibLabel(raw.epochMs)})" else ""
            val link = if (raw.url.startsWith("https://")) " | [Baca Sumber](${raw.url})" else ""
            out += "${out.size + 1}. ${stripSourceSuffix(title, media)}$mediaText$whenText$link"
        }
        return out.joinToString("\n")
    }

    /**
     * Full grounded-news reply: a one-line header with the WIB retrieval time, then the clean
     * numbered list, then the scope note. When nothing verifiable was retrieved, the existing
     * honest failure text is returned unchanged so no fabricated roundup can appear.
     */
    fun renderGrounded(query: String, bundle: WebSearch.NewsBundle): String {
        if (bundle.articles.isEmpty()) return bundle.summary
        val list = formatUserList(bundle.articles.map { from(it) })
        if (list.isBlank()) return bundle.summary
        return buildString {
            append("Berita terverifikasi dari feed penerbit (waktu WIB)")
            if (bundle.query.isNotBlank()) append(" — ${clean(bundle.query)}")
            append(":\n\n")
            append(list)
            append("\n\nSumber: RSS penerbit asli; tanggal/jam dikonversi ke WIB (GMT+7). Ketuk \"Baca Sumber\" untuk membuka artikel.")
        }
    }

    /** Removes markdown link labels so text-to-speech does not read raw URLs. */
    fun stripLinksForSpeech(text: String): String =
        text.replace(Regex("\\[[^\\]]*]\\(https?://[^)\\s]+\\)"), "")
            .replace(Regex("\\s+\\|\\s*"), " ")
            .trim()

    /** Strips tags/entities/extra whitespace; the last defense against raw HTML in the UI. */
    fun clean(value: String): String = value
        .replace(Regex("(?is)<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("&#x([0-9a-fA-F]+);")) { m -> m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value }
        .replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value }
        .replace(Regex("\\s+"), " ")
        .trim()
        .let { d -> if (d.length <= 220) d else d.take(220).substringBeforeLast(' ').trimEnd(',', ';', ':', '-') + "…" }

    /** Drops a trailing "- Media" / "| Media" suffix when it repeats the publisher name. */
    private fun stripSourceSuffix(title: String, media: String): String {
        if (media.isBlank()) return title
        val escaped = Regex.escape(media)
        return title.replace(Regex("(?i)\\s*[-–—|]\\s*$escaped\\s*\$"), "").trim()
    }

    private fun dedupKey(title: String): String {
        // A syndicated copy often appends the publisher after a dash; compare without it.
        val withoutSuffix = title.replace(Regex("(?i)\\s*[-–—|]\\s*\\S+\\s*\$"), "")
        val candidates = listOf(title, withoutSuffix)
        return candidates.maxByOrNull { it.length }?.let {
            it.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
        }.orEmpty()
    }

    /** Conservative near-duplicate test: only merges long, almost-identical headline word sets. */
    private fun nearDuplicate(a: String, b: String): Boolean {
        if (a == b) return true
        val wa = a.split(' ').filter { it.isNotBlank() }
        val wb = b.split(' ').filter { it.isNotBlank() }
        if (wa.size < 5 || wb.size < 5) return false
        val setA = wa.toSet()
        val setB = wb.toSet()
        val overlap = setA.intersect(setB).size
        val union = setA.union(setB).size
        return union > 0 && overlap.toFloat() / union >= 0.85f
    }
}
