package com.zychocuwek.musify.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zychocuwek.musify.data.Card
import com.zychocuwek.musify.data.Song
import com.zychocuwek.musify.data.Store
import com.zychocuwek.musify.data.prettyDuration
import com.zychocuwek.musify.player.PlayerController

/** Semua aksi navigasi + pemutar yang dibutuhkan layar. */
class AppNav(
    val player: PlayerController,
    val openArtist: (String) -> Unit,
    val openAlbum: (String) -> Unit,
    val openPlaylist: (String) -> Unit,
    val openPlayer: () -> Unit
)

@Composable
fun Cover(url: String, size: Dp, round: Boolean = false, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url.ifBlank { null }, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(if (round) CircleShape else RoundedCornerShape(10.dp)).background(C.Raised)
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, color = C.Bone, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp))

@Composable
fun ErrorBox(msg: String, onRetry: () -> Unit) = Column(
    Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(msg, color = C.Muted, fontSize = 14.sp)
    Spacer(Modifier.height(12.dp))
    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = C.Accent, contentColor = C.AccentInk)) { Text("Coba lagi") }
}

@Composable
fun Loading() = Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = C.Accent, strokeWidth = 3.dp)
}

/** Baris lagu + menu titik tiga. */
@Composable
fun SongRow(song: Song, nav: AppNav, onClick: () -> Unit, extraMenu: (@Composable (dismiss: () -> Unit) -> Unit)? = null) {
    val ctx = LocalContext.current
    val ui by nav.player.ui.collectAsState()
    val liked by Store.liked.collectAsState()
    val isCurrent = ui.current?.videoId == song.videoId
    var menu by remember { mutableStateOf(false) }
    var pickPlaylist by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(song.cover, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = if (isCurrent) C.Accent else C.Bone, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(song.artist, song.duration.prettyDuration()).filter { it.isNotBlank() }.joinToString(" • "),
                color = C.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Menu", tint = C.Muted) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = C.Raised) {
                DropdownMenuItem(text = { Text("Putar berikutnya") }, onClick = { nav.player.playNext(song); menu = false; Toast.makeText(ctx, "Diputar berikutnya", Toast.LENGTH_SHORT).show() })
                DropdownMenuItem(text = { Text("Tambah ke antrean") }, onClick = { nav.player.addToQueue(song); menu = false; Toast.makeText(ctx, "Masuk antrean", Toast.LENGTH_SHORT).show() })
                DropdownMenuItem(text = { Text(if (liked.any { it.videoId == song.videoId }) "Hapus dari Disukai" else "Sukai") }, onClick = { Store.toggleLike(song); menu = false })
                DropdownMenuItem(text = { Text("Tambah ke playlist") }, onClick = { menu = false; pickPlaylist = true })
                if (song.artistId.isNotBlank()) DropdownMenuItem(text = { Text("Buka artis") }, onClick = { menu = false; nav.openArtist(song.artistId) })
                extraMenu?.invoke { menu = false }
            }
        }
    }
    if (pickPlaylist) PlaylistPicker(song) { pickPlaylist = false }
}

@Composable
fun PlaylistPicker(song: Song, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val pls by Store.playlists.collectAsState()
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = C.Surface,
        title = { Text("Tambah ke playlist") },
        text = {
            Column {
                pls.forEach { pl ->
                    Text(
                        "${pl.name}  (${pl.songs.size}/15)", color = C.Bone,
                        modifier = Modifier.fillMaxWidth().clickable {
                            Toast.makeText(ctx, Store.addToPlaylist(pl.id, song), Toast.LENGTH_SHORT).show(); onDismiss()
                        }.padding(vertical = 12.dp)
                    )
                }
                OutlinedTextField(newName, { newName = it }, label = { Text("Playlist baru") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newName.isNotBlank()) {
                    Store.createPlaylist(newName)
                    Store.playlists.value.lastOrNull()?.let { Toast.makeText(ctx, Store.addToPlaylist(it.id, song), Toast.LENGTH_SHORT).show() }
                    onDismiss()
                }
            }) { Text("Buat & tambah", color = C.Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = C.Muted) } }
    )
}

@Composable
fun CardRow(items: List<Card>, round: Boolean = false, onClick: (Card) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(items) { c ->
            Column(Modifier.width(132.dp).clickable { onClick(c) }, horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start) {
                Cover(c.cover, 132.dp, round)
                Spacer(Modifier.height(6.dp))
                Text(c.title, color = C.Bone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (c.subtitle.isNotBlank()) Text(c.subtitle, color = C.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun PlayAllButton(onClick: () -> Unit, label: String = "Putar semua") =
    Button(
        onClick = onClick, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = C.Accent, contentColor = C.AccentInk),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(label, fontWeight = FontWeight.Bold) }

@Composable
fun MiniPlayer(nav: AppNav, modifier: Modifier = Modifier) {
    val ui by nav.player.ui.collectAsState()
    val pos by nav.player.positionMs.collectAsState()
    val dur by nav.player.durationMs.collectAsState()
    val s = ui.current ?: return
    Column(
        modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp)).background(C.Raised).clickable { nav.openPlayer() }
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(s.cover, 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, color = C.Bone, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.artist, color = C.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (ui.loading) CircularProgressIndicator(Modifier.size(24.dp), color = C.Accent, strokeWidth = 2.dp)
            else IconButton(onClick = { nav.player.toggle() }) {
                Icon(if (ui.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Putar/Jeda", tint = C.Accent, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { nav.player.next() }) { Icon(Icons.Rounded.SkipNext, "Berikutnya", tint = C.Bone) }
        }
        LinearProgressIndicator(
            progress = { if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp), color = C.Accent, trackColor = C.Line
        )
    }
}
