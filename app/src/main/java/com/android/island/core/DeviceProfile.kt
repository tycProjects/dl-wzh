package com.android.island.core

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Renderer budget controlled only by the user's two-position mode switch.
 * tier 0 = Normal (full premium rendering); tier 2 = Low-End (minimum-allocation rendering).
 * No RAM probing or automatic profile selection is used.
 *
 * The renderer tier is a three-state resolution, not a two-state flag:
 *  - UNRESOLVED: the saved preference has not been authoritatively read yet (no fast-flag entry
 *    exists — fresh install, or an upgrade from a build that predates it). Every reader in this
 *    state MUST behave exactly as it would for LOW_END: this is a safety default, never a guess
 *    at Normal. There is no time window in which an unresolved tier renders premium content.
 *  - NORMAL / LOW_END: the user's actual saved choice, once known.
 *
 * [tier] and [staticGlass] are kept as derived read-only properties so every existing call site
 * (FluidGlass, ExpandedIsland, WhatsAppBanner, CallBanner, …) continues to work unchanged: they
 * already treat "not Normal" as "use the flat, shader-free path", so folding UNRESOLVED into that
 * same bucket is enough to make the whole existing isolation apply to the unresolved window too,
 * with no changes required at those sites.
 */
object DeviceProfile {

    enum class RenderTier { UNRESOLVED, NORMAL, LOW_END }

    // Compose state (same as GlassTuning): a change must recompose the readers, otherwise a
    // Low-End toggle (or a late resolution out of UNRESOLVED) leaves already-composed cards and
    // cached draw blocks on the old path. mutableStateOf writes are thread-safe and recompose
    // regardless of which thread performs the write (SettingsRepository resolves this from a
    // background DataStore read — see [setLowEnd]).
    var renderTierState: RenderTier by mutableStateOf(RenderTier.UNRESOLVED)
        private set

    /** 0 = Normal, 2 = Low-End. UNRESOLVED reads as 2 (Low-End) everywhere `tier` is consulted:
     *  it is the safe bucket, never a silent default to Normal. */
    val tier: Int get() = if (renderTierState == RenderTier.NORMAL) 0 else 2

    /** True whenever the isolated, flat, shader-free Low-End renderer must be used: confirmed
     *  Low-End, OR the preference is not yet known. Only confirmed Normal returns false. */
    val staticGlass: Boolean get() = renderTierState != RenderTier.NORMAL

    /** True once the saved preference has been authoritatively resolved (fast flag at init, or
     *  the first DataStore read completing). False only during the brief first-launch/upgrade
     *  gap before that; see [RenderTier.UNRESOLVED]. */
    val isResolved: Boolean get() = renderTierState != RenderTier.UNRESOLVED

    /** Thin alias kept for existing call sites (IslandRoot calls this and [setLowEnd] together
     *  with the same boolean on every composition). staticGlass is now derived from
     *  [renderTierState], so this simply routes through the same resolver; named apply* (not
     *  set*) because the property's JVM setter is already setStaticGlass(Boolean). */
    fun applyStaticGlass(enabled: Boolean) { setLowEnd(enabled) }

    /**
     * Resolves the user's saved rendering tier as early as architecturally possible, so the
     * first frame never touches a premium resource when Low-End was saved:
     *
     * - If a fast-flag entry already exists (every install that has run 1.9.23+ at least once),
     *   this resolves synchronously, here, before any Activity/Service/Composable exists —
     *   exactly the 1.9.23 fast path, unchanged.
     * - If no fast-flag entry exists yet (fresh install, or upgrading from a build that predates
     *   it), the real preference is only known once SettingsRepository's DataStore read
     *   completes, which is an async IO operation too slow to block this call on and which must
     *   not be awaited with an arbitrary delay. The tier is left UNRESOLVED — which every
     *   consumer already treats identically to LOW_END — until SettingsRepository calls
     *   [setLowEnd] with the authoritative value (see SettingsRepository's init block). No
     *   premium resource is ever created while UNRESOLVED.
     */
    fun init(context: Context) {
        val fastFlag = com.android.island.data.SettingsRepository.readLowEndFastFlag(context)
        if (fastFlag != null) {
            renderTierState = if (fastFlag) RenderTier.LOW_END else RenderTier.NORMAL
            com.android.island.util.IslandDiagnostics.record(
                "RENDER_TIER_INITIALIZED", "source=fast_flag tier=$tier lowEnd=$fastFlag"
            )
        } else {
            renderTierState = RenderTier.UNRESOLVED
            com.android.island.util.IslandDiagnostics.record("RENDER_TIER_UNRESOLVED", "reason=no_fast_flag_entry")
            com.android.island.util.IslandDiagnostics.record("SAFE_RENDER_PATH_SELECTED", "reason=awaiting_datastore")
        }
    }

    /**
     * Opt-in minimum-allocation profile. Only the explicit Low-End toggle selects it;
     * Low Power and Android Battery Saver never switch the rendering mode.
     * Core island features and Jelly motion remain available; artwork buffers are reduced
     * and expensive lens/backdrop processing is disabled in Low-End.
     *
     * Also the single resolver used to leave [RenderTier.UNRESOLVED]: SettingsRepository calls
     * this with the authoritative DataStore value the moment its first read completes, whether
     * or not any overlay/Composable has run yet, so resolution never depends on IslandRoot having
     * composed. Every subsequent call (from IslandRoot's SideEffect, or an actual user toggle)
     * either confirms the same state (no-op) or performs a real transition.
     */
    fun setLowEnd(enabled: Boolean) {
        // Only the explicit Low-End toggle selects the minimum-allocation profile.
        // Disabling it restores full-quality artwork and the normal convex lens renderer.
        val next = if (enabled) RenderTier.LOW_END else RenderTier.NORMAL
        if (renderTierState == next) return
        val wasUnresolved = renderTierState == RenderTier.UNRESOLVED
        renderTierState = next
        if (wasUnresolved) {
            com.android.island.util.IslandDiagnostics.record(
                "RENDER_TIER_RESOLVED", "tier=${if (enabled) 2 else 0} lowEnd=$enabled source=datastore"
            )
        }
        com.android.island.util.IslandDiagnostics.record("RENDER_TIER_CHANGED", "tier=${if (enabled) 2 else 0}")
        com.android.island.util.IslandDiagnostics.record(
            if (enabled) "LOW_END_RENDER_PATH_SELECTED" else "NORMAL_RENDER_PATH_SELECTED"
        )
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

    /** Decorative ambient refresh. Interaction/open-close animations remain independent.
     *  Battery Saver halves the decorative tick rate (30 Hz -> 15 Hz in Normal): drift clocks,
     *  wave dividers and the capsule waveform are seconds-scale motion, so the difference is
     *  invisible while the CPU wakes half as often. Feature and layout are unchanged. */
    @Volatile private var powerSaveMode = false

    /** True while Android's Battery Saver is on (mirrored by ScreenState.refreshPowerSave).
     *  It never changes WHAT is rendered — only how often decorative loops wake the CPU. */
    fun setPowerSave(on: Boolean) {
        if (powerSaveMode == on) return
        powerSaveMode = on
        com.android.island.util.IslandDiagnostics.record("POWER_SAVE_TICK_ADJUSTED", "on=$on")
    }

    /** Decorative clock step. Low-End steps at 20 Hz (Battery Saver 15 Hz): the bars look the same on a small capsule. */
    val tickMs: Long get() = if (powerSaveMode) 66L else if (staticGlass) 50L else 33L

    /** Longest side (px) of the private backdrop bitmap. */
    val artMaxPx: Int get() = if (staticGlass) 224 else 320

    /** Progress/lyric polling interval; slower polling reduces short-lived Compose allocations.
     *  Battery Saver relaxes it one step; the progress bar and lyric switches stay current. */
    val positionTickMs: Long get() = if (powerSaveMode) 333L else 250L

    /** Longest side (px) of artwork retained from media/notification sources. */
    val sourceArtMaxPx: Int get() = if (staticGlass) 224 else 320

    /** Entries kept in the artwork LRU caches. */
    val artCacheSize: Int get() = 1

    /**
     * Divisor of the lens bake resolution (bitmap = surface / divisor). Must match LiveLensArt in FluidGlass.kt,
     * which ALWAYS records the lens layer at half size. With 1 here the shader's rounded-rect was twice the
     * layer, so the rim (bevel, refraction, dispersion) only appeared on the top and left edges.
     */
    val lensDivisor: Int get() = 2

    /** Lens (software AGSL bake) is enabled only once CONFIRMED Normal — never while UNRESOLVED. */
    val lensEnabled: Boolean get() = renderTierState == RenderTier.NORMAL
}
