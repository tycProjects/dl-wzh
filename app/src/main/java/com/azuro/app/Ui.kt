package com.azuro.app

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object T {
    var dark = false
    fun init(c: Context) { dark = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES }
    private fun p(l: String, d: String) = Color.parseColor(if (dark) d else l)
    val bg get() = p("#F5F7FC", "#0E1322")
    val card get() = p("#FFFFFF", "#171E33")
    val ink get() = p("#0F1B3D", "#EAF0FF")
    val mut get() = p("#6B7794", "#8E9AB8")
    val line get() = p("#E3E8F3", "#252E4A")
    val pri get() = p("#2F6BFF", "#5B8CFF")
    val priSoft get() = p("#E8EFFF", "#1D2B54")
    val ok get() = p("#1E9E6A", "#3DD598")
    val okSoft get() = p("#E3F6EE", "#143A2E")
    val field get() = p("#EEF2FA", "#1F2740")
    val err get() = p("#D93F4B", "#FF7A85")
    val mark = Color.parseColor("#FF4D6D")
}

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()
fun Context.dpf(v: Float) = v * resources.displayMetrics.density

fun Context.shape(color: Int, r: Float, stroke: Int = 0, sw: Int = 1) = GradientDrawable().apply {
    setColor(color); cornerRadius = r; if (stroke != 0) setStroke(sw, stroke)
}
fun Context.topShape(color: Int, r: Float) = GradientDrawable().apply {
    setColor(color); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
}
fun Context.ripple(base: Drawable, r: Float): Drawable =
    RippleDrawable(ColorStateList.valueOf(if (T.dark) 0x33FFFFFF else 0x14000000), base, shape(Color.WHITE, r))

fun Context.tv(t: CharSequence, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
    text = t; textSize = sp; setTextColor(color); includeFontPadding = false
    typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
}
fun Context.icon(res: Int, color: Int, size: Int = 20) = ImageView(this).apply {
    setImageResource(res); setColorFilter(color); layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
}
fun Context.iconBtn(res: Int, color: Int, size: Int = 40, bg: Int = 0, click: () -> Unit) = ImageView(this).apply {
    setImageResource(res); setColorFilter(color)
    val p = dp((size - 20) / 2); setPadding(p, p, p, p)
    background = ripple(shape(bg, dpf(size / 2f)), dpf(size / 2f))
    layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)); setOnClickListener { click() }
}
fun Context.divider() = View(this).apply { setBackgroundColor(T.line); layoutParams = LinearLayout.LayoutParams(-1, 1) }

/** Small pill button. toggle=true keeps an on/off state. */
class Chip(private val c: Context, iconRes: Int, label: String, private val toggle: Boolean, private val click: (Boolean) -> Unit) {
    val view = LinearLayout(c)
    var on = false
    private val ic = c.icon(iconRes, T.mut, 16)
    private val tx = c.tv(label, 12.5f, T.mut, true)
    init {
        view.orientation = LinearLayout.HORIZONTAL; view.gravity = Gravity.CENTER_VERTICAL
        view.setPadding(c.dp(12), c.dp(8), c.dp(14), c.dp(8))
        view.addView(ic)
        view.addView(tx, LinearLayout.LayoutParams(-2, -2).apply { marginStart = c.dp(6) })
        set(false)
        view.setOnClickListener { if (toggle) { on = !on; set(on) }; click(on) }
    }
    fun set(v: Boolean) {
        on = v
        val col = if (toggle && v) T.pri else T.mut
        ic.setColorFilter(col); tx.setTextColor(col)
        view.background = c.ripple(c.shape(if (toggle && v) T.priSoft else T.field, c.dpf(18f)), c.dpf(18f))
    }
}
