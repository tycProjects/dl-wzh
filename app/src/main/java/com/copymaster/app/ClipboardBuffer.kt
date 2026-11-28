package com.copymaster.app

/**
 * Volatile, process-local RAM buffer. No file/database persistence.
 * Android may terminate the process at any time, clearing this data.
 */
object ClipboardBuffer {
    @Volatile var text: String = ""
    @Volatile var chunkSize: Int = 20000

    fun clear() {
        text = ""
    }

    fun chunkAt(offset: Int): String {
        if (offset < 0 || offset >= text.length) return ""
        val end = minOf(offset + chunkSize.coerceIn(1, 500000), text.length)
        return text.substring(offset, end)
    }
}
