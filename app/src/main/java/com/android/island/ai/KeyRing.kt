package com.android.island.ai

/** Builds the list of API keys to try: the primary key first, then backups. Pure logic, unit tested. */
object KeyRing {
    const val MAX_KEYS = 5

    fun keys(primary: String, extra: String): List<String> =
        (listOf(primary) + extra.lines()).map { it.trim() }.filter { it.isNotBlank() }.distinct().take(MAX_KEYS)

    /**
     * Try order for one request: start at [start] and wrap around (1 -> 2 -> ... -> N -> 1),
     * skipping keys whose index is in [skip]. If every key is temporarily exhausted, return
     * an empty list so the caller waits for the provider's reset instead of hammering the same key.
     */
    fun rotationOrder(count: Int, start: Int, skip: Set<Int>): List<Int> {
        if (count <= 0) return emptyList()
        val rotated = (0 until count).map { (start.mod(count) + it) % count }
        return rotated.filter { it !in skip }
    }
}
