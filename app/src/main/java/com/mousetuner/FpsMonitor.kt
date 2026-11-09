package com.mousetuner

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/** Monitoring FPS kiểu điện thoại gaming: FPS lớn đổi màu theo độ mượt, đồ thị frametime, CPU/GPU %, xung nhịp, nhiệt độ, ping. Viền RGB khi bật. */
class FpsHud(c: Context) : View(c) {
    private var fps = 0f; private var ms = 0f; private var cap = 60f; private var temp = 0f; private var net = -1f
    private var sys: Sys? = null
    var compact = false
        set(v) { if (v != field) { field = v; requestLayout() } }
    private var graph = FloatArray(0); private var note: String? = null; private var rot = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cols = intArrayOf(0xFFFF0040.toInt(), 0xFFFFE600.toInt(), 0xFF00FF66.toInt(), 0xFF00E5FF.toInt(), 0xFF3D5AFF.toInt(), 0xFFFF00E5.toInt(), 0xFFFF0040.toInt())
    private val anim = android.animation.ValueAnimator.ofFloat(0f, 360f).apply { duration = 4000; repeatCount = android.animation.ValueAnimator.INFINITE; interpolator = android.view.animation.LinearInterpolator(); addUpdateListener { rot = it.animatedValue as Float; invalidate() } }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }
    fun set(f: Float, m: Float, g: FloatArray, hz: Float, t: Float, n: Float, s: Sys?) { fps = f; ms = m; graph = g; cap = hz; temp = t; net = n; sys = s; note = null; invalidate() }
    fun setNote(n: String) { note = n; invalidate() }
    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(context.dp(if (compact) 92 else 262), context.dp(if (compact) 44 else 68))
    private fun cell(c: Canvas, x: Float, y: Float, lab: String, v: String, hot: Boolean, d: Float) {
        p.style = Paint.Style.FILL; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 7.5f * d; p.color = 0xFF8E8A84.toInt(); c.drawText(lab, x, y, p)
        p.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD); p.textSize = 11f * d; p.color = if (hot) 0xFFFF5A2E.toInt() else Color.WHITE; c.drawText(v, x + 22f * d, y, p)
    }
    override fun onDraw(c: Canvas) {
        val d = context.dp(1).toFloat(); val w = width.toFloat(); val h = height.toFloat(); val r = 14f * d
        val rect = RectF(1.2f * d, 1.2f * d, w - 1.2f * d, h - 1.2f * d)
        p.style = Paint.Style.FILL; p.shader = LinearGradient(0f, 0f, 0f, h, 0xE8121216.toInt(), 0xEE060608.toInt(), Shader.TileMode.CLAMP); c.drawRoundRect(rect, r, r, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f * d
        if (Theme.rgb) p.shader = SweepGradient(w / 2f, h / 2f, cols, null).also { it.setLocalMatrix(Matrix().apply { postRotate(rot, w / 2f, h / 2f) }) } else p.color = Color.argb(190, Color.red(Theme.acc), Color.green(Theme.acc), Color.blue(Theme.acc))
        c.drawRoundRect(rect, r, r, p); p.shader = null; p.style = Paint.Style.FILL
        if (note != null) { p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 10f * d; p.color = 0xFFFFC107.toInt(); c.drawText(note!!, 12f * d, h / 2f + 4f * d, p); return }
        val ratio = if (cap > 0) fps / cap else 0f
        val fc = when { ratio >= 0.9f -> 0xFF3DDC84.toInt(); ratio >= 0.6f -> 0xFFFFC107.toInt(); else -> 0xFFFF3B30.toInt() }
        p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 8.5f * d; p.letterSpacing = 0.25f; p.color = Theme.acc; c.drawText("FPS", 13f * d, 16f * d, p); p.letterSpacing = 0f
        p.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC); p.textSize = (if (compact) 26f else 32f) * d; p.color = fc
        p.setShadowLayer(8f * d, 0f, 0f, Color.argb(120, Color.red(fc), Color.green(fc), Color.blue(fc))); c.drawText("${fps.toInt()}", 12f * d, h - 11f * d, p); p.clearShadowLayer()
        if (compact) return
        // đồ thị frametime (cao = chậm), đường đứt = mục tiêu
        val gx = 74f * d; val gw = 90f * d; val gy = 12f * d; val gh = h - 24f * d
        val target = 1000f / cap; val mx = target * 2.2f
        p.style = Paint.Style.STROKE; p.strokeWidth = 1f * d; p.color = 0x55FFFFFF; p.pathEffect = DashPathEffect(floatArrayOf(3f * d, 3f * d), 0f)
        val ty = gy + gh * (1f - target / mx); c.drawLine(gx, ty, gx + gw, ty, p); p.pathEffect = null
        val n = Math.min(graph.size, 44)
        if (n > 1) {
            val pts = FloatArray(n) { graph[graph.size - n + it].coerceIn(0f, mx) }
            val line = Path(); val fill = Path()
            for (i in 0 until n) { val x = gx + gw * i / (n - 1f); val y = gy + gh * (1f - pts[i] / mx); if (i == 0) { line.moveTo(x, y); fill.moveTo(x, gy + gh); fill.lineTo(x, y) } else { line.lineTo(x, y); fill.lineTo(x, y) } }
            fill.lineTo(gx + gw, gy + gh); fill.close()
            p.style = Paint.Style.FILL; p.shader = LinearGradient(0f, gy, 0f, gy + gh, Color.argb(110, Color.red(fc), Color.green(fc), Color.blue(fc)), 0x00000000, Shader.TileMode.CLAMP); c.drawPath(fill, p); p.shader = null
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f * d; p.color = fc; p.strokeJoin = Paint.Join.ROUND; c.drawPath(line, p)
            p.style = Paint.Style.FILL; p.color = 0xFFFF3B30.toInt()
            for (i in 0 until n) if (pts[i] > target * 1.6f) c.drawCircle(gx + gw * i / (n - 1f), gy + gh * (1f - pts[i] / mx), 1.7f * d, p)
        }
        // lưới số liệu 3x2
        val s = sys; val x1 = 174f * d; val x2 = 218f * d
        fun f1(v: Float, fmt: String) = if (v < 0) "--" else fmt.format(java.util.Locale.US, v)
        cell(c, x1, 22f * d, "CPU", f1(s?.cpu ?: -1f, "%.0f%%"), (s?.cpu ?: 0f) > 85f, d); cell(c, x2, 22f * d, "GPU", f1(s?.gpu ?: -1f, "%.0f%%"), (s?.gpu ?: 0f) > 85f, d)
        cell(c, x1, 40f * d, "GHZ", f1(s?.ghz ?: -1f, "%.2f"), false, d); cell(c, x2, 40f * d, "MHZ", if ((s?.mhz ?: -1) < 0) "--" else "${s!!.mhz}", false, d)
        cell(c, x1, 58f * d, "TEMP", f1(temp, "%.0f°"), temp > 43f, d); cell(c, x2, 58f * d, "PING", if (net < 0) "--" else "${net.toInt()}", net > 120f, d)
    }
}

/** Tìm layer SurfaceFlinger của game để đo FPS. */
fun findGameLayer(pkg: String): String? {
    val ls = ShizukuShell.exec("dumpsys SurfaceFlinger --list").second.lines().map { it.trim() }
        .filter { it.contains(pkg) && !it.startsWith("Background for") && !it.startsWith("Bounds for") && !it.contains("'") }
    return ls.firstOrNull { it.startsWith("SurfaceView") && it.contains("BLAST") }
        ?: ls.firstOrNull { it.startsWith("SurfaceView") } ?: ls.firstOrNull { it.contains("BLAST") } ?: ls.firstOrNull()
}

/** Đo FPS thật của game từ SurfaceFlinger (thời điểm frame được present lên màn hình). */
class FpsMonitor(private val ctx: Context) {
    companion object {
        @Volatile var live = 0f; @Volatile var liveLow = 0f; @Volatile var liveAt = 0L; @Volatile var liveHz = 60f
        /** Số FPS thật mới nhất (null nếu quá 3 giây chưa có khung hình → không ở trong game). */
        fun fresh(): Float? = if (SystemClock.uptimeMillis() - liveAt < 3000 && live > 0f) live else null
    }
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private val hud = FpsHud(ctx)
    @Volatile private var shown = false
    @Volatile var lastFps = 0f
    private var layer: String? = null; private var layerPkg: String? = null
    private var lastTs = 0L; private var empty = 0
    @Volatile private var net = -1f; @Volatile private var pingBusy = false; private var tk = 0
    private val ts = ArrayDeque<Long>()          // present-time trong cửa sổ 1s
    private val iv = ArrayDeque<Float>()         // frametime (ms), tối đa ~5s để tính 1% low

    private fun lp() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START; x = ctx.dp(8); y = ctx.dp(6)      // góc trái-trên như điện thoại gaming
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    private fun show() { if (shown) return; shown = true; main.post { try { wm.addView(hud, lp()) } catch (_: Throwable) {} } }
    fun hide() { if (!shown) return; shown = false; main.post { try { wm.removeView(hud) } catch (_: Throwable) {} } }

    /** Gọi trên luồng nền, ~mỗi 0.7s khi game đang chạy. */
    fun tick(pkg: String) {
        if (Cfg.fps(ctx)) show() else hide()
        if (layerPkg != pkg) { layer = null; layerPkg = pkg; lastTs = 0; ts.clear(); iv.clear() }
        if (layer == null) {
            layer = findGameLayer(pkg)
            if (layer == null) { main.post { hud.setNote("Không thấy layer game") }; return }
        }
        val out = ShizukuShell.exec("dumpsys SurfaceFlinger --latency '$layer'").second.lines()
        val fresh = out.drop(1).mapNotNull { it.trim().split(Regex("\\s+")).getOrNull(1)?.toLongOrNull() }
            .filter { it > lastTs && it < Long.MAX_VALUE / 2 }.sorted()
        if (fresh.isEmpty()) {
            empty++
            if (empty == 2) main.post { hud.set(0f, 0f, FloatArray(0), 60f, 0f, -1f, null) }
            if (empty >= 4) layer = null
            return
        }
        empty = 0
        for (t in fresh) {
            if (lastTs > 0) { val d = (t - lastTs) / 1e6f; if (d < 500f) { iv.addLast(d); if (iv.size > 600) iv.removeFirst() } }
            ts.addLast(t); lastTs = t
        }
        while (ts.size > 1 && ts.last() - ts.first() > 1_000_000_000L) ts.removeFirst()
        if (ts.size < 2) return
        val span = (ts.last() - ts.first()) / 1e9
        val fps = ((ts.size - 1) / span).toFloat()
        val worst = iv.sortedDescending(); val k = Math.max(1, worst.size / 100)
        val low = if (worst.isEmpty()) fps else 1000f / (worst.take(k).average().toFloat())
        val g = iv.toList().takeLast(60).toFloatArray()
        @Suppress("DEPRECATION") val hz = wm.defaultDisplay.refreshRate
        val bi = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        if (tk++ % 6 == 0 && !pingBusy) {                      // đo ping ~4s/lần, không chặn vòng đo FPS
            pingBusy = true
            Thread {
                val o = ShizukuShell.exec("ping -c 1 -W 1 1.1.1.1").second
                net = Regex("time=([0-9.]+)").find(o)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
                pingBusy = false
            }.start()
        }
        val nv = net; val sys = ShizukuShell.sysStats()
        lastFps = fps; live = fps; liveLow = low; liveHz = hz; liveAt = SystemClock.uptimeMillis()
        main.post { hud.alpha = Cfg.hudAlpha(ctx) / 100f; hud.compact = Cfg.hudCompact(ctx); hud.set(fps, if (g.isEmpty()) 0f else g.last(), g, hz, temp, nv, sys) }
    }
}
