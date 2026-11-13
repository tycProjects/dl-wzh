package com.gamerec

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ThumbnailUtils
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File

class MainActivity : Activity() {
    private lateinit var mainBtn: TextView
    private lateinit var prev: Bubble
    private val st = arrayOfNulls<ImageView>(4)
    private fun d(v: Int) = Ui.dp(this, v)
    private fun text(s: String, sz: Float, c: Int, bold: Boolean = false) =
        TextView(this).apply { text = s; textSize = sz; setTextColor(c); if (bold) typeface = Typeface.DEFAULT_BOLD }

    private fun card(title: String, build: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(d(16), d(14), d(16), d(16))
        background = Ui.round(this@MainActivity, Ui.CARD, 20, Ui.LINE)
        layoutParams = LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP).apply { topMargin = d(14) }
        addView(text(title, 15f, Ui.TX, true)); build()
    }

    private fun LinearLayout.seek(label: String, lo: Int, hi: Int, cur: Int, unit: String, on: (Int) -> Unit) {
        val t = text("$label: $cur$unit", 12.5f, Ui.MUT).apply { setPadding(0, d(12), 0, 0) }
        addView(t)
        addView(SeekBar(context).apply {
            max = hi - lo; progress = cur - lo
            progressTintList = ColorStateList.valueOf(Cfg.c1()); thumbTintList = ColorStateList.valueOf(Cfg.c1())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, u: Boolean) { t.text = "$label: ${p + lo}$unit"; if (u) on(p + lo) }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        })
    }

    private fun LinearLayout.chips(label: String, opts: List<String>, sel: Int, on: (Int) -> Unit) {
        addView(text(label, 12.5f, Ui.MUT).apply { setPadding(0, d(12), 0, d(6)) })
        val row = LinearLayout(context); val vs = mutableListOf<TextView>()
        fun paint(i: Int) = vs.forEachIndexed { k, v ->
            v.background = if (k == i) Ui.grad(context, Cfg.c1(), Cfg.c2(), 12) else Ui.round(context, 0xFF1E2440.toInt(), 12)
            v.setTextColor(if (k == i) Color.WHITE else Ui.MUT)
        }
        opts.forEachIndexed { i, o ->
            vs.add(text(o, 13f, Ui.MUT, true).apply {
                gravity = Gravity.CENTER; setPadding(0, d(10), 0, d(10))
                layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f).apply { marginEnd = d(6) }
                setOnClickListener { paint(i); on(i) }
            }); row.addView(vs[i])
        }
        paint(sel); addView(row)
    }

    private fun pill(ic: Int, label: String, a: () -> Unit) = LinearLayout(this).apply {
        gravity = Gravity.CENTER; setPadding(d(12), d(10), d(12), d(10))
        background = Ui.round(this@MainActivity, 0xFF1E2440.toInt(), 12)
        addView(ImageView(this@MainActivity).apply { setImageResource(ic); layoutParams = LinearLayout.LayoutParams(d(20), d(20)) })
        addView(text(label, 13f, Ui.TX, true).apply { setPaddingRelative(d(8), 0, 0, 0) })
        layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f).apply { marginEnd = d(8) }
        setOnClickListener { a() }
    }

    private fun row(i: Int, title: String, sub: String) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL; setPadding(0, d(12), 0, 0)
        st[i] = ImageView(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(d(22), d(22)) }
        addView(st[i])
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL; setPaddingRelative(d(12), 0, 0, 0)
            addView(text(title, 14f, Ui.TX, true)); addView(text(sub, 12f, Ui.MUT))
        })
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); Cfg.init(this)
        window.statusBarColor = Ui.BG; window.navigationBarColor = Ui.BG
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(d(18), d(18), d(18), d(30)) }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Ui.BG); fitsSystemWindows = true; layoutDirection = View.LAYOUT_DIRECTION_RTL; addView(col)
        })
        col.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_logo); setPadding(d(12), d(12), d(12), d(12))
                background = Ui.grad(this@MainActivity, Cfg.c1(), Cfg.c2(), 18); layoutParams = LinearLayout.LayoutParams(d(56), d(56))
            })
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL; setPaddingRelative(d(14), 0, 0, 0)
                addView(text("مسجّل الألعاب", 24f, Ui.TX, true)); addView(text("سجّل وصوّر وأنت داخل اللعبة", 13f, Ui.MUT))
            })
        })
        mainBtn = text("", 17f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER; setPadding(0, d(16), 0, d(16))
            layoutParams = LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP).apply { topMargin = d(20) }
            setOnClickListener { onMain() }
        }
        col.addView(mainBtn)
        col.addView(card("الصلاحيات") {
            addView(row(0, "الظهور فوق التطبيقات", "عشان تطلع الدائرة فوق اللعبة"))
            addView(row(1, "المايك والإشعارات", "لتسجيل صوتك"))
            addView(row(2, "التشغيل في الخلفية", "عشان النظام ما يقفل الدائرة وأنت تلعب"))
            addView(row(3, "الدائرة شغالة", "تسجيل الشاشة يُطلب عند التشغيل"))
        })
        prev = Bubble(this)
        col.addView(card("شكل الدائرة") {
            addView(FrameLayout(this@MainActivity).apply {
                background = Ui.round(this@MainActivity, Ui.BG, 16)
                layoutParams = LinearLayout.LayoutParams(Ui.MATCH, d(120)).apply { topMargin = d(12) }
                addView(prev)
            })
            seek("الحجم", 32, 64, Cfg.size, "") { Cfg.size = it; syncPrev() }
            seek("الشفافية", 40, 100, Cfg.alpha, "%") { Cfg.alpha = it; syncPrev() }
            addView(text("اللون", 12.5f, Ui.MUT).apply { setPadding(0, d(12), 0, 0) })
            addView(LinearLayout(this@MainActivity).apply {
                setPadding(0, d(10), 0, d(4))
                Cfg.pal.forEachIndexed { i, p ->
                    addView(View(this@MainActivity).apply {
                        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(p.first, p.second)).apply {
                            shape = GradientDrawable.OVAL; if (i == Cfg.color) setStroke(d(3), Color.WHITE)
                        }
                        layoutParams = LinearLayout.LayoutParams(d(38), d(38)).apply { marginEnd = d(10) }
                        setOnClickListener { Cfg.color = i; recreate() }
                    })
                }
            })
            addView(LinearLayout(this@MainActivity).apply {
                setPadding(0, d(12), 0, 0)
                addView(pill(R.drawable.ic_image, "اختر صورة") {
                    startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 2)
                })
                addView(pill(R.drawable.ic_close, "إزالة") { File(filesDir, "bubble.png").delete(); syncPrev() })
            })
        })
        col.addView(card("التسجيل") {
            chips("الدقة", listOf("720p", "1080p", "الأعلى"), Cfg.res) { Cfg.res = it }
            chips("الإطارات", listOf("30 fps", "60 fps"), if (Cfg.fps >= 60) 1 else 0) { Cfg.fps = if (it == 1) 60 else 30 }
            chips("العد التنازلي", listOf("بدون", "3 ثواني", "5 ثواني"), when (Cfg.count) { 0 -> 0; 3 -> 1; else -> 2 }) { Cfg.count = listOf(0, 3, 5)[it] }
            chips("مصدر الصوت", listOf("تلقائي (لعبة + مايك)", "مكالمة بالسماعة"), Cfg.audio) { Cfg.audio = it }
            addView(text("أندرويد يمنع تسجيل صوت مكالمات ديسكورد وواتساب مباشرة. عشان يتسجل صوت صاحبك: شغّل المكالمة على السماعة واختر «مكالمة بالسماعة»، فيلتقط المايك صوته وصوتك وصوت اللعبة مع بعض.", 12f, Ui.MUT).apply { setPadding(0, d(12), 0, 0) })
        })
        col.addView(text("الفيديوهات تنحفظ في الملفات: Movies/GameRecorder\nواللقطات: Pictures/GameRecorder", 12f, Ui.MUT).apply { setPadding(d(4), d(16), d(4), 0) })
        syncPrev()
    }

    private fun syncPrev() {
        val s = d(Cfg.size)
        prev.core = s.toFloat(); prev.accent = Cfg.c1(); prev.accent2 = Cfg.c2(); prev.img = Cfg.img(this); prev.alpha = Cfg.alpha / 100f
        val w = (s * 1.45f).toInt()
        prev.layoutParams = FrameLayout.LayoutParams(w, w, Gravity.CENTER)
        prev.invalidate(); FloatingService.inst?.applyCfg()
    }

    private fun battOk() = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
    private fun micOk() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun onMain() {
        when {
            FloatingService.inst != null -> { stopService(Intent(this, FloatingService::class.java)); mainBtn.postDelayed({ refresh() }, 300) }
            !Settings.canDrawOverlays(this) -> startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            !micOk() -> requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS), 1)
            !battOk() && !Cfg.battAsked -> {
                Cfg.battAsked = true
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            }
            else -> startActivityForResult(
                (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent(), 1)
        }
    }

    private fun mark(i: Int, ok: Boolean) = st[i]?.run {
        setImageResource(if (ok) R.drawable.ic_check else R.drawable.ic_dot); setColorFilter(if (ok) Ui.OK else Ui.MUT)
    }

    private fun refresh() {
        val o = Settings.canDrawOverlays(this); val m = micOk(); val on = FloatingService.inst != null
        val bt = battOk()
        mark(0, o); mark(1, m); mark(2, bt); mark(3, on)
        mainBtn.text = when {
            on -> "إيقاف الدائرة"; !o -> "تفعيل الظهور فوق التطبيقات"; !m -> "السماح بالمايك والإشعارات"
            !bt && !Cfg.battAsked -> "السماح بالتشغيل في الخلفية"; else -> "تشغيل الدائرة"
        }
        mainBtn.background = if (on) Ui.round(this, 0xFF2A2F4F.toInt(), 18) else Ui.grad(this, Cfg.c1(), Cfg.c2(), 18)
    }

    override fun onResume() { super.onResume(); FloatingService.appOpen = true; FloatingService.inst?.sync(); refresh() }
    override fun onStop() { FloatingService.appOpen = false; FloatingService.inst?.sync(); super.onStop() }
    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) = refresh()

    @Suppress("DEPRECATION")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 1 && res == RESULT_OK && data != null) {
            startForegroundService(Intent(this, FloatingService::class.java).putExtra("code", res).putExtra("data", data))
            moveTaskToBack(true)
        } else if (req == 2) {
            data?.data?.let { u ->
                contentResolver.openInputStream(u)?.use { s ->
                    val t = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(s), 256, 256)
                    File(filesDir, "bubble.png").outputStream().use { t.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                syncPrev()
            }
        }
    }
}
