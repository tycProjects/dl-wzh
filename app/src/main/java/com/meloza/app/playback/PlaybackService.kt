package com.meloza.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.meloza.app.ui.MainActivity

/**
 * Foreground media service: owns ExoPlayer and the MediaSession.
 * Lock-screen, Bluetooth headset and notification controls are provided
 * by the MediaSession automatically (Android MediaSession support).
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .build()

        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openIntent)
            .build()

        // Equalizer is attached to the real audio session of the player.
        EqualizerHolder.bind(player.audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (!p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
            session = null
        }
        EqualizerHolder.release()
        super.onDestroy()
    }
}

/** Shared holder for the system equalizer bound to the current audio session. */
object EqualizerHolder {
    private var eq: Equalizer? = null

    fun bind(audioSessionId: Int) {
        release()
        if (audioSessionId == 0) return
        eq = runCatching { Equalizer(0, audioSessionId).apply { enabled = false } }.getOrNull()
    }

    fun get(): Equalizer? = eq

    fun release() {
        eq?.release()
        eq = null
    }
}
