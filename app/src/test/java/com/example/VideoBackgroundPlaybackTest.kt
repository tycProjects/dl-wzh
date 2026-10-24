package com.example

import android.app.Notification
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ThemeColor
import com.example.playback.ActiveMediaSessionOwner
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.VideoPlaybackController
import com.example.playback.VideoPlaybackMode
import com.example.ui.components.VideoMiniPlayer
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.screens.VideoScreen
import com.example.ui.theme.JnmfTheme
import kotlinx.coroutines.flow.first
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
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class VideoBackgroundPlaybackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleVideos = listOf(
        VideoItem(
            id = 101L,
            title = "JNMF Live Fullbeat Stage Vol 1",
            durationMs = 240_000L,
            sizeBytes = 35L * 1024L * 1024L,
            dateAdded = 1_000L,
            contentUri = Uri.parse("content://media/external/video/media/101")
        ),
        VideoItem(
            id = 102L,
            title = "DJ Slow Indo Remix Visual Session",
            durationMs = 195_000L,
            sizeBytes = 28L * 1024L * 1024L,
            dateAdded = 2_000L,
            contentUri = Uri.parse("content://media/external/video/media/102")
        ),
        VideoItem(
            id = 103L,
            title = "Bass Horeg Night Festival Clip",
            durationMs = 300_000L,
            sizeBytes = 50L * 1024L * 1024L,
            dateAdded = 3_000L,
            contentUri = Uri.parse("content://media/external/video/media/103")
        )
    )

    @Before
    fun setUp() {
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
        VideoPlaybackController.setLanguage(AppLanguage.ENGLISH)
        MusicController.setLanguage(AppLanguage.ENGLISH)
        MusicController.pause()
    }

    @After
    fun tearDown() {
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
    }

    @Test
    fun normalVideoMode_foregroundBackgroundLockScreenAndReturn_disablesAndRestoresVideoTrack() {
        val context = RuntimeEnvironment.getApplication()
        val exoPlayer = ExoPlayer.Builder(context).build()
        try {
            VideoPlaybackController.attachPlayer(exoPlayer)
            VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
            VideoPlaybackController.setPlayerUiVisible(true)

            // 1. Start in foreground Normal Video mode
            VideoPlaybackController.playVideo(sampleVideos[0], sampleVideos, startPositionMs = 42_000L)
            VideoPlaybackController.notifyVideoSurfaceAttached(true)

            val fgState = VideoPlaybackController.state.value
            assertEquals(VideoPlaybackMode.NORMAL_VIDEO, fgState.playbackMode)
            assertTrue(fgState.isPlaying)
            assertTrue(fgState.shouldRenderVideoFrames)
            assertFalse(fgState.isVideoTrackDisabled)
            assertTrue(fgState.isVideoSurfaceAttached)
            assertFalse(
                exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
            )
            assertEquals(42_000L, fgState.currentPositionMs)

            // 2. Simulate leaving screen / backgrounding app / locking screen
            VideoPlaybackController.seekTo(68_500L)
            VideoPlaybackController.setPlayerUiVisible(false)

            val bgState = VideoPlaybackController.state.value
            assertEquals(VideoPlaybackMode.NORMAL_VIDEO, bgState.playbackMode)
            assertTrue("Audio should continue playing in background", bgState.isPlaying)
            assertFalse("Video frames must not render in background", bgState.shouldRenderVideoFrames)
            assertTrue("Video track must be disabled in background", bgState.isVideoTrackDisabled)
            assertFalse("Video surface must be detached in background", bgState.isVideoSurfaceAttached)
            assertTrue(
                exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
            )
            assertEquals(68_500L, bgState.currentPositionMs)

            // 3. Return to foreground video player: mode and position preserved, video track re-enabled
            VideoPlaybackController.setPlayerUiVisible(true)
            val restoredState = VideoPlaybackController.state.value
            assertEquals(VideoPlaybackMode.NORMAL_VIDEO, restoredState.playbackMode)
            assertTrue(restoredState.isPlaying)
            assertTrue(restoredState.shouldRenderVideoFrames)
            assertFalse(restoredState.isVideoTrackDisabled)
            assertFalse(
                exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
            )
            assertEquals(68_500L, restoredState.currentPositionMs)
            assertEquals(101L, restoredState.currentVideo?.id)
        } finally {
            VideoPlaybackController.detachPlayer()
            exoPlayer.release()
        }
    }

    @Test
    fun audioOnlyMode_neverRendersVideoFramesInForegroundOrBackgroundAndPreservesPosition() {
        val context = RuntimeEnvironment.getApplication()
        val exoPlayer = ExoPlayer.Builder(context).build()
        try {
            VideoPlaybackController.attachPlayer(exoPlayer)
            VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.AUDIO_ONLY)
            VideoPlaybackController.setPlayerUiVisible(true)

            VideoPlaybackController.playVideo(sampleVideos[1], sampleVideos, startPositionMs = 55_000L)
            // Even if a surface attempts to attach, Audio Only rejects frame rendering
            VideoPlaybackController.notifyVideoSurfaceAttached(true)

            val fgAudioOnly = VideoPlaybackController.state.value
            assertEquals(VideoPlaybackMode.AUDIO_ONLY, fgAudioOnly.playbackMode)
            assertTrue(fgAudioOnly.isPlaying)
            assertFalse(fgAudioOnly.shouldRenderVideoFrames)
            assertTrue(fgAudioOnly.isVideoTrackDisabled)
            assertFalse(fgAudioOnly.isVideoSurfaceAttached)
            assertTrue(
                exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
            )
            assertEquals(55_000L, fgAudioOnly.currentPositionMs)

            // Simulate app switch / screen lock
            VideoPlaybackController.seekTo(91_200L)
            VideoPlaybackController.setPlayerUiVisible(false)

            val lockedState = VideoPlaybackController.state.value
            assertTrue(lockedState.isPlaying)
            assertEquals(VideoPlaybackMode.AUDIO_ONLY, lockedState.playbackMode)
            assertTrue(lockedState.isVideoTrackDisabled)
            assertEquals(91_200L, lockedState.currentPositionMs)

            // Return to foreground screen
            VideoPlaybackController.setPlayerUiVisible(true)
            val returnedState = VideoPlaybackController.state.value
            assertTrue(returnedState.isPlaying)
            assertEquals(VideoPlaybackMode.AUDIO_ONLY, returnedState.playbackMode)
            assertFalse(returnedState.shouldRenderVideoFrames)
            assertTrue(returnedState.isVideoTrackDisabled)
            assertEquals(91_200L, returnedState.currentPositionMs)
            assertEquals("DJ Slow Indo Remix Visual Session", returnedState.currentVideo?.title)
        } finally {
            VideoPlaybackController.detachPlayer()
            exoPlayer.release()
        }
    }

    @Test
    fun liveModeSwitching_preservesPlaybackPositionAndDoesNotStopAudio() {
        val context = RuntimeEnvironment.getApplication()
        val exoPlayer = ExoPlayer.Builder(context).build()
        try {
            VideoPlaybackController.attachPlayer(exoPlayer)
            VideoPlaybackController.setPlayerUiVisible(true)
            VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
            VideoPlaybackController.playVideo(sampleVideos[0], sampleVideos, startPositionMs = 102_400L)

            assertEquals(102_400L, VideoPlaybackController.state.value.currentPositionMs)
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)

            // Switch Normal Video -> Audio Only mid-playback
            VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.AUDIO_ONLY)
            val afterToAudio = VideoPlaybackController.state.value
            assertEquals(VideoPlaybackMode.AUDIO_ONLY, afterToAudio.playbackMode)
            assertEquals(102_400L, afterToAudio.currentPositionMs)
            assertTrue(afterToAudio.isPlaying)
            assertTrue(afterToAudio.isVideoTrackDisabled)
            assertTrue(
                exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
            )

            // Switch Audio Only -> Normal Video mid-playback
            VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
            val afterToNormal = VideoPlaybackController.state.value
            assertEquals(VideoPlaybackMode.NORMAL_VIDEO, afterToNormal.playbackMode)
            assertEquals(102_400L, afterToNormal.currentPositionMs)
            assertTrue(afterToNormal.isPlaying)
            assertFalse(afterToNormal.isVideoTrackDisabled)
            assertFalse(
                exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
            )
        } finally {
            VideoPlaybackController.detachPlayer()
            exoPlayer.release()
        }
    }

    @Test
    fun playbackServiceNotificationAndMediaSessionControls_syncTitleModeAndActions() {
        val controller = Robolectric.buildService(PlaybackService::class.java).create()
        val service = controller.get()
        try {
            VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.AUDIO_ONLY)
            VideoPlaybackController.playVideo(sampleVideos[0], sampleVideos, startPositionMs = 15_000L)

            assertEquals(ActiveMediaSessionOwner.VIDEO, VideoPlaybackController.activeOwner.value)

            // Verify notification title, mode subtitle, and 4 actions (Previous, Pause, Next, Stop)
            val notif1 = service.buildForegroundNotificationForCurrentSession()
            assertEquals(
                "JNMF Live Fullbeat Stage Vol 1",
                notif1.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            )
            assertEquals(
                "Mode: Audio Only",
                notif1.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            )
            assertEquals(4, notif1.actions.size)
            assertEquals("Previous", notif1.actions[0].title.toString())
            assertEquals("Pause", notif1.actions[1].title.toString())
            assertEquals("Next", notif1.actions[2].title.toString())
            assertEquals("Stop", notif1.actions[3].title.toString())

            // Trigger Next via service intent (simulating notification button tap)
            service.onStartCommand(
                Intent(service, PlaybackService::class.java).apply {
                    action = PlaybackService.ACTION_NEXT
                },
                0,
                1
            )
            assertEquals(102L, VideoPlaybackController.state.value.currentVideo?.id)
            assertEquals(VideoPlaybackMode.AUDIO_ONLY, VideoPlaybackController.state.value.playbackMode)

            val notif2 = service.buildForegroundNotificationForCurrentSession()
            assertEquals(
                "DJ Slow Indo Remix Visual Session",
                notif2.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            )

            // Trigger Pause & Play via service intent
            service.onStartCommand(
                Intent(service, PlaybackService::class.java).apply {
                    action = PlaybackService.ACTION_PAUSE
                },
                0,
                2
            )
            assertFalse(VideoPlaybackController.state.value.isPlaying)
            val pausedNotif = service.buildForegroundNotificationForCurrentSession()
            assertEquals("Play", pausedNotif.actions[1].title.toString())

            service.onStartCommand(
                Intent(service, PlaybackService::class.java).apply {
                    action = PlaybackService.ACTION_PLAY
                },
                0,
                3
            )
            assertTrue(VideoPlaybackController.state.value.isPlaying)

            // Localization check on notification (Bahasa Indonesia & Cebuano)
            MusicController.setLanguage(AppLanguage.INDONESIAN)
            VideoPlaybackController.setLanguage(AppLanguage.INDONESIAN)
            val indoNotif = service.buildForegroundNotificationForCurrentSession()
            assertEquals(
                "Mode: Hanya Audio",
                indoNotif.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            )
            assertEquals("Berhenti", indoNotif.actions[3].title.toString())

            // Trigger Stop via service intent
            service.onStartCommand(
                Intent(service, PlaybackService::class.java).apply {
                    action = PlaybackService.ACTION_STOP
                },
                0,
                4
            )
            assertNull(VideoPlaybackController.state.value.currentVideo)
            assertFalse(VideoPlaybackController.state.value.isPlaying)
            assertEquals(ActiveMediaSessionOwner.MUSIC, VideoPlaybackController.activeOwner.value)
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun audioFocusHeadphoneDisconnectAndMusicHandoff_behaveCleanly() {
        val controller = Robolectric.buildService(PlaybackService::class.java).create()
        val service = controller.get()
        try {
            // 1. Start Music first
            val song = Song(
                id = 77L,
                title = "DJ Malam Pagi Fullbeat",
                artist = "DJ Jussar",
                album = "JNMF Club",
                durationMs = 200_000L,
                sizeBytes = 6_000_000L,
                dateAdded = 1000L,
                contentUri = Uri.parse("content://media/audio/77")
            )
            MusicController.playSong(song, listOf(song))
            assertTrue(MusicController.state.value.isPlaying)
            assertEquals(ActiveMediaSessionOwner.MUSIC, VideoPlaybackController.activeOwner.value)

            // 2. Starting Video pauses Music cleanly without destroying Music queue
            VideoPlaybackController.playVideo(sampleVideos[0], sampleVideos, startPositionMs = 30_000L)
            assertFalse("Music should pause when video starts", MusicController.state.value.isPlaying)
            assertEquals("DJ Malam Pagi Fullbeat", MusicController.state.value.currentSong?.title)
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            assertEquals(ActiveMediaSessionOwner.VIDEO, VideoPlaybackController.activeOwner.value)

            // 3. Headphone disconnect broadcast (ACTION_AUDIO_BECOMING_NOISY) pauses video & preserves position
            RuntimeEnvironment.getApplication().sendBroadcast(
                Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY).setPackage(service.packageName)
            )
            // Also invoke handler directly to verify deterministic pause behavior
            VideoPlaybackController.handleAudioBecomingNoisy()
            assertFalse(VideoPlaybackController.state.value.isPlaying)
            assertEquals(30_000L, VideoPlaybackController.state.value.currentPositionMs)

            // 4. Resume video, then test transient audio focus loss & regain
            VideoPlaybackController.resume()
            assertTrue(VideoPlaybackController.state.value.isPlaying)

            VideoPlaybackController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            assertFalse(VideoPlaybackController.state.value.isPlaying)

            VideoPlaybackController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
            assertTrue(VideoPlaybackController.state.value.isPlaying)

            // Permanent audio focus loss does NOT auto-resume on subsequent gain
            VideoPlaybackController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
            assertFalse(VideoPlaybackController.state.value.isPlaying)
            VideoPlaybackController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
            assertFalse(VideoPlaybackController.state.value.isPlaying)

            // 5. Resuming Music pauses active Video and hands MediaSession back to Music
            VideoPlaybackController.resume()
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            MusicController.resume()
            assertFalse(VideoPlaybackController.state.value.isPlaying)
            assertEquals(ActiveMediaSessionOwner.MUSIC, VideoPlaybackController.activeOwner.value)
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun settingsRepository_persistsVideoPlaybackMode() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)

        repo.setVideoPlaybackMode(VideoPlaybackMode.AUDIO_ONLY)
        assertEquals(VideoPlaybackMode.AUDIO_ONLY, repo.getInitialVideoPlaybackMode())
        assertEquals(VideoPlaybackMode.AUDIO_ONLY, repo.videoPlaybackModeFlow.first())
        assertEquals(VideoPlaybackMode.AUDIO_ONLY, SettingsRepository.getSavedVideoPlaybackMode(context))

        repo.setVideoPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
        assertEquals(VideoPlaybackMode.NORMAL_VIDEO, repo.getInitialVideoPlaybackMode())
        assertEquals(VideoPlaybackMode.NORMAL_VIDEO, repo.videoPlaybackModeFlow.first())
    }

    @Test
    @Config(sdk = [33], qualifiers = "w412dp-h915dp")
    fun videoScreenAndMiniPlayerUi_switchModesNavigateLockLifecycleAndRestoreState() {
        val customLifecycleOwner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
            override val lifecycle: Lifecycle get() = registry
        }

        var isExpanded by mutableStateOf(false)

        composeTestRule.setContent {
            JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                CompositionLocalProvider(
                    LocalAppLanguage provides AppLanguage.ENGLISH,
                    LocalLifecycleOwner provides customLifecycleOwner
                ) {
                    val vState by VideoPlaybackController.state.collectAsState()
                    Column(modifier = Modifier.fillMaxSize()) {
                        VideoScreen(
                            videos = sampleVideos,
                            isPlayerExpanded = isExpanded,
                            onPlayerExpandedChanged = { isExpanded = it },
                            language = AppLanguage.ENGLISH,
                            modifier = Modifier.weight(1f)
                        )
                        if (!isExpanded && vState.currentVideo != null) {
                            VideoMiniPlayer(
                                videoState = vState,
                                onPlayPause = { VideoPlaybackController.togglePlayPause() },
                                onPrevious = { VideoPlaybackController.previous() },
                                onNext = { VideoPlaybackController.next() },
                                onStop = { VideoPlaybackController.stop() },
                                onClick = { isExpanded = true }
                            )
                        }
                    }
                }
            }
        }

        // 1. Tap first video from gallery
        composeTestRule.onNodeWithTag("video_item_101").performClick()
        composeTestRule.waitForIdle()

        assertTrue(isExpanded)
        composeTestRule.onNodeWithTag("video_player_title")
            .assertTextEquals("JNMF Live Fullbeat Stage Vol 1")
        composeTestRule.onNodeWithTag("video_mode_indicator")
            .assertTextEquals("Normal Video")
        composeTestRule.onNodeWithTag("embedded_video_surface").assertIsDisplayed()
        assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)

        // Seek to 75 seconds and switch to Audio Only in UI
        composeTestRule.runOnIdle {
            VideoPlaybackController.seekTo(75_000L)
        }
        composeTestRule.onNodeWithTag("video_mode_audio_only_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify Audio Only stage replaces video surface and disables video frame rendering
        composeTestRule.onNodeWithTag("video_mode_indicator")
            .assertTextEquals("Audio Only")
        composeTestRule.onNodeWithTag("video_audio_only_stage").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("embedded_video_surface").assertCountEquals(0)
        assertTrue(VideoPlaybackController.state.value.isVideoTrackDisabled)
        assertEquals(75_000L, VideoPlaybackController.state.value.currentPositionMs)

        // Advance to Next video via player UI button -> title updates, Audio Only mode preserved
        composeTestRule.onNodeWithTag("video_next_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("video_player_title")
            .assertTextEquals("DJ Slow Indo Remix Visual Session")
        composeTestRule.onNodeWithTag("video_mode_indicator")
            .assertTextEquals("Audio Only")

        // Switch back to Normal Video, seek to 50s, then simulate screen lock (Lifecycle ON_STOP)
        composeTestRule.onNodeWithTag("video_mode_normal_button")
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            VideoPlaybackController.seekTo(50_000L)
        }
        composeTestRule.waitForIdle()
        assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)

        composeTestRule.runOnIdle {
            customLifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        composeTestRule.waitForIdle()
        // When screen locks / backgrounds in Normal Video mode, video frame rendering stops while audio continues
        assertTrue(VideoPlaybackController.state.value.isVideoTrackDisabled)
        assertTrue(VideoPlaybackController.state.value.isPlaying)
        assertEquals(50_000L, VideoPlaybackController.state.value.currentPositionMs)

        // Unlock screen (ON_START / ON_RESUME) -> restores Normal Video rendering and position
        composeTestRule.runOnIdle {
            customLifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        composeTestRule.waitForIdle()
        assertFalse(VideoPlaybackController.state.value.isVideoTrackDisabled)
        assertEquals(50_000L, VideoPlaybackController.state.value.currentPositionMs)

        // Minimize player back to Video List -> VideoMiniPlayer appears synchronized & audio keeps playing
        composeTestRule.onNodeWithTag("btn_close_player")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertFalse(isExpanded)
        assertTrue(VideoPlaybackController.state.value.isVideoTrackDisabled)
        assertTrue(VideoPlaybackController.state.value.isPlaying)
        composeTestRule.onNodeWithTag("video_mini_player").assertIsDisplayed()
        composeTestRule.onNodeWithTag("video_mini_title", useUnmergedTree = true)
            .assertTextEquals("DJ Slow Indo Remix Visual Session")

        // Tap VideoMiniPlayer open area -> returns to Video Player at preserved position & mode
        composeTestRule.onNodeWithTag("video_mini_open_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(isExpanded)
        assertEquals(50_000L, VideoPlaybackController.state.value.currentPositionMs)
        assertEquals(VideoPlaybackMode.NORMAL_VIDEO, VideoPlaybackController.state.value.playbackMode)

        // Tap Stop button -> cleanly stops playback
        composeTestRule.onNodeWithTag("video_stop_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        assertNull(VideoPlaybackController.state.value.currentVideo)
        assertFalse(VideoPlaybackController.state.value.isPlaying)
    }
}
