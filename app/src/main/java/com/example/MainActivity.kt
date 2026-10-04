package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppPreferences
import com.example.model.AppUiColor
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.JnmfLogo
import com.example.ui.components.KickSongDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.AudioEngineStatusScreen
import com.example.ui.screens.AudioTestScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.KickedSongsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingSheet
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.PossibleDuplicatesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfNeonBright
import com.example.ui.theme.JnmfNeonGreen
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfTheme
import com.example.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

enum class MainTab(val title: String, val icon: ImageVector) {
    LIBRARY("Library", Icons.Default.LibraryMusic),
    PLAYLISTS("Playlists", Icons.Default.QueueMusic),
    EQUALIZER("Equalizer", Icons.Default.Equalizer),
    SETTINGS("Settings", Icons.Default.Settings)
}

enum class SubScreen {
    NONE,
    KICKED_SONGS,
    POSSIBLE_DUPLICATES,
    AUDIO_ENGINE_STATUS,
    AUDIO_TEST
}

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by viewModel.preferences.collectAsState()
            JnmfTheme(
                uiColor = preferences.uiColor,
                language = preferences.language
            ) {
                val strings = com.example.ui.i18n.LocalAppStrings.current
                val colors = JnmfTheme.colors
                val songs by viewModel.songs.collectAsState()
                val kickedSongs by viewModel.kickedSongs.collectAsState()
                val possibleDuplicates by viewModel.possibleDuplicates.collectAsState()
                val isDuplicateScanning by viewModel.isDuplicateScanning.collectAsState()

                val playlists by viewModel.playlists.collectAsState()
                val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
                val songsInSelectedPlaylist by viewModel.songsInSelectedPlaylist.collectAsState()

                val currentSong by viewModel.currentSong.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val currentPositionMs by viewModel.currentPositionMs.collectAsState()
                val durationMs by viewModel.durationMs.collectAsState()
                val repeatMode by viewModel.repeatMode.collectAsState()
                val isShuffle by viewModel.isShuffle.collectAsState()

                val equalizerBands by viewModel.equalizerBands.collectAsState()
                val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()
                val currentPreset by viewModel.currentPreset.collectAsState()
                val bassBoostStrength by viewModel.bassBoostStrength.collectAsState()
                val bassBoostEnabled by viewModel.bassBoostEnabled.collectAsState()
                val stereoWidthStrength by viewModel.stereoWidthStrength.collectAsState()
                val depth3DStrength by viewModel.depth3DStrength.collectAsState()
                val clarityStrength by viewModel.clarityStrength.collectAsState()
                val limiterEnabled by viewModel.limiterEnabled.collectAsState()
                val twsHeadsetMode by viewModel.twsHeadsetMode.collectAsState()
                val audioEngineStatus by viewModel.audioEngineStatus.collectAsState()
                val audioTestResults by viewModel.audioTestResults.collectAsState()
                val equalizerCompatibilityReport by viewModel.equalizerCompatibilityReport.collectAsState()
                val bassGainDb by viewModel.bassGainDb.collectAsState()
                val midGainDb by viewModel.midGainDb.collectAsState()
                val trebleGainDb by viewModel.trebleGainDb.collectAsState()

                val isScanning by viewModel.isScanning.collectAsState()
                val isNowPlayingOpen by viewModel.nowPlayingOpen.collectAsState()
                val songForAddToPlaylist by viewModel.songForAddToPlaylist.collectAsState()
                val songToKick by viewModel.songToKick.collectAsState()

                val scope = rememberCoroutineScope()

                var currentTab by rememberSaveable(
                    stateSaver = Saver(
                        save = { it.name },
                        restore = { try { MainTab.valueOf(it) } catch (e: Exception) { MainTab.LIBRARY } }
                    )
                ) { mutableStateOf(MainTab.LIBRARY) }

                var activeSubScreen by rememberSaveable(
                    stateSaver = Saver(
                        save = { it.name },
                        restore = { try { SubScreen.valueOf(it) } catch (e: Exception) { SubScreen.NONE } }
                    )
                ) { mutableStateOf(SubScreen.NONE) }

                var showCreatePlaylistFromAddDialog by remember { mutableStateOf(false) }

                var isReloadingTheme by remember { mutableStateOf(false) }
                var showPostReloadTransition by rememberSaveable { mutableStateOf(false) }
                var reloadMessage by rememberSaveable { mutableStateOf("Applying Theme...") }
                var reloadTargetColorId by rememberSaveable { mutableStateOf("") }

                LaunchedEffect(showPostReloadTransition) {
                    if (showPostReloadTransition) {
                        kotlinx.coroutines.delay(250)
                        showPostReloadTransition = false
                    }
                }

                // Audio File Picker launcher
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    uris.forEach { uri ->
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {
                            // Some providers do not support persistable permissions
                        }
                        viewModel.importAudioUri(uri)
                    }
                }

                // Storage permissions launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions.values.any { it }
                    if (granted) {
                        viewModel.scanDeviceAudio()
                    }
                }

                LaunchedEffect(Unit) {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_AUDIO,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(permissions)
                }

                // Subscreen back handler
                BackHandler(enabled = activeSubScreen != SubScreen.NONE) {
                    activeSubScreen = SubScreen.NONE
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(JnmfDarkBg)
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (activeSubScreen == SubScreen.NONE) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Persistent Bottom Mini Player
                                    AnimatedVisibility(
                                        visible = currentSong != null && !isNowPlayingOpen,
                                        enter = slideInVertically { it },
                                        exit = slideOutVertically { it }
                                    ) {
                                        MiniPlayer(
                                            currentSong = currentSong,
                                            isPlaying = isPlaying,
                                            currentPositionMs = currentPositionMs,
                                            durationMs = durationMs,
                                            onPlayPause = { viewModel.togglePlayPause() },
                                            onNext = { viewModel.playNext() },
                                            onClick = { viewModel.openNowPlaying() }
                                        )
                                    }

                                    // Cyber Terminal Bottom Navigation Bar
                                    NavigationBar(
                                        containerColor = JnmfSurface,
                                        modifier = Modifier
                                            .border(0.5.dp, JnmfBorder)
                                            .testTag("main_navigation_bar")
                                    ) {
                                        MainTab.entries.forEach { tab ->
                                            val isSelected = currentTab == tab
                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = {
                                                    currentTab = tab
                                                    activeSubScreen = SubScreen.NONE
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = tab.title
                                                    )
                                                },
                                                label = {
                                                    val localizedTabTitle = when (tab) {
                                                        MainTab.LIBRARY -> strings.tabLibrary
                                                        MainTab.PLAYLISTS -> strings.tabPlaylists
                                                        MainTab.EQUALIZER -> strings.tabEqualizer
                                                        MainTab.SETTINGS -> strings.tabSettings
                                                    }
                                                    Text(
                                                        text = localizedTabTitle,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = colors.bright,
                                                    selectedTextColor = colors.bright,
                                                    unselectedIconColor = JnmfTextMuted,
                                                    unselectedTextColor = JnmfTextMuted,
                                                    indicatorColor = colors.primary.copy(alpha = 0.18f)
                                                ),
                                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeSubScreen) {
                                SubScreen.KICKED_SONGS -> {
                                    KickedSongsScreen(
                                        kickedSongs = kickedSongs,
                                        onBack = { activeSubScreen = SubScreen.NONE },
                                        onRestoreSong = { id -> viewModel.restoreKickedSong(id) },
                                        onUpdateSong = { updated -> viewModel.updateKickedSong(updated) }
                                    )
                                }
                                SubScreen.POSSIBLE_DUPLICATES -> {
                                    PossibleDuplicatesScreen(
                                        duplicates = possibleDuplicates,
                                        isScanning = isDuplicateScanning,
                                        onBack = { activeSubScreen = SubScreen.NONE },
                                        onRescan = { viewModel.scanDuplicates() },
                                        onPreviewSong = { song -> viewModel.previewSong(song) },
                                        onDeleteSongPermanently = { song -> viewModel.deleteDuplicateFilePermanently(song) }
                                    )
                                }
                                SubScreen.AUDIO_ENGINE_STATUS -> {
                                    AudioEngineStatusScreen(
                                        status = audioEngineStatus,
                                        onBack = { activeSubScreen = SubScreen.NONE },
                                        onRefresh = { viewModel.refreshAudioHardwareStatus() }
                                    )
                                }
                                SubScreen.AUDIO_TEST -> {
                                    AudioTestScreen(
                                        testResults = audioTestResults,
                                        onRunTest = { type -> viewModel.runAudioTest(type) },
                                        onRunAllTests = { viewModel.runAllAudioTests() },
                                        onBack = { activeSubScreen = SubScreen.NONE }
                                    )
                                }
                                SubScreen.NONE -> {
                                    when (currentTab) {
                                        MainTab.LIBRARY -> {
                                            LibraryScreen(
                                                songs = songs,
                                                currentSong = currentSong,
                                                isPlaying = isPlaying,
                                                isScanning = isScanning,
                                                onPlaySong = { song -> viewModel.playSong(song) },
                                                onPlayAll = {
                                                    if (songs.isNotEmpty()) viewModel.playQueue(songs, 0)
                                                },
                                                onScanDevice = {
                                                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                                                    } else {
                                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                                    }
                                                    permissionLauncher.launch(permissions)
                                                },
                                                onImportFiles = {
                                                    filePickerLauncher.launch(arrayOf("audio/*"))
                                                },
                                                onLoadDemoTracks = { viewModel.reloadDemoTracks() },
                                                onAddToPlaylist = { song -> viewModel.promptAddToPlaylist(song) },
                                                onKickSong = { song -> viewModel.promptKickSong(song) }
                                            )
                                        }
                                        MainTab.PLAYLISTS -> {
                                            PlaylistScreen(
                                                playlists = playlists,
                                                selectedPlaylist = selectedPlaylist,
                                                songsInPlaylist = songsInSelectedPlaylist,
                                                allLibrarySongs = songs,
                                                currentSong = currentSong,
                                                isPlaying = isPlaying,
                                                onSelectPlaylist = { pl -> viewModel.selectPlaylist(pl) },
                                                onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                                onRenamePlaylist = { id, name -> viewModel.renamePlaylist(id, name) },
                                                onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                                                onPlayPlaylist = { list, startIdx ->
                                                    if (list.isNotEmpty()) viewModel.playQueue(list, startIdx)
                                                },
                                                onRemoveSongFromPlaylist = { plId, sId ->
                                                    viewModel.removeSongFromPlaylist(plId, sId)
                                                },
                                                onReorderSongs = { plId, orderedIds ->
                                                    viewModel.reorderPlaylist(plId, orderedIds)
                                                },
                                                onAddSongsToPlaylist = { plId, sIds ->
                                                    sIds.forEach { sId ->
                                                        viewModel.addSongToPlaylist(plId, sId)
                                                    }
                                                }
                                            )
                                        }
                                        MainTab.EQUALIZER -> {
                                            EqualizerScreen(
                                                bands = equalizerBands,
                                                isEqualizerEnabled = equalizerEnabled,
                                                currentPreset = currentPreset,
                                                bassBoostStrength = bassBoostStrength,
                                                isBassBoostEnabled = bassBoostEnabled,
                                                stereoWidthStrength = stereoWidthStrength,
                                                depth3DStrength = depth3DStrength,
                                                clarityStrength = clarityStrength,
                                                limiterEnabled = limiterEnabled,
                                                twsHeadsetMode = twsHeadsetMode,
                                                bassGainDb = bassGainDb,
                                                midGainDb = midGainDb,
                                                trebleGainDb = trebleGainDb,
                                                compatibilityReport = equalizerCompatibilityReport,
                                                onRunStrictCompatibilityCheck = { viewModel.runStrictEqualizerCompatibilityCheck() },
                                                onToggleEqualizer = { viewModel.setEqualizerEnabled(it) },
                                                onSetBandLevel = { band, level -> viewModel.setBandLevel(band, level) },
                                                onSetBassGain = { viewModel.setBassGain(it) },
                                                onSetMidGain = { viewModel.setMidGain(it) },
                                                onSetTrebleGain = { viewModel.setTrebleGain(it) },
                                                onSelectPreset = { preset -> viewModel.setPreset(preset) },
                                                onSetBassBoostStrength = { viewModel.setBassBoostStrength(it) },
                                                onToggleBassBoost = { viewModel.setBassBoostEnabled(it) },
                                                onSetStereoWidthStrength = { viewModel.setStereoWidthStrength(it) },
                                                onSetDepth3DStrength = { viewModel.setDepth3DStrength(it) },
                                                onSetClarityStrength = { viewModel.setClarityStrength(it) },
                                                onSetLimiterEnabled = { viewModel.setLimiterEnabled(it) },
                                                onSetTwsHeadsetMode = { viewModel.setTwsHeadsetMode(it) }
                                            )
                                        }
                                        MainTab.SETTINGS -> {
                                            SettingsScreen(
                                                preferences = preferences,
                                                totalSongCount = songs.size,
                                                kickedSongCount = kickedSongs.size,
                                                possibleDuplicateCount = possibleDuplicates.size,
                                                totalPlaylistCount = playlists.size,
                                                onUpdatePreferences = { prefs ->
                                                    viewModel.updatePreferences(prefs)
                                                    viewModel.setBassBoostStrength(prefs.bassBoostStrength)
                                                    viewModel.setStereoWidthStrength(prefs.stereoWidthStrength)
                                                    viewModel.setDepth3DStrength(prefs.depth3DStrength)
                                                    viewModel.setClarityStrength(prefs.clarityStrength)
                                                    viewModel.setLimiterEnabled(prefs.limiterEnabled)
                                                    viewModel.setTwsHeadsetMode(prefs.twsHeadsetMode)
                                                },
                                                onSelectUiColor = { color ->
                                                    if (color != preferences.uiColor) {
                                                        reloadTargetColorId = color.name
                                                        reloadMessage = "Applying Theme..."
                                                        isReloadingTheme = true
                                                        showPostReloadTransition = true
                                                        scope.launch {
                                                            viewModel.setUiColor(color).join()
                                                            kotlinx.coroutines.delay(350)
                                                            recreate()
                                                        }
                                                    }
                                                },
                                                onSelectLanguage = { lang ->
                                                    if (lang != preferences.language) {
                                                        reloadTargetColorId = preferences.uiColor.name
                                                        reloadMessage = "Loading UI..."
                                                        isReloadingTheme = true
                                                        showPostReloadTransition = true
                                                        scope.launch {
                                                            viewModel.setLanguage(lang).join()
                                                            kotlinx.coroutines.delay(350)
                                                            recreate()
                                                        }
                                                    }
                                                },
                                                onScanLibrary = { viewModel.scanDeviceAudio() },
                                                onReloadDemoTracks = { viewModel.reloadDemoTracks() },
                                                onOpenEqualizer = { currentTab = MainTab.EQUALIZER },
                                                onOpenAudioEngineStatus = { activeSubScreen = SubScreen.AUDIO_ENGINE_STATUS },
                                                onOpenAudioTest = { activeSubScreen = SubScreen.AUDIO_TEST },
                                                onOpenKickedSongs = { activeSubScreen = SubScreen.KICKED_SONGS },
                                                onOpenPossibleDuplicates = { activeSubScreen = SubScreen.POSSIBLE_DUPLICATES }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Full Screen Now Playing Sheet
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    if (isNowPlayingOpen && currentSong != null) {
                        ModalBottomSheet(
                            onDismissRequest = { viewModel.closeNowPlaying() },
                            sheetState = sheetState,
                            containerColor = JnmfDarkBg,
                            dragHandle = null,
                            modifier = Modifier.testTag("now_playing_modal_sheet")
                        ) {
                            NowPlayingSheet(
                                song = currentSong,
                                isPlaying = isPlaying,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                isShuffle = isShuffle,
                                repeatMode = repeatMode,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onPrevious = { viewModel.playPrevious() },
                                onNext = { viewModel.playNext() },
                                onSeek = { pos -> viewModel.seekTo(pos) },
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                onCycleRepeat = { viewModel.cycleRepeatMode() },
                                onOpenEqualizer = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        viewModel.closeNowPlaying()
                                        currentTab = MainTab.EQUALIZER
                                    }
                                },
                                onAddToPlaylist = {
                                    currentSong?.let { viewModel.promptAddToPlaylist(it) }
                                },
                                onCollapse = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        viewModel.closeNowPlaying()
                                    }
                                }
                            )
                        }
                    }

                    // Add to Playlist Dialog
                    songForAddToPlaylist?.let { targetSong ->
                        AddToPlaylistDialog(
                            song = targetSong,
                            playlists = playlists,
                            onDismiss = { viewModel.dismissAddToPlaylist() },
                            onSelectPlaylist = { pl ->
                                viewModel.addSongToPlaylist(pl.id, targetSong.id)
                                viewModel.dismissAddToPlaylist()
                            },
                            onCreateNewPlaylist = {
                                viewModel.dismissAddToPlaylist()
                                showCreatePlaylistFromAddDialog = true
                            }
                        )
                    }

                    if (showCreatePlaylistFromAddDialog) {
                        CreatePlaylistDialog(
                            onDismiss = { showCreatePlaylistFromAddDialog = false },
                            onConfirm = { name ->
                                showCreatePlaylistFromAddDialog = false
                                viewModel.createPlaylist(name)
                            }
                        )
                    }

                    // Kick Song Confirmation Dialog
                    songToKick?.let { targetSong ->
                        KickSongDialog(
                            song = targetSong,
                            onDismiss = { viewModel.promptKickSong(null) },
                            onConfirm = { notes ->
                                viewModel.confirmKickSong(targetSong, notes)
                            }
                        )
                    }

                    // Smooth Theme Reload Loading Overlay (Single Tap Immediate Transition)
                    val isShowingReloadOverlay = isReloadingTheme || showPostReloadTransition
                    if (isShowingReloadOverlay) {
                        val targetColor = if (reloadTargetColorId.isNotEmpty()) {
                            try { AppUiColor.valueOf(reloadTargetColorId) } catch (e: Exception) { preferences.uiColor }
                        } else preferences.uiColor

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(JnmfDarkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                JnmfLogo(size = 72.dp)
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "JNMF",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    color = targetColor.primary,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = reloadMessage,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = JnmfTextSecondary
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                CircularProgressIndicator(
                                    color = targetColor.primary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
