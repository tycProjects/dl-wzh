package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AgentTaskParserStage51Test {
    @Test fun tiktokSearchEnterAndFirstVideoIsRecognizedAsOneGoal() {
        val plan = AgentTaskParser.parse("Buka tiktok lalu cari preset lalu enter lalu klik video yg muncul")
        assertNotNull(plan)
        assertEquals("tiktok", plan?.app?.lowercase())
        assertEquals("preset", plan?.query?.lowercase())
    }
}
