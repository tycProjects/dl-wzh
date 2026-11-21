package ai.casper.app.net

import ai.casper.app.BuildConfig
import ai.casper.app.data.ChatTurn
import ai.casper.app.data.ModelInfo
import ai.casper.app.data.ModelsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// Работа с сервером Casper AI (Vercel). Ключ OpenRouter здесь не используется: он только на сервере.
object Api {
    private val json = Json { ignoreUnknownKeys = true }
    private val media = "application/json".toMediaType()
    private val base = BuildConfig.API_BASE_URL.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun fetchModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base/api/models").get().build()
        client.newCall(req).execute().use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(errorMessage(body, res.code))
            json.decodeFromString<ModelsResponse>(body).models
        }
    }

    // Потоковый ответ. onDelta вызывается для каждого куска текста.
    // Отмена корутины закрывает соединение с сервером.
    suspend fun streamChat(model: String, messages: List<ChatTurn>, onDelta: suspend (String) -> Unit) {
        val payload = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                messages.forEach { m ->
                    addJsonObject {
                        put("role", m.role)
                        if (m.images.isEmpty()) {
                            put("content", m.content)
                        } else {
                            putJsonArray("content") {
                                addJsonObject {
                                    put("type", "text")
                                    put("text", m.content)
                                }
                                m.images.forEach { url ->
                                    addJsonObject {
                                        put("type", "image_url")
                                        putJsonObject("image_url") { put("url", url) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.toString()

        val req = Request.Builder().url("$base/api/chat").post(payload.toRequestBody(media)).build()
        val call = client.newCall(req)
        coroutineScope {
            // Если корутину отменили (кнопка «Стоп»), закрываем соединение.
            val watcher = launch {
                try {
                    awaitCancellation()
                } finally {
                    call.cancel()
                }
            }
            try {
                readStream(call, onDelta)
            } finally {
                watcher.cancel()
            }
        }
    }

    private suspend fun readStream(call: okhttp3.Call, onDelta: suspend (String) -> Unit) {
        run {
            withContext(Dispatchers.IO) {
                call.execute().use { res ->
                    if (!res.isSuccessful) throw IOException(errorMessage(res.body?.string().orEmpty(), res.code))
                    val source = res.body?.source() ?: throw IOException("Пустой ответ сервера")
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        val obj = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull() ?: continue
                        obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull?.let {
                            throw IOException(it)
                        }
                        val delta = obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("delta")?.jsonObject
                            ?.get("content")?.jsonPrimitive?.contentOrNull
                        if (!delta.isNullOrEmpty()) onDelta(delta)
                    }
                }
            }
        }
    }

    private fun errorMessage(body: String, code: Int): String =
        runCatching {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: "Ошибка сервера ($code)"
}
