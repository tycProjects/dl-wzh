package com.example

import android.app.Notification
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.SettingsRepository
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.RepeatState
import com.example.playback.VideoPlaybackController
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class DjCrossfadeEngineTest {

    companion object {
        private const val TRACK_DURATION_MS = 120_000L
    }

    private val createdFiles = mutableListOf<File>()
    private lateinit var wavTrack1: Song
    private lateinit var wavTrack2: Song
    private lateinit var wavTrack3: Song

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(SettingsRepository.SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        MusicController.resetInMemoryStateForTesting()
        MusicController.setLanguage(AppLanguage.ENGLISH)

        val f1 = createPcmWavFile(File(context.cacheDir, "dj_mix_track_1.wav"), durationMs = TRACK_DURATION_MS.toInt(), freqStep = 1)
        val f2 = createPcmWavFile(File(context.cacheDir, "dj_mix_track_2.wav"), durationMs = TRACK_DURATION_MS.toInt(), freqStep = 2)
        val f3 = createPcmWavFile(File(context.cacheDir, "dj_mix_track_3.wav"), durationMs = TRACK_DURATION_MS.toInt(), freqStep = 3)

        wavTrack1 = Song(
            id = 901L,
            title = "JNMF Club Intro 1",
            artist = "DJ Jussar",
            album = "DJ Mix Session",
            durationMs = TRACK_DURATION_MS,
            sizeBytes = f1.length(),
            dateAdded = 1_000L,
            contentUri = Uri.fromFile(f1),
            fileName = f1.name,
            remixCategory = RemixCategory.FULLBEAT
        )
        wavTrack2 = Song(
            id = 902L,
            title = "JNMF Bass Horeg 2",
            artist = "DJ Golden",
            album = "DJ Mix Session",
            durationMs = TRACK_DURATION_MS,
            sizeBytes = f2.length(),
            dateAdded = 2_000L,
            contentUri = Uri.fromFile(f2),
            fileName = f2.name,
            remixCategory = RemixCategory.INDO_REMIX
        )
        wavTrack3 = Song(
            id = 903L,
            title = "JNMF Breakbeat Outro 3",
            artist = "DJ Indo",
            album = "DJ Mix Session",
            durationMs = TRACK_DURATION_MS,
            sizeBytes = f3.length(),
            dateAdded = 3_000L,
            contentUri = Uri.fromFile(f3),
            fileName = f3.name,
            remixCategory = RemixCategory.BREAKBEAT
        )
    }

    @After
    fun tearDown() {
        MusicController.resetInMemoryStateForTesting()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        createdFiles.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        createdFiles.clear()
    }

    @Test
    fun realWavOverlap_3s_5s_10s_genuinelyOverlapsTwoTracks_rampsVolumesSmoothly_andContinuesWithoutRestart() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        try {
            val queue = listOf(wavTrack1, wavTrack2, wavTrack3)

            // Default must be OFF with only 1 allocated deck (low memory footprint for Redmi 10C)
            assertFalse(MusicController.state.value.djModeEnabled)
            assertEquals(1, MusicController.totalAllocatedDecksForTesting())

            MusicController.restorePlaybackModes(
                isShuffle = false,
                repeatState = RepeatState.ALL,
                isReverseQueue = false
            )

            // Verify ALL THREE user-selectable transition durations: 3s, 5s, and 10s!
            val durationsToVerify = listOf(
                Triple(3, wavTrack1, wavTrack2),
                Triple(5, wavTrack2, wavTrack3),
                Triple(10, wavTrack3, wavTrack1)
            )

            // Start initial song on Deck A
            MusicController.playSong(wavTrack1, queue)
            awaitCondition("Initial song must reach STATE_READY + isPlaying") {
                val deck = MusicController.activeDeckForTesting()
                deck != null && deck.playbackState == Player.STATE_READY && deck.isPlaying
            }

            for ((seconds, expectedOutgoingSong, expectedIncomingSong) in durationsToVerify) {
                val transitionMs = seconds * 1_000L
                MusicController.setDjModeSettings(enabled = true, transitionSeconds = seconds)

                val deckBeforeTransition = MusicController.activeDeckForTesting()!!
                assertEquals(
                    "Unexpected current song before ${seconds}s transition",
                    expectedOutgoingSong.id,
                    MusicController.state.value.currentSong?.id
                )

                // 1. Trigger advance preload (~2.5s before crossfade window) while outgoing deck is actively playing
                val preloadEvalPos = TRACK_DURATION_MS - transitionMs - 2_500L
                MusicController.evaluatePlaybackPositionForDjTransition(preloadEvalPos, TRACK_DURATION_MS)

                val standbyDeck = MusicController.standbyDeckForTesting()
                assertNotNull("Standby deck must be lazily prepared in advance (${seconds}s mode)", standbyDeck)
                assertNotSame("Standby deck must be distinct from active deck", deckBeforeTransition, standbyDeck)
                awaitCondition("Preloaded standby deck must reach STATE_READY silently at volume 0 (${seconds}s mode)") {
                    standbyDeck!!.playbackState == Player.STATE_READY
                }
                assertEquals(0.0f, standbyDeck!!.volume, 0.001f)
                assertFalse("Preloaded standby deck must remain paused until crossfade point", standbyDeck.isPlaying)
                assertEquals(expectedIncomingSong.id.toString(), standbyDeck.currentMediaItem?.mediaId)
                assertTrue("Never allocate more than 2 decks", MusicController.totalAllocatedDecksForTesting() <= 2)

                // 2. Trigger overlapping crossfade at (TRACK_DURATION_MS - transitionMs)
                val crossfadeTriggerPos = TRACK_DURATION_MS - transitionMs
                MusicController.evaluatePlaybackPositionForDjTransition(crossfadeTriggerPos, TRACK_DURATION_MS)
                shadowOf(Looper.getMainLooper()).idle()

                awaitCondition("Incoming deck must start playing when ${seconds}s crossfade begins") {
                    MusicController.state.value.isDjTransitionActive &&
                        MusicController.activeDeckForTesting() === standbyDeck &&
                        standbyDeck.isPlaying
                }

                // Advance ~halfway through the transition duration and verify GENUINE SIMULTANEOUS OVERLAP!
                val halfRampMs = transitionMs / 2L
                advancePlaybackClock(halfRampMs)

                val outgoingMid = MusicController.outgoingDeckForTesting()
                val incomingMid = MusicController.activeDeckForTesting()
                assertNotNull(
                    "Outgoing deck must still be active mid-crossfade (${seconds}s mode); " +
                        "stateSong=${MusicController.state.value.currentSong?.id}, " +
                        "isDjActive=${MusicController.state.value.isDjTransitionActive}, " +
                        "isPlaying=${MusicController.state.value.isPlaying}, " +
                        "samples=${MusicController.recordedOverlapSamples.size}",
                    outgoingMid
                )
                assertNotNull("Incoming deck must be active mid-crossfade (${seconds}s mode)", incomingMid)
                assertSame(deckBeforeTransition, outgoingMid)
                assertSame(standbyDeck, incomingMid)

                assertTrue(
                    "Outgoing track (${expectedOutgoingSong.title}) must still be playing mid-crossfade (${seconds}s)",
                    outgoingMid!!.playWhenReady && outgoingMid.mediaItemCount > 0
                )
                assertTrue(
                    "Incoming track (${expectedIncomingSong.title}) must be playing simultaneously mid-crossfade (${seconds}s)",
                    incomingMid!!.isPlaying && incomingMid.playWhenReady
                )
                assertEquals(
                    "Exactly 2 decks must overlap mid-transition (${seconds}s)",
                    2,
                    MusicController.simultaneouslyActiveDeckCountForTesting()
                )
                assertTrue(
                    "Outgoing volume (${outgoingMid.volume}) must be partially faded out mid-transition (${seconds}s)",
                    outgoingMid.volume in 0.15f..0.92f
                )
                assertTrue(
                    "Incoming volume (${incomingMid.volume}) must be partially faded in mid-transition (${seconds}s)",
                    incomingMid.volume in 0.15f..0.92f
                )

                // 3. Advance past the end of the crossfade (+remaining half + 600ms buffer)
                advancePlaybackClock(halfRampMs + 600L)

                assertFalse("DJ transition flag must clear after ${seconds}s ramp completes", MusicController.state.value.isDjTransitionActive)
                assertNull("Outgoing deck reference must clear after ${seconds}s ramp", MusicController.outgoingDeckForTesting())
                assertEquals("Outgoing deck must be muted after transition", 0.0f, deckBeforeTransition.volume, 0.001f)
                assertEquals("Outgoing deck media items must be cleared to save RAM", 0, deckBeforeTransition.mediaItemCount)
                assertFalse("Outgoing deck must no longer be playing", deckBeforeTransition.isPlaying)

                // Incoming deck continues playing smoothly WITHOUT restarting back to 0ms!
                assertSame("Incoming deck must remain the active player without reset", standbyDeck, MusicController.activeDeckForTesting())
                assertEquals(expectedIncomingSong.id, MusicController.state.value.currentSong?.id)
                assertEquals(1.0f, standbyDeck.volume, 0.001f)
                assertTrue("Incoming deck must still be playing normally after crossfade", standbyDeck.isPlaying)
                assertTrue(
                    "Incoming track must continue past ${transitionMs}ms without restarting (actual=${standbyDeck.currentPosition}ms)",
                    standbyDeck.currentPosition >= transitionMs - 800L
                )

                // Verify recorded ramp curves are monotonic and smooth (outgoing decreases, incoming increases)
                val samples = MusicController.recordedOverlapSamples.toList()
                assertTrue("Expected smooth multi-step crossfade samples (${samples.size})", samples.size >= 8)
                assertTrue("Overlap samples must confirm both decks were active simultaneously", samples.any { it.bothDecksActive })
                for (i in 1 until samples.size) {
                    assertTrue(
                        "Outgoing volume must monotonically decrease",
                        samples[i].outgoingVolume <= samples[i - 1].outgoingVolume + 0.001f
                    )
                    assertTrue(
                        "Incoming volume must monotonically increase",
                        samples[i].incomingVolume >= samples[i - 1].incomingVolume - 0.001f
                    )
                }
            }
        } finally {
            serviceController.destroy()
        }
    }

    @Test
    fun backgroundPlaybackCrossfade_updatesMediaSessionPlayerAndNotificationSeamlessly() {
        val context = RuntimeEnvironment.getApplication()
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()
        var media3Controller: MediaController? = null

        try {
            val session = service.currentMediaSession
            assertNotNull(session)

            val future = MediaController.Builder(context, session!!.token).buildAsync()
            awaitCondition("MediaController must connect to PlaybackService") { future.isDone }
            media3Controller = future.get()

            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 3)
            MusicController.playSong(wavTrack1, listOf(wavTrack1, wavTrack2))

            awaitCondition("Background MediaSession must start playing Track 1") {
                media3Controller.playbackState == Player.STATE_READY && media3Controller.isPlaying
            }

            val notifBefore = service.buildForegroundNotificationForCurrentSession()
            assertEquals("JNMF Club Intro 1", notifBefore.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())

            // Trigger 3s background crossfade from Track 1 -> Track 2
            MusicController.evaluatePlaybackPositionForDjTransition(TRACK_DURATION_MS - 3_000L, TRACK_DURATION_MS)
            awaitCondition("Background crossfade to Track 2 must begin") {
                MusicController.state.value.currentSong?.id == wavTrack2.id
            }

            // Advance 3.5s in background (no UI mounted) so crossfade completes on Deck B
            advancePlaybackClock(3_500L)

            assertEquals(wavTrack2.id, MusicController.state.value.currentSong?.id)
            assertTrue(media3Controller.isPlaying)
            assertEquals(Player.STATE_READY, media3Controller.playbackState)
            assertTrue(
                "MediaSession player must report Track 2's continuous position >= 2,400ms (actual=${session.player.currentPosition})",
                session.player.currentPosition >= 2_400L
            )

            // Foreground notification must show Track 2 title and Pause action
            val notifAfter = service.buildForegroundNotificationForCurrentSession()
            assertEquals("JNMF Bass Horeg 2", notifAfter.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
            assertEquals("Pause", notifAfter.actions[1].title.toString())

            // Background pause & resume via MediaSession must control newly active Deck B cleanly
            media3Controller.pause()
            awaitCondition("Background MediaController pause must pause Deck B") {
                !media3Controller.isPlaying && !MusicController.state.value.isPlaying
            }

            val posWhilePaused = session.player.currentPosition
            media3Controller.play()
            awaitCondition("Background MediaController play must resume Deck B without restarting") {
                media3Controller.isPlaying && MusicController.state.value.isPlaying
            }
            assertTrue(session.player.currentPosition >= posWhilePaused - 100L)
        } finally {
            media3Controller?.release()
            serviceController.destroy()
        }
    }

    @Test
    fun edgeCases_shortTracks_missingNextFile_audioFocusChanges_andRapidNextPresses() {
        val context = RuntimeEnvironment.getApplication()
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        try {
            // 1. Short track handling: < 6s skips crossfade; 8s with 10s configured clamps to 4s
            assertNull(MusicController.computeEffectiveTransitionMs(durationMs = 4_000L, configuredSeconds = 3))
            assertEquals(4_000L, MusicController.computeEffectiveTransitionMs(durationMs = 8_000L, configuredSeconds = 10))
            assertEquals(10_000L, MusicController.computeEffectiveTransitionMs(durationMs = TRACK_DURATION_MS, configuredSeconds = 10))

            // 2. Missing file in queue: [wavTrack1, missingSong, wavTrack3]
            val missingSong = Song(
                id = 999L,
                title = "Deleted Track",
                artist = "Unknown",
                album = "Missing",
                durationMs = TRACK_DURATION_MS,
                sizeBytes = 0L,
                dateAdded = 4_000L,
                contentUri = Uri.fromFile(File(context.cacheDir, "does_not_exist_999.wav")),
                fileName = "does_not_exist_999.wav",
                remixCategory = RemixCategory.SLOW
            )
            val queueWithMissing = listOf(wavTrack1, missingSong, wavTrack3)
            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 3)
            MusicController.playSong(wavTrack1, queueWithMissing)
            awaitCondition("Track 1 must play") {
                val deck = MusicController.activeDeckForTesting()
                deck != null && deck.playbackState == Player.STATE_READY && deck.isPlaying
            }

            // Trigger preload at 5s remaining -> attempts to preload missingSong on standby deck
            MusicController.evaluatePlaybackPositionForDjTransition(TRACK_DURATION_MS - 5_000L, TRACK_DURATION_MS)
            advancePlaybackClock(300L)

            // Track 1 must STILL be playing at full 1.0f volume without error or flickering to 999!
            assertEquals(wavTrack1.id, MusicController.state.value.currentSong?.id)
            assertTrue(MusicController.state.value.isPlaying)
            assertEquals(1.0f, MusicController.activeDeckForTesting()!!.volume, 0.001f)
            assertNull(MusicController.state.value.errorCode)

            // Crossfade trigger at 3s remaining automatically skips broken missingSong and crossfades into wavTrack3!
            MusicController.evaluatePlaybackPositionForDjTransition(TRACK_DURATION_MS - 3_000L, TRACK_DURATION_MS)
            awaitCondition("Must skip broken file and commit crossfade directly into Track 3") {
                MusicController.state.value.currentSong?.id == wavTrack3.id &&
                    MusicController.state.value.isDjTransitionActive &&
                    MusicController.outgoingDeckForTesting() != null
            }
            advancePlaybackClock(1_200L)
            assertEquals(wavTrack3.id, MusicController.state.value.currentSong?.id)
            assertNotNull("Outgoing deck must still be active mid-crossfade into Track 3", MusicController.outgoingDeckForTesting())

            // 3. Audio Focus Ducking mid-crossfade -> scales BOTH decks without stopping overlap
            val outBeforeDuck = MusicController.outgoingDeckForTesting()!!.volume
            val inBeforeDuck = MusicController.activeDeckForTesting()!!.volume
            assertTrue("Expected positive mid-crossfade volumes before duck", outBeforeDuck > 0.05f && inBeforeDuck > 0.05f)

            MusicController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
            assertEquals(outBeforeDuck * 0.25f, MusicController.outgoingDeckForTesting()!!.volume, 0.02f)
            assertEquals(inBeforeDuck * 0.25f, MusicController.activeDeckForTesting()!!.volume, 0.02f)

            MusicController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
            assertEquals(outBeforeDuck, MusicController.outgoingDeckForTesting()!!.volume, 0.02f)
            assertEquals(inBeforeDuck, MusicController.activeDeckForTesting()!!.volume, 0.02f)

            // 4. Rapid Next-button presses mid-crossfade -> never overlaps > 2 tracks, cancels cleanly
            repeat(7) {
                MusicController.next()
            }
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(MusicController.state.value.isDjTransitionActive)
            assertNull(MusicController.outgoingDeckForTesting())
            assertTrue(MusicController.totalAllocatedDecksForTesting() <= 2)
            assertEquals(1, MusicController.simultaneouslyActiveDeckCountForTesting())
            assertEquals(1.0f, MusicController.activeDeckForTesting()!!.volume, 0.001f)

            // 5. Transient Audio Focus Loss & Gain -> pauses and auto-resumes cleanly
            awaitCondition("Active track after rapid skips must reach STATE_READY + isPlaying") {
                val deck = MusicController.activeDeckForTesting()
                deck != null && deck.playbackState == Player.STATE_READY && deck.isPlaying
            }
            MusicController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
            assertFalse(MusicController.state.value.isPlaying)

            MusicController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
            awaitCondition("Transient focus gain must resume playback") {
                MusicController.state.value.isPlaying && MusicController.activeDeckForTesting()?.isPlaying == true
            }

            // Permanent Audio Focus Loss -> pauses and stays paused
            MusicController.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
            assertFalse(MusicController.state.value.isPlaying)
        } finally {
            serviceController.destroy()
        }
    }

    @Test
    fun queueModes_repeatOne_shuffle_reverseQueue_andLazyDeckReleaseWhenDisabled() {
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        try {
            val queue = listOf(wavTrack1, wavTrack2, wavTrack3)
            MusicController.djCrossfadeStepDelayMs = 0L

            // 1. Repeat.ONE crossfades track into itself across Deck A -> Deck B
            MusicController.restorePlaybackModes(isShuffle = false, repeatState = RepeatState.ONE, isReverseQueue = false)
            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 3)
            MusicController.playSong(wavTrack1, queue)
            val firstDeck = MusicController.activeDeckForTesting()
            MusicController.evaluatePlaybackPositionForDjTransition(TRACK_DURATION_MS - 3_000L, TRACK_DURATION_MS)
            assertEquals(wavTrack1.id, MusicController.state.value.currentSong?.id)
            assertNotSame("Repeat.ONE must crossfade onto the opposite deck smoothly", firstDeck, MusicController.activeDeckForTesting())

            // 2. Shuffle preloads a non-current song and crossfades to that exact preloaded song
            MusicController.restorePlaybackModes(isShuffle = true, repeatState = RepeatState.ALL, isReverseQueue = false)
            MusicController.playSong(wavTrack1, queue)
            MusicController.evaluatePlaybackPositionForDjTransition(TRACK_DURATION_MS - 5_500L, TRACK_DURATION_MS)
            val preloadedId = MusicController.standbyDeckForTesting()?.currentMediaItem?.mediaId?.toLongOrNull()
            assertNotNull(preloadedId)
            assertTrue("Shuffle must pick a different song when queue has >1 tracks", preloadedId != wavTrack1.id)

            MusicController.evaluatePlaybackPositionForDjTransition(TRACK_DURATION_MS - 3_000L, TRACK_DURATION_MS)
            assertEquals("Crossfade must transition to the exact preloaded shuffled track", preloadedId, MusicController.state.value.currentSong?.id)

            // 3. Reverse Queue respects reversed queue order
            MusicController.restorePlaybackModes(isShuffle = false, repeatState = RepeatState.ALL, isReverseQueue = true)
            MusicController.playSong(wavTrack3, queue) // reversed queue is [903, 902, 901]
            MusicController.evaluatePlaybackPositionForDjTransition(TRACK_DURATION_MS - 3_000L, TRACK_DURATION_MS)
            assertEquals(wavTrack2.id, MusicController.state.value.currentSong?.id)

            // 4. Disabling DJ Mode releases idle secondary deck when Deck A is active
            MusicController.evaluatePlaybackPositionForDjTransition(TRACK_DURATION_MS - 3_000L, TRACK_DURATION_MS) // swaps back to Deck A (901)
            assertEquals(2, MusicController.totalAllocatedDecksForTesting())
            MusicController.setDjModeSettings(enabled = false, transitionSeconds = 3)
            assertEquals("Idle secondary deck must release when DJ Mode is turned OFF", 1, MusicController.totalAllocatedDecksForTesting())
        } finally {
            serviceController.destroy()
        }
    }

    private fun advancePlaybackClock(totalMs: Long) {
        val stepMs = 50L
        var advanced = 0L
        var stepIndex = 0
        while (advanced < totalMs) {
            val delta = minOf(stepMs, totalMs - advanced)
            ShadowSystemClock.advanceBy(Duration.ofMillis(delta))
            shadowOf(Looper.getMainLooper()).idle()
            if (stepIndex % 6 == 0) {
                Thread.sleep(3)
                shadowOf(Looper.getMainLooper()).idle()
            }
            advanced += delta
            stepIndex++
        }
    }

    private fun awaitCondition(
        failureMessage: String,
        timeoutMs: Long = 5_000L,
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
        assertTrue(failureMessage, condition())
    }

    private fun createPcmWavFile(file: File, durationMs: Int, freqStep: Int): File {
        createdFiles.add(file)
        val sampleRate = 16_000
        val numSamples = (sampleRate * durationMs) / 1_000
        val byteRate = sampleRate * 2 // 16-bit mono
        val dataSize = numSamples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        FileOutputStream(file).use { out ->
            out.write(header.array())
            val chunk = ByteArray(8_192)
            for (i in chunk.indices step 2) {
                val sample = (((i * freqStep) % 64) - 32) * 120
                chunk[i] = (sample and 0xFF).toByte()
                chunk[i + 1] = ((sample shr 8) and 0xFF).toByte()
            }
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
