package com.android.island.ai

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiGeminiToolPayloadTest {
    @Test fun nonStreamingGeminiPayloadDeclaresAndroidActionFunction() {
        val endpoint = AiEndpoint("https://generativelanguage.googleapis.com/v1beta", "test-key", "gemini-test", 0.2f, 1024, false, "gemini")
        val payload = JSONObject(AiClient.buildGeminiPayload(endpoint, listOf(ApiChatMessage.user("cari berita"))))
        val declaration = payload.getJSONArray("tools").getJSONObject(0)
            .getJSONArray("functionDeclarations").getJSONObject(0)
        assertEquals("android_action", declaration.getString("name"))
        assertEquals("AUTO", payload.getJSONObject("toolConfig").getJSONObject("functionCallingConfig").getString("mode"))
        assertTrue(declaration.getJSONObject("parameters").getJSONObject("properties").has("type"))
    }


    @Test fun geminiFunctionCallResponseBecomesValidatedActionEnvelope() {
        val response = JSONObject().put("candidates", org.json.JSONArray().put(
            JSONObject().put("content", JSONObject().put("parts", org.json.JSONArray().put(
                JSONObject().put("functionCall", JSONObject()
                    .put("name", "android_action")
                    .put("args", JSONObject().put("type", "search").put("query", "berita terbaru")))
            )))
        ))
        val normalized = AiClient.extractGeminiText(response)
        assertEquals(listOf(AiAction.WebSearch("berita terbaru")), AiActionProtocol.parse(normalized).actions)
    }

    @Test fun streamingGeminiPayloadOmitsFunctionDeclarationsUntilChunksAreSafe() {        val endpoint = AiEndpoint("https://generativelanguage.googleapis.com/v1beta", "test-key", "gemini-test", 0.2f, 1024, true, "gemini")
        val payload = JSONObject(AiClient.buildGeminiPayload(endpoint, listOf(ApiChatMessage.user("halo"))))
        assertFalse(payload.has("tools"))
        assertFalse(payload.has("toolConfig"))
    }
}
