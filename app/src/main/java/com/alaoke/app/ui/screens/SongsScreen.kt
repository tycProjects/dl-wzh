package com.alaoke.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alaoke.app.data.Song
import com.alaoke.app.ui.AlaokeViewModel
import com.alaoke.app.ui.components.AddToQueueDialog
import com.alaoke.app.ui.components.EmptyState
import com.alaoke.app.ui.components.SongRow

@Composable
fun SongsScreen(
    viewModel: AlaokeViewModel,
    onOpenPlayer: () -> Unit,
    onGoToYouTubeSearch: () -> Unit
) {
    val allSongs by viewModel.songs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    var query by remember { mutableStateOf("") }
    var queueTarget by remember { mutableStateOf<Song?>(null) }

    val filtered = remember(allSongs, query) {
        if (query.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Song Library", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Titles you know — tap to search YouTube for the karaoke video.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            placeholder = { Text("Search title or artist") },
            leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )

        if (filtered.isEmpty()) {
            EmptyState("No songs match \"$query\". Try YouTube Search instead.")
            Button(onClick = onGoToYouTubeSearch, modifier = Modifier.fillMaxWidth()) {
                Text("Search YouTube")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { song ->
                    val isFav = favorites.any { it.songTitle == song.title && it.artist == song.artist }
                    SongRow(
                        title = song.title,
                        artist = song.artist,
                        thumbnailUrl = song.thumbnailUrl,
                        isFavorite = isFav,
                        onFavoriteClick = {
                            viewModel.toggleFavorite(song.title, song.artist, song.thumbnailUrl, song.youtubeVideoId, isFav)
                        },
                        onQueueClick = { queueTarget = song },
                        onPlayClick = {
                            if (song.youtubeVideoId != null) {
                                viewModel.playNow(song.title, song.artist, song.thumbnailUrl, song.youtubeVideoId)
                                onOpenPlayer()
                            } else {
                                onGoToYouTubeSearch()
                            }
                        },
                        onClick = {
                            if (song.youtubeVideoId != null) {
                                viewModel.playNow(song.title, song.artist, song.thumbnailUrl, song.youtubeVideoId)
                                onOpenPlayer()
                            } else {
                                onGoToYouTubeSearch()
                            }
                        }
                    )
                }
            }
        }
    }

    queueTarget?.let { song ->
        AddToQueueDialog(
            viewModel = viewModel,
            songTitle = song.title,
            artist = song.artist,
            thumbnailUrl = song.thumbnailUrl,
            youtubeVideoId = song.youtubeVideoId,
            onDismiss = { queueTarget = null }
        )
    }
}
