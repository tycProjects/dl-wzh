package id.webtermux

import android.app.Activity
import android.os.Bundle
import android.webkit.*
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream

class MainActivity : Activity() {
    private lateinit var web: WebView
    // Serialize installation and shell execution: cwd and runtime files are shared state.
    private val io = Executors.newSingleThreadExecutor()
    private val runtime by lazy { File(filesDir, "runtime") }
    private val rootfs by lazy { File(runtime, "debian/rootfs") }
    private val proot by lazy { File(runtime, "usr/bin/proot") }
    @Volatile private var cwd = "/root"
    @Volatile private var lastExitCode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = false
        web.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Bridge(), "Android")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    private fun js(name: String, vararg args: String) {
        val encoded = args.joinToString(",") { JSONObject.quote(it) }
        runOnUiThread { if (::web.isInitialized) web.evaluateJavascript("window.$name($encoded)", null) }
    }

    inner class Bridge {
        @JavascriptInterface fun runtimeStatus(): String = when {
            File(rootfs, "bin/bash").exists() && proot.exists() -> "LINUX_READY"
            !android.os.Build.SUPPORTED_ABIS.contains("arm64-v8a") -> "UNSUPPORTED_ABI"
            else -> "NOT_INSTALLED"
        }

        @JavascriptInterface fun installLinux() {
            io.execute {
                try {
                    if (!android.os.Build.SUPPORTED_ABIS.contains("arm64-v8a"))
                        throw IOException("Perangkat ini tidak melaporkan ABI arm64-v8a. Runtime Debian ARM64 tidak dapat dipasang.")
                    if (runtimeStatus() == "LINUX_READY") { js("onInstall", "Linux sudah terpasang.", "100", "done"); return@execute }
                    runtime.mkdirs()
                    js("onInstall", "Menyiapkan direktori runtime…", "2", "working")
                    installTermuxProot()
                    js("onInstall", "Mengunduh Debian Bookworm ARM64… (butuh koneksi dan ruang kosong beberapa GB)", "10", "working")
                    val archive = File(runtime, "debian-bookworm.tar.xz")
                    downloadVerified(
                        "https://github.com/termux/proot-distro/releases/download/v4.17.3/debian-bookworm-aarch64-pd-v4.17.3.tar.xz",
                        archive,
                        "3a841a794ae5999b33e33b329582ed0379d4f54ca62c6ce5a8eb9cff5ef8900b"
                    ) { pct -> js("onInstall", "Mengunduh Debian…", (10 + pct * 0.55).toInt().toString(), "working") }
                    js("onInstall", "Mengekstrak Debian rootfs…", "68", "working")
                    val staging = File(runtime, "debian-staging/rootfs")
                    deleteTree(staging.parentFile!!); staging.mkdirs()
                    extractXzTar(archive, staging)
                    if (!File(staging, "bin/bash").exists() || !File(staging, "etc").exists())
                        throw IOException("Arsip terunduh tetapi struktur Debian tidak valid.")
                    deleteTree(rootfs.parentFile!!)
                    rootfs.parentFile!!.mkdirs()
                    if (!staging.renameTo(rootfs)) throw IOException("Tidak dapat memasang rootfs ke penyimpanan aplikasi.")
                    archive.delete()
                    configureRootfs()
                    if (!proot.canExecute()) proot.setExecutable(true, true)
                    js("onInstall", "Menguji PRoot dan Debian…", "94", "working")
                    val check = runProcess(listOf(proot.absolutePath, "--version"), runtime, 15000)
                    if (check.first != 0) throw IOException("PRoot gagal dijalankan: ${check.second}")
                    val shell = executeBlocking("printf 'WEBTERMUX_LINUX_OK\\n'; uname -m")
                    if (!shell.contains("WEBTERMUX_LINUX_OK")) throw IOException("Debian gagal memulai shell: $shell")
                    js("onInstall", "Debian Bookworm ARM64 berhasil dipasang. Ketik apt update untuk memperbarui paket.", "100", "done")
                } catch (e: Exception) {
                    js("onInstall", "Gagal: ${e.message ?: e.javaClass.simpleName}", "0", "error")
                }
            }
        }

        @JavascriptInterface fun execute(command: String, requestId: String) {
            if (command.length > 16000) { js("onCommand", requestId, "Perintah terlalu panjang.", "1", "true"); return }
            io.execute {
                try {
                    if (runtimeStatus() != "LINUX_READY") {
                        js("onCommand", requestId, "Linux belum terpasang. Tekan INSTALL LINUX terlebih dahulu.", "127", "true")
                        return@execute
                    }
                    val result = executeBlocking(command)
                    js("onCommand", requestId, result, lastExitCode.toString(), "true")
                } catch (e: Exception) {
                    js("onCommand", requestId, "${e.message}", if (lastExitCode == 124) "124" else "1", "true")
                }
            }
        }
    }

    private fun installTermuxProot() {
        val indexUrl = "https://packages.termux.dev/apt/termux-main/dists/stable/main/binary-aarch64/Packages.gz"
        val bytes = downloadBytes(indexUrl)
        val text = GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader().use { it.readText() }
        val blocks = text.split("\n\n")
        val wanted = listOf("libandroid-shmem", "libtalloc", "proot")
        val selected = wanted.map { name ->
            val block = blocks.firstOrNull { Regex("(?m)^Package: ${Regex.escape(name)}\$").containsMatchIn(it) }
                ?: throw IOException("Paket $name tidak ditemukan pada repositori Termux.")
            val fields = block.lines().mapNotNull { line ->
                val i = line.indexOf(':'); if (i > 0) line.substring(0, i) to line.substring(i + 1).trim() else null
            }.toMap()
            Triple(name, fields["Filename"] ?: throw IOException("Filename $name tidak ada"), fields["SHA256"] ?: throw IOException("SHA256 $name tidak ada"))
        }
        val prefix = "https://packages.termux.dev/apt/termux-main/"
        for ((i, item) in selected.withIndex()) {
            val (name, path, sha) = item
            js("onInstall", "Mengunduh komponen PRoot: $name…", (3 + i * 2).toString(), "working")
            val deb = File(runtime, "$name.deb")
            downloadVerified(prefix + path, deb, sha)
            extractDeb(deb, File(runtime, "usr"))
            deb.delete()
        }
        if (!proot.exists()) throw IOException("Paket PRoot tidak berisi executable yang diharapkan.")
        proot.setExecutable(true, true)
    }

    private fun downloadBytes(url: String): ByteArray {
        val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 20000; readTimeout = 60000; instanceFollowRedirects = true }
        try { c.inputStream.use { return it.readBytes() } } finally { c.disconnect() }
    }

    private fun downloadVerified(url: String, target: File, expected: String, progress: ((Int) -> Unit)? = null) {
        val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 30000; readTimeout = 120000; instanceFollowRedirects = true }
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode} saat mengunduh runtime")
            val total = c.contentLengthLong
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            val temp = File(target.parentFile, target.name + ".part")
            c.inputStream.buffered().use { input -> FileOutputStream(temp).buffered().use { output ->
                val buffer = ByteArray(64 * 1024); var n: Int; var last = -1
                while (input.read(buffer).also { n = it } >= 0) {
                    output.write(buffer, 0, n); digest.update(buffer, 0, n); count += n
                    if (total > 0) { val p = (count * 100 / total).toInt(); if (p != last) { last = p; progress?.invoke(p) } }
                }
            } }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(expected, true)) { temp.delete(); throw IOException("SHA-256 tidak cocok untuk ${target.name}. File ditolak demi keamanan.") }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) throw IOException("Gagal menyimpan ${target.name}")
        } finally { c.disconnect() }
    }

    private fun extractDeb(deb: File, destination: File) {
        FileInputStream(deb).use { fis -> ArArchiveInputStream(BufferedInputStream(fis)).use { ar ->
            var entry = ar.nextArEntry
            while (entry != null) {
                if (entry.name.startsWith("data.tar")) {
                    val payload = File(runtime, "deb-data.tmp")
                    FileOutputStream(payload).use { output ->
                        var remaining = entry.size
                        val buffer = ByteArray(32 * 1024)
                        while (remaining > 0) {
                            val n = ar.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (n < 0) throw EOFException("Payload DEB terpotong: ${deb.name}")
                            output.write(buffer, 0, n)
                            remaining -= n
                        }
                    }
                    val raw = FileInputStream(payload)
                    val decoded: InputStream = when {
                        entry.name.endsWith(".xz") -> XZCompressorInputStream(raw)
                        entry.name.endsWith(".gz") -> GZIPInputStream(raw)
                        else -> raw
                    }
                    extractTar(decoded, destination, "data/data/com.termux/files/usr/")
                    payload.delete(); return
                }
                entry = ar.nextArEntry
            }
        } }
        throw IOException("Arsip Debian package rusak: ${deb.name}")
    }

    private fun extractXzTar(archive: File, destination: File) {
        XZCompressorInputStream(BufferedInputStream(FileInputStream(archive))).use { extractTar(it, destination, null) }
    }

    private fun extractTar(input: InputStream, destination: File, stripPrefix: String?) {
        TarArchiveInputStream(BufferedInputStream(input)).use { tar ->
            var e: TarArchiveEntry? = tar.nextTarEntry
            while (e != null) {
                var name = e.name.replace('\\', '/').removePrefix("./")
                if (stripPrefix != null) {
                    name = name.removePrefix("./")
                    val marker = stripPrefix
                    val idx = name.indexOf(marker)
                    if (idx < 0) { e = tar.nextTarEntry; continue }
                    name = name.substring(idx + marker.length)
                } else {
                    val first = name.substringBefore('/')
                    if (first.startsWith("debian-") || first == "debian") name = name.substringAfter('/', "")
                }
                if (name.isBlank() || name.startsWith("/") || name.split('/').any { it == ".." }) { e = tar.nextTarEntry; continue }
                val out = File(destination, name)
                if (!out.canonicalPath.startsWith(destination.canonicalPath + File.separator)) { e = tar.nextTarEntry; continue }
                when {
                    e.isDirectory -> out.mkdirs()
                    e.isSymbolicLink -> {
                        out.parentFile?.mkdirs(); if (out.exists() || java.nio.file.Files.isSymbolicLink(out.toPath())) out.delete()
                        try { java.nio.file.Files.createSymbolicLink(out.toPath(), java.nio.file.Paths.get(e.linkName)) } catch (_: Exception) { }
                    }
                    e.isLink -> {
                        out.parentFile?.mkdirs(); val link = File(destination, e.linkName.removePrefix("./"))
                        if (link.exists()) try { link.copyTo(out, true) } catch (_: Exception) { }
                    }
                    e.isFile -> {
                        out.parentFile?.mkdirs(); FileOutputStream(out).use { output ->
                            // TarArchiveInputStream spans the whole archive; copy ONLY this entry.
                            var remaining = e.size
                            val buffer = ByteArray(32 * 1024)
                            while (remaining > 0) {
                                val n = tar.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                                if (n < 0) throw EOFException("Entri TAR terpotong: $name")
                                output.write(buffer, 0, n)
                                remaining -= n
                            }
                        }
                        if ((e.mode and 73) != 0) out.setExecutable(true, true)
                    }
                }
                e = tar.nextTarEntry
            }
        }
    }

    private fun configureRootfs() {
        val etc = File(rootfs, "etc"); etc.mkdirs()
        File(etc, "resolv.conf").apply { if (exists()) delete(); writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n") }
        File(etc, "hosts").apply { if (!exists()) writeText("127.0.0.1 localhost\n::1 localhost\n") }
        File(rootfs, "root").mkdirs()
        File(rootfs, "root/.bashrc").apply { if (!exists()) writeText("export TERM=xterm-256color\nexport PS1='\\[\\e[1;32m\\]root@webtermux\\[\\e[0m\\]:\\w# '\n") }
    }

    private fun executeBlocking(command: String): String {
        // A command may delete its own working directory; recover to /root before launching PRoot.
        val safeCwd = cwd.takeIf { it.startsWith("/") && File(rootfs, it.trimStart('/')).isDirectory } ?: "/root"
        cwd = safeCwd
        val wrapped = "cd ${shellQuote(safeCwd)} 2>/dev/null || cd /root; $command; __wt_rc=\$?; printf '\\n__WT_META__%s\\t%s\\n' \"\$PWD\" \"\$__wt_rc\""
        val args = mutableListOf(proot.absolutePath, "--link2symlink", "--kill-on-exit", "--rootfs=${rootfs.absolutePath}", "--bind=/dev", "--bind=/proc", "--bind=/sys")
        listOf("/system", "/apex", "/storage").forEach { if (File(it).exists()) args.add("--bind=$it") }
        args.addAll(listOf("--cwd=$safeCwd", "/usr/bin/env", "HOME=/root", "USER=root", "LOGNAME=root", "TERM=xterm-256color", "LANG=C.UTF-8", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "/bin/bash", "--noprofile", "--norc", "-lc", wrapped))
        val capture = File(runtime, "command-output.tmp")
        val pb = ProcessBuilder(args).directory(runtime).redirectErrorStream(true).redirectOutput(capture)
        val env = pb.environment(); env["LD_LIBRARY_PATH"] = File(runtime, "usr/lib").absolutePath
        val p = pb.start()
        if (!p.waitFor(10, java.util.concurrent.TimeUnit.MINUTES)) {
            p.destroyForcibly()
            p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            lastExitCode = 124
            val partial = if (capture.exists()) capture.readText() else ""
            capture.delete()
            throw IOException("Perintah melewati batas waktu 10 menit.\n$partial")
        }
        lastExitCode = p.exitValue()
        val output = StringBuilder()
        if (capture.exists()) capture.useLines { lines -> lines.forEach { line ->
            if (line.startsWith("__WT_META__")) {
                val fields = line.removePrefix("__WT_META__").split('\t')
                if (fields.size >= 2) {
                    if (fields[0].startsWith("/")) cwd = fields[0]
                    lastExitCode = fields[1].toIntOrNull() ?: 1
                }
            } else output.append(line).append('\n')
        } }
        capture.delete()
        return output.toString().trimEnd()
    }

    private fun runProcess(args: List<String>, dir: File, timeout: Long): Pair<Int, String> {
        val capture = File(runtime, "proot-version-check.log")
        val p = ProcessBuilder(args).directory(dir)
            .apply { environment()["LD_LIBRARY_PATH"] = File(runtime, "usr/lib").absolutePath }
            .redirectErrorStream(true).redirectOutput(capture).start()
        if (!p.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            p.destroyForcibly()
            p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
            val partial = if (capture.exists()) capture.readText() else ""
            capture.delete()
            return 124 to partial
        }
        val text = if (capture.exists()) capture.readText() else ""
        capture.delete()
        return p.exitValue() to text
    }

    private fun shellQuote(s: String) = "'" + s.replace("'", "'\\''") + "'"
    private fun deleteTree(file: File) { if (file.isDirectory) file.listFiles()?.forEach { deleteTree(it) }; file.delete() }

    override fun onDestroy() { io.shutdownNow(); if (::web.isInitialized) web.destroy(); super.onDestroy() }
}
