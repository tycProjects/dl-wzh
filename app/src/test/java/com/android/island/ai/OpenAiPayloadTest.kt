package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAiPayloadTest {
    @Test
    fun openAiAndGroqUseMaxCompletionTokens() {
        assertEquals("max_completion_tokens", AiClient.maxTokensField("https://api.openai.com/v1/chat/completions"))
        assertEquals("max_completion_tokens", AiClient.maxTokensField("https://api.groq.com/openai/v1/chat/completions"))
    }

    @Test
    fun otherCompatibleServersKeepMaxTokens() {
        assertEquals("max_tokens", AiClient.maxTokensField("https://openrouter.ai/api/v1/chat/completions"))
    }

}
