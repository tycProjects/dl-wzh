package com.aetherisland.feature.lyrics

/** Timestamped lyric line. timestampMs is null for unsynchronized/plain lyrics. */
data class LyricsLine(val timestampMs: Long?, val text: String)

data class LyricsDocument(
    val trackKey: String,
    val lines: List<LyricsLine>,
    val source: String,
    val isSynchronized: Boolean
)

interface LyricsRepository {
    suspend fun findLyrics(title: String, artist: String, durationMs: Long?): LyricsDocument?
}
