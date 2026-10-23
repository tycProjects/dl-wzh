package com.example.model

enum class AiProvider(val displayName: String) {
    AUTO("Auto (Gemini + Grok)"),
    GEMINI("Gemini AI"),
    GROK("Grok AI")
}

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

enum class CodeTaskType(val label: String) {
    GENERATE("Generate Code"),
    EXPLAIN("Explain Code"),
    DEBUG("Debug & Fix"),
    PROJECT_STRUCT("Folder Structure"),
    PROMPT_GEN("AI App Prompt")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val providerUsed: String? = null,
    val imageBase64: String? = null,
    val isError: Boolean = false
)

data class CodeSnippet(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val language: String,
    val code: String,
    val explanation: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val isBuiltInShortcut: Boolean = false
)

data class AppSettings(
    val primaryProvider: AiProvider = AiProvider.AUTO,
    val fallbackProvider: AiProvider = AiProvider.GROK,
    val geminiModel: String = "gemini-3.5-flash",
    val grokModel: String = "grok-2-latest",
    val voiceLanguage: String = "en-US", // "en-US", "bn-BD", "hi-IN"
    val ttsEnabled: Boolean = true,
    val customGeminiKey: String = "",
    val customGrokKey: String = "",
    val hudAnimationEnabled: Boolean = true
)
