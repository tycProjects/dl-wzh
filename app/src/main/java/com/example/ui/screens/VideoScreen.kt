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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.SettingsRepository
import com.example.playback.PlaybackService
import com.example.playback.VideoPlaybackController
import com.example.playback.VideoPlaybackMode
import com.example.ui.components.JnmfGlobeLogo
import com.example.ui.components.JnmfTaskProgressBar
import com.example.ui.components.TaskProgressState
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    isPlayerExpanded: Boolean? = null,
    onPlayerExpandedChanged: (Boolean) -> Unit = {},
    onSelectPlaybackMode: ((VideoPlaybackMode) -> Unit)? = null,
    language: AppLanguage = LocalAppLanguage.current,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }

    val videoState by VideoPlaybackController.state.collectAsState()
    val activeVideo = videoState.currentVideo

    var localExpanded by remember { mutableStateOf(activeVideo != null) }
    val effectiveExpanded = (isPlayerExpanded ?: localExpanded) && activeVideo != null

    fun setExpanded(expanded: Boolean) {
        localExpanded = expanded
        onPlayerExpandedChanged(expanded)
    }

    var localFullscreen by remember { mutableStateOf(false) }
    val effectiveFullscreen = (isFullscreen || localFullscreen) && effectiveExpanded

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

    fun selectMode(mode: VideoPlaybackMode) {
        if (onSelectPlaybackMode != null) {
            onSelectPlaybackMode(mode)
        } else {
            VideoPlaybackController.setPlaybackMode(mode)
            scope.launch {
                settingsRepo.setVideoPlaybackMode(mode)
            }
        }
    }

    fun startOrSelectVideo(video: VideoItem) {
        PlaybackService.startService(context)
        if (videoState.currentVideo?.id == video.id) {
            // Returning to the already-active video restores position & mode without resetting
            setExpanded(true)
        } else {
            VideoPlaybackController.playVideo(video, videos, startPositionMs = 0L)
            setExpanded(true)
        }
    }

    fun stopVideoPlayback() {
        setFullscreenMode(false)
        setExpanded(false)
        VideoPlaybackController.stop()
    }

    // Keep controller queue synchronized with discovered videos
    LaunchedEffect(videos) {
        if (videos.isNotEmpty()) {
            VideoPlaybackController.updateQueue(videos)
        }
    }

    // Track Activity/Screen foreground lifecycle (ON_START / ON_STOP) for background & lock-screen transitions
    var isLifecycleStarted by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleStarted = true
                }
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    isLifecycleStarted = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isPlayerForegroundVisible = effectiveExpanded && isLifecycleStarted

    LaunchedEffect(isPlayerForegroundVisible) {
        VideoPlaybackController.setPlayerUiVisible(isPlayerForegroundVisible)
    }

    DisposableEffect(Unit) {
        onDispose {
            // Leaving VideoScreen (e.g. switching bottom tabs): stop rendering video frames
            // while allowing background audio playback to continue via PlaybackService!
            VideoPlaybackController.notifyVideoSurfaceAttached(false)
            VideoPlaybackController.setPlayerUiVisible(false)
            setSystemBarsVisibility(activity, true)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onFullscreenChanged(false)
        }
    }

    // Close expanded state automatically if playback was stopped externally (e.g. notification Stop button)
    LaunchedEffect(activeVideo) {
        if (activeVideo == null && (localExpanded || localFullscreen)) {
            setFullscreenMode(false)
            setExpanded(false)
        }
    }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }

    // Hardware Back press inside video view:
    // Fullscreen -> exit fullscreen; Embedded -> minimize to list while preserving background playback!
    BackHandler(enabled = effectiveExpanded) {
        if (effectiveFullscreen) {
            setFullscreenMode(false)
        } else {
            setExpanded(false)
        }
    }

    LaunchedEffect(showControls, videoState.isPlaying, isScrubbing, videoState.playbackMode) {
        if (showControls && videoState.isPlaying && !isScrubbing &&
            videoState.playbackMode == VideoPlaybackMode.NORMAL_VIDEO
        ) {
            delay(4500)
            showControls = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (effectiveExpanded) {
            val vid = activeVideo ?: return@Surface
            val modeLabel = JnmfStrings.videoModeLabel(videoState.playbackMode, language)

            if (effectiveFullscreen) {
                // =========================================================================
                // FULLSCREEN MODE
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
                    if (videoState.shouldRenderVideoFrames) {
                        val exo = VideoPlaybackController.exoPlayerInstance
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
                                    this.player = exo
                                    VideoPlaybackController.notifyVideoSurfaceAttached(exo != null)
                                }
                            },
                            update = { playerView ->
                                val currentExo = VideoPlaybackController.exoPlayerInstance
                                if (playerView.player != currentExo) {
                                    playerView.player = currentExo
                                }
                                VideoPlaybackController.notifyVideoSurfaceAttached(currentExo != null)
                            },
                            onReset = { playerView ->
                                playerView.player = null
                                VideoPlaybackController.notifyVideoSurfaceAttached(false)
                            },
                            onRelease = { playerView ->
                                playerView.player = null
                                VideoPlaybackController.notifyVideoSurfaceAttached(false)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("fullscreen_video_surface")
                        )
                    } else {
                        VideoAudioOnlyStage(
                            videoTitle = vid.title,
                            isPlaying = videoState.isPlaying,
                            language = language,
                            onSwitchToNormalVideo = { selectMode(VideoPlaybackMode.NORMAL_VIDEO) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (videoState.isBuffering && videoState.errorMessage == null && videoState.shouldRenderVideoFrames) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp),
                            strokeWidth = 3.dp
                        )
                    }

                    AnimatedVisibility(
                        visible = showControls || videoState.errorMessage != null ||
                            videoState.playbackMode == VideoPlaybackMode.AUDIO_ONLY,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (videoState.playbackMode == VideoPlaybackMode.AUDIO_ONLY) {
                                        Color.Transparent
                                    } else {
                                        Color.Black.copy(alpha = 0.55f)
                                    }
                                )
                                .safeDrawingPadding()
                        ) {
                            // Top Bar: Back, Title, Mode Chip, Stop, Exit Fullscreen
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
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
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
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("fullscreen_video_title")
                                    )
                                    Text(
                                        text = "${vid.formattedDuration} • $modeLabel",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                }

                                // 1-tap Mode Toggle Chip in Fullscreen
                                OutlinedButton(
                                    onClick = {
                                        val nextMode = if (videoState.playbackMode == VideoPlaybackMode.NORMAL_VIDEO) {
                                            VideoPlaybackMode.AUDIO_ONLY
                                        } else {
                                            VideoPlaybackMode.NORMAL_VIDEO
                                        }
                                        selectMode(nextMode)
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("fullscreen_mode_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (videoState.playbackMode == VideoPlaybackMode.AUDIO_ONLY) {
                                            Icons.Default.Headphones
                                        } else {
                                            Icons.Default.Videocam
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = modeLabel,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { stopVideoPlayback() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .testTag("fullscreen_video_stop")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = JnmfStrings.videoStop(language),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { setFullscreenMode(false) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .testTag("toggle_fullscreen_exit")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = JnmfStrings.exitFullscreen(language),
                                        tint = Color.White
                                    )
                                }
                            }

                            // Center Transport Controls in Fullscreen (only overlay center when Normal Video)
                            if (videoState.playbackMode == VideoPlaybackMode.NORMAL_VIDEO) {
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { VideoPlaybackController.previous() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .testTag("fullscreen_video_previous")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = JnmfStrings.previous(language),
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { VideoPlaybackController.seekBy(-10_000L) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .testTag("fullscreen_video_rewind_10s")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = JnmfStrings.seekBackward10s(language),
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { VideoPlaybackController.togglePlayPause() },
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .testTag("fullscreen_video_play_pause")
                                    ) {
                                        Icon(
                                            imageVector = if (videoState.isPlaying) {
                                                Icons.Default.Pause
                                            } else {
                                                Icons.Default.PlayArrow
                                            },
                                            contentDescription = if (videoState.isPlaying) {
                                                JnmfStrings.pause(language)
                                            } else {
                                                JnmfStrings.play(language)
                                            },
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { VideoPlaybackController.seekBy(10_000L) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .testTag("fullscreen_video_forward_10s")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = JnmfStrings.seekForward10s(language),
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { VideoPlaybackController.next() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .testTag("fullscreen_video_next")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = JnmfStrings.next(language),
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom Scrubber Bar
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                if (videoState.playbackMode == VideoPlaybackMode.AUDIO_ONLY) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { VideoPlaybackController.previous() },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("fullscreen_video_previous")
                                        ) {
                                            Icon(
                                                Icons.Default.SkipPrevious,
                                                contentDescription = JnmfStrings.previous(language),
                                                tint = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = { VideoPlaybackController.togglePlayPause() },
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .testTag("fullscreen_video_play_pause")
                                        ) {
                                            Icon(
                                                imageVector = if (videoState.isPlaying) {
                                                    Icons.Default.Pause
                                                } else {
                                                    Icons.Default.PlayArrow
                                                },
                                                contentDescription = if (videoState.isPlaying) {
                                                    JnmfStrings.pause(language)
                                                } else {
                                                    JnmfStrings.play(language)
                                                },
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = { VideoPlaybackController.next() },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("fullscreen_video_next")
                                        ) {
                                            Icon(
                                                Icons.Default.SkipNext,
                                                contentDescription = JnmfStrings.next(language),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }

                                val currentPos = if (isScrubbing) {
                                    scrubPositionMs.toLong()
                                } else {
                                    videoState.currentPositionMs
                                }
                                val dur = videoState.durationMs.coerceAtLeast(1L)
                                val sliderVal = (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

                                Slider(
                                    value = sliderVal,
                                    onValueChange = { newVal ->
                                        isScrubbing = true
                                        scrubPositionMs = newVal * dur
                                    },
                                    onValueChangeFinished = {
                                        VideoPlaybackController.seekTo(scrubPositionMs.toLong())
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
                                        text = formatVideoDuration(currentPos),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatVideoDuration(videoState.durationMs),
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (videoState.errorMessage != null) {
                        VideoErrorCard(
                            errorMessage = videoState.errorMessage,
                            language = language,
                            onRetry = { VideoPlaybackController.retry() },
                            onClose = { stopVideoPlayback() }
                        )
                    }
                }
            } else {
                // =========================================================================
                // EMBEDDED PORTRAIT MODE
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .testTag("embedded_video_player_container")
                ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(videoState.videoAspectRatio.coerceIn(1.25f, 2.2f))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (videoState.shouldRenderVideoFrames) {
                                val exo = VideoPlaybackController.exoPlayerInstance
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
                                            this.player = exo
                                            VideoPlaybackController.notifyVideoSurfaceAttached(exo != null)
                                        }
                                    },
                                    update = { playerView ->
                                        val currentExo = VideoPlaybackController.exoPlayerInstance
                                        if (playerView.player != currentExo) {
                                            playerView.player = currentExo
                                        }
                                        VideoPlaybackController.notifyVideoSurfaceAttached(currentExo != null)
                                    },
                                    onReset = { playerView ->
                                        playerView.player = null
                                        VideoPlaybackController.notifyVideoSurfaceAttached(false)
                                    },
                                    onRelease = { playerView ->
                                        playerView.player = null
                                        VideoPlaybackController.notifyVideoSurfaceAttached(false)
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("embedded_video_surface")
                                )
                            } else {
                                VideoAudioOnlyStage(
                                    videoTitle = vid.title,
                                    isPlaying = videoState.isPlaying,
                                    language = language,
                                    onSwitchToNormalVideo = { selectMode(VideoPlaybackMode.NORMAL_VIDEO) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (videoState.isBuffering && videoState.errorMessage == null && videoState.shouldRenderVideoFrames) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp),
                                    strokeWidth = 3.dp
                                )
                            }

                            // Top quick actions inside video stage (Minimize & Fullscreen)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { setExpanded(false) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .testTag("embedded_video_close")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = JnmfStrings.videoMinimizeToBg(language),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { setFullscreenMode(true) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .testTag("embedded_video_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = JnmfStrings.fullscreen(language),
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            if (videoState.errorMessage != null) {
                                VideoErrorCard(
                                    errorMessage = videoState.errorMessage,
                                    language = language,
                                    onRetry = { VideoPlaybackController.retry() },
                                    onClose = { stopVideoPlayback() }
                                )
                            }
                        }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                    // Always-accessible Transport, Scrubber & Background Playback Mode Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Current Video Title & Metadata
                                Text(
                                    text = vid.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("video_player_title")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${vid.formattedDuration} • ${(vid.sizeBytes / (1024 * 1024))} MB • $modeLabel",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Seek Scrubber
                                val currentPos = if (isScrubbing) {
                                    scrubPositionMs.toLong()
                                } else {
                                    videoState.currentPositionMs
                                }
                                val dur = videoState.durationMs.coerceAtLeast(1L)
                                val sliderVal = (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

                                Slider(
                                    value = sliderVal,
                                    onValueChange = { newVal ->
                                        isScrubbing = true
                                        scrubPositionMs = newVal * dur
                                    },
                                    onValueChangeFinished = {
                                        VideoPlaybackController.seekTo(scrubPositionMs.toLong())
                                        isScrubbing = false
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("video_seek_slider")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatVideoDuration(currentPos),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        modifier = Modifier.testTag("video_current_position_text")
                                    )
                                    Text(
                                        text = formatVideoDuration(videoState.durationMs),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Primary Transport Row: Prev, -10s, Play/Pause, +10s, Next, Stop
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { VideoPlaybackController.previous() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("video_previous_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = JnmfStrings.previous(language),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { VideoPlaybackController.seekBy(-10_000L) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("video_rewind_10s")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = JnmfStrings.seekBackward10s(language),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { VideoPlaybackController.togglePlayPause() },
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .testTag("video_play_pause")
                                    ) {
                                        Icon(
                                            imageVector = if (videoState.isPlaying) {
                                                Icons.Default.Pause
                                            } else {
                                                Icons.Default.PlayArrow
                                            },
                                            contentDescription = if (videoState.isPlaying) {
                                                JnmfStrings.pause(language)
                                            } else {
                                                JnmfStrings.play(language)
                                            },
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { VideoPlaybackController.seekBy(10_000L) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("video_forward_10s")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = JnmfStrings.seekForward10s(language),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { VideoPlaybackController.next() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("video_next_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = JnmfStrings.next(language),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { stopVideoPlayback() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
                                                CircleShape
                                            )
                                            .testTag("video_stop_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = JnmfStrings.videoStop(language),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Background Playback Control (Normal Video vs Audio Only)
                                BackgroundPlaybackModeSelector(
                                    selectedMode = videoState.playbackMode,
                                    language = language,
                                    onSelectMode = { selectMode(it) }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Fullscreen & Back-to-List Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { setFullscreenMode(true) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("btn_enter_fullscreen")
                                    ) {
                                        Icon(
                                            Icons.Default.Fullscreen,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = JnmfStrings.fullscreen(language),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { setExpanded(false) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("btn_close_player")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = JnmfStrings.videoMinimizeToBg(language),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                                text = JnmfStrings.videosCount(videos.size, language),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(videos, key = { it.id }) { item ->
                        val isCurrent = (item.id == vid.id)
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            VideoListItemCard(
                                video = item,
                                isCurrent = isCurrent,
                                activeModeLabel = if (isCurrent) modeLabel else null,
                                onClick = { startOrSelectVideo(item) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
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
                            text = JnmfStrings.videosFoundOnDevice(videos.size, language),
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
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("refresh_videos_button")
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(
                                    JnmfStrings.refreshVideos(language),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    val activeModeLabel = JnmfStrings.videoModeLabel(videoState.playbackMode, language)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(videos, key = { it.id }) { video ->
                            val isCurrent = (activeVideo?.id == video.id)
                            VideoListItemCard(
                                video = video,
                                isCurrent = isCurrent,
                                activeModeLabel = if (isCurrent) activeModeLabel else null,
                                onClick = { startOrSelectVideo(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}
