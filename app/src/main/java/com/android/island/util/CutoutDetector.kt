package com.android.island.util

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.DisplayCutout
import android.view.WindowManager
import com.android.island.core.CutoutInfo
import com.android.island.core.CutoutType

/**
 * Reads the real display-cutout geometry (Android 15+, public API only).
 *
 * Several sources are tried in order because OEMs differ (Samsung One UI, Xiaomi, Oppo, Transsion...):
 *   1. current window metrics   2. maximum window metrics   3. Display.getCutout()
 *   4. status-bar-height heuristic: a tall status bar means a punch hole even if the ROM reports no cutout
 *
 *  - centred notch / iPhone-style island  -> centerX ~ 0.5
 *  - off-centre punch hole (left/right)   -> centerX away from 0.5, the capsule follows it
 *  - Samsung-style hole that does not touch the top edge -> still found (any rect inside the status area)
 *  - no cutout at all                     -> NONE, capsule drawn centred at the top
 */
object CutoutDetector {
    @Volatile private var cachedKey: CacheKey? = null
    @Volatile private var cachedInfo: CutoutInfo? = null

    private data class CacheKey(val width: Int, val height: Int, val densityDpi: Int, val rotation: Int)

    @Synchronized
    fun detect(context: Context): CutoutInfo {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = context.resources.displayMetrics
        val bounds = runCatching { wm.currentWindowMetrics.bounds }.getOrNull()
            ?: Rect(0, 0, dm.widthPixels, dm.heightPixels)
        val rotation = runCatching { context.display?.rotation }.getOrNull() ?: 0
        val key = CacheKey(bounds.width(), bounds.height(), dm.densityDpi, rotation)
        if (cachedKey == key) return cachedInfo ?: CutoutInfo()

        val result = runCatching { detectReal(context, wm, bounds, dm.density) }.getOrNull()
            ?: CutoutInfo(type = CutoutType.NONE)
        cachedKey = key
        cachedInfo = result
        return result
    }

    private fun detectReal(context: Context, wm: WindowManager, bounds: Rect, density: Float): CutoutInfo {
        val sources = listOf<() -> DisplayCutout?>(
            { wm.currentWindowMetrics.windowInsets.displayCutout },
            { wm.maximumWindowMetrics.windowInsets.displayCutout },
            {
                (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                    ?.getDisplay(Display.DEFAULT_DISPLAY)?.cutout
            }
        )
        for (source in sources) {
            val cutout = runCatching { source() }.getOrNull() ?: continue
            val rect = topRect(cutout, bounds.height()) ?: continue
            return classify(rect, bounds.width(), density)
        }
        return heuristic(context, bounds.width(), density)
    }

    /** Top-edge rect; otherwise any rect that sits inside the status-bar band (holes that do not touch the edge). */
    private fun topRect(cutout: DisplayCutout, heightPx: Int): Rect? {
        cutout.boundingRectTop.takeIf { !it.isEmpty }?.let { return it }
        val band = (heightPx * 0.08f).toInt()
        return cutout.boundingRects
            .filter { !it.isEmpty && it.top < band && it.bottom <= band * 2 }
            .maxByOrNull { it.width().toLong() * it.height().toLong() }
    }

    private fun classify(rect: Rect, screenWidthPx: Int, density: Float): CutoutInfo {
        val widthDp = rect.width() / density
        val heightDp = rect.height() / density
        val type = when {
            widthDp >= 110f -> CutoutType.NOTCH
            heightDp >= widthDp * 0.9f && widthDp >= 20f -> CutoutType.WATERDROP
            else -> CutoutType.PUNCH_HOLE
        }
        return CutoutInfo(
            type = type,
            left = rect.left, top = rect.top, right = rect.right, bottom = rect.bottom,
            width = rect.width(), height = rect.height(),
            centerX = ((rect.left + rect.right) / 2f / screenWidthPx.coerceAtLeast(1)).coerceIn(0f, 1f)
        )
    }

    /**
     * Some ROMs report no cutout to a non-activity context. A status bar taller than a plain 24 dp bar
     * means a camera hole sits there: assume a centred punch hole in the middle of the bar. A wrong guess is
     * harmless: the capsule stays centred and the vertical shift is clamped to zero.
     */
    private fun heuristic(context: Context, screenWidthPx: Int, density: Float): CutoutInfo {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val barPx = if (id > 0) context.resources.getDimensionPixelSize(id) else 0
        if (barPx <= 26f * density) return CutoutInfo(type = CutoutType.NONE)
        val holePx = (12f * density).toInt()
        val top = ((barPx - holePx) / 2).coerceAtLeast(0)
        val left = (screenWidthPx - holePx) / 2
        return CutoutInfo(
            type = CutoutType.PUNCH_HOLE,
            left = left, top = top, right = left + holePx, bottom = top + holePx,
            width = holePx, height = holePx, centerX = 0.5f
        )
    }

    @Synchronized
    fun invalidate() {
        cachedKey = null
        cachedInfo = null
    }
}
