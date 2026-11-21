package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiClientUrlTest {
    @Test
    fun groqBaseUrlRemainsBaseUrl() {
        assertEquals(
            "https://api.groq.com/openai/v1",
            AiClient.normalizeApiRoot("https://api.groq.com/openai/v1")
        )
    }

    @Test
    fun fullGroqChatCompletionsUrlIsNormalized() {
        assertEquals(
            "https://api.groq.com/openai/v1",
            AiClient.normalizeApiRoot("https://api.groq.com/openai/v1/chat/completions")
        )
    }

    @Test
    fun modelsEndpointIsNormalized() {
        assertEquals(
            "https://api.groq.com/openai/v1",
            AiClient.normalizeApiRoot("https://api.groq.com/openai/v1/models")
        )
    }

    @Test
    fun canonicalReturnsFullReadyGroqEndpoint() {
        assertEquals(
            "https://api.groq.com/openai/v1/chat/completions",
            AiClient.canonicalBaseUrl("openai", "https://api.groq.com")
        )
        assertEquals(
            "https://api.groq.com/openai/v1/chat/completions",
            AiClient.canonicalBaseUrl("openai", "https://api.groq.com/openai/v1")
        )
        assertEquals(
            "https://api.groq.com/openai/v1/chat/completions",
            AiClient.canonicalBaseUrl("openai", "https://api.groq.com/openai/v1/chat/completions")
        )
    }

    @Test
    fun canonicalGroqProviderHasDedicatedDefault() {
        assertEquals(
            "https://api.groq.com/openai/v1/chat/completions",
            AiClient.canonicalBaseUrl("groq", "")
        )
    }

    @Test
    fun canonicalGeminiStaysNativeRoot() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta",
            AiClient.canonicalBaseUrl("gemini", "https://generativelanguage.googleapis.com")
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta",
            AiClient.canonicalBaseUrl("gemini", "https://generativelanguage.googleapis.com/v1beta")
        )
    }

    @Test
    fun canonicalOpenAiAndOthersAreFullChatEndpoints() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            AiClient.canonicalBaseUrl("openai", "https://api.openai.com/v1")
        )
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            AiClient.canonicalBaseUrl("openai", "https://openrouter.ai/api/v1")
        )
        assertEquals(
            "https://api.x.ai/v1/chat/completions",
            AiClient.canonicalBaseUrl("openai", "https://api.x.ai/v1")
        )
    }

    @Test
    fun requestPathNeverDoublesChatCompletions() {
        val stored = AiClient.canonicalBaseUrl("openai", "https://api.groq.com")
        val root = AiClient.normalizeApiRoot(stored)
        assertEquals("https://api.groq.com/openai/v1", root)
        assertEquals("https://api.groq.com/openai/v1/chat/completions", "$root/chat/completions")
    }
}
