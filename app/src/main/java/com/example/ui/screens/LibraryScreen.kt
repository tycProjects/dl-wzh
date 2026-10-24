package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Song
import com.example.data.repository.SortOption
import com.example.playback.MusicController
import com.example.playback.PlaybackState
import com.example.data.repository.AppLanguage
import com.example.ui.components.CompactSongRow
import com.example.ui.components.FastScrollHandle
import com.example.ui.components.JnmfGlobeLogo
import com.example.ui.components.JnmfTaskProgressBar
import com.example.ui.components.TaskProgressState
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    songs: List<Song>,
    playbackState: PlaybackState,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    currentSort: SortOption,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    favoriteSongIds: Set<Long>,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onHideSong: (Song) -> Unit,
    language: AppLanguage = LocalAppLanguage.current,
    musicScanState: TaskProgressState = TaskProgressState.IDLE,
    onDismissProgress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showSortDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar & Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = JnmfStrings.searchTracksCountPlaceholder(songs.size, language),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = JnmfStrings.search(language),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = JnmfStrings.clearSearch(language),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("search_field")
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Sort Button
                IconButton(
                    onClick = { showSortDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("sort_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = JnmfStrings.sortBy(language),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Refresh Button (prevents duplicate scans while already scanning)
                IconButton(
                    onClick = {
                        if (!musicScanState.isRunning) {
                            onRefresh()
                        }
                    },
                    enabled = !musicScanState.isRunning,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("refresh_button")
                ) {
                    if (musicScanState.isRunning) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = JnmfStrings.refreshLibrary(language),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Consistent Loading Progress System (1% to 100% or indeterminate)
            JnmfTaskProgressBar(
                state = musicScanState,
                onDismiss = onDismissProgress
            )

            // Permission Request State if not granted
            if (!hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = JnmfStrings.audioAccessRequired(language),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = JnmfStrings.permissionDesc(language),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("grant_permission_button")
                            ) {
                                Text(JnmfStrings.grantAudioAccess(language), color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                return@Column
            }

            // Empty Library State
            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        JnmfGlobeLogo(
                            size = 80.dp,
                            animated = musicScanState.isRunning,
                            showEqualizerWaves = true,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                JnmfStrings.noTracksMatchQuery(searchQuery, language)
                            } else {
                                JnmfStrings.noSongsFound(language)
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            enabled = !musicScanState.isRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(JnmfStrings.rescanStorage(language), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                return@Column
            }

            // Compact Song List + Fast Scroll Handle
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 24.dp) // Leave space for fast-scroll handle
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { _, song ->
                        val isCurrent = playbackState.currentSong?.id == song.id
                        CompactSongRow(
                            song = song,
                            isPlaying = playbackState.isPlaying,
                            isCurrentTrack = isCurrent,
                            isFavorite = favoriteSongIds.contains(song.id),
                            onClick = {
                                MusicController.playSong(song, songs)
                            },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onHideSong = { onHideSong(song) },
                            language = language
                        )
                    }
                }

                // Fast Scroll Handle
                FastScrollHandle(
                    onLetterSelected = { letter ->
                        val targetIndex = if (letter == '#') {
                            songs.indexOfFirst { !it.title.first().isLetter() }
                        } else {
                            songs.indexOfFirst { it.title.startsWith(letter, ignoreCase = true) }
                        }
                        if (targetIndex >= 0) {
                            scope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // Sort Dialog
        if (showSortDialog) {
            Dialog(onDismissRequest = { showSortDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = JnmfStrings.sortTracksTitle(language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SortOption.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSortSelected(option)
                                        showSortDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentSort == option,
                                    onClick = {
                                        onSortSelected(option)
                                        showSortDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = JnmfStrings.sortOptionLabel(option, language),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
