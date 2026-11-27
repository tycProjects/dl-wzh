package com.zephyr.music

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay

private val Bg = Color(0xFF090B10)
private val Panel = Color(0xFF151922)
private val Accent = Color(0xFF39E6A0)
private data class Track(val title: String, val subtitle: String, val uri: Uri)

class MainActivity : ComponentActivity() {
    private var controller by mutableStateOf<MediaController?>(null)
    private var connectionFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        connectionFuture = MediaController.Builder(this, token).buildAsync()
        connectionFuture?.addListener({
            try { controller = connectionFuture?.get() } catch (_: Exception) { }
        }, MoreExecutors.directExecutor())
        setContent { ZephyrApp(controller) }
    }

    override fun onDestroy() {
        controller?.release()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZephyrApp(player: MediaController?) {
    var tracks by remember { mutableStateOf(listOf<Track>()) }
    var currentTitle by remember { mutableStateOf("Belum ada lagu") }
    var currentArtist by remember { mutableStateOf("Tambahkan musik lokal atau URL stream") }
    var playing by remember { mutableStateOf(false) }
    var showUrl by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            val title = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Audio lokal"
            tracks = tracks + Track(title, "File lokal", uri)
            player?.let {
                val item = MediaItem.Builder().setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist("File lokal").build()).build()
                it.setMediaItem(item); it.prepare(); it.play()
                currentTitle = title; currentArtist = "File lokal"; playing = true
            }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (player != null) {
                playing = player.isPlaying
                duration = player.duration.takeIf { it > 0 } ?: 0L
                progress = if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
                player.currentMediaItem?.mediaMetadata?.let { meta ->
                    currentTitle = meta.title?.toString() ?: currentTitle
                    currentArtist = meta.artist?.toString() ?: currentArtist
                }
            }
            delay(500)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
        Scaffold(containerColor = Bg, bottomBar = {
            NavigationBar(containerColor = Color(0xFF10141C), contentColor = Color.White) {
                listOf("Home", "Search", "Library").forEachIndexed { i, label ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {
                        Icon(when(i) { 0 -> Icons.Default.Home; 1 -> Icons.Default.Search; else -> Icons.Default.LibraryMusic }, null)
                    }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Accent, indicatorColor = Color(0xFF20372F), selectedTextColor = Accent, unselectedTextColor = Color.LightGray))
                }
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Accent, Color(0xFF167DFF))), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.GraphicEq, null, tint = Bg, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ZEPHYR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 2.sp)
                        Text("MUSIC PLAYER", color = Accent, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    IconButton(onClick = { showUrl = true }) { Icon(Icons.Default.AddLink, "Tambah stream", tint = Color.White) }
                }

                when (tab) {
                    0 -> {
                        Text("Selamat malam", color = Color.LightGray, fontSize = 13.sp)
                        Text("Temukan ritmemu.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.linearGradient(listOf(Color(0xFF183D36), Color(0xFF18233B), Color(0xFF282044))), RoundedCornerShape(24.dp)).padding(20.dp)) {
                            Column(Modifier.align(Alignment.CenterStart)) {
                                Text("YOUR SOUND. YOUR SPACE.", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Musik tanpa jeda\nuntuk harimu.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp)
                            }
                            Icon(Icons.Default.GraphicEq, null, tint = Color.White.copy(alpha = .15f), modifier = Modifier.size(100.dp).align(Alignment.CenterEnd))
                        }
                        Spacer(Modifier.height(22.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Koleksi musik", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Text("+ Tambah file", color = Accent) }
                        }
                        if (tracks.isEmpty()) EmptyState(onLocal = { picker.launch(arrayOf("audio/*")) }, onStream = { showUrl = true })
                        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tracks) { track ->
                                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).clickable {
                                    player?.let { p -> p.setMediaItem(MediaItem.fromUri(track.uri)); p.prepare(); p.play(); currentTitle = track.title; currentArtist = track.subtitle; playing = true }
                                }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(44.dp).background(Color(0xFF24352F), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Accent) }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(track.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                        Text(track.subtitle, color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Icon(Icons.Default.PlayCircle, null, tint = Accent)
                                }
                            }
                        }
                    }
                    1 -> {
                        Text("Cari musik", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text("Tambahkan URL audio langsung dari sumber yang mengizinkan streaming, atau impor file dari perangkat.", color = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showUrl = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Icon(Icons.Default.Link, null); Spacer(Modifier.width(8.dp)); Text("Tambah URL streaming") }
                        OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Pilih musik dari perangkat") }
                    }
                    else -> {
                        Text("Library", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Musik yang kamu tambahkan tersimpan di daftar sesi ini.", color = Color.LightGray)
                        Spacer(Modifier.height(14.dp))
                        Text("${tracks.size} lagu", color = Accent, fontWeight = FontWeight.Bold)
                        tracks.forEach { track -> Text("♪  ${track.title}", color = Color.White, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                }
                Spacer(Modifier.weight(1f, fill = false))
                PlayerBar(currentTitle, currentArtist, playing, progress, duration,
                    onPlay = { player?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onNext = { player?.seekToNextMediaItem() }, onPrev = { player?.seekToPreviousMediaItem() })
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showUrl) AlertDialog(
        onDismissRequest = { showUrl = false }, containerColor = Panel,
        title = { Text("Tambah streaming", color = Color.White) },
        text = { Column {
            Text("Masukkan URL file audio/stream yang legal dan bisa diakses langsung (mis. MP3 atau HLS).", color = Color.LightGray, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("https://...") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = {
            val clean = url.trim()
            if (clean.startsWith("https://")) {
                val uri = Uri.parse(clean)
                tracks = tracks + Track(clean.substringAfterLast('/').ifBlank { "Online stream" }, "Online stream", uri)
                player?.let { p ->
                    val item = MediaItem.Builder().setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(clean.substringAfterLast('/').ifBlank { "Online stream" }).setArtist("Streaming").build()).build()
                    p.setMediaItem(item); p.prepare(); p.play(); currentTitle = item.mediaMetadata.title?.toString() ?: "Online stream"; currentArtist = "Streaming"; playing = true
                }
                url = ""; showUrl = false
            }
        }) { Text("PUTAR", color = Accent) } },
        dismissButton = { TextButton(onClick = { showUrl = false }) { Text("Batal", color = Color.LightGray) } }
    )
}

@Composable
private fun EmptyState(onLocal: () -> Unit, onStream: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LibraryMusic, null, tint = Accent, modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(8.dp))
        Text("Library masih kosong", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Mulai dengan musik dari perangkat atau URL streaming.", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLocal, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("Pilih musik") }
        TextButton(onClick = onStream) { Text("Tambah URL online", color = Accent) }
    }
}

@Composable
private fun PlayerBar(title: String, artist: String, playing: Boolean, progress: Float, duration: Long, onPlay: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF171D26), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFF263A34), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Accent) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onPrev) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White) }
            IconButton(onClick = onPlay) { Icon(if (playing) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, null, tint = Accent, modifier = Modifier.size(34.dp)) }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = Color.White) }
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Accent, trackColor = Color(0xFF343B46))
    }
}
