package com.aetherisland.feature.lyrics

/** Small, allocation-light lookup used by the expanded island lyric view. */
object LyricsSynchronizer {
    fun activeLineIndex(lines: List<LyricsLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var active = -1
        for (index in lines.indices) {
            val timestamp = lines[index].timestampMs ?: continue
            if (timestamp <= positionMs) active = index else break
        }
        return active
    }
}
