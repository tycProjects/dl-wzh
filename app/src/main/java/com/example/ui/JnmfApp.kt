package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.data.analysis.AudioAnalysisEngine
import com.example.data.db.JnmfDatabase
import com.example.data.db.PlaylistTrackEntity
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.DjModeSettings
import com.example.data.repository.MediaStoreRepository
import com.example.data.repository.RadioScheduleRepository
import com.example.data.repository.SaveResult
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SortOption
import com.example.data.repository.UserDataRepository
import com.example.playback.ActiveMediaSessionOwner
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.QueueSourceType
import com.example.playback.RadioPlaybackController
import com.example.playback.VideoPlaybackController
import com.example.ui.components.JnmfRefreshTransitionBanner
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NotificationDialog
import com.example.ui.components.RadioMiniPlayer
import com.example.ui.components.TaskProgressState
import com.example.ui.components.TaskType
import com.example.ui.components.UiTransitionController
import com.example.ui.components.VideoMiniPlayer
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.RadioDjStrings
import com.example.ui.screens.ClassifierScreen
import com.example.ui.screens.DuplicateFinderScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreenContent
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistScreenContent
import com.example.ui.screens.RadioDjSchedulerScreen
import com.example.ui.screens.RadioPlaylistPickerDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StartupLoadingScreen
import com.example.ui.screens.VideoScreen
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class MainTab {
    MUSIC, PLAYLISTS, EQUALIZER, RADIO_DJ, VIDEOS, SETTINGS;

    companion object {
        fun fromNameOrDefault(name: String?): MainTab {
            return entries.find { it.name == name } ?: MUSIC
        }
    }
}

enum class SubScreen {
    NONE, NOW_PLAYING, DUPLICATES, CLASSIFIER
}

private enum class StartupPhase {
    INIT, SCANNING, READY
}

@Composable
fun JnmfApp(
    openVideoPlayerToken: Long = 0L,
    openRadioSchedulerToken: Long = 0L
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val database = remember { JnmfDatabase.getDatabase(context) }
    val dao = remember(database) { database.jnmfDao() }
    val userDataRepo = remember(database) { UserDataRepository(context, dao) }
    val radioRepo = remember(database) { RadioScheduleRepository(context, dao) }
    val mediaStoreRepo = remember { MediaStoreRepository(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val analysisEngine = remember { AudioAnalysisEngine(context) }
    val transitionController = remember(scope) { UiTransitionController(scope) }
    val transitionState by transitionController.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Immediate boot restoration from synchronous preferences + reactive DataStore sync
    var activeTheme by remember { mutableStateOf(settingsRepo.getInitialThemeColor()) }
    var activeLanguage by remember { mutableStateOf(settingsRepo.getInitialLanguage()) }
    val initialDjSettings = remember { settingsRepo.getInitialDjModeSettings() }
    var djModeEnabled by remember { mutableStateOf(initialDjSettings.enabled) }
    var djTransitionSeconds by remember { mutableStateOf(initialDjSettings.transitionSeconds) }

    LaunchedEffect(Unit) {
        MusicController.bindAppContext(context)
        RadioPlaybackController.bindAppContext(context)
        MusicController.setDjModeSettings(
            enabled = initialDjSettings.enabled,
            transitionSeconds = initialDjSettings.transitionSeconds,
            persist = false
        )
        if (MusicController.state.value.currentSong == null) {
            val savedModes = settingsRepo.getInitialMusicModes()
            MusicController.restorePlaybackModes(
                isShuffle = savedModes.isShuffle,
                repeatState = savedModes.repeatState,
                isReverseQueue = savedModes.isReverseQueue
            )
            val savedEq = settingsRepo.getInitialEqSettings()
            MusicController.equalizerManager.restoreSavedSettings(
                enabled = savedEq.enabled,
                bass = savedEq.bass,
                virtualizer = savedEq.virtualizer,
                preset = savedEq.preset,
                clarity = savedEq.clarity,
                customBandsCsv = savedEq.customBandsCsv
            )
        }
        if (VideoPlaybackController.state.value.currentVideo == null) {
            VideoPlaybackController.setPlaybackMode(settingsRepo.getInitialVideoPlaybackMode())
            VideoPlaybackController.setQueueMode(settingsRepo.getInitialVideoQueueMode())
        }
    }

    // Flush playback position snapshot whenever lifecycle transitions to ON_STOP
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                MusicController.flushPlaybackSnapshotNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(settingsRepo) {
        launch {
            settingsRepo.themeColorFlow.collect { savedTheme ->
                activeTheme = savedTheme
            }
        }
        launch {
            settingsRepo.languageFlow.collect { savedLang ->
                activeLanguage = savedLang
            }
        }
        launch {
            settingsRepo.videoPlaybackModeFlow.collect { savedVideoMode ->
                if (VideoPlaybackController.state.value.playbackMode != savedVideoMode) {
                    VideoPlaybackController.setPlaybackMode(savedVideoMode)
                }
            }
        }
        launch {
            settingsRepo.videoQueueModeFlow.collect { savedQueueMode ->
                if (VideoPlaybackController.state.value.queueMode != savedQueueMode) {
                    VideoPlaybackController.setQueueMode(savedQueueMode)
                }
            }
        }
        launch {
            settingsRepo.djModeSettingsFlow.collect { savedDj ->
                djModeEnabled = savedDj.enabled
                djTransitionSeconds = savedDj.transitionSeconds
                val s = MusicController.state.value
                if (s.djModeEnabled != savedDj.enabled || s.djTransitionSeconds != savedDj.transitionSeconds) {
                    MusicController.setDjModeSettings(
                        enabled = savedDj.enabled,
                        transitionSeconds = savedDj.transitionSeconds,
                        persist = false
                    )
                }
            }
        }
    }

    // Keep MusicController, VideoPlaybackController, RadioPlaybackController & foreground notification localized
    LaunchedEffect(activeLanguage) {
        MusicController.setLanguage(activeLanguage)
        VideoPlaybackController.setLanguage(activeLanguage)
        RadioPlaybackController.setLanguage(activeLanguage)
    }

    // Room Persistent State
    val playlists by dao.getAllPlaylists().collectAsState(initial = emptyList())
    val allPlaylistTracks by dao.getAllPlaylistTracks().collectAsState(initial = emptyList())
    val radioSchedules by dao.getAllRadioSchedules().collectAsState(initial = emptyList())
    val allRadioScheduleItems by dao.getAllRadioScheduleItems().collectAsState(initial = emptyList())
    val favoriteIds by dao.getAllFavoriteIds().collectAsState(initial = emptyList())
    val hiddenSongs by dao.getAllHiddenSongs().collectAsState(initial = emptyList())
    val savedClassifications by dao.getAllClassifications().collectAsState(initial = emptyList())
    val favoriteSongIds = remember(favoriteIds) { favoriteIds.toHashSet() }
    val hiddenSongIds = remember(hiddenSongs) { hiddenSongs.map { it.songId }.toSet() }

    // MediaStore State (kept separate from searchQuery so Playlists/Favorites/Classifier always see full library)
    var rawSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var currentSort by rememberSaveable { mutableStateOf(settingsRepo.getInitialSortOption()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var hasPermission by rememberSaveable { mutableStateOf(false) }
    var hasRestoredSessionFromDisk by rememberSaveable { mutableStateOf(false) }

    val songs = remember(rawSongs, savedClassifications) {
        if (savedClassifications.isEmpty()) {
            rawSongs
        } else {
            val classMap = savedClassifications.associateBy { it.songId }
            rawSongs.map { song ->
                val override = classMap[song.id]
                if (override != null) {
                    val cat = RemixCategory.entries.find { it.name == override.categoryName } ?: song.remixCategory
                    song.copy(
                        remixCategory = cat,
                        bpmEstimate = if (override.estimatedBpm > 0) override.estimatedBpm else song.bpmEstimate
                    )
                } else {
                    song
                }
            }
        }
    }

    // Sync library metadata in-place and restore last paused track + position + Smart Shuffle state once MediaStore finishes scanning
    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            MusicController.syncLibraryMetadata(songs)
            if (!hasRestoredSessionFromDisk && MusicController.state.value.currentSong == null) {
                hasRestoredSessionFromDisk = true
                val savedSession = settingsRepo.getInitialMusicSession()
                if (savedSession != null) {
                    val matchedSong = songs.find { it.id == savedSession.songId }
                    if (matchedSong != null) {
                        val songsById = songs.associateBy { it.id }
                        val restoredQueue = savedSession.queueSongIds
                            .mapNotNull { id -> songsById[id] }
                            .ifEmpty { songs }
                        MusicController.restorePausedSession(
                            song = matchedSong,
                            queue = restoredQueue,
                            positionMs = savedSession.positionMs,
                            savedShufflePlayedIds = savedSession.shufflePlayedIds,
                            savedShuffleRemainingIds = savedSession.shuffleRemainingIds,
                            savedShuffleHistoryIds = savedSession.shuffleHistoryIds,
                            sourceType = QueueSourceType.fromNameOrDefault(savedSession.queueSourceType),
                            sourcePlaylistId = savedSession.queueSourcePlaylistId
                        )
                    }
                }
            }
        }
    }

    // Dynamically synchronize active playback queue (and Smart Shuffle remaining pool) when songs are added/removed
    LaunchedEffect(songs, favoriteIds, allPlaylistTracks) {
        if (songs.isNotEmpty() && MusicController.state.value.currentSong != null) {
            when (MusicController.activeQueueSourceType) {
                QueueSourceType.LIBRARY -> {
                    MusicController.updateActiveQueueSongs(songs)
                }
                QueueSourceType.FAVORITES -> {
                    val favSet = favoriteIds.toHashSet()
                    MusicController.updateActiveQueueSongs(songs.filter { favSet.contains(it.id) })
                }
                QueueSourceType.PLAYLIST -> {
                    val activePlId = MusicController.activeQueuePlaylistId
                    if (activePlId > 0L) {
                        val songsById = songs.associateBy { it.id }
                        val updatedPlaylistSongs = allPlaylistTracks
                            .asSequence()
                            .filter { it.playlistId == activePlId }
                            .sortedBy { it.orderIndex }
                            .mapNotNull { songsById[it.songId] }
                            .toList()
                        MusicController.updateActiveQueueSongs(updatedPlaylistSongs)
                    }
                }
                QueueSourceType.CUSTOM -> Unit
            }
        }
    }

    // Startup & Progress States (skip splash when media session is already active or after Activity recreation)
    var isStartupFinished by rememberSaveable {
        mutableStateOf(
            MusicController.state.value.currentSong != null ||
                VideoPlaybackController.state.value.currentVideo != null ||
                RadioPlaybackController.state.value.currentItem != null
        )
    }
    var startupPhase by remember { mutableStateOf(StartupPhase.INIT) }
    var lastScannedMusicTitle by remember { mutableStateOf<String?>(null) }
    var lastScannedVideoTitle by remember { mutableStateOf<String?>(null) }
    var musicCustomError by remember { mutableStateOf(false) }
    var videoCustomError by remember { mutableStateOf(false) }

    var startupProgressState by remember {
        mutableStateOf(
            TaskProgressState(
                taskType = TaskType.STARTUP,
                title = JnmfStrings.startupInitTitle(activeLanguage),
                progress = 0.1f,
                statusMessage = JnmfStrings.startupInitStatus(activeLanguage),
                isRunning = true
            )
        )
    }
    var musicScanState by remember { mutableStateOf(TaskProgressState.IDLE) }
    var videoScanState by remember { mutableStateOf(TaskProgressState.IDLE) }
    var isScanningMusic by remember { mutableStateOf(false) }
    var isScanningVideos by remember { mutableStateOf(false) }

    // Re-localize any visible scan/startup progress banner in place if language changes
    LaunchedEffect(activeLanguage) {
        if (!isStartupFinished) {
            startupProgressState = when (startupPhase) {
                StartupPhase.INIT -> startupProgressState.copy(
                    title = JnmfStrings.startupInitTitle(activeLanguage),
                    statusMessage = JnmfStrings.startupInitStatus(activeLanguage)
                )
                StartupPhase.SCANNING -> {
                    val cur = startupProgressState.current
                    val tot = startupProgressState.total
                    val item = lastScannedMusicTitle
                    startupProgressState.copy(
                        title = JnmfStrings.startupScanTitle(activeLanguage),
                        statusMessage = if (cur != null && tot != null && tot > 0 && item != null) {
                            JnmfStrings.startupScanProgress(cur, tot, item, activeLanguage)
                        } else {
                            JnmfStrings.startupScanStatus(activeLanguage)
                        }
                    )
                }
                StartupPhase.READY -> startupProgressState.copy(
                    title = JnmfStrings.startupReadyTitle(activeLanguage),
                    statusMessage = JnmfStrings.startupReadyStatus(songs.size, activeLanguage)
                )
            }
        }

        if (musicScanState != TaskProgressState.IDLE) {
            musicScanState = when {
                musicScanState.error != null -> musicScanState.copy(
                    title = JnmfStrings.musicScanErrorTitle(activeLanguage),
                    error = if (musicCustomError) musicScanState.error else JnmfStrings.failedScanStorage(activeLanguage)
                )
                musicScanState.isCompleted -> musicScanState.copy(
                    title = JnmfStrings.loadingMusicReady(activeLanguage),
                    statusMessage = JnmfStrings.foundTracksCount(musicScanState.total ?: songs.size, activeLanguage)
                )
                musicScanState.isRunning -> {
                    val cur = musicScanState.current
                    val tot = musicScanState.total
                    val item = lastScannedMusicTitle
                    musicScanState.copy(
                        title = JnmfStrings.loadingScanningMusic(activeLanguage),
                        statusMessage = if (cur != null && tot != null && tot > 0 && item != null) {
                            JnmfStrings.readingItemProgress(item, cur, tot, activeLanguage)
                        } else {
                            JnmfStrings.indexingAudioFiles(activeLanguage)
                        }
                    )
                }
                else -> musicScanState
            }
        }

        if (videoScanState != TaskProgressState.IDLE) {
            videoScanState = when {
                videoScanState.error != null -> videoScanState.copy(
                    title = JnmfStrings.videoScanErrorTitle(activeLanguage),
                    error = if (videoCustomError) videoScanState.error else JnmfStrings.failedScanVideos(activeLanguage)
                )
                videoScanState.isCompleted -> videoScanState.copy(
                    title = JnmfStrings.loadingVideosReady(activeLanguage),
                    statusMessage = JnmfStrings.foundVideosCount(videoScanState.total ?: videos.size, activeLanguage)
                )
                videoScanState.isRunning -> {
                    val cur = videoScanState.current
                    val tot = videoScanState.total
                    val item = lastScannedVideoTitle
                    videoScanState.copy(
                        title = JnmfStrings.loadingScanningVideos(activeLanguage),
                        statusMessage = if (cur != null && tot != null && tot > 0 && item != null) {
                            JnmfStrings.readingItemProgress(item, cur, tot, activeLanguage)
                        } else {
                            JnmfStrings.discoveringVideos(activeLanguage)
                        }
                    )
                }
                else -> videoScanState
            }
        }
    }

    // Navigation & Overlay State (persisted across screen changes, closures, and restarts)
    val initialPlaylistSection = remember { settingsRepo.getInitialPlaylistSectionState() }
    var currentTab by rememberSaveable {
        mutableStateOf(MainTab.fromNameOrDefault(settingsRepo.getInitialMainTabName()))
    }
    var selectedPlaylistId by rememberSaveable {
        mutableStateOf(initialPlaylistSection.selectedPlaylistId)
    }
    var selectedRadioScheduleId by rememberSaveable {
        mutableStateOf(settingsRepo.getInitialRadioScheduleId())
    }
    var viewingFavoritesInPlaylists by rememberSaveable {
        mutableStateOf(initialPlaylistSection.viewingFavorites)
    }
    var currentSubScreen by rememberSaveable { mutableStateOf(SubScreen.NONE) }
    var songForPlaylistPicker by remember { mutableStateOf<Song?>(null) }
    var songsForRadioPlaylistPicker by remember { mutableStateOf<List<Song>?>(null) }
    var newPlaylistInput by rememberSaveable { mutableStateOf("") }
    var isCreatingNewInPicker by rememberSaveable { mutableStateOf(false) }
    var showNotificationDialog by rememberSaveable { mutableStateOf(false) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var isVideoFullscreen by rememberSaveable { mutableStateOf(false) }
    var isVideoLandscape by rememberSaveable { mutableStateOf(false) }
    var isVideoPlayerExpanded by rememberSaveable {
        mutableStateOf(VideoPlaybackController.state.value.currentVideo != null)
    }

    // Persist tab, sort, playlist, and radio schedule selections
    LaunchedEffect(currentTab) {
        if (currentTab != MainTab.VIDEOS) {
            isVideoFullscreen = false
            isVideoLandscape = false
        }
        settingsRepo.setLastMainTabName(currentTab.name)
    }

    LaunchedEffect(currentSort) {
        settingsRepo.setLastSortOption(currentSort)
    }

    LaunchedEffect(selectedPlaylistId, viewingFavoritesInPlaylists) {
        settingsRepo.setPlaylistSectionState(selectedPlaylistId, viewingFavoritesInPlaylists)
    }

    LaunchedEffect(selectedRadioScheduleId) {
        settingsRepo.setLastSelectedRadioScheduleId(selectedRadioScheduleId)
    }

    // Helper to display clear feedback when storage is full or save fails (never falsely reporting success)
    fun showSaveFeedbackIfFailed(
        result: SaveResult<*>,
        duplicateMessage: String? = null
    ): Boolean {
        return when (result) {
            is SaveResult.Success -> true
            is SaveResult.DuplicateIgnored -> {
                if (duplicateMessage != null) {
                    scope.launch { snackbarHostState.showSnackbar(duplicateMessage) }
                }
                false
            }
            is SaveResult.InsufficientStorage -> {
                scope.launch {
                    snackbarHostState.showSnackbar(JnmfStrings.storageLowSaveBlocked(activeLanguage))
                }
                false
            }
            is SaveResult.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(JnmfStrings.saveErrorGeneric(activeLanguage))
                }
                false
            }
        }
    }

    // Structural Playback State slices (isolates JnmfApp from 200ms progress/crossfade ticks)
    val currentSongId by remember {
        MusicController.state.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsState(initial = MusicController.state.value.currentSong?.id)
    val isMusicPlaying by remember {
        MusicController.state.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = MusicController.state.value.isPlaying)
    val musicErrorMessage by remember {
        MusicController.state.map { it.errorMessage }.distinctUntilChanged()
    }.collectAsState(initial = MusicController.state.value.errorMessage)
    val hasCurrentVideo by remember {
        VideoPlaybackController.state.map { it.currentVideo != null }.distinctUntilChanged()
    }.collectAsState(initial = VideoPlaybackController.state.value.currentVideo != null)
    val activeSessionOwner by VideoPlaybackController.activeOwner.collectAsState()

    LaunchedEffect(openVideoPlayerToken) {
        if (openVideoPlayerToken > 0L && VideoPlaybackController.state.value.currentVideo != null) {
            isStartupFinished = true
            currentSubScreen = SubScreen.NONE
            currentTab = MainTab.VIDEOS
            isVideoPlayerExpanded = true
        }
    }

    LaunchedEffect(openRadioSchedulerToken) {
        if (openRadioSchedulerToken > 0L) {
            isStartupFinished = true
            currentSubScreen = SubScreen.NONE
            currentTab = MainTab.RADIO_DJ
        }
    }

    LaunchedEffect(hasCurrentVideo) {
        if (!hasCurrentVideo) {
            isVideoFullscreen = false
            isVideoLandscape = false
            isVideoPlayerExpanded = false
        }
    }

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
        lastScannedMusicTitle = null
        musicCustomError = false

        musicScanState = TaskProgressState(
            taskType = TaskType.MUSIC_SCAN,
            title = JnmfStrings.loadingScanningMusic(activeLanguage),
            progress = 0f,
            statusMessage = JnmfStrings.indexingAudioFiles(activeLanguage),
            isRunning = true
        )
        if (isStartup) {
            startupPhase = StartupPhase.SCANNING
            startupProgressState = TaskProgressState(
                taskType = TaskType.STARTUP,
                title = JnmfStrings.startupScanTitle(activeLanguage),
                progress = 0.2f,
                statusMessage = JnmfStrings.startupScanStatus(activeLanguage),
                isRunning = true
            )
        }

        scope.launch {
            try {
                val scannedSongs = mediaStoreRepo.queryLocalSongs(
                    sortOption = currentSort,
                    hiddenSongIds = hiddenSongIds,
                    searchQuery = "",
                    onProgress = { current, total, title ->
                        lastScannedMusicTitle = title
                        val fraction = if (total > 0) current.toFloat() / total.toFloat() else null
                        musicScanState = musicScanState.copy(
                            title = JnmfStrings.loadingScanningMusic(activeLanguage),
                            progress = fraction,
                            current = current,
                            total = total,
                            statusMessage = if (total > 0) {
                                JnmfStrings.readingItemProgress(title, current, total, activeLanguage)
                            } else {
                                JnmfStrings.indexingAudio(activeLanguage)
                            }
                        )
                        if (isStartup) {
                            startupProgressState = startupProgressState.copy(
                                title = JnmfStrings.startupScanTitle(activeLanguage),
                                progress = fraction,
                                current = current,
                                total = total,
                                statusMessage = if (total > 0) {
                                    JnmfStrings.startupScanProgress(current, total, title, activeLanguage)
                                } else {
                                    JnmfStrings.startupScanningTracks(activeLanguage)
                                }
                            )
                        }
                    }
                )
                rawSongs = scannedSongs
                isScanningMusic = false
                musicScanState = TaskProgressState(
                    taskType = TaskType.MUSIC_SCAN,
                    title = JnmfStrings.loadingMusicReady(activeLanguage),
                    current = scannedSongs.size,
                    total = scannedSongs.size,
                    statusMessage = JnmfStrings.foundTracksCount(scannedSongs.size, activeLanguage),
                    isCompleted = true,
                    isRunning = false
                )
                if (isStartup) {
                    startupPhase = StartupPhase.READY
                    startupProgressState = TaskProgressState(
                        taskType = TaskType.STARTUP,
                        title = JnmfStrings.startupReadyTitle(activeLanguage),
                        progress = 1.0f,
                        statusMessage = JnmfStrings.startupReadyStatus(scannedSongs.size, activeLanguage),
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
                val customMsg = e.localizedMessage?.takeIf { it.isNotBlank() }
                musicCustomError = customMsg != null
                musicScanState = TaskProgressState(
                    taskType = TaskType.MUSIC_SCAN,
                    title = JnmfStrings.musicScanErrorTitle(activeLanguage),
                    error = customMsg ?: JnmfStrings.failedScanStorage(activeLanguage),
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
        lastScannedVideoTitle = null
        videoCustomError = false

        videoScanState = TaskProgressState(
            taskType = TaskType.VIDEO_SCAN,
            title = JnmfStrings.loadingScanningVideos(activeLanguage),
            progress = 0f,
            statusMessage = JnmfStrings.discoveringVideos(activeLanguage),
            isRunning = true
        )

        scope.launch {
            try {
                val scannedVideos = mediaStoreRepo.queryLocalVideos(
                    onProgress = { current, total, title ->
                        lastScannedVideoTitle = title
                        val fraction = if (total > 0) current.toFloat() / total.toFloat() else null
                        videoScanState = videoScanState.copy(
                            title = JnmfStrings.loadingScanningVideos(activeLanguage),
                            progress = fraction,
                            current = current,
                            total = total,
                            statusMessage = if (total > 0) {
                                JnmfStrings.readingItemProgress(title, current, total, activeLanguage)
                            } else {
                                JnmfStrings.indexingVideos(activeLanguage)
                            }
                        )
                    }
                )
                videos = scannedVideos
                videoScanState = TaskProgressState(
                    taskType = TaskType.VIDEO_SCAN,
                    title = JnmfStrings.loadingVideosReady(activeLanguage),
                    current = scannedVideos.size,
                    total = scannedVideos.size,
                    statusMessage = JnmfStrings.foundVideosCount(scannedVideos.size, activeLanguage),
                    isCompleted = true,
                    isRunning = false
                )
                delay(2500)
                if (videoScanState.isCompleted) {
                    videoScanState = TaskProgressState.IDLE
                }
            } catch (e: Exception) {
                val customMsg = e.localizedMessage?.takeIf { it.isNotBlank() }
                videoCustomError = customMsg != null
                videoScanState = TaskProgressState(
                    taskType = TaskType.VIDEO_SCAN,
                    title = JnmfStrings.videoScanErrorTitle(activeLanguage),
                    error = customMsg ?: JnmfStrings.failedScanVideos(activeLanguage),
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
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (granted) {
            refreshMedia(isStartup = !isStartupFinished)
            refreshVideos()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                PlaybackService.startService(context)
            }
        } else {
            if (!isStartupFinished) {
                isStartupFinished = true
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    LaunchedEffect(currentSort, hiddenSongIds) {
        if (hasPermission && isStartupFinished && !isScanningMusic) {
            rawSongs = mediaStoreRepo.queryLocalSongs(
                sortOption = currentSort,
                hiddenSongIds = hiddenSongIds,
                searchQuery = ""
            )
        }
    }

    // Back handling for overlays
    BackHandler(enabled = currentSubScreen != SubScreen.NONE) {
        currentSubScreen = SubScreen.NONE
    }

    BackHandler(enabled = isVideoFullscreen && currentTab != MainTab.VIDEOS) {
        isVideoFullscreen = false
    }

    JnmfTheme(themeColor = activeTheme) {
        val reducedMotion = rememberReducedMotionEnabled()

        // Subtle GPU-only refresh pulse on language/color change (never unmounts or resets UI state)
        val refreshAlpha = remember { Animatable(1f) }
        LaunchedEffect(transitionState.transitionToken) {
            if (transitionState.transitionToken > 0L && !reducedMotion) {
                refreshAlpha.snapTo(0.92f)
                refreshAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                )
            } else {
                refreshAlpha.snapTo(1f)
            }
        }

        CompositionLocalProvider(LocalAppLanguage provides activeLanguage) {
            if (!isStartupFinished) {
                StartupLoadingScreen(
                    progressState = startupProgressState,
                    onSkipOrReady = { isStartupFinished = true }
                )
            } else {
                val hideOuterChromeForVideo =
                    currentTab == MainTab.VIDEOS &&
                        (isVideoFullscreen || (isVideoPlayerExpanded && isVideoLandscape))

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.testTag("jnmf_snackbar_host")
                        )
                    },
                    topBar = {
                        if (!hideOuterChromeForVideo) {
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
                                        // Official JNMF Logo
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_jnmf_official_logo),
                                            contentDescription = JnmfStrings.officialLogoDesc(activeLanguage),
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

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
                                                text = JnmfStrings.topSubtitle(activeLanguage),
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
                                                    imageVector = if (notificationsEnabled) {
                                                        Icons.Default.Notifications
                                                    } else {
                                                        Icons.Default.NotificationsNone
                                                    },
                                                    contentDescription = JnmfStrings.quickNotifications(activeLanguage),
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
                                                contentDescription = JnmfStrings.quickDuplicates(activeLanguage),
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
                                                contentDescription = JnmfStrings.quickClassifier(activeLanguage),
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
                        if (!hideOuterChromeForVideo) {
                            Column {
                                // Floating MiniPlayer / Background Video MiniPlayer docked directly above the navigation bar
                                JnmfDockedMiniPlayerHost(
                                    activeSessionOwner = activeSessionOwner,
                                    isNowPlayingOpen = currentSubScreen == SubScreen.NOW_PLAYING,
                                    isVideoTabExpanded = currentTab == MainTab.VIDEOS && isVideoPlayerExpanded,
                                    isRadioTabOpen = currentTab == MainTab.RADIO_DJ,
                                    onStopVideo = {
                                        isVideoFullscreen = false
                                        isVideoPlayerExpanded = false
                                        VideoPlaybackController.stop()
                                    },
                                    onExpandVideo = {
                                        currentSubScreen = SubScreen.NONE
                                        currentTab = MainTab.VIDEOS
                                        isVideoPlayerExpanded = true
                                    },
                                    onOpenRadioScheduler = {
                                        currentSubScreen = SubScreen.NONE
                                        currentTab = MainTab.RADIO_DJ
                                    },
                                    onOpenNowPlaying = {
                                        currentSubScreen = SubScreen.NOW_PLAYING
                                    }
                                )

                                // Bottom Navigation Bar (1. Music, 2. Playlists, 3. Equalizer, 4. Radio DJ, 5. Videos, 6. Settings)
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.MUSIC,
                                        onClick = { currentTab = MainTab.MUSIC },
                                        icon = {
                                            Icon(
                                                Icons.Default.Audiotrack,
                                                contentDescription = JnmfStrings.navMusic(activeLanguage)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = JnmfStrings.navMusic(activeLanguage),
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("bottom_nav_music")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.PLAYLISTS,
                                        onClick = { currentTab = MainTab.PLAYLISTS },
                                        icon = {
                                            Icon(
                                                Icons.Default.QueueMusic,
                                                contentDescription = JnmfStrings.navPlaylists(activeLanguage)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = JnmfStrings.navPlaylists(activeLanguage),
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("bottom_nav_playlists")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.EQUALIZER,
                                        onClick = { currentTab = MainTab.EQUALIZER },
                                        icon = {
                                            Icon(
                                                Icons.Default.Equalizer,
                                                contentDescription = JnmfStrings.navEqualizer(activeLanguage)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = JnmfStrings.navEqualizer(activeLanguage),
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("bottom_nav_equalizer")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.RADIO_DJ,
                                        onClick = { currentTab = MainTab.RADIO_DJ },
                                        icon = {
                                            Icon(
                                                Icons.Default.Radio,
                                                contentDescription = RadioDjStrings.navRadioDj(activeLanguage)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = RadioDjStrings.navRadioDj(activeLanguage),
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("bottom_nav_radio_dj")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.VIDEOS,
                                        onClick = { currentTab = MainTab.VIDEOS },
                                        icon = {
                                            Icon(
                                                Icons.Default.Videocam,
                                                contentDescription = JnmfStrings.navVideos(activeLanguage)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = JnmfStrings.navVideos(activeLanguage),
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("bottom_nav_videos")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.SETTINGS,
                                        onClick = { currentTab = MainTab.SETTINGS },
                                        icon = {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = JnmfStrings.navSettings(activeLanguage)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = JnmfStrings.navSettings(activeLanguage),
                                                fontSize = 9.5.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("bottom_nav_settings")
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    val contentModifier = if (hideOuterChromeForVideo) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .graphicsLayer { alpha = refreshAlpha.value }
                    }

                    Box(modifier = contentModifier) {
                        // Tab Content (kept alive in place when changing language or color)
                        when (currentTab) {
                            MainTab.MUSIC -> {
                                LibraryScreenContent(
                                    songs = songs,
                                    currentSongId = currentSongId,
                                    isPlaying = isMusicPlaying,
                                    hasPermission = hasPermission,
                                    onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
                                    onRefresh = { refreshMedia() },
                                    onSortSelected = { option ->
                                        if (currentSort == option) {
                                            refreshMedia()
                                        } else {
                                            currentSort = option
                                        }
                                    },
                                    currentSort = currentSort,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    favoriteSongIds = favoriteSongIds,
                                    onToggleFavorite = { song ->
                                        scope.launch {
                                            val result = userDataRepo.toggleFavorite(
                                                songId = song.id,
                                                isCurrentlyFavorite = favoriteSongIds.contains(song.id)
                                            )
                                            showSaveFeedbackIfFailed(result)
                                        }
                                    },
                                    onAddToPlaylist = { song ->
                                        songForPlaylistPicker = song
                                    },
                                    onHideSong = { song ->
                                        scope.launch {
                                            val result = userDataRepo.hideSong(song)
                                            showSaveFeedbackIfFailed(result)
                                        }
                                    },
                                    onAddToRadioPlaylist = { song ->
                                        songsForRadioPlaylistPicker = listOf(song)
                                    },
                                    onQuickAddToRadio = { song ->
                                        songsForRadioPlaylistPicker = listOf(song)
                                    },
                                    onAddSelectedToRadio = { selectedList ->
                                        songsForRadioPlaylistPicker = selectedList
                                    },
                                    musicScanState = musicScanState,
                                    language = activeLanguage,
                                    onDismissProgress = { musicScanState = TaskProgressState.IDLE }
                                )
                            }
                            MainTab.PLAYLISTS -> {
                                val favoriteSongs = remember(favoriteSongIds, songs) {
                                    songs.filter { favoriteSongIds.contains(it.id) }
                                }
                                val tracksByPlaylist = remember(allPlaylistTracks) {
                                    allPlaylistTracks.groupBy { it.playlistId }
                                }
                                val getPlaylistTracksRemembered = remember<(Long) -> List<PlaylistTrackEntity>>(tracksByPlaylist) {
                                    { plId -> tracksByPlaylist[plId].orEmpty() }
                                }
                                val getPlaylistTrackIdsRemembered = remember<(Long) -> List<Long>>(tracksByPlaylist) {
                                    { plId -> tracksByPlaylist[plId].orEmpty().map { it.songId } }
                                }

                                PlaylistScreenContent(
                                    playlists = playlists,
                                    favoriteSongs = favoriteSongs,
                                    allSongs = songs,
                                    currentSongId = currentSongId,
                                    isPlaying = isMusicPlaying,
                                    selectedPlaylistId = selectedPlaylistId,
                                    onSelectedPlaylistIdChange = { selectedPlaylistId = it },
                                    viewingFavorites = viewingFavoritesInPlaylists,
                                    onViewingFavoritesChange = { viewingFavoritesInPlaylists = it },
                                    onCreatePlaylist = { name ->
                                        scope.launch {
                                            val result = userDataRepo.createPlaylist(name)
                                            showSaveFeedbackIfFailed(
                                                result,
                                                duplicateMessage = JnmfStrings.playlistAlreadyExists(activeLanguage)
                                            )
                                        }
                                    },
                                    onCreatePlaylistSuspend = { name ->
                                        val result = userDataRepo.createPlaylist(name)
                                        showSaveFeedbackIfFailed(
                                            result,
                                            duplicateMessage = JnmfStrings.playlistAlreadyExists(activeLanguage)
                                        )
                                    },
                                    onRenamePlaylist = { id, newName ->
                                        scope.launch {
                                            val result = userDataRepo.renamePlaylist(id, newName)
                                            showSaveFeedbackIfFailed(
                                                result,
                                                duplicateMessage = JnmfStrings.playlistAlreadyExists(activeLanguage)
                                            )
                                        }
                                    },
                                    onRenamePlaylistSuspend = { id, newName ->
                                        val result = userDataRepo.renamePlaylist(id, newName)
                                        showSaveFeedbackIfFailed(
                                            result,
                                            duplicateMessage = JnmfStrings.playlistAlreadyExists(activeLanguage)
                                        )
                                    },
                                    onDeletePlaylist = { id ->
                                        scope.launch {
                                            val result = userDataRepo.deletePlaylist(id)
                                            showSaveFeedbackIfFailed(result)
                                        }
                                    },
                                    onAddSongToPlaylist = { plId, songId ->
                                        scope.launch {
                                            val result = userDataRepo.addSongIdToPlaylist(plId, songId, songs)
                                            showSaveFeedbackIfFailed(
                                                result,
                                                duplicateMessage = JnmfStrings.songAlreadyInPlaylist(activeLanguage)
                                            )
                                        }
                                    },
                                    onRemoveSongFromPlaylist = { plId, songId ->
                                        scope.launch {
                                            val result = userDataRepo.removeSongFromPlaylist(plId, songId)
                                            showSaveFeedbackIfFailed(result)
                                        }
                                    },
                                    onMoveTrack = { plId, from, to ->
                                        scope.launch {
                                            val result = userDataRepo.moveTrackInPlaylist(plId, from, to)
                                            showSaveFeedbackIfFailed(result)
                                        }
                                    },
                                    onToggleFavorite = { song ->
                                        scope.launch {
                                            val result = userDataRepo.toggleFavorite(
                                                songId = song.id,
                                                isCurrentlyFavorite = favoriteSongIds.contains(song.id)
                                            )
                                            showSaveFeedbackIfFailed(result)
                                        }
                                    },
                                    onRequestAddToPlaylist = { song ->
                                        songForPlaylistPicker = song
                                    },
                                    getPlaylistTracks = getPlaylistTracksRemembered,
                                    getPlaylistTrackIds = getPlaylistTrackIdsRemembered,
                                    onMissingSongTapped = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                JnmfStrings.errorAudioMissing(activeLanguage)
                                            )
                                        }
                                    }
                                )
                            }
                            MainTab.EQUALIZER -> {
                                EqualizerScreen(
                                    equalizerManager = MusicController.equalizerManager,
                                    onBack = { currentTab = MainTab.MUSIC }
                                )
                            }
                            MainTab.RADIO_DJ -> {
                                RadioDjSchedulerScreen(
                                    schedules = radioSchedules,
                                    allScheduleItems = allRadioScheduleItems,
                                    allSongs = songs,
                                    hasScannedLibrary = songs.isNotEmpty() || !isScanningMusic,
                                    selectedScheduleId = selectedRadioScheduleId,
                                    onSelectedScheduleIdChange = { selectedRadioScheduleId = it },
                                    radioRepo = radioRepo,
                                    language = activeLanguage,
                                    onSaveResultFeedback = { result, duplicateMsg ->
                                        showSaveFeedbackIfFailed(result, duplicateMsg)
                                    }
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
                                    onLandscapeChanged = { isVideoLandscape = it },
                                    isPlayerExpanded = isVideoPlayerExpanded,
                                    onPlayerExpandedChanged = { expanded ->
                                        isVideoPlayerExpanded = expanded
                                        if (!expanded) isVideoLandscape = false
                                    },
                                    onSelectPlaybackMode = { mode ->
                                        VideoPlaybackController.setPlaybackMode(mode)
                                        scope.launch {
                                            settingsRepo.setVideoPlaybackMode(mode)
                                        }
                                    },
                                    onSelectQueueMode = { queueMode ->
                                        VideoPlaybackController.setQueueMode(queueMode)
                                        scope.launch {
                                            settingsRepo.setVideoQueueMode(queueMode)
                                        }
                                    },
                                    language = activeLanguage
                                )
                            }
                            MainTab.SETTINGS -> {
                                SettingsScreen(
                                    currentTheme = activeTheme,
                                    currentLanguage = activeLanguage,
                                    hiddenSongs = hiddenSongs,
                                    onSelectTheme = { theme ->
                                        transitionController.triggerThemeTransition(
                                            newTheme = theme,
                                            currentTheme = activeTheme,
                                            currentLanguage = activeLanguage,
                                            reducedMotion = reducedMotion,
                                            onApplyImmediate = { updatedTheme ->
                                                activeTheme = updatedTheme
                                            },
                                            persistAction = { updatedTheme ->
                                                settingsRepo.setThemeColor(updatedTheme)
                                            }
                                        )
                                    },
                                    onSelectLanguage = { lang ->
                                        transitionController.triggerLanguageTransition(
                                            newLanguage = lang,
                                            currentLanguage = activeLanguage,
                                            reducedMotion = reducedMotion,
                                            onApplyImmediate = { updatedLang ->
                                                activeLanguage = updatedLang
                                            },
                                            persistAction = { updatedLang ->
                                                settingsRepo.setLanguage(updatedLang)
                                            }
                                        )
                                    },
                                    onUnhideSong = { songId ->
                                        scope.launch {
                                            val result = userDataRepo.unhideSong(songId)
                                            showSaveFeedbackIfFailed(result)
                                        }
                                    },
                                    djModeEnabled = djModeEnabled,
                                    djTransitionSeconds = djTransitionSeconds,
                                    onDjModeEnabledChange = { enabled ->
                                        djModeEnabled = enabled
                                        MusicController.setDjModeSettings(
                                            enabled = enabled,
                                            transitionSeconds = djTransitionSeconds,
                                            persist = true
                                        )
                                        scope.launch {
                                            settingsRepo.setDjModeEnabled(enabled)
                                        }
                                    },
                                    onDjTransitionSecondsChange = { seconds ->
                                        val normalized = DjModeSettings.normalizeSeconds(seconds)
                                        djTransitionSeconds = normalized
                                        MusicController.setDjModeSettings(
                                            enabled = djModeEnabled,
                                            transitionSeconds = normalized,
                                            persist = true
                                        )
                                        scope.launch {
                                            settingsRepo.setDjTransitionSeconds(normalized)
                                        }
                                    },
                                    onOpenNotificationSettings = { showNotificationDialog = true },
                                    onBack = { currentTab = MainTab.MUSIC }
                                )
                            }
                        }

                        // Now Playing Fullscreen Sheet
                        AnimatedVisibility(
                            visible = currentSubScreen == SubScreen.NOW_PLAYING,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            JnmfNowPlayingHost(
                                onClose = { currentSubScreen = SubScreen.NONE },
                                onOpenEqualizer = {
                                    currentSubScreen = SubScreen.NONE
                                    currentTab = MainTab.EQUALIZER
                                }
                            )
                        }

                        // Duplicate Finder Overlay (removes only from library view, never deletes user media files)
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
                                    scope.launch {
                                        val result = userDataRepo.hideSong(song)
                                        if (showSaveFeedbackIfFailed(result)) {
                                            rawSongs = rawSongs.filter { it.id != song.id }
                                        }
                                    }
                                }
                            )
                        }

                        // Remix Classifier Overlay (persists manual category corrections across restarts)
                        AnimatedVisibility(
                            visible = currentSubScreen == SubScreen.CLASSIFIER,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            ClassifierScreen(
                                songs = songs,
                                onBack = { currentSubScreen = SubScreen.NONE },
                                onManualCorrection = { song, newCategory ->
                                    scope.launch {
                                        val result = userDataRepo.saveSongClassification(
                                            songId = song.id,
                                            category = newCategory,
                                            estimatedBpm = song.bpmEstimate
                                        )
                                        if (showSaveFeedbackIfFailed(result)) {
                                            rawSongs = rawSongs.map {
                                                if (it.id == song.id) it.copy(remixCategory = newCategory) else it
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // Global Playlist Selector Dialog when adding a song from Library or Favorites
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
                                        text = JnmfStrings.addSongToPlaylistTitle(targetSong.title, activeLanguage),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (isCreatingNewInPicker) {
                                            OutlinedTextField(
                                                value = newPlaylistInput,
                                                onValueChange = { newPlaylistInput = it },
                                                label = { Text(JnmfStrings.newPlaylistNameLabel(activeLanguage)) },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("picker_new_playlist_input")
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    val trimmed = newPlaylistInput.trim()
                                                    if (trimmed.isNotBlank()) {
                                                        scope.launch {
                                                            val result = userDataRepo.createPlaylistAndAddSong(
                                                                rawName = trimmed,
                                                                song = targetSong
                                                            )
                                                            if (showSaveFeedbackIfFailed(result)) {
                                                                songForPlaylistPicker = null
                                                                isCreatingNewInPicker = false
                                                                newPlaylistInput = ""
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("picker_create_and_add_button")
                                            ) {
                                                Text(
                                                    text = JnmfStrings.createAndAddTrack(activeLanguage),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        } else {
                                            if (playlists.isEmpty()) {
                                                Text(
                                                    text = JnmfStrings.noPlaylistsCreatePrompt(activeLanguage),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = { isCreatingNewInPicker = true },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = JnmfStrings.createNewPlaylist(activeLanguage),
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
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
                                                                        val result = userDataRepo.addSongToPlaylist(
                                                                            playlistId = pl.id,
                                                                            song = targetSong
                                                                        )
                                                                        if (result is SaveResult.Success) {
                                                                            songForPlaylistPicker = null
                                                                        } else if (result is SaveResult.DuplicateIgnored) {
                                                                            songForPlaylistPicker = null
                                                                            snackbarHostState.showSnackbar(
                                                                                JnmfStrings.songAlreadyInPlaylist(activeLanguage)
                                                                            )
                                                                        } else {
                                                                            showSaveFeedbackIfFailed(result)
                                                                        }
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
                                                                    text = JnmfStrings.tracksCount(trackCount, activeLanguage),
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = JnmfStrings.add(activeLanguage),
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
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "+ ${JnmfStrings.createNewPlaylist(activeLanguage)}",
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            songForPlaylistPicker = null
                                            isCreatingNewInPicker = false
                                            newPlaylistInput = ""
                                        }
                                    ) {
                                        Text(
                                            text = JnmfStrings.cancel(activeLanguage),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }

                        // Global Radio Playlist Selector Dialog when adding songs to a Radio DJ Playlist
                        if (songsForRadioPlaylistPicker != null) {
                            val songsToAdd = songsForRadioPlaylistPicker!!
                            RadioPlaylistPickerDialog(
                                songsToAdd = songsToAdd,
                                availableSchedules = radioSchedules,
                                lang = activeLanguage,
                                onDismiss = { songsForRadioPlaylistPicker = null },
                                onSelectSchedule = { schedule ->
                                    scope.launch {
                                        if (songsToAdd.size == 1) {
                                            val singleSong = songsToAdd.first()
                                            val result = radioRepo.addSongItem(schedule.id, singleSong)
                                            when (result) {
                                                is SaveResult.Success -> {
                                                    snackbarHostState.showSnackbar(
                                                        RadioDjStrings.addedSingleSongSuccess(singleSong.title, schedule.name, activeLanguage)
                                                    )
                                                }
                                                is SaveResult.DuplicateIgnored -> {
                                                    snackbarHostState.showSnackbar(
                                                        RadioDjStrings.alreadyInRadioPlaylist(activeLanguage)
                                                    )
                                                }
                                                else -> {
                                                    showSaveFeedbackIfFailed(result)
                                                }
                                            }
                                        } else {
                                            val result = radioRepo.addSongItems(schedule.id, songsToAdd)
                                            when (result) {
                                                is SaveResult.Success -> {
                                                    snackbarHostState.showSnackbar(
                                                        RadioDjStrings.addedBatchSongsSuccess(result.data, schedule.name, activeLanguage)
                                                    )
                                                }
                                                else -> {
                                                    showSaveFeedbackIfFailed(result)
                                                }
                                            }
                                        }
                                        songsForRadioPlaylistPicker = null
                                    }
                                },
                                onCreateNewScheduleAndAdd = { newScheduleName ->
                                    scope.launch {
                                        if (songsToAdd.size == 1) {
                                            val singleSong = songsToAdd.first()
                                            val result = radioRepo.createScheduleAndAddSong(newScheduleName, singleSong)
                                            when (result) {
                                                is SaveResult.Success -> {
                                                    snackbarHostState.showSnackbar(
                                                        RadioDjStrings.addedSingleSongSuccess(singleSong.title, newScheduleName, activeLanguage)
                                                    )
                                                }
                                                is SaveResult.DuplicateIgnored -> {
                                                    snackbarHostState.showSnackbar(
                                                        RadioDjStrings.scheduleAlreadyExists(activeLanguage)
                                                    )
                                                }
                                                else -> {
                                                    showSaveFeedbackIfFailed(result)
                                                }
                                            }
                                        } else {
                                            val result = radioRepo.createScheduleAndAddSongs(newScheduleName, songsToAdd)
                                            when (result) {
                                                is SaveResult.Success -> {
                                                    snackbarHostState.showSnackbar(
                                                        RadioDjStrings.addedBatchSongsSuccess(result.data, newScheduleName, activeLanguage)
                                                    )
                                                }
                                                is SaveResult.DuplicateIgnored -> {
                                                    snackbarHostState.showSnackbar(
                                                        RadioDjStrings.scheduleAlreadyExists(activeLanguage)
                                                    )
                                                }
                                                else -> {
                                                    showSaveFeedbackIfFailed(result)
                                                }
                                            }
                                        }
                                        songsForRadioPlaylistPicker = null
                                    }
                                }
                            )
                        }

                        // Non-intrusive playback error banner
                        if (musicErrorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = JnmfStrings.errorLabel(activeLanguage),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = musicErrorMessage ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { MusicController.clearError() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = JnmfStrings.dismiss(activeLanguage),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Notification dialog for inspecting and requesting playback permissions
                        if (showNotificationDialog) {
                            NotificationDialog(
                                language = activeLanguage,
                                onDismiss = {
                                    showNotificationDialog = false
                                    notificationsEnabled =
                                        NotificationManagerCompat.from(context).areNotificationsEnabled()
                                }
                            )
                        }

                        // Smooth, lightweight floating refresh pill for language & accent color changes
                        JnmfRefreshTransitionBanner(
                            state = transitionState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JnmfDockedMiniPlayerHost(
    activeSessionOwner: ActiveMediaSessionOwner,
    isNowPlayingOpen: Boolean,
    isVideoTabExpanded: Boolean,
    isRadioTabOpen: Boolean,
    onStopVideo: () -> Unit,
    onExpandVideo: () -> Unit,
    onOpenRadioScheduler: () -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    if (isNowPlayingOpen) return

    val playbackState by MusicController.state.collectAsState()
    val videoPlaybackState by VideoPlaybackController.state.collectAsState()
    val radioPlaybackState by RadioPlaybackController.state.collectAsState()

    val showVideoMiniPlayer =
        activeSessionOwner == ActiveMediaSessionOwner.VIDEO &&
            videoPlaybackState.currentVideo != null &&
            !isVideoTabExpanded

    val showRadioMiniPlayer =
        activeSessionOwner == ActiveMediaSessionOwner.RADIO &&
            radioPlaybackState.currentItem != null &&
            !isRadioTabOpen

    when {
        showVideoMiniPlayer -> {
            VideoMiniPlayer(
                videoState = videoPlaybackState,
                onPlayPause = { VideoPlaybackController.togglePlayPause() },
                onPrevious = { VideoPlaybackController.previous() },
                onNext = { VideoPlaybackController.next() },
                onStop = onStopVideo,
                onClick = onExpandVideo
            )
        }
        showRadioMiniPlayer -> {
            RadioMiniPlayer(
                radioState = radioPlaybackState,
                onPlayPause = { RadioPlaybackController.togglePlayPause() },
                onPrevious = { RadioPlaybackController.previous() },
                onNext = { RadioPlaybackController.next() },
                onStop = { RadioPlaybackController.stop() },
                onClick = onOpenRadioScheduler
            )
        }
        playbackState.currentSong != null &&
            activeSessionOwner != ActiveMediaSessionOwner.RADIO &&
            (activeSessionOwner == ActiveMediaSessionOwner.MUSIC || videoPlaybackState.currentVideo == null) -> {
            MiniPlayer(
                playbackState = playbackState,
                onPlayPause = { MusicController.togglePlayPause() },
                onNext = { MusicController.next() },
                onPrevious = { MusicController.previous() },
                onClick = onOpenNowPlaying
            )
        }
    }
}

@Composable
private fun JnmfNowPlayingHost(
    onClose: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val playbackState by MusicController.state.collectAsState()
    NowPlayingScreen(
        playbackState = playbackState,
        onClose = onClose,
        onOpenEqualizer = onOpenEqualizer
    )
}
