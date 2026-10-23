package com.alaoke.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alaoke.app.ui.AlaokeViewModel

/** Prompts for a singer name (or picks one already used tonight) before adding a song to the queue. */
@Composable
fun AddToQueueDialog(
    viewModel: AlaokeViewModel,
    songTitle: String,
    artist: String,
    thumbnailUrl: String?,
    youtubeVideoId: String?,
    onDismiss: () -> Unit
) {
    val singers by viewModel.singers.collectAsState()
    var singerName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Party Queue") },
        text = {
            Column {
                Text("\"$songTitle\" — $artist")
                OutlinedTextField(
                    value = singerName,
                    onValueChange = { singerName = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    placeholder = { Text("Singer's name") },
                    singleLine = true
                )
                if (singers.isNotEmpty()) {
                    LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                        items(singers) { singer ->
                            AssistChip(
                                onClick = { singerName = singer.name },
                                label = { Text(singer.name) },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val name = singerName.ifBlank { "Guest" }
                viewModel.addSinger(name)
                viewModel.addToQueue(songTitle, artist, thumbnailUrl, youtubeVideoId, name)
                onDismiss()
            }) { Text("Add to Queue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
