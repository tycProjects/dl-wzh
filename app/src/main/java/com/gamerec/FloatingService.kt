package com.gamerec

import android.animation.ValueAnimator
import android.app.*
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import kotlin.math.abs

class FloatingService : Service() {
    companion object { @Volatile var inst: FloatingService? = null; @Volatile var appOpen = false }

    private lateinit var wm: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var bubble: Bubble
    private lateinit var panel: LinearLayout
    private lateinit var recIv: ImageView
    private lateinit var timeTv: TextView
    private val h = Handler(Looper.getMainLooper())
    private var proj: MediaProjection? = null
    private var vd: VirtualDisplay? = null
    private var rec: Recorder? = null
    private var counting = false
    private var W = 0; private var H = 0; private var D = 0
    private var t0 = 0L

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() { super.onCreate(); Cfg.init(this); wm = getSystemService(WINDOW_SERVICE) as WindowManager; buildUi() }

    @Suppress("DEPRECATION")
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (i?.action == "STOP_REC") { h.post { if (rec != null) toggleRec() }; return START_NOT_STICKY }
        if (proj != null || i == null) return START_NOT_STICKY
        Mix.micOn = true; Mix.sysOn = true; Mix.mic = 1f; Mix.sys = 1f
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("rec", "Recorder", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, notif(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        val data = i.getParcelableExtra<Intent>("data")!!
        proj = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            .getMediaProjection(i.getIntExtra("code", 0), data)
        proj!!.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, h)
        measure()
        vd = proj!!.createVirtualDisplay("rec", W, H, D, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, null)
        wm.addView(root, lp); inst = this; sync()
        return START_NOT_STICKY
    }

    // الدائرة ما تختفي أبداً وقت التسجيل أو العد التنازلي
    fun sync() { h.post { root.visibility = if (appOpen && rec == null && !counting) View.GONE else View.VISIBLE } }

    private fun notif(): Notification {
        val b = Notification.Builder(this, "rec").setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(if (rec != null) "جاري التسجيل" else "مسجّل الألعاب شغال")
        if (rec != null) b.addAction(Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_stop), "إيقاف التسجيل",
            PendingIntent.getService(this, 1, Intent(this, FloatingService::class.java).setAction("STOP_REC"), PendingIntent.FLAG_IMMUTABLE)).build())
        return b.build()
    }
    private fun notifyUpd() = getSystemService(NotificationManager::class.java).notify(1, notif())

    fun applyCfg() {
        val s = dp(Cfg.size)
        bubble.core = s.toFloat(); bubble.accent = Cfg.c1(); bubble.accent2 = Cfg.c2(); bubble.img = Cfg.img(this)
        bubble.alpha = Cfg.alpha / 100f
        bubble.layoutParams = LinearLayout.LayoutParams((s * 1.45f).toInt(), (s * 1.45f).toInt())
        bubble.invalidate()
    }

    @Suppress("DEPRECATION")
    private fun measure() {
        val dm = DisplayMetrics(); wm.defaultDisplay.getRealMetrics(dm)
        val cap = when (Cfg.res) { 0 -> 1280f; 1 -> 1920f; else -> 2560f }
        val s = minOf(1f, cap / maxOf(dm.widthPixels, dm.heightPixels))
        W = (dm.widthPixels * s).toInt() and 1.inv(); H = (dm.heightPixels * s).toInt() and 1.inv(); D = dm.densityDpi
    }

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun toast(t: String) = h.post { Toast.makeText(this, t, Toast.LENGTH_SHORT).show() }

    private fun tool(ic: Int, label: String, a: (ImageView) -> Unit): Pair<LinearLayout, ImageView> {
        val iv = ImageView(this).apply {
            setImageResource(ic); setPadding(dp(11), dp(11), dp(11), dp(11))
            background = Ui.round(this@FloatingService, 0xFF232A48.toInt(), 14); layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
        }
        val tv = TextView(this).apply { text = label; textSize = 10.5f; setTextColor(Ui.MUT); gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; addView(iv); addView(tv)
            setOnClickListener { a(iv) }; layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        }
        return box to iv
    }

    private fun slider(label: String, set: (Float) -> Unit) {
        panel.addView(TextView(this).apply { text = label; textSize = 11f; setTextColor(Ui.MUT); setPadding(0, dp(10), 0, 0) })
        panel.addView(SeekBar(this).apply {
            max = 200; progress = 100
            progressTintList = ColorStateList.valueOf(Cfg.c1()); thumbTintList = ColorStateList.valueOf(Cfg.c1())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, u: Boolean) = set(p / 100f)
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        })
    }

    private fun buildUi() {
        bubble = Bubble(this)
        timeTv = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; visibility = View.GONE
            background = Ui.round(this@FloatingService, Ui.RED, 10); setPadding(dp(8), dp(2), dp(8), dp(2))
        }
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE; setPadding(dp(12), dp(12), dp(12), dp(14))
            layoutParams = LinearLayout.LayoutParams(dp(250), Ui.WRAP).apply { topMargin = dp(6) }
            background = Ui.round(this@FloatingService, 0xF2151A2E.toInt(), 20, Ui.LINE)
        }
        val shot = tool(R.drawable.ic_shot, "لقطة") { screenshot() }
        val rc = tool(R.drawable.ic_rec, "تسجيل") { toggleRec() }; recIv = rc.second
        val mic = tool(R.drawable.ic_mic, "المايك") { Mix.micOn = !Mix.micOn; it.setImageResource(if (Mix.micOn) R.drawable.ic_mic else R.drawable.ic_mic_off) }
        val sys = tool(R.drawable.ic_spk, "اللعبة") { Mix.sysOn = !Mix.sysOn; it.setImageResource(if (Mix.sysOn) R.drawable.ic_spk else R.drawable.ic_spk_off) }
        val set = tool(R.drawable.ic_tune, "الإعدادات") {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        val cls = tool(R.drawable.ic_close, "إغلاق") { stopSelf() }
        panel.addView(LinearLayout(this).apply { addView(shot.first); addView(rc.first); addView(mic.first) })
        panel.addView(LinearLayout(this).apply { setPadding(0, dp(10), 0, 0); addView(sys.first); addView(set.first); addView(cls.first) })
        slider("صوت المايك") { Mix.mic = it }
        slider("صوت اللعبة") { Mix.sys = it }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            addView(bubble); addView(timeTv); addView(panel)
        }
        applyCfg()
        lp = WindowManager.LayoutParams(Ui.WRAP, Ui.WRAP, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = dp(200) }
        var dx = 0f; var dy = 0f; var sx = 0; var sy = 0; var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; sx = lp.x; sy = lp.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(e.rawX - dx) + abs(e.rawY - dy) > 12) moved = true
                    if (moved) { lp.x = (sx + e.rawX - dx).toInt(); lp.y = (sy + e.rawY - dy).toInt(); wm.updateViewLayout(root, lp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) {
                    panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    if (panel.visibility == View.VISIBLE) {
                        lp.x = lp.x.coerceIn(0, maxOf(0, resources.displayMetrics.widthPixels - dp(250)))
                        lp.y = minOf(lp.y, resources.displayMetrics.heightPixels - dp(340))
                    }
                    wm.updateViewLayout(root, lp)
                } else if (panel.visibility != View.VISIBLE) {
                    val to = if (lp.x + root.width / 2 < resources.displayMetrics.widthPixels / 2) 0 else resources.displayMetrics.widthPixels - root.width
                    ValueAnimator.ofInt(lp.x, to).apply {
                        duration = 180
                        addUpdateListener { lp.x = it.animatedValue as Int; try { wm.updateViewLayout(root, lp) } catch (_: Exception) {} }
                        start()
                    }
                }
            }
            true
        }
    }

    private fun screenshot() {
        panel.visibility = View.GONE
        val hide = rec == null
        if (hide) root.visibility = View.INVISIBLE
        h.postDelayed({ sync() }, 2500)
        h.postDelayed({
            if (rec == null) { measure(); vd?.resize(W, H, D) }
            val r = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 2)
            r.setOnImageAvailableListener({ rd ->
                val img = rd.acquireLatestImage() ?: return@setOnImageAvailableListener
                rd.setOnImageAvailableListener(null, null)
                val p = img.planes[0]
                val bmp = Bitmap.createBitmap(p.rowStride / p.pixelStride, H, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(p.buffer); img.close()
                vd?.setSurface(rec?.surface); rd.close(); sync()
                saveShot(Bitmap.createBitmap(bmp, 0, 0, W, H))
            }, h)
            vd?.setSurface(r.surface)
        }, if (hide) 300L else 0L)
    }

    private fun saveShot(b: Bitmap) = Thread {
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "shot_${stamp()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GameRecorder")
        }
        val u = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
        contentResolver.openOutputStream(u)!!.use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        toast("تم حفظ اللقطة")
    }.start()

    private val tick = object : Runnable {
        override fun run() {
            val s = (System.currentTimeMillis() - t0) / 1000
            timeTv.text = "%02d:%02d".format(s / 60, s % 60); h.postDelayed(this, 1000)
        }
    }

    private fun countdown(n: Int) {
        bubble.label = if (n > 0) "$n" else null; bubble.invalidate()
        if (n > 0) h.postDelayed({ countdown(n - 1) }, 1000) else startRec()
    }

    private fun toggleRec() {
        panel.visibility = View.GONE
        val cur = rec
        if (cur == null) { if (!counting) { counting = true; sync(); countdown(Cfg.count) } }
        else {
            rec = null; h.removeCallbacks(tick); timeTv.visibility = View.GONE
            vd?.setSurface(null); recIv.setImageResource(R.drawable.ic_rec); bubble.setRec(false); notifyUpd(); sync()
            cur.stop { toast("تم حفظ الفيديو") }
        }
    }

    private fun startRec() {
        counting = false
        try {
            measure(); vd?.resize(W, H, D)
            val base = listOf(6, 10, 16)[Cfg.res] * 1_000_000
            val r = Recorder(this, proj!!, W, H, Cfg.fps, if (Cfg.fps >= 60) base * 3 / 2 else base, Cfg.audio)
            vd?.setSurface(r.surface); r.begin(); rec = r
            t0 = System.currentTimeMillis(); timeTv.visibility = View.VISIBLE; h.post(tick)
            recIv.setImageResource(R.drawable.ic_stop); bubble.setRec(true); notifyUpd(); sync()
        } catch (e: Exception) { toast("ما انبدأ التسجيل: ${e.message}"); sync() }
    }

    override fun onDestroy() {
        inst = null; h.removeCallbacksAndMessages(null)
        rec?.stop {}; vd?.release(); proj?.stop()
        try { wm.removeView(root) } catch (_: Exception) {}
        super.onDestroy()
    }
}
