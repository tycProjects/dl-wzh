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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.alaoke.app.BuildConfig
import com.alaoke.app.ui.AlaokeViewModel
import com.alaoke.app.ui.components.EmptyState

@Composable
fun SettingsScreen(viewModel: AlaokeViewModel) {
    val singers by viewModel.singers.collectAsState()
    var newSinger by remember { mutableStateOf("") }

    val apiKeyConfigured = BuildConfig.YOUTUBE_API_KEY.isNotBlank() &&
        BuildConfig.YOUTUBE_API_KEY != "PUT_YOUR_YOUTUBE_DATA_API_KEY_HERE"

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("YouTube Data API", fontWeight = FontWeight.Bold)
                Text(
                    if (apiKeyConfigured) {
                        "✅ API key configured — YouTube Search is ready to use."
                    } else {
                        "⚠️ No API key configured yet. Add YOUTUBE_API_KEY in local.properties " +
                            "(or app/build.gradle.kts) to enable YouTube Search."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Text("Manage Singers", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newSinger,
                onValueChange = { newSinger = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a singer's name") },
                singleLine = true
            )
            IconButton(onClick = {
                viewModel.addSinger(newSinger)
                newSinger = ""
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add singer")
            }
        }

        if (singers.isEmpty()) {
            EmptyState("No singers added yet.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(singers, key = { it.id }) { singer ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(singer.name)
                            IconButton(onClick = { viewModel.removeSinger(singer) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove singer")
                            }
                        }
                    }
                }
            }
        }

        Text("Data", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        OutlinedButton(onClick = { viewModel.clearQueue() }, modifier = Modifier.fillMaxWidth()) {
            Text("Clear Party Queue")
        }

        Text(
            "Alaoke — Don't Just Sing. Own the Stage.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}
