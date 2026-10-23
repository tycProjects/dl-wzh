package com.alaoke.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.ui.graphics.vector.ImageVector

enum class AlaokeDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    YOUTUBE_SEARCH("youtube_search", "YT Search", Icons.Filled.Search),
    SONGS("songs", "Songs", Icons.Filled.LibraryMusic),
    QUEUE("queue", "Queue", Icons.Filled.PlaylistPlay),
    SMART_PARTY("smart_party", "Smart Party", Icons.Filled.Celebration),
    SETTINGS("settings", "Settings", Icons.Filled.Settings);

    companion object {
        val bottomBarItems = listOf(HOME, YOUTUBE_SEARCH, SONGS, QUEUE, SMART_PARTY, SETTINGS)
    }
}

const val PLAYER_ROUTE = "player"
