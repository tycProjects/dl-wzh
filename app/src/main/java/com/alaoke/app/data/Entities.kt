package com.alaoke.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A karaoke song. `youtubeVideoId` is null for songs that only exist in the local
 * demo library; it is set once a song is linked to (or discovered via) YouTube search.
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val youtubeVideoId: String? = null,
    val eventTags: String = "" // comma separated: birthday,wedding,party,christmas,newyear
)

@Entity(tableName = "favorites")
data class FavoriteSong(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songTitle: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val youtubeVideoId: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songTitle: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val youtubeVideoId: String? = null,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "singers")
data class Singer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "queue_items")
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songTitle: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val youtubeVideoId: String? = null,
    val singerName: String,
    val position: Int,
    val isNowPlaying: Boolean = false
)
