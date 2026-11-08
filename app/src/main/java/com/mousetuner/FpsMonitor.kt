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

/** HUD FPS dạng thanh gọn (kiểu Game Turbo): FPS · 1% · frametime · nhiệt độ · ping + sparkline. Dính góc trái dưới. */
class FpsHud(c: Context) : View(c) {
    private var fps = 0f; private var low = 0f; private var ms = 0f; private var cap = 60f
    private var temp = 0f; private var net = -1f
    var compact = false
        set(v) { if (v != field) { field = v; requestLayout() } }
    private var graph = FloatArray(0); private var note: String? = null
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB8000000.toInt() }
    private val lab = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9A9AA0.toInt(); textSize = c.dp(8).toFloat(); typeface = Typeface.DEFAULT_BOLD }
    private val num = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textSize = c.dp(17).toFloat() }
    private val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textSize = c.dp(12).toFloat() }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val div = Paint().apply { color = 0x33FFFFFF; strokeWidth = 1f }
    fun set(f: Float, l: Float, m: Float, g: FloatArray, hz: Float, t: Float = 0f, n: Float = -1f) {
        fps = f; low = l; ms = m; graph = g; cap = hz; temp = t; net = n; note = null; invalidate()
    }
    fun setNote(n: String) { note = n; invalidate() }
    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(context.dp(if (compact) 64 else 196), context.dp(32))
    override fun onDraw(c: Canvas) {
        val d = context.dp(1).toFloat(); val r = 10 * d
        // chỉ bo góc trên-phải: các cạnh trái/dưới thẳng, dính sát góc màn hình
        val shape = Path().apply { addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), floatArrayOf(0f, 0f, r, r, 0f, 0f, 0f, 0f), Path.Direction.CW) }
        c.drawPath(shape, bg)
        if (note != null) { valP.color = 0xFFFFC107.toInt(); c.drawText(note!!, 8 * d, 20 * d, valP); valP.color = Color.WHITE; return }
        val ratio = if (cap > 0) fps / cap else 0f
        num.color = when { ratio >= 0.9f -> 0xFF3DDC84.toInt(); ratio >= 0.6f -> 0xFFFFC107.toInt(); else -> 0xFFFF3B30.toInt() }
        var x = 8 * d
        c.drawText("FPS", x, 11 * d, lab); c.drawText("${fps.toInt()}", x, 27 * d, num); x += 46 * d
        if (compact) return
        val segs = listOf("MS" to "%.1f".format(ms), "TEMP" to "%.0f°".format(temp), "NET" to (if (net < 0) "--" else "${net.toInt()}"))
        for ((l, v) in segs) {
            c.drawLine(x - 6 * d, 6 * d, x - 6 * d, 26 * d, div)
            c.drawText(l, x, 11 * d, lab); c.drawText(v, x, 26 * d, valP); x += 40 * d
        }
        // sparkline frametime (cao = chậm)
        val n = graph.size; val bw = 1.6f * d; val sx = width - 8 * d - 36 * bw
        if (n > 0) for (i in 0 until n.coerceAtMost(36)) {
            val v = graph[n - Math.min(n, 36) + i]; val k = Math.min(v / (2f * 1000f / cap), 1f)
            bar.color = if (v > 1000f / cap * 1.6f) 0xFFFF3B30.toInt() else 0xFF3DDC84.toInt()
            val bx = sx + (36 - Math.min(n, 36) + i) * bw
            c.drawRect(bx, 28 * d - 18 * d * k, bx + bw * 0.8f, 28 * d, bar)
        }
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
        gravity = Gravity.BOTTOM or Gravity.START; x = 0; y = 0      // sát góc trái-dưới
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
            if (empty == 2) main.post { hud.set(0f, 0f, 0f, FloatArray(0), 60f) }
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
        val nv = net
        lastFps = fps; live = fps; liveLow = low; liveHz = hz; liveAt = SystemClock.uptimeMillis()
        main.post { hud.alpha = Cfg.hudAlpha(ctx) / 100f; hud.compact = Cfg.hudCompact(ctx); hud.set(fps, low, if (g.isEmpty()) 0f else g.last(), g, hz, temp, nv) }
    }
}
