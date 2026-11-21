package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelPickerTest {
    @Test fun picksNewestStableFlashAndIgnoresPreview() {
        val ids = listOf("gemini-3.1-flash-lite", "gemini-3.5-flash", "gemini-3.5-flash-preview-05-2026", "gemini-2.5-flash")
        assertEquals("gemini-3.5-flash", ModelPicker.pickChat(ids, ModelPicker.GEMINI_FLASH))
    }
    @Test fun prefersGemini38FlashForNewProjectsWhenAvailable() {
        val ids = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.8-flash")
        assertEquals("gemini-3.8-flash", ModelPicker.pickGeminiChat(ids))
    }

    @Test fun prefersGemini35FlashLiteAsDefaultWhenAvailable() {
        val ids = listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-2.5-flash")
        assertEquals("gemini-3.5-flash-lite", ModelPicker.pickGeminiChat(ids))
    }

    @Test fun fallsBackToAvailableGeminiFlashIfLatestIsNotExposed() {
        val ids = listOf("gemini-2.5-flash", "gemini-3.5-flash")
        assertEquals("gemini-3.5-flash", ModelPicker.pickGeminiChat(ids))
    }

    @Test fun returnsNullWhenFamilyMissing() {
        assertNull(ModelPicker.pickChat(listOf("llama-3.3-70b"), ModelPicker.GEMINI_FLASH))
    }
    @Test fun comparesVersionsNumerically() {
        assertEquals(1, ModelPicker.compareVersions(listOf(3, 10), listOf(3, 9)))
    }
    @Test fun picksPrimaryImageModelFromCatalog() {
        assertEquals("gpt-image-1", ModelPicker.pickImage(listOf("gpt-image-1-mini", "gpt-image-1", "dall-e-3")))
    }
    @Test fun imageModelNotInCatalogIsNotPicked() {
        assertNull(ModelPicker.pickImage(listOf("dall-e-3")))
    }

    @Test fun geminiFlashImageModelIsAnImageModelWhenOfferedByGemini() {
        assertEquals("gemini-2.5-flash-image", ModelPicker.pickImage(listOf("gemini-2.5-flash-image")))
    }

    @Test fun openAiDefaultIsTheNewestSolNotAnAlphabeticalOldModel() {
        val ids = listOf("babbage-002", "dall-e-3", "gpt-6-luna", "gpt-6.1-sol", "gpt-6-sol", "text-embedding-3-large", "whisper-1")
        assertEquals("gpt-6.1-sol", ModelPicker.pickDefault("openai", "https://api.openai.com/v1/chat/completions", ids, ModelPicker.GPT_SOL))
    }
    @Test fun nonChatIdsAreNeverPickedAsDefault() {
        val ids = listOf("dall-e-3", "gpt-image-2", "tts-1", "whisper-1", "some-chat-model")
        assertEquals("some-chat-model", ModelPicker.pickDefault("openai", "https://example.com/v1/chat/completions", ids))
    }
    @Test fun openRouterVendorPrefixedSolIsFound() {
        val ids = listOf("anthropic/x", "openai/gpt-6-sol", "openai/gpt-6.1-sol")
        assertEquals("openai/gpt-6.1-sol", ModelPicker.pickDefault("openai", "https://openrouter.ai/api/v1/chat/completions", ids))
    }
    @Test fun groqPrefersGptOss120b() {
        val ids = listOf("llama-3.3-70b-versatile", "openai/gpt-oss-120b")
        assertEquals("openai/gpt-oss-120b", ModelPicker.pickDefault("groq", "https://api.groq.com/openai/v1/chat/completions", ids))
    }
    @Test fun xaiFamilyRegexPicksNewestPlainGrok() {
        val ids = listOf("grok-3", "grok-4.3", "grok-4.5", "grok-4.5-mini")
        assertEquals("grok-4.5", ModelPicker.pickDefault("openai", "https://api.x.ai/v1/chat/completions", ids, Regex("""^grok-(\d+(?:\.\d+)*)$""")))
    }
    @Test fun defaultIsAlwaysAnIdFromTheList() {
        assertNull(ModelPicker.pickDefault("openai", "https://example.com", emptyList()))
    }
}
