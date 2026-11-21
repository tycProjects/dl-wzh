package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollGeometryTest {
    @Test
    fun scrollDownMovesFingerUpTheScreen() {
        assertEquals(720 to 280, ScrollGeometry.swipeY(1000, down = true))
    }

    @Test
    fun scrollUpMovesFingerDownTheScreen() {
        assertEquals(280 to 720, ScrollGeometry.swipeY(1000, down = false))
    }
}
