package com.android.island.island

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Launcher icon of the app that posted a notification, used as the card background when a card has
 * no avatar. A tiny LRU keeps at most [MAX_ICONS] bitmaps, so a burst of apps never piles up memory.
 */
private const val MAX_ICONS = 6
private val iconCache = object : LinkedHashMap<String, Bitmap>(8, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > MAX_ICONS
}

@Synchronized
fun appIconBitmap(context: Context, pkg: String, sizePx: Int = 128): Bitmap {
    val key = "$pkg@$sizePx"
    iconCache[key]?.takeIf { !it.isRecycled }?.let { return it }
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val drawn = pkg.isNotBlank() && runCatching {
        val drawable = context.packageManager.getApplicationIcon(pkg)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        true
    }.getOrDefault(false)
    if (!drawn) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentForPackage(pkg).toArgb() }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
    }
    iconCache[key] = bmp
    return bmp
}

/** Drops cached icons (called when the overlay is idle). */
@Synchronized
fun clearAppIconCache() = iconCache.clear()

/** WhatsApp keeps its brand green; every other app gets a stable hue derived from its package name. */
fun accentForPackage(pkg: String): Color {
    if (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b" || pkg.isBlank()) return Color(0xFF25D366)
    val hue = ((pkg.hashCode() and 0x7fffffff) % 360).toFloat()
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.95f)))
}
