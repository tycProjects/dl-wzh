package com.filemax.app.archive

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Streaming ZIP create/extract with traversal protection. */
object ZipEngine {
    fun create(sources: List<File>, output: File) {
        require(sources.isNotEmpty()) { "اختر ملفات أولاً" }
        require(!output.exists()) { "ملف الأرشيف موجود بالفعل" }
        val base = sources.first().parentFile ?: sources.first()
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zip ->
                sources.forEach { source -> addRecursively(zip, source, base) }
            }
        } catch (e: Exception) {
            output.delete()
            throw e
        }
    }

    private fun addRecursively(zip: ZipOutputStream, file: File, base: File) {
        val relative = file.relativeTo(base).invariantSeparatorsPath
        if (file.isDirectory) {
            val dirName = if (relative.isEmpty()) "" else "$relative/"
            if (dirName.isNotEmpty()) zip.putNextEntry(ZipEntry(dirName).apply { time = file.lastModified() })
            if (dirName.isNotEmpty()) zip.closeEntry()
            file.listFiles()?.forEach { addRecursively(zip, it, base) }
        } else {
            zip.putNextEntry(ZipEntry(relative).apply { time = file.lastModified() })
            file.inputStream().buffered().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    fun extract(archive: File, destination: File, overwrite: Boolean = false): Int {
        require(archive.isFile) { "الأرشيف غير موجود" }
        require(destination.exists() || destination.mkdirs()) { "تعذر إنشاء مجلد الوجهة" }
        val root = destination.canonicalFile
        var count = 0
        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val output = File(root, entry.name).canonicalFile
                require(output.path == root.path || output.path.startsWith(root.path + File.separator)) {
                    "تم رفض مسار غير آمن داخل الأرشيف: ${entry.name}"
                }
                if (entry.isDirectory) {
                    require(output.exists() || output.mkdirs()) { "تعذر إنشاء مجلد" }
                } else {
                    output.parentFile?.let { require(it.exists() || it.mkdirs()) }
                    require(overwrite || !output.exists()) { "يوجد ملف بنفس الاسم: ${output.name}" }
                    FileOutputStream(output).use { out -> zip.copyTo(out) }
                    count++
                }
                zip.closeEntry()
            }
        }
        return count
    }
}
