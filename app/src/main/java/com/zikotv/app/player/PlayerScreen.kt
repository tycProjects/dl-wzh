package com.zikotv.app.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zikotv.app.R
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.Locale

@Composable
fun PlayerScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    DisposableEffect(player, activity) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) errorMessage = null
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = error.message ?: "Playback error"
                isBuffering = false
            }
        }
        player.addListener(listener)

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        onDispose {
            player.removeListener(listener)
            player.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(url) {
        if (url.isBlank()) {
            errorMessage = "No stream URL"
            isBuffering = false
            return@LaunchedEffect
        }
        errorMessage = null
        isBuffering = true
        prepareStream(player, url)
    }

    LaunchedEffect(player, isSeeking) {
        while (true) {
            if (!isSeeking) {
                position = player.currentPosition.coerceAtLeast(0L)
                duration = player.duration.coerceAtLeast(0L)
                sliderPosition =
                    if (duration > 0L) position.toFloat() / duration.toFloat() else 0f
            }
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerChrome(
                title = title.ifBlank { "Ziko TV" },
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                sliderPosition = sliderPosition,
                onBack = onBack,
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                onRewind = {
                    player.seekTo((player.currentPosition - 10_000L).coerceAtLeast(0L))
                },
                onForward = {
                    val target = player.currentPosition + 10_000L
                    player.seekTo(
                        if (player.duration > 0) target.coerceAtMost(player.duration) else target
                    )
                },
                onSliderChange = {
                    isSeeking = true
                    sliderPosition = it
                },
                onSliderFinish = {
                    if (duration > 0) player.seekTo((duration * sliderPosition).toLong())
                    isSeeking = false
                }
            )
        }

        errorMessage?.let { error ->
            ErrorPanel(
                message = error,
                onRetry = {
                    errorMessage = null
                    isBuffering = true
                    prepareStream(player, url)
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PlayerChrome(
    title: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    sliderPosition: Float,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSliderChange: (Float) -> Unit,
    onSliderFinish: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.72f),
                    0.35f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.82f)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassRoundButton(iconRes = R.drawable.ic_back, onClick = onBack)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ZIKO TV PLAYER",
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
            }
            GlassRoundButton(iconRes = R.drawable.ic_settings, onClick = {})
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassRoundButton(
                iconRes = R.drawable.ic_replay_10,
                size = 58,
                onClick = onRewind
            )
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(44.dp)
                )
            }
            GlassRoundButton(
                iconRes = R.drawable.ic_forward_10,
                size = 58,
                onClick = onForward
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.075f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.14f),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Slider(
                value = sliderPosition.coerceIn(0f, 1f),
                onValueChange = onSliderChange,
                onValueChangeFinished = onSliderFinish,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${formatTime(position)}  /  ${formatTime(duration)}",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_fullscreen),
                    contentDescription = "Fullscreen",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassRoundButton(
    iconRes: Int,
    size: Int = 50,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.34f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size((size * 0.52f).dp)
        )
    }
}

@Composable
private fun ErrorPanel(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Black.copy(alpha = 0.88f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(26.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Playback unavailable",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Text(
            message,
            color = Color.White.copy(alpha = 0.56f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

private fun prepareStream(player: ExoPlayer, url: String) {
    val httpFactory = OkHttpDataSource.Factory(OkHttpClient.Builder().build())
    val item = MediaItem.fromUri(url)
    val clean = url.substringBefore('?').lowercase(Locale.US)

    val source = when {
        clean.endsWith(".m3u8") ->
            HlsMediaSource.Factory(httpFactory).createMediaSource(item)

        clean.endsWith(".mpd") ->
            DashMediaSource.Factory(httpFactory).createMediaSource(item)

        else ->
            ProgressiveMediaSource.Factory(httpFactory).createMediaSource(item)
    }

    player.setMediaSource(source)
    player.prepare()
    player.playWhenReady = true
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3600L
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
