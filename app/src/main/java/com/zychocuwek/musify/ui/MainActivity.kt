package com.zychocuwek.musify.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.zychocuwek.musify.player.PlayerController

private enum class NavTab(val label: String, val icon: ImageVector) {
    Home("Beranda", Icons.Rounded.Home),
    Search("Cari", Icons.Rounded.Search),
    Library("Pustaka", Icons.Rounded.LibraryMusic),
    Profile("Profil", Icons.Rounded.Person)
}

private sealed interface Route {
    data class Artist(val id: String) : Route
    data class Album(val id: String) : Route
    data class Playlist(val id: String) : Route
}

class MainActivity : ComponentActivity() {
    private lateinit var player: PlayerController

    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = PlayerController(applicationContext).also { it.connect() }
        if (Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { MusifyTheme { Root(player) } }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@Composable
private fun Root(player: PlayerController) {
    var tab by rememberSaveable { mutableStateOf(NavTab.Home) }
    var stack by remember { mutableStateOf(listOf<Route>()) }
    var showPlayer by remember { mutableStateOf(false) }
    val ui by player.ui.collectAsState()

    val nav = remember {
        AppNav(
            player = player,
            openArtist = { stack = stack + Route.Artist(it) },
            openAlbum = { stack = stack + Route.Album(it) },
            openPlaylist = { stack = stack + Route.Playlist(it) },
            openPlayer = { showPlayer = true }
        )
    }
    val pop = { stack = stack.dropLast(1) }

    BackHandler(showPlayer) { showPlayer = false }
    BackHandler(!showPlayer && stack.isNotEmpty()) { pop() }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = C.Bg,
            bottomBar = {
                Column {
                    if (ui.current != null) MiniPlayer(nav, Modifier.padding(bottom = 6.dp))
                    NavigationBar(containerColor = C.Surface) {
                        NavTab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t && stack.isEmpty(),
                                onClick = { tab = t; stack = emptyList() },
                                icon = { Icon(t.icon, t.label) }, label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = C.AccentInk, selectedTextColor = C.Accent,
                                    indicatorColor = C.Accent, unselectedIconColor = C.Muted, unselectedTextColor = C.Muted
                                )
                            )
                        }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (val r = stack.lastOrNull()) {
                    is Route.Artist -> ArtistScreen(r.id, nav, pop)
                    is Route.Album -> AlbumScreen(r.id, nav, pop)
                    is Route.Playlist -> UserPlaylistScreen(r.id, nav, pop)
                    null -> when (tab) {
                        NavTab.Home -> HomeScreen(nav)
                        NavTab.Search -> SearchScreen(nav)
                        NavTab.Library -> LibraryScreen(nav)
                        NavTab.Profile -> ProfileScreen()
                    }
                }
            }
        }
        AnimatedVisibility(showPlayer, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            NowPlayingScreen(nav) { showPlayer = false }
        }
    }
}
