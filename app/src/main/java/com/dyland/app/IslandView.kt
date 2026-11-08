package com.dyland.app
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.dynamicanimation.animation.*

class IslandView(c: Context, private val resize: (Int, Int) -> Unit) : FrameLayout(c) {
    enum class S(val ww: Int, val wh: Int, val bw: Int, val bh: Int, val r: Int) {
        IDLE(150, 64, 124, 34, 17), COMPACT(280, 72, 250, 42, 21), OPEN(380, 200, 356, 148, 36)
    }
    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d
    private fun px(v: Int) = if (v < 0) v else (v * d).toInt()
    private var st = S.IDLE; private var tok = 0
    private var w = dp(124f); private var h = dp(34f); private var r = dp(17f); private var drop = 0f
    private val hd = Handler(Looper.getMainLooper()); private var ev: Ev? = null
    private val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val lens = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, dp(5f), intArrayOf(0xFFB07CFF.toInt(), 0xFF3B5BFF.toInt()), null, Shader.TileMode.CLAMP)
    }
    private val goo = object : View(c) {
        override fun onDraw(cv: Canvas) {
            val cx = width / 2f; val t = dp(6f)
            cv.drawRoundRect(cx - w / 2, t, cx + w / 2, t + h, r, r, black)
            if (drop > 0f) cv.drawCircle(cx, t + h + dp(9f) * Math.sin(Math.PI * drop).toFloat(), dp(8f) * (1f - drop * .6f), black)
        }
    }
    private val box = FrameLayout(c)
    private val ic = ImageView(c).apply {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() { override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, v.width * .28f) } }
    }
    private val tt = TextView(c).apply { setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
    private val bd = TextView(c).apply { setTextColor(0xB3FFFFFF.toInt()); textSize = 13f; maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
    private fun pill(t: String) = TextView(c).apply {
        text = t; setTextColor(Color.WHITE); textSize = 13f; gravity = Gravity.CENTER
        setPadding(px(18), px(8), px(18), px(8))
        background = GradientDrawable().apply { setColor(0x26FFFFFF); cornerRadius = dp(30f) }
    }
    private val close = pill("✕"); private val open = pill("Mở")

    init {
        if (Build.VERSION.SDK_INT >= 31) goo.setRenderEffect(RenderEffect.createChainEffect(
            RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(ColorMatrix(floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,28f,-2300f)))),
            RenderEffect.createBlurEffect(dp(5f), dp(5f), Shader.TileMode.CLAMP)))
        addView(goo, LayoutParams(-1, -1))
        addView(box, LayoutParams(px(124), px(34), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = px(6) })
        listOf(ic, tt, bd, close, open).forEach { box.addView(it); it.alpha = 0f }
        close.setOnClickListener { go(S.IDLE) }
        open.setOnClickListener { try { ev?.intent?.send() } catch (e: Exception) {}; go(S.IDLE) }
        setOnClickListener {
            when (st) {
                S.COMPACT -> { hd.removeCallbacksAndMessages(null); go(S.OPEN); hd.postDelayed({ go(S.IDLE) }, 9000) }
                S.OPEN -> go(S.IDLE)
                S.IDLE -> { val k = ++tok; sp(dp(152f), dp(124f), k) { w = it; apply() } }
            }
        }
        lay(S.IDLE)
    }
    private fun L(wd: Int, ht: Int, g: Int, ms: Int = 0, mt: Int = 0, me: Int = 0, mb: Int = 0) =
        LayoutParams(px(wd), px(ht), g).apply { marginStart = px(ms); topMargin = px(mt); marginEnd = px(me); bottomMargin = px(mb) }
    private fun lay(s: S) {
        val sv = Gravity.START or Gravity.CENTER_VERTICAL; val st0 = Gravity.START or Gravity.TOP
        if (s == S.OPEN) {
            ic.layoutParams = L(34, 34, st0, 18, 18); tt.layoutParams = L(-1, -2, st0, 62, 20, 18); tt.textSize = 15f
            bd.layoutParams = L(-1, -2, st0, 18, 64, 18)
            close.layoutParams = L(-2, -2, Gravity.END or Gravity.BOTTOM, me = 92, mb = 14); open.layoutParams = L(-2, -2, Gravity.END or Gravity.BOTTOM, me = 18, mb = 14)
        } else {
            ic.layoutParams = L(26, 26, sv, 12); tt.layoutParams = L(-1, -2, sv, 46, me = 14); tt.textSize = 13f
        }
        val main = if (s == S.IDLE) 0f else 1f; val extra = if (s == S.OPEN) 1f else 0f
        listOf(ic, tt).forEach { it.animate().alpha(main).setDuration(200).start() }
        listOf(bd, close, open).forEach { it.animate().alpha(extra).setStartDelay(if (extra > 0) 140 else 0).setDuration(200).start() }
    }
    private fun apply() {
        (box.layoutParams as LayoutParams).let { it.width = w.toInt(); it.height = h.toInt(); box.layoutParams = it }
        goo.invalidate(); invalidate()
    }
    private fun sp(from: Float, to: Float, k: Int, e: () -> Unit = {}, u: (Float) -> Unit) {
        SpringAnimation(FloatValueHolder(from)).setSpring(SpringForce(to).setDampingRatio(.58f).setStiffness(420f)).apply {
            addUpdateListener { _, v, _ -> if (k == tok) u(v) }; addEndListener { _, _, _, _ -> e() }; start()
        }
    }
    fun go(s: S) {
        val p = st; if (p == s) return; st = s; val k = ++tok; lay(s)
        val grow = s.ww >= p.ww && s.wh >= p.wh
        if (grow) resize(s.ww, s.wh)
        sp(w, dp(s.bw.toFloat()), k) { w = it; apply() }
        sp(r, dp(s.r.toFloat()), k) { r = it }
        sp(h, dp(s.bh.toFloat()), k, { if (!grow && k == tok) resize(s.ww, s.wh) }) { h = it; apply() }
        ValueAnimator.ofFloat(0f, 1f).apply { duration = 560; addUpdateListener { drop = it.animatedValue as Float }; start() }
    }
    fun show(e: Ev) {
        ev = e; ic.setImageDrawable(e.icon)
        if (e.tint) { ic.setColorFilter(Color.WHITE); ic.setPadding(px(4), px(4), px(4), px(4)) } else { ic.clearColorFilter(); ic.setPadding(0, 0, 0, 0) }
        tt.text = e.title; bd.text = e.text
        hd.removeCallbacksAndMessages(null)
        go(S.COMPACT)
        hd.postDelayed({ go(S.IDLE) }, 4500)
    }
    override fun dispatchDraw(cv: Canvas) {
        super.dispatchDraw(cv)
        val a = 1f - ic.alpha
        if (a > .02f) { lens.alpha = (a * 255).toInt(); cv.save(); cv.translate(width / 2f + w / 2 - dp(15f), dp(6f) + h / 2); cv.drawCircle(0f, 0f, dp(5f), lens); cv.restore() }
    }
}
