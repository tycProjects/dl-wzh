package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenSummaryTest {
    @Test fun longScreenIsCutToLimit() {
        assertEquals(ScreenSummary.LIMIT, ScreenSummary.trim("a".repeat(5000)).length)
    }
    @Test fun blankScreenStaysBlank() {
        assertEquals("", ScreenSummary.trim("   "))
    }
}
