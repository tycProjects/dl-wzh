package com.example.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.audio.BpmAnalyzer
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MusicScanner {

    private const val TAG = "MusicScanner"

    suspend fun scanDeviceAudio(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Track"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol)
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val filePath = if (dataCol != -1) cursor.getString(dataCol) else null
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val albumArtUri = if (albumIdCol != -1) {
                        val albumId = cursor.getLong(albumIdCol)
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                    } else null

                    val cleanArtist = if (artist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else artist
                    val cleanAlbum = if (album.contains("<unknown>", ignoreCase = true)) "Unknown Album" else album

                    val (beatCat, analysis) = BpmAnalyzer.analyzeSong(title, cleanArtist, duration, filePath)

                    songs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            durationMs = duration,
                            uriString = contentUri.toString(),
                            albumArtUri = albumArtUri,
                            isSampleTrack = false,
                            dateAdded = System.currentTimeMillis(),
                            bpm = analysis.bpm,
                            beatCategory = beatCat,
                            analysis = analysis,
                            fileSize = size,
                            filePath = filePath,
                            isKicked = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MediaStore: ${e.message}", e)
        }

        songs
    }

    suspend fun parseSongFromUri(context: Context, uri: Uri): Song? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "Imported Track"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            var fileSize = 0L
            var filePath: String? = null
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) {
                    fileSize = f.length()
                    filePath = f.absolutePath
                }
            } else {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        fileSize = pfd.statSize
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val (beatCat, analysis) = BpmAnalyzer.analyzeSong(title, artist, durationMs, filePath ?: uri.path)

            Song(
                id = uri.hashCode().toLong(),
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uriString = uri.toString(),
                albumArtUri = null,
                isSampleTrack = false,
                dateAdded = System.currentTimeMillis(),
                bpm = analysis.bpm,
                beatCategory = beatCat,
                analysis = analysis,
                fileSize = fileSize,
                filePath = filePath,
                isKicked = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading metadata from Uri: ${e.message}", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
