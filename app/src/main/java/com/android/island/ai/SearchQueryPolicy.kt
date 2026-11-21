package com.android.island.ai

import java.util.Locale

/** Normalizes web-search queries so the same request is not repeated within one AI turn. */
internal object SearchQueryPolicy {
    private const val OBSERVATION_PREFIX = "Hasil pencarian untuk \""
    private const val DEEP_OBSERVATION_PREFIX = "Hasil pencarian mendalam untuk \""

    fun key(query: String): String = query
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun wasAlreadySearched(query: String, observations: List<String>): Boolean {
        val wanted = key(query)
        if (wanted.isBlank()) return false
        return observations.asSequence()
            .filter { it.startsWith(OBSERVATION_PREFIX) || it.startsWith(DEEP_OBSERVATION_PREFIX) }
            .any { observation ->
                val prefix = when {
                    observation.startsWith(OBSERVATION_PREFIX) -> OBSERVATION_PREFIX
                    else -> DEEP_OBSERVATION_PREFIX
                }
                val previousQuery = observation
                    .removePrefix(prefix)
                    .substringBefore("\":")
                key(previousQuery) == wanted
            }
    }
}
