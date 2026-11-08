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

    fun set(on: Boolean) {
        _interactive.value = on
    }

    private val _locked = MutableStateFlow(false)

    /** True while the keyguard (lock screen) is showing. The island and every banner are hidden then. */
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Re-reads the keyguard state from the system. Safe to call often; no permission needed. */
    fun refreshLocked(context: android.content.Context) {
        val km = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        _locked.value = km?.isKeyguardLocked == true
    }

    /** Returns at once while the screen is on; otherwise suspends until it comes back on. */
    suspend fun awaitInteractive() {
        _interactive.first { it }
    }
}
