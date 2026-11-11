package com.logalyze.app.analysis

import java.util.Locale

/** Reads device details (name, Android, CPU, RAM...) from the header of a log. */
object DeviceInfo {
    private const val HEAD_LINES = 600
    private const val MAX_LINE = 300

    private fun rule(labels: String) =
        Regex("(?<![A-Za-z])(?:$labels)\\s*[:=]\\s*(.+)\$", RegexOption.IGNORE_CASE)

    private val brandRx = rule("brand|manufacturer")
    private val modelRx = rule("device model|device name|phone model|device|model")
    private val releaseRx = rule("android version|release version|os version|android release")
    private val apiRx = rule("android api|api level|sdk version|sdk int|api version")
    private val cpuRx = rule("cpu model|cpu|processor|soc|chipset|hardware|model name")
    private val archRx = rule("architecture|arch|cpu abi|supported abis")
    private val ramRx = Regex(
        "(?:(?<![A-Za-z])(?:total ram|device ram|total memory|physical memory|ram size|ram total)|^\\s*(?:info\\s*:\\s*)?ram)\\s*[:=]\\s*(.+)\$",
        RegexOption.IGNORE_CASE
    )
    private val javaRx = rule("java version|java runtime|java vm version")
    private val rendererRx = rule("gl renderer|opengl renderer|renderer")
    private val gpuRx = rule("gpu|gl vendor|graphics card")
    private val memTotalRx = Regex("MemTotal\\s*:\\s*(\\d+)\\s*kB", RegexOption.IGNORE_CASE)

    private fun grab(rx: Regex, line: String): String? {
        val v = rx.find(line)?.groupValues?.get(1) ?: return null
        val c = v.trim().trimEnd(',', ';').take(80).trim()
        return c.ifEmpty { null }
    }

    /** Returns (key, value) pairs. Keys: device, android, cpu, arch, ram, java, renderer, gpu. */
    fun extract(lines: List<String>): List<Pair<String, String>> {
        var brand: String? = null
        var model: String? = null
        var release: String? = null
        var api: String? = null
        var cpu: String? = null
        var arch: String? = null
        var ram: String? = null
        var java: String? = null
        var renderer: String? = null
        var gpu: String? = null

        for (raw in lines.asSequence().take(HEAD_LINES)) {
            if (raw.isBlank()) continue
            val line = if (raw.length > MAX_LINE) raw.substring(0, MAX_LINE) else raw
            if (brand == null) brand = grab(brandRx, line)
            if (model == null) model = grab(modelRx, line)
            if (release == null) release = grab(releaseRx, line)
            if (api == null) api = grab(apiRx, line)
            if (cpu == null) cpu = grab(cpuRx, line)
            if (arch == null) arch = grab(archRx, line)
            if (ram == null) {
                ram = grab(ramRx, line)
                if (ram == null) {
                    val kb = memTotalRx.find(line)?.groupValues?.get(1)?.toLongOrNull()
                    if (kb != null) ram = String.format(Locale.US, "%.1f GB", kb / 1024.0 / 1024.0)
                }
            }
            if (java == null) java = grab(javaRx, line)
            if (renderer == null) renderer = grab(rendererRx, line)
            if (gpu == null) gpu = grab(gpuRx, line)
        }

        val out = mutableListOf<Pair<String, String>>()

        val b = brand
        val m = model
        val deviceName = when {
            b != null && m != null -> if (m.startsWith(b, ignoreCase = true)) m else "$b $m"
            else -> m ?: b
        }
        if (deviceName != null) out += "device" to deviceName

        val rel = release?.let { if (it.startsWith("android", ignoreCase = true)) it else "Android $it" }
        val a = api
        val androidValue = when {
            rel != null && a != null -> "$rel (API $a)"
            rel != null -> rel
            a != null -> "API $a"
            else -> null
        }
        if (androidValue != null) out += "android" to androidValue

        cpu?.let { out += "cpu" to it }
        arch?.let { out += "arch" to it }
        ram?.let { out += "ram" to it }
        java?.let { out += "java" to it }
        renderer?.let { out += "renderer" to it }
        gpu?.let { out += "gpu" to it }
        return out
    }
}
