package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageReaderTest {
    @Test fun readsArticlePublishedTimeMetadata() {
        val html = "<html><head><meta property=\"article:published_time\" content=\"2026-11-18T09:30:00Z\"></head></html>"
        assertEquals("2026-11-18T09:30:00Z", PageReader.publicationDate(html))
    }

    @Test fun readsJsonLdPublishedDate() {
        val html = "<script type=\"application/ld+json\">{\"datePublished\":\"2026-11-17\"}</script>"
        assertEquals("2026-11-17", PageReader.publicationDate(html))
    }

    @Test fun doesNotInventMissingPublicationDate() {
        assertNull(PageReader.publicationDate("<html><body><p>Berita tanpa metadata tanggal.</p></body></html>"))
    }
}
