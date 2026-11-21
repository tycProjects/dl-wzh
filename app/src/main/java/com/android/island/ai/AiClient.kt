package com.android.island.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal OpenAI-compatible chat-completions client.
 *
 * Deliberately built on [HttpURLConnection] + org.json instead of an HTTP SDK: the AI
 * runs only while the expanded island chat is open, so the whole client must add zero
 * classes beyond this file and no extra dependencies (RAM budget of the island).
 * Works with OpenAI, OpenRouter, Groq, Together, Ollama / LM Studio (local) and any
 * other endpoint implementing POST {baseUrl}/chat/completions.
 */

/** One content part of a request message: plain text or a base64-encoded JPEG image. */
sealed class AiPart {
    data class Text(val text: String) : AiPart()
    data class ImageData(val base64Jpeg: String) : AiPart()
}

/** A single chat-completions message. role: "system" | "user" | "assistant". */
data class ApiChatMessage(val role: String, val parts: List<AiPart>) {
    companion object {
        fun system(text: String) = ApiChatMessage("system", listOf(AiPart.Text(text)))
        fun user(text: String) = ApiChatMessage("user", listOf(AiPart.Text(text)))
        fun assistant(text: String) = ApiChatMessage("assistant", listOf(AiPart.Text(text)))
    }
}

/** Endpoint + sampling configuration snapshot taken when a request starts. */
data class AiEndpoint(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val temperature: Float,
    val maxTokens: Int,
    val streaming: Boolean,
    /** Provider adapter identity; Gemini uses Google's OpenAI-compatible Chat Completions endpoint. */
    val provider: String = "openai",
    /** Native tool schema is only for the explicit agent mode; ordinary chat never sends it. */
    val allowTools: Boolean = true
)

/** HTTP failure with an already human-readable message (Indonesian). */
class AiHttpException(val httpStatus: Int, message: String) : Exception(message)

/** Model metadata returned by Google's live models.list endpoint. */
data class GeminiModelDescriptor(
    val id: String,
    val displayName: String,
    val supportedMethods: Set<String>,
    val inputTokenLimit: Int?,
    val outputTokenLimit: Int?
)

/** Default Gemini model: the Lite tier is the cheapest and fastest. Actual selection is intersected with the live catalog. */
private const val GEMINI_DEFAULT_MODEL = "gemini-3.5-flash-lite"

object AiClient {

    private const val CONNECT_TIMEOUT_MS = 8_000
    // Long generations on slow providers/local models can legally take minutes.
    private const val READ_TIMEOUT_MS = 60_000
    private data class GeminiCatalogCache(val cacheKey: String, val fetchedAtMs: Long, val models: List<GeminiModelDescriptor>)
    @Volatile private var geminiCatalogCache: GeminiCatalogCache? = null

    /** Reuse a verified model catalog briefly so normal chat does not pay an extra models.list RTT. */
    private suspend fun cachedGeminiCatalog(baseUrl: String, apiKey: String): List<GeminiModelDescriptor> {
        val cacheKey = "${baseUrl.trim()}#${apiKey.hashCode()}"
        val now = android.os.SystemClock.elapsedRealtime()
        geminiCatalogCache?.let { cached ->
            if (cached.cacheKey == cacheKey && now - cached.fetchedAtMs < 10 * 60 * 1000L) return cached.models
        }
        val fresh = discoverGeminiCatalog(baseUrl, apiKey)
        geminiCatalogCache = GeminiCatalogCache(cacheKey, now, fresh)
        return fresh
    }

    /** Normalize a user-entered base URL or full endpoint to the provider API root. */
    fun normalizeApiRoot(input: String): String {
        var value = input.trim().trimEnd('/')
        require(value.startsWith("https://") || value.startsWith("http://")) {
            "URL harus diawali https:// atau http://"
        }
        val suffixes = listOf("/chat/completions", "/models", "/openai/v1/chat/completions")
        suffixes.forEach { suffix ->
            if (value.endsWith(suffix, ignoreCase = true)) value = value.dropLast(suffix.length).trimEnd('/')
        }
        // Collapse accidental double path segments left by partial pastes.
        value = value.replace(Regex("/v1/v1\$", RegexOption.IGNORE_CASE), "/v1")
        return value
    }

    /**
     * Canonical **ready-to-use** endpoint stored in settings / shown in the UI.
     *
     * OpenAI-compatible providers get the full chat-completions URL so the field
     * is immediately usable, e.g. https://api.groq.com/openai/v1/chat/completions.
     * Gemini uses Google's OpenAI-compatible endpoint (/v1beta/openai/chat/completions).
     *
     * Request code always runs the value through [normalizeApiRoot] first (strips
     * /chat/completions or /models), then appends the correct path again.
     */
    fun canonicalBaseUrl(provider: String, input: String): String {
        val raw = input.trim().ifBlank {
            return when (provider) {
                "gemini" -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
                "groq" -> "https://api.groq.com/openai/v1/chat/completions"
                "deepseek" -> "https://api.deepseek.com/chat/completions"
                else -> "https://api.openai.com/v1/chat/completions"
            }
        }
        val rawClean = raw.trimEnd('/')
        val normalized = runCatching { normalizeApiRoot(rawClean) }.getOrElse { rawClean }
        val lower = normalized.lowercase()
        val hostRoot = runCatching { java.net.URI(normalized).host.orEmpty().lowercase() }.getOrDefault("")
        val customPath = { "$normalized/chat/completions" }

        // A complete endpoint is user-authored configuration: retain it verbatim, except
        // DeepSeek's obsolete /v1 route which is intentionally migrated to its current root.
        if (rawClean.endsWith("/chat/completions", true) &&
            !(hostRoot == "api.deepseek.com" && normalized.endsWith("/v1", true))) return rawClean

        return when {
            hostRoot == "generativelanguage.googleapis.com" -> when {
                normalized.endsWith("/v1beta/openai", true) -> "$normalized/chat/completions"
                normalized.endsWith("/v1beta", true) -> "$normalized/openai/chat/completions"
                normalized == "https://generativelanguage.googleapis.com" -> "$normalized/v1beta/openai/chat/completions"
                else -> customPath()
            }
            hostRoot == "api.groq.com" -> when {
                normalized == "https://api.groq.com" -> "https://api.groq.com/openai/v1/chat/completions"
                normalized.endsWith("/openai/v1", true) -> "$normalized/chat/completions"
                else -> customPath()
            }
            hostRoot == "openrouter.ai" -> when {
                normalized == "https://openrouter.ai" -> "https://openrouter.ai/api/v1/chat/completions"
                normalized.endsWith("/api/v1", true) -> "$normalized/chat/completions"
                else -> customPath()
            }
            hostRoot == "api.openai.com" -> when {
                normalized == "https://api.openai.com" -> "https://api.openai.com/v1/chat/completions"
                normalized.endsWith("/v1", true) -> "$normalized/chat/completions"
                else -> customPath()
            }
            hostRoot == "api.deepseek.com" -> when {
                normalized == "https://api.deepseek.com" || normalized.endsWith("/v1", true) ->
                    "https://api.deepseek.com/chat/completions"
                else -> customPath()
            }
            hostRoot == "api.mistral.ai" -> when {
                normalized == "https://api.mistral.ai" -> "https://api.mistral.ai/v1/chat/completions"
                normalized.endsWith("/v1", true) -> "$normalized/chat/completions"
                else -> customPath()
            }
            hostRoot == "api.together.ai" || hostRoot == "api.together.xyz" -> when {
                normalized == "https://api.together.ai" || normalized == "https://api.together.xyz" ->
                    "https://api.together.ai/v1/chat/completions"
                normalized.endsWith("/v1", true) -> "$normalized/chat/completions"
                else -> customPath()
            }
            hostRoot == "api.fireworks.ai" -> when {
                normalized == "https://api.fireworks.ai" -> "https://api.fireworks.ai/inference/v1/chat/completions"
                normalized.endsWith("/inference/v1", true) -> "$normalized/chat/completions"
                else -> customPath()
            }
            hostRoot == "api.cerebras.ai" -> when {
                normalized == "https://api.cerebras.ai" -> "https://api.cerebras.ai/v1/chat/completions"
                normalized.endsWith("/v1", true) -> "$normalized/chat/completions"
                else -> customPath()
            }
            hostRoot == "api.x.ai" -> when {
                normalized == "https://api.x.ai" -> "https://api.x.ai/v1/chat/completions"
                normalized.endsWith("/v1", true) -> "$normalized/chat/completions"
                else -> customPath()
            }
            hostRoot == "127.0.0.1" || hostRoot == "localhost" -> {
                if (normalized.endsWith("/v1", true) || normalized.contains("/v1/")) customPath()
                else "$normalized/v1/chat/completions"
            }
            else -> customPath()
        }
    }

    /**
     * Preserve a manually edited Base URL when provider auto-detection or provider chips change.
     * A URL is considered preset only when it still matches the current provider's shipped default.
     */
    fun baseUrlForProviderChange(currentBaseUrl: String, currentPresetBaseUrl: String?, nextProvider: String, nextPresetUrl: String): String {
        val current = currentBaseUrl.trimEnd('/')
        val currentPreset = currentPresetBaseUrl?.let { canonicalBaseUrl("openai", it).trimEnd('/') }
        val isManual = current.isNotBlank() && (currentPreset == null || !current.equals(currentPreset, ignoreCase = true))
        return if (isManual) currentBaseUrl else canonicalBaseUrl(nextProvider, nextPresetUrl)
    }

    /** Fetch model IDs from a compatible /models endpoint or Gemini's native model catalog. */
    /**
     * Gemini models the app offers: generation 3.5 and newer (and 4+), text chat only.
     * Older 2.x and 3.0-3.4 models are retired or limited; image, audio, live and embedding are not chat models.
     */
    /** Gemini chat options: any text or chat model of any version. Media, audio, embedding and agent models are excluded. */
    internal fun isUsableGeminiChat(id: String): Boolean {
        val lower = id.lowercase().removePrefix("models/")
        val nonChat = listOf(
            "tts", "image", "live", "embed", "embedding", "robotics", "computer-use", "nano-banana", "omni",
            "transcribe", "audio", "native", "lyria", "veo", "imagen", "deep-research", "antigravity", "translate", "decision"
        )
        return (lower.startsWith("gemini-") || lower.startsWith("gemma-")) && nonChat.none { lower.contains(it) }
    }

    /** True only for Google's first-party Gemini Developer API URL, not a user's custom gateway. */
    internal fun usesGoogleGeminiNativeCatalog(baseUrl: String): Boolean = runCatching {
        val root = normalizeApiRoot(canonicalBaseUrl("gemini", baseUrl))
        val uri = java.net.URI(root)
        uri.scheme.equals("https", true) &&
            uri.host.equals("generativelanguage.googleapis.com", true) &&
            (root.endsWith("/v1beta/openai", true) || root.endsWith("/v1beta", true) ||
                root == "https://generativelanguage.googleapis.com")
    }.getOrDefault(false)

    /** Native Gemini root retained for image-generation/catalog calls that still use generateContent. */
    fun geminiNativeApiRoot(baseUrl: String): String {
        var root = normalizeApiRoot(baseUrl)
        if (root.endsWith("/openai", ignoreCase = true)) root = root.dropLast("/openai".length)
        if (!root.endsWith("/v1beta", ignoreCase = true)) root = "$root/v1beta"
        return root
    }

    /** Fetches the provider's live Gemini catalog, preserving API capability metadata. */
    suspend fun discoverGeminiCatalog(baseUrl: String, apiKey: String): List<GeminiModelDescriptor> =
        withContext(Dispatchers.IO) {
            val rootUrl = geminiNativeApiRoot(baseUrl)
            val url = "$rootUrl/models?pageSize=1000"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = 20_000
                setRequestProperty("x-goog-api-key", apiKey)
                setRequestProperty("Accept", "application/json")
            }
            try {
                val status = conn.responseCode
                if (status !in 200..299) {
                    val detail = runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull().orEmpty()
                    val diagnosticProvider = if (baseUrl.contains("groq.com", true)) "groq" else "gemini"
                    throw AiHttpException(status, humanizeError(status, detail, diagnosticProvider) + if (status == 404) " [URL diminta: $url]" else "")
                }
                val root = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val data = root.optJSONArray("models") ?: JSONArray()
                buildList {
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        val id = item.optString("name").removePrefix("models/").trim()
                        if (id.isBlank()) continue
                        val methods = item.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                        val supported = buildSet {
                            for (j in 0 until methods.length()) add(methods.optString(j))
                        }
                        add(GeminiModelDescriptor(
                            id = id,
                            displayName = item.optString("displayName").ifBlank { id },
                            supportedMethods = supported,
                            inputTokenLimit = item.optInt("inputTokenLimit").takeIf { it > 0 },
                            outputTokenLimit = item.optInt("outputTokenLimit").takeIf { it > 0 }
                        ))
                    }
                }.distinctBy { it.id }
            } finally { runCatching { conn.disconnect() } }
        }

    suspend fun discoverModels(
        baseUrl: String,
        apiKey: String,
        provider: String = "openai",
        includeMedia: Boolean = false
    ): List<String> {
        if (provider.equals("gemini", ignoreCase = true) && usesGoogleGeminiNativeCatalog(baseUrl)) {
            // Google documents chat completions on /v1beta/openai, but the reliable
            // first-party model catalog is native /v1beta/models authenticated with
            // x-goog-api-key. User-defined Gemini gateways keep their own /models route.
            return cachedGeminiCatalog(baseUrl, apiKey)
                .asSequence()
                .filter { isUsableGeminiChat(it.id) }
                .filter { it.supportedMethods.isEmpty() || "generateContent" in it.supportedMethods }
                .map { it.id }
                .distinct()
                .sorted()
                .toList()
        }
        return withContext(Dispatchers.IO) {
            val rootUrl = normalizeApiRoot(canonicalBaseUrl(provider, baseUrl))
            val url = "$rootUrl/models"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = 20_000
                if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val status = conn.responseCode
                if (status !in 200..299) {
                    val detail = runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull().orEmpty()
                    val diagnosticProvider = if (baseUrl.contains("groq.com", true)) "groq" else provider
                    throw AiHttpException(status, humanizeError(status, detail, diagnosticProvider) + if (status == 404) " [URL diminta: $url]" else "")
                }
                val root = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val data = root.optJSONArray("data") ?: JSONArray()
                buildList {
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        val id = item.optString("id").trim()
                        if (id.isNotEmpty() && ModelPicker.isChatModel(id) && item.optBoolean("active", true)) add(id)
                    }
                }.distinct().sorted()
            } finally { runCatching { conn.disconnect() } }
        }
    }

    /**
     * POST {baseUrl}/chat/completions.
     * [onDelta] is invoked on the IO thread as stream chunks arrive (or once for a
     * non-streaming answer). Returns the complete assistant text. Cancelling the calling
     * coroutine aborts the connection.
     */
    /** Set by the readers when the provider stopped because it hit the output cap (finish_reason=length / MAX_TOKENS). */
    @Volatile private var lastFinishTruncated = false
    private const val MAX_CONTINUATIONS = 3

    suspend fun chatCompletions(
        endpoint: AiEndpoint,
        messages: List<ApiChatMessage>,
        onDelta: (String) -> Unit
    ): String {
        fun textOnly(msgs: List<ApiChatMessage>): List<ApiChatMessage> = msgs.map { m ->
            val hasImage = m.parts.any { it is AiPart.ImageData }
            if (!hasImage) m
            else {
                val text = m.parts.filterIsInstance<AiPart.Text>().joinToString("") { it.text }
                    .ifBlank { "(pesan berisi gambar)" }
                ApiChatMessage(m.role, listOf(AiPart.Text("$text\n\n[Catatan: gambar terlampir tidak dikirim karena model ini hanya mendukung teks]")))
            }
        }
        val requestEndpoint = endpoint
        val call: suspend (AiEndpoint, List<ApiChatMessage>) -> String = { candidate, msgs ->
            openAiChatCompletions(candidate, msgs, onDelta)
        }
        lastFinishTruncated = false
        val first = try {
            call(requestEndpoint, messages)
        } catch (e: AiHttpException) {
            // Some hosted models have a smaller output cap than the app default.
            // Retry exactly once only when the provider explicitly reports that cap.
            val cap = Regex("(?:less than or equal to|maximum value for `?(?:max_tokens|max_completion_tokens)`? is)\\s*`?(\\d+)", RegexOption.IGNORE_CASE)
                .find(e.message.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (e.httpStatus == 400 && cap != null && cap in 1 until endpoint.maxTokens) {
                call(requestEndpoint.copy(maxTokens = cap), messages)
            } else if (e.httpStatus == 400 &&
                e.message.orEmpty().contains("content must be a string", ignoreCase = true) &&
                messages.any { it.parts.any { p -> p is AiPart.ImageData } }
            ) {
                // Non-vision models reject the OpenAI vision content array. Retry once as pure text.
                call(requestEndpoint, textOnly(messages))
            } else throw e
        }
        // An answer cut off by the output cap is continued automatically (up to MAX_CONTINUATIONS times),
        // so the user never sees a sentence that just stops. Agent JSON is never continued: it must be one
        // complete object, and glued text would corrupt it.
        var full = first
        var rounds = 0
        while (lastFinishTruncated && full.isNotBlank() && !full.trimStart().startsWith("{") && rounds < MAX_CONTINUATIONS) {
            lastFinishTruncated = false
            rounds++
            val more = call(
                requestEndpoint,
                messages + ApiChatMessage("assistant", listOf(AiPart.Text(full))) +
                    ApiChatMessage("user", listOf(AiPart.Text(
                        "Lanjutkan jawabanmu persis dari kata terakhir yang terpotong. Jangan mengulang bagian sebelumnya dan jangan memberi kalimat pembuka."
                    )))
            )
            if (more.isBlank()) break
            full += more
        }
        return full
    }

    private suspend fun openAiChatCompletions(
        endpoint: AiEndpoint,
        messages: List<ApiChatMessage>,
        onDelta: (String) -> Unit,
        includeTools: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        // Settings may store the full ready endpoint (.../chat/completions).
        // Strip to root then re-append so we never get a double path.
        val apiRoot = normalizeApiRoot(canonicalBaseUrl(endpoint.provider, endpoint.baseUrl))
        val url = "$apiRoot/chat/completions"

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", if (endpoint.streaming) "text/event-stream" else "application/json")
            if (endpoint.apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${endpoint.apiKey}")
            }
        }
        val sendTools = includeTools && endpoint.allowTools && !endpoint.streaming
        try {
            val payload = buildPayload(endpoint, messages, sendTools)
            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            val status = conn.responseCode
            if (status < 200 || status >= 300) {
                val err = runCatching {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                }.getOrNull().orEmpty()
                // Some OpenAI-compatible endpoints accept chat completions but not native tools.
                // Retry once without the optional tools declaration; never hide unrelated errors.
                val toolUnsupported = sendTools && status == 400 &&
                    (err.contains("tools", true) || err.contains("tool_choice", true) || err.contains("function calling", true))
                if (toolUnsupported) {
                    com.android.island.util.IslandDiagnostics.record("AI_NATIVE_TOOLS_UNSUPPORTED", endpoint.baseUrl.take(120))
                    return@withContext openAiChatCompletions(endpoint, messages, onDelta, includeTools = false)
                }
                val diagnosticProvider = if (endpoint.baseUrl.contains("groq.com", true)) "groq" else endpoint.provider
                throw AiHttpException(status, humanizeError(status, err, diagnosticProvider) + if (status == 404) " [URL diminta: $url]" else "")
            }
            if (endpoint.streaming) readStream(conn, onDelta) else readOnce(conn, onDelta)
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    // ---- Gemini / Google AI Studio --------------------------------------------
    //
    // Not OpenAI-compatible: the key goes in the x-goog-api-key header, roles are "user"/"model"
    // with no inline "system" role (a top-level systemInstruction instead), and images are
    // inlineData parts. Streaming is Server-Sent Events of whole JSON candidate chunks
    // (the "delta" IS the chunk's text, same onDelta contract as the OpenAI path).

    private suspend fun geminiGenerateContent(
        endpoint: AiEndpoint,
        messages: List<ApiChatMessage>,
        onDelta: (String) -> Unit,
        includeTools: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val model = endpoint.model.trim().ifEmpty { GEMINI_DEFAULT_MODEL }.removePrefix("models/")
        val base = normalizeApiRoot(canonicalBaseUrl("gemini", endpoint.baseUrl))
        val method = if (endpoint.streaming) "streamGenerateContent" else "generateContent"
        val sseParam = if (endpoint.streaming) "?alt=sse" else ""
        val url = "$base/models/$model:$method$sseParam"

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", endpoint.apiKey)
        }
        try {
            val payload = buildGeminiPayload(endpoint, messages, includeTools)
            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            val status = conn.responseCode
            if (status < 200 || status >= 300) {
                val err = runCatching {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                }.getOrNull().orEmpty()
                val toolsUnsupported = includeTools && !endpoint.streaming && status == 400 &&
                    (err.contains("functionDeclarations", true) || err.contains("toolConfig", true) || err.contains("function calling", true))
                if (toolsUnsupported) {
                    com.android.island.util.IslandDiagnostics.record("AI_GEMINI_TOOLS_UNSUPPORTED", endpoint.model.take(100))
                    return@withContext geminiGenerateContent(endpoint, messages, onDelta, includeTools = false)
                }
                throw AiHttpException(status, humanizeError(status, err, "gemini"))
            }
            if (endpoint.streaming) readGeminiStream(conn, onDelta) else readGeminiOnce(conn, onDelta)
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    internal fun buildGeminiPayload(endpoint: AiEndpoint, messages: List<ApiChatMessage>, includeTools: Boolean = true): String {
        val root = JSONObject()
        val contents = JSONArray()
        val systemParts = JSONArray()
        for (m in messages) {
            if (m.role == "system") {
                for (p in m.parts) if (p is AiPart.Text) systemParts.put(JSONObject().put("text", p.text))
                continue
            }
            val parts = JSONArray()
            for (p in m.parts) {
                when (p) {
                    is AiPart.Text -> parts.put(JSONObject().put("text", p.text))
                    is AiPart.ImageData -> parts.put(
                        JSONObject().put(
                            "inlineData",
                            JSONObject().put("mimeType", "image/jpeg").put("data", p.base64Jpeg)
                        )
                    )
                }
            }
            contents.put(
                JSONObject()
                    .put("role", if (m.role == "assistant") "model" else "user")
                    .put("parts", parts)
            )
        }
        root.put("contents", contents)
        if (systemParts.length() > 0) {
            root.put("systemInstruction", JSONObject().put("parts", systemParts))
        }
        val gen = JSONObject()
            .put("temperature", endpoint.temperature.toDouble())
            .put("maxOutputTokens", endpoint.maxTokens)
        // Gemini 3.x sets reasoning depth with thinkingLevel (MINIMAL, LOW, MEDIUM, HIGH). LOW keeps chat fast.
        if (endpoint.model.ifBlank { GEMINI_DEFAULT_MODEL }.removePrefix("models/").startsWith("gemini-3")) {
            gen.put("thinkingConfig", JSONObject().put("thinkingLevel", "LOW"))
        }
        root.put("generationConfig", gen)
        // Gemini native function calling is enabled for non-streaming agent turns. The app
        // validates the returned action through IntentResolver and ShellPolicy before execution.
        if (!endpoint.streaming && includeTools) {
            val actionTypes = JSONArray().apply {
                listOf(
                    "open_app", "open_url", "search", "click", "scroll", "wait", "back", "play_song",
                    "play_in_app", "search_in_app", "tap_first_result", "press_enter", "tap", "click_index", "media", "media_status", "volume",
                    "volume_set", "list_apps", "read_screen", "notification_summary", "search_screen",
                    "current_app", "system_key", "timer", "alarm", "call", "sms", "navigate", "camera",
                    "copy", "share", "type", "generate_image", "open_settings", "diagnose", "force_stop",
                    "shell", "api_find", "api_get", "toggle"
                ).forEach { put(it) }
            }
            fun geminiField(type: String) = JSONObject().put("type", type)
            val properties = JSONObject()
                .put("type", geminiField("STRING").put("enum", actionTypes))
                .put("query", geminiField("STRING"))
                .put("url", geminiField("STRING"))
                .put("command", geminiField("STRING"))
                .put("app", geminiField("STRING"))
                .put("text", geminiField("STRING"))
                .put("prompt", geminiField("STRING"))
                .put("direction", geminiField("STRING"))
                .put("times", geminiField("INTEGER"))
                .put("ms", geminiField("INTEGER"))
                .put("setting", geminiField("STRING"))
                .put("on", geminiField("BOOLEAN"))
                .put("kind", geminiField("STRING"))
                .put("number", geminiField("STRING"))
                .put("destination", geminiField("STRING"))
                .put("mode", geminiField("STRING"))
                .put("op", geminiField("STRING"))
                .put("percent", geminiField("INTEGER"))
                .put("seconds", geminiField("INTEGER"))
                .put("hour", geminiField("INTEGER"))
                .put("minute", geminiField("INTEGER"))
                .put("message", geminiField("STRING"))
                .put("key", geminiField("STRING"))
                .put("video", geminiField("BOOLEAN"))
                .put("page", geminiField("STRING"))
                .put("x", geminiField("INTEGER"))
                .put("y", geminiField("INTEGER"))
                .put("index", geminiField("INTEGER"))
            val declaration = JSONObject()
                .put("name", "android_action")
                .put("description", "Request one Android Island action. Put the actual action name in the type field and parameters in matching fields.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", properties)
                    .put("required", JSONArray().put("type")))
            root.put("tools", JSONArray().put(JSONObject().put("functionDeclarations", JSONArray().put(declaration))))
            root.put("toolConfig", JSONObject().put("functionCallingConfig", JSONObject().put("mode", "AUTO")))
        }
        return root.toString()
    }

    /** One SSE "data: {json}" event per candidate chunk; each chunk's text is itself the delta. */
    private suspend fun readGeminiStream(conn: HttpURLConnection, onDelta: (String) -> Unit): String {
        val full = StringBuilder()
        conn.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (raw in lines) {
                currentCoroutineContext().ensureActive()
                val line = raw.trimEnd()
                if (line.isEmpty() || !line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty() || data == "[DONE]") continue
                val delta = runCatching { extractGeminiText(JSONObject(data)) }.getOrDefault("")
                if (delta.isNotEmpty()) {
                    full.append(delta)
                    onDelta(delta)
                }
            }
        }
        return full.toString()
    }

    private suspend fun readGeminiOnce(conn: HttpURLConnection, onDelta: (String) -> Unit): String {
        currentCoroutineContext().ensureActive()
        val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        currentCoroutineContext().ensureActive()
        val content = runCatching { extractGeminiText(JSONObject(body)) }.getOrDefault("")
        if (content.isNotEmpty()) onDelta(content)
        return content
    }

    internal fun extractGeminiText(obj: JSONObject): String {
        val candidate = obj.optJSONArray("candidates")?.optJSONObject(0) ?: return ""
        if (candidate.optString("finishReason") == "MAX_TOKENS") lastFinishTruncated = true
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        val text = StringBuilder()
        val actions = JSONArray()
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val t = part.optString("text")
            if (t.isNotEmpty()) text.append(t)
            val call = part.optJSONObject("functionCall") ?: continue
            val name = call.optString("name").trim()
            val args = call.optJSONObject("args") ?: continue
            if (name.isBlank()) continue
            val normalized = JSONObject(args.toString())
            if (!name.equals("android_action", true) || normalized.optString("type").isBlank()) {
                normalized.put("type", name)
            }
            actions.put(normalized)
        }
        if (actions.length() > 0) {
            return JSONObject().put("reply", text.toString().trim())
                .put("actions", actions).put("done", false).toString()
        }
        return text.toString()
    }

    // ---- payload -------------------------------------------------------------

    /**
     * OpenAI and Groq require max_completion_tokens (max_tokens is rejected by newer OpenAI models);
     * other OpenAI-compatible servers still expect max_tokens.
     */
    internal fun maxTokensField(baseUrl: String): String =
        if (baseUrl.contains("api.openai.com", true) || baseUrl.contains("groq.com", true)) "max_completion_tokens"
        else "max_tokens"

    internal fun buildPayload(endpoint: AiEndpoint, messages: List<ApiChatMessage>, includeTools: Boolean = true): String {
        val root = JSONObject()
        root.put("model", endpoint.model)
        root.put("temperature", endpoint.temperature.toDouble())
        root.put(maxTokensField(endpoint.baseUrl), endpoint.maxTokens)
        root.put("stream", endpoint.streaming)
        val arr = JSONArray()
        for (m in messages) {
            val obj = JSONObject()
            obj.put("role", m.role)
            val images = m.parts.filterIsInstance<AiPart.ImageData>()
            if (images.isEmpty()) {
                obj.put("content", m.parts.filterIsInstance<AiPart.Text>().joinToString("") { it.text })
            } else {
                val parts = JSONArray()
                for (p in m.parts) {
                    when (p) {
                        is AiPart.Text -> parts.put(
                            JSONObject().put("type", "text").put("text", p.text)
                        )
                        is AiPart.ImageData -> parts.put(
                            JSONObject()
                                .put("type", "image_url")
                                .put("image_url", JSONObject().put("url", "data:image/jpeg;base64," + p.base64Jpeg))
                        )
                    }
                }
                obj.put("content", parts)
            }
            arr.put(obj)
        }
        root.put("messages", arr)
        // One generic, schema-constrained dispatcher lets compatible models return native
        // tool_calls. The app still validates the inner action through IntentResolver and
        // ShellPolicy before execution. Streaming calls omit tools because this client does not
        // yet assemble fragmented tool-call deltas safely.
        if (includeTools && !endpoint.streaming) {
            val actionTypes = JSONArray().apply {
                listOf(
                    "open_app", "open_url", "search", "click", "scroll", "wait", "back", "play_song",
                    "play_in_app", "search_in_app", "tap_first_result", "press_enter", "tap", "click_index", "media", "media_status", "volume",
                    "volume_set", "list_apps", "read_screen", "notification_summary", "search_screen",
                    "current_app", "system_key", "timer", "alarm", "call", "sms", "navigate", "camera",
                    "copy", "share", "type", "generate_image", "open_settings", "diagnose", "force_stop",
                    "shell", "api_find", "api_get", "toggle"
                ).forEach { put(it) }
            }
            val properties = JSONObject()
                .put("type", JSONObject().put("type", "string").put("enum", actionTypes))
                .put("query", JSONObject().put("type", "string"))
                .put("url", JSONObject().put("type", "string"))
                .put("command", JSONObject().put("type", "string"))
                .put("app", JSONObject().put("type", "string"))
                .put("text", JSONObject().put("type", "string"))
                .put("prompt", JSONObject().put("type", "string"))
                .put("direction", JSONObject().put("type", "string"))
                .put("times", JSONObject().put("type", "integer"))
                .put("ms", JSONObject().put("type", "integer"))
                .put("setting", JSONObject().put("type", "string"))
                .put("on", JSONObject().put("type", "boolean"))
                .put("kind", JSONObject().put("type", "string"))
                .put("number", JSONObject().put("type", "string"))
                .put("destination", JSONObject().put("type", "string"))
                .put("mode", JSONObject().put("type", "string"))
                .put("op", JSONObject().put("type", "string"))
                .put("percent", JSONObject().put("type", "integer"))
                .put("seconds", JSONObject().put("type", "integer"))
                .put("hour", JSONObject().put("type", "integer"))
                .put("minute", JSONObject().put("type", "integer"))
                .put("message", JSONObject().put("type", "string"))
                .put("key", JSONObject().put("type", "string"))
                .put("video", JSONObject().put("type", "boolean"))
                .put("page", JSONObject().put("type", "string"))
                .put("x", JSONObject().put("type", "integer"))
                .put("y", JSONObject().put("type", "integer"))
                .put("index", JSONObject().put("type", "integer"))
            val function = JSONObject()
                .put("name", "android_action")
                .put("description", "Request one Android Island action. Put the actual action name in the type field and its parameters in the matching fields.")
                .put("parameters", JSONObject()
                    .put("type", "object")
                    .put("properties", properties)
                    .put("required", JSONArray().put("type"))
                    .put("additionalProperties", true))
            root.put("tools", JSONArray().put(JSONObject().put("type", "function").put("function", function)))
            root.put("tool_choice", "auto")
        }
        return root.toString()
    }

    // ---- responses -----------------------------------------------------------

    /** SSE stream: "data: {json}" lines, terminated by "data: [DONE]". */
    private suspend fun readStream(conn: HttpURLConnection, onDelta: (String) -> Unit): String {
        val full = StringBuilder()
        // Some relays ignore stream=true and answer with one plain JSON body; keep it as a fallback.
        val nonSse = StringBuilder()
        conn.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (raw in lines) {
                currentCoroutineContext().ensureActive()
                val line = raw.trimEnd()
                if (line.isEmpty() || line.startsWith(":")) continue
                if (!line.startsWith("data:")) {
                    if (nonSse.length < 2_000_000) nonSse.append(line).append('\n')
                    continue
                }
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                val delta = runCatching {
                    val obj = JSONObject(data)
                    val choice = obj.optJSONArray("choices")?.optJSONObject(0) ?: return@runCatching ""
                    if (choice.optString("finish_reason") == "length") lastFinishTruncated = true
                    // Strict streaming puts the chunk in delta.content; some relays use message.content.
                    val d = choice.optJSONObject("delta")?.optString("content").orEmpty()
                    if (d.isNotEmpty()) d
                    else choice.optJSONObject("message")?.let { extractContent(it) }.orEmpty()
                }.getOrDefault("")
                if (delta.isNotEmpty()) {
                    full.append(delta)
                    onDelta(delta)
                }
            }
        }
        if (full.isEmpty() && nonSse.isNotEmpty()) {
            val whole = parseWholeBody(nonSse.toString())
            if (whole.isNotEmpty()) {
                onDelta(whole)
                return whole
            }
        }
        return full.toString()
    }

    private fun parseWholeBody(body: String): String = runCatching {
        val obj = JSONObject(body)
        val choice = obj.optJSONArray("choices")?.optJSONObject(0)
        if (choice?.optString("finish_reason") == "length") lastFinishTruncated = true
        choice?.optJSONObject("message")?.let { extractContent(it) }.orEmpty()
            .ifEmpty { choice?.optString("text").orEmpty() }
    }.getOrDefault("")

    /** Whole-body JSON response (stream=false). */
    private suspend fun readOnce(conn: HttpURLConnection, onDelta: (String) -> Unit): String {
        currentCoroutineContext().ensureActive()
        val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        currentCoroutineContext().ensureActive()
        val content = parseWholeBody(body)
        if (content.isNotEmpty()) onDelta(content)
        return content
    }

    /** content may be a plain string (normal) or a parts array (vision-style replies). */
    private fun extractContent(messageObj: JSONObject): String {
        val c = messageObj.opt("content")
        val content = when (c) {
            is String -> c
            is JSONArray -> buildString {
                for (i in 0 until c.length()) {
                    val part = c.optJSONObject(i) ?: continue
                    val t = part.optString("text")
                    if (t.isNotEmpty()) {
                        if (isNotEmpty()) append('\n')
                        append(t)
                    }
                }
            }
            null, JSONObject.NULL -> ""
            else -> c.toString()
        }
        // Most compatible models return plain text, but some relays return native tool_calls.
        // Normalize the latter into the same strict app envelope; IntentResolver still allowlists
        // every action and ShellPolicy remains the final gate for terminal commands.
        return AiActionProtocol.envelopeFromToolCalls(messageObj) ?: content
    }

    // ---- errors --------------------------------------------------------------

    private fun humanizeError(status: Int, body: String, provider: String = ""): String {
        val detail = runCatching {
            val root = JSONObject(body)
            root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: root.optString("message").takeIf { it.isNotBlank() }
        }.getOrNull() ?: body.take(280).replace('\n', ' ')
        val tail = if (detail.isBlank()) "" else " ($detail)"
        return when (status) {
            400 -> if (detail.contains("must be a string", ignoreCase = true)) {
                "Model ini tidak menerima gambar (screenshot). Pilih model yang mendukung gambar (vision), lalu kirim ulang."
            } else "Permintaan gagal (HTTP 400).$tail"
            401 -> when (provider) {
                "deepseek" -> "API key DeepSeek ditolak (HTTP 401). Pastikan key dibuat di platform.deepseek.com dan masih aktif.$tail"
                "groq" -> "Autentikasi Groq gagal (HTTP 401). Pastikan memakai API key dari GroqCloud (biasanya berawalan gsk_), bukan key OpenAI/Gemini. Base URL harus https://api.groq.com/openai/v1. Buat ulang key jika sudah dicabut.$tail"
                "gemini" -> "Autentikasi Gemini gagal (HTTP 401). Periksa API key Google AI Studio dan pastikan key berasal dari project yang memiliki akses Gemini API.$tail"
                else -> "API key ditolak (HTTP 401). Pastikan key berasal dari provider yang sesuai dengan Base URL.$tail"
            }
            402 -> if (provider == "deepseek") "Saldo atau kuota akun DeepSeek tidak mencukupi (HTTP 402). Periksa billing dan balance akun.$tail" else "Pembayaran atau kuota akun tidak mencukupi (HTTP 402).$tail"
            403 -> when (provider) {
                "deepseek" -> "DeepSeek menolak akses (HTTP 403). Periksa status akun, pembatasan key, dan akses API.$tail"
                "groq" -> "Groq menolak akses (HTTP 403). Key dikenali tetapi project/organisasi mungkin tidak memiliki izin untuk resource atau model ini. Periksa status akun dan akses model GroqCloud.$tail"
                "gemini" -> "Gemini menolak akses (HTTP 403). Periksa project Google AI Studio, status Gemini API, pembatasan key, dan izin project.$tail"
                else -> "Akses API ditolak (HTTP 403). Periksa izin project dan akses model.$tail"
            }
            404 -> if (provider == "deepseek") "Endpoint atau model DeepSeek tidak ditemukan (HTTP 404). Gunakan https://api.deepseek.com/chat/completions dan model yang tersedia pada katalog akun.$tail" else "Endpoint atau model tidak ditemukan (HTTP 404). Periksa Base URL (mis. https://api.openai.com/v1) dan nama model.$tail"
            429 -> if (detail.isNotBlank()) detail else "HTTP 429"
            in 500..599 -> "Server AI sedang bermasalah (HTTP $status).$tail"
            else -> "Permintaan gagal (HTTP $status).$tail"
        }
    }
}
