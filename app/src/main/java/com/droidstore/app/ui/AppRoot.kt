package com.droidstore.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidstore.app.data.Api
import com.droidstore.app.data.Session
import com.droidstore.app.data.User
import com.droidstore.app.ui.screens.AppDetailScreen
import com.droidstore.app.ui.screens.AppEditorScreen
import com.droidstore.app.ui.screens.CatalogScreen
import com.droidstore.app.ui.screens.LoginScreen
import com.droidstore.app.ui.screens.StudioScreen
import kotlinx.coroutines.launch

/** Экраны приложения. Переходы между ними анимируются в AnimatedContent. */
sealed interface Screen {
    data object Catalog : Screen
    data object Studio : Screen
    data object Profile : Screen
    data class Detail(val appId: Int) : Screen
    data class Editor(val studioId: Int, val appId: Int?) : Screen
}

@Composable
fun AppRoot() {
    var loggedIn by remember { mutableStateOf(Session.isLoggedIn) }
    if (!loggedIn) {
        LoginScreen(onLoggedIn = { loggedIn = true })
        return
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Catalog) }
    val tab: Screen? = when (screen) {
        is Screen.Catalog, is Screen.Studio, is Screen.Profile -> screen
        else -> null
    }
    // Системная кнопка «Назад» возвращает к каталогу из вложенных экранов
    BackHandler(enabled = tab == null) { screen = Screen.Catalog }

    Scaffold(
        bottomBar = {
            if (tab != null) {
                NavigationBar {
                    listOf(
                        Triple(Screen.Catalog, "Каталог", "⌂"),
                        Triple(Screen.Studio, "Студия", "✎"),
                        Triple(Screen.Profile, "Профиль", "◉"),
                    ).forEach { (target, label, icon) ->
                        NavigationBarItem(
                            selected = screen == target,
                            onClick = { screen = target },
                            icon = { Text(icon) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.padding(padding),
            transitionSpec = {
                // Вложенные экраны выезжают справа, вкладки плавно сменяют друг друга
                val nested = targetState is Screen.Detail || targetState is Screen.Editor
                if (nested) {
                    (slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { -it / 6 } + fadeOut(tween(200)))
                } else {
                    fadeIn(tween(250)) togetherWith fadeOut(tween(150))
                }
            },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.Catalog -> CatalogScreen(onOpenApp = { screen = Screen.Detail(it) })
                Screen.Studio -> StudioScreen(onOpenEditor = { studioId, appId -> screen = Screen.Editor(studioId, appId) })
                Screen.Profile -> ProfileScreen(onLoggedOut = { loggedIn = false })
                is Screen.Detail -> AppDetailScreen(appId = current.appId, onBack = { screen = Screen.Catalog })
                is Screen.Editor -> AppEditorScreen(studioId = current.studioId, appId = current.appId, onDone = { screen = Screen.Studio })
            }
        }
    }
}

@Composable
private fun ProfileScreen(onLoggedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { user = User.from(Api.me().getJSONObject("user")) } catch (e: Exception) { error = e.message }
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)
        user?.let {
            Text(it.name, style = MaterialTheme.typography.titleMedium)
            Text(it.email, style = MaterialTheme.typography.bodyMedium)
            Text("Роль: ${it.role}", style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            scope.launch {
                Api.logout()
                Session.clear()
                onLoggedOut()
            }
        }) { Text("Выйти") }
    }
}
