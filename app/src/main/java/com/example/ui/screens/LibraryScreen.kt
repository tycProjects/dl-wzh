package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.model.SongSortOption
import com.example.ui.components.FastScrollMusicList
import com.example.ui.components.JnmfHeaderTitle
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfTheme
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier,
    onPlaySortedQueue: ((List<Song>, Int) -> Unit)? = null
) {
    val colors = JnmfTheme.colors
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedBeatFilter by remember { mutableStateOf<BeatCategory?>(null) }

    // Persistent sorting selection across reloads
    var selectedSortOption by rememberSaveable(
        stateSaver = Saver(
            save = { it.name },
            restore = { try { SongSortOption.valueOf(it) } catch (e: Exception) { SongSortOption.A_TO_Z } }
        )
    ) { mutableStateOf(SongSortOption.A_TO_Z) }

    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Filter and sort songs in memory (no disk modification)
    val filteredAndSortedSongs = remember(songs, searchQuery, selectedBeatFilter, selectedSortOption) {
        val filtered = songs.filter { song ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                song.title.contains(searchQuery, ignoreCase = true) ||
                        song.artist.contains(searchQuery, ignoreCase = true) ||
                        song.album.contains(searchQuery, ignoreCase = true)
            }
            val matchesBeat = selectedBeatFilter == null || song.beatCategory == selectedBeatFilter
            matchesQuery && matchesBeat
        }
        selectedSortOption.sort(filtered)
    }

    // Scroll to top when sorting order changes
    LaunchedEffect(selectedSortOption) {
        if (filteredAndSortedSongs.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Main Header with official JNMF logo beside "JNMF" text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            JnmfHeaderTitle(subtitle = "INDO REMIX")

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colors.bright,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = onScanDevice,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("scan_device_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan device storage",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onImportFiles,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("import_files_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Import files",
                        tint = colors.bright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Compact Search Bar with cyber styling
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tracks, artist, BPM...", fontSize = 12.5.sp, color = JnmfTextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.primary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = JnmfTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .testTag("library_search_input"),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = JnmfBorder,
                focusedTextColor = JnmfTextPrimary,
                unfocusedTextColor = JnmfTextPrimary,
                focusedContainerColor = JnmfSurfaceElevated,
                unfocusedContainerColor = JnmfSurface
            )
        )

        // Beat Category Filter Chips (Indo Remix Tempo Filter)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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

        // Action & Sort Header: Track count, Sort Picker & Play All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredAndSortedSongs.size} TRACKS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = JnmfTextSecondary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sort Menu Trigger Button
                Box {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("sort_options_button"),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.bright,
                            containerColor = JnmfSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Options",
                            modifier = Modifier.size(13.dp),
                            tint = colors.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedSortOption.shortLabel,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = colors.bright
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = JnmfTextSecondary
                        )
                    }

                    // Sort Options Dropdown Menu (all 6 required sorting options)
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier
                            .background(JnmfSurfaceElevated)
                            .border(1.dp, JnmfBorder, RoundedCornerShape(8.dp))
                    ) {
                        SongSortOption.entries.forEach { option ->
                            val isSelected = option == selectedSortOption
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = option.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.bright else JnmfTextPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = option.subtitle,
                                            color = JnmfTextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = colors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    selectedSortOption = option
                                    sortMenuExpanded = false
                                },
                                modifier = Modifier.testTag("sort_option_${option.id}")
                            )
                        }
                    }
                }

                // Play All Button (plays active sorted and filtered queue)
                if (filteredAndSortedSongs.isNotEmpty()) {
                    Button(
                        onClick = {
                            if (onPlaySortedQueue != null) {
                                onPlaySortedQueue(filteredAndSortedSongs, 0)
                            } else {
                                onPlayAll()
                            }
                        },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("play_all_button"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = JnmfDarkBg
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("PLAY ALL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }
                }
            }
        }

        // Song List with Fast-Scrolling Handle or Empty State
        if (filteredAndSortedSongs.isEmpty()) {
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
            FastScrollMusicList(
                songs = filteredAndSortedSongs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                sortOption = selectedSortOption,
                listState = listState,
                onPlaySong = { song ->
                    if (onPlaySortedQueue != null) {
                        val idx = filteredAndSortedSongs.indexOfFirst { it.id == song.id }
                        onPlaySortedQueue(filteredAndSortedSongs, if (idx != -1) idx else 0)
                    } else {
                        onPlaySong(song)
                    }
                },
                onAddToPlaylist = onAddToPlaylist,
                onKickSong = onKickSong
            )
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
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) tagColor else JnmfTextSecondary
        )
    }
}
