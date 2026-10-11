package dev.tajim.jarvis.ai

import dev.tajim.jarvis.data.Role

data class Turn(val role: Role, val text: String)

/** Provider-independent surface. Gemini is the first adapter; others can implement this without touching the UI. */
interface AiProvider {
    val name: String
    suspend fun listModels(apiKey: String): Result<List<String>>
    suspend fun chat(apiKey: String, model: String, systemPrompt: String, turns: List<Turn>): Result<String>
}
