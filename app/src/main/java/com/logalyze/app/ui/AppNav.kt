package com.logalyze.app.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.logalyze.app.IncomingLog
import com.logalyze.app.LocaleHelper
import com.logalyze.app.R
import com.logalyze.app.analysis.LogAnalyzer
import kotlinx.coroutines.launch

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home("home", R.string.nav_home, Icons.Filled.Home),
    History("history", R.string.nav_history, Icons.Filled.List),
    Knowledge("knowledge", R.string.nav_knowledge, Icons.Filled.Search),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings)
}

@Composable
fun AppNav(incoming: IncomingLog) {
    // Skip the splash when the app was opened from "Share log".
    var showSplash by rememberSaveable { mutableStateOf(incoming.file == null) }
    if (showSplash) {
        SplashScreen(onDone = { showSplash = false })
    } else {
        MainScaffold(incoming)
    }
}

@Composable
fun Wordmark(size: Int) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append("Log") }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("alyze") }
        },
        fontSize = size.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
fun MainScaffold(incoming: IncomingLog) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var analyzing by remember { mutableStateOf(false) }
    var stage by remember { mutableIntStateOf(0) }
    val vm: HistoryViewModel = viewModel()
    val history by vm.items.collectAsState()
    var shownId by rememberSaveable { mutableStateOf<Long?>(null) }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    fun goTo(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun startAnalysis(file: LogFile) {
        if (analyzing) return
        scope.launch {
            stage = 0
            analyzing = true
            try {
                val lang = LocaleHelper.current(context)
                val result = LogAnalyzer.analyze(context, file.uri, lang) { stage = it }
                val now = System.currentTimeMillis()
                val item = HistoryItem(now, file.name, result, now)
                vm.add(item)
                shownId = item.id
                nav.navigate("result")
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_read_fail),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                analyzing = false
            }
        }
    }

    // A log shared from a launcher is analyzed right away.
    LaunchedEffect(incoming.file) {
        val shared = incoming.file
        if (shared != null) {
            incoming.file = null
            startAnalysis(shared)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { tab ->
                        val label = stringResource(tab.label)
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = { goTo(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Tab.Home.route,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(120)) }
            ) {
                composable(Tab.Home.route) {
                    HomeScreen(
                        onOpenHistory = { goTo(Tab.History.route) },
                        analyzing = analyzing,
                        onAnalyze = { file -> startAnalysis(file) }
                    )
                }
                composable(Tab.History.route) {
                    HistoryScreen(
                        items = history,
                        onOpen = { item ->
                            shownId = item.id
                            nav.navigate("result")
                        },
                        onDelete = { item -> vm.delete(item) },
                        onClearAll = { vm.clearAll() }
                    )
                }
                composable(Tab.Knowledge.route) { KnowledgeScreen() }
                composable(Tab.Settings.route) { SettingsScreen() }
                composable("result") {
                    val item = history.firstOrNull { it.id == shownId }
                    if (item != null) {
                        ResultScreen(
                            result = item.result,
                            fileName = item.fileName,
                            resolved = item.resolved,
                            onResolvedChange = { vm.setResolved(item.id, it) },
                            onAskAi = { nav.navigate("chat") },
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
                composable("chat") {
                    val item = history.firstOrNull { it.id == shownId }
                    if (item != null) {
                        ChatScreen(item = item, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
        if (analyzing) AnalyzingOverlay(stage)
    }
}
