package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionResult
import coil.Coil
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.SettingsRepository
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.RadioDjStrings
import com.example.ui.screens.VideoThumbnailLoader
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.WeakHashMap

@UnstableApi
private class JnmfListenerWrapper(
    private val delegate: Player.Listener,
    private val getAugmentedCommands: () -> Player.Commands,
    private val forwardingPlayer: Player
) : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) {
        delegate.onEvents(forwardingPlayer, events)
    }

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        delegate.onAvailableCommandsChanged(getAugmentedCommands())
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        delegate.onTimelineChanged(timeline, reason)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        delegate.onMediaItemTransition(mediaItem, reason)
    }

    override fun onTracksChanged(tracks: Tracks) {
        delegate.onTracksChanged(tracks)
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        delegate.onMediaMetadataChanged(mediaMetadata)
    }

    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
        delegate.onPlaylistMetadataChanged(mediaMetadata)
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        delegate.onIsLoadingChanged(isLoading)
    }

    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
        delegate.onTrackSelectionParametersChanged(parameters)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        delegate.onPlaybackStateChanged(playbackState)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        delegate.onPlayWhenReadyChanged(playWhenReady, reason)
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        delegate.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        delegate.onIsPlayingChanged(isPlaying)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        delegate.onRepeatModeChanged(repeatMode)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        delegate.onShuffleModeEnabledChanged(shuffleModeEnabled)
    }

    override fun onPlayerError(error: PlaybackException) {
        delegate.onPlayerError(error)
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        delegate.onPlayerErrorChanged(error)
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        delegate.onPositionDiscontinuity(oldPosition, newPosition, reason)
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        delegate.onPlaybackParametersChanged(playbackParameters)
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        delegate.onSeekBackIncrementChanged(seekBackIncrementMs)
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        delegate.onSeekForwardIncrementChanged(seekForwardIncrementMs)
    }

    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) {
        delegate.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        delegate.onAudioSessionIdChanged(audioSessionId)
    }

    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
        delegate.onAudioAttributesChanged(audioAttributes)
    }

    override fun onVolumeChanged(volume: Float) {
        delegate.onVolumeChanged(volume)
    }

    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
        delegate.onSkipSilenceEnabledChanged(skipSilenceEnabled)
    }

    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        delegate.onDeviceInfoChanged(deviceInfo)
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        delegate.onDeviceVolumeChanged(volume, muted)
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        delegate.onVideoSizeChanged(videoSize)
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {
        delegate.onSurfaceSizeChanged(width, height)
    }

    override fun onRenderedFirstFrame() {
        delegate.onRenderedFirstFrame()
    }

    override fun onCues(cueGroup: CueGroup) {
        delegate.onCues(cueGroup)
    }

    override fun onMetadata(metadata: Metadata) {
        delegate.onMetadata(metadata)
    }
}

@UnstableApi
private class JnmfMusicForwardingPlayer(
    private val exo: ExoPlayer
) : ForwardingPlayer(exo) {

    private val listenerMap = Collections.synchronizedMap(
        WeakHashMap<Player.Listener, Player.Listener>()
    )

    val augmentedCommands: Player.Commands
        get() = exo.availableCommands.buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_STOP)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .build()

    override fun getAvailableCommands(): Player.Commands = augmentedCommands

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_STOP -> true
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasNextMediaItem(): Boolean = true
    override fun hasPreviousMediaItem(): Boolean = true

    override fun play() {
        MusicController.resume()
    }

    override fun pause() {
        MusicController.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            MusicController.resume()
        } else {
            MusicController.pause()
        }
    }

    override fun stop() {
        MusicController.pause()
    }

    override fun seekTo(positionMs: Long) {
        MusicController.seekTo(positionMs)
    }

    override fun seekToNext() {
        MusicController.next()
    }

    override fun seekToNextMediaItem() {
        MusicController.next()
    }

    override fun seekToPrevious() {
        MusicController.previous()
    }

    override fun seekToPreviousMediaItem() {
        MusicController.previous()
    }

    override fun addListener(listener: Player.Listener) {
        val wrapped = JnmfListenerWrapper(listener, { augmentedCommands }, this)
        listenerMap[listener] = wrapped
        super.addListener(wrapped)
    }

    override fun removeListener(listener: Player.Listener) {
        val wrapped = listenerMap.remove(listener) ?: listener
        super.removeListener(wrapped)
    }
}

@UnstableApi
private class JnmfVideoForwardingPlayer(
    private val exo: ExoPlayer
) : ForwardingPlayer(exo) {

    private val listenerMap = Collections.synchronizedMap(
        WeakHashMap<Player.Listener, Player.Listener>()
    )

    val augmentedCommands: Player.Commands
        get() = exo.availableCommands.buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_STOP)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .build()

    override fun getAvailableCommands(): Player.Commands = augmentedCommands

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_STOP -> true
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasNextMediaItem(): Boolean = true
    override fun hasPreviousMediaItem(): Boolean = true

    override fun getMediaMetadata(): MediaMetadata {
        val videoState = VideoPlaybackController.state.value
        val video = videoState.currentVideo ?: return super.getMediaMetadata()
        val lang = VideoPlaybackController.language.value
        val modeLabel = JnmfStrings.videoNotifModeSubtitle(videoState.playbackMode, lang)
        return MediaMetadata.Builder()
            .setTitle(video.title)
            .setDisplayTitle(video.title)
            .setArtist(modeLabel)
            .setSubtitle(modeLabel)
            .setAlbumTitle(JnmfStrings.videoNotifBadge(lang))
            .setIsPlayable(true)
            .build()
    }

    override fun play() {
        VideoPlaybackController.resume()
    }

    override fun pause() {
        VideoPlaybackController.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            VideoPlaybackController.resume()
        } else {
            VideoPlaybackController.pause()
        }
    }

    override fun stop() {
        VideoPlaybackController.stop()
    }

    override fun seekTo(positionMs: Long) {
        VideoPlaybackController.seekTo(positionMs)
    }

    override fun seekToNext() {
        VideoPlaybackController.next()
    }

    override fun seekToNextMediaItem() {
        VideoPlaybackController.next()
    }

    override fun seekToPrevious() {
        VideoPlaybackController.previous()
    }

    override fun seekToPreviousMediaItem() {
        VideoPlaybackController.previous()
    }

    override fun addListener(listener: Player.Listener) {
        val wrapped = JnmfListenerWrapper(listener, { augmentedCommands }, this)
        listenerMap[listener] = wrapped
        super.addListener(wrapped)
    }

    override fun removeListener(listener: Player.Listener) {
        val wrapped = listenerMap.remove(listener) ?: listener
        super.removeListener(wrapped)
    }
}

@UnstableApi
private class JnmfRadioForwardingPlayer(
    private val exo: ExoPlayer
) : ForwardingPlayer(exo) {

    private val listenerMap = Collections.synchronizedMap(
        WeakHashMap<Player.Listener, Player.Listener>()
    )

    val augmentedCommands: Player.Commands
        get() = exo.availableCommands.buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_STOP)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .build()

    override fun getAvailableCommands(): Player.Commands = augmentedCommands

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_STOP -> true
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasNextMediaItem(): Boolean = RadioPlaybackController.state.value.upcomingItem != null
    override fun hasPreviousMediaItem(): Boolean = true

    override fun play() {
        RadioPlaybackController.resume()
    }

    override fun pause() {
        RadioPlaybackController.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            RadioPlaybackController.resume()
        } else {
            RadioPlaybackController.pause()
        }
    }

    override fun stop() {
        RadioPlaybackController.stop()
    }

    override fun seekTo(positionMs: Long) {
        RadioPlaybackController.seekTo(positionMs)
    }

    override fun seekToNext() {
        RadioPlaybackController.next()
    }

    override fun seekToNextMediaItem() {
        RadioPlaybackController.next()
    }

    override fun seekToPrevious() {
        RadioPlaybackController.previous()
    }

    override fun seekToPreviousMediaItem() {
        RadioPlaybackController.previous()
    }

    override fun addListener(listener: Player.Listener) {
        val wrapped = JnmfListenerWrapper(listener, { augmentedCommands }, this)
        listenerMap[listener] = wrapped
        super.addListener(wrapped)
    }

    override fun removeListener(listener: Player.Listener) {
        val wrapped = listenerMap.remove(listener) ?: listener
        super.removeListener(wrapped)
    }
}

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    internal val currentMediaSession: MediaSession?
        get() = mediaSession
    private var musicPlayer: ExoPlayer? = null
    private var secondaryMusicPlayer: ExoPlayer? = null
    private var radioPlayer: ExoPlayer? = null
    private var videoPlayer: ExoPlayer? = null
    private var musicForwardingPlayer: Player? = null
    private var secondaryMusicForwardingPlayer: Player? = null
    private var radioForwardingPlayer: Player? = null
    private var videoForwardingPlayer: Player? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentArtworkSongId: Long? = null
    private var currentArtworkBitmap: Bitmap? = null
    private var currentVideoArtworkId: Long? = null
    private var currentVideoArtworkBitmap: Bitmap? = null
    private var musicArtworkJob: Job? = null
    private var videoArtworkJob: Job? = null
    private var defaultLogoBitmap: Bitmap? = null
    private var noisyReceiverRegistered = false

    private data class NotificationSnapshot(
        val sessionOwner: ActiveMediaSessionOwner,
        val mediaId: Long,
        val title: String,
        val subtitle: String,
        val subText: String,
        val isPlaying: Boolean,
        val language: AppLanguage,
        val artworkIdentity: Int
    )

    private var lastPostedNotificationSnapshot: NotificationSnapshot? = null
    internal var notificationPostCountForTesting: Int = 0
        private set

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                VideoPlaybackController.handleAudioBecomingNoisy()
                RadioPlaybackController.handleAudioBecomingNoisy()
                MusicController.handleAudioBecomingNoisy()
                updateNotification()
            }
        }
    }

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
        const val ACTION_STOP = "com.example.playback.ACTION_STOP"

        const val EXTRA_STATUS_TEXT = "extra_status_text"
        const val EXTRA_OPEN_VIDEO_PLAYER = "extra_open_video_player"
        const val EXTRA_OPEN_RADIO_SCHEDULER = "extra_open_radio_scheduler"

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
        MusicController.bindAppContext(applicationContext)
        RadioPlaybackController.bindAppContext(applicationContext)
        val initialLang = SettingsRepository.getSavedLanguage(this)
        val initialVideoMode = SettingsRepository.getSavedVideoPlaybackMode(this)
        val initialQueueMode = SettingsRepository.getSavedVideoQueueMode(this)
        MusicController.setLanguage(initialLang)
        VideoPlaybackController.setLanguage(initialLang)
        RadioPlaybackController.setLanguage(initialLang)
        if (MusicController.state.value.currentSong == null) {
            val savedModes = SettingsRepository.getSavedMusicModes(this)
            MusicController.restorePlaybackModes(
                isShuffle = savedModes.isShuffle,
                repeatState = savedModes.repeatState,
                isReverseQueue = savedModes.isReverseQueue
            )
            val savedEq = SettingsRepository.getSavedEqSettings(this)
            MusicController.equalizerManager.restoreSavedSettings(
                enabled = savedEq.enabled,
                bass = savedEq.bass,
                virtualizer = savedEq.virtualizer,
                preset = savedEq.preset,
                clarity = savedEq.clarity,
                customBandsCsv = savedEq.customBandsCsv
            )
            val savedDj = SettingsRepository.getSavedDjModeSettings(this)
            MusicController.setDjModeSettings(
                enabled = savedDj.enabled,
                transitionSeconds = savedDj.transitionSeconds,
                persist = false
            )
        }
        if (VideoPlaybackController.state.value.currentVideo == null) {
            VideoPlaybackController.setPlaybackMode(initialVideoMode)
            VideoPlaybackController.setQueueMode(initialQueueMode)
        }
        createNotificationChannel(initialLang)

        try {
            defaultLogoBitmap = android.graphics.BitmapFactory.decodeResource(
                resources,
                R.drawable.ic_jnmf_official_logo
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not load default logo bitmap: ${e.message}")
        }

        try {
            ContextCompat.registerReceiver(
                this,
                becomingNoisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            noisyReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not register becomingNoisyReceiver: ${e.message}")
        }

        try {
            val musicExo = MusicController.buildMusicExoPlayer(this)

            val videoAudioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val videoExo = ExoPlayer.Builder(this)
                .setAudioAttributes(videoAudioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()
                .apply {
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }

            this.musicPlayer = musicExo
            this.videoPlayer = videoExo

            val musicForwarder = JnmfMusicForwardingPlayer(musicExo)
            val videoForwarder = JnmfVideoForwardingPlayer(videoExo)
            this.musicForwardingPlayer = musicForwarder
            this.videoForwardingPlayer = videoForwarder

            MusicController.attachPlayer(
                player = musicExo,
                secondaryPlayerFactory = {
                    secondaryMusicPlayer ?: MusicController.buildMusicExoPlayer(this@PlaybackService).also { secExo ->
                        secondaryMusicPlayer = secExo
                        secondaryMusicForwardingPlayer = JnmfMusicForwardingPlayer(secExo)
                    }
                },
                secondaryPlayerDisposer = { playerToDispose ->
                    if (playerToDispose === secondaryMusicPlayer) {
                        secondaryMusicForwardingPlayer = null
                        secondaryMusicPlayer = null
                        try {
                            playerToDispose.release()
                        } catch (_: Exception) {
                        }
                    }
                }
            )
            VideoPlaybackController.attachPlayer(videoExo)
            RadioPlaybackController.attachPlayerProvider(
                context = this,
                factory = {
                    radioPlayer ?: MusicController.buildMusicExoPlayer(this@PlaybackService).also { radExo ->
                        radioPlayer = radExo
                        radioForwardingPlayer = JnmfRadioForwardingPlayer(radExo)
                    }
                },
                disposer = { playerToDispose ->
                    if (playerToDispose === radioPlayer) {
                        radioForwardingPlayer = null
                        radioPlayer = null
                        try {
                            playerToDispose.release()
                        } catch (_: Exception) {
                        }
                    }
                }
            )

            val initialForwarder = when {
                VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.VIDEO &&
                    VideoPlaybackController.state.value.currentVideo != null -> videoForwarder
                VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.RADIO &&
                    radioForwardingPlayer != null -> radioForwardingPlayer!!
                else -> musicForwarder
            }

            val sessionActivityIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val sessionCallback = object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val connectionResult = super.onConnect(session, controller)
                    val availablePlayerCommands = connectionResult.availablePlayerCommands
                        .buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_STOP)
                        .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                        .build()

                    return MediaSession.ConnectionResult.accept(
                        connectionResult.availableSessionCommands,
                        availablePlayerCommands
                    )
                }

                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    when (playerCommand) {
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_STOP -> return SessionResult.RESULT_SUCCESS
                    }
                    return super.onPlayerCommandRequest(session, controller, playerCommand)
                }
            }

            mediaSession = MediaSession.Builder(this, initialForwarder)
                .setSessionActivity(sessionActivityIntent)
                .setCallback(sessionCallback)
                .build()

            val initialNotification = buildForegroundNotificationForCurrentSession()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }

            setMediaNotificationProvider(object : MediaNotification.Provider {
                override fun createNotification(
                    mediaSession: MediaSession,
                    customLayout: ImmutableList<CommandButton>,
                    actionFactory: MediaNotification.ActionFactory,
                    onNotificationChangedCallback: MediaNotification.Provider.Callback
                ): MediaNotification {
                    return MediaNotification(
                        NOTIFICATION_ID,
                        buildForegroundNotificationForCurrentSession()
                    )
                }

                override fun handleCustomCommand(
                    session: MediaSession,
                    action: String,
                    extras: Bundle
                ): Boolean = false
            })

            // Dynamically switch MediaSession player across Music/Video/Radio ownership and Dual-Deck DJ crossfades
            serviceScope.launch {
                combine(
                    VideoPlaybackController.activeOwner,
                    MusicController.activeExoPlayerFlow,
                    RadioPlaybackController.activeExoPlayerFlow
                ) { owner, activeMusicExo, activeRadioExo ->
                    Triple(owner, activeMusicExo, activeRadioExo)
                }.collect { (owner, activeMusicExo, activeRadioExo) ->
                    val targetPlayer = when {
                        owner == ActiveMediaSessionOwner.VIDEO -> videoForwardingPlayer
                        owner == ActiveMediaSessionOwner.RADIO && activeRadioExo != null ->
                            radioForwardingPlayer ?: musicForwardingPlayer
                        activeMusicExo != null && activeMusicExo === secondaryMusicPlayer ->
                            secondaryMusicForwardingPlayer ?: musicForwardingPlayer
                        else -> musicForwardingPlayer
                    }
                    if (targetPlayer != null && mediaSession?.player != targetPlayer) {
                        mediaSession?.player = targetPlayer
                    }
                    updateSessionActivityIntent(owner)
                    updateNotification()
                }
            }

            // Observe MusicController state
            serviceScope.launch {
                MusicController.state.collect { state ->
                    updateArtworkForSong(state.currentSong)
                    if (VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.MUSIC) {
                        updateNotification()
                    }
                }
            }

            // Observe VideoPlaybackController state
            serviceScope.launch {
                VideoPlaybackController.state.collect { videoState ->
                    updateArtworkForVideo(videoState.currentVideo)
                    if (VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.VIDEO) {
                        updateNotification()
                    }
                }
            }

            // Observe RadioPlaybackController state
            serviceScope.launch {
                RadioPlaybackController.state.collect {
                    if (VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.RADIO) {
                        updateNotification()
                    }
                }
            }

            // Observe language changes to update notification channel & labels in place
            serviceScope.launch {
                MusicController.language.collect { lang ->
                    VideoPlaybackController.setLanguage(lang)
                    RadioPlaybackController.setLanguage(lang)
                    createNotificationChannel(lang)
                    updateNotification()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer or MediaSession: ${e.message}", e)
        }
    }

    private fun updateSessionActivityIntent(owner: ActiveMediaSessionOwner) {
        val openVideo = (owner == ActiveMediaSessionOwner.VIDEO && VideoPlaybackController.state.value.currentVideo != null)
        val openRadio = (owner == ActiveMediaSessionOwner.RADIO && RadioPlaybackController.state.value.currentItem != null)
        val reqCode = when {
            openVideo -> 10
            openRadio -> 20
            else -> 0
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            reqCode,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_VIDEO_PLAYER, openVideo)
                putExtra(EXTRA_OPEN_RADIO_SCHEDULER, openRadio)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession?.setSessionActivity(pendingIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val activeOwner = VideoPlaybackController.activeOwner.value
        val isVideoOwner = activeOwner == ActiveMediaSessionOwner.VIDEO &&
            VideoPlaybackController.state.value.currentVideo != null
        val isRadioOwner = activeOwner == ActiveMediaSessionOwner.RADIO &&
            RadioPlaybackController.state.value.currentItem != null

        when (intent?.action) {
            ACTION_PREVIOUS -> when {
                isVideoOwner -> VideoPlaybackController.previous()
                isRadioOwner -> RadioPlaybackController.previous()
                else -> MusicController.previous()
            }
            ACTION_PLAY_PAUSE -> when {
                isVideoOwner -> VideoPlaybackController.togglePlayPause()
                isRadioOwner -> RadioPlaybackController.togglePlayPause()
                else -> MusicController.togglePlayPause()
            }
            ACTION_PLAY -> when {
                isVideoOwner -> VideoPlaybackController.resume()
                isRadioOwner -> RadioPlaybackController.resume()
                else -> MusicController.resume()
            }
            ACTION_PAUSE -> when {
                isVideoOwner -> VideoPlaybackController.pause()
                isRadioOwner -> RadioPlaybackController.pause()
                else -> MusicController.pause()
            }
            ACTION_NEXT -> when {
                isVideoOwner -> VideoPlaybackController.next()
                isRadioOwner -> RadioPlaybackController.next()
                else -> MusicController.next()
            }
            ACTION_STOP -> when {
                isVideoOwner -> VideoPlaybackController.stop()
                isRadioOwner -> RadioPlaybackController.stop()
                else -> MusicController.pause()
            }
        }

        updateNotification(force = intent?.action != null)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun resolveCurrentNotificationSnapshot(): NotificationSnapshot {
        val lang = MusicController.language.value
        val activeOwner = VideoPlaybackController.activeOwner.value
        val videoState = VideoPlaybackController.state.value
        val radioState = RadioPlaybackController.state.value
        val isVideoActive = activeOwner == ActiveMediaSessionOwner.VIDEO && videoState.currentVideo != null
        val isRadioActive = activeOwner == ActiveMediaSessionOwner.RADIO && radioState.currentItem != null

        return when {
            isVideoActive -> {
                val video = videoState.currentVideo
                val art = currentVideoArtworkBitmap ?: defaultLogoBitmap
                NotificationSnapshot(
                    sessionOwner = ActiveMediaSessionOwner.VIDEO,
                    mediaId = video?.id ?: -1L,
                    title = video?.title ?: JnmfStrings.creditAppName(),
                    subtitle = JnmfStrings.videoNotifModeSubtitle(videoState.playbackMode, lang),
                    subText = JnmfStrings.videoNotifBadge(lang),
                    isPlaying = videoState.isPlaying,
                    language = lang,
                    artworkIdentity = System.identityHashCode(art)
                )
            }
            isRadioActive -> {
                val item = radioState.currentItem
                val typeLabel = item?.let { RadioDjStrings.itemTypeLabel(it.itemType, lang) }.orEmpty()
                val subtitle = buildString {
                    append(typeLabel)
                    if (radioState.activeScheduleName.isNotBlank()) {
                        append(" • ")
                        append(radioState.activeScheduleName)
                    }
                }
                NotificationSnapshot(
                    sessionOwner = ActiveMediaSessionOwner.RADIO,
                    mediaId = item?.id ?: -1L,
                    title = item?.title ?: RadioDjStrings.notifBadge(lang),
                    subtitle = subtitle,
                    subText = RadioDjStrings.notifBadge(lang),
                    isPlaying = radioState.isPlaying,
                    language = lang,
                    artworkIdentity = System.identityHashCode(defaultLogoBitmap)
                )
            }
            else -> {
                val state = MusicController.state.value
                val song = state.currentSong
                val activeMusicExo = MusicController.activeExoPlayerFlow.value ?: musicPlayer
                val isPlaying = activeMusicExo?.let { player ->
                    player.isPlaying || (
                        player.playWhenReady &&
                            player.playbackState == Player.STATE_BUFFERING &&
                            song != null
                        )
                } ?: state.isPlaying
                val art = currentArtworkBitmap ?: defaultLogoBitmap
                NotificationSnapshot(
                    sessionOwner = ActiveMediaSessionOwner.MUSIC,
                    mediaId = song?.id ?: -1L,
                    title = song?.title ?: JnmfStrings.creditAppName(),
                    subtitle = song?.artist ?: JnmfStrings.notifReadyForPlayback(lang),
                    subText = song?.let { JnmfStrings.remixCategoryName(it.remixCategory, lang) } ?: "JNMF",
                    isPlaying = isPlaying,
                    language = lang,
                    artworkIdentity = System.identityHashCode(art)
                )
            }
        }
    }

    private fun updateNotification(force: Boolean = false) {
        try {
            val snapshot = resolveCurrentNotificationSnapshot()
            if (!force && snapshot == lastPostedNotificationSnapshot) {
                return
            }
            lastPostedNotificationSnapshot = snapshot
            val notification = buildForegroundNotificationForCurrentSession()
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
            notificationPostCountForTesting++
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing foreground notification: ${e.message}", e)
        }
    }

    private fun updateArtworkForSong(song: Song?) {
        if (song == null) {
            musicArtworkJob?.cancel()
            musicArtworkJob = null
            currentArtworkSongId = null
            currentArtworkBitmap = null
            return
        }
        if (song.id == currentArtworkSongId) return
        musicArtworkJob?.cancel()
        currentArtworkSongId = song.id
        currentArtworkBitmap = null

        val artUri = song.albumArtUri
        if (artUri != null) {
            musicArtworkJob = serviceScope.launch(Dispatchers.IO) {
                try {
                    val loader = Coil.imageLoader(this@PlaybackService)
                    val request = ImageRequest.Builder(this@PlaybackService)
                        .data(artUri)
                        .size(300, 300)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        val loadedBitmap = drawable.bitmap
                        withContext(Dispatchers.Main) {
                            if (currentArtworkSongId == song.id &&
                                MusicController.state.value.currentSong?.id == song.id
                            ) {
                                currentArtworkBitmap = loadedBitmap
                                updateNotification()
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load artwork bitmap for notification: ${e.message}")
                }
            }
        }
    }

    /**
     * Loads a single static thumbnail frame for the video notification (never a moving video image).
     */
    private fun updateArtworkForVideo(video: VideoItem?) {
        if (video == null) {
            videoArtworkJob?.cancel()
            videoArtworkJob = null
            currentVideoArtworkId = null
            currentVideoArtworkBitmap = null
            return
        }
        if (video.id == currentVideoArtworkId) return
        videoArtworkJob?.cancel()
        currentVideoArtworkId = video.id
        currentVideoArtworkBitmap = null

        // Reuse in-memory cached video thumbnail immediately when available
        val cachedThumb = VideoThumbnailLoader.getCached(video.id, video.contentUri)
        if (cachedThumb != null && !cachedThumb.isRecycled) {
            currentVideoArtworkBitmap = cachedThumb
            return
        }

        videoArtworkJob = serviceScope.launch(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this@PlaybackService, video.contentUri)
                val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        300,
                        300
                    )
                } else {
                    retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                if (frame != null) {
                    val scaled = if (frame.width > 300 || frame.height > 300) {
                        val scale = minOf(300f / frame.width.toFloat(), 300f / frame.height.toFloat())
                        val tw = (frame.width * scale).toInt().coerceAtLeast(1)
                        val th = (frame.height * scale).toInt().coerceAtLeast(1)
                        val created = Bitmap.createScaledBitmap(frame, tw, th, true)
                        if (created !== frame) {
                            frame.recycle()
                        }
                        created
                    } else {
                        frame
                    }
                    withContext(Dispatchers.Main) {
                        if (currentVideoArtworkId == video.id &&
                            VideoPlaybackController.state.value.currentVideo?.id == video.id
                        ) {
                            currentVideoArtworkBitmap = scaled
                            updateNotification()
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Fallback to default JNMF static logo when local frame extraction is unavailable
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }
    }

    internal fun buildForegroundNotificationForCurrentSession(): Notification {
        val activeOwner = VideoPlaybackController.activeOwner.value
        val isVideoActive = activeOwner == ActiveMediaSessionOwner.VIDEO &&
            VideoPlaybackController.state.value.currentVideo != null
        val isRadioActive = activeOwner == ActiveMediaSessionOwner.RADIO &&
            RadioPlaybackController.state.value.currentItem != null

        return when {
            isVideoActive -> buildVideoForegroundNotification(
                videoState = VideoPlaybackController.state.value,
                artworkBitmap = currentVideoArtworkBitmap
            )
            isRadioActive -> buildRadioForegroundNotification(
                radioState = RadioPlaybackController.state.value
            )
            else -> buildMusicForegroundNotification(
                state = MusicController.state.value,
                artworkBitmap = currentArtworkBitmap
            )
        }
    }

    private fun createServiceActionPendingIntent(requestCode: Int, action: String): PendingIntent {
        val piFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val intent = Intent(this, PlaybackService::class.java).apply { this.action = action }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, requestCode, intent, piFlags)
        } else {
            PendingIntent.getService(this, requestCode, intent, piFlags)
        }
    }

    private fun buildVideoForegroundNotification(
        videoState: VideoPlaybackState,
        artworkBitmap: Bitmap? = null
    ): Notification {
        val video = videoState.currentVideo
        val isPlaying = videoState.isPlaying
        val lang = MusicController.language.value

        val launchIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_VIDEO_PLAYER, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = createServiceActionPendingIntent(11, ACTION_PREVIOUS)
        val playPauseIntent = createServiceActionPendingIntent(
            if (isPlaying) 15 else 12,
            if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        )
        val nextIntent = createServiceActionPendingIntent(13, ACTION_NEXT)
        val stopIntent = createServiceActionPendingIntent(14, ACTION_STOP)

        val title = video?.title ?: JnmfStrings.creditAppName()
        val modeSubtitle = JnmfStrings.videoNotifModeSubtitle(videoState.playbackMode, lang)
        val badgeText = JnmfStrings.videoNotifBadge(lang)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(modeSubtitle)
            .setSubText(badgeText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(launchIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)

        val largeIcon = artworkBitmap ?: defaultLogoBitmap
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        // Actions: [0] Previous, [1] Play/Pause, [2] Next, [3] Stop
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_previous,
                JnmfStrings.previous(lang),
                prevIntent
            ).build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (isPlaying) JnmfStrings.pause(lang) else JnmfStrings.play(lang),
                playPauseIntent
            ).build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_next,
                JnmfStrings.next(lang),
                nextIntent
            ).build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_stop,
                JnmfStrings.videoStop(lang),
                stopIntent
            ).build()
        )

        mediaSession?.let { session ->
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        return builder.build()
    }

    private fun buildMusicForegroundNotification(
        state: PlaybackState,
        artworkBitmap: Bitmap? = null
    ): Notification {
        val song = state.currentSong
        val activeMusicExo = MusicController.activeExoPlayerFlow.value ?: musicPlayer
        val isPlaying = activeMusicExo?.let { player ->
            player.isPlaying || (
                player.playWhenReady &&
                    player.playbackState == Player.STATE_BUFFERING &&
                    song != null
                )
        } ?: state.isPlaying

        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_VIDEO_PLAYER, false)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = createServiceActionPendingIntent(1, ACTION_PREVIOUS)
        val playPauseIntent = createServiceActionPendingIntent(
            if (isPlaying) 4 else 2,
            if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        )
        val nextIntent = createServiceActionPendingIntent(3, ACTION_NEXT)

        val lang = MusicController.language.value
        val title = song?.title ?: JnmfStrings.creditAppName()
        val artist = song?.artist ?: JnmfStrings.notifReadyForPlayback(lang)
        val subText = song?.let { JnmfStrings.remixCategoryName(it.remixCategory, lang) } ?: "JNMF"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText(subText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(launchIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)

        val largeIcon = artworkBitmap ?: defaultLogoBitmap
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        // Actions: [0] Previous, [1] Play/Pause, [2] Next
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_previous,
                JnmfStrings.previous(lang),
                prevIntent
            ).build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (isPlaying) JnmfStrings.pause(lang) else JnmfStrings.play(lang),
                playPauseIntent
            ).build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_next,
                JnmfStrings.next(lang),
                nextIntent
            ).build()
        )

        mediaSession?.let { session ->
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        return builder.build()
    }

    private fun buildRadioForegroundNotification(
        radioState: RadioPlaybackState
    ): Notification {
        val item = radioState.currentItem
        val isPlaying = radioState.isPlaying
        val lang = MusicController.language.value

        val launchIntent = PendingIntent.getActivity(
            this,
            20,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_VIDEO_PLAYER, false)
                putExtra(EXTRA_OPEN_RADIO_SCHEDULER, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = createServiceActionPendingIntent(21, ACTION_PREVIOUS)
        val playPauseIntent = createServiceActionPendingIntent(
            if (isPlaying) 25 else 22,
            if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        )
        val nextIntent = createServiceActionPendingIntent(23, ACTION_NEXT)
        val stopIntent = createServiceActionPendingIntent(24, ACTION_STOP)

        val title = item?.title ?: RadioDjStrings.notifBadge(lang)
        val typeText = item?.let { RadioDjStrings.itemTypeLabel(it.itemType, lang) }.orEmpty()
        val subtitle = buildString {
            append(typeText)
            if (radioState.activeScheduleName.isNotBlank()) {
                append(" • ")
                append(radioState.activeScheduleName)
            }
        }
        val badgeText = RadioDjStrings.notifBadge(lang)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText(badgeText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(launchIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)

        defaultLogoBitmap?.let { builder.setLargeIcon(it) }

        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_previous,
                JnmfStrings.previous(lang),
                prevIntent
            ).build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (isPlaying) JnmfStrings.pause(lang) else JnmfStrings.play(lang),
                playPauseIntent
            ).build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_next,
                JnmfStrings.next(lang),
                nextIntent
            ).build()
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_stop,
                RadioDjStrings.stopRadio(lang),
                stopIntent
            ).build()
        )

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

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
        updateNotification(force = startInForegroundRequired)
    }

    private fun createNotificationChannel(lang: AppLanguage = MusicController.language.value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                JnmfStrings.notifChannelName(lang),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = JnmfStrings.notifChannelDesc(lang)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        MusicController.flushPlaybackSnapshotNow()
        val isMusicPlaying = MusicController.state.value.isPlaying
        val isVideoPlaying = VideoPlaybackController.state.value.isPlaying
        val isRadioPlaying = RadioPlaybackController.state.value.isPlaying
        if (!isMusicPlaying && !isVideoPlaying && !isRadioPlaying) {
            try {
                stopSelf()
            } catch (_: Exception) {
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        MusicController.flushPlaybackSnapshotNow()
        try {
            if (noisyReceiverRegistered) {
                unregisterReceiver(becomingNoisyReceiver)
                noisyReceiverRegistered = false
            }
        } catch (_: Exception) {}

        try {
            serviceScope.cancel()
            MusicController.equalizerManager.releaseEffects()
            MusicController.detachPlayer()
            RadioPlaybackController.detachPlayerProvider()
            VideoPlaybackController.detachPlayer()
            mediaSession?.run {
                release()
                mediaSession = null
            }
            musicPlayer?.release()
            secondaryMusicPlayer?.release()
            radioPlayer?.release()
            videoPlayer?.release()
            musicPlayer = null
            secondaryMusicPlayer = null
            radioPlayer = null
            videoPlayer = null
            musicForwardingPlayer = null
            secondaryMusicForwardingPlayer = null
            radioForwardingPlayer = null
            videoForwardingPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during PlaybackService onDestroy: ${e.message}", e)
        }
        super.onDestroy()
    }
}
