package com.meloza.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.meloza.app.MelozaApp
import com.meloza.app.R
import com.meloza.app.data.Track
import com.meloza.app.data.albums
import com.meloza.app.data.artists
import com.meloza.app.ui.components.Artwork
import com.meloza.app.ui.components.TrackRow
import kotlinx.coroutines.launch

/**
 * Generic track list for: album, artist, genre, playlist, mix (daily | history).
 * Playlists are editable: rename, delete, add/remove tracks, move up/down.
 */
@Composable
fun TrackListScreen(app: MelozaApp, nav: NavController, kind: String, key: String) {
    val library by app.repository.library.collectAsState()
    val all: List<Track> = (library as? com.meloza.app.data.LibraryState.Ready)?.tracks ?: emptyList()
    val favorites by app.repository.favorites().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val playlistId = if (kind == "playlist") key.toLongOrNull() else null
    val playlistEntries by (playlistId?.let { app.repository.playlistTracks(it) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val playlist by app.repository.playlists().collectAsState(initial = emptyList())
    val playlistMeta = playlist.firstOrNull { it.id == playlistId }

    val history by app.repository.history(200).collectAsState(initial = emptyList())

    val tracks: List<Track> = remember(kind, key, all, playlistEntries, history) {
        when (kind) {
            "album" -> all.filter { it.albumId.toString() == key }
            "artist" -> all.filter { it.artistId.toString() == key }
            "genre" -> emptyList() // resolved via LocalLibrary below
            "playlist" -> app.repository.byIds(playlistEntries.map { it.trackId })
            "mix" -> if (key == "history") app.repository.byIds(history.map { it.trackId }.distinct())
                     else all.shuffled(java.util.Random(java.time.LocalDate.now().toEpochDay())).take(20)
            else -> emptyList()
        }
    }
    var genreTracks by remember { mutableStateOf<List<Track>?>(null) }
    if (kind == "genre") {
        androidx.compose.runtime.LaunchedEffect(key) {
            genreTracks = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.meloza.app.data.LocalLibrary.tracksOfGenre(app.applicationContext, key.toLong())
            }
        }
    }
    val shown = if (kind == "genre") genreTracks ?: emptyList() else tracks

    val title = when (kind) {
        "album" -> all.firstOrNull { it.albumId.toString() == key }?.album
        "artist" -> all.firstOrNull { it.artistId.toString() == key }?.artist
        "playlist" -> playlistMeta?.name
        "mix" -> stringResource(if (key == "history") R.string.lib_history else R.string.section_daily_mix)
        else -> stringResource(R.string.lib_genres)
    } ?: stringResource(R.string.lib_songs)

    var showRename by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back)) }
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            if (kind == "playlist") {
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, stringResource(R.string.action_add_tracks)) }
                IconButton(onClick = { showRename = true }) { Icon(Icons.Default.Edit, stringResource(R.string.action_rename)) }
                IconButton(onClick = {
                    scope.launch { app.repository.deletePlaylist(playlistId!!) }
                    nav.popBackStack()
                }) { Icon(Icons.Default.Delete, stringResource(R.string.action_delete)) }
            }
        }

        if (shown.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(pluralStringResource(R.plurals.track_count, shown.size, shown.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Button(onClick = { app.player.play(shown, 0); nav.navigate("now_playing") }, shape = RoundedCornerShape(24.dp)) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_play))
                }
            }
        }

        if (shown.isEmpty()) {
            CenteredMessage(
                if (kind == "playlist") stringResource(R.string.playlist_empty_tracks)
                else stringResource(R.string.list_empty)
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
                itemsIndexed(shown) { index, t ->
                    val playing = app.player.state.collectAsState().value.current?.id == t.id
                    if (kind == "playlist") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                IconButton(onClick = {
                                    if (index > 0) scope.launch {
                                        app.repository.reorderPlaylist(playlistId!!, shown.map { it.id }.toMutableList()
                                            .also { java.util.Collections.swap(it, index, index - 1) })
                                    }
                                }) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.action_move_up)) }
                                IconButton(onClick = {
                                    if (index < shown.lastIndex) scope.launch {
                                        app.repository.reorderPlaylist(playlistId!!, shown.map { it.id }.toMutableList()
                                            .also { java.util.Collections.swap(it, index, index + 1) })
                                    }
                                }) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.action_move_down)) }
                            }
                            TrackRow(
                                track = t,
                                isFavorite = t.id in favorites,
                                isPlaying = playing,
                                onClick = { app.player.play(shown, index); nav.navigate("now_playing") },
                                onFavorite = { app.appScope.launch { app.repository.toggleFavorite(t.id) } },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                scope.launch { app.repository.removeFromPlaylist(playlistId!!, t.id) }
                            }) { Icon(Icons.Default.Remove, stringResource(R.string.action_remove)) }
                        }
                    } else {
                        TrackRow(
                            track = t,
                            isFavorite = t.id in favorites,
                            isPlaying = playing,
                            onClick = { app.player.play(shown, index); nav.navigate("now_playing") },
                            onFavorite = { app.appScope.launch { app.repository.toggleFavorite(t.id) } },
                        )
                    }
                }
            }
        }
    }

    if (showRename && playlistId != null) {
        NameDialog(
            title = stringResource(R.string.action_rename),
            initial = playlistMeta?.name ?: "",
            confirm = stringResource(R.string.action_save),
            onDismiss = { showRename = false },
            onConfirm = { name ->
                if (name.isNotBlank()) scope.launch { app.repository.renamePlaylist(playlistId, name) }
                showRename = false
            },
        )
    }

    if (showAdd && playlistId != null) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.action_add_tracks)) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().height(420.dp)) {
                    itemsIndexed(all) { _, t ->
                        val inList = playlistEntries.any { it.trackId == t.id }
                        Row(Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                if (inList) app.repository.removeFromPlaylist(playlistId, t.id)
                                else app.repository.addToPlaylist(playlistId, t.id)
                            }
                        }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = inList, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.artist, style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.action_done)) } },
        )
    }
}
