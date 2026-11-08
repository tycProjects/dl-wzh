package com.android.island.island

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint

/**
 * WhatsApp icon as a bitmap, used as the stack background when a chat has no avatar.
 * Uses the installed WhatsApp launcher icon; falls back to a green disc if it is unavailable.
 */
/** One shared bitmap for every card: decoded once instead of once per card. */
private var cachedWhatsAppIcon: Bitmap? = null

@Synchronized
fun whatsAppIconBitmap(context: Context, sizePx: Int = 256): Bitmap {
    cachedWhatsAppIcon?.takeIf { !it.isRecycled && it.width == sizePx }?.let { return it }
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val drawn = runCatching {
        val drawable = context.packageManager.getApplicationIcon("com.whatsapp")
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        true
    }.getOrDefault(false)
    if (!drawn) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(0x25, 0xD3, 0x66) }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
    }
    cachedWhatsAppIcon = bmp
    return bmp
}
