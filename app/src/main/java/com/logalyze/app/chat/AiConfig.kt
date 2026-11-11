package com.logalyze.app.chat

/**
 * AI settings baked into the app.
 * Put your key in API_KEY. Do NOT upload this file to a public repository.
 */
object AiConfig {
    const val API_KEY = "PUT_YOUR_KEY_HERE"
    const val BASE_URL = "https://api.groq.com/openai/v1" // any OpenAI-compatible endpoint
    const val MODEL = "llama-3.3-70b-versatile"            // used only if the model list can't be loaded

    val isConfigured: Boolean
        get() = API_KEY.isNotBlank() && API_KEY != "PUT_YOUR_KEY_HERE"
}
