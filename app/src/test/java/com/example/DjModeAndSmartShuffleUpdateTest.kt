package com.example

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.DjModeSettings
import com.example.data.repository.SettingsRepository
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.QueueSourceType
import com.example.playback.RepeatState
import com.example.playback.VideoPlaybackController
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
class DjModeAndSmartShuffleUpdateTest {

    companion object {
        private const val LONG_WAV_DURATION_MS = 120_000L
        private const val DJ_SHUFFLE_WAV_DURATION_MS = 60_000L
    }

    private val createdFiles = mutableListOf<File>()

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
    fun djModeExtendedDuration_optionsAndPersistence_supports3s5s10sAnd20s() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)

        assertEquals(listOf(3, 5, 10, 20), DjModeSettings.VALID_TRANSITION_OPTIONS)
        val initialSettings = repo.getInitialDjModeSettings()
        assertFalse(initialSettings.enabled)
        assertEquals(3, initialSettings.transitionSeconds)

        for (sec in listOf(3, 5, 10, 20)) {
            assertEquals(sec, DjModeSettings.normalizeSeconds(sec))
            assertTrue(repo.saveDjModeSettings(enabled = true, transitionSeconds = sec))
            val restored = SettingsRepository(context).getInitialDjModeSettings()
            assertTrue(restored.enabled)
            assertEquals(sec, restored.transitionSeconds)
        }
        // Invalid values fall back safely to 3s default
        assertEquals(3, DjModeSettings.normalizeSeconds(15))
        assertEquals(20_000L, MusicController.computeEffectiveTransitionMs(durationMs = 120_000L, configuredSeconds = 20))
    }

    @Test
    fun djModeExtendedDuration_realWavOverlap_supports3s5s10sAnd20s_withoutTripleTrackPlayback() {
        val context = RuntimeEnvironment.getApplication()
        val wavSongs = (1..5).map { idx ->
            val file = createPcmWavFile(
                File(context.cacheDir, "dj_20s_test_$idx.wav"),
                durationMs = LONG_WAV_DURATION_MS.toInt(),
                freqStep = idx
            )
            Song(
                id = 2000L + idx,
                title = "Extended DJ Track $idx",
                artist = "DJ JNMF",
                album = "Extended Mix",
                durationMs = LONG_WAV_DURATION_MS,
                sizeBytes = file.length(),
                dateAdded = idx * 1000L,
                contentUri = Uri.fromFile(file),
                fileName = file.name,
                remixCategory = RemixCategory.FULLBEAT
            )
        }

        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        try {
            MusicController.restorePlaybackModes(
                isShuffle = false,
                repeatState = RepeatState.ALL,
                isReverseQueue = false
            )

            // Start initial song on Deck A and chain 3s -> 5s -> 10s -> 20s seamlessly without restarting!
            MusicController.playSong(wavSongs[0], wavSongs)
            awaitCondition("Initial deck must reach STATE_READY + isPlaying") {
                val deck = MusicController.activeDeckForTesting()
                deck != null && deck.playbackState == Player.STATE_READY && deck.isPlaying
            }

            val durationsToTest = listOf(3, 5, 10, 20)
            for ((stepIdx, durationSec) in durationsToTest.withIndex()) {
                val fromSong = wavSongs[stepIdx]
                val expectedNextSong = wavSongs[stepIdx + 1]
                val durationMs = durationSec * 1_000L

                MusicController.setDjModeSettings(enabled = true, transitionSeconds = durationSec)
                assertEquals(
                    "Active song before ${durationSec}s crossfade must be ${fromSong.title}",
                    fromSong.id,
                    MusicController.state.value.currentSong?.id
                )

                // Preload 2.5s before crossfade window and wait for standby deck to reach STATE_READY
                val preloadPos = LONG_WAV_DURATION_MS - durationMs - 2_500L
                MusicController.evaluatePlaybackPositionForDjTransition(preloadPos, LONG_WAV_DURATION_MS)
                awaitCondition("Standby deck must reach STATE_READY during advance preload (${durationSec}s)") {
                    MusicController.standbyDeckForTesting()?.playbackState == Player.STATE_READY
                }
                assertEquals(
                    "Track must not switch during advance preload (${durationSec}s)",
                    fromSong.id,
                    MusicController.state.value.currentSong?.id
                )

                // Trigger crossfade at exact (duration - transitionSeconds)
                val triggerPos = LONG_WAV_DURATION_MS - durationMs
                MusicController.evaluatePlaybackPositionForDjTransition(triggerPos, LONG_WAV_DURATION_MS)
                shadowOf(Looper.getMainLooper()).idle()
                awaitCondition("Incoming song ${expectedNextSong.id} must activate for ${durationSec}s crossfade") {
                    MusicController.state.value.currentSong?.id == expectedNextSong.id &&
                        MusicController.state.value.isDjTransitionActive &&
                        MusicController.activeDeckForTesting()?.isPlaying == true
                }

                // Advance halfway through the transition: both decks must overlap simultaneously, never > 2 decks!
                val halfMs = durationMs / 2L
                advancePlaybackClock(halfMs)
                assertTrue(
                    "Crossfade (${durationSec}s) must still be active at midpoint (${halfMs}ms)",
                    MusicController.state.value.isDjTransitionActive
                )
                assertTrue(MusicController.totalAllocatedDecksForTesting() <= 2)
                assertEquals(2, MusicController.simultaneouslyActiveDeckCountForTesting())

                val midSample = MusicController.recordedOverlapSamples.lastOrNull()
                assertNotNull(midSample)
                assertTrue(
                    "At ${durationSec}s midpoint both outgoing (${midSample!!.outgoingVolume}) and incoming (${midSample.incomingVolume}) must be audible",
                    midSample.outgoingVolume in 0.15f..0.92f && midSample.incomingVolume in 0.15f..0.92f
                )

                // Verify accurate duration: 600ms before the end of the selected duration, transition MUST still be active!
                val nearEndStepMs = durationMs - halfMs - 600L
                if (nearEndStepMs > 0L) {
                    advancePlaybackClock(nearEndStepMs)
                    assertTrue(
                        "Crossfade (${durationSec}s) must still be active at ${durationMs - 600L}ms",
                        MusicController.state.value.isDjTransitionActive
                    )
                }

                // Advance past the remainder of the transition and verify clean completion without restarting
                advancePlaybackClock(1_000L)
                assertFalse(
                    "Crossfade (${durationSec}s) must finish cleanly after ${durationSec}s",
                    MusicController.state.value.isDjTransitionActive
                )
                assertEquals(expectedNextSong.id, MusicController.state.value.currentSong?.id)
                assertEquals(1.0f, MusicController.activeDeckForTesting()!!.volume, 0.01f)
                assertEquals(1, MusicController.simultaneouslyActiveDeckCountForTesting())
                assertTrue(
                    "Incoming song must continue normally without restarting after ${durationSec}s transition",
                    MusicController.activeDeckForTesting()!!.currentPosition >= durationMs - 1_500L
                )
            }
        } finally {
            serviceController.destroy()
        }
    }

    @Test
    fun smartShuffle_smallAndLargePlaylists_neverRepeatsWithinCycle_autoCreatesNewCycle_andAvoidsBoundaryRepeat() {
        // 1. Single-song playlist edge case
        val singlePlaylist = makeSyntheticSongs(1, idBase = 10L)
        MusicController.restorePlaybackModes(isShuffle = true, repeatState = RepeatState.ALL, isReverseQueue = false)
        MusicController.playSong(singlePlaylist[0], singlePlaylist)
        assertEquals(10L, MusicController.state.value.currentSong?.id)
        MusicController.next()
        assertEquals(10L, MusicController.state.value.currentSong?.id)
        MusicController.previous()
        assertEquals(10L, MusicController.state.value.currentSong?.id)

        // 2. Two-song playlist across 10 cycles: zero intra-cycle repeats AND zero boundary repeats (strict alternation)
        val twoSongPlaylist = makeSyntheticSongs(2, idBase = 20L)
        MusicController.playSong(twoSongPlaylist[0], twoSongPlaylist)
        var prevId = MusicController.state.value.currentSong!!.id
        repeat(19) {
            MusicController.next()
            val currId = MusicController.state.value.currentSong!!.id
            assertNotEquals(
                "2-song Smart Shuffle must never repeat the same song back-to-back within or across cycles",
                prevId,
                currId
            )
            prevId = currId
        }

        // 3. Small 5-song playlist across 6 full cycles (30 plays)
        val smallPlaylist = makeSyntheticSongs(5, idBase = 100L)
        MusicController.playSong(smallPlaylist[0], smallPlaylist)
        var previousCycleLastId: Long? = null
        for (cycle in 0 until 6) {
            val cyclePlayed = mutableListOf<Long>()
            if (cycle == 0) {
                cyclePlayed.add(MusicController.state.value.currentSong!!.id)
            } else {
                MusicController.next()
                val firstOfNewCycle = MusicController.state.value.currentSong!!.id
                assertNotEquals(
                    "First song of cycle $cycle must not repeat last song of cycle ${cycle - 1}",
                    previousCycleLastId,
                    firstOfNewCycle
                )
                cyclePlayed.add(firstOfNewCycle)
            }
            repeat(4) {
                assertEquals(
                    "Played songs must be excluded from remaining shuffle queue",
                    5 - cyclePlayed.size,
                    MusicController.smartShuffleRemainingIdsForTesting().size
                )
                assertTrue(
                    MusicController.smartShuffleRemainingIdsForTesting()
                        .intersect(cyclePlayed.toSet())
                        .isEmpty()
                )
                MusicController.next()
                cyclePlayed.add(MusicController.state.value.currentSong!!.id)
            }
            assertEquals("Every song in 5-song playlist must play once per cycle", 5, cyclePlayed.toSet().size)
            previousCycleLastId = cyclePlayed.last()
        }

        // 4. Large 300-song playlist across 2 full cycles (600 plays)
        val largePlaylist = makeSyntheticSongs(300, idBase = 5000L)
        MusicController.playSong(largePlaylist[0], largePlaylist)

        val cycle1Order = mutableListOf(MusicController.state.value.currentSong!!.id)
        repeat(299) {
            MusicController.next()
            cycle1Order.add(MusicController.state.value.currentSong!!.id)
        }
        assertEquals(300, cycle1Order.size)
        assertEquals("Large 300-song cycle 1 must have 0 repeated songs", 300, cycle1Order.toSet().size)
        assertEquals(0, MusicController.smartShuffleRemainingIdsForTesting().size)
        assertNotEquals(
            "300-song cycle 1 must be genuinely randomized",
            largePlaylist.map { it.id },
            cycle1Order
        )

        // Advance into Cycle 2 automatically
        MusicController.next()
        assertEquals(1, MusicController.smartShuffleCompletedCyclesForTesting())
        val cycle2Order = mutableListOf(MusicController.state.value.currentSong!!.id)
        assertNotEquals(
            "Cycle 2 must not immediately repeat the 300th song of Cycle 1",
            cycle1Order.last(),
            cycle2Order.first()
        )
        repeat(299) {
            MusicController.next()
            cycle2Order.add(MusicController.state.value.currentSong!!.id)
        }
        assertEquals("Large 300-song cycle 2 must have 0 repeated songs", 300, cycle2Order.toSet().size)
        assertNotEquals("Cycle 2 must generate a fresh random order distinct from Cycle 1", cycle1Order, cycle2Order)
    }

    @Test
    fun smartShuffle_nextAndPreviousControls_walkExactPlayHistoryAndSelectNextUnplayedSong() {
        val playlist = makeSyntheticSongs(10, idBase = 300L)
        MusicController.restorePlaybackModes(isShuffle = true, repeatState = RepeatState.ALL, isReverseQueue = false)
        MusicController.playSong(playlist[0], playlist)

        val visitedFirstFive = mutableListOf(MusicController.state.value.currentSong!!.id)
        repeat(4) {
            MusicController.next()
            visitedFirstFive.add(MusicController.state.value.currentSong!!.id)
        }
        assertEquals(5, visitedFirstFive.toSet().size)

        // Step all the way back through the 4 previous tracks using Previous
        for (idx in 3 downTo 0) {
            MusicController.previous()
            assertEquals(
                "Previous must return to exact previously played track at step $idx",
                visitedFirstFive[idx],
                MusicController.state.value.currentSong?.id
            )
        }

        // Pressing Previous again at the start of shuffle history keeps the initial song instead of picking randomly
        MusicController.previous()
        assertEquals(visitedFirstFive[0], MusicController.state.value.currentSong?.id)

        // Pressing Next now must select the remaining 5 UNPLAYED songs without repeating any of visitedFirstFive!
        val remainingFivePlayed = mutableListOf<Long>()
        repeat(5) {
            MusicController.next()
            val nextId = MusicController.state.value.currentSong!!.id
            assertFalse(
                "Next after Previous must pick an unplayed song in the cycle, never repeating $visitedFirstFive",
                nextId in visitedFirstFive
            )
            remainingFivePlayed.add(nextId)
        }
        assertEquals(5, remainingFivePlayed.toSet().size)
        assertEquals(
            playlist.map { it.id }.toSet(),
            (visitedFirstFive + remainingFivePlayed).toSet()
        )
    }

    @Test
    fun smartShuffle_dynamicAddAndRemoveMidCycle_updatesRemainingQueueWithoutRepeatingPlayedSongs() {
        val initialSix = makeSyntheticSongs(6, idBase = 700L) // 700..705
        val addedThree = makeSyntheticSongs(3, idBase = 800L) // 800..802

        MusicController.restorePlaybackModes(isShuffle = true, repeatState = RepeatState.ALL, isReverseQueue = false)
        MusicController.playSong(initialSix[0], initialSix, sourceType = QueueSourceType.PLAYLIST, sourcePlaylistId = 42L)

        // Play 3 songs total: P0 -> P1 -> P2
        val playedSoFar = mutableListOf(MusicController.state.value.currentSong!!.id)
        MusicController.next()
        playedSoFar.add(MusicController.state.value.currentSong!!.id)
        MusicController.next()
        playedSoFar.add(MusicController.state.value.currentSong!!.id)
        assertEquals(3, playedSoFar.toSet().size)

        val activeAtMutation = playedSoFar.last()
        val removedPlayedId = playedSoFar[0]
        val unplayedBeforeMutation = MusicController.smartShuffleRemainingIdsForTesting()
        assertEquals(3, unplayedBeforeMutation.size)
        val removedUnplayedId = unplayedBeforeMutation.first()

        // Mutate playlist mid-cycle: remove 1 already-played + 1 unplayed song, add 3 new songs
        val updatedPlaylist = initialSix.filterNot {
            it.id == removedPlayedId || it.id == removedUnplayedId
        } + addedThree

        MusicController.updateActiveQueueSongs(updatedPlaylist)

        // Active playback must remain on activeAtMutation uninterrupted
        assertEquals(activeAtMutation, MusicController.state.value.currentSong?.id)
        assertTrue(MusicController.state.value.isPlaying)

        // Remaining pool must contain the 2 surviving unplayed songs + 3 newly added songs = 5 songs,
        // and must NOT contain any already-played or removed songs!
        val remainingAfterMutation = MusicController.smartShuffleRemainingIdsForTesting()
        assertEquals(5, remainingAfterMutation.size)
        assertFalse(removedUnplayedId in remainingAfterMutation)
        assertFalse(removedPlayedId in remainingAfterMutation)
        assertFalse(playedSoFar[1] in remainingAfterMutation)
        assertFalse(activeAtMutation in remainingAfterMutation)
        assertTrue(addedThree.all { it.id in remainingAfterMutation })

        // Play through the remaining 5 songs of the cycle: zero repeats of playedSoFar and all 3 new songs play!
        val playedAfterMutation = mutableListOf<Long>()
        repeat(5) {
            MusicController.next()
            val id = MusicController.state.value.currentSong!!.id
            assertFalse("Already-played song $id must not repeat after queue update", id in playedSoFar)
            assertNotEquals("Removed song must never play", removedUnplayedId, id)
            assertNotEquals("Removed song must never play", removedPlayedId, id)
            playedAfterMutation.add(id)
        }
        assertEquals(5, playedAfterMutation.toSet().size)
        assertTrue(addedThree.all { it.id in playedAfterMutation })
    }

    @Test
    fun smartShuffle_pauseResumeNavigationBackgroundAndAppRestart_preservesShuffleHistoryAndRemainingCycle() {
        val context = RuntimeEnvironment.getApplication()
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()
        var media3Controller: MediaController? = null

        val playlist = (0 until 8).map { idx ->
            val f = createPcmWavFile(File(context.cacheDir, "restart_shuf_$idx.wav"), durationMs = 10_000, freqStep = idx + 1)
            Song(
                id = 900L + idx,
                title = "Continuity Track $idx",
                artist = "DJ Continuity",
                album = "Session",
                durationMs = 10_000L,
                sizeBytes = f.length(),
                dateAdded = 1000L + idx,
                contentUri = Uri.fromFile(f),
                fileName = f.name
            )
        }

        try {
            val session = service.currentMediaSession!!
            val future = MediaController.Builder(context, session.token).buildAsync()
            awaitCondition("MediaController connects") { future.isDone }
            media3Controller = future.get()

            MusicController.restorePlaybackModes(isShuffle = true, repeatState = RepeatState.ALL, isReverseQueue = false)
            MusicController.playSong(playlist[0], playlist, sourceType = QueueSourceType.LIBRARY)
            awaitCondition("Initial shuffled track ready") { media3Controller.playbackState == Player.STATE_READY }

            val cycleVisited = mutableListOf(MusicController.state.value.currentSong!!.id)

            // Play 2 more via Next, then pause/resume + language change (simulating screen navigation), then 1 via MediaSession Next
            MusicController.next()
            cycleVisited.add(MusicController.state.value.currentSong!!.id)

            MusicController.pause()
            MusicController.setLanguage(AppLanguage.INDONESIAN)
            MusicController.resume()
            assertEquals("Pause/resume/navigation must not reset shuffle history", 2, MusicController.smartShuffleCyclePlayedIdsForTesting().size)

            media3Controller.seekToNext()
            shadowOf(Looper.getMainLooper()).idle()
            cycleVisited.add(MusicController.state.value.currentSong!!.id)

            MusicController.next()
            cycleVisited.add(MusicController.state.value.currentSong!!.id)
            assertEquals(4, cycleVisited.toSet().size)
            assertEquals(4, MusicController.smartShuffleRemainingIdsForTesting().size)

            val trackBeforeRestart = cycleVisited[3]
            val prevTrackBeforeRestart = cycleVisited[2]
            MusicController.seekTo(4_200L)
            MusicController.flushPlaybackSnapshotNow()
        } finally {
            media3Controller?.release()
            serviceController.destroy()
        }

        // Simulate full app closure / process death and cold reopen
        MusicController.resetInMemoryStateForTesting()

        val savedModes = SettingsRepository.getSavedMusicModes(context)
        val savedSnapshot = SettingsRepository.getSavedMusicSessionSnapshot(context)
        assertNotNull(savedSnapshot)
        assertTrue(savedModes.isShuffle)
        assertEquals(4, savedSnapshot!!.shufflePlayedIds.size)
        assertEquals(4, savedSnapshot.shuffleRemainingIds.size)

        MusicController.bindAppContext(context)
        MusicController.restorePlaybackModes(
            isShuffle = savedModes.isShuffle,
            repeatState = savedModes.repeatState,
            isReverseQueue = savedModes.isReverseQueue
        )
        val songsById = playlist.associateBy { it.id }
        val restoredSong = songsById.getValue(savedSnapshot.songId)
        val restoredQueue = savedSnapshot.queueSongIds.mapNotNull { songsById[it] }
        MusicController.restorePausedSession(
            song = restoredSong,
            queue = restoredQueue,
            positionMs = savedSnapshot.positionMs,
            savedShufflePlayedIds = savedSnapshot.shufflePlayedIds,
            savedShuffleRemainingIds = savedSnapshot.shuffleRemainingIds,
            savedShuffleHistoryIds = savedSnapshot.shuffleHistoryIds,
            sourceType = QueueSourceType.fromNameOrDefault(savedSnapshot.queueSourceType),
            sourcePlaylistId = savedSnapshot.queueSourcePlaylistId
        )

        assertFalse("Restored session must stay paused until user resumes", MusicController.state.value.isPlaying)
        assertEquals(4_200L, MusicController.state.value.currentPositionMs)

        // Verify Previous works across cold restart!
        val expectedHistoryTop = savedSnapshot.shuffleHistoryIds.last()
        MusicController.previous()
        assertEquals(
            "Previous after app restart must return to the pre-restart previously played track",
            expectedHistoryTop,
            MusicController.state.value.currentSong?.id
        )

        // Finish the remaining 4 unplayed songs of the 8-song shuffle cycle without any pre-restart repeats!
        val postRestartPlayed = mutableListOf<Long>()
        repeat(4) {
            MusicController.next()
            val id = MusicController.state.value.currentSong!!.id
            assertFalse(
                "Post-restart Next must not repeat pre-restart played tracks ${savedSnapshot.shufflePlayedIds}",
                id in savedSnapshot.shufflePlayedIds
            )
            postRestartPlayed.add(id)
        }
        assertEquals(4, postRestartPlayed.toSet().size)
        assertEquals(
            playlist.map { it.id }.toSet(),
            (savedSnapshot.shufflePlayedIds + postRestartPlayed).toSet()
        )
    }

    @Test
    fun djModeWithSmartShuffle_transitionsToSmartShuffleSelection_marksPlayedOnce_andHandlesCycleWrap() {
        val context = RuntimeEnvironment.getApplication()
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()

        val wavPlaylist = (0 until 4).map { idx ->
            val f = createPcmWavFile(
                File(context.cacheDir, "dj_smart_shuf_$idx.wav"),
                durationMs = DJ_SHUFFLE_WAV_DURATION_MS.toInt(),
                freqStep = idx + 1
            )
            Song(
                id = 1200L + idx,
                title = "DJ Smart Shuffle $idx",
                artist = "DJ Mix",
                album = "Club",
                durationMs = DJ_SHUFFLE_WAV_DURATION_MS,
                sizeBytes = f.length(),
                dateAdded = 2000L + idx,
                contentUri = Uri.fromFile(f),
                fileName = f.name
            )
        }

        try {
            MusicController.restorePlaybackModes(isShuffle = true, repeatState = RepeatState.ALL, isReverseQueue = false)
            MusicController.setDjModeSettings(enabled = true, transitionSeconds = 5)
            MusicController.playSong(wavPlaylist[0], wavPlaylist)

            awaitCondition("First track ready") {
                MusicController.activeDeckForTesting()?.playbackState == Player.STATE_READY &&
                    MusicController.state.value.isPlaying
            }

            val cycle1Tracks = mutableListOf(MusicController.state.value.currentSong!!.id)
            assertEquals(listOf(wavPlaylist[0].id), MusicController.smartShuffleCyclePlayedIdsForTesting())
            val expectedSecondId = MusicController.smartShuffleRemainingIdsForTesting().first()

            // 1. Trigger advance preload at 7.5s remaining (2.5s before 5s crossfade):
            //    Must preload expectedSecondId WITHOUT marking it played yet!
            MusicController.evaluatePlaybackPositionForDjTransition(
                DJ_SHUFFLE_WAV_DURATION_MS - 7_500L,
                DJ_SHUFFLE_WAV_DURATION_MS
            )
            awaitCondition("Standby deck ready for Smart Shuffle candidate") {
                MusicController.standbyDeckForTesting()?.playbackState == Player.STATE_READY
            }
            assertEquals(
                expectedSecondId,
                MusicController.standbyDeckForTesting()?.currentMediaItem?.mediaId?.toLongOrNull()
            )
            assertEquals(
                "Advance preload must NOT mark incoming song as played before crossfade activates",
                listOf(wavPlaylist[0].id),
                MusicController.smartShuffleCyclePlayedIdsForTesting()
            )
            assertEquals(0, MusicController.smartShuffleActivationCountForTesting())

            // 2. Crossfade through the remaining 3 songs of Cycle 1 (total 4 distinct tracks)
            repeat(3) { step ->
                if (step > 0) {
                    MusicController.evaluatePlaybackPositionForDjTransition(
                        DJ_SHUFFLE_WAV_DURATION_MS - 7_500L,
                        DJ_SHUFFLE_WAV_DURATION_MS
                    )
                    awaitCondition("Standby deck ready for Smart Shuffle step $step") {
                        MusicController.standbyDeckForTesting()?.playbackState == Player.STATE_READY
                    }
                }
                val activationsBefore = MusicController.smartShuffleActivationCountForTesting()
                val expectedIncoming = MusicController.smartShuffleRemainingIdsForTesting().first()

                MusicController.evaluatePlaybackPositionForDjTransition(
                    DJ_SHUFFLE_WAV_DURATION_MS - 5_000L,
                    DJ_SHUFFLE_WAV_DURATION_MS
                )
                shadowOf(Looper.getMainLooper()).idle()
                awaitCondition("DJ crossfade step $step activates Smart Shuffle track $expectedIncoming") {
                    MusicController.state.value.currentSong?.id == expectedIncoming &&
                        MusicController.state.value.isDjTransitionActive
                }

                // Advance 7.0s so crossfade completes cleanly without entering next song's crossfade window
                advancePlaybackClock(7_000L)
                awaitCondition("DJ crossfade step $step completes cleanly") {
                    !MusicController.state.value.isDjTransitionActive
                }
                assertEquals(
                    "Crossfade must mark incoming track as played ONCE (never twice)",
                    activationsBefore + 1,
                    MusicController.smartShuffleActivationCountForTesting()
                )
                assertTrue(MusicController.simultaneouslyActiveDeckCountForTesting() <= 2)
                cycle1Tracks.add(MusicController.state.value.currentSong!!.id)
            }

            assertEquals("All 4 tracks must play once during DJ + Smart Shuffle Cycle 1", 4, cycle1Tracks.toSet().size)
            assertEquals(0, MusicController.smartShuffleRemainingIdsForTesting().size)

            // 3. Crossfade across cycle boundary into Cycle 2: must NOT repeat Cycle 1's last song!
            val lastOfCycle1 = cycle1Tracks.last()
            MusicController.evaluatePlaybackPositionForDjTransition(
                DJ_SHUFFLE_WAV_DURATION_MS - 7_500L,
                DJ_SHUFFLE_WAV_DURATION_MS
            )
            awaitCondition("Standby deck ready for Cycle 2 boundary candidate") {
                MusicController.standbyDeckForTesting()?.playbackState == Player.STATE_READY
            }
            MusicController.evaluatePlaybackPositionForDjTransition(
                DJ_SHUFFLE_WAV_DURATION_MS - 5_000L,
                DJ_SHUFFLE_WAV_DURATION_MS
            )
            shadowOf(Looper.getMainLooper()).idle()
            awaitCondition("DJ crossfade wraps into Smart Shuffle Cycle 2") {
                MusicController.smartShuffleCompletedCyclesForTesting() == 1 &&
                    MusicController.state.value.isDjTransitionActive
            }
            advancePlaybackClock(7_000L)
            awaitCondition("Cycle 2 DJ crossfade completes cleanly") {
                !MusicController.state.value.isDjTransitionActive
            }
            val firstOfCycle2 = MusicController.state.value.currentSong!!.id
            assertNotEquals(
                "DJ Crossfade into Cycle 2 must avoid repeating the last song of Cycle 1",
                lastOfCycle1,
                firstOfCycle2
            )

            // 4. Pressing Previous after DJ crossfade returns directly to lastOfCycle1!
            MusicController.previous()
            assertEquals(lastOfCycle1, MusicController.state.value.currentSong?.id)
        } finally {
            serviceController.destroy()
        }
    }

    private fun makeSyntheticSongs(count: Int, idBase: Long): List<Song> {
        return (0 until count).map { idx ->
            val id = idBase + idx
            Song(
                id = id,
                title = "Track $id",
                artist = "Artist ${idx % 7}",
                album = "Album ${idx % 5}",
                durationMs = 180_000L,
                sizeBytes = 5_000_000L,
                dateAdded = 10_000L + idx,
                contentUri = Uri.parse("content://media/external/audio/media/$id"),
                fileName = "track_$id.mp3"
            )
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
                Thread.sleep(3L)
                shadowOf(Looper.getMainLooper()).idle()
            }
            advanced += delta
            stepIndex++
        }
    }

    private fun awaitCondition(message: String, timeoutMs: Long = 5_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (predicate()) return
            if (!MusicController.state.value.isDjTransitionActive) {
                ShadowSystemClock.advanceBy(Duration.ofMillis(20))
            }
            Thread.sleep(5L)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(message, predicate())
    }

    private fun createPcmWavFile(target: File, durationMs: Int, freqStep: Int): File {
        createdFiles.add(target)
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

        FileOutputStream(target).use { out ->
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
        return target
    }
}
