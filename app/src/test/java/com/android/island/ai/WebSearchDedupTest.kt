package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSearchDedupTest {
    @Test
    fun detectsCurrentNewsRequestsThatMustNotFallBackToUndatedInstantAnswers() {
        assertTrue(WebSearch.isCurrentNewsQuery("berita terbaru Indonesia"))
        assertTrue(WebSearch.isCurrentNewsQuery("breaking news today"))
        assertTrue(!WebSearch.isCurrentNewsQuery("cara kerja RAM Android"))
    }

    @Test
    fun removesDuplicateTitlesAndTrackingVariantsOfSameUrl() {
        val input = listOf(
            WebSearch.Hit("Breaking News: Example", "https://example.com/story?utm_source=one"),
            WebSearch.Hit("breaking news example!!!", "https://other.example/story"),
            WebSearch.Hit("Different headline", "https://example.com/story?ref=home"),
            WebSearch.Hit("Another story", "https://example.com/another")
        )

        val result = WebSearch.uniqueHits(input)

        assertEquals(listOf("Breaking News: Example", "Another story"), result.map { it.title })
    }
    @Test
    fun duplicateUrlDoesNotPoisonTitleDeduplication() {
        val input = listOf(
            WebSearch.Hit("First headline", "https://example.com/story"),
            WebSearch.Hit("Second headline", "https://example.com/story?utm_source=feed"),
            WebSearch.Hit("Second headline", "https://another.example/real-story")
        )
        val result = WebSearch.uniqueHits(input)
        assertEquals(listOf("First headline", "Second headline"), result.map { it.title })
    }

    @Test
    fun meaningfulQueryParametersArePreservedWhenCanonicalizing() {
        assertEquals(
            "example.com/search?q=weather",
            WebSearch.canonicalUrl("https://EXAMPLE.com/search?q=weather&utm_source=feed#top")
        )
        assertEquals(
            "example.com/search?q=sports",
            WebSearch.canonicalUrl("https://example.com/search?q=sports")
        )
        assertTrue(WebSearch.canonicalUrl("https://example.com/search?q=weather") !=
            WebSearch.canonicalUrl("https://example.com/search?q=sports"))
    }

    @Test
    fun parsesBraveGroundingWithSourceUrlsAndExtractedSnippets() {
        val json = """{"grounding":{"generic":[{"url":"https://example.com/story","title":"Example Story","snippets":["A verified passage.","Second passage."]}]},"sources":{"https://example.com/story":{"age":["Tuesday, November 17, 2026","2026-11-17"]}}}"""
        val parsed = WebSearch.parseBraveResponse(json)
        assertTrue(parsed!!.contains("Example Story"))
        assertTrue(parsed.contains("https://example.com/story"))
        assertTrue(parsed.contains("A verified passage."))
        assertTrue(parsed.contains("2026-11-17"))
    }

    @Test
    fun rejectsMalformedBraveResponseAndUnsafeSourceUrls() {
        assertEquals(null, WebSearch.parseBraveResponse("not-json"))
        assertEquals(null, WebSearch.safeSourceUrl("http://example.com/story"))
        assertEquals(null, WebSearch.safeSourceUrl("https://user:pass@example.com/story"))
        assertEquals(null, WebSearch.safeSourceUrl("https://localhost/private"))
        assertEquals("https://example.com/story", WebSearch.safeSourceUrl("https://example.com/story"))
    }

    @Test
    fun emptyBraveGroundingIsNotTreatedAsEvidence() {
        val parsed = WebSearch.parseBraveResponse("""{"grounding":{"generic":[]}}""")
        assertEquals("", parsed)
    }

}
