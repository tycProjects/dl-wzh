package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResult(
    val text: String,
    val providerUsed: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class AiServiceManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val systemPrompt = """
        You are ARAFAT AI, an advanced futuristic personal AI assistant developed by ARAFAT MOJMUNDAR.
        You are ultra-intelligent, precise, fast, and helpful.
        You communicate fluently in English, Bengali (বাংলা), Hindi, and any requested language.
        You excel in programming, system troubleshooting, general intelligence, reasoning, writing, and app automation.
        When providing code, format it cleanly with language-tagged markdown code blocks.
        Maintain your identity as ARAFAT AI created by ARAFAT MOJMUNDAR.
    """.trimIndent()

    fun getResolvedGeminiKey(customKey: String): String {
        if (customKey.isNotBlank()) return customKey.trim()
        val envKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (envKey.isNotBlank() && envKey != "MY_GEMINI_API_KEY") envKey else ""
    }

    fun getResolvedGrokKey(customKey: String): String {
        if (customKey.isNotBlank()) return customKey.trim()
        val envKey = try {
            BuildConfig.XAI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (envKey.isNotBlank() && envKey != "MY_XAI_API_KEY") envKey else ""
    }

    suspend fun generateResponse(
        prompt: String,
        imageBase64: String? = null,
        provider: AiProvider,
        geminiModel: String = "gemini-3.5-flash",
        grokModel: String = "grok-2-latest",
        customGeminiKey: String = "",
        customGrokKey: String = ""
    ): AiResult = withContext(Dispatchers.IO) {
        when (provider) {
            AiProvider.GEMINI -> {
                callGemini(prompt, imageBase64, geminiModel, customGeminiKey)
            }
            AiProvider.GROK -> {
                callGrok(prompt, imageBase64, grokModel, customGrokKey)
            }
            AiProvider.AUTO -> {
                // Try Gemini first as default high-speed multimodal provider
                val geminiKey = getResolvedGeminiKey(customGeminiKey)
                val grokKey = getResolvedGrokKey(customGrokKey)

                if (geminiKey.isNotBlank()) {
                    val geminiRes = callGemini(prompt, imageBase64, geminiModel, customGeminiKey)
                    if (geminiRes.isSuccess) {
                        return@withContext geminiRes
                    }
                    Log.w("AiServiceManager", "Gemini failed in AUTO mode: ${geminiRes.errorMessage}, trying Grok fallback")
                }

                if (grokKey.isNotBlank()) {
                    val grokRes = callGrok(prompt, imageBase64, grokModel, customGrokKey)
                    if (grokRes.isSuccess) {
                        return@withContext grokRes.copy(
                            providerUsed = "Grok AI ($grokModel - Auto Fallback)"
                        )
                    }
                }

                // If both failed or neither has keys
                if (geminiKey.isBlank() && grokKey.isBlank()) {
                    AiResult(
                        text = "⚠️ Neither Gemini nor Grok API Key is configured.\n\n" +
                                "To activate ARAFAT AI:\n" +
                                "1. Open Settings in ARAFAT AI\n" +
                                "2. Enter your Gemini API Key or Grok API Key (or configure GEMINI_API_KEY in AI Studio Secrets panel).\n\n" +
                                "Once configured, ARAFAT AI will respond immediately in English, Bengali, or your preferred language.",
                        providerUsed = "System Diagnostics",
                        isSuccess = false,
                        errorMessage = "Missing API Keys"
                    )
                } else {
                    AiResult(
                        text = "⚠️ Auto-mode could not reach configured AI providers. Please check your internet connectivity or verify API quotas in Settings.",
                        providerUsed = "Auto Mode Diagnostics",
                        isSuccess = false,
                        errorMessage = "Request failed across available providers"
                    )
                }
            }
        }
    }

    private fun callGemini(
        prompt: String,
        imageBase64: String?,
        model: String,
        customKey: String
    ): AiResult {
        val apiKey = getResolvedGeminiKey(customKey)
        if (apiKey.isBlank()) {
            return AiResult(
                text = "⚠️ Gemini API key is missing.\n\nPlease set GEMINI_API_KEY in the Secrets panel in AI Studio, or enter your key in ARAFAT AI Settings > Gemini Configuration.",
                providerUsed = "Gemini",
                isSuccess = false,
                errorMessage = "Gemini API key not configured"
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val partsArray = JSONArray()
            val textPart = JSONObject().put("text", prompt)
            partsArray.put(textPart)

            if (!imageBase64.isNullOrBlank()) {
                val inlineData = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", imageBase64)
                val imagePart = JSONObject().put("inlineData", inlineData)
                partsArray.put(imagePart)
            }

            val contentObj = JSONObject()
                .put("role", "user")
                .put("parts", partsArray)

            val contentsArray = JSONArray().put(contentObj)

            val systemInstruction = JSONObject()
                .put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))

            val requestJson = JSONObject()
                .put("contents", contentsArray)
                .put("systemInstruction", systemInstruction)
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", 0.7)
                        .put("maxOutputTokens", 4096)
                )

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                return AiResult(
                    text = "⚠️ Gemini API Error (${response.code}): $errorMsg",
                    providerUsed = "Gemini AI ($model)",
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text", "")
                        sb.append(text)
                    }
                    val outputText = sb.toString().trim()
                    if (outputText.isNotBlank()) {
                        return AiResult(
                            text = outputText,
                            providerUsed = "Gemini AI ($model)",
                            isSuccess = true
                        )
                    }
                }
            }

            return AiResult(
                text = "No response content received from Gemini model.",
                providerUsed = "Gemini AI ($model)",
                isSuccess = false,
                errorMessage = "Empty candidates in response"
            )

        } catch (e: Exception) {
            Log.e("AiServiceManager", "Gemini call exception", e)
            return AiResult(
                text = "⚠️ Connection Error: ${e.localizedMessage ?: "Failed to reach Gemini servers. Please check internet connection."}",
                providerUsed = "Gemini AI ($model)",
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    private fun callGrok(
        prompt: String,
        imageBase64: String?,
        model: String,
        customKey: String
    ): AiResult {
        val apiKey = getResolvedGrokKey(customKey)
        if (apiKey.isBlank()) {
            return AiResult(
                text = "⚠️ Grok (xAI) API key is missing.\n\nPlease enter your xAI API key in ARAFAT AI Settings > Grok AI Configuration (or provide XAI_API_KEY environment variable).",
                providerUsed = "Grok",
                isSuccess = false,
                errorMessage = "Grok API key not configured"
            )
        }

        try {
            val url = "https://api.x.ai/v1/chat/completions"

            val messagesArray = JSONArray()

            // System message
            messagesArray.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", systemPrompt)
            )

            // User message
            if (!imageBase64.isNullOrBlank()) {
                val userContentArray = JSONArray()
                userContentArray.put(JSONObject().put("type", "text").put("text", prompt))
                userContentArray.put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$imageBase64"))
                )
                messagesArray.put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", userContentArray)
                )
            } else {
                messagesArray.put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            }

            val requestJson = JSONObject()
                .put("model", model)
                .put("messages", messagesArray)
                .put("temperature", 0.7)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                return AiResult(
                    text = "⚠️ Grok API Error (${response.code}): $errorMsg",
                    providerUsed = "Grok AI ($model)",
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }

            val rootJson = JSONObject(responseBody)
            val choices = rootJson.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val message = choice.optJSONObject("message")
                val content = message?.optString("content", "") ?: ""
                if (content.isNotBlank()) {
                    return AiResult(
                        text = content.trim(),
                        providerUsed = "Grok AI ($model)",
                        isSuccess = true
                    )
                }
            }

            return AiResult(
                text = "No response content received from Grok model.",
                providerUsed = "Grok AI ($model)",
                isSuccess = false,
                errorMessage = "Empty choices in Grok response"
            )

        } catch (e: Exception) {
            Log.e("AiServiceManager", "Grok call exception", e)
            return AiResult(
                text = "⚠️ Grok Connection Error: ${e.localizedMessage ?: "Failed to connect to xAI API."}",
                providerUsed = "Grok AI ($model)",
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message")
            if (!message.isNullOrBlank()) {
                message
            } else {
                "HTTP $statusCode: $responseBody"
            }
        } catch (e: Exception) {
            "HTTP $statusCode"
        }
    }
}
