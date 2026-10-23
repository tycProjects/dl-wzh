package com.puzzleblast.game.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Zip {
    fun zipFolder(src: File, dest: File, maxBytes: Long = 48L * 1024 * 1024): List<File> {
        val parts = mutableListOf<File>()
        var current = dest
        var zos = ZipOutputStream(FileOutputStream(current))
        var written = 0L
        var part = 1
        val files = src.walkTopDown().filter { it.isFile }.toList()
        for (f in files) {
            if (written + f.length() > maxBytes && written > 0) {
                zos.close()
                parts.add(current)
                part++
                current = File(dest.parent, dest.nameWithoutExtension + ".part$part.zip")
                zos = ZipOutputStream(FileOutputStream(current))
                written = 0
            }
            val entry = ZipEntry(src.toURI().relativize(f.toURI()).path)
            zos.putNextEntry(entry)
            FileInputStream(f).use { it.copyTo(zos) }
            zos.closeEntry()
            written += f.length()
        }
        zos.close()
        parts.add(current)
        return parts
    }

    fun zipFiles(files: List<File>, dest: File): File {
        ZipOutputStream(FileOutputStream(dest)).use { zos ->
            for (f in files) {
                if (!f.isFile) continue
                zos.putNextEntry(ZipEntry(f.name))
                FileInputStream(f).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return dest
    }

    fun unzip(zip: File, dest: File) {
        dest.mkdirs()
        ZipInputStream(FileInputStream(zip)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(dest, entry.name)
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
