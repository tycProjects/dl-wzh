package com.example

import android.net.Uri
import androidx.room.Room
import com.example.data.analysis.AudioAnalysisEngine
import com.example.data.db.JnmfDatabase
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistTrackEntity
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import com.example.data.model.VideoItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    private lateinit var db: JnmfDatabase

    @Before
    fun createDb() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, JnmfDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testRoomPlaylistPersistenceAndReordering() = runBlocking {
        val dao = db.jnmfDao()

        // 1. Create playlist
        val playlistId = dao.insertPlaylist(PlaylistEntity(name = "Indo Fullbeat Horeg"))
        assertTrue(playlistId > 0)

        // 2. Add songs
        dao.addSongToPlaylist(playlistId, 101L)
        dao.addSongToPlaylist(playlistId, 202L)
        dao.addSongToPlaylist(playlistId, 303L)

        // 3. Verify order
        var trackIds = dao.getTrackIdsForPlaylistSync(playlistId)
        assertEquals(listOf(101L, 202L, 303L), trackIds)

        // 4. Move track (move track at index 2 to index 0: 303 -> 101 -> 202)
        dao.moveTrackInPlaylist(playlistId, 2, 0)
        trackIds = dao.getTrackIdsForPlaylistSync(playlistId)
        assertEquals(listOf(303L, 101L, 202L), trackIds)

        // 5. Remove track
        dao.removeSongFromPlaylist(playlistId, 101L)
        trackIds = dao.getTrackIdsForPlaylistSync(playlistId)
        assertEquals(listOf(303L, 202L), trackIds)

        // 6. Rename playlist
        dao.renamePlaylist(playlistId, "Renamed Playlist")
        // Verify playlist still intact
        trackIds = dao.getTrackIdsForPlaylistSync(playlistId)
        assertEquals(listOf(303L, 202L), trackIds)
    }

    @Test
    fun testRemixClassification() {
        val context = RuntimeEnvironment.getApplication()
        val engine = AudioAnalysisEngine(context)

        val fullbeatSong = Song(
            id = 1L,
            title = "DJ Malam Pagi Fullbeat Horeg",
            artist = "DJ Indo",
            album = "Remix 2024",
            durationMs = 210000,
            sizeBytes = 8000000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/1")
        )

        val slowSong = Song(
            id = 2L,
            title = "DJ Cinta Slow Remix Santuy",
            artist = "DJ Indo",
            album = "Chill Remix",
            durationMs = 240000,
            sizeBytes = 6000000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/2")
        )

        assertEquals(RemixCategory.FULLBEAT, engine.classifySong(fullbeatSong))
        assertEquals(RemixCategory.SLOW, engine.classifySong(slowSong))
    }

    @Test
    fun testVideoItemFormatting() {
        val video = VideoItem(
            id = 5L,
            title = "Live Performance Indo Fullbeat",
            durationMs = 325000, // 5 min 25 sec
            sizeBytes = 41943040, // 40 MB
            dateAdded = 2000L,
            contentUri = Uri.parse("content://media/video/5")
        )

        assertEquals("05:25", video.formattedDuration)
    }

    @Test
    fun testSongFormattedDisplay() {
        val song = Song(
            id = 10L,
            title = "Test Track",
            artist = "Artist",
            album = "Album",
            durationMs = 195000, // 3 mins 15 secs
            sizeBytes = 10485760, // 10.0 MB
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/10")
        )

        assertEquals("03:15", song.formattedDuration)
        assertEquals("10.0 MB", song.formattedSize)
    }

    @Test
    fun testTaskProgressStateCalculations() {
        val indeterminateState = com.example.ui.components.TaskProgressState(
            taskType = com.example.ui.components.TaskType.STARTUP,
            title = "Initializing JNMF",
            progress = null,
            isRunning = true
        )
        assertEquals(0, indeterminateState.progressPercent)
        assertTrue(indeterminateState.isRunning)

        val measurableState = com.example.ui.components.TaskProgressState(
            taskType = com.example.ui.components.TaskType.MUSIC_SCAN,
            title = "Scanning Music",
            progress = 0.678f,
            current = 68,
            total = 100,
            isRunning = true
        )
        assertEquals(67, measurableState.progressPercent)

        val completedState = measurableState.copy(
            isCompleted = true,
            isRunning = false,
            statusMessage = "Found 100 tracks"
        )
        assertTrue(completedState.isCompleted)
        org.junit.Assert.assertFalse(completedState.isRunning)
    }
}
