package dev.tajim.jarvis.ui.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.tajim.jarvis.AppContainer
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.about.AboutScreen
import dev.tajim.jarvis.ui.ailab.AiLabScreen
import dev.tajim.jarvis.ui.chat.ChatScreen
import dev.tajim.jarvis.ui.chat.ChatViewModel
import dev.tajim.jarvis.ui.common.NotBuiltScreen
import dev.tajim.jarvis.ui.home.HomeScreen
import dev.tajim.jarvis.ui.home.HomeViewModel
import dev.tajim.jarvis.ui.memory.MemoryScreen
import dev.tajim.jarvis.ui.memory.MemoryViewModel
import dev.tajim.jarvis.ui.settings.AiProviderPage
import dev.tajim.jarvis.ui.settings.AppearancePage
import dev.tajim.jarvis.ui.settings.HandsFreePage
import dev.tajim.jarvis.ui.settings.LanguagePage
import dev.tajim.jarvis.ui.settings.PermissionsPage
import dev.tajim.jarvis.ui.settings.PersonaPage
import dev.tajim.jarvis.ui.settings.SettingsScreen
import dev.tajim.jarvis.ui.settings.SettingsViewModel
import dev.tajim.jarvis.ui.settings.WeatherPage
import dev.tajim.jarvis.ui.theme.LocalJarvisMotion
import dev.tajim.jarvis.ui.updates.UpdatesScreen
import dev.tajim.jarvis.ui.workspace.TextLabScreen
import dev.tajim.jarvis.ui.workspace.TextLabViewModel
import dev.tajim.jarvis.ui.workspace.WorkspacePlaceholder
import dev.tajim.jarvis.voice.MicPermission

/**
 * App frame: one NavHost plus the bottom bar on the four tab screens.
 *
 * Workspace engine: a workspace is its own destination. While it is on top, Home is not in composition, so the Orb,
 * panels, rails, lines and Home cards are gone and cannot take touches. Back, Close and Android Back all pop to the
 * previous screen. The background assistant (if switched on) keeps running while a workspace is open.
 */
@Composable
fun JarvisShell(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val assistant = container.assistant
    val assistantState by assistant.state.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route != null && route in Routes.tabs
    val ms = if (LocalJarvisMotion.current.reduced) 0 else 220

    fun navigateTab(target: String) {
        navController.navigate(target) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) assistant.tapToTalk() else assistant.micDenied()
    }
    val onMicClick: () -> Unit = {
        if (route != Routes.HOME && route != Routes.CHAT) navigateTab(Routes.HOME) // answers show on Home or in Chat
        if (MicPermission.isGranted(context)) assistant.tapToTalk() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // A one-off tap-to-talk is cancelled when a workspace opens; the background assistant is left running.
    LaunchedEffect(route) { if (route != null && route !in Routes.tabs && !assistant.isRunning) assistant.stop() }

    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                enterTransition = { fadeIn(tween(ms)) },
                exitTransition = { fadeOut(tween(ms)) },
                popEnterTransition = { fadeIn(tween(ms)) },
                popExitTransition = { fadeOut(tween(ms)) },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel(factory = HomeViewModel.factory(container)),
                        onMicClick = onMicClick,
                        onOpenWorkspace = { navController.navigate(Routes.workspace(it)) },
                        onOpenSettings = { navigateTab(Routes.SETTINGS) },
                        onOpenAbout = { navController.navigate(Routes.ABOUT) },
                        onOpenWeatherSettings = { navController.navigate(Routes.S_WEATHER) },
                        onOpenMemory = { navController.navigate(Routes.MEMORY) },
                        onOpenLanguage = { navController.navigate(Routes.S_LANGUAGE) },
                    )
                }
                composable(Routes.CHAT) {
                    ChatScreen(
                        viewModel = viewModel(factory = ChatViewModel.factory(container)),
                        onMicClick = onMicClick,
                        onOpenAiSettings = { navController.navigate(Routes.S_AI) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel(factory = SettingsViewModel.factory(container)),
                        onNavigate = { navController.navigate(it) },
                    )
                }
                composable(Routes.UPDATES) { UpdatesScreen() }
                composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.MEMORY) {
                    MemoryScreen(viewModel(factory = MemoryViewModel.factory(container)), onBack = { navController.popBackStack() })
                }
                composable(Routes.S_HANDSFREE) {
                    HandsFreePage(
                        viewModel(factory = SettingsViewModel.factory(container)),
                        onBack = { navController.popBackStack() },
                        onOpenPermissions = { navController.navigate(Routes.S_PERMISSIONS) },
                    )
                }
                composable(Routes.S_LANGUAGE) { LanguagePage(viewModel(factory = SettingsViewModel.factory(container)), { navController.popBackStack() }) }
                composable(Routes.S_PERSONA) { PersonaPage(viewModel(factory = SettingsViewModel.factory(container)), { navController.popBackStack() }) }
                composable(Routes.S_AI) { AiProviderPage(viewModel(factory = SettingsViewModel.factory(container)), { navController.popBackStack() }) }
                composable(Routes.S_WEATHER) { WeatherPage(viewModel(factory = SettingsViewModel.factory(container)), { navController.popBackStack() }) }
                composable(Routes.S_PERMISSIONS) { PermissionsPage { navController.popBackStack() } }
                composable(Routes.S_APPEARANCE) { AppearancePage(viewModel(factory = SettingsViewModel.factory(container)), { navController.popBackStack() }) }
                composable(
                    Routes.WORKSPACE,
                    arguments = listOf(navArgument(Routes.WORKSPACE_ARG) { type = NavType.StringType }),
                ) { backEntry ->
                    val workspace = Workspace.fromKey(backEntry.arguments?.getString(Routes.WORKSPACE_ARG))
                    val close: () -> Unit = { navController.popBackStack() }
                    when (workspace) {
                        null -> NotBuiltScreen(title = stringResource(R.string.not_built_title), onBack = close)
                        Workspace.TEXT_LAB -> TextLabScreen(viewModel(factory = TextLabViewModel.factory(container)), close)
                        Workspace.AI_LAB -> AiLabScreen(
                            onClose = close,
                            onOpenChat = { navController.popBackStack(); navigateTab(Routes.CHAT) },
                            onOpenAiSettings = { navController.navigate(Routes.S_AI) },
                        )
                        else -> WorkspacePlaceholder(workspace, close)
                    }
                }
            }
        }
        if (showBar) {
            JarvisBottomBar(
                selectedRoute = route,
                onNavigate = { navigateTab(it) },
                micActive = assistantState.busy,
                onMicClick = onMicClick,
            )
        }
    }
}
