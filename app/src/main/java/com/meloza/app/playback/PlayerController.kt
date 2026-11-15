package com.meloza.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.meloza.app.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatOne: Boolean = false,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val sleepTimerEndsAt: Long? = null,
)

/**
 * UI-side controller: connects to [PlaybackService] through a MediaController
 * and exposes a single state flow. All commands go through the real player.
 */
class PlayerController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    private var controller: MediaController? = null
    private var sleepJob: Job? = null
    private var tracksById: Map<String, Track> = emptyMap()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /** Called by the UI once it has permission and the library is loaded. */
    var onTrackStarted: ((Track) -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            item?.mediaId?.let { id -> tracksById[id]?.let { onTrackStarted?.invoke(it) } }
        }
    }

    fun connect(onReady: () -> Unit = {}) {
        if (controller != null) return onReady()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(listener); publish(it) }
            onReady()
        }, MoreExecutors.directExecutor())
    }

    /** Starts playback of [tracks] from [startIndex]; the list becomes the queue. */
    fun play(tracks: List<Track>, startIndex: Int = 0) {
        val c = controller ?: return connect { play(tracks, startIndex) }
        if (tracks.isEmpty()) return
        tracksById = tracks.associateBy { it.id.toString() }
        val items = tracks.map { t ->
            MediaItem.Builder()
                .setMediaId(t.id.toString())
                .setUri(t.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.title)
                        .setArtist(t.artist)
                        .setAlbumTitle(t.album)
                        .setArtworkUri(t.artworkUri)
                        .build()
                )
                .build()
        }
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() = controller?.let { if (it.isPlaying) it.pause() else it.play() }
    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) = controller?.seekTo(ms)
    fun skipTo(index: Int) = controller?.seekTo(index, 0L)

    fun toggleShuffle() = controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun toggleRepeatOne() = controller?.let {
        it.repeatMode = if (it.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }
    fun setVolume(v: Float) { controller?.volume = v.coerceIn(0f, 1f) }

    /** Sleep timer: pauses playback after [minutes]. Pass 0 to cancel. */
    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _state.value = _state.value.copy(sleepTimerEndsAt = null)
            return
        }
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        _state.value = _state.value.copy(sleepTimerEndsAt = endsAt)
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _state.value = _state.value.copy(sleepTimerEndsAt = null)
        }
    }

    private fun publish(p: Player) {
        val count = p.mediaItemCount
        val queue = (0 until count).mapNotNull { i -> p.getMediaItemAt(i).mediaId.let { tracksById[it] } }
        val currentId = p.currentMediaItem?.mediaId
        _state.value = _state.value.copy(
            current = currentId?.let { tracksById[it] },
            isPlaying = p.isPlaying,
            positionMs = p.currentPosition.coerceAtLeast(0),
            durationMs = p.duration.takeIf { it > 0 } ?: 0L,
            shuffle = p.shuffleModeEnabled,
            repeatOne = p.repeatMode == Player.REPEAT_MODE_ONE,
            queue = queue,
            currentIndex = p.currentMediaItemIndex,
        )
    }

    /** Equalizer bands: returns (centerFrequencyHz, levelMillibel range) or empty if unsupported. */
    fun equalizerBands(): List<Pair<Int, Int>> {
        val eq = EqualizerHolder.get() ?: return emptyList()
        return (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) to it }
    }
    fun setEqBand(band: Int, levelMb: Short) = EqualizerHolder.get()?.setBandLevel(band.toShort(), levelMb)
    fun setEqEnabled(enabled: Boolean) { EqualizerHolder.get()?.enabled = enabled }
    fun eqLevelRange(): Pair<Short, Short> = EqualizerHolder.get()?.bandLevelRange?.let { it[0] to it[1] } ?: (0.toShort() to 0.toShort())
    fun eqBandLevel(band: Int): Short = EqualizerHolder.get()?.getBandLevel(band.toShort()) ?: 0

    fun disconnect() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}
