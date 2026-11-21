package com.android.island.ai

import java.util.Locale

/**
 * Proves (or fails to prove) that pressing Enter did something. A route that merely "accepted" the key
 * is not evidence: TikTok ignores ACTION_IME_ENTER yet the call returns true. Only a changed screen counts.
 * Pure logic, unit tested.
 */
object SubmitVerifier {
    enum class Verdict { SUBMITTED, UNCHANGED }

    private val CLOCK = Regex("^\\d{1,2}[:.]\\d{2}\$")
    private val BATTERY = Regex("^\\d{1,3}\\s?%\$")

    private fun normalise(texts: List<String>): Set<String> =
        texts.map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() && it.length <= 120 && !CLOCK.matches(it) && !BATTERY.matches(it) }
            .toSet()

    fun verdict(before: List<String>, after: List<String>): Verdict {
        val b = normalise(before)
        val a = normalise(after)
        if (b.isEmpty() && a.isEmpty()) return Verdict.UNCHANGED
        val union = (b + a).size.coerceAtLeast(1)
        val similarity = b.intersect(a).size.toDouble() / union
        val vanished = if (b.isEmpty()) 0.0 else b.count { it !in a }.toDouble() / b.size
        return if (similarity < 0.75 || vanished >= 0.4) Verdict.SUBMITTED else Verdict.UNCHANGED
    }
}
