package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.ChatMessageEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

enum class GeminiConnectionStatus {
    READY,
    CONNECTING,
    CONNECTED,
    ERROR
}

sealed interface StreamChunk {
    data class Delta(val accumulatedText: String, val deltaText: String) : StreamChunk
    data class Completed(val fullText: String) : StreamChunk
}

class GeminiBrain {
    companion object {
        private const val TAG = "GeminiBrain"
        val SUPPORTED_MODELS = listOf(
            "gemini-3.8-flash" to "Gemini 3.8 Flash (দ্রুত ও স্মার্ট - ডিফল্ট)",
            "gemini-3.5-flash-lite" to "Gemini 3.5 Flash Lite (সবচেয়ে দ্রুত)",
            "gemini-4-argon" to "Gemini 4 Argon (জটিল বিশ্লেষণ)"
        )
    }

    private val _status = MutableStateFlow(GeminiConnectionStatus.READY)
    val status: StateFlow<GeminiConnectionStatus> = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val modelCache = mutableMapOf<String, GenerativeModel>()

    private fun getFirebaseModel(modelName: String): GenerativeModel {
        val safeModel = SUPPORTED_MODELS.firstOrNull { it.first == modelName }?.first ?: "gemini-3.8-flash"
        return modelCache.getOrPut(safeModel) {
            Firebase.ai.generativeModel(
                modelName = safeModel,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    topP = 0.9f
                    maxOutputTokens = 650
                },
                systemInstruction = content { text(MayaPrompt.SYSTEM_INSTRUCTION) }
            )
        }
    }

    fun streamResponse(
        userMessage: String,
        history: List<ChatMessageEntity>,
        deviceContextSummary: String,
        modelName: String,
        languageMode: String,
        customApiKey: String? = null,
        screenBitmap: Bitmap? = null
    ): Flow<StreamChunk> = flow {
        _status.value = GeminiConnectionStatus.CONNECTING
        _lastError.value = null

        val contextualPrompt = MayaPrompt.buildContextualPrompt(
            userMessage = userMessage,
            deviceContextSummary = deviceContextSummary,
            languagePreference = languageMode
        )

        val accumulated = StringBuilder()

        try {
            if (!customApiKey.isNullOrBlank()) {
                streamViaCustomKey(
                    apiKey = customApiKey,
                    modelName = modelName,
                    prompt = contextualPrompt,
                    history = history,
                    screenBitmap = screenBitmap
                ).collect { delta ->
                    accumulated.append(delta)
                    emit(StreamChunk.Delta(accumulated.toString(), delta))
                }
            } else {
                val model = getFirebaseModel(modelName)
                withTimeout(60_000L) {
                    val responseFlow = if (screenBitmap != null) {
                        val multimodalContent = content {
                            image(screenBitmap)
                            text(contextualPrompt)
                        }
                        model.generateContentStream(multimodalContent)
                    } else {
                        val recentHistory: List<Content> = history
                            .filter { it.role == "user" || it.role == "maya" }
                            .takeLast(10)
                            .map { msg ->
                                content(role = if (msg.role == "user") "user" else "model") {
                                    text(msg.text)
                                }
                            }
                        val chat = model.startChat(history = recentHistory)
                        chat.sendMessageStream(contextualPrompt)
                    }

                    responseFlow.collect { chunk ->
                        val delta = chunk.text.orEmpty()
                        if (delta.isNotEmpty()) {
                            accumulated.append(delta)
                            emit(StreamChunk.Delta(accumulated.toString(), delta))
                        }
                    }
                }
            }

            val finalResponse = accumulated.toString().trim()
            if (finalResponse.isBlank()) {
                throw IllegalStateException("Gemini থেকে কোনো উত্তর পাওয়া যায়নি।")
            }
            _status.value = GeminiConnectionStatus.CONNECTED
            emit(StreamChunk.Completed(finalResponse))
        } catch (e: CancellationException) {
            _status.value = GeminiConnectionStatus.READY
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini request timed out", e)
            val msg = "উত্তর আসতে একটু বেশি সময় লাগছে, আবার চেষ্টা করো।"
            _lastError.value = msg
            _status.value = GeminiConnectionStatus.ERROR
            throw IllegalStateException(msg, e)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network unavailable", e)
            val msg = "ইন্টারনেট সংযোগ পাচ্ছি না।"
            _lastError.value = msg
            _status.value = GeminiConnectionStatus.ERROR
            throw IllegalStateException(msg, e)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed", e)
            val msg = mapErrorToBengali(e)
            _lastError.value = msg
            _status.value = GeminiConnectionStatus.ERROR
            throw IllegalStateException(msg, e)
        }
    }.flowOn(Dispatchers.IO)

    private fun streamViaCustomKey(
        apiKey: String,
        modelName: String,
        prompt: String,
        history: List<ChatMessageEntity>,
        screenBitmap: Bitmap?
    ): Flow<String> = flow {
        val safeModel = SUPPORTED_MODELS.firstOrNull { it.first == modelName }?.first ?: "gemini-3.8-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$safeModel:streamGenerateContent?alt=sse&key=$apiKey"

        val contentsArray = JSONArray()
        history.filter { it.role == "user" || it.role == "maya" }.takeLast(8).forEach { msg ->
            contentsArray.put(
                JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "model")
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                }
            )
        }

        val currentParts = JSONArray().put(JSONObject().put("text", prompt))
        if (screenBitmap != null) {
            val baos = ByteArrayOutputStream()
            screenBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            val b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
            currentParts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", b64)
                    }
                )
            )
        }
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", currentParts)
            }
        )

        val payload = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", MayaPrompt.SYSTEM_INSTRUCTION))))
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 650)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        withTimeout(60_000L) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    throw IllegalStateException("Gemini API ত্রুটি (${response.code}): ${errBody.take(120)}")
                }
                val body = response.body ?: throw IllegalStateException("Empty SSE stream")
                BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim().orEmpty()
                        if (trimmed.startsWith("data:")) {
                            val jsonStr = trimmed.removePrefix("data:").trim()
                            if (jsonStr.isNotEmpty() && jsonStr != "[DONE]") {
                                val chunkObj = JSONObject(jsonStr)
                                val candidates = chunkObj.optJSONArray("candidates")
                                val parts = candidates?.optJSONObject(0)
                                    ?.optJSONObject("content")
                                    ?.optJSONArray("parts")
                                val delta = parts?.optJSONObject(0)?.optString("text").orEmpty()
                                if (delta.isNotEmpty()) emit(delta)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun testConnection(modelName: String, customApiKey: String? = null): Result<String> {
        return try {
            _status.value = GeminiConnectionStatus.CONNECTING
            val reply = StringBuilder()
            streamResponse(
                userMessage = "হ্যালো মায়া, এক বাক্যে তোমার পরিচয় দাও।",
                history = emptyList(),
                deviceContextSummary = "",
                modelName = modelName,
                languageMode = "bn-BD",
                customApiKey = customApiKey
            ).collect { chunk ->
                if (chunk is StreamChunk.Completed) {
                    reply.append(chunk.fullText)
                }
            }
            _status.value = GeminiConnectionStatus.CONNECTED
            Result.success(reply.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _status.value = GeminiConnectionStatus.ERROR
            Result.failure(e)
        }
    }

    private fun mapErrorToBengali(e: Exception): String {
        val raw = e.localizedMessage.orEmpty().lowercase()
        return when {
            "unable to resolve host" in raw || "network" in raw || "connect" in raw ->
                "ইন্টারনেট সংযোগ পাচ্ছি না।"
            "403" in raw || "app check" in raw || "permission" in raw ->
                "API যাচাইয়ে সমস্যা হয়েছে (App Check / Key পরীক্ষা করো)।"
            "429" in raw || "quota" in raw ->
                "এই মুহূর্তে অনুরোধের সীমা পূর্ণ হয়েছে, একটু পর চেষ্টা করো।"
            "401" in raw || "api key" in raw ->
                "তোমার দেওয়া API Key সঠিক নয়।"
            else -> "AI থেকে উত্তর আনতে সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
        }
    }
}
