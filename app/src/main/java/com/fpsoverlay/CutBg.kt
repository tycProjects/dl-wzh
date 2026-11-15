package com.fpsoverlay

import android.graphics.*
import android.graphics.drawable.Drawable

/** Nền vát góc kiểu game (Free Fire): vát góc trên-trái + dưới-phải, hoặc hình bình hành khi có skew. */
class CutBg(
    private val fill: Int, private val fill2: Int, private val stroke: Int, private val strokeW: Float,
    private val cut: Float, private val skew: Float = 0f, private val glow: Boolean = false
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var shader: Shader? = null

    override fun onBoundsChange(b: Rect) {
        val inset = if (glow) strokeW * 3f else strokeW / 2f
        val l = b.left + inset; val t = b.top + inset; val r = b.right - inset; val bt = b.bottom - inset
        path.reset()
        if (skew > 0f) {
            path.moveTo(l + skew, t); path.lineTo(r, t); path.lineTo(r - skew, bt); path.lineTo(l, bt)
        } else {
            path.moveTo(l + cut, t); path.lineTo(r, t); path.lineTo(r, bt - cut)
            path.lineTo(r - cut, bt); path.lineTo(l, bt); path.lineTo(l, t + cut)
        }
        path.close()
        shader = LinearGradient(l, t, r, bt, fill, fill2, Shader.TileMode.CLAMP)
    }

    override fun draw(c: Canvas) {
        paint.style = Paint.Style.FILL; paint.shader = shader; c.drawPath(path, paint); paint.shader = null
        if (strokeW > 0f && stroke != 0) {
            paint.style = Paint.Style.STROKE; paint.color = stroke
            if (glow) { paint.strokeWidth = strokeW * 5f; paint.alpha = 45; c.drawPath(path, paint); paint.color = stroke }
            paint.strokeWidth = strokeW; c.drawPath(path, paint)
        }
    }

    override fun setAlpha(a: Int) { paint.alpha = a }
    override fun setColorFilter(f: ColorFilter?) { paint.colorFilter = f }
    @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
