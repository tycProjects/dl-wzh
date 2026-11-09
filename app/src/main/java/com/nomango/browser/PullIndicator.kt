package com.nomango.browser

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator

/**
 * The round indicator of pull to refresh: a disc with a ring that grows while you pull
 * and turns while the page reloads. Colours come from the app theme.
 */
class PullIndicator(ctx: Context) : View(ctx) {

    private val density = ctx.resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arc = RectF()
    private var progress = 0f
    private var spin = 0f
    private var spinning = false
    private var spinner: ValueAnimator? = null

    init {
        ring.style = Paint.Style.STROKE
        ring.strokeCap = Paint.Cap.ROUND
        ring.strokeWidth = 3f * density
        visibility = GONE
        elevation = 8f * density
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
    }

    fun setColors(surface: Int, accent: Int) {
        fill.color = surface
        ring.color = accent
        invalidate()
    }

    /** [p] is the pull distance divided by the distance that triggers a refresh. */
    fun setPull(p: Float) {
        progress = if (p < 0f) 0f else if (p > 1f) 1f else p
        invalidate()
    }

    fun startSpin() {
        if (spinning) return
        spinning = true
        val a = ValueAnimator.ofFloat(0f, 360f)
        a.duration = 900L
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            spin = it.animatedValue as Float
            invalidate()
        }
        spinner = a
        a.start()
    }

    fun stopSpin() {
        spinning = false
        spinner?.cancel()
        spinner = null
        progress = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        canvas.drawCircle(w / 2f, h / 2f, w / 2f, fill)
        val pad = 11f * density
        arc.set(pad, pad, w - pad, h - pad)
        if (spinning) {
            canvas.drawArc(arc, spin, 270f, false, ring)
        } else {
            canvas.drawArc(arc, -90f + progress * 200f, 20f + progress * 280f, false, ring)
        }
    }

    override fun onDetachedFromWindow() {
        spinner?.cancel()
        super.onDetachedFromWindow()
    }
}
