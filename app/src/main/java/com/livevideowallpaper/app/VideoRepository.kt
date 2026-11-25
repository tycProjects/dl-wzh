package com.livevideowallpaper.app

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors

object Bg {
    val io = Executors.newFixedThreadPool(3)
}

data class VideoItem(val file: File, val durationMs: Long) {
    val name: String get() = file.nameWithoutExtension
}

object VideoRepository {
    val EXTENSIONS = setOf("mp4", "m4v", "3gp", "webm", "mkv")
    const val MAX_FILE_BYTES = 500L * 1024 * 1024

    fun dir(c: Context): File = File(c.filesDir, "videos").apply { mkdirs() }

    fun isVideoName(name: String) = name.substringAfterLast('.', "").lowercase() in EXTENSIONS

    fun list(c: Context): List<VideoItem> =
        dir(c).listFiles()
            ?.filter { it.isFile && isVideoName(it.name) }
            ?.sortedBy { it.name.lowercase() }
            ?.map { VideoItem(it, durationOf(it)) }
            ?: emptyList()

    fun durationOf(f: File): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(f.absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: -1L
        } catch (e: Exception) {
            -1L
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    fun isPlayable(f: File) = durationOf(f) > 0

    /** Keeps only the last path segment and safe characters. Prevents path traversal. */
    fun safeName(raw: String): String {
        val base = raw.replace('\\', '/').substringAfterLast('/')
        val cleaned = base.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().trimStart('.')
        val name = if (cleaned.isBlank()) "video.mp4" else cleaned
        return if (name.length > 100) name.takeLast(100) else name
    }

    fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (f.exists()) {
            f = File(dir, if (ext.isEmpty()) "${stem}_$i" else "${stem}_$i.$ext")
            i++
        }
        return f
    }

    /** Copies at most [limit] bytes; throws IOException if the stream is larger. Returns bytes copied. */
    fun copyLimited(input: InputStream, output: OutputStream, limit: Long): Long {
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > limit) throw IOException("File is too large")
            output.write(buf, 0, n)
        }
        return total
    }

    /** Imports one video picked through the Storage Access Framework. Returns null on failure. */
    fun importVideo(c: Context, uri: Uri): File? {
        var displayName = "video.mp4"
        try {
            c.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0)?.let { n -> displayName = n }
            }
        } catch (_: Exception) {}
        var safe = safeName(displayName)
        if (!isVideoName(safe)) safe += ".mp4"
        val dest = uniqueFile(dir(c), safe)
        val tmp = File(dest.parentFile, dest.name + ".part")
        return try {
            c.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { out -> copyLimited(input, out, MAX_FILE_BYTES) }
            } ?: return null
            if (!tmp.renameTo(dest)) { tmp.delete(); return null }
            if (!isPlayable(dest)) { dest.delete(); return null }
            dest
        } catch (e: Exception) {
            tmp.delete()
            null
        }
    }

    fun totalSize(c: Context): Long = dir(c).listFiles()?.sumOf { it.length() } ?: 0L

    fun deleteAll(c: Context) {
        dir(c).listFiles()?.forEach { it.delete() }
        Prefs.setSelectedPath(c, null)
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "--:--"
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }
}
