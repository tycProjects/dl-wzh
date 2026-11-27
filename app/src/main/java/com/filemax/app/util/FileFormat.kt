package com.filemax.app.util

import java.io.File
import java.util.Locale

/** Human-readable file metadata helpers. */
object FileFormat {
    fun size(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble(); var index = -1
        do { value /= 1024.0; index++ } while (value >= 1024 && index < units.lastIndex)
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }
    fun kind(file: File): String = when {
        file.isDirectory -> "مجلد"
        file.extension.lowercase() in listOf("zip", "jar", "apk") -> "ZIP"
        file.extension.lowercase() in listOf("7z", "rar", "tar", "gz", "bz2", "xz") -> "أرشيف"
        file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif") -> "صورة"
        file.extension.lowercase() in listOf("mp4", "mkv", "3gp", "webm") -> "فيديو"
        file.extension.lowercase() in listOf("mp3", "wav", "ogg", "m4a") -> "صوت"
        file.extension.lowercase() == "pdf" -> "PDF"
        else -> file.extension.uppercase().ifBlank { "ملف" }
    }
}
