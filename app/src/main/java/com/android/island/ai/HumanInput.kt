package com.android.island.ai

import kotlin.random.Random

/**
 * Builds touch input that looks like a finger instead of a script: the point is jittered inside the
 * target, the press has a real hold time, and a scroll is a slightly slanted swipe with a human speed.
 * Pure logic (no Android types), unit tested. Shizuku executes the resulting `input` commands, which are
 * real injected touch events, not accessibility actions.
 */
object HumanInput {
    data class Tap(val x: Int, val y: Int, val holdMs: Int)
    data class Swipe(val x1: Int, val y1: Int, val x2: Int, val y2: Int, val durationMs: Int)

    fun tap(x: Int, y: Int, width: Int, height: Int, rnd: Random = Random.Default, jitter: Int = 4): Tap {
        val j = jitter.coerceAtLeast(0)
        return Tap(
            x = (x + rnd.nextInt(-j, j + 1)).coerceIn(0, (width - 1).coerceAtLeast(0)),
            y = (y + rnd.nextInt(-j, j + 1)).coerceIn(0, (height - 1).coerceAtLeast(0)),
            holdMs = rnd.nextInt(55, 121)
        )
    }

    fun scroll(down: Boolean, width: Int, height: Int, rnd: Random = Random.Default): Swipe {
        val (fromY, toY) = ScrollGeometry.swipeY(height, down)
        val drift = (width / 20).coerceAtLeast(0)
        val vJitter = (height / 60).coerceAtLeast(0)
        val cx = width / 2 + rnd.nextInt(-drift, drift + 1)
        val slant = (width / 40).coerceAtLeast(0)
        return Swipe(
            x1 = cx,
            y1 = fromY + rnd.nextInt(-vJitter, vJitter + 1),
            x2 = (cx + rnd.nextInt(-slant, slant + 1)).coerceIn(0, (width - 1).coerceAtLeast(0)),
            y2 = toY + rnd.nextInt(-vJitter, vJitter + 1),
            durationMs = rnd.nextInt(260, 421)
        )
    }

    /** A press-and-release at one point: `input swipe` with identical start and end holds the finger down. */
    fun tapCommand(t: Tap): String = "input swipe ${t.x} ${t.y} ${t.x} ${t.y} ${t.holdMs}"

    fun swipeCommand(s: Swipe): String = "input swipe ${s.x1} ${s.y1} ${s.x2} ${s.y2} ${s.durationMs}"
}
