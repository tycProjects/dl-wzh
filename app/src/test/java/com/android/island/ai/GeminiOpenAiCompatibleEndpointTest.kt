package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiOpenAiCompatibleEndpointTest {
    private val expected = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"

    @Test fun geminiDefaultUsesGoogleOpenAiCompatibleEndpoint() {
        assertEquals(expected, AiClient.canonicalBaseUrl("gemini", ""))
    }

    @Test fun geminiBaseUrlWithOpenAiPathIsNotStripped() {
        assertEquals(expected, AiClient.canonicalBaseUrl(
            "gemini", "https://generativelanguage.googleapis.com/v1beta/openai/"
        ))
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/openai",
            AiClient.normalizeApiRoot(expected)
        )
    }

    @Test fun legacyGeminiNativeRootIsUpgradedToOpenAiCompatiblePath() {
        assertEquals(expected, AiClient.canonicalBaseUrl(
            "gemini", "https://generativelanguage.googleapis.com/v1beta"
        ))
    }

    @Test fun nativeGeminiRootRemainsAvailableForImageApi() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta",
            AiClient.geminiNativeApiRoot(expected)
        )
    }


    @Test fun geminiNativeCatalogRootIsDerivedFromOpenAiCompatibleBaseUrl() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta",
            AiClient.geminiNativeApiRoot("https://generativelanguage.googleapis.com/v1beta/openai/")
        )
    }


    @Test fun nativeGeminiCatalogIsUsedOnlyForGoogleFirstPartyEndpoint() {
        assertTrue(AiClient.usesGoogleGeminiNativeCatalog(expected))
        assertTrue(AiClient.usesGoogleGeminiNativeCatalog("https://generativelanguage.googleapis.com/v1beta/openai/"))
        assertTrue(!AiClient.usesGoogleGeminiNativeCatalog("https://gateway.example/gemini/v1/chat/completions"))
    }

    @Test fun manuallyEnteredBaseUrlSurvivesProviderSwitch() {
        val manual = "https://my-gateway.example/api/v1/chat/completions"
        assertEquals(manual, AiClient.baseUrlForProviderChange(
            manual, "https://api.openai.com/v1/chat/completions", "gemini", expected
        ))
    }

    @Test fun customBaseUrlPathOnKnownHostIsNotForcedBackToProviderPreset() {
        assertEquals(
            "https://api.groq.com/my-proxy/chat/completions",
            AiClient.canonicalBaseUrl("groq", "https://api.groq.com/my-proxy")
        )
    }

    @Test fun fullCustomChatEndpointIsPreserved() {
        val custom = "https://gateway.example/custom/v2/chat/completions"
        assertEquals(custom, AiClient.canonicalBaseUrl("gemini", custom))
    }

    @Test fun shippedPresetMayChangeWhenProviderChanges() {
        assertEquals(expected, AiClient.baseUrlForProviderChange(
            AiClient.canonicalBaseUrl("openai", ""), "https://api.openai.com/v1/chat/completions", "gemini", expected
        ))
        assertTrue(AiClient.canonicalBaseUrl("gemini", "").contains("/v1beta/openai/"))
    }
}
