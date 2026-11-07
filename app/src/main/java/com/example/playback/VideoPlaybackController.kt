package com.example.playback

import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.ui.i18n.JnmfStrings
import java.lang.ref.WeakReference
import kotlin.random.Random
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
 * Selectable playback modes for JNMF Video Player.
 * - [NORMAL_VIDEO]: Plays video + audio in foreground; continues audio in background while disabling video frame rendering.
 * - [AUDIO_ONLY]: Plays only the audio track without rendering video frames in foreground or background.
 */
enum class VideoPlaybackMode(val key: String) {
    NORMAL_VIDEO("normal_video"),
    AUDIO_ONLY("audio_only");

    companion object {
        fun fromKey(key: String?): VideoPlaybackMode {
            return entries.find { it.key == key } ?: NORMAL_VIDEO
        }
    }
}

/**
 * Selectable queue / repeat / shuffle playback modes for JNMF Video Player.
 * 1. [PLAY_ONCE]: Play the selected video once, then stop at the end.
 * 2. [REPEAT_ONE]: Automatically replay the current video when it finishes.
 * 3. [SHUFFLE]: Randomize the order of videos when choosing the next video without unnecessary repeats.
 * 4. [REPEAT_ALL]: After the last video, continue from the beginning of the video list.
 */
enum class VideoQueueMode(val key: String) {
    PLAY_ONCE("play_once"),
    REPEAT_ONE("repeat_one"),
    SHUFFLE("shuffle"),
    REPEAT_ALL("repeat_all");

    fun nextMode(): VideoQueueMode = when (this) {
        PLAY_ONCE -> REPEAT_ONE
        REPEAT_ONE -> SHUFFLE
        SHUFFLE -> REPEAT_ALL
        REPEAT_ALL -> PLAY_ONCE
    }

    companion object {
        fun fromKey(key: String?): VideoQueueMode {
            return entries.find { it.key == key } ?: PLAY_ONCE
        }
    }
}

/**
 * Tracks which playback controller currently owns Android's MediaSession and foreground notification.
 */
enum class ActiveMediaSessionOwner {
    MUSIC,
    VIDEO,
    RADIO
}

enum class VideoErrorCode {
    FILE_MISSING,
    UNSUPPORTED_CODEC,
    LOAD_FAILED,
    GENERIC
}

data class VideoPlaybackState(
    val currentVideo: VideoItem? = null,
    val queue: List<VideoItem> = emptyList(),
    val queueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackMode: VideoPlaybackMode = VideoPlaybackMode.NORMAL_VIDEO,
    val queueMode: VideoQueueMode = VideoQueueMode.PLAY_ONCE,
    val isPlayerUiVisible: Boolean = false,
    val isVideoSurfaceAttached: Boolean = false,
    val isVideoTrackDisabled: Boolean = true,
    val videoAspectRatio: Float = 16f / 9f,
    val playerInstanceId: Int = 0,
    val errorCode: VideoErrorCode? = null,
    val errorMessage: String? = null
) {
    /**
     * Video frames should ONLY be decoded and rendered when:
     * 1. A video is loaded,
     * 2. Selected mode is [VideoPlaybackMode.NORMAL_VIDEO], AND
     * 3. The video player UI is actively visible in the unlocked foreground Activity.
     */
    val shouldRenderVideoFrames: Boolean
        get() = currentVideo != null &&
            playbackMode == VideoPlaybackMode.NORMAL_VIDEO &&
            isPlayerUiVisible
}

object VideoPlaybackController {
    private const val TAG = "JNMF_VideoController"

    @Volatile
    private var exoPlayer: ExoPlayer? = null
    private var attachedListener: Player.Listener? = null

    @Volatile
    private var activePlayerViewRef: WeakReference<PlayerView>? = null

    private val _state = MutableStateFlow(VideoPlaybackState())
    val state: StateFlow<VideoPlaybackState> = _state.asStateFlow()

    private val _activeOwner = MutableStateFlow(ActiveMediaSessionOwner.MUSIC)
    val activeOwner: StateFlow<ActiveMediaSessionOwner> = _activeOwner.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var progressJob: Job? = null
    private var resumeOnTransientFocusGain: Boolean = false
    private var isHandlingVideoEnded: Boolean = false

    private var shuffleRandom: Random = Random.Default
    private val shuffleHistory = ArrayDeque<Long>()
    private val unplayedShuffleIds = ArrayDeque<Long>()

    internal fun setShuffleRandomForTesting(random: Random) {
        shuffleRandom = random
    }

    val exoPlayerInstance: ExoPlayer?
        get() = exoPlayer

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        val errCode = _state.value.errorCode
        if (errCode != null) {
            _state.value = _state.value.copy(
                errorMessage = resolveErrorMessage(errCode, lang)
            )
        }
        refreshPlayerMediaMetadata()
    }

    fun clearError() {
        _state.value = _state.value.copy(errorCode = null, errorMessage = null)
    }

    private fun resolveErrorMessage(code: VideoErrorCode, lang: AppLanguage = _language.value): String {
        return when (code) {
            VideoErrorCode.FILE_MISSING -> JnmfStrings.videoFileMissing(lang)
            VideoErrorCode.UNSUPPORTED_CODEC -> JnmfStrings.videoUnsupportedCodec(lang)
            VideoErrorCode.LOAD_FAILED -> JnmfStrings.videoLoadFailed(lang)
            VideoErrorCode.GENERIC -> JnmfStrings.videoPlaybackErrorDefault(lang)
        }
    }

    fun buildMediaItem(
        video: VideoItem,
        mode: VideoPlaybackMode = _state.value.playbackMode,
        lang: AppLanguage = _language.value
    ): MediaItem {
        val modeSubtitle = JnmfStrings.videoNotifModeSubtitle(mode, lang)
        val metadata = MediaMetadata.Builder()
            .setTitle(video.title)
            .setDisplayTitle(video.title)
            .setArtist(modeSubtitle)
            .setSubtitle(modeSubtitle)
            .setAlbumTitle(JnmfStrings.videoNotifBadge(lang))
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId("video_${video.id}")
            .setUri(video.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun attachPlayer(player: ExoPlayer) {
        attachedListener?.let { oldListener ->
            exoPlayer?.removeListener(oldListener)
        }
        this.exoPlayer = player
        _state.value = _state.value.copy(playerInstanceId = System.identityHashCode(player))

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (_state.value.currentVideo != null) {
                    if (!isPlaying && (
                            isHandlingVideoEnded ||
                                player.playbackState == Player.STATE_ENDED ||
                                (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING)
                            )
                    ) {
                        stopProgressTracker()
                        return
                    }
                    val pos = player.currentPosition.coerceAtLeast(0L)
                        .let { if (it > 0L) it else _state.value.currentPositionMs }
                    _state.value = _state.value.copy(
                        isPlaying = isPlaying,
                        currentPositionMs = pos
                    )
                    if (isPlaying) startProgressTracker() else stopProgressTracker()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (_state.value.currentVideo == null || isHandlingVideoEnded) return
                if (!playWhenReady) {
                    syncPositionFromPlayer()
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false
                    )
                    stopProgressTracker()
                } else if (
                    player.playbackState == Player.STATE_READY ||
                    player.playbackState == Player.STATE_BUFFERING
                ) {
                    _state.value = _state.value.copy(
                        isPlaying = true,
                        isBuffering = player.playbackState == Player.STATE_BUFFERING
                    )
                    if (player.isPlaying) {
                        startProgressTracker()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (_state.value.currentVideo == null) return
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _state.value = _state.value.copy(isBuffering = true)
                    }
                    Player.STATE_READY -> {
                        val dur = player.duration.coerceAtLeast(0L)
                            .let { if (it > 0L) it else _state.value.durationMs }
                        _state.value = _state.value.copy(
                            isBuffering = false,
                            durationMs = dur,
                            errorCode = null,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        _state.value = _state.value.copy(isBuffering = false)
                        handleVideoEnded()
                    }
                    Player.STATE_IDLE -> {
                        _state.value = _state.value.copy(isBuffering = false)
                    }
                }
            }

            override fun onRenderedFirstFrame() {
                if (_state.value.currentVideo != null) {
                    _state.value = _state.value.copy(isBuffering = false)
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val pixelRatio = if (videoSize.pixelWidthHeightRatio > 0f) {
                        videoSize.pixelWidthHeightRatio
                    } else {
                        1f
                    }
                    val ratio = (videoSize.width * pixelRatio) / videoSize.height.toFloat()
                    if (ratio.isFinite() && ratio > 0f) {
                        _state.value = _state.value.copy(videoAspectRatio = ratio)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Video ExoPlayer error [${error.errorCode}]: ${error.message}", error)
                val code = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> VideoErrorCode.FILE_MISSING
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> VideoErrorCode.UNSUPPORTED_CODEC
                    else -> VideoErrorCode.GENERIC
                }
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorCode = code,
                    errorMessage = resolveErrorMessage(code)
                )
            }
        }

        attachedListener = listener
        player.addListener(listener)

        // Ensure current track-rendering policy and any pending video are applied to the newly attached player
        applyVideoRenderingPolicy()
        val pendingVideo = _state.value.currentVideo
        if (pendingVideo != null && player.mediaItemCount == 0) {
            prepareVideoOnPlayer(
                player = player,
                video = pendingVideo,
                startPositionMs = _state.value.currentPositionMs,
                playImmediately = _state.value.isPlaying
            )
        } else if (pendingVideo != null) {
            val readyDur = player.duration.coerceAtLeast(0L)
            _state.value = _state.value.copy(
                isBuffering = player.playbackState == Player.STATE_BUFFERING && player.playWhenReady,
                durationMs = if (readyDur > 0L) readyDur else _state.value.durationMs
            )
        }
        activePlayerViewRef?.get()?.let { pendingView ->
            if (_state.value.shouldRenderVideoFrames) {
                attachVideoPlayerView(pendingView)
            }
        }
    }

    fun detachPlayer() {
        stopProgressTracker()
        activePlayerViewRef?.get()?.let { view ->
            try {
                if (view.player != null) {
                    view.player = null
                }
            } catch (_: Exception) {
            }
        }
        attachedListener?.let { listener ->
            exoPlayer?.removeListener(listener)
        }
        attachedListener = null
        exoPlayer = null
        _state.value = _state.value.copy(
            isVideoSurfaceAttached = false,
            playerInstanceId = 0
        )
    }

    /**
     * Called when MusicController starts or resumes music playback so audio outputs never collide.
     * Preserves the loaded video's position and mode while pausing it cleanly and returning
     * MediaSession ownership to Music.
     */
    fun onMusicPlaybackStarted() {
        resumeOnTransientFocusGain = false
        if (_state.value.isPlaying || exoPlayer?.playWhenReady == true) {
            pause()
        }
        RadioPlaybackController.onExternalPlaybackStarted()
        _activeOwner.value = ActiveMediaSessionOwner.MUSIC
    }

    /**
     * Called when RadioPlaybackController starts or resumes broadcast/preview playback.
     * Pauses any active video cleanly and assigns MediaSession ownership to Radio.
     */
    fun onRadioPlaybackStarted() {
        resumeOnTransientFocusGain = false
        if (_state.value.isPlaying || exoPlayer?.playWhenReady == true) {
            pause()
        }
        _activeOwner.value = ActiveMediaSessionOwner.RADIO
    }

    fun releaseRadioSessionOwnership() {
        if (_activeOwner.value == ActiveMediaSessionOwner.RADIO) {
            _activeOwner.value = ActiveMediaSessionOwner.MUSIC
        }
    }

    /**
     * Updates the selected [VideoPlaybackMode] in place without resetting playback position
     * or interrupting audio playback.
     */
    fun setPlaybackMode(mode: VideoPlaybackMode) {
        syncPositionFromPlayer()
        _state.value = _state.value.copy(playbackMode = mode)
        applyVideoRenderingPolicy()
        refreshPlayerMediaMetadata()
    }

    /**
     * Updates the selected [VideoQueueMode] (Play Once, Repeat One, Shuffle, Repeat All)
     * without resetting playback position, fullscreen state, lock state, or active playback.
     */
    fun setQueueMode(mode: VideoQueueMode) {
        syncPositionFromPlayer()
        val previous = _state.value.queueMode
        _state.value = _state.value.copy(queueMode = mode)
        if (mode == VideoQueueMode.SHUFFLE && previous != VideoQueueMode.SHUFFLE) {
            shuffleHistory.clear()
            refillShuffleDeck(
                queue = _state.value.queue,
                excludeId = _state.value.currentVideo?.id,
                initialRound = true
            )
        }
    }

    /**
     * Cycles through [VideoQueueMode.PLAY_ONCE] -> [VideoQueueMode.REPEAT_ONE] ->
     * [VideoQueueMode.SHUFFLE] -> [VideoQueueMode.REPEAT_ALL] -> [VideoQueueMode.PLAY_ONCE].
     */
    fun cycleQueueMode(): VideoQueueMode {
        val next = _state.value.queueMode.nextMode()
        setQueueMode(next)
        return next
    }

    private fun refillShuffleDeck(
        queue: List<VideoItem>,
        excludeId: Long?,
        initialRound: Boolean
    ) {
        unplayedShuffleIds.clear()
        if (queue.size <= 1) return

        val candidateIds = if (initialRound && excludeId != null) {
            queue.map { it.id }.filter { it != excludeId }.toMutableList()
        } else {
            queue.map { it.id }.toMutableList()
        }
        if (candidateIds.isEmpty()) return

        candidateIds.shuffle(shuffleRandom)
        if (!initialRound && excludeId != null && candidateIds.size > 1 && candidateIds[0] == excludeId) {
            val swapIdx = shuffleRandom.nextInt(1, candidateIds.size)
            val tmp = candidateIds[0]
            candidateIds[0] = candidateIds[swapIdx]
            candidateIds[swapIdx] = tmp
        }
        candidateIds.forEach { unplayedShuffleIds.addLast(it) }
    }

    private fun nextShuffledVideo(queue: List<VideoItem>, currentId: Long): VideoItem {
        val validIdsInQueue = queue.map { it.id }.toSet()
        unplayedShuffleIds.removeAll { it !in validIdsInQueue || it == currentId }
        if (unplayedShuffleIds.isEmpty()) {
            refillShuffleDeck(queue = queue, excludeId = currentId, initialRound = false)
        }
        val nextId = if (unplayedShuffleIds.isNotEmpty()) {
            unplayedShuffleIds.removeFirst()
        } else {
            queue.firstOrNull { it.id != currentId }?.id ?: currentId
        }
        return queue.firstOrNull { it.id == nextId } ?: queue[0]
    }

    /**
     * Updates whether the foreground Video Player UI is currently visible on an unlocked screen.
     * In [VideoPlaybackMode.NORMAL_VIDEO], leaving the screen (`visible = false`) disables video frame
     * decoding/rendering while audio continues in the background; returning (`visible = true`) restores
     * video frame rendering at the exact current playback position.
     */
    fun setPlayerUiVisible(visible: Boolean) {
        syncPositionFromPlayer()
        if (_state.value.isPlayerUiVisible == visible) {
            applyVideoRenderingPolicy()
            return
        }
        _state.value = _state.value.copy(isPlayerUiVisible = visible)
        applyVideoRenderingPolicy()
    }

    /**
     * Atomically binds [playerView] to the active [ExoPlayer] using [PlayerView.switchTargetView],
     * preventing outgoing embedded/fullscreen views from wiping out the newly mounted surface.
     */
    @OptIn(UnstableApi::class)
    fun attachVideoPlayerView(playerView: PlayerView) {
        val currentState = _state.value
        if (!currentState.shouldRenderVideoFrames) {
            try {
                if (playerView.player != null) {
                    playerView.player = null
                }
            } catch (_: Exception) {
            }
            if (activePlayerViewRef?.get() === playerView) {
                activePlayerViewRef = null
            }
            if (currentState.isVideoSurfaceAttached) {
                _state.value = currentState.copy(isVideoSurfaceAttached = false)
            }
            return
        }

        val player = exoPlayer
        val previousView = activePlayerViewRef?.get()
        if (
            previousView === playerView &&
            player != null &&
            playerView.player === player &&
            currentState.isVideoSurfaceAttached &&
            !currentState.isVideoTrackDisabled
        ) {
            return
        }

        activePlayerViewRef = WeakReference(playerView)

        if (player != null) {
            try {
                val currentParams = player.trackSelectionParameters
                val wasTrackDisabled = currentParams.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
                if (wasTrackDisabled) {
                    player.trackSelectionParameters = currentParams
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                        .build()
                }

                if (previousView != null && previousView !== playerView) {
                    PlayerView.switchTargetView(player, previousView, playerView)
                } else if (playerView.player !== player) {
                    playerView.player = player
                } else if (!currentState.isVideoSurfaceAttached) {
                    playerView.player = null
                    playerView.player = player
                }

                if (
                    wasTrackDisabled &&
                    !player.playWhenReady &&
                    player.playbackState == Player.STATE_READY &&
                    player.mediaItemCount > 0
                ) {
                    player.seekTo(player.currentPosition.coerceAtLeast(0L))
                }

                if (_state.value.isVideoTrackDisabled || !_state.value.isVideoSurfaceAttached) {
                    _state.value = _state.value.copy(
                        isVideoTrackDisabled = false,
                        isVideoSurfaceAttached = true
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error attaching PlayerView surface: ${e.message}")
            }
        } else {
            if (_state.value.isVideoTrackDisabled || _state.value.isVideoSurfaceAttached) {
                _state.value = _state.value.copy(
                    isVideoTrackDisabled = false,
                    isVideoSurfaceAttached = false
                )
            }
        }
    }

    /**
     * Detaches [playerView] when Compose disposes or resets it.
     * Crucially, if another [PlayerView] has already become the active target (e.g. during
     * embedded <-> fullscreen transitions), this will NEVER clear the active ExoPlayer surface.
     */
    @OptIn(UnstableApi::class)
    fun detachVideoPlayerView(playerView: PlayerView) {
        val isCurrentActive = activePlayerViewRef?.get() === playerView
        try {
            if (playerView.player != null) {
                playerView.player = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unbinding PlayerView: ${e.message}")
        }
        if (isCurrentActive) {
            activePlayerViewRef = null
            _state.value = _state.value.copy(isVideoSurfaceAttached = false)
        }
    }

    /**
     * Unbinds all UI [PlayerView] references when leaving the video screen completely.
     */
    fun clearActiveVideoViews() {
        activePlayerViewRef?.get()?.let { view ->
            try {
                if (view.player != null) {
                    view.player = null
                }
            } catch (_: Exception) {
            }
        }
        activePlayerViewRef = null
        notifyVideoSurfaceAttached(false)
    }

    /**
     * Records whether a UI Surface/PlayerView is actively bound to the underlying ExoPlayer.
     */
    fun notifyVideoSurfaceAttached(attached: Boolean) {
        val allowed = attached && _state.value.shouldRenderVideoFrames
        if (!allowed) {
            try {
                exoPlayer?.clearVideoSurface()
            } catch (e: Exception) {
                Log.w(TAG, "Error clearing video surface: ${e.message}")
            }
        }
        _state.value = _state.value.copy(isVideoSurfaceAttached = allowed)
    }

    /**
     * Enforces zero video frame decoding/rendering whenever in [VideoPlaybackMode.AUDIO_ONLY]
     * or whenever the player UI is backgrounded / off-screen, and restores active surface binding
     * immediately when returning to foreground normal video mode.
     */
    private fun applyVideoRenderingPolicy() {
        val currentState = _state.value
        val shouldRender = currentState.shouldRenderVideoFrames
        val disableVideoTrack = !shouldRender

        val player = exoPlayer
        var surfaceAttached = currentState.isVideoSurfaceAttached
        if (player != null) {
            try {
                val currentParams = player.trackSelectionParameters
                val alreadyDisabled = currentParams.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
                if (alreadyDisabled != disableVideoTrack) {
                    player.trackSelectionParameters = currentParams
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, disableVideoTrack)
                        .build()
                }
                if (disableVideoTrack) {
                    activePlayerViewRef?.get()?.let { view ->
                        if (view.player != null) {
                            view.player = null
                        }
                    }
                    player.clearVideoSurface()
                    surfaceAttached = false
                } else {
                    val activeView = activePlayerViewRef?.get()
                    if (activeView != null) {
                        if (activeView.player !== player) {
                            activeView.player = player
                        }
                        surfaceAttached = true
                        if (
                            alreadyDisabled &&
                            !player.playWhenReady &&
                            player.playbackState == Player.STATE_READY &&
                            player.mediaItemCount > 0
                        ) {
                            player.seekTo(player.currentPosition.coerceAtLeast(0L))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error applying video rendering policy: ${e.message}")
            }
        } else if (disableVideoTrack) {
            surfaceAttached = false
        }

        val resolvedSurfaceAttached = if (disableVideoTrack) false else surfaceAttached
        if (
            _state.value.isVideoTrackDisabled != disableVideoTrack ||
            _state.value.isVideoSurfaceAttached != resolvedSurfaceAttached
        ) {
            _state.value = _state.value.copy(
                isVideoTrackDisabled = disableVideoTrack,
                isVideoSurfaceAttached = resolvedSurfaceAttached
            )
        }
    }

    private fun refreshPlayerMediaMetadata() {
        // Notification and MediaSession metadata are dynamically projected from VideoPlaybackController.state
        // via PlaybackService and JnmfVideoForwardingPlayer, avoiding any re-prepare or seek glitch on ExoPlayer.
    }

    fun updateQueue(videos: List<VideoItem>) {
        if (videos.isEmpty()) return
        val curr = _state.value.currentVideo
        if (curr == null) {
            _state.value = _state.value.copy(queue = videos)
        } else {
            val idx = videos.indexOfFirst { it.id == curr.id }
            if (idx >= 0) {
                _state.value = _state.value.copy(
                    queue = videos,
                    queueIndex = idx,
                    currentVideo = videos[idx]
                )
            }
        }
        val validIds = videos.map { it.id }.toSet()
        unplayedShuffleIds.removeAll { it !in validIds }
        shuffleHistory.removeAll { it !in validIds }
    }

    fun playVideo(
        video: VideoItem,
        newQueue: List<VideoItem> = listOf(video),
        startPositionMs: Long = 0L
    ) {
        playVideoInternal(
            video = video,
            newQueue = newQueue,
            startPositionMs = startPositionMs,
            fromShuffleStep = false
        )
    }

    private fun playVideoInternal(
        video: VideoItem,
        newQueue: List<VideoItem>,
        startPositionMs: Long,
        fromShuffleStep: Boolean
    ) {
        resumeOnTransientFocusGain = false
        // Pause Music & Radio cleanly without destroying music queue, radio schedules, or EQ settings
        MusicController.pause()
        RadioPlaybackController.onExternalPlaybackStarted()
        _activeOwner.value = ActiveMediaSessionOwner.VIDEO

        val effectiveQueue = if (newQueue.isNotEmpty()) newQueue else listOf(video)
        val index = effectiveQueue.indexOfFirst { it.id == video.id }.let { if (it >= 0) it else 0 }
        val clampedStart = startPositionMs.coerceAtLeast(0L)

        if (!fromShuffleStep) {
            shuffleHistory.clear()
            refillShuffleDeck(
                queue = effectiveQueue,
                excludeId = video.id,
                initialRound = true
            )
        } else {
            unplayedShuffleIds.remove(video.id)
        }

        _state.value = _state.value.copy(
            currentVideo = video,
            queue = effectiveQueue,
            queueIndex = index,
            isPlaying = true,
            isBuffering = true,
            currentPositionMs = clampedStart,
            durationMs = video.durationMs.coerceAtLeast(0L),
            errorCode = null,
            errorMessage = null
        )

        applyVideoRenderingPolicy()

        val player = exoPlayer ?: return
        prepareVideoOnPlayer(
            player = player,
            video = video,
            startPositionMs = clampedStart,
            playImmediately = true
        )
    }

    private fun prepareVideoOnPlayer(
        player: ExoPlayer,
        video: VideoItem,
        startPositionMs: Long,
        playImmediately: Boolean
    ) {
        try {
            applyVideoRenderingPolicy()
            val mediaItem = buildMediaItem(video)
            player.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
            player.prepare()
            player.playWhenReady = playImmediately
            if (playImmediately) {
                player.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed preparing video on ExoPlayer: ${e.message}", e)
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorCode = VideoErrorCode.LOAD_FAILED,
                errorMessage = resolveErrorMessage(VideoErrorCode.LOAD_FAILED)
            )
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
        syncPositionFromPlayer()
        _state.value = _state.value.copy(
            isPlaying = false,
            isBuffering = false
        )
        stopProgressTracker()
        try {
            exoPlayer?.playWhenReady = false
            exoPlayer?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing video player: ${e.message}")
        }
    }

    fun resume() {
        val curr = _state.value.currentVideo ?: return
        MusicController.pause()
        RadioPlaybackController.onExternalPlaybackStarted()
        _activeOwner.value = ActiveMediaSessionOwner.VIDEO
        _state.value = _state.value.copy(isPlaying = true, errorCode = null, errorMessage = null)
        applyVideoRenderingPolicy()

        val player = exoPlayer ?: return
        try {
            if (player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 0) {
                prepareVideoOnPlayer(
                    player = player,
                    video = curr,
                    startPositionMs = _state.value.currentPositionMs,
                    playImmediately = true
                )
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0L)
                    _state.value = _state.value.copy(currentPositionMs = 0L)
                }
                if (player.playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(isBuffering = false)
                }
                player.playWhenReady = true
                player.play()
                if (player.isPlaying) {
                    startProgressTracker()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming video player: ${e.message}")
        }
    }

    fun next() {
        var queue = _state.value.queue
        if (queue.isEmpty()) {
            val curr = _state.value.currentVideo ?: return
            queue = listOf(curr)
        }
        if (queue.size <= 1) {
            replayCurrentOrSingleVideo(queue[0])
            return
        }
        val currentIdx = if (_state.value.queueIndex in queue.indices) _state.value.queueIndex else 0
        when (_state.value.queueMode) {
            VideoQueueMode.SHUFFLE -> {
                val currentVideo = queue[currentIdx]
                if (shuffleHistory.lastOrNull() != currentVideo.id) {
                    if (shuffleHistory.size >= 100) {
                        shuffleHistory.removeFirst()
                    }
                    shuffleHistory.addLast(currentVideo.id)
                }
                val nextVideo = nextShuffledVideo(queue, currentVideo.id)
                playVideoInternal(nextVideo, queue, startPositionMs = 0L, fromShuffleStep = true)
            }
            VideoQueueMode.PLAY_ONCE,
            VideoQueueMode.REPEAT_ONE,
            VideoQueueMode.REPEAT_ALL -> {
                val nextIdx = (currentIdx + 1) % queue.size
                playVideoInternal(queue[nextIdx], queue, startPositionMs = 0L, fromShuffleStep = false)
            }
        }
    }

    fun previous() {
        var queue = _state.value.queue
        if (queue.isEmpty()) {
            val curr = _state.value.currentVideo ?: return
            queue = listOf(curr)
        }
        if (queue.size <= 1) {
            replayCurrentOrSingleVideo(queue[0])
            return
        }
        val currentIdx = if (_state.value.queueIndex in queue.indices) _state.value.queueIndex else 0
        when (_state.value.queueMode) {
            VideoQueueMode.SHUFFLE -> {
                val currentVideo = queue[currentIdx]
                var prevVideo: VideoItem? = null
                while (shuffleHistory.isNotEmpty() && prevVideo == null) {
                    val prevId = shuffleHistory.removeLast()
                    if (prevId != currentVideo.id) {
                        prevVideo = queue.firstOrNull { it.id == prevId }
                    }
                }
                if (prevVideo != null) {
                    unplayedShuffleIds.remove(currentVideo.id)
                    unplayedShuffleIds.addFirst(currentVideo.id)
                    playVideoInternal(prevVideo, queue, startPositionMs = 0L, fromShuffleStep = true)
                } else {
                    val fallbackRandom = nextShuffledVideo(queue, currentVideo.id)
                    playVideoInternal(fallbackRandom, queue, startPositionMs = 0L, fromShuffleStep = true)
                }
            }
            VideoQueueMode.PLAY_ONCE,
            VideoQueueMode.REPEAT_ONE,
            VideoQueueMode.REPEAT_ALL -> {
                val prevIdx = if (currentIdx - 1 < 0) queue.size - 1 else currentIdx - 1
                playVideoInternal(queue[prevIdx], queue, startPositionMs = 0L, fromShuffleStep = false)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _state.value.durationMs
        val target = if (duration > 0L) {
            positionMs.coerceIn(0L, duration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        _state.value = _state.value.copy(currentPositionMs = target)
        try {
            val player = exoPlayer
            if (player != null) {
                player.seekTo(target)
                if (player.playbackState == Player.STATE_READY && !player.isLoading) {
                    _state.value = _state.value.copy(isBuffering = false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking video player: ${e.message}")
        }
    }

    fun seekBy(deltaMs: Long) {
        syncPositionFromPlayer()
        seekTo(_state.value.currentPositionMs + deltaMs)
    }

    fun retry() {
        val curr = _state.value.currentVideo ?: return
        val resumePos = _state.value.currentPositionMs
        playVideo(curr, _state.value.queue, startPositionMs = resumePos)
    }

    /**
     * Stops video playback completely, clears rendered surfaces, and releases MediaSession ownership.
     */
    fun stop() {
        resumeOnTransientFocusGain = false
        stopProgressTracker()
        shuffleHistory.clear()
        unplayedShuffleIds.clear()
        activePlayerViewRef?.get()?.let { view ->
            try {
                if (view.player != null) {
                    view.player = null
                }
            } catch (_: Exception) {
            }
        }
        activePlayerViewRef = null
        try {
            exoPlayer?.let { p ->
                p.playWhenReady = false
                p.stop()
                p.clearMediaItems()
                p.clearVideoSurface()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video player: ${e.message}")
        }
        val preservedMode = _state.value.playbackMode
        val preservedQueueMode = _state.value.queueMode
        val preservedQueue = _state.value.queue
        val currentInstanceId = exoPlayer?.let { System.identityHashCode(it) } ?: 0
        _state.value = VideoPlaybackState(
            currentVideo = null,
            queue = preservedQueue,
            queueIndex = -1,
            isPlaying = false,
            isBuffering = false,
            currentPositionMs = 0L,
            durationMs = 0L,
            playbackMode = preservedMode,
            queueMode = preservedQueueMode,
            isPlayerUiVisible = false,
            isVideoSurfaceAttached = false,
            isVideoTrackDisabled = true,
            playerInstanceId = currentInstanceId
        )
        _activeOwner.value = ActiveMediaSessionOwner.MUSIC
    }

    /**
     * Handles headphone disconnection (`AudioManager.ACTION_AUDIO_BECOMING_NOISY`).
     */
    fun handleAudioBecomingNoisy() {
        if (_state.value.isPlaying) {
            pause()
        }
    }

    /**
     * Handles explicit system audio focus transitions deterministically alongside ExoPlayer's
     * built-in AudioFocusManager.
     */
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
                if (resumeOnTransientFocusGain && _state.value.currentVideo != null) {
                    resumeOnTransientFocusGain = false
                    resume()
                }
            }
        }
    }

    private fun stopAtEndOfCurrentVideo() {
        val wasActive = _state.value.isPlaying || exoPlayer?.playWhenReady == true
        stopProgressTracker()
        _state.value = _state.value.copy(
            isPlaying = false,
            isBuffering = false,
            currentPositionMs = 0L
        )
        if (wasActive) {
            try {
                exoPlayer?.let { p ->
                    p.playWhenReady = false
                    p.pause()
                    p.seekTo(0L)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error resetting video at end: ${e.message}")
            }
        }
    }

    private fun replayCurrentOrSingleVideo(video: VideoItem) {
        MusicController.pause()
        RadioPlaybackController.onExternalPlaybackStarted()
        _activeOwner.value = ActiveMediaSessionOwner.VIDEO
        _state.value = _state.value.copy(
            currentVideo = video,
            isPlaying = true,
            isBuffering = false,
            currentPositionMs = 0L,
            errorCode = null,
            errorMessage = null
        )
        applyVideoRenderingPolicy()
        val player = exoPlayer ?: return
        try {
            if (player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 0) {
                prepareVideoOnPlayer(
                    player = player,
                    video = video,
                    startPositionMs = 0L,
                    playImmediately = true
                )
            } else {
                player.seekTo(0L)
                player.playWhenReady = true
                player.play()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error replaying video: ${e.message}")
        }
    }

    internal fun handleVideoEnded() {
        if (isHandlingVideoEnded) return
        val curr = _state.value.currentVideo ?: return
        isHandlingVideoEnded = true
        try {
            when (_state.value.queueMode) {
                VideoQueueMode.PLAY_ONCE -> {
                    stopAtEndOfCurrentVideo()
                }
                VideoQueueMode.REPEAT_ONE -> {
                    replayCurrentOrSingleVideo(curr)
                }
                VideoQueueMode.SHUFFLE -> {
                    val queue = _state.value.queue
                    if (queue.size <= 1) {
                        stopAtEndOfCurrentVideo()
                    } else {
                        next()
                    }
                }
                VideoQueueMode.REPEAT_ALL -> {
                    val queue = _state.value.queue
                    if (queue.size <= 1) {
                        replayCurrentOrSingleVideo(curr)
                    } else {
                        val currentIdx = if (_state.value.queueIndex in queue.indices) {
                            _state.value.queueIndex
                        } else {
                            0
                        }
                        val nextIdx = (currentIdx + 1) % queue.size
                        playVideoInternal(
                            video = queue[nextIdx],
                            newQueue = queue,
                            startPositionMs = 0L,
                            fromShuffleStep = false
                        )
                    }
                }
            }
        } finally {
            isHandlingVideoEnded = false
        }
    }

    private fun syncPositionFromPlayer() {
        try {
            val p = exoPlayer ?: return
            if (_state.value.currentVideo != null && p.mediaItemCount > 0) {
                val playerPos = p.currentPosition.coerceAtLeast(0L)
                // Only overwrite state position when player reports a positive position or is actively ready
                if (playerPos > 0L || p.playbackState == Player.STATE_READY) {
                    val dur = p.duration.coerceAtLeast(0L)
                    _state.value = _state.value.copy(
                        currentPositionMs = playerPos,
                        durationMs = if (dur > 0L) dur else _state.value.durationMs
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing position from player: ${e.message}")
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                syncPositionFromPlayer()
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
