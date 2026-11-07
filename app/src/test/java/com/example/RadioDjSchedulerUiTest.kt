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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.media3.common.util.UnstableApi
import com.example.data.db.JnmfDatabase
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.RadioScheduleRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ThemeColor
import com.example.playback.MusicController
import com.example.playback.RadioPlaybackController
import com.example.playback.VideoPlaybackController
import com.example.ui.JnmfApp
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.screens.RadioDjSchedulerScreen
import com.example.ui.theme.JnmfTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class RadioDjSchedulerUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleLibrarySongs = listOf(
        Song(
            id = 301L,
            title = "Song 1",
            artist = "DJ Jussar",
            album = "JNMF Club",
            durationMs = 150_000L, // 2:30
            sizeBytes = 3_000_000L,
            dateAdded = 1_700_000_000L,
            contentUri = Uri.parse("content://media/external/audio/media/301"),
            remixCategory = RemixCategory.FULLBEAT,
            fileName = "song1.mp3"
        ),
        Song(
            id = 302L,
            title = "Radio Ad 1",
            artist = "Sponsor",
            album = "Spots",
            durationMs = 15_000L, // 0:15
            sizeBytes = 300_000L,
            dateAdded = 1_700_000_010L,
            contentUri = Uri.parse("content://media/external/audio/media/302"),
            remixCategory = RemixCategory.UNCLASSIFIED,
            fileName = "ad1.mp3"
        ),
        Song(
            id = 303L,
            title = "Song 2",
            artist = "DJ Golden",
            album = "JNMF Club",
            durationMs = 130_000L, // 2:10
            sizeBytes = 2_600_000L,
            dateAdded = 1_700_000_020L,
            contentUri = Uri.parse("content://media/external/audio/media/303"),
            remixCategory = RemixCategory.INDO_REMIX,
            fileName = "song2.mp3"
        )
    )

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
        MusicController.resetInMemoryStateForTesting()
    }

    @After
    fun tearDown() {
        RadioPlaybackController.resetForTesting()
        VideoPlaybackController.stop()
        MusicController.resetInMemoryStateForTesting()
        JnmfDatabase.closeAndResetForTesting()
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(JnmfDatabase.DB_NAME)
    }

    @Test
    fun mainNavigation_displaysAllSixTabsOnRedmi10CScreen_andOpensDedicatedRadioDjSection() {
        composeTestRule.setContent {
            JnmfApp(openRadioSchedulerToken = 1L)
        }
        composeTestRule.waitForIdle()

        // Verify all 6 main navigation tabs are present and visible simultaneously
        composeTestRule.onNodeWithTag("bottom_nav_music").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_playlists").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_equalizer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_radio_dj").assertIsDisplayed().assertIsSelected()
        composeTestRule.onNodeWithTag("bottom_nav_videos").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_settings").assertIsDisplayed()

        // Verify Radio DJ Scheduler screen is rendered independently
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").assertIsDisplayed()

        // Switch to Playlists and back to Radio DJ to confirm clean separation
        composeTestRule.onNodeWithTag("bottom_nav_playlists").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bottom_nav_playlists").assertIsSelected()

        composeTestRule.onNodeWithTag("bottom_nav_radio_dj").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bottom_nav_radio_dj").assertIsSelected()
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").assertIsDisplayed()
    }

    @Test
    fun radioProgramBuilderUi_createsSchedule_displaysEstimatedTimeline_andUpdatesOnReorderAndRemove() {
        val context = RuntimeEnvironment.getApplication()
        val db = JnmfDatabase.getDatabase(context)
        val dao = db.jnmfDao()
        val radioRepo = RadioScheduleRepository(context, dao)

        var observedItemCount = 0

        composeTestRule.setContent {
            JnmfTheme(themeColor = ThemeColor.NEON_GREEN) {
                CompositionLocalProvider(LocalAppLanguage provides AppLanguage.ENGLISH) {
                    val schedules by dao.getAllRadioSchedules().collectAsState(initial = emptyList())
                    val allItems by dao.getAllRadioScheduleItems().collectAsState(initial = emptyList())
                    observedItemCount = allItems.size
                    var selectedId by remember { mutableStateOf<Long?>(null) }

                    RadioDjSchedulerScreen(
                        schedules = schedules,
                        allScheduleItems = allItems,
                        allSongs = sampleLibrarySongs,
                        hasScannedLibrary = true,
                        selectedScheduleId = selectedId,
                        onSelectedScheduleIdChange = { selectedId = it },
                        radioRepo = radioRepo,
                        language = AppLanguage.ENGLISH,
                        onSaveResultFeedback = { res, _ -> res.isSuccess }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // 1. Create "JNMF Evening Radio" (defaults to 5:00:00 PM start)
        composeTestRule.onNodeWithTag("radio_new_schedule_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("radio_schedule_name_input").performTextReplacement("JNMF Evening Radio")
        composeTestRule.onNodeWithTag("radio_schedule_save_confirm").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            composeTestRule.onAllNodesWithTag("radio_add_item_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify default 5:00:00 PM start & explicit Estimated Timeline disclaimer
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_estimated_disclaimer"))
        composeTestRule.onNodeWithTag("radio_program_start_display", useUnmergedTree = true)
            .assertTextContains("5:00:00 PM")
        composeTestRule.onNodeWithTag("radio_estimated_disclaimer", useUnmergedTree = true)
            .assertIsDisplayed()

        // 2. Add Song 1 (MUSIC, 2:30)
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_add_item_button"))
        composeTestRule.onNodeWithTag("radio_add_item_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("radio_picker_type_MUSIC").performClick()
        composeTestRule.onNodeWithTag("radio_picker_song_301").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000L) { observedItemCount == 1 }
        composeTestRule.waitForIdle()

        // 3. Add Radio Ad 1 (ADVERTISEMENT, 0:15)
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_add_item_button"))
        composeTestRule.onNodeWithTag("radio_add_item_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("radio_picker_type_ADVERTISEMENT").performClick()
        composeTestRule.onNodeWithTag("radio_picker_song_302").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000L) { observedItemCount == 2 }
        composeTestRule.waitForIdle()

        // 4. Add Song 2 (MUSIC, 2:10)
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_add_item_button"))
        composeTestRule.onNodeWithTag("radio_add_item_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("radio_picker_type_MUSIC").performClick()
        composeTestRule.onNodeWithTag("radio_picker_song_303").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000L) { observedItemCount == 3 }
        composeTestRule.waitForIdle()

        // Verify calculated timeline slots and estimated finish time (5:04:55 PM)
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_estimated_finish_display"))
        composeTestRule.onNodeWithTag("radio_estimated_finish_display", useUnmergedTree = true)
            .assertTextContains("5:04:55 PM")

        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_timeline_item_0"))
        composeTestRule.onNodeWithTag("radio_item_clock_0", useUnmergedTree = true)
            .assertTextContains("5:00:00 PM – Est. 5:02:30 PM")

        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_timeline_item_1"))
        composeTestRule.onNodeWithTag("radio_item_clock_1", useUnmergedTree = true)
            .assertTextContains("5:02:30 PM – Est. 5:02:45 PM")

        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_timeline_item_2"))
        composeTestRule.onNodeWithTag("radio_item_clock_2", useUnmergedTree = true)
            .assertTextContains("5:02:45 PM – Est. 5:04:55 PM")

        // 5. Reorder: Move Radio Ad 1 (index 1) UP to index 0 -> Radio Ad 1 becomes 5:00:00 PM – Est. 5:00:15 PM!
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_timeline_item_1"))
        composeTestRule.onNodeWithTag("radio_item_move_up_1").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000L) {
            val nodes = composeTestRule.onAllNodesWithTag("radio_item_title_0", useUnmergedTree = true)
                .fetchSemanticsNodes()
            nodes.isNotEmpty() && nodes[0].config.toString().contains("Radio Ad 1")
        }

        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_timeline_item_0"))
        composeTestRule.onNodeWithTag("radio_item_clock_0", useUnmergedTree = true)
            .assertTextContains("5:00:00 PM – Est. 5:00:15 PM")
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_timeline_item_1"))
        composeTestRule.onNodeWithTag("radio_item_clock_1", useUnmergedTree = true)
            .assertTextContains("5:00:15 PM – Est. 5:02:45 PM")

        // 6. Remove slot 0 (Radio Ad 1, 0:15) -> Est. Finish immediately recalculates to 5:04:40 PM!
        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_timeline_item_0"))
        composeTestRule.onNodeWithTag("radio_item_remove_0").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3_000L) { observedItemCount == 2 }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("radio_dj_scheduler_screen").performScrollToNode(hasTestTag("radio_estimated_finish_display"))
        composeTestRule.onNodeWithTag("radio_estimated_finish_display", useUnmergedTree = true)
            .assertTextContains("5:04:40 PM")
        assertEquals(0, kotlinx.coroutines.runBlocking { dao.getAllPlaylistsSync().size })
    }
}
