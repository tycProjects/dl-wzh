package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
import com.example.playback.VideoQueueMode
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

private fun hasVideoReadPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val fullVideo = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
        val partialVisual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        fullVideo || partialVisual
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun JnmfVideoPlayerSurface(
    playerInstanceId: Int,
    shouldRender: Boolean,
    testTag: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false
                isClickable = false
                isFocusable = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setKeepContentOnPlayerReset(true)
                keepScreenOn = true
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                if (shouldRender && !VideoPlaybackController.state.value.isPlayerUiVisible) {
                    VideoPlaybackController.setPlayerUiVisible(true)
                }
                VideoPlaybackController.attachVideoPlayerView(this)
            }
        },
        update = { playerView ->
            // Observe playerInstanceId & shouldRender so surface stays bound across player/layout changes
            val observedPlayerId = playerInstanceId
            if (shouldRender && observedPlayerId != -1) {
                if (!VideoPlaybackController.state.value.isPlayerUiVisible) {
                    VideoPlaybackController.setPlayerUiVisible(true)
                }
                VideoPlaybackController.attachVideoPlayerView(playerView)
            } else {
                VideoPlaybackController.detachVideoPlayerView(playerView)
            }
        },
        onReset = { playerView ->
            VideoPlaybackController.detachVideoPlayerView(playerView)
        },
        onRelease = { playerView ->
            VideoPlaybackController.detachVideoPlayerView(playerView)
        },
        modifier = modifier.testTag(testTag)
    )
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
    onLandscapeChanged: (Boolean) -> Unit = {},
    isPlayerExpanded: Boolean? = null,
    onPlayerExpandedChanged: (Boolean) -> Unit = {},
    onSelectPlaybackMode: ((VideoPlaybackMode) -> Unit)? = null,
    onSelectQueueMode: ((VideoQueueMode) -> Unit)? = null,
    language: AppLanguage = LocalAppLanguage.current,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val activity = context as? Activity
    val initialActivityOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }

    val videoState by VideoPlaybackController.state.collectAsState()
    val activeVideo = videoState.currentVideo

    var hasVideoAccess by remember { mutableStateOf(hasVideoReadPermission(context)) }
    val videoPermissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = hasVideoReadPermission(context) || results.values.any { it }
        hasVideoAccess = granted
        if (granted) {
            onRefresh()
        }
    }

    var localExpanded by rememberSaveable { mutableStateOf(activeVideo != null) }
    val effectiveExpanded = (isPlayerExpanded ?: localExpanded) && activeVideo != null

    var localFullscreen by rememberSaveable { mutableStateOf(false) }
    var lastExternalFullscreen by remember { mutableStateOf(isFullscreen) }
    if (isFullscreen != lastExternalFullscreen) {
        lastExternalFullscreen = isFullscreen
        localFullscreen = isFullscreen
    }
    val effectiveFullscreen = (isFullscreen || localFullscreen) && effectiveExpanded

    // Explicit user orientation override while active video player is open:
    // true = Landscape, false = Portrait, null = follow default mode/fullscreen rule
    var userRequestedLandscape by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var isControlsLocked by rememberSaveable { mutableStateOf(false) }

    val defaultFullscreenLandscape = !(videoState.videoAspectRatio in 0.01f..0.85f)
    val isLandscapeOrientation = userRequestedLandscape ?: when {
        effectiveFullscreen -> defaultFullscreenLandscape
        else -> configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun applyVideoScreenOrientationAndBars(
        expanded: Boolean = effectiveExpanded,
        fullscreen: Boolean = effectiveFullscreen,
        requestedLandscape: Boolean? = userRequestedLandscape
    ) {
        if (!expanded) {
            setSystemBarsVisibility(activity, true)
            activity?.requestedOrientation = initialActivityOrientation
            onLandscapeChanged(false)
            return
        }
        val resolvedLandscape = requestedLandscape ?: when {
            fullscreen -> !(videoState.videoAspectRatio in 0.01f..0.85f)
            else -> configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        val hideSystemBars = fullscreen || requestedLandscape == true
        setSystemBarsVisibility(activity, !hideSystemBars)
        activity?.requestedOrientation = when {
            requestedLandscape == true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            requestedLandscape == false -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            fullscreen -> if (videoState.videoAspectRatio in 0.01f..0.85f) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            else -> initialActivityOrientation
        }
        onLandscapeChanged(resolvedLandscape)
    }

    fun setExpanded(expanded: Boolean) {
        localExpanded = expanded
        onPlayerExpandedChanged(expanded)
        if (!expanded) {
            userRequestedLandscape = null
            applyVideoScreenOrientationAndBars(
                expanded = false,
                fullscreen = false,
                requestedLandscape = null
            )
        }
    }

    fun setFullscreenMode(enabled: Boolean) {
        val nextRequestedLandscape = when {
            !enabled && effectiveFullscreen -> null
            userRequestedLandscape == false -> null
            else -> userRequestedLandscape
        }
        localFullscreen = enabled
        userRequestedLandscape = nextRequestedLandscape
        onFullscreenChanged(enabled)
        applyVideoScreenOrientationAndBars(
            expanded = effectiveExpanded || enabled,
            fullscreen = enabled,
            requestedLandscape = nextRequestedLandscape
        )
    }

    fun toggleVideoOrientation() {
        if (isControlsLocked) return
        val nextLandscape = !isLandscapeOrientation
        userRequestedLandscape = nextLandscape
        applyVideoScreenOrientationAndBars(
            expanded = effectiveExpanded,
            fullscreen = effectiveFullscreen,
            requestedLandscape = nextLandscape
        )
    }

    // Keep system bars and screen orientation synchronized across fullscreen, orientation, and expansion changes
    LaunchedEffect(
        effectiveExpanded,
        effectiveFullscreen,
        userRequestedLandscape,
        videoState.videoAspectRatio,
        configuration.orientation
    ) {
        applyVideoScreenOrientationAndBars(
            expanded = effectiveExpanded,
            fullscreen = effectiveFullscreen,
            requestedLandscape = userRequestedLandscape
        )
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

    fun selectQueueMode(mode: VideoQueueMode) {
        if (onSelectQueueMode != null) {
            onSelectQueueMode(mode)
        } else {
            VideoPlaybackController.setQueueMode(mode)
            scope.launch {
                settingsRepo.setVideoQueueMode(mode)
            }
        }
    }

    fun cycleQueueMode() {
        selectQueueMode(videoState.queueMode.nextMode())
    }

    fun startOrSelectVideo(video: VideoItem) {
        PlaybackService.startService(context)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            VideoPlaybackController.setPlayerUiVisible(true)
        }
        if (videoState.currentVideo?.id == video.id) {
            // Returning to the already-active video restores position & mode without resetting;
            // if already inside the expanded player and stopped/paused (e.g. after Play Once ended), resume cleanly.
            val wasAlreadyExpanded = effectiveExpanded
            setExpanded(true)
            if (wasAlreadyExpanded && !videoState.isPlaying) {
                VideoPlaybackController.resume()
            }
        } else {
            VideoPlaybackController.playVideo(video, videos, startPositionMs = 0L)
            setExpanded(true)
        }
    }

    var showLockedOverlay by remember { mutableStateOf(false) }
    var isScrubbing by remember(activeVideo?.id) { mutableStateOf(false) }
    var scrubPositionMs by remember(activeVideo?.id) { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }

    fun lockControls() {
        isScrubbing = false
        isControlsLocked = true
        showControls = false
        showLockedOverlay = true
    }

    fun unlockControls() {
        isControlsLocked = false
        showLockedOverlay = false
        showControls = true
    }

    fun stopVideoPlayback() {
        isControlsLocked = false
        showLockedOverlay = false
        userRequestedLandscape = null
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
                Lifecycle.Event.ON_START -> {
                    isLifecycleStarted = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    isLifecycleStarted = true
                    hasVideoAccess = hasVideoReadPermission(context)
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
    val shouldRenderVideoSurface = isPlayerForegroundVisible &&
        videoState.playbackMode == VideoPlaybackMode.NORMAL_VIDEO

    DisposableEffect(isPlayerForegroundVisible) {
        VideoPlaybackController.setPlayerUiVisible(isPlayerForegroundVisible)
        onDispose {}
    }

    DisposableEffect(Unit) {
        onDispose {
            // Leaving VideoScreen (e.g. switching bottom tabs): stop rendering video frames
            // while allowing background audio playback to continue via PlaybackService,
            // and always restore the Activity's previous orientation!
            VideoPlaybackController.clearActiveVideoViews()
            VideoPlaybackController.setPlayerUiVisible(false)
            setSystemBarsVisibility(activity, true)
            activity?.requestedOrientation = initialActivityOrientation
            onFullscreenChanged(false)
            onLandscapeChanged(false)
        }
    }

    // Close expanded state and clear lock/orientation automatically if playback stopped externally or errored
    LaunchedEffect(activeVideo, videoState.errorMessage) {
        if (activeVideo == null) {
            isControlsLocked = false
            showLockedOverlay = false
            userRequestedLandscape = null
            if (localExpanded || localFullscreen) {
                setFullscreenMode(false)
                setExpanded(false)
            }
        } else if (videoState.errorMessage != null && isControlsLocked) {
            unlockControls()
        }
    }

    LaunchedEffect(effectiveExpanded) {
        if (!effectiveExpanded) {
            if (isControlsLocked) {
                isControlsLocked = false
                showLockedOverlay = false
            }
            if (userRequestedLandscape != null) {
                userRequestedLandscape = null
            }
        }
    }

    // Hardware Back press inside video view:
    // Locked -> reveal Unlock button instead of accidental exit;
    // Fullscreen -> exit fullscreen; Embedded Landscape -> return to Portrait; Embedded Portrait -> minimize to list!
    BackHandler(enabled = effectiveExpanded) {
        if (isControlsLocked) {
            showLockedOverlay = true
        } else if (effectiveFullscreen) {
            setFullscreenMode(false)
        } else if (userRequestedLandscape == true) {
            toggleVideoOrientation()
        } else {
            setExpanded(false)
        }
    }

    LaunchedEffect(showControls, videoState.isPlaying, isScrubbing, videoState.playbackMode, isControlsLocked) {
        if (!isControlsLocked && showControls && videoState.isPlaying && !isScrubbing &&
            videoState.playbackMode == VideoPlaybackMode.NORMAL_VIDEO
        ) {
            delay(4500)
            showControls = false
        }
    }

    LaunchedEffect(isControlsLocked, showLockedOverlay) {
        if (isControlsLocked && showLockedOverlay) {
            delay(3500)
            showLockedOverlay = false
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
                val onStageTapped = {
                    if (isControlsLocked) {
                        showLockedOverlay = !showLockedOverlay
                    } else {
                        showControls = !showControls
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .testTag("fullscreen_player_container"),
                    contentAlignment = Alignment.Center
                ) {
                    if (shouldRenderVideoSurface) {
                        JnmfVideoPlayerSurface(
                            playerInstanceId = videoState.playerInstanceId,
                            shouldRender = shouldRenderVideoSurface,
                            testTag = "fullscreen_video_surface",
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onStageTapped
                                )
                                .testTag("fullscreen_stage_tap_layer")
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onStageTapped
                                )
                                .testTag("fullscreen_stage_tap_layer")
                        )
                        VideoAudioOnlyStage(
                            videoTitle = vid.title,
                            isPlaying = videoState.isPlaying,
                            language = language,
                            onSwitchToNormalVideo = { selectMode(VideoPlaybackMode.NORMAL_VIDEO) },
                            controlsLocked = isControlsLocked,
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
                        visible = !isControlsLocked && (
                            showControls || videoState.errorMessage != null ||
                                videoState.playbackMode == VideoPlaybackMode.AUDIO_ONLY
                            ),
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
                            // Top Bar: Back, Title, Mode Chip, Exit Fullscreen
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

                                Spacer(modifier = Modifier.width(10.dp))

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
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

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
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { toggleVideoOrientation() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .testTag("fullscreen_video_orientation_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = if (isLandscapeOrientation) {
                                            JnmfStrings.videoSwitchToPortrait(language)
                                        } else {
                                            JnmfStrings.videoSwitchToLandscape(language)
                                        },
                                        tint = if (isLandscapeOrientation) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White
                                        }
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        IconButton(
                                            onClick = { lockControls() },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .testTag("video_lock_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = JnmfStrings.videoLockControls(language),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "${formatVideoDuration(currentPos)} / ${formatVideoDuration(videoState.durationMs)}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { cycleQueueMode() },
                                            shape = RoundedCornerShape(24.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.Black.copy(alpha = 0.65f),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            ),
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                .testTag("fullscreen_queue_mode_button")
                                        ) {
                                            Icon(
                                                imageVector = videoQueueModeIcon(videoState.queueMode),
                                                contentDescription = JnmfStrings.videoQueueModeDescription(
                                                    videoState.queueMode,
                                                    language
                                                ),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = JnmfStrings.videoQueueModeLabel(
                                                    videoState.queueMode,
                                                    language
                                                ),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

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
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isControlsLocked && showLockedOverlay,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .safeDrawingPadding()
                            .padding(top = 16.dp)
                    ) {
                        VideoLockedControlsOverlay(
                            language = language,
                            onUnlock = { unlockControls() }
                        )
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
                // EMBEDDED VIDEO PLAYER MODE (Portrait & Landscape Adaptive)
                // =========================================================================
                val isViewportLandscape =
                    configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
                        (userRequestedLandscape == true && configuration.screenWidthDp > configuration.screenHeightDp)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .let { base -> if (isViewportLandscape) base.safeDrawingPadding() else base }
                        .testTag("embedded_video_player_container")
                ) {
                    val useSideBySideLandscape = isViewportLandscape || maxWidth > maxHeight

                    val renderEmbeddedStage: @Composable (Modifier) -> Unit = { stageBoxModifier ->
                        val onEmbeddedStageTapped = {
                            if (isControlsLocked) {
                                showLockedOverlay = !showLockedOverlay
                            } else {
                                showControls = !showControls
                            }
                        }
                        Box(
                            modifier = stageBoxModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            if (shouldRenderVideoSurface) {
                                JnmfVideoPlayerSurface(
                                    playerInstanceId = videoState.playerInstanceId,
                                    shouldRender = shouldRenderVideoSurface,
                                    testTag = "embedded_video_surface",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onEmbeddedStageTapped
                                        )
                                        .testTag("embedded_stage_tap_layer")
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onEmbeddedStageTapped
                                        )
                                        .testTag("embedded_stage_tap_layer")
                                )
                                VideoAudioOnlyStage(
                                    videoTitle = vid.title,
                                    isPlaying = videoState.isPlaying,
                                    language = language,
                                    onSwitchToNormalVideo = { selectMode(VideoPlaybackMode.NORMAL_VIDEO) },
                                    controlsLocked = isControlsLocked,
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

                            // Top quick actions inside video stage (Minimize, Lock, Orientation & Fullscreen)
                            if (!isControlsLocked) {
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

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { lockControls() },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .testTag("video_lock_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = JnmfStrings.videoLockControls(language),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { toggleVideoOrientation() },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .testTag("embedded_video_orientation_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ScreenRotation,
                                                contentDescription = if (isLandscapeOrientation) {
                                                    JnmfStrings.videoSwitchToPortrait(language)
                                                } else {
                                                    JnmfStrings.videoSwitchToLandscape(language)
                                                },
                                                tint = if (isLandscapeOrientation) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.White
                                                },
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
                                }
                            }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isControlsLocked && showLockedOverlay,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        ) {
                            VideoLockedControlsOverlay(
                                language = language,
                                onUnlock = { unlockControls() }
                            )
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
                    }

                    val renderEmbeddedDetailsPane: @Composable (Modifier) -> Unit = { detailsModifier ->
                        if (isControlsLocked) {
                            EmbeddedLockedInfoCard(
                                video = vid,
                                currentPositionMs = videoState.currentPositionMs,
                                durationMs = videoState.durationMs,
                                modeLabel = modeLabel,
                                language = language,
                                onTapLockedArea = { showLockedOverlay = true },
                                modifier = detailsModifier
                            )
                        } else {
                            LazyColumn(
                                modifier = detailsModifier
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

                                Spacer(modifier = Modifier.height(10.dp))

                                // Video Playback Queue Mode Control (Play Once, Repeat One, Shuffle, Repeat All)
                                VideoQueueModeControl(
                                    queueMode = videoState.queueMode,
                                    language = language,
                                    onCycleMode = { cycleQueueMode() }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Background Playback Control (Normal Video vs Audio Only)
                                BackgroundPlaybackModeSelector(
                                    selectedMode = videoState.playbackMode,
                                    language = language,
                                    onSelectMode = { selectMode(it) }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Fullscreen, Landscape/Portrait & Back-to-List Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { setFullscreenMode(true) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
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
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = JnmfStrings.fullscreen(language),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { toggleVideoOrientation() },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isLandscapeOrientation) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            }
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("btn_toggle_orientation")
                                    ) {
                                        Icon(
                                            Icons.Default.ScreenRotation,
                                            contentDescription = if (isLandscapeOrientation) {
                                                JnmfStrings.videoSwitchToPortrait(language)
                                            } else {
                                                JnmfStrings.videoSwitchToLandscape(language)
                                            },
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isLandscapeOrientation) {
                                                JnmfStrings.videoPortraitMode(language)
                                            } else {
                                                JnmfStrings.videoLandscapeMode(language)
                                            },
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { setExpanded(false) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
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
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = JnmfStrings.videoMinimizeToBg(language),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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

                    items(
                        items = videos,
                        key = { it.id },
                        contentType = { "video_item_card" }
                    ) { item ->
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

                    if (useSideBySideLandscape) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            renderEmbeddedStage(
                                Modifier
                                    .weight(1.25f)
                                    .fillMaxHeight()
                                    .background(Color.Black)
                                    .testTag("embedded_video_stage_box")
                            )
                            renderEmbeddedDetailsPane(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            renderEmbeddedStage(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(videoState.videoAspectRatio.coerceIn(1.25f, 2.2f))
                                    .background(Color.Black)
                                    .testTag("embedded_video_stage_box")
                            )
                            renderEmbeddedDetailsPane(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
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
                            if (!hasVideoAccess) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = JnmfStrings.videoPermissionHint(language),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        videoPermissionLauncher.launch(videoPermissionsToRequest)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("grant_video_permission_button")
                                ) {
                                    Text(
                                        text = JnmfStrings.grantVideoAccess(language),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
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
                        items(
                            items = videos,
                            key = { it.id },
                            contentType = { "video_item_card" }
                        ) { video ->
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
