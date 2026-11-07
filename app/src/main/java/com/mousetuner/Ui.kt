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
        track.color = ev.evaluate(t, 0xFF2B2B30.toInt(), 0xFF9B0014.toInt()) as Int
        if (t > 0f) {
            glow.color = RED; glow.alpha = (220 * t).toInt()
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
    background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFF1A1A20.toInt(), 0xFF0E0E12.toInt())).apply { cornerRadius = dp(18).toFloat(); setStroke(dp(1), 0x55FF2A3D) }
}

/** Thẻ tính năng: tiêu đề + mô tả + công tắc, kèm phần mở rộng bên dưới. */
fun Context.feature(t: String, d: String, s: NeonSwitch?, extra: View? = null): LinearLayout {
    val c = gcard()
    val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val txt = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    txt.addView(TextView(this).apply { text = t; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD })
    txt.addView(TextView(this).apply { text = d; setTextColor(0xFF9A9AA0.toInt()); textSize = 12f; setPadding(0, dp(4), 0, 0) })
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
        progressTintList = ColorStateList.valueOf(RED); thumbTintList = ColorStateList.valueOf(RED)
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
    addView(View(context).apply { background = rounded(RED, dp(2).toFloat()) }, LinearLayout.LayoutParams(dp(4), dp(16)))
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
    text = t; isAllCaps = false; setTextColor(Color.WHITE); textSize = 13f; typeface = Typeface.DEFAULT_BOLD
    background = if (primary) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFFFF2A3D.toInt(), 0xFFB00020.toInt())).apply { cornerRadius = dp(14).toFloat() }
        else GradientDrawable().apply { setColor(0xFF1B1B21.toInt()); cornerRadius = dp(14).toFloat(); setStroke(dp(1), 0x55FF2A3D) }
    setPadding(dp(14), dp(10), dp(14), dp(10)); setOnClickListener { a() }
}
