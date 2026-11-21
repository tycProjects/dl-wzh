package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekProviderTest {
    @Test fun currentEndpointDoesNotAddLegacyV1Prefix() {
        val full = AiClient.canonicalBaseUrl("deepseek", "https://api.deepseek.com/v1/chat/completions")
        assertEquals("https://api.deepseek.com/chat/completions", full)
        assertEquals("https://api.deepseek.com", AiClient.normalizeApiRoot(full))
    }

    @Test fun deepseekPresetHasReadyToUseEndpoint() {
        assertEquals(
            "https://api.deepseek.com/chat/completions",
            AiClient.canonicalBaseUrl("deepseek", "")
        )
    }

    @Test fun liveDeepseekCatalogPrefersFastAgentModel() {
        val selected = ModelPicker.pickDefault(
            "deepseek",
            "https://api.deepseek.com/chat/completions",
            listOf("deepseek-v4-pro", "deepseek-flash")
        )
        assertEquals("deepseek-flash", selected)
    }

    @Test fun deepseekCatalogCanStillSelectProWhenFlashUnavailable() {
        val selected = ModelPicker.pickDefault(
            "deepseek",
            "https://api.deepseek.com/chat/completions",
            listOf("deepseek-v4-pro")
        )
        assertEquals("deepseek-v4-pro", selected)
        assertTrue(ModelPicker.isChatModel(selected!!))
    }
}
