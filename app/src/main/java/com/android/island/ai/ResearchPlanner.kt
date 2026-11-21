package com.android.island.ai

import java.util.Locale

/**
 * Plans "cari lebih dalam" (deep research) requests:
 *  - [isDeepResearchQuery] detects when the user asks for depth, so the request skips the fast
 *    grounded-news path and runs through the full agent loop with a bigger budget.
 *  - [variants] produces sharpened follow-up queries for the later rounds of a research task.
 *  - The research budget constants extend AgentLoopPolicy's defaults: deep research legally needs
 *    more steps (search rounds + page reads), while ordinary tasks keep the fast budget.
 * Pure logic, no Android types, so it is unit tested.
 */
object ResearchPlanner {

    /** Extended loop budget while a deep research task is running. */
    const val RESEARCH_MAX_STEPS = 26
    const val RESEARCH_MAX_MS = 300_000L

    /** Pages read in full during one deep research action (each read is time-boxed by PageReader). */
    const val RESEARCH_MAX_PAGES = 3

    private val DEEP_HINTS = listOf(
        "lebih dalam", "mendalam", "secara dalam", "gali", "telusuri", "teltah", "susuri",
        "usut", "riset", "research", "deep dive", "kronologi", "detail lengkap",
        "cari tahu lebih", "cari lebih", "periksa lebih", "sejarah lengkap", "fakta lengkap"
    )

    fun isDeepResearchQuery(text: String): Boolean {
        val t = text.lowercase(Locale.ROOT)
        return DEEP_HINTS.any { it in t }
    }

    /** Filler removed before the remainder becomes the research core keywords. */
    private val FILLER = setOf(
        "cari", "carikan", "cariin", "tolong", "berita", "terbaru", "terkini", "lebih", "dalam",
        "mendalam", "gali", "telusuri", "riset", "research", "tentang", "soal", "mengenai",
        "apa", "yang", "dan", "untuk", "secara", "lengkap", "detail", "detil", "kronologi",
        "usut", "periksa", "tahu", "today", "latest", "news", "please", "find", "deep", "dive",
        "search", "about", "hari", "ini", "itu", "aja", "saja", "dong", "dengan", "dari",
        "the", "of", "and", "for", "please"
    )

    /** Core content words of the request; blank when the request had none (e.g. only filler). */
    fun coreTerms(query: String): List<String> = Regex("[\\p{L}\\p{N}]{3,}")
        .findAll(query.lowercase(Locale.ROOT))
        .map { it.value }
        .filterNot { it in FILLER }
        .distinct()
        .take(6)
        .toList()

    /**
     * Sharpened follow-up queries for rounds 2..3 of a deep research task. Each variant angles at
     * a different evidence type (chronology, official explanation, verified data) instead of
     * repeating the same query the base round already used.
     */
    fun variants(query: String): List<String> {
        val core = coreTerms(query).joinToString(" ")
        if (core.isBlank()) return emptyList()
        return listOf(
            "$core kronologi fakta",
            "$core penjelasan resmi",
            "$core data terverifikasi"
        )
    }
}
