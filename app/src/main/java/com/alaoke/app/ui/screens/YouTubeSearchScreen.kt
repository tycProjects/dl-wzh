package com.alaoke.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alaoke.app.ui.AlaokeViewModel
import com.alaoke.app.ui.components.AddToQueueDialog
import com.alaoke.app.ui.components.EmptyState
import com.alaoke.app.ui.components.SongRow
import com.alaoke.app.youtube.YouTubeSearchResult

@Composable
fun YouTubeSearchScreen(
    viewModel: AlaokeViewModel,
    onOpenPlayer: () -> Unit
) {
    val query by viewModel.ytQuery.collectAsState()
    val results by viewModel.ytResults.collectAsState()
    val loading by viewModel.ytLoading.collectAsState()
    val error by viewModel.ytError.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var queueTarget by remember { mutableStateOf<YouTubeSearchResult?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("YouTube Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Find karaoke videos using YouTube's search. Playback stays inside YouTube's own player — nothing is ever downloaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setYtQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Song title or artist") },
                singleLine = true
            )
            IconButton(onClick = viewModel::searchYouTube) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }

        when {
            loading -> Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            error != null -> EmptyState("⚠️ $error")

            results.isEmpty() -> EmptyState("Search for a song to see karaoke videos from YouTube.")

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.videoId }) { result ->
                    val isFav = favorites.any { it.youtubeVideoId == result.videoId }
                    SongRow(
                        title = result.title,
                        artist = result.channelTitle,
                        thumbnailUrl = result.thumbnailUrl,
                        isFavorite = isFav,
                        onFavoriteClick = {
                            viewModel.toggleFavorite(result.title, result.channelTitle, result.thumbnailUrl, result.videoId, isFav)
                        },
                        onQueueClick = { queueTarget = result },
                        onPlayClick = {
                            viewModel.playNow(result.title, result.channelTitle, result.thumbnailUrl, result.videoId)
                            onOpenPlayer()
                        },
                        onClick = {
                            viewModel.playNow(result.title, result.channelTitle, result.thumbnailUrl, result.videoId)
                            onOpenPlayer()
                        }
                    )
                }
            }
        }
    }

    queueTarget?.let { result ->
        AddToQueueDialog(
            viewModel = viewModel,
            songTitle = result.title,
            artist = result.channelTitle,
            thumbnailUrl = result.thumbnailUrl,
            youtubeVideoId = result.videoId,
            onDismiss = { queueTarget = null }
        )
    }
}
