package com.example.playback

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.ui.i18n.JnmfStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatState {
    OFF, ALL, ONE
}

enum class PlaybackErrorCode {
    AUDIO_MISSING,
    AUDIO_UNSUPPORTED,
    CANNOT_PLAY_TRACK,
    GENERIC
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val isShuffle: Boolean = false,
    val repeatState: RepeatState = RepeatState.ALL,
    val isReverseQueue: Boolean = false,
    val errorCode: PlaybackErrorCode? = null,
    val errorMessage: String? = null
)

object MusicController {
    private const val TAG = "JNMF_MusicController"
    @Volatile
    private var exoPlayer: ExoPlayer? = null
    private var attachedListener: Player.Listener? = null
    val equalizerManager = EqualizerManager()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private var originalQueue: List<Song> = emptyList()

    /**
     * Updates active localization for background notifications and playback errors
     * without stopping, seeking, recreating, or altering ExoPlayer or EqualizerManager.
     */
    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        val currentErrCode = _state.value.errorCode
        if (currentErrCode != null) {
            _state.value = _state.value.copy(
                errorMessage = resolveErrorMessage(currentErrCode, lang)
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorCode = null, errorMessage = null)
    }

    private fun resolveErrorMessage(code: PlaybackErrorCode, lang: AppLanguage = _language.value): String {
        return when (code) {
            PlaybackErrorCode.AUDIO_MISSING -> JnmfStrings.errorAudioMissing(lang)
            PlaybackErrorCode.AUDIO_UNSUPPORTED -> JnmfStrings.errorAudioUnsupported(lang)
            PlaybackErrorCode.CANNOT_PLAY_TRACK -> JnmfStrings.errorCannotPlayTrack(lang)
            PlaybackErrorCode.GENERIC -> JnmfStrings.errorPlaybackGeneric(lang)
        }
    }

    fun buildMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.albumArtUri)
            .setDisplayTitle(song.title)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun attachPlayer(player: ExoPlayer) {
        attachedListener?.let { oldListener ->
            exoPlayer?.removeListener(oldListener)
        }
        this.exoPlayer = player

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _state.value = _state.value.copy(isPlaying = true)
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                    if (!player.playWhenReady ||
                        player.playbackState == Player.STATE_IDLE ||
                        player.playbackState == Player.STATE_ENDED
                    ) {
                        _state.value = _state.value.copy(isPlaying = false)
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    stopProgressTracker()
                    _state.value = _state.value.copy(isPlaying = false)
                } else if (
                    _state.value.currentSong != null &&
                    (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING)
                ) {
                    _state.value = _state.value.copy(isPlaying = true)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null) {
                    val mediaId = mediaItem.mediaId.toLongOrNull()
                    val currentQueue = _state.value.queue
                    val matchingSong = currentQueue.find { it.id == mediaId }
                        ?: _state.value.currentSong
                    if (matchingSong != null) {
                        val newIndex = currentQueue.indexOfFirst { it.id == matchingSong.id }
                        _state.value = _state.value.copy(
                            currentSong = matchingSong,
                            queueIndex = if (newIndex >= 0) newIndex else _state.value.queueIndex,
                            errorCode = null,
                            errorMessage = null
                        )
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val duration = player.duration.coerceAtLeast(0)
                        _state.value = _state.value.copy(
                            durationMs = if (duration > 0L) duration else _state.value.durationMs,
                            isPlaying = player.playWhenReady,
                            errorCode = null,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        handleTrackEnded()
                    }
                    Player.STATE_IDLE -> {
                        if (!player.playWhenReady) {
                            _state.value = _state.value.copy(isPlaying = false)
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        // Buffering state; transitions to STATE_READY once local track is prepared
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer playback error [${error.errorCode}]: ${error.message}", error)
                player.playWhenReady = false
                val code = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlaybackErrorCode.AUDIO_MISSING
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> PlaybackErrorCode.AUDIO_UNSUPPORTED
                    else -> PlaybackErrorCode.GENERIC
                }
                _state.value = _state.value.copy(
                    isPlaying = false,
                    errorCode = code,
                    errorMessage = resolveErrorMessage(code)
                )
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                try {
                    equalizerManager.attachAudioSession(audioSessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed attaching audio session: ${e.message}")
                }
            }
        }

        attachedListener = listener
        player.addListener(listener)

        // Initial audio session check
        try {
            val sid = player.audioSessionId
            if (sid > 0) {
                equalizerManager.attachAudioSession(sid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking initial audioSessionId: ${e.message}")
        }

        // If a song was queued beforePlaybackService finished attaching ExoPlayer, prepare it in place
        val pendingSong = _state.value.currentSong
        if (pendingSong != null && player.mediaItemCount == 0) {
            try {
                val startPos = _state.value.currentPositionMs.coerceAtLeast(0L)
                val shouldPlay = _state.value.isPlaying
                player.setMediaItem(buildMediaItem(pendingSong), startPos)
                player.prepare()
                player.playWhenReady = shouldPlay
                if (shouldPlay) {
                    player.play()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed preparing pending song on attachPlayer: ${e.message}", e)
            }
        }
    }

    fun detachPlayer() {
        progressJob?.cancel()
        progressJob = null
        attachedListener?.let { oldListener ->
            exoPlayer?.removeListener(oldListener)
        }
        attachedListener = null
        exoPlayer = null
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    exoPlayer?.let { p ->
                        if (p.playbackState == Player.STATE_READY || p.isPlaying) {
                            _state.value = _state.value.copy(
                                currentPositionMs = p.currentPosition.coerceAtLeast(0),
                                durationMs = p.duration.coerceAtLeast(0)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error tracking progress: ${e.message}")
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
        try {
            exoPlayer?.let { p ->
                if (_state.value.currentSong != null && p.mediaItemCount > 0) {
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    if (pos > 0L || p.playbackState == Player.STATE_READY) {
                        _state.value = _state.value.copy(currentPositionMs = pos)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in stopProgressTracker: ${e.message}")
        }
    }

    fun playSong(song: Song, newQueue: List<Song> = listOf(song)) {
        VideoPlaybackController.onMusicPlaybackStarted()
        originalQueue = newQueue

        val effectiveQueue = buildEffectiveQueue()
        val index = effectiveQueue.indexOfFirst { it.id == song.id }.let {
            if (it >= 0) it else 0
        }

        // Update state immediately so UI and Notification reflect new song with zero lag
        _state.value = _state.value.copy(
            queue = effectiveQueue,
            queueIndex = index,
            currentSong = song,
            currentPositionMs = 0L,
            durationMs = song.durationMs,
            isPlaying = true,
            errorCode = null,
            errorMessage = null
        )

        val player = exoPlayer
        if (player == null) {
            Log.w(TAG, "ExoPlayer not attached yet. Queuing song.")
            return
        }

        val mediaItem = buildMediaItem(song)

        try {
            // Smoothly set new media item without stopping the player session
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during playSong: ${e.message}", e)
            _state.value = _state.value.copy(
                isPlaying = false,
                errorCode = PlaybackErrorCode.CANNOT_PLAY_TRACK,
                errorMessage = resolveErrorMessage(PlaybackErrorCode.CANNOT_PLAY_TRACK)
            )
        }
    }

    fun togglePlayPause() {
        val currentlyPlaying = exoPlayer?.let { player ->
            player.isPlaying || (
                player.playWhenReady &&
                    player.playbackState == Player.STATE_BUFFERING &&
                    _state.value.currentSong != null
                )
        } ?: _state.value.isPlaying

        if (currentlyPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun next() {
        var queue = _state.value.queue
        if (queue.isEmpty()) {
            val curr = _state.value.currentSong ?: return
            queue = listOf(curr)
        }
        val currentIndex = if (_state.value.queueIndex in queue.indices) _state.value.queueIndex else 0
        val nextIndex = when {
            _state.value.repeatState == RepeatState.ONE -> currentIndex
            _state.value.isShuffle -> {
                if (queue.size > 1) {
                    var rand = queue.indices.random()
                    while (rand == currentIndex) {
                        rand = queue.indices.random()
                    }
                    rand
                } else 0
            }
            else -> (currentIndex + 1) % queue.size
        }
        val nextSong = queue[nextIndex]
        playSongAtIndex(nextSong, nextIndex)
    }

    fun previous() {
        var queue = _state.value.queue
        if (queue.isEmpty()) {
            val curr = _state.value.currentSong ?: return
            queue = listOf(curr)
        }
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000) {
            try {
                player.seekTo(0)
                _state.value = _state.value.copy(currentPositionMs = 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking to 0: ${e.message}")
            }
            return
        }
        val currentIndex = if (_state.value.queueIndex in queue.indices) _state.value.queueIndex else 0
        val prevIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
        val prevSong = queue[prevIndex]
        playSongAtIndex(prevSong, prevIndex)
    }

    private fun playSongAtIndex(song: Song, index: Int) {
        VideoPlaybackController.onMusicPlaybackStarted()
        // Immediately update state with new track for realtime UI and notification response
        _state.value = _state.value.copy(
            currentSong = song,
            queueIndex = index,
            currentPositionMs = 0L,
            durationMs = song.durationMs,
            isPlaying = true,
            errorCode = null,
            errorMessage = null
        )
        val player = exoPlayer ?: return
        val mediaItem = buildMediaItem(song)
        try {
            // Smoothly switch media item without destroying playback session or equalizer
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Playback error in playSongAtIndex: ${e.message}", e)
            _state.value = _state.value.copy(
                isPlaying = false,
                errorCode = PlaybackErrorCode.CANNOT_PLAY_TRACK,
                errorMessage = resolveErrorMessage(PlaybackErrorCode.CANNOT_PLAY_TRACK)
            )
        }
    }

    private fun handleTrackEnded() {
        when (_state.value.repeatState) {
            RepeatState.ONE -> {
                try {
                    exoPlayer?.seekTo(0)
                    exoPlayer?.play()
                } catch (e: Exception) {
                    Log.e(TAG, "Error repeating one: ${e.message}")
                }
            }
            RepeatState.ALL -> next()
            RepeatState.OFF -> {
                if (_state.value.queueIndex < _state.value.queue.size - 1) {
                    next()
                } else {
                    try {
                        exoPlayer?.pause()
                        exoPlayer?.seekTo(0)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping at queue end: ${e.message}")
                    }
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            exoPlayer?.seekTo(positionMs)
            _state.value = _state.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error in seekTo: ${e.message}")
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffle
        _state.value = _state.value.copy(isShuffle = newShuffle)
    }

    fun cycleRepeatMode() {
        val nextRepeat = when (_state.value.repeatState) {
            RepeatState.OFF -> RepeatState.ALL
            RepeatState.ALL -> RepeatState.ONE
            RepeatState.ONE -> RepeatState.OFF
        }
        _state.value = _state.value.copy(repeatState = nextRepeat)
    }

    fun toggleReverseQueue() {
        val newReverse = !_state.value.isReverseQueue
        _state.value = _state.value.copy(isReverseQueue = newReverse)
        val currentSong = _state.value.currentSong
        val updatedQueue = buildEffectiveQueue()
        val newIndex = if (currentSong != null) updatedQueue.indexOfFirst { it.id == currentSong.id } else 0
        _state.value = _state.value.copy(
            queue = updatedQueue,
            queueIndex = if (newIndex >= 0) newIndex else 0
        )
    }

    private fun buildEffectiveQueue(): List<Song> {
        val base = originalQueue
        return if (_state.value.isReverseQueue) base.reversed() else base
    }

    fun pause() {
        progressJob?.cancel()
        progressJob = null
        var preservedPosition = _state.value.currentPositionMs
        try {
            exoPlayer?.let { player ->
                if (_state.value.currentSong != null && player.mediaItemCount > 0) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    if (pos > 0L || player.playbackState == Player.STATE_READY) {
                        preservedPosition = pos
                    }
                }
                player.playWhenReady = false
                player.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing player: ${e.message}")
        }
        _state.value = _state.value.copy(
            isPlaying = false,
            currentPositionMs = preservedPosition
        )
    }

    fun resume() {
        val song = _state.value.currentSong ?: return
        VideoPlaybackController.onMusicPlaybackStarted()
        _state.value = _state.value.copy(
            isPlaying = true,
            errorCode = null,
            errorMessage = null
        )
        val player = exoPlayer ?: return
        try {
            when {
                player.mediaItemCount == 0 || player.playbackState == Player.STATE_IDLE -> {
                    val resumePos = _state.value.currentPositionMs.coerceAtLeast(0L)
                    player.setMediaItem(buildMediaItem(song), resumePos)
                    player.prepare()
                    player.playWhenReady = true
                    player.play()
                }
                player.playbackState == Player.STATE_ENDED -> {
                    player.seekTo(0L)
                    _state.value = _state.value.copy(currentPositionMs = 0L)
                    player.playWhenReady = true
                    player.play()
                }
                else -> {
                    if (!player.playWhenReady) {
                        player.playWhenReady = true
                        player.play()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming player: ${e.message}")
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    fun handleAudioBecomingNoisy() {
        if (_state.value.isPlaying) {
            pause()
        }
    }
}
