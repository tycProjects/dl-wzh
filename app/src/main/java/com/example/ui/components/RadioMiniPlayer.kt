package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.RadioPlaybackState
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.RadioDjStrings

/**
 * Compact docked Radio DJ mini-player displayed above the bottom navigation bar when Radio
 * owns the active media session and the user navigates to another tab.
 */
@Composable
fun RadioMiniPlayer(
    radioState: RadioPlaybackState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = radioState.currentItem ?: return
    val lang = LocalAppLanguage.current

    val progress = if (radioState.durationMs > 0L) {
        (radioState.currentPositionMs.toFloat() / radioState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val statusBadge = when {
        radioState.isPreviewMode -> RadioDjStrings.statusPreviewing(lang)
        radioState.isPlaying -> RadioDjStrings.statusOnAir(lang)
        else -> RadioDjStrings.statusPaused(lang)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("radio_mini_player")
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = RadioDjStrings.navRadioDj(lang),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(statusBadge)
                            append(" • ")
                            append(RadioDjStrings.itemTypeLabel(item.itemType, lang))
                            if (radioState.activeScheduleName.isNotBlank()) {
                                append(" • ")
                                append(radioState.activeScheduleName)
                            }
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("radio_mini_previous")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = JnmfStrings.previous(lang),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("radio_mini_play_pause")
                ) {
                    Icon(
                        imageVector = if (radioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (radioState.isPlaying) JnmfStrings.pause(lang) else JnmfStrings.play(lang),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("radio_mini_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = JnmfStrings.next(lang),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("radio_mini_stop")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = RadioDjStrings.stopRadio(lang),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
