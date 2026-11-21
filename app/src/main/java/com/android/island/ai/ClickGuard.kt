package com.android.island.ai

/** Blocks destructive, financial, or externally sending taps unless the user explicitly asked. */
object ClickGuard {
    private val RISKY = listOf(
        "hapus", "delete", "remove", "clear data", "clear storage", "factory reset",
        "bayar", "pay", "beli", "buy", "purchase", "checkout", "transfer",
        "kirim", "send", "submit", "uninstall", "reset", "format"
    )

    private fun containsPhrase(text: String, phrase: String): Boolean {
        val escaped = Regex.escape(phrase)
        return Regex("(?<![\\p{L}\\p{N}])$escaped(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)
    }

    fun allowed(label: String, userText: String): Boolean {
        val risky = RISKY.firstOrNull { containsPhrase(label, it) } ?: return true
        return containsPhrase(userText, risky)
    }
}
