package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.audio.SampleAudioGenerator
import com.example.data.db.AppDatabase
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.KickedSongEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.SongEntity
import com.example.model.AppPreferences
import com.example.model.KickedSong
import com.example.model.Playlist
import com.example.model.PossibleDuplicateGroup
import com.example.model.Song
import com.example.scanner.DuplicateDetector
import com.example.scanner.MusicScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val settingsDao = database.appSettingsDao()
    private val kickedDao = database.kickedSongDao()

    private val tag = "JnmfMusicRepo"

    // Normal Library: strictly excludes any songs in kicked_songs table
    val songs: Flow<List<Song>> = combine(
        songDao.getAllSongs(),
        kickedDao.getAllKickedSongs()
    ) { allSongs, kickedSongs ->
        val kickedUris = kickedSongs.map { it.uriString }.toSet()
        val kickedPaths = kickedSongs.mapNotNull { it.filePath }.toSet()
        val kickedIds = kickedSongs.map { it.id }.toSet()

        allSongs.map { it.toSong() }.filter { song ->
            !kickedIds.contains(song.id) &&
                    !kickedUris.contains(song.uriString) &&
                    (song.filePath == null || !kickedPaths.contains(song.filePath))
        }
    }

    val kickedSongs: Flow<List<KickedSong>> = kickedDao.getAllKickedSongs().map { list ->
        list.map { it.toKickedSong() }
    }

    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { list ->
        list.map { entity ->
            val count = playlistDao.getSongCountForPlaylist(entity.id).first()
            entity.toPlaylist(count)
        }
    }

    val preferences: Flow<AppPreferences> = settingsDao.getSettingsFlow().map { entity ->
        entity?.toPreferences() ?: AppPreferences()
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            checkAndSeedDemoTracks()
        }
    }

    suspend fun checkAndSeedDemoTracks(force: Boolean = false) = withContext(Dispatchers.IO) {
        val count = songDao.getSongCount().first()
        if (count == 0 || force) {
            val sampleSongs = SampleAudioGenerator.generateSampleTracksIfEmpty(context)
            songDao.insertSongs(sampleSongs.map { SongEntity.fromSong(it) })

            val existingPlaylists = playlistDao.getAllPlaylists().first()
            if (existingPlaylists.isEmpty()) {
                val plId = playlistDao.insertPlaylist(
                    PlaylistEntity(name = "Indo Remix Favorites", createdAt = System.currentTimeMillis())
                )
                sampleSongs.forEach { song ->
                    playlistDao.addSongToPlaylist(plId, song.id)
                }
            }
        }
    }

    suspend fun scanDeviceAndSync() = withContext(Dispatchers.IO) {
        val scanned = MusicScanner.scanDeviceAudio(context)
        if (scanned.isNotEmpty()) {
            val kickedList = kickedDao.getAllKickedSongs().first()
            val kickedUris = kickedList.map { it.uriString }.toSet()
            val kickedPaths = kickedList.mapNotNull { it.filePath }.toSet()

            // Filter out any songs that were kicked previously
            val eligible = scanned.filter { song ->
                !kickedUris.contains(song.uriString) &&
                        (song.filePath == null || !kickedPaths.contains(song.filePath))
            }
            songDao.insertSongs(eligible.map { SongEntity.fromSong(it) })
        }
    }

    // --- Kicked Songs (Hide from JNMF Only) ---

    suspend fun kickSong(song: Song, notes: String? = null) = withContext(Dispatchers.IO) {
        val kickedEntity = KickedSongEntity(
            id = song.id,
            uriString = song.uriString,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            bpm = song.bpm,
            beatCategory = song.beatCategory.displayName,
            fileSize = song.fileSize,
            filePath = song.filePath,
            dateKicked = System.currentTimeMillis(),
            notes = notes
        )
        kickedDao.insertKickedSong(kickedEntity)
    }

    suspend fun restoreKickedSong(kickedSongId: Long) = withContext(Dispatchers.IO) {
        // Removes from kicked_songs table. Makes visible in normal library again.
        // Does NOT duplicate, move, or delete file.
        kickedDao.deleteKickedSongById(kickedSongId)
    }

    suspend fun updateKickedSong(kickedSong: KickedSong) = withContext(Dispatchers.IO) {
        kickedDao.updateKickedSong(KickedSongEntity.fromKickedSong(kickedSong))
    }

    // --- Duplicate Detection ---

    suspend fun findDuplicates(): List<PossibleDuplicateGroup> = withContext(Dispatchers.IO) {
        val currentSongs = songs.first()
        DuplicateDetector.findPossibleDuplicates(currentSongs)
    }

    // --- Permanent Delete (Allowed ONLY inside Possible Duplicates) ---

    suspend fun deleteDuplicateFilePermanently(song: Song): Boolean = withContext(Dispatchers.IO) {
        var fileDeleted = false

        // 1. Delete physical file via File API if path exists
        if (!song.filePath.isNullOrEmpty()) {
            try {
                val f = File(song.filePath)
                if (f.exists()) {
                    fileDeleted = f.delete()
                }
            } catch (e: Exception) {
                Log.w(tag, "File.delete failed for ${song.filePath}: ${e.message}")
            }
        }

        // 2. Delete via ContentResolver if URI is content scheme
        if (!fileDeleted && song.uriString.startsWith("content://")) {
            try {
                val uri = Uri.parse(song.uriString)
                val rows = context.contentResolver.delete(uri, null, null)
                fileDeleted = rows > 0
            } catch (e: Exception) {
                Log.w(tag, "ContentResolver.delete failed for ${song.uriString}: ${e.message}")
            }
        }

        // 3. Remove entry from local Room database
        songDao.deleteSongById(song.id)

        // Return true if either the file or the database record was removed
        true
    }

    // --- Playlists ---

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { list ->
            list.map { it.toSong() }
        }
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim().ifEmpty { "New Playlist" },
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun renamePlaylist(id: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(id, newName.trim().ifEmpty { "Playlist" })
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderPlaylist(playlistId: Long, songIdsInOrder: List<Long>) = withContext(Dispatchers.IO) {
        playlistDao.updateSongOrder(playlistId, songIdsInOrder)
    }

    suspend fun insertSong(song: Song) = withContext(Dispatchers.IO) {
        songDao.insertSong(SongEntity.fromSong(song))
    }

    suspend fun savePreferences(prefs: AppPreferences) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(AppSettingsEntity.fromPreferences(prefs))
    }
}
