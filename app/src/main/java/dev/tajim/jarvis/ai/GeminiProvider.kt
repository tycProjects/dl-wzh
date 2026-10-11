package dev.tajim.jarvis.ai

import dev.tajim.jarvis.data.Role
import org.json.JSONArray
import org.json.JSONObject

/**
 * Google Gemini over the public REST API (generativelanguage.googleapis.com, v1beta).
 * The model id is never hard-coded: the Settings screen asks Google which models this key can use.
 */
class GeminiProvider(private val base: String = "https://generativelanguage.googleapis.com/v1beta") : AiProvider {
    override val name = "Gemini"

    private fun headers(key: String) = mapOf("x-goog-api-key" to key, "Content-Type" to "application/json")

    private fun fail(code: Int, body: String): Nothing {
        val detail = runCatching { JSONObject(body).getJSONObject("error").optString("message") }.getOrNull().orEmpty()
        val kind = when (code) {
            400, 401, 403 -> AiException.Kind.AUTH
            429 -> AiException.Kind.RATE_LIMIT
            in 500..599 -> AiException.Kind.SERVER
            else -> AiException.Kind.BAD_RESPONSE
        }
        val msg = when (kind) {
            AiException.Kind.AUTH -> "Gemini rejected the API key or request (HTTP $code). ${detail.take(160)}"
            AiException.Kind.RATE_LIMIT -> "Rate limit reached (HTTP 429). Wait a moment and try again."
            AiException.Kind.SERVER -> "Gemini is unavailable right now (HTTP $code)."
            else -> "Unexpected response (HTTP $code). ${detail.take(160)}"
        }
        throw AiException(kind, msg.trim())
    }

    override suspend fun listModels(apiKey: String): Result<List<String>> = safely {
        val (code, body) = Http.request("$base/models?pageSize=200", headers = headers(apiKey))
        if (code !in 200..299) fail(code, body)
        val arr = JSONObject(body).optJSONArray("models") ?: JSONArray()
        (0 until arr.length()).map { arr.getJSONObject(it) }
            .filter { m ->
                val methods = m.optJSONArray("supportedGenerationMethods")
                methods != null && (0 until methods.length()).any { methods.getString(it) == "generateContent" }
            }
            .map { it.getString("name").removePrefix("models/") }
            .sorted()
    }

    override suspend fun chat(apiKey: String, model: String, systemPrompt: String, turns: List<Turn>): Result<String> = safely {
        val contents = JSONArray()
        turns.forEach { t ->
            contents.put(
                JSONObject()
                    .put("role", if (t.role == Role.USER) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", t.text))),
            )
        }
        val req = JSONObject().put("contents", contents)
        if (systemPrompt.isNotBlank()) {
            req.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
        }
        val (code, body) = Http.request("$base/models/$model:generateContent", "POST", headers(apiKey), req.toString(), 60_000)
        if (code !in 200..299) fail(code, body)
        val root = JSONObject(body)
        val parts = root.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val text = parts?.let { p -> (0 until p.length()).joinToString("") { p.getJSONObject(it).optString("text") } }.orEmpty().trim()
        if (text.isEmpty()) {
            val reason = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            throw AiException(AiException.Kind.BAD_RESPONSE, if (reason.isNotEmpty()) "The request was blocked ($reason)." else "Gemini returned an empty answer.")
        }
        text
    }
}
