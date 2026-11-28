package com.filemax.manager

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.tukaani.xz.LZMA2Options
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** level: 0 = store, 1 = fast, 5 = normal, 9 = maximum. */
data class CreateOptions(
    val format: String = "zip",
    val level: Int = 5,
    val password: CharArray? = null,
    val zipCrypto: Boolean = false,
    val comment: String = "",
    val excludeHidden: Boolean = false,
    val splitBytes: Long = 0L
)

object ArchiveWriter {
    val FORMATS = listOf("zip", "7z", "tar", "tar.gz", "tar.bz2", "tar.xz", "tar.zst", "gz", "bz2", "xz", "zst")
    fun supportsPassword(format: String) = format == "zip" || format == "7z"
    fun singleFileOnly(format: String) = format in listOf("gz", "bz2", "xz", "zst")

    fun totalSize(files: List<File>, hidden: Boolean): Long = files.sumOf { f ->
        if (hidden && f.name.startsWith(".")) 0L
        else if (f.isFile) f.length() else f.walkTopDown().filter { it.isFile && !(hidden && it.name.startsWith(".")) }.sumOf { it.length() }
    }

    fun create(inputs: List<File>, output: File, o: CreateOptions, ctl: TaskControl = TaskControl()) {
        require(inputs.isNotEmpty()) { "لا توجد ملفات" }
        require(!output.exists()) { "الملف الوجهة موجود" }
        if (singleFileOnly(o.format)) require(inputs.size == 1 && inputs[0].isFile) { "هذه الصيغة تضغط ملفاً واحداً فقط" }
        output.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("تعذر إنشاء مجلد الوجهة") }
        ctl.total = totalSize(inputs, o.excludeHidden); ctl.done = 0
        val free = output.parentFile?.usableSpace ?: Long.MAX_VALUE
        if (o.level == 0 && ctl.total > free) throw IOException("المساحة غير كافية: المطلوب ${formatBytes(ctl.total)} والمتاح ${formatBytes(free)}")
        var nativeSplit = false
        try {
            when (o.format) {
                "zip" -> nativeSplit = createZip(inputs, output, o, ctl)
                "7z" -> create7z(inputs, output, o, ctl)
                "tar", "tar.gz", "tar.bz2", "tar.xz", "tar.zst" -> createTar(inputs, output, o, ctl)
                else -> createSingle(inputs[0], output, o, ctl)
            }
            if (o.splitBytes >= 65536 && !nativeSplit) split(output, o.splitBytes)
        } catch (t: Throwable) {
            output.delete()
            File(output.parentFile, output.name + ".001").takeIf { it.exists() }?.let { first ->
                var i = 1; while (File(output.parentFile, output.name + ".%03d".format(i)).delete()) i++
            }
            run { var i = 1; while (File(output.parentFile, output.nameWithoutExtension + ".z%02d".format(i)).delete()) i++ }
            throw t
        } finally { o.password?.fill('\u0000') }
    }

    private fun skip(f: File, o: CreateOptions) = o.excludeHidden && f.name.startsWith(".")

    /** Returns true when a native split ZIP (.z01, .z02 ... + .zip) was produced. */
    private fun createZip(inputs: List<File>, output: File, o: CreateOptions, ctl: TaskControl): Boolean {
        val params = ZipParameters().apply {
            if (o.level == 0) compressionMethod = CompressionMethod.STORE
            else { compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = when { o.level <= 1 -> CompressionLevel.FASTEST; o.level >= 9 -> CompressionLevel.ULTRA; else -> CompressionLevel.NORMAL } }
            if (!o.password.isNullOrEmpty()) {
                isEncryptFiles = true
                if (o.zipCrypto) encryptionMethod = EncryptionMethod.ZIP_STANDARD
                else { encryptionMethod = EncryptionMethod.AES; aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256 }
            }
            if (o.excludeHidden) excludeFileFilter = ExcludeFileFilter { it.name.startsWith(".") }
        }
        if (o.splitBytes >= 65536) {
            val files = inputs.filter { !skip(it, o) }
            files.firstOrNull { !it.exists() }?.let { throw IOException("الملف اختفى: ${it.name}") }
            val splittable = files.isNotEmpty() && (files.all { it.isFile } || (files.size == 1 && files[0].isDirectory))
            if (splittable) {
                // AUTO-REPAIR: disabled -- unresolved reference 'isNullOrEmpty' (was: val z = if (o.password.isNullOrEmpty()) ZipFile(output) else ZipFile(output, o.password))
                z.isRunInThread = true
                ctl.label = files[0].name
                if (files.all { it.isFile }) z.createSplitZipFile(files, params, true, o.splitBytes)
                else z.createSplitZipFileFromFolder(files[0], params, true, o.splitBytes)
                val m = z.progressMonitor
                while (m.state == ProgressMonitor.State.BUSY) {
                    if (ctl.cancelled) m.isCancelAllTasks = true
                    ctl.done = m.workCompleted
                    Thread.sleep(80)
                }
                if (m.result == ProgressMonitor.Result.CANCELLED) throw CancelledByUser()
                if (m.result == ProgressMonitor.Result.ERROR) throw (m.exception ?: IOException("فشل الضغط"))
                return true
            }
        }
        // AUTO-REPAIR: disabled -- unresolved reference 'isNullOrEmpty' (was: val zip = if (o.password.isNullOrEmpty()) ZipFile(output) else ZipFile(output, o.password))
        zip.isRunInThread = true
        var base = 0L
        for (f in inputs) {
            if (!f.exists()) throw IOException("الملف اختفى: ${f.name}")
            if (skip(f, o)) continue
            ctl.label = f.name
            if (f.isDirectory) zip.addFolder(f, params) else zip.addFile(f, params)
            val m = zip.progressMonitor
            while (m.state == ProgressMonitor.State.BUSY) {
                if (ctl.cancelled) m.isCancelAllTasks = true
                ctl.done = base + m.workCompleted
                Thread.sleep(80)
            }
            base += m.totalWork
            if (m.result == ProgressMonitor.Result.CANCELLED) throw CancelledByUser()
            if (m.result == ProgressMonitor.Result.ERROR) throw (m.exception ?: IOException("فشل الضغط"))
        }
        if (o.comment.isNotBlank() && output.exists()) zip.comment = o.comment
        return false
    }

    private fun collect(f: File, name: String, o: CreateOptions, out: MutableList<Pair<File, String>>) {
        if (skip(f, o)) return
        out += f to name
        if (f.isDirectory) f.listFiles()?.sortedBy { it.name }?.forEach { collect(it, "$name/${it.name}", o, out) }
    }

    private fun create7z(inputs: List<File>, output: File, o: CreateOptions, ctl: TaskControl) {
        if (!o.password.isNullOrEmpty()) {
            val list = ArrayList<Pair<File, String>>()
            inputs.forEach { if (!it.exists()) throw IOException("الملف اختفى: ${it.name}"); collect(it, it.name, o, list) }
            SevenZipEngine.create7z(list, output, o, ctl)
            return
        }
        SevenZOutputFile(output).use { out ->
            if (o.level == 0) out.setContentCompression(SevenZMethod.COPY)
            else out.setContentMethods(listOf(SevenZMethodConfiguration(SevenZMethod.LZMA2, LZMA2Options(if (o.level >= 9) 9 else if (o.level <= 1) 1 else 5))))
            fun add(f: File, name: String) {
                if (skip(f, o)) return
                ctl.check()
                out.putArchiveEntry(out.createArchiveEntry(f, name))
                if (f.isFile) { ctl.label = f.name; f.inputStream().use { i ->
                    val buf = ByteArray(64 * 1024)
                    while (true) { val n = i.read(buf); if (n < 0) break; out.write(buf, 0, n); ctl.add(n.toLong()) } } }
                out.closeArchiveEntry()
                if (f.isDirectory) f.listFiles()?.sortedBy { it.name }?.forEach { add(it, "$name/${it.name}") }
            }
            inputs.forEach { add(it, it.name) }
        }
    }

    private fun createTar(inputs: List<File>, output: File, o: CreateOptions, ctl: TaskControl) {
        val raw = output.outputStream().buffered(64 * 1024)
        val lv = if (o.level == 0) 0 else if (o.level <= 1) 1 else if (o.level >= 9) 9 else 6
        val comp: OutputStream = when (o.format) {
            "tar.gz" -> GzipCompressorOutputStream(raw, GzipParameters().apply { compressionLevel = lv })
            "tar.bz2" -> BZip2CompressorOutputStream(raw, if (o.level <= 1) 1 else 9)
            "tar.xz" -> XZCompressorOutputStream(raw, lv)
            "tar.zst" -> org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream(raw, if (o.level == 0) 1 else if (o.level <= 1) 1 else if (o.level >= 9) 19 else 3)
            else -> raw
        }
        TarArchiveOutputStream(comp).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            fun add(f: File, name: String) {
                if (skip(f, o)) return
                ctl.check()
                tar.putArchiveEntry(tar.createArchiveEntry(f, name) as TarArchiveEntry)
                if (f.isFile) { ctl.label = f.name; f.inputStream().use { i ->
                    val buf = ByteArray(64 * 1024)
                    while (true) { val n = i.read(buf); if (n < 0) break; tar.write(buf, 0, n); ctl.add(n.toLong()) } } }
                tar.closeArchiveEntry()
                if (f.isDirectory) f.listFiles()?.sortedBy { it.name }?.forEach { add(it, "$name/${it.name}") }
            }
            inputs.forEach { add(it, it.name) }
        }
    }

    private fun createSingle(input: File, output: File, o: CreateOptions, ctl: TaskControl) {
        val raw = output.outputStream().buffered(64 * 1024)
        val lv = if (o.level == 0) 0 else if (o.level <= 1) 1 else if (o.level >= 9) 9 else 6
        val comp: OutputStream = when (o.format) {
            "gz" -> GzipCompressorOutputStream(raw, GzipParameters().apply { compressionLevel = lv; filename = input.name })
            "bz2" -> BZip2CompressorOutputStream(raw)
            "zst" -> org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream(raw, if (o.level <= 1) 1 else if (o.level >= 9) 19 else 3)
            else -> XZCompressorOutputStream(raw, lv)
        }
        comp.use { out -> input.inputStream().use { i ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = i.read(buf); if (n < 0) break; out.write(buf, 0, n); ctl.add(n.toLong()) } } }
    }

    /** Raw volume split (name.ext.001, .002 ...), compatible with 7-Zip / ZArchiver style volumes. */
    fun split(file: File, part: Long) {
        val buf = ByteArray(64 * 1024)
        file.inputStream().use { inp ->
            var idx = 1
            while (true) {
                var written = 0L
                val target = File(file.parentFile, file.name + ".%03d".format(idx))
                var any = false
                target.outputStream().buffered().use { o ->
                    while (written < part) {
                        val n = inp.read(buf, 0, minOf(buf.size.toLong(), part - written).toInt())
                        if (n < 0) break
                        o.write(buf, 0, n); written += n; any = true
                    }
                }
                if (!any) { target.delete(); break }
                idx++
                if (written < part) break
            }
        }
        file.delete()
    }
}
