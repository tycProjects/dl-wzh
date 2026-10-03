package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toPlaylist(songCount: Int = 0): Playlist = Playlist(
        id = id,
        name = name,
        createdAt = createdAt,
        songCount = songCount
    )
}
