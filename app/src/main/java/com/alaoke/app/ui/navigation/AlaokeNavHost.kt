package com.alaoke.app.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alaoke.app.ui.AlaokeViewModel
import com.alaoke.app.ui.screens.HomeScreen
import com.alaoke.app.ui.screens.PartyQueueScreen
import com.alaoke.app.ui.screens.PlayerScreen
import com.alaoke.app.ui.screens.SettingsScreen
import com.alaoke.app.ui.screens.SmartPartyScreen
import com.alaoke.app.ui.screens.SongsScreen
import com.alaoke.app.ui.screens.YouTubeSearchScreen

private const val WIDE_LAYOUT_THRESHOLD_DP = 600

@Composable
fun AlaokeNavHost(viewModel: AlaokeViewModel) {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val useSideRail = configuration.screenWidthDp >= WIDE_LAYOUT_THRESHOLD_DP

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateTo(destination: AlaokeDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navGraph: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(
            navController = navController,
            startDestination = AlaokeDestination.HOME.route,
            modifier = modifier
        ) {
            composable(AlaokeDestination.HOME.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenPlayer = { navController.navigate(PLAYER_ROUTE) },
                    onNavigate = ::navigateTo
                )
            }
            composable(AlaokeDestination.YOUTUBE_SEARCH.route) {
                YouTubeSearchScreen(
                    viewModel = viewModel,
                    onOpenPlayer = { navController.navigate(PLAYER_ROUTE) }
                )
            }
            composable(AlaokeDestination.SONGS.route) {
                SongsScreen(
                    viewModel = viewModel,
                    onOpenPlayer = { navController.navigate(PLAYER_ROUTE) },
                    onGoToYouTubeSearch = { navigateTo(AlaokeDestination.YOUTUBE_SEARCH) }
                )
            }
            composable(AlaokeDestination.QUEUE.route) {
                PartyQueueScreen(
                    viewModel = viewModel,
                    onOpenPlayer = { navController.navigate(PLAYER_ROUTE) }
                )
            }
            composable(AlaokeDestination.SMART_PARTY.route) {
                SmartPartyScreen(
                    viewModel = viewModel,
                    onOpenPlayer = { navController.navigate(PLAYER_ROUTE) }
                )
            }
            composable(AlaokeDestination.SETTINGS.route) {
                SettingsScreen(viewModel = viewModel)
            }
            composable(PLAYER_ROUTE) {
                PlayerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (currentRoute == PLAYER_ROUTE) {
        // Full-bleed player: no bars, maximizes lyrics/video real estate (also TV-friendly).
        navGraph(Modifier)
        return
    }

    if (useSideRail) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail {
                AlaokeDestination.bottomBarItems.forEach { dest ->
                    NavigationRailItem(
                        selected = currentRoute == dest.route,
                        onClick = { navigateTo(dest) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
            navGraph(Modifier.weight(1f))
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    AlaokeDestination.bottomBarItems.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { navigateTo(dest) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            navGraph(Modifier.padding(innerPadding))
        }
    }
}
