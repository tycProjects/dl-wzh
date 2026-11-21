package com.android.island.ai

/**
 * Checks figures in an AI reply against real command output. Memory values may be expressed
 * in MB/MiB even though Android dumpsys reports KB; equivalent conversions are accepted.
 */
object NumberGuard {
    private val FIGURE = Regex("""(\d[\d.,]*)\s*(KiB|KB|kB|MiB|MB|GiB|GB|%)|PID\s*:?\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val MEMORY = Regex("""(\d[\d.,]*)\s*(KiB|KB|kB|MiB|MB|GiB|GB)""", RegexOption.IGNORE_CASE)

    private fun digits(s: String): String = s.filter { it.isDigit() }

    private fun number(raw: String): Double? {
        val normalized = when {
            raw.contains(',') && raw.contains('.') -> raw.replace(",", "")
            raw.contains(',') -> raw.replace(',', '.')
            else -> raw
        }
        return normalized.toDoubleOrNull()
    }

    private fun bytes(value: Double, unit: String): List<Double> {
        val u = unit.lowercase()
        val multiplier = when (u) {
            "kb", "kib" -> listOf(1000.0, 1024.0)
            // Many Android tools and users say MB/GB while dividing by 1024.
            "mb" -> listOf(1_000_000.0, 1_048_576.0)
            "mib" -> listOf(1_048_576.0)
            "gb" -> listOf(1_000_000_000.0, 1_073_741_824.0)
            "gib" -> listOf(1_073_741_824.0)
            else -> emptyList()
        }
        return multiplier.map { value * it }
    }

    private fun memoryEquivalent(replyFigure: String, evidence: String): Boolean {
        val candidate = MEMORY.find(replyFigure) ?: return false
        val candidateValue = number(candidate.groupValues[1]) ?: return false
        val candidateBytes = bytes(candidateValue, candidate.groupValues[2])
        if (candidateBytes.isEmpty()) return false
        val sourceBytes = mutableListOf<Double>()
        MEMORY.findAll(evidence).forEach { source ->
            val sourceValue = number(source.groupValues[1]) ?: return@forEach
            sourceBytes += bytes(sourceValue, source.groupValues[2])
        }
        // dumpsys meminfo declares "Applications Memory Usage (in Kilobytes)" once,
        // then prints TOTAL PSS/RSS without repeating the unit on those summary lines.
        // TOTAL PSS and TOTAL RSS often share one line, so the anchor is a word
        // boundary, not a line start (line-start-only missed TOTAL RSS entirely).
        Regex("(?i)(?:^|\\s)TOTAL\\s+(?:PSS|RSS)\\s*:\\s*([\\d,]+)").findAll(evidence).forEach { source ->
            number(source.groupValues[1])?.let { sourceBytes += bytes(it, "KB") }
        }
        return candidateBytes.any { a -> sourceBytes.any { b ->
            val scale = maxOf(kotlin.math.abs(a), kotlin.math.abs(b), 1.0)
            kotlin.math.abs(a - b) / scale <= 0.015
        } }
    }

    /** Figures whose values are neither present nor a defensible memory-unit conversion. */
    fun unverifiedFigures(reply: String, evidence: String): List<String> {
        if (evidence.isBlank()) return emptyList()
        val evidenceDigits = digits(evidence)
        return FIGURE.findAll(reply).map { it.value.trim() }.filter { figure ->
            val n = digits(figure)
            if (n.isEmpty()) return@filter false
            if (evidenceDigits.contains(n)) return@filter false
            if (memoryEquivalent(figure, evidence)) return@filter false
            true
        }.distinct().toList()
    }
}
