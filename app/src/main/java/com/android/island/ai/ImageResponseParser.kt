package com.android.island.ai

import org.json.JSONObject

/** Reads the image bytes from a provider's JSON response. Pure logic, unit tested. */
object ImageResponseParser {

    /** OpenAI images endpoint: data[0].b64_json. Null when no image is present. */
    fun openai(json: String): ByteArray? = runCatching {
        val b64 = JSONObject(json).optJSONArray("data")?.optJSONObject(0)?.optString("b64_json") ?: ""
        if (b64.isBlank()) null else java.util.Base64.getDecoder().decode(b64)
    }.getOrNull()

    /** Gemini generateContent: first candidate part with inlineData.data. Null when none. */
    fun gemini(json: String): ByteArray? = runCatching {
        val parts = JSONObject(json).optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts")
            ?: return@runCatching null
        for (i in 0 until parts.length()) {
            val data = parts.optJSONObject(i)?.optJSONObject("inlineData")?.optString("data") ?: ""
            if (data.isNotBlank()) return@runCatching java.util.Base64.getDecoder().decode(data)
        }
        null
    }.getOrNull()
}
