package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.KickedSong

@Entity(tableName = "kicked_songs")
data class KickedSongEntity(
    @PrimaryKey val id: Long,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val bpm: Int = 128,
    val beatCategory: String = "Fast Beat",
    val fileSize: Long = 0L,
    val filePath: String? = null,
    val dateKicked: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    fun toKickedSong(): KickedSong = KickedSong(
        id = id,
        uriString = uriString,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        bpm = bpm,
        beatCategory = beatCategory,
        fileSize = fileSize,
        filePath = filePath,
        dateKicked = dateKicked,
        notes = notes
    )

    companion object {
        fun fromKickedSong(kicked: KickedSong): KickedSongEntity = KickedSongEntity(
            id = kicked.id,
            uriString = kicked.uriString,
            title = kicked.title,
            artist = kicked.artist,
            album = kicked.album,
            durationMs = kicked.durationMs,
            bpm = kicked.bpm,
            beatCategory = kicked.beatCategory,
            fileSize = kicked.fileSize,
            filePath = kicked.filePath,
            dateKicked = kicked.dateKicked,
            notes = kicked.notes
        )
    }
}
