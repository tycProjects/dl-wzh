package com.filemax.manager

import android.content.Context
import android.os.Environment
import java.io.File
import java.security.MessageDigest

object FileTools {
    fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name); if (!f.exists()) return f
        val ext = if (name.endsWith(".tar.gz")) ".tar.gz" else if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        val base = name.removeSuffix(ext)
        var i = 1
        while (f.exists()) { f = File(dir, "$base ($i)$ext"); i++ }
        return f
    }

    fun hash(file: File, algo: String = "SHA-256", ctl: TaskControl? = null): String {
        val md = MessageDigest.getInstance(algo); val buf = ByteArray(64 * 1024)
        file.inputStream().use { while (true) { val n = it.read(buf); if (n < 0) break; md.update(buf, 0, n); ctl?.check() } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    data class Stats(val files: Int, val folders: Int, val bytes: Long)
    fun stats(f: File): Stats {
        if (f.isFile) return Stats(1, 0, f.length())
        var a = 0; var d = 0; var s = 0L
        f.walkTopDown().forEach { if (it.isDirectory) { if (it != f) d++ } else { a++; s += it.length() } }
        return Stats(a, d, s)
    }

    fun storageRoots(context: Context): List<File> {
        val primary = Environment.getExternalStorageDirectory()
        val others = context.getExternalFilesDirs(null).filterNotNull().mapNotNull { File(it.absolutePath.substringBefore("/Android/")) }
        return (listOf(primary) + others).distinctBy { it.absolutePath }.filter { it.exists() }
    }

    fun searchRecursive(root: File, query: String, hidden: Boolean, limit: Int = 500, ctl: TaskControl = TaskControl()): List<File> {
        val out = ArrayList<File>()
        root.walkTopDown().onEnter { hidden || !it.name.startsWith(".") || it == root }.forEach {
            ctl.check()
            if (it != root && it.name.contains(query, true) && (hidden || !it.name.startsWith("."))) { out += it; if (out.size >= limit) return out }
        }
        return out
    }

    fun findDuplicates(root: File, hidden: Boolean, ctl: TaskControl = TaskControl()): List<List<File>> {
        val bySize = HashMap<Long, MutableList<File>>()
        root.walkTopDown().onEnter { hidden || !it.name.startsWith(".") || it == root }.forEach {
            ctl.check(); if (it.isFile && it.length() > 0) bySize.getOrPut(it.length()) { mutableListOf() }.add(it) }
        val groups = ArrayList<List<File>>()
        for (same in bySize.values.filter { it.size > 1 }) {
            same.groupBy { runCatching { hash(it, "SHA-256", ctl) }.getOrNull() }.filter { it.key != null && it.value.size > 1 }.values.forEach { groups += it }
        }
        return groups.sortedByDescending { it[0].length() * it.size }
    }

    fun category(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> "صور"
        "mp4", "mkv", "avi", "mov", "3gp", "webm" -> "فيديو"
        "mp3", "wav", "flac", "m4a", "ogg", "aac" -> "صوت"
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "epub" -> "مستندات"
        "zip", "7z", "rar", "tar", "gz", "bz2", "xz", "tgz", "arj", "jar" -> "أرشيفات"
        "apk", "xapk" -> "تطبيقات"
        else -> "أخرى"
    }

    fun analyze(root: File, ctl: TaskControl = TaskControl()): Pair<Map<String, Long>, List<File>> {
        val cats = HashMap<String, Long>(); val all = ArrayList<File>()
        root.walkTopDown().onEnter { it == root || !it.absolutePath.contains("/Android/data") }.forEach {
            ctl.check(); if (it.isFile) { cats.merge(category(it.name), it.length(), Long::plus); all += it } }
        return cats to all.sortedByDescending { it.length() }.take(30)
    }

    // ---- Recycle bin ----
    fun trashDir(): File = File(Environment.getExternalStorageDirectory(), ".filemax_trash").also { it.mkdirs() }
    fun moveToTrash(f: File) {
        val id = System.currentTimeMillis().toString() + "_" + (0..999).random()
        val dir = File(trashDir(), id).also { it.mkdirs() }
        File(dir, "origin.txt").writeText(f.absolutePath)
        val target = File(dir, f.name)
        if (!f.renameTo(target)) { f.copyRecursively(target, true); if (!f.deleteRecursively()) throw java.io.IOException("تعذر الحذف") }
    }
    data class TrashItem(val dir: File, val file: File, val origin: String)
    fun listTrash(): List<TrashItem> = trashDir().listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { d ->
        val origin = File(d, "origin.txt").takeIf { it.exists() }?.readText() ?: return@mapNotNull null
        d.listFiles()?.firstOrNull { it.name != "origin.txt" }?.let { TrashItem(d, it, origin) }
    }.sortedByDescending { it.dir.name }
    fun restore(t: TrashItem) {
        val dest = File(t.origin); dest.parentFile?.mkdirs()
        val target = if (dest.exists()) uniqueFile(dest.parentFile!!, dest.name) else dest
        if (!t.file.renameTo(target)) { t.file.copyRecursively(target, true); t.file.deleteRecursively() }
        t.dir.deleteRecursively()
    }
    fun emptyTrash() { trashDir().listFiles()?.forEach { it.deleteRecursively() } }

    // ---- Batch rename: pattern uses {n} for the counter and {name} for the original base name ----
    fun batchPreview(files: List<File>, pattern: String, start: Int): List<Pair<File, String>> = files.mapIndexed { i, f ->
        val ext = if (f.isFile && f.name.contains('.')) "." + f.extension else ""
        f to pattern.replace("{n}", (start + i).toString()).replace("{name}", f.nameWithoutExtension) + ext
    }
}
