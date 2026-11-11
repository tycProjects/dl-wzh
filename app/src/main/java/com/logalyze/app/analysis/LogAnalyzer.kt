package com.logalyze.app.analysis

import android.content.Context
import android.net.Uri
import com.logalyze.app.ui.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

object LogAnalyzer {
    const val NO_ERROR_EN = "No error found"
    const val NO_ERROR_AR = "لم يتم العثور على خطأ"

    fun isNoError(type: String) = type == NO_ERROR_EN || type == NO_ERROR_AR

    private const val HEAD_CHARS = 100_000
    private const val TAIL_CHARS = 1_500_000
    private const val MAX_LINE = 4_000
    private const val STAGE_DELAY = 250L

    private class Scan(val lines: List<String>, val hits: Array<IntArray>)
    private class Match(val entry: KnownError, val matched: Int, val hits: Int, val score: Int)

    private val errorNameRegex = Regex("([A-Za-z0-9_.]+(?:Exception|Error))")

    /** Stages: 0 read, 1 scan, 2 match, 3 build. onStage is called as each one starts. */
    suspend fun analyze(
        context: Context,
        uri: Uri,
        lang: String,
        onStage: (Int) -> Unit = {}
    ): AnalysisResult {
        onStage(0)
        val raw = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("Cannot open file")
        }
        delay(STAGE_DELAY)

        onStage(1)
        val text = if (raw.length > HEAD_CHARS + TAIL_CHARS) {
            raw.take(HEAD_CHARS) + "\n" + raw.takeLast(TAIL_CHARS)
        } else raw
        val scan = withContext(Dispatchers.Default) { scan(text) }
        delay(STAGE_DELAY)

        onStage(2)
        val best = withContext(Dispatchers.Default) { pick(scan) }
        delay(STAGE_DELAY)

        onStage(3)
        val result = withContext(Dispatchers.Default) {
            val r = if (best != null) buildKnown(best, scan.lines, lang) else buildUnknown(scan.lines, lang)
            r.copy(launcher = detectLauncher(scan.lines), deviceInfo = DeviceInfo.extract(scan.lines),
                mods = ModDetector.detect(scan.lines)
            )
        }
        delay(STAGE_DELAY)
        return result
    }

    // Checked in this order. Zalith and FCL are Pojav-based, so they come first.
    private val launchers = listOf(
        "Zalith Launcher" to listOf("zalithlauncher", "zalith launcher", "zalith"),
        "Fold Craft Launcher" to listOf("foldcraftlauncher", "fold craft launcher", "com.tungsten.fcl"),
        "PojavLauncher" to listOf("pojavlauncher", "net.kdt.pojavlaunch", "pojav"),
        "Prism Launcher" to listOf("prismlauncher", "prism launcher"),
        "PolyMC" to listOf("polymc"),
        "MultiMC" to listOf("multimc"),
        "HMCL" to listOf("hmcl", "hello minecraft! launcher"),
        "TLauncher" to listOf("tlauncher")
    )

    private fun detectLauncher(lines: List<String>): String? {
        val head = lines.asSequence().take(600).joinToString("\n")
        return launchers.firstOrNull { (_, keys) ->
            keys.any { head.contains(it, ignoreCase = true) }
        }?.first
    }

    private fun scan(text: String): Scan {
        val entries = KnowledgeBase.entries
        val lines = text.lines()
        val hits = Array(entries.size) { IntArray(entries[it].regexes.size) }
        for (raw in lines) {
            if (raw.isBlank()) continue
            val line = if (raw.length > MAX_LINE) raw.substring(0, MAX_LINE) else raw
            for (i in entries.indices) {
                val rs = entries[i].regexes
                for (j in rs.indices) {
                    if (rs[j].containsMatchIn(line)) hits[i][j]++
                }
            }
        }
        return Scan(lines, hits)
    }

    private fun pick(scan: Scan): Match? {
        var best: Match? = null
        val entries = KnowledgeBase.entries
        for (i in entries.indices) {
            val h = scan.hits[i]
            val matched = h.count { it > 0 }
            if (matched == 0) continue
            val total = h.sum()
            val score = entries[i].priority * 100 + matched * 10 + minOf(total, 9)
            val current = best
            if (current == null || score > current.score) {
                best = Match(entries[i], matched, total, score)
            }
        }
        return best
    }

    private fun buildKnown(m: Match, lines: List<String>, lang: String): AnalysisResult {
        val e = m.entry
        val t = e.text(lang)
        val evidence = lines.asSequence()
            .filter { l -> e.regexes.any { it.containsMatchIn(l.take(MAX_LINE)) } }
            .map { it.trim().take(200) }
            .distinct()
            .take(3)
            .toList()
        val confidence = minOf(98, e.baseConfidence + (m.matched - 1) * 4 + minOf(m.hits, 3))
        return AnalysisResult(
            errorType = t.type,
            summary = t.summary,
            confidence = confidence,
            rootCause = t.cause,
            evidence = evidence,
            fixes = t.fixes
        )
    }

    private fun buildUnknown(lines: List<String>, lang: String): AnalysisResult {
        val ar = lang == "ar"
        val line = lines.lastOrNull { errorNameRegex.containsMatchIn(it.take(MAX_LINE)) }
        if (line == null) {
            return AnalysisResult(
                errorType = if (ar) NO_ERROR_AR else NO_ERROR_EN,
                summary = if (ar) "لم يُعثر على نمط انهيار معروف في هذا السجل."
                else "No known crash pattern was found in this log.",
                confidence = 20,
                rootCause = if (ar) "لا يحتوي هذا السجل على خطأ واضح. قد يكون جلسة عادية انتهت بدون انهيار، أو أن الانهيار في ملف آخر."
                else "This log has no recognizable error. It may be a normal session that ended without a crash, or the crash is in a different file.",
                evidence = emptyList(),
                fixes = if (ar) listOf(
                    "ارفع تقرير الانهيار من مجلد crash-reports",
                    "ارفع latest.log مباشرة بعد حدوث الانهيار"
                ) else listOf(
                    "Upload the crash report from the crash-reports folder",
                    "Upload latest.log right after the crash happens"
                )
            )
        }
        val name = errorNameRegex.find(line.take(MAX_LINE))!!.groupValues[1].substringAfterLast('.')
        return AnalysisResult(
            errorType = name,
            summary = if (ar) "تم العثور على خطأ لكنه غير موجود في قاعدة المعرفة بعد."
            else "An error was found, but it is not in the knowledge base yet.",
            confidence = 30,
            rootCause = if (ar) "يحتوي السجل على $name، لكن Logalyze لا يملك حلًا مؤكدًا له بعد. قد يشير السطر أدناه إلى المود أو الملف المعني."
            else "The log contains $name, but Logalyze has no verified solution for it yet. The line below may point to the mod or file involved.",
            evidence = listOf(line.trim().take(200)),
            fixes = if (ar) listOf(
                "ابحث في سطر الخطأ عن اسم مود أو مسار ملف",
                "حدّث المودات التي أضفتها مؤخرًا أو احذفها",
                "ابحث عن اسم الخطأ مع إصدار ماينكرافت واللودر"
            ) else listOf(
                "Look for a mod name or a file path in the error line",
                "Update or remove the mods you added most recently",
                "Search for the exact error name with your Minecraft version and loader"
            )
        )
    }
}
