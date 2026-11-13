package com.meloza.app.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri

/** A real audio file found on the device through MediaStore. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val genre: String,
    val durationMs: Long,
    val uri: Uri,
    val dateAdded: Long,
) {
    val artworkUri: Uri
        get() = ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId)
}

data class Album(val id: Long, val title: String, val artist: String, val artworkUri: Uri, val trackCount: Int)
data class Artist(val id: Long, val name: String, val trackCount: Int)
data class Genre(val id: Long, val name: String, val trackCount: Int)

/**
 * Reads the device's audio library. Returns an empty list when nothing is found;
 * throws on permission or I/O failure so the UI can show an error state.
 */
object LocalLibrary {

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.IS_MUSIC,
    )

    fun scan(context: Context): List<Track> {
        val resolver: ContentResolver = context.contentResolver
        val result = mutableListOf<Track>()
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                result += Track(
                    id = id,
                    title = c.getString(titleCol) ?: "—",
                    artist = c.getString(artistCol)?.takeUnless { it == "<unknown>" } ?: "—",
                    album = c.getString(albumCol)?.takeUnless { it == "<unknown>" } ?: "—",
                    albumId = c.getLong(albumIdCol),
                    artistId = c.getLong(artistIdCol),
                    genre = "",
                    durationMs = c.getLong(durCol),
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                    dateAdded = c.getLong(addedCol),
                )
            }
        }
        return result
    }

    /** Genres are resolved via MediaStore.Audio.Genres; each track is mapped to its first genre. */
    fun genres(context: Context): List<Genre> {
        val out = mutableListOf<Genre>()
        context.contentResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val name = c.getString(1) ?: continue
                val count = countGenreTracks(context, id)
                if (count > 0) out += Genre(id, name, count)
            }
        }
        return out.sortedBy { it.name.lowercase() }
    }

    fun tracksOfGenre(context: Context, genreId: Long): List<Track> {
        val ids = mutableListOf<Long>()
        context.contentResolver.query(
            MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
            arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null, null,
        )?.use { c -> while (c.moveToNext()) ids += c.getLong(0) }
        val set = ids.toHashSet()
        return scan(context).filter { it.id in set }
    }

    private fun countGenreTracks(context: Context, genreId: Long): Int =
        context.contentResolver.query(
            MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
            arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null, null,
        )?.use { it.count } ?: 0
}

fun List<Track>.albums(): List<Album> =
    groupBy { it.albumId }.map { (id, list) ->
        Album(id, list.first().album, list.first().artist, list.first().artworkUri, list.size)
    }.sortedBy { it.title.lowercase() }

fun List<Track>.artists(): List<Artist> =
    groupBy { it.artistId }.map { (id, list) ->
        Artist(id, list.first().artist, list.size)
    }.sortedBy { it.name.lowercase() }
