package com.android.island.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRepositoryTimingTest {
    private val repository = LyricsRepository()

    @Test
    fun plainTextWithoutTimestampsProducesNoTimedLines() {
        assertTrue(repository.parse("Baris pertama\nBaris kedua\nBaris ketiga").isEmpty())
    }

    @Test
    fun validLrcTimestampsArePreservedInOrder() {
        val parsed = repository.parse("[00:01.00]Baris pertama\n[00:02.50]Baris kedua")
        assertEquals(listOf(1_000L, 2_500L), parsed.map { it.timeMs })
    }

    @Test
    fun enhancedLrcWordTagsAreNotShownAsLyricText() {
        val parsed = repository.parse("[00:01.00]<00:01.00>Halo <00:01.40>dunia\n[00:03.00]Baris kedua")
        assertEquals("Halo dunia", parsed.first().text)
    }

    @Test
    fun plainOnlyLyricsfileIsRejectedInsteadOfSpreadOverTheSong() {
        val yaml = """
            version: '1.0'
            metadata:
              title: 'Lagu'
              artist: 'Artis'
              duration_ms: 180000
            plain: 'Baris pertama Baris kedua'
        """.trimIndent()
        assertTrue(repository.importLyricsFile(yaml) == null)
    }
}
