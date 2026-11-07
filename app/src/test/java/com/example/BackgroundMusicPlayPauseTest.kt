package com.example

import android.app.Notification
import android.content.Intent
import android.media.session.MediaController as PlatformMediaController
import android.media.session.PlaybackState as PlatformPlaybackState
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.ThemeColor
import com.example.playback.ActiveMediaSessionOwner
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.VideoPlaybackController
import com.example.ui.components.MiniPlayer
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.JnmfTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
import org.robolectric.shadows.ShadowSystemClock
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class BackgroundMusicPlayPauseTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testWavFile: File
    private lateinit var sampleSong: Song

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        MusicController.pause()
        MusicController.detachPlayer()
        MusicController.setLanguage(AppLanguage.ENGLISH)

        testWavFile = createPcmWavFile(
            file = File(context.cacheDir, "jnmf_test_track.wav"),
            durationMs = 120_000
        )
        sampleSong = Song(
            id = 501L,
            title = "JNMF Midnight Bassline",
            artist = "DJ Jussar",
            album = "JNMF Club Vol 2",
            durationMs = 120_000L,
            sizeBytes = testWavFile.length(),
            dateAdded = 1_700_000_000L,
            contentUri = Uri.fromFile(testWavFile),
            remixCategory = RemixCategory.FULLBEAT
        )
    }

    @After
    fun tearDown() {
        MusicController.pause()
        MusicController.detachPlayer()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        if (::testWavFile.isInitialized && testWavFile.exists()) {
            testWavFile.delete()
        }
    }

    @Test
    fun backgroundMediaSessionAndNotification_playPauseTransitionsCleanlyWithoutStuckBufferingSpinner() {
        val context = RuntimeEnvironment.getApplication()
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()
        var media3Controller: MediaController? = null

        try {
            val session = service.currentMediaSession
            assertNotNull("PlaybackService must initialize MediaSession", session)

            val controllerFuture = MediaController.Builder(context, session!!.token).buildAsync()
            awaitMainLooperCondition(
                failureMessage = { "Media3 MediaController failed to connect to PlaybackService session" }
            ) {
                controllerFuture.isDone
            }
            media3Controller = controllerFuture.get()
            val platformController = PlatformMediaController(context, session.platformToken)

            // Configure Equalizer before playback to verify it stays intact across Play/Pause/Resume
            MusicController.equalizerManager.setEnabled(true)
            MusicController.equalizerManager.applyPreset("Bass Horeg")
            MusicController.equalizerManager.setBassStrength(820)
            val expectedBandsBefore = MusicController.equalizerManager.uiState.value.bands

            // 1. Start playing a real local WAV song
            MusicController.playSong(sampleSong, listOf(sampleSong))

            awaitMainLooperCondition(
                failureMessage = {
                    "Expected MediaSession controller to reach STATE_READY + isPlaying=true (not stuck buffering); " +
                        "media3State=${media3Controller.playbackState}, media3IsPlaying=${media3Controller.isPlaying}"
                }
            ) {
                media3Controller.playbackState == Player.STATE_READY &&
                    media3Controller.isPlaying &&
                    media3Controller.playWhenReady
            }

            assertEquals(ActiveMediaSessionOwner.MUSIC, VideoPlaybackController.activeOwner.value)
            assertTrue(MusicController.state.value.isPlaying)
            assertEquals(Player.STATE_READY, media3Controller.playbackState)
            assertNotEquals(
                "MediaSession must not remain stuck in STATE_BUFFERING during normal playback",
                Player.STATE_BUFFERING,
                media3Controller.playbackState
            )
            platformController.playbackState?.let { ps ->
                assertEquals(PlatformPlaybackState.STATE_PLAYING, ps.state)
                assertNotEquals(PlatformPlaybackState.STATE_BUFFERING, ps.state)
            }

            // Verify notification shows Pause icon (⏸️) while playing
            val playingNotif = service.buildForegroundNotificationForCurrentSession()
            assertEquals(
                "JNMF Midnight Bassline",
                playingNotif.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            )
            assertEquals(3, playingNotif.actions.size)
            assertEquals("Pause", playingNotif.actions[1].title.toString())
            assertEquals(R.drawable.ic_pause, playingNotif.actions[1].getIcon()?.resId)

            // 2. Tap Pause via background MediaSession controller (simulates Android SystemUI notification button)
            media3Controller.pause()
            awaitMainLooperCondition(
                failureMessage = { "Expected MediaSession controller to pause promptly" }
            ) {
                !media3Controller.isPlaying &&
                    !media3Controller.playWhenReady &&
                    !MusicController.state.value.isPlaying
            }

            // Seek to a known mid-song position (5,000ms) while paused and wait for STATE_READY
            MusicController.seekTo(5_000L)
            awaitMainLooperCondition(
                failureMessage = {
                    "Expected paused seek to settle in STATE_READY near 5,000ms (actual=${session.player.currentPosition})"
                }
            ) {
                media3Controller.playbackState == Player.STATE_READY &&
                    session.player.currentPosition in 4_950L..5_050L
            }

            assertFalse(MusicController.state.value.isPlaying)
            assertEquals(Player.STATE_READY, media3Controller.playbackState)
            assertNotEquals(Player.STATE_BUFFERING, media3Controller.playbackState)
            platformController.playbackState?.let { ps ->
                assertEquals(PlatformPlaybackState.STATE_PAUSED, ps.state)
            }
            assertEquals(501L, MusicController.state.value.currentSong?.id)
            assertTrue(MusicController.state.value.currentPositionMs in 4_950L..5_050L)

            // Verify notification shows Play icon (▶️) when paused
            val pausedNotif = service.buildForegroundNotificationForCurrentSession()
            assertEquals("Play", pausedNotif.actions[1].title.toString())
            assertEquals(R.drawable.ic_play_arrow, pausedNotif.actions[1].getIcon()?.resId)

            // Record any unexpected resets/discontinuities during resume & subsequent pause
            val unexpectedResetEvents = mutableListOf<String>()
            val resetWatcher = object : Player.Listener {
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    unexpectedResetEvents.add("discontinuity(reason=$reason,${oldPosition.positionMs}->${newPosition.positionMs})")
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    unexpectedResetEvents.add("mediaItemTransition(id=${mediaItem?.mediaId},reason=$reason)")
                }
            }
            session.player.addListener(resetWatcher)

            // 3. Tap Play via background MediaSession controller -> resumes same song & position without spinner or reset
            media3Controller.play()
            awaitMainLooperCondition(
                failureMessage = { "Expected MediaSession controller to resume STATE_READY + isPlaying=true" }
            ) {
                media3Controller.playbackState == Player.STATE_READY &&
                    media3Controller.isPlaying &&
                    MusicController.state.value.isPlaying
            }

            assertTrue(
                "Resuming playback must not reset or seek the media item: $unexpectedResetEvents",
                unexpectedResetEvents.isEmpty()
            )
            assertTrue(MusicController.state.value.isPlaying)
            assertEquals(Player.STATE_READY, media3Controller.playbackState)
            assertNotEquals(Player.STATE_BUFFERING, media3Controller.playbackState)
            platformController.playbackState?.let { ps ->
                assertEquals(PlatformPlaybackState.STATE_PLAYING, ps.state)
            }
            assertEquals(501L, MusicController.state.value.currentSong?.id)
            assertTrue(
                "Expected resumed position (${session.player.currentPosition} ms) to remain >= 4950ms",
                session.player.currentPosition >= 4_950L &&
                    MusicController.state.value.currentPositionMs >= 4_950L
            )

            val resumedNotif = service.buildForegroundNotificationForCurrentSession()
            assertEquals("Pause", resumedNotif.actions[1].title.toString())
            assertEquals(R.drawable.ic_pause, resumedNotif.actions[1].getIcon()?.resId)

            // 4. Also verify notification PendingIntent service actions (ACTION_PAUSE / ACTION_PLAY)
            service.onStartCommand(
                Intent(service, PlaybackService::class.java).apply { action = PlaybackService.ACTION_PAUSE },
                0,
                10
            )
            awaitMainLooperCondition(
                failureMessage = { "Expected ACTION_PAUSE intent to pause MediaSession" }
            ) {
                !media3Controller.isPlaying && !MusicController.state.value.isPlaying
            }
            assertEquals(
                R.drawable.ic_play_arrow,
                service.buildForegroundNotificationForCurrentSession().actions[1].getIcon()?.resId
            )

            service.onStartCommand(
                Intent(service, PlaybackService::class.java).apply { action = PlaybackService.ACTION_PLAY },
                0,
                11
            )
            awaitMainLooperCondition(
                failureMessage = { "Expected ACTION_PLAY intent to resume MediaSession" }
            ) {
                media3Controller.isPlaying && MusicController.state.value.isPlaying
            }
            assertEquals(
                R.drawable.ic_pause,
                service.buildForegroundNotificationForCurrentSession().actions[1].getIcon()?.resId
            )

            session.player.removeListener(resetWatcher)

            // 5. Verify Equalizer settings remain completely unaffected
            val eqAfter = MusicController.equalizerManager.uiState.value
            assertTrue(eqAfter.isEnabled)
            assertEquals("Bass Horeg", eqAfter.currentPreset)
            assertEquals(820, eqAfter.bassStrength)
            assertEquals(expectedBandsBefore, eqAfter.bands)
        } finally {
            media3Controller?.release()
            serviceController.destroy()
        }
    }

    @Test
    fun rapidRepeatedPlayPauseTaps_doNotConflictResetTrackOrStickInBuffering() {
        val context = RuntimeEnvironment.getApplication()
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()
        var media3Controller: MediaController? = null

        try {
            val controllerFuture = MediaController.Builder(
                context,
                service.currentMediaSession!!.token
            ).buildAsync()
            awaitMainLooperCondition(
                failureMessage = { "Media3 MediaController failed to connect" }
            ) {
                controllerFuture.isDone
            }
            media3Controller = controllerFuture.get()

            // Start song and immediately tap Play/Pause rapidly even while ExoPlayer is preparing
            MusicController.playSong(sampleSong, listOf(sampleSong))

            // 1st tap -> Pause
            MusicController.togglePlayPause()
            assertFalse(MusicController.state.value.isPlaying)

            // 2nd tap -> Resume
            MusicController.togglePlayPause()
            assertTrue(MusicController.state.value.isPlaying)

            // 3rd tap -> Pause
            MusicController.togglePlayPause()
            assertFalse(MusicController.state.value.isPlaying)

            // 4th tap -> Resume
            MusicController.togglePlayPause()
            assertTrue(MusicController.state.value.isPlaying)

            // Let ExoPlayer settle and verify MediaSession lands cleanly in STATE_READY + isPlaying (no stuck spinner)
            awaitMainLooperCondition(
                failureMessage = {
                    "Expected rapid taps to settle in STATE_READY + isPlaying; " +
                        "media3State=${media3Controller.playbackState}, media3IsPlaying=${media3Controller.isPlaying}"
                }
            ) {
                media3Controller.playbackState == Player.STATE_READY && media3Controller.isPlaying
            }
            assertEquals(501L, MusicController.state.value.currentSong?.id)

            // Seek to 6,000ms, then rapidly fire ACTION_PLAY_PAUSE 3 times (odd count -> ends Paused)
            MusicController.seekTo(6_000L)
            awaitMainLooperCondition(
                failureMessage = {
                    "Expected seek to 6000ms to settle (state=${media3Controller.playbackState}, pos=${MusicController.state.value.currentPositionMs})"
                }
            ) {
                media3Controller.playbackState == Player.STATE_READY &&
                    MusicController.state.value.currentPositionMs >= 5_950L
            }

            repeat(3) { idx ->
                service.onStartCommand(
                    Intent(service, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_PLAY_PAUSE
                    },
                    0,
                    20 + idx
                )
                shadowOf(Looper.getMainLooper()).idle()
            }

            awaitMainLooperCondition(
                failureMessage = {
                    "Expected 3 rapid toggles from playing to settle paused in STATE_READY; " +
                        "media3State=${media3Controller.playbackState}, media3IsPlaying=${media3Controller.isPlaying}"
                }
            ) {
                media3Controller.playbackState == Player.STATE_READY &&
                    !media3Controller.isPlaying &&
                    !MusicController.state.value.isPlaying
            }
            assertEquals(501L, MusicController.state.value.currentSong?.id)
            assertTrue(MusicController.state.value.currentPositionMs >= 5_950L)
            assertEquals(
                R.drawable.ic_play_arrow,
                service.buildForegroundNotificationForCurrentSession().actions[1].getIcon()?.resId
            )
        } finally {
            media3Controller?.release()
            serviceController.destroy()
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w412dp-h915dp")
    fun miniPlayerPlayPauseButton_updatesIconPromptlyWithRealPlaybackState() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        try {
            MusicController.playSong(sampleSong, listOf(sampleSong))
            awaitMainLooperCondition(
                failureMessage = { "Wait for MusicController playing state" }
            ) {
                MusicController.state.value.isPlaying
            }

            composeTestRule.setContent {
                JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                    CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                        val playbackState by MusicController.state.collectAsState()
                        MiniPlayer(
                            playbackState = playbackState,
                            onPlayPause = { MusicController.togglePlayPause() },
                            onNext = { MusicController.next() },
                            onPrevious = { MusicController.previous() },
                            onClick = {}
                        )
                    }
                }
            }
            composeTestRule.waitForIdle()

            // Playing -> shows Pause icon
            composeTestRule.onNodeWithTag("mini_play_pause")
                .assertContentDescriptionEquals("Pause")

            // Tap button -> pauses promptly and shows Play icon
            composeTestRule.onNodeWithTag("mini_play_pause").performClick()
            composeTestRule.waitForIdle()
            assertFalse(MusicController.state.value.isPlaying)
            composeTestRule.onNodeWithTag("mini_play_pause")
                .assertContentDescriptionEquals("Play")

            // Tap button again -> resumes promptly and shows Pause icon
            composeTestRule.onNodeWithTag("mini_play_pause").performClick()
            composeTestRule.waitForIdle()
            assertTrue(MusicController.state.value.isPlaying)
            composeTestRule.onNodeWithTag("mini_play_pause")
                .assertContentDescriptionEquals("Pause")
        } finally {
            serviceController.destroy()
        }
    }

    private fun awaitMainLooperCondition(
        timeoutMs: Long = 5_000L,
        failureMessage: () -> String,
        condition: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            ShadowSystemClock.advanceBy(Duration.ofMillis(20))
            Thread.sleep(5)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(failureMessage(), condition())
    }

    private fun createPcmWavFile(file: File, durationMs: Int): File {
        val sampleRate = 16_000
        val numSamples = (sampleRate * durationMs) / 1_000
        val byteRate = sampleRate * 2 // 16-bit mono
        val dataSize = numSamples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF WAVE header
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // PCM chunk size
        header.putShort(1) // AudioFormat = PCM
        header.putShort(1) // NumChannels = 1
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(2) // BlockAlign
        header.putShort(16) // BitsPerSample
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        FileOutputStream(file).use { out ->
            out.write(header.array())
            val chunk = ByteArray(8_192)
            var remaining = dataSize
            while (remaining > 0) {
                val toWrite = minOf(remaining, chunk.size)
                out.write(chunk, 0, toWrite)
                remaining -= toWrite
            }
        }
        return file
    }
}
