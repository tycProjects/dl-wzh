package com.android.island.ai

import java.util.Locale

/**
 * Recognises a plain-language phone-control request ("buka whatsapp lalu buka kontak ummi",
 * "cari di google ...") without needing the explicit ". " marker, so such a request reaches the
 * agent instead of the ordinary chat model (which used to answer with a Python "simulation").
 * Deliberately conservative: questions, explanations and writing/code requests stay chat.
 * Pure logic, unit tested.
 */
object AgentIntentClassifier {
    private val POLITE = setOf(
        "tolong", "mohon", "please", "pls", "coba", "bisa", "silakan", "ayo", "yuk", "aku", "saya", "mau",
        "bantu", "dong", "ya", "bukakan", "tlg"
    )

    /** Verbs that unambiguously mean "do something on the phone". */
    private val DEVICE_VERBS = setOf(
        "buka", "bukain", "bukakan", "jalankan", "open", "launch",
        "klik", "click", "tekan", "ketuk", "tap", "scroll", "gulir", "swipe", "geser",
        "nyalakan", "nyalain", "hidupkan", "aktifkan", "matikan", "matiin", "nonaktifkan",
        "putar", "mainkan", "play", "pause", "jeda", "lanjutkan",
        "screenshot", "tahan", "seret", "ketik"
    )

    private val SEARCH_VERBS = setOf("cari", "carikan", "cariin", "search", "telusuri")

    /** Writing / coding / explaining requests are never phone control, whatever verb they use. */
    private val CONTENT_WORDS = Regex(
        "\\b(script|skrip|kode|code|coding|program|python|javascript|kotlin|html|css|java|artikel|esai|puisi|cerita|" +
            "email|surat|terjemahkan|terjemah|rangkum|ringkas|jelaskan|jelasin|tuliskan|tulis|buatkan|bikinkan|contoh|" +
            "rumus|resep|caption)\\b",
        RegexOption.IGNORE_CASE
    )

    private val APP_TARGET = Regex(
        "\\b(?:di|pada|lewat|via|dari)\\s+(google|chrome|youtube|tiktok|instagram|whatsapp|wa|spotify|google maps|maps|" +
            "play store|shopee|tokopedia|lazada|facebook|twitter|telegram|pinterest|gmail|netflix|gojek|grab|traveloka|bukalapak)\\b",
        RegexOption.IGNORE_CASE
    )

    private val CHAIN = Regex("\\b(lalu|terus|kemudian|setelah itu|abis itu|habis itu)\\b", RegexOption.IGNORE_CASE)

    private val CHAT_ESCAPES = listOf("/chat ", "chat: ", "/tanya ")

    /** Text after a chat escape such as "/chat ", or null when the message has none. */
    fun stripChatEscape(text: String): String? {
        val t = text.trim()
        val lower = t.lowercase(Locale.ROOT)
        val prefix = CHAT_ESCAPES.firstOrNull { lower.startsWith(it) } ?: return null
        return t.drop(prefix.length).trim()
    }

    fun hasChain(text: String): Boolean = CHAIN.containsMatchIn(text)

    private fun firstVerb(tokens: List<String>): String? {
        var i = 0
        while (i < tokens.size && i < 5 && tokens[i] in POLITE) i++
        return tokens.getOrNull(i)
    }

    fun isDeviceCommand(text: String): Boolean {
        val t = text.trim()
        if (t.length !in 3..400 || t.endsWith("?") || t.endsWith("？")) return false
        if (CONTENT_WORDS.containsMatchIn(t)) return false
        if (!IntentResolver.looksLikeCommand(t)) return false
        val tokens = t.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() }
        val verb = firstVerb(tokens) ?: return false
        return when (verb) {
            in DEVICE_VERBS -> true
            in SEARCH_VERBS -> APP_TARGET.containsMatchIn(t) || hasChain(t)
            else -> false
        }
    }

    /** "cari di google X" / "cari X di youtube": one search with no follow-up steps. */
    fun isSingleStepSearch(text: String): Boolean {
        val t = text.trim()
        if (hasChain(t) || AgentTaskParser.requestsFirstResult(t)) return false
        val verb = firstVerb(t.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotBlank() })
        return verb in SEARCH_VERBS && APP_TARGET.containsMatchIn(t)
    }
}
