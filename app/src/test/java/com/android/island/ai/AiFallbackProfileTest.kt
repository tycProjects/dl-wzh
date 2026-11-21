package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiFallbackProfileTest {
    @Test fun parsesExplicitCrossProviderProfilesInOrder() {
        val profiles = AiFallbackProfile.parse(
            """groq|https://api.groq.com/openai/v1|openai/gpt-oss-120b|gsk-one
                gemini|https://generativelanguage.googleapis.com/v1beta|gemini-3.8-flash|AIza-two""".trimIndent()
        )
        assertEquals(2, profiles.size)
        assertEquals("groq", profiles[0].provider)
        assertEquals("gemini", profiles[1].provider)
        assertEquals("openai/gpt-oss-120b", profiles[0].model)
    }

    @Test fun acceptsOtherOpenAiCompatibleProviders() {
        val profiles = AiFallbackProfile.parse(
            "deepseek|https://api.deepseek.com|deepseek-chat|key\nopenrouter|https://openrouter.ai/api/v1|model/name|key"
        )
        assertEquals(listOf("deepseek", "openrouter"), profiles.map { it.provider })
    }

    @Test fun allowsLocalOllamaWithoutAnApiKey() {
        val profiles = AiFallbackProfile.parse("ollama|http://127.0.0.1:11434/v1|llama3.2|")
        assertEquals(1, profiles.size)
        assertEquals("", profiles.single().apiKey)
    }

    @Test fun rejectsMalformedOrUnsupportedProfiles() {
        val profiles = AiFallbackProfile.parse(
            """anthropic|https://api.example.com|model|key
                openai|not-a-url|model|key
                openai|https://api.groq.com/openai/v1||key""".trimIndent()
        )
        assertTrue(profiles.isEmpty())
    }
}
