package com.android.island.ai

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Raised with a message already written for the user (see [ImageClient.errorMessage]). */
class ImageException(message: String) : Exception(message)

/**
 * Creates one image for a prompt. The model is picked automatically from the provider's own
 * model list, so the same key works without a separate image setting.
 */
object ImageClient {
    private const val TIMEOUT_MS = 120_000

    /** Turns a provider error into a short Indonesian message with the next step. Pure. */
    fun errorMessage(code: Int, body: String): String {
        val lower = body.lowercase()
        return when {
            code == 403 && (lower.contains("organization") || lower.contains("verif")) ->
                "Organisasi OpenAI belum terverifikasi untuk model gambar. Verifikasi di konsol OpenAI, lalu coba lagi."
            code == 401 || code == 403 -> "Kunci API ditolak penyedia. Periksa kunci API di pengaturan."
            code == 429 -> "Batas permintaan penyedia tercapai. Coba lagi sebentar lagi."
            else -> "Pembuatan gambar gagal (kode $code)."
        }
    }

    suspend fun generate(provider: String, baseUrl: String, apiKey: String, prompt: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = if (provider == "gemini") AiClient.geminiNativeApiRoot(baseUrl)
                    else AiClient.normalizeApiRoot(AiClient.canonicalBaseUrl(provider, baseUrl))
                val model = if (provider == "gemini") {
                    val catalog = AiClient.discoverGeminiCatalog(baseUrl, apiKey)
                    GeminiTaskRouter.select(GeminiTask.IMAGE_GENERATION, catalog)
                } else {
                    ModelPicker.pickImage(AiClient.discoverModels(baseUrl, apiKey, provider, includeMedia = true))
                } ?: throw ImageException("Penyedia ini belum memiliki model gambar yang tersedia dan terverifikasi untuk API key ini.")
                if (provider == "gemini") {
                    val body = JSONObject()
                        .put("contents", JSONArray().put(
                            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                        ))
                        .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("IMAGE")))
                    val raw = post("$root/models/$model:generateContent", mapOf("x-goog-api-key" to apiKey), body.toString())
                    ImageResponseParser.gemini(raw) ?: throw ImageException("Respons gambar kosong dari penyedia.")
                } else {
                    val body = JSONObject()
                        .put("model", model).put("prompt", prompt).put("n", 1).put("size", "1024x1024")
                    val raw = post("$root/images/generations", mapOf("Authorization" to "Bearer $apiKey"), body.toString())
                    ImageResponseParser.openai(raw) ?: throw ImageException("Respons gambar kosong dari penyedia.")
                }
            }
        }

    private fun post(url: String, headers: Map<String, String>, body: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw ImageException(errorMessage(code, text))
        return text
    }
}
