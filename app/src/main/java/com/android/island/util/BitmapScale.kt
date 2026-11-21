package com.android.island.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/**
 * One shared way to shrink a bitmap (including HARDWARE ones, which cannot be copied or scaled
 * with createScaledBitmap) straight into its final small ARGB_8888 size. Replaces three
 * identical copies that lived in WhatsAppNotificationListener (x2) and FluidGlass.
 */
internal fun drawScaled(src: Bitmap, width: Int, height: Int): Bitmap {
    val out = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(
        src,
        Rect(0, 0, src.width, src.height),
        Rect(0, 0, out.width, out.height),
        Paint(Paint.FILTER_BITMAP_FLAG)
    )
    return out
}
