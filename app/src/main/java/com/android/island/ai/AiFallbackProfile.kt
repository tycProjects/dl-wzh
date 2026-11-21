package com.android.island.ai

/** Explicitly configured cross-provider fallback. No provider or model is guessed. */
data class AiFallbackProfile(
    val provider: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String
) {
    fun endpoint(primary: AiEndpoint): AiEndpoint = AiEndpoint(
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        temperature = primary.temperature,
        maxTokens = primary.maxTokens,
        streaming = primary.streaming,
        provider = provider,
        allowTools = primary.allowTools
    )

    companion object {
        const val MAX_PROFILES = 3

        /** Format per line: provider|baseUrl|model|apiKey. */
        fun parse(raw: String): List<AiFallbackProfile> = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val fields = line.split('|', limit = 4).map { it.trim() }
                if (fields.size != 4) return@mapNotNull null
                val provider = fields[0].lowercase()
                val base = fields[1]
                val model = fields[2]
                val key = fields[3]
                // AiClient supports Gemini's adapter plus arbitrary OpenAI-compatible endpoints.
                // Do not reject valid providers such as DeepSeek, OpenRouter, Mistral, Together,
                // Fireworks, Cerebras, xAI, or a user's own compatible server.
                val compatibleProviders = setOf(
                    "gemini", "openai", "groq", "deepseek", "openrouter", "mistral",
                    "together", "fireworks", "cerebras", "xai", "ollama", "lmstudio",
                    "openai-compatible", "custom"
                )
                if (provider !in compatibleProviders ||
                    !(base.startsWith("https://") || base.startsWith("http://")) ||
                    model.isBlank() || (key.isBlank() && provider != "ollama")) return@mapNotNull null
                runCatching { AiClient.canonicalBaseUrl(provider, base) }.getOrNull()?.let {
                    AiFallbackProfile(provider, it, model, key)
                }
            }
            .distinctBy { "${it.provider}|${it.baseUrl}|${it.model}|${it.apiKey}" }
            .take(MAX_PROFILES)
            .toList()
    }
}
