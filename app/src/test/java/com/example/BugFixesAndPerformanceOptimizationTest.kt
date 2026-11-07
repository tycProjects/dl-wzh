package com.example

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.MediaSearchMatcher
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ThemeColor
import com.example.playback.EqualizerManager
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.VideoPlaybackController
import com.example.playback.VideoPlaybackMode
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.screens.VideoScreen
import com.example.ui.screens.VideoThumbnailLoader
import com.example.ui.theme.JnmfTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BugFixesAndPerformanceOptimizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSongs = listOf(
        Song(
            id = 901L,
            title = "JNMF_Fullbeat_Anthem",
            artist = "DJ Alpha",
            album = "Club Session",
            durationMs = 180_000L,
            sizeBytes = 5_242_880L,
            dateAdded = 1_700_000_001L,
            contentUri = Uri.parse("content://media/external/audio/media/901"),
            fileName = "JNMF_Fullbeat_Anthem_2026.mp3"
        ),
        Song(
            id = 902L,
            title = "Santuy Midnight Slow",
            artist = "DJ Beta",
            album = "Late Drive",
            durationMs = 210_000L,
            sizeBytes = 6_291_456L,
            dateAdded = 1_700_000_002L,
            contentUri = Uri.parse("content://media/external/audio/media/902"),
            fileName = "Santuy_Midnight_Slow.mp3"
        )
    )

    private val testVideos = listOf(
        VideoItem(
            id = 951L,
            title = "Live Stage Mix",
            durationMs = 120_000L,
            sizeBytes = 15_728_640L,
            dateAdded = 1_700_000_101L,
            contentUri = Uri.parse("content://media/external/video/media/951"),
            fileName = "Live_Stage_Mix_HD.mp4"
        ),
        VideoItem(
            id = 952L,
            title = "Afterparty Visuals",
            durationMs = 95_000L,
            sizeBytes = 11_534_336L,
            dateAdded = 1_700_000_102L,
            contentUri = Uri.parse("content://media/external/video/media/952"),
            fileName = "Afterparty_Visuals.mp4"
        )
    )

    @Before
    fun setUp() {
        MusicController.resetInMemoryStateForTesting()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        VideoThumbnailLoader.clearCache()
    }

    @After
    fun tearDown() {
        MusicController.resetInMemoryStateForTesting()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        VideoThumbnailLoader.clearCache()
    }

    @Test
    fun equalizer_attachAudioSession_preservesCustomBandsAndUserStrengthsAcrossDjDeckHandoff() {
        val eqManager = EqualizerManager()
        try {
            eqManager.setEnabled(true)
            eqManager.attachAudioSession(101)

            // User customizes EQ bands (switches preset to "Custom") and tweaks Bass/Virtualizer
            eqManager.setBandLevel(0, 850)
            eqManager.setBandLevel(2, -300)
            eqManager.setBandLevel(4, 700)
            eqManager.setBassStrength(780)
            eqManager.setVirtualizerStrength(640)

            assertEquals("Custom", eqManager.uiState.value.currentPreset)
            assertEquals(850.toShort(), eqManager.uiState.value.bands[0].currentLevelMilliBel)
            assertEquals((-300).toShort(), eqManager.uiState.value.bands[2].currentLevelMilliBel)
            assertEquals(700.toShort(), eqManager.uiState.value.bands[4].currentLevelMilliBel)

            // Simulate DJ Crossfade handoff from Deck A (session 101) to Deck B (session 202)
            eqManager.attachAudioSession(202)

            val stateAfterHandoff = eqManager.uiState.value
            assertEquals("Custom", stateAfterHandoff.currentPreset)
            assertEquals(850.toShort(), stateAfterHandoff.bands[0].currentLevelMilliBel)
            assertEquals((-300).toShort(), stateAfterHandoff.bands[2].currentLevelMilliBel)
            assertEquals(700.toShort(), stateAfterHandoff.bands[4].currentLevelMilliBel)
            assertEquals(780, stateAfterHandoff.bassStrength)
            assertEquals(640, stateAfterHandoff.virtualizerStrength)
        } finally {
            eqManager.releaseEffects()
        }
    }

    @Test
    fun djMode_changingTransitionDurationWhileEnabled_updatesImmediatelyAndPersistsAsyncSnapshot() {
        val context = RuntimeEnvironment.getApplication()
        MusicController.bindAppContext(context)

        val deckA = ExoPlayer.Builder(context).build()
        val deckB = ExoPlayer.Builder(context).build()
        try {
            MusicController.attachPlayer(deckA, secondaryPlayerFactory = { deckB })
            MusicController.playSong(testSongs[0], testSongs)

            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 3)
            assertTrue(MusicController.state.value.djModeEnabled)
            assertEquals(3, MusicController.state.value.djTransitionSeconds)

            // Switch DJ transition duration from 3s -> 10s while DJ Mode is already ON
            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 10)
            assertTrue(MusicController.state.value.djModeEnabled)
            assertEquals(10, MusicController.state.value.djTransitionSeconds)
            assertEquals(901L, MusicController.state.value.currentSong?.id)

            // Verify async SharedPreferences snapshot updated in-memory immediately without blocking
            MusicController.seekTo(45_000L)
            MusicController.pause()
            val savedSnapshot = SettingsRepository.getSavedMusicSessionSnapshot(context)
            assertNotNull(savedSnapshot)
            assertEquals(901L, savedSnapshot?.songId)
            assertEquals(45_000L, savedSnapshot?.positionMs)
        } finally {
            MusicController.detachPlayer()
            deckA.release()
            deckB.release()
        }
    }

    @Test
    fun playbackService_deduplicatesNotificationPostsOnPositionTicks_andRoutesVideoSetPlayWhenReady() {
        val context = RuntimeEnvironment.getApplication()
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()
        var media3Controller: MediaController? = null

        try {
            val session = service.currentMediaSession
            assertNotNull("PlaybackService must expose active MediaSession", session)

            val future = MediaController.Builder(context, session!!.token).buildAsync()
            val deadline = System.currentTimeMillis() + 5_000L
            while (!future.isDone && System.currentTimeMillis() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10L)
            }
            assertTrue("MediaController must connect to PlaybackService", future.isDone)
            media3Controller = future.get()

            // 1. Start music playback and settle initial notification post
            MusicController.playSong(testSongs[0], testSongs)
            shadowOf(Looper.getMainLooper()).idle()
            val baselinePosts = service.notificationPostCountForTesting
            assertTrue("Initial active playback must post notification", baselinePosts >= 1)

            // Simulate repeated progress position updates where title/isPlaying/mode/art do not change
            MusicController.seekTo(10_000L)
            shadowOf(Looper.getMainLooper()).idle()
            MusicController.seekTo(20_000L)
            shadowOf(Looper.getMainLooper()).idle()
            MusicController.seekTo(30_000L)
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(
                "Pure position ticks must NOT re-post identical notification via Binder IPC",
                baselinePosts,
                service.notificationPostCountForTesting
            )

            // Flipping Play -> Pause MUST post updated notification
            MusicController.pause()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(baselinePosts + 1, service.notificationPostCountForTesting)

            // 2. Switch ownership to Video and verify MediaSession setPlayWhenReady routes through VideoPlaybackController
            VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 5_000L)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(VideoPlaybackController.state.value.isPlaying)

            media3Controller.playWhenReady = false
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(
                "MediaSession setPlayWhenReady(false) must update VideoPlaybackController.state.isPlaying",
                VideoPlaybackController.state.value.isPlaying
            )

            media3Controller.playWhenReady = true
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(
                "MediaSession setPlayWhenReady(true) must resume VideoPlaybackController.state.isPlaying",
                VideoPlaybackController.state.value.isPlaying
            )
        } finally {
            media3Controller?.release()
            shadowOf(Looper.getMainLooper()).idle()
            serviceController.destroy()
        }
    }

    @Test
    fun videoScreen_returningFromTabNavigation_rendersVideoSurfaceWithoutAudioOnlyFlash_andFullscreenExitResetsOrientation() {
        val context = RuntimeEnvironment.getApplication()
        val videoPlayer = ExoPlayer.Builder(context).build()
        try {
            VideoPlaybackController.attachPlayer(videoPlayer)
            VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
            VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 12_000L)

            var hostActivity: Activity? = null
            var onVideosTab by mutableStateOf(true)
            var isFullscreen by mutableStateOf(false)

            composeTestRule.setContent {
                hostActivity = LocalContext.current as? Activity
                JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                    CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                        if (onVideosTab) {
                            VideoScreen(
                                videos = testVideos,
                                isFullscreen = isFullscreen,
                                onFullscreenChanged = { isFullscreen = it },
                                isPlayerExpanded = true,
                                language = AppLanguage.ENGLISH
                            )
                        }
                    }
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("embedded_video_surface").assertIsDisplayed()

            // Navigate away from Videos tab (e.g. to Music tab)
            onVideosTab = false
            composeTestRule.waitForIdle()
            assertFalse(VideoPlaybackController.state.value.isPlayerUiVisible)
            assertTrue(VideoPlaybackController.state.value.isVideoTrackDisabled)

            // Return to Videos tab: surface must mount immediately without flashing VideoAudioOnlyStage
            onVideosTab = true
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("embedded_video_surface").assertIsDisplayed()
            assertTrue(composeTestRule.onAllNodesWithTag("video_audio_only_stage").fetchSemanticsNodes().isEmpty())
            assertTrue(VideoPlaybackController.state.value.isPlayerUiVisible)
            assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)

            // Enter Fullscreen -> rotate orientation -> exit Fullscreen back to embedded player
            composeTestRule.onNodeWithTag("embedded_video_fullscreen_button").performClick()
            composeTestRule.waitForIdle()
            assertTrue(isFullscreen)
            composeTestRule.onNodeWithTag("fullscreen_video_surface").assertIsDisplayed()

            composeTestRule.onNodeWithTag("fullscreen_video_orientation_button").performClick()
            composeTestRule.waitForIdle()
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, hostActivity?.requestedOrientation)

            composeTestRule.onNodeWithTag("exit_fullscreen_top_button").performClick()
            composeTestRule.waitForIdle()
            assertFalse(isFullscreen)
            composeTestRule.onNodeWithTag("embedded_video_surface").assertIsDisplayed()
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, hostActivity?.requestedOrientation)
        } finally {
            VideoPlaybackController.detachPlayer()
            videoPlayer.release()
        }
    }

    @Test
    fun searchIndex_andThumbnailNegativeCache_preserveNormalizedMatchingAndPreventRepeatedFailedDecodes() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val song = testSongs[0]
        val video = testVideos[0]

        val queries = listOf(
            "jnmf fullbeat",
            "JNMF_FULLBEAT_ANTHEM",
            "  fullbeat___anthem_2026 ",
            "dj_alpha",
            "club   session",
            "anthem alpha" // Cross-field query must NOT match either MediaSearchMatcher or normalizedSearchIndex
        )
        for (q in queries) {
            val expected = MediaSearchMatcher.matchesSong(
                query = q,
                title = song.title,
                fileName = song.fileName,
                artist = song.artist,
                album = song.album
            )
            val normQ = MediaSearchMatcher.normalizeForSearch(q)
            val actual = normQ.isEmpty() || song.normalizedSearchIndex.contains(normQ)
            assertEquals("Mismatch for song query '$q'", expected, actual)
        }

        val videoQueries = listOf("live_stage", "STAGE   MIX   HD", "nonexistent_clip")
        for (q in videoQueries) {
            val expected = MediaSearchMatcher.matchesVideo(q, video.title, video.fileName)
            val normQ = MediaSearchMatcher.normalizeForSearch(q)
            val actual = normQ.isEmpty() || video.normalizedSearchIndex.contains(normQ)
            assertEquals("Mismatch for video query '$q'", expected, actual)
        }

        // Verify unreadable video URI returns null cleanly and negative cache clears when seeded/reset
        val missingVideo = VideoItem(
            id = 999_999L,
            title = "Missing Clip",
            durationMs = 10_000L,
            sizeBytes = 1024L,
            dateAdded = 1L,
            contentUri = Uri.parse("file:///nonexistent_jnmf_test_dir/missing_video.mp4"),
            fileName = "missing_video.mp4"
        )
        assertNull(VideoThumbnailLoader.loadThumbnail(context, missingVideo))
        assertNull(VideoThumbnailLoader.loadThumbnail(context, missingVideo))

        val seededBitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        VideoThumbnailLoader.putCachedForTest(missingVideo.id, missingVideo.contentUri, seededBitmap)
        assertNotNull(VideoThumbnailLoader.loadThumbnail(context, missingVideo))
    }
}
