package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistTrackEntity
import com.example.data.model.Song
import com.example.playback.MusicController
import com.example.playback.PlaybackState
import com.example.playback.QueueSourceType
import com.example.ui.components.CompactSongRow
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen(
    playlists: List<PlaylistEntity>,
    favoriteSongs: List<Song>,
    allSongs: List<Song>,
    playbackState: PlaybackState,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onRemoveSongFromPlaylist: (Long, Long) -> Unit,
    onMoveTrack: (Long, Int, Int) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    getPlaylistTrackIds: (Long) -> List<Long>,
    onRequestAddToPlaylist: (Song) -> Unit = {},
    getPlaylistTracks: ((Long) -> List<PlaylistTrackEntity>)? = null,
    selectedPlaylistId: Long? = null,
    onSelectedPlaylistIdChange: ((Long?) -> Unit)? = null,
    viewingFavorites: Boolean? = null,
    onViewingFavoritesChange: ((Boolean) -> Unit)? = null,
    isSongFileAvailable: (Song) -> Boolean = { true },
    onCreatePlaylistSuspend: (suspend (String) -> Boolean)? = null,
    onRenamePlaylistSuspend: (suspend (Long, String) -> Boolean)? = null,
    onMissingSongTapped: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlaylistScreenContent(
        playlists = playlists,
        favoriteSongs = favoriteSongs,
        allSongs = allSongs,
        currentSongId = playbackState.currentSong?.id,
        isPlaying = playbackState.isPlaying,
        onCreatePlaylist = onCreatePlaylist,
        onRenamePlaylist = onRenamePlaylist,
        onDeletePlaylist = onDeletePlaylist,
        onAddSongToPlaylist = onAddSongToPlaylist,
        onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
        onMoveTrack = onMoveTrack,
        onToggleFavorite = onToggleFavorite,
        getPlaylistTrackIds = getPlaylistTrackIds,
        onRequestAddToPlaylist = onRequestAddToPlaylist,
        getPlaylistTracks = getPlaylistTracks,
        selectedPlaylistId = selectedPlaylistId,
        onSelectedPlaylistIdChange = onSelectedPlaylistIdChange,
        viewingFavorites = viewingFavorites,
        onViewingFavoritesChange = onViewingFavoritesChange,
        isSongFileAvailable = isSongFileAvailable,
        onCreatePlaylistSuspend = onCreatePlaylistSuspend,
        onRenamePlaylistSuspend = onRenamePlaylistSuspend,
        onMissingSongTapped = onMissingSongTapped,
        modifier = modifier
    )
}

@Composable
fun PlaylistScreenContent(
    playlists: List<PlaylistEntity>,
    favoriteSongs: List<Song>,
    allSongs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onRemoveSongFromPlaylist: (Long, Long) -> Unit,
    onMoveTrack: (Long, Int, Int) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    getPlaylistTrackIds: (Long) -> List<Long>,
    onRequestAddToPlaylist: (Song) -> Unit = {},
    getPlaylistTracks: ((Long) -> List<PlaylistTrackEntity>)? = null,
    selectedPlaylistId: Long? = null,
    onSelectedPlaylistIdChange: ((Long?) -> Unit)? = null,
    viewingFavorites: Boolean? = null,
    onViewingFavoritesChange: ((Boolean) -> Unit)? = null,
    isSongFileAvailable: (Song) -> Boolean = { true },
    onCreatePlaylistSuspend: (suspend (String) -> Boolean)? = null,
    onRenamePlaylistSuspend: (suspend (Long, String) -> Boolean)? = null,
    onMissingSongTapped: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val favoriteSongIdSet = remember(favoriteSongs) {
        favoriteSongs.mapTo(HashSet(favoriteSongs.size.coerceAtLeast(4))) { it.id }
    }

    var internalSelectedId by rememberSaveable { mutableLongStateOf(-1L) }
    var internalViewingFavorites by rememberSaveable { mutableStateOf(false) }

    val activePlaylistId: Long? = if (onSelectedPlaylistIdChange != null) {
        selectedPlaylistId
    } else {
        internalSelectedId.takeIf { it > 0L }
    }

    val setActivePlaylistId: (Long?) -> Unit = { nextId ->
        if (onSelectedPlaylistIdChange != null) {
            onSelectedPlaylistIdChange(nextId)
        } else {
            internalSelectedId = nextId ?: -1L
        }
    }

    val isViewingFavorites: Boolean = viewingFavorites ?: internalViewingFavorites
    val setViewingFavorites: (Boolean) -> Unit = { nextViewing ->
        if (onViewingFavoritesChange != null) {
            onViewingFavoritesChange(nextViewing)
        } else {
            internalViewingFavorites = nextViewing
        }
    }

    // Resolve dynamically from playlists so renames and deletions reflect immediately without stale copies
    val selectedPlaylist: PlaylistEntity? = remember(playlists, activePlaylistId) {
        activePlaylistId?.let { targetId -> playlists.find { it.id == targetId } }
    }

    var showAddSongsSheet by rememberSaveable { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var renameDialogPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteConfirmPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var playlistNameInput by rememberSaveable { mutableStateOf("") }

    val showRenameDialog = remember(playlists, renameDialogPlaylistId) {
        renameDialogPlaylistId?.let { id -> playlists.find { it.id == id } }
    }
    val showDeleteConfirmDialog = remember(playlists, deleteConfirmPlaylistId) {
        deleteConfirmPlaylistId?.let { id -> playlists.find { it.id == id } }
    }

    // Intercept back button to navigate back within playlist hierarchy instead of closing app
    BackHandler(enabled = selectedPlaylist != null || isViewingFavorites || showAddSongsSheet) {
        if (showAddSongsSheet) {
            showAddSongsSheet = false
        } else if (isViewingFavorites) {
            setViewingFavorites(false)
        } else {
            setActivePlaylistId(null)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isViewingFavorites) {
            // Favorites View
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { setViewingFavorites(false) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = JnmfStrings.back(lang),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${JnmfStrings.favoritesTitle(lang)} (${favoriteSongs.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (favoriteSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = JnmfStrings.emptyFavorites(lang),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = favoriteSongs,
                            key = { it.id },
                            contentType = { "favorite_song_row" }
                        ) { song ->
                            val isCurrent = currentSongId == song.id
                            CompactSongRow(
                                song = song,
                                isPlaying = isCurrent && isPlaying,
                                isCurrentTrack = isCurrent,
                                isFavorite = true,
                                onClick = {
                                    MusicController.playSong(
                                        song = song,
                                        newQueue = favoriteSongs,
                                        sourceType = QueueSourceType.FAVORITES
                                    )
                                },
                                onToggleFavorite = { onToggleFavorite(song) },
                                onAddToPlaylist = { onRequestAddToPlaylist(song) },
                                onHideSong = { onToggleFavorite(song) },
                                removeActionLabel = JnmfStrings.removeFromPlaylist(lang)
                            )
                        }
                    }
                }
            }
        } else if (selectedPlaylist != null) {
            // Playlist Detail View
            val pl = selectedPlaylist
            val orderedTracks: List<PlaylistTrackEntity> = remember(pl.id, getPlaylistTracks, getPlaylistTrackIds) {
                getPlaylistTracks?.invoke(pl.id)
                    ?: getPlaylistTrackIds(pl.id).mapIndexed { index, id ->
                        PlaylistTrackEntity(
                            playlistId = pl.id,
                            songId = id,
                            orderIndex = index
                        )
                    }
            }
            val trackIdSet = remember(orderedTracks) {
                orderedTracks.mapTo(HashSet(orderedTracks.size.coerceAtLeast(4))) { it.songId }
            }
            val songsById = remember(allSongs) { allSongs.associateBy { it.id } }

            // Map tracks to live songs or preserved missing-file entries without deleting the playlist
            val playlistItems = remember(orderedTracks, songsById, lang) {
                orderedTracks.map { track ->
                    val found = songsById[track.songId]
                    val available = found != null && isSongFileAvailable(found)
                    if (available && found != null) {
                        found to true
                    } else {
                        val preservedTitle = found?.title?.takeIf { it.isNotBlank() }
                            ?: track.savedTitle.takeIf { it.isNotBlank() }
                            ?: track.savedFileName.substringBeforeLast('.').takeIf { it.isNotBlank() }
                            ?: JnmfStrings.missingTrackTitle(track.songId, lang)
                        val artistPrefix = (found?.artist ?: track.savedArtist)
                            .takeIf { it.isNotBlank() }
                            ?.let { "$it • " }
                            .orEmpty()
                        Song(
                            id = track.songId,
                            title = preservedTitle,
                            artist = "$artistPrefix${JnmfStrings.missingFileSubtitle(lang)}",
                            album = JnmfStrings.unavailable(lang),
                            durationMs = found?.durationMs ?: 0L,
                            sizeBytes = 0L,
                            dateAdded = 0L,
                            contentUri = Uri.EMPTY,
                            fileName = track.savedFileName
                        ) to false
                    }
                }
            }

            val playableSongs = remember(playlistItems) {
                playlistItems.mapNotNull { if (it.second) it.first else null }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Playlist Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { setActivePlaylistId(null) },
                        modifier = Modifier.testTag("playlist_detail_back")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = JnmfStrings.back(lang),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pl.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = JnmfStrings.savedTracksCount(playlistItems.size, lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Add Songs to this playlist button
                    IconButton(
                        onClick = { showAddSongsSheet = true },
                        modifier = Modifier.testTag("add_songs_to_playlist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = JnmfStrings.addTracks(lang),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Play All button
                    if (playableSongs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val startSong = if (MusicController.state.value.isShuffle) {
                                    playableSongs.random()
                                } else {
                                    playableSongs.first()
                                }
                                MusicController.playSong(
                                    song = startSong,
                                    newQueue = playableSongs,
                                    sourceType = QueueSourceType.PLAYLIST,
                                    sourcePlaylistId = pl.id
                                )
                            },
                            modifier = Modifier.testTag("play_all_playlist")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = JnmfStrings.playAll(lang),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (playlistItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = JnmfStrings.playlistEmpty(lang),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showAddSongsSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(JnmfStrings.addTracksNow(lang), color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(
                            items = playlistItems,
                            key = { _, pair -> pair.first.id },
                            contentType = { _, _ -> "playlist_song_row" }
                        ) { index, pair ->
                            val (song, isAvailable) = pair
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (!isAvailable) {
                                        Row(
                                            modifier = Modifier
                                                .padding(start = 12.dp, top = 6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                .testTag("missing_song_badge_${song.id}"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = JnmfStrings.missingFileBadge(lang),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = JnmfStrings.missingFileBadge(lang),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    val isCurrent = currentSongId == song.id
                                    CompactSongRow(
                                        song = song,
                                        isPlaying = isCurrent && isPlaying,
                                        isCurrentTrack = isCurrent,
                                        isFavorite = favoriteSongIdSet.contains(song.id),
                                        onClick = {
                                            if (isAvailable) {
                                                MusicController.playSong(
                                                    song = song,
                                                    newQueue = playableSongs,
                                                    sourceType = QueueSourceType.PLAYLIST,
                                                    sourcePlaylistId = pl.id
                                                )
                                            } else {
                                                onMissingSongTapped(song)
                                            }
                                        },
                                        onToggleFavorite = { if (isAvailable) onToggleFavorite(song) },
                                        onAddToPlaylist = { if (isAvailable) onRequestAddToPlaylist(song) },
                                        onHideSong = { onRemoveSongFromPlaylist(pl.id, song.id) },
                                        removeActionLabel = JnmfStrings.removeFromPlaylist(lang)
                                    )
                                }

                                // Reordering handles (Move Up & Down)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                onMoveTrack(pl.id, index, index - 1)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("move_up_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = JnmfStrings.moveUp(lang),
                                            tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < playlistItems.size - 1) {
                                                onMoveTrack(pl.id, index, index + 1)
                                            }
                                        },
                                        enabled = index < playlistItems.size - 1,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("move_down_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = JnmfStrings.moveDown(lang),
                                            tint = if (index < playlistItems.size - 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Modal dialog to add tracks to current playlist
            if (showAddSongsSheet) {
                AlertDialog(
                    onDismissRequest = { showAddSongsSheet = false },
                    title = {
                        Text(
                            text = JnmfStrings.addTracksToPlaylistTitle(pl.name, lang),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        val availableToAdd = remember(allSongs, trackIdSet) {
                            allSongs.filter { song -> !trackIdSet.contains(song.id) }
                        }
                        if (availableToAdd.isEmpty()) {
                            Text(
                                text = JnmfStrings.allSongsInPlaylist(lang),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(modifier = Modifier.height(300.dp)) {
                                items(
                                    items = availableToAdd,
                                    key = { it.id },
                                    contentType = { "add_song_option" }
                                ) { song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onAddSongToPlaylist(pl.id, song.id)
                                            }
                                            .padding(vertical = 8.dp)
                                            .testTag("add_song_option_${song.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${song.artist} • ${song.formattedDuration}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = JnmfStrings.add(lang),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showAddSongsSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("add_songs_done_button")
                        ) {
                            Text(JnmfStrings.done(lang), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                )
            }
        } else {
            // Main Playlists Overview
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Favorites Special Card
                    item {
                        Card(
                            onClick = { setViewingFavorites(true) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("favorites_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = JnmfStrings.favoritesTitle(lang),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = JnmfStrings.favoritesTitle(lang),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = JnmfStrings.savedFavoritesSubtitle(favoriteSongs.size, lang),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = JnmfStrings.customPlaylistsHeader(lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }

                    if (playlists.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = JnmfStrings.emptyPlaylists(lang),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                )
                            }
                        }
                    }

                    // User Playlists List
                    items(playlists, key = { it.id }) { playlist ->
                        val count = getPlaylistTracks?.invoke(playlist.id)?.size
                            ?: getPlaylistTrackIds(playlist.id).size
                        Card(
                            onClick = { setActivePlaylistId(playlist.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_card_${playlist.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = JnmfStrings.tracksCount(count, lang),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Rename
                                IconButton(
                                    onClick = {
                                        playlistNameInput = playlist.name
                                        renameDialogPlaylistId = playlist.id
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = JnmfStrings.rename(lang),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = { deleteConfirmPlaylistId = playlist.id },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = JnmfStrings.delete(lang),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // FAB: New Playlist
                FloatingActionButton(
                    onClick = {
                        playlistNameInput = ""
                        showCreateDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("fab_create_playlist")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = JnmfStrings.newPlaylist(lang))
                }
            }
        }

        // Create Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(JnmfStrings.createNewPlaylist(lang), color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text(JnmfStrings.enterPlaylistName(lang)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = playlistNameInput.trim()
                            if (trimmed.isNotBlank()) {
                                if (onCreatePlaylistSuspend != null) {
                                    scope.launch {
                                        if (onCreatePlaylistSuspend(trimmed)) {
                                            showCreateDialog = false
                                        }
                                    }
                                } else {
                                    onCreatePlaylist(trimmed)
                                    showCreateDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("confirm_create_playlist")
                    ) {
                        Text(JnmfStrings.create(lang), color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(JnmfStrings.cancel(lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // Rename Dialog
        if (showRenameDialog != null) {
            val pl = showRenameDialog
            AlertDialog(
                onDismissRequest = { renameDialogPlaylistId = null },
                title = { Text(JnmfStrings.renamePlaylistTitle(lang), color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text(JnmfStrings.enterPlaylistName(lang)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_playlist_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = playlistNameInput.trim()
                            if (trimmed.isNotBlank()) {
                                if (onRenamePlaylistSuspend != null) {
                                    scope.launch {
                                        if (onRenamePlaylistSuspend(pl.id, trimmed)) {
                                            renameDialogPlaylistId = null
                                        }
                                    }
                                } else {
                                    onRenamePlaylist(pl.id, trimmed)
                                    renameDialogPlaylistId = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("confirm_rename_playlist")
                    ) {
                        Text(JnmfStrings.save(lang), color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogPlaylistId = null }) {
                        Text(JnmfStrings.cancel(lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirmDialog != null) {
            val pl = showDeleteConfirmDialog
            AlertDialog(
                onDismissRequest = { deleteConfirmPlaylistId = null },
                title = { Text(JnmfStrings.deletePlaylistTitle(lang), color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Text(
                        JnmfStrings.deletePlaylistConfirmMsg(pl.name, lang),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (activePlaylistId == pl.id) {
                                setActivePlaylistId(null)
                            }
                            onDeletePlaylist(pl.id)
                            deleteConfirmPlaylistId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_playlist")
                    ) {
                        Text(JnmfStrings.delete(lang), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmPlaylistId = null }) {
                        Text(JnmfStrings.cancel(lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}
