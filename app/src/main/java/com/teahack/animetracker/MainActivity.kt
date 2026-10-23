package com.teahack.animetracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Night = Color(0xFF100B1C)
private val Panel = Color(0xFF1D1630)
private val Purple = Color(0xFF9B7BFF)
private val Mint = Color(0xFF72E6C1)
private val Muted = Color(0xFFB8AECF)

data class Anime(val id: Int, val title: String, val episodes: Int?, val score: Double?, val synopsis: String)
data class TrackedAnime(val anime: Anime, val watched: Int = 0, val status: String = "Watching")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnimeTrackerApp(this) }
    }
}

@Composable
fun AnimeTrackerApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("anime_tracker", Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var watchlist by remember { mutableStateOf(loadWatchlist(prefs)) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    fun save(items: List<TrackedAnime>) {
        watchlist = items
        val arr = JSONArray()
        items.forEach { t -> arr.put(JSONObject().apply {
            put("id", t.anime.id); put("title", t.anime.title)
            put("episodes", t.anime.episodes ?: JSONObject.NULL)
            put("score", t.anime.score ?: JSONObject.NULL)
            put("synopsis", t.anime.synopsis); put("watched", t.watched); put("status", t.status)
        }) }
        prefs.edit().putString("watchlist", arr.toString()).apply()
    }

    fun runSearch() {
        if (query.isBlank()) return
        loading = true; error = ""
        scope.launch {
            try { results = searchAnime(query.trim()) }
            catch (_: Exception) { error = "Search failed. Check your internet connection and try again." }
            finally { loading = false }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, secondary = Mint, background = Night, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Night, topBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("ANIME TRACKER", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Your next episode awaits.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Discover") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("My Watchlist (${watchlist.size})") })
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                if (tab == 0) {
                    OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search anime, e.g. Naruto") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { runSearch() }))
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { runSearch() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Search anime")
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        error.isNotBlank() -> Text(error, color = Color(0xFFFF9C9C), modifier = Modifier.padding(12.dp))
                        results.isEmpty() -> Text("Search the anime catalogue to add shows to your list.", color = Muted, modifier = Modifier.padding(vertical = 14.dp))
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(results, key = { it.id }) { anime ->
                            AnimeSearchCard(anime, watchlist.any { it.anime.id == anime.id }) {
                                if (watchlist.none { it.anime.id == anime.id }) save(watchlist + TrackedAnime(anime))
                            }
                        }
                    }
                } else {
                    if (watchlist.isEmpty()) {
                        Text("Your watchlist is empty.", color = Muted, modifier = Modifier.padding(top = 20.dp))
                        Text("Open Discover and search for an anime to add it.", color = Muted, fontSize = 13.sp)
                    } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(watchlist, key = { it.anime.id }) { tracked ->
                            WatchlistCard(tracked,
                                onIncrement = {
                                    val max = tracked.anime.episodes
                                    val next = if (max != null && max > 0) (tracked.watched + 1).coerceAtMost(max) else tracked.watched + 1
                                    save(watchlist.map { if (it.anime.id == tracked.anime.id) it.copy(watched = next, status = if (max != null && next >= max) "Completed" else it.status) else it })
                                },
                                onDecrement = { save(watchlist.map { if (it.anime.id == tracked.anime.id) it.copy(watched = (it.watched - 1).coerceAtLeast(0), status = if (it.status == "Completed") "Watching" else it.status) else it }) },
                                onStatus = { status -> save(watchlist.map { if (it.anime.id == tracked.anime.id) it.copy(status = status) else it }) },
                                onDelete = { save(watchlist.filterNot { it.anime.id == tracked.anime.id }) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeSearchCard(anime: Anime, isAdded: Boolean, onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(anime.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Episodes: ${anime.episodes?.toString() ?: "Unknown"}", color = Muted, fontSize = 12.sp)
                Text("★ ${anime.score?.toString() ?: "N/A"}", color = Mint, fontSize = 12.sp)
            }
            if (anime.synopsis.isNotBlank()) { Spacer(Modifier.height(7.dp)); Text(anime.synopsis, color = Muted, fontSize = 12.sp, maxLines = 3) }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onAdd, enabled = !isAdded, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(if (isAdded) Icons.Default.Check else Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(if (isAdded) "In watchlist" else "Add to watchlist")
            }
        }
    }
}

@Composable
fun WatchlistCard(tracked: TrackedAnime, onIncrement: () -> Unit, onDecrement: () -> Unit, onStatus: (String) -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val total = tracked.anime.episodes
    val progress = if (total != null && total > 0) (tracked.watched.toFloat() / total).coerceIn(0f, 1f) else 0f
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(tracked.anime.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${tracked.watched} / ${total?.toString() ?: "?"} episodes watched", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remove from watchlist", tint = Color(0xFFFF8C9B)) }
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Mint, trackColor = Color(0xFF39304D))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDecrement, enabled = tracked.watched > 0, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("− EP") }
                Button(onClick = onIncrement, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("+ EP") }
                Spacer(Modifier.weight(1f)); Text(tracked.status, color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide status options" else "Change status") }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("Plan to Watch", "Watching", "Completed", "On Hold", "Dropped").forEach { status ->
                        TextButton(onClick = { onStatus(status); expanded = false }, modifier = Modifier.fillMaxWidth()) { Text(status) }
                    }
                }
            }
        }
    }
}

suspend fun searchAnime(query: String): List<Anime> = withContext(Dispatchers.IO) {
    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
    val connection = URL("https://api.jikan.moe/v4/anime?q=$encoded&limit=20&sfw=true").openConnection() as HttpURLConnection
    connection.requestMethod = "GET"; connection.connectTimeout = 10000; connection.readTimeout = 15000
    try {
        if (connection.responseCode !in 200..299) throw IllegalStateException("Search HTTP error")
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val array = JSONObject(body).optJSONArray("data") ?: JSONArray()
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optInt("mal_id", -1); if (id < 0) continue
                add(Anime(id, obj.optString("title", "Untitled anime"), if (obj.isNull("episodes")) null else obj.optInt("episodes"),
                    if (obj.isNull("score")) null else obj.optDouble("score"), obj.optString("synopsis", "").orEmpty()))
            }
        }
    } finally { connection.disconnect() }
}

fun loadWatchlist(prefs: android.content.SharedPreferences): List<TrackedAnime> {
    val raw = prefs.getString("watchlist", null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val anime = Anime(o.optInt("id"), o.optString("title"), if (o.isNull("episodes")) null else o.optInt("episodes"),
                    if (o.isNull("score")) null else o.optDouble("score"), o.optString("synopsis"))
                add(TrackedAnime(anime, o.optInt("watched", 0), o.optString("status", "Watching")))
            }
        }
    } catch (_: Exception) { emptyList() }
}
