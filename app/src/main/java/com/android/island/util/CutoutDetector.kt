package com.android.island.util

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.android.island.core.CutoutInfo
import com.android.island.core.CutoutType

/**
 * Detects real display-cutout geometry where the public API exposes it.
 * Older releases use a conservative status-bar heuristic as a fallback.
 */
object CutoutDetector {
    @Volatile private var cachedKey: CacheKey? = null
    @Volatile private var cachedInfo: CutoutInfo? = null

    private data class CacheKey(
        val width: Int,
        val height: Int,
        val densityDpi: Int,
        val statusBarHeight: Int,
        val rotation: Int,
        val sdk: Int
    )

    @Synchronized
    fun detect(context: Context): CutoutInfo {
        val metrics = context.resources.displayMetrics
        val statusBarHeight = getStatusBarHeight(context)
        val rotation = runCatching {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        }.getOrDefault(0)
        val key = CacheKey(
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            statusBarHeight = statusBarHeight,
            rotation = rotation,
            sdk = Build.VERSION.SDK_INT
        )
        if (cachedKey == key) return cachedInfo ?: CutoutInfo()

        val result = detectRealCutout(context, metrics.widthPixels, metrics.density)
            ?: detectLegacyCutout(statusBarHeight, metrics.density)

        cachedKey = key
        cachedInfo = result
        return result
    }

    private fun detectRealCutout(context: Context, screenWidthPx: Int, density: Float): CutoutInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val cutout = wm.currentWindowMetrics.windowInsets.displayCutout
                ?: return@runCatching CutoutInfo(type = CutoutType.NONE)
            val rect = cutout.boundingRects
                .filter { it.width() > 0 && it.height() > 0 }
                .maxByOrNull { it.width().toLong() * it.height().toLong() }
                ?: return@runCatching CutoutInfo(type = CutoutType.NONE)
            val widthDp = rect.width() / density
            val heightDp = rect.height() / density
            val type = when {
                widthDp >= 110f -> CutoutType.NOTCH
                heightDp >= widthDp * 0.9f -> CutoutType.WATERDROP
                else -> CutoutType.PUNCH_HOLE
            }
            CutoutInfo(
                type = type,
                left = rect.left,
                top = rect.top,
                right = rect.right,
                bottom = rect.bottom,
                width = rect.width(),
                height = rect.height(),
                centerX = ((rect.left + rect.right) / 2f / screenWidthPx.coerceAtLeast(1)).coerceIn(0f, 1f)
            )
        }.getOrNull()
    }

    private fun detectLegacyCutout(statusBarHeight: Int, density: Float): CutoutInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return CutoutInfo(type = CutoutType.NONE)
        val type = when {
            statusBarHeight > 40 * density -> CutoutType.NOTCH
            statusBarHeight > 28 * density -> CutoutType.WATERDROP
            statusBarHeight > 20 * density -> CutoutType.PUNCH_HOLE
            else -> CutoutType.NONE
        }
        return CutoutInfo(
            type = type,
            top = statusBarHeight,
            width = 0,
            height = statusBarHeight,
            centerX = 0.5f
        )
    }

    @Synchronized
    fun invalidate() {
        cachedKey = null
        cachedInfo = null
    }

    private fun getStatusBarHeight(context: Context): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }
}
