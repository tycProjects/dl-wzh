package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.BeatCategory
import com.example.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val albumArtUri: String?,
    val isSampleTrack: Boolean,
    val dateAdded: Long,
    val bpm: Int = 128,
    val beatCategory: String = "FAST_BEAT",
    val fileSize: Long = 0L,
    val filePath: String? = null
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uriString = uriString,
        albumArtUri = albumArtUri,
        isSampleTrack = isSampleTrack,
        dateAdded = dateAdded,
        bpm = bpm,
        beatCategory = BeatCategory.fromName(beatCategory),
        fileSize = fileSize,
        filePath = filePath,
        isKicked = false
    )

    companion object {
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            uriString = song.uriString,
            albumArtUri = song.albumArtUri,
            isSampleTrack = song.isSampleTrack,
            dateAdded = song.dateAdded,
            bpm = song.bpm,
            beatCategory = song.beatCategory.name,
            fileSize = song.fileSize,
            filePath = song.filePath
        )
    }
}
