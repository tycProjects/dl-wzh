package com.android.island.island

/**
 * Turns the chat log lines of one request into steps for the step trail (jejak langkah).
 * Pure logic so the state rules are unit tested without Compose.
 */
object StepTrailModel {

    enum class State { DONE, ACTIVE, FAILED }

    data class Step(val text: String, val state: State)

    /** Words the device layer uses when a step could not do what was asked. */
    private val FAILED_HINTS = listOf("Tidak", "Gagal", "belum", "dilewati")

    fun from(lines: List<String>, running: Boolean): List<Step> =
        lines.mapIndexed { i, line ->
            val failed = FAILED_HINTS.any { line.contains(it, ignoreCase = true) }
            val state = when {
                failed -> State.FAILED
                running && i == lines.lastIndex -> State.ACTIVE
                else -> State.DONE
            }
            Step(line, state)
        }
}
