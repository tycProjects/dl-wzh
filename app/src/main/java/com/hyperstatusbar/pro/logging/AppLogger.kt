package com.hyperstatusbar.pro.logging

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Локальный журнал приложения.
 * Хранит только события, которые приложение само передало logger-у.
 * Не собирает уведомления, содержимое экрана, контакты или полный logcat.
 */
class AppLogger(context: Context) {
    private val file = File(context.filesDir, "hyperstatusbar.log")
    private val backup = File(context.filesDir, "hyperstatusbar.log.1")
    private val lock = Any()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun info(tag: String, message: String) = append("INFO", tag, message)
    fun warn(tag: String, message: String) = append("WARN", tag, message)
    fun error(tag: String, message: String, error: Throwable? = null) {
        val details = buildString {
            append(message)
            if (error != null) {
                append("\n")
                append(Log.getStackTraceString(error))
            }
        }
        append("ERROR", tag, details)
    }

    private fun append(level: String, tag: String, message: String) {
        val safeTag = tag.replace('\n', ' ').take(48)
        val safeMessage = message.replace("\u0000", "").take(6000)
        val line = "[${timeFormat.format(Date())}] [$level] [$safeTag] $safeMessage\n"
        synchronized(lock) {
            runCatching {
                if (file.exists() && file.length() > 512 * 1024) {
                    backup.delete()
                    file.renameTo(backup)
                }
                file.appendText(line, Charsets.UTF_8)
            }
        }
        when (level) {
            "ERROR" -> Log.e(safeTag, safeMessage)
            "WARN" -> Log.w(safeTag, safeMessage)
            else -> Log.i(safeTag, safeMessage)
        }
    }

    fun readAll(): String = synchronized(lock) {
        buildString {
            appendLine("=== HyperStatusBar Pro diagnostic log ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("App log files: internal app events only")
            appendLine("==========================================")
            if (backup.exists()) append(backup.readText(Charsets.UTF_8))
            if (file.exists()) append(file.readText(Charsets.UTF_8))
            if (!backup.exists() && !file.exists()) appendLine("Лог пока пуст.")
        }
    }

    fun clear() = synchronized(lock) {
        file.delete()
        backup.delete()
        info("AppLogger", "Журнал очищен пользователем")
    }
}
