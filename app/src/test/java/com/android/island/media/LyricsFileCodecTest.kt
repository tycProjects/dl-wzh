package com.android.island.media

import org.junit.Assert.*
import org.junit.Test

class LyricsFileCodecTest {
    @Test fun parsesLineAndWordSynchronization() {
        val yaml = """
            version: '1.0'
            metadata:
              title: 'Example'
              artist: 'Artist'
              duration_ms: 10000
            lines:
              - text: 'Hello world'
                start_ms: 1000
                end_ms: 3000
                words:
                  - text: 'Hello '
                    start_ms: 1000
                    end_ms: 1800
                  - text: 'world'
                    start_ms: 1800
                    end_ms: 3000
            plain: 'Hello world'
        """.trimIndent()
        val doc = LyricsFileCodec.parse(yaml)
        assertEquals("Example", doc.title)
        assertEquals(2, doc.lines.single().words.size)
        assertEquals("Hello world", doc.lines.single().words.joinToString("") { it.text })
    }

    @Test fun encodeThenParseKeepsWordTimingAndPlainText() {
        val doc = LyricsDocument(
            title = "Example", artist = "Artist", durationMs = 5000,
            lines = listOf(LyricCue("Hi!", 500, 1200, listOf(LyricWord("Hi!", 500, 1200)))),
            plain = "Hi!\n"
        )
        val decoded = LyricsFileCodec.parse(LyricsFileCodec.encode(doc))
        assertEquals(doc.title, decoded.title)
        assertEquals(doc.lines, decoded.lines)
        assertEquals(doc.plain, decoded.plain)
    }

    @Test fun rejectsUnknownVersionAndInvalidTimes() {
        val unknown = "version: '2.0'\nmetadata:\n  title: A\n  artist: B\n"
        assertFalse(runCatching { LyricsFileCodec.parse(unknown) }.isSuccess)
        val invalid = """
            version: '1.0'
            metadata:
              title: A
              artist: B
            lines:
              - text: Bad
                start_ms: -1
        """.trimIndent()
        assertFalse(runCatching { LyricsFileCodec.parse(invalid) }.isSuccess)
    }

    // STAGE 77: timestamped lyrics that only cover part of the song must still be accepted.
    @Test fun partialTimestampedLyricsAreStillAccepted() {
        val doc = LyricsDocument(
            title = "Lagu", artist = "Artis", durationMs = 240_000L,
            lines = listOf(
                LyricCue("baris satu", 150_000L),
                LyricCue("baris dua", 160_000L),
                LyricCue("baris tiga", 170_000L)
            )
        )
        val report = LyricsFileCodec.validate(doc, 240_000L)
        assertTrue(report.valid)
        assertTrue("score=${report.score}", report.score >= 40)
    }
}
