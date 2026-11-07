package com.example.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
 * State representing independent audio preview in the Radio DJ Scheduler.
 *
 * Designed to allow listening, comparing, and adjusting individual sound levels
 * in real-time WITHOUT interrupting or stopping the active radio transmission.
 */
data class RadioPreviewState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val previewItemId: Long? = null,
    val title: String = "",
    val uri: Uri? = null,
    val gainPercent: Int = 100,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

/**
 * Isolated audio preview controller for Radio DJ Scheduler items.
 *
 * Runs an independent ExoPlayer instance so sound adjustments can be previewed
 * live without touching RadioPlaybackController's on-air broadcast or MusicController.
 */
@OptIn(UnstableApi::class)
object RadioPreviewController {
    private const val TAG = "JNMF_RadioPreview"

    private val _state = MutableStateFlow(RadioPreviewState())
    val state: StateFlow<RadioPreviewState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var progressJob: Job? = null

    fun previewItem(
        context: Context,
        itemId: Long,
        uri: Uri,
        title: String,
        gainPercent: Int = 100
    ) {
        val current = _state.value
        // If clicking same item and already playing -> toggle pause
        if (current.previewItemId == itemId && exoPlayer != null) {
            if (current.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        stopInternal()

        val clampedGain = gainPercent.coerceIn(50, 150)
        _state.value = RadioPreviewState(
            isPlaying = true,
            isBuffering = true,
            previewItemId = itemId,
            title = title,
            uri = uri,
            gainPercent = clampedGain,
            currentPositionMs = 0L,
            durationMs = 0L
        )

        try {
            val player = ExoPlayer.Builder(context.applicationContext).build()
            player.volume = (clampedGain / 100f).coerceIn(0.5f, 1.5f)
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _state.value = _state.value.copy(isBuffering = true)
                        }
                        Player.STATE_READY -> {
                            val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
                            _state.value = _state.value.copy(
                                isBuffering = false,
                                durationMs = dur
                            )
                        }
                        Player.STATE_ENDED -> {
                            stop()
                        }
                        Player.STATE_IDLE -> {
                            _state.value = _state.value.copy(isBuffering = false)
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(
                        isPlaying = isPlaying,
                        isBuffering = if (isPlaying) false else _state.value.isBuffering
                    )
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }
            })

            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            player.play()
            exoPlayer = player
            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio preview: ${e.message}", e)
            stopInternal()
        }
    }

    fun setGain(gainPercent: Int) {
        val clamped = gainPercent.coerceIn(50, 150)
        _state.value = _state.value.copy(gainPercent = clamped)
        exoPlayer?.volume = (clamped / 100f).coerceIn(0.5f, 1.5f)
    }

    fun pause() {
        try {
            exoPlayer?.pause()
        } catch (_: Exception) {}
        _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
        stopProgressTracker()
    }

    fun resume() {
        try {
            exoPlayer?.play()
            _state.value = _state.value.copy(isPlaying = true)
            startProgressTracker()
        } catch (_: Exception) {}
    }

    fun stop() {
        stopInternal()
    }

    private fun stopInternal() {
        stopProgressTracker()
        try {
            exoPlayer?.let { p ->
                p.playWhenReady = false
                p.stop()
                p.clearMediaItems()
                p.release()
            }
        } catch (_: Exception) {}
        exoPlayer = null
        _state.value = RadioPreviewState()
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
                }
                delay(200L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
