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
    /** Same window height in every mode (Normal and Low-End differ only in the glass effect). */
    val HEIGHT_FRACTION: Float get() = 0.48f

    /**
     * STAGE 72: ONE shared cap for the AI panel height, used by all three sides that must agree:
     * the panel's own measured height (AiPanel), the expanded host height (IslandRoot), and the
     * touch window (IslandAccessibilityService). Before, the panel could measure up to ~0.94 of
     * the screen while the touch window was hard-capped at 0.78 — stretching the panel "sampai
     * mentok" put the chip row and the command field below the touchable surface, so taps fell
     * through to the system. With this single cap the chips are ALWAYS inside the touch window.
     *
     * STAGE 74 (RAM): 0.78 -> 0.74. The panel is still comfortably tall (over two thirds of the
     * screen), but the DRAW WINDOW surface that hosts it stops reaching the full display: the
     * window is roughly four graphics buffers of width x height x 4 bytes, so every 10 dp of cap
     * is real RAM. All three sides read this same function, so panel, host and touch window stay
     * in exact agreement.
     */
    fun aiPanelCapDp(screenHeightDp: Float, capsuleHDp: Float, offsetYDp: Float): Float =
        (screenHeightDp * 0.46f - capsuleHDp.coerceIn(24f, 64f) -
            offsetYDp.coerceAtLeast(0f) - 12f).coerceAtLeast(240f)

    /**
     * STAGE 73 (RAM): room the DRAW WINDOW must reserve for the AI panel. Nonzero ONLY while
     * the panel is actually open (IslandActivity.Ai). The old unconditional reserve (measured
     * height or 420 dp whenever AI was enabled) kept the window surface near full height forever
     * after the first panel use — that surface (about four graphics buffers of width x height x 4
     * bytes) was the single biggest chunk of the app's GL/RSS memory while the screen is on, and
     * is exactly the "lama-lama RAM naik lagi" report. While open the SHARED CAP is reserved
     * (not the measured height): one window resize per open and per close, zero churn while the
     * panel measures itself, and the panel can never grow past the room it reserved.
     */
    fun aiPanelWindowReserveDp(
        screenHeightDp: Float,
        capsuleHDp: Float,
        offsetYDp: Float,
        aiEnabled: Boolean,
        panelOpen: Boolean
    ): Float = if (aiEnabled && panelOpen) aiPanelCapDp(screenHeightDp, capsuleHDp, offsetYDp) else 0f

    /**
     * Window height in dp. A fraction of the screen height is right in portrait, but in landscape the screen is
     * only ~320-400 dp tall, so a fraction would cut the expanded card off (seen in games). The window is
     * therefore never smaller than what the card needs ([requiredDp]), and never taller than the screen.
     */
    fun windowHeightDp(screenHeightDp: Float, requiredDp: Float): Float =
        maxOf(screenHeightDp * HEIGHT_FRACTION, requiredDp).coerceAtMost(screenHeightDp)

    // -----------------------------------------------------------------------------------------
    // Jelly overshoot reserve — ONE source of truth for both sides of the window:
    //   - IslandRoot reads it to know exactly how much room below the resting panel bottom the
    //     drawing window guarantees (so the squash-and-stretch amplitude is clamped to real
    //     room instead of being cut flat at the surface edge).
    //   - IslandAccessibilityService adds it to the required window height, so the guarantee is
    //     actually true. The two sides compute the same numbers from the same inputs.
    // -----------------------------------------------------------------------------------------

    /**
     * Worst-case spring overshoot of the Jelly morph past its resting size, per elasticity.
     * For a damped spring the first overshoot peak is exp(-pi*zeta/sqrt(1-zeta^2)); the values
     * below are that peak for the [Elasticity] damping table in jellyDamping(), rounded up, so
     * a reserve sized with them always covers the real spring path.
     */
    fun jellyOvershootFraction(elasticity: Elasticity): Float = when (elasticity) {
        Elasticity.NONE -> 0.02f   // zeta 0.86 -> ~0.5 %
        Elasticity.LOW -> 0.06f    // zeta 0.70 -> ~4.6 %
        Elasticity.MEDIUM -> 0.11f // zeta 0.60 -> ~9.5 %
        Elasticity.HIGH -> 0.22f   // zeta 0.46 -> ~19.7 %
    }

    /**
     * Room (dp) that must exist below the resting panel bottom for the jelly crest:
     * the growth distance from capsule to resting panel bottom, times the overshoot fraction,
     * plus a small fixed margin so the settle never kisses the window edge.
     */
    fun jellyRoomReserveDp(growDistanceDp: Float, overshootFraction: Float): Float =
        (growDistanceDp.coerceAtLeast(1f) * overshootFraction.coerceIn(0f, 0.25f) + 12f)

    /**
     * Full required height (dp) of the drawing window for the given content budget. Shared by
     * the service (which owns the real window) and IslandRoot (which must know the room the
     * window guarantees), so "nothing is ever cut" no longer depends on two formulas agreeing.
     *
     * [cardDp]        — tallest resting expanded surface (media min / user min / AI measured).
     * [bannerDp]      — tallest banner surface incl. the quick-reply row expansion.
     * [jellyActive]   — Jelly option with a non-INSTANT expand style (the only overshooting pair).
     * [aiPanelOpen]   — STAGE 74: true while the AI panel owns the card budget. Its crest room is
     *   clamped to a small constant: IslandRoot already reduces the jelly amplitude to whatever
     *   room the window actually has (never a flat cut), so reserving the FULL spring overshoot
     *   on top of a ~0.74-screen cap used to push the window to the full display height — the
     *   single biggest graphics allocation of the app (dumpsys: GL mtrack ~52 MB while the panel
     *   was open). Media/banner cards keep the full reserve as before.
     */
    fun drawWindowRequiredHeightDp(
        capsuleHDp: Float,
        offsetYDp: Float,
        cardDp: Float,
        compactBannerHDp: Float,
        standardBannerHDp: Float,
        jellyActive: Boolean,
        elasticity: Elasticity,
        aiPanelOpen: Boolean = false
    ): Float {
        val panelTop = capsuleHDp + offsetYDp.coerceAtLeast(0f)
        val grow = (panelTop + cardDp - capsuleHDp).coerceAtLeast(1f)
        var slack = if (jellyActive) jellyRoomReserveDp(grow, jellyOvershootFraction(elasticity)) else 0f
        if (aiPanelOpen) slack = slack.coerceAtMost(16f)
        // Banner content: it sits capsuleH + 8 below the anchor; the reply row can add ~56 dp.
        val bannerContent = capsuleHDp + 8f + maxOf(compactBannerHDp, standardBannerHDp) + 56f
        val content = maxOf(cardDp, bannerContent)
        return panelTop + content + 56f + slack
    }

    /** Clear room on each side of the widest content, so edges and the WhatsApp bubble are never cut. */
    const val LANE_EDGE_DP = 8f

    /**
     * Width of the drawing window (its "lane"). The window is centred on the screen and the scene is
     * centred inside it, so the capsule keeps its exact screen position at any lane width. The lane only
     * has to cover the widest state the island can show, and it is set from settings, never mid-morph.
     */
    fun laneWidthDp(screenWidthDp: Float, widestContentDp: Float): Float =
        (widestContentDp + 2f * LANE_EDGE_DP).coerceAtMost(screenWidthDp)
}
