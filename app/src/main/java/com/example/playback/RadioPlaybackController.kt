package com.example.playback

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.RadioItemType
import com.example.data.model.RadioScheduledItem
import com.example.data.model.RadioTimelineCalculator
import com.example.data.repository.AppLanguage
import com.example.data.repository.RadioScheduleRepository
import com.example.ui.i18n.RadioDjStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Immutable snapshot of the JNMF Radio DJ Scheduler playback state.
 * Completely independent from normal [MusicController] playlists, Smart Shuffle, and DJ crossfade.
 */
data class RadioPlaybackState(
    val activeScheduleId: Long? = null,
    val activeScheduleName: String = "",
    val scheduledItems: List<RadioScheduledItem> = emptyList(),
    val currentIndex: Int = -1,
    val currentItem: RadioScheduledItem? = null,
    val upcomingItem: RadioScheduledItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isPreviewMode: Boolean = false,
    val isProgramCompleted: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val unavailableItemIds: Set<Long> = emptySet(),
    val statusNotice: String? = null,
    val errorMessage: String? = null,
    val isTimeAnnouncementPlaying: Boolean = false,
    val announcementTitle: String? = null
) {
    val hasActiveRadioSession: Boolean
        get() = currentItem != null && (isPlaying || isBuffering || currentPositionMs > 0L || !isProgramCompleted)
}

/**
 * Dedicated sequential broadcast playback engine for the JNMF Radio DJ Scheduler.
 *
 * Guarantees:
 * - Plays scheduled items (Music, Radio Ads, DJ Announcements, Station IDs/Jingles, Inserts)
 *   in exact configured sequential order without shuffling or resetting normal playlists.
 * - Lazily acquires a single shared audio [ExoPlayer] from [PlaybackService] only while Radio
 *   is active and releases it on Stop / Program End to conserve RAM on Redmi 10C.
 * - Arbitrates cleanly with [MusicController] and [VideoPlaybackController] so two sessions
 *   never overlap or play simultaneously.
 * - Gracefully skips missing, deleted, or unreadable audio files without crashing or looping.
 */
@OptIn(UnstableApi::class)
object RadioPlaybackController {
    private const val TAG = "JNMF_RadioController"

    @Volatile
    private var appContext: Context? = null

    private var playerFactory: (() -> ExoPlayer)? = null
    private var playerDisposer: ((ExoPlayer) -> Unit)? = null

    @Volatile
    private var exoPlayer: ExoPlayer? = null
    private var attachedListener: Player.Listener? = null

    private val _activeExoPlayerFlow = MutableStateFlow<ExoPlayer?>(null)
    val activeExoPlayerFlow: StateFlow<ExoPlayer?> = _activeExoPlayerFlow.asStateFlow()

    private val _state = MutableStateFlow(RadioPlaybackState())
    val state: StateFlow<RadioPlaybackState> = _state.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var progressJob: Job? = null
    private var playbackGeneration: Long = 0L
    private var pendingAutoPlayOnAttach: Boolean = false
    private var resumeOnTransientFocusGain: Boolean = false
    private var lastDjAdvancedIndex: Int = -1

    data class TimeAnnouncementSettings(
        val enabled: Boolean = false,
        val hourly: Boolean = false,
        val customMinutes: String = "0",
        val audioUri: android.net.Uri? = null,
        val title: String = ""
    )
    private var announcementSettings = TimeAnnouncementSettings()
    private var lastAnnouncementTriggerMinute: Int = -1
    private var postAnnouncementResumeIndex: Int = -1
    private var isTimeAnnouncementActive: Boolean = false

    // Preserved program snapshot when user triggers a temporary single-item Preview
    private var savedProgramBeforePreview: SavedProgramSnapshot? = null

    private data class SavedProgramSnapshot(
        val scheduleId: Long?,
        val scheduleName: String,
        val items: List<RadioScheduledItem>,
        val index: Int,
        val positionMs: Long,
        val durationMs: Long,
        val isCompleted: Boolean
    )

    fun bindAppContext(context: Context) {
        appContext = context.applicationContext
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun attachPlayerProvider(
        context: Context,
        factory: () -> ExoPlayer,
        disposer: (ExoPlayer) -> Unit
    ) {
        appContext = context.applicationContext
        playerFactory = factory
        playerDisposer = disposer

        val s = _state.value
        if (pendingAutoPlayOnAttach && s.isPlaying && s.currentItem != null) {
            pendingAutoPlayOnAttach = false
            prepareAndPlayCurrentItem(
                item = s.currentItem,
                startPositionMs = s.currentPositionMs,
                playImmediately = true
            )
        }
    }

    fun detachPlayerProvider() {
        pendingAutoPlayOnAttach = false
        stopProgressTracker()
        detachAndDisposePlayerInternal()
        playerFactory = null
        playerDisposer = null
    }

    /**
     * Resets all in-memory state cleanly for isolated unit/Robolectric tests.
     */
    internal fun resetForTesting() {
        playbackGeneration++
        pendingAutoPlayOnAttach = false
        resumeOnTransientFocusGain = false
        savedProgramBeforePreview = null
        lastDjAdvancedIndex = -1
        lastAnnouncementTriggerMinute = -1
        postAnnouncementResumeIndex = -1
        isTimeAnnouncementActive = false
        announcementSettings = TimeAnnouncementSettings()
        stopProgressTracker()
        try {
            exoPlayer?.let { p ->
                p.playWhenReady = false
                p.stop()
                p.clearMediaItems()
            }
        } catch (_: Exception) {
        }
        detachAndDisposePlayerInternal()
        VideoPlaybackController.releaseRadioSessionOwnership()
        _state.value = RadioPlaybackState()
    }

    /**
     * Called when normal Music ([MusicController]) or Video ([VideoPlaybackController]) starts
     * playback so Radio DJ never plays simultaneously with another media session.
     */
    fun onExternalPlaybackStarted() {
        resumeOnTransientFocusGain = false
        pendingAutoPlayOnAttach = false
        val s = _state.value
        if (s.isPreviewMode) {
            stopPreviewInternal(restoreOwnerToMusic = false)
            return
        }
        if (s.isPlaying || s.isBuffering || exoPlayer?.playWhenReady == true) {
            pauseInternal()
        }
    }

    /**
     * Starts sequential Radio DJ broadcast playback from [startIndex] (default 0).
     */
    fun startRadioProgram(
        scheduleId: Long,
        scheduleName: String,
        items: List<RadioScheduledItem>,
        startIndex: Int = 0
    ) {
        resumeOnTransientFocusGain = false
        savedProgramBeforePreview = null
        lastDjAdvancedIndex = -1
        isTimeAnnouncementActive = false
        postAnnouncementResumeIndex = -1

        if (items.isEmpty()) {
            _state.value = _state.value.copy(
                activeScheduleId = scheduleId,
                activeScheduleName = scheduleName,
                scheduledItems = emptyList(),
                currentIndex = -1,
                currentItem = null,
                upcomingItem = null,
                isPlaying = false,
                isBuffering = false,
                isPreviewMode = false,
                isProgramCompleted = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                errorMessage = null
            )
            return
        }

        // Arbitrate sessions: pause normal Music (including any DJ crossfade) and Video cleanly
        claimRadioSessionOwnership()

        val clampedStart = startIndex.coerceIn(0, items.lastIndex)
        playFromIndexSkippingUnavailable(
            scheduleId = scheduleId,
            scheduleName = scheduleName,
            items = items,
            candidateStartIndex = clampedStart,
            initialSkippedNotice = null
        )
    }

    /**
     * Plays a single item in Preview mode without altering the saved schedule or normal playlists.
     */
    fun previewItem(item: RadioScheduledItem, scheduleName: String = "") {
        resumeOnTransientFocusGain = false
        val current = _state.value

        // If already previewing this exact item and playing, toggle pause/stop preview
        if (current.isPreviewMode && current.currentItem?.id == item.id && current.isPlaying) {
            stopPreview()
            return
        }

        if (!current.isPreviewMode && current.activeScheduleId != null) {
            syncPositionFromPlayer()
            val refreshed = _state.value
            savedProgramBeforePreview = SavedProgramSnapshot(
                scheduleId = refreshed.activeScheduleId,
                scheduleName = refreshed.activeScheduleName,
                items = refreshed.scheduledItems,
                index = refreshed.currentIndex,
                positionMs = refreshed.currentPositionMs,
                durationMs = refreshed.durationMs,
                isCompleted = refreshed.isProgramCompleted
            )
        }

        if (!isItemReadableNow(item)) {
            markItemUnavailable(item.id)
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                isPreviewMode = false,
                errorMessage = RadioDjStrings.skippedMissingNotice(item.title, _language.value)
            )
            return
        }

        claimRadioSessionOwnership()

        playbackGeneration++
        _state.value = _state.value.copy(
            activeScheduleName = scheduleName.ifBlank { _state.value.activeScheduleName },
            currentItem = item,
            upcomingItem = null,
            isPlaying = true,
            isBuffering = true,
            isPreviewMode = true,
            isProgramCompleted = false,
            currentPositionMs = 0L,
            durationMs = item.durationMs.coerceAtLeast(0L),
            statusNotice = null,
            errorMessage = null
        )

        prepareAndPlayCurrentItem(
            item = item,
            startPositionMs = 0L,
            playImmediately = true
        )
    }

    fun stopPreview() {
        stopPreviewInternal(restoreOwnerToMusic = false)
    }

    private fun stopPreviewInternal(restoreOwnerToMusic: Boolean) {
        playbackGeneration++
        pendingAutoPlayOnAttach = false
        stopProgressTracker()
        try {
            exoPlayer?.let { p ->
                p.playWhenReady = false
                p.stop()
                p.clearMediaItems()
            }
        } catch (_: Exception) {
        }
        detachAndDisposePlayerInternal()

        val snapshot = savedProgramBeforePreview
        savedProgramBeforePreview = null
        if (snapshot != null) {
            val restoredCurrent = snapshot.items.getOrNull(snapshot.index)
            val restoredNext = findNextUpcomingItem(snapshot.items, snapshot.index, _state.value.unavailableItemIds)
            _state.value = _state.value.copy(
                activeScheduleId = snapshot.scheduleId,
                activeScheduleName = snapshot.scheduleName,
                scheduledItems = snapshot.items,
                currentIndex = snapshot.index,
                currentItem = restoredCurrent,
                upcomingItem = restoredNext,
                isPlaying = false,
                isBuffering = false,
                isPreviewMode = false,
                isProgramCompleted = snapshot.isCompleted,
                currentPositionMs = snapshot.positionMs,
                durationMs = snapshot.durationMs,
                statusNotice = null
            )
        } else {
            _state.value = _state.value.copy(
                currentItem = null,
                upcomingItem = null,
                isPlaying = false,
                isBuffering = false,
                isPreviewMode = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                statusNotice = null
            )
        }
        if (restoreOwnerToMusic && VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.RADIO) {
            VideoPlaybackController.releaseRadioSessionOwnership()
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        resumeOnTransientFocusGain = false
        pauseInternal()
    }

    private fun pauseInternal() {
        pendingAutoPlayOnAttach = false
        syncPositionFromPlayer()
        stopProgressTracker()
        _state.value = _state.value.copy(
            isPlaying = false,
            isBuffering = false
        )
        try {
            exoPlayer?.let { p ->
                p.playWhenReady = false
                p.pause()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing Radio ExoPlayer: ${e.message}")
        }
    }

    fun resume() {
        val s = _state.value
        if (s.isPreviewMode && s.currentItem != null) {
            claimRadioSessionOwnership()
            _state.value = s.copy(isPlaying = true, errorMessage = null)
            val p = exoPlayer
            if (p != null && p.playbackState != Player.STATE_IDLE && p.mediaItemCount > 0) {
                if (p.playbackState == Player.STATE_ENDED) {
                    p.seekTo(0L)
                }
                p.playWhenReady = true
                p.play()
                startProgressTracker()
            } else {
                prepareAndPlayCurrentItem(
                    item = s.currentItem,
                    startPositionMs = s.currentPositionMs,
                    playImmediately = true
                )
            }
            return
        }

        if (s.scheduledItems.isEmpty()) return

        // If program already completed, restart from beginning cleanly
        if (s.isProgramCompleted || s.currentItem == null || s.currentIndex !in s.scheduledItems.indices) {
            startRadioProgram(
                scheduleId = s.activeScheduleId ?: -1L,
                scheduleName = s.activeScheduleName,
                items = s.scheduledItems,
                startIndex = 0
            )
            return
        }

        claimRadioSessionOwnership()
        _state.value = s.copy(isPlaying = true, isProgramCompleted = false, errorMessage = null)

        val p = exoPlayer
        if (p != null && p.playbackState != Player.STATE_IDLE && p.mediaItemCount > 0) {
            try {
                if (p.playbackState == Player.STATE_ENDED) {
                    p.seekTo(0L)
                    _state.value = _state.value.copy(currentPositionMs = 0L)
                }
                p.playWhenReady = true
                p.play()
                startProgressTracker()
            } catch (e: Exception) {
                Log.w(TAG, "Error resuming Radio ExoPlayer, re-preparing: ${e.message}")
                prepareAndPlayCurrentItem(
                    item = s.currentItem,
                    startPositionMs = s.currentPositionMs,
                    playImmediately = true
                )
            }
        } else {
            prepareAndPlayCurrentItem(
                item = s.currentItem,
                startPositionMs = s.currentPositionMs,
                playImmediately = true
            )
        }
    }

    fun next() {
        val s = _state.value
        if (s.isPreviewMode) {
            stopPreview()
            return
        }
        if (s.scheduledItems.isEmpty()) return
        val nextCandidate = s.currentIndex + 1
        if (nextCandidate > s.scheduledItems.lastIndex) {
            finishProgramCleanly()
            return
        }
        claimRadioSessionOwnership()
        playFromIndexSkippingUnavailable(
            scheduleId = s.activeScheduleId ?: -1L,
            scheduleName = s.activeScheduleName,
            items = s.scheduledItems,
            candidateStartIndex = nextCandidate,
            initialSkippedNotice = null
        )
    }

    fun previous() {
        val s = _state.value
        if (s.isPreviewMode) {
            seekTo(0L)
            return
        }
        if (s.scheduledItems.isEmpty()) return

        syncPositionFromPlayer()
        if (_state.value.currentPositionMs > 3_500L && s.currentItem != null) {
            seekTo(0L)
            return
        }

        // Find the closest playable item before currentIndex, or restart current item at 0:00
        var prevIdx = s.currentIndex - 1
        while (prevIdx >= 0) {
            val candidate = s.scheduledItems[prevIdx]
            if (isItemReadableNow(candidate)) {
                claimRadioSessionOwnership()
                playExactPlayableIndex(
                    scheduleId = s.activeScheduleId ?: -1L,
                    scheduleName = s.activeScheduleName,
                    items = s.scheduledItems,
                    targetIndex = prevIdx,
                    statusNotice = null
                )
                return
            } else {
                markItemUnavailable(candidate.id)
            }
            prevIdx--
        }

        // Already at first playable item -> seek to start
        if (s.currentItem != null) {
            seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        _state.value = _state.value.copy(currentPositionMs = target)
        try {
            exoPlayer?.seekTo(target)
        } catch (e: Exception) {
            Log.w(TAG, "Error seeking Radio ExoPlayer: ${e.message}")
        }
    }

    fun stop() {
        playbackGeneration++
        pendingAutoPlayOnAttach = false
        resumeOnTransientFocusGain = false
        savedProgramBeforePreview = null
        isTimeAnnouncementActive = false
        postAnnouncementResumeIndex = -1
        stopProgressTracker()
        try {
            exoPlayer?.let { p ->
                p.playWhenReady = false
                p.stop()
                p.clearMediaItems()
            }
        } catch (_: Exception) {
        }
        detachAndDisposePlayerInternal()

        val s = _state.value
        _state.value = s.copy(
            currentIndex = -1,
            currentItem = null,
            upcomingItem = null,
            isPlaying = false,
            isBuffering = false,
            isPreviewMode = false,
            isProgramCompleted = false,
            currentPositionMs = 0L,
            durationMs = 0L,
            statusNotice = null,
            errorMessage = null
        )

        if (VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.RADIO) {
            VideoPlaybackController.releaseRadioSessionOwnership()
        }
    }

    /**
     * Synchronizes edits (reorder, add, remove, replace, rename, type change) made to the
     * currently active schedule in real time without disrupting ongoing audio playback.
     */
    fun syncActiveScheduleEdits(
        scheduleId: Long,
        scheduleName: String,
        updatedItems: List<RadioScheduledItem>
    ) {
        val s = _state.value
        if (s.isPreviewMode || s.activeScheduleId != scheduleId) return

        if (updatedItems.isEmpty()) {
            stop()
            return
        }

        val currentId = s.currentItem?.id
        val newIndex = if (currentId != null) {
            updatedItems.indexOfFirst { it.id == currentId }
        } else {
            -1
        }

        if (newIndex >= 0) {
            val updatedCurrent = updatedItems[newIndex]
            val uriChanged = s.currentItem?.contentUri != updatedCurrent.contentUri
            val upcoming = findNextUpcomingItem(updatedItems, newIndex, s.unavailableItemIds)
            _state.value = s.copy(
                activeScheduleName = scheduleName,
                scheduledItems = updatedItems,
                currentIndex = newIndex,
                currentItem = updatedCurrent,
                upcomingItem = upcoming,
                durationMs = if (uriChanged) updatedCurrent.durationMs else s.durationMs.coerceAtLeast(updatedCurrent.durationMs)
            )
            if (uriChanged && s.isPlaying) {
                playFromIndexSkippingUnavailable(
                    scheduleId = scheduleId,
                    scheduleName = scheduleName,
                    items = updatedItems,
                    candidateStartIndex = newIndex,
                    initialSkippedNotice = null
                )
            }
        } else {
            // Currently playing item was removed from schedule
            val fallbackIdx = s.currentIndex.coerceIn(0, updatedItems.lastIndex)
            if (s.isPlaying) {
                playFromIndexSkippingUnavailable(
                    scheduleId = scheduleId,
                    scheduleName = scheduleName,
                    items = updatedItems,
                    candidateStartIndex = fallbackIdx,
                    initialSkippedNotice = null
                )
            } else {
                val fallbackItem = updatedItems[fallbackIdx]
                val upcoming = findNextUpcomingItem(updatedItems, fallbackIdx, s.unavailableItemIds)
                _state.value = s.copy(
                    activeScheduleName = scheduleName,
                    scheduledItems = updatedItems,
                    currentIndex = fallbackIdx,
                    currentItem = fallbackItem,
                    upcomingItem = upcoming,
                    currentPositionMs = 0L,
                    durationMs = fallbackItem.durationMs
                )
            }
        }
    }

    fun configureTimeAnnouncement(
        enabled: Boolean,
        hourly: Boolean,
        customMinutes: String,
        audioUri: android.net.Uri?,
        title: String
    ) {
        announcementSettings = TimeAnnouncementSettings(
            enabled = enabled,
            hourly = hourly,
            customMinutes = customMinutes,
            audioUri = audioUri,
            title = title
        )
    }

    fun updateCurrentItemGainIfActive(itemId: Long, gainPercent: Int) {
        val s = _state.value
        val clamped = gainPercent.coerceIn(50, 150)
        if (s.currentItem?.id == itemId) {
            val updated = s.currentItem.copy(gainPercent = clamped)
            _state.value = s.copy(currentItem = updated)
            exoPlayer?.volume = (clamped / 100f).coerceIn(0.5f, 1.5f)
        }
    }

    fun triggerTimeAnnouncementNow(uri: android.net.Uri, title: String) {
        val s = _state.value
        if (!s.isPlaying && !s.hasActiveRadioSession) return
        if (isTimeAnnouncementActive) return

        isTimeAnnouncementActive = true
        postAnnouncementResumeIndex = if (s.currentIndex >= 0) s.currentIndex + 1 else 0

        playbackGeneration++
        stopProgressTracker()

        _state.value = s.copy(
            isTimeAnnouncementPlaying = true,
            announcementTitle = title.ifBlank { "Time Announcement" },
            isBuffering = true
        )

        val player = ensurePlayer() ?: return
        try {
            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(title.ifBlank { "Time Announcement" })
                .setArtist("Radio DJ Station")
                .setAlbumTitle(_state.value.activeScheduleName.ifBlank { "JNMF Radio DJ" })

            val mediaItem = MediaItem.Builder()
                .setMediaId("radio_announcement_${System.currentTimeMillis()}")
                .setUri(uri)
                .setMediaMetadata(metadataBuilder.build())
                .build()

            player.repeatMode = Player.REPEAT_MODE_OFF
            player.shuffleModeEnabled = false
            player.volume = 1.0f
            player.setMediaItem(mediaItem, 0L)
            player.prepare()
            player.playWhenReady = true
            player.play()
            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play time announcement: ${e.message}", e)
            isTimeAnnouncementActive = false
            _state.value = _state.value.copy(
                isTimeAnnouncementPlaying = false,
                announcementTitle = null
            )
            val nextIdx = postAnnouncementResumeIndex
            if (nextIdx in s.scheduledItems.indices) {
                playFromIndexSkippingUnavailable(
                    scheduleId = s.activeScheduleId ?: -1L,
                    scheduleName = s.activeScheduleName,
                    items = s.scheduledItems,
                    candidateStartIndex = nextIdx,
                    initialSkippedNotice = null
                )
            } else {
                finishProgramCleanly()
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(statusNotice = null, errorMessage = null)
    }

    fun handleAudioBecomingNoisy() {
        if (_state.value.isPlaying) {
            pause()
        }
    }

    fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnTransientFocusGain = false
                if (_state.value.isPlaying) {
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.isPlaying) {
                    pause()
                    resumeOnTransientFocusGain = true
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeOnTransientFocusGain && _state.value.currentItem != null) {
                    resumeOnTransientFocusGain = false
                    resume()
                }
            }
        }
    }

    private fun claimRadioSessionOwnership() {
        MusicController.pause()
        VideoPlaybackController.onRadioPlaybackStarted()
        appContext?.let { ctx ->
            PlaybackService.startService(ctx)
        }
    }

    private fun playFromIndexSkippingUnavailable(
        scheduleId: Long,
        scheduleName: String,
        items: List<RadioScheduledItem>,
        candidateStartIndex: Int,
        initialSkippedNotice: String?
    ) {
        var idx = candidateStartIndex
        var lastSkippedTitle: String? = null
        val newlyUnavailable = HashSet(_state.value.unavailableItemIds)

        while (idx in items.indices) {
            val candidate = items[idx]
            if (isItemReadableNow(candidate)) {
                newlyUnavailable.remove(candidate.id)
                val notice = when {
                    lastSkippedTitle != null -> RadioDjStrings.skippedMissingNotice(lastSkippedTitle, _language.value)
                    else -> initialSkippedNotice
                }
                _state.value = _state.value.copy(unavailableItemIds = newlyUnavailable)
                playExactPlayableIndex(
                    scheduleId = scheduleId,
                    scheduleName = scheduleName,
                    items = items,
                    targetIndex = idx,
                    statusNotice = notice
                )
                return
            } else {
                newlyUnavailable.add(candidate.id)
                lastSkippedTitle = candidate.title
                idx++
            }
        }

        // Reached end of schedule with no playable items left
        _state.value = _state.value.copy(unavailableItemIds = newlyUnavailable)
        if (candidateStartIndex == 0) {
            // Entire schedule is unavailable/unreadable
            playbackGeneration++
            pendingAutoPlayOnAttach = false
            stopProgressTracker()
            detachAndDisposePlayerInternal()
            _state.value = _state.value.copy(
                activeScheduleId = scheduleId,
                activeScheduleName = scheduleName,
                scheduledItems = items,
                currentIndex = -1,
                currentItem = null,
                upcomingItem = null,
                isPlaying = false,
                isBuffering = false,
                isPreviewMode = false,
                isProgramCompleted = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                errorMessage = RadioDjStrings.allItemsUnavailableError(_language.value)
            )
        } else {
            finishProgramCleanly(
                statusNotice = lastSkippedTitle?.let {
                    RadioDjStrings.skippedMissingNotice(it, _language.value)
                } ?: initialSkippedNotice
            )
        }
    }

    private fun playExactPlayableIndex(
        scheduleId: Long,
        scheduleName: String,
        items: List<RadioScheduledItem>,
        targetIndex: Int,
        statusNotice: String?
    ) {
        val targetItem = items[targetIndex]
        val upcoming = findNextUpcomingItem(items, targetIndex, _state.value.unavailableItemIds)
        if (targetIndex != lastDjAdvancedIndex) {
            lastDjAdvancedIndex = -1
        }

        playbackGeneration++
        _state.value = _state.value.copy(
            activeScheduleId = scheduleId,
            activeScheduleName = scheduleName,
            scheduledItems = items,
            currentIndex = targetIndex,
            currentItem = targetItem,
            upcomingItem = upcoming,
            isPlaying = true,
            isBuffering = true,
            isPreviewMode = false,
            isProgramCompleted = false,
            currentPositionMs = 0L,
            durationMs = targetItem.durationMs.coerceAtLeast(0L),
            statusNotice = statusNotice,
            errorMessage = null
        )

        prepareAndPlayCurrentItem(
            item = targetItem,
            startPositionMs = 0L,
            playImmediately = true
        )
    }

    private fun prepareAndPlayCurrentItem(
        item: RadioScheduledItem,
        startPositionMs: Long,
        playImmediately: Boolean
    ) {
        val player = ensurePlayer()
        if (player == null) {
            // PlaybackService is starting up asynchronously; will auto-play in attachPlayerProvider
            pendingAutoPlayOnAttach = playImmediately
            return
        }

        val expectedGen = playbackGeneration
        try {
            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(
                    buildString {
                        append(RadioDjStrings.itemTypeLabel(item.itemType, _language.value))
                        if (item.subtitle.isNotBlank()) {
                            append(" • ")
                            append(item.subtitle)
                        }
                    }
                )
                .setAlbumTitle(_state.value.activeScheduleName.ifBlank { "JNMF Radio DJ" })

            item.albumArtUri?.let { art ->
                metadataBuilder.setArtworkUri(art)
            }

            val mediaItem = MediaItem.Builder()
                .setMediaId("radio_item_${item.id}")
                .setUri(item.contentUri)
                .setMediaMetadata(metadataBuilder.build())
                .build()

            player.repeatMode = Player.REPEAT_MODE_OFF
            player.shuffleModeEnabled = false
            player.volume = (item.gainPercent / 100f).coerceIn(0.5f, 1.5f)
            player.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
            player.prepare()
            player.playWhenReady = playImmediately
            if (playImmediately) {
                player.play()
                startProgressTracker()
            }
            if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET && player.audioSessionId > 0) {
                MusicController.equalizerManager.attachAudioSession(player.audioSessionId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed preparing radio item '${item.title}': ${e.message}", e)
            if (expectedGen == playbackGeneration) {
                handleCurrentItemFailure(item)
            }
        }
    }

    private fun handleCurrentItemFailure(failedItem: RadioScheduledItem) {
        markItemUnavailable(failedItem.id)
        val s = _state.value
        val notice = RadioDjStrings.skippedMissingNotice(failedItem.title, _language.value)
        if (s.isPreviewMode) {
            stopPreviewInternal(restoreOwnerToMusic = false)
            _state.value = _state.value.copy(errorMessage = notice)
            return
        }
        val nextIdx = s.currentIndex + 1
        if (nextIdx <= s.scheduledItems.lastIndex) {
            playFromIndexSkippingUnavailable(
                scheduleId = s.activeScheduleId ?: -1L,
                scheduleName = s.activeScheduleName,
                items = s.scheduledItems,
                candidateStartIndex = nextIdx,
                initialSkippedNotice = notice
            )
        } else {
            finishProgramCleanly(statusNotice = notice)
        }
    }

    private fun finishProgramCleanly(statusNotice: String? = null) {
        playbackGeneration++
        pendingAutoPlayOnAttach = false
        stopProgressTracker()
        try {
            exoPlayer?.let { p ->
                p.playWhenReady = false
                p.pause()
                p.clearMediaItems()
            }
        } catch (_: Exception) {
        }
        detachAndDisposePlayerInternal()

        val s = _state.value
        _state.value = s.copy(
            isPlaying = false,
            isBuffering = false,
            isPreviewMode = false,
            isProgramCompleted = true,
            currentPositionMs = 0L,
            upcomingItem = null,
            statusNotice = statusNotice ?: s.statusNotice
        )
    }

    private fun ensurePlayer(): ExoPlayer? {
        exoPlayer?.let { return it }
        val factory = playerFactory ?: return null
        val created = try {
            factory()
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating Radio ExoPlayer: ${e.message}", e)
            return null
        }

        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    MusicController.equalizerManager.attachAudioSession(audioSessionId)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val p = exoPlayer ?: return
                val genSnapshot = playbackGeneration
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        if (_state.value.isPlaying) {
                            _state.value = _state.value.copy(isBuffering = true)
                        }
                    }
                    Player.STATE_READY -> {
                        val realDur = p.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                            ?: _state.value.durationMs
                        _state.value = _state.value.copy(
                            isBuffering = false,
                            durationMs = realDur
                        )
                    }
                    Player.STATE_ENDED -> {
                        if (genSnapshot != playbackGeneration) return
                        val currState = _state.value
                        if (isTimeAnnouncementActive) {
                            isTimeAnnouncementActive = false
                            _state.value = currState.copy(
                                isTimeAnnouncementPlaying = false,
                                announcementTitle = null
                            )
                            val nextIdx = postAnnouncementResumeIndex
                            if (nextIdx in currState.scheduledItems.indices) {
                                playFromIndexSkippingUnavailable(
                                    scheduleId = currState.activeScheduleId ?: -1L,
                                    scheduleName = currState.activeScheduleName,
                                    items = currState.scheduledItems,
                                    candidateStartIndex = nextIdx,
                                    initialSkippedNotice = null
                                )
                            } else {
                                finishProgramCleanly()
                            }
                            return
                        }
                        if (currState.isPreviewMode) {
                            stopPreviewInternal(restoreOwnerToMusic = false)
                        } else if (currState.scheduledItems.isNotEmpty() && currState.currentIndex >= 0) {
                            val nextIdx = currState.currentIndex + 1
                            if (nextIdx <= currState.scheduledItems.lastIndex) {
                                playFromIndexSkippingUnavailable(
                                    scheduleId = currState.activeScheduleId ?: -1L,
                                    scheduleName = currState.activeScheduleName,
                                    items = currState.scheduledItems,
                                    candidateStartIndex = nextIdx,
                                    initialSkippedNotice = null
                                )
                            } else {
                                finishProgramCleanly()
                            }
                        }
                    }
                    Player.STATE_IDLE -> {
                        _state.value = _state.value.copy(isBuffering = false)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _state.value = _state.value.copy(isPlaying = true, isBuffering = false)
                    startProgressTracker()
                } else {
                    val p = exoPlayer
                    // Keep isPlaying=true while transitioning/buffering if playWhenReady is still active
                    val stillExpectingPlay = p != null && p.playWhenReady && p.playbackState == Player.STATE_BUFFERING
                    if (!stillExpectingPlay && p?.playbackState != Player.STATE_ENDED) {
                        syncPositionFromPlayer()
                        _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
                        stopProgressTracker()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "Radio ExoPlayer error: ${error.errorCodeName} - ${error.message}")
                val curr = _state.value.currentItem
                if (curr != null) {
                    handleCurrentItemFailure(curr)
                } else {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = error.localizedMessage ?: "Audio playback error"
                    )
                }
            }
        }

        created.addListener(listener)
        exoPlayer = created
        attachedListener = listener
        _activeExoPlayerFlow.value = created
        return created
    }

    private fun detachAndDisposePlayerInternal() {
        val existing = exoPlayer ?: return
        val listener = attachedListener
        exoPlayer = null
        attachedListener = null
        _activeExoPlayerFlow.value = null
        try {
            if (listener != null) {
                existing.removeListener(listener)
            }
        } catch (_: Exception) {
        }
        try {
            playerDisposer?.invoke(existing)
        } catch (_: Exception) {
        }
    }

    private fun startProgressTracker() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                val p = exoPlayer
                if (p != null && (p.isPlaying || p.playWhenReady)) {
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    val dur = p.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: _state.value.durationMs
                    _state.value = _state.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur
                    )
                    checkClockTimeAnnouncement()
                    checkAndTriggerRadioDjTransition(pos, dur)
                }
                delay(250L)
            }
        }
    }

    private fun checkClockTimeAnnouncement() {
        val s = _state.value
        if (!s.isPlaying || isTimeAnnouncementActive || s.scheduledItems.isEmpty()) return
        val config = announcementSettings
        if (!config.enabled || config.audioUri == null) return

        // Priority system:
        // Priority 1: Time Announcement interrupts Music immediately
        // Priority 2: Non-music inserts (Ads, News, Radio Programs, Jingles) complete before announcement
        val current = s.currentItem
        if (current != null && current.itemType != RadioItemType.MUSIC) {
            return
        }

        val cal = java.util.Calendar.getInstance()
        val minute = cal.get(java.util.Calendar.MINUTE)
        val second = cal.get(java.util.Calendar.SECOND)

        if (second > 5) return
        if (minute == lastAnnouncementTriggerMinute) return

        val matches = if (config.hourly && minute == 0) {
            true
        } else {
            val list = config.customMinutes.split(",", " ", ";")
                .mapNotNull { it.trim().toIntOrNull() }
            list.contains(minute)
        }

        if (matches) {
            lastAnnouncementTriggerMinute = minute
            triggerTimeAnnouncementNow(config.audioUri, config.title)
        }
    }

    private fun checkAndTriggerRadioDjTransition(pos: Long, dur: Long) {
        val s = _state.value
        if (s.isPreviewMode || !s.isPlaying || s.scheduledItems.isEmpty()) return
        if (s.currentIndex < 0 || s.currentIndex == lastDjAdvancedIndex) return
        val currentItem = s.currentItem ?: return
        val upcomingItem = s.upcomingItem ?: return

        // Crossfade / overlap occurs ONLY when both current and upcoming items are MUSIC
        // Advertisements, News, Announcements, and Radio Programs are never cut off early
        if (currentItem.itemType != RadioItemType.MUSIC || upcomingItem.itemType != RadioItemType.MUSIC) {
            return
        }

        val djEnabled = MusicController.state.value.djModeEnabled
        val djSeconds = MusicController.state.value.djTransitionSeconds
        if (!djEnabled || djSeconds <= 0) return

        val overlapMs = RadioTimelineCalculator.computeMusicOverlapMs(
            currDurationMs = dur,
            nextDurationMs = upcomingItem.durationMs,
            djModeEnabled = djEnabled,
            djTransitionSeconds = djSeconds
        )
        if (overlapMs < 1_500L) return

        val triggerPositionMs = dur - overlapMs
        if (pos >= triggerPositionMs) {
            lastDjAdvancedIndex = s.currentIndex
            val nextIdx = s.currentIndex + 1
            if (nextIdx <= s.scheduledItems.lastIndex) {
                playFromIndexSkippingUnavailable(
                    scheduleId = s.activeScheduleId ?: -1L,
                    scheduleName = s.activeScheduleName,
                    items = s.scheduledItems,
                    candidateStartIndex = nextIdx,
                    initialSkippedNotice = null
                )
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun syncPositionFromPlayer() {
        val p = exoPlayer ?: return
        try {
            val pos = p.currentPosition.coerceAtLeast(0L)
            val dur = p.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: _state.value.durationMs
            _state.value = _state.value.copy(
                currentPositionMs = pos,
                durationMs = dur
            )
        } catch (_: Exception) {
        }
    }

    private fun isItemReadableNow(item: RadioScheduledItem): Boolean {
        if (!item.isAvailable) return false
        val ctx = appContext ?: return true
        return RadioScheduleRepository.isAudioUriReadable(ctx, item.contentUri)
    }

    private fun markItemUnavailable(itemId: Long) {
        if (itemId <= 0L) return
        val updated = HashSet(_state.value.unavailableItemIds)
        updated.add(itemId)
        _state.value = _state.value.copy(unavailableItemIds = updated)
    }

    private fun findNextUpcomingItem(
        items: List<RadioScheduledItem>,
        currentIndex: Int,
        unavailableIds: Set<Long>
    ): RadioScheduledItem? {
        var idx = currentIndex + 1
        while (idx in items.indices) {
            val candidate = items[idx]
            if (candidate.isAvailable && !unavailableIds.contains(candidate.id)) {
                return candidate
            }
            idx++
        }
        return null
    }
}
