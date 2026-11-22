package com.example.musicplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver

/** Фоновый сервис: держит процесс живым, показывает MediaStyle-уведомление и MediaSession */
class MediaPlaybackService : Service() {
    companion object {
        @Volatile var instance: MediaPlaybackService? = null
        const val CH = "player"
        const val ID = 1
    }

    private lateinit var session: MediaSessionCompat
    private var started = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CH, "Воспроизведение", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        session = MediaSessionCompat(this, "MusicPlayer")
        session.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { Bridge.cmd("play") }
            override fun onPause() { Bridge.cmd("pause") }
            override fun onStop() { Bridge.cmd("pause") }
            override fun onSkipToNext() { Bridge.cmd("next") }
            override fun onSkipToPrevious() { Bridge.cmd("prev") }
            override fun onSeekTo(pos: Long) { Bridge.cmd("seek:" + pos / 1000.0) }
        })
        session.isActive = true
        refresh()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(session, intent)   // кнопки уведомления и гарнитуры
        return START_NOT_STICKY
    }

    fun refresh() {
        val s = PlayerState
        session.setMetadata(MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, s.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, s.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, s.dur)
            .apply { s.cover?.let { putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) } }
            .build())
        session.setPlaybackState(PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO)
            .setState(if (s.playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                s.pos, if (s.playing) 1f else 0f)
            .build())

        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun act(icon: Int, title: String, a: Long) =
            NotificationCompat.Action(icon, title, MediaButtonReceiver.buildMediaButtonPendingIntent(this, a))
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle(if (s.title.isEmpty()) getString(R.string.app_name) else s.title)
            .setContentText(s.artist)
            .setLargeIcon(s.cover)
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(s.playing)
            .addAction(act(android.R.drawable.ic_media_previous, "Назад", PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
            .addAction(if (s.playing) act(android.R.drawable.ic_media_pause, "Пауза", PlaybackStateCompat.ACTION_PAUSE)
                       else act(android.R.drawable.ic_media_play, "Играть", PlaybackStateCompat.ACTION_PLAY))
            .addAction(act(android.R.drawable.ic_media_next, "Вперёд", PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
            .setStyle(MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()

        if (!started) {
            started = true
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(ID, n)
        } else {
            getSystemService(NotificationManager::class.java).notify(ID, n)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        session.isActive = false
        session.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
