package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int
)

@Entity(tableName = "favorites")
data class FavoriteSongEntity(
    @PrimaryKey
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_songs")
data class HiddenSongEntity(
    @PrimaryKey
    val songId: Long,
    val title: String,
    val artist: String,
    val hiddenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "song_classifications")
data class SongClassificationEntity(
    @PrimaryKey
    val songId: Long,
    val categoryName: String,
    val estimatedBpm: Int = 0,
    val confidence: Float = 1.0f
)
