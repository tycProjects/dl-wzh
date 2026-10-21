package com.example.playback

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
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
    val errorMessage: String? = null
)

object MusicController {
    private const val TAG = "JNMF_MusicController"
    @Volatile
    private var exoPlayer: ExoPlayer? = null
    val equalizerManager = EqualizerManager()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private var originalQueue: List<Song> = emptyList()

    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startProgressTracker() else stopProgressTracker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val duration = player.duration.coerceAtLeast(0)
                        _state.value = _state.value.copy(
                            durationMs = duration,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        handleTrackEnded()
                    }
                    Player.STATE_IDLE -> {
                        // Player in idle state
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer playback error [${error.errorCode}]: ${error.message}", error)
                val friendly = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Audio file missing or deleted from storage"
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Unsupported audio format or corrupt file"
                    else -> "Playback error: ${error.localizedMessage ?: "Unknown error"}"
                }
                _state.value = _state.value.copy(
                    isPlaying = false,
                    errorMessage = friendly
                )
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                try {
                    equalizerManager.attachAudioSession(audioSessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed attaching audio session: ${e.message}")
                }
            }
        })

        // Initial audio session check
        try {
            val sid = player.audioSessionId
            if (sid > 0) {
                equalizerManager.attachAudioSession(sid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking initial audioSessionId: ${e.message}")
        }
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
        try {
            exoPlayer?.let { p ->
                _state.value = _state.value.copy(
                    currentPositionMs = p.currentPosition.coerceAtLeast(0)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in stopProgressTracker: ${e.message}")
        }
    }

    fun playSong(song: Song, newQueue: List<Song> = listOf(song)) {
        val player = exoPlayer
        if (player == null) {
            Log.w(TAG, "ExoPlayer not attached yet. Queuing song.")
            _state.value = _state.value.copy(currentSong = song)
            return
        }

        originalQueue = newQueue

        val effectiveQueue = buildEffectiveQueue()
        val index = effectiveQueue.indexOfFirst { it.id == song.id }.let {
            if (it >= 0) it else 0
        }

        _state.value = _state.value.copy(
            queue = effectiveQueue,
            queueIndex = index,
            currentSong = song,
            errorMessage = null
        )

        try {
            val mediaItem = MediaItem.fromUri(song.contentUri)
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during playSong: ${e.message}", e)
            _state.value = _state.value.copy(
                isPlaying = false,
                errorMessage = "Cannot play track: ${e.localizedMessage ?: "File inaccessible"}"
            )
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 0) {
                    _state.value.currentSong?.let { playSong(it, _state.value.queue) }
                } else {
                    player.play()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause: ${e.message}", e)
        }
    }

    fun next() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val nextIndex = when {
            _state.value.repeatState == RepeatState.ONE -> _state.value.queueIndex
            _state.value.isShuffle -> queue.indices.random()
            else -> (_state.value.queueIndex + 1) % queue.size
        }
        val nextSong = queue[nextIndex]
        playSongAtIndex(nextSong, nextIndex)
    }

    fun previous() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000) {
            try {
                player.seekTo(0)
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking to 0: ${e.message}")
            }
            return
        }
        val prevIndex = if (_state.value.queueIndex - 1 < 0) queue.size - 1 else _state.value.queueIndex - 1
        val prevSong = queue[prevIndex]
        playSongAtIndex(prevSong, prevIndex)
    }

    private fun playSongAtIndex(song: Song, index: Int) {
        val player = exoPlayer ?: return
        _state.value = _state.value.copy(
            currentSong = song,
            queueIndex = index,
            errorMessage = null
        )
        try {
            val mediaItem = MediaItem.fromUri(song.contentUri)
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Playback error in playSongAtIndex: ${e.message}", e)
            _state.value = _state.value.copy(
                isPlaying = false,
                errorMessage = "Playback error: ${e.localizedMessage ?: "File inaccessible"}"
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
        try {
            exoPlayer?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing player: ${e.message}")
        }
    }

    fun resume() {
        try {
            exoPlayer?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming player: ${e.message}")
        }
    }
}
