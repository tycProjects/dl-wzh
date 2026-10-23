package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.playback.MusicController
import com.example.ui.components.JnmfGlobeLogo
import com.example.ui.components.JnmfTaskProgressBar
import com.example.ui.components.TaskProgressState
import com.example.ui.i18n.JnmfStrings
import kotlinx.coroutines.delay

/**
 * Safely toggles Android system status and navigation bars visibility for immersive video playback.
 */
private fun setSystemBarsVisibility(activity: Activity?, visible: Boolean) {
    activity?.window?.let { window ->
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoScreen(
    videos: List<VideoItem>,
    videoScanState: TaskProgressState = TaskProgressState.IDLE,
    onRefresh: () -> Unit = {},
    onDismissProgress: () -> Unit = {},
    isFullscreen: Boolean = false,
    onFullscreenChanged: (Boolean) -> Unit = {},
    language: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var activeVideo by remember { mutableStateOf<VideoItem?>(null) }
    var localFullscreen by remember { mutableStateOf(false) }

    // Sync fullscreen state with parent Scaffold if needed
    val effectiveFullscreen = isFullscreen || localFullscreen

    fun setFullscreenMode(enabled: Boolean) {
        localFullscreen = enabled
        onFullscreenChanged(enabled)
        if (enabled) {
            setSystemBarsVisibility(activity, false)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            setSystemBarsVisibility(activity, true)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Video Player State
    var isVideoPlaying by remember { mutableStateOf(false) }
    var videoPositionMs by remember { mutableLongStateOf(0L) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(false) }
    var videoErrorMessage by remember { mutableStateOf<String?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

    // Hardware back press inside video view:
    // If in fullscreen, exits fullscreen mode.
    // If in embedded view, returns to video gallery.
    BackHandler(enabled = activeVideo != null) {
        if (effectiveFullscreen) {
            setFullscreenMode(false)
        } else {
            activeVideo = null
        }
    }

    // Manage ExoPlayer instance and audio focus
    var videoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(activeVideo) {
        val currentVid = activeVideo
        if (currentVid != null) {
            // Coordinate with music player: pause music playback to prevent competing audio output
            MusicController.pause()
            videoErrorMessage = null
            isBuffering = true

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val player = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isVideoPlaying = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_READY) {
                        videoDurationMs = player.duration.coerceAtLeast(0L)
                        videoErrorMessage = null
                    }
                }

                override fun onRenderedFirstFrame() {
                    isBuffering = false
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val friendly = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                            "Video file missing or deleted from storage."
                        PlaybackException.ERROR_CODE_DECODING_FAILED,
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                            "Unsupported video codec or corrupted file on this device."
                        else -> "Playback error: ${error.localizedMessage ?: "Unknown error"}"
                    }
                    videoErrorMessage = friendly
                    isVideoPlaying = false
                    isBuffering = false
                }
            })

            try {
                player.setMediaItem(MediaItem.fromUri(currentVid.contentUri))
                player.prepare()
                if (videoPositionMs > 0) {
                    player.seekTo(videoPositionMs)
                }
                player.playWhenReady = true
            } catch (e: Exception) {
                videoErrorMessage = "Failed to load video: ${e.localizedMessage ?: "Inaccessible"}"
                isBuffering = false
            }

            videoPlayer = player
        }

        onDispose {
            videoPlayer?.let { p ->
                videoPositionMs = p.currentPosition
                p.release()
            }
            videoPlayer = null
            setSystemBarsVisibility(activity, true)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onFullscreenChanged(false)
        }
    }

    // Auto-hide controls overlay after 4 seconds of playback with no touch
    LaunchedEffect(showControls, isVideoPlaying, isScrubbing) {
        if (showControls && isVideoPlaying && !isScrubbing) {
            delay(4000)
            showControls = false
        }
    }

    // Polling progress position safely
    LaunchedEffect(isVideoPlaying, activeVideo) {
        while (activeVideo != null) {
            videoPlayer?.let { p ->
                if (!isScrubbing && p.playbackState == Player.STATE_READY) {
                    videoPositionMs = p.currentPosition.coerceAtLeast(0L)
                    videoDurationMs = p.duration.coerceAtLeast(0L)
                }
            }
            delay(250)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (activeVideo != null) {
            val vid = activeVideo!!

            if (effectiveFullscreen) {
                // =========================================================================
                // FULLSCREEN MODE (Immersive 20.6:9 Redmi 10C display with notch protection)
                // =========================================================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControls = !showControls
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Video Picture Surface
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                keepScreenOn = true
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                this.player = videoPlayer
                            }
                        },
                        update = { playerView ->
                            if (playerView.player != videoPlayer) {
                                playerView.player = videoPlayer
                            }
                        },
                        onReset = { playerView -> playerView.player = null },
                        onRelease = { playerView -> playerView.player = null },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Buffering Indicator
                    if (isBuffering && videoErrorMessage == null) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp),
                            strokeWidth = 3.dp
                        )
                    }

                    // Fullscreen Controls HUD Overlay (uses safeDrawingPadding to avoid notch collision)
                    AnimatedVisibility(
                        visible = showControls || videoErrorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .safeDrawingPadding()
                        ) {
                            // Top Bar: Exit Fullscreen / Back button, Title, Duration
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { setFullscreenMode(false) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .testTag("exit_fullscreen_top_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = JnmfStrings.exitFullscreen(language),
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vid.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${vid.formattedDuration} • ${(vid.sizeBytes / (1024 * 1024))} MB",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = { setFullscreenMode(false) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .testTag("toggle_fullscreen_exit")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = JnmfStrings.exitFullscreen(language),
                                        tint = Color.White
                                    )
                                }
                            }

                            // Center Playback Buttons: -10s, Play/Pause, +10s
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val newPos = (videoPositionMs - 10000L).coerceAtLeast(0L)
                                        videoPlayer?.seekTo(newPos)
                                        videoPositionMs = newPos
                                    },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Seek -10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        videoPlayer?.let { p ->
                                            if (p.isPlaying) p.pause() else p.play()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("fullscreen_video_play_pause")
                                ) {
                                    Icon(
                                        imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val newPos = (videoPositionMs + 10000L).coerceAtMost(videoDurationMs)
                                        videoPlayer?.seekTo(newPos)
                                        videoPositionMs = newPos
                                    },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Seek +10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Bottom Scrubber Bar
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                val currentPos = if (isScrubbing) scrubPositionMs.toLong() else videoPositionMs
                                val dur = videoDurationMs.coerceAtLeast(1L)
                                val sliderVal = (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

                                Slider(
                                    value = sliderVal,
                                    onValueChange = { newVal ->
                                        isScrubbing = true
                                        scrubPositionMs = newVal * dur
                                    },
                                    onValueChangeFinished = {
                                        videoPlayer?.seekTo(scrubPositionMs.toLong())
                                        videoPositionMs = scrubPositionMs.toLong()
                                        isScrubbing = false
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("fullscreen_video_seek_slider")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatDuration(currentPos),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatDuration(videoDurationMs),
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Error Dialog in Fullscreen
                    if (videoErrorMessage != null) {
                        VideoErrorCard(
                            errorMessage = videoErrorMessage,
                            onRetry = {
                                videoErrorMessage = null
                                videoPlayer?.prepare()
                                videoPlayer?.play()
                            },
                            onClose = {
                                setFullscreenMode(false)
                                activeVideo = null
                            }
                        )
                    }
                }
            } else {
                // =========================================================================
                // EMBEDDED MODE (Portrait non-fullscreen: Top bar is fully visible above)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Video View Box with responsive aspect ratio (default 16:9 on Redmi 10C)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(videoAspectRatio.coerceIn(1.1f, 2.4f))
                            .background(Color.Black)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showControls = !showControls
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Video Surface View
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                    keepScreenOn = true
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    this.player = videoPlayer
                                }
                            },
                            update = { playerView ->
                                if (playerView.player != videoPlayer) {
                                    playerView.player = videoPlayer
                                }
                            },
                            onReset = { playerView -> playerView.player = null },
                            onRelease = { playerView -> playerView.player = null },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Buffering Spinner
                        if (isBuffering && videoErrorMessage == null) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 3.dp
                            )
                        }

                        // Embedded Controls HUD Overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showControls || videoErrorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                // Top row inside video container: Close button & Fullscreen button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { activeVideo = null },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .testTag("embedded_video_close")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = JnmfStrings.closePlayer(language),
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { setFullscreenMode(true) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .testTag("embedded_video_fullscreen_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = JnmfStrings.fullscreen(language),
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Center play controls: -10s, Play/Pause, +10s
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val newPos = (videoPositionMs - 10000L).coerceAtLeast(0L)
                                            videoPlayer?.seekTo(newPos)
                                            videoPositionMs = newPos
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Seek -10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            videoPlayer?.let { p ->
                                                if (p.isPlaying) p.pause() else p.play()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .testTag("video_play_pause")
                                    ) {
                                        Icon(
                                            imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val newPos = (videoPositionMs + 10000L).coerceAtMost(videoDurationMs)
                                            videoPlayer?.seekTo(newPos)
                                            videoPositionMs = newPos
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Seek +10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Bottom Scrubber in embedded mode
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    val currentPos = if (isScrubbing) scrubPositionMs.toLong() else videoPositionMs
                                    val dur = videoDurationMs.coerceAtLeast(1L)
                                    val sliderVal = (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

                                    Slider(
                                        value = sliderVal,
                                        onValueChange = { newVal ->
                                            isScrubbing = true
                                            scrubPositionMs = newVal * dur
                                        },
                                        onValueChangeFinished = {
                                            videoPlayer?.seekTo(scrubPositionMs.toLong())
                                            videoPositionMs = scrubPositionMs.toLong()
                                            isScrubbing = false
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color.DarkGray
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("video_seek_slider")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatDuration(currentPos),
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = formatDuration(videoDurationMs),
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Error in Embedded View
                        if (videoErrorMessage != null) {
                            VideoErrorCard(
                                errorMessage = videoErrorMessage,
                                onRetry = {
                                    videoErrorMessage = null
                                    videoPlayer?.prepare()
                                    videoPlayer?.play()
                                },
                                onClose = { activeVideo = null }
                            )
                        }
                    }

                    // Metadata Card below player
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = vid.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${vid.formattedDuration} • ${(vid.sizeBytes / (1024 * 1024))} MB",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { setFullscreenMode(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_enter_fullscreen")
                                ) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = JnmfStrings.fullscreen(language),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { activeVideo = null },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_close_player")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = JnmfStrings.closePlayer(language),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Other Videos Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = JnmfStrings.moreVideos(language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${videos.size} videos",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Other Videos List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(videos, key = { it.id }) { item ->
                            val isCurrent = (item.id == vid.id)
                            VideoListItemCard(
                                video = item,
                                isCurrent = isCurrent,
                                onClick = {
                                    if (!isCurrent) {
                                        videoPositionMs = 0L
                                        activeVideo = item
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // VIDEO GALLERY BROWSER (List of all local videos)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = JnmfStrings.videosTitle(language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${videos.size} videos found on device",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            if (!videoScanState.isRunning) {
                                onRefresh()
                            }
                        },
                        enabled = !videoScanState.isRunning,
                        modifier = Modifier.testTag("refresh_videos_button")
                    ) {
                        if (videoScanState.isRunning) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = JnmfStrings.refreshVideos(language),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Loading progress indicator
                JnmfTaskProgressBar(
                    state = videoScanState,
                    onDismiss = onDismissProgress
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (videos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            JnmfGlobeLogo(
                                size = 80.dp,
                                animated = videoScanState.isRunning,
                                showEqualizerWaves = false,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = JnmfStrings.noVideosFound(language),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRefresh,
                                enabled = !videoScanState.isRunning,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(JnmfStrings.refreshVideos(language), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(videos, key = { it.id }) { video ->
                            VideoListItemCard(
                                video = video,
                                isCurrent = false,
                                onClick = {
                                    videoPositionMs = 0L
                                    activeVideo = video
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoListItemCard(
    video: VideoItem,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(10.dp),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_item_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with Coil
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = video.contentUri,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${video.formattedDuration} • ${(video.sizeBytes / (1024 * 1024))} MB",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VideoErrorCard(
    errorMessage: String?,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Video Playback Error",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage ?: "Unable to play video on this device",
                fontSize = 12.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
