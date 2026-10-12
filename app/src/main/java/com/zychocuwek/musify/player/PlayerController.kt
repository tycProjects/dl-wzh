package com.zychocuwek.musify.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.zychocuwek.musify.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUi(
    val current: Song? = null,
    val playing: Boolean = false,
    val loading: Boolean = false,
    val queue: List<Song> = emptyList(),
    val index: Int = 0,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val speed: Float = 1f
)

/** Jembatan UI <-> PlaybackService (lewat MediaController). */
class PlayerController(private val ctx: Context) {
    private var controller: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var ticker: Job? = null

    private val _ui = MutableStateFlow(PlayerUi())
    val ui: StateFlow<PlayerUi> = _ui
    private val _pos = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _pos
    private val _dur = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _dur

    private var pending: (() -> Unit)? = null

    fun connect() {
        if (controller != null) return
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).buildAsync()
        f.addListener({
            val c = f.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = sync()
            })
            sync()
            pending?.invoke(); pending = null
        }, ContextCompat.getMainExecutor(ctx))
        ticker = scope.launch {
            while (true) {
                controller?.let { c ->
                    _pos.value = c.currentPosition.coerceAtLeast(0)
                    _dur.value = c.duration.takeIf { it > 0 } ?: 0L
                }
                delay(300)
            }
        }
    }

    fun release() { ticker?.cancel(); controller?.release(); controller = null }

    private fun sync() {
        val c = controller ?: return
        val q = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).toSong() }
        _ui.value = PlayerUi(
            current = c.currentMediaItem?.toSong(),
            playing = c.isPlaying || (c.playWhenReady && c.playbackState == Player.STATE_BUFFERING),
            loading = c.playbackState == Player.STATE_BUFFERING,
            queue = q, index = c.currentMediaItemIndex,
            repeat = c.repeatMode, shuffle = c.shuffleModeEnabled, speed = c.playbackParameters.speed
        )
    }

    // ---- Perintah ----
    fun play(list: List<Song>, index: Int = 0) = whenReady { c ->
        if (list.isEmpty()) return@whenReady
        c.setMediaItems(list.map { it.toMediaItem() }, index.coerceIn(0, list.lastIndex), 0L)
        c.prepare(); c.play()
    }

    fun addToQueue(s: Song) = whenReady { c ->
        if (c.mediaItemCount == 0) play(listOf(s)) else c.addMediaItem(s.toMediaItem())
    }

    fun playNext(s: Song) = whenReady { c ->
        if (c.mediaItemCount == 0) play(listOf(s)) else c.addMediaItem(c.currentMediaItemIndex + 1, s.toMediaItem())
    }

    fun toggle() = controller?.let { if (it.isPlaying) it.pause() else it.play() }
    fun next() { controller?.seekToNextMediaItem() }
    fun prev() { controller?.let { if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem() } }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }
    fun jumpTo(index: Int) { controller?.seekTo(index, 0L) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    fun setSpeed(s: Float) { controller?.setPlaybackSpeed(s) }

    private fun whenReady(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else { connect(); pending = { controller?.let(block) } }
    }
}

// ---- Song <-> MediaItem ----
fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(videoId)
    .setUri(Uri.parse("musify://track/$videoId"))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title).setArtist(artist)
            .setArtworkUri(if (cover.isNotBlank()) Uri.parse(cover) else null)
            .setExtras(Bundle().apply { putString("artistId", artistId); putString("albumId", albumId); putString("duration", duration) })
            .build()
    ).build()

fun MediaItem.toSong(): Song = Song(
    videoId = mediaId,
    title = mediaMetadata.title?.toString().orEmpty(),
    artist = mediaMetadata.artist?.toString().orEmpty(),
    artistId = mediaMetadata.extras?.getString("artistId").orEmpty(),
    albumId = mediaMetadata.extras?.getString("albumId").orEmpty(),
    cover = mediaMetadata.artworkUri?.toString().orEmpty(),
    duration = mediaMetadata.extras?.getString("duration").orEmpty()
)
