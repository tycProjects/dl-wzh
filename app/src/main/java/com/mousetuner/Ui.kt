package com.mousetuner

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.*
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()
fun rounded(c: Int, r: Float) = GradientDrawable().apply { setColor(c); cornerRadius = r }

fun Context.btn(t: String, accent: Boolean = false, a: () -> Unit) = Button(this).apply {
    text = t; isAllCaps = false; setTextColor(Color.WHITE); textSize = 13f
    background = if (accent) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFFFF2A3D.toInt(), 0xFFB00020.toInt())).apply { cornerRadius = dp(12).toFloat() }
        else GradientDrawable().apply { setColor(0xFF1B1B21.toInt()); cornerRadius = dp(12).toFloat(); setStroke(dp(1), 0x33FF2A3D) }
    setPadding(dp(12), dp(4), dp(12), dp(4)); setOnClickListener { a() }
}
fun Context.title(t: String) = TextView(this).apply { text = t; setTextColor(0xFFFF4D5E.toInt()); textSize = 15f; setPadding(0, dp(6), 0, dp(4)) }
fun Context.body(t: String) = TextView(this).apply { text = t; setTextColor(Color.WHITE); textSize = 13f }
fun Context.card() = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(14), dp(8), dp(14), dp(12)); background = rounded(0xFF140A0C.toInt(), dp(14).toFloat())
}
fun Context.row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
fun Context.sw(t: String, on: (Boolean) -> Unit) = Switch(this).apply {
    text = t; setTextColor(Color.WHITE); textSize = 13f; setOnCheckedChangeListener { _, c -> on(c) }
}

/** Thêm view vào LinearLayout với lề; hàng ngang thì chia đều. */
fun LinearLayout.put(v: View): LinearLayout {
    val m = context.dp(4)
    val lp = if (orientation == LinearLayout.HORIZONTAL) LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
             else LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    lp.setMargins(m, m, m, m); addView(v, lp); return this
}

val RED = 0xFFFF2A3D.toInt()
/** Hai bảng màu: Vàng (y chang ảnh mẫu) và Đỏ. */
object Theme {
    @Volatile var gold = true
    @Volatile var rgb = true
    val acc: Int get() = if (gold) 0xFFF7D858.toInt() else 0xFFFF2A3D.toInt()
    val acc2: Int get() = if (gold) 0xFFF0A820.toInt() else 0xFFB00020.toInt()
    val onAcc: Int get() = if (gold) 0xFF1A1400.toInt() else 0xFFFFFFFF.toInt()
    val track: Int get() = if (gold) 0xFFB38F00.toInt() else 0xFF9B0014.toInt()
    val panel: Int get() = if (gold) 0xE61C1B1A.toInt() else 0xE8101014.toInt()
    val rail: Int get() = if (gold) 0xEB151413.toInt() else 0xEB0B0B0F.toInt()
    val line: Int get() = if (gold) 0x22FFFFFF else 0x33FF6B78
    fun load(c: Context) { gold = Cfg.b(c, "golddash", true); rgb = Cfg.b(c, "rgb", true) }
}
val GOLD: Int get() = Theme.acc
fun lpw(w: Int, h: Int, wt: Float = 0f) = LinearLayout.LayoutParams(w, h, wt)

/** Công tắc neon đỏ kiểu gaming. */
class NeonSwitch(c: Context, on: Boolean = false, private val cb: (Boolean) -> Unit = {}) : View(c) {
    var checked = on; private set
    private var t = if (on) 1f else 0f
    private val ev = ArgbEvaluator()
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    init { setLayerType(LAYER_TYPE_SOFTWARE, null); setOnClickListener { set(!checked, true) } }
    fun set(v: Boolean, notify: Boolean = false) {
        if (v == checked) return
        checked = v
        ValueAnimator.ofFloat(t, if (v) 1f else 0f).apply {
            duration = 160; addUpdateListener { t = it.animatedValue as Float; invalidate() }; start()
        }
        if (notify) cb(v)
    }
    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(context.dp(64), context.dp(40))
    override fun onDraw(c: Canvas) {
        val pad = context.dp(6).toFloat()
        val r = RectF(pad, pad, width - pad, height - pad); val rad = r.height() / 2
        track.color = ev.evaluate(t, 0xFF2B2B30.toInt(), Theme.track) as Int
        if (t > 0f) {
            glow.color = GOLD; glow.alpha = (220 * t).toInt()
            glow.maskFilter = BlurMaskFilter(context.dp(6).toFloat(), BlurMaskFilter.Blur.NORMAL)
            c.drawRoundRect(r, rad, rad, glow)
        }
        c.drawRoundRect(r, rad, rad, track)
        val tr = rad - context.dp(3)
        val cx = (r.left + rad) + ((r.right - rad) - (r.left + rad)) * t
        c.drawCircle(cx, r.centerY(), tr, thumb)
    }
}

fun Context.gcard() = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(16), dp(12), dp(16), dp(14))
    background = GradientDrawable().apply { setColor(0x12FFFFFF); cornerRadius = dp(18).toFloat(); setStroke(dp(1), 0x33FF2A3D) }
}

/** Thẻ tính năng: tiêu đề + mô tả + công tắc, kèm phần mở rộng bên dưới. */
fun Context.feature(t: String, d: String, s: NeonSwitch?, extra: View? = null): LinearLayout {
    val c = gcard()
    val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val txt = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    txt.addView(TextView(this).apply { text = t; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD })
    top.addView(txt, lpw(0, ViewGroup_WRAP, 1f))
    if (s != null) top.addView(s, lpw(ViewGroup_WRAP, ViewGroup_WRAP).apply { leftMargin = dp(12) })
    c.addView(top); if (extra != null) c.addView(extra)
    return c
}
const val ViewGroup_WRAP = -2

fun Context.slider(label: String, maxV: Int, init: Int, fmt: (Int) -> String, done: (Int) -> Unit): LinearLayout {
    val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, 0) }
    val tv = body(label + fmt(init))
    val sb = SeekBar(this).apply {
        max = maxV; progress = init
        progressTintList = ColorStateList.valueOf(GOLD); thumbTintList = ColorStateList.valueOf(GOLD)
    }
    sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { tv.text = label + fmt(p) }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) { done(sb.progress) }
    })
    l.addView(tv); l.addView(sb); return l
}

fun Context.sectionTitle(t: String) = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(2), 0, dp(6))
    addView(View(context).apply { background = rounded(GOLD, dp(2).toFloat()) }, LinearLayout.LayoutParams(dp(4), dp(16)))
    addView(TextView(context).apply { text = t; setTextColor(Color.WHITE); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.12f; setPadding(dp(8), 0, 0, 0) })
}
fun Context.chip(t: String, ok: Boolean?) = TextView(this).apply {
    val col = when (ok) { true -> 0xFF3DDC84.toInt(); false -> 0xFFFF3B30.toInt(); null -> 0xFF9A9AA0.toInt() }
    text = "● $t"; setTextColor(col); textSize = 12f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
    setPadding(dp(10), dp(8), dp(10), dp(8))
    background = GradientDrawable().apply { setColor(0x14FFFFFF); cornerRadius = dp(10).toFloat(); setStroke(dp(1), (col and 0x00FFFFFF) or 0x55000000) }
}

/** Nút gradient lớn: primary = đỏ neon, còn lại = tối viền đỏ. */
fun Context.gbtn(t: String, primary: Boolean, a: () -> Unit) = Button(this).apply {
    text = t; isAllCaps = false; setTextColor(if (primary) Theme.onAcc else Color.WHITE); textSize = 13f; typeface = Typeface.DEFAULT_BOLD
    background = if (primary) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Theme.acc, Theme.acc2)).apply { cornerRadius = dp(14).toFloat() }
        else GradientDrawable().apply { setColor(0xFF1B1B21.toInt()); cornerRadius = dp(14).toFloat(); setStroke(dp(1), 0x55FF2A3D) }
    setPadding(dp(14), dp(10), dp(14), dp(10)); setOnClickListener { a() }
}

/** Bộ chọn phân đoạn kiểu Game Space (mục chọn = nền vàng). */
fun Context.seg(opts: List<String>, sel: Int, onSel: (Int) -> Unit): LinearLayout {
    val g = Theme.gold
    val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(4), dp(4), dp(4), dp(4)); background = rounded(if (g) 0xFF2A2927.toInt() else 0xFF1C1C22.toInt(), dp(if (g) 20 else 14).toFloat()) }
    val tvs = ArrayList<TextView>()
    fun paint(s: Int) = tvs.forEachIndexed { i, v ->
        v.setTextColor(if (i == s) Theme.onAcc else if (g) 0xFF8E8C88.toInt() else 0xFF9A9AA0.toInt())
        v.typeface = if (i == s) Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC) else Typeface.DEFAULT
        v.background = if (i == s) GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(if (g) 0xFFFADF62.toInt() else Theme.acc, Theme.acc2)).apply { cornerRadius = dp(if (g) 17 else 11).toFloat() } else null
    }
    opts.forEachIndexed { i, t ->
        val v = TextView(this).apply { text = t; gravity = Gravity.CENTER; textSize = if (g) 15f else 12f; setPadding(dp(6), dp(if (g) 14 else 10), dp(6), dp(if (g) 14 else 10)); setOnClickListener { paint(i); onSel(i) } }
        tvs.add(v); r.addView(v, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
    }
    paint(sel); return r
}

/** Đồng hồ kiểu điện thoại Gaming: vòng tua gồm các thanh vát chéo sáng dần, vùng redline, mũi chỉ giá trị, khung bát giác HUD ở giữa,
 *  vòng đứt nét xoay, làn sóng sáng chạy trên các thanh, số in nghiêng kiểu đồng hồ tốc độ. */
class Gauge(c: Context, private val name: String, private var unit: String, private val sz: Int = 120) : View(c) {
    private var shown = 0f; private var txt = "--"; private var phase = 0f; private var lastSet = ""
    private var fromF = 0f; private var toF = 0f; private var fromN = Float.NaN; private var toN = Float.NaN; private var dec = 0; private var numShown = Float.NaN
    private var tween: ValueAnimator? = null
    private val k = sz / 120f
    private val n = if (sz >= 110) 40 else 26
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private var segs: Array<Path> = emptyArray()
    private var popNs = 0L; private val rnd = java.util.Random(); private var tHue = 0f
    private fun hue(h: Float): Int = Color.HSVToColor(floatArrayOf((((h % 1f) + 1f) % 1f) * 360f, 0.85f, 1f))
    private val loop = ValueAnimator.ofFloat(0f, 1f).apply { duration = 2600; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); addUpdateListener { phase = it.animatedValue as Float; invalidate() } }

    fun set(f: Float, v: String, u: String? = null) = post {
        if (u != null) unit = u
        if (v != lastSet) { lastSet = v; popNs = System.nanoTime() }
        val nv = v.toFloatOrNull(); dec = if (nv != null && v.contains('.')) v.length - v.indexOf('.') - 1 else 0
        fromF = shown; toF = f.coerceIn(0f, 1f); fromN = if (numShown.isNaN()) (nv ?: Float.NaN) else numShown; toN = nv ?: Float.NaN
        if (nv == null) { numShown = Float.NaN; txt = v }
        tween?.cancel()
        tween = ValueAnimator.ofFloat(0f, 1f).apply { duration = 750; interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { val e = it.animatedValue as Float; shown = fromF + (toF - fromF) * e
                if (!toN.isNaN() && !fromN.isNaN()) numShown = fromN + (toN - fromN) * e else if (!toN.isNaN()) numShown = toN }
            start() }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); if (!loop.isStarted) loop.start() }
    override fun onDetachedFromWindow() { loop.cancel(); tween?.cancel(); super.onDetachedFromWindow() }
    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(context.dp(sz), context.dp(sz))

    private fun pt(cx: Float, cy: Float, r: Float, deg: Float) = Math.toRadians(deg.toDouble()).let { floatArrayOf(cx + (r * Math.cos(it)).toFloat(), cy + (r * Math.sin(it)).toFloat()) }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val d = context.dp(1) * k; val cx = w / 2f; val cy = h / 2f
        val ro = w / 2f - 11f * d; val ri = ro - 15f * d; val step = 270f / n; val sw = step * 0.7f; val skew = step * 0.32f
        segs = Array(n) { i -> val a0 = 135f + i * step + (step - sw) / 2f; val a1 = a0 + sw
            val p1 = pt(cx, cy, ro, a0); val p2 = pt(cx, cy, ro, a1); val p3 = pt(cx, cy, ri, a1 + skew); val p4 = pt(cx, cy, ri, a0 + skew)
            Path().apply { moveTo(p1[0], p1[1]); lineTo(p2[0], p2[1]); lineTo(p3[0], p3[1]); lineTo(p4[0], p4[1]); close() } }
    }
    private fun lerp(a: Int, b: Int, f: Float): Int { val x = f.coerceIn(0f, 1f)
        return Color.argb(255, (Color.red(a) + (Color.red(b) - Color.red(a)) * x).toInt(), (Color.green(a) + (Color.green(b) - Color.green(a)) * x).toInt(), (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * x).toInt()) }
    private fun mix3(a: Int, b: Int, c: Int, x: Float) = if (x < 0.5f) lerp(a, b, x * 2f) else lerp(b, c, (x - 0.5f) * 2f)
    private fun segColor(f: Float): Int = if (Theme.rgb) hue(f * 0.7f + tHue) else if (Theme.gold) { if (f > 0.85f) 0xFFFF5A2E.toInt() else mix3(0xFFF0A820.toInt(), 0xFFF7D858.toInt(), 0xFFFFF1C2.toInt(), f / 0.85f) }
        else { if (f > 0.85f) 0xFFFFD2B8.toInt() else mix3(0xFFB00020.toInt(), 0xFFFF2A3D.toInt(), 0xFFFF7A4D.toInt(), f / 0.85f) }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime(); val d = context.dp(1) * k; val cx = width / 2f; val cy = height / 2f
        tHue = (now / 1e9f * 0.14f) % 1f; val acc = if (Theme.rgb) hue(tHue) else Theme.acc; val ar = Color.red(acc); val ag = Color.green(acc); val ab = Color.blue(acc)
        val pulse = (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI)).toFloat()
        val outerR = width / 2f - 2.5f * d; val ro = width / 2f - 11f * d; val ri = ro - 15f * d
        c.save()
        if (shown > 0.93f) { val j = (shown - 0.93f) * 14f; c.translate((rnd.nextFloat() - .5f) * 2f * d * j, (rnd.nextFloat() - .5f) * 2f * d * j) }
        // nền đĩa
        fill.shader = RadialGradient(cx, cy, ri, intArrayOf(Color.argb((70 + 40 * pulse).toInt(), ar, ag, ab), 0xCC0A0A0C.toInt()), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP); c.drawCircle(cx, cy, ri, fill); fill.shader = null
        // vành ngoài + vạch chia
        stroke.strokeCap = Paint.Cap.BUTT; stroke.pathEffect = null
        stroke.strokeWidth = 1.2f * d; stroke.color = 0x33FFFFFF; c.drawArc(RectF(cx - outerR, cy - outerR, cx + outerR, cy + outerR), 135f, 270f, false, stroke)
        stroke.strokeWidth = 1.8f * d; stroke.color = 0x88FFFFFF.toInt()
        for (i in 0..4) { val a = 135f + 67.5f * i; val p1 = pt(cx, cy, outerR, a); val p2 = pt(cx, cy, outerR - 5f * d, a); c.drawLine(p1[0], p1[1], p2[0], p2[1], stroke) }
        // thanh chưa sáng
        for (i in 0 until n) { fill.color = if (Theme.gold) 0xFF21201E.toInt() else 0xFF1F1B1E.toInt(); c.drawPath(segs[i], fill) }
        stroke.strokeWidth = 0.8f * d; stroke.color = 0x22FFFFFF; for (i in 0 until n) c.drawPath(segs[i], stroke)
        // quầng sáng dưới các thanh sáng
        val lit = shown * n
        if (lit > 0.05f) {
            val mid = (ro + ri) / 2f; val rect = RectF(cx - mid, cy - mid, cx + mid, cy + mid); val gc = segColor(shown)
            stroke.strokeCap = Paint.Cap.BUTT
            for (i in 4 downTo 1) { stroke.strokeWidth = (ro - ri) + i * 4.5f * d; stroke.color = Color.argb(((14 + 16 * pulse) / i * 1.8f).toInt().coerceIn(0, 255), Color.red(gc), Color.green(gc), Color.blue(gc)); c.drawArc(rect, 135f, 270f * shown, false, stroke) }
            val wave = phase * (lit + 8f) - 4f
            for (i in 0 until Math.ceil(lit.toDouble()).toInt().coerceAtMost(n)) {
                val f = i / (n - 1f); var col = segColor(f)
                val w = (1f - Math.abs(i - wave) / 3.5f).coerceIn(0f, 1f); col = lerp(col, Color.WHITE, w * 0.55f)
                val isPeak = i == Math.ceil(lit.toDouble()).toInt() - 1
                if (isPeak) col = lerp(col, Color.WHITE, 0.25f + 0.35f * pulse)
                val part = if (isPeak) (lit - i).coerceIn(0.25f, 1f) else 1f
                fill.color = Color.argb((255 * part).toInt(), Color.red(col), Color.green(col), Color.blue(col)); c.drawPath(segs[i], fill)
            }
            // mũi chỉ giá trị
            val ang = 135f + 270f * shown; val tip = pt(cx, cy, outerR + 1f * d, ang); val l = pt(cx, cy, outerR + 9f * d, ang - 3.2f); val r = pt(cx, cy, outerR + 9f * d, ang + 3.2f)
            fill.color = Color.argb((70 + 70 * pulse).toInt(), Color.red(gc), Color.green(gc), Color.blue(gc)); c.drawCircle(tip[0], tip[1], 6f * d, fill)
            fill.color = Color.WHITE; c.drawPath(Path().apply { moveTo(tip[0], tip[1]); lineTo(l[0], l[1]); lineTo(r[0], r[1]); close() }, fill)
        }
        // vòng đứt nét xoay + khung bát giác
        stroke.strokeCap = Paint.Cap.BUTT; stroke.strokeWidth = 1.2f * d; stroke.color = Color.argb(120, ar, ag, ab)
        stroke.pathEffect = DashPathEffect(floatArrayOf(3f * d, 6f * d), -phase * 54f * d); c.drawCircle(cx, cy, ri - 4f * d, stroke); stroke.pathEffect = null
        val rc = ri - 11f * d; val oct = Path()
        for (i in 0 until 8) { val q = pt(cx, cy, rc, 22.5f + 45f * i); if (i == 0) oct.moveTo(q[0], q[1]) else oct.lineTo(q[0], q[1]) }; oct.close()
        stroke.strokeWidth = 1.4f * d; stroke.color = Color.argb((90 + 60 * pulse).toInt(), ar, ag, ab); c.drawPath(oct, stroke)
        fill.color = Color.argb(230, ar, ag, ab); for (i in 0..3) { val q = pt(cx, cy, rc * 0.99f, 22.5f + 90f * i + 22.5f); c.drawCircle(q[0], q[1], 2f * d, fill) }
        // chữ
        val fs = width * 0.255f; val pop = (1f - (now - popNs) / 3.2e8f).coerceIn(0f, 1f)
        t.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD); t.letterSpacing = 0.28f
        t.color = Color.argb(255, ar, ag, ab); t.textSize = fs * 0.36f; c.drawText(name, cx, cy - fs * 0.62f, t); t.letterSpacing = 0f
        val shownTxt = if (!numShown.isNaN()) (if (dec > 0) "%.${dec}f".format(java.util.Locale.US, numShown) else "${Math.round(numShown)}") else txt
        t.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC)
        c.save(); val sc = 1f + 0.12f * pop; c.scale(sc, sc, cx, cy + fs * 0.2f)
        t.textSize = fs; t.color = Color.WHITE; t.setShadowLayer(9f * d, 0f, 0f, Color.argb((90 + 70 * pulse + 70 * pop).toInt().coerceIn(0, 255), ar, ag, ab))
        c.drawText(shownTxt, cx, cy + fs * 0.36f, t); t.clearShadowLayer(); c.restore()
        if (unit.isNotEmpty()) { t.typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL); t.letterSpacing = 0.2f; t.color = 0xFF8E8A84.toInt(); t.textSize = fs * 0.3f; c.drawText(unit, cx, cy + fs * 0.82f, t); t.letterSpacing = 0f }
        c.restore()
    }
}

/** Hình nền động: nền đỏ–đen vẽ sẵn một lần + vệt sáng trôi + tàn lửa bay lên. */
class WallView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bg: Bitmap? = null
    private val N = 48
    private val ex = FloatArray(N); private val ey = FloatArray(N); private val es = FloatArray(N); private val ev = FloatArray(N); private val eph = FloatArray(N)
    private var t = 0f; private var lastNs = 0L
    private val rnd = java.util.Random()
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply { duration = 1000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); addUpdateListener { invalidate() } }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        if (w <= 0 || h <= 0) return
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); paintStatic(Canvas(b), w.toFloat(), h.toFloat()); bg = b
        for (i in 0 until N) respawn(i, true)
    }
    private fun respawn(i: Int, any: Boolean) {
        ex[i] = rnd.nextFloat() * width; ey[i] = if (any) rnd.nextFloat() * height else height + 10f
        es[i] = (1f + rnd.nextFloat() * 2.6f) * context.dp(1); ev[i] = (14f + rnd.nextFloat() * 40f) * context.dp(1); eph[i] = rnd.nextFloat() * 6.28f
    }
    private fun paintStatic(c: Canvas, w: Float, h: Float) {
        p.shader = LinearGradient(0f, 0f, w, h, if (Theme.gold) intArrayOf(0xFF0A0908.toInt(), 0xFF17120C.toInt(), 0xFF2A1D0C.toInt()) else intArrayOf(0xFF07070A.toInt(), 0xFF1E0509.toInt(), 0xFF3A0610.toInt()), null, Shader.TileMode.CLAMP); c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.85f, h * 0.1f, w * 0.55f, if (Theme.gold) 0x55F7B733 else 0x66FF2A3D, 0x00FF2A3D, Shader.TileMode.CLAMP); c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.2f, h * 1.05f, w * 0.5f, if (Theme.gold) 0x44C77B10 else 0x55B00020, 0x00B00020, Shader.TileMode.CLAMP); c.drawRect(0f, 0f, w, h, p)
        p.shader = null; p.style = Paint.Style.FILL
        for (i in 0..5) { p.color = if (i % 2 == 0) 0x14FF2A3D else 0x0AFFFFFF
            val x = w * (0.35f + i * 0.14f); val path = Path(); path.moveTo(x, 0f); path.lineTo(x + w * 0.05f, 0f); path.lineTo(x - w * 0.25f, h); path.lineTo(x - w * 0.30f, h); path.close(); c.drawPath(path, p) }
        p.color = 0x08FFFFFF; p.strokeWidth = 1f; val g = context.dp(28).toFloat(); var x = 0f
        while (x < w) { c.drawLine(x, 0f, x, h, p); x += g }
        var y = 0f; while (y < h) { c.drawLine(0f, y, w, y, p); y += g }
        p.shader = RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.75f, 0x00000000, 0xAA000000.toInt(), Shader.TileMode.CLAMP); c.drawRect(0f, 0f, w, h, p); p.shader = null
    }
    override fun onDraw(c: Canvas) {
        val now = System.nanoTime(); val dt = if (lastNs == 0L) 0.016f else ((now - lastNs) / 1e9f).coerceAtMost(0.05f); lastNs = now; t += dt
        bg?.let { c.drawBitmap(it, 0f, 0f, null) }
        val w = width.toFloat(); val h = height.toFloat()
        // chùm sáng trôi ngang
        val bx = ((t * 0.06f) % 1.6f - 0.3f) * w
        p.style = Paint.Style.FILL; p.shader = LinearGradient(bx, 0f, bx + w * 0.25f, h * 0.3f, intArrayOf(0x00FF2A3D, 0x1AFF4060, 0x00FF2A3D), null, Shader.TileMode.CLAMP); c.drawRect(0f, 0f, w, h, p); p.shader = null
        // tàn lửa
        for (i in 0 until N) {
            ey[i] -= ev[i] * dt; ex[i] += Math.sin((t * 1.4f + eph[i]).toDouble()).toFloat() * 14f * dt * context.dp(1)
            if (ey[i] < -10f) respawn(i, false)
            val a = (0.35f + 0.65f * (0.5f + 0.5f * Math.sin((t * 3f + eph[i]).toDouble()).toFloat())) * (ey[i] / h).coerceIn(0f, 1f)
            if (Theme.rgb) { val hc = Color.HSVToColor(floatArrayOf((t * 40f + eph[i] * 57f) % 360f, 0.85f, 1f))
                p.color = Color.argb((150 * a).toInt(), Color.red(hc), Color.green(hc), Color.blue(hc)); c.drawCircle(ex[i], ey[i], es[i] * 2.4f, p)
                p.color = Color.argb((230 * a).toInt(), 255, 255, 255); c.drawCircle(ex[i], ey[i], es[i], p)
            } else {
                p.color = Color.argb((150 * a).toInt(), 255, 120, 70); c.drawCircle(ex[i], ey[i], es[i] * 2.4f, p)
                p.color = Color.argb((230 * a).toInt(), 255, 200, 150); c.drawCircle(ex[i], ey[i], es[i], p)
            }
        }
    }
}

/** Khung kính tối bo tròn (thanh bên + bảng chính). */
fun Context.glass(r: Int, fill: Int) = GradientDrawable().apply { setColor(fill); cornerRadius = dp(r).toFloat(); setStroke(dp(1), Theme.line) }

/** Mục điều hướng trên thanh bên (biểu tượng + nhãn). */
fun Context.navItem(icon: String, label: String, sel: Boolean, click: () -> Unit) = TextView(this).apply {
    text = "$icon\n$label"; gravity = Gravity.CENTER; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD
    setPadding(dp(2), dp(10), dp(2), dp(10))
    if (Theme.gold) setTextColor(if (sel) Theme.acc else 0xFFEDE8DC.toInt())
    else { setTextColor(if (sel) Color.WHITE else 0xFFB0A8AA.toInt()); if (sel) background = GradientDrawable().apply { setColor(0x33FF2A3D); cornerRadius = dp(16).toFloat(); setStroke(dp(1), RED) } }
    setOnClickListener { click() }
}

/** Khung viền RGB: dải cầu vồng chạy vòng quanh khung bo tròn (tắt được trong Game Center). */
class RgbFrame(c: Context, private val r: Int) : FrameLayout(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = c.dp(2).toFloat() }
    private var rot = 0f
    private val anim = ValueAnimator.ofFloat(0f, 360f).apply { duration = 5000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); addUpdateListener { rot = it.animatedValue as Float; if (Theme.rgb) invalidate() } }
    private val cols = intArrayOf(0xFFFF0040.toInt(), 0xFFFFE600.toInt(), 0xFF00FF66.toInt(), 0xFF00E5FF.toInt(), 0xFF3D5AFF.toInt(), 0xFFFF00E5.toInt(), 0xFFFF0040.toInt())
    init { setWillNotDraw(false) }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }
    override fun dispatchDraw(c: Canvas) {
        super.dispatchDraw(c)
        if (!Theme.rgb || width == 0) return
        val sh = SweepGradient(width / 2f, height / 2f, cols, null); sh.setLocalMatrix(Matrix().apply { postRotate(rot, width / 2f, height / 2f) })
        p.shader = sh; val h = p.strokeWidth / 2f
        p.maskFilter = null; c.drawRoundRect(RectF(h, h, width - h, height - h), context.dp(r).toFloat(), context.dp(r).toFloat(), p)
    }
}
