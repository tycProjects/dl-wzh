package com.meloza.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.ExperimentalMaterial3Api
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.meloza.app.MelozaApp
import com.meloza.app.R
import com.meloza.app.data.LibraryState
import com.meloza.app.ui.components.Artwork
import com.meloza.app.ui.screens.HomeScreen
import com.meloza.app.ui.screens.LibraryScreen
import com.meloza.app.ui.screens.NowPlayingScreen
import com.meloza.app.ui.screens.ProfileScreen
import com.meloza.app.ui.screens.SearchScreen
import com.meloza.app.ui.screens.TrackListScreen
import com.meloza.app.ui.screens.TutorialScreen
import com.meloza.app.ui.theme.MelozaTheme
import com.meloza.app.ui.theme.extractAccent
import com.meloza.app.ui.components.SoundWave
import com.meloza.app.ui.components.glass
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Shape
import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val prefs: SharedPreferences by lazy { getSharedPreferences("meloza_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MelozaApp
        app.player.connect()

        setContent {
            val darkPref = prefs.getInt("theme", 0) // 0 system, 1 light, 2 dark
            val dynamicAccent = prefs.getBoolean("artwork_accent", true)
            val dark = when (darkPref) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val playerState by app.player.state.collectAsState()
            var accentColor by remember { mutableStateOf<Color?>(null) }
            val context = LocalContext.current

            // Dynamic colors: sample the playing album's artwork.
            LaunchedEffect(playerState.current?.id, dynamicAccent) {
                val track = playerState.current
                accentColor = if (!dynamicAccent || track == null) null else {
                    val req = ImageRequest.Builder(context)
                        .data(track.artworkUri)
                        .allowHardware(false)
                        .size(128)
                        .build()
                    val result = context.imageLoader.execute(req)
                    val bitmap = (result as? SuccessResult)?.drawable?.let { d ->
                        (d as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    } as Bitmap?
                    extractAccent(bitmap)
                }
            }

            MelozaTheme(accent = accentColor, darkTheme = dark) {
                AppRoot(app)
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) (application as MelozaApp).player.disconnect()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(app: MelozaApp) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("meloza_prefs", Context.MODE_PRIVATE) }
    var showTutorial by remember { mutableStateOf(!prefs.getBoolean("tutorial_done", false)) }

    val hasPermission = remember { mutableStateOf(hasAudioPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission.value = granted
    }
    LaunchedEffect(hasPermission.value) {
        app.repository.refresh(hasPermission.value)
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (!hasPermission.value) launcher.launch(audioPermission())
        // Every track that actually starts is written to the listening history.
        app.player.onTrackStarted = { track -> scope.launch { app.repository.recordPlay(track.id) } }
    }
    if (showTutorial) {
        TutorialScreen(onFinish = {
            prefs.edit().putBoolean("tutorial_done", true).apply()
            showTutorial = false
        })
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val playerState by app.player.state.collectAsState()
    val showMini = playerState.current != null && route != "now_playing"

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
            ) {
                composable("home") { HomeScreen(app, nav, hasPermission.value, { launcher.launch(audioPermission()) }) }
                composable("search") { SearchScreen(app, nav) }
                composable("library") { LibraryScreen(app, nav) }
                composable("profile") { ProfileScreen(app, nav) }
                composable("now_playing") { NowPlayingScreen(app, nav) }
                composable(
                    "list/{kind}/{key}",
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("key") { type = NavType.StringType },
                    ),
                ) { entry ->
                    TrackListScreen(
                        app, nav,
                        kind = entry.arguments?.getString("kind") ?: "",
                        key = entry.arguments?.getString("key") ?: "",
                    )
                }
            }

            // Mini player above the bottom bar.
            AnimatedVisibility(
                visible = showMini && route in setOf("home", "search", "library", "profile"),
                enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                MiniPlayer(
                    app = app,
                    onOpen = { nav.navigate("now_playing") },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
                )
            }

            if (route in setOf("home", "search", "library", "profile")) {
                BottomBar(route = route, onSelect = { target ->
                    nav.navigate(target) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun BottomBar(route: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .glass(RoundedCornerShape(32.dp), MaterialTheme.colorScheme.surfaceContainer),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        val items = listOf(
            Triple("home", R.string.nav_home, Icons.Default.Home),
            Triple("search", R.string.nav_search, Icons.Default.Search),
            Triple("library", R.string.nav_library, Icons.Default.LibraryMusic),
            Triple("profile", R.string.nav_profile, Icons.Default.Person),
        )
        items.forEach { (r, label, icon) ->
            NavigationBarItem(
                selected = route == r,
                onClick = { onSelect(r) },
                icon = { Icon(icon, contentDescription = stringResource(label)) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun MiniPlayer(app: MelozaApp, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val state by app.player.state.collectAsState()
    val track = state.current ?: return
    Surface(
        modifier = modifier.fillMaxWidth()
            .glass(RoundedCornerShape(28.dp), MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onOpen),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(track.artworkUri, Modifier.size(48.dp), RoundedCornerShape(16.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(track.artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
            }
            SoundWave(state.isPlaying, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.size(width = 28.dp, height = 20.dp), bars = 5)
            IconButton(onClick = { app.player.togglePlay() }) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (state.isPlaying) R.string.action_pause else R.string.action_play),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            IconButton(onClick = { app.player.next() }) {
                Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.action_next),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

fun hasAudioPermission(context: Context): Boolean =
    context.checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED
