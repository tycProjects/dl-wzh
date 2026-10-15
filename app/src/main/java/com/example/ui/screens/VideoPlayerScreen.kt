package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoItem
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    video: VideoItem,
    onBack: () -> Unit,
    onProgressUpdate: (Int, Boolean) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentSeconds by remember { mutableFloatStateOf(video.currentPositionSeconds.toFloat()) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(video.isCompleted) }
    var showControls by remember { mutableStateOf(true) }

    val totalDurationSeconds = video.durationSeconds.toFloat().coerceAtLeast(60f)

    // Auto-advance video time and periodically save watch progress
    LaunchedEffect(isPlaying, playbackSpeed) {
        while (isPlaying) {
            delay((1000 / playbackSpeed).toLong())
            if (currentSeconds < totalDurationSeconds) {
                currentSeconds += 1f
                // Auto-save watch position every 5 seconds
                if (currentSeconds.toInt() % 5 == 0) {
                    onProgressUpdate(currentSeconds.toInt(), isCompleted)
                }
            } else {
                isPlaying = false
                isCompleted = true
                onProgressUpdate(totalDurationSeconds.toInt(), true)
            }
        }
    }

    fun formatTime(sec: Float): String {
        val s = sec.toInt()
        val m = s / 60
        val r = s % 60
        return "%02d:%02d".format(m, r)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        // Video Stage Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullscreen) 400.dp else 240.dp)
                .background(Color(0xFF0F172A))
                .clickable { showControls = !showControls }
                .testTag("video_player_stage")
        ) {
            // Simulated video content graphics
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = BrandPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Top Overlay controls
            if (showControls) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "سرعة العرض: ${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { isFullscreen = !isFullscreen }) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = "ملء الشاشة",
                            tint = Color.White
                        )
                    }
                }

                // Center play/pause & skip controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentSeconds = (currentSeconds - 10f).coerceAtLeast(0f) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastRewind,
                            contentDescription = "تأخير 10 ثواني",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(60.dp)
                            .background(BrandPrimary, CircleShape)
                            .testTag("player_toggle_play_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { currentSeconds = (currentSeconds + 10f).coerceAtMost(totalDurationSeconds) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = "تقديم 10 ثواني",
                            tint = Color.White
                        )
                    }
                }

                // Bottom Progress Bar & Time
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Slider(
                        value = currentSeconds,
                        onValueChange = {
                            currentSeconds = it
                            onProgressUpdate(it.toInt(), isCompleted)
                        },
                        valueRange = 0f..totalDurationSeconds,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandPrimary,
                            activeTrackColor = BrandPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Text(
                            text = formatTime(totalDurationSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Details Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandLightBlue
                            ) {
                                Text(
                                    text = video.categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (isCompleted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "مكتمل",
                                        tint = BrandSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "مكتمل",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = video.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Playback Speed Controls
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "سرعة التشغيل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                val isSelected = playbackSpeed == speed
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { playbackSpeed = speed },
                                    label = { Text("${speed}x") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // Completion Toggle
                Button(
                    onClick = {
                        isCompleted = !isCompleted
                        onProgressUpdate(currentSeconds.toInt(), isCompleted)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) BrandSuccess else BrandPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("toggle_video_completed_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompleted) "تم حفظ المحاضرة كمكتملة بنجاح" else "تحديد المحاضرة كمكتملة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
