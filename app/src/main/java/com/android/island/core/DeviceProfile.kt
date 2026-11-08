package com.android.island.core

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Performance tier of the device, chosen once at start-up from RAM and the system's own low-RAM flag.
 * Chipset-independent on purpose: Exynos, Snapdragon, MediaTek and Unisoc mid-range phones all land in
 * the same tier when they have the same memory, and the island scales its cost accordingly.
 *
 *  tier 0  >= 10 GB RAM   full quality
 *  tier 1  < 10 GB        efficient: smaller art buffers, ~20 Hz ambient loops, lighter lens, smaller window
 *  tier 2  <= 4 GB / low-RAM flag: minimal: no lens bake, tiny art, ~15 Hz loops, smallest window
 */
object DeviceProfile {
    @Volatile var tier: Int = 1
        private set

    fun init(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        val totalGb = info.totalMem / (1024.0 * 1024.0 * 1024.0)
        tier = when {
            am?.isLowRamDevice == true || (totalGb in 0.1..4.4) -> 2
            totalGb >= 10.0 -> 0
            else -> 1
        }
    }

    /** Samsung Exynos SoC (any generation), detected from the board/SoC strings the system exposes. */
    val isExynos: Boolean by lazy {
        val id = listOf(Build.HARDWARE, Build.BOARD, Build.SOC_MODEL, Build.SOC_MANUFACTURER)
            .joinToString(" ").lowercase()
        "exynos" in id || "s5e" in id || "universal" in id
    }

    /**
     * Flat-colour glass: the artwork frost, lens bake and artwork bitmap layers are replaced by a two-tone
     * colour taken from the artwork (same mood, no per-frame texture work). Used on Exynos phones below
     * 10 GB RAM (Mali + A55 mid-range) and on the weakest tier of any chipset. Flagship Exynos keep full glass.
     */
    val flatGlass: Boolean get() = tier == 2 || (isExynos && tier >= 1)

    /** Delay between ticks of ambient animation loops (wave, sheen, drift). */
    val tickMs: Long get() = when (tier) { 0 -> 33L; 1 -> 50L; else -> 66L }

    /** Longest side (px) of the stored artwork copy. */
    val artMaxPx: Int get() = when (tier) { 0 -> 512; 1 -> 256; else -> 160 }

    /** Progress-bar / synced-lyric clock while expanded and playing. Lyric lines last seconds, so 250 ms is plenty. */
    val positionTickMs: Long get() = when (tier) { 0 -> 250L; 1 -> 333L; else -> 500L }

    /** Longest side (px) of the artwork bitmap kept for the track (shown in the expanded island). */
    val sourceArtMaxPx: Int get() = when (tier) { 0 -> 480; 1 -> 400; else -> 300 }

    /** Entries kept in the artwork LRU caches. */
    val artCacheSize: Int get() = if (tier == 0) 3 else if (tier == 1) 2 else 1

    /** Divisor of the lens bake resolution (bitmap = surface / divisor). */
    val lensDivisor: Int get() = if (tier == 0) 2 else 3

    /** Lens (software AGSL bake) is skipped entirely on the weakest tier. */
    val lensEnabled: Boolean get() = tier < 2
}
