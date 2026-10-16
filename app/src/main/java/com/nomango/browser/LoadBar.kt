package com.nomango.browser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * The page loading bar under the address bar.
 * It fills from the left as the page loads, and its colours keep flowing left to right while it is visible.
 * Everything is drawn in onDraw with one shader, so it costs almost nothing.
 */
class LoadBar @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val matrix = Matrix()
    private var shader: LinearGradient? = null

    /** the last colour must equal the first one, so the flow has no seam */
    private var colors = intArrayOf(
        0xFFFF2D3F.toInt(), 0xFFFF7A2F.toInt(), 0xFF8B5CF6.toInt(),
        0xFF4F8BFF.toInt(), 0xFF16A56A.toInt(), 0xFFFF2D3F.toInt()
    )
    private var frac = 0f
    private var shift = 0f
    private var fillAnim: ValueAnimator? = null
    private var flowAnim: ValueAnimator? = null

    init {
        alpha = 0f
    }

    fun setColors(c: IntArray) {
        if (c.size < 2) return
        colors = c
        buildShader(width)
        invalidate()
    }

    private fun buildShader(w: Int) {
        if (w <= 0) {
            shader = null
            return
        }
        shader = LinearGradient(0f, 0f, w.toFloat(), 0f, colors, null, Shader.TileMode.REPEAT)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildShader(w)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val sh = shader
        if (frac <= 0f || w <= 0f || h <= 0f || sh == null) return
        matrix.setTranslate(shift, 0f)
        sh.setLocalMatrix(matrix)
        paint.shader = sh
        rect.set(0f, 0f, w * frac, h)
        canvas.drawRoundRect(rect, h / 2f, h / 2f, paint)
    }

    /** [percent] is the 0..100 the WebView reports. [anim] is false when the user turned animations off. */
    fun update(percent: Int, anim: Boolean) {
        if (percent in 1..99) {
            animate().cancel()
            alpha = 1f
            startFlow(anim)
            tween(percent / 100f, 240L, anim, null)
        } else if (percent >= 100) {
            animate().cancel()
            alpha = 1f
            startFlow(anim)
            tween(1f, 180L, anim, Runnable { fadeOut(anim) })
        } else {
            reset()
        }
    }

    fun reset() {
        fillAnim?.cancel()
        animate().cancel()
        stopFlow()
        frac = 0f
        alpha = 0f
        invalidate()
    }

    private fun tween(to: Float, ms: Long, anim: Boolean, end: Runnable?) {
        fillAnim?.cancel()
        if (!anim) {
            frac = to
            invalidate()
            end?.run()
            return
        }
        val a = ValueAnimator.ofFloat(frac, to)
        a.duration = ms
        a.interpolator = DecelerateInterpolator()
        a.addUpdateListener {
            frac = it.animatedValue as Float
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            private var cancelled = false

            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!cancelled && end != null) end.run()
            }
        })
        fillAnim = a
        a.start()
    }

    private fun fadeOut(anim: Boolean) {
        if (!anim) {
            hideNow()
            return
        }
        animate().alpha(0f).setDuration(240L).withEndAction { hideNow() }.start()
    }

    private fun hideNow() {
        frac = 0f
        stopFlow()
        alpha = 0f
        invalidate()
    }

    private fun startFlow(anim: Boolean) {
        if (!anim) return
        val running = flowAnim
        if (running != null && running.isRunning) return
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 1100L
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            shift = (it.animatedValue as Float) * width
            invalidate()
        }
        flowAnim = a
        a.start()
    }

    private fun stopFlow() {
        flowAnim?.cancel()
        flowAnim = null
    }

    override fun onDetachedFromWindow() {
        stopFlow()
        fillAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
