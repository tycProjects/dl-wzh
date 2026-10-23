package com.alaoke.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.alaoke.app.ui.components.SongRow
import com.alaoke.app.ui.theme.PartyEvent

@Composable
fun SmartPartyScreen(
    viewModel: AlaokeViewModel,
    onOpenPlayer: () -> Unit
) {
    val selectedEvent by viewModel.selectedEvent.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val backgroundColor by animateColorAsState(
        targetValue = selectedEvent?.accent?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.background,
        label = "smart-party-bg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text("Smart Party Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Pick the occasion — Alaoke sets the mood and suggests songs to match.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(PartyEvent.entries) { event ->
                EventTile(
                    event = event,
                    selected = event == selectedEvent,
                    onClick = { viewModel.selectEvent(if (event == selectedEvent) null else event) }
                )
            }
        }

        if (selectedEvent == null) {
            EmptyState("Select an occasion above to see matching song suggestions.")
        } else {
            val suggestions by viewModel.songsForEvent(selectedEvent!!).collectAsState()
            Text(
                "${selectedEvent!!.emoji} ${selectedEvent!!.displayName} Suggestions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (suggestions.isEmpty()) {
                EmptyState("No suggestions yet for this occasion — search YouTube to add your own.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions, key = { it.id }) { song ->
                        val isFav = favorites.any { it.songTitle == song.title && it.artist == song.artist }
                        SongRow(
                            title = song.title,
                            artist = song.artist,
                            thumbnailUrl = song.thumbnailUrl,
                            isFavorite = isFav,
                            onFavoriteClick = {
                                viewModel.toggleFavorite(song.title, song.artist, song.thumbnailUrl, song.youtubeVideoId, isFav)
                            },
                            onPlayClick = {
                                if (song.youtubeVideoId != null) {
                                    viewModel.playNow(song.title, song.artist, song.thumbnailUrl, song.youtubeVideoId)
                                    onOpenPlayer()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventTile(event: PartyEvent, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) event.accent.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) Brush.linearGradient(listOf(event.accent, event.accent.copy(alpha = 0.4f)))
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(event.emoji, style = MaterialTheme.typography.headlineLarge)
                Text(event.displayName, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
