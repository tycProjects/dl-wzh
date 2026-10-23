package com.alaoke.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alaoke.app.data.QueueItem
import com.alaoke.app.ui.AlaokeViewModel
import com.alaoke.app.ui.components.EmptyState
import com.alaoke.app.ui.components.SongThumbnail
import com.alaoke.app.ui.theme.AlaokeNeonPink

@Composable
fun PartyQueueScreen(
    viewModel: AlaokeViewModel,
    onOpenPlayer: () -> Unit
) {
    val queue by viewModel.queue.collectAsState()
    val nowPlaying by viewModel.nowPlaying.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Party Queue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("NOW PLAYING", style = MaterialTheme.typography.labelLarge, color = AlaokeNeonPink)
                if (nowPlaying != null) {
                    Text(
                        "\uD83C\uDFA4 ${nowPlaying?.singerName ?: "You"} - ${nowPlaying?.title}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(nowPlaying?.artist.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onOpenPlayer, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Open Player")
                    }
                } else {
                    Text("Nothing on stage yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (queue.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.playNextFromQueue()
                                onOpenPlayer()
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Start Next Singer") }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("UP NEXT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (queue.isNotEmpty()) {
                OutlinedButton(onClick = { viewModel.clearQueue() }) { Text("Clear All") }
            }
        }

        if (queue.isEmpty()) {
            EmptyState("The queue is empty. Add songs from the Songs or YouTube Search tab.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(queue, key = { it.id }) { item ->
                    val index = queue.indexOf(item)
                    QueueRow(
                        index = index + 1,
                        item = item,
                        canMoveUp = index > 0,
                        canMoveDown = index < queue.lastIndex,
                        onMoveUp = {
                            val mutable = queue.toMutableList()
                            mutable.removeAt(index)
                            mutable.add(index - 1, item)
                            viewModel.reorderQueue(mutable)
                        },
                        onMoveDown = {
                            val mutable = queue.toMutableList()
                            mutable.removeAt(index)
                            mutable.add(index + 1, item)
                            viewModel.reorderQueue(mutable)
                        },
                        onRemove = { viewModel.removeFromQueue(item) },
                        onSkipToThis = {
                            val mutable = queue.toMutableList()
                            mutable.removeAt(index)
                            mutable.add(0, item)
                            viewModel.reorderQueue(mutable)
                            viewModel.playNextFromQueue()
                            onOpenPlayer()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    index: Int,
    item: QueueItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onSkipToThis: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("$index.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SongThumbnail(item.thumbnailUrl)
            Column(Modifier.weight(1f)) {
                Text("${item.singerName} - ${item.songTitle}", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(item.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up") }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down") }
            IconButton(onClick = onSkipToThis) { Icon(Icons.Filled.SkipNext, contentDescription = "Play now") }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
        }
    }
}
