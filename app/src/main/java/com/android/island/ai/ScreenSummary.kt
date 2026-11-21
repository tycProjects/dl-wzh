package com.android.island.ai

/** Keeps the screen text sent back to the model small, so each loop step stays cheap. */
object ScreenSummary {
    const val LIMIT = 1500

    fun trim(text: String): String =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n").take(LIMIT)
}
