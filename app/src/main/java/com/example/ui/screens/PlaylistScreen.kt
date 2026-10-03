package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.RenamePlaylistDialog
import com.example.ui.theme.MatrixBackground
import com.example.ui.theme.MatrixGreenBright
import com.example.ui.theme.MatrixGreenPrimary
import com.example.ui.theme.MatrixOutline
import com.example.ui.theme.MatrixSurfaceElevated
import com.example.ui.theme.MatrixSurfaceVariant

@Composable
fun PlaylistScreen(
    playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    songsInPlaylist: List<Song>,
    allLibrarySongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSelectPlaylist: (Playlist?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPlaylist: (List<Song>, Int) -> Unit,
    onRemoveSongFromPlaylist: (Long, Long) -> Unit,
    onReorderSongs: (Long, List<Long>) -> Unit,
    onAddSongsToPlaylist: (Long, List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showAddSongsPicker by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                onCreatePlaylist(name)
            }
        )
    }

    playlistToRename?.let { pl ->
        RenamePlaylistDialog(
            initialName = pl.name,
            onDismiss = { playlistToRename = null },
            onConfirm = { newName ->
                playlistToRename = null
                onRenamePlaylist(pl.id, newName)
            }
        )
    }

    playlistToDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = MatrixSurfaceElevated,
            title = { Text("Delete Playlist", color = MatrixGreenBright) },
            text = { Text("Are you sure you want to delete '${pl.name}'? Songs in your library will not be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = pl.id
                        playlistToDelete = null
                        onDeletePlaylist(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showAddSongsPicker && selectedPlaylist != null) {
        AddSongsPickerModal(
            allSongs = allLibrarySongs,
            existingSongIds = songsInPlaylist.map { it.id }.toSet(),
            onDismiss = { showAddSongsPicker = false },
            onAddSelected = { selectedIds ->
                showAddSongsPicker = false
                onAddSongsToPlaylist(selectedPlaylist.id, selectedIds)
            }
        )
    }

    BackHandler(enabled = selectedPlaylist != null) {
        onSelectPlaylist(null)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedPlaylist == null) {
            // View All Playlists List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.JnmfHeaderTitle(subtitle = "PLAYLISTS")

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatrixGreenPrimary,
                            contentColor = MatrixBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("create_playlist_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New", fontWeight = FontWeight.Bold)
                    }
                }

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = MatrixGreenBright,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Playlists Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Create customized playlists to organize your favorite offline tracks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MatrixGreenPrimary,
                                    contentColor = MatrixBackground
                                )
                            ) {
                                Text("Create Playlist")
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(playlists, key = { it.id }) { pl ->
                            PlaylistCardItem(
                                playlist = pl,
                                onClick = { onSelectPlaylist(pl) },
                                onRename = { playlistToRename = pl },
                                onDelete = { playlistToDelete = pl },
                                onPlay = {
                                    onSelectPlaylist(pl)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        } else {
            // Selected Playlist Detail Screen
            PlaylistDetailView(
                playlist = selectedPlaylist,
                songs = songsInPlaylist,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onBack = { onSelectPlaylist(null) },
                onPlayPlaylist = { startIndex -> onPlayPlaylist(songsInPlaylist, startIndex) },
                onRemoveSong = { songId -> onRemoveSongFromPlaylist(selectedPlaylist.id, songId) },
                onMoveUp = { index ->
                    if (index > 0) {
                        val reordered = songsInPlaylist.map { it.id }.toMutableList()
                        val temp = reordered[index]
                        reordered[index] = reordered[index - 1]
                        reordered[index - 1] = temp
                        onReorderSongs(selectedPlaylist.id, reordered)
                    }
                },
                onMoveDown = { index ->
                    if (index < songsInPlaylist.size - 1) {
                        val reordered = songsInPlaylist.map { it.id }.toMutableList()
                        val temp = reordered[index]
                        reordered[index] = reordered[index + 1]
                        reordered[index + 1] = temp
                        onReorderSongs(selectedPlaylist.id, reordered)
                    }
                },
                onOpenAddSongs = { showAddSongsPicker = true },
                onRename = { playlistToRename = selectedPlaylist },
                onDelete = {
                    playlistToDelete = selectedPlaylist
                    onSelectPlaylist(null)
                }
            )
        }
    }
}

@Composable
private fun PlaylistCardItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("playlist_card_${playlist.id}")
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, MatrixOutline, RoundedCornerShape(14.dp)),
        color = MatrixSurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MatrixGreenPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = MatrixGreenBright,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.songCount} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlay, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Playlist",
                        tint = MatrixGreenPrimary
                    )
                }

                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.background(MatrixSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayPlaylist: (Int) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onOpenAddSongs: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Detail Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("playlist_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Playlists",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MatrixGreenBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${songs.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Action Buttons Row (Play All & Add Songs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { if (songs.isNotEmpty()) onPlayPlaylist(0) },
                enabled = songs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MatrixGreenPrimary,
                    contentColor = MatrixBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("play_entire_playlist_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play All", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onOpenAddSongs,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreenBright),
                border = androidx.compose.foundation.BorderStroke(1.dp, MatrixOutline),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_songs_to_playlist_button")
            ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Tracks")
            }
        }

        // List of Songs in Playlist with Reorder and Remove
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Playlist is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tap 'Add Tracks' to populate this playlist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    val isCurrent = currentSong?.id == song.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, if (isCurrent) MatrixGreenPrimary else MatrixOutline, RoundedCornerShape(12.dp)),
                        color = MatrixSurfaceElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MatrixGreenPrimary,
                                modifier = Modifier.width(24.dp)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPlayPlaylist(index) }
                            ) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isCurrent) MatrixGreenBright else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • ${song.formattedDuration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Reorder Controls: Up & Down arrows
                            IconButton(
                                onClick = { onMoveUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) MatrixGreenPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMoveDown(index) },
                                enabled = index < songs.size - 1,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (index < songs.size - 1) MatrixGreenPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Remove button
                            IconButton(
                                onClick = { onRemoveSong(song.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove from Playlist",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun AddSongsPickerModal(
    allSongs: List<Song>,
    existingSongIds: Set<Long>,
    onDismiss: () -> Unit,
    onAddSelected: (List<Long>) -> Unit
) {
    val selectedIds = remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MatrixSurfaceElevated,
        title = { Text("Select Tracks to Add", color = MatrixGreenBright) },
        text = {
            val availableSongs = allSongs.filter { it.id !in existingSongIds }
            if (availableSongs.isEmpty()) {
                Text(
                    "All songs in your library are already in this playlist!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    items(availableSongs) { song ->
                        val isChecked = song.id in selectedIds.value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selectedIds.value
                                    selectedIds.value = if (isChecked) current - song.id else current + song.id
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) MatrixGreenPrimary else MatrixSurfaceVariant)
                                    .border(1.dp, MatrixGreenBright, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MatrixBackground,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(song.title, color = MaterialTheme.colorScheme.onSurface)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddSelected(selectedIds.value.toList()) },
                enabled = selectedIds.value.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MatrixGreenPrimary,
                    contentColor = MatrixBackground
                )
            ) {
                Text("Add Selected (${selectedIds.value.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
