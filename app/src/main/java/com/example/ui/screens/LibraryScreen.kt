package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BeatCategory
import com.example.model.Song
import com.example.ui.components.JnmfHeaderTitle
import com.example.ui.components.SongItemCard
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfBorderGlow
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfSurfaceVariant
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfTheme

@Composable
fun LibraryScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    isScanning: Boolean,
    onPlaySong: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onScanDevice: () -> Unit,
    onImportFiles: () -> Unit,
    onLoadDemoTracks: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onKickSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JnmfTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedBeatFilter by remember { mutableStateOf<BeatCategory?>(null) }

    val filteredSongs = remember(songs, searchQuery, selectedBeatFilter) {
        songs.filter { song ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true) ||
                        song.album.contains(searchQuery, ignoreCase = true)
            }
            val matchesBeat = selectedBeatFilter == null || song.beatCategory == selectedBeatFilter
            matchesQuery && matchesBeat
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Main Header with official JNMF logo beside "JNMF" text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            JnmfHeaderTitle(subtitle = "INDO REMIX")

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colors.bright,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onScanDevice,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("scan_device_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan device storage",
                        tint = colors.primary
                    )
                }

                IconButton(
                    onClick = onImportFiles,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("import_files_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Import files",
                        tint = colors.bright
                    )
                }
            }
        }

        // Search Bar with cyber styling
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Indo Remix tracks, artist, BPM...", fontSize = 13.sp, color = JnmfTextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.primary, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = JnmfTextSecondary)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("library_search_input"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = JnmfBorder,
                focusedTextColor = JnmfTextPrimary,
                unfocusedTextColor = JnmfTextPrimary,
                focusedContainerColor = JnmfSurfaceElevated,
                unfocusedContainerColor = JnmfSurface
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Beat Category Filter Chips (Indo Remix Tempo Filter)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                BeatFilterChip(
                    label = "ALL",
                    isSelected = selectedBeatFilter == null,
                    onClick = { selectedBeatFilter = null }
                )
            }
            items(BeatCategory.entries) { category ->
                BeatFilterChip(
                    label = "${category.displayName.uppercase()} (${category.bpmRange})",
                    isSelected = selectedBeatFilter == category,
                    tagColor = category.tagColor,
                    onClick = {
                        selectedBeatFilter = if (selectedBeatFilter == category) null else category
                    }
                )
            }
        }

        // Action Header: Track count & Play All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredSongs.size} TRACKS ACTIVE",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = JnmfTextSecondary
            )

            if (filteredSongs.isNotEmpty()) {
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("play_all_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = JnmfDarkBg
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PLAY ALL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Song List or Empty State
        if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = JnmfSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(JnmfSurfaceElevated)
                                .border(1.dp, colors.borderGlow, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = colors.bright,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedBeatFilter != null)
                                "NO MATCHING TRACKS"
                            else
                                "NO TRACKS IN LIBRARY",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedBeatFilter != null)
                                "Try resetting search query or beat filters"
                            else
                                "Scan device storage for audio files or load synthesized Indo Remix demo tracks.",
                            color = JnmfTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onScanDevice,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = JnmfDarkBg
                                )
                            ) {
                                Text("Scan Device", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onLoadDemoTracks,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
                            ) {
                                Text("Load Demos")
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 70.dp)
                    .testTag("song_list")
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    val isSelected = currentSong?.id == song.id
                    SongItemCard(
                        song = song,
                        isSelected = isSelected,
                        isPlaying = isPlaying,
                        onClick = { onPlaySong(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onKickSong = { onKickSong(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BeatFilterChip(
    label: String,
    isSelected: Boolean,
    tagColor: androidx.compose.ui.graphics.Color = JnmfTheme.colors.primary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) tagColor.copy(alpha = 0.2f) else JnmfSurface)
            .border(
                1.dp,
                if (isSelected) tagColor else JnmfBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) tagColor else JnmfTextSecondary
        )
    }
}
