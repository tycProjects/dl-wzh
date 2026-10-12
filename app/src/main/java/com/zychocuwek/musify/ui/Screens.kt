package com.zychocuwek.musify.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zychocuwek.musify.BuildConfig
import com.zychocuwek.musify.R
import com.zychocuwek.musify.data.*
import com.zychocuwek.musify.data.Card
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =====================================================================================
//  BERANDA
// =====================================================================================
private data class HomeData(val songs: List<Song>, val artists: List<Card>, val playlists: List<Card>)

@Composable
fun HomeScreen(nav: AppNav) {
    val l = rememberLoader("home") {
        coroutineScope {
            val songs = async { Api.search("lagu indonesia populer 2026", "songs").songs }
            val artists = async { runCatching { Api.search("XXXTENTACION Lil Peep Juice WRLD Lil Loaded Hindia", "artists").artists }.getOrDefault(emptyList()) }
            val pls = async { runCatching { Api.search("top hits playlist", "playlists").playlists }.getOrDefault(emptyList()) }
            HomeData(songs.await(), artists.await(), pls.await())
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                Image(painterResource(R.drawable.banner_poster), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, C.Bg))))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text("Musify", color = C.Bone, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Dengarkan apa saja, kapan saja", color = C.Muted, fontSize = 14.sp)
                }
            }
        }
        item {
            l.Show { d ->
                Column {
                    if (d.songs.isNotEmpty()) {
                        SectionTitle("Lagu pilihan")
                        PlayAllButton({ nav.player.play(d.songs, 0); nav.openPlayer() }, "Putar acak pilihan")
                        Spacer(Modifier.height(6.dp))
                        d.songs.take(12).forEachIndexed { i, s -> SongRow(s, nav, onClick = { nav.player.play(d.songs, i) }) }
                    }
                    if (d.artists.isNotEmpty()) { SectionTitle("Artis"); CardRow(d.artists, round = true) { nav.openArtist(it.id) } }
                    if (d.playlists.isNotEmpty()) { SectionTitle("Playlist"); CardRow(d.playlists) { nav.openAlbum(it.id) } }
                }
            }
        }
    }
}

// =====================================================================================
//  CARI
// =====================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: AppNav) {
    var query by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<Load<SearchResult>?>(null) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var retryTick by remember { mutableIntStateOf(0) }

    // pencarian real-time (debounce) + saran
    LaunchedEffect(query, retryTick) {
        val q = query.trim()
        if (q.length < 2) { result = null; suggestions = emptyList(); return@LaunchedEffect }
        delay(250)
        suggestions = Api.suggest(q)
        delay(250)
        result = Load.Busy
        result = try { Load.Ok(Api.search(q)) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { Load.Err(e.message ?: "Gagal mencari") }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("Cari lagu, artis, album…", color = C.Muted2) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = C.Muted) },
            trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Rounded.Close, "Hapus", tint = C.Muted) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { suggestions = emptyList() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.Accent, unfocusedBorderColor = C.Line, focusedContainerColor = C.Surface, unfocusedContainerColor = C.Surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        if (suggestions.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { s ->
                    AssistChip(onClick = { query = s; suggestions = emptyList() }, label = { Text(s, maxLines = 1) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = C.Raised, labelColor = C.Bone))
                }
            }
        }
        when (val r = result) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ketik untuk mencari musik", color = C.Muted2) }
            is Load.Busy -> Loading()
            is Load.Err -> ErrorBox(r.msg) { retryTick++ }
            is Load.Ok -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                val d = r.v
                if (d.artists.isNotEmpty()) item { SectionTitle("Artis"); CardRow(d.artists, true) { nav.openArtist(it.id) } }
                if (d.songs.isNotEmpty()) {
                    item { SectionTitle("Lagu") }
                    items(d.songs.size) { i -> SongRow(d.songs[i], nav, onClick = { nav.player.play(d.songs, i) }) }
                }
                if (d.albums.isNotEmpty()) item { SectionTitle("Album"); CardRow(d.albums) { nav.openAlbum(it.id) } }
                if (d.playlists.isNotEmpty()) item { SectionTitle("Playlist"); CardRow(d.playlists) { nav.openAlbum(it.id) } }
                if (d.songs.isEmpty() && d.artists.isEmpty() && d.albums.isEmpty() && d.playlists.isEmpty())
                    item { Text("Tidak ada hasil", color = C.Muted, modifier = Modifier.padding(32.dp)) }
            }
        }
    }
}

// =====================================================================================
//  PUSTAKA (Disukai + Playlist)
// =====================================================================================
@Composable
fun LibraryScreen(nav: AppNav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val liked by Store.liked.collectAsState()
    val pls by Store.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Text("Pustaka", color = C.Bone, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(16.dp))
        TabRow(selectedTabIndex = tab, containerColor = C.Bg, contentColor = C.Accent) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Disukai (${liked.size})") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Playlist (${pls.size})") })
        }
        if (tab == 0) {
            if (liked.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada lagu disukai", color = C.Muted2) }
            else LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item { Spacer(Modifier.height(12.dp)); PlayAllButton({ nav.player.play(liked, 0); nav.openPlayer() }) }
                items(liked.size) { i -> SongRow(liked[i], nav, onClick = { nav.player.play(liked, i) }) }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    TextButton(onClick = { creating = true }, modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Rounded.Add, null, tint = C.Accent); Spacer(Modifier.width(6.dp)); Text("Buat playlist", color = C.Accent)
                    }
                }
                items(pls) { pl ->
                    Row(Modifier.fillMaxWidth().clickable { nav.openPlaylist(pl.id) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Cover(pl.songs.firstOrNull()?.cover.orEmpty(), 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(pl.name, color = C.Bone, fontWeight = FontWeight.SemiBold)
                            Text("${pl.songs.size} lagu", color = C.Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
    if (creating) AlertDialog(
        onDismissRequest = { creating = false }, containerColor = C.Surface, title = { Text("Playlist baru") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Nama") }) },
        confirmButton = { TextButton({ if (name.isNotBlank()) { Store.createPlaylist(name); name = ""; creating = false } }) { Text("Buat", color = C.Accent) } },
        dismissButton = { TextButton({ creating = false }) { Text("Batal", color = C.Muted) } }
    )
}

@Composable
fun UserPlaylistScreen(id: String, nav: AppNav, onBack: () -> Unit) {
    val pls by Store.playlists.collectAsState()
    val pl = pls.find { it.id == id }
    if (pl == null) { LaunchedEffect(Unit) { onBack() }; return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            BackBar(pl.name, onBack) {
                IconButton({ Store.deletePlaylist(id) }) { Icon(Icons.Rounded.Delete, "Hapus playlist", tint = C.Muted) }
            }
            if (pl.songs.isNotEmpty()) PlayAllButton({ nav.player.play(pl.songs, 0); nav.openPlayer() })
            else Text("Playlist kosong. Tambahkan lagu lewat menu ⋮ pada lagu.", color = C.Muted, modifier = Modifier.padding(16.dp))
        }
        items(pl.songs.size) { i ->
            val s = pl.songs[i]
            SongRow(s, nav, onClick = { nav.player.play(pl.songs, i) }, extraMenu = { dismiss ->
                DropdownMenuItem(text = { Text("Hapus dari playlist") }, onClick = { Store.removeFromPlaylist(id, s.videoId); dismiss() })
            })
        }
    }
}

// =====================================================================================
//  ARTIS & ALBUM
// =====================================================================================
@Composable
fun BackBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", tint = C.Bone) }
        Text(title, color = C.Bone, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun ArtistScreen(id: String, nav: AppNav, onBack: () -> Unit) {
    val l = rememberLoader(id) { Api.artist(id) ?: error("Artis tidak ditemukan") }
    Column(Modifier.fillMaxSize()) {
        BackBar("Artis", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            l.Show { a ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Cover(a.cover, 160.dp, round = true)
                    Spacer(Modifier.height(10.dp))
                    Text(a.name, color = C.Bone, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(12.dp))
                if (a.topSongs.isNotEmpty()) {
                    PlayAllButton({ nav.player.play(a.topSongs, 0); nav.openPlayer() })
                    SectionTitle("Lagu populer")
                    a.topSongs.forEachIndexed { i, s -> SongRow(s.copy(artistId = id), nav, onClick = { nav.player.play(a.topSongs, i) }) }
                }
                if (a.albums.isNotEmpty()) { SectionTitle("Album"); CardRow(a.albums) { nav.openAlbum(it.id) } }
                if (a.singles.isNotEmpty()) { SectionTitle("Single & EP"); CardRow(a.singles) { nav.openAlbum(it.id) } }
                if (a.similar.isNotEmpty()) { SectionTitle("Artis serupa"); CardRow(a.similar, true) { nav.openArtist(it.id) } }
            }
        }
    }
}

@Composable
fun AlbumScreen(id: String, nav: AppNav, onBack: () -> Unit) {
    val l = rememberLoader(id) { Api.album(id) ?: error("Album tidak ditemukan") }
    Column(Modifier.fillMaxSize()) {
        BackBar("Album", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            l.Show { a ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Cover(a.cover, 200.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(a.title, color = C.Bone, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 16.dp))
                    Text("${a.songs.size} lagu", color = C.Muted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                PlayAllButton({ nav.player.play(a.songs, 0); nav.openPlayer() })
                Spacer(Modifier.height(6.dp))
                a.songs.forEachIndexed { i, s -> SongRow(s.copy(cover = s.cover.ifBlank { a.cover }), nav, onClick = { nav.player.play(a.songs, i) }) }
            }
        }
    }
}

// =====================================================================================
//  PROFIL
// =====================================================================================
private val changelog = listOf(
    "Tampilan baru" to "Palet terong & saffron, permukaan solid.",
    "Aplikasi native" to "Pemutar Media3: lanjut di latar belakang, notifikasi & layar kunci.",
    "Lebih ringan" to "Tanpa WebView; daftar lagu dan lirik digambar langsung oleh Compose."
)

@Composable
fun ProfileScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(Store.baseUrl) }
    var status by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo), null, Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Musify", color = C.Bone, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Versi ${BuildConfig.VERSION_NAME} · oleh zychocuwek", color = C.Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Server", color = C.Bone, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Alamat backend Musify (folder api/ yang di-deploy ke Vercel / VPS).", color = C.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(url, { url = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("https://domain-kamu.vercel.app") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.Accent, unfocusedBorderColor = C.Line))
        Row {
            TextButton({ Store.baseUrl = url; status = "Tersimpan" }) { Text("Simpan", color = C.Accent) }
            TextButton({
                Store.baseUrl = url; status = "Menguji…"
                scope.launch {
                    status = try {
                        val j = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Api.getJsonBlocking("/api/health") }
                        if (j.optBoolean("ok")) "Terhubung ✓" else "Server menjawab, tapi bukan backend Musify"
                    } catch (e: Exception) { "Gagal: ${e.message}" }
                }
            }) { Text("Tes koneksi", color = C.Accent) }
        }
        if (status.isNotBlank()) Text(status, color = C.Muted, fontSize = 13.sp)

        Spacer(Modifier.height(24.dp))
        Text("Yang baru di ${BuildConfig.VERSION_NAME}", color = C.Bone, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        changelog.forEach { (t, d) ->
            Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(14.dp)).background(C.Surface).padding(14.dp)) {
                Text(t, color = C.Accent, fontWeight = FontWeight.Bold)
                Text(d, color = C.Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029Vb8gHJ7545uuF9Ox1I2z"))) },
            colors = ButtonDefaults.buttonColors(containerColor = C.Raised, contentColor = C.Bone), modifier = Modifier.fillMaxWidth()
        ) { Text("Saluran WhatsApp") }
        Spacer(Modifier.height(100.dp))
    }
}
