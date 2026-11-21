package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchPlannerTest {

    @Test
    fun detectsDeepResearchRequests() {
        assertTrue(ResearchPlanner.isDeepResearchQuery("cari berita tentang gibran lebih dalam"))
        assertTrue(ResearchPlanner.isDeepResearchQuery("telusuri kronologi kasus Giring"))
        assertTrue(ResearchPlanner.isDeepResearchQuery("riset mendalam soal IHSG"))
        assertTrue(ResearchPlanner.isDeepResearchQuery("deep dive quantum computing"))
        assertFalse(ResearchPlanner.isDeepResearchQuery("berita hari ini"))
        assertFalse(ResearchPlanner.isDeepResearchQuery("berapa harga emas sekarang"))
    }

    @Test
    fun coreTermsDropFillerWords() {
        val terms = ResearchPlanner.coreTerms("cari lebih dalam berita tentang Gibran Rakabuming")
        assertTrue(terms.contains("gibran"))
        assertTrue(terms.contains("rakabuming"))
        assertFalse(terms.contains("berita"))
        assertFalse(terms.contains("tentang"))
        assertFalse(terms.contains("cari"))
    }

    @Test
    fun variantsAreDistinctAndNonEmpty() {
        val variants = ResearchPlanner.variants("kronologi kasus korupsi Timbun Matahari")
        assertEquals(3, variants.size)
        assertEquals(variants.size, variants.toSet().size)
        assertTrue(variants.all { it.isNotBlank() })
        // Every variant keeps the core content keywords.
        assertTrue(variants.all { it.contains("korupsi") })
    }

    @Test
    fun variantsEmptyWithoutCoreTerms() {
        assertTrue(ResearchPlanner.variants("cari lebih dalam").isEmpty())
    }

    @Test
    fun deepResearchBudgetIsWiderThanDefault() {
        val (steps, ms) = AgentLoopPolicy.budgetFor(isDeepResearch = true)
        assertTrue(steps > AgentLoopPolicy.MAX_STEPS)
        assertTrue(ms > AgentLoopPolicy.MAX_MS)
        val (normalSteps, normalMs) = AgentLoopPolicy.budgetFor(isDeepResearch = false)
        assertEquals(AgentLoopPolicy.MAX_STEPS, normalSteps)
        assertEquals(AgentLoopPolicy.MAX_MS, normalMs)
    }
}
