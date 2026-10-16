package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.example.MainActivity
import com.example.JnmfApp
import com.example.R
import com.example.audio.AudioEngine
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MusicService : Service() {

    private val binder = MusicBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObserverJob: Job? = null
    private var positionObserverJob: Job? = null
    private var songObserverJob: Job? = null
    private var durationObserverJob: Job? = null
    private var mediaSession: MediaSessionCompat? = null

    private val artworkCache = android.util.LruCache<Long, Bitmap>(30)

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                (application as? JnmfApp)?.audioEngine?.pause()
            }
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        observePlaybackState()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    noisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                    RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            }
        } catch (_: Exception) {}

        // Ensure immediate foreground notification if playback started before service binding
        val app = application as? JnmfApp
        val song = app?.audioEngine?.currentSong?.value
        val isPlaying = app?.audioEngine?.isPlaying?.value == true
        if (song != null) {
            val notification = buildMediaNotification(song, isPlaying)
            if (isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun setupMediaSession() {
        val app = application as? JnmfApp ?: return
        val audioEngine = app.audioEngine

        mediaSession = MediaSessionCompat(this, "JNMF_MediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    audioEngine.play()
                }

                override fun onPause() {
                    audioEngine.pause()
                }

                override fun onSkipToNext() {
                    audioEngine.playNext()
                    val newSong = audioEngine.currentSong.value
                    if (newSong != null) {
                        val dur = if (audioEngine.durationMs.value > 0) audioEngine.durationMs.value else newSong.durationMs
                        updateMediaSessionState(true, 0L, dur, newSong)
                        updateNotification(newSong, true)
                    }
                }

                override fun onSkipToPrevious() {
                    audioEngine.playPrevious()
                    val newSong = audioEngine.currentSong.value
                    if (newSong != null) {
                        val dur = if (audioEngine.durationMs.value > 0) audioEngine.durationMs.value else newSong.durationMs
                        updateMediaSessionState(true, 0L, dur, newSong)
                        updateNotification(newSong, true)
                    }
                }

                override fun onSeekTo(pos: Long) {
                    audioEngine.seekTo(pos)
                }

                override fun onStop() {
                    audioEngine.pause()
                    stopForeground(true)
                }

                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                                audioEngine.togglePlayPause()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                audioEngine.play()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                audioEngine.pause()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                audioEngine.playNext()
                                val newSong = audioEngine.currentSong.value
                                if (newSong != null) {
                                    val dur = if (audioEngine.durationMs.value > 0) audioEngine.durationMs.value else newSong.durationMs
                                    updateMediaSessionState(true, 0L, dur, newSong)
                                    updateNotification(newSong, true)
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                audioEngine.playPrevious()
                                val newSong = audioEngine.currentSong.value
                                if (newSong != null) {
                                    val dur = if (audioEngine.durationMs.value > 0) audioEngine.durationMs.value else newSong.durationMs
                                    updateMediaSessionState(true, 0L, dur, newSong)
                                    updateNotification(newSong, true)
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_STOP -> {
                                audioEngine.pause()
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }
            })

            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
    }

    private fun observePlaybackState() {
        val app = application as? JnmfApp ?: return
        val audioEngine = app.audioEngine

        // 1. Observe song changes (NEXT, PREVIOUS, Auto-completion) -> updates notification & lockscreen immediately
        songObserverJob?.cancel()
        songObserverJob = scope.launch {
            audioEngine.currentSong.collectLatest { song ->
                if (song != null) {
                    val isPlaying = audioEngine.isPlaying.value
                    val position = audioEngine.currentPositionMs.value
                    val duration = audioEngine.durationMs.value
                    updateMediaSessionState(isPlaying, position, duration, song)
                    updateNotification(song, isPlaying)
                }
            }
        }

        // 2. Observe play/pause changes
        stateObserverJob?.cancel()
        stateObserverJob = scope.launch {
            audioEngine.isPlaying.collectLatest { isPlaying ->
                val song = audioEngine.currentSong.value
                val position = audioEngine.currentPositionMs.value
                val duration = audioEngine.durationMs.value

                updateMediaSessionState(isPlaying, position, duration, song)

                if (song != null) {
                    updateNotification(song, isPlaying)
                }
            }
        }

        // 3. Observe exact duration once player prepares audio
        durationObserverJob?.cancel()
        durationObserverJob = scope.launch {
            audioEngine.durationMs.collectLatest { duration ->
                val song = audioEngine.currentSong.value
                val isPlaying = audioEngine.isPlaying.value
                val position = audioEngine.currentPositionMs.value
                if (song != null && duration > 0) {
                    updateMediaSessionState(isPlaying, position, duration, song)
                }
            }
        }

        // 4. Observe position for transport controls & seekbar
        positionObserverJob?.cancel()
        positionObserverJob = scope.launch {
            audioEngine.currentPositionMs.collectLatest { pos ->
                val song = audioEngine.currentSong.value
                val isPlaying = audioEngine.isPlaying.value
                val duration = audioEngine.durationMs.value
                updateMediaSessionState(isPlaying, pos, duration, song)
            }
        }
    }

    private fun updateNotification(song: Song, isPlaying: Boolean) {
        val notification = buildMediaNotification(song, isPlaying)
        if (isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun updateMediaSessionState(isPlaying: Boolean, position: Long, duration: Long, song: Song?) {
        val session = mediaSession ?: return

        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, position, 1.0f)
            .build()
        session.setPlaybackState(playbackState)

        if (song != null) {
            val validDuration = if (duration > 0) duration else song.durationMs
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, validDuration)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, createPlaceholderArt(song))
                .build()
            session.setMetadata(metadata)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? JnmfApp
        val audioEngine = app?.audioEngine

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                val isCurrentlyPlaying = audioEngine?.isPlaying?.value == true
                if (!isCurrentlyPlaying) {
                    val song = audioEngine?.currentSong?.value
                    if (song != null) {
                        val notification = buildMediaNotification(song, true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                        } else {
                            startForeground(NOTIFICATION_ID, notification)
                        }
                    }
                }
                audioEngine?.togglePlayPause()
            }
            ACTION_PREV -> {
                audioEngine?.playPrevious()
                val current = audioEngine?.currentSong?.value
                if (current != null) {
                    val isPlaying = audioEngine.isPlaying.value
                    val dur = if (audioEngine.durationMs.value > 0) audioEngine.durationMs.value else current.durationMs
                    updateMediaSessionState(isPlaying, 0L, dur, current)
                    updateNotification(current, isPlaying)
                }
            }
            ACTION_NEXT -> {
                audioEngine?.playNext()
                val current = audioEngine?.currentSong?.value
                if (current != null) {
                    val isPlaying = audioEngine.isPlaying.value
                    val dur = if (audioEngine.durationMs.value > 0) audioEngine.durationMs.value else current.durationMs
                    updateMediaSessionState(isPlaying, 0L, dur, current)
                    updateNotification(current, isPlaying)
                }
            }
            ACTION_STOP -> {
                audioEngine?.pause()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
            else -> {
                // Forward any hardware media button intents (Bluetooth TWS, wired headset, etc.)
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
        }
        return START_STICKY
    }

    private fun buildMediaNotification(song: Song, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Fast-path direct foreground service intents for 0-latency background notification clicks
        val prevIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                1,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                1,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val playPauseIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                2,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                2,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val nextIntent = Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                3,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                3,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.let {
            mediaStyle.setMediaSession(it.sessionToken)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText("${song.artist} • ${song.album}")
            .setSubText("${song.beatCategory.displayName} • ${song.bpm} BPM")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(createPlaceholderArt(song))
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setStyle(mediaStyle)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .build()
    }

    private fun createPlaceholderArt(song: Song): Bitmap {
        val cached = artworkCache.get(song.id)
        if (cached != null) return cached

        val size = 160
        if (!song.albumArtUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(song.albumArtUri)
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        artworkCache.put(song.id, bmp)
                        return bmp
                    }
                }
            } catch (_: Exception) {}
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(8, 14, 10)
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Draw monogram with active cyber style
        val initial = song.title.firstOrNull()?.uppercaseChar()?.toString() ?: "J"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 255, 102)
            textSize = 72f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, yPos, textPaint)
        artworkCache.put(song.id, bitmap)
        return bitmap
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "JNMF Indo Remix Playback"
            val descriptionText = "Controls and status for currently playing audio"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
        stateObserverJob?.cancel()
        positionObserverJob?.cancel()
        songObserverJob?.cancel()
        durationObserverJob?.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "jnmf_playback_channel"
        const val NOTIFICATION_ID = 404

        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_STOP = "com.example.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
