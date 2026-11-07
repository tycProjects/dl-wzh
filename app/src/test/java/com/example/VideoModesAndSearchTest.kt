package com.example

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.media3.common.util.UnstableApi
import com.example.data.model.Song
import com.example.data.model.VideoItem
import com.example.data.repository.AppLanguage
import com.example.data.repository.MediaSearchMatcher
import com.example.data.repository.MediaStoreRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SortOption
import com.example.data.repository.ThemeColor
import com.example.playback.MusicController
import com.example.playback.PlaybackState
import com.example.playback.VideoPlaybackController
import com.example.playback.VideoPlaybackMode
import com.example.playback.VideoQueueMode
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.VideoScreen
import com.example.ui.theme.JnmfTheme
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
class VideoModesAndSearchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testVideos = listOf(
        VideoItem(
            id = 701L,
            title = "JNMF Stage Set 01",
            fileName = "JNMF_Stage_Set_01.mp4",
            durationMs = 120_000L,
            sizeBytes = 25L * 1024L * 1024L,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/external/video/media/701")
        ),
        VideoItem(
            id = 702L,
            title = "JNMF Stage Set 02",
            fileName = "JNMF Stage Set 02.mp4",
            durationMs = 150_000L,
            sizeBytes = 30L * 1024L * 1024L,
            dateAdded = 2000L,
            contentUri = Uri.parse("content://media/external/video/media/702")
        ),
        VideoItem(
            id = 703L,
            title = "JNMF Stage Set 03",
            fileName = "JNMF_Stage_Set_03.mp4",
            durationMs = 180_000L,
            sizeBytes = 35L * 1024L * 1024L,
            dateAdded = 3000L,
            contentUri = Uri.parse("content://media/external/video/media/703")
        ),
        VideoItem(
            id = 704L,
            title = "JNMF Stage Set 04",
            fileName = "JNMF_Stage_Set_04.mp4",
            durationMs = 210_000L,
            sizeBytes = 40L * 1024L * 1024L,
            dateAdded = 4000L,
            contentUri = Uri.parse("content://media/external/video/media/704")
        )
    )

    @Before
    fun setUp() {
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        VideoPlaybackController.setPlaybackMode(VideoPlaybackMode.NORMAL_VIDEO)
        VideoPlaybackController.setQueueMode(VideoQueueMode.PLAY_ONCE)
        VideoPlaybackController.setShuffleRandomForTesting(Random(42L))
        VideoPlaybackController.setLanguage(AppLanguage.ENGLISH)
        MusicController.setLanguage(AppLanguage.ENGLISH)
        MusicController.pause()
    }

    @After
    fun tearDown() {
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        VideoPlaybackController.setQueueMode(VideoQueueMode.PLAY_ONCE)
    }

    @Test
    fun task1_playOnceStopsAfterOnePlaybackAndIgnoresDuplicateEndEvents() {
        VideoPlaybackController.setQueueMode(VideoQueueMode.PLAY_ONCE)
        VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 45_000L)
        assertTrue(VideoPlaybackController.state.value.isPlaying)
        assertEquals(701L, VideoPlaybackController.state.value.currentVideo?.id)

        // End of video in Play Once stops playback cleanly at 0ms without advancing to video #2
        VideoPlaybackController.handleVideoEnded()
        val afterEnd = VideoPlaybackController.state.value
        assertFalse("Play Once must stop playback when video finishes", afterEnd.isPlaying)
        assertEquals("Play Once must remain on the selected video", 701L, afterEnd.currentVideo?.id)
        assertEquals(0L, afterEnd.currentPositionMs)

        // Duplicate end-of-video callback must be a safe no-op
        VideoPlaybackController.handleVideoEnded()
        assertFalse(VideoPlaybackController.state.value.isPlaying)
        assertEquals(701L, VideoPlaybackController.state.value.currentVideo?.id)
    }

    @Test
    fun task1_repeatOneReplaysCurrentVideoInMultiAndSingleVideoQueues() {
        VideoPlaybackController.setQueueMode(VideoQueueMode.REPEAT_ONE)
        VideoPlaybackController.playVideo(testVideos[1], testVideos, startPositionMs = 90_000L)

        VideoPlaybackController.handleVideoEnded()
        val stateAfterEnd = VideoPlaybackController.state.value
        assertTrue("Repeat One must automatically continue playing", stateAfterEnd.isPlaying)
        assertEquals("Repeat One must replay the same video", 702L, stateAfterEnd.currentVideo?.id)
        assertEquals("Repeat One must restart from 0ms", 0L, stateAfterEnd.currentPositionMs)

        // Single-video queue in Repeat One also replays cleanly
        val singleQueue = listOf(testVideos[0])
        VideoPlaybackController.playVideo(testVideos[0], singleQueue, startPositionMs = 60_000L)
        VideoPlaybackController.handleVideoEnded()
        assertTrue(VideoPlaybackController.state.value.isPlaying)
        assertEquals(701L, VideoPlaybackController.state.value.currentVideo?.id)
        assertEquals(0L, VideoPlaybackController.state.value.currentPositionMs)
    }

    @Test
    fun task1_repeatAllWrapsToBeginningAfterLastVideoAndHandlesSingleVideo() {
        VideoPlaybackController.setQueueMode(VideoQueueMode.REPEAT_ALL)
        // Start on the last video in the list (index 3 -> id 704)
        VideoPlaybackController.playVideo(testVideos[3], testVideos, startPositionMs = 200_000L)
        assertEquals(704L, VideoPlaybackController.state.value.currentVideo?.id)

        // Ending on last video wraps to first video (id 701) and keeps playing
        VideoPlaybackController.handleVideoEnded()
        val wrappedState = VideoPlaybackController.state.value
        assertTrue("Repeat All must continue playing after wrapping", wrappedState.isPlaying)
        assertEquals("Repeat All must wrap from last video to first video", 701L, wrappedState.currentVideo?.id)
        assertEquals(0L, wrappedState.currentPositionMs)

        // Ending on first video advances sequentially to second video (id 702)
        VideoPlaybackController.handleVideoEnded()
        assertEquals(702L, VideoPlaybackController.state.value.currentVideo?.id)
        assertTrue(VideoPlaybackController.state.value.isPlaying)

        // Single-video queue in Repeat All replays the single video gracefully
        val singleQueue = listOf(testVideos[2])
        VideoPlaybackController.playVideo(testVideos[2], singleQueue, startPositionMs = 50_000L)
        VideoPlaybackController.handleVideoEnded()
        assertTrue(VideoPlaybackController.state.value.isPlaying)
        assertEquals(703L, VideoPlaybackController.state.value.currentVideo?.id)
    }

    @Test
    fun task1_shuffleSelectsRandomizedNonRepeatingOrderAndSupportsHistoryPrevious() {
        VideoPlaybackController.setShuffleRandomForTesting(Random(2026L))
        VideoPlaybackController.setQueueMode(VideoQueueMode.SHUFFLE)
        VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 0L)

        val visitedIds = mutableListOf(VideoPlaybackController.state.value.currentVideo!!.id)
        // Step through 12 auto-ended transitions across multiple shuffle rounds
        repeat(12) {
            val prevId = VideoPlaybackController.state.value.currentVideo!!.id
            VideoPlaybackController.handleVideoEnded()
            val nextId = VideoPlaybackController.state.value.currentVideo!!.id
            assertNotEquals(
                "Shuffle must not select the same video consecutively when other videos exist",
                prevId,
                nextId
            )
            assertTrue(VideoPlaybackController.state.value.isPlaying)
            visitedIds.add(nextId)
        }

        // First full round of 4 videos must cover all 4 distinct videos
        assertEquals(4, visitedIds.take(4).toSet().size)
        // Confirm order is genuinely shuffled rather than sequential [701, 702, 703, 704, 701, 702, 703, 704]
        val sequentialOrder = listOf(701L, 702L, 703L, 704L, 701L, 702L, 703L, 704L)
        assertNotEquals(sequentialOrder, visitedIds.take(8))

        // Next and Previous in Shuffle mode follow shuffle order and shuffle history
        val beforeNextId = VideoPlaybackController.state.value.currentVideo!!.id
        VideoPlaybackController.next()
        val afterNextId = VideoPlaybackController.state.value.currentVideo!!.id
        assertNotEquals(beforeNextId, afterNextId)

        VideoPlaybackController.previous()
        assertEquals(
            "Previous in Shuffle mode must return to the previously played shuffled video",
            beforeNextId,
            VideoPlaybackController.state.value.currentVideo?.id
        )

        // Single-video queue in Shuffle mode handles end/next/previous gracefully without looping infinitely
        val singleQueue = listOf(testVideos[0])
        VideoPlaybackController.playVideo(testVideos[0], singleQueue, startPositionMs = 10_000L)
        VideoPlaybackController.next()
        assertEquals(701L, VideoPlaybackController.state.value.currentVideo?.id)
        VideoPlaybackController.previous()
        assertEquals(701L, VideoPlaybackController.state.value.currentVideo?.id)
        VideoPlaybackController.handleVideoEnded()
        assertFalse("Single-video Shuffle stops cleanly at end when no other videos exist", VideoPlaybackController.state.value.isPlaying)
    }

    @Test
    fun task1_nextAndPreviousSequentialModesModeButtonPersistenceAndLockPreservation() {
        val context = RuntimeEnvironment.getApplication()
        val settingsRepo = SettingsRepository(context)

        // Sequential Next/Previous in Play Once / Repeat All / Repeat One
        VideoPlaybackController.setQueueMode(VideoQueueMode.PLAY_ONCE)
        VideoPlaybackController.playVideo(testVideos[0], testVideos, startPositionMs = 33_000L)
        VideoPlaybackController.previous() // wraps 0 -> 3 (704)
        assertEquals(704L, VideoPlaybackController.state.value.currentVideo?.id)
        VideoPlaybackController.next() // wraps 3 -> 0 (701)
        assertEquals(701L, VideoPlaybackController.state.value.currentVideo?.id)

        // Seek to 48s so we can verify changing modes preserves playback position, fullscreen, and lock
        VideoPlaybackController.seekTo(48_000L)
        VideoPlaybackController.setPlayerUiVisible(true)

        var isFullscreen by mutableStateOf(false)

        composeTestRule.setContent {
            JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                    VideoScreen(
                        videos = testVideos,
                        isFullscreen = isFullscreen,
                        onFullscreenChanged = { isFullscreen = it },
                        isPlayerExpanded = true,
                        onSelectQueueMode = { mode ->
                            VideoPlaybackController.setQueueMode(mode)
                            runBlocking { settingsRepo.setVideoQueueMode(mode) }
                        },
                        language = AppLanguage.ENGLISH
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Verify initial Play Once button label & description in embedded controls
        composeTestRule.onNodeWithTag("video_queue_mode_button")
            .assertIsDisplayed()
            .assertTextEquals(JnmfStrings.videoQueueModeLabel(VideoQueueMode.PLAY_ONCE, AppLanguage.ENGLISH))
        composeTestRule.onNodeWithTag("video_queue_mode_description")
            .assertIsDisplayed()
            .assertTextEquals(JnmfStrings.videoQueueModeDescription(VideoQueueMode.PLAY_ONCE, AppLanguage.ENGLISH))

        // Cycle: Play Once -> Repeat One -> Shuffle -> Repeat All -> Play Once
        composeTestRule.onNodeWithTag("video_queue_mode_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(VideoQueueMode.REPEAT_ONE, VideoPlaybackController.state.value.queueMode)
        assertEquals(48_000L, VideoPlaybackController.state.value.currentPositionMs)
        assertEquals(VideoQueueMode.REPEAT_ONE, SettingsRepository.getSavedVideoQueueMode(context))
        composeTestRule.onNodeWithTag("video_queue_mode_button")
            .assertTextEquals(JnmfStrings.videoQueueModeLabel(VideoQueueMode.REPEAT_ONE, AppLanguage.ENGLISH))

        composeTestRule.onNodeWithTag("video_queue_mode_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(VideoQueueMode.SHUFFLE, VideoPlaybackController.state.value.queueMode)
        assertEquals(48_000L, VideoPlaybackController.state.value.currentPositionMs)
        assertEquals(VideoQueueMode.SHUFFLE, runBlocking { settingsRepo.videoQueueModeFlow.first() })

        composeTestRule.onNodeWithTag("video_queue_mode_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(VideoQueueMode.REPEAT_ALL, VideoPlaybackController.state.value.queueMode)

        // Enter fullscreen and verify fullscreen mode button cycles and respects video lock
        composeTestRule.onNodeWithTag("btn_enter_fullscreen").performClick()
        composeTestRule.waitForIdle()
        assertTrue(isFullscreen)
        composeTestRule.onNodeWithTag("fullscreen_queue_mode_button")
            .assertIsDisplayed()
            .assertTextEquals(JnmfStrings.videoQueueModeLabel(VideoQueueMode.REPEAT_ALL, AppLanguage.ENGLISH))

        composeTestRule.onNodeWithTag("fullscreen_queue_mode_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(VideoQueueMode.PLAY_ONCE, VideoPlaybackController.state.value.queueMode)
        assertEquals(48_000L, VideoPlaybackController.state.value.currentPositionMs)
        assertTrue("Changing playback mode in fullscreen must keep fullscreen active", isFullscreen)

        // Locking video controls hides/disables fullscreen_queue_mode_button against accidental touches
        composeTestRule.onNodeWithTag("video_lock_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("fullscreen_queue_mode_button").assertCountEquals(0)

        // Unlocking restores fullscreen_queue_mode_button without resetting position
        composeTestRule.onNodeWithTag("video_unlock_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fullscreen_queue_mode_button").assertIsDisplayed()
        assertEquals(48_000L, VideoPlaybackController.state.value.currentPositionMs)
    }

    @Test
    fun task2_searchNormalizationTreatsSpacesAndUnderscoresAsEquivalentInMatcherAndMediaStore() {
        // 1. Filename with underscores found using space-separated query
        assertTrue(
            MediaSearchMatcher.matchesSong(
                query = "DJ Random Indo Remix",
                title = "Track 01",
                fileName = "DJ_Random_Indo_Remix.mp3",
                artist = "Unknown Artist",
                album = "Club Mixes"
            )
        )

        // 2. Filename with spaces found using underscore-separated query
        assertTrue(
            MediaSearchMatcher.matchesSong(
                query = "DJ_Random_Indo_Remix",
                title = "Track 02",
                fileName = "DJ Random Indo Remix.mp3",
                artist = "Unknown Artist",
                album = "Club Mixes"
            )
        )

        // 3. Case-insensitivity, multiple consecutive underscores/spaces, and artist/album metadata preserved
        assertTrue(
            MediaSearchMatcher.matchesSong(
                query = "  dj__random   indo_remix ",
                title = "DJ_Random_Indo_Remix",
                fileName = "DJ_Random_Indo_Remix.mp3",
                artist = "JNMF",
                album = "Single"
            )
        )
        assertTrue(
            MediaSearchMatcher.matchesSong(
                query = "dj_indo_artist",
                title = "Bass Drop",
                fileName = "bass_drop.flac",
                artist = "DJ Indo Artist",
                album = "Night Session"
            )
        )
        assertFalse(
            MediaSearchMatcher.matchesSong(
                query = "Acoustic Ballad",
                title = "DJ_Random_Indo_Remix",
                fileName = "DJ_Random_Indo_Remix.mp3",
                artist = "DJ Indo",
                album = "Club"
            )
        )

        // 4. Video search normalization also works consistently
        assertTrue(
            MediaSearchMatcher.matchesVideo(
                query = "JNMF Stage Set 01",
                title = "JNMF_Stage_Set_01",
                fileName = "JNMF_Stage_Set_01.mp4"
            )
        )
        assertTrue(
            MediaSearchMatcher.matchesVideo(
                query = "JNMF_Stage_Set_02",
                title = "JNMF Stage Set 02",
                fileName = "JNMF Stage Set 02.mp4"
            )
        )

        // 5. Verify MediaStoreRepository end-to-end query via ContentProvider preserves original titles & filenames
        Robolectric.buildContentProvider(FakeMediaStoreProvider::class.java)
            .create("media")
        val repo = MediaStoreRepository(RuntimeEnvironment.getApplication())

        val spaceSearchResults = runBlocking {
            repo.queryLocalSongs(
                sortOption = SortOption.NAME_ASC,
                searchQuery = "DJ Random Indo Remix"
            )
        }
        assertEquals(2, spaceSearchResults.size)
        val underscoreSong = spaceSearchResults.first { it.id == 9001L }
        assertEquals("Original title must remain unchanged", "DJ_Random_Indo_Remix", underscoreSong.title)
        assertEquals("Original filename must remain unchanged", "DJ_Random_Indo_Remix.mp3", underscoreSong.fileName)

        val underscoreSearchResults = runBlocking {
            repo.queryLocalSongs(
                sortOption = SortOption.NAME_ASC,
                searchQuery = "DJ_Random_Indo_Remix"
            )
        }
        assertEquals(2, underscoreSearchResults.size)
        val spaceSong = underscoreSearchResults.first { it.id == 9002L }
        assertEquals("Original title with spaces must remain unchanged", "DJ Random Indo Remix", spaceSong.title)
        assertEquals("Original filename with spaces must remain unchanged", "DJ Random Indo Remix.mp3", spaceSong.fileName)

        // Artist metadata search continues to work
        val artistResults = runBlocking {
            repo.queryLocalSongs(
                sortOption = SortOption.NAME_ASC,
                searchQuery = "Cebu Bass Crew"
            )
        }
        assertEquals(1, artistResults.size)
        assertEquals(9003L, artistResults[0].id)

        // Video repository search also matches spaces <-> underscores while preserving original title/fileName
        val videoResults = runBlocking {
            repo.queryLocalVideos(searchQuery = "Live Crowd Clip")
        }
        assertEquals(1, videoResults.size)
        assertEquals("Live_Crowd_Clip", videoResults[0].title)
        assertEquals("Live_Crowd_Clip.mp4", videoResults[0].fileName)
    }

    @Test
    fun task2_libraryScreenDisplaysUnmodifiedTitlesWhenSearchingSpacesOrUnderscores() {
        val sampleSongs = listOf(
            Song(
                id = 1L,
                title = "DJ_Random_Indo_Remix",
                fileName = "DJ_Random_Indo_Remix.mp3",
                artist = "DJ Putra",
                album = "Indo Club",
                durationMs = 210_000L,
                sizeBytes = 6_500_000L,
                dateAdded = 100L,
                contentUri = Uri.parse("content://media/external/audio/media/1")
            ),
            Song(
                id = 2L,
                title = "Bisaya Party Anthem",
                fileName = "Bisaya Party Anthem.mp3",
                artist = "DJ Cebu",
                album = "Visayas Mix",
                durationMs = 195_000L,
                sizeBytes = 5_800_000L,
                dateAdded = 200L,
                contentUri = Uri.parse("content://media/external/audio/media/2")
            )
        )

        var query by mutableStateOf("")

        composeTestRule.setContent {
            JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                    LibraryScreen(
                        songs = sampleSongs,
                        playbackState = PlaybackState(),
                        hasPermission = true,
                        onRequestPermission = {},
                        onRefresh = {},
                        onSortSelected = {},
                        currentSort = SortOption.NAME_ASC,
                        searchQuery = query,
                        onSearchQueryChange = { query = it },
                        favoriteSongIds = emptySet(),
                        onToggleFavorite = {},
                        onAddToPlaylist = {},
                        onHideSong = {},
                        language = AppLanguage.ENGLISH
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Search with spaces -> finds song whose title/filename uses underscores, displaying original underscore title
        composeTestRule.onNodeWithTag("search_field").performTextReplacement("DJ Random Indo Remix")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DJ_Random_Indo_Remix").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Bisaya Party Anthem").assertCountEquals(0)

        // Search with underscores -> finds song whose title/filename uses spaces, displaying original space title
        composeTestRule.onNodeWithTag("search_field").performTextReplacement("Bisaya_Party_Anthem")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bisaya Party Anthem").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("DJ_Random_Indo_Remix").assertCountEquals(0)
    }

    class FakeMediaStoreProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val uriStr = uri.toString()
            return if (uriStr.contains("video")) {
                val cols = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DATE_ADDED
                )
                MatrixCursor(cols).apply {
                    addRow(arrayOf<Any?>(8001L, "Live_Crowd_Clip", "Live_Crowd_Clip.mp4", 95_000L, 12_000_000L, 500L))
                    addRow(arrayOf<Any?>(8002L, "Backstage Intro", "Backstage Intro.mp4", 45_000L, 6_000_000L, 600L))
                }
            } else {
                val cols = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                MatrixCursor(cols).apply {
                    addRow(
                        arrayOf<Any?>(
                            9001L,
                            "DJ_Random_Indo_Remix",
                            "DJ_Random_Indo_Remix.mp3",
                            "DJ Indo",
                            "Remix Vol 1",
                            240_000L,
                            7_200_000L,
                            1000L,
                            11L
                        )
                    )
                    addRow(
                        arrayOf<Any?>(
                            9002L,
                            "DJ Random Indo Remix",
                            "DJ Random Indo Remix.mp3",
                            "DJ Indo",
                            "Remix Vol 2",
                            245_000L,
                            7_400_000L,
                            1001L,
                            12L
                        )
                    )
                    addRow(
                        arrayOf<Any?>(
                            9003L,
                            "Budots Festival",
                            "Budots_Festival_2026.mp3",
                            "Cebu Bass Crew",
                            "Street Beats",
                            180_000L,
                            5_500_000L,
                            1002L,
                            13L
                        )
                    )
                }
            }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int = 0
    }
}
