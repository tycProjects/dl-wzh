package dev.tajim.jarvis.ai

import dev.tajim.jarvis.data.ChatRepository
import dev.tajim.jarvis.data.MemoryRepository
import dev.tajim.jarvis.data.Role
import dev.tajim.jarvis.data.SettingsRepository

/** Builds the prompt (persona, memories) and talks to the provider. Shared by the Chat screen and the voice assistant. */
class AiService(
    private val settings: SettingsRepository,
    private val provider: AiProvider,
    private val chat: ChatRepository,
    private val memories: MemoryRepository,
) {
    /** Sends [text] as a new user message. Returns the reply, or a failure the UI shows as a real error. */
    suspend fun ask(text: String, codeMode: Boolean = false): Result<String> {
        val userMsg = chat.add(Role.USER, text)
        return complete(userMsg.id, codeMode)
    }

    /** Re-sends a user message that failed earlier. */
    suspend fun retry(messageId: Long, codeMode: Boolean = false): Result<String> {
        chat.update(messageId) { it.copy(failed = false) }
        return complete(messageId, codeMode)
    }

    private suspend fun complete(userMessageId: Long, codeMode: Boolean): Result<String> {
        val result = safely { callProvider(userMessageId, codeMode) }
        return result.also { r ->
            if (r.isSuccess) chat.add(Role.ASSISTANT, r.getOrThrow())
            else chat.update(userMessageId) { it.copy(failed = true) }
        }
    }

    private suspend fun callProvider(userMessageId: Long, codeMode: Boolean): String {
        val s = settings.current()
        val key = settings.apiKey()
            ?: throw AiException(AiException.Kind.NOT_CONFIGURED, "No API key yet. Add your Gemini API key in Settings, System & Data Engine.")
        if (s.aiModel.isBlank()) {
            throw AiException(AiException.Kind.NOT_CONFIGURED, "No model selected. Open Settings, System & Data Engine and choose one.")
        }
        val history = chat.messages.value
        val upTo = history.indexOfFirst { it.id == userMessageId }.let { if (it < 0) history.size - 1 else it }
        val turns = history.take(upTo + 1).takeLast(24).map { Turn(it.role, it.text) }

        val system = buildString {
            append("You are JARVIS, an AI assistant inside an Android app. You are an AI, not a human. ")
            append(s.persona.prompt).append(' ')
            append("Reply in the language the user wrote in. Keep spoken-style answers short unless asked for detail. ")
            if (codeMode) append("The user wants coding help: give correct code in fenced code blocks with a brief explanation. ")
            if (s.customInstructions.isNotBlank()) append("\nUser's extra instructions: ").append(s.customInstructions).append('\n')
            if (s.useMemories) {
                val mem = memories.items.value.take(20)
                if (mem.isNotEmpty()) append("\nThings the user asked you to remember:\n").append(mem.joinToString("\n") { "- " + it.text })
            }
        }
        return provider.chat(key, s.aiModel, system, turns).getOrThrow()
    }
}
