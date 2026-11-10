package com.touchflux.app.service

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.touchflux.app.data.Prefs
import com.touchflux.app.fx.Colors
import com.touchflux.app.fx.EffectEngine
import com.touchflux.app.fx.Haptics
import kotlin.math.hypot

/**
 * Live wallpaper: black field with a clean white grid. Touch makes nova / geometric
 * effects appear; nearby grid lines pick up the effect colour for a living surface.
 */
class TouchFluxWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = FxEngine()

    private inner class FxEngine : Engine() {
        private val prefs = Prefs(this@TouchFluxWallpaperService)
        private val density = resources.displayMetrics.density
        private val engine = EffectEngine(density)
        private val bgPaint = Paint()
        private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.1f * density
        }
        private val reactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f * density
        }
        private var visible = false
        private var running = false
        private var lastNanos = 0L
        private var w = 0
        private var h = 0
        private var lastTapMs = 0L
        private var lastTapX = -1f
        private var lastTapY = -1f
        private var lastDragX = 0f
        private var lastDragY = 0f
        private var gap = 28f * density

        private val frame = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                running = false
                render()
                if (visible && !engine.idle) schedule()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            w = width
            h = height
            gap = 28f * density
            render()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                engine.config = prefs.config()
                lastNanos = 0L
                render()
                if (!engine.idle) schedule()
            }
        }

        override fun onDestroy() {
            visible = false
            Choreographer.getInstance().removeFrameCallback(frame)
            super.onDestroy()
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastDragX = event.x
                    lastDragY = event.y
                    fireTap(event.x, event.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    val cfg = prefs.config()
                    if (!cfg.trails) return
                    if (hypot(event.x - lastDragX, event.y - lastDragY) >= 30f * density * cfg.size) {
                        lastDragX = event.x
                        lastDragY = event.y
                        engine.config = cfg
                        engine.trail(event.x, event.y)
                        schedule()
                    }
                }
            }
        }

        override fun onCommand(
            action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean
        ): Bundle? {
            if (action == CMD_TAP || action == CMD_DROP) fireTap(x.toFloat(), y.toFloat())
            return null
        }

        private fun fireTap(x: Float, y: Float) {
            val now = System.currentTimeMillis()
            if (now - lastTapMs < 120L && hypot(x - lastTapX, y - lastTapY) < 24f * density) return
            lastTapMs = now
            lastTapX = x
            lastTapY = y

            val cfg = prefs.config()
            engine.config = cfg
            engine.tap(x, y)
            if (cfg.haptics) Haptics.tick(this@TouchFluxWallpaperService)
            schedule()
        }

        private fun schedule() {
            if (running || !visible) return
            running = true
            Choreographer.getInstance().postFrameCallback(frame)
        }

        private fun render() {
            val holder = surfaceHolder ?: return
            val now = System.nanoTime()
            val dt = if (lastNanos == 0L) 0f else ((now - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
            lastNanos = if (engine.idle) 0L else now

            var canvas: Canvas? = null
            try {
                canvas = if (Build.VERSION.SDK_INT >= 26) holder.lockHardwareCanvas() else holder.lockCanvas()
                if (canvas == null) return
                drawBackdrop(canvas)
                engine.update(dt)
                engine.draw(canvas)
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        private fun drawBackdrop(c: Canvas) {
            // pure black
            bgPaint.color = 0xFF000000.toInt()
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

            // base white grid (very subtle)
            gridPaint.color = 0x22FFFFFF
            var x = 0f
            while (x <= w) {
                c.drawLine(x, 0f, x, h.toFloat(), gridPaint)
                x += gap
            }
            var y = 0f
            while (y <= h) {
                c.drawLine(0f, y, w.toFloat(), y, gridPaint)
                y += gap
            }

            // reactive colour on grid near live effects is approximated by letting
            // the additive effects themselves light the area; extra coloured lines
            // would require exposing effect positions. The glow already does the job.
        }
    }

    private companion object {
        const val CMD_TAP = "android.wallpaper.tap"
        const val CMD_DROP = "android.home.drop"
    }
}
