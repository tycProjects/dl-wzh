package com.alaoke.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicExternalOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alaoke.app.ui.AlaokeViewModel
import com.alaoke.app.ui.components.EmptyState
import com.alaoke.app.ui.components.SectionHeader
import com.alaoke.app.ui.components.SongRow
import com.alaoke.app.ui.navigation.AlaokeDestination
import com.alaoke.app.ui.theme.AlaokeNeonBlue
import com.alaoke.app.ui.theme.AlaokeNeonPink
import com.alaoke.app.ui.theme.AlaokeNeonPurple

@Composable
fun HomeScreen(
    viewModel: AlaokeViewModel,
    onOpenPlayer: () -> Unit,
    onNavigate: (AlaokeDestination) -> Unit
) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val history by viewModel.history.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { HeroHeader() }

        item {
            QuickActionsRow(
                onSearchYouTube = { onNavigate(AlaokeDestination.YOUTUBE_SEARCH) },
                onOpenQueue = { onNavigate(AlaokeDestination.QUEUE) },
                onOpenSmartParty = { onNavigate(AlaokeDestination.SMART_PARTY) }
            )
        }

        if (nowPlaying != null || queue.isNotEmpty()) {
            item { SectionHeader("On Stage Now") }
            item {
                Card(
                    onClick = onOpenPlayer,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MicExternalOn, contentDescription = null, tint = AlaokeNeonPink)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                nowPlaying?.let { "🎤 ${it.singerName ?: "You"} — ${it.title}" }
                                    ?: "Up next: ${queue.first().singerName} — ${queue.first().songTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                nowPlaying?.artist ?: queue.first().artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Open player", tint = AlaokeNeonBlue)
                    }
                }
            }
        }

        item { SectionHeader("Recently Played") }
        if (history.isEmpty()) {
            item { EmptyState("Songs you play will show up here.") }
        } else {
            items(history.take(8)) { item ->
                SongRow(
                    title = item.songTitle,
                    artist = item.artist,
                    thumbnailUrl = item.thumbnailUrl,
                    onPlayClick = {
                        viewModel.playNow(item.songTitle, item.artist, item.thumbnailUrl, item.youtubeVideoId)
                        onOpenPlayer()
                    }
                )
            }
        }

        item { SectionHeader("Favorites") }
        if (favorites.isEmpty()) {
            item { EmptyState("Tap the heart on any song to save it here.") }
        } else {
            items(favorites.take(8)) { fav ->
                SongRow(
                    title = fav.songTitle,
                    artist = fav.artist,
                    thumbnailUrl = fav.thumbnailUrl,
                    onPlayClick = {
                        viewModel.playNow(fav.songTitle, fav.artist, fav.thumbnailUrl, fav.youtubeVideoId)
                        onOpenPlayer()
                    }
                )
            }
        }
    }
}

@Composable
private fun HeroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(AlaokeNeonPurple, AlaokeNeonPink, AlaokeNeonBlue))
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                "ALAOKE",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "Don't Just Sing. Own the Stage.",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onSearchYouTube: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSmartParty: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(
            listOf(
                Triple("🔎 Find a Song", "Search YouTube karaoke", onSearchYouTube),
                Triple("📋 Party Queue", "See who's up next", onOpenQueue),
                Triple("🎉 Smart Party", "Set the mood", onOpenSmartParty)
            )
        ) { (title, subtitle, onClick) ->
            Card(
                onClick = onClick,
                modifier = Modifier.aspectRatio(1.4f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
