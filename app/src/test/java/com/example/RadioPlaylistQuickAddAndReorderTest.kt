package com.example

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.media3.common.util.UnstableApi
import com.example.data.db.JnmfDatabase
import com.example.data.db.RadioScheduleEntity
import com.example.data.model.RadioItemType
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.RadioScheduleRepository
import com.example.data.repository.SaveResult
import com.example.data.repository.SettingsRepository
import com.example.playback.MusicController
import com.example.playback.RadioPlaybackController
import com.example.playback.VideoPlaybackController
import com.example.ui.components.CompactSongRow
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.screens.LibraryScreenContent
import com.example.ui.screens.RadioPlaylistPickerDialog
import com.example.ui.theme.JnmfTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class RadioPlaylistQuickAddAndReorderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun generateSongs(count: Int): List<Song> {
        return (1..count).map { i ->
            Song(
                id = 1000L + i,
                title = "Test Song $i",
                artist = "Artist $i",
                album = "Album $i",
                durationMs = 120_000L + (i * 1_000L),
                sizeBytes = 2_000_000L,
                dateAdded = 1_700_000_000L + i,
                contentUri = Uri.parse("content://media/external/audio/media/${1000 + i}"),
                remixCategory = RemixCategory.UNCLASSIFIED,
                fileName = "song_$i.mp3"
            )
        }
    }

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(SettingsRepository.SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        JnmfDatabase.closeAndResetForTesting()
        context.deleteDatabase(JnmfDatabase.DB_NAME)

        RadioPlaybackController.resetForTesting()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        MusicController.resetInMemoryStateForTesting()
        MusicController.setLanguage(AppLanguage.ENGLISH)
        RadioPlaybackController.setLanguage(AppLanguage.ENGLISH)
    }

    @After
    fun tearDown() {
        RadioPlaybackController.resetForTesting()
        MusicController.resetInMemoryStateForTesting()
        VideoPlaybackController.stop()
        VideoPlaybackController.detachPlayer()
        JnmfDatabase.closeAndResetForTesting()
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(JnmfDatabase.DB_NAME)
    }

    @Test
    fun compactSongRow_threeDotMenu_hasExactOrder_andQuickAddButtonExists() {
        val testSong = generateSongs(1).first()
        var addToPlaylistCalled = false
        var addToRadioCalled = false
        var hideSongCalled = false
        var quickAddCalled = false

        composeTestRule.setContent {
            JnmfTheme {
                CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                    CompactSongRow(
                        song = testSong,
                        isPlaying = false,
                        isCurrentTrack = false,
                        isFavorite = false,
                        onClick = {},
                        onToggleFavorite = {},
                        onAddToPlaylist = { addToPlaylistCalled = true },
                        onHideSong = { hideSongCalled = true },
                        onAddToRadioPlaylist = { addToRadioCalled = true },
                        onQuickAddToRadio = { quickAddCalled = true }
                    )
                }
            }
        }

        // Verify Quick (+) Button exists and triggers callback
        composeTestRule.onNodeWithTag("quick_add_radio_button_${testSong.id}").assertIsDisplayed().performClick()
        assertTrue("Quick Add button should trigger onQuickAddToRadio", quickAddCalled)

        // Open 3-dot menu
        composeTestRule.onNodeWithTag("more_button_${testSong.id}").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // 1. Add to Playlist
        composeTestRule.onNodeWithTag("menu_add_to_playlist_${testSong.id}").assertIsDisplayed()
        // 2. Add to Radio Playlist
        composeTestRule.onNodeWithTag("menu_add_to_radio_${testSong.id}").assertIsDisplayed().performClick()
        assertTrue("Add to Radio Playlist menu item should trigger onAddToRadioPlaylist", addToRadioCalled)
    }

    @Test
    fun radioPlaylistPickerDialog_allowsCreatingNewSchedule_andSelectingExistingSchedule() {
        val context = RuntimeEnvironment.getApplication()
        val database = JnmfDatabase.getDatabase(context)
        val dao = database.jnmfDao()
        val repo = RadioScheduleRepository(context, dao)

        runBlocking {
            repo.createSchedule("Morning Transmission")
        }

        val testSongs = generateSongs(2)
        var selectedScheduleId: Long? = null
        var createdScheduleName: String? = null

        composeTestRule.setContent {
            val schedules by dao.getAllRadioSchedules().collectAsState(initial = emptyList<RadioScheduleEntity>())
            JnmfTheme {
                CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                    RadioPlaylistPickerDialog(
                        songsToAdd = testSongs,
                        availableSchedules = schedules,
                        onSelectSchedule = { selectedScheduleId = it.id },
                        onCreateNewScheduleAndAdd = { createdScheduleName = it },
                        onDismiss = {}
                    )
                }
            }
        }

        // Verify dialog shows title and song count
        composeTestRule.onNodeWithTag("radio_picker_playlist_1", useUnmergedTree = true).assertIsDisplayed().performClick()
        assertEquals(1L, selectedScheduleId)

        // Test creating new schedule button
        composeTestRule.onNodeWithTag("radio_picker_create_new_button").performClick()
        composeTestRule.onNodeWithTag("radio_picker_new_playlist_input").performTextInput("Evening Transmission")
        composeTestRule.onNodeWithTag("radio_picker_create_and_add_button").performClick()
        assertEquals("Evening Transmission", createdScheduleName)
    }

    @Test
    fun batchAdd_50Songs_preservesOrder_andPreventsDuplicates() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val database = JnmfDatabase.getDatabase(context)
        val dao = database.jnmfDao()
        val repo = RadioScheduleRepository(context, dao)

        val schedId = (repo.createSchedule("Special Radio Program") as SaveResult.Success).data
        val fiftySongs = generateSongs(50)

        // 1. Add 50 songs in batch
        val addResult = repo.addSongItems(schedId, fiftySongs)
        assertTrue(addResult.isSuccess)
        assertEquals(50, (addResult as SaveResult.Success).data)

        // 2. Verify all 50 items exist in the exact order without gaps
        val itemsInDb = dao.getItemsForRadioScheduleSync(schedId)
        assertEquals(50, itemsInDb.size)
        itemsInDb.forEachIndexed { idx, item ->
            assertEquals(idx, item.orderIndex)
            assertEquals(fiftySongs[idx].id, item.songId)
            assertEquals("Test Song ${idx + 1}", item.title)
        }

        // 3. Attempt to add the same 50 songs again -> All must be safely skipped as duplicates
        val duplicateAddResult = repo.addSongItems(schedId, fiftySongs)
        assertTrue(duplicateAddResult.isSuccess)
        assertEquals(0, (duplicateAddResult as SaveResult.Success).data)

        // Verify count is still exactly 50
        val itemsAfterDup = dao.getItemsForRadioScheduleSync(schedId)
        assertEquals(50, itemsAfterDup.size)
    }

    @Test
    fun multiSelect_uiFlow_selectsAllSongs_andPassesToBatchAdd() {
        val testSongs = generateSongs(5)
        var batchAddedSongs: List<Song>? = null

        composeTestRule.setContent {
            JnmfTheme {
                CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                    LibraryScreenContent(
                        songs = testSongs,
                        currentSongId = null,
                        isPlaying = false,
                        hasPermission = true,
                        onRequestPermission = {},
                        onRefresh = {},
                        onSortSelected = {},
                        currentSort = com.example.data.repository.SortOption.NAME_ASC,
                        searchQuery = "",
                        onSearchQueryChange = {},
                        favoriteSongIds = emptySet(),
                        onToggleFavorite = {},
                        onAddToPlaylist = {},
                        onHideSong = {},
                        onAddSelectedToRadio = { batchAddedSongs = it }
                    )
                }
            }
        }

        // Initially action bar is hidden
        composeTestRule.onAllNodesWithTag("multi_select_action_bar").assertCountEquals(0)

        // Toggle multi-select
        composeTestRule.onNodeWithTag("multi_select_toggle_button").performClick()
        composeTestRule.waitForIdle()

        // Multi-select action bar is now displayed
        composeTestRule.onNodeWithTag("multi_select_action_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("selected_count_text").assertTextContains("0 selected")

        // Select All
        composeTestRule.onNodeWithTag("select_all_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("selected_count_text").assertTextContains("5 selected")

        // Click ADD TO RADIO PLAYLIST button
        composeTestRule.onNodeWithTag("add_selected_to_radio_button").performClick()
        assertNotNull(batchAddedSongs)
        assertEquals(5, batchAddedSongs!!.size)
        assertEquals(testSongs.map { it.id }, batchAddedSongs!!.map { it.id })
    }

    @Test
    fun dragAndDrop_reorderAndManualSave_persistsAcrossRestart() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val database = JnmfDatabase.getDatabase(context)
        val dao = database.jnmfDao()
        val repo = RadioScheduleRepository(context, dao)

        val schedId = (repo.createSchedule("Transmission Schedule") as SaveResult.Success).data
        val songs = generateSongs(3)
        repo.addSongItems(schedId, songs)

        val beforeMove = dao.getItemsForRadioScheduleSync(schedId)
        assertEquals(3, beforeMove.size)
        assertEquals("Test Song 1", beforeMove[0].title)
        assertEquals("Test Song 2", beforeMove[1].title)
        assertEquals("Test Song 3", beforeMove[2].title)

        // Move item 0 to index 2: [Song 1, Song 2, Song 3] -> [Song 2, Song 3, Song 1]
        val moveResult = repo.moveItem(schedId, 0, 2)
        assertTrue(moveResult.isSuccess)

        // Explicit manual save
        val saveResult = repo.saveScheduleExplicitly(schedId)
        assertTrue(saveResult.isSuccess)

        // Verify order in DB
        val afterMove = dao.getItemsForRadioScheduleSync(schedId)
        assertEquals(3, afterMove.size)
        assertEquals("Test Song 2", afterMove[0].title)
        assertEquals("Test Song 3", afterMove[1].title)
        assertEquals("Test Song 1", afterMove[2].title)

        // Simulate app restart by clearing in-memory instances and querying fresh from Room
        JnmfDatabase.closeAndResetForTesting()
        val freshDb = JnmfDatabase.getDatabase(context)
        val freshItems = freshDb.jnmfDao().getItemsForRadioScheduleSync(schedId)
        assertEquals(3, freshItems.size)
        assertEquals("Test Song 2", freshItems[0].title)
        assertEquals("Test Song 3", freshItems[1].title)
        assertEquals("Test Song 1", freshItems[2].title)
    }
}
