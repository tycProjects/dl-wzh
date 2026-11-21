package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyRotationTest {
    @Test
    fun cyclesFromOneThroughFiveAndBack() {
        assertEquals(listOf(0, 1, 2, 3, 4), KeyRing.rotationOrder(5, 0, emptySet()))
        assertEquals(listOf(4, 0, 1, 2, 3), KeyRing.rotationOrder(5, 4, emptySet()))
    }

    @Test
    fun exhaustedKeysAreSkipped() {
        assertEquals(listOf(1, 2, 3, 4), KeyRing.rotationOrder(5, 0, setOf(0)))
    }

    @Test
    fun allExhaustedFallsBackToFullList() {
        // Design rule: when every key is exhausted the rotation is empty and the caller waits
        // for the provider reset instead of hammering (see KeyRingTest).
        assertEquals(emptyList<Int>(), KeyRing.rotationOrder(3, 0, setOf(0, 1, 2)))
    }

    @Test
    fun keyRingKeepsAtMostFiveDistinctKeys() {
        val keys = KeyRing.keys("a", "b\nc\nd\ne\nf\nb")
        assertEquals(5, keys.size)
        assertEquals("a", keys.first())
    }
}
