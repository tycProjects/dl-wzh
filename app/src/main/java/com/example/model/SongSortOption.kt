package com.example.model

import java.util.Locale

enum class SongSortOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val shortLabel: String
) {
    A_TO_Z(
        id = "a_to_z",
        title = "A → Z",
        subtitle = "Alphabetical (A to Z)",
        shortLabel = "A → Z"
    ),
    Z_TO_A(
        id = "z_to_a",
        title = "Z → A",
        subtitle = "Alphabetical (Z to A)",
        shortLabel = "Z → A"
    ),
    DATE_ASC(
        id = "date_asc",
        title = "Date Ascending",
        subtitle = "Oldest to newest",
        shortLabel = "Date ↑"
    ),
    DATE_DESC(
        id = "date_desc",
        title = "Date Descending",
        subtitle = "Newest to oldest",
        shortLabel = "Date ↓"
    ),
    SIZE_ASC(
        id = "size_asc",
        title = "Size Ascending",
        subtitle = "Smallest to largest",
        shortLabel = "Size ↑"
    ),
    SIZE_DESC(
        id = "size_desc",
        title = "Size Descending",
        subtitle = "Largest to smallest",
        shortLabel = "Size ↓"
    );

    /**
     * Pure in-memory sort function.
     * Guarantees the actual music files and database entries remain completely untouched.
     */
    fun sort(songs: List<Song>): List<Song> {
        return when (this) {
            A_TO_Z -> songs.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            Z_TO_A -> songs.sortedWith(
                compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            DATE_ASC -> songs.sortedWith(
                compareBy<Song> { it.dateAdded }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            DATE_DESC -> songs.sortedWith(
                compareByDescending<Song> { it.dateAdded }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            SIZE_ASC -> songs.sortedWith(
                compareBy<Song> { it.fileSize }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            SIZE_DESC -> songs.sortedWith(
                compareByDescending<Song> { it.fileSize }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
        }
    }
}
