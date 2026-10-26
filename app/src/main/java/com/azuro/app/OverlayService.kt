package com.azuro.app

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubble: ImageView
    private lateinit var bubbleLp: WindowManager.LayoutParams
    private lateinit var panel: LinearLayout
    private lateinit var panelLp: WindowManager.LayoutParams
    private lateinit var drawView: DrawView
    private lateinit var log: TextView
    private lateinit var title: TextView
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var useScreen: CheckBox
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private val main = Handler(Looper.getMainLooper())
    private val history = JSONArray()
    private var panelOpen = false
    private var panelWasOpen = false
    private var screenH = 0
    private val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (projection != null || i == null) return START_NOT_STICKY
        startFg()
        @Suppress("DEPRECATION") val data = i.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        setupCapture(i.getIntExtra("code", 0), data)
        buildUi()
        return START_NOT_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("azuro", "Azuro", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "azuro").setContentTitle("Azuro đang chạy")
            .setContentText("Chạm nút nổi để hỏi AI").setSmallIcon(R.drawable.logo).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(1, n)
    }

    private fun realSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 30) { val b = wm.maximumWindowMetrics.bounds; return b.width() to b.height() }
        val dm = DisplayMetrics(); @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm); return dm.widthPixels to dm.heightPixels
    }

    private fun setupCapture(code: Int, data: Intent) {
        val mp = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data)
        projection = mp
        mp.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, main)
        val (w0, h0) = realSize(); screenH = h0
        val s = minOf(1f, 1280f / maxOf(w0, h0)); val w = (w0 * s).toInt(); val h = (h0 * s).toInt()
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        vd = mp.createVirtualDisplay("azuro", w, h, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, null)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        drawView = DrawView(this)
        wm.addView(drawView, WindowManager.LayoutParams(-1, -1, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT))

        val r = dp(20).toFloat()
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) }
        }
        title = TextView(this).apply { text = "Azuro"; textSize = 18f; setTextColor(Color.parseColor("#0A3AB8")) }
        useScreen = CheckBox(this).apply { text = "Xem màn hình"; isChecked = true }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(useScreen)
        head.addView(Button(this).apply { text = "Xoá vẽ"; setOnClickListener { drawView.clear() } })
        head.addView(Button(this).apply { text = "✕"; setOnClickListener { setPanel(false) } })
        panel.addView(head)
        log = TextView(this).apply { text = "Azuro: Chào bạn! Hỏi mình về những gì đang hiện trên màn hình nhé."; textSize = 15f; setTextColor(Color.BLACK) }
        scroll = ScrollView(this).apply { addView(log) }
        panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        input = EditText(this).apply { hint = "Hỏi Azuro…"; maxLines = 3 }
        val row = LinearLayout(this)
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "Gửi"; setOnClickListener { send() } })
        panel.addView(row)
        panelLp = WindowManager.LayoutParams(-1, (screenH * 0.5f).toInt(), type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM; softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        bubble = ImageView(this).apply { setImageResource(R.drawable.logo) }
        bubbleLp = WindowManager.LayoutParams(dp(56), dp(56), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = dp(8); y = dp(200)
        }
        bubble.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = bubbleLp.x; sy = bubbleLp.y; tx = e.rawX; ty = e.rawY; moved = false }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) moved = true
                        if (moved) { bubbleLp.x = sx + dx; bubbleLp.y = sy + dy; wm.updateViewLayout(bubble, bubbleLp) }
                    }
                    MotionEvent.ACTION_UP -> if (!moved) setPanel(!panelOpen)
                }
                return true
            }
        })
        wm.addView(bubble, bubbleLp)
    }

    private fun setPanel(open: Boolean) {
        if (open && !panelOpen) wm.addView(panel, panelLp)
        else if (!open && panelOpen) wm.removeView(panel)
        panelOpen = open
    }

    private fun hideUi() { panelWasOpen = panelOpen; if (panelOpen) setPanel(false); bubble.visibility = View.INVISIBLE; drawView.alpha = 0f }
    private fun showUi() { bubble.visibility = View.VISIBLE; drawView.alpha = 1f; if (panelWasOpen) setPanel(true) }

    private fun addLog(s: String) { log.append("\n\n$s"); scroll.post { scroll.fullScroll(View.FOCUS_DOWN) } }

    private fun send() {
        val t = input.text.toString().trim(); if (t.isEmpty()) return
        input.setText(""); addLog("Bạn: $t"); title.text = "Azuro • đang nghĩ…"
        if (useScreen.isChecked) capture { ask(t, it) } else ask(t, null)
    }

    private fun capture(cb: (Bitmap?) -> Unit) {
        hideUi()
        main.postDelayed({ poll(0, cb) }, 450)
    }

    private fun poll(n: Int, cb: (Bitmap?) -> Unit) {
        val img = try { reader?.acquireLatestImage() } catch (e: Exception) { null }
        if (img != null) { val b = toBitmap(img); img.close(); showUi(); cb(b) }
        else if (n < 15) main.postDelayed({ poll(n + 1, cb) }, 100)
        else { showUi(); cb(null) }
    }

    private fun toBitmap(img: Image): Bitmap {
        val p = img.planes[0]; val pad = p.rowStride - p.pixelStride * img.width
        val b = Bitmap.createBitmap(img.width + pad / p.pixelStride, img.height, Bitmap.Config.ARGB_8888)
        b.copyPixelsFromBuffer(p.buffer)
        return if (pad == 0) b else Bitmap.createBitmap(b, 0, 0, img.width, img.height)
    }

    private fun ask(t: String, bmp: Bitmap?) {
        val sp = getSharedPreferences("azuro", MODE_PRIVATE)
        val key = sp.getString("key", "") ?: ""; val model = sp.getString("model", "claude-sonnet-5-5") ?: "claude-sonnet-5-5"
        Thread {
            val res: Pair<String?, String?> = try { ClaudeClient.chat(key, model, history, t, bmp) to null }
                catch (e: Exception) { null to (e.message ?: "lỗi") }
            main.post { onReply(t, res) }
        }.start()
    }

    private fun onReply(user: String, pair: Pair<String?, String?>) {
        title.text = "Azuro"
        if (pair.first == null) { addLog("Lỗi: ${pair.second}"); return }
        val full = pair.first!!
        val re = Regex("```draw\\s*(.*?)```", RegexOption.DOT_MATCHES_ALL)
        drawView.clear()
        re.find(full)?.let { try { drawView.set(JSONArray(it.groupValues[1].trim())) } catch (_: Exception) {} }
        addLog("Azuro: " + full.replace(re, "").trim())
        history.put(JSONObject().put("role", "user").put("content", user))
        history.put(JSONObject().put("role", "assistant").put("content", full))
        while (history.length() > 10) history.remove(0)
    }

    override fun onDestroy() {
        try { setPanel(false); wm.removeView(bubble); wm.removeView(drawView) } catch (_: Exception) {}
        vd?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
}
