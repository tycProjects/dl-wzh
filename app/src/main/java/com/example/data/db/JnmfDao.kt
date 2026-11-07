package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface JnmfDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsSync(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getPlaylistByNameSync(name: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: Long)

    @Transaction
    suspend fun deletePlaylistWithTracks(playlistId: Long) {
        deletePlaylistTracks(playlistId)
        deletePlaylist(playlistId)
    }

    // Playlist Tracks
    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId ASC, orderIndex ASC")
    fun getAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getTracksForPlaylistSync(playlistId: Long): List<PlaylistTrackEntity>

    @Query("SELECT songId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getTrackIdsForPlaylist(playlistId: Long): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun isSongInPlaylistSync(playlistId: Long, songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrackIgnore(track: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Transaction
    suspend fun removeSongFromPlaylistAndReindex(playlistId: Long, songId: Long) {
        removeSongFromPlaylist(playlistId, songId)
        val remaining = getTracksForPlaylistSync(playlistId)
        remaining.forEachIndexed { idx, track ->
            if (track.orderIndex != idx) {
                insertPlaylistTrack(track.copy(orderIndex = idx))
            }
        }
    }

    @Query("SELECT MAX(orderIndex) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Transaction
    suspend fun addSongToPlaylist(
        playlistId: Long,
        songId: Long,
        savedTitle: String = "",
        savedArtist: String = "",
        savedUri: String = "",
        savedFileName: String = ""
    ): Boolean {
        if (isSongInPlaylistSync(playlistId, songId)) {
            // Update metadata snapshot in place if newly provided without altering orderIndex or duplicating
            if (savedTitle.isNotBlank()) {
                val existing = getTracksForPlaylistSync(playlistId).find { it.songId == songId }
                if (existing != null && existing.savedTitle.isBlank()) {
                    insertPlaylistTrack(
                        existing.copy(
                            savedTitle = savedTitle,
                            savedArtist = savedArtist,
                            savedUri = savedUri,
                            savedFileName = savedFileName
                        )
                    )
                }
            }
            return false
        }
        val maxOrder = getMaxOrderIndex(playlistId) ?: -1
        val rowId = insertPlaylistTrackIgnore(
            PlaylistTrackEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = maxOrder + 1,
                savedTitle = savedTitle,
                savedArtist = savedArtist,
                savedUri = savedUri,
                savedFileName = savedFileName
            )
        )
        return rowId != -1L
    }

    @Query("SELECT songId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getTrackIdsForPlaylistSync(playlistId: Long): List<Long>

    @Transaction
    suspend fun reorderPlaylist(playlistId: Long, orderedSongIds: List<Long>) {
        val existingBySongId = getTracksForPlaylistSync(playlistId).associateBy { it.songId }
        val uniqueOrderedIds = orderedSongIds.distinct()
        deletePlaylistTracks(playlistId)
        uniqueOrderedIds.forEachIndexed { index, songId ->
            val previous = existingBySongId[songId]
            insertPlaylistTrack(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    songId = songId,
                    orderIndex = index,
                    savedTitle = previous?.savedTitle.orEmpty(),
                    savedArtist = previous?.savedArtist.orEmpty(),
                    savedUri = previous?.savedUri.orEmpty(),
                    savedFileName = previous?.savedFileName.orEmpty()
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

    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    suspend fun getAllFavoriteIdsSync(): List<Long>

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

    @Query("SELECT * FROM song_classifications")
    suspend fun getAllClassificationsSync(): List<SongClassificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveClassification(classification: SongClassificationEntity)

    // --- Radio DJ Schedules (Isolated from Normal Playlists) ---

    @Query("SELECT * FROM radio_schedules ORDER BY updatedAt DESC, id DESC")
    fun getAllRadioSchedules(): Flow<List<RadioScheduleEntity>>

    @Query("SELECT * FROM radio_schedules ORDER BY updatedAt DESC, id DESC")
    suspend fun getAllRadioSchedulesSync(): List<RadioScheduleEntity>

    @Query("SELECT * FROM radio_schedules WHERE id = :scheduleId LIMIT 1")
    suspend fun getRadioScheduleByIdSync(scheduleId: Long): RadioScheduleEntity?

    @Query("SELECT * FROM radio_schedules WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getRadioScheduleByNameSync(name: String): RadioScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadioSchedule(schedule: RadioScheduleEntity): Long

    @Query("UPDATE radio_schedules SET name = :newName, updatedAt = :updatedAt WHERE id = :scheduleId")
    suspend fun renameRadioSchedule(scheduleId: Long, newName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE radio_schedules SET startTimeSecondsOfDay = :startTimeSeconds, updatedAt = :updatedAt WHERE id = :scheduleId")
    suspend fun updateRadioScheduleStartTime(
        scheduleId: Long,
        startTimeSeconds: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE radio_schedules SET updatedAt = :updatedAt WHERE id = :scheduleId")
    suspend fun touchRadioScheduleUpdatedAt(scheduleId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM radio_schedules WHERE id = :scheduleId")
    suspend fun deleteRadioScheduleRow(scheduleId: Long)

    @Query("DELETE FROM radio_schedule_items WHERE scheduleId = :scheduleId")
    suspend fun deleteRadioScheduleItems(scheduleId: Long)

    @Transaction
    suspend fun deleteRadioScheduleWithItems(scheduleId: Long) {
        deleteRadioScheduleItems(scheduleId)
        deleteRadioScheduleRow(scheduleId)
    }

    @Query("SELECT * FROM radio_schedule_items ORDER BY scheduleId ASC, orderIndex ASC, id ASC")
    fun getAllRadioScheduleItems(): Flow<List<RadioScheduleItemEntity>>

    @Query("SELECT * FROM radio_schedule_items WHERE scheduleId = :scheduleId ORDER BY orderIndex ASC, id ASC")
    fun getItemsForRadioSchedule(scheduleId: Long): Flow<List<RadioScheduleItemEntity>>

    @Query("SELECT * FROM radio_schedule_items WHERE scheduleId = :scheduleId ORDER BY orderIndex ASC, id ASC")
    suspend fun getItemsForRadioScheduleSync(scheduleId: Long): List<RadioScheduleItemEntity>

    @Query("SELECT * FROM radio_schedule_items WHERE id = :itemId LIMIT 1")
    suspend fun getRadioScheduleItemByIdSync(itemId: Long): RadioScheduleItemEntity?

    @Query("SELECT MAX(orderIndex) FROM radio_schedule_items WHERE scheduleId = :scheduleId")
    suspend fun getMaxRadioItemOrderIndex(scheduleId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadioScheduleItem(item: RadioScheduleItemEntity): Long

    @Query("DELETE FROM radio_schedule_items WHERE id = :itemId")
    suspend fun deleteRadioScheduleItemById(itemId: Long)

    @Transaction
    suspend fun appendRadioScheduleItem(
        scheduleId: Long,
        itemType: String,
        songId: Long,
        title: String,
        subtitle: String,
        contentUri: String,
        durationMs: Long,
        fileName: String,
        gainPercent: Int = 100
    ): Long {
        val nextIndex = (getMaxRadioItemOrderIndex(scheduleId) ?: -1) + 1
        val id = insertRadioScheduleItem(
            RadioScheduleItemEntity(
                scheduleId = scheduleId,
                orderIndex = nextIndex,
                itemType = itemType,
                songId = songId,
                title = title,
                subtitle = subtitle,
                contentUri = contentUri,
                durationMs = durationMs,
                fileName = fileName,
                gainPercent = gainPercent
            )
        )
        touchRadioScheduleUpdatedAt(scheduleId)
        return id
    }

    @Transaction
    suspend fun appendRadioScheduleSongs(
        scheduleId: Long,
        songs: List<Song>,
        itemType: String = "MUSIC",
        gainPercent: Int = 100
    ): Int {
        if (songs.isEmpty()) return 0
        val existingItems = getItemsForRadioScheduleSync(scheduleId)
        val existingSongIds = existingItems.mapNotNull { if (it.songId > 0L) it.songId else null }.toSet()
        var nextIndex = (getMaxRadioItemOrderIndex(scheduleId) ?: -1) + 1
        var addedCount = 0

        for (song in songs) {
            if (existingSongIds.contains(song.id)) {
                continue
            }
            insertRadioScheduleItem(
                RadioScheduleItemEntity(
                    scheduleId = scheduleId,
                    orderIndex = nextIndex,
                    itemType = itemType,
                    songId = song.id,
                    title = song.title,
                    subtitle = song.artist,
                    contentUri = song.contentUri.toString(),
                    durationMs = song.durationMs.coerceAtLeast(0L),
                    fileName = song.fileName,
                    gainPercent = gainPercent
                )
            )
            nextIndex++
            addedCount++
        }
        if (addedCount > 0) {
            touchRadioScheduleUpdatedAt(scheduleId)
        }
        return addedCount
    }

    @Transaction
    suspend fun replaceRadioScheduleItem(
        itemId: Long,
        newItemType: String,
        newSongId: Long,
        newTitle: String,
        newSubtitle: String,
        newContentUri: String,
        newDurationMs: Long,
        newFileName: String
    ): Boolean {
        val existing = getRadioScheduleItemByIdSync(itemId) ?: return false
        insertRadioScheduleItem(
            existing.copy(
                itemType = newItemType,
                songId = newSongId,
                title = newTitle,
                subtitle = newSubtitle,
                contentUri = newContentUri,
                durationMs = newDurationMs,
                fileName = newFileName
            )
        )
        touchRadioScheduleUpdatedAt(existing.scheduleId)
        return true
    }

    @Transaction
    suspend fun updateRadioScheduleItemType(itemId: Long, newItemType: String): Boolean {
        val existing = getRadioScheduleItemByIdSync(itemId) ?: return false
        if (existing.itemType == newItemType) return true
        insertRadioScheduleItem(existing.copy(itemType = newItemType))
        touchRadioScheduleUpdatedAt(existing.scheduleId)
        return true
    }

    @Transaction
    suspend fun updateRadioScheduleItemGain(itemId: Long, gainPercent: Int): Boolean {
        val existing = getRadioScheduleItemByIdSync(itemId) ?: return false
        val clamped = gainPercent.coerceIn(50, 150)
        if (existing.gainPercent == clamped) return true
        insertRadioScheduleItem(existing.copy(gainPercent = clamped))
        touchRadioScheduleUpdatedAt(existing.scheduleId)
        return true
    }

    @Transaction
    suspend fun updateRadioScheduleAutoSchedule(scheduleId: Long, isAutoScheduled: Boolean): Boolean {
        val existing = getRadioScheduleByIdSync(scheduleId) ?: return false
        insertRadioSchedule(existing.copy(isAutoScheduled = isAutoScheduled, updatedAt = System.currentTimeMillis()))
        return true
    }

    @Transaction
    suspend fun updateRadioScheduleAnnouncementSettings(
        scheduleId: Long,
        enabled: Boolean,
        hourly: Boolean,
        audioUri: String,
        title: String,
        durMs: Long,
        customMinutes: String
    ): Boolean {
        val existing = getRadioScheduleByIdSync(scheduleId) ?: return false
        insertRadioSchedule(
            existing.copy(
                timeAnnouncementEnabled = enabled,
                hourlyAnnouncementEnabled = hourly,
                timeAnnouncementAudioUri = audioUri,
                timeAnnouncementTitle = title,
                timeAnnouncementDurationMs = durMs,
                customAnnouncementMinutes = customMinutes,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    @Transaction
    suspend fun removeRadioScheduleItemAndReindex(scheduleId: Long, itemId: Long) {
        deleteRadioScheduleItemById(itemId)
        val remaining = getItemsForRadioScheduleSync(scheduleId)
        remaining.forEachIndexed { idx, entry ->
            if (entry.orderIndex != idx) {
                insertRadioScheduleItem(entry.copy(orderIndex = idx))
            }
        }
        touchRadioScheduleUpdatedAt(scheduleId)
    }

    @Transaction
    suspend fun moveRadioScheduleItem(scheduleId: Long, fromIndex: Int, toIndex: Int) {
        val currentItems = getItemsForRadioScheduleSync(scheduleId).toMutableList()
        if (fromIndex in currentItems.indices && toIndex in currentItems.indices && fromIndex != toIndex) {
            val moved = currentItems.removeAt(fromIndex)
            currentItems.add(toIndex, moved)
            currentItems.forEachIndexed { idx, entry ->
                if (entry.orderIndex != idx) {
                    insertRadioScheduleItem(entry.copy(orderIndex = idx))
                }
            }
            touchRadioScheduleUpdatedAt(scheduleId)
        }
    }

    @Transaction
    suspend fun duplicateRadioSchedule(sourceScheduleId: Long, newName: String): Long {
        val source = getRadioScheduleByIdSync(sourceScheduleId) ?: return -1L
        val now = System.currentTimeMillis()
        val newScheduleId = insertRadioSchedule(
            RadioScheduleEntity(
                name = newName,
                startTimeSecondsOfDay = source.startTimeSecondsOfDay,
                isAutoScheduled = source.isAutoScheduled,
                timeAnnouncementEnabled = source.timeAnnouncementEnabled,
                hourlyAnnouncementEnabled = source.hourlyAnnouncementEnabled,
                timeAnnouncementAudioUri = source.timeAnnouncementAudioUri,
                timeAnnouncementTitle = source.timeAnnouncementTitle,
                timeAnnouncementDurationMs = source.timeAnnouncementDurationMs,
                customAnnouncementMinutes = source.customAnnouncementMinutes,
                createdAt = now,
                updatedAt = now
            )
        )
        if (newScheduleId <= 0L) return -1L
        val items = getItemsForRadioScheduleSync(sourceScheduleId)
        items.forEachIndexed { idx, item ->
            insertRadioScheduleItem(
                RadioScheduleItemEntity(
                    scheduleId = newScheduleId,
                    orderIndex = idx,
                    itemType = item.itemType,
                    songId = item.songId,
                    title = item.title,
                    subtitle = item.subtitle,
                    contentUri = item.contentUri,
                    durationMs = item.durationMs,
                    fileName = item.fileName,
                    gainPercent = item.gainPercent
                )
            )
        }
        return newScheduleId
    }
}
