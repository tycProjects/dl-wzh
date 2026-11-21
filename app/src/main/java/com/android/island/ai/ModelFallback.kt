package com.android.island.ai

/**
 * Reads a "model retired" error from the provider and finds the replacement it names.
 * Pure logic, unit tested.
 */
object ModelFallback {
    private val MODEL_REF = Regex("""models/([A-Za-z0-9._-]+)""")
    private val RETIRED_HINTS = listOf("no longer available", "no longer supported", "shut down", "retired", "deprecated")

    private val MISSING_HINTS = listOf(
        "does not exist", "model_not_found", "not found", "tidak ditemukan", "unknown model", "invalid model",
        "must provide a model", "model is required", "is missing", "decommissioned"
    )

    /** The provider rejected the model name itself (missing, blank, or removed), as opposed to a bad key or quota. */
    fun isMissing(status: Int, message: String): Boolean =
        (status == 404 || status == 400) && message.contains("model", ignoreCase = true) &&
            MISSING_HINTS.any { message.contains(it, ignoreCase = true) }

    fun isRetired(message: String): Boolean =
        RETIRED_HINTS.any { message.contains(it, ignoreCase = true) }

    /** The model the provider points to instead of [current], or null when the message names none. */
    fun suggest(message: String, current: String): String? {
        val cur = current.removePrefix("models/")
        return MODEL_REF.findAll(message).map { it.groupValues[1] }.firstOrNull { it != cur }
    }
}
