package com.zewmod.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.util.Log
import android.view.*
import android.view.Choreographer
import com.zewmod.hack.*

/**
 * ZewMod — ESP Overlay Service v5 — ZERO LAG + LOCK-ON ESP
 *
 * FIX UTAMA (ESP trượt khỏi địch khi xoay):
 *
 *   Trước:
 *     Patch → Unity WorldToScreen → UDP → ProxyRelay → HackState → render
 *     Vấn đề: UDP roundtrip ~3–10ms + 1 frame delay = ESP selalu 1-2 frame
 *     di belakang kamera. Khi xoay nhanh, screen coords sudah stale.
 *
 *   Sekarang:
 *     Patch → UDP (world pos 3D + camera matrix) → ProxyRelay → HackState
 *     onDraw (tiap VSYNC) → project ulang worldToScreen dengan matrix terbaru
 *     → Tidak ada delay. Camera matrix dan enemy world pos selalu sync.
 *
 *   EspRenderer.project() dipanggil SEKALI per enemy per frame:
 *     Semua bones di-project ke screen dalam satu pass.
 *     Hasilnya disimpan di ProjectedEnemy.
 *     Box, skeleton, HP, name semuanya pakai data dari struct yang sama.
 *     Tidak ada redundant projection call.
 *
 *   Choreographer.FrameCallback (bukan postDelayed):
 *     Sync tepat dengan VSYNC, tidak drift, tidak accumulate delay.
 *
 *   LAYER_TYPE_HARDWARE:
 *     Canvas dirender ke GPU texture layer — compositing bebas CPU.
 *
 *   Zero allocation dalam onDraw:
 *     Semua Paint pre-allocated. ArrayList reused dengan clear().
 *     String cache untuk number display.
 */
class OverlayService : Service() {

    private var wm: WindowManager? = null
    private var overlayView: EspOverlayView? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val v = EspOverlayView(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 0 }
        wm?.addView(v, params)
        v.startRenderLoop()
        overlayView = v
        Log.i("ZewMod.Overlay", "ESP started v5 — lock-on")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        overlayView?.stopRenderLoop()
        overlayView?.let { wm?.removeView(it) }
        super.onDestroy()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ESP VIEW
// ─────────────────────────────────────────────────────────────────────────────

class EspOverlayView(context: Context) : View(context) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
    }

    // ── VSYNC loop ──────────────────────────────────────────────────────────

    private var running = false
    private val choreo = Choreographer.getInstance()
    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            invalidate()
            choreo.postFrameCallback(this)
        }
    }

    fun startRenderLoop() { running = true; choreo.postFrameCallback(frameCb) }
    fun stopRenderLoop()  { running = false; choreo.removeFrameCallback(frameCb) }

    // ── COLORS ──────────────────────────────────────────────────────────────

    private val CLR_BOX     = Color.parseColor("#7EC8E3")   // cyan nhạt
    private val CLR_BOX_DIM = Color.argb(70, 126, 200, 227)
    private val CLR_HP_G    = Color.parseColor("#4CAF50")   // hijau — hp > 50
    private val CLR_HP_Y    = Color.parseColor("#FFC107")   // kuning — hp 25–50
    private val CLR_HP_R    = Color.parseColor("#F44336")   // merah — hp ≤ 25
    private val CLR_SKEL    = Color.argb(200, 255, 255, 255)
    private val CLR_LINE    = Color.argb(170, 255, 255, 255)
    private val CLR_DOTS    = Color.WHITE

    // ── PAINTS (pre-allocated) ───────────────────────────────────────────────

    private val pBox = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = CLR_BOX; strokeWidth = 2.4f
    }
    private val pSkel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = CLR_SKEL; strokeWidth = 1.5f
        strokeCap = Paint.Cap.ROUND
    }
    private val pLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = CLR_LINE; strokeWidth = 1.2f
    }
    private val pHpBg = Paint().apply {
        style = Paint.Style.FILL; color = Color.argb(150, 0, 0, 0)
    }
    private val pHpFill = Paint().apply { style = Paint.Style.FILL }
    private val pHpNum  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f; textAlign = Paint.Align.CENTER
        isFakeBoldText = true; setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }
    private val pName = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 25f; color = Color.WHITE; textAlign = Paint.Align.CENTER
        isFakeBoldText = true; setShadowLayer(4f, 0f, 1f, Color.BLACK)
    }
    private val pDist = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f; color = Color.argb(200, 255, 215, 50); textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 1f, Color.BLACK)
    }
    private val pFov  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.argb(80, 255, 255, 255); strokeWidth = 1f
    }
    private val pDot  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = CLR_DOTS
    }
    private val pCntBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.argb(200, 8, 8, 8)
    }
    private val pCntBd = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = CLR_BOX; strokeWidth = 1.5f
    }
    private val pCntNum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 42f; textAlign = Paint.Align.CENTER
        isFakeBoldText = true; setShadowLayer(6f, 0f, 2f, Color.BLACK)
    }
    private val pCntLbl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CLR_BOX; textSize = 17f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val pConn = Paint().apply {
        style = Paint.Style.STROKE; color = CLR_BOX_DIM; strokeWidth = 1f
    }

    // ── REUSABLE OBJECTS (zero alloc per frame) ──────────────────────────────

    private val counterRect = RectF()
    private val projected   = ArrayList<ProjectedEnemy>(32)
    private val distCache   = HashMap<Int, String>(32)
    private val hpCache     = HashMap<Int, String>(101)
    private val cntCache    = Array(33) { it.toString() }

    // ── DRAW ────────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        val cfg  = HackState.esp
        if (!cfg.enabled) return

        val sw = width.toFloat()
        val sh = height.toFloat()
        val cx = sw / 2f
        val cy = sh / 2f

        // ── PROJECT semua enemy dari world → screen (1 kali per frame per enemy)
        // Ini adalah kunci utama fix: WorldToScreen pakai camera matrix terbaru,
        // dihitung pada frame yang sama dengan render → tidak ada lag sama sekali

        val enemies = HackState.getEnemies()
        projected.clear()
        for (e in enemies) {
            if (e.isLocalPlayer || e.hp <= 0) continue
            val p = EspRenderer.project(e, sw.toInt(), sh.toInt())
            if (p.onScreen) projected.add(p)
        }

        // ── FOV circle
        if (HackState.aimbot.enabled) {
            val r = (HackState.aimbot.fovDegrees / 180f) * sw * 0.5f
            canvas.drawCircle(cx, cy, r, pFov)
        }

        // ── Crosshair
        canvas.drawCircle(cx, cy, 3.5f, pDot)

        // ── Per-enemy render — semua data sudah di ProjectedEnemy
        for (p in projected) {
            val e = p.src

            if (cfg.lineEsp) {
                // Snapline: tengah bawah → kaki musuh
                canvas.drawLine(cx, sh, p.footX, p.footY, pLine)
            }

            if (cfg.boxEsp && p.boxW > 4f && p.boxH > 4f) {
                drawCornerBox(canvas, p.boxX, p.boxY, p.boxW, p.boxH)
            }

            if (cfg.skeletonEsp) {
                drawSkeleton(canvas, p)
            }

            if (cfg.hpEsp) {
                drawHpBar(canvas, e.hp, p.boxX, p.boxY, p.boxH)
            }

            if (cfg.nameEsp && e.name.isNotBlank()) {
                canvas.drawText(e.name, p.head.x, p.boxY - 8f, pName)
            }

            if (cfg.distanceEsp) {
                val dm  = EspRenderer.distanceM(e)
                val str = distCache.getOrPut(dm) { "${dm}m" }
                canvas.drawText(str, p.head.x, p.boxY - 24f, pDist)
            }
        }

        // ── Enemy counter widget
        drawCounter(canvas, projected.size, cx)
    }

    // ── CORNER BOX ──────────────────────────────────────────────────────────

    private fun drawCornerBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val len = minOf(w * 0.22f, h * 0.22f, 20f)
        // TL
        canvas.drawLine(x,       y,       x+len,   y,       pBox)
        canvas.drawLine(x,       y,       x,       y+len,   pBox)
        // TR
        canvas.drawLine(x+w-len, y,       x+w,     y,       pBox)
        canvas.drawLine(x+w,     y,       x+w,     y+len,   pBox)
        // BL
        canvas.drawLine(x,       y+h-len, x,       y+h,     pBox)
        canvas.drawLine(x,       y+h,     x+len,   y+h,     pBox)
        // BR
        canvas.drawLine(x+w,     y+h-len, x+w,     y+h,     pBox)
        canvas.drawLine(x+w-len, y+h,     x+w,     y+h,     pBox)
    }

    // ── SKELETON ────────────────────────────────────────────────────────────

    private fun drawSkeleton(canvas: Canvas, p: ProjectedEnemy) {
        drawBone(canvas, p.head,    p.spine)
        drawBone(canvas, p.spine,   p.hips)
        drawBone(canvas, p.hips,    p.ankleL)
        drawBone(canvas, p.ankleL,  p.toeL)
        drawBone(canvas, p.hips,    p.ankleR)
        drawBone(canvas, p.ankleR,  p.toeR)
        drawBone(canvas, p.spine,   p.shoulder)
    }

    private inline fun drawBone(canvas: Canvas, a: PointF, b: PointF) {
        if (a.x > 0f && b.x > 0f && a.y > 0f && b.y > 0f)
            canvas.drawLine(a.x, a.y, b.x, b.y, pSkel)
    }

    // ── HP BAR ──────────────────────────────────────────────────────────────
    // Hijau (>50), Kuning (25–50), Đỏ (≤25)

    private fun drawHpBar(canvas: Canvas, hp: Int, bx: Float, by: Float, bh: Float) {
        val barW   = 5f
        val barX   = bx - barW - 4f
        val frac   = (hp / 100f).coerceIn(0f, 1f)
        val fillH  = bh * frac
        val fillTop = by + bh - fillH

        canvas.drawRect(barX, by, barX + barW, by + bh, pHpBg)

        pHpFill.color = when {
            hp > 50 -> CLR_HP_G
            hp > 25 -> CLR_HP_Y
            else    -> CLR_HP_R
        }
        canvas.drawRect(barX, fillTop, barX + barW, by + bh, pHpFill)

        if (bh > 52f) {
            pHpNum.color = pHpFill.color
            val s = hpCache.getOrPut(hp) { "$hp" }
            canvas.drawText(s, barX + barW * 0.5f, by - 2f, pHpNum)
        }
    }

    // ── COUNTER WIDGET ───────────────────────────────────────────────────────

    private fun drawCounter(canvas: Canvas, count: Int, cx: Float) {
        val bw = 88f; val bh = 64f; val bt = 32f
        counterRect.set(cx - bw/2f, bt, cx + bw/2f, bt + bh)
        canvas.drawRoundRect(counterRect, 10f, 10f, pCntBg)
        canvas.drawRoundRect(counterRect, 10f, 10f, pCntBd)
        canvas.drawText("ESP", cx, bt + 18f, pCntLbl)
        val str = if (count < cntCache.size) cntCache[count] else "$count"
        canvas.drawText(str, cx, bt + bh - 6f, pCntNum)
        canvas.drawLine(cx, bt + bh, cx, bt + bh + 15f, pConn)
    }
}
