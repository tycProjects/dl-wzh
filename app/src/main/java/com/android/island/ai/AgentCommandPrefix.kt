package com.android.island.ai

/**
 * Decides whether a message is phone control (agent) or ordinary chat.
 *
 * STAGE 74: the leading dot is the ONLY agent marker. A plain sentence such as "buka tiktok"
 * without a dot is ordinary chat and must never trigger the observe/act loop; this keeps the
 * two modes unambiguous for the user (agent does, chat talks) and removes the old heuristics
 * that could send chat text into the device protocol by accident.
 * - ". perintah"  -> agent
 * - "/chat teks"  -> ordinary chat, whatever the text looks like
 * - everything else -> ordinary chat
 */
object AgentCommandPrefix {
    data class Parsed(val command: String, val isAgentRequest: Boolean, val autoDetected: Boolean = false)

    fun parse(input: String, hasAttachments: Boolean = false): Parsed {
        val value = input.trim()
        AgentIntentClassifier.stripChatEscape(value)?.let { return Parsed(it.ifBlank { value }, false) }
        if (hasAttachments || value.length < 3) return Parsed(value, false)
        if (value[0] == '.') {
            // ". perintah", ".perintah" and ".. perintah" all run the agent — the leading dot
            // is the explicit marker. A message of only dots/space carries no command and
            // stays ordinary chat.
            val command = value.trimStart('.', ' ', '\t').trim()
            return if (command.isBlank()) Parsed(value, false) else Parsed(command, true)
        }
        return Parsed(value, false)
    }
}
