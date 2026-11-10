package com.touchflux.app.fx

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
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

    private val bg = Paint()
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.1f * density
        color = 0x28FFFFFF
    }
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 15f * resources.displayMetrics.scaledDensity
    }

    private val lastX = FloatArray(MAX_POINTERS)
    private val lastY = FloatArray(MAX_POINTERS)
    private var gap = 28f * density

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
                val step = 30f * density * engine.config.size
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    if (id >= MAX_POINTERS) continue
                    val x = e.getX(i)
                    val y = e.getY(i)
                    if (hypot(x - lastX[id], y - lastY[id]) >= step) {
                        lastX[id] = x
                        lastY[id] = y
                        trail(x, y)
                    }
                }
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gap = 28f * density
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (lastNanos == 0L) 0f else ((now - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        lastNanos = now

        if (showBackdrop) {
            bg.color = 0xFF000000.toInt()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

            // clean square grid
            var x = 0f
            while (x <= width) {
                canvas.drawLine(x, 0f, x, height.toFloat(), grid)
                x += gap
            }
            var y = 0f
            while (y <= height) {
                canvas.drawLine(0f, y, width.toFloat(), y, grid)
                y += gap
            }

            if (touched) hintAlpha = (hintAlpha - dt * 2f).coerceAtLeast(0f)
            if (hintAlpha > 0.01f) {
                hint.alpha = (hintAlpha * 150f).toInt()
                canvas.drawText("Touch anywhere", width / 2f, height / 2f + hint.textSize / 3f, hint)
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
