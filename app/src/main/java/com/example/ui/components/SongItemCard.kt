package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfBorderGlow
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfSurfaceVariant
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfWarningYellow

@Composable
fun SongItemCard(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayNext: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onKickSong: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val colors = JnmfTheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("song_item_${song.id}")
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (isSelected) colors.borderGlow else JnmfBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        color = if (isSelected) JnmfSurfaceElevated else JnmfSurfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon with cyber glow
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else JnmfSurface)
                    .border(
                        1.dp,
                        if (isSelected) colors.primary else JnmfBorder,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected && isPlaying) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Currently Playing",
                        tint = colors.bright,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Song",
                        tint = if (isSelected) colors.primary else JnmfTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Artist, & Cyber BPM chip
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) colors.bright else JnmfTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${song.artist} • ${song.formattedDuration}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = JnmfTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Beat Category & BPM badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(song.beatCategory.tagColor.copy(alpha = 0.15f))
                            .border(0.5.dp, song.beatCategory.tagColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${song.bpm} BPM",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = song.beatCategory.tagColor
                        )
                    }
                }
            }

            // Options 3-dots button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options for ${song.title}",
                        tint = JnmfTextSecondary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(JnmfSurfaceElevated)
                        .border(1.dp, JnmfBorder, RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = JnmfTextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = colors.primary)
                        },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )

                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            text = { Text("Play Next", color = JnmfTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = colors.primary)
                            },
                            onClick = {
                                menuExpanded = false
                                onPlayNext()
                            }
                        )
                    }

                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from Playlist", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromPlaylist()
                            }
                        )
                    }

                    // KICK SONG: Hides from JNMF only, keeps original file intact on device
                    if (onKickSong != null) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Kick Song", color = JnmfWarningYellow, fontWeight = FontWeight.Bold)
                                    Text("Hide from JNMF (File stays on phone)", color = JnmfTextMuted, fontSize = 10.sp)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Block, contentDescription = null, tint = JnmfWarningYellow)
                            },
                            onClick = {
                                menuExpanded = false
                                onKickSong()
                            }
                        )
                    }
                }
            }
        }
    }
}
