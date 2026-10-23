package com.example.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationManagerCompat
import com.example.ui.components.NotificationDialog
import com.example.ui.i18n.JnmfStrings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.analysis.AudioAnalysisEngine
import com.example.data.db.FavoriteSongEntity
import com.example.data.db.HiddenSongEntity
import com.example.data.db.JnmfDatabase
import com.example.data.db.PlaylistEntity
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.MediaStoreRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SortOption
import com.example.data.repository.ThemeColor
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.ui.components.JnmfGlobeLogo
import com.example.ui.components.MiniPlayer
import com.example.ui.components.TaskProgressState
import com.example.ui.components.TaskType
import com.example.ui.screens.ClassifierScreen
import com.example.ui.screens.DuplicateFinderScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StartupLoadingScreen
import com.example.ui.screens.VideoScreen
import com.example.ui.theme.JnmfTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MainTab {
    MUSIC, PLAYLISTS, EQUALIZER, VIDEOS, SETTINGS
}

enum class SubScreen {
    NONE, NOW_PLAYING, DUPLICATES, CLASSIFIER
}

@Composable
fun JnmfApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val database = remember { JnmfDatabase.getDatabase(context) }
    val dao = database.jnmfDao()
    val mediaStoreRepo = remember { MediaStoreRepository(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val analysisEngine = remember { AudioAnalysisEngine(context) }

    // Settings
    val themeColor by settingsRepo.themeColorFlow.collectAsState(initial = ThemeColor.NEON_GREEN)
    val appLanguage by settingsRepo.languageFlow.collectAsState(initial = AppLanguage.ENGLISH)

    // Room Persistent State
    val playlists by dao.getAllPlaylists().collectAsState(initial = emptyList())
    val allPlaylistTracks by dao.getAllPlaylistTracks().collectAsState(initial = emptyList())
    val favoriteIds by dao.getAllFavoriteIds().collectAsState(initial = emptyList())
    val hiddenSongs by dao.getAllHiddenSongs().collectAsState(initial = emptyList())
    val hiddenSongIds = remember(hiddenSongs) { hiddenSongs.map { it.songId }.toSet() }

    // MediaStore State
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var currentSort by remember { mutableStateOf(SortOption.NAME_ASC) }
    var searchQuery by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }

    // Startup & Progress States
    var isStartupFinished by remember { mutableStateOf(false) }
    var startupProgressState by remember {
        mutableStateOf(
            TaskProgressState(
                taskType = TaskType.STARTUP,
                title = "Initializing JNMF Audio Engine",
                progress = 0.1f,
                statusMessage = "Setting up hardware equalizer & playback engine...",
                isRunning = true
            )
        )
    }
    var musicScanState by remember { mutableStateOf(TaskProgressState.IDLE) }
    var videoScanState by remember { mutableStateOf(TaskProgressState.IDLE) }
    var isScanningMusic by remember { mutableStateOf(false) }
    var isScanningVideos by remember { mutableStateOf(false) }

    // Navigation & Overlay State
    var currentTab by remember { mutableStateOf(MainTab.MUSIC) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var songForPlaylistPicker by remember { mutableStateOf<Song?>(null) }
    var newPlaylistInput by remember { mutableStateOf("") }
    var isCreatingNewInPicker by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var isVideoFullscreen by remember { mutableStateOf(false) }

    // Playback State
    val playbackState by MusicController.state.collectAsState()

    // Permission Launcher
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun refreshMedia(isStartup: Boolean = false) {
        if (isScanningMusic) return
        isScanningMusic = true

        musicScanState = TaskProgressState(
            taskType = TaskType.MUSIC_SCAN,
            title = "Scanning Music Library",
            progress = 0f,
            statusMessage = "Indexing local audio files...",
            isRunning = true
        )
        if (isStartup) {
            startupProgressState = TaskProgressState(
                taskType = TaskType.STARTUP,
                title = "Scanning Local Audio Library",
                progress = 0.2f,
                statusMessage = "Loading Indo Remix & Fullbeat tracks...",
                isRunning = true
            )
        }

        scope.launch {
            try {
                val scannedSongs = mediaStoreRepo.queryLocalSongs(
                    sortOption = currentSort,
                    hiddenSongIds = hiddenSongIds,
                    searchQuery = searchQuery,
                    onProgress = { current, total, title ->
                        val fraction = if (total > 0) current.toFloat() / total.toFloat() else null
                        musicScanState = musicScanState.copy(
                            progress = fraction,
                            current = current,
                            total = total,
                            statusMessage = if (total > 0) "Reading: $title ($current/$total)" else "Indexing audio..."
                        )
                        if (isStartup) {
                            startupProgressState = startupProgressState.copy(
                                progress = fraction,
                                current = current,
                                total = total,
                                statusMessage = if (total > 0) "$current / $total tracks ($title)" else "Scanning tracks..."
                            )
                        }
                    }
                )
                songs = scannedSongs
                musicScanState = TaskProgressState(
                    taskType = TaskType.MUSIC_SCAN,
                    title = "Music Library Ready",
                    current = scannedSongs.size,
                    total = scannedSongs.size,
                    statusMessage = "Found ${scannedSongs.size} tracks",
                    isCompleted = true,
                    isRunning = false
                )
                if (isStartup) {
                    startupProgressState = TaskProgressState(
                        taskType = TaskType.STARTUP,
                        title = "JNMF Engine Ready",
                        progress = 1.0f,
                        statusMessage = "Loaded ${scannedSongs.size} tracks. Welcome to JNMF!",
                        isCompleted = true,
                        isRunning = false
                    )
                    delay(700)
                    isStartupFinished = true
                } else {
                    delay(2500)
                    if (musicScanState.isCompleted) {
                        musicScanState = TaskProgressState.IDLE
                    }
                }
            } catch (e: Exception) {
                musicScanState = TaskProgressState(
                    taskType = TaskType.MUSIC_SCAN,
                    title = "Music Scan Error",
                    error = e.localizedMessage ?: "Failed to scan storage",
                    isRunning = false
                )
                if (isStartup) {
                    isStartupFinished = true
                }
            } finally {
                isScanningMusic = false
            }
        }
    }

    fun refreshVideos() {
        if (isScanningVideos) return
        isScanningVideos = true

        videoScanState = TaskProgressState(
            taskType = TaskType.VIDEO_SCAN,
            title = "Scanning Local Videos",
            progress = 0f,
            statusMessage = "Discovering videos on device...",
            isRunning = true
        )

        scope.launch {
            try {
                val scannedVideos = mediaStoreRepo.queryLocalVideos(
                    onProgress = { current, total, title ->
                        val fraction = if (total > 0) current.toFloat() / total.toFloat() else null
                        videoScanState = videoScanState.copy(
                            progress = fraction,
                            current = current,
                            total = total,
                            statusMessage = if (total > 0) "Reading: $title ($current/$total)" else "Indexing videos..."
                        )
                    }
                )
                videos = scannedVideos
                videoScanState = TaskProgressState(
                    taskType = TaskType.VIDEO_SCAN,
                    title = "Video Library Scanned",
                    current = scannedVideos.size,
                    total = scannedVideos.size,
                    statusMessage = "Found ${scannedVideos.size} videos",
                    isCompleted = true,
                    isRunning = false
                )
                delay(2500)
                if (videoScanState.isCompleted) {
                    videoScanState = TaskProgressState.IDLE
                }
            } catch (e: Exception) {
                videoScanState = TaskProgressState(
                    taskType = TaskType.VIDEO_SCAN,
                    title = "Video Scan Error",
                    error = e.localizedMessage ?: "Failed to scan videos",
                    isRunning = false
                )
            } finally {
                isScanningVideos = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        hasPermission = granted
        if (granted) {
            refreshMedia(isStartup = !isStartupFinished)
            refreshVideos()
            PlaybackService.startService(context)
        } else {
            if (!isStartupFinished) {
                isStartupFinished = true
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    LaunchedEffect(currentSort, searchQuery, hiddenSongIds) {
        if (hasPermission && isStartupFinished) {
            refreshMedia()
        }
    }

    // Back handling for overlays
    BackHandler(enabled = currentSubScreen != SubScreen.NONE) {
        currentSubScreen = SubScreen.NONE
    }

    BackHandler(enabled = isVideoFullscreen) {
        isVideoFullscreen = false
    }

    LaunchedEffect(currentTab) {
        if (currentTab != MainTab.VIDEOS) {
            isVideoFullscreen = false
        }
    }

    JnmfTheme(themeColor = themeColor) {
        if (!isStartupFinished) {
            StartupLoadingScreen(
                progressState = startupProgressState,
                onSkipOrReady = { isStartupFinished = true }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (currentTab != MainTab.VIDEOS || !isVideoFullscreen) {
                        // Header Bar with green outline globe logo, branding & quick tool shortcuts
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.statusBarsPadding()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // JNMF Green Outline Globe Logo (🌐)
                                    JnmfGlobeLogo(
                                        size = 32.dp,
                                        animated = false,
                                        showEqualizerWaves = false,
                                        showFlankingWaves = false,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "JNMF",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "JNMF Music Player",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = JnmfStrings.topSubtitle(appLanguage),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Notification button with active/disabled status indicator
                                    IconButton(
                                        onClick = { showNotificationDialog = true },
                                        modifier = Modifier.testTag("nav_notifications_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                                                contentDescription = JnmfStrings.quickNotifications(appLanguage),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            if (!notificationsEnabled) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFF9800))
                                                        .align(Alignment.TopEnd)
                                                )
                                            }
                                        }
                                    }

                                    // Duplicate Detector shortcut
                                    IconButton(
                                        onClick = { currentSubScreen = SubScreen.DUPLICATES },
                                        modifier = Modifier.testTag("nav_duplicates_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterNone,
                                            contentDescription = JnmfStrings.quickDuplicates(appLanguage),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Remix Classifier shortcut
                                    IconButton(
                                        onClick = { currentSubScreen = SubScreen.CLASSIFIER },
                                        modifier = Modifier.testTag("nav_classifier_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = JnmfStrings.quickClassifier(appLanguage),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            bottomBar = {
                if (currentTab != MainTab.VIDEOS || !isVideoFullscreen) {
                    Column {
                        // Floating MiniPlayer docked directly above the navigation bar
                        if (playbackState.currentSong != null && currentSubScreen != SubScreen.NOW_PLAYING) {
                            MiniPlayer(
                                playbackState = playbackState,
                                onPlayPause = { MusicController.togglePlayPause() },
                                onNext = { MusicController.next() },
                                onPrevious = { MusicController.previous() },
                                onClick = { currentSubScreen = SubScreen.NOW_PLAYING }
                            )
                        }

                        // Bottom Navigation Bar
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            NavigationBarItem(
                                selected = currentTab == MainTab.MUSIC,
                                onClick = { currentTab = MainTab.MUSIC },
                                icon = { Icon(Icons.Default.Audiotrack, contentDescription = JnmfStrings.navMusic(appLanguage)) },
                                label = { Text(JnmfStrings.navMusic(appLanguage), fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.PLAYLISTS,
                                onClick = { currentTab = MainTab.PLAYLISTS },
                                icon = { Icon(Icons.Default.QueueMusic, contentDescription = JnmfStrings.navPlaylists(appLanguage)) },
                                label = { Text(JnmfStrings.navPlaylists(appLanguage), fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.EQUALIZER,
                                onClick = { currentTab = MainTab.EQUALIZER },
                                icon = { Icon(Icons.Default.Equalizer, contentDescription = JnmfStrings.navEqualizer(appLanguage)) },
                                label = { Text(JnmfStrings.navEqualizer(appLanguage), fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.VIDEOS,
                                onClick = { currentTab = MainTab.VIDEOS },
                                icon = { Icon(Icons.Default.Videocam, contentDescription = JnmfStrings.navVideos(appLanguage)) },
                                label = { Text(JnmfStrings.navVideos(appLanguage), fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.SETTINGS,
                                onClick = { currentTab = MainTab.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = JnmfStrings.navSettings(appLanguage)) },
                                label = { Text(JnmfStrings.navSettings(appLanguage), fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val contentModifier = if (currentTab == MainTab.VIDEOS && isVideoFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            }
            Box(
                modifier = contentModifier
            ) {
                // Tab Content
                when (currentTab) {
                    MainTab.MUSIC -> {
                        LibraryScreen(
                            songs = songs,
                            playbackState = playbackState,
                            hasPermission = hasPermission,
                            onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
                            onRefresh = { refreshMedia() },
                            onSortSelected = { option ->
                                currentSort = option
                                refreshMedia()
                            },
                            currentSort = currentSort,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            favoriteSongIds = favoriteIds.toSet(),
                            onToggleFavorite = { song ->
                                scope.launch {
                                    if (favoriteIds.contains(song.id)) {
                                        dao.removeFavorite(song.id)
                                    } else {
                                        dao.addFavorite(FavoriteSongEntity(songId = song.id))
                                    }
                                }
                            },
                            onAddToPlaylist = { song ->
                                songForPlaylistPicker = song
                            },
                            onHideSong = { song ->
                                scope.launch {
                                    dao.hideSong(
                                        HiddenSongEntity(
                                            songId = song.id,
                                            title = song.title,
                                            artist = song.artist
                                        )
                                    )
                                    refreshMedia()
                                }
                            },
                            musicScanState = musicScanState,
                            language = appLanguage,
                            onDismissProgress = { musicScanState = TaskProgressState.IDLE }
                        )
                    }
                    MainTab.PLAYLISTS -> {
                        val favoriteSongs = remember(favoriteIds, songs) {
                            val favSet = favoriteIds.toSet()
                            songs.filter { favSet.contains(it.id) }
                        }

                        PlaylistScreen(
                            playlists = playlists,
                            favoriteSongs = favoriteSongs,
                            allSongs = songs,
                            playbackState = playbackState,
                            onCreatePlaylist = { name ->
                                scope.launch { dao.insertPlaylist(PlaylistEntity(name = name)) }
                            },
                            onRenamePlaylist = { id, newName ->
                                scope.launch { dao.renamePlaylist(id, newName) }
                            },
                            onDeletePlaylist = { id ->
                                scope.launch {
                                    dao.deletePlaylistTracks(id)
                                    dao.deletePlaylist(id)
                                }
                            },
                            onAddSongToPlaylist = { plId, songId ->
                                scope.launch { dao.addSongToPlaylist(plId, songId) }
                            },
                            onRemoveSongFromPlaylist = { plId, songId ->
                                scope.launch { dao.removeSongFromPlaylist(plId, songId) }
                            },
                            onMoveTrack = { plId, from, to ->
                                scope.launch { dao.moveTrackInPlaylist(plId, from, to) }
                            },
                            onToggleFavorite = { song ->
                                scope.launch {
                                    if (favoriteIds.contains(song.id)) {
                                        dao.removeFavorite(song.id)
                                    } else {
                                        dao.addFavorite(FavoriteSongEntity(songId = song.id))
                                    }
                                }
                            },
                            getPlaylistTrackIds = { plId ->
                                allPlaylistTracks.filter { it.playlistId == plId }.map { it.songId }
                            }
                        )
                    }
                    MainTab.EQUALIZER -> {
                        EqualizerScreen(
                            equalizerManager = MusicController.equalizerManager,
                            onBack = { currentTab = MainTab.MUSIC }
                        )
                    }
                    MainTab.VIDEOS -> {
                        VideoScreen(
                            videos = videos,
                            videoScanState = videoScanState,
                            onRefresh = { refreshVideos() },
                            onDismissProgress = { videoScanState = TaskProgressState.IDLE },
                            isFullscreen = isVideoFullscreen,
                            onFullscreenChanged = { isVideoFullscreen = it },
                            language = appLanguage
                        )
                    }
                    MainTab.SETTINGS -> {
                        SettingsScreen(
                            currentTheme = themeColor,
                            currentLanguage = appLanguage,
                            hiddenSongs = hiddenSongs,
                            onSelectTheme = { theme ->
                                scope.launch { settingsRepo.setThemeColor(theme) }
                            },
                            onSelectLanguage = { lang ->
                                scope.launch { settingsRepo.setLanguage(lang) }
                            },
                            onUnhideSong = { songId ->
                                scope.launch {
                                    dao.unhideSong(songId)
                                    refreshMedia()
                                }
                            },
                            onOpenNotificationSettings = { showNotificationDialog = true }
                        )
                    }
                }

                // Now Playing Fullscreen Sheet
                AnimatedVisibility(
                    visible = currentSubScreen == SubScreen.NOW_PLAYING,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NowPlayingScreen(
                        playbackState = playbackState,
                        onClose = { currentSubScreen = SubScreen.NONE },
                        onOpenEqualizer = {
                            currentSubScreen = SubScreen.NONE
                            currentTab = MainTab.EQUALIZER
                        }
                    )
                }

                // Duplicate Finder Overlay
                AnimatedVisibility(
                    visible = currentSubScreen == SubScreen.DUPLICATES,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    DuplicateFinderScreen(
                        analysisEngine = analysisEngine,
                        songs = songs,
                        onBack = { currentSubScreen = SubScreen.NONE },
                        onDeleteSongRequested = { song ->
                            // Soft remove from current library view and refresh
                            songs = songs.filter { it.id != song.id }
                        }
                    )
                }

                // Remix Classifier Overlay
                AnimatedVisibility(
                    visible = currentSubScreen == SubScreen.CLASSIFIER,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    ClassifierScreen(
                        songs = songs,
                        onBack = { currentSubScreen = SubScreen.NONE },
                        onManualCorrection = { song, newCategory ->
                            songs = songs.map {
                                if (it.id == song.id) it.copy(remixCategory = newCategory) else it
                            }
                        }
                    )
                }

                // Global Playlist Selector Dialog when adding a song from Library
                if (songForPlaylistPicker != null) {
                    val targetSong = songForPlaylistPicker!!
                    AlertDialog(
                        onDismissRequest = {
                            songForPlaylistPicker = null
                            isCreatingNewInPicker = false
                            newPlaylistInput = ""
                        },
                        title = {
                            Text(
                                text = "Add '${targetSong.title}' to Playlist",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (isCreatingNewInPicker) {
                                    OutlinedTextField(
                                        value = newPlaylistInput,
                                        onValueChange = { newPlaylistInput = it },
                                        label = { Text("New Playlist Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            if (newPlaylistInput.isNotBlank()) {
                                                scope.launch {
                                                    val newId = dao.insertPlaylist(PlaylistEntity(name = newPlaylistInput.trim()))
                                                    dao.addSongToPlaylist(newId, targetSong.id)
                                                    songForPlaylistPicker = null
                                                    isCreatingNewInPicker = false
                                                    newPlaylistInput = ""
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Create & Add Track", color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                } else {
                                    // List existing playlists
                                    if (playlists.isEmpty()) {
                                        Text(
                                            text = "No playlists found. Create one now:",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { isCreatingNewInPicker = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Create New Playlist", color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.height(220.dp)) {
                                            items(playlists, key = { it.id }) { pl ->
                                                val trackCount = allPlaylistTracks.count { it.playlistId == pl.id }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            scope.launch {
                                                                dao.addSongToPlaylist(pl.id, targetSong.id)
                                                                songForPlaylistPicker = null
                                                            }
                                                        }
                                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = pl.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "$trackCount tracks",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(
                                            onClick = { isCreatingNewInPicker = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Create New Playlist", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = {
                                songForPlaylistPicker = null
                                isCreatingNewInPicker = false
                                newPlaylistInput = ""
                            }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                }

                // Non-intrusive playback error banner
                if (playbackState.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = playbackState.errorMessage ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Notification dialog for inspecting and requesting playback permissions
                if (showNotificationDialog) {
                    NotificationDialog(
                        language = appLanguage,
                        onDismiss = {
                            showNotificationDialog = false
                            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                        }
                    )
                }
            }
        }
    }
}
}

