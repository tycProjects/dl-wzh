package com.supra.turbo

import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private val ui = Handler(Looper.getMainLooper())
    private val sp by lazy { SupraEngine.sp(this) }
    private val accents = intArrayOf(0xFF00E5FF.toInt(), 0xFF7C4DFF.toInt(), 0xFFFF4D6A.toInt(), 0xFF2EE6A6.toInt(), 0xFFFFB300.toInt())
    private var accent = accents[0]

    private lateinit var tvPill: TextView; private lateinit var tvRam: TextView
    private lateinit var tvTemp: TextView; private lateinit var tvSupport: TextView
    private lateinit var tvResCap: TextView; private lateinit var tvSharpCap: TextView
    private lateinit var tvFf: TextView; private lateinit var tvTitle2: TextView
    private lateinit var swEnable: SwitchMaterial; private lateinit var swFps: SwitchMaterial
    private lateinit var swSession: SwitchMaterial; private lateinit var swSharp: SwitchMaterial
    private lateinit var swAuto: SwitchMaterial
    private lateinit var cgRes: ChipGroup; private lateinit var cgFps: ChipGroup; private lateinit var cgSharp: ChipGroup
    private lateinit var llAccent: LinearLayout; private lateinit var btnOpen: Button

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        tvPill = findViewById(R.id.tvPill); tvRam = findViewById(R.id.tvRam); tvTemp = findViewById(R.id.tvTemp)
        tvSupport = findViewById(R.id.tvSupport); tvResCap = findViewById(R.id.tvResCap)
        tvSharpCap = findViewById(R.id.tvSharpCap); tvFf = findViewById(R.id.tvFf); tvTitle2 = findViewById(R.id.tvTitle2)
        swEnable = findViewById(R.id.swEnable); swFps = findViewById(R.id.swFps); swSession = findViewById(R.id.swSession)
        swSharp = findViewById(R.id.swSharp); swAuto = findViewById(R.id.swAuto)
        cgRes = findViewById(R.id.cgRes); cgFps = findViewById(R.id.cgFps); cgSharp = findViewById(R.id.cgSharp)
        llAccent = findViewById(R.id.llAccent); btnOpen = findViewById(R.id.btnOpen)
        findViewById<TextView>(R.id.tvCopyright).text = getString(R.string.copyright, getString(R.string.owner_name))

        swEnable.setOnCheckedChangeListener { v, on ->
            if (!v.isPressed) return@setOnCheckedChangeListener
            if (SupraEngine.installed(this).isEmpty()) { toast("Không tìm thấy Free Fire trên máy"); swEnable.isChecked = false; return@setOnCheckedChangeListener }
            sp.edit().putBoolean("enabled", on).apply(); pushGame()
            if (on && sp.getBoolean("auto", true)) startAuto()
        }
        swFps.setOnCheckedChangeListener { v, on -> if (v.isPressed) { sp.edit().putBoolean("fpsLock", on).apply(); pushGame() } }
        swSession.setOnCheckedChangeListener { v, on -> if (v.isPressed) sp.edit().putBoolean("session", on).apply() }
        swSharp.setOnCheckedChangeListener { v, on -> if (v.isPressed) sp.edit().putBoolean("sharp", on).apply() }
        swAuto.setOnCheckedChangeListener { v, on ->
            if (!v.isPressed) return@setOnCheckedChangeListener
            sp.edit().putBoolean("auto", on).apply()
            if (on) startAuto() else stopService(Intent(this, SupraService::class.java))
        }
        btnOpen.setOnClickListener {
            val pk = SupraEngine.installed(this).firstOrNull()
            val i = pk?.let { packageManager.getLaunchIntentForPackage(it) }
            if (i != null) startActivity(i) else toast("Không tìm thấy Free Fire trên máy")
        }
        findViewById<Button>(R.id.btnRestore).setOnClickListener {
            if (!SupraEngine.hasShizuku()) { toast("Chưa có quyền Shizuku"); return@setOnClickListener }
            stopService(Intent(this, SupraService::class.java))
            SupraEngine.async { SupraEngine.restoreAll(this); ui.post { loadState(); toast("Đã khôi phục mặc định") } }
        }
    }

    private fun startAuto() { if (SupraEngine.allGranted(this)) startForegroundService(Intent(this, SupraService::class.java)) }

    /** Đẩy cấu hình độ phân giải + FPS xuống Game Mode (chạy nền, tuần tự). */
    private fun pushGame() {
        SupraEngine.async {
            val ok = SupraEngine.applyGame(this)
            ui.post { loadState(); if (!ok && sp.getBoolean("enabled", false)) toast("Máy không nhận lệnh Game Mode (cần Android 13+)") }
        }
    }

    private fun chips(cg: ChipGroup, labels: List<String>, sel: Int, pick: (Int) -> Unit) {
        cg.removeAllViews()
        val st = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        labels.forEachIndexed { i, t ->
            cg.addView(Chip(this).apply {
                id = View.generateViewId(); text = t; isCheckable = true; isCheckedIconVisible = false
                chipBackgroundColor = ColorStateList(st, intArrayOf(accent, 0xFF1A1D28.toInt()))
                setTextColor(ColorStateList(st, intArrayOf(0xFF001014.toInt(), 0xFFE8EAF2.toInt())))
                isChecked = i == sel
                setOnClickListener { pick(i) }
            })
        }
    }

    private fun style(sw: SwitchMaterial) {
        val st = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        sw.thumbTintList = ColorStateList(st, intArrayOf(accent, 0xFF8B90A5.toInt()))
        sw.trackTintList = ColorStateList(st, intArrayOf((accent and 0x00FFFFFF) or 0x66000000, 0xFF2A2D3B.toInt()))
    }

    private fun loadState() {
        accent = accents[sp.getInt("accent", 0).coerceIn(0, accents.size - 1)]
        tvTitle2.setTextColor(accent); btnOpen.backgroundTintList = ColorStateList.valueOf(accent)
        listOf(swEnable, swFps, swSession, swSharp, swAuto).forEach { style(it) }
        swEnable.isChecked = sp.getBoolean("enabled", false)
        swFps.isChecked = sp.getBoolean("fpsLock", true)
        swSession.isChecked = sp.getBoolean("session", true)
        swSharp.isChecked = sp.getBoolean("sharp", false)
        swAuto.isChecked = sp.getBoolean("auto", true)

        val pk = SupraEngine.installed(this)
        tvFf.text = if (pk.isEmpty()) "Chưa cài Free Fire" else pk.joinToString(" · ") { if (it.endsWith("max")) "Free Fire MAX" else "Free Fire" }
        try { if (pk.isNotEmpty()) findViewById<ImageView>(R.id.ivIcon).setImageDrawable(packageManager.getApplicationIcon(pk[0])) } catch (_: Exception) {}

        val res = sp.getInt("res", 1).coerceIn(0, 3)
        chips(cgRes, ResLevel.values().map { "${it.pct}%" }, res) { sp.edit().putInt("res", it).apply(); if (sp.getBoolean("enabled", false)) pushGame() else loadState() }
        chips(cgFps, SupraEngine.FPS_OPTIONS.map { "$it FPS" }, SupraEngine.FPS_OPTIONS.indexOf(sp.getInt("fps", 60)).coerceAtLeast(1)) {
            sp.edit().putInt("fps", SupraEngine.FPS_OPTIONS[it]).apply(); if (sp.getBoolean("enabled", false)) pushGame() }
        chips(cgSharp, Sharp.values().map { it.label }, sp.getInt("sharpLv", 1).coerceIn(0, 2)) { sp.edit().putInt("sharpLv", it).apply() }

        val lv = ResLevel.values()[res]
        tvResCap.text = when {
            !sp.getBoolean("enabled", false) -> "Đang tắt · giữ độ phân giải gốc. Bật tối ưu để áp dụng mức ${lv.pct}%."
            sp.getInt("support", -1) == 1 -> "Đã áp dụng ${lv.pct}% (${lv.label}) · khởi động lại Free Fire để có hiệu lực."
            sp.getInt("support", -1) == 0 -> "Máy không hỗ trợ giảm độ phân giải theo từng game (cần Android 13+, một số ROM chặn)."
            else -> "Đang áp dụng…"
        }
        tvSharpCap.text = if (sp.getInt("sharpOk", -1) == 0)
            "Máy/ROM chặn lệnh chỉnh màn hình nên tính năng này không có tác dụng. Hãy tắt."
        else "Tăng tương phản và độ rực khi vào game để hình nét hơn. Đây là tăng cảm giác nét, không phải làm nét thật; có thể tốn GPU."

        llAccent.removeAllViews()
        val d = resources.displayMetrics.density
        accents.forEachIndexed { i, c ->
            llAccent.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt()).apply { marginEnd = (12 * d).toInt() }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(c)
                    if (c == accent) setStroke((3 * d).toInt(), 0xFFFFFFFF.toInt())
                }
                setOnClickListener { sp.edit().putInt("accent", i).apply(); loadState() }
            })
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            val mi = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
            tvRam.text = "${mi.availMem / 1048576} MB"
            val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val t = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            tvTemp.text = "%.1f°C".format(t)
            tvTemp.setTextColor(when { t >= 42 -> 0xFFFF4D6A.toInt(); t >= 39 -> 0xFFFFB300.toInt(); else -> 0xFFE8EAF2.toInt() })
            tvSupport.text = when {
                Build.VERSION.SDK_INT < 33 -> "Cần A13+"
                sp.getInt("support", -1) == 1 -> "Hỗ trợ"
                sp.getInt("support", -1) == 0 -> "Không"
                else -> "Chưa thử"
            }
            val sh = SupraEngine.hasShizuku()
            tvPill.text = if (sh) "● Shizuku sẵn sàng" else "○ Chưa kết nối"
            tvPill.setTextColor(if (sh) 0xFF2EE6A6.toInt() else 0xFFFF4D6A.toInt())
            ui.postDelayed(this, 2000)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!SupraEngine.allGranted(this)) { startActivity(Intent(this, PermissionActivity::class.java)); return }
        loadState(); ui.post(tick)
        if (sp.getBoolean("enabled", false) && sp.getBoolean("auto", true)) startAuto()
    }

    override fun onPause() { ui.removeCallbacks(tick); super.onPause() }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
