package com.personal.snapstreak

/**
 * Reads a screenshot of Snapchat's "Send To" screen (dark mode).
 * Sample points are on the same 720 x 1600 grid as your taps and get scaled to the real screenshot size.
 *
 *   NOT_LOADED : Send To screen isn't showing (yet)
 *   ALL_TAB    : Send To is showing and the "All" tab is selected (blue pill)
 *   OTHER_TAB  : Send To is showing and "All" is NOT selected, so another tab (the face melt one) is
 */
object ScreenCheck {
    enum class State { NOT_LOADED, ALL_TAB, OTHER_TAB }

    private const val REF_W = 720f
    private const val REF_H = 1600f

    fun read(width: Int, height: Int, pixel: (Int, Int) -> Int): State {
        val barA = average(width, height, pixel, 420, 98)   // grey search bar
        val barB = average(width, height, pixel, 520, 98)
        val gap = average(width, height, pixel, 360, 150)   // empty page background under the bar
        val pill = average(width, height, pixel, 38, 196)   // inside the "All" tab

        val loaded = isNeutral(barA, 32, 56) && isNeutral(barB, 32, 56) && isNeutral(gap, 8, 30)
        if (!loaded) return State.NOT_LOADED

        val allSelected = pill[2] > 80 && pill[2] - pill[0] > 60 && pill[2] - pill[1] > 20
        return if (allSelected) State.ALL_TAB else State.OTHER_TAB
    }

    /** Average colour (r, g, b) of a small box around a point given on the 720 x 1600 grid. */
    private fun average(width: Int, height: Int, pixel: (Int, Int) -> Int, rx: Int, ry: Int): IntArray {
        val cx = (rx * width / REF_W).toInt()
        val cy = (ry * height / REF_H).toInt()
        var r = 0
        var g = 0
        var b = 0
        var n = 0
        for (dx in -2..2) {
            for (dy in -2..2) {
                val x = (cx + dx).coerceIn(0, width - 1)
                val y = (cy + dy).coerceIn(0, height - 1)
                val c = pixel(x, y)
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
                n++
            }
        }
        return intArrayOf(r / n, g / n, b / n)
    }

    private fun isNeutral(c: IntArray, lo: Int, hi: Int): Boolean =
        c.all { it in lo..hi } && (c.max() - c.min()) < 10
}
