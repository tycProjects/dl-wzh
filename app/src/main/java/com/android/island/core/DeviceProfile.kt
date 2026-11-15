package com.android.island.core

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Renderer budget controlled only by the user's two-position mode switch.
 * tier 0 = Normal (full premium rendering); tier 2 = Low-End (minimum-allocation rendering).
 * No RAM probing or automatic profile selection is used.
 */
object DeviceProfile {
    // Compose state (same as GlassTuning): a change must recompose the readers, otherwise a
    // Low-End toggle leaves already-composed cards and cached draw blocks on the old path.
    var tier: Int by mutableIntStateOf(0)
        private set

    /** True when the user explicitly requests the isolated, flat, shader-free Low-End renderer. */
    var staticGlass: Boolean by mutableStateOf(false)
        private set

    /** Named apply* (not set*): the property's JVM setter is already setStaticGlass(Boolean), so a set* here would clash. */
    fun applyStaticGlass(enabled: Boolean) { staticGlass = enabled }

    /** Start in Normal; the saved explicit Low-End switch is applied by IslandRoot. */
    fun init(@Suppress("UNUSED_PARAMETER") context: Context) {
        tier = 0
    }

    /**
     * Opt-in minimum-allocation profile. Only the explicit Low-End toggle selects it;
     * Low Power and Android Battery Saver never switch the rendering mode.
     * Core island features and Jelly motion remain available; artwork buffers are reduced
     * and expensive lens/backdrop processing is disabled in Low-End.
     */
    fun setLowEnd(enabled: Boolean) {
        // Only the explicit Low-End toggle selects the minimum-allocation profile.
        // Disabling it restores full-quality artwork and the normal convex lens renderer.
        tier = if (enabled) 2 else 0
    }

    /** Samsung Exynos SoC (any generation), detected from the board/SoC strings the system exposes. */
    val isExynos: Boolean by lazy {
        val id = listOf(Build.HARDWARE, Build.BOARD, Build.SOC_MODEL, Build.SOC_MANUFACTURER)
            .joinToString(" ").lowercase()
        "exynos" in id || "s5e" in id || "universal" in id
    }

    /**
     * Legacy flat-glass fallback. Kept disabled so memory optimisation does not silently change
     * the user's selected glass appearance; Low-End Mode controls static glass separately.
     */
    val flatGlass: Boolean get() = false

    /** Decorative ambient refresh. Interaction/open-close animations remain independent. */
    val tickMs: Long get() = when (tier) { 0 -> 33L; 1 -> 66L; else -> 100L }

    /** Longest side (px) of the private backdrop bitmap. */
    val artMaxPx: Int get() = when (tier) { 0 -> 320; 1 -> 192; else -> 96 }

    /** Progress/lyric polling interval; slower polling reduces short-lived Compose allocations. */
    val positionTickMs: Long get() = when (tier) { 0 -> 250L; 1 -> 333L; else -> 500L }

    /** Longest side (px) of artwork retained from media/notification sources. */
    val sourceArtMaxPx: Int get() = when (tier) { 0 -> 320; 1 -> 224; else -> 112 }

    /** Entries kept in the artwork LRU caches. */
    val artCacheSize: Int get() = 1

    /** Divisor of the lens bake resolution (bitmap = surface / divisor). */
    val lensDivisor: Int get() = if (tier == 0) 1 else 2

    /** Lens (software AGSL bake) is skipped entirely on the weakest tier. */
    val lensEnabled: Boolean get() = tier < 2
}
