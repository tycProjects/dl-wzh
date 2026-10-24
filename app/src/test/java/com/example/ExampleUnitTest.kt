package com.example

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import com.example.data.analysis.AudioAnalysisEngine
import com.example.data.db.HiddenSongEntity
import com.example.data.db.JnmfDatabase
import com.example.data.db.PlaylistEntity
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SortOption
import com.example.data.repository.ThemeColor
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.ui.components.JnmfRefreshTransitionBanner
import com.example.ui.components.TaskProgressState
import com.example.ui.components.TaskType
import com.example.ui.components.UiTransitionController
import com.example.ui.components.UiTransitionKind
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.StrKey
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JnmfBackgroundDark
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.getJnmfColorScheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ExampleUnitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: JnmfDatabase

    @Before
    fun createDb() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, JnmfDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testRoomPlaylistPersistenceAndReordering() = runBlocking {
        val dao = db.jnmfDao()

        // 1. Create playlist
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Indo Fullbeat Horeg"))
        assertTrue(playlistId > 0)

        // 2. Add songs
        dao.addSongToPlaylist(playlistId, 101L)
        dao.addSongToPlaylist(playlistId, 202L)
        dao.addSongToPlaylist(playlistId, 303L)

        // 3. Verify order
        var trackIds = dao.getTrackIdsForPlaylistSync(playlistId)
        assertEquals(listOf(101L, 202L, 303L), trackIds)

        // 4. Move track (move track at index 2 to index 0: 303 -> 101 -> 202)
        dao.moveTrackInPlaylist(playlistId, 2, 0)
        trackIds = dao.getTrackIdsForPlaylistSync(playlistId)
        assertEquals(listOf(303L, 101L, 202L), trackIds)

        // 5. Remove track
        dao.removeSongFromPlaylist(playlistId, 101L)
        trackIds = dao.getTrackIdsForPlaylistSync(playlistId)
        assertEquals(listOf(303L, 202L), trackIds)

        // 6. Rename playlist
        dao.renamePlaylist(playlistId, "Renamed Playlist")
        trackIds = dao.getTrackIdsForPlaylistSync(playlistId)
        assertEquals(listOf(303L, 202L), trackIds)
    }

    @Test
    fun testRemixClassification() {
        val context = RuntimeEnvironment.getApplication()
        val engine = AudioAnalysisEngine(context)

        val fullbeatSong = Song(
            id = 1L,
            title = "DJ Malam Pagi Fullbeat Horeg",
            artist = "DJ Indo",
            album = "Remix 2024",
            durationMs = 210000,
            sizeBytes = 8000000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/1")
        )

        val slowSong = Song(
            id = 2L,
            title = "DJ Cinta Slow Remix Santuy",
            artist = "DJ Indo",
            album = "Chill Remix",
            durationMs = 240000,
            sizeBytes = 6000000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/2")
        )

        assertEquals(RemixCategory.FULLBEAT, engine.classifySong(fullbeatSong))
        assertEquals(RemixCategory.SLOW, engine.classifySong(slowSong))
    }

    @Test
    fun testVideoItemFormatting() {
        val video = VideoItem(
            id = 5L,
            title = "Live Performance Indo Fullbeat",
            durationMs = 325000, // 5 min 25 sec
            sizeBytes = 41943040, // 40 MB
            dateAdded = 2000L,
            contentUri = Uri.parse("content://media/video/5")
        )

        assertEquals("05:25", video.formattedDuration)
    }

    @Test
    fun testSongFormattedDisplay() {
        val song = Song(
            id = 10L,
            title = "Test Track",
            artist = "Artist",
            album = "Album",
            durationMs = 195000, // 3 mins 15 secs
            sizeBytes = 10485760, // 10.0 MB
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/10")
        )

        assertEquals("03:15", song.formattedDuration)
        assertEquals("10.0 MB", song.formattedSize)
    }

    @Test
    fun testTaskProgressStateCalculations() {
        val indeterminateState = TaskProgressState(
            taskType = TaskType.STARTUP,
            title = "Initializing JNMF",
            progress = null,
            isRunning = true
        )
        assertEquals(0, indeterminateState.progressPercent)
        assertTrue(indeterminateState.isRunning)

        val measurableState = TaskProgressState(
            taskType = TaskType.MUSIC_SCAN,
            title = "Scanning Music",
            progress = 0.678f,
            current = 68,
            total = 100,
            isRunning = true
        )
        assertEquals(67, measurableState.progressPercent)

        val completedState = measurableState.copy(
            isCompleted = true,
            isRunning = false,
            statusMessage = "Found 100 tracks"
        )
        assertTrue(completedState.isCompleted)
        assertFalse(completedState.isRunning)
    }

    @Test
    fun testLanguageTranslationsCoverage() {
        val languages = listOf(
            AppLanguage.ENGLISH,
            AppLanguage.CEBUANO,
            AppLanguage.INDONESIAN
        )

        for (lang in languages) {
            assertTrue(JnmfStrings.navMusic(lang).isNotBlank())
            assertTrue(JnmfStrings.navPlaylists(lang).isNotBlank())
            assertTrue(JnmfStrings.navSettings(lang).isNotBlank())
            assertTrue(JnmfStrings.notifDialogTitle(lang).isNotBlank())
            assertTrue(JnmfStrings.notifDescription(lang).isNotBlank())
            assertTrue(JnmfStrings.uiColorTitle(lang).isNotBlank())
            assertTrue(JnmfStrings.langTitle(lang).isNotBlank())
        }

        assertEquals("Music", JnmfStrings.navMusic(AppLanguage.ENGLISH))
        assertEquals("Musika", JnmfStrings.navMusic(AppLanguage.CEBUANO))
        assertEquals("Musik", JnmfStrings.navMusic(AppLanguage.INDONESIAN))

        assertEquals("Settings", JnmfStrings.navSettings(AppLanguage.ENGLISH))
        assertEquals("Mga Setting", JnmfStrings.navSettings(AppLanguage.CEBUANO))
        assertEquals("Pengaturan", JnmfStrings.navSettings(AppLanguage.INDONESIAN))
    }

    @Test
    fun testCompleteCentralizedLocalizationCoverageAllThreeLanguages() {
        val languages = AppLanguage.entries
        assertEquals(3, languages.size)
        assertTrue(StrKey.entries.size >= 170)

        // Verify 100% explicit translation coverage for every key across English, Cebuano, and Bahasa Indonesia
        for (key in StrKey.entries) {
            for (lang in languages) {
                assertTrue(
                    "Missing explicit translation for $key in $lang",
                    JnmfStrings.hasExplicitTranslation(key, lang)
                )
                val value = JnmfStrings.get(key, lang)
                assertTrue(
                    "Blank translation for $key in $lang",
                    value.isNotBlank()
                )
            }
        }

        // Verify transition messages
        assertEquals("Applying language...", JnmfStrings.applyingLanguage(AppLanguage.ENGLISH))
        assertEquals("Nag-apply sa pinulongan...", JnmfStrings.applyingLanguage(AppLanguage.CEBUANO))
        assertEquals("Menerapkan bahasa...", JnmfStrings.applyingLanguage(AppLanguage.INDONESIAN))

        // Verify parameterized formatting helpers in all 3 languages (no format exceptions, preserves user content)
        val untranslateableSongTitle = "DJ Malam Pagi Fullbeat 2026"
        val untranslateablePlaylist = "My Horeg Favorites"
        for (lang in languages) {
            assertTrue(JnmfStrings.tracksCount(42, lang).contains("42"))
            assertTrue(JnmfStrings.startupScanProgress(5, 20, untranslateableSongTitle, lang).contains(untranslateableSongTitle))
            assertTrue(JnmfStrings.readingItemProgress(untranslateableSongTitle, 3, 10, lang).contains(untranslateableSongTitle))
            assertTrue(JnmfStrings.addSongToPlaylistTitle(untranslateableSongTitle, lang).contains(untranslateableSongTitle))
            assertTrue(JnmfStrings.addTracksToPlaylistTitle(untranslateablePlaylist, lang).contains(untranslateablePlaylist))
            assertTrue(JnmfStrings.deletePlaylistConfirmMsg(untranslateablePlaylist, lang).contains(untranslateablePlaylist))
            assertTrue(JnmfStrings.confirmDeleteSongMsg(untranslateableSongTitle, "8.4 MB", lang).contains(untranslateableSongTitle))
            assertTrue(JnmfStrings.eqPipelineAttached(108, lang).contains("108"))

            for (sort in SortOption.entries) {
                assertTrue(JnmfStrings.sortOptionLabel(sort, lang).isNotBlank())
            }
            for (theme in ThemeColor.entries) {
                assertTrue(JnmfStrings.themeColorName(theme, lang).isNotBlank())
                assertTrue(JnmfStrings.applyingThemeColor(theme, lang).isNotBlank())
            }
            val localizedReason = JnmfStrings.localizeMatchReason("Identical artist credit: DJ Jussar", lang)
            assertTrue(localizedReason.contains("DJ Jussar"))
        }
    }

    @Test
    fun testThemeColorsIntegrity() {
        val allThemes = ThemeColor.entries
        assertEquals(6, allThemes.size)
        assertEquals(ThemeColor.NEON_GREEN, allThemes.first()) // Green is default

        val keys = allThemes.map { it.key }.toSet()
        assertTrue(keys.contains("neon_green"))
        assertTrue(keys.contains("electric_blue"))
        assertTrue(keys.contains("crimson_red"))
        assertTrue(keys.contains("modern_white"))
        assertTrue(keys.contains("vibrant_cyan"))
        assertTrue(keys.contains("deep_violet"))

        val distinctPrimaries = mutableSetOf<Color>()
        for (theme in allThemes) {
            val scheme = getJnmfColorScheme(theme)
            assertNotNull(scheme.primary)
            assertEquals(JnmfBackgroundDark, scheme.background)
            distinctPrimaries.add(scheme.primary)

            // Verify high-contrast onPrimary text readability
            when (theme) {
                ThemeColor.CRIMSON_RED, ThemeColor.DEEP_VIOLET -> assertEquals(Color.White, scheme.onPrimary)
                else -> assertEquals(Color.Black, scheme.onPrimary)
            }
        }
        assertEquals("All 6 accent colors must have distinct primary colors", 6, distinctPrimaries.size)
    }

    @Test
    fun testLanguageAndColorPersistenceAcrossReopening() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo1 = SettingsRepository(context)

        // Test persisting all 3 languages and verifying across new repository instances
        for (lang in AppLanguage.entries) {
            repo1.setLanguage(lang)
            val reopenedRepo = SettingsRepository(context)
            assertEquals(lang, reopenedRepo.getInitialLanguage())
            assertEquals(lang, SettingsRepository.getSavedLanguage(context))
            assertEquals(lang, reopenedRepo.languageFlow.first())
        }

        // Test persisting all 6 accent colors and verifying across new repository instances
        for (theme in ThemeColor.entries) {
            repo1.setThemeColor(theme)
            val reopenedRepo = SettingsRepository(context)
            assertEquals(theme, reopenedRepo.getInitialThemeColor())
            assertEquals(theme, SettingsRepository.getSavedThemeColor(context))
            assertEquals(theme, reopenedRepo.themeColorFlow.first())
        }
    }

    @Test
    fun testSmoothTransitionControllerAnimationsRapidTapsAndErrorRecovery() = runTest {
        val controller = UiTransitionController(this)
        var appliedLanguage = AppLanguage.ENGLISH
        var persistedLanguage = AppLanguage.ENGLISH
        var appliedTheme = ThemeColor.NEON_GREEN
        var persistedTheme = ThemeColor.NEON_GREEN

        // 1. Language transition shows localized refresh banner and dismisses cleanly
        controller.triggerLanguageTransition(
            newLanguage = AppLanguage.CEBUANO,
            currentLanguage = appliedLanguage,
            reducedMotion = false,
            onApplyImmediate = { appliedLanguage = it },
            persistAction = {
                delay(40)
                persistedLanguage = it
            }
        )

        assertEquals(AppLanguage.CEBUANO, appliedLanguage)
        assertTrue(controller.state.value.isActive)
        assertEquals(UiTransitionKind.LANGUAGE, controller.state.value.kind)
        assertEquals(AppLanguage.CEBUANO, controller.state.value.targetLanguage)

        advanceUntilIdle()
        assertFalse(controller.state.value.isActive)
        assertEquals(UiTransitionKind.NONE, controller.state.value.kind)
        assertEquals(AppLanguage.CEBUANO, persistedLanguage)

        // 2. Rapid overlapping taps on accent colors cancel superseded jobs without getting stuck
        val rapidSequence = listOf(
            ThemeColor.ELECTRIC_BLUE,
            ThemeColor.CRIMSON_RED,
            ThemeColor.MODERN_WHITE,
            ThemeColor.VIBRANT_CYAN,
            ThemeColor.DEEP_VIOLET
        )
        for (theme in rapidSequence) {
            controller.triggerThemeTransition(
                newTheme = theme,
                currentTheme = appliedTheme,
                currentLanguage = appliedLanguage,
                reducedMotion = false,
                onApplyImmediate = { appliedTheme = it },
                persistAction = {
                    delay(60)
                    persistedTheme = it
                }
            )
            advanceTimeBy(25)
        }

        assertEquals(ThemeColor.DEEP_VIOLET, appliedTheme)
        assertTrue(controller.state.value.isActive)
        assertEquals(UiTransitionKind.ACCENT_COLOR, controller.state.value.kind)

        advanceUntilIdle()
        assertFalse(controller.state.value.isActive)
        assertEquals(UiTransitionKind.NONE, controller.state.value.kind)
        assertEquals(ThemeColor.DEEP_VIOLET, persistedTheme)

        // 3. Reduced motion + persistence error still recovers cleanly without freezing UI
        controller.triggerLanguageTransition(
            newLanguage = AppLanguage.INDONESIAN,
            currentLanguage = appliedLanguage,
            reducedMotion = true,
            onApplyImmediate = { appliedLanguage = it },
            persistAction = {
                throw IllegalStateException("Simulated disk error")
            }
        )
        assertEquals(AppLanguage.INDONESIAN, appliedLanguage)
        advanceUntilIdle()
        assertFalse(controller.state.value.isActive)
        assertEquals(UiTransitionKind.NONE, controller.state.value.kind)
    }

    @Test
    fun testPlaybackQueueAndEqualizerPreservedDuringLanguageAndColorChanges() {
        val song1 = Song(
            id = 101L,
            title = "DJ Malam Pagi Fullbeat Original",
            artist = "DJ Jussar",
            album = "Indo Bass Vol 1",
            durationMs = 210000,
            sizeBytes = 8000000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/101")
        )
        val song2 = Song(
            id = 102L,
            title = "DJ Cinta Santuy Slow Remix",
            artist = "DJ Golden",
            album = "Indo Bass Vol 2",
            durationMs = 230000,
            sizeBytes = 8500000,
            dateAdded = 2000L,
            contentUri = Uri.parse("content://media/102")
        )

        val queue = listOf(song1, song2)
        MusicController.playSong(song1, queue)
        MusicController.next() // Advance to song2 at index 1

        val eq = MusicController.equalizerManager
        eq.setEnabled(true)
        eq.applyPreset("Bass Horeg")
        eq.setBassStrength(880)
        val expectedBands = eq.uiState.value.bands.map { it.currentLevelMilliBel }

        // Cycle through all 3 languages and all 6 accent color schemes while music is playing
        for (lang in AppLanguage.entries) {
            MusicController.setLanguage(lang)
            assertEquals(lang, MusicController.language.value)

            for (theme in ThemeColor.entries) {
                val scheme = getJnmfColorScheme(theme)
                assertNotNull(scheme.primary)

                val pbState = MusicController.state.value
                assertTrue("Music must remain playing during $lang / $theme change", pbState.isPlaying)
                assertEquals(102L, pbState.currentSong?.id)
                assertEquals("DJ Cinta Santuy Slow Remix", pbState.currentSong?.title)
                assertEquals("DJ Golden", pbState.currentSong?.artist)
                assertEquals(1, pbState.queueIndex)
                assertEquals(2, pbState.queue.size)

                val eqState = eq.uiState.value
                assertTrue(eqState.isEnabled)
                assertEquals("Bass Horeg", eqState.currentPreset)
                assertEquals(880, eqState.bassStrength)
                assertEquals(expectedBands, eqState.bands.map { it.currentLevelMilliBel })
            }
        }
    }

    @Test
    fun testComposeInPlaceLanguageAndColorUpdatePreservesScreenState() {
        var currentThemeState = ThemeColor.NEON_GREEN
        var currentLangState = AppLanguage.ENGLISH

        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            val controller = remember(scope) { UiTransitionController(scope) }
            val transitionState by controller.state.collectAsState()
            var theme by remember { mutableStateOf(ThemeColor.NEON_GREEN) }
            var lang by remember { mutableStateOf(AppLanguage.ENGLISH) }

            currentThemeState = theme
            currentLangState = lang

            JnmfTheme(themeColor = theme) {
                CompositionLocalProvider(LocalAppLanguage provides lang) {
                    Box {
                        SettingsScreen(
                            currentTheme = theme,
                            currentLanguage = lang,
                            hiddenSongs = listOf(
                                HiddenSongEntity(songId = 7L, title = "Untranslated Hidden Track", artist = "DJ Test")
                            ),
                            onSelectTheme = { nextTheme ->
                                controller.triggerThemeTransition(
                                    newTheme = nextTheme,
                                    currentTheme = theme,
                                    currentLanguage = lang,
                                    reducedMotion = false,
                                    onApplyImmediate = { theme = it },
                                    persistAction = {}
                                )
                            },
                            onSelectLanguage = { nextLang ->
                                controller.triggerLanguageTransition(
                                    newLanguage = nextLang,
                                    currentLanguage = lang,
                                    reducedMotion = false,
                                    onApplyImmediate = { lang = it },
                                    persistAction = {}
                                )
                            },
                            onUnhideSong = {},
                            onOpenNotificationSettings = {}
                        )
                        JnmfRefreshTransitionBanner(state = transitionState)
                    }
                }
            }
        }

        // Initial English state on SettingsScreen
        composeTestRule.onNodeWithText("JNMF Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("UI Accent Color").assertIsDisplayed()

        // Tap Cebuano option in SettingsScreen -> stays on Settings, updates text in place, shows transition banner
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("lang_ceb").performClick()
        composeTestRule.mainClock.advanceTimeBy(80)

        assertEquals(AppLanguage.CEBUANO, currentLangState)
        composeTestRule.onNodeWithText("Mga Setting sa JNMF").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kolor sa Tema").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ui_transition_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nag-apply sa pinulongan...").assertIsDisplayed()

        // Advance past transition hold -> banner dismisses cleanly, stays on Cebuano Settings
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitUntil(timeoutMillis = 2500) {
            !transitionStateSnapshotIsActive(composeTestRule)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mga Setting sa JNMF").assertIsDisplayed()

        // Tap Violet color swatch -> updates theme in place without leaving Settings
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("theme_color_deep_violet").performClick()
        composeTestRule.mainClock.advanceTimeBy(80)

        assertEquals(ThemeColor.DEEP_VIOLET, currentThemeState)
        composeTestRule.onNodeWithTag("ui_transition_banner").assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitUntil(timeoutMillis = 2500) {
            !transitionStateSnapshotIsActive(composeTestRule)
        }
        composeTestRule.waitForIdle()

        // Scroll to Bahasa Indonesia (JUSSARSian) & tap -> updates Settings in place while preserving scroll position
        composeTestRule.onNodeWithTag("lang_in").performScrollTo()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("lang_in").performClick()
        composeTestRule.mainClock.advanceTimeBy(80)

        assertEquals(AppLanguage.INDONESIAN, currentLangState)
        composeTestRule.onNodeWithTag("ui_transition_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Menerapkan bahasa...").assertIsDisplayed()
        composeTestRule.onNodeWithText(JnmfStrings.langTitle(AppLanguage.INDONESIAN)).assertIsDisplayed()
        composeTestRule.onNodeWithTag("lang_in").assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitUntil(timeoutMillis = 2500) {
            !transitionStateSnapshotIsActive(composeTestRule)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lang_in").assertIsDisplayed()
    }

    private fun transitionStateSnapshotIsActive(
        rule: androidx.compose.ui.test.junit4.ComposeContentTestRule
    ): Boolean = try {
        rule.onAllNodes(androidx.compose.ui.test.hasTestTag("ui_transition_banner"))
            .fetchSemanticsNodes()
            .isNotEmpty()
    } catch (_: Exception) {
        false
    }

    @Test
    fun testSongMetadataPreservation() {
        val originalTitle = "DJSound Indo Remix Fullbeat 2026"
        val originalArtist = "DJ JUSSAR"
        val song = Song(
            id = 99L,
            title = originalTitle,
            artist = originalArtist,
            album = "Indo Bass",
            durationMs = 210000,
            sizeBytes = 8000000,
            dateAdded = 5000L,
            contentUri = Uri.parse("content://media/99")
        )

        assertEquals(originalTitle, song.title)
        assertEquals(originalArtist, song.artist)
    }

    @Test
    fun testVideoTranslationsCoverage() {
        val languages = listOf(
            AppLanguage.ENGLISH,
            AppLanguage.CEBUANO,
            AppLanguage.INDONESIAN
        )

        for (lang in languages) {
            assertTrue(JnmfStrings.fullscreen(lang).isNotBlank())
            assertTrue(JnmfStrings.exitFullscreen(lang).isNotBlank())
            assertTrue(JnmfStrings.closePlayer(lang).isNotBlank())
            assertTrue(JnmfStrings.moreVideos(lang).isNotBlank())
        }

        assertEquals("Fullscreen", JnmfStrings.fullscreen(AppLanguage.ENGLISH))
        assertEquals("Tibuok Screen", JnmfStrings.fullscreen(AppLanguage.CEBUANO))
        assertEquals("Layar Penuh", JnmfStrings.fullscreen(AppLanguage.INDONESIAN))

        assertEquals("Exit Fullscreen", JnmfStrings.exitFullscreen(AppLanguage.ENGLISH))
        assertEquals("Gawas sa Tibuok Screen", JnmfStrings.exitFullscreen(AppLanguage.CEBUANO))
        assertEquals("Keluar Layar Penuh", JnmfStrings.exitFullscreen(AppLanguage.INDONESIAN))
    }

    @Test
    fun testRedmi10CAspectRatioBounds() {
        val redmi10CWidth = 720f
        val redmi10CHeight = 1650f
        val landscapeRatio = redmi10CHeight / redmi10CWidth
        val standard169 = 16f / 9f
        val ultrawide219 = 21f / 9f

        val coercedLandscape = landscapeRatio.coerceIn(1.1f, 2.4f)
        assertEquals(landscapeRatio, coercedLandscape, 0.001f)

        val coerced169 = standard169.coerceIn(1.1f, 2.4f)
        assertEquals(standard169, coerced169, 0.001f)

        val coercedUltrawide = ultrawide219.coerceIn(1.1f, 2.4f)
        assertEquals(ultrawide219, coercedUltrawide, 0.001f)
    }

    @Test
    fun testMediaItemMetadataCreation() {
        val song = Song(
            id = 42L,
            title = "DJ Malam Pagi Indo Bass",
            artist = "DJ Jussar",
            album = "Indo Remix Vol 1",
            durationMs = 215000,
            sizeBytes = 8500000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/external/audio/media/42"),
            albumArtUri = Uri.parse("content://media/external/audio/albumart/5")
        )

        val mediaItem = MusicController.buildMediaItem(song)
        assertEquals("42", mediaItem.mediaId)
        assertEquals(Uri.parse("content://media/external/audio/media/42"), mediaItem.localConfiguration?.uri)

        val metadata = mediaItem.mediaMetadata
        assertEquals("DJ Malam Pagi Indo Bass", metadata.title?.toString())
        assertEquals("DJ Jussar", metadata.artist?.toString())
        assertEquals("Indo Remix Vol 1", metadata.albumTitle?.toString())
        assertEquals(Uri.parse("content://media/external/audio/albumart/5"), metadata.artworkUri)
        assertEquals(true, metadata.isPlayable)
    }

    @Test
    fun testPlaybackServiceActionConstants() {
        assertEquals("com.example.playback.ACTION_PREVIOUS", PlaybackService.ACTION_PREVIOUS)
        assertEquals("com.example.playback.ACTION_PLAY_PAUSE", PlaybackService.ACTION_PLAY_PAUSE)
        assertEquals("com.example.playback.ACTION_PLAY", PlaybackService.ACTION_PLAY)
        assertEquals("com.example.playback.ACTION_PAUSE", PlaybackService.ACTION_PAUSE)
        assertEquals("com.example.playback.ACTION_NEXT", PlaybackService.ACTION_NEXT)
        assertEquals("jnmf_playback_channel", PlaybackService.CHANNEL_ID)
        assertEquals(1001, PlaybackService.NOTIFICATION_ID)
    }

    @Test
    fun testMediaDrawablesResourcesExist() {
        val context = RuntimeEnvironment.getApplication()
        val prevResId = R.drawable.ic_skip_previous
        val playResId = R.drawable.ic_play_arrow
        val pauseResId = R.drawable.ic_pause
        val nextResId = R.drawable.ic_skip_next
        val officialLogoResId = R.drawable.ic_jnmf_official_logo

        assertTrue(prevResId != 0)
        assertTrue(playResId != 0)
        assertTrue(pauseResId != 0)
        assertTrue(nextResId != 0)
        assertTrue(officialLogoResId != 0)

        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, prevResId))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, playResId))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, pauseResId))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, nextResId))
        assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, officialLogoResId))
    }

    @Test
    fun testNextAndPreviousQueueNavigation() {
        val song1 = Song(
            id = 1L,
            title = "DJ Fullbeat Indo Track 1",
            artist = "Artist 1",
            album = "Album 1",
            durationMs = 180000,
            sizeBytes = 5000000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/1")
        )
        val song2 = Song(
            id = 2L,
            title = "DJ Slow Indo Track 2",
            artist = "Artist 2",
            album = "Album 2",
            durationMs = 200000,
            sizeBytes = 6000000,
            dateAdded = 2000L,
            contentUri = Uri.parse("content://media/2")
        )
        val song3 = Song(
            id = 3L,
            title = "DJ Koplo Indo Track 3",
            artist = "Artist 3",
            album = "Album 3",
            durationMs = 220000,
            sizeBytes = 7000000,
            dateAdded = 3000L,
            contentUri = Uri.parse("content://media/3")
        )

        val queue = listOf(song1, song2, song3)
        MusicController.playSong(song1, queue)

        val state1 = MusicController.state.value
        assertEquals(1L, state1.currentSong?.id)
        assertEquals("DJ Fullbeat Indo Track 1", state1.currentSong?.title)
        assertEquals("Artist 1", state1.currentSong?.artist)
        assertEquals(0, state1.queueIndex)

        MusicController.next()
        val state2 = MusicController.state.value
        assertEquals(2L, state2.currentSong?.id)
        assertEquals("DJ Slow Indo Track 2", state2.currentSong?.title)
        assertEquals("Artist 2", state2.currentSong?.artist)
        assertEquals(1, state2.queueIndex)

        MusicController.next()
        val state3 = MusicController.state.value
        assertEquals(3L, state3.currentSong?.id)
        assertEquals("DJ Koplo Indo Track 3", state3.currentSong?.title)
        assertEquals("Artist 3", state3.currentSong?.artist)
        assertEquals(2, state3.queueIndex)

        MusicController.next()
        val state4 = MusicController.state.value
        assertEquals(1L, state4.currentSong?.id)
        assertEquals(0, state4.queueIndex)

        MusicController.previous()
        val state5 = MusicController.state.value
        assertEquals(3L, state5.currentSong?.id)
        assertEquals("DJ Koplo Indo Track 3", state5.currentSong?.title)
        assertEquals(2, state5.queueIndex)

        MusicController.previous()
        val state6 = MusicController.state.value
        assertEquals(2L, state6.currentSong?.id)
        assertEquals("DJ Slow Indo Track 2", state6.currentSong?.title)
        assertEquals(1, state6.queueIndex)
    }

    @Test
    fun testPlayPauseStateAndImmediateMetadata() {
        val song = Song(
            id = 55L,
            title = "DJ Malam Pagi Remix",
            artist = "DJ Jussar",
            album = "Indo Bass 2026",
            durationMs = 210000,
            sizeBytes = 6000000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/55")
        )

        MusicController.playSong(song, listOf(song))
        val state = MusicController.state.value
        assertEquals("DJ Malam Pagi Remix", state.currentSong?.title)
        assertEquals("DJ Jussar", state.currentSong?.artist)
        assertEquals(true, state.isPlaying)

        MusicController.pause()
        MusicController.resume()
    }
}
