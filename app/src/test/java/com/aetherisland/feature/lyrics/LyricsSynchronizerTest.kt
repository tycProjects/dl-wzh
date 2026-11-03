package com.aetherisland.feature.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsSynchronizerTest {
    @Test fun findsCurrentTimestampedLine() {
        val lines = listOf(LyricsLine(0, "first"), LyricsLine(2_000, "second"), LyricsLine(4_000, "third"))
        assertEquals(1, LyricsSynchronizer.activeLineIndex(lines, 3_100))
    }

    @Test fun returnsMinusOneBeforeFirstLine() {
        assertEquals(-1, LyricsSynchronizer.activeLineIndex(listOf(LyricsLine(1_000, "line")), 0))
    }
}
