package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiTaskRouterTest {
    private fun model(id: String, vararg methods: String) = GeminiModelDescriptor(
        id = id,
        displayName = id,
        supportedMethods = methods.toSet(),
        inputTokenLimit = 1_000_000,
        outputTokenLimit = 64_000
    )

    @Test fun picksEfficientGeminiChatModelOnlyWhenLiveCatalogListsIt() {
        val catalog = listOf(
            model("gemini-2.5-pro", "generateContent"),
            model("gemini-2.5-flash", "generateContent"),
            model("gemini-3.8-live", "bidiGenerateContent")
        )
        assertEquals("gemini-2.5-flash", GeminiTaskRouter.select(GeminiTask.CHAT, catalog))
    }

    @Test fun explicitlySelectedGeminiModelIsHonoredForChat() {
        val catalog = listOf(model("gemini-2.5-pro", "generateContent"), model("gemini-2.5-flash", "generateContent"))
        assertEquals("gemini-2.5-pro", GeminiTaskRouter.select(GeminiTask.CHAT, catalog, "gemini-2.5-pro"))
    }

    @Test fun complexPromptRoutesToStrongReasoningModel() {
        val catalog = listOf(
            model("gemini-2.5-flash", "generateContent"),
            model("gemini-2.5-pro", "generateContent"),
            model("gemini-3-pro-preview", "generateContent")
        )
        assertEquals("gemini-3-pro-preview", GeminiTaskRouter.select(GeminiTask.COMPLEX_REASONING, catalog))
        assertEquals(GeminiTask.COMPLEX_REASONING, GeminiTaskRouter.inferTask("Tolong debug kode Kotlin ini", false))
        assertEquals(GeminiTask.CHAT, GeminiTaskRouter.inferTask("Halo, apa kabar?", false))
        assertEquals(GeminiTask.CHAT, GeminiTaskRouter.inferTask("Buka aplikasi WhatsApp", false))
        assertEquals(GeminiTask.CHAT, GeminiTaskRouter.inferTask("Cek memori WhatsApp", false))
    }

    @Test fun imageUnderstandingNeverSelectsLiveOrImageGenerationModel() {
        val catalog = listOf(
            model("gemini-3.8-live", "bidiGenerateContent"),
            model("gemini-3.1-flash-image", "generateContent"),
            model("gemini-3.8-flash", "generateContent")
        )
        assertEquals("gemini-3.8-flash", GeminiTaskRouter.select(GeminiTask.SCREEN_ANALYSIS, catalog))
        assertFalse(GeminiTaskRouter.supports(GeminiTask.IMAGE_UNDERSTANDING, model("gemini-3.1-flash-image", "generateContent")))
    }

    @Test fun imageGenerationUsesOnlyImageModelsInLiveCatalog() {
        val catalog = listOf(
            model("gemini-2.5-flash", "generateContent"),
            model("gemini-2.5-flash-image", "generateContent"),
            model("gemini-2.0-flash-preview-image-generation", "generateContent")
        )
        assertEquals("gemini-2.5-flash-image", GeminiTaskRouter.select(GeminiTask.IMAGE_GENERATION, catalog))
    }

    @Test fun unsupportedTaskTransportsDoNotFallBackToChat() {
        val catalog = listOf(model("gemini-4-hydrogen", "generateContent"))
        assertNull(GeminiTaskRouter.select(GeminiTask.LIVE_VOICE, catalog))
        assertNull(GeminiTaskRouter.select(GeminiTask.TEXT_TO_SPEECH, catalog))
        assertNull(GeminiTaskRouter.select(GeminiTask.VIDEO_GENERATION, catalog))
        assertNull(GeminiTaskRouter.select(GeminiTask.EMBEDDINGS, catalog))
        assertNull(GeminiTaskRouter.select(GeminiTask.DEEP_RESEARCH, catalog))
        assertTrue(GeminiTaskRouter.clientSupportMessage(GeminiTask.LIVE_VOICE).isNotBlank())
    }


    @Test fun explicitOlderGeminiChoiceIsNotSilentlyOverridden() {
        val catalog = listOf(
            model("gemini-2.5-flash", "generateContent"),
            model("gemini-3.8-flash", "generateContent")
        )
        assertEquals("gemini-2.5-flash", GeminiTaskRouter.select(GeminiTask.CHAT, catalog, "gemini-2.5-flash"))
    }
}
