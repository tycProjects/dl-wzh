package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionResult
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentArtworkSongId: Long? = null
    private var currentArtworkBitmap: Bitmap? = null

    companion object {
        const val TAG = "JNMF_PlaybackService"
        const val CHANNEL_ID = "jnmf_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_UPDATE_NOTIFICATION = "com.example.playback.ACTION_UPDATE_NOTIFICATION"
        const val ACTION_PREVIOUS = "com.example.playback.ACTION_PREVIOUS"
        const val ACTION_PLAY_PAUSE = "com.example.playback.ACTION_PLAY_PAUSE"
        const val ACTION_PLAY = "com.example.playback.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.playback.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.playback.ACTION_NEXT"

        const val EXTRA_STATUS_TEXT = "extra_status_text"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, PlaybackService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting PlaybackService: ${e.message}", e)
            }
        }

        fun triggerNotificationUpdate(context: Context, statusText: String? = null) {
            try {
                val intent = Intent(context, PlaybackService::class.java).apply {
                    action = ACTION_UPDATE_NOTIFICATION
                    if (statusText != null) putExtra(EXTRA_STATUS_TEXT, statusText)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering notification update: ${e.message}", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Post initial foreground notification immediately to satisfy Android 8-14 requirements
        try {
            val initialNotification = buildForegroundNotification(MusicController.state.value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating startForeground: ${e.message}", e)
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val exo = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true) // Handles audio focus automatically
                .setHandleAudioBecomingNoisy(true) // Auto pauses on headphone disconnect
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()

            this.player = exo

            val sessionActivityIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Custom MediaSession callback to handle lock screen / Bluetooth commands directly
            val sessionCallback = object : MediaSession.Callback {
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    when (playerCommand) {
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                            MusicController.next()
                            return SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                            MusicController.previous()
                            return SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_PLAY_PAUSE -> {
                            MusicController.togglePlayPause()
                            return SessionResult.RESULT_SUCCESS
                        }
                    }
                    return super.onPlayerCommandRequest(session, controller, playerCommand)
                }
            }

            mediaSession = MediaSession.Builder(this, exo)
                .setSessionActivity(sessionActivityIntent)
                .setCallback(sessionCallback)
                .build()

            MusicController.attachPlayer(exo)

            // Observe MusicController state to update lock-screen / status bar notification in real-time
            serviceScope.launch {
                MusicController.state.collect { state ->
                    val song = state.currentSong
                    updateArtworkForSong(song)
                    updateNotification(state)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer or MediaSession: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREVIOUS -> {
                MusicController.previous()
            }
            ACTION_PLAY_PAUSE -> {
                MusicController.togglePlayPause()
            }
            ACTION_PLAY -> {
                MusicController.resume()
            }
            ACTION_PAUSE -> {
                MusicController.pause()
            }
            ACTION_NEXT -> {
                MusicController.next()
            }
        }

        updateNotification(MusicController.state.value)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification(state: PlaybackState = MusicController.state.value) {
        try {
            val notification = buildForegroundNotification(state, currentArtworkBitmap)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing foreground notification: ${e.message}", e)
        }
    }

    private fun updateArtworkForSong(song: Song?) {
        if (song == null) {
            currentArtworkSongId = null
            currentArtworkBitmap = null
            return
        }
        if (song.id == currentArtworkSongId) {
            return
        }
        currentArtworkSongId = song.id
        currentArtworkBitmap = null

        val artUri = song.albumArtUri
        if (artUri != null) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(this@PlaybackService)
                    val request = ImageRequest.Builder(this@PlaybackService)
                        .data(artUri)
                        .size(300, 300)
                        .allowHardware(false) // Software bitmap required for Notification
                        .build()
                    val result = loader.execute(request)
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        if (currentArtworkSongId == song.id) {
                            currentArtworkBitmap = drawable.bitmap
                            // Re-post notification with loaded artwork
                            serviceScope.launch(Dispatchers.Main) {
                                updateNotification()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load artwork bitmap for notification: ${e.message}")
                }
            }
        }
    }

    private fun buildForegroundNotification(state: PlaybackState, artworkBitmap: Bitmap? = null): Notification {
        val song = state.currentSong
        val isPlaying = state.isPlaying

        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Previous Action Intent
        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Play/Pause Action Intent
        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Next Action Intent
        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = song?.title ?: "JNMF Music Player"
        val artist = song?.artist ?: "Ready for playback"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(song?.remixCategory?.displayName ?: "JNMF")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(launchIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)

        if (artworkBitmap != null) {
            builder.setLargeIcon(artworkBitmap)
        }

        // Add media controls: [0] Previous, [1] Play/Pause, [2] Next
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_previous,
                "Previous",
                prevIntent
            ).build()
        )

        builder.addAction(
            NotificationCompat.Action.Builder(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            ).build()
        )

        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_next,
                "Next",
                nextIntent
            ).build()
        )

        // Connect notification to MediaSession using MediaStyle
        mediaSession?.let { session ->
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        return builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JNMF Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground playback controls for JNMF Music Player"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            serviceScope.cancel()
            MusicController.equalizerManager.releaseEffects()
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
            player = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during PlaybackService onDestroy: ${e.message}", e)
        }
        super.onDestroy()
    }
}
