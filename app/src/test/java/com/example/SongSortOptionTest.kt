package com.example

import com.example.model.BeatCategory
import com.example.model.Song
import com.example.model.SongSortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class SongSortOptionTest {

    private val sampleSongs = listOf(
        Song(
            id = 1L,
            title = "Zeta Beat Indo",
            artist = "DJ Angklung",
            album = "Indo 2024",
            durationMs = 180_000L,
            uriString = "content://audio/1",
            dateAdded = 1000L,
            fileSize = 10_000_000L,
            bpm = 140,
            beatCategory = BeatCategory.FULL_BEAT
        ),
        Song(
            id = 2L,
            title = "Alpha Bass Drop",
            artist = "DJ Remix King",
            album = "Indo 2024",
            durationMs = 210_000L,
            uriString = "content://audio/2",
            dateAdded = 3000L,
            fileSize = 2_000_000L,
            bpm = 130,
            beatCategory = BeatCategory.FAST_BEAT
        ),
        Song(
            id = 3L,
            title = "Gamma Jedag Jedug",
            artist = "DJ Breakbeat",
            album = "Indo 2024",
            durationMs = 195_000L,
            uriString = "content://audio/3",
            dateAdded = 2000L,
            fileSize = 5_000_000L,
            bpm = 125,
            beatCategory = BeatCategory.MEDIUM_BEAT
        )
    )

    @Test
    fun testSortAToZ() {
        val sorted = SongSortOption.A_TO_Z.sort(sampleSongs)
        assertEquals("Alpha Bass Drop", sorted[0].title)
        assertEquals("Gamma Jedag Jedug", sorted[1].title)
        assertEquals("Zeta Beat Indo", sorted[2].title)
    }

    @Test
    fun testSortZToA() {
        val sorted = SongSortOption.Z_TO_A.sort(sampleSongs)
        assertEquals("Zeta Beat Indo", sorted[0].title)
        assertEquals("Gamma Jedag Jedug", sorted[1].title)
        assertEquals("Alpha Bass Drop", sorted[2].title)
    }

    @Test
    fun testSortDateAscending() {
        // Date Ascending — oldest to newest (1000L, 2000L, 3000L)
        val sorted = SongSortOption.DATE_ASC.sort(sampleSongs)
        assertEquals(1000L, sorted[0].dateAdded)
        assertEquals(2000L, sorted[1].dateAdded)
        assertEquals(3000L, sorted[2].dateAdded)
        assertEquals("Zeta Beat Indo", sorted[0].title)
        assertEquals("Alpha Bass Drop", sorted[2].title)
    }

    @Test
    fun testSortDateDescending() {
        // Date Descending — newest to oldest (3000L, 2000L, 1000L)
        val sorted = SongSortOption.DATE_DESC.sort(sampleSongs)
        assertEquals(3000L, sorted[0].dateAdded)
        assertEquals(2000L, sorted[1].dateAdded)
        assertEquals(1000L, sorted[2].dateAdded)
        assertEquals("Alpha Bass Drop", sorted[0].title)
        assertEquals("Zeta Beat Indo", sorted[2].title)
    }

    @Test
    fun testSortSizeAscending() {
        // Size Ascending — smallest to largest (2MB, 5MB, 10MB)
        val sorted = SongSortOption.SIZE_ASC.sort(sampleSongs)
        assertEquals(2_000_000L, sorted[0].fileSize)
        assertEquals(5_000_000L, sorted[1].fileSize)
        assertEquals(10_000_000L, sorted[2].fileSize)
        assertEquals("Alpha Bass Drop", sorted[0].title)
        assertEquals("Zeta Beat Indo", sorted[2].title)
    }

    @Test
    fun testSortSizeDescending() {
        // Size Descending — largest to smallest (10MB, 5MB, 2MB)
        val sorted = SongSortOption.SIZE_DESC.sort(sampleSongs)
        assertEquals(10_000_000L, sorted[0].fileSize)
        assertEquals(5_000_000L, sorted[1].fileSize)
        assertEquals(2_000_000L, sorted[2].fileSize)
        assertEquals("Zeta Beat Indo", sorted[0].title)
        assertEquals("Alpha Bass Drop", sorted[2].title)
    }

    @Test
    fun testSortingDoesNotMutateOriginalList() {
        val originalCopy = ArrayList(sampleSongs)
        val sorted = SongSortOption.A_TO_Z.sort(sampleSongs)
        assertNotSame(sampleSongs, sorted)
        assertEquals(originalCopy, sampleSongs)
    }
}
