package com.android.island.ai

/**
 * Chooses a model from the list a provider returns, without hardcoding names that go stale.
 * Pure logic, unit tested.
 */
object ModelPicker {

    /** Stable Gemini Flash models, e.g. gemini-2.5-flash. Excludes lite and other variants by shape. */
    val GEMINI_FLASH = Regex("""^gemini-(\d+(?:\.\d+)*)-flash$""")

    /** Stable GPT Sol models (flagship tier in the OpenAI changelog, e.g. gpt-5.6-sol, Aug 2026). */
    val GPT_SOL = Regex("""^gpt-(\d+(?:\.\d+)*)-sol$""")

    /** GPT Sol models with or without a vendor prefix (OpenAI "gpt-6.1-sol", OpenRouter "openai/gpt-6.1-sol"). */
    val GPT_SOL_ANY = Regex("""(?:^|/)gpt-(\d+(?:\.\d+)*)-sol$""")

    /** Ids that providers list in /models but that cannot do plain text chat. */
    private val NON_CHAT = listOf(
        "whisper", "prompt-guard", "llama-guard", "safeguard", "embedding", "embed", "tts", "speech",
        "transcri", "moderation", "dall-e", "image", "sora", "realtime", "audio", "davinci", "babbage",
        "rerank", "search-preview", "ocr"
    )

    fun isChatModel(id: String): Boolean = id.lowercase().let { lower -> NON_CHAT.none { lower.contains(it) } }

    /**
     * The model to use when the user has none yet or the saved one no longer exists. Always chosen from
     * the provider's own live list; never a name that is not in [ids]. [familyRegex] picks the newest
     * member of a family when the provider has one.
     */
    fun pickDefault(provider: String, baseUrl: String, ids: List<String>, familyRegex: Regex? = null): String? {
        if (provider == "gemini") return pickGeminiChat(ids)
        val chat = ids.filter(::isChatModel).ifEmpty { ids }
        val picked = when {
            provider == "deepseek" || baseUrl.contains("api.deepseek.com", ignoreCase = true) ->
                listOf("deepseek-flash", "deepseek-v4-pro").firstOrNull { it in chat }
            baseUrl.contains("groq.com", ignoreCase = true) ->
                listOf("openai/gpt-oss-120b", "openai/gpt-oss-20b", "llama-3.3-70b-versatile").firstOrNull { it in chat }
            else -> familyRegex?.let { pickChat(chat, it) }
                ?: pickChat(chat, GPT_SOL_ANY)
                ?: chat.firstOrNull { it.contains("gpt-oss", ignoreCase = true) }
        }
        return picked ?: chat.firstOrNull()
    }

    /** Variants that are never auto-picked for ordinary chat. */
    private val EXCLUDED = listOf("-preview", "-live", "-tts", "-image", "-embedding", "-transcribe")

    /** Chat preference is efficiency-first; each candidate is still intersected with the live API catalog.
     *  Only model IDs that really exist on Google AI Studio are listed here; newer releases are
     *  still picked automatically by the version regex below. */
    val GEMINI_CHAT_PRIORITY = listOf(
        // Lite first: cheapest and lightest. The default is gemini-3.5-flash-lite (user choice).
        "gemini-3.5-flash-lite", "gemini-3.1-flash-lite",
        // Full Flash models only when no Lite model is exposed, newest first.
        "gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash",
        // Legacy models remain last-resort options only when newer models are not exposed.
        "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.5-pro"
    )

    /** Gemini image models, preferred newest/effective first; availability is always checked live. */
    val GEMINI_IMAGE_CATALOG = listOf(
        "gemini-2.5-flash-image", "gemini-2.0-flash-preview-image-generation"
    )

    /** Image models for providers with OpenAI-compatible image-generation endpoints. */
    val OPENAI_IMAGE_CATALOG = listOf("gpt-image-1", "gpt-image-1-mini")
    val IMAGE_CATALOG = OPENAI_IMAGE_CATALOG + GEMINI_IMAGE_CATALOG

    /** Highest version among ids that match [familyRegex], ignoring excluded variants. */
    fun pickChat(ids: List<String>, familyRegex: Regex): String? =
        ids.filter { id -> familyRegex.containsMatchIn(id) && EXCLUDED.none { id.contains(it) } }
            .maxWithOrNull(Comparator { a, b -> compareVersions(versionOf(a, familyRegex), versionOf(b, familyRegex)) })

    /** First catalog model the provider offers, in priority order. Null when none is available. */
    fun pickImage(ids: List<String>): String? = IMAGE_CATALOG.firstOrNull { it in ids }

    fun pickGeminiChat(ids: List<String>): String? =
        GEMINI_CHAT_PRIORITY.firstOrNull { it in ids }
            ?: pickChat(ids, GEMINI_FLASH)
            ?: ids.firstOrNull { AiClient.isUsableGeminiChat(it) }

    fun pickGeminiImage(ids: List<String>): String? = GEMINI_IMAGE_CATALOG.firstOrNull { it in ids }

    internal fun versionOf(id: String, regex: Regex): List<Int> =
        regex.find(id)?.groupValues?.getOrNull(1)
            ?.split('.')?.mapNotNull { it.toIntOrNull() }
            ?: emptyList()

    /** Compares dotted versions numerically, so 3.10 is newer than 3.9. */
    fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
