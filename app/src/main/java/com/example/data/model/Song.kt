package com.example.data.model

import android.net.Uri

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
    val isHidden: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return String.format("%.1f MB", mb)
        }
}

enum class RemixCategory(val displayName: String) {
    UNCLASSIFIED("Standard"),
    FULLBEAT("Fullbeat"),
    SLOW("Slow Remix"),
    INDO_REMIX("Indo Remix"),
    BREAKBEAT("Breakbeat")
}

data class VideoItem(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val contentUri: Uri
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
