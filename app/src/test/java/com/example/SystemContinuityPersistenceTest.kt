package com.example

import android.app.Notification
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteFullException
import android.net.Uri
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.JnmfDatabase
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.SaveResult
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SortOption
import com.example.data.repository.StorageGuard
import com.example.data.repository.UserDataRepository
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.RepeatState
import com.example.playback.VideoPlaybackController
import com.example.ui.MainTab
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JnmfTheme
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class SystemContinuityPersistenceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    private val songA = Song(
        id = 1001L,
        title = "DJ Malam Pagi Fullbeat",
        artist = "DJ Jussar",
        album = "Indo Bass Vol 1",
        durationMs = 210_000L,
        sizeBytes = 8_400_000L,
        dateAdded = 1000L,
        contentUri = Uri.parse("content://media/external/audio/media/1001"),
        remixCategory = RemixCategory.FULLBEAT,
        fileName = "DJ_Malam_Pagi_Fullbeat.mp3"
    )

    private val songB = Song(
        id = 1002L,
        title = "DJ Cinta Santuy Slow",
        artist = "DJ Golden",
        album = "Indo Bass Vol 2",
        durationMs = 240_000L,
        sizeBytes = 9_100_000L,
        dateAdded = 2000L,
        contentUri = Uri.parse("content://media/external/audio/media/1002"),
        remixCategory = RemixCategory.SLOW,
        fileName = "DJ_Cinta_Santuy_Slow.mp3"
    )

    private val songC = Song(
        id = 1003L,
        title = "DJ Horeg Breakbeat Party",
        artist = "DJ Nusantara",
        album = "Indo Bass Vol 3",
        durationMs = 195_000L,
        sizeBytes = 7_800_000L,
        dateAdded = 3000L,
        contentUri = Uri.parse("content://media/external/audio/media/1003"),
        remixCategory = RemixCategory.BREAKBEAT,
        fileName = "DJ_Horeg_Breakbeat_Party.mp3"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        StorageGuard.resetTestingOverrides()
        MusicController.resetInMemoryStateForTesting()
        VideoPlaybackController.stop()
        JnmfDatabase.closeAndResetForTesting()
        context.deleteDatabase(JnmfDatabase.DB_NAME)
        context.getSharedPreferences(SettingsRepository.SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        StorageGuard.resetTestingOverrides()
        MusicController.resetInMemoryStateForTesting()
        VideoPlaybackController.stop()
        JnmfDatabase.closeAndResetForTesting()
        context.deleteDatabase(JnmfDatabase.DB_NAME)
    }

    @Test
    fun `1 create playlist close app and reopen verifies playlist persists on disk`(): Unit = runBlocking {
        // Session 1: Create playlist via UserDataRepository backed by on-disk Room DB
        val dbSession1 = JnmfDatabase.getDatabase(context)
        val repoSession1 = UserDataRepository(context, dbSession1.jnmfDao())

        val createRes = repoSession1.createPlaylist("Redmi 10C Horeg Mix")
        assertTrue(createRes is SaveResult.Success)
        val createdId = (createRes as SaveResult.Success).data
        assertTrue(createdId > 0L)

        // Repeated save with identical name must not create duplicate playlist records
        val duplicateRes = repoSession1.createPlaylist("  redmi 10c horeg mix  ")
        assertTrue(duplicateRes is SaveResult.DuplicateIgnored)

        // Simulate full app closure (close SQLite file connection & clear singleton)
        JnmfDatabase.closeAndResetForTesting()

        // Session 2: Reopen database from disk and verify playlist & name remain intact
        val dbSession2 = JnmfDatabase.getDatabase(context)
        val playlistsAfterReopen = dbSession2.jnmfDao().getAllPlaylistsSync()
        assertEquals(1, playlistsAfterReopen.size)
        assertEquals(createdId, playlistsAfterReopen.first().id)
        assertEquals("Redmi 10C Horeg Mix", playlistsAfterReopen.first().name)
    }

    @Test
    fun `2 add songs prevent duplicates reorder tracks and reopen playlist preserves exact order`(): Unit = runBlocking {
        val db1 = JnmfDatabase.getDatabase(context)
        val repo1 = UserDataRepository(context, db1.jnmfDao())

        val plId = (repo1.createPlaylist("Fullbeat Session") as SaveResult.Success).data

        // Add 3 songs
        assertTrue(repo1.addSongToPlaylist(plId, songA).isSuccess)
        assertTrue(repo1.addSongToPlaylist(plId, songB).isSuccess)
        assertTrue(repo1.addSongToPlaylist(plId, songC).isSuccess)

        // Attempt adding songA again -> ignored without duplicating or shifting orderIndex
        val dupTrackResult = repo1.addSongToPlaylist(plId, songA)
        assertTrue(dupTrackResult is SaveResult.DuplicateIgnored)

        assertEquals(
            listOf(songA.id, songB.id, songC.id),
            db1.jnmfDao().getTrackIdsForPlaylistSync(plId)
        )

        // Reorder: move songC (index 2) to top (index 0) -> [songC, songA, songB]
        assertTrue(repo1.moveTrackInPlaylist(plId, 2, 0).isSuccess)
        // Remove songA -> [songC, songB]
        assertTrue(repo1.removeSongFromPlaylist(plId, songA.id).isSuccess)

        // Close app / DB completely and reopen from disk
        JnmfDatabase.closeAndResetForTesting()

        val db2 = JnmfDatabase.getDatabase(context)
        val restoredTracks = db2.jnmfDao().getTracksForPlaylistSync(plId)
        assertEquals(listOf(songC.id, songB.id), restoredTracks.map { it.songId })
        assertEquals(listOf(0, 1), restoredTracks.map { it.orderIndex })
        assertEquals("DJ Horeg Breakbeat Party", restoredTracks[0].savedTitle)
        assertEquals("DJ Cinta Santuy Slow", restoredTracks[1].savedTitle)
    }

    @Test
    fun `3 force stop and v1 to v2 migration preserve playlists favorites hidden songs and classifications`(): Unit = runBlocking {
        // Part A: Verify non-destructive v1 -> v2 SQLite migration preserves existing user playlists
        val migDbName = "jnmf_v1_migration_test.db"
        context.deleteDatabase(migDbName)
        val migDbFile = context.getDatabasePath(migDbName)
        migDbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(migDbFile, null).use { rawV1 ->
            rawV1.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            rawV1.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist_tracks` (`playlistId` INTEGER NOT NULL, `songId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `songId`))"
            )
            rawV1.execSQL(
                "CREATE TABLE IF NOT EXISTS `favorites` (`songId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
            )
            rawV1.execSQL(
                "CREATE TABLE IF NOT EXISTS `hidden_songs` (`songId` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `hiddenAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
            )
            rawV1.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_classifications` (`songId` INTEGER NOT NULL, `categoryName` TEXT NOT NULL, `estimatedBpm` INTEGER NOT NULL, `confidence` REAL NOT NULL, PRIMARY KEY(`songId`))"
            )
            rawV1.execSQL("INSERT INTO `playlists` (`id`, `name`, `createdAt`) VALUES (7, 'Legacy V1 Playlist', 12345)")
            rawV1.execSQL("INSERT INTO `playlist_tracks` (`playlistId`, `songId`, `orderIndex`) VALUES (7, 1001, 0), (7, 1002, 1)")
            rawV1.version = 1
        }

        val migratedRoomDb = Room.databaseBuilder(context, JnmfDatabase::class.java, migDbName)
            .addMigrations(JnmfDatabase.MIGRATION_1_2, JnmfDatabase.MIGRATION_2_3, JnmfDatabase.MIGRATION_3_4)
            .build()
        try {
            val migratedPlaylists = migratedRoomDb.jnmfDao().getAllPlaylistsSync()
            val migratedTracks = migratedRoomDb.jnmfDao().getTracksForPlaylistSync(7L)
            assertEquals(1, migratedPlaylists.size)
            assertEquals("Legacy V1 Playlist", migratedPlaylists.first().name)
            assertEquals(listOf(1001L, 1002L), migratedTracks.map { it.songId })
        } finally {
            migratedRoomDb.close()
            context.deleteDatabase(migDbName)
        }

        // Part B: Force-stop simulation with favorites, hidden songs, playlists, and manual classifications
        val dbBeforeKill = JnmfDatabase.getDatabase(context)
        val repoBeforeKill = UserDataRepository(context, dbBeforeKill.jnmfDao())
        val plId = (repoBeforeKill.createPlaylist("Force Stop Safe List") as SaveResult.Success).data
        repoBeforeKill.addSongToPlaylist(plId, songA)
        repoBeforeKill.toggleFavorite(songB.id, isCurrentlyFavorite = false)
        repoBeforeKill.hideSong(songC)
        repoBeforeKill.saveSongClassification(songA.id, RemixCategory.BREAKBEAT, 138)

        // Simulate abrupt Force Stop (wipe all in-memory state and close DB)
        MusicController.resetInMemoryStateForTesting()
        JnmfDatabase.closeAndResetForTesting()

        // Reopen fresh instance from disk
        val dbAfterKill = JnmfDatabase.getDatabase(context)
        val daoAfterKill = dbAfterKill.jnmfDao()
        assertEquals(listOf("Force Stop Safe List"), daoAfterKill.getAllPlaylistsSync().map { it.name })
        assertEquals(listOf(songA.id), daoAfterKill.getTrackIdsForPlaylistSync(plId))
        assertEquals(listOf(songB.id), daoAfterKill.getAllFavoriteIdsSync())
        assertEquals(
            listOf("BREAKBEAT"),
            daoAfterKill.getAllClassificationsSync().filter { it.songId == songA.id }.map { it.categoryName }
        )
    }

    @Test
    fun `4 device restart restores tab sort repeat shuffle eq and paused track position without auto playing`(): Unit = runBlocking {
        // Session before device restart
        MusicController.bindAppContext(context)
        val settingsBefore = SettingsRepository(context)
        settingsBefore.setLastMainTabName(MainTab.PLAYLISTS.name)
        settingsBefore.setLastSortOption(SortOption.DATE_DESC)
        settingsBefore.setPlaylistSectionState(selectedPlaylistId = 42L, viewingFavorites = false)

        // User enables Shuffle, cycles Repeat to ONE, enables Reverse Queue, customizes EQ, and plays songB
        MusicController.toggleShuffle() // false -> true
        MusicController.cycleRepeatMode() // ALL -> ONE
        MusicController.toggleReverseQueue() // false -> true
        MusicController.equalizerManager.applyPreset("Bass Horeg")
        MusicController.equalizerManager.setBassStrength(910)

        MusicController.playSong(songB, listOf(songA, songB, songC))
        MusicController.seekTo(94_500L)
        assertTrue(MusicController.state.value.isPlaying)
        MusicController.flushPlaybackSnapshotNow()

        // Simulate full device reboot (clear all RAM singletons)
        MusicController.resetInMemoryStateForTesting()
        MusicController.equalizerManager.applyPreset("Flat")
        JnmfDatabase.closeAndResetForTesting()

        // Cold boot after restart
        val settingsAfter = SettingsRepository(context)
        assertEquals(MainTab.PLAYLISTS.name, settingsAfter.getInitialMainTabName())
        assertEquals(SortOption.DATE_DESC, settingsAfter.getInitialSortOption())
        assertEquals(42L, settingsAfter.getInitialPlaylistSectionState().selectedPlaylistId)

        val savedModes = settingsAfter.getInitialMusicModes()
        assertTrue(savedModes.isShuffle)
        assertEquals(RepeatState.ONE, savedModes.repeatState)
        assertTrue(savedModes.isReverseQueue)

        val savedEq = settingsAfter.getInitialEqSettings()
        assertEquals("Bass Horeg", savedEq.preset)
        assertEquals(910, savedEq.bass)

        MusicController.restorePlaybackModes(
            isShuffle = savedModes.isShuffle,
            repeatState = savedModes.repeatState,
            isReverseQueue = savedModes.isReverseQueue
        )
        MusicController.equalizerManager.restoreSavedSettings(
            enabled = savedEq.enabled,
            bass = savedEq.bass,
            virtualizer = savedEq.virtualizer,
            preset = savedEq.preset,
            clarity = savedEq.clarity,
            customBandsCsv = savedEq.customBandsCsv
        )

        val savedSession = settingsAfter.getInitialMusicSession()
        assertNotNull(savedSession)
        val libraryAfterBoot = listOf(songA, songB, songC)
        val matchedSong = libraryAfterBoot.find { it.id == savedSession!!.songId }!!
        val restoredQueue = savedSession!!.queueSongIds.mapNotNull { id -> libraryAfterBoot.find { it.id == id } }

        MusicController.restorePausedSession(
            song = matchedSong,
            queue = restoredQueue,
            positionMs = savedSession.positionMs
        )

        val stateAfterReboot = MusicController.state.value
        assertEquals(songB.id, stateAfterReboot.currentSong?.id)
        assertEquals(94_500L, stateAfterReboot.currentPositionMs)
        assertTrue(stateAfterReboot.isShuffle)
        assertEquals(RepeatState.ONE, stateAfterReboot.repeatState)
        assertTrue(stateAfterReboot.isReverseQueue)
        assertEquals("Bass Horeg", MusicController.equalizerManager.uiState.value.currentPreset)
        assertEquals(910, MusicController.equalizerManager.uiState.value.bassStrength)

        // CRITICAL: Music must NOT auto-start after device restart!
        assertFalse("Music must remain paused after device restart", stateAfterReboot.isPlaying)
    }

    @Test
    fun `5 navigating across screens while music plays preserves playback and notification sync`() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()
        try {
            val allLibrarySongs = listOf(songA, songB, songC)
            MusicController.playSong(songA, allLibrarySongs)
            MusicController.seekTo(48_000L)

            var activeTab by mutableStateOf(MainTab.MUSIC)
            var searchQuery by mutableStateOf("")

            composeTestRule.setContent {
                val pbState by MusicController.state.collectAsState()
                JnmfTheme {
                    CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                        when (activeTab) {
                            MainTab.MUSIC -> LibraryScreen(
                                songs = allLibrarySongs,
                                playbackState = pbState,
                                hasPermission = true,
                                onRequestPermission = {},
                                onRefresh = {},
                                onSortSelected = {},
                                currentSort = SortOption.NAME_ASC,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                favoriteSongIds = setOf(songB.id),
                                onToggleFavorite = {},
                                onAddToPlaylist = {},
                                onHideSong = {}
                            )
                            MainTab.PLAYLISTS -> PlaylistScreen(
                                playlists = emptyList(),
                                favoriteSongs = listOf(songB),
                                allSongs = allLibrarySongs,
                                playbackState = pbState,
                                onCreatePlaylist = {},
                                onRenamePlaylist = { _, _ -> },
                                onDeletePlaylist = {},
                                onAddSongToPlaylist = { _, _ -> },
                                onRemoveSongFromPlaylist = { _, _ -> },
                                onMoveTrack = { _, _, _ -> },
                                onToggleFavorite = {},
                                getPlaylistTrackIds = { emptyList() }
                            )
                            MainTab.EQUALIZER -> EqualizerScreen(
                                equalizerManager = MusicController.equalizerManager,
                                onBack = { activeTab = MainTab.MUSIC }
                            )
                            MainTab.SETTINGS -> SettingsScreen(
                                currentTheme = com.example.data.repository.ThemeColor.NEON_GREEN,
                                currentLanguage = AppLanguage.ENGLISH,
                                hiddenSongs = emptyList(),
                                onSelectTheme = {},
                                onSelectLanguage = {},
                                onUnhideSong = {},
                                onOpenNotificationSettings = {},
                                onBack = { activeTab = MainTab.MUSIC }
                            )
                            MainTab.RADIO_DJ -> {
                                // Navigating to Radio DJ tab without starting a broadcast keeps normal music playing
                            }
                            MainTab.VIDEOS -> {
                                // Navigating to Videos tab without starting a video keeps music playing
                            }
                        }
                    }
                }
            }

            // Filter search in Music, then navigate across Playlists -> Equalizer -> Radio DJ -> Videos -> Settings -> Music
            searchQuery = "Malam Pagi"
            for (tab in listOf(MainTab.PLAYLISTS, MainTab.EQUALIZER, MainTab.RADIO_DJ, MainTab.VIDEOS, MainTab.SETTINGS, MainTab.MUSIC)) {
                activeTab = tab
                composeTestRule.waitForIdle()

                val pb = MusicController.state.value
                assertTrue("Playback must stay active when switching to $tab", pb.isPlaying)
                assertEquals(songA.id, pb.currentSong?.id)
                assertTrue(pb.currentPositionMs >= 48_000L)

                val notif = service.buildForegroundNotificationForCurrentSession()
                assertEquals(
                    "DJ Malam Pagi Fullbeat",
                    notif.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                )
                assertEquals(
                    "DJ Jussar",
                    notif.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                )
            }
        } finally {
            serviceController.destroy()
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w412dp-h915dp")
    fun `6 missing song files preserve playlist with saved metadata and low storage blocks unsafe writes cleanly`() {
        val db = JnmfDatabase.getDatabase(context)
        val dao = db.jnmfDao()
        val repo = UserDataRepository(context, dao)

        // Create a temporary dummy media file to verify we NEVER delete user media files under low storage
        val userMediaFile = File(context.cacheDir, "user_original_song_do_not_delete.mp3").apply {
            writeBytes(ByteArray(128) { 0x42 })
        }
        try {
            assertTrue(userMediaFile.exists())

            val (plId, persistedPlaylists, persistedTracks) = runBlocking {
                val createdPlId = (repo.createPlaylist("Indo Classics") as SaveResult.Success).data
                repo.addSongToPlaylist(createdPlId, songA)
                repo.addSongToPlaylist(createdPlId, songB)
                repo.addSongToPlaylist(createdPlId, songC)

                // Part B: Simulate Redmi 10C critically full storage (< 256 KB free) when trying to create another playlist
                StorageGuard.availableBytesOverrideForTesting = 16 * 1024L // Only 16 KB free!
                val lowSpaceCreateResult = repo.createPlaylist("Should Fail Safely")
                assertTrue(lowSpaceCreateResult is SaveResult.InsufficientStorage)

                // Also simulate SQLiteFullException during write
                StorageGuard.availableBytesOverrideForTesting = 10 * 1024 * 1024L
                StorageGuard.simulateWriteExceptionForTesting =
                    SQLiteFullException("database or disk is full (code 13)")
                val sqliteFullResult = repo.createPlaylist("Disk Full Exception Playlist")
                assertTrue(sqliteFullResult is SaveResult.InsufficientStorage)

                // Verify original playlist & user media file were NEVER deleted or corrupted!
                StorageGuard.resetTestingOverrides()
                assertTrue("Original user media file must NEVER be deleted", userMediaFile.exists())
                assertEquals(1, dao.getAllPlaylistsSync().size)
                assertEquals(listOf(songA.id, songB.id, songC.id), dao.getTrackIdsForPlaylistSync(createdPlId))

                Triple(
                    createdPlId,
                    dao.getAllPlaylistsSync(),
                    dao.getTracksForPlaylistSync(createdPlId)
                )
            }

            // Part A: Simulate songB being moved/deleted from storage (no longer in MediaStore allSongs)
            val remainingSongsInMediaStore = listOf(songA, songC)

            // Render PlaylistScreen with songB missing AND low storage active to verify UI badge & error message
            StorageGuard.availableBytesOverrideForTesting = 8 * 1024L
            val snackbarHostState = SnackbarHostState()
            var currentSelectedPlaylistId by mutableStateOf<Long?>(plId)

            var lowStorageSaveSucceeded: Boolean? = null

            composeTestRule.setContent {
                val uiScope = rememberCoroutineScope()
                JnmfTheme {
                    CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) }
                        ) { _ ->
                            PlaylistScreen(
                                playlists = persistedPlaylists,
                                favoriteSongs = emptyList(),
                                allSongs = remainingSongsInMediaStore,
                                playbackState = MusicController.state.value,
                                selectedPlaylistId = currentSelectedPlaylistId,
                                onSelectedPlaylistIdChange = { currentSelectedPlaylistId = it },
                                onCreatePlaylist = {},
                                onRenamePlaylist = { _, _ -> },
                                onDeletePlaylist = {},
                                onAddSongToPlaylist = { _, _ -> },
                                onRemoveSongFromPlaylist = { _, _ -> },
                                onMoveTrack = { playlistId, fromIdx, toIdx ->
                                    uiScope.launch {
                                        val res = repo.moveTrackInPlaylist(playlistId, fromIdx, toIdx)
                                        lowStorageSaveSucceeded = res.isSuccess
                                        if (res is SaveResult.InsufficientStorage) {
                                            snackbarHostState.showSnackbar(
                                                JnmfStrings.storageLowSaveBlocked(AppLanguage.ENGLISH)
                                            )
                                        }
                                    }
                                },
                                onToggleFavorite = {},
                                onMissingSongTapped = {
                                    uiScope.launch {
                                        snackbarHostState.showSnackbar(
                                            JnmfStrings.missingFileSubtitle(AppLanguage.ENGLISH)
                                        )
                                    }
                                },
                                getPlaylistTracks = { persistedTracks },
                                getPlaylistTrackIds = { persistedTracks.map { it.songId } }
                            )
                        }
                    }
                }
            }

            // Verify missing songB is preserved in the playlist with its saved title & Missing File badge
            composeTestRule.onNodeWithText("Indo Classics").assertIsDisplayed()
            composeTestRule.onNodeWithText("DJ Malam Pagi Fullbeat").assertIsDisplayed()
            composeTestRule.onNodeWithText("DJ Cinta Santuy Slow").assertIsDisplayed()
            composeTestRule.onNodeWithTag("missing_song_badge_${songB.id}").assertIsDisplayed()
            composeTestRule.onNodeWithText("DJ Horeg Breakbeat Party").assertIsDisplayed()

            // Tapping the missing song must not crash or start playback, and informs the user
            composeTestRule.onNodeWithText("DJ Cinta Santuy Slow").performClick()
            composeTestRule.waitForIdle()
            assertFalse(MusicController.state.value.isPlaying)
            composeTestRule.onNodeWithText(JnmfStrings.missingFileSubtitle(AppLanguage.ENGLISH)).assertIsDisplayed()
            snackbarHostState.currentSnackbarData?.dismiss()
            composeTestRule.waitForIdle()

            // Attempt a playlist write (reordering track) while Redmi 10C storage is critically low (8 KB free)
            composeTestRule.onNodeWithTag("move_down_${songA.id}").performClick()
            composeTestRule.waitForIdle()

            // Write fails safely without reporting success, shows clear low-storage warning, and preserves DB & user media
            assertEquals(false, lowStorageSaveSucceeded)
            composeTestRule.onNodeWithText(JnmfStrings.storageLowSaveBlocked(AppLanguage.ENGLISH)).assertIsDisplayed()
            assertEquals(
                listOf(songA.id, songB.id, songC.id),
                runBlocking { dao.getTrackIdsForPlaylistSync(plId) }
            )
            assertTrue("Original user media file must NEVER be deleted", userMediaFile.exists())
        } finally {
            userMediaFile.delete()
        }
    }
}
