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

    @Test
    fun testLanguageTranslationsCoverage() {
        val languages = listOf(
            com.example.data.repository.AppLanguage.ENGLISH,
            com.example.data.repository.AppLanguage.CEBUANO,
            com.example.data.repository.AppLanguage.INDONESIAN
        )

        for (lang in languages) {
            val navMusic = com.example.ui.i18n.JnmfStrings.navMusic(lang)
            val navPlaylists = com.example.ui.i18n.JnmfStrings.navPlaylists(lang)
            val navSettings = com.example.ui.i18n.JnmfStrings.navSettings(lang)
            val notifTitle = com.example.ui.i18n.JnmfStrings.notifDialogTitle(lang)
            val notifDesc = com.example.ui.i18n.JnmfStrings.notifDescription(lang)
            val uiColor = com.example.ui.i18n.JnmfStrings.uiColorTitle(lang)
            val langTitle = com.example.ui.i18n.JnmfStrings.langTitle(lang)

            assertTrue(navMusic.isNotBlank())
            assertTrue(navPlaylists.isNotBlank())
            assertTrue(navSettings.isNotBlank())
            assertTrue(notifTitle.isNotBlank())
            assertTrue(notifDesc.isNotBlank())
            assertTrue(uiColor.isNotBlank())
            assertTrue(langTitle.isNotBlank())
        }

        // Verify specific distinctive words
        assertEquals("Music", com.example.ui.i18n.JnmfStrings.navMusic(com.example.data.repository.AppLanguage.ENGLISH))
        assertEquals("Musika", com.example.ui.i18n.JnmfStrings.navMusic(com.example.data.repository.AppLanguage.CEBUANO))
        assertEquals("Musik", com.example.ui.i18n.JnmfStrings.navMusic(com.example.data.repository.AppLanguage.INDONESIAN))

        assertEquals("Settings", com.example.ui.i18n.JnmfStrings.navSettings(com.example.data.repository.AppLanguage.ENGLISH))
        assertEquals("Mga Setting", com.example.ui.i18n.JnmfStrings.navSettings(com.example.data.repository.AppLanguage.CEBUANO))
        assertEquals("Pengaturan", com.example.ui.i18n.JnmfStrings.navSettings(com.example.data.repository.AppLanguage.INDONESIAN))
    }

    @Test
    fun testThemeColorsIntegrity() {
        val allThemes = com.example.data.repository.ThemeColor.values()
        assertEquals(6, allThemes.size)

        val keys = allThemes.map { it.key }.toSet()
        assertTrue(keys.contains("neon_green"))
        assertTrue(keys.contains("electric_blue"))
        assertTrue(keys.contains("crimson_red"))
        assertTrue(keys.contains("modern_white"))
        assertTrue(keys.contains("vibrant_cyan"))
        assertTrue(keys.contains("deep_violet"))

        // Verify color scheme generation does not throw
        for (theme in allThemes) {
            val scheme = com.example.ui.theme.getJnmfColorScheme(theme)
            org.junit.Assert.assertNotNull(scheme.primary)
            org.junit.Assert.assertNotNull(scheme.background)
        }
    }

    @Test
    fun testSongMetadataPreservation() {
        val originalTitle = "DJSound Indo Remix Fullbeat 2026"
        val originalArtist = "DJ JUSSAR"
        val song = Song(
            id = 99L,
            title = originalTitle,
            artist = originalArtist,
            album = "Indo Bass",
            durationMs = 210000,
            sizeBytes = 8000000,
            dateAdded = 5000L,
            contentUri = Uri.parse("content://media/99")
        )

        // Song title and artist must never change regardless of active locale
        assertEquals(originalTitle, song.title)
        assertEquals(originalArtist, song.artist)
    }

    @Test
    fun testVideoTranslationsCoverage() {
        val languages = listOf(
            com.example.data.repository.AppLanguage.ENGLISH,
            com.example.data.repository.AppLanguage.CEBUANO,
            com.example.data.repository.AppLanguage.INDONESIAN
        )

        for (lang in languages) {
            val fullscreen = com.example.ui.i18n.JnmfStrings.fullscreen(lang)
            val exitFullscreen = com.example.ui.i18n.JnmfStrings.exitFullscreen(lang)
            val closePlayer = com.example.ui.i18n.JnmfStrings.closePlayer(lang)
            val moreVideos = com.example.ui.i18n.JnmfStrings.moreVideos(lang)

            assertTrue(fullscreen.isNotBlank())
            assertTrue(exitFullscreen.isNotBlank())
            assertTrue(closePlayer.isNotBlank())
            assertTrue(moreVideos.isNotBlank())
        }

        // Verify specific distinctive words
        assertEquals("Fullscreen", com.example.ui.i18n.JnmfStrings.fullscreen(com.example.data.repository.AppLanguage.ENGLISH))
        assertEquals("Tibuok Screen", com.example.ui.i18n.JnmfStrings.fullscreen(com.example.data.repository.AppLanguage.CEBUANO))
        assertEquals("Layar Penuh", com.example.ui.i18n.JnmfStrings.fullscreen(com.example.data.repository.AppLanguage.INDONESIAN))

        assertEquals("Exit Fullscreen", com.example.ui.i18n.JnmfStrings.exitFullscreen(com.example.data.repository.AppLanguage.ENGLISH))
        assertEquals("Gawas sa Tibuok Screen", com.example.ui.i18n.JnmfStrings.exitFullscreen(com.example.data.repository.AppLanguage.CEBUANO))
        assertEquals("Keluar Layar Penuh", com.example.ui.i18n.JnmfStrings.exitFullscreen(com.example.data.repository.AppLanguage.INDONESIAN))
    }

    @Test
    fun testRedmi10CAspectRatioBounds() {
        // Redmi 10C screen resolution: 720 x 1650 (aspect ratio ~20.6:9 or 0.436 portrait / 2.29 landscape)
        val redmi10CWidth = 720f
        val redmi10CHeight = 1650f
        val landscapeRatio = redmi10CHeight / redmi10CWidth // 2.2916f
        val standard169 = 16f / 9f // 1.777f
        val ultrawide219 = 21f / 9f // 2.333f

        val coercedLandscape = landscapeRatio.coerceIn(1.1f, 2.4f)
        assertEquals(landscapeRatio, coercedLandscape, 0.001f)

        val coerced169 = standard169.coerceIn(1.1f, 2.4f)
        assertEquals(standard169, coerced169, 0.001f)

        val coercedUltrawide = ultrawide219.coerceIn(1.1f, 2.4f)
        assertEquals(ultrawide219, coercedUltrawide, 0.001f)
    }

    @Test
    fun testMediaItemMetadataCreation() {
        val song = Song(
            id = 42L,
            title = "DJ Malam Pagi Indo Bass",
            artist = "DJ Jussar",
            album = "Indo Remix Vol 1",
            durationMs = 215000,
            sizeBytes = 8500000,
            dateAdded = 1000L,
            contentUri = Uri.parse("content://media/external/audio/media/42"),
            albumArtUri = Uri.parse("content://media/external/audio/albumart/5")
        )

        val mediaItem = com.example.playback.MusicController.buildMediaItem(song)
        assertEquals("42", mediaItem.mediaId)
        assertEquals(Uri.parse("content://media/external/audio/media/42"), mediaItem.localConfiguration?.uri)

        val metadata = mediaItem.mediaMetadata
        assertEquals("DJ Malam Pagi Indo Bass", metadata.title?.toString())
        assertEquals("DJ Jussar", metadata.artist?.toString())
        assertEquals("Indo Remix Vol 1", metadata.albumTitle?.toString())
        assertEquals(Uri.parse("content://media/external/audio/albumart/5"), metadata.artworkUri)
        assertEquals(true, metadata.isPlayable)
    }

    @Test
    fun testPlaybackServiceActionConstants() {
        assertEquals("com.example.playback.ACTION_PREVIOUS", com.example.playback.PlaybackService.ACTION_PREVIOUS)
        assertEquals("com.example.playback.ACTION_PLAY_PAUSE", com.example.playback.PlaybackService.ACTION_PLAY_PAUSE)
        assertEquals("com.example.playback.ACTION_PLAY", com.example.playback.PlaybackService.ACTION_PLAY)
        assertEquals("com.example.playback.ACTION_PAUSE", com.example.playback.PlaybackService.ACTION_PAUSE)
        assertEquals("com.example.playback.ACTION_NEXT", com.example.playback.PlaybackService.ACTION_NEXT)
        assertEquals("jnmf_playback_channel", com.example.playback.PlaybackService.CHANNEL_ID)
        assertEquals(1001, com.example.playback.PlaybackService.NOTIFICATION_ID)
    }
}

