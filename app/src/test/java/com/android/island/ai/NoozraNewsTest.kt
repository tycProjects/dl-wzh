package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoozraNewsTest {
    @Test
    fun mapsNewsTopicsToRelevantFeeds() {
        assertEquals(listOf("world", "politics", "finance"), NoozraNews.categoriesFor("berita terbaru"))
        assertTrue(NoozraNews.categoriesFor("berita sains NASA").contains("science"))
        assertTrue(NoozraNews.categoriesFor("berita teknologi AI").contains("tech"))
        assertTrue(NoozraNews.categoriesFor("berita olahraga").contains("sports"))
    }

    @Test
    fun parsesPublisherFieldsAndPublicationDateFromRss() {
        val xml = """
            <rss><channel><item>
              <title><![CDATA[Judul &amp; berita terbaru]]></title>
              <link>https://publisher.example/news/story</link>
              <pubDate>Wed, 18 Nov 2026 10:00:00 GMT</pubDate>
              <source>Publisher Example</source>
              <description><![CDATA[Ringkasan dari penerbit &amp; detail.]]></description>
            </item></channel></rss>
        """.trimIndent()
        val article = NoozraNews.parseFeed(xml).single()
        assertEquals("Judul & berita terbaru", article.title)
        assertEquals("Publisher Example", article.source)
        assertEquals("https://publisher.example/news/story", article.url)
        assertTrue(article.description.contains("Ringkasan dari penerbit"))
        assertTrue(article.publishedEpoch > 0L)
    }

    @Test
    fun parsesAtomEntriesWithHttpsLinkAndPublicationDate() {
        val xml = """
            <feed xmlns="http://www.w3.org/2005/Atom"><entry>
              <title>Berita Indonesia terbaru</title>
              <link rel="alternate" href="https://publisher.example/berita" />
              <published>2026-11-19T06:30:00Z</published>
              <summary>Deskripsi penerbit.</summary>
            </entry></feed>
        """.trimIndent()
        val article = NoozraNews.parseFeed(xml).single()
        assertEquals("Berita Indonesia terbaru", article.title)
        assertEquals("https://publisher.example/berita", article.url)
        assertEquals("2026-11-19T06:30:00Z", article.published)
        assertTrue(article.publishedEpoch > 0L)
    }

    @Test
    fun rejectsMissingDateAndNonHttpsPublisherLinks() {
        val noDate = "<rss><channel><item><title>Judul</title><link>https://publisher.example/a</link></item></channel></rss>"
        val unsafe = "<rss><channel><item><title>Judul</title><link>http://publisher.example/a</link><pubDate>Wed, 18 Nov 2026 10:00:00 GMT</pubDate></item></channel></rss>"
        assertTrue(NoozraNews.parseFeed(noDate).isEmpty())
        assertTrue(NoozraNews.parseFeed(unsafe).isEmpty())
    }
}
