package com.android.island.ai

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiNativeToolPayloadTest {
    @Test fun compatibleNonStreamingPayloadOffersValidatedAndroidActionTool() {
        val endpoint = AiEndpoint("https://api.openai.com/v1", "test-key", "test-model", 0.2f, 1024, false)
        val payload = JSONObject(AiClient.buildPayload(endpoint, listOf(ApiChatMessage.user("cari berita"))))
        assertEquals("auto", payload.optString("tool_choice"))
        val tool = payload.getJSONArray("tools").getJSONObject(0).getJSONObject("function")
        assertEquals("android_action", tool.getString("name"))
        assertTrue(tool.getJSONObject("parameters").getJSONObject("properties").has("type"))
    }

    @Test fun streamingPayloadOmitsToolsUntilToolDeltasAreSupported() {
        val endpoint = AiEndpoint("https://api.openai.com/v1", "test-key", "test-model", 0.2f, 1024, true)
        val payload = JSONObject(AiClient.buildPayload(endpoint, listOf(ApiChatMessage.user("halo"))))
        assertFalse(payload.has("tools"))
    }

    @Test fun compatibilityFallbackCanBuildPayloadWithoutTools() {
        val endpoint = AiEndpoint("https://custom.example/v1", "", "test-model", 0.2f, 1024, false)
        val payload = JSONObject(AiClient.buildPayload(endpoint, listOf(ApiChatMessage.user("halo")), includeTools = false))
        assertFalse(payload.has("tools"))
        assertFalse(payload.has("tool_choice"))
    }
}
