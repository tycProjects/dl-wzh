package com.android.island.ai

/**
 * Which device diagnostics and force-stops the AI may run. Pure logic, unit tested.
 * Diagnostics are read-only. Force-stop needs the user's own words and a valid package name.
 */
object DiagnosticPolicy {
    enum class Kind { MEMINFO, BATTERY, CPU, STORAGE }

    private val PACKAGE = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+\$")
    private val FORCE_STOP_WORDS = listOf("paksa berhenti", "paksa tutup", "force stop", "force-stop", "matikan paksa", "tutup paksa", "hentikan paksa")

    fun kindFrom(name: String): Kind? = runCatching { Kind.valueOf(name.trim().uppercase()) }.getOrNull()

    fun safePackage(pkg: String): Boolean = PACKAGE.matches(pkg)

    /** Fixed read-only command for a kind. MEMINFO needs a valid package, otherwise null. */
    fun command(kind: Kind, pkg: String?): String? = when (kind) {
        // The full dump is long and its TOTAL lines come last, so keep only the summary lines.
        // No pipe: the full dump is filtered in Kotlin by [summarizeMeminfo], so the command stays a plain dumpsys call.
        Kind.MEMINFO -> pkg?.takeIf { safePackage(it) }?.let { "dumpsys meminfo $it" }
        Kind.BATTERY -> "dumpsys battery"
        Kind.CPU -> "dumpsys cpuinfo"
        Kind.STORAGE -> "df -h /data"
    }

    private val MEMINFO_KEYS = listOf("TOTAL", "Heap", "Graphics", "Private", "Native", "Dalvik", "Code", "Stack")

    /** Keeps only the summary lines of a meminfo dump. Falls back to the raw text if none match. */
    fun summarizeMeminfo(raw: String): String {
        val summary = raw.lineSequence().filter { line -> MEMINFO_KEYS.any { line.contains(it) } }.joinToString("\n")
        return summary.ifBlank { raw.trim() }
    }

    fun forceStopAllowed(userText: String): Boolean {
        val text = userText.lowercase()
        return FORCE_STOP_WORDS.any { text.contains(it) }
    }

    fun forceStopCommand(pkg: String): String? = if (safePackage(pkg)) "am force-stop $pkg" else null
}
