package com.generated.aiaddi

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    // Nama model bisa berubah dari waktu ke waktu. Kalau muncul error "model not found",
    // ganti nama model di tiga baris ini.
    const val OPENAI_MODEL = "gpt-4o-mini"
    const val GEMINI_MODEL = "gemini-3.8-flash"
    const val GROK_MODEL = "grok-3-mini"

    // model: nama model dari Pengaturan AI. Kalau kosong, pakai model bawaan di atas.
    fun ask(provider: String, key: String, prompt: String, model: String = ""): String =
        when (provider.trim()) {
            "1" -> openAiStyle("https://api.openai.com/v1/chat/completions", model.ifBlank { OPENAI_MODEL }, key, prompt)
            "3" -> openAiStyle("https://api.x.ai/v1/chat/completions", model.ifBlank { GROK_MODEL }, key, prompt)
            "2" -> gemini(model.ifBlank { GEMINI_MODEL }, key, prompt)
            else -> throw Exception("Provider Custom belum didukung. Pilih 1 (OpenAI), 2 (Gemini), atau 3 (Grok).")
        }

    private fun openAiStyle(url: String, model: String, key: String, prompt: String): String {
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        val r = post(url, mapOf("Authorization" to "Bearer $key"), body.toString())
        return JSONObject(r).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content").trim()
    }

    private fun gemini(model: String, key: String, prompt: String): String {
        val body = JSONObject().put(
            "contents",
            JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt))))
        )
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val r = post(url, mapOf("x-goog-api-key" to key), body.toString())
        return JSONObject(r).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
    }

    private fun post(url: String, headers: Map<String, String>, body: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 20000
        c.readTimeout = 60000
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw Exception("Error $code: ${text.take(300)}")
        return text
    }
}
