package com.android.island.ai

/** Prevents a generic post-action acknowledgement from replacing a useful explanation. */
object AgentReplyPolicy {
    private val genericCompletion = listOf(
        "saya telah menjelaskan",
        "sudah saya jelaskan",
        "telah saya jelaskan",
        "apakah ada hal lain yang ingin",
        "ada hal lain yang ingin anda",
        "ada hal lain yang bisa saya bantu",
        "anything else you would like",
        "is there anything else i can help"
    )

    fun keepSubstantiveAnswer(previous: String, candidate: String): String {
        val old = previous.trim()
        val next = candidate.trim()
        if (next.isBlank()) return old
        val isGeneric = genericCompletion.any { next.contains(it, ignoreCase = true) }
        val previousIsSubstantive = old.length >= 240 &&
            old.count { it.isLetterOrDigit() } >= 180
        return if (isGeneric && previousIsSubstantive) old else next
    }
}
