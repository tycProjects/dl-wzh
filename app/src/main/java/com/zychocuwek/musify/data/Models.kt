package com.zychocuwek.musify.data

data class Song(
    val videoId: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val albumId: String = "",
    val cover: String = "",
    val duration: String = ""
)

/** Kartu serbaguna: album / playlist / artis. */
data class Card(val id: String, val title: String, val subtitle: String, val cover: String)

data class SearchResult(
    val songs: List<Song>,
    val albums: List<Card>,
    val playlists: List<Card>,
    val artists: List<Card>
)

data class ArtistDetail(
    val id: String,
    val name: String,
    val cover: String,
    val topSongs: List<Song>,
    val albums: List<Card>,
    val singles: List<Card>,
    val similar: List<Card>
)

data class AlbumDetail(val id: String, val title: String, val cover: String, val songs: List<Song>)

data class LyricLine(val time: Double, val text: String)
data class Lyrics(val synced: Boolean, val lines: List<LyricLine>)

data class UserPlaylist(val id: String, val name: String, val songs: List<Song>)

fun String.prettyDuration() = replace('.', ':')
