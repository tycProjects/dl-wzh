package id.len.tools

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Public demo endpoints supplied for LEN TOOLS.
 * For production, proxy these calls through your own backend.
 * Never place private API credentials in this Android app.
 */
object LenApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(65, TimeUnit.SECONDS)
        .build()

    suspend fun ask(ai: String, prompt: String): String = withContext(Dispatchers.IO) {
        val url = when (ai) {
            "GPT LEN AI" -> "https://api.nexadev.my.id/ai/gpt4".toHttpUrl()
                .newBuilder().addQueryParameter("q", prompt).build()
            else -> "https://api.nexadev.my.id/ai/deepsek".toHttpUrl()
                .newBuilder().addQueryParameter("text", prompt).build()
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: ${body.take(180)}")
            }
            extractAnswer(body)
        }
    }

    suspend fun createNetflixToken(apiKey: String, targetCountry: String): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API key wajib diisi." }
        val url = "https://api.nexadev.my.id/api/netflix".toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("targetCountry", targetCountry)
            .build()
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${body.take(250)}")
            try {
                val json = JSONObject(body)
                val pretty = json.toString(2)
                pretty.ifBlank { body }
            } catch (_: Exception) { body.ifBlank { "API mengembalikan respons kosong." } }
        }
    }

    private fun extractAnswer(body: String): String {
        return try {
            val json = JSONObject(body)
            val keys = listOf("answer", "response", "result", "message", "text", "output", "data")
            for (key in keys) {
                val value = json.opt(key)
                if (value is String && value.isNotBlank()) return value
                if (value is JSONObject) {
                    for (nested in keys) {
                        val nestedValue = value.opt(nested)
                        if (nestedValue is String && nestedValue.isNotBlank()) return nestedValue
                    }
                }
            }
            json.toString(2)
        } catch (_: Exception) {
            body.ifBlank { "API mengembalikan respons kosong." }
        }
    }
}
