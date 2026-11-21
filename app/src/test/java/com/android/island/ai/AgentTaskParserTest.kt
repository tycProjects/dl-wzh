package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentTaskParserTest {
    @Test fun parsesCommonIndonesianSearchAndFirstVideoRequest() {
        val plan = AgentTaskParser.parse("Buka tiktok lalu cari preset lalu enter lalu klik video yg muncul")
        assertNotNull(plan)
        assertEquals("tiktok", plan?.app)
        assertEquals("preset", plan?.query)
        assertTrue(AgentTaskParser.requestsFirstResult("Buka tiktok lalu cari preset lalu enter lalu klik video yg muncul"))
    }

    @Test fun acceptsNaturalConjunctions() {
        val plan = AgentTaskParser.parse("Tolong buka aplikasi TikTok dan cari preset dan setelah itu klik video pertama yang muncul")
        assertNotNull(plan)
        assertEquals("TikTok", plan?.app)
        assertEquals("preset", plan?.query)
    }

    @Test fun doesNotTreatOrdinarySearchAsFirstResultRequest() {
        assertNull(AgentTaskParser.parse("Buka TikTok lalu cari preset"))
        assertTrue(!AgentTaskParser.requestsFirstResult("Cari preset di TikTok"))
    }

    @Test fun identifiesAmbiguousSearchLabels() {
        assertTrue(AgentTaskParser.isGenericSearchLabel("Cari"))
        assertTrue(AgentTaskParser.isGenericSearchLabel("Pencarian · 250wpm"))
    }
}
