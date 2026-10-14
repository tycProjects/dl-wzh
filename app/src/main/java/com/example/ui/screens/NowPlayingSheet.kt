package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.ui.components.AlbumArtView
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfBorderGlow
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfSurfaceVariant
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val strings = com.example.ui.i18n.LocalAppStrings.current
    val colors = com.example.ui.theme.JnmfTheme.colors

    var isUserDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }

    val activeSliderPos = if (isUserDragging) {
        dragPositionMs
    } else {
        currentPositionMs.toFloat()
    }

    val maxSliderVal = durationMs.toFloat().coerceAtLeast(1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JnmfDarkBg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top bar with collapse and quick actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("now_playing_collapse_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = strings.collapseAction,
                    tint = colors.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = strings.terminalHeader,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (song.isSampleTrack) "Synthesized Indo Remix" else "Offline Local Audio",
                    fontSize = 10.sp,
                    color = JnmfTextMuted
                )
            }

            IconButton(
                onClick = onOpenEqualizer,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("now_playing_eq_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = strings.equalizerAction,
                    tint = colors.bright,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center: Vinyl Album Art with Cyber Glow border
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .border(2.dp, colors.primary, CircleShape)
                .background(JnmfSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtView(
                song = song,
                isPlaying = isPlaying,
                modifier = Modifier.size(232.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Song Title, Artist & Beat Category Chips
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = JnmfTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = JnmfTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Indo Remix Multi-Factor Beat Analysis Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(JnmfSurfaceElevated)
                    .border(1.dp, colors.borderGlow, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BPM: ${song.bpm}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.bright
                        )
                        Text(
                            text = "Classification: ${song.beatCategory.displayName.uppercase()}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = song.beatCategory.tagColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Beat Density: ${song.displayBeatDensity}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = JnmfTextSecondary
                        )
                        Text(
                            text = "Beat Intensity: ${song.displayBeatIntensity}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = JnmfTextSecondary
                        )
                        Text(
                            text = "Energy: ${song.displayEnergy}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = JnmfTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Seek Bar & Timers
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = activeSliderPos.coerceIn(0f, maxSliderVal),
                onValueChange = {
                    isUserDragging = true
                    dragPositionMs = it
                },
                onValueChangeFinished = {
                    onSeek(dragPositionMs.toLong())
                    isUserDragging = false
                },
                valueRange = 0f..maxSliderVal,
                colors = SliderDefaults.colors(
                    thumbColor = colors.bright,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = JnmfSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("now_playing_seek_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Song.formatDuration(activeSliderPos.toLong()),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colors.primary
                )
                Text(
                    text = Song.formatDuration(durationMs),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = JnmfTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("now_playing_shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) colors.bright else JnmfTextMuted
                )
            }

            // Previous
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("now_playing_previous_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = JnmfTextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            val mainPlayButtonScale by animateFloatAsState(
                targetValue = if (isPlaying) 1.06f else 1.0f,
                animationSpec = tween(durationMillis = 200),
                label = "mainPlayScale"
            )

            // Play / Pause (Neon Button)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(mainPlayButtonScale)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .border(2.dp, colors.bright, CircleShape)
                    .testTag("now_playing_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = JnmfDarkBg,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Next
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("now_playing_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = JnmfTextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = onCycleRepeat,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("now_playing_repeat_button")
            ) {
                Icon(
                    imageVector = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        RepeatMode.ALL -> Icons.Default.Repeat
                        RepeatMode.OFF -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat: $repeatMode",
                    tint = if (repeatMode != RepeatMode.OFF) colors.bright else JnmfTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom action: Add to Playlist
        IconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier.testTag("now_playing_add_playlist_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = colors.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to Playlist", fontSize = 12.sp, color = JnmfTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
