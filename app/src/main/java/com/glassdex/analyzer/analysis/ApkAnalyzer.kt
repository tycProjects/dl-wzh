package com.glassdex.analyzer.analysis

import java.io.File
import java.io.FileInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipFile
import com.glassdex.analyzer.analysis.AxmlParser

class ApkResult(
    val info: AppInfo,
    val findings: List<Finding>,
    val urls: List<String>,
    val fileCount: Int
)

/** Unpacks one APK, decodes manifest/resources, reads DEX, scans for risk. */
object ApkAnalyzer {

    private const val MAX_ENTRIES = 200_000
    private const val MAX_TOTAL_BYTES = 4L * 1024 * 1024 * 1024

    fun analyze(apk: File, outDir: File, progress: (Progress) -> Unit, base: Float, span: Float): ApkResult {
        outDir.mkdirs()
        val extracted = File(outDir, "extracted").also { it.mkdirs() }
        val decoded = File(outDir, "decoded").also { it.mkdirs() }
        val smali = File(outDir, "classes_smali").also { it.mkdirs() }

        ZipFile(apk).use { zip ->
            val entries = zip.entries().toList()
            require(entries.size <= MAX_ENTRIES) { "Archive has too many entries" }

            // ---- 1. extract everything (real, usable files) ----
            progress(Progress("Extracting files", base + span * 0.05f))
            var total = 0L
            val rootPath = extracted.canonicalPath + File.separator
            var fileCount = 0
            val buf = ByteArray(128 * 1024)
            for ((i, e) in entries.withIndex()) {
                if (e.isDirectory) continue
                val target = File(extracted, e.name)
                if (!target.canonicalPath.startsWith(rootPath)) continue // zip-slip guard
                target.parentFile?.mkdirs()
                zip.getInputStream(e).use { ins ->
                    target.outputStream().use { outs ->
                        while (true) {
                            val n = ins.read(buf); if (n < 0) break
                            outs.write(buf, 0, n)
                            total += n
                        }
                    }
                }
                require(total <= MAX_TOTAL_BYTES) { "Archive expands beyond 4 GB (zip bomb?)" }
                fileCount++
                if (i % 200 == 0) progress(Progress("Extracting files", base + span * (0.05f + 0.35f * i / entries.size)))
            }

            // ---- 2. decode binary XML ----
            progress(Progress("Decoding manifest & resources", base + span * 0.42f))
            var manifestParser: AxmlParser? = null
            var resXml = 0
            for (e in entries) {
                if (e.isDirectory) continue
                val n = e.name
                val isManifest = n == "AndroidManifest.xml"
                if (isManifest || (n.startsWith("res/") && n.endsWith(".xml"))) {
                    val bytes = zip.getInputStream(e).use { it.readBytes() }
                    val p = AxmlParser(bytes)
                    if (p.xml.isNotEmpty()) {
                        val t = File(decoded, n)
                        t.parentFile?.mkdirs()
                        t.writeText(p.xml)
                        if (!isManifest) resXml++
                    }
                    if (isManifest) manifestParser = p
                }
            }

            val root = manifestParser?.root
            val pkg = root?.attrs?.get("package")
            val appNode = root?.children?.firstOrNull { it.name == "application" }

            fun comp(tag: String) = appNode?.children?.filter { it.name == tag }?.mapNotNull { n ->
                n.attrs["name"]?.let { nm -> if (nm.startsWith(".") && pkg != null) pkg + nm else nm }
            } ?: emptyList()

            val perms = root?.children?.filter { it.name == "uses-permission" || it.name == "uses-permission-sdk-23" }
                ?.mapNotNull { it.attrs["name"] }?.distinct()?.map { MalwareScanner.permissionInfo(it) } ?: emptyList()

            val sdk = root?.children?.firstOrNull { it.name == "uses-sdk" }
            val rawLabel = appNode?.attrs?.get("label")
            val label = rawLabel?.takeIf { !it.startsWith("@") } ?: pkg

            // ---- 3. certificates ----
            progress(Progress("Reading signature", base + span * 0.5f))
            val certs = readCerts(zip, entries.map { it.name })

            // ---- 4. DEX ----
            val dexEntries = entries.filter { Regex("classes\\d*\\.dex").matches(it.name) }.sortedWith(compareBy({ it.name.length }, { it.name }))
            val allStrings = ArrayList<String>()
            val allRefs = HashSet<String>()
            var classes = 0; var methods = 0
            val written = intArrayOf(0)
            for ((i, de) in dexEntries.withIndex()) {
                progress(Progress("Decompiling ${de.name}", base + span * (0.55f + 0.3f * i / dexEntries.size.coerceAtLeast(1))))
                try {
                    val bytes = zip.getInputStream(de).use { it.readBytes() }
                    val r = DexParser.parse(bytes, File(smali, de.name.removeSuffix(".dex")), written)
                    classes += r.classCount; methods += r.methodCount
                    allStrings += r.strings; allRefs += r.methodRefs
                } catch (_: Throwable) { }
            }
            if (allStrings.isNotEmpty()) {
                File(outDir, "strings.txt").bufferedWriter().use { w -> allStrings.asSequence().take(300_000).forEach { w.write(it.replace('\n', ' ')); w.newLine() } }
            }

            // ---- 5. assemble info ----
            val libs = entries.filter { it.name.startsWith("lib/") && it.name.endsWith(".so") }.map { it.name }
            val abis = libs.mapNotNull { it.split('/').getOrNull(1) }.distinct()
            val embedded = entries.any {
                (it.name.startsWith("assets/") || it.name.startsWith("res/raw/")) &&
                    (it.name.endsWith(".apk") || it.name.endsWith(".dex") || it.name.endsWith(".jar"))
            }
            val icon = entries.map { it.name }.filter { it.startsWith("res/") && it.contains("ic_launcher") && (it.endsWith(".png") || it.endsWith(".webp")) && !it.contains("foreground") && !it.contains("background") }
                .maxByOrNull { densityRank(it) }

            val info = AppInfo(
                label = label,
                packageName = pkg,
                versionName = root?.attrs?.get("versionName"),
                versionCode = root?.attrs?.get("versionCode"),
                minSdk = sdk?.attrs?.get("minSdkVersion"),
                targetSdk = sdk?.attrs?.get("targetSdkVersion"),
                permissions = perms,
                activities = comp("activity") + comp("activity-alias"),
                services = comp("service"),
                receivers = comp("receiver"),
                providers = comp("provider"),
                debuggable = appNode?.attrs?.get("debuggable") == "true",
                allowBackup = appNode?.attrs?.get("allowBackup") != "false",
                cleartextTraffic = appNode?.attrs?.get("usesCleartextTraffic") == "true",
                certs = certs,
                nativeLibs = libs,
                abis = abis,
                dexCount = dexEntries.size,
                classCount = classes,
                methodCount = methods,
                stringCount = allStrings.size,
                resourceFileCount = resXml,
                iconPath = icon?.let { File(extracted, it).absolutePath }
            )

            progress(Progress("Scanning for malware patterns", base + span * 0.9f))
            val (findings, urls) = MalwareScanner.scanApk(info, allRefs, allStrings, embedded)
            return ApkResult(info, findings, urls, fileCount)
        }
    }

    private fun densityRank(path: String): Int = when {
        path.contains("xxxhdpi") -> 5
        path.contains("xxhdpi") -> 4
        path.contains("xhdpi") -> 3
        path.contains("hdpi") -> 2
        path.contains("mdpi") -> 1
        else -> 0
    }

    private fun readCerts(zip: ZipFile, names: List<String>): List<CertInfo> {
        val out = ArrayList<CertInfo>()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = System.currentTimeMillis()
        for (n in names) {
            if (!n.startsWith("META-INF/") || !(n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC"))) continue
            try {
                zip.getInputStream(zip.getEntry(n)).use { ins ->
                    val certs = CertificateFactory.getInstance("X.509").generateCertificates(ins)
                    for (c in certs) {
                        val x = c as? X509Certificate ?: continue
                        out += CertInfo(
                            subject = x.subjectX500Principal.name,
                            issuer = x.issuerX500Principal.name,
                            validFrom = fmt.format(x.notBefore),
                            validTo = fmt.format(x.notAfter),
                            sha256 = Hashing.sha256(x.encoded).chunked(2).joinToString(":").uppercase(),
                            selfSigned = x.subjectX500Principal == x.issuerX500Principal,
                            expired = x.notAfter.time < now
                        )
                    }
                }
            } catch (_: Throwable) { }
        }
        return out.distinctBy { it.sha256 }
    }
}
