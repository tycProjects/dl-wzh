package com.livevideowallpaper.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/** Photos / videos the user uploads as backgrounds for the dialer, call screen and lock screen. */
object BgRepository {
    val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp")

    fun dir(c: Context): File = File(c.filesDir, "backgrounds").apply { mkdirs() }
    fun ext(name: String) = name.substringAfterLast('.', "").lowercase()
    fun isImage(f: File) = ext(f.name) in IMAGE_EXT
    fun isVideo(f: File) = VideoRepository.isVideoName(f.name)

    private fun imageOk(f: File): Boolean {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, o)
        return o.outWidth > 0
    }

    private fun removeOld(c: Context, slot: String) {
        val old = Prefs.bgPath(c, slot) ?: return
        val f = File(old)
        if (f.isFile && f.parentFile?.canonicalPath == dir(c).canonicalPath) f.delete()
    }

    fun clear(c: Context, slot: String) {
        removeOld(c, slot)
        Prefs.setBgPath(c, slot, null)
    }

    /** Copies a picked photo/video into app storage and stores it for [slot]. Returns null on failure. */
    fun import(c: Context, uri: Uri, slot: String): File? {
        var display = "background"
        try {
            c.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0)?.let { n -> display = n }
            }
        } catch (_: Exception) {}
        val mime = c.contentResolver.getType(uri) ?: ""
        var safe = VideoRepository.safeName(display)
        if (ext(safe) !in IMAGE_EXT && !VideoRepository.isVideoName(safe)) {
            safe += when {
                mime.startsWith("image/png") -> ".png"
                mime.startsWith("image/webp") -> ".webp"
                mime.startsWith("image/") -> ".jpg"
                else -> ".mp4"
            }
        }
        val dest = File(dir(c), "${slot}_${System.currentTimeMillis()}_$safe")
        val tmp = File(dest.parentFile, dest.name + ".part")
        return try {
            c.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { out -> VideoRepository.copyLimited(input, out, VideoRepository.MAX_FILE_BYTES) }
            } ?: return null
            if (!tmp.renameTo(dest)) { tmp.delete(); return null }
            val ok = if (isVideo(dest)) VideoRepository.isPlayable(dest) else imageOk(dest)
            if (!ok) { dest.delete(); return null }
            removeOld(c, slot)
            Prefs.setBgPath(c, slot, dest.absolutePath)
            dest
        } catch (e: Exception) {
            tmp.delete()
            null
        }
    }
}
