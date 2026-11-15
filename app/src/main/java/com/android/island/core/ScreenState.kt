package com.android.island.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Whether the screen is on. Repeating animation loops (waveform, lyric ticker, call timer, drift)
 * wait here while the screen is off, so they do not wake the CPU for nothing.
 * Updated by a SCREEN_ON / SCREEN_OFF receiver registered in IslandApplication.
 */
object ScreenState {
    private val _interactive = MutableStateFlow(true)
    val interactive: StateFlow<Boolean> = _interactive.asStateFlow()

    /** Artwork not used for this long is released while the screen stays off. */
    const val IDLE_ART_MS = 30_000L
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    // Idle cache release is part of Low-End Mode only. In normal mode the cached
    // convex lens/artwork is intentionally retained so reopening does not rebake it.
    private val idleTrim = Runnable {
        if (DeviceProfile.staticGlass) com.android.island.island.trimIdleArt(IDLE_ART_MS)
    }

    /**
     * After a long screen-off the GPU lens bitmap and lens effect are released too. Both are rebuilt on
     * the next expand, so the cost is one lens render, paid only after a long idle and never while the
     * island is visible. Two one-shot callbacks per screen-off, no periodic work, so the battery is untouched.
     */
    const val DEEP_IDLE_MS = 120_000L
    private val deepTrim = Runnable {
        if (DeviceProfile.staticGlass) com.android.island.island.trimDeepIdleCaches()
    }

    fun set(on: Boolean) {
        _interactive.value = on
        mainHandler.removeCallbacks(idleTrim)
        mainHandler.removeCallbacks(deepTrim)
        if (!on) {
            mainHandler.postDelayed(idleTrim, IDLE_ART_MS)
            mainHandler.postDelayed(deepTrim, DEEP_IDLE_MS)
        }
    }

    private val _powerSave = MutableStateFlow(false)

    /** True while Android's battery saver is on. The island then uses the efficient profile. */
    val powerSave: StateFlow<Boolean> = _powerSave.asStateFlow()

    fun refreshPowerSave(context: android.content.Context) {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        _powerSave.value = pm?.isPowerSaveMode == true
    }

    private val _locked = MutableStateFlow(false)

    /** True while the keyguard (lock screen) is showing. The island and every banner are hidden then. */
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Re-reads the keyguard state from the system. Safe to call often; no permission needed. */
    fun refreshLocked(context: android.content.Context) {
        val km = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        _locked.value = km?.isKeyguardLocked == true
    }

    /**
     * Re-reads both flags from the system. Gates call this before deciding, so a missed broadcast can no
     * longer leave the island or the notification listener stuck on a stale "locked" or "not interactive".
     */
    fun refresh(context: android.content.Context) {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        _interactive.value = pm?.isInteractive ?: true
        refreshLocked(context)
    }

    /** Returns at once while the screen is on; otherwise suspends until it comes back on. */
    suspend fun awaitInteractive() {
        _interactive.first { it }
    }
}
