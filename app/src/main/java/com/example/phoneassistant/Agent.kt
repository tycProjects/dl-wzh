package com.example.phoneassistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

const val MODEL = "claude-sonnet-5-5"

class Agent(private val apiKey: () -> String, private val actions: PhoneActions) {

    private val http = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()
    private val history = JSONArray()
    private val system =
        "Ты ассистент на Android-телефоне. Выполняй просьбы пользователя с помощью инструментов. " +
        "Отвечай коротко и на языке пользователя. Если не хватает данных (номер, время) — спроси."

    suspend fun ask(text: String): String = withContext(Dispatchers.IO) {
        history.put(JSONObject().put("role", "user").put("content", text))

        for (step in 0 until 6) {                       // защита от бесконечного цикла
            val resp = call()
            val content = resp.getJSONArray("content")
            history.put(JSONObject().put("role", "assistant").put("content", content))

            if (resp.optString("stop_reason") != "tool_use") {
                return@withContext (0 until content.length())
                    .map { content.getJSONObject(it) }
                    .filter { it.getString("type") == "text" }
                    .joinToString("\n") { it.getString("text") }
            }

            val results = JSONArray()
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.getString("type") != "tool_use") continue
                val out = try {
                    actions.run(block.getString("name"), block.getJSONObject("input"))
                } catch (e: Exception) {
                    "Ошибка: ${e.message}"
                }
                results.put(
                    JSONObject().put("type", "tool_result")
                        .put("tool_use_id", block.getString("id"))
                        .put("content", out)
                )
            }
            history.put(JSONObject().put("role", "user").put("content", results))
        }
        "Не удалось завершить действие."
    }

    private fun call(): JSONObject {
        val body = JSONObject()
            .put("model", MODEL).put("max_tokens", 1024).put("system", system)
            .put("tools", actions.toolSchemas()).put("messages", history)
        val req = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey())
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { r ->
            val s = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("API ${r.code}: $s")
            return JSONObject(s)
        }
    }
}
