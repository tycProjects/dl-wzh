package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface JnmfDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: Long)

    // Playlist Tracks
    @Query("SELECT * FROM playlist_tracks ORDER BY orderIndex ASC")
    fun getAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT songId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getTrackIdsForPlaylist(playlistId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT MAX(orderIndex) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val maxOrder = getMaxOrderIndex(playlistId) ?: -1
        insertPlaylistTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = maxOrder + 1
            )
        )
    }

    @Query("SELECT songId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getTrackIdsForPlaylistSync(playlistId: Long): List<Long>

    @Transaction
    suspend fun reorderPlaylist(playlistId: Long, orderedSongIds: List<Long>) {
        deletePlaylistTracks(playlistId)
        orderedSongIds.forEachIndexed { index, songId ->
            insertPlaylistTrack(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    songId = songId,
                    orderIndex = index
                )
            )
        }
    }

    @Transaction
    suspend fun moveTrackInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val trackIds = getTrackIdsForPlaylistSync(playlistId).toMutableList()
        if (fromIndex in trackIds.indices && toIndex in trackIds.indices && fromIndex != toIndex) {
            val item = trackIds.removeAt(fromIndex)
            trackIds.add(toIndex, item)
            reorderPlaylist(playlistId, trackIds)
        }
    }

    // Favorites
    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    fun getAllFavoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavorite(songId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteSongEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    // Hidden Songs
    @Query("SELECT * FROM hidden_songs ORDER BY hiddenAt DESC")
    fun getAllHiddenSongs(): Flow<List<HiddenSongEntity>>

    @Query("SELECT songId FROM hidden_songs")
    fun getAllHiddenSongIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideSong(hiddenSong: HiddenSongEntity)

    @Query("DELETE FROM hidden_songs WHERE songId = :songId")
    suspend fun unhideSong(songId: Long)

    // Classifications
    @Query("SELECT * FROM song_classifications")
    fun getAllClassifications(): Flow<List<SongClassificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClassification(classification: SongClassificationEntity)
}
