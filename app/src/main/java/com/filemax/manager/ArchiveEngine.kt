package com.filemax.manager

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** ZIP engine. Passwords are passed only for the duration of an operation and never logged. */
object ArchiveEngine {
    fun createZip(inputs: List<File>, output: File, password: CharArray? = null) {
        require(inputs.isNotEmpty()) { "No input files" }
        require(!output.exists()) { "Destination already exists" }
        output.parentFile?.let { require(it.exists() || it.mkdirs()) }
        val params = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            // AUTO-REPAIR: disabled -- unresolved reference 'isNullOrEmpty' (was: isEncryptFiles = !password.isNullOrEmpty())
            if (password != null && password.isNotEmpty()) {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
        }
        try {
            val zip = if (password == null || password.isEmpty()) ZipFile(output) else ZipFile(output, password)
            inputs.forEach { input ->
                if (!input.exists()) throw IOException("Input disappeared: ${input.name}")
                if (input.isDirectory) zip.addFolder(input, params) else zip.addFile(input, params)
            }
        } catch (t: Throwable) {
            output.delete()
            throw t
        } finally {
            password?.fill('\u0000')
        }
    }

    /** Extracts each entry manually after canonical-path validation (zip-slip protection). */
    fun extractZip(archive: File, destination: File, password: CharArray? = null) {
        require(archive.isFile) { "Archive not found" }
        destination.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("Cannot create destination parent") }
        require(!destination.exists()) { "Destination already exists" }
        val staging = File(destination.parentFile, ".${destination.name}.filemax_tmp")
        staging.deleteRecursively()
        if (!staging.mkdirs()) throw IOException("Cannot create temporary destination")
        val zip = if (password == null || password.isEmpty()) ZipFile(archive) else ZipFile(archive, password)
        try {
            val root = staging.canonicalFile
            zip.fileHeaders.forEach { header ->
                val target = safeTarget(root, header.fileName)
                if (header.isDirectory) {
                    if (!target.exists() && !target.mkdirs()) throw IOException("Cannot create directory")
                } else {
                    target.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("Cannot create parent") }
                    zip.getInputStream(header).use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output, 64 * 1024) }
                    }
                    header.lastModifiedTimeEpoch.let { if (it > 0L) target.setLastModified(it) }
                }
            }
            if (destination.exists() || !staging.renameTo(destination)) throw IOException("Cannot finalize extracted files")
        } catch (t: Throwable) {
            staging.deleteRecursively()
            throw t
        } finally {
            password?.fill('\u0000')
        }
    }

    fun isZip(file: File): Boolean = runCatching {
        FileInputStream(file).use { input ->
            val b = ByteArray(4)
            input.read(b) == 4 && b[0] == 0x50.toByte() && b[1] == 0x4b.toByte()
        }
    }.getOrDefault(false)

    fun safeTarget(root: File, entryName: String): File {
        val base = root.canonicalFile
        val target = File(base, entryName).canonicalFile
        if (target != base && !target.path.startsWith(base.path + File.separator)) {
            throw SecurityException("Unsafe archive path")
        }
        return target
    }
}

/** Additional non-encrypted archive formats powered by Apache Commons Compress. */
object ExtendedArchiveEngine {
    fun create7z(inputs: List<File>, output: File) {
        require(inputs.isNotEmpty()) { "No input files" }
        require(!output.exists()) { "Destination already exists" }
        output.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("Cannot create destination") }
        val seven = org.apache.commons.compress.archivers.sevenz.SevenZOutputFile(output)
        try {
            inputs.forEach { input ->
                require(input.exists()) { "Input not found: ${input.name}" }
                add7z(seven, input, input.name)
            }
        } catch (t: Throwable) {
            output.delete(); throw t
        } finally { seven.close() }
    }

    private fun add7z(out: org.apache.commons.compress.archivers.sevenz.SevenZOutputFile, file: File, name: String) {
        val entry = out.createArchiveEntry(file, name)
        out.putArchiveEntry(entry)
        if (file.isFile) file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; out.write(buffer, 0, count) }
        }
        out.closeArchiveEntry()
        if (file.isDirectory) file.listFiles()?.sortedBy { it.name }?.forEach { add7z(out, it, "$name/${it.name}") }
    }

    fun extract7z(archive: File, destination: File) {
        require(archive.isFile) { "Archive not found" }
        require(!destination.exists()) { "Destination already exists" }
        val staging = File(destination.parentFile, ".${destination.name}.filemax_tmp")
        staging.deleteRecursively(); if (!staging.mkdirs()) throw IOException("Cannot create temporary folder")
        try {
            org.apache.commons.compress.archivers.sevenz.SevenZFile(archive).use { seven ->
                val root = staging.canonicalFile
                var entry = seven.nextEntry
                val buffer = ByteArray(64 * 1024)
                while (entry != null) {
                    val target = ArchiveEngine.safeTarget(root, entry.name)
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("Cannot create parent") }
                        target.outputStream().buffered().use { output ->
                            while (true) { val n = seven.read(buffer); if (n <= 0) break; output.write(buffer, 0, n) }
                        }
                    }
                    entry = seven.nextEntry
                }
            }
            if (!staging.renameTo(destination)) throw IOException("Cannot finalize extraction")
        } catch (t: Throwable) { staging.deleteRecursively(); throw t }
    }

    fun createTar(inputs: List<File>, output: File, gzip: Boolean = false) {
        require(inputs.isNotEmpty()) { "No input files" }; require(!output.exists()) { "Destination already exists" }
        output.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("Cannot create destination") }
        try {
            val fileOut = output.outputStream().buffered()
            val compressed: java.io.OutputStream = if (gzip) org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream(fileOut) else fileOut
            org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(compressed).use { tar ->
                tar.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX)
                inputs.forEach { addTar(tar, it, it.name) }
            }
        } catch (t: Throwable) { output.delete(); throw t }
    }

    private fun addTar(tar: org.apache.commons.compress.archivers.tar.TarArchiveOutputStream, file: File, name: String) {
        val entry = tar.createArchiveEntry(file, name) as org.apache.commons.compress.archivers.tar.TarArchiveEntry
        tar.putArchiveEntry(entry)
        if (file.isFile) file.inputStream().use { it.copyTo(tar, 64 * 1024) }
        tar.closeArchiveEntry()
        if (file.isDirectory) file.listFiles()?.sortedBy { it.name }?.forEach { addTar(tar, it, "$name/${it.name}") }
    }

    fun extractTar(archive: File, destination: File, gzip: Boolean = false) {
        require(archive.isFile) { "Archive not found" }; require(!destination.exists()) { "Destination already exists" }
        val staging = File(destination.parentFile, ".${destination.name}.filemax_tmp")
        staging.deleteRecursively(); if (!staging.mkdirs()) throw IOException("Cannot create temporary folder")
        try {
            val raw = archive.inputStream().buffered()
            val decoded = if (gzip) org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(raw) else raw
            org.apache.commons.compress.archivers.tar.TarArchiveInputStream(decoded).use { tar ->
                val root = staging.canonicalFile; val buffer = ByteArray(64 * 1024)
                while (true) {
                    val entry = tar.nextTarEntry ?: break
                    val target = ArchiveEngine.safeTarget(root, entry.name)
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("Cannot create parent") }
                        target.outputStream().buffered().use { out -> while (true) { val n = tar.read(buffer); if (n < 0) break; out.write(buffer, 0, n) } }
                    }
                }
            }
            if (!staging.renameTo(destination)) throw IOException("Cannot finalize extraction")
        } catch (t: Throwable) { staging.deleteRecursively(); throw t }
    }
}