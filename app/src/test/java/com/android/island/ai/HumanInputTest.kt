package com.android.island.ai

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HumanInputTest {
    @Test fun tapStaysNearTargetWithRealHoldTime() {
        repeat(200) { seed ->
            val t = HumanInput.tap(500, 900, 1080, 2400, Random(seed))
            assertTrue(t.x in 496..504 && t.y in 896..904)
            assertTrue(t.holdMs in 55..120)
        }
    }

    @Test fun tapNeverLeavesTheScreen() {
        repeat(100) { seed ->
            val t = HumanInput.tap(0, 2399, 1080, 2400, Random(seed))
            assertTrue(t.x in 0..1079 && t.y in 0..2399)
        }
    }

    @Test fun tapsAreNotAllIdentical() {
        val taps = (0 until 50).map { HumanInput.tap(500, 900, 1080, 2400, Random(it)) }.toSet()
        assertTrue(taps.size > 10)
    }

    @Test fun scrollDownMovesFingerUpWithHumanSpeed() {
        repeat(100) { seed ->
            val s = HumanInput.scroll(true, 1080, 2400, Random(seed))
            assertTrue(s.y1 > s.y2)
            assertTrue(s.durationMs in 260..420)
            assertTrue(s.x1 in 400..700)
        }
        assertTrue(HumanInput.scroll(false, 1080, 2400, Random(1)).let { it.y1 < it.y2 })
    }

    @Test fun commandsUseIntegersOnly() {
        assertEquals("input swipe 10 20 10 20 80", HumanInput.tapCommand(HumanInput.Tap(10, 20, 80)))
        assertEquals("input swipe 1 2 3 4 300", HumanInput.swipeCommand(HumanInput.Swipe(1, 2, 3, 4, 300)))
    }
}
