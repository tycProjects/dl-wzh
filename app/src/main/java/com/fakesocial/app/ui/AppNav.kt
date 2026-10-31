package com.fakesocial.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fakesocial.app.EditorViewModel
import com.fakesocial.app.FakeSocialApp
import com.fakesocial.app.HistoryViewModel
import com.fakesocial.app.data.Platform

private const val HOME = "home"
private const val HISTORY = "history"
private const val EDITOR = "editor/{platform}/{id}"

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as FakeSocialApp

    NavHost(navController = nav, startDestination = HOME) {
        composable(HOME) {
            HomeScreen(
                onPlatform = { nav.navigate("editor/${it.name}/0") },
                onHistory = { nav.navigate(HISTORY) }
            )
        }
        composable(
            route = EDITOR,
            arguments = listOf(
                navArgument("platform") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType }
            )
        ) { entry ->
            val platform = Platform.fromName(entry.arguments?.getString("platform"))
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(app.repository, platform, id))
            EditorScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(HISTORY) {
            val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(app.repository))
            HistoryScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onOpen = { nav.navigate("editor/${it.platform.name}/${it.id}") }
            )
        }
    }
}
