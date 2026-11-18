package com.glassdex.analyzer.analysis

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Entry point: take a picked file, work out what it is, analyze it, write results to disk. */
object Analyzer {

    fun outputRoot(ctx: Context) = File(ctx.filesDir, "output").also { it.mkdirs() }

    fun analyze(ctx: Context, uri: Uri, vtKey: String?, progress: (Progress) -> Unit): AnalysisReport {
        val t0 = System.currentTimeMillis()
        progress(Progress("Reading file", 0.01f))

        val name = displayName(ctx, uri) ?: "file"
        val cache = File(ctx.cacheDir, "in_${System.nanoTime()}.bin")
        ctx.contentResolver.openInputStream(uri)?.use { i -> cache.outputStream().use { o -> i.copyTo(o, 256 * 1024) } }
            ?: error("Cannot open the selected file")

        try {
            progress(Progress("Hashing", 0.04f))
            val hashes = FileInputStream(cache).let { Hashing.all(it) }
            val size = cache.length()
            val kind = detectKind(cache, name)

            val safe = name.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_.-]"), "_").take(40)
            val out = File(outputRoot(ctx), "${safe}_${System.currentTimeMillis() / 1000}").also { it.mkdirs() }

            var appInfo: AppInfo? = null
            var peInfo: PeInfo? = null
            var findings: List<Finding> = emptyList()
            var urls: List<String> = emptyList()
            var fileCount = 0
            var subApks: List<String> = emptyList()

            when (kind) {
                FileKind.APK -> {
                    val r = ApkAnalyzer.analyze(cache, out, progress, 0.06f, 0.70f)
                    appInfo = r.info; findings = r.findings; urls = r.urls; fileCount = r.fileCount
                }
                FileKind.XAPK -> {
                    val x = analyzeXapk(cache, out, progress)
                    appInfo = x.info; findings = x.findings; urls = x.urls; fileCount = x.fileCount; subApks = x.subApks
                }
                FileKind.EXE -> {
                    progress(Progress("Parsing PE headers", 0.2f))
                    peInfo = PeParser.parse(cache)
                    if (peInfo != null) {
                        val (f, u) = MalwareScanner.scanPe(peInfo, size)
                        findings = f; urls = u
                        progress(Progress("Extracting strings", 0.5f))
                        val strs = extractStrings(cache)
                        File(out, "strings.txt").writeText(strs.joinToString("\n"))
                        urls = strs.filter { it.startsWith("http://") || it.startsWith("https://") }.distinct().take(150)
                        File(out, "pe_report.txt").writeText(peText(peInfo))
                        fileCount = 2
                    } else findings = listOf(Finding("Invalid PE", "Header is damaged or not a real PE file.", 10))
                }
                FileKind.UNKNOWN -> findings = listOf(Finding("Unknown format", "Not an APK, XAPK or Windows executable. Only hashes were computed.", 0))
            }

            progress(Progress("Checking VirusTotal", 0.88f))
            val vt = VirusTotal.lookup(hashes.sha256, vtKey)

            var score = findings.sumOf { it.points }
            if (vt.state == "found") {
                score += vt.malicious * 6 + vt.suspicious * 2
                if (vt.malicious == 0 && vt.suspicious == 0 && vt.harmless + vt.undetected > 20) score = (score * 0.6).toInt()
            }
            score = score.coerceIn(0, 100)
            var risk = MalwareScanner.toRisk(score)
            if (vt.state == "found" && vt.malicious >= 5) risk = Risk.HIGH

            val report = AnalysisReport(
                fileName = name, kind = kind, sizeBytes = size, hashes = hashes,
                appInfo = appInfo, peInfo = peInfo,
                findings = findings.sortedByDescending { it.points },
                riskScore = score, risk = risk, vt = vt,
                outputDir = out.absolutePath, fileCount = fileCount,
                tookMs = System.currentTimeMillis() - t0,
                subApks = subApks, urls = urls
            )
            File(out, "REPORT.txt").writeText(reportText(report))
            progress(Progress("Done", 1f))
            return report
        } finally {
            cache.delete()
        }
    }

    private class XapkResult(val info: AppInfo, val findings: List<Finding>, val urls: List<String>, val fileCount: Int, val subApks: List<String>)

    private fun analyzeXapk(file: File, out: File, progress: (Progress) -> Unit): XapkResult {
        progress(Progress("Opening XAPK", 0.08f))
        val contents = File(out, "xapk_contents").also { it.mkdirs() }
        val apkNames = ArrayList<String>()
        var count = 0
        var manifestPkg: String? = null
        ZipFile(file).use { z ->
            val root = contents.canonicalPath + File.separator
            for (e in z.entries()) {
                if (e.isDirectory) continue
                val t = File(contents, e.name)
                if (!t.canonicalPath.startsWith(root)) continue
                t.parentFile?.mkdirs()
                z.getInputStream(e).use { i -> t.outputStream().use { o -> i.copyTo(o, 256 * 1024) } }
                count++
                if (e.name.endsWith(".apk", true)) apkNames += e.name
                if (e.name == "manifest.json") {
                    manifestPkg = Regex("\"package_name\"\\s*:\\s*\"([^\"]+)\"").find(t.readText())?.groupValues?.get(1)
                }
            }
        }
        require(apkNames.isNotEmpty()) { "No APK found inside this XAPK" }
        val primary = apkNames.firstOrNull { manifestPkg != null && it.substringAfterLast('/') == "$manifestPkg.apk" }
            ?: apkNames.maxByOrNull { File(contents, it).length() }!!

        progress(Progress("Analyzing base APK", 0.15f))
        val r = ApkAnalyzer.analyze(File(contents, primary), File(out, "base_apk"), progress, 0.15f, 0.65f)

        // Extra finding if splits contain code of their own
        val extra = ArrayList<Finding>()
        val others = apkNames.filter { it != primary }
        for (o in others) {
            try {
                ZipFile(File(contents, o)).use { z ->
                    val dex = z.entries().toList().count { it.name.endsWith(".dex") }
                    if (dex > 0) extra += Finding("Split APK has its own code", "$o contains $dex dex file(s). Check it separately.", 2)
                }
            } catch (_: Throwable) { }
        }
        return XapkResult(r.info, r.findings + extra, r.urls, r.fileCount + count, apkNames)
    }

    fun detectKind(f: File, name: String): FileKind {
        FileInputStream(f).use { ins ->
            val h = ByteArray(4); val n = ins.read(h)
            if (n >= 2 && h[0] == 'M'.code.toByte() && h[1] == 'Z'.code.toByte()) return FileKind.EXE
            if (n >= 4 && h[0] == 'P'.code.toByte() && h[1] == 'K'.code.toByte()) {
                return try {
                    ZipFile(f).use { z ->
                        val names = z.entries().toList().map { it.name }
                        when {
                            "AndroidManifest.xml" in names -> FileKind.APK
                            names.any { it.endsWith(".apk", true) } -> FileKind.XAPK
                            else -> FileKind.UNKNOWN
                        }
                    }
                } catch (_: Throwable) { FileKind.UNKNOWN }
            }
        }
        return FileKind.UNKNOWN
    }

    private fun displayName(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Throwable) { null } ?: uri.lastPathSegment

    /** printable ASCII runs >= 6 chars */
    private fun extractStrings(f: File, max: Int = 50_000): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        FileInputStream(f).buffered(256 * 1024).use { ins ->
            while (out.size < max) {
                val c = ins.read(); if (c < 0) break
                if (c in 32..126) sb.append(c.toChar())
                else { if (sb.length >= 6) out += sb.toString(); sb.setLength(0) }
                if (sb.length > 400) { out += sb.toString(); sb.setLength(0) }
            }
        }
        return out
    }

    private fun peText(p: PeInfo) = buildString {
        appendLine("Machine: ${p.machine}  (${if (p.is64) "PE32+" else "PE32"})  Subsystem: ${p.subsystem}  DLL: ${p.isDll}  .NET: ${p.dotNet}")
        appendLine("Compile time: ${p.timestamp}   Entry point: ${p.entryPoint}   Signed: ${p.signed}   Packer: ${p.packer ?: "none detected"}")
        appendLine("\nSections:")
        p.sections.forEach { appendLine("  %-8s raw=%-9d virt=%-9d entropy=%.2f %s%s".format(it.name, it.rawSize, it.virtualSize, it.entropy, if (it.executable) "X" else "-", if (it.writable) "W" else "-")) }
        appendLine("\nImports:")
        p.imports.forEach { (d, fns) -> appendLine("  $d (${fns.size})"); fns.forEach { appendLine("      $it") } }
    }

    fun reportText(r: AnalysisReport) = buildString {
        appendLine("GlassDex report")
        appendLine("File: ${r.fileName}  (${r.kind}, ${r.sizeBytes} bytes)")
        appendLine("MD5:    ${r.hashes.md5}\nSHA1:   ${r.hashes.sha1}\nSHA256: ${r.hashes.sha256}")
        appendLine("Risk: ${r.risk.label} (score ${r.riskScore}/100)   Analysis time: ${r.tookMs} ms")
        appendLine("VirusTotal: ${r.vt.state} ${if (r.vt.state == "found") "malicious=${r.vt.malicious} suspicious=${r.vt.suspicious} harmless=${r.vt.harmless} undetected=${r.vt.undetected}" else r.vt.message ?: ""}")
        r.appInfo?.let { a ->
            appendLine("\nApp: ${a.label}  package=${a.packageName}  v${a.versionName} (${a.versionCode})  minSdk=${a.minSdk} targetSdk=${a.targetSdk}")
            appendLine("Dex files=${a.dexCount} classes=${a.classCount} methods=${a.methodCount} strings=${a.stringCount}")
            appendLine("Permissions (${a.permissions.size}):"); a.permissions.forEach { appendLine("  ${if (it.dangerous) "[!]" else "   "} ${it.name} ${it.note}") }
            a.certs.forEach { appendLine("Cert: ${it.subject}  valid ${it.validFrom}..${it.validTo}  SHA-256 ${it.sha256}") }
        }
        r.peInfo?.let { appendLine("\n" + peText(it)) }
        appendLine("\nFindings:")
        r.findings.forEach { appendLine("  +${it.points}  ${it.title}: ${it.detail}") }
        if (r.urls.isNotEmpty()) { appendLine("\nEndpoints:"); r.urls.forEach { appendLine("  $it") } }
    }

    /** Zips a finished output folder so it can be shared. */
    fun zipOutput(dir: File): File {
        val zip = File(dir.parentFile, dir.name + ".zip")
        ZipOutputStream(zip.outputStream().buffered()).use { zo ->
            val base = dir.toPath()
            dir.walkTopDown().filter { it.isFile }.forEach { f ->
                zo.putNextEntry(ZipEntry(base.relativize(f.toPath()).toString().replace(File.separatorChar, '/')))
                f.inputStream().use { it.copyTo(zo, 128 * 1024) }
                zo.closeEntry()
            }
        }
        return zip
    }
}
