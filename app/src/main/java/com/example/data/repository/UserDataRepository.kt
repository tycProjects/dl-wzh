package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.db.FavoriteSongEntity
import com.example.data.db.HiddenSongEntity
import com.example.data.db.JnmfDao
import com.example.data.db.PlaylistEntity
import com.example.data.db.SongClassificationEntity
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class SaveResult<out T> {
    data class Success<T>(val data: T) : SaveResult<T>()
    data object DuplicateIgnored : SaveResult<Nothing>()
    data object InsufficientStorage : SaveResult<Nothing>()
    data class Error(val cause: Throwable?) : SaveResult<Nothing>()

    val isSuccess: Boolean
        get() = this is Success
}

/**
 * Safe persistence gateway for playlists, favorites, hidden songs, and remix classifications.
 *
 * Guarantees:
 * - Writes execute inside [Dispatchers.IO] + [NonCancellable] serialized by [writeMutex] so
 *   navigating away or closing an Activity never truncates an active write.
 * - Pre-checks available internal storage via [StorageGuard] before allocating new rows.
 * - Prevents duplicate playlists and duplicate song entries within a playlist.
 * - Never copies media files, never creates redundant database backups, and never deletes
 *   the user's original audio/video files.
 */
class UserDataRepository(
    private val context: Context,
    private val dao: JnmfDao
) {
    private val appContext = context.applicationContext
    private val writeMutex = Mutex()

    companion object {
        private const val TAG = "JNMF_UserDataRepo"
    }

    private suspend fun <T> executeSafeWrite(
        requiresFreeSpace: Boolean = true,
        operationName: String,
        block: suspend () -> SaveResult<T>
    ): SaveResult<T> = withContext(Dispatchers.IO + NonCancellable) {
        writeMutex.withLock {
            if (requiresFreeSpace && !StorageGuard.hasEnoughSpaceForWrite(appContext)) {
                Log.w(TAG, "Blocked $operationName due to critically low internal storage.")
                return@withLock SaveResult.InsufficientStorage
            }
            try {
                StorageGuard.throwIfSimulatedFailure()
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "Error during $operationName: ${t.message}", t)
                if (StorageGuard.isOutOfSpaceError(t)) {
                    SaveResult.InsufficientStorage
                } else {
                    SaveResult.Error(t)
                }
            }
        }
    }

    suspend fun createPlaylist(rawName: String): SaveResult<Long> {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return SaveResult.Error(IllegalArgumentException("Blank playlist name"))
        return executeSafeWrite(operationName = "createPlaylist") {
            val existing = dao.getPlaylistByNameSync(trimmed)
            if (existing != null) {
                SaveResult.DuplicateIgnored
            } else {
                val id = dao.insertPlaylist(PlaylistEntity(name = trimmed))
                if (id > 0L) SaveResult.Success(id) else SaveResult.Error(null)
            }
        }
    }

    suspend fun createPlaylistAndAddSong(rawName: String, song: Song): SaveResult<Long> {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return SaveResult.Error(IllegalArgumentException("Blank playlist name"))
        return executeSafeWrite(operationName = "createPlaylistAndAddSong") {
            val existing = dao.getPlaylistByNameSync(trimmed)
            val playlistId = existing?.id ?: dao.insertPlaylist(PlaylistEntity(name = trimmed))
            if (playlistId <= 0L) {
                SaveResult.Error(null)
            } else {
                dao.addSongToPlaylist(
                    playlistId = playlistId,
                    songId = song.id,
                    savedTitle = song.title,
                    savedArtist = song.artist,
                    savedUri = song.contentUri.toString(),
                    savedFileName = song.fileName
                )
                SaveResult.Success(playlistId)
            }
        }
    }

    suspend fun renamePlaylist(playlistId: Long, rawNewName: String): SaveResult<Unit> {
        val trimmed = rawNewName.trim()
        if (trimmed.isEmpty()) return SaveResult.Error(IllegalArgumentException("Blank playlist name"))
        return executeSafeWrite(operationName = "renamePlaylist") {
            val existing = dao.getPlaylistByNameSync(trimmed)
            if (existing != null && existing.id != playlistId) {
                SaveResult.DuplicateIgnored
            } else {
                dao.renamePlaylist(playlistId, trimmed)
                SaveResult.Success(Unit)
            }
        }
    }

    suspend fun deletePlaylist(playlistId: Long): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "deletePlaylist") {
            dao.deletePlaylistWithTracks(playlistId)
            SaveResult.Success(Unit)
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song): SaveResult<Boolean> {
        return executeSafeWrite(operationName = "addSongToPlaylist") {
            val inserted = dao.addSongToPlaylist(
                playlistId = playlistId,
                songId = song.id,
                savedTitle = song.title,
                savedArtist = song.artist,
                savedUri = song.contentUri.toString(),
                savedFileName = song.fileName
            )
            if (inserted) SaveResult.Success(true) else SaveResult.DuplicateIgnored
        }
    }

    suspend fun addSongIdToPlaylist(playlistId: Long, songId: Long, allSongs: List<Song>): SaveResult<Boolean> {
        val song = allSongs.find { it.id == songId }
        return executeSafeWrite(operationName = "addSongIdToPlaylist") {
            val inserted = dao.addSongToPlaylist(
                playlistId = playlistId,
                songId = songId,
                savedTitle = song?.title.orEmpty(),
                savedArtist = song?.artist.orEmpty(),
                savedUri = song?.contentUri?.toString().orEmpty(),
                savedFileName = song?.fileName.orEmpty()
            )
            if (inserted) SaveResult.Success(true) else SaveResult.DuplicateIgnored
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "removeSongFromPlaylist") {
            dao.removeSongFromPlaylistAndReindex(playlistId, songId)
            SaveResult.Success(Unit)
        }
    }

    suspend fun moveTrackInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int): SaveResult<Unit> {
        return executeSafeWrite(operationName = "moveTrackInPlaylist") {
            dao.moveTrackInPlaylist(playlistId, fromIndex, toIndex)
            SaveResult.Success(Unit)
        }
    }

    suspend fun toggleFavorite(songId: Long, isCurrentlyFavorite: Boolean): SaveResult<Boolean> {
        return executeSafeWrite(
            requiresFreeSpace = !isCurrentlyFavorite,
            operationName = "toggleFavorite"
        ) {
            if (isCurrentlyFavorite) {
                dao.removeFavorite(songId)
                SaveResult.Success(false)
            } else {
                dao.addFavorite(FavoriteSongEntity(songId = songId))
                SaveResult.Success(true)
            }
        }
    }

    suspend fun hideSong(song: Song): SaveResult<Unit> {
        return executeSafeWrite(operationName = "hideSong") {
            dao.hideSong(
                HiddenSongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist
                )
            )
            SaveResult.Success(Unit)
        }
    }

    suspend fun unhideSong(songId: Long): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "unhideSong") {
            dao.unhideSong(songId)
            SaveResult.Success(Unit)
        }
    }

    suspend fun saveSongClassification(
        songId: Long,
        category: RemixCategory,
        estimatedBpm: Int = 0
    ): SaveResult<Unit> {
        return executeSafeWrite(operationName = "saveSongClassification") {
            dao.saveClassification(
                SongClassificationEntity(
                    songId = songId,
                    categoryName = category.name,
                    estimatedBpm = estimatedBpm,
                    confidence = 1.0f
                )
            )
            SaveResult.Success(Unit)
        }
    }
}
