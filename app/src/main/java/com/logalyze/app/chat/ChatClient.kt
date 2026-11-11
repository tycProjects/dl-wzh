package com.logalyze.app.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val fromUser: Boolean, val text: String)

/** Talks to an OpenAI-compatible chat endpoint using the key in [AiConfig]. */
object ChatClient {
    private class ModelError(message: String) : IOException(message)

    @Volatile
    private var resolvedModel: String? = null

    private val preferred = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "openai/gpt-oss-20b",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "qwen/qwen3-32b"
    )
    private val notChat = listOf("whisper", "guard", "tts", "playai", "orpheus", "embed", "safeguard")

    suspend fun ask(system: String, history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        if (!AiConfig.isConfigured) throw IllegalStateException("AI is not configured")
        val model = resolvedModel ?: resolveModel().also { resolvedModel = it }
        try {
            request(model, system, history)
        } catch (e: ModelError) {
            // The model may have been retired. Look for a working one and retry once.
            resolvedModel = null
            val fresh = resolveModel()
            resolvedModel = fresh
            if (fresh == model) throw e
            request(fresh, system, history)
        }
    }

    private fun open(path: String, method: String): HttpURLConnection {
        val base = AiConfig.BASE_URL.trim().trimEnd('/')
        val conn = URL("$base$path").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000
        conn.setRequestProperty("Authorization", "Bearer ${AiConfig.API_KEY.trim()}")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Logalyze/1.0 (Android)")
        return conn
    }

    private fun readBody(conn: HttpURLConnection): Pair<Int, String> {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        return code to (stream?.bufferedReader()?.use { it.readText() } ?: "")
    }

    /** Picks a working chat model from the provider's own list. */
    private fun resolveModel(): String {
        val conn = open("/models", "GET")
        return try {
            val (code, body) = readBody(conn)
            if (code !in 200..299) return AiConfig.MODEL
            val data = JSONObject(body).getJSONArray("data")
            val ids = List(data.length()) { data.getJSONObject(it).getString("id") }
            preferred.firstOrNull { it in ids }
                ?: ids.firstOrNull { id -> notChat.none { id.contains(it, ignoreCase = true) } }
                ?: AiConfig.MODEL
        } catch (e: Exception) {
            AiConfig.MODEL
        } finally {
            conn.disconnect()
        }
    }

    private fun request(model: String, system: String, history: List<ChatMessage>): String {
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        history.forEach {
            messages.put(
                JSONObject()
                    .put("role", if (it.fromUser) "user" else "assistant")
                    .put("content", it.text)
            )
        }
        val body = JSONObject()
            .put("model", model)
            .put("temperature", 0.3)
            .put("messages", messages)

        val conn = open("/chat/completions", "POST")
        try {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val (code, text) = readBody(conn)
            if (code !in 200..299) {
                val msg = errorMessage(code, text)
                if ((code == 400 || code == 404) && text.contains("model", ignoreCase = true)) {
                    throw ModelError(msg)
                }
                throw IOException(msg)
            }
            return JSONObject(text)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(code: Int, body: String): String {
        val msg = try {
            JSONObject(body).optJSONObject("error")?.optString("message")
        } catch (e: Exception) {
            null
        }
        val detail = if (!msg.isNullOrBlank()) msg else body.trim()
        return if (detail.isNotEmpty()) "HTTP $code - ${detail.take(160)}" else "HTTP $code"
    }
}
