package com.fpsoverlay

import android.animation.ValueAnimator
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.text.*
import android.view.*
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class OverlayService : Service() {
    companion object { @Volatile var running = false }

    private val palette = listOf("#FFFFFF", "#4ADE80", "#22D3EE", "#60A5FA", "#A78BFA", "#F472B6",
        "#F87171", "#FB923C", "#FACC15", "#39FF14", "#FF2E63", "#00F5D4").map { Color.parseColor(it) }
    private val bgPal = listOf("#080814", "#000000", "#1A1A2E", "#0F172A", "#1E1B4B", "#3B0764", "#450A0A", "#052E16",
        "#082F49", "#27272A", "#FFFFFF", "#7C3AED").map { Color.parseColor(it) }
    private val cCyan = Color.parseColor("#22D3EE")
    private val cGreen = Color.parseColor("#4ADE80")
    private val fonts = listOf("sans-serif", "monospace", "serif", "sans-serif-condensed", "cursive")

    private lateinit var wm: WindowManager
    private lateinit var ui: Context
    private lateinit var sp: SharedPreferences
    private lateinit var panel: LinearLayout
    private lateinit var panelP: WindowManager.LayoutParams
    private lateinit var labelTv: TextView
    private lateinit var numTv: TextView
    private var bubble: View? = null
    private var menu: View? = null
    private var menuP: WindowManager.LayoutParams? = null
    private var fx: ValueAnimator? = null
    private val h = Handler(Looper.getMainLooper())
    private var frames = 0
    private var lastT = 0L
    private var cur = 0
    private class FText(val id: Int) { lateinit var v: EditText; lateinit var p: WindowManager.LayoutParams; var editing = false }
    private val texts = mutableListOf<FText>()
    private var selId = -1

    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(t: Long) { frames++; Choreographer.getInstance().postFrameCallback(this) }
    }

    private val tick = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val real = if (lastT == 0L) 0 else (frames * 1000f / (now - lastT)).toInt()
            frames = 0; lastT = now
            val t = sp.getInt("tfps", 144)
            cur = when {
                sp.getInt("src", 0) == 1 -> real
                sp.getBoolean("freeze", false) -> t
                else -> (t + Random.nextInt(-sp.getInt("jit", 2), sp.getInt("jit", 2) + 1)).coerceAtLeast(1)
            }
            numTv.text = "$cur" + (sp.getString("suffix", "") ?: "")
            if (!sp.getBoolean("fx_rainbow", false)) setNumColor(numColor())
            h.postDelayed(this, sp.getInt("speed", 500).toLong())
        }
    }

    override fun onBind(i: Intent?) = null

    private fun lp(w: Int, hh: Int, flags: Int) = WindowManager.LayoutParams(
        w, hh, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT
    ).apply {
        if (Build.VERSION.SDK_INT >= 28)
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        ui = ContextThemeWrapper(this, android.R.style.Theme_Material)
        sp = getSharedPreferences("fps", MODE_PRIVATE)
        startFg(); buildPanel(); buildBubble(); startFx(); loadTexts()
        Choreographer.getInstance().postFrameCallback(frameCb)
        h.post(tick)
    }

    private fun startFg() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("fps", "FPS Overlay", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "fps").setContentTitle("T•HẢI PCM · FPS Overlay đang chạy")
            .setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    // ===== Bảng FPS: chỉ "FPS 144", dính cứng góc TRÁI-DƯỚI (x=0, y=0), chạm xuyên qua =====
    private fun buildPanel() {
        panel = LinearLayout(ui).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        labelTv = TextView(ui); numTv = TextView(ui).apply { setPadding(dp(6), 0, 0, 0) }
        panel.addView(labelTv); panel.addView(numTv)
        panelP = lp(-2, -2, 0).apply {
            gravity = Gravity.BOTTOM or Gravity.START; x = 0; y = 0
        }
        // Bấm 2 lần liên tiếp vào ô FPS để mở menu chỉnh
        var taps = 0; var lastTap = 0L
        panel.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP) {
                val n = SystemClock.elapsedRealtime()
                taps = if (n - lastTap < 500) taps + 1 else 1; lastTap = n
                if (taps >= 2) { taps = 0; toggleMenu() }
            }
            true
        }
        wm.addView(panel, panelP)
        applyStyle(); intro()
    }

    private fun numColor(): Int =
        if (sp.getBoolean("fx_auto", false)) when { cur >= 60 -> cGreen; cur >= 30 -> Color.parseColor("#FACC15"); else -> Color.parseColor("#F87171") }
        else sp.getInt("ncol", cGreen)

    private fun glow(tv: TextView, c: Int) {
        if (sp.getBoolean("fx_glow", true)) tv.setShadowLayer(sp.getInt("glow_r", 8).coerceAtLeast(1).toFloat() * 1.5f, 0f, 0f, c)
        else tv.setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    private fun setNumColor(c: Int) { numTv.setTextColor(c); glow(numTv, c) }

    private fun applyStyle() {
        val size = sp.getInt("size", 18).toFloat()
        val up = sp.getBoolean("upper", true)
        val lab = sp.getString("label", "FPS") ?: ""
        labelTv.text = if (up) lab.uppercase() else lab
        labelTv.visibility = if (lab.isEmpty()) View.GONE else View.VISIBLE
        val style = (if (sp.getBoolean("bold", true)) Typeface.BOLD else 0) or (if (sp.getBoolean("italic", false)) Typeface.ITALIC else 0)
        val tf = Typeface.create(fonts[sp.getInt("font", 3).coerceIn(0, fonts.lastIndex)], style)
        listOf(labelTv, numTv).forEach { it.textSize = size; it.typeface = tf; it.letterSpacing = sp.getInt("spacing", 4) / 100f }
        val lc = sp.getInt("lcol", cCyan)
        labelTv.setTextColor(lc); glow(labelTv, lc)
        setNumColor(numColor())
        val bg = sp.getBoolean("bg", true); val st = sp.getBoolean("stroke", false)
        panel.background = if (!bg && !st) null else GradientDrawable().apply {
            val bc = sp.getInt("bgcol", bgPal[0]); setColor(if (bg) Color.argb(sp.getInt("alpha", 90) * 255 / 100, Color.red(bc), Color.green(bc), Color.blue(bc)) else Color.TRANSPARENT)
            val r = dp(sp.getInt("radius", 10)).toFloat(); cornerRadii = floatArrayOf(0f, 0f, r, r, 0f, 0f, 0f, 0f) // chỉ bo góc trên-phải
            if (st) setStroke(dp(sp.getInt("stw", 1)), sp.getInt("scol", cCyan))
        }
        val pd = sp.getInt("pad", 8)
        panel.setPadding(dp(pd), dp(pd / 2), dp(pd + 4), dp(pd / 2))
        numTv.setPadding(dp(sp.getInt("gap", 6)), 0, 0, 0)
        texts.forEach { styleText(it) }
    }

    private fun intro() {
        if (!sp.getBoolean("fx_intro", true)) return
        panel.post {
            panel.translationX = -panel.width.toFloat()
            panel.animate().translationX(0f).setDuration(450).setInterpolator(OvershootInterpolator()).start()
        }
    }

    private fun startFx() {
        fx = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener {
                val dur = sp.getInt("fx_speed", 2400); val f = (SystemClock.elapsedRealtime() % dur) / dur.toFloat()
                if (sp.getBoolean("fx_rainbow", false)) setNumColor(Color.HSVToColor(floatArrayOf(f * 360f, 0.85f, 1f)))
                if (sp.getBoolean("fx_rainbow_label", false)) {
                    val c = Color.HSVToColor(floatArrayOf((f * 360f + 180f) % 360f, 0.85f, 1f)); labelTv.setTextColor(c); glow(labelTv, c)
                }
                panel.alpha = (if (sp.getBoolean("fx_breathe", false)) 0.5f + 0.5f * (0.5f + 0.5f * sin(f * 2 * PI).toFloat()) else 1f) * sp.getInt("op", 100) / 100f
            }
            start()
        }
    }

    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        panelP.gravity = Gravity.BOTTOM or Gravity.START; panelP.x = 0; panelP.y = 0
        wm.updateViewLayout(panel, panelP)
        closeMenu()
    }

    private fun buildBubble() {
        val b = ImageView(ui).apply {
            setImageResource(R.drawable.logo); scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) { o.setOval(0, 0, v.width, v.height) }
            }
            elevation = 12f
        }
        val p = lp(dp(50), dp(50), 0).apply { gravity = Gravity.TOP or Gravity.START; x = dp(8); y = dp(200) }
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        b.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = p.x; sy = p.y; tx = e.rawX; ty = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (Math.abs(dx) + Math.abs(dy) > dp(6)) moved = true
                    p.x = sx + dx; p.y = sy + dy; wm.updateViewLayout(b, p)
                }
                MotionEvent.ACTION_UP -> if (!moved) toggleMenu()
            }
            true
        }
        bubble = b; wm.addView(b, p)
    }

    private fun closeMenu() { menu?.let { runCatching { wm.removeView(it) } }; menu = null; menuP = null }

    private fun setMenuFocusable(on: Boolean) {
        val p = menuP ?: return; val m = menu ?: return
        p.flags = if (on) p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        else p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        wm.updateViewLayout(m, p)
    }

    // ===== Bảng điều chỉnh kiểu gaming: 4 tab FPS / TEXT / MÀU / FX =====
    private fun toggleMenu() {
        if (menu != null) { closeMenu(); return }
        val imm = getSystemService(InputMethodManager::class.java)
        val root = LinearLayout(ui).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12))
            background = round(Color.parseColor("#F20B0B1A"), dp(22).toFloat(), Color.parseColor("#8806B6D4"))
        }
        val content = LinearLayout(ui).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, dp(10)) }
        fun lab(t: String) = content.addView(TextView(ui).apply { text = t; setTextColor(Color.parseColor("#B0B3D6")); textSize = 12f; setPadding(0, dp(10), 0, dp(4)) })
        fun sw(label: String, key: String, def: Boolean, after: () -> Unit = {}) = content.addView(Switch(ui).apply {
            text = label; setTextColor(Color.WHITE); textSize = 14f; isChecked = sp.getBoolean(key, def); setPadding(0, dp(4), 0, dp(4))
            setOnCheckedChangeListener { _, v -> sp.edit().putBoolean(key, v).apply(); applyStyle(); after() }
        })
        fun seek(label: String, key: String, min: Int, max: Int, def: Int, unit: String = "") {
            val t = TextView(ui).apply { setTextColor(Color.parseColor("#B0B3D6")); textSize = 12f; setPadding(0, dp(10), 0, 0) }
            t.text = "$label: ${sp.getInt(key, def)}$unit"
            val s = SeekBar(ui).apply {
                this.max = max - min; progress = sp.getInt(key, def) - min
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                        sp.edit().putInt(key, v + min).apply(); t.text = "$label: ${v + min}$unit"; applyStyle()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            val step = maxOf(1, (max - min) / 100)
            fun stepBtn(txt: String, d: Int) = TextView(ui).apply {
                text = txt; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 18f; setTypeface(typeface, Typeface.BOLD)
                background = round(Color.parseColor("#337C3AED"), dp(10).toFloat(), Color.parseColor("#887C3AED"))
                val rep = object : Runnable {
                    override fun run() { s.progress = (s.progress + d * step).coerceIn(0, s.max); h.postDelayed(this, 70) }
                }
                setOnTouchListener { _, e ->
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> { s.progress = (s.progress + d * step).coerceIn(0, s.max); h.postDelayed(rep, 400) }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> h.removeCallbacks(rep)
                    }
                    true
                }
            }
            content.addView(t)
            content.addView(LinearLayout(ui).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(stepBtn("−", -1), LinearLayout.LayoutParams(dp(36), dp(36)))
                addView(s, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
                addView(stepBtn("+", 1), LinearLayout.LayoutParams(dp(36), dp(36)))
            })
        }
        fun chips(label: String, key: String, opts: List<String>, def: Int) {
            lab(label)
            val row = LinearLayout(ui).apply { orientation = LinearLayout.HORIZONTAL }
            val vs = opts.map { o -> TextView(ui).apply { text = o; textSize = 12f; setTextColor(Color.WHITE); setPadding(dp(12), dp(6), dp(12), dp(6)) } }
            fun paint() { val c = sp.getInt(key, def); vs.forEachIndexed { i, v -> v.background = round(Color.parseColor(if (i == c) "#7C3AED" else "#22FFFFFF"), dp(12).toFloat()) } }
            vs.forEachIndexed { i, v ->
                v.setOnClickListener { sp.edit().putInt(key, i).apply(); paint(); applyStyle() }
                row.addView(v, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
            }
            paint(); content.addView(HorizontalScrollView(ui).apply { isHorizontalScrollBarEnabled = false; addView(row) })
        }
        fun colors(label: String, key: String, def: Int, pal: List<Int> = palette) {
            lab(label)
            val row = LinearLayout(ui).apply { orientation = LinearLayout.HORIZONTAL }
            val ds = pal.map { View(ui) }
            fun paint() { val c = sp.getInt(key, def); ds.forEachIndexed { i, v ->
                v.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(pal[i])
                    setStroke(if (pal[i] == c) dp(3) else 1, if (pal[i] == c) Color.WHITE else Color.parseColor("#55FFFFFF")) } } }
            ds.forEachIndexed { i, v ->
                v.setOnClickListener { sp.edit().putInt(key, pal[i]).apply(); paint(); applyStyle() }
                row.addView(v, LinearLayout.LayoutParams(dp(30), dp(30)).apply { rightMargin = dp(8) })
            }
            paint(); content.addView(HorizontalScrollView(ui).apply { isHorizontalScrollBarEnabled = false; addView(row) })
        }
        // TEXT VIEW: sửa chữ trực tiếp, hiện ngay trên màn hình
        fun edit(label: String, key: String, def: String, maxLen: Int) {
            lab(label)
            val e = EditText(ui).apply {
                setText(sp.getString(key, def)); setTextColor(Color.WHITE); textSize = 15f; isSingleLine = true
                filters = arrayOf(InputFilter.LengthFilter(maxLen)); imeOptions = EditorInfo.IME_ACTION_DONE
                background = round(Color.parseColor("#22FFFFFF"), dp(12).toFloat(), Color.parseColor("#4406B6D4"))
                setPadding(dp(12), dp(8), dp(12), dp(8))
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) { sp.edit().putString(key, s.toString()).apply(); applyStyle() }
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                })
                setOnTouchListener { v, ev ->
                    if (ev.action == MotionEvent.ACTION_DOWN) { setMenuFocusable(true); v.postDelayed({ v.requestFocus(); imm.showSoftInput(v, 0) }, 150) }
                    false
                }
                setOnEditorActionListener { v, _, _ -> imm.hideSoftInputFromWindow(v.windowToken, 0); setMenuFocusable(false); true }
            }
            content.addView(e)
        }

        fun cbtn(t: String, col: String, f: () -> Unit) = content.addView(TextView(ui).apply {
            text = t; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 13f; setTypeface(typeface, Typeface.BOLD)
            background = round(Color.parseColor(col), dp(12).toFloat()); setPadding(0, dp(10), 0, dp(10)); setOnClickListener { f() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        val tabNames = listOf("FPS", "NHÃN", "MÀU", "FX", "TEXT")
        val tabViews = tabNames.map { n -> TextView(ui).apply { text = n; gravity = Gravity.CENTER; textSize = 13f; setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(8), 0, dp(8)) } }
        fun showTab(i: Int) {
            tabViews.forEachIndexed { k, v ->
                v.setTextColor(if (k == i) Color.WHITE else Color.parseColor("#8A8DB5"))
                v.background = if (k == i) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#06B6D4"))).apply { cornerRadius = dp(12).toFloat() } else null
            }
            content.removeAllViews()
            when (i) {
                0 -> {
                    chips("Nguồn số FPS", "src", listOf("Giả (tuỳ chỉnh)", "Thật (đo máy)"), 0)
                    seek("Số FPS giả", "tfps", 30, 360, 144); seek("Độ dao động", "jit", 0, 10, 2, " fps")
                    sw("Giữ cố định số (không dao động)", "freeze", false)
                    seek("Tốc độ cập nhật", "speed", 100, 2000, 500, " ms")
                }
                1 -> {
                    edit("Chữ nhãn (đổi chữ FPS thành gì tuỳ ý, để trống = ẩn)", "label", "FPS", 12)
                    edit("Chữ sau số (vd: fps, Hz)", "suffix", "", 8)
                    chips("Font", "font", listOf("Sans", "Mono", "Serif", "Condensed", "Cursive"), 3)
                    sw("Chữ đậm", "bold", true); sw("Chữ nghiêng", "italic", false); sw("IN HOA nhãn", "upper", true)
                    seek("Cỡ chữ", "size", 10, 48, 18); seek("Giãn chữ", "spacing", 0, 30, 4)
                }
                2 -> {
                    colors("Màu chữ nhãn", "lcol", cCyan); colors("Màu số FPS", "ncol", cGreen)
                    sw("Nền", "bg", true); colors("Màu nền", "bgcol", bgPal[0], bgPal); seek("Độ đậm nền", "alpha", 0, 100, 90, "%")
                    seek("Bo góc", "radius", 0, 24, 10); seek("Khoảng đệm", "pad", 0, 24, 8); seek("Khoảng cách chữ-số", "gap", 0, 24, 6)
                    sw("Viền", "stroke", false); colors("Màu viền", "scol", cCyan); seek("Độ dày viền", "stw", 1, 6, 1)
                    seek("Độ mờ cả bảng", "op", 20, 100, 100, "%")
                }
                3 -> {
                    sw("🌈 Số FPS đổi màu cầu vồng", "fx_rainbow", false); sw("🌈 Cả chữ nhãn đổi màu", "fx_rainbow_label", false)
                    seek("Tốc độ hiệu ứng", "fx_speed", 500, 6000, 2400, " ms"); sw("Phát sáng (glow)", "fx_glow", true); seek("Độ sáng glow", "glow_r", 1, 30, 8)
                    sw("Nhịp thở (mờ/sáng)", "fx_breathe", false)
                    sw("Màu theo FPS (xanh/vàng/đỏ)", "fx_auto", false)
                    sw("Hiệu ứng trượt vào", "fx_intro", true) { intro() }
                }
                else -> {
                    // TEXT VIEW: chữ nổi riêng, sửa/kéo trực tiếp trên màn hình
                    lab("Chạm vào chữ trên màn hình để sửa trực tiếp, kéo để di chuyển")
                    cbtn("+ Thêm chữ mới", "#7C3AED") { addTextAndSelect(); showTab(4) }
                    if (texts.size > 1) {
                        lab("Chọn chữ để chỉnh")
                        val row = LinearLayout(ui).apply { orientation = LinearLayout.HORIZONTAL }
                        texts.forEach { x ->
                            row.addView(TextView(ui).apply {
                                text = x.v.text.toString().take(8).ifEmpty { "…" }; textSize = 12f; setTextColor(Color.WHITE); setPadding(dp(12), dp(6), dp(12), dp(6))
                                background = round(Color.parseColor(if (x.id == selId) "#7C3AED" else "#22FFFFFF"), dp(12).toFloat())
                                setOnClickListener { selId = x.id; showTab(4) }
                            }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
                        }
                        content.addView(HorizontalScrollView(ui).apply { isHorizontalScrollBarEnabled = false; addView(row) })
                    }
                    val t = texts.find { it.id == selId }
                    if (t == null) lab("Chưa chọn chữ nào. Thêm mới hoặc chạm vào một chữ.") else {
                        val k = "t${t.id}_"
                        lab("Đang chọn: \"${t.v.text}\"")
                        seek("Cỡ chữ", k + "size", 10, 80, 22); colors("Màu chữ", k + "color", Color.WHITE)
                        chips("Font", k + "font", listOf("Sans", "Mono", "Serif", "Condensed", "Cursive"), 3)
                        sw("Chữ đậm", k + "bold", true); sw("Chữ nghiêng", k + "italic", false); sw("IN HOA", k + "upper", false)
                        seek("Giãn chữ", k + "sp", 0, 30, 0); seek("Độ mờ chữ", k + "op", 20, 100, 100, "%")
                        sw("Phát sáng", k + "glow", true); seek("Độ sáng glow", k + "gr", 1, 30, 12)
                        sw("Nền", k + "bg", true); colors("Màu nền", k + "bgc", Color.BLACK, bgPal); seek("Độ đậm nền", k + "bga", 0, 100, 70, "%")
                        seek("Bo góc nền", k + "rad", 0, 30, 8); seek("Khoảng đệm", k + "pad", 0, 30, 8)
                        sw("Viền", k + "st", false); colors("Màu viền", k + "stc", cCyan); seek("Độ dày viền", k + "stw", 1, 8, 2)
                        cbtn("Xoá chữ này", "#DC2626") { removeText(t.id); showTab(4) }
                    }
                }
            }
        }
        tabViews.forEachIndexed { i, v -> v.setOnClickListener { showTab(i) } }

        root.addView(TextView(ui).apply { text = "⚡ FPS CONTROL"; setTextColor(cCyan); textSize = 17f; letterSpacing = 0.1f; setTypeface(typeface, Typeface.BOLD) })
        root.addView(TextView(ui).apply { text = "by T•HẢI PCM"; setTextColor(Color.parseColor("#A78BFA")); textSize = 11f; letterSpacing = 0.1f })
        val tabRow = LinearLayout(ui).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10), 0, dp(4)) }
        tabViews.forEach { tabRow.addView(it, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) }) }
        root.addView(tabRow)
        root.addView(ScrollView(ui).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val foot = LinearLayout(ui).apply { orientation = LinearLayout.HORIZONTAL }
        fun pill(t: String, col: String, f: () -> Unit) = foot.addView(TextView(ui).apply {
            text = t; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 13f; setTypeface(typeface, Typeface.BOLD)
            background = round(Color.parseColor(col), dp(12).toFloat()); setPadding(0, dp(10), 0, dp(10)); setOnClickListener { f() }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), dp(6), dp(3), 0) })
        pill("Mặc định", "#33FFFFFF") { texts.toList().forEach { removeText(it.id) }; sp.edit().clear().apply(); selId = -1; applyStyle(); intro(); closeMenu() }
        pill("Đóng", "#7C3AED") { closeMenu() }
        pill("Tắt", "#DC2626") { stopSelf() }
        root.addView(foot)
        root.addView(TextView(ui).apply { text = "© 2026 T•HẢI PCM · Bảo lưu mọi quyền"; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#6B6E95")); textSize = 10f; setPadding(0, dp(8), 0, 0) })
        showTab(0)

        val maxH = minOf((resources.displayMetrics.heightPixels * 0.78f).toInt(), dp(560))
        val p = lp(dp(320), maxH, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL).apply {
            gravity = Gravity.CENTER; softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        menu = root; menuP = p; wm.addView(root, p)
    }

    // ===== TEXT VIEW chữ nổi =====
    private fun loadTexts() { (sp.getString("tids", "") ?: "").split(",").filter { it.isNotEmpty() }.forEach { addTextView(it.toInt()) } }
    private fun saveIds() { sp.edit().putString("tids", texts.joinToString(",") { it.id.toString() }).apply() }
    private fun addTextAndSelect() {
        val id = sp.getInt("tnext", 1); sp.edit().putInt("tnext", id + 1).apply()
        addTextView(id); saveIds(); selId = id
    }
    private fun removeText(id: Int) {
        val t = texts.find { it.id == id } ?: return
        runCatching { wm.removeView(t.v) }; texts.remove(t)
        val ed = sp.edit(); listOf("text", "x", "y", "size", "color", "bold", "italic", "upper", "font", "sp", "op", "glow", "gr", "bg", "bgc", "bga", "rad", "pad", "st", "stc", "stw").forEach { ed.remove("t${id}_$it") }; ed.apply()
        saveIds(); if (selId == id) selId = texts.lastOrNull()?.id ?: -1
    }
    private fun startEdit(t: FText) {
        t.editing = true
        t.p.flags = t.p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv(); wm.updateViewLayout(t.v, t.p)
        t.v.isCursorVisible = true
        t.v.postDelayed({ t.v.requestFocus(); t.v.setSelection(t.v.text.length); getSystemService(InputMethodManager::class.java).showSoftInput(t.v, 0) }, 120)
    }
    private fun endEdit(t: FText) {
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(t.v.windowToken, 0)
        t.editing = false; t.v.isCursorVisible = false; t.v.clearFocus()
        t.p.flags = t.p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; wm.updateViewLayout(t.v, t.p)
    }
    private fun addTextView(id: Int) {
        val t = FText(id)
        val e = EditText(ui).apply {
            setText(sp.getString("t${id}_text", "Văn bản")); isSingleLine = true; imeOptions = EditorInfo.IME_ACTION_DONE
            isCursorVisible = false; setPadding(dp(8), dp(4), dp(8), dp(4)); minWidth = dp(40)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { sp.edit().putString("t${id}_text", s.toString()).apply() }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
            setOnEditorActionListener { _, _, _ -> endEdit(t); true }
        }
        val p = lp(-2, -2, 0).apply {
            gravity = Gravity.TOP or Gravity.START; x = sp.getInt("t${id}_x", dp(80)); y = sp.getInt("t${id}_y", dp(300))
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
        t.v = e; t.p = p
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        e.setOnTouchListener { _, ev ->
            selId = id
            if (t.editing) return@setOnTouchListener false
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sx = p.x; sy = p.y; tx = ev.rawX; ty = ev.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - tx).toInt(); val dy = (ev.rawY - ty).toInt()
                    if (Math.abs(dx) + Math.abs(dy) > dp(6)) moved = true
                    if (moved) { p.x = sx + dx; p.y = sy + dy; wm.updateViewLayout(e, p) }
                }
                MotionEvent.ACTION_UP -> if (moved) sp.edit().putInt("t${id}_x", p.x).putInt("t${id}_y", p.y).apply() else startEdit(t)
            }
            true
        }
        texts.add(t); wm.addView(e, p); styleText(t)
    }
    private fun styleText(t: FText) {
        val k = "t${t.id}_"; val e = t.v; val c = sp.getInt(k + "color", Color.WHITE)
        e.textSize = sp.getInt(k + "size", 22).toFloat(); e.setTextColor(c)
        val ts = (if (sp.getBoolean(k + "bold", true)) Typeface.BOLD else 0) or (if (sp.getBoolean(k + "italic", false)) Typeface.ITALIC else 0)
        e.typeface = Typeface.create(fonts[sp.getInt(k + "font", 3).coerceIn(0, fonts.lastIndex)], ts)
        e.setAllCaps(sp.getBoolean(k + "upper", false))
        e.letterSpacing = sp.getInt(k + "sp", 0) / 100f
        e.alpha = sp.getInt(k + "op", 100) / 100f
        if (sp.getBoolean(k + "glow", true)) e.setShadowLayer(sp.getInt(k + "gr", 12) * 1.5f, 0f, 0f, c) else e.setShadowLayer(3f, 0f, 0f, Color.BLACK)
        val bgOn = sp.getBoolean(k + "bg", true); val stOn = sp.getBoolean(k + "st", false)
        e.background = if (!bgOn && !stOn) null else GradientDrawable().apply {
            val bc = sp.getInt(k + "bgc", Color.BLACK)
            setColor(if (bgOn) Color.argb(sp.getInt(k + "bga", 70) * 255 / 100, Color.red(bc), Color.green(bc), Color.blue(bc)) else Color.TRANSPARENT)
            cornerRadius = dp(sp.getInt(k + "rad", 8)).toFloat()
            if (stOn) setStroke(dp(sp.getInt(k + "stw", 2)), sp.getInt(k + "stc", cCyan))
        }
        val pd = dp(sp.getInt(k + "pad", 8)); e.setPadding(pd, pd / 2, pd, pd / 2)
    }

    override fun onDestroy() {
        running = false
        fx?.cancel(); h.removeCallbacksAndMessages(null)
        Choreographer.getInstance().removeFrameCallback(frameCb)
        (listOfNotNull(panel, bubble, menu) + texts.map { it.v }).forEach { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
