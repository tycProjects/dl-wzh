package com.meloza.app.data

import android.content.Context
import com.meloza.app.data.db.MelozaDatabase
import com.meloza.app.data.db.PlayEventEntity
import com.meloza.app.data.db.PlaylistEntity
import com.meloza.app.data.db.PlaylistTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface LibraryState {
    data object Loading : LibraryState
    data object NoPermission : LibraryState
    data object Empty : LibraryState
    data class Error(val message: String) : LibraryState
    data class Ready(val tracks: List<Track>) : LibraryState
}

class MusicRepository(private val context: Context, private val db: MelozaDatabase) {

    private val _library = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val library: StateFlow<LibraryState> = _library.asStateFlow()

    private var cache: List<Track> = emptyList()

    /** Reloads the device library. Call after permission is granted and on pull-to-refresh. */
    suspend fun refresh(hasPermission: Boolean) {
        if (!hasPermission) {
            _library.value = LibraryState.NoPermission
            return
        }
        _library.value = LibraryState.Loading
        _library.value = try {
            val tracks = withContext(Dispatchers.IO) { LocalLibrary.scan(context) }
            cache = tracks
            if (tracks.isEmpty()) LibraryState.Empty else LibraryState.Ready(tracks)
        } catch (e: SecurityException) {
            LibraryState.NoPermission
        } catch (e: Exception) {
            LibraryState.Error(e.localizedMessage ?: "unknown")
        }
    }

    fun allTracks(): List<Track> = cache

    fun search(query: String): List<Track> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return cache.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }

    fun byIds(ids: List<Long>): List<Track> {
        val map = cache.associateBy { it.id }
        return ids.mapNotNull { map[it] }
    }

    // ---- Favorites ----
    fun favorites(): Flow<List<Long>> = db.favoriteDao().all().map { list -> list.map { it.trackId } }
    suspend fun toggleFavorite(trackId: Long) = db.favoriteDao().toggle(trackId)

    // ---- History ----
    suspend fun recordPlay(trackId: Long) {
        db.playEventDao().insert(PlayEventEntity(trackId = trackId, playedAt = System.currentTimeMillis()))
    }
    fun history(limit: Int = 200): Flow<List<PlayEventEntity>> = db.playEventDao().recent(limit)
    fun playCount(): Flow<Int> = db.playEventDao().count()

    // ---- Playlists ----
    fun playlists(): Flow<List<PlaylistEntity>> = db.playlistDao().all()
    fun playlistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> = db.playlistDao().tracks(playlistId)
    suspend fun createPlaylist(name: String): Long =
        db.playlistDao().insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))
    suspend fun renamePlaylist(id: Long, name: String) = db.playlistDao().rename(id, name.trim())
    suspend fun deletePlaylist(id: Long) = db.playlistDao().deletePlaylist(id)
    suspend fun addToPlaylist(playlistId: Long, trackId: Long) {
        val pos = db.playlistDao().nextPosition(playlistId)
        db.playlistDao().insertTrack(PlaylistTrackEntity(playlistId, trackId, pos))
    }
    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) =
        db.playlistDao().removeTrack(playlistId, trackId)
    suspend fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>) =
        db.playlistDao().reorder(playlistId, orderedTrackIds)
}
