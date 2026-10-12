package com.zychocuwek.musify.player

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zychocuwek.musify.data.Api
import java.util.concurrent.ConcurrentHashMap

/**
 * Pemutar latar belakang + notifikasi media + kontrol layar kunci.
 * MediaItem memakai URI "musify://track/<videoId>"; link audio asli baru di-resolve ke backend
 * tepat saat data dibutuhkan (ResolvingDataSource), jadi link yang kedaluwarsa tidak pernah tersimpan di antrean.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val main = Handler(Looper.getMainLooper())

    private data class Cached(val r: Api.Resolved, val at: Long)
    private val cache = ConcurrentHashMap<String, Cached>()
    private val forceFresh = ConcurrentHashMap.newKeySet<String>()
    private val alt = ConcurrentHashMap<String, Api.Resolved>()
    private val altTried = ConcurrentHashMap.newKeySet<String>()
    private var recoverVid: String? = null
    private var recoverTries = 0

    override fun onCreate() {
        super.onCreate()

        val resolver = ResolvingDataSource.Resolver { spec: DataSpec ->
            if (spec.uri.scheme != "musify") return@Resolver spec
            val vid = spec.uri.lastPathSegment ?: return@Resolver spec
            alt[vid]?.let { return@Resolver spec.withUri(Uri.parse(it.url)) }
            val fresh = forceFresh.remove(vid)
            val hit = cache[vid]
            val r = if (!fresh && hit != null && System.currentTimeMillis() - hit.at < 20 * 60_000L) hit.r
            else Api.resolveAudioBlocking(vid, fresh).also { cache[vid] = Cached(it, System.currentTimeMillis()) }
            spec.withUri(Uri.parse(r.url))
        }
        val dsFactory = ResolvingDataSource.Factory(
            DefaultDataSource.Factory(this, OkHttpDataSource.Factory(Api.http)), resolver
        )

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dsFactory))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) = recover()
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) recoverTries = 0 }
        })

        session = MediaSession.Builder(this, player).build()
    }

    /** Pemulihan bertingkat seperti recoverPlayback() di versi web: link baru x3 -> versi lain -> lewati. */
    private fun recover() {
        val item = player.currentMediaItem ?: return
        val vid = item.mediaId
        if (recoverVid != vid) { recoverVid = vid; recoverTries = 0 }
        val pos = player.currentPosition

        fun retry() { player.prepare(); player.seekTo(pos); player.play() }

        if (recoverTries < 3) {
            recoverTries++
            forceFresh.add(vid)
            main.postDelayed({ retry() }, 400L * recoverTries)
            return
        }
        if (altTried.add(vid)) {
            val title = item.mediaMetadata.title?.toString().orEmpty()
            val artist = item.mediaMetadata.artist?.toString().orEmpty()
            Thread {
                try {
                    val r = Api.resolveAlternativeBlocking(title, artist, vid)
                    main.post { alt[vid] = r; toast("Memutar versi lain dari lagu ini"); retry() }
                } catch (e: Exception) { main.post { giveUp() } }
            }.start()
            return
        }
        giveUp()
    }

    private fun giveUp() {
        toast("Lagu ini belum bisa dimuat. Coba lagu lain.")
        if (player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare(); player.play() }
    }

    private fun toast(m: String) = Toast.makeText(applicationContext, m, Toast.LENGTH_SHORT).show()

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
