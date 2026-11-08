package com.mousetuner

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlin.concurrent.thread

/** Bảng hiệu suất dùng chung cho màn hình chính và menu vuốt trong game: CPU · FPS · GPU, giờ chơi, pin, chế độ, làm mới, độ sáng. */
class PerfPanel(private val c: Context, compact: Boolean = false) {
    val view = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
    var onChange: (() -> Unit)? = null
    private val big = if (compact) 128 else 170
    private val mid = if (compact) 74 else 92
    private val cpuG = Gauge(c, "CPU", "GHz", big)
    private val gpuG = Gauge(c, "GPU", "MHz", big)
    private val fpsG = Gauge(c, "FPS", "", mid)
    private val playTv = TextView(c)
    private val batTv = TextView(c)
    private val stat = c.body("…")
    private val ui = Handler(Looper.getMainLooper())
    private val loop = object : Runnable { override fun run() { refresh(); ui.postDelayed(this, 1000) } }

    private fun sp(v: String, u: String): CharSequence = SpannableString(v + u).apply { setSpan(RelativeSizeSpan(0.5f), v.length, length, 0) }
    private fun hzNow() = (c.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate
    private fun div() = View(c).apply { setBackgroundColor(0x26FFFFFF) }
    private fun h2(t: String) = TextView(c).apply { text = t; setTextColor(Color.WHITE); textSize = if (Theme.gold) 21f else 17f; typeface = if (Theme.gold) Typeface.DEFAULT else Typeface.DEFAULT_BOLD; setPadding(0, c.dp(14), 0, c.dp(6)) }
    private fun bar(max: Int, prog: Int, stop: (Int) -> Unit) = SeekBar(c).apply {
        this.max = max; progress = prog
        progressTintList = ColorStateList.valueOf(if (Theme.gold) Theme.acc else RED); progressBackgroundTintList = ColorStateList.valueOf(0xFF5A5854.toInt())
        if (Theme.gold) thumb = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(0xFF141414.toInt()); setStroke(c.dp(3), Theme.acc); setSize(c.dp(26), c.dp(26)) }
        else thumbTintList = ColorStateList.valueOf(Color.WHITE)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {}
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) { stop(progress) }
        })
    }
    private fun bigBox(tv: TextView, lab: String, al: Int) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        addView(tv.apply { setTextColor(Color.WHITE); textSize = if (Theme.gold) 32f else 26f; typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC); gravity = al })
        addView(TextView(c).apply { text = lab; setTextColor(if (Theme.gold) 0xFFD8D4CC.toInt() else 0xFFB8A0A4.toInt()); textSize = 14f; gravity = al })
    }

    init {
        val gr = c.row().apply { gravity = Gravity.CENTER }
        gr.addView(cpuG, lpw(0, -2, 1f)); gr.addView(fpsG, lpw(c.dp(mid), -2)); gr.addView(gpuG, lpw(0, -2, 1f)); view.addView(gr)
        val ps = c.row().apply { setPadding(c.dp(8), 0, c.dp(8), c.dp(8)) }
        ps.addView(bigBox(playTv, "Thời lượng trò chơi", Gravity.START), lpw(0, -2, 1f)); ps.addView(bigBox(batTv, "Pin còn lại", Gravity.END), lpw(0, -2, 1f)); view.addView(ps)
        playTv.text = sp("%.1f".format(java.util.Locale.forLanguageTag("vi-VN"), Cfg.f(c, "playsec", 0f) / 3600f), "h")
        view.addView(c.seg(listOf("Tiết kiệm\npin", "Cân bằng", "MONSTER"), Cfg.mode(c)) { Cfg.setI(c, "mode", it); Engine.applyAsync(c) })
        stat.apply { gravity = Gravity.CENTER; textSize = 11f; setPadding(0, c.dp(6), 0, 0) }; view.addView(stat)
        view.addView(div(), lpw(-1, 1).apply { topMargin = c.dp(8) })

        // Tốc độ làm mới: 3 nấc
        view.addView(h2("Tốc độ làm mới"))
        val maxHz = ShizukuShell.supportedHz(c).maxOrNull() ?: 60
        val labs = listOf("Chuyển đổi thông minh", "Tiêu chuẩn · 60Hz", "Cao · ${maxHz}Hz").map { t -> TextView(c).apply { text = t; textSize = 12f } }
        fun paint(s: Int) = labs.forEachIndexed { i, v -> v.setTextColor(if (i == s) Color.WHITE else 0xFF8A8A90.toInt()); v.typeface = if (i == s) Typeface.DEFAULT_BOLD else Typeface.DEFAULT }
        paint(Cfg.hzMode(c))
        view.addView(bar(2, Cfg.hzMode(c)) { Cfg.setI(c, "hzm", it); paint(it); Engine.applyAsync(c) })
        val lr = c.row(); labs[0].gravity = Gravity.START; labs[1].gravity = Gravity.CENTER; labs[2].gravity = Gravity.END
        labs.forEach { lr.addView(it, lpw(0, -2, 1f)) }; view.addView(lr)
        view.addView(div(), lpw(-1, 1).apply { topMargin = c.dp(12) })

        // Độ sáng
        view.addView(h2("Độ sáng"))
        val cur = Settings.System.getInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        view.addView(bar(255, cur) { v -> val b = v.coerceAtLeast(1)
            if (ShizukuShell.ready()) thread { ShizukuShell.setBrightness(b) }
            else if (Settings.System.canWrite(c)) { Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0); Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS, b) } })
        val go = c.row().apply { setPadding(0, c.dp(10), 0, 0) }
        go.put(c.gbtn("GAME ON", true) { Engine.profile(c, true); Toast.makeText(c, "Đã bật bộ tính năng chơi game", Toast.LENGTH_SHORT).show(); onChange?.invoke() })
        go.put(c.gbtn("TẮT HẾT", false) { Engine.profile(c, false); Toast.makeText(c, "Đã về mặc định", Toast.LENGTH_SHORT).show(); onChange?.invoke() })
        view.addView(go)

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { ui.removeCallbacks(loop); loop.run() }
            override fun onViewDetachedFromWindow(v: View) { ui.removeCallbacks(loop) }
        })
    }

    fun refresh() = thread {
        val ok = ShizukuShell.ready()
        val g = if (ok) ShizukuShell.cpuGhz() else -1f; val m = if (ok) ShizukuShell.gpuMhz() else -1
        val bi = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val pct = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1; val temp = (bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val mi = ActivityManager.MemoryInfo(); (c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        val ram = 100 - (mi.availMem * 100 / mi.totalMem).toInt()
        val f = FpsMonitor.fresh(); val hz = if (FpsMonitor.liveHz > 0) FpsMonitor.liveHz else hzNow()
        ui.post {
            if (f != null) fpsG.set(f / hz, "${Math.round(f)}", "") else fpsG.set(0f, "--", "vào game")
            cpuG.set(if (g < 0) 0f else g / 3.5f, if (g < 0) "--" else "%.2f".format(g)); gpuG.set(if (m < 0) 0f else m / 1000f, if (m < 0) "--" else "$m")
            batTv.text = sp("$pct", "%"); playTv.text = sp("%.1f".format(java.util.Locale.forLanguageTag("vi-VN"), Cfg.f(c, "playsec", 0f) / 3600f), "h")
            stat.text = "🌡 %.1f°C     RAM $ram%%     ${Math.round(hzNow())}Hz".format(temp)
        }
    }
}
