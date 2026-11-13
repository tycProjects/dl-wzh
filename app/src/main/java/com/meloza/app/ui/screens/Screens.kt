package com.meloza.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.meloza.app.MelozaApp
import com.meloza.app.R
import com.meloza.app.data.Album
import com.meloza.app.data.Artist
import com.meloza.app.data.LibraryState
import com.meloza.app.data.LocalLibrary
import com.meloza.app.data.Track
import com.meloza.app.data.albums
import com.meloza.app.data.artists
import com.meloza.app.data.db.PlaylistEntity
import com.meloza.app.ui.audioPermission
import com.meloza.app.ui.components.AlbumCard
import com.meloza.app.ui.components.Artwork
import com.meloza.app.ui.components.TrackRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable as clickableNoop

// ------------------------------------------------------------------ shared states

/** Shows loading / permission / empty / error states, or the content for ready libraries. */
@Composable
fun LibraryGate(
    app: MelozaApp,
    onGrant: () -> Unit,
    content: @Composable (List<Track>) -> Unit,
) {
    val state by app.repository.library.collectAsState()
    when (val s = state) {
        LibraryState.Loading -> CenteredMessage(stringResource(R.string.state_loading), showSpinner = true)
        LibraryState.NoPermission -> EmptyState(
            title = stringResource(R.string.permission_title),
            text = stringResource(R.string.permission_text),
            action = stringResource(R.string.action_grant), onAction = onGrant,
        )
        LibraryState.Empty -> EmptyState(
            title = stringResource(R.string.empty_title),
            text = stringResource(R.string.empty_text),
            action = stringResource(R.string.action_retry),
            onAction = { app.appScope.launch { app.repository.refresh(true) } },
        )
        is LibraryState.Error -> EmptyState(
            title = stringResource(R.string.error_title), text = s.message,
            action = stringResource(R.string.action_retry),
            onAction = { app.appScope.launch { app.repository.refresh(true) } },
        )
        is LibraryState.Ready -> content(s.tracks)
    }
}

@Composable
fun CenteredMessage(text: String, showSpinner: Boolean = false) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (showSpinner) CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyState(title: String, text: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAction, shape = RoundedCornerShape(24.dp)) { Text(action) }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp))
}

// ------------------------------------------------------------------ Home

@Composable
fun HomeScreen(app: MelozaApp, nav: NavController, hasPermission: Boolean, onGrant: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val history by app.repository.history(60).collectAsState(initial = emptyList())
    val favorites by app.repository.favorites().collectAsState(initial = emptyList())

    LibraryGate(app, onGrant) { tracks ->
        val albums = remember(tracks) { tracks.albums() }
        val artists = remember(tracks) { tracks.artists() }
        val recentTracks = remember(history, tracks) {
            history.map { it.trackId }.distinct().take(10).let { app.repository.byIds(it) }
        }
        val popularAlbums = remember(history, albums) {
            val counts = history.groupingBy { it.trackId }.eachCount()
            albums.sortedByDescending { a ->
                tracks.filter { it.albumId == a.id }.sumOf { counts[it.id] ?: 0 }
            }
        }
        val newReleases = remember(tracks, albums) {
            albums.sortedByDescending { a -> tracks.filter { it.albumId == a.id }.maxOf { it.dateAdded } }
        }
        val favoriteArtists = remember(favorites, tracks, artists) {
            val favTracks = tracks.filter { it.id in favorites }
            artists.sortedByDescending { a -> favTracks.count { it.artistId == a.id } }.take(10)
        }
        val dailyMix = remember(tracks) {
            val seed = java.time.LocalDate.now().toEpochDay()
            tracks.shuffled(java.util.Random(seed)).take(20)
        }

        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 180.dp),
        ) {
            item {
                Column(Modifier.padding(20.dp)) {
                    Text(greeting(), style = MaterialTheme.typography.displaySmall)
                    Text(
                        stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (recentTracks.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.section_continue)) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(recentTracks) { index, t ->
                            AlbumCard(Album(t.albumId, t.title, t.artist, t.artworkUri, 1), onClick = {
                                app.player.play(recentTracks, index)
                                nav.navigate("now_playing")
                            }, width = 140.dp)
                        }
                    }
                }
            }
            item {
                DailyMixCard(
                    tracks = dailyMix,
                    onPlay = { app.player.play(dailyMix, 0); nav.navigate("now_playing") },
                    onOpen = { nav.navigate("list/mix/daily") },
                )
            }
            item { SectionTitle(stringResource(R.string.section_new_releases)) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(newReleases.take(12)) { a ->
                        AlbumCard(a, onClick = { nav.navigate("list/album/${a.id}") })
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.section_popular_albums)) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(popularAlbums.take(12)) { a ->
                        AlbumCard(a, onClick = { nav.navigate("list/album/${a.id}") })
                    }
                }
            }
            if (favoriteArtists.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.section_favorite_artists)) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(favoriteArtists) { a ->
                            Column(Modifier.width(96.dp).clickable { nav.navigate("list/artist/${a.id}") },
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Artwork(tracks.first { it.artistId == a.id }.artworkUri, Modifier.size(96.dp), CircleShape)
                                Spacer(Modifier.height(6.dp))
                                Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.section_genres)) }
            item { GenreChips(nav) }
            item {
                Text(
                    stringResource(R.string.home_local_note),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

@Composable
private fun DailyMixCard(tracks: List<Track>, onPlay: () -> Unit, onOpen: () -> Unit) {
    if (tracks.isEmpty()) return
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.section_daily_mix), style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    tracks.take(3).joinToString(" · ") { it.artist },
                    style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledIcon(onClick = onPlay)
        }
    }
}

@Composable
private fun FilledIcon(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_play),
            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun GenreChips(nav: NavController) {
    val context = LocalContext.current
    val genres by produceState(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { runCatching { LocalLibrary.genres(context) }.getOrDefault(emptyList()) }
    }
    if (genres.isEmpty()) {
        Text(stringResource(R.string.genres_none), modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(genres) { g ->
            FilterChip(selected = false, onClick = { nav.navigate("list/genre/${g.id}") },
                label = { Text(g.name) })
        }
    }
}

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when {
        h < 5 -> R.string.greeting_night
        h < 12 -> R.string.greeting_morning
        h < 18 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }.let { id -> ctxString(id) }
}

// Helper used by greeting(); resolved via the application context.
private fun ctxString(id: Int): String = MelozaApp.instance.getString(id)

// ------------------------------------------------------------------ Search

@Composable
fun SearchScreen(app: MelozaApp, nav: NavController) {
    var query by remember { mutableStateOf("") }
    val library by app.repository.library.collectAsState()
    val ready = library as? LibraryState.Ready
    val results = remember(query, library) { app.repository.search(query) }
    val artistResults = remember(results) { results.artists().take(20) }
    val albumResults = remember(results) { results.albums().take(20) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(stringResource(R.string.nav_search), style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(20.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Clear, stringResource(R.string.action_clear))
                }
            },
        )
        Spacer(Modifier.height(12.dp))

        when {
            ready == null -> Unit
            query.isBlank() -> Text(
                stringResource(R.string.search_idle), modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            results.isEmpty() -> Text(
                stringResource(R.string.search_empty, query), modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
                if (artistResults.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_artists)) }
                    items(artistResults) { a ->
                        Row(Modifier.fillMaxWidth().clickable { nav.navigate("list/artist/${a.id}") }
                            .padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(a.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(pluralStringResource(R.plurals.track_count, a.trackCount, a.trackCount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (albumResults.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.search_albums)) }
                    items(albumResults) { a ->
                        Row(Modifier.fillMaxWidth().clickable { nav.navigate("list/album/${a.id}") }
                            .padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Artwork(a.artworkUri, Modifier.size(52.dp), RoundedCornerShape(14.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(a.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
                item { SectionTitle(stringResource(R.string.search_songs)) }
                itemsIndexed(results.take(200)) { index, t ->
                    TrackRow(
                        track = t,
                        isFavorite = false,
                        isPlaying = app.player.state.collectAsState().value.current?.id == t.id,
                        onClick = { app.player.play(results, index); nav.navigate("now_playing") },
                        onFavorite = { },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Library

private enum class LibTab(val labelRes: Int) {
    Songs(R.string.lib_songs), Albums(R.string.lib_albums), Artists(R.string.lib_artists),
    Genres(R.string.lib_genres), Playlists(R.string.lib_playlists), Favorites(R.string.lib_favorites),
    Recent(R.string.lib_recent), History(R.string.lib_history),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(app: MelozaApp, nav: NavController) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = LibTab.entries
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(stringResource(R.string.nav_library), style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(20.dp))
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t.labelRes)) })
            }
        }
        LibraryGate(app, onGrant = {}) { tracks ->
            when (tabs[tab]) {
                LibTab.Songs -> TrackListBody(app, nav, tracks)
                LibTab.Albums -> AlbumGrid(tracks.albums()) { nav.navigate("list/album/$it") }
                LibTab.Artists -> ArtistList(tracks.artists()) { nav.navigate("list/artist/$it") }
                LibTab.Genres -> GenreList(nav)
                LibTab.Playlists -> PlaylistsTab(app, nav)
                LibTab.Favorites -> FavoritesTab(app, nav, tracks)
                LibTab.Recent -> RecentTab(app, nav, tracks)
                LibTab.History -> HistoryTab(app, nav, tracks)
            }
        }
    }
}

@Composable
fun TrackListBody(app: MelozaApp, nav: NavController, tracks: List<Track>) {
    val favorites by app.repository.favorites().collectAsState(initial = emptyList())
    val playing by app.player.state.collectAsState()
    LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
        itemsIndexed(tracks) { index, t ->
            TrackRow(
                track = t,
                isFavorite = t.id in favorites,
                isPlaying = playing.current?.id == t.id,
                onClick = { app.player.play(tracks, index); nav.navigate("now_playing") },
                onFavorite = { app.appScope.launch { app.repository.toggleFavorite(t.id) } },
            )
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>, onOpen: (Long) -> Unit) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums) { a -> AlbumCard(a, onClick = { onOpen(a.id) }, width = 150.dp) }
    }
}

@Composable
private fun ArtistList(artists: List<Artist>, onOpen: (Long) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
        items(artists) { a ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(a.id) }.padding(20.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(a.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.track_count, a.trackCount, a.trackCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GenreList(nav: NavController) {
    val context = LocalContext.current
    val genres by produceState<List<com.meloza.app.data.Genre>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { runCatching { LocalLibrary.genres(context) }.getOrDefault(emptyList()) }
    }
    val list = genres
    if (list == null) {
        CenteredMessage(stringResource(R.string.state_loading), showSpinner = true)
        return
    }
    if (list.isEmpty()) {
        CenteredMessage(stringResource(R.string.genres_none))
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
        items(list) { g ->
            Row(Modifier.fillMaxWidth().clickable { nav.navigate("list/genre/${g.id}") }.padding(20.dp, 14.dp)) {
                Text(g.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(pluralStringResource(R.plurals.track_count, g.trackCount, g.trackCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlaylistsTab(app: MelozaApp, nav: NavController) {
    val playlists by app.repository.playlists().collectAsState(initial = emptyList())
    var showCreate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            CenteredMessage(stringResource(R.string.playlists_empty))
        } else LazyColumn(contentPadding = PaddingValues(bottom = 200.dp)) {
            items(playlists) { p ->
                Row(Modifier.fillMaxWidth().clickable { nav.navigate("list/playlist/${p.id}") }.padding(20.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QueueMusic, null)
                    Spacer(Modifier.width(16.dp))
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        FloatingActionButton(
            onClick = { showCreate = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).padding(bottom = 96.dp),
            shape = RoundedCornerShape(24.dp),
        ) { Icon(Icons.Default.Add, stringResource(R.string.playlist_new)) }
    }
    if (showCreate) {
        NameDialog(
            title = stringResource(R.string.playlist_new),
            initial = "",
            confirm = stringResource(R.string.action_create),
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                if (name.isNotBlank()) scope.launch { app.repository.createPlaylist(name) }
                showCreate = false
            },
        )
    }
}

@Composable
private fun FavoritesTab(app: MelozaApp, nav: NavController, tracks: List<Track>) {
    val favIds by app.repository.favorites().collectAsState(initial = emptyList())
    val favs = remember(favIds, tracks) { tracks.filter { it.id in favIds } }
    if (favs.isEmpty()) CenteredMessage(stringResource(R.string.favorites_empty))
    else TrackListBody(app, nav, favs)
}

@Composable
private fun RecentTab(app: MelozaApp, nav: NavController, tracks: List<Track>) {
    val recent = remember(tracks) { tracks.sortedByDescending { it.dateAdded }.take(100) }
    TrackListBody(app, nav, recent)
}

@Composable
private fun HistoryTab(app: MelozaApp, nav: NavController, tracks: List<Track>) {
    val history by app.repository.history(200).collectAsState(initial = emptyList())
    val played = remember(history, tracks) { app.repository.byIds(history.map { it.trackId }.distinct()) }
    if (played.isEmpty()) CenteredMessage(stringResource(R.string.history_empty))
    else TrackListBody(app, nav, played)
}

@Composable
fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true,
                placeholder = { Text(stringResource(R.string.playlist_name)) })
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// ------------------------------------------------------------------ Profile

@Composable
fun ProfileScreen(app: MelozaApp, nav: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("meloza_prefs", Context.MODE_PRIVATE) }
    var name by remember { mutableStateOf(prefs.getString("profile_name", null) ?: "") }
    var showEdit by remember { mutableStateOf(false) }
    val plays by app.repository.playCount().collectAsState(initial = 0)
    val favs by app.repository.favorites().collectAsState(initial = emptyList())
    val history by app.repository.history(500).collectAsState(initial = emptyList())
    val playlists by app.repository.playlists().collectAsState(initial = emptyList())
    var theme by remember { mutableIntStateOf(prefs.getInt("theme", 0)) }
    var accent by remember { mutableStateOf(prefs.getBoolean("artwork_accent", true)) }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 180.dp)) {
        item {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showEdit = true }, contentAlignment = Alignment.Center) {
                    Text(name.firstOrNull()?.uppercase() ?: "M", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(name.ifBlank { stringResource(R.string.profile_default_name) }, style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.profile_edit_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showEdit = true }) { Icon(Icons.Default.Edit, stringResource(R.string.action_rename)) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(plays.toString(), stringResource(R.string.profile_plays), Modifier.weight(1f))
                StatCard(favs.size.toString(), stringResource(R.string.profile_favorites), Modifier.weight(1f))
                StatCard(playlists.size.toString(), stringResource(R.string.profile_playlists), Modifier.weight(1f))
            }
        }
        item { SectionTitle(stringResource(R.string.profile_top_artists)) }
        item {
            val tracksById = remember(history) { app.repository.byIds(history.map { it.trackId }) }
            val top = tracksById.groupingBy { it.artist }.eachCount().entries.sortedByDescending { it.value }.take(5)
            if (top.isEmpty()) Text(stringResource(R.string.profile_no_history), modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Column(Modifier.padding(horizontal = 20.dp)) {
                top.forEach { Text("${it.key} · ${it.value}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp)) }
            }
        }
        item { SectionTitle(stringResource(R.string.settings_title)) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                listOf(
                    0 to R.string.theme_system, 1 to R.string.theme_light, 2 to R.string.theme_dark,
                ).forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().clickable {
                        theme = value; prefs.edit().putInt("theme", value).apply()
                    }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme == value, onClick = null)
                        Text(stringResource(label))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clickable {
                    accent = !accent; prefs.edit().putBoolean("artwork_accent", accent).apply()
                }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_artwork_accent), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.settings_artwork_accent_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(checked = accent, onCheckedChange = null)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.settings_quality_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.online_not_connected), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { nav.navigate("list/mix/history") }) { Text(stringResource(R.string.profile_open_history)) }
            }
        }
    }

    if (showEdit) {
        NameDialog(
            title = stringResource(R.string.profile_name),
            initial = name,
            confirm = stringResource(R.string.action_save),
            onDismiss = { showEdit = false },
            onConfirm = { new ->
                name = new.trim()
                prefs.edit().putString("profile_name", name).apply()
                showEdit = false
            },
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
