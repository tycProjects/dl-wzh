package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SortOption(val title: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    SIZE_DESC("File Size (Largest first)"),
    SIZE_ASC("File Size (Smallest first)")
}

class MediaStoreRepository(private val context: Context) {

    suspend fun queryLocalSongs(
        sortOption: SortOption = SortOption.NAME_ASC,
        hiddenSongIds: Set<Long> = emptySet(),
        searchQuery: String = "",
        onProgress: ((current: Int, total: Int, itemTitle: String) -> Unit)? = null
    ): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = when (sortOption) {
            SortOption.NAME_ASC -> "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            SortOption.NAME_DESC -> "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE DESC"
            SortOption.DATE_DESC -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            SortOption.DATE_ASC -> "${MediaStore.Audio.Media.DATE_ADDED} ASC"
            SortOption.SIZE_DESC -> "${MediaStore.Audio.Media.SIZE} DESC"
            SortOption.SIZE_ASC -> "${MediaStore.Audio.Media.SIZE} ASC"
        }

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val totalCount = it.count
                var processedCount = 0

                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                val artBaseUri = Uri.parse("content://media/external/audio/albumart")

                while (it.moveToNext()) {
                    processedCount++
                    val id = it.getLong(idCol)
                    if (hiddenSongIds.contains(id)) {
                        continue // Excluded from normal library
                    }

                    val title = it.getString(titleCol) ?: "Unknown Title"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val duration = it.getLong(durationCol)
                    val size = it.getLong(sizeCol)
                    val dateAdded = it.getLong(dateCol)
                    val albumId = it.getLong(albumIdCol)

                    // Skip ultra-short ringtones or notification clips < 5 seconds
                    if (duration < 5000 && duration > 0) continue

                    if (searchQuery.isNotBlank()) {
                        val matches = title.contains(searchQuery, ignoreCase = true) ||
                                artist.contains(searchQuery, ignoreCase = true) ||
                                album.contains(searchQuery, ignoreCase = true)
                        if (!matches) continue
                    }

                    if (onProgress != null && (processedCount % 10 == 0 || processedCount == totalCount)) {
                        onProgress(processedCount, totalCount, title)
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val albumArtUri = ContentUris.withAppendedId(artBaseUri, albumId)

                    // Initial remix category heuristic based on Indo Remix track naming patterns
                    val lowerTitle = title.lowercase()
                    val initialCategory = when {
                        lowerTitle.contains("fullbeat") || lowerTitle.contains("full beat") || lowerTitle.contains("horeg") ->
                            RemixCategory.FULLBEAT
                        lowerTitle.contains("slow") || lowerTitle.contains("santuy") || lowerTitle.contains("chill") ->
                            RemixCategory.SLOW
                        lowerTitle.contains("remix") || lowerTitle.contains("jedag") || lowerTitle.contains("breakbeat") ->
                            RemixCategory.INDO_REMIX
                        else -> RemixCategory.UNCLASSIFIED
                    }

                    songList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            remixCategory = initialCategory
                        )
                    )
                }

                onProgress?.invoke(totalCount, totalCount, "Completed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songList
    }

    suspend fun queryLocalVideos(
        onProgress: ((current: Int, total: Int, itemTitle: String) -> Unit)? = null
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val totalCount = it.count
                var processedCount = 0

                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (it.moveToNext()) {
                    processedCount++
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Untitled Video"
                    val duration = it.getLong(durationCol)
                    val size = it.getLong(sizeCol)
                    val dateAdded = it.getLong(dateCol)

                    if (onProgress != null && (processedCount % 5 == 0 || processedCount == totalCount)) {
                        onProgress(processedCount, totalCount, title)
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    videoList.add(
                        VideoItem(
                            id = id,
                            title = title,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAdded = dateAdded,
                            contentUri = contentUri
                        )
                    )
                }

                onProgress?.invoke(totalCount, totalCount, "Completed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList
    }
}
