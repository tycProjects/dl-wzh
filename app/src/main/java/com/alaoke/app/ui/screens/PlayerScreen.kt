package com.alaoke.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alaoke.app.ui.AlaokeViewModel
import com.alaoke.app.ui.theme.LyricsTextStyle
import com.alaoke.app.youtube.YouTubePlayerController
import com.alaoke.app.youtube.YouTubePlayerView

@Composable
fun PlayerScreen(
    viewModel: AlaokeViewModel,
    onBack: () -> Unit
) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val queue by viewModel.queue.collectAsState()

    val controller = remember { YouTubePlayerController() }
    var isPlaying by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(80f) }

    remember(nowPlaying?.youtubeVideoId) {
        controller.onVideoEnded = {
            if (queue.isNotEmpty()) viewModel.playNextFromQueue()
        }
        true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video / lyrics surface
        if (nowPlaying?.youtubeVideoId != null) {
            YouTubePlayerView(
                controller = controller,
                initialVideoId = nowPlaying?.youtubeVideoId,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    nowPlaying?.title ?: "Nothing playing",
                    style = LyricsTextStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    nowPlaying?.artist ?: "Pick a song from Songs or YouTube Search",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isFullscreen) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        nowPlaying?.let { "\uD83C\uDFA4 ${it.singerName ?: "You"} - ${it.title}" } ?: "Karaoke Player",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    nowPlaying?.artist?.let { Text(it, color = Color.White.copy(alpha = 0.8f), maxLines = 1) }
                }
                IconButton(onClick = { isFullscreen = true }) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolumeDown, contentDescription = null, tint = Color.White)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            controller.setVolume(it.toInt())
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f).height(24.dp)
                    )
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isPlaying = !isPlaying
                            if (isPlaying) controller.play() else controller.pause()
                        }
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.height(40.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.playNextFromQueue() },
                        enabled = queue.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = if (queue.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
                if (queue.isNotEmpty()) {
                    Text(
                        "Up next: ${queue.first().singerName} - ${queue.first().songTitle}",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            IconButton(
                onClick = { isFullscreen = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit fullscreen", tint = Color.White)
            }
        }
    }
}
