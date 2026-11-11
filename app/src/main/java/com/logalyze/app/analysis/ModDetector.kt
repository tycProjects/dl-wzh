package com.logalyze.app.analysis

/** Finds mod names mentioned near errors in a log. Returns at most 5 distinct entries. */
object ModDetector {
    private const val MAX_LINE = 600
    private const val MAX_RESULTS = 5

    private val suspectedRx = Regex("Suspected Mods?\\s*:\\s*(.+)\$", RegexOption.IGNORE_CASE)
    private val mixinApplyRx = Regex("Mixin apply for mod\\s+(\\S+)\\s+failed", RegexOption.IGNORE_CASE)
    private val mixinConfigRx = Regex("([A-Za-z0-9_.-]+)\\.mixins\\.json")
    private val fabricReqRx = Regex("Mod '([^']+)' \\(([^)]+)\\)(?: \\S+)? (?:requires|depends|is incompatible|conflicts)")
    private val forgeReqRx = Regex("Mod ID: '([^']+)', Requested by: '([^']+)'")
    private val loadingIssueRx = Regex("Mod loading issue for:?\\s*(\\S+)", RegexOption.IGNORE_CASE)
    private val jarRx = Regex("mods[/\\\\]([^/\\\\\\s:]+\\.jar)", RegexOption.IGNORE_CASE)
    private val problemRx = Regex("exception|error|failed|caused by", RegexOption.IGNORE_CASE)

    private val ignored = setOf("none", "unknown", "n/a", "minecraft", "java", "forge", "fabric", "neoforge")

    fun detect(lines: List<String>): List<String> {
        val suspected = LinkedHashSet<String>()
        val deps = LinkedHashSet<String>()
        val mixins = LinkedHashSet<String>()
        val issues = LinkedHashSet<String>()
        val jars = LinkedHashSet<String>()

        fun add(set: LinkedHashSet<String>, value: String) {
            val v = value.trim().trimEnd(',', ';', '.').take(60)
            if (v.isNotEmpty() && v.lowercase() !in ignored && set.size < MAX_RESULTS) set.add(v)
        }

        for (raw in lines) {
            if (raw.length < 8 || raw.length > MAX_LINE) continue

            if (raw.contains("Suspected Mod", ignoreCase = true)) {
                suspectedRx.find(raw)?.groupValues?.get(1)?.split(',')?.forEach { add(suspected, it) }
                continue
            }
            if (raw.contains("Mod '")) {
                fabricReqRx.find(raw)?.let { add(deps, "${it.groupValues[1]} (${it.groupValues[2]})") }
            }
            if (raw.contains("Requested by")) {
                forgeReqRx.find(raw)?.let { add(deps, "${it.groupValues[2]} (needs ${it.groupValues[1]})") }
            }
            if (raw.contains("mixin", ignoreCase = true)) {
                mixinApplyRx.find(raw)?.let { add(mixins, it.groupValues[1]) }
                if (problemRx.containsMatchIn(raw)) {
                    mixinConfigRx.find(raw)?.let { add(mixins, it.groupValues[1]) }
                }
            }
            if (raw.contains("Mod loading issue", ignoreCase = true)) {
                loadingIssueRx.find(raw)?.let { add(issues, it.groupValues[1]) }
            }
            if (jars.size < MAX_RESULTS && raw.contains(".jar", ignoreCase = true) && problemRx.containsMatchIn(raw)) {
                jarRx.find(raw)?.let { add(jars, it.groupValues[1]) }
            }
        }

        val all = LinkedHashSet<String>()
        all.addAll(suspected)
        all.addAll(deps)
        all.addAll(mixins)
        all.addAll(issues)
        if (all.isEmpty()) all.addAll(jars)
        return all.take(MAX_RESULTS)
    }
}
