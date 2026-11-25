package com.livevideowallpaper.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Safe ZIP import:
 *  - only the last path segment of an entry is used (no path traversal), entries containing ".." are rejected
 *  - only known video extensions are extracted; everything else is ignored
 *  - per-file, total-size and entry-count limits, measured while copying (headers are not trusted)
 *  - each extracted file must be readable as a video, otherwise it is deleted
 *  - extracted files are never executed
 */
object ZipImporter {
    const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
    const val MAX_ENTRIES = 500

    class Summary(val imported: Int, val skipped: Int, val messages: List<String>)

    private fun hasTraversal(entry: ZipEntry): Boolean =
        entry.name.replace('\\', '/').split('/').any { it == ".." }

    private fun isIgnored(entry: ZipEntry): Boolean {
        val p = entry.name.replace('\\', '/')
        if (entry.isDirectory) return true
        if (p.startsWith("__MACOSX/") || p.contains("/__MACOSX/")) return true
        val base = p.substringAfterLast('/')
        return base.startsWith(".") || !VideoRepository.isVideoName(base)
    }

    /** First pass: counts importable videos. Throws IOException for an unreadable archive. */
    fun scan(ctx: Context, uri: Uri): Int {
        var count = 0
        var entries = 0
        val stream = ctx.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
        ZipInputStream(stream.buffered()).use { zip ->
            while (true) {
                val e = zip.getNextEntry() ?: break
                if (++entries > MAX_ENTRIES) throw IOException("ZIP has too many files (max $MAX_ENTRIES)")
                if (!hasTraversal(e) && !isIgnored(e)) count++
                zip.closeEntry()
            }
        }
        return count
    }

    fun extract(ctx: Context, uri: Uri, total: Int, progress: (done: Int, total: Int, name: String) -> Unit): Summary {
        val dir = VideoRepository.dir(ctx)
        val dirCanonical = dir.canonicalPath
        var imported = 0
        var skipped = 0
        var bytes = 0L
        var entries = 0
        val messages = ArrayList<String>()

        try {
            val stream = ctx.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
            ZipInputStream(stream.buffered()).use { zip ->
                while (true) {
                    val e = zip.getNextEntry() ?: break
                    if (++entries > MAX_ENTRIES) { messages.add("Stopped: too many files in ZIP"); break }
                    if (hasTraversal(e)) {
                        skipped++; messages.add("Blocked unsafe path: ${e.name}"); zip.closeEntry(); continue
                    }
                    if (isIgnored(e)) { zip.closeEntry(); continue }

                    val safe = VideoRepository.safeName(e.name)
                    val dest = VideoRepository.uniqueFile(dir, safe)
                    if (!dest.canonicalPath.startsWith(dirCanonical + File.separator)) {
                        skipped++; messages.add("Blocked unsafe path: ${e.name}"); zip.closeEntry(); continue
                    }
                    progress(imported + skipped, total, safe)

                    val tmp = File(dir, dest.name + ".part")
                    try {
                        val limit = minOf(VideoRepository.MAX_FILE_BYTES, MAX_TOTAL_BYTES - bytes)
                        if (limit <= 0) throw IOException("Total size limit reached")
                        val written = tmp.outputStream().use { out -> VideoRepository.copyLimited(zip, out, limit) }
                        bytes += written
                        if (!tmp.renameTo(dest)) throw IOException("Could not save file")
                        if (!VideoRepository.isPlayable(dest)) {
                            dest.delete(); throw IOException("Not a playable video")
                        }
                        imported++
                    } catch (ex: IOException) {
                        tmp.delete()
                        skipped++
                        messages.add("Skipped $safe: ${ex.message}")
                    }
                    zip.closeEntry()
                }
            }
        } catch (ex: IOException) {
            messages.add("The ZIP is damaged or unreadable: ${ex.message}. Imported what could be read.")
        }
        return Summary(imported, skipped, messages)
    }
}
