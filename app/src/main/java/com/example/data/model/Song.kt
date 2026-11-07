package com.example.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.example.data.repository.MediaSearchMatcher
import java.util.Locale

private fun formatDurationFast(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return buildString(6) {
        if (minutes < 10L) append('0')
        append(minutes)
        append(':')
        if (seconds < 10L) append('0')
        append(seconds)
    }
}

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val isFavorite: Boolean = false,
    val bpmEstimate: Int = 0,
    val remixCategory: RemixCategory = RemixCategory.UNCLASSIFIED,
    val isHidden: Boolean = false,
    val fileName: String = ""
) {
    val formattedDuration: String = formatDurationFast(durationMs)

    val formattedSize: String = String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))

    internal val normalizedSearchIndex: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val fileStem = fileName.substringBeforeLast('.', fileName)
        buildString(title.length + fileName.length + artist.length + album.length + 8) {
            append(MediaSearchMatcher.normalizeForSearch(title))
            if (fileName.isNotEmpty()) {
                append('\n')
                append(MediaSearchMatcher.normalizeForSearch(fileName))
            }
            if (fileStem.isNotEmpty() && fileStem != fileName) {
                append('\n')
                append(MediaSearchMatcher.normalizeForSearch(fileStem))
            }
            if (artist.isNotEmpty()) {
                append('\n')
                append(MediaSearchMatcher.normalizeForSearch(artist))
            }
            if (album.isNotEmpty()) {
                append('\n')
                append(MediaSearchMatcher.normalizeForSearch(album))
            }
        }
    }
}

enum class RemixCategory(val displayName: String) {
    UNCLASSIFIED("Standard"),
    FULLBEAT("Fullbeat"),
    SLOW("Slow Remix"),
    INDO_REMIX("Indo Remix"),
    BREAKBEAT("Breakbeat")
}

@Immutable
data class VideoItem(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val contentUri: Uri,
    val fileName: String = ""
) {
    val formattedDuration: String = formatDurationFast(durationMs)

    internal val normalizedSearchIndex: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val fileStem = fileName.substringBeforeLast('.', fileName)
        buildString(title.length + fileName.length + 4) {
            append(MediaSearchMatcher.normalizeForSearch(title))
            if (fileName.isNotEmpty()) {
                append('\n')
                append(MediaSearchMatcher.normalizeForSearch(fileName))
            }
            if (fileStem.isNotEmpty() && fileStem != fileName) {
                append('\n')
                append(MediaSearchMatcher.normalizeForSearch(fileStem))
            }
        }
    }
}
