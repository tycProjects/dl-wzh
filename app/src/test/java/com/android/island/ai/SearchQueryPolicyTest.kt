package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryPolicyTest {
    @Test
    fun sameQueryWithCaseAndPunctuationHasSameKey() {
        assertEquals(SearchQueryPolicy.key("Berita terbaru!"), SearchQueryPolicy.key("  BERITA   TERBARU? "))
    }

    @Test
    fun differentQueriesRemainDifferent() {
        assertFalse(SearchQueryPolicy.key("berita hari ini") == SearchQueryPolicy.key("berita minggu ini"))
    }

    @Test
    fun repeatedSearchInSameTurnIsDetected() {
        val observations = listOf("Hasil pencarian untuk \"Berita terbaru!\":\nisi berita")
        assertTrue(SearchQueryPolicy.wasAlreadySearched(" BERITA TERBARU? ", observations))
        assertFalse(SearchQueryPolicy.wasAlreadySearched("berita minggu ini", observations))
    }
}
