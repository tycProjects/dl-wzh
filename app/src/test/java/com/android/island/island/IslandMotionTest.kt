package com.android.island.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression checks for the scalar easing used by the capsule open/close choreography. */
class IslandMotionTest {
    @Test
    fun smoothIsBoundedAndClampedAtBothEnds() {
        assertEquals(0f, smooth(-2f, 0f, 1f), 0f)
        assertEquals(0f, smooth(0f, 0f, 1f), 0f)
        assertEquals(1f, smooth(1f, 0f, 1f), 0f)
        assertEquals(1f, smooth(3f, 0f, 1f), 0f)
    }

    @Test
    fun smoothProgressIsMonotonic() {
        var previous = smooth(0f, 0f, 1f)
        for (step in 1..100) {
            val current = smooth(step / 100f, 0f, 1f)
            assertTrue("progress must not move backwards at step $step", current >= previous)
            assertTrue("progress must remain within 0..1", current in 0f..1f)
            previous = current
        }
    }

    @Test
    fun smoothHasStableStartAndEndValues() {
        assertEquals(0f, smooth(0.2f, 0.2f, 0.8f), 0f)
        assertEquals(1f, smooth(0.8f, 0.2f, 0.8f), 0f)
    }

    @Test
    fun bannerTuckScaleAndAlphaStayInExpectedRanges() {
        for (step in 0..100) {
            val progress = step / 100f
            assertTrue(tuckScaleX(progress) in 0.46f..1f)
            assertTrue(tuckScaleY(progress) in 0.30f..1f)
            assertTrue(tuckAlpha(progress) in 0f..1f)
        }
        assertEquals(1f, tuckAlpha(0f), 0f)
        assertEquals(0f, tuckAlpha(1f), 0f)
    }
}
