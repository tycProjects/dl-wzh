package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.playback.MusicController
import com.example.ui.components.JnmfGlobeLogo
import com.example.ui.components.JnmfTaskProgressBar
import com.example.ui.components.TaskProgressState
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoScreen(
    videos: List<VideoItem>,
    videoScanState: TaskProgressState = TaskProgressState.IDLE,
    onRefresh: () -> Unit = {},
    onDismissProgress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var activeVideo by remember { mutableStateOf<VideoItem?>(null) }
    var isFullscreenLandscape by remember { mutableStateOf(false) }

    // Video Player State
    var isVideoPlaying by remember { mutableStateOf(false) }
    var videoPositionMs by remember { mutableLongStateOf(0L) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(false) }
    var videoErrorMessage by remember { mutableStateOf<String?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }

    // Hardware back press inside video view returns to gallery instead of exiting app
    BackHandler(enabled = activeVideo != null) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        isFullscreenLandscape = false
        activeVideo = null
    }

    // Manage ExoPlayer instance and audio focus
    var videoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(activeVideo) {
        val currentVid = activeVideo
        if (currentVid != null) {
            // Coordinate with music player: pause music playback to prevent competing audio output
            MusicController.pause()
            videoErrorMessage = null

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val player = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()

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
            }

            videoPlayer = player
        }

        onDispose {
            videoPlayer?.let { p ->
                videoPositionMs = p.currentPosition
                p.release()
            }
            videoPlayer = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

            // Fullscreen Video Player View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            isFullscreenLandscape = false
                            activeVideo = null
                        },
                        modifier = Modifier.testTag("back_from_video")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vid.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${vid.formattedDuration} • ${(vid.sizeBytes / (1024 * 1024))} MB",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }

                    // Fullscreen orientation toggle
                    IconButton(
                        onClick = {
                            isFullscreenLandscape = !isFullscreenLandscape
                            if (isFullscreenLandscape) {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            } else {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        },
                        modifier = Modifier.testTag("toggle_fullscreen")
                    ) {
                        Icon(
                            imageVector = if (isFullscreenLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = Color.White
                        )
                    }
                }

                // Video Surface & Overlay Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = videoPlayer
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = false // Use custom responsive Jetpack Compose controls
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Buffering Indicator
                    if (isBuffering && videoErrorMessage == null) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Error Overlay if codec or file fails
                    if (videoErrorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(12.dp),
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
                                    text = "Playback Error",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = videoErrorMessage ?: "Unable to play video",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            videoErrorMessage = null
                                            videoPlayer?.prepare()
                                            videoPlayer?.play()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Button(
                                        onClick = {
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                            activeVideo = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Text("Close", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Video Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val currentPos = if (isScrubbing) scrubPositionMs.toLong() else videoPositionMs
                    val dur = videoDurationMs.coerceAtLeast(1L)
                    val sliderVal = (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

                    // Seek Slider
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

                    // Time labels and controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(currentPos),
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        // Seek -10s, Play/Pause, Seek +10s
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newPos = (videoPositionMs - 10000L).coerceAtLeast(0L)
                                    videoPlayer?.seekTo(newPos)
                                    videoPositionMs = newPos
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Seek -10s",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    videoPlayer?.let { p ->
                                        if (p.isPlaying) p.pause() else p.play()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("video_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val newPos = (videoPositionMs + 10000L).coerceAtMost(videoDurationMs)
                                    videoPlayer?.seekTo(newPos)
                                    videoPositionMs = newPos
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Seek +10s",
                                    tint = Color.White
                                )
                            }
                        }

                        Text(
                            text = formatDuration(videoDurationMs),
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            // Video Gallery Browser
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
                            text = "Local Video Gallery",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${videos.size} video files found on device",
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
                                contentDescription = "Refresh Videos",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Loading progress indicator (1% to 100% or indeterminate)
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
                                text = "No local videos found.",
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
                                Text("Rescan Storage", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(videos, key = { it.id }) { video ->
                            Card(
                                onClick = {
                                    videoPositionMs = 0L
                                    activeVideo = video
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(10.dp),
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
                                            .size(width = 64.dp, height = 48.dp)
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
                                            color = MaterialTheme.colorScheme.onSurface,
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
                    }
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
