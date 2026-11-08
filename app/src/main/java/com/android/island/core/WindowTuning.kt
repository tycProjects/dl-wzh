package com.android.island.core

/**
 * One place for the drawing-window size, so the window (IslandAccessibilityService) and the content
 * that must fit inside it (IslandRoot's WhatsApp stack cap) can never disagree.
 *
 * The window is a fixed-size surface. Its graphics buffers cost width x height x 4 bytes each, and the
 * system keeps about four of them. 0.48 of the screen height holds the largest media card and a
 * banner with room for the jelly overshoot; longer WhatsApp stacks overlap and scroll.
 */
object WindowTuning {
    /** Smaller window on mid/low devices: fewer graphics-buffer bytes. Stacks scroll sooner there. */
    val HEIGHT_FRACTION: Float get() = when (DeviceProfile.tier) { 0 -> 0.48f; 1 -> 0.42f; else -> 0.38f }

    /**
     * Window height in dp. A fraction of the screen height is right in portrait, but in landscape the screen is
     * only ~320-400 dp tall, so a fraction would cut the expanded card off (seen in games). The window is
     * therefore never smaller than what the card needs ([requiredDp]), and never taller than the screen.
     */
    fun windowHeightDp(screenHeightDp: Float, requiredDp: Float): Float =
        maxOf(screenHeightDp * HEIGHT_FRACTION, requiredDp).coerceAtMost(screenHeightDp)
}
