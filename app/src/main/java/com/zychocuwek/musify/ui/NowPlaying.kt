package com.zychocuwek.musify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.zychocuwek.musify.data.Api
import com.zychocuwek.musify.data.Lyrics
import com.zychocuwek.musify.data.Store

private fun fmt(ms: Long): String { val s = (ms / 1000).toInt(); return "%d:%02d".format(s / 60, s % 60) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(nav: AppNav, onClose: () -> Unit) {
    val ui by nav.player.ui.collectAsState()
    val pos by nav.player.positionMs.collectAsState()
    val dur by nav.player.durationMs.collectAsState()
    val liked by Store.liked.collectAsState()
    val song = ui.current ?: run { LaunchedEffect(Unit) { onClose() }; return }

    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var lyrics by remember(song.videoId) { mutableStateOf<Lyrics?>(null) }
    var lyricsBusy by remember(song.videoId) { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(song.videoId, showLyrics) {
        if (showLyrics && lyrics == null) {
            lyricsBusy = true
            lyrics = try { Api.lyrics(song) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
            lyricsBusy = false
        }
    }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Tutup", tint = C.Bone, modifier = Modifier.size(32.dp)) }
            Text("Sedang diputar", color = C.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton({ showQueue = true }) { Icon(Icons.Rounded.QueueMusic, "Antrean", tint = C.Bone) }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (!showLyrics) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Cover(song.cover, minOf(maxWidth, maxHeight))
                }
            } else {
                val ly = lyrics
                when {
                    lyricsBusy -> CircularProgressIndicator(color = C.Accent)
                    ly == null -> Text("Lirik tidak tersedia untuk lagu ini", color = C.Muted)
                    else -> {
                        val sec = pos / 1000.0
                        val current = if (ly.synced) ly.lines.indexOfLast { it.time >= 0 && it.time <= sec } else -1
                        val state = rememberLazyListState()
                        LaunchedEffect(current) { if (current >= 0) state.animateScrollToItem((current - 2).coerceAtLeast(0)) }
                        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 40.dp)) {
                            itemsIndexed(ly.lines) { i, line ->
                                val active = i == current
                                Text(
                                    line.text,
                                    color = if (active) C.Accent else if (ly.synced) C.Muted2 else C.Bone,
                                    fontSize = if (active) 24.sp else 20.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = ly.synced && line.time >= 0) { nav.player.seekTo((line.time * 1000).toLong()) }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, color = C.Bone, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = C.Muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = song.artistId.isNotBlank()) { onClose(); nav.openArtist(song.artistId) })
            }
            val isLiked = liked.any { it.videoId == song.videoId }
            IconButton({ Store.toggleLike(song) }) {
                Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Sukai", tint = if (isLiked) C.Accent else C.Bone)
            }
        }

        val shown = if (dragging) dragValue else if (dur > 0) pos.toFloat() / dur else 0f
        Slider(
            value = shown.coerceIn(0f, 1f),
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = { nav.player.seekTo((dragValue * dur).toLong()); dragging = false },
            colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.Line)
        )
        Row(Modifier.fillMaxWidth()) {
            Text(fmt(if (dragging) (dragValue * dur).toLong() else pos), color = C.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(fmt(dur), color = C.Muted, fontSize = 12.sp)
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton({ nav.player.toggleShuffle() }) { Icon(Icons.Rounded.Shuffle, "Acak", tint = if (ui.shuffle) C.Accent else C.Muted) }
            IconButton({ nav.player.prev() }) { Icon(Icons.Rounded.SkipPrevious, "Sebelumnya", tint = C.Bone, modifier = Modifier.size(36.dp)) }
            Box(Modifier.size(68.dp).clip(CircleShape).background(C.Accent).clickable { nav.player.toggle() }, contentAlignment = Alignment.Center) {
                if (ui.loading) CircularProgressIndicator(Modifier.size(30.dp), color = C.AccentInk, strokeWidth = 3.dp)
                else Icon(if (ui.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Putar/Jeda", tint = C.AccentInk, modifier = Modifier.size(38.dp))
            }
            IconButton({ nav.player.next() }) { Icon(Icons.Rounded.SkipNext, "Berikutnya", tint = C.Bone, modifier = Modifier.size(36.dp)) }
            IconButton({ nav.player.cycleRepeat() }) {
                Icon(if (ui.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Ulangi",
                    tint = if (ui.repeat == Player.REPEAT_MODE_OFF) C.Muted else C.Accent)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton({ showLyrics = !showLyrics }) { Text(if (showLyrics) "Sampul" else "Lirik", color = C.Accent, fontWeight = FontWeight.Bold) }
            val speeds = listOf(0.75f, 1f, 1.25f, 1.5f)
            TextButton({ nav.player.setSpeed(speeds[(speeds.indexOf(ui.speed).let { if (it < 0) 1 else it } + 1) % speeds.size]) }) {
                Text("${ui.speed}x", color = C.Accent, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showQueue) ModalBottomSheet(onDismissRequest = { showQueue = false }, containerColor = C.Surface) {
        Text("Antrean (${ui.queue.size})", color = C.Bone, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(16.dp))
        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            itemsIndexed(ui.queue) { i, s ->
                Row(Modifier.fillMaxWidth().clickable { nav.player.jumpTo(i); showQueue = false }.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(s.cover, 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(s.title, color = if (i == ui.index) C.Accent else C.Bone, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(s.artist, color = C.Muted, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
