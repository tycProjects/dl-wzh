package com.touchflux.app.fx

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/**
 * Draws effects and (optionally) turns its own touches into effects.
 *
 * Used directly as the in-app playground and, with [showBackdrop] off and touches fed by hand,
 * as the transparent full-screen layer of the system-wide mode.
 */
class EffectView(context: Context) : View(context) {
    val engine = EffectEngine(resources.displayMetrics.density)

    var showBackdrop = false
    var interactive = true

    private val density = resources.displayMetrics.density
    private var lastNanos = 0L
    private var hintAlpha = 1f
    private var touched = false

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x30FFFFFF
        strokeWidth = 0.7f * density
    }
    private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26FFFFFF
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2f * density
    }
    private var dotPoints = FloatArray(0)
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 15f * resources.displayMetrics.scaledDensity
    }

    private val lastX = FloatArray(MAX_POINTERS)
    private val lastY = FloatArray(MAX_POINTERS)

    fun tap(x: Float, y: Float, haptic: Boolean = true) {
        touched = true
        engine.tap(x, y)
        if (haptic && engine.config.haptics) Haptics.tick(context)
        invalidate()
    }

    fun trail(x: Float, y: Float) {
        touched = true
        engine.trail(x, y)
        invalidate()
    }

    /** Fires one effect in the middle of the view (used for live previews of settings). */
    fun demo() {
        if (width > 0) tap(width / 2f, height / 2f, haptic = false)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!interactive) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (e.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
                val i = e.actionIndex
                val id = e.getPointerId(i)
                if (id < MAX_POINTERS) {
                    lastX[id] = e.getX(i)
                    lastY[id] = e.getY(i)
                }
                tap(e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> {
                if (!engine.config.trails) return true
                // Emit evenly spaced samples along the finger path. This avoids dotted trails
                // at high touch velocity and keeps the ribbon visually continuous.
                val step = 11f * density * engine.config.size
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    if (id >= MAX_POINTERS) continue
                    val x = e.getX(i)
                    val y = e.getY(i)
                    val dx = x - lastX[id]
                    val dy = y - lastY[id]
                    val distance = hypot(dx, dy)
                    if (distance >= step) {
                        val count = (distance / step).toInt().coerceAtMost(8)
                        val startX = lastX[id]
                        val startY = lastY[id]
                        for (sample in 1..count) {
                            val f = sample.toFloat() / count
                            trail(startX + dx * f, startY + dy * f)
                        }
                        lastX[id] = x
                        lastY[id] = y
                    }
                }
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bg.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            0xFF080808.toInt(), 0xFF000000.toInt(), Shader.TileMode.CLAMP
        )
        val gap = 42f * density
        val cols = (w / gap).toInt() + 1
        val rows = (h / gap).toInt() + 1
        val pts = FloatArray(cols * rows * 2)
        var k = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            pts[k++] = gap * 0.5f + c * gap
            pts[k++] = gap * 0.5f + r * gap
        }
        dotPoints = pts
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (lastNanos == 0L) 0f else ((now - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        lastNanos = now

        if (showBackdrop) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
            val gap = 42f * density
            var gx = gap
            while (gx < width) { canvas.drawLine(gx, 0f, gx, height.toFloat(), grid); gx += gap }
            var gy = gap
            while (gy < height) { canvas.drawLine(0f, gy, width.toFloat(), gy, grid); gy += gap }
            if (touched) hintAlpha = (hintAlpha - dt * 2f).coerceAtLeast(0f)
            if (hintAlpha > 0.01f) {
                hint.alpha = (hintAlpha * 150f).toInt()
                canvas.drawText("Chạm để thử hiệu ứng", width / 2f, height / 2f + hint.textSize / 3f, hint)
            }
        }

        engine.update(dt)
        engine.draw(canvas)

        if (!engine.idle || (showBackdrop && touched && hintAlpha > 0f)) {
            postInvalidateOnAnimation()
        } else {
            lastNanos = 0L
        }
    }

    private companion object {
        const val MAX_POINTERS = 10
    }
}
