package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.db.JnmfDao
import com.example.data.db.RadioScheduleEntity
import com.example.data.db.RadioScheduleItemEntity
import com.example.data.model.RadioItemType
import com.example.data.model.RadioScheduledItem
import com.example.data.model.RadioTimelineCalculator
import com.example.data.model.Song
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Extracted metadata from an external local audio file (e.g., radio ad, DJ voiceover, jingle).
 */
data class ExtractedAudioClipInfo(
    val title: String,
    val subtitle: String,
    val fileName: String,
    val durationMs: Long,
    val isReadable: Boolean
)

/**
 * Persistence and metadata gateway for the JNMF Radio DJ Scheduler.
 *
 * Completely isolated from normal Playlists ([UserDataRepository]) so creating, editing,
 * duplicating, reordering, or deleting radio schedules never touches normal playlists.
 */
class RadioScheduleRepository(
    context: Context,
    private val dao: JnmfDao
) {
    private val appContext = context.applicationContext
    private val writeMutex = Mutex()

    companion object {
        private const val TAG = "JNMF_RadioScheduleRepo"

        /**
         * Verifies on [Dispatchers.IO] (or caller thread) whether an audio [Uri] can be opened.
         */
        fun isAudioUriReadable(context: Context, uri: Uri): Boolean {
            val scheme = uri.scheme?.lowercase()
            if (scheme.isNullOrBlank()) return false
            if (scheme == "file") {
                val path = uri.path ?: return false
                val f = File(path)
                return f.exists() && f.isFile && f.length() > 0L
            }
            return try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    afd.length != 0L
                } ?: false
            } catch (_: Throwable) {
                false
            }
        }

        /**
         * Reads display name, title, artist/subtitle, and audio duration (with WAV RIFF header
         * fallback when platform extractors are unavailable) without copying the file.
         */
        fun inspectExternalAudioUri(
            context: Context,
            uri: Uri,
            fallbackLabel: String = "Audio Clip"
        ): ExtractedAudioClipInfo {
            var rawFileName = ""
            if (uri.scheme.equals("file", ignoreCase = true)) {
                rawFileName = uri.lastPathSegment.orEmpty()
            } else {
                try {
                    context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) {
                                rawFileName = cursor.getString(idx).orEmpty()
                            }
                        }
                    }
                } catch (_: Throwable) {
                }
            }
            if (rawFileName.isBlank()) {
                rawFileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':').orEmpty()
            }

            val readable = isAudioUriReadable(context, uri)
            var metaTitle = ""
            var metaArtist = ""
            var durationMs = 0L

            if (readable) {
                val retriever = MediaMetadataRetriever()
                try {
                    if (uri.scheme.equals("file", ignoreCase = true) && !uri.path.isNullOrBlank()) {
                        retriever.setDataSource(uri.path)
                    } else {
                        retriever.setDataSource(context, uri)
                    }
                    metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
                    metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty().trim()
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?.coerceAtLeast(0L) ?: 0L
                } catch (_: Throwable) {
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Throwable) {
                    }
                }

                if (durationMs <= 0L) {
                    durationMs = tryReadWavDurationMs(context, uri)
                }
            }

            val cleanStem = rawFileName.substringBeforeLast('.', rawFileName).trim()
            val resolvedTitle = when {
                metaTitle.isNotBlank() -> metaTitle
                cleanStem.isNotBlank() -> cleanStem
                else -> fallbackLabel
            }
            val resolvedSubtitle = when {
                metaArtist.isNotBlank() -> metaArtist
                rawFileName.isNotBlank() -> rawFileName
                else -> fallbackLabel
            }

            return ExtractedAudioClipInfo(
                title = resolvedTitle,
                subtitle = resolvedSubtitle,
                fileName = rawFileName,
                durationMs = durationMs.coerceAtLeast(0L),
                isReadable = readable
            )
        }

        /**
         * Lightweight RIFF/WAVE header parser so PCM WAV ads, jingles, or test clips always yield
         * exact millisecond durations even on minimal codecs or JVM Robolectric environments.
         */
        internal fun tryReadWavDurationMs(context: Context, uri: Uri): Long {
            val stream = try {
                if (uri.scheme.equals("file", ignoreCase = true)) {
                    val path = uri.path ?: return 0L
                    val f = File(path)
                    if (!f.exists()) return 0L
                    f.inputStream()
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            } catch (_: Throwable) {
                null
            } ?: return 0L

            return try {
                stream.use { input ->
                    val header = ByteArray(128)
                    val read = input.read(header)
                    if (read < 44) return 0L
                    if (header[0] != 'R'.code.toByte() ||
                        header[1] != 'I'.code.toByte() ||
                        header[2] != 'F'.code.toByte() ||
                        header[3] != 'F'.code.toByte() ||
                        header[8] != 'W'.code.toByte() ||
                        header[9] != 'A'.code.toByte() ||
                        header[10] != 'V'.code.toByte() ||
                        header[11] != 'E'.code.toByte()
                    ) {
                        return 0L
                    }
                    val buf = ByteBuffer.wrap(header, 0, read).order(ByteOrder.LITTLE_ENDIAN)
                    var offset = 12
                    var byteRate = 0
                    var dataBytes = 0L
                    while (offset + 8 <= read) {
                        val c0 = header[offset].toInt().toChar()
                        val c1 = header[offset + 1].toInt().toChar()
                        val c2 = header[offset + 2].toInt().toChar()
                        val c3 = header[offset + 3].toInt().toChar()
                        val chunkId = "$c0$c1$c2$c3"
                        val chunkSize = buf.getInt(offset + 4).toLong() and 0xFFFFFFFFL
                        if (chunkId == "fmt " && offset + 20 <= read) {
                            byteRate = buf.getInt(offset + 16)
                        } else if (chunkId == "data") {
                            dataBytes = chunkSize
                            break
                        }
                        val step = 8 + chunkSize.toInt() + (if (chunkSize % 2L != 0L) 1 else 0)
                        if (step <= 8) break
                        offset += step
                    }
                    if (byteRate > 0 && dataBytes > 0L) {
                        (dataBytes * 1000L) / byteRate.toLong()
                    } else {
                        0L
                    }
                }
            } catch (_: Throwable) {
                0L
            }
        }
    }

    val allSchedulesFlow: Flow<List<RadioScheduleEntity>> = dao.getAllRadioSchedules()
    val allScheduleItemsFlow: Flow<List<RadioScheduleItemEntity>> = dao.getAllRadioScheduleItems()

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

    suspend fun createSchedule(
        rawName: String,
        startTimeSecondsOfDay: Int = RadioTimelineCalculator.DEFAULT_START_TIME_SECONDS
    ): SaveResult<Long> {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            return SaveResult.Error(IllegalArgumentException("Blank radio schedule name"))
        }
        val normalizedStart = RadioTimelineCalculator.normalizeStartTimeSeconds(startTimeSecondsOfDay)
        return executeSafeWrite(operationName = "createRadioSchedule") {
            val existing = dao.getRadioScheduleByNameSync(trimmed)
            if (existing != null) {
                SaveResult.DuplicateIgnored
            } else {
                val now = System.currentTimeMillis()
                val id = dao.insertRadioSchedule(
                    RadioScheduleEntity(
                        name = trimmed,
                        startTimeSecondsOfDay = normalizedStart,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                if (id > 0L) SaveResult.Success(id) else SaveResult.Error(null)
            }
        }
    }

    suspend fun renameSchedule(scheduleId: Long, rawNewName: String): SaveResult<Unit> {
        val trimmed = rawNewName.trim()
        if (trimmed.isEmpty()) {
            return SaveResult.Error(IllegalArgumentException("Blank radio schedule name"))
        }
        return executeSafeWrite(operationName = "renameRadioSchedule") {
            val existing = dao.getRadioScheduleByNameSync(trimmed)
            if (existing != null && existing.id != scheduleId) {
                SaveResult.DuplicateIgnored
            } else {
                dao.renameRadioSchedule(scheduleId, trimmed)
                SaveResult.Success(Unit)
            }
        }
    }

    suspend fun updateScheduleStartTime(scheduleId: Long, startTimeSecondsOfDay: Int): SaveResult<Unit> {
        val normalized = RadioTimelineCalculator.normalizeStartTimeSeconds(startTimeSecondsOfDay)
        return executeSafeWrite(requiresFreeSpace = false, operationName = "updateRadioScheduleStartTime") {
            dao.updateRadioScheduleStartTime(scheduleId, normalized)
            SaveResult.Success(Unit)
        }
    }

    suspend fun duplicateSchedule(
        sourceScheduleId: Long,
        requestedName: String? = null
    ): SaveResult<Long> {
        return executeSafeWrite(operationName = "duplicateRadioSchedule") {
            val source = dao.getRadioScheduleByIdSync(sourceScheduleId)
                ?: return@executeSafeWrite SaveResult.Error(IllegalArgumentException("Source schedule not found"))

            val baseDesired = requestedName?.trim()?.takeIf { it.isNotEmpty() }
                ?: "${source.name} (Copy)"
            var candidateName = baseDesired
            var suffix = 2
            while (dao.getRadioScheduleByNameSync(candidateName) != null) {
                candidateName = "$baseDesired $suffix"
                suffix++
            }

            val duplicatedId = dao.duplicateRadioSchedule(sourceScheduleId, candidateName)
            if (duplicatedId > 0L) SaveResult.Success(duplicatedId) else SaveResult.Error(null)
        }
    }

    suspend fun deleteSchedule(scheduleId: Long): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "deleteRadioSchedule") {
            dao.deleteRadioScheduleWithItems(scheduleId)
            SaveResult.Success(Unit)
        }
    }

    suspend fun addSongItem(
        scheduleId: Long,
        song: Song,
        itemType: RadioItemType = RadioItemType.MUSIC
    ): SaveResult<Long> {
        return executeSafeWrite(operationName = "addSongItemToRadioSchedule") {
            val existingItems = dao.getItemsForRadioScheduleSync(scheduleId)
            if (existingItems.any { it.songId == song.id }) {
                return@executeSafeWrite SaveResult.DuplicateIgnored
            }
            val itemId = dao.appendRadioScheduleItem(
                scheduleId = scheduleId,
                itemType = itemType.storageKey,
                songId = song.id,
                title = song.title,
                subtitle = song.artist,
                contentUri = song.contentUri.toString(),
                durationMs = song.durationMs.coerceAtLeast(0L),
                fileName = song.fileName
            )
            if (itemId > 0L) SaveResult.Success(itemId) else SaveResult.Error(null)
        }
    }

    suspend fun addSongItems(
        scheduleId: Long,
        songs: List<Song>,
        itemType: RadioItemType = RadioItemType.MUSIC
    ): SaveResult<Int> {
        if (songs.isEmpty()) return SaveResult.Success(0)
        return executeSafeWrite(operationName = "addSongItemsToRadioSchedule") {
            val added = dao.appendRadioScheduleSongs(
                scheduleId = scheduleId,
                songs = songs,
                itemType = itemType.storageKey
            )
            SaveResult.Success(added)
        }
    }

    suspend fun createScheduleAndAddSong(
        rawName: String,
        song: Song,
        itemType: RadioItemType = RadioItemType.MUSIC
    ): SaveResult<Long> {
        val createResult = createSchedule(rawName)
        if (!createResult.isSuccess) {
            return createResult
        }
        val newScheduleId = (createResult as SaveResult.Success).data
        val addResult = addSongItem(newScheduleId, song, itemType)
        return if (addResult.isSuccess) {
            SaveResult.Success(newScheduleId)
        } else {
            addResult
        }
    }

    suspend fun createScheduleAndAddSongs(
        rawName: String,
        songs: List<Song>,
        itemType: RadioItemType = RadioItemType.MUSIC
    ): SaveResult<Int> {
        val createResult = createSchedule(rawName)
        if (!createResult.isSuccess) {
            return when (createResult) {
                is SaveResult.DuplicateIgnored -> SaveResult.DuplicateIgnored
                is SaveResult.InsufficientStorage -> SaveResult.InsufficientStorage
                is SaveResult.Error -> SaveResult.Error(createResult.cause)
                is SaveResult.Success -> SaveResult.Success(0)
            }
        }
        val newScheduleId = (createResult as SaveResult.Success).data
        return addSongItems(newScheduleId, songs, itemType)
    }

    suspend fun saveScheduleExplicitly(scheduleId: Long): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "saveRadioScheduleExplicitly") {
            dao.touchRadioScheduleUpdatedAt(scheduleId)
            SaveResult.Success(Unit)
        }
    }

    suspend fun addExternalUriItem(
        scheduleId: Long,
        uri: Uri,
        itemType: RadioItemType,
        customTitle: String? = null,
        explicitDurationMs: Long? = null
    ): SaveResult<Long> {
        return executeSafeWrite(operationName = "addExternalUriItemToRadioSchedule") {
            takePersistableReadPermissionBestEffort(uri)
            val info = inspectExternalAudioUri(appContext, uri)
            val resolvedTitle = customTitle?.trim()?.takeIf { it.isNotEmpty() } ?: info.title
            val resolvedDuration = (explicitDurationMs ?: info.durationMs).coerceAtLeast(0L)
            val itemId = dao.appendRadioScheduleItem(
                scheduleId = scheduleId,
                itemType = itemType.storageKey,
                songId = -1L,
                title = resolvedTitle,
                subtitle = info.subtitle,
                contentUri = uri.toString(),
                durationMs = resolvedDuration,
                fileName = info.fileName
            )
            if (itemId > 0L) SaveResult.Success(itemId) else SaveResult.Error(null)
        }
    }

    suspend fun replaceItemWithSong(
        itemId: Long,
        song: Song,
        itemType: RadioItemType
    ): SaveResult<Unit> {
        return executeSafeWrite(operationName = "replaceRadioItemWithSong") {
            val updated = dao.replaceRadioScheduleItem(
                itemId = itemId,
                newItemType = itemType.storageKey,
                newSongId = song.id,
                newTitle = song.title,
                newSubtitle = song.artist,
                newContentUri = song.contentUri.toString(),
                newDurationMs = song.durationMs.coerceAtLeast(0L),
                newFileName = song.fileName
            )
            if (updated) SaveResult.Success(Unit) else SaveResult.Error(null)
        }
    }

    suspend fun replaceItemWithExternalUri(
        itemId: Long,
        uri: Uri,
        itemType: RadioItemType,
        customTitle: String? = null,
        explicitDurationMs: Long? = null
    ): SaveResult<Unit> {
        return executeSafeWrite(operationName = "replaceRadioItemWithExternalUri") {
            takePersistableReadPermissionBestEffort(uri)
            val info = inspectExternalAudioUri(appContext, uri)
            val resolvedTitle = customTitle?.trim()?.takeIf { it.isNotEmpty() } ?: info.title
            val resolvedDuration = (explicitDurationMs ?: info.durationMs).coerceAtLeast(0L)
            val updated = dao.replaceRadioScheduleItem(
                itemId = itemId,
                newItemType = itemType.storageKey,
                newSongId = -1L,
                newTitle = resolvedTitle,
                newSubtitle = info.subtitle,
                newContentUri = uri.toString(),
                newDurationMs = resolvedDuration,
                newFileName = info.fileName
            )
            if (updated) SaveResult.Success(Unit) else SaveResult.Error(null)
        }
    }

    suspend fun updateItemType(itemId: Long, newType: RadioItemType): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "updateRadioItemType") {
            val ok = dao.updateRadioScheduleItemType(itemId, newType.storageKey)
            if (ok) SaveResult.Success(Unit) else SaveResult.Error(null)
        }
    }

    suspend fun removeItem(scheduleId: Long, itemId: Long): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "removeRadioScheduleItem") {
            dao.removeRadioScheduleItemAndReindex(scheduleId, itemId)
            SaveResult.Success(Unit)
        }
    }

    suspend fun moveItem(scheduleId: Long, fromIndex: Int, toIndex: Int): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "moveRadioScheduleItem") {
            dao.moveRadioScheduleItem(scheduleId, fromIndex, toIndex)
            SaveResult.Success(Unit)
        }
    }

    suspend fun updateItemGain(itemId: Long, gainPercent: Int): SaveResult<Unit> {
        val clamped = gainPercent.coerceIn(50, 150)
        return executeSafeWrite(requiresFreeSpace = false, operationName = "updateRadioScheduleItemGain") {
            val ok = dao.updateRadioScheduleItemGain(itemId, clamped)
            if (ok) SaveResult.Success(Unit) else SaveResult.Error(null)
        }
    }

    suspend fun updateAutoSchedule(scheduleId: Long, isAutoScheduled: Boolean): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "updateRadioScheduleAutoSchedule") {
            val ok = dao.updateRadioScheduleAutoSchedule(scheduleId, isAutoScheduled)
            if (ok) SaveResult.Success(Unit) else SaveResult.Error(null)
        }
    }

    suspend fun updateAnnouncementSettings(
        scheduleId: Long,
        enabled: Boolean,
        hourly: Boolean,
        audioUri: String,
        title: String,
        durMs: Long,
        customMinutes: String
    ): SaveResult<Unit> {
        return executeSafeWrite(requiresFreeSpace = false, operationName = "updateRadioScheduleAnnouncementSettings") {
            val ok = dao.updateRadioScheduleAnnouncementSettings(
                scheduleId = scheduleId,
                enabled = enabled,
                hourly = hourly,
                audioUri = audioUri,
                title = title,
                durMs = durMs,
                customMinutes = customMinutes
            )
            if (ok) SaveResult.Success(Unit) else SaveResult.Error(null)
        }
    }

    suspend fun getScheduleById(scheduleId: Long): RadioScheduleEntity? = withContext(Dispatchers.IO) {
        dao.getRadioScheduleByIdSync(scheduleId)
    }

    suspend fun getAutoScheduledSchedules(): List<RadioScheduleEntity> = withContext(Dispatchers.IO) {
        dao.getAllRadioSchedulesSync().filter { it.isAutoScheduled }
    }

    suspend fun getItemsForScheduleSync(scheduleId: Long): List<RadioScheduleItemEntity> = withContext(Dispatchers.IO) {
        dao.getItemsForRadioScheduleSync(scheduleId)
    }

    /**
     * Resolves persisted [RadioScheduleItemEntity] rows into UI/playback [RadioScheduledItem] objects
     * without blocking the main thread on slow ContentResolver calls.
     */
    fun resolveItemsForUi(
        entities: List<RadioScheduleItemEntity>,
        songsById: Map<Long, Song>,
        hasScannedLibrary: Boolean,
        knownUnavailableItemIds: Set<Long> = emptySet()
    ): List<RadioScheduledItem> {
        if (entities.isEmpty()) return emptyList()
        return entities
            .sortedWith(compareBy<RadioScheduleItemEntity> { it.orderIndex }.thenBy { it.id })
            .mapIndexed { idx, entity ->
                val matchedSong = if (entity.songId > 0L) songsById[entity.songId] else null
                val effectiveUri = matchedSong?.contentUri ?: Uri.parse(entity.contentUri)
                val fileAvailable = when {
                    knownUnavailableItemIds.contains(entity.id) -> false
                    entity.contentUri.isBlank() && matchedSong == null -> false
                    effectiveUri.scheme.equals("file", ignoreCase = true) -> {
                        val path = effectiveUri.path
                        !path.isNullOrBlank() && File(path).exists()
                    }
                    entity.songId > 0L && hasScannedLibrary -> matchedSong != null
                    else -> true
                }

                RadioScheduledItem(
                    id = entity.id,
                    scheduleId = entity.scheduleId,
                    orderIndex = idx,
                    itemType = RadioItemType.fromKey(entity.itemType),
                    songId = entity.songId,
                    title = matchedSong?.title?.takeIf { it.isNotBlank() } ?: entity.title,
                    subtitle = matchedSong?.artist?.takeIf { it.isNotBlank() } ?: entity.subtitle,
                    contentUri = effectiveUri,
                    durationMs = (matchedSong?.durationMs?.takeIf { it > 0L } ?: entity.durationMs).coerceAtLeast(0L),
                    fileName = matchedSong?.fileName?.takeIf { it.isNotBlank() } ?: entity.fileName,
                    albumArtUri = matchedSong?.albumArtUri,
                    isAvailable = fileAvailable,
                    gainPercent = entity.gainPercent
                )
            }
    }

    /**
     * Background verification pass on [Dispatchers.IO] returning IDs of items whose underlying
     * file or `content://` document is missing or unreadable.
     */
    suspend fun scanUnavailableItemIds(items: List<RadioScheduledItem>): Set<Long> =
        withContext(Dispatchers.IO) {
            if (items.isEmpty()) return@withContext emptySet()
            val missing = HashSet<Long>()
            for (item in items) {
                if (!item.isAvailable || !isAudioUriReadable(appContext, item.contentUri)) {
                    missing.add(item.id)
                }
            }
            missing
        }

    private fun takePersistableReadPermissionBestEffort(uri: Uri) {
        if (!uri.scheme.equals("content", ignoreCase = true)) return
        try {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Normal for MediaStore or non-persistable providers; safe to ignore.
        } catch (_: Throwable) {
        }
    }
}
