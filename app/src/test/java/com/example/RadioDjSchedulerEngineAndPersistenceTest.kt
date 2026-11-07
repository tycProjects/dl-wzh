package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.db.JnmfDatabase
import com.example.data.model.RadioItemType
import com.example.data.model.RadioScheduledItem
import com.example.data.model.RadioTimelineCalculator
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.RadioScheduleRepository
import com.example.data.repository.SaveResult
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserDataRepository
import com.example.playback.ActiveMediaSessionOwner
import com.example.playback.MusicController
import com.example.playback.PlaybackService
import com.example.playback.RadioPlaybackController
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class RadioDjSchedulerEngineAndPersistenceTest {

    private val createdFiles = mutableListOf<File>()

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
        createdFiles.forEach { f ->
            if (f.exists()) f.delete()
        }
        createdFiles.clear()
    }

    @Test
    fun timelineCalculator_computesExactStartAndEstimatedEndTimes_andRecalculatesOnEdits() {
        // 5:00:00 PM start = 17 * 3600 seconds
        val start5Pm = 17 * 3600
        val song1 = sampleRadioItem(id = 1L, order = 0, type = RadioItemType.MUSIC, title = "Song 1", durationMs = 150_000L) // 2:30
        val ad1 = sampleRadioItem(id = 2L, order = 1, type = RadioItemType.ADVERTISEMENT, title = "Radio Ad 1", durationMs = 15_000L) // 0:15
        val song2 = sampleRadioItem(id = 3L, order = 2, type = RadioItemType.MUSIC, title = "Song 2", durationMs = 130_000L) // 2:10
        val djVoice = sampleRadioItem(id = 4L, order = 3, type = RadioItemType.ANNOUNCEMENT, title = "DJ Voice", durationMs = 20_000L) // 0:20
        val stationId = sampleRadioItem(id = 5L, order = 4, type = RadioItemType.JINGLE, title = "Station ID", durationMs = 10_000L) // 0:10

        val initialTimeline = RadioTimelineCalculator.computeTimeline(
            startTimeSecondsOfDay = start5Pm,
            items = listOf(song1, ad1, song2, djVoice, stationId)
        )

        assertEquals("5:00:00 PM", initialTimeline.formattedProgramStartTime)
        assertEquals("5:05:25 PM", initialTimeline.formattedEstimatedFinishTime)
        assertEquals(325_000L, initialTimeline.totalDurationMs)
        assertEquals(2, initialTimeline.musicCount)
        assertEquals(3, initialTimeline.nonMusicInsertCount)

        // Verify prompt example slots:
        assertEquals("5:00:00 PM", initialTimeline.entries[0].formattedStartTime)
        assertEquals("5:02:30 PM", initialTimeline.entries[0].formattedEndTime)

        assertEquals("5:02:30 PM", initialTimeline.entries[1].formattedStartTime)
        assertEquals("5:02:45 PM", initialTimeline.entries[1].formattedEndTime)

        assertEquals("5:02:45 PM", initialTimeline.entries[2].formattedStartTime)
        assertEquals("5:04:55 PM", initialTimeline.entries[2].formattedEndTime)

        assertEquals("5:04:55 PM", initialTimeline.entries[3].formattedStartTime)
        assertEquals("5:05:15 PM", initialTimeline.entries[3].formattedEndTime)

        assertEquals("5:05:15 PM", initialTimeline.entries[4].formattedStartTime)
        assertEquals("5:05:25 PM", initialTimeline.entries[4].formattedEndTime)

        // Reorder: move Station ID to slot 0 -> all subsequent slots recalculate immediately
        val reordered = RadioTimelineCalculator.computeTimeline(
            startTimeSecondsOfDay = start5Pm,
            items = listOf(stationId, song1, ad1, song2, djVoice)
        )
        assertEquals("5:00:00 PM", reordered.entries[0].formattedStartTime)
        assertEquals("5:00:10 PM", reordered.entries[0].formattedEndTime)
        assertEquals("5:00:10 PM", reordered.entries[1].formattedStartTime)
        assertEquals("5:02:40 PM", reordered.entries[1].formattedEndTime)
        assertEquals("5:05:25 PM", reordered.formattedEstimatedFinishTime)
    }

    @Test
    fun databaseMigration2To3_preservesExistingNormalPlaylists_andPersistsMultipleRadioSchedulesAcrossRestarts() = runBlocking {
        val context = RuntimeEnvironment.getApplication()

        // 1. Seed a real version-2 SQLite database containing a normal user playlist
        val v2Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(JnmfDatabase.DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_tracks` (`playlistId` INTEGER NOT NULL, `songId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `savedTitle` TEXT NOT NULL DEFAULT '', `savedArtist` TEXT NOT NULL DEFAULT '', `savedUri` TEXT NOT NULL DEFAULT '', `savedFileName` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`playlistId`, `songId`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`songId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `hidden_songs` (`songId` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `hiddenAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `song_classifications` (`songId` INTEGER NOT NULL, `categoryName` TEXT NOT NULL, `estimatedBpm` INTEGER NOT NULL, `confidence` REAL NOT NULL, PRIMARY KEY(`songId`))")
                    db.execSQL("INSERT INTO `playlists` (`id`, `name`, `createdAt`) VALUES (1, 'My Indo Fullbeat Mix', 1000)")
                    db.execSQL("INSERT INTO `playlist_tracks` (`playlistId`, `songId`, `orderIndex`, `savedTitle`, `savedArtist`, `savedUri`, `savedFileName`) VALUES (1, 101, 0, 'Remix One', 'DJ A', 'content://media/external/audio/media/101', 'remix1.mp3')")
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(v2Config).use { helper ->
            helper.writableDatabase.close()
        }

        // 2. Open JnmfDatabase v3 (runs MIGRATION_2_3) and verify normal playlist is untouched
        val db = JnmfDatabase.getDatabase(context)
        val dao = db.jnmfDao()
        val normalPlaylistsBefore = dao.getAllPlaylistsSync()
        assertEquals(1, normalPlaylistsBefore.size)
        assertEquals("My Indo Fullbeat Mix", normalPlaylistsBefore[0].name)

        // 3. Create multiple named Radio Schedules, add repeated jingles + ads + songs, reorder, duplicate, rename, delete
        val radioRepo = RadioScheduleRepository(context, dao)
        val userRepo = UserDataRepository(context, dao)
        val wavAd = createPcmWavFile(File(context.cacheDir, "radio_ad.wav"), durationMs = 2_000, freqStep = 5)
        val wavJingle = createPcmWavFile(File(context.cacheDir, "station_id.wav"), durationMs = 1_500, freqStep = 7)

        val morningRes = radioRepo.createSchedule("JNMF Morning Radio", startTimeSecondsOfDay = 6 * 3600)
        val afternoonRes = radioRepo.createSchedule("JNMF Afternoon Mix", startTimeSecondsOfDay = 14 * 3600)
        val nightRes = radioRepo.createSchedule("JNMF Night Program", startTimeSecondsOfDay = 21 * 3600)
        assertTrue(morningRes is SaveResult.Success)
        assertTrue(afternoonRes is SaveResult.Success)
        assertTrue(nightRes is SaveResult.Success)
        val morningId = (morningRes as SaveResult.Success).data
        val afternoonId = (afternoonRes as SaveResult.Success).data
        val nightId = (nightRes as SaveResult.Success).data

        val songA = makeSong(201L, "Morning Opener", 120_000L, Uri.fromFile(wavAd))
        val songB = makeSong(202L, "Second Anthem", 140_000L, Uri.fromFile(wavAd))

        // Same station ID jingle can appear at both start and end of the same radio schedule
        radioRepo.addExternalUriItem(morningId, Uri.fromFile(wavJingle), RadioItemType.JINGLE, customTitle = "JNMF Station ID")
        radioRepo.addSongItem(morningId, songA, RadioItemType.MUSIC)
        radioRepo.addExternalUriItem(morningId, Uri.fromFile(wavAd), RadioItemType.ADVERTISEMENT, customTitle = "Coffee Sponsor Ad")
        radioRepo.addSongItem(morningId, songB, RadioItemType.MUSIC)
        radioRepo.addExternalUriItem(morningId, Uri.fromFile(wavJingle), RadioItemType.JINGLE, customTitle = "JNMF Station ID")

        // Move Coffee Sponsor Ad (index 2) to index 1, replace songB, and change type
        radioRepo.moveItem(morningId, fromIndex = 2, toIndex = 1)
        val itemsAfterMove = dao.getItemsForRadioScheduleSync(morningId)
        assertEquals(5, itemsAfterMove.size)
        assertEquals("Coffee Sponsor Ad", itemsAfterMove[1].title)

        // Duplicate Morning Radio, rename Afternoon Mix, delete Night Program
        val dupRes = radioRepo.duplicateSchedule(morningId)
        assertTrue(dupRes is SaveResult.Success)
        val dupId = (dupRes as SaveResult.Success).data
        radioRepo.renameSchedule(afternoonId, "JNMF Drive Time")
        radioRepo.deleteSchedule(nightId)

        // Verify normal playlists were NEVER touched by any radio schedule operations
        val normalPlaylistsAfter = dao.getAllPlaylistsSync()
        assertEquals(1, normalPlaylistsAfter.size)
        assertEquals("My Indo Fullbeat Mix", normalPlaylistsAfter[0].name)
        assertEquals(listOf(101L), dao.getTrackIdsForPlaylistSync(1L))
        assertNotNull(userRepo)

        // 4. Close DB singleton to simulate force-stop / device restart and reopen
        JnmfDatabase.closeAndResetForTesting()
        val reopenedDb = JnmfDatabase.getDatabase(context)
        val reopenedDao = reopenedDb.jnmfDao()
        val schedulesAfterRestart = reopenedDao.getAllRadioSchedulesSync()
        val names = schedulesAfterRestart.map { it.name }.toSet()
        assertEquals(setOf("JNMF Morning Radio", "JNMF Morning Radio (Copy)", "JNMF Drive Time"), names)

        val restoredMorningItems = reopenedDao.getItemsForRadioScheduleSync(morningId)
        assertEquals(5, restoredMorningItems.size)
        assertEquals(listOf(0, 1, 2, 3, 4), restoredMorningItems.map { it.orderIndex })
        assertEquals("Coffee Sponsor Ad", restoredMorningItems[1].title)
        assertEquals(RadioItemType.ADVERTISEMENT.storageKey, restoredMorningItems[1].itemType)

        val restoredDupItems = reopenedDao.getItemsForRadioScheduleSync(dupId)
        assertEquals(5, restoredDupItems.size)
    }

    @Test
    fun automaticRadioPlayback_playsMixedProgramSequentially_skipsMissingFile_andPreservesNormalMusicQueue() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val serviceController = Robolectric.buildService(PlaybackService::class.java).create()
        val service = serviceController.get()
        shadowOf(Looper.getMainLooper()).idle()

        try {
            // Prepare real short PCM WAV files for normal music + mixed radio program
            val normalSongFile1 = createPcmWavFile(File(context.cacheDir, "normal_1.wav"), durationMs = 2_000, freqStep = 3)
            val normalSongFile2 = createPcmWavFile(File(context.cacheDir, "normal_2.wav"), durationMs = 2_000, freqStep = 4)
            val normalSong1 = makeSong(11L, "Normal Playlist Track 1", 2_000L, Uri.fromFile(normalSongFile1))
            val normalSong2 = makeSong(12L, "Normal Playlist Track 2", 2_000L, Uri.fromFile(normalSongFile2))

            // Start normal music first so we can verify Radio arbitrates cleanly without wiping normal queue
            MusicController.playSong(normalSong1, listOf(normalSong1, normalSong2))
            awaitCondition("Normal music should start") {
                MusicController.state.value.isPlaying &&
                    VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.MUSIC
            }

            // Build mixed Radio DJ schedule:
            // [0] Song 1 (MUSIC, 650ms)
            // [1] Radio Ad 1 (ADVERTISEMENT, 550ms)
            // [2] Missing Voiceover (ANNOUNCEMENT, deleted file -> must skip gracefully!)
            // [3] Song 2 (MUSIC, 650ms -> must play Song 2, NOT restart at Song 1!)
            // [4] Station ID (JINGLE, 500ms)
            val radioSong1File = createPcmWavFile(File(context.cacheDir, "radio_song1.wav"), durationMs = 650, freqStep = 5)
            val radioAdFile = createPcmWavFile(File(context.cacheDir, "radio_ad1.wav"), durationMs = 550, freqStep = 7)
            val missingVoiceFile = File(context.cacheDir, "deleted_dj_voice.wav").also { if (it.exists()) it.delete() }
            val radioSong2File = createPcmWavFile(File(context.cacheDir, "radio_song2.wav"), durationMs = 650, freqStep = 9)
            val jingleFile = createPcmWavFile(File(context.cacheDir, "radio_jingle.wav"), durationMs = 500, freqStep = 11)

            val scheduleItems = listOf(
                RadioScheduledItem(
                    id = 501L,
                    scheduleId = 77L,
                    orderIndex = 0,
                    itemType = RadioItemType.MUSIC,
                    songId = 901L,
                    title = "Radio Song 1",
                    subtitle = "DJ One",
                    contentUri = Uri.fromFile(radioSong1File),
                    durationMs = 650L
                ),
                RadioScheduledItem(
                    id = 502L,
                    scheduleId = 77L,
                    orderIndex = 1,
                    itemType = RadioItemType.ADVERTISEMENT,
                    songId = -1L,
                    title = "Sponsor Radio Ad",
                    subtitle = "radio_ad1.wav",
                    contentUri = Uri.fromFile(radioAdFile),
                    durationMs = 550L
                ),
                RadioScheduledItem(
                    id = 503L,
                    scheduleId = 77L,
                    orderIndex = 2,
                    itemType = RadioItemType.ANNOUNCEMENT,
                    songId = -1L,
                    title = "Missing DJ Voiceover",
                    subtitle = "deleted_dj_voice.wav",
                    contentUri = Uri.fromFile(missingVoiceFile),
                    durationMs = 400L,
                    isAvailable = true // Simulate file deleted right before transition!
                ),
                RadioScheduledItem(
                    id = 504L,
                    scheduleId = 77L,
                    orderIndex = 3,
                    itemType = RadioItemType.MUSIC,
                    songId = 902L,
                    title = "Radio Song 2",
                    subtitle = "DJ Two",
                    contentUri = Uri.fromFile(radioSong2File),
                    durationMs = 650L
                ),
                RadioScheduledItem(
                    id = 505L,
                    scheduleId = 77L,
                    orderIndex = 4,
                    itemType = RadioItemType.JINGLE,
                    songId = -1L,
                    title = "Closing Station ID",
                    subtitle = "radio_jingle.wav",
                    contentUri = Uri.fromFile(jingleFile),
                    durationMs = 500L
                )
            )

            // Start Radio program!
            RadioPlaybackController.startRadioProgram(
                scheduleId = 77L,
                scheduleName = "JNMF Evening Broadcast",
                items = scheduleItems,
                startIndex = 0
            )

            awaitCondition("Radio slot 0 should start playing and claim RADIO session owner") {
                RadioPlaybackController.state.value.isPlaying &&
                    RadioPlaybackController.state.value.currentIndex == 0 &&
                    VideoPlaybackController.activeOwner.value == ActiveMediaSessionOwner.RADIO
            }

            // Normal music must be paused cleanly while preserving its queue & currentSong!
            assertFalse(MusicController.state.value.isPlaying)
            assertEquals("Normal Playlist Track 1", MusicController.state.value.currentSong?.title)
            assertEquals(2, MusicController.state.value.queue.size)
            assertEquals("Sponsor Radio Ad", RadioPlaybackController.state.value.upcomingItem?.title)

            // Verify foreground notification reflects Radio DJ broadcast
            val radioNotif = service.buildForegroundNotificationForCurrentSession()
            assertEquals(
                "Radio Song 1",
                radioNotif.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
            )

            // Let Slot 0 ("Radio Song 1") play to completion -> must auto-advance to Slot 1 ("Sponsor Radio Ad")
            awaitCondition("Radio should automatically advance from Song 1 to Sponsor Radio Ad", timeoutMs = 6_000L) {
                advanceClockStep(80L)
                RadioPlaybackController.state.value.currentIndex == 1 &&
                    RadioPlaybackController.state.value.currentItem?.title == "Sponsor Radio Ad"
            }

            // Let Slot 1 ("Sponsor Radio Ad") finish -> must skip missing Slot 2 and continue to Slot 3 ("Radio Song 2")!
            awaitCondition("Radio should skip missing voiceover and continue to Radio Song 2 (never restarting Song 1)", timeoutMs = 6_000L) {
                advanceClockStep(80L)
                RadioPlaybackController.state.value.currentIndex == 3 &&
                    RadioPlaybackController.state.value.currentItem?.title == "Radio Song 2"
            }
            assertTrue(RadioPlaybackController.state.value.unavailableItemIds.contains(503L))
            assertNotNull(RadioPlaybackController.state.value.statusNotice)
            assertEquals("Closing Station ID", RadioPlaybackController.state.value.upcomingItem?.title)

            // Test notification action NEXT -> advances from Slot 3 ("Radio Song 2") to Slot 4 ("Closing Station ID")
            service.onStartCommand(Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_NEXT
            }, 0, 1)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(4, RadioPlaybackController.state.value.currentIndex)
            assertEquals("Closing Station ID", RadioPlaybackController.state.value.currentItem?.title)
            assertNull(RadioPlaybackController.state.value.upcomingItem)

            // Let Slot 4 ("Closing Station ID") finish -> must complete and stop cleanly at end of program
            awaitCondition("Radio should stop cleanly at end of scheduled program", timeoutMs = 6_000L) {
                advanceClockStep(80L)
                RadioPlaybackController.state.value.isProgramCompleted &&
                    !RadioPlaybackController.state.value.isPlaying
            }

            // Verify an all-missing schedule fails gracefully without hanging or looping
            val allMissing = listOf(
                scheduleItems[2].copy(id = 601L, orderIndex = 0),
                scheduleItems[2].copy(id = 602L, orderIndex = 1)
            )
            RadioPlaybackController.startRadioProgram(
                scheduleId = 88L,
                scheduleName = "Broken Schedule",
                items = allMissing,
                startIndex = 0
            )
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(RadioPlaybackController.state.value.isPlaying)
            assertNotNull(RadioPlaybackController.state.value.errorMessage)
        } finally {
            serviceController.destroy()
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun advanceClockStep(ms: Long) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(ms))
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(5L)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitCondition(message: String, timeoutMs: Long = 5_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (predicate()) return
            ShadowSystemClock.advanceBy(Duration.ofMillis(25))
            Thread.sleep(5L)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(message, predicate())
    }

    private fun sampleRadioItem(
        id: Long,
        order: Int,
        type: RadioItemType,
        title: String,
        durationMs: Long
    ) = RadioScheduledItem(
        id = id,
        scheduleId = 1L,
        orderIndex = order,
        itemType = type,
        songId = if (type == RadioItemType.MUSIC) 100L + id else -1L,
        title = title,
        subtitle = "JNMF",
        contentUri = Uri.parse("content://media/external/audio/media/$id"),
        durationMs = durationMs
    )

    private fun makeSong(id: Long, title: String, durationMs: Long, uri: Uri) = Song(
        id = id,
        title = title,
        artist = "JNMF Artist",
        album = "JNMF Broadcast",
        durationMs = durationMs,
        sizeBytes = 64_000L,
        dateAdded = 1_700_000_000L,
        contentUri = uri,
        remixCategory = RemixCategory.FULLBEAT,
        fileName = "$title.wav"
    )

    private fun createPcmWavFile(target: File, durationMs: Int, freqStep: Int): File {
        createdFiles.add(target)
        val sampleRate = 16_000
        val numSamples = (sampleRate * durationMs) / 1_000
        val byteRate = sampleRate * 2
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
            val chunk = ByteArray(4_096)
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
