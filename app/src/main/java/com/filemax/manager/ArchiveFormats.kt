package com.filemax.manager

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

enum class ArchiveKind(val label: String) {
    ZIP("ZIP"), SEVENZ("7Z"), RAR("RAR"), TAR("TAR"), TAR_GZ("TAR.GZ"), TAR_BZ2("TAR.BZ2"), TAR_XZ("TAR.XZ"),
    GZ("GZ"), BZ2("BZ2"), XZ("XZ"), ARJ("ARJ"), ZST("ZST"), TAR_ZST("TAR.ZST"), ISO("ISO"), CAB("CAB"), LZH("LZH"), UNKNOWN("?")
}

data class ArchiveEntryInfo(
    val path: String, val isDir: Boolean, val size: Long, val packed: Long = -1,
    val modified: Long = 0, val crc: Long = -1, val method: String = "", val encrypted: Boolean = false
) { val name: String get() = path.substringAfterLast('/') }

class PasswordException(val wrong: Boolean) : Exception(if (wrong) "كلمة المرور غير صحيحة" else "الأرشيف محمي بكلمة مرور")
class CancelledByUser : Exception("أُلغيت العملية")

/** Shared progress/cancel state, read by the UI thread and written by workers. */
class TaskControl {
    @Volatile var cancelled = false
    @Volatile var paused = false
    /** Blocking callback used when the policy is ASK; [stickyPolicy] stores an "apply to all" answer. */
    @Volatile var onExists: ((File) -> ExistsPolicy)? = null
    @Volatile var stickyPolicy: ExistsPolicy? = null
    @Volatile var done = 0L
    @Volatile var total = 0L
    @Volatile var label = ""
    fun add(n: Long) { while (paused && !cancelled) Thread.sleep(100); if (cancelled) throw CancelledByUser(); done += n }
    fun check() { if (cancelled) throw CancelledByUser() }
    val fraction: Float get() = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else -1f
}

enum class ExistsPolicy { OVERWRITE, SKIP, RENAME, ASK }

object ArchiveReader {
    private val EXTS = listOf(".tar.gz" to ArchiveKind.TAR_GZ, ".tgz" to ArchiveKind.TAR_GZ, ".tar.bz2" to ArchiveKind.TAR_BZ2,
        ".tbz2" to ArchiveKind.TAR_BZ2, ".tbz" to ArchiveKind.TAR_BZ2, ".tar.xz" to ArchiveKind.TAR_XZ, ".txz" to ArchiveKind.TAR_XZ,
        ".tar.zst" to ArchiveKind.TAR_ZST, ".tzst" to ArchiveKind.TAR_ZST, ".zst" to ArchiveKind.ZST, ".iso" to ArchiveKind.ISO,
        ".tar" to ArchiveKind.TAR, ".zip" to ArchiveKind.ZIP, ".jar" to ArchiveKind.ZIP, ".apk" to ArchiveKind.ZIP,
        ".epub" to ArchiveKind.ZIP, ".7z" to ArchiveKind.SEVENZ, ".rar" to ArchiveKind.RAR, ".gz" to ArchiveKind.GZ,
        ".bz2" to ArchiveKind.BZ2, ".xz" to ArchiveKind.XZ, ".arj" to ArchiveKind.ARJ,
        ".cab" to ArchiveKind.CAB, ".lzh" to ArchiveKind.LZH, ".lha" to ArchiveKind.LZH)

    fun kindByName(name: String): ArchiveKind {
        val n = name.lowercase().removeSuffix(".001")
        return EXTS.firstOrNull { n.endsWith(it.first) }?.second ?: ArchiveKind.UNKNOWN
    }
    fun isArchiveName(name: String) = kindByName(name) != ArchiveKind.UNKNOWN

    /** Detects by magic bytes, falling back to the extension (needed to tell TAR.* from single-file GZ/BZ2/XZ). */
    fun detect(file: File): ArchiveKind {
        val byName = kindByName(file.name)
        if (byName == ArchiveKind.ISO && isIso(file)) return ArchiveKind.ISO
        val head = ByteArray(265)
        val n = runCatching { file.inputStream().use { it.read(head) } }.getOrDefault(-1)
        if (n < 4) return byName
        fun at(vararg b: Int) = b.indices.all { (head[it].toInt() and 0xFF) == b[it] }
        val magic = when {
            at(0x50, 0x4B) -> ArchiveKind.ZIP
            at(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C) -> ArchiveKind.SEVENZ
            at(0x52, 0x61, 0x72, 0x21) -> ArchiveKind.RAR
            at(0x60, 0xEA) -> ArchiveKind.ARJ
            at(0x4D, 0x53, 0x43, 0x46) -> ArchiveKind.CAB
            n >= 7 && head[2].toInt() == 0x2D && head[3].toInt() == 0x6C && (head[4].toInt() == 0x68 || head[4].toInt() == 0x7A) && head[6].toInt() == 0x2D -> ArchiveKind.LZH
            at(0x1F, 0x8B) -> if (byName == ArchiveKind.TAR_GZ) ArchiveKind.TAR_GZ else ArchiveKind.GZ
            at(0x42, 0x5A, 0x68) -> if (byName == ArchiveKind.TAR_BZ2) ArchiveKind.TAR_BZ2 else ArchiveKind.BZ2
            at(0x28, 0xB5, 0x2F, 0xFD) -> if (byName == ArchiveKind.TAR_ZST) ArchiveKind.TAR_ZST else ArchiveKind.ZST
            at(0xFD, 0x37, 0x7A, 0x58, 0x5A) -> if (byName == ArchiveKind.TAR_XZ) ArchiveKind.TAR_XZ else ArchiveKind.XZ
            n >= 262 && String(head, 257, 5, Charsets.ISO_8859_1) == "ustar" -> ArchiveKind.TAR
            else -> ArchiveKind.UNKNOWN
        }
        return if (magic == ArchiveKind.UNKNOWN) byName else magic
    }

    private fun isIso(f: File) = runCatching { java.io.RandomAccessFile(f, "r").use { r -> val b = ByteArray(5); r.seek(0x8001); r.readFully(b); String(b, Charsets.ISO_8859_1) == "CD001" } }.getOrDefault(false)

    /** True when the extension does not match the real content (shown as a warning). */
    fun extensionMismatch(file: File): Boolean {
        val a = kindByName(file.name); val b = detect(file)
        fun fam(k: ArchiveKind) = when (k) { ArchiveKind.TAR_GZ, ArchiveKind.GZ -> 1; ArchiveKind.TAR_BZ2, ArchiveKind.BZ2 -> 2; ArchiveKind.TAR_XZ, ArchiveKind.XZ -> 3; ArchiveKind.TAR_ZST, ArchiveKind.ZST -> 4; else -> k.ordinal + 10 }
        return a != ArchiveKind.UNKNOWN && fam(a) != fam(b)
    }

    /** Joins name.ext.001, .002 ... into a single cache file; returns the input when it is not a first volume. */
    fun joinVolumes(first: File, cacheDir: File): File {
        if (!first.name.endsWith(".001")) return first
        cacheDir.mkdirs()
        val out = File(cacheDir, first.name.removeSuffix(".001"))
        out.outputStream().buffered().use { o ->
            var i = 1
            while (true) {
                val part = File(first.parentFile, first.name.removeSuffix(".001") + ".%03d".format(i))
                if (!part.isFile) { if (i == 1) throw IOException("الجزء الأول غير موجود"); break }
                part.inputStream().use { it.copyTo(o, 64 * 1024) }; i++
            }
        }
        return out
    }

    fun comment(file: File): String? = runCatching { if (detect(file) == ArchiveKind.ZIP) ZipFile(file).comment?.takeIf { it.isNotBlank() } else null }.getOrNull()

    fun list(file: File, password: CharArray? = null, ctl: TaskControl = TaskControl()): List<ArchiveEntryInfo> {
        val out = ArrayList<ArchiveEntryInfo>()
        visit(file, password, ctl) { info, _ -> out += info }
        return out
    }

    /** Reads every entry fully (CRC/format verification). Returns the number of entries; throws on damage. */
    fun test(file: File, password: CharArray? = null, ctl: TaskControl = TaskControl()): Int {
        var count = 0
        val sink = object : OutputStream() { override fun write(b: Int) {}; override fun write(b: ByteArray, o: Int, l: Int) {} }
        visit(file, password, ctl) { info, write -> if (!info.isDir) write(sink); count++ }
        return count
    }

    /**
     * Extracts [selection] (entry paths; folders include their children) or everything when null.
     * Every target is canonical-path checked (zip-slip). Partial output is removed on failure/cancel.
     */
    fun extract(file: File, dest: File, password: CharArray? = null, selection: Set<String>? = null, flatten: Boolean = false,
                policy: ExistsPolicy = ExistsPolicy.RENAME, ctl: TaskControl = TaskControl()): Int {
        val existed = dest.exists()
        if (!existed && !dest.mkdirs()) throw IOException("تعذر إنشاء المجلد الوجهة")
        val root = dest.canonicalFile
        val created = ArrayList<File>()
        var count = 0
        try {
            if (detect(file) in listOf(ArchiveKind.ZIP, ArchiveKind.SEVENZ, ArchiveKind.RAR, ArchiveKind.CAB, ArchiveKind.LZH)) {
                ctl.total = list(file, password, ctl).filter { !it.isDir && matches(it.path, selection) }.sumOf { maxOf(it.size, 0L) }
            }
            visit(file, password, ctl) { info, write ->
                if (!matches(info.path, selection)) return@visit
                val rel = if (flatten) info.name else info.path
                if (rel.isBlank()) return@visit
                var target = ArchiveEngine.safeTarget(root, rel)
                if (info.isDir) {
                    if (!target.exists()) { if (!target.mkdirs()) throw IOException("تعذر إنشاء مجلد"); created += target }
                    count++; return@visit
                }
                if (target.exists()) when (if (policy == ExistsPolicy.ASK) (ctl.stickyPolicy ?: ctl.onExists?.invoke(target) ?: ExistsPolicy.RENAME) else policy) {
                    ExistsPolicy.SKIP -> return@visit
                    ExistsPolicy.RENAME -> target = FileTools.uniqueFile(target.parentFile!!, target.name)
                    ExistsPolicy.OVERWRITE, ExistsPolicy.ASK -> {}
                }
                target.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("تعذر إنشاء مجلد") }
                ctl.label = info.name
                created += target
                target.outputStream().buffered(64 * 1024).use { write(it) }
                if (info.modified > 0) target.setLastModified(info.modified)
                count++
            }
        } catch (t: Throwable) {
            created.asReversed().forEach { it.deleteRecursively() }
            if (!existed) dest.delete()
            throw t
        }
        return count
    }

    private fun matches(path: String, sel: Set<String>?) = sel == null || sel.any { path == it || path.startsWith("$it/") }

    private fun copy(i: InputStream, o: OutputStream, ctl: TaskControl) {
        val buf = ByteArray(64 * 1024)
        while (true) { val n = i.read(buf); if (n < 0) break; o.write(buf, 0, n); ctl.add(n.toLong()) }
    }

    @Suppress("DEPRECATION")
    fun visit(file: File, password: CharArray?, ctl: TaskControl, visitor: (ArchiveEntryInfo, (OutputStream) -> Unit) -> Unit) {
        require(file.isFile) { "الأرشيف غير موجود" }
        val pw = password?.takeIf { it.isNotEmpty() }
        when (val kind = detect(file)) {
            ArchiveKind.ZIP -> try {
                val zip = if (pw == null) ZipFile(file) else ZipFile(file, pw)
                if (!zip.isValidZipFile) throw IOException("ملف ZIP غير صالح أو تالف")
                if (zip.isEncrypted && pw == null) throw PasswordException(false)
                for (h in zip.fileHeaders) {
                    ctl.check()
                    val m = h.compressionMethod?.name ?: ""
                    val info = ArchiveEntryInfo(h.fileName.trimEnd('/'), h.isDirectory, h.uncompressedSize, h.compressedSize,
                        h.lastModifiedTimeEpoch, h.crc, if (h.isEncrypted) "$m+${h.encryptionMethod?.name}" else m, h.isEncrypted)
                    visitor(info) { out -> zip.getInputStream(h).use { copy(it, out, ctl) } }
                }
            } catch (e: ZipException) {
                if (e.type == ZipException.Type.WRONG_PASSWORD || (pw != null && e.type == ZipException.Type.CHECKSUM_MISMATCH)) throw PasswordException(true)
                throw e
            }
            ArchiveKind.SEVENZ -> {
                var started = false
                try { visitSevenZ(file, pw, ctl) { i, w -> started = true; visitor(i, w) } }
                catch (e: PasswordException) { throw e
                } catch (e: CancelledByUser) { throw e
                } catch (e: Exception) {
                    // Fallback: native 7-Zip engine for methods the Java reader does not support (only before any output was produced).
                    if (started) throw e
                    try { SevenZipEngine.visit(file, pw, ctl, visitor) }
                    catch (e2: PasswordException) { throw e2 } catch (e2: CancelledByUser) { throw e2 } catch (e2: Throwable) { throw e }
                }
            }
            ArchiveKind.CAB, ArchiveKind.LZH -> SevenZipEngine.visit(file, pw, ctl, visitor)
            ArchiveKind.RAR -> try {
                val ar = if (pw == null) com.github.junrar.Archive(file) else com.github.junrar.Archive(file, String(pw))
                ar.use { a ->
                    for (h in a.fileHeaders) {
                        ctl.check()
                        val info = ArchiveEntryInfo(h.fileName.replace('\\', '/').trimEnd('/'), h.isDirectory, h.fullUnpackSize, h.fullPackSize,
                            0L, -1L, "RAR", h.isEncrypted)
                        visitor(info) { out -> a.extractFile(h, out) }
                    }
                }
            } catch (e: CancelledByUser) { throw e
            } catch (e: PasswordException) { throw e
            } catch (e: Exception) {
                if (e.message.orEmpty().contains("password", true)) throw PasswordException(pw != null)
                throw e
            }
            ArchiveKind.TAR, ArchiveKind.TAR_GZ, ArchiveKind.TAR_BZ2, ArchiveKind.TAR_XZ, ArchiveKind.TAR_ZST -> {
                TarArchiveInputStream(decoder(kind, file)).use { tar ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        ctl.check()
                        val e = tar.nextTarEntry ?: break
                        val info = ArchiveEntryInfo(e.name.trimEnd('/'), e.isDirectory, e.size, -1, e.modTime?.time ?: 0L, -1L, kind.label)
                        visitor(info) { out -> while (true) { val n = tar.read(buf); if (n < 0) break; out.write(buf, 0, n); ctl.add(n.toLong()) } }
                    }
                }
            }
            ArchiveKind.ARJ -> ArjArchiveInputStream(file.inputStream().buffered()).use { arj ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    ctl.check()
                    val e = arj.nextEntry ?: break
                    val info = ArchiveEntryInfo(e.name.replace('\\', '/').trimEnd('/'), e.isDirectory, e.size, -1, e.lastModifiedDate?.time ?: 0L, -1L, "ARJ")
                    visitor(info) { out -> while (true) { val n = arj.read(buf); if (n < 0) break; out.write(buf, 0, n); ctl.add(n.toLong()) } }
                }
            }
            ArchiveKind.ISO -> IsoReader.visit(file, ctl, visitor)
            ArchiveKind.GZ, ArchiveKind.BZ2, ArchiveKind.XZ, ArchiveKind.ZST -> {
                val base = file.name.substringBeforeLast('.')
                val info = ArchiveEntryInfo(base, false, -1, file.length(), file.lastModified(), -1L, kind.label)
                decoder(kind, file).use { inp -> visitor(info) { out -> copy(inp, out, ctl) } }
            }
            ArchiveKind.UNKNOWN -> throw IOException("صيغة الأرشيف غير مدعومة: ${file.extension.uppercase().ifEmpty { "؟" }}")
        }
    }

    @Suppress("DEPRECATION")
    private fun visitSevenZ(file: File, pw: CharArray?, ctl: TaskControl, visitor: (ArchiveEntryInfo, (OutputStream) -> Unit) -> Unit) {
        try {
            val sz = if (pw == null) org.apache.commons.compress.archivers.sevenz.SevenZFile(file)
                     else org.apache.commons.compress.archivers.sevenz.SevenZFile(file, pw)
            sz.use { s ->
                var e = s.nextEntry
                val buf = ByteArray(64 * 1024)
                while (e != null) {
                    ctl.check()
                    val entry = e
                    val info = ArchiveEntryInfo(entry.name.trimEnd('/'), entry.isDirectory, entry.size, -1,
                        if (entry.hasLastModifiedDate) entry.lastModifiedDate.time else 0L, if (entry.hasCrc) entry.crcValue else -1L,
                        entry.contentMethods?.joinToString("+") { it.method.name } ?: "", false)
                    visitor(info) { out -> while (true) { val n = s.read(buf); if (n < 0) break; out.write(buf, 0, n); ctl.add(n.toLong()) } }
                    e = s.nextEntry
                }
            }
        } catch (e: org.apache.commons.compress.PasswordRequiredException) { throw PasswordException(false)
        } catch (e: IOException) {
            if (pw != null && Regex("checksum|corrupt|password|decrypt|data error", RegexOption.IGNORE_CASE).containsMatchIn(e.message.orEmpty())) throw PasswordException(true)
            throw e
        }
    }

    private fun decoder(kind: ArchiveKind, file: File): InputStream {
        val raw = file.inputStream().buffered(64 * 1024)
        return when (kind) {
            ArchiveKind.TAR_GZ, ArchiveKind.GZ -> GzipCompressorInputStream(raw)
            ArchiveKind.TAR_BZ2, ArchiveKind.BZ2 -> BZip2CompressorInputStream(raw)
            ArchiveKind.TAR_XZ, ArchiveKind.XZ -> XZCompressorInputStream(raw)
            ArchiveKind.TAR_ZST, ArchiveKind.ZST -> org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream(raw)
            else -> raw
        }
    }
}

/** Minimal ISO-9660 reader (Joliet long names preferred) — browse and extract without external libraries. */
object IsoReader {
    private fun le32(b: ByteArray, o: Int): Long = (b[o].toLong() and 0xFF) or ((b[o + 1].toLong() and 0xFF) shl 8) or ((b[o + 2].toLong() and 0xFF) shl 16) or ((b[o + 3].toLong() and 0xFF) shl 24)

    fun visit(file: File, ctl: TaskControl, visitor: (ArchiveEntryInfo, (OutputStream) -> Unit) -> Unit) {
        java.io.RandomAccessFile(file, "r").use { raf ->
            var joliet = false; var rootLba = 0L; var rootLen = 0L; var sector = 16L
            while (true) {
                val b = ByteArray(2048); raf.seek(sector * 2048); if (raf.read(b) < 2048) break
                if (String(b, 1, 5, Charsets.ISO_8859_1) != "CD001" || (b[0].toInt() and 0xFF) == 255) break
                val type = b[0].toInt()
                if (type == 1 && !joliet) { rootLba = le32(b, 158); rootLen = le32(b, 166) }
                if (type == 2 && String(b, 88, 2, Charsets.ISO_8859_1) == "%/") { joliet = true; rootLba = le32(b, 158); rootLen = le32(b, 166) }
                sector++
            }
            if (rootLba <= 0) throw IOException("ملف ISO غير صالح")
            val buf = ByteArray(64 * 1024)
            fun walk(lba: Long, len: Long, prefix: String) {
                val data = ByteArray(len.toInt()); raf.seek(lba * 2048); raf.readFully(data)
                var off = 0
                while (off < data.size) {
                    ctl.check()
                    val l = data[off].toInt() and 0xFF
                    if (l == 0) { off = (off / 2048 + 1) * 2048; continue }
                    val isDir = (data[off + 25].toInt() and 2) != 0
                    val nameLen = data[off + 32].toInt() and 0xFF
                    val nameBytes = data.copyOfRange(off + 33, off + 33 + nameLen)
                    val eLba = le32(data, off + 2); val eSize = le32(data, off + 10)
                    val cal = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC")).apply { clear(); set(1900 + (data[off + 18].toInt() and 0xFF), (data[off + 19].toInt() and 0xFF) - 1, data[off + 20].toInt(), data[off + 21].toInt(), data[off + 22].toInt(), data[off + 23].toInt()) }
                    off += l
                    if (nameLen == 1 && (nameBytes[0].toInt() == 0 || nameBytes[0].toInt() == 1)) continue
                    var name = if (joliet) String(nameBytes, Charsets.UTF_16BE) else String(nameBytes, Charsets.ISO_8859_1)
                    name = name.substringBefore(';').removeSuffix(".")
                    val path = prefix + name
                    visitor(ArchiveEntryInfo(path, isDir, if (isDir) 0 else eSize, -1, cal.timeInMillis, -1L, "ISO")) { out ->
                        var left = eSize; raf.seek(eLba * 2048)
                        while (left > 0) { val n = raf.read(buf, 0, minOf(buf.size.toLong(), left).toInt()); if (n < 0) break; out.write(buf, 0, n); left -= n; ctl.add(n.toLong()) }
                    }
                    if (isDir) walk(eLba, eSize, "$path/")
                }
            }
            walk(rootLba, rootLen, "")
        }
    }
}
