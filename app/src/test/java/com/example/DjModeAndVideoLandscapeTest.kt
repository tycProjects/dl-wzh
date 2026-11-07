package com.example

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.DjModeSettings
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ThemeColor
import com.example.playback.MusicController
import com.example.playback.RepeatState
import com.example.playback.VideoPlaybackController
import com.example.playback.VideoPlaybackMode
import com.example.playback.VideoQueueMode
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoScreen
import com.example.ui.theme.JnmfTheme
import kotlinx.coroutines.flow.first
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class DjModeAndVideoLandscapeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSongs = listOf(
        Song(
            id = 101L,
            title = "Indo Fullbeat Anthem 1",
            artist = "DJ Jussar",
            album = "JNMF Club",
            durationMs = 200_000L,
            contentUri = Uri.parse("content://media/external/audio/media/101"),
            albumArtUri = null,
            dateAdded = 1_000L,
            sizeBytes = 6_000_000L,
            fileName = "Indo_Fullbeat_Anthem_1.mp3",
            remixCategory = RemixCategory.FULLBEAT
        ),
        Song(
            id = 102L,
            title = "Indo Slow Bass 2",
            artist = "DJ Golden",
            album = "JNMF Club",
            durationMs = 210_000L,
            contentUri = Uri.parse("content://media/external/audio/media/102"),
            albumArtUri = null,
            dateAdded = 2_000L,
            sizeBytes = 6_500_000L,
            fileName = "Indo_Slow_Bass_2.mp3",
            remixCategory = RemixCategory.SLOW
        ),
        Song(
            id = 103L,
            title = "Breakbeat Finale 3",
            artist = "DJ Indo",
            album = "JNMF Club",
            durationMs = 195_000L,
            contentUri = Uri.parse("content://media/external/audio/media/103"),
            albumArtUri = null,
            dateAdded = 3_000L,
            sizeBytes = 6_200_000L,
            fileName = "Breakbeat_Finale_3.mp3",
            remixCategory = RemixCategory.INDO_REMIX
        )
    )

    private val testVideos = listOf(
        VideoItem(
            id = 701L,
            title = "JNMF Live DJ Set #1",
            durationMs = 180_000L,
            sizeBytes = 32L * 1024L * 1024L,
            dateAdded = 10_000L,
            contentUri = Uri.parse("content://media/external/video/media/701")
        ),
        VideoItem(
            id = 702L,
            title = "JNMF Live DJ Set #2",
            durationMs = 240_000L,
            sizeBytes = 40L * 1024L * 1024L,
            dateAdded = 20_000L,
            contentUri = Uri.parse("content://media/external/video/media/702")
        )
    )

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(SettingsRepository.SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        MusicController.resetInMemoryStateForTesting()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
        VideoPlaybackController.setQueueMode(VideoQueueMode.PLAY_ONCE)
    }

    @After
    fun tearDown() {
        MusicController.resetInMemoryStateForTesting()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
    }

    @Test
    fun djModeSettingsUi_defaultsOff_hidesOptionsWhenOff_shows3s5s10sWith3sDefaultWhenOn() {
        var enabledState by mutableStateOf(false)
        var secondsState by mutableIntStateOf(DjModeSettings.DEFAULT_TRANSITION_SECONDS)

        composeTestRule.setContent {
            JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                SettingsScreen(
                    currentTheme = ThemeColor.NEON_GREEN,
                    currentLanguage = AppLanguage.ENGLISH,
                    hiddenSongs = emptyList(),
                    onSelectTheme = {},
                    onSelectLanguage = {},
                    onUnhideSong = {},
                    djModeEnabled = enabledState,
                    djTransitionSeconds = secondsState,
                    onDjModeEnabledChange = { enabledState = it },
                    onDjTransitionSecondsChange = { secondsState = it }
                )
            }
        }

        // Scroll LazyColumn to DJ Mode card so it is fully on-screen
        composeTestRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("settings_dj_mode_card"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dj_mode_switch").assertIsOff()
        composeTestRule.onNodeWithTag("dj_duration_3s").assertDoesNotExist()
        composeTestRule.onNodeWithTag("dj_duration_5s").assertDoesNotExist()
        composeTestRule.onNodeWithTag("dj_duration_10s").assertDoesNotExist()
        composeTestRule.onNodeWithTag("dj_duration_20s").assertDoesNotExist()

        // Turn DJ Mode ON -> transition options appear with 3 Seconds selected by default
        composeTestRule.onNodeWithTag("dj_mode_switch").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("dj_transition_options_row"))
        composeTestRule.waitForIdle()

        assertTrue(enabledState)
        assertEquals(3, secondsState)
        composeTestRule.onNodeWithTag("dj_mode_switch").assertIsOn()
        composeTestRule.onNodeWithTag("dj_duration_3s").assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithTag("dj_duration_5s").assertIsDisplayed().assertIsNotSelected()
        composeTestRule.onNodeWithTag("dj_duration_10s").assertIsDisplayed().assertIsNotSelected()
        composeTestRule.onNodeWithTag("dj_duration_20s").assertIsDisplayed().assertIsNotSelected()

        // Select 5 Seconds -> exclusively selected
        composeTestRule.onNodeWithTag("dj_duration_5s").performClick()
        composeTestRule.waitForIdle()
        assertEquals(5, secondsState)
        composeTestRule.onNodeWithTag("dj_duration_5s").assertIsSelected()
        composeTestRule.onNodeWithTag("dj_duration_3s").assertIsNotSelected()
        composeTestRule.onNodeWithTag("dj_duration_10s").assertIsNotSelected()
        composeTestRule.onNodeWithTag("dj_duration_20s").assertIsNotSelected()

        // Select 10 Seconds -> exclusively selected
        composeTestRule.onNodeWithTag("dj_duration_10s").performClick()
        composeTestRule.waitForIdle()
        assertEquals(10, secondsState)
        composeTestRule.onNodeWithTag("dj_duration_10s").assertIsSelected()
        composeTestRule.onNodeWithTag("dj_duration_5s").assertIsNotSelected()
        composeTestRule.onNodeWithTag("dj_duration_20s").assertIsNotSelected()

        // Select 20 Seconds -> exclusively selected
        composeTestRule.onNodeWithTag("dj_duration_20s").performClick()
        composeTestRule.waitForIdle()
        assertEquals(20, secondsState)
        composeTestRule.onNodeWithTag("dj_duration_20s").assertIsSelected()
        composeTestRule.onNodeWithTag("dj_duration_10s").assertIsNotSelected()
        composeTestRule.onNodeWithTag("dj_duration_5s").assertIsNotSelected()
        composeTestRule.onNodeWithTag("dj_duration_3s").assertIsNotSelected()

        // Turn DJ Mode OFF -> transition options hide again
        composeTestRule.onNodeWithTag("dj_mode_switch").performClick()
        composeTestRule.waitForIdle()
        assertFalse(enabledState)
        composeTestRule.onNodeWithTag("dj_mode_switch").assertIsOff()
        composeTestRule.onNodeWithTag("dj_duration_3s").assertDoesNotExist()
        composeTestRule.onNodeWithTag("dj_duration_5s").assertDoesNotExist()
        composeTestRule.onNodeWithTag("dj_duration_10s").assertDoesNotExist()
        composeTestRule.onNodeWithTag("dj_duration_20s").assertDoesNotExist()
    }

    @Test
    fun djModePersistence_savesAndRestoresEnabledStateAndSelectedDurationAcrossReopen() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo1 = SettingsRepository(context)

        // Fresh default is OFF with 3 Seconds
        val initial = repo1.getInitialDjModeSettings()
        assertFalse(initial.enabled)
        assertEquals(3, initial.transitionSeconds)

        // Enable DJ Mode and select 10 Seconds
        assertTrue(repo1.setDjModeEnabled(true))
        assertTrue(repo1.setDjTransitionSeconds(10))

        // Simulate app closed and reopened with a fresh repository instance + cold MusicController
        MusicController.resetInMemoryStateForTesting()
        val repo2 = SettingsRepository(context)
        val restoredSync = repo2.getInitialDjModeSettings()
        val restoredFlow = repo2.djModeSettingsFlow.first()

        assertTrue(restoredSync.enabled)
        assertEquals(10, restoredSync.transitionSeconds)
        assertTrue(restoredFlow.enabled)
        assertEquals(10, restoredFlow.transitionSeconds)

        // Also verify 5 Seconds persistence and live update without app restart
        assertTrue(repo2.setDjTransitionSeconds(5))
        MusicController.setDjModeSettings(enabled = true, transitionSeconds = 5, persist = false)
        assertEquals(true, MusicController.state.value.djModeEnabled)
        assertEquals(5, MusicController.state.value.djTransitionSeconds)
        assertEquals(5, SettingsRepository.getSavedDjModeSettings(context).transitionSeconds)
    }

    @Test
    fun djModePlayback_offPlaysNormally_onTransitionsAt3s5sAnd10sWithSmoothDualDeckCrossfade() {
        val context = RuntimeEnvironment.getApplication()
        val primaryExoPlayer = MusicController.buildMusicExoPlayer(context)
        try {
            MusicController.bindAppContext(context)
            MusicController.attachPlayer(primaryExoPlayer)
            MusicController.djCrossfadeStepDelayMs = 0L // deterministic synchronous envelope verification
            MusicController.restorePlaybackModes(
                isShuffle = false,
                repeatState = RepeatState.ALL,
                isReverseQueue = false
            )

            // 1. DJ Mode OFF: only 1 deck allocated, no early transition at 10s, 5s, or 3s before track end
            MusicController.setDjModeSettings(enabled = false, transitionSeconds = 10)
            MusicController.playSong(testSongs[0], testSongs)
            assertEquals(101L, MusicController.state.value.currentSong?.id)
            assertEquals(1, MusicController.totalAllocatedDecksForTesting())

            MusicController.evaluatePlaybackPositionForDjTransition(190_000L, 200_000L)
            MusicController.evaluatePlaybackPositionForDjTransition(195_000L, 200_000L)
            MusicController.evaluatePlaybackPositionForDjTransition(198_500L, 200_000L)
            assertEquals("DJ Mode OFF must not transition early", 101L, MusicController.state.value.currentSong?.id)
            assertEquals(1.0f, MusicController.activeDeckForTesting()!!.volume, 0.001f)

            // 2. DJ Mode ON — 3 Seconds (advance preload at 3.5s remaining, crossfade at 3.0s remaining)
            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 3)
            MusicController.evaluatePlaybackPositionForDjTransition(196_500L, 200_000L) // 3.5s remaining -> preloads standby deck silently
            assertEquals(101L, MusicController.state.value.currentSong?.id)
            assertEquals(0.0f, MusicController.standbyDeckForTesting()!!.volume, 0.001f)

            MusicController.evaluatePlaybackPositionForDjTransition(197_000L, 200_000L) // 3.0s remaining -> overlapping crossfade Deck A -> Deck B!
            assertEquals("3s DJ Mode must transition to Song #2", 102L, MusicController.state.value.currentSong?.id)
            assertTrue("Overlap samples must record simultaneous dual-deck ramp", MusicController.recordedOverlapSamples.size >= 12)
            assertTrue("Both decks must be active simultaneously during overlap", MusicController.recordedOverlapSamples.all { it.bothDecksActive })
            assertEquals("Active incoming deck volume must reach 1.0f after fade-in", 1.0f, MusicController.activeDeckForTesting()!!.volume, 0.001f)
            assertEquals("Outgoing deck must be silenced and cleared", 0.0f, primaryExoPlayer.volume, 0.001f)
            assertEquals(0, primaryExoPlayer.mediaItemCount)
            assertEquals(2, MusicController.totalAllocatedDecksForTesting())

            // 3. DJ Mode ON — 5 Seconds (on Song #2, duration 210_000ms -> swaps back Deck B -> Deck A at 205_000ms)
            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 5)
            MusicController.evaluatePlaybackPositionForDjTransition(204_000L, 210_000L) // 6.0s remaining -> preloads Deck A
            assertEquals(102L, MusicController.state.value.currentSong?.id)

            MusicController.evaluatePlaybackPositionForDjTransition(205_000L, 210_000L) // 5.0s remaining -> trigger!
            assertEquals("5s DJ Mode must transition to Song #3", 103L, MusicController.state.value.currentSong?.id)
            assertEquals(primaryExoPlayer, MusicController.activeDeckForTesting())
            assertEquals(1.0f, MusicController.activeDeckForTesting()!!.volume, 0.001f)

            // 4. DJ Mode ON — 10 Seconds (on Song #3, duration 195_000ms -> wraps to Song #1 at 185_000ms under Repeat.ALL)
            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 10)
            MusicController.evaluatePlaybackPositionForDjTransition(184_000L, 195_000L) // 11.0s remaining -> preloads
            assertEquals(103L, MusicController.state.value.currentSong?.id)

            MusicController.evaluatePlaybackPositionForDjTransition(185_000L, 195_000L) // 10.0s remaining -> trigger!
            assertEquals("10s DJ Mode must transition back to Song #1 under Repeat.ALL", 101L, MusicController.state.value.currentSong?.id)
            assertEquals(1.0f, MusicController.activeDeckForTesting()!!.volume, 0.001f)
            assertTrue(MusicController.totalAllocatedDecksForTesting() <= 2)

            // 5. Repeat.OFF at final song in queue: must NOT cut off final song early
            MusicController.restorePlaybackModes(isShuffle = false, repeatState = RepeatState.OFF, isReverseQueue = false)
            MusicController.playSong(testSongs[2], testSongs)
            assertEquals(103L, MusicController.state.value.currentSong?.id)
            MusicController.evaluatePlaybackPositionForDjTransition(190_000L, 195_000L)
            assertEquals("Final song with Repeat.OFF must play to natural completion", 103L, MusicController.state.value.currentSong?.id)
        } finally {
            MusicController.detachPlayer()
            primaryExoPlayer.release()
        }
    }

    @Test
    fun videoOrientation_switchesPortraitAndLandscapeWithAutoRotateOff_respectsLockAndRestoresOnExit() {
        val appContext = RuntimeEnvironment.getApplication()
        // Explicitly simulate system Auto-Rotate OFF
        Settings.System.putInt(appContext.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)

        val videoPlayer = ExoPlayer.Builder(appContext).build()
        try {
            VideoPlaybackController.attachPlayer(videoPlayer)
            VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 42_000L)
            val originalPlayerId = VideoPlaybackController.state.value.playerInstanceId

            var hostActivity: Activity? = null
            var showVideoScreen by mutableStateOf(true)
            var fullscreenState by mutableStateOf(false)
            var landscapeState by mutableStateOf(false)

            composeTestRule.setContent {
                hostActivity = LocalContext.current as? Activity
                JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                    CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                        if (showVideoScreen) {
                            VideoScreen(
                                videos = testVideos,
                                isFullscreen = fullscreenState,
                                onFullscreenChanged = { fullscreenState = it },
                                onLandscapeChanged = { landscapeState = it },
                                isPlayerExpanded = true,
                                language = AppLanguage.ENGLISH
                            )
                        }
                    }
                }
            }
            composeTestRule.waitForIdle()
            val activity = hostActivity
            assertNotNull("Host Activity must be available", activity)

            // Orientation controls visible in normal embedded player
            composeTestRule.onNodeWithTag("embedded_video_orientation_button").assertIsDisplayed()

            // Tap orientation button in Portrait -> switches to Landscape even with Auto-Rotate OFF
            composeTestRule.onNodeWithTag("embedded_video_orientation_button").performClick()
            composeTestRule.waitForIdle()

            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                activity!!.requestedOrientation
            )
            assertTrue(landscapeState)
            // Continuous playback: same video, same player instance, playback position preserved
            assertEquals(701L, VideoPlaybackController.state.value.currentVideo?.id)
            assertEquals(42_000L, VideoPlaybackController.state.value.currentPositionMs)
            assertEquals(originalPlayerId, VideoPlaybackController.state.value.playerInstanceId)

            // Tap orientation button again -> switches back to Portrait without resetting video
            composeTestRule.onNodeWithTag("embedded_video_orientation_button").performClick()
            composeTestRule.waitForIdle()

            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                activity.requestedOrientation
            )
            assertFalse(landscapeState)
            assertEquals(701L, VideoPlaybackController.state.value.currentVideo?.id)
            assertEquals(42_000L, VideoPlaybackController.state.value.currentPositionMs)

            // Video Lock compatibility: locking hides orientation buttons and prevents orientation changes
            composeTestRule.onNodeWithTag("video_lock_button").performClick()
            composeTestRule.waitForIdle()
            assertTrue(composeTestRule.onAllNodesWithTag("embedded_video_orientation_button").fetchSemanticsNodes().isEmpty())
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)

            // Unlock -> switch to Fullscreen -> toggle orientation in Fullscreen -> leave VideoScreen
            composeTestRule.onNodeWithTag("video_unlock_button").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("embedded_video_fullscreen_button").performClick()
            composeTestRule.waitForIdle()
            assertTrue(fullscreenState)
            composeTestRule.onNodeWithTag("fullscreen_video_orientation_button").assertIsDisplayed()

            // Switch Fullscreen from Landscape -> Portrait -> Landscape
            composeTestRule.onNodeWithTag("fullscreen_video_orientation_button").performClick()
            composeTestRule.waitForIdle()
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)

            composeTestRule.onNodeWithTag("fullscreen_video_orientation_button").performClick()
            composeTestRule.waitForIdle()
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
            assertEquals(originalPlayerId, VideoPlaybackController.state.value.playerInstanceId)

            // Leaving VideoScreen restores initial orientation so the app is not stuck in landscape
            showVideoScreen = false
            composeTestRule.waitForIdle()
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
            assertFalse(landscapeState)
            assertFalse(fullscreenState)
        } finally {
            VideoPlaybackController.detachPlayer()
            videoPlayer.release()
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w915dp-h412dp-land")
    fun videoOrientation_landscapeViewportRendersVideoSurfaceAndAccessibleControlsCleanly() {
        val appContext = RuntimeEnvironment.getApplication()
        val videoPlayer = ExoPlayer.Builder(appContext).build()
        try {
            VideoPlaybackController.attachPlayer(videoPlayer)
            VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 15_000L)

            composeTestRule.setContent {
                JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                    CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                        VideoScreen(
                            videos = testVideos,
                            isFullscreen = false,
                            isPlayerExpanded = true,
                            language = AppLanguage.ENGLISH
                        )
                    }
                }
            }
            composeTestRule.waitForIdle()

            // Verify video stage, surface, lock, orientation, fullscreen, and transport controls are usable in landscape
            composeTestRule.onNodeWithTag("embedded_video_stage_box").assertIsDisplayed()
            composeTestRule.onNodeWithTag("embedded_video_surface").assertIsDisplayed()
            composeTestRule.onNodeWithTag("embedded_video_orientation_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("embedded_video_fullscreen_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("video_play_pause").assertIsDisplayed()
        } finally {
            VideoPlaybackController.detachPlayer()
            videoPlayer.release()
        }
    }
}
