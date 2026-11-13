package com.meloza.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.meloza.app.MelozaApp
import com.meloza.app.R
import com.meloza.app.data.Track
import com.meloza.app.playback.PlayerUiState
import com.meloza.app.ui.components.Artwork
import com.meloza.app.ui.components.LiquidBackdrop
import com.meloza.app.ui.components.SoundWave
import com.meloza.app.ui.components.TrackRow
import com.meloza.app.ui.components.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(app: MelozaApp, nav: NavController) {
    val state by app.player.state.collectAsState()
    val favorites by app.repository.favorites().collectAsState(initial = emptyList())
    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showRadio by remember { mutableStateOf(false) }

    val track = state.current
    if (track == null) {
        // Nothing playing: send the user back instead of showing an empty screen.
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    // Live playback position (ExoPlayer does not push ticks; poll while the screen is visible).
    var position by remember { mutableStateOf(state.positionMs) }
    LaunchedEffect(state.isPlaying, track.id) {
        while (true) {
            position = app.player.state.value.positionMs
            delay(if (state.isPlaying) 250 else 1000)
        }
    }
    val duration = state.durationMs.coerceAtLeast(track.durationMs)
    val progress = if (duration > 0) position.toFloat() / duration else 0f

    // Dominant color behind the screen comes from the active theme accent (set in MainActivity).
    val bg = Brush.verticalGradient(
        listOf(colorScheme.primaryContainer.copy(alpha = 0.9f), colorScheme.background),
    )

    Box(Modifier.fillMaxSize().background(bg)) {
        LiquidBackdrop(track.artworkUri)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.action_back))
                }
                Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { showEq = true }) { Icon(Icons.Default.Tune, stringResource(R.string.action_equalizer)) }
                IconButton(onClick = { showQueue = true }) { Icon(Icons.Default.QueueMusic, stringResource(R.string.action_queue)) }
            }

            // Swipe between queue items: the pager is the queue, and swiping changes the track.
            val pagerState = rememberPagerState(
                initialPage = state.currentIndex.coerceAtLeast(0),
                pageCount = { state.queue.size.coerceAtLeast(1) },
            )
            LaunchedEffect(state.currentIndex) {
                if (state.currentIndex >= 0 && pagerState.currentPage != state.currentIndex) {
                    pagerState.animateScrollToPage(state.currentIndex)
                }
            }
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { page ->
                    if (page != state.currentIndex && page in state.queue.indices) app.player.skipTo(page)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 0.dp,
            ) { page ->
                val t = state.queue.getOrNull(page) ?: track
                val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                val scale by animateFloatAsState(
                    targetValue = if (page == pagerState.currentPage) 1f else 0.86f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f), label = "scale",
                )
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale },
                ) {
                    Artwork(t.artworkUri, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(40.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = track.id,
                transitionSpec = {
                    (slideInVertically(spring(stiffness = 400f)) { it / 3 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 3 } + fadeOut())
                },
                label = "title",
            ) { _ ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    Text(track.artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                        color = colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(16.dp))
            SoundWave(state.isPlaying, colorScheme.primary, Modifier.fillMaxWidth().height(40.dp))

            Spacer(Modifier.height(8.dp))
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { app.player.seekTo((it * duration).toLong()); position = (it * duration).toLong() },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(position), style = MaterialTheme.typography.labelLarge)
                Text(formatDuration(duration), style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { app.player.toggleShuffle() }) {
                    Icon(Icons.Default.Shuffle, stringResource(R.string.action_shuffle),
                        tint = if (state.shuffle) colorScheme.primary else colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { app.player.previous() }, modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Default.SkipPrevious, stringResource(R.string.action_prev), modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick = { app.player.togglePlay() },
                    modifier = Modifier.size(84.dp),
                    shape = RoundedCornerShape(32.dp),
                ) {
                    AnimatedContent(targetState = state.isPlaying, label = "playpause") { playing ->
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (playing) R.string.action_pause else R.string.action_play),
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
                IconButton(onClick = { app.player.next() }, modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Default.SkipNext, stringResource(R.string.action_next), modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { app.player.toggleRepeatOne() }) {
                    Icon(
                        if (state.repeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        stringResource(R.string.action_repeat),
                        tint = if (state.repeatOne) colorScheme.primary else colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                val isFav = track.id in favorites
                FilledIconButton(
                    onClick = { app.appScope.launch { app.repository.toggleFavorite(track.id) } },
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isFav) colorScheme.primary else colorScheme.surfaceContainerHigh,
                        contentColor = if (isFav) colorScheme.onPrimary else colorScheme.onSurface,
                    ),
                    modifier = Modifier.size(56.dp),
                ) { Icon(Icons.Default.Favorite, stringResource(R.string.action_favorite)) }

                FilledIconButton(onClick = { showSleep = true }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Timer, stringResource(R.string.action_sleep_timer))
                }
                FilledIconButton(onClick = { showLyrics = true }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Notes, stringResource(R.string.action_lyrics))
                }
                FilledIconButton(onClick = { showRadio = true }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Radio, stringResource(R.string.action_radio))
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                var volume by remember { mutableFloatStateOf(1f) }
                Slider(value = volume, onValueChange = { volume = it; app.player.setVolume(it) }, modifier = Modifier.weight(1f))
            }
        }
    }

    if (showQueue) QueueSheet(app, state.queue, state.currentIndex) { showQueue = false }

    if (showSleep) {
        val endsAt = state.sleepTimerEndsAt
        AlertDialog(
            onDismissRequest = { showSleep = false },
            title = { Text(stringResource(R.string.action_sleep_timer)) },
            text = {
                Column {
                    if (endsAt != null) Text(stringResource(R.string.sleep_active, formatDuration(endsAt - System.currentTimeMillis())))
                    listOf(0, 15, 30, 45, 60).forEach { m ->
                        TextButton(onClick = { app.player.setSleepTimer(m); showSleep = false }) {
                            Text(if (m == 0) stringResource(R.string.sleep_off) else stringResource(R.string.sleep_minutes, m))
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (showEq) EqualizerDialog(app) { showEq = false }

    if (showLyrics) {
        AlertDialog(
            onDismissRequest = { showLyrics = false },
            title = { Text(stringResource(R.string.action_lyrics)) },
            text = { Text(stringResource(R.string.lyrics_unavailable)) },
            confirmButton = { TextButton(onClick = { showLyrics = false }) { Text(stringResource(R.string.action_ok)) } },
        )
    }

    if (showRadio) {
        // Radio from the current track: plays other local tracks by the same artist first, then the rest.
        AlertDialog(
            onDismissRequest = { showRadio = false },
            title = { Text(stringResource(R.string.action_radio)) },
            text = { Text(stringResource(R.string.radio_explain)) },
            confirmButton = {
                Button(onClick = {
                    val all = app.repository.allTracks()
                    val same = all.filter { it.artistId == track.artistId && it.id != track.id }
                    val rest = all.filter { it.artistId != track.artistId }.shuffled()
                    val queue = listOf(track) + same.shuffled() + rest.take(50)
                    app.player.play(queue, 0)
                    showRadio = false
                }) { Text(stringResource(R.string.action_start)) }
            },
            dismissButton = { TextButton(onClick = { showRadio = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(app: MelozaApp, queue: List<Track>, currentIndex: Int, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val favorites by app.repository.favorites().collectAsState(initial = emptyList())
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Text(stringResource(R.string.queue_title), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            itemsIndexed(queue) { index, t ->
                TrackRow(
                    track = t,
                    isFavorite = t.id in favorites,
                    isPlaying = index == currentIndex,
                    onClick = { app.player.skipTo(index) },
                    onFavorite = { app.appScope.launch { app.repository.toggleFavorite(t.id) } },
                )
            }
        }
    }
}

@Composable
private fun EqualizerDialog(app: MelozaApp, onDismiss: () -> Unit) {
    val bands = remember { app.player.equalizerBands() }
    val range = remember { app.player.eqLevelRange() }
    var enabled by remember { mutableStateOf(false) }
    val levels = remember { mutableStateOf(bands.map { app.player.eqBandLevel(it.second).toFloat() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.eq_title)) },
        text = {
            if (bands.isEmpty()) {
                Text(stringResource(R.string.eq_unsupported))
            } else Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.eq_enabled), modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it; app.player.setEqEnabled(it) })
                }
                Spacer(Modifier.height(8.dp))
                bands.forEachIndexed { i, (freqMilliHz, band) ->
                    Text("${freqMilliHz / 1000} Hz", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = levels.value[i],
                        valueRange = range.first.toFloat()..range.second.toFloat(),
                        onValueChange = { v ->
                            levels.value = levels.value.toMutableList().also { it[i] = v }
                            app.player.setEqBand(band, v.toInt().toShort())
                        },
                        enabled = enabled,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}
