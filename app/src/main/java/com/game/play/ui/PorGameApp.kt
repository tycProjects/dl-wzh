package com.game.play.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun PorGameApp(vm: StoreViewModel) {
    val nav = rememberNavController()
    val route = currentRoute(nav)

    // Saat kembali ke app (mis. selesai memasang), perbarui status terpasang -> tombol "Buka".
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshInstalled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route == "apps" || route == "account") {
                Box(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(40.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 12.dp,
                        border = cardBorder(),
                        modifier = Modifier.width(200.dp).height(72.dp)
                    ) {
                        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TabIcon(active = route == "apps", onClick = { switchTab(nav, "apps") }) {
                                Icon(Icons.Outlined.GridView, contentDescription = "Aplikasi")
                            }
                            TabIcon(active = route == "account", onClick = { switchTab(nav, "account") }) {
                                Icon(Icons.Outlined.Person, contentDescription = "Akun")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "apps", modifier = Modifier.padding(padding)) {
            composable("apps") {
                AppsScreen(vm, onDetail = { id -> nav.navigate("detail/" + Uri.encode(id)) }, onSettings = { nav.navigate("settings") })
            }
            composable("account") {
                AccountScreen(vm, onSubmit = { nav.navigate("submit") }, onSettings = { nav.navigate("settings") })
            }
            composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                DetailScreen(vm, id = entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
            }
            composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }, onSubmit = { nav.navigate("submit") }) }
            composable("submit") { SubmitScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}

private fun switchTab(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo("apps") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun currentRoute(nav: NavHostController): String? =
    nav.currentBackStackEntryAsState().value?.destination?.route

@Composable
private fun RowScope.TabIcon(active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        Modifier.weight(1f).fillMaxHeight()
            .clip(RoundedCornerShape(30.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ) { icon() }
    }
}
