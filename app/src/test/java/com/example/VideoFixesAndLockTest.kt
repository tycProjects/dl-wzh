package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.ThemeColor
import com.example.playback.MusicController
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
class VideoFixesAndLockTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testVideos = (1..12).map { idx ->
        VideoItem(
            id = 500L + idx,
            title = "JNMF Club Video Clip #$idx",
            durationMs = (120_000L + idx * 15_000L),
            sizeBytes = (20L + idx) * 1024L * 1024L,
            dateAdded = 1_000L * idx,
            contentUri = Uri.parse("content://media/external/video/media/${500 + idx}")
        )
    }

    @Before
    fun setUp() {
        VideoThumbnailLoader.clearCache()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
        VideoPlaybackController.setLanguage(AppLanguage.ENGLISH)
        MusicController.setLanguage(AppLanguage.ENGLISH)
        MusicController.pause()
    }

    @After
    fun tearDown() {
        VideoThumbnailLoader.clearCache()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
    }

    @Test
    fun issue1_videoLibraryShowsActualThumbnailsPlaceholderFallbackAndScrollsCleanly() {
        val validThumb1 = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888)
        val validThumb10 = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888)

        // Pre-populate actual decoded frames for video #1 and #10; leave #2 unresolvable to test placeholder fallback
        VideoThumbnailLoader.putCachedForTest(testVideos[0].id, testVideos[0].contentUri, validThumb1)
        VideoThumbnailLoader.putCachedForTest(testVideos[9].id, testVideos[9].contentUri, validThumb10)

        // Verify unreadable/non-existent URI returns null cleanly without crashing or OOM
        val context = RuntimeEnvironment.getApplication()
        val failedLoad = runBlocking {
            VideoThumbnailLoader.loadThumbnail(context, testVideos[1])
        }
        assertNull("Unreadable content URI must return null cleanly for placeholder fallback", failedLoad)

        composeTestRule.setContent {
            JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                    VideoScreen(
                        videos = testVideos,
                        isPlayerExpanded = false,
                        language = AppLanguage.ENGLISH
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Video #1 displays actual thumbnail bitmap + title + duration/size metadata
        composeTestRule.onNodeWithTag("video_thumbnail_image_501", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("video_thumbnail_placeholder_501", useUnmergedTree = true)
            .assertCountEquals(0)
        composeTestRule.onNodeWithText("JNMF Club Video Clip #1").assertIsDisplayed()
        composeTestRule.onNodeWithText("02:15 • 21 MB").assertIsDisplayed()

        // Video #2 (failed thumbnail load) displays clean placeholder instead of broken/blank area + metadata
        composeTestRule.onNodeWithTag("video_thumbnail_placeholder_502", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("video_thumbnail_image_502", useUnmergedTree = true)
            .assertCountEquals(0)
        composeTestRule.onNodeWithText("JNMF Club Video Clip #2").assertIsDisplayed()

        // Scroll library down to Video #10 and verify thumbnail & metadata render while scrolling
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(9)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("video_thumbnail_image_510", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("JNMF Club Video Clip #10").assertIsDisplayed()
    }

    @Test
    fun issue2_playerViewSwitchTargetViewHandoff_oldPlayerViewReleaseNeverClearsActiveSurface() {
        val context = RuntimeEnvironment.getApplication()
        val exoPlayer = ExoPlayer.Builder(context).build()
        try {
            VideoPlaybackController.attachPlayer(exoPlayer)
            VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
            VideoPlaybackController.setPlayerUiVisible(true)
            VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 33_000L)

            val embeddedView = PlayerView(context)
            val fullscreenView = PlayerView(context)

            // 1. Embedded PlayerView attaches initially
            VideoPlaybackController.attachVideoPlayerView(embeddedView)
            assertSame(exoPlayer, embeddedView.player)
            assertTrue(VideoPlaybackController.state.value.isVideoSurfaceAttached)
            assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)

            // 2. Enter Fullscreen: new fullscreenView attaches BEFORE old embeddedView disposes
            VideoPlaybackController.attachVideoPlayerView(fullscreenView)
            // Old embeddedView's onRelease fires afterwards
            VideoPlaybackController.detachVideoPlayerView(embeddedView)

            assertNull("Outgoing embedded PlayerView must be unbound", embeddedView.player)
            assertSame("Fullscreen PlayerView must remain bound to ExoPlayer", exoPlayer, fullscreenView.player)
            assertTrue(
                "Surface must remain attached after outgoing embedded view disposes",
                VideoPlaybackController.state.value.isVideoSurfaceAttached
            )
            assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)
            assertEquals(33_000L, VideoPlaybackController.state.value.currentPositionMs)

            // 3. Exit Fullscreen back to Embedded: new embeddedView2 attaches BEFORE fullscreenView disposes
            VideoPlaybackController.seekTo(54_250L)
            val restoredEmbeddedView = PlayerView(context)
            VideoPlaybackController.attachVideoPlayerView(restoredEmbeddedView)
            VideoPlaybackController.detachVideoPlayerView(fullscreenView)

            assertNull("Outgoing fullscreen PlayerView must be unbound", fullscreenView.player)
            assertSame(
                "Restored embedded PlayerView must remain bound immediately without toggling Audio Only",
                exoPlayer,
                restoredEmbeddedView.player
            )
            assertTrue(VideoPlaybackController.state.value.isVideoSurfaceAttached)
            assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            assertEquals(54_250L, VideoPlaybackController.state.value.currentPositionMs)

            // Verify anamorphic/non-square pixel aspect ratio calculation
            val anamorphicSize = VideoSize(1440, 1080, 1.3333334f)
            assertEquals(16f / 9f, (anamorphicSize.width * anamorphicSize.pixelWidthHeightRatio) / anamorphicSize.height, 0.01f)
        } finally {
            VideoPlaybackController.detachPlayer()
            exoPlayer.release()
        }
    }

    @Test
    fun issue2_composeFullscreenEnterAndExit_preservesSurfaceBindingPositionAndPlaybackWithoutAudioOnlyToggle() {
        val context = RuntimeEnvironment.getApplication()
        val exoPlayer = ExoPlayer.Builder(context).build()
        val lifecycleOwner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
            override val lifecycle: Lifecycle get() = registry
        }

        try {
            VideoPlaybackController.attachPlayer(exoPlayer)
            var isExpanded by mutableStateOf(false)
            var isFullscreen by mutableStateOf(false)

            composeTestRule.setContent {
                JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                    CompositionLocalProvider(
                        LocalAppLanguage provides AppLanguage.ENGLISH,
                        LocalLifecycleOwner provides lifecycleOwner
                    ) {
                        VideoScreen(
                            videos = testVideos.take(3),
                            isFullscreen = isFullscreen,
                            onFullscreenChanged = { isFullscreen = it },
                            isPlayerExpanded = isExpanded,
                            onPlayerExpandedChanged = { isExpanded = it },
                            language = AppLanguage.ENGLISH
                        )
                    }
                }
            }

            // 1. Open video from library
            composeTestRule.onNodeWithTag("video_item_501").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.runOnIdle {
                VideoPlaybackController.seekTo(41_000L)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("embedded_video_surface").assertIsDisplayed()
            assertTrue(VideoPlaybackController.state.value.isVideoSurfaceAttached)
            assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)
            assertFalse(exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO))

            // 2. Enter Fullscreen via button
            composeTestRule.onNodeWithTag("embedded_video_fullscreen_button").performClick()
            composeTestRule.waitForIdle()

            assertTrue(isFullscreen)
            composeTestRule.onNodeWithTag("fullscreen_video_surface").assertIsDisplayed()
            composeTestRule.onAllNodesWithTag("embedded_video_surface").assertCountEquals(0)
            assertTrue(
                "Fullscreen surface must remain attached after embedded surface disposes",
                VideoPlaybackController.state.value.isVideoSurfaceAttached
            )
            assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            assertEquals(41_000L, VideoPlaybackController.state.value.currentPositionMs)

            // Advance position while in fullscreen, then exit fullscreen back to normal embedded view
            composeTestRule.runOnIdle {
                VideoPlaybackController.seekTo(67_800L)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("toggle_fullscreen_exit").performClick()
            composeTestRule.waitForIdle()

            assertFalse(isFullscreen)
            composeTestRule.onNodeWithTag("embedded_video_surface").assertIsDisplayed()
            composeTestRule.onAllNodesWithTag("fullscreen_video_surface").assertCountEquals(0)
            assertEquals(VideoPlaybackMode.NORMAL_VIDEO, VideoPlaybackController.state.value.playbackMode)
            assertTrue(
                "Embedded surface must be immediately attached upon exiting fullscreen without toggling Audio Only",
                VideoPlaybackController.state.value.isVideoSurfaceAttached
            )
            assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            assertEquals(67_800L, VideoPlaybackController.state.value.currentPositionMs)
        } finally {
            VideoPlaybackController.detachPlayer()
            exoPlayer.release()
        }
    }

    @Test
    fun issue3_videoScreenLockAndUnlock_disablesAccidentalTouchesRevealsUnlockWhenHiddenAndPreservesPlayback() {
        val context = RuntimeEnvironment.getApplication()
        val exoPlayer = ExoPlayer.Builder(context).build()
        val lifecycleOwner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
            override val lifecycle: Lifecycle get() = registry
        }

        try {
            VideoPlaybackController.attachPlayer(exoPlayer)
            var isExpanded by mutableStateOf(false)
            var isFullscreen by mutableStateOf(false)

            composeTestRule.setContent {
                JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                    CompositionLocalProvider(
                        LocalAppLanguage provides AppLanguage.ENGLISH,
                        LocalLifecycleOwner provides lifecycleOwner
                    ) {
                        VideoScreen(
                            videos = testVideos.take(3),
                            isFullscreen = isFullscreen,
                            onFullscreenChanged = { isFullscreen = it },
                            isPlayerExpanded = isExpanded,
                            onPlayerExpandedChanged = { isExpanded = it },
                            language = AppLanguage.ENGLISH
                        )
                    }
                }
            }

            // Start Video #1 and seek to 29s
            composeTestRule.onNodeWithTag("video_item_501").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.runOnIdle {
                VideoPlaybackController.seekTo(29_000L)
            }
            composeTestRule.waitForIdle()

            // Verify Lock button is visible in Embedded mode
            composeTestRule.onNodeWithTag("video_lock_button").assertIsDisplayed()

            // 1. Lock controls in Embedded mode (use manual clock to test auto-hide + tap-to-reveal)
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.onNodeWithTag("video_lock_button").performClick()
            composeTestRule.mainClock.advanceTimeBy(400)

            // Locked overlay & Unlock button are visible; interactive controls are absent/disabled
            composeTestRule.onNodeWithTag("video_locked_badge").assertIsDisplayed()
            composeTestRule.onNodeWithTag("video_unlock_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("embedded_locked_info_panel").assertIsDisplayed()
            composeTestRule.onAllNodesWithTag("video_play_pause").assertCountEquals(0)
            composeTestRule.onAllNodesWithTag("video_next_button").assertCountEquals(0)
            composeTestRule.onAllNodesWithTag("video_seek_slider").assertCountEquals(0)
            composeTestRule.onAllNodesWithTag("embedded_video_close").assertCountEquals(0)
            composeTestRule.onAllNodesWithTag("btn_enter_fullscreen").assertCountEquals(0)
            composeTestRule.onAllNodesWithTag("video_item_502").assertCountEquals(0)

            // Video keeps playing normally at preserved position
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            assertEquals(29_000L, VideoPlaybackController.state.value.currentPositionMs)
            assertEquals(501L, VideoPlaybackController.state.value.currentVideo?.id)

            // Advance past 3500ms auto-hide timeout -> Unlock overlay hides cleanly so video view is unobstructed
            composeTestRule.mainClock.advanceTimeBy(4000)
            composeTestRule.onAllNodesWithTag("video_unlock_button").assertCountEquals(0)
            assertTrue(VideoPlaybackController.state.value.isPlaying)

            // Tap locked video stage while regular controls & unlock overlay are hidden -> Unlock button reliably reappears!
            composeTestRule.onNodeWithTag("embedded_video_stage_box").performClick()
            composeTestRule.mainClock.advanceTimeBy(400)
            composeTestRule.onNodeWithTag("video_unlock_button").assertIsDisplayed()

            // Unlock controls -> restores normal controls without restarting or resetting position
            composeTestRule.onNodeWithTag("video_unlock_button").performClick()
            composeTestRule.mainClock.autoAdvance = true
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("video_play_pause").assertIsDisplayed()
            composeTestRule.onNodeWithTag("video_lock_button").assertIsDisplayed()
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            assertEquals(29_000L, VideoPlaybackController.state.value.currentPositionMs)
            assertEquals(501L, VideoPlaybackController.state.value.currentVideo?.id)

            // 2. Enter Fullscreen and test Lock/Unlock in Fullscreen mode
            composeTestRule.onNodeWithTag("embedded_video_fullscreen_button").performClick()
            composeTestRule.waitForIdle()
            assertTrue(isFullscreen)
            composeTestRule.onNodeWithTag("video_lock_button").assertIsDisplayed()

            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.onNodeWithTag("video_lock_button").performClick()
            composeTestRule.mainClock.advanceTimeBy(400)

            // All fullscreen playback/navigation controls are hidden while locked
            composeTestRule.onNodeWithTag("video_unlock_button").assertIsDisplayed()
            composeTestRule.onAllNodesWithTag("fullscreen_video_play_pause").assertCountEquals(0)
            composeTestRule.onAllNodesWithTag("fullscreen_video_seek_slider").assertCountEquals(0)
            composeTestRule.onAllNodesWithTag("toggle_fullscreen_exit").assertCountEquals(0)
            composeTestRule.onAllNodesWithTag("exit_fullscreen_top_button").assertCountEquals(0)
            composeTestRule.onNodeWithTag("fullscreen_video_surface").assertIsDisplayed()

            // Auto-hide in Fullscreen after 3.5s, then tap fullscreen container to reveal Unlock button
            composeTestRule.mainClock.advanceTimeBy(4000)
            composeTestRule.onAllNodesWithTag("video_unlock_button").assertCountEquals(0)

            composeTestRule.onNodeWithTag("fullscreen_player_container").performClick()
            composeTestRule.mainClock.advanceTimeBy(400)
            composeTestRule.onNodeWithTag("video_unlock_button").assertIsDisplayed()

            // Unlock in Fullscreen -> restores controls immediately at exact position & playing state
            composeTestRule.onNodeWithTag("video_unlock_button").performClick()
            composeTestRule.mainClock.autoAdvance = true
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("fullscreen_video_play_pause").assertIsDisplayed()
            composeTestRule.onNodeWithTag("toggle_fullscreen_exit").assertIsDisplayed()
            assertTrue(isFullscreen)
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            assertEquals(29_000L, VideoPlaybackController.state.value.currentPositionMs)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
            VideoPlaybackController.detachPlayer()
            exoPlayer.release()
        }
    }

    @Test
    fun regressionCheck_audioOnlyModeAndBackgroundMusicPlaybackRemainFunctional() {
        val song = Song(
            id = 888L,
            title = "DJ Indo Remix Regression Check",
            artist = "DJ Jussar",
            album = "JNMF",
            durationMs = 200_000L,
            sizeBytes = 6_000_000L,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/external/audio/media/888")
        )

        MusicController.playSong(song, listOf(song))
        assertTrue(MusicController.state.value.isPlaying)
        assertNull(MusicController.state.value.errorCode)

        MusicController.togglePlayPause()
        assertFalse(MusicController.state.value.isPlaying)
        assertNull(MusicController.state.value.errorCode)

        MusicController.togglePlayPause()
        assertTrue(MusicController.state.value.isPlaying)

        // Switching to video in Audio Only mode pauses music and keeps video frames disabled
        VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.AUDIO_ONLY)
        VideoPlaybackController.setPlayerUiVisible(true)
        VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 18_500L)

        assertFalse(MusicController.state.value.isPlaying)
        assertTrue(VideoPlaybackController.state.value.isPlaying)
        assertEquals(VideoPlaybackMode.AUDIO_ONLY, VideoPlaybackController.state.value.playbackMode)
        assertFalse(VideoPlaybackController.state.value.shouldRenderVideoFrames)
        assertTrue(VideoPlaybackController.state.value.isVideoTrackDisabled)
        assertEquals(18_500L, VideoPlaybackController.state.value.currentPositionMs)
    }
}
