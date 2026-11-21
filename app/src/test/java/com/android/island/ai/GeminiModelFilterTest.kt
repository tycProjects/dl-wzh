package com.android.island.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelFilterTest {
    @Test fun textModelsOfAnyVersionAreChatOptions() {
        assertTrue(AiClient.isUsableGeminiChat("gemini-2.5-flash"))
        assertTrue(AiClient.isUsableGeminiChat("gemini-3.1-flash-lite"))
        assertTrue(AiClient.isUsableGeminiChat("gemini-3.1-pro-preview"))
        assertTrue(AiClient.isUsableGeminiChat("gemma-4-26b-a4b-it"))
    }

    @Test fun mediaAndAgentModelsAreNotChatOptions() {
        assertFalse(AiClient.isUsableGeminiChat("gemini-3.1-flash-image"))
        assertFalse(AiClient.isUsableGeminiChat("gemini-3.1-flash-tts-preview"))
        assertFalse(AiClient.isUsableGeminiChat("gemini-3.1-flash-live-preview"))
        assertFalse(AiClient.isUsableGeminiChat("gemini-embedding-001"))
        assertFalse(AiClient.isUsableGeminiChat("gemini-omni-flash"))
    }

    @Test fun currentFlashAndGemini4AreChatOptions() {
        assertTrue(AiClient.isUsableGeminiChat("gemini-3.8-flash"))
        assertTrue(AiClient.isUsableGeminiChat("gemini-4-argon"))
    }

    @Test fun decisionModelIsNotAChatOption() {
        assertFalse(AiClient.isUsableGeminiChat("gemini-decision-1"))
        assertFalse(AiClient.isUsableGeminiChat("gemini-nano-banana-2.1"))
    }
}
