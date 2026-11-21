package com.android.island.ai

/** Pure geometry for scroll gestures, kept free of Android types so it can be unit tested. */
object ScrollGeometry {

    /** Pause between two scroll gestures so the feed can settle before the next one. */
    const val GAP_MS = 600L

    /**
     * Vertical start and end (in pixels) of a full-width swipe at the screen centre.
     * "Scroll down" means the content moves up, so the finger travels from the lower part
     * of the screen towards the top. Returned as (fromY, toY).
     */
    fun swipeY(heightPx: Int, down: Boolean): Pair<Int, Int> {
        val high = heightPx * 72 / 100
        val low = heightPx * 28 / 100
        return if (down) high to low else low to high
    }
}
