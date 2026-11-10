package com.touchflux.app.fx

import android.graphics.Color

/** Colour helpers used by every effect. Single-threaded (UI thread) by design. */
object Colors {
    private val hsv = FloatArray(3)

    fun alpha(c: Int, a: Float): Int {
        val v = (Ease.clamp01(a) * 255f + 0.5f).toInt()
        return (c and 0x00FFFFFF) or (v shl 24)
    }

    fun blend(a: Int, b: Int, t: Float): Int {
        val k = Ease.clamp01(t)
        val al = Color.alpha(a) + ((Color.alpha(b) - Color.alpha(a)) * k).toInt()
        val r = Color.red(a) + ((Color.red(b) - Color.red(a)) * k).toInt()
        val g = Color.green(a) + ((Color.green(b) - Color.green(a)) * k).toInt()
        val bl = Color.blue(a) + ((Color.blue(b) - Color.blue(a)) * k).toInt()
        return Color.argb(al, r, g, bl)
    }

    fun lighten(c: Int, f: Float): Int = blend(c, Color.WHITE, f)

    fun darken(c: Int, f: Float): Int = blend(c, Color.BLACK, f)

    fun hueShift(c: Int, degrees: Float): Int {
        Color.colorToHSV(c, hsv)
        hsv[0] = (hsv[0] + degrees + 360f) % 360f
        return Color.HSVToColor(Color.alpha(c), hsv)
    }

    fun fromHue(hue: Float, saturation: Float = 0.78f, value: Float = 1f): Int {
        hsv[0] = ((hue % 360f) + 360f) % 360f
        hsv[1] = saturation
        hsv[2] = value
        return Color.HSVToColor(hsv)
    }
}
