package com.azuro.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.*
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
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var drawView: DrawView
    private lateinit var bubble: FrameLayout
    private lateinit var bubbleImg: ImageView
    private lateinit var bubbleLp: WindowManager.LayoutParams
    private lateinit var panel: LinearLayout
    private lateinit var panelLp: WindowManager.LayoutParams
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendBtn: ImageView
    private lateinit var status: TextView
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private val main = Handler(Looper.getMainLooper())
    private val history = JSONArray()
    private var panelOpen = false; private var panelAttached = false; private var panelWasOpen = false
    private var sw = 0; private var sh = 0
    private var busy = false; private var screenOn = true
    private var lastScene: String? = null
    private var elements = HashMap<Int, RectF>()
    private var pulseAnim: ObjectAnimator? = null
    private val clearMarks = Runnable { if (::drawView.isInitialized) drawView.clear() }
    private val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private val prefs get() = getSharedPreferences("azuro", MODE_PRIVATE)

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (i?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (projection != null || i == null) return START_NOT_STICKY
        T.init(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startFg()
        @Suppress("DEPRECATION") val data = i.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
        setupCapture(i.getIntExtra("code", 0), data)
        buildUi()
        return START_NOT_STICKY
    }

    private fun startFg() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("azuro", "Azuro", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 1, Intent(this, OverlayService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        @Suppress("DEPRECATION")
        val n = Notification.Builder(this, "azuro").setContentTitle("Azuro is running")
            .setContentText("Tap the floating logo to ask about your screen")
            .setSmallIcon(R.drawable.ic_eye).addAction(0, "Stop", stop).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(1, n)
    }

    private fun realSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 30) { val b = wm.maximumWindowMetrics.bounds; return b.width() to b.height() }
        val dm = DisplayMetrics(); @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm); return dm.widthPixels to dm.heightPixels
    }

    private fun setupCapture(code: Int, data: Intent) {
        val mp = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data)
        projection = mp
        mp.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, main)
        val (w0, h0) = realSize(); sw = w0; sh = h0
        val s = minOf(1f, 1280f / maxOf(w0, h0)); val w = (w0 * s).toInt(); val h = (h0 * s).toInt()
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        vd = mp.createVirtualDisplay("azuro", w, h, resources.displayMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, null)
    }

    // ───────────────────────── UI ─────────────────────────
    private fun buildUi() {
        drawView = DrawView(this).also { it.setScreen(sw, sh) }
        wm.addView(drawView, WindowManager.LayoutParams(sw, sh, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= 30) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        })

        // panel
        panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = topShape(T.bg, dpf(26f)) }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(12), dp(6), dp(10)) }
        head.addView(ImageView(this).apply { setImageResource(R.drawable.logo) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        val hc = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        hc.addView(tv("Azuro", 17f, T.ink, true))
        status = tv("Ready", 12f, T.mut); hc.addView(status, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
        head.addView(hc, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        head.addView(iconBtn(R.drawable.ic_close, T.mut) { setPanel(false, true) })
        panel.addView(head); panel.addView(divider())

        chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(6), dp(14), dp(14)) }
        scroll = ScrollView(this).apply { addView(chat); isVerticalScrollBarEnabled = false }
        panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val tools = LinearLayout(this).apply { setPadding(dp(12), dp(6), dp(12), dp(2)) }
        val sc = Chip(this, R.drawable.ic_eye, "Screen", true) { screenOn = it }; sc.set(true)
        tools.addView(sc.view)
        tools.addView(Chip(this, R.drawable.ic_cancel, "Clear marks", false) { main.removeCallbacks(clearMarks); drawView.clear() }.view, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        tools.addView(Chip(this, R.drawable.ic_refresh, "New chat", false) { newChat() }.view, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        panel.addView(HorizontalScrollView(this).apply { addView(tools); isHorizontalScrollBarEnabled = false })

        val bar = LinearLayout(this).apply { gravity = Gravity.BOTTOM; setPadding(dp(12), dp(6), dp(12), dp(12)) }
        input = EditText(this).apply {
            hint = "Ask about your screen"; setHintTextColor(T.mut); setTextColor(T.ink); textSize = 15f; background = null; maxLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(dp(4), dp(11), dp(4), dp(11))
        }
        val field = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), 0, dp(8), 0); background = shape(T.field, dpf(24f)) }
        field.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        sendBtn = iconBtn(R.drawable.ic_send, Color.WHITE, 38, T.pri) { send() }
        field.addView(sendBtn, LinearLayout.LayoutParams(dp(38), dp(38)).apply { gravity = Gravity.BOTTOM; bottomMargin = dp(5); marginStart = dp(6) })
        bar.addView(field, LinearLayout.LayoutParams(-1, -2))
        panel.addView(bar)
        panelLp = WindowManager.LayoutParams(-1, (sh * 0.62f).toInt(), type, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM; softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        addIntro()

        // bubble
        val size = dp(64)
        bubbleImg = ImageView(this).apply { setImageResource(R.drawable.logo); background = shape(T.card, dpf(16f)); elevation = dpf(6f); clipToOutline = true }
        bubble = FrameLayout(this).apply { addView(bubbleImg, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER)) }
        bubbleLp = WindowManager.LayoutParams(size, size, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = sw - size + dp(6); y = (sh * 0.35f).toInt()
        }
        bubble.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false; var longFired = false
            val longPress = Runnable {
                if (!moved) { longFired = true; bubble.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); main.removeCallbacks(clearMarks); drawView.clear() }
            }
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = bubbleLp.x; sy = bubbleLp.y; tx = e.rawX; ty = e.rawY; moved = false; longFired = false; main.postDelayed(longPress, 550) }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                        if (abs(dx) > dp(6) || abs(dy) > dp(6)) { moved = true; main.removeCallbacks(longPress) }
                        if (moved) { bubbleLp.x = sx + dx; bubbleLp.y = (sy + dy).coerceIn(0, sh - dp(64)); wm.updateViewLayout(bubble, bubbleLp) }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        main.removeCallbacks(longPress)
                        if (moved) snap() else if (!longFired && e.action == MotionEvent.ACTION_UP) setPanel(!panelOpen, true)
                    }
                }
                return true
            }
        })
        wm.addView(bubble, bubbleLp)
    }

    private fun snap() {
        val size = dp(64); val target = if (bubbleLp.x + size / 2 < sw / 2) -dp(6) else sw - size + dp(6)
        ValueAnimator.ofInt(bubbleLp.x, target).apply {
            duration = 180
            addUpdateListener { bubbleLp.x = it.animatedValue as Int; try { wm.updateViewLayout(bubble, bubbleLp) } catch (_: Exception) {} }
            start()
        }
    }

    private fun setPanel(open: Boolean, animate: Boolean) {
        panelOpen = open
        if (open) {
            panel.animate().cancel()
            if (!panelAttached) {
                wm.addView(panel, panelLp); panelAttached = true
                if (animate) { panel.translationY = panelLp.height.toFloat(); panel.animate().translationY(0f).setDuration(200).start() } else panel.translationY = 0f
            } else panel.animate().translationY(0f).setDuration(150).start()
        } else if (panelAttached) {
            if (animate) panel.animate().translationY(panelLp.height.toFloat()).setDuration(170).withEndAction {
                if (!panelOpen && panelAttached) { try { wm.removeView(panel) } catch (_: Exception) {}; panelAttached = false }
            }.start()
            else { panel.animate().cancel(); try { wm.removeView(panel) } catch (_: Exception) {}; panelAttached = false }
        }
    }

    private fun hideUi() { panelWasOpen = panelOpen; if (panelAttached) setPanel(false, false); bubble.visibility = View.INVISIBLE; drawView.alpha = 0f }
    private fun showUi() { bubble.visibility = View.VISIBLE; drawView.alpha = 1f; if (panelWasOpen) setPanel(true, false) }

    private fun setBusy(b: Boolean, msg: String) {
        busy = b; status.text = msg; sendBtn.alpha = if (b) 0.4f else 1f
        pulseAnim?.cancel(); bubbleImg.alpha = 1f
        if (b) pulseAnim = ObjectAnimator.ofFloat(bubbleImg, "alpha", 1f, 0.45f).apply { duration = 650; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE; start() }
    }

    // ───────────────────────── messages ─────────────────────────
    private fun scrollDown() { scroll.post { scroll.fullScroll(View.FOCUS_DOWN) } }

    private fun addIntro() {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); background = shape(T.priSoft, dpf(16f)) }
        c.addView(tv("Ask anything about what’s on your screen.", 15f, T.ink, true))
        c.addView(tv("I’ll read it, explain it, and point at what matters. Long-press the logo to clear marks.", 13f, T.mut), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
        chat.addView(c, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
    }

    private fun newChat() {
        while (history.length() > 0) history.remove(0)
        lastScene = null; elements.clear(); main.removeCallbacks(clearMarks); drawView.clear()
        chat.removeAllViews(); addIntro()
    }

    private fun addUser(text: String) {
        val t = tv(text, 15f, Color.WHITE).apply {
            setPadding(dp(14), dp(10), dp(14), dp(10)); background = shape(T.pri, dpf(18f)); maxWidth = (sw * 0.8f).toInt(); setTextIsSelectable(true); setLineSpacing(dpf(2f), 1f)
        }
        chat.addView(t, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END; topMargin = dp(10); marginStart = dp(40) }); scrollDown()
    }

    private fun addNote(text: String, error: Boolean) {
        chat.addView(tv(text, 12.5f, if (error) T.err else T.mut).apply { gravity = Gravity.CENTER; setPadding(dp(8), dp(4), dp(8), dp(4)) },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }); scrollDown()
    }

    private fun addScene(summary: String, model: String) {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)); background = shape(T.field, dpf(14f)) }
        c.addView(tv("Screen read by ${model.substringAfterLast('/')}  ·  tap to expand", 11.5f, T.mut, true))
        val body = tv(summary, 12.5f, T.mut).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END; setLineSpacing(dpf(2f), 1f) }
        c.addView(body, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
        c.setOnClickListener { val open = body.maxLines == 2; body.maxLines = if (open) 40 else 2 }
        chat.addView(c, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }); scrollDown()
    }

    private fun copyBtn(label: String, text: () -> String): View {
        val r = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(6), dp(10), dp(6)); background = ripple(shape(0, 0f), dpf(10f)) }
        val ic = icon(R.drawable.ic_copy, T.mut, 14); val tx = tv(label, 12f, T.mut, true)
        r.addView(ic); r.addView(tx, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(5) })
        r.setOnClickListener {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Azuro", text()))
            ic.setImageResource(R.drawable.ic_check); ic.setColorFilter(T.ok); tx.text = "Copied"; tx.setTextColor(T.ok)
            main.postDelayed({ ic.setImageResource(R.drawable.ic_copy); ic.setColorFilter(T.mut); tx.text = label; tx.setTextColor(T.mut) }, 1300)
        }
        return r
    }

    private fun md(src: String): CharSequence {
        val s = src.replace(Regex("(?m)^#{1,6}\\s*(.+)\$"), "**\$1**").replace(Regex("(?m)^\\s*[-*]\\s+"), "• ")
        val sb = SpannableStringBuilder(); var i = 0
        for (m in Regex("\\*\\*(.+?)\\*\\*|`([^`\\n]+)`").findAll(s)) {
            sb.append(s.substring(i, m.range.first)); val st = sb.length
            if (m.groupValues[1].isNotEmpty()) { sb.append(m.groupValues[1]); sb.setSpan(StyleSpan(Typeface.BOLD), st, sb.length, 0) }
            else { sb.append(m.groupValues[2]); sb.setSpan(TypefaceSpan("monospace"), st, sb.length, 0); sb.setSpan(BackgroundColorSpan(T.field), st, sb.length, 0) }
            i = m.range.last + 1
        }
        return sb.append(s.substring(i))
    }

    private fun addCode(lang: String, code: String, into: LinearLayout) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = shape(T.field, dpf(12f), T.line, dp(1)) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(2), dp(2), 0) }
        top.addView(tv(lang.ifEmpty { "code" }, 11.5f, T.mut, true), LinearLayout.LayoutParams(0, -2, 1f)); top.addView(copyBtn("Copy") { code })
        box.addView(top)
        val body = tv(code, 12.5f, T.ink).apply { typeface = Typeface.MONOSPACE; setTextIsSelectable(true); setPadding(dp(12), dp(2), dp(12), dp(12)) }
        box.addView(HorizontalScrollView(this).apply { addView(body); isHorizontalScrollBarEnabled = false })
        into.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4); bottomMargin = dp(8) })
    }

    private fun addPara(t: String, into: LinearLayout) {
        if (t.isBlank()) return
        into.addView(tv(md(t.trim()), 15f, T.ink).apply { setTextIsSelectable(true); setLineSpacing(dpf(3f), 1f) }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
    }

    private fun addAzuro(text: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(8), dp(4)); background = shape(T.card, dpf(18f), T.line, dp(1)) }
        var i = 0
        for (m in Regex("```([\\w+#.-]*)[ \\t]*\\n?(.*?)```", RegexOption.DOT_MATCHES_ALL).findAll(text)) {
            addPara(text.substring(i, m.range.first), box); addCode(m.groupValues[1], m.groupValues[2].trimEnd('\n'), box); i = m.range.last + 1
        }
        addPara(text.substring(i), box)
        box.addView(copyBtn("Copy") { text }, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END })
        chat.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); marginEnd = dp(24) }); scrollDown()
    }

    // ───────────────────────── capture ─────────────────────────
    private fun capture(cb: (Bitmap?) -> Unit) { hideUi(); main.postDelayed({ poll(0, cb) }, 450) }

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

    /** Faint reference marks every 100 units (0-1000) + edge labels, so the vision model can localise precisely. */
    private fun addGrid(src: Bitmap): Bitmap {
        val b = src.copy(Bitmap.Config.ARGB_8888, true); val c = Canvas(b); val w = b.width.toFloat(); val h = b.height.toFloat()
        val ts = h / 75f
        val plus = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(210, 255, 0, 200); strokeWidth = max(1.5f, ts / 9); style = Paint.Style.STROKE }
        val halo = Paint(plus).apply { color = Color.argb(170, 255, 255, 255); strokeWidth = plus.strokeWidth * 2.6f }
        val tx = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(200, 0, 160); textSize = ts; isFakeBoldText = true; setShadowLayer(ts / 5, 0f, 0f, Color.WHITE) }
        val k = ts * 0.4f
        for (i in 1..9) for (j in 1..9) { val x = w * i / 10f; val y = h * j / 10f; for (p in listOf(halo, plus)) { c.drawLine(x - k, y, x + k, y, p); c.drawLine(x, y - k, x, y + k, p) } }
        for (i in 1..9) {
            val s = (i * 100).toString()
            c.drawText(s, w * i / 10f - tx.measureText(s) / 2, ts * 1.1f, tx); c.drawText(s, 3f, h * i / 10f + ts / 3, tx)
        }
        return b
    }

    // ───────────────────────── pipeline: vision model → chat model ─────────────────────────
    private fun send() {
        if (busy) return
        val t = input.text.toString().trim(); if (t.isEmpty()) return
        input.setText(""); addUser(t); main.removeCallbacks(clearMarks); drawView.clear()
        if (screenOn) { setBusy(true, "Capturing screen…"); capture { bmp -> if (bmp == null) addNote("Couldn’t capture the screen. Answering without it.", true); run(t, bmp) } }
        else { setBusy(true, "Thinking…"); run(t, null) }
    }

    private fun run(t: String, bmp: Bitmap?) {
        val key = prefs.getString("key", "") ?: ""
        val vm = prefs.getString("vmodel", DEF_V) ?: DEF_V; val cm = prefs.getString("cmodel", DEF_C) ?: DEF_C
        val useGrid = prefs.getBoolean("grid", true)
        Thread {
            try {
                var sceneStr: String? = null
                if (bmp != null) {
                    main.post { status.text = "Reading your screen…" }
                    val img = if (useGrid) addGrid(bmp) else bmp
                    val raw = Api.chat(key, vm, JSONArray().put(Api.sys(Prompts.VISION)).put(Api.user("User question: $t", img)), 2200)
                    val j = Api.json(raw); val els = HashMap<Int, RectF>()
                    val arr = j?.optJSONArray("elements")
                    if (arr != null) for (k in 0 until arr.length()) {
                        val o = arr.optJSONObject(k) ?: continue; val bx = o.optJSONArray("box") ?: continue
                        if (bx.length() < 4) continue
                        val x1 = bx.optDouble(0) / 1000; val y1 = bx.optDouble(1) / 1000; val x2 = bx.optDouble(2) / 1000; val y2 = bx.optDouble(3) / 1000
                        els[o.optInt("id", k + 1)] = RectF(minOf(x1, x2).coerceIn(0.0, 1.0).toFloat(), minOf(y1, y2).coerceIn(0.0, 1.0).toFloat(),
                            maxOf(x1, x2).coerceIn(0.0, 1.0).toFloat(), maxOf(y1, y2).coerceIn(0.0, 1.0).toFloat())
                    }
                    sceneStr = j?.toString() ?: raw.take(2500)
                    val sum = j?.optString("summary")?.takeIf { it.isNotBlank() } ?: raw.take(300)
                    main.post { elements = els; lastScene = sceneStr; addScene(sum, vm); status.text = "Thinking…" }
                }
                val ctx = sceneStr ?: lastScene?.let { "(from an earlier screenshot, may be outdated) $it" }
                val msgs = JSONArray().put(Api.sys(Prompts.CHAT))
                for (k in 0 until history.length()) msgs.put(history.get(k))
                msgs.put(Api.user(if (ctx != null) "Screen (JSON):\n$ctx\n\nUser: $t" else t, null))
                val reply = Api.chat(key, cm, msgs, 1400)
                main.post { onReply(t, reply) }
            } catch (e: Exception) { main.post { fail(e) } }
        }.start()
    }

    private fun onReply(user: String, reply: String) {
        setBusy(false, "Ready")
        val re = Regex("```draw\\s*(.*?)(```|\$)", RegexOption.DOT_MATCHES_ALL)
        val m = re.find(reply)
        val text = reply.replace(re, "").trim().ifEmpty { "Done." }
        if (m != null) try {
            val a = JSONArray(m.groupValues[1].trim()); val list = ArrayList<DrawView.Sh>()
            for (k in 0 until a.length()) {
                val o = a.optJSONObject(k) ?: continue; val box = elements[o.optInt("id", -1)] ?: continue
                list += DrawView.Sh(o.optString("t", "box"), RectF(box), o.optString("label", ""), o.optInt("n", list.size + 1))
            }
            if (list.isNotEmpty()) { drawView.set(list); main.removeCallbacks(clearMarks); main.postDelayed(clearMarks, 60000) }
            else addNote("Couldn’t locate that element on screen.", false)
        } catch (_: Exception) { addNote("Couldn’t read the markers.", false) }
        history.put(JSONObject().put("role", "user").put("content", user)); history.put(JSONObject().put("role", "assistant").put("content", reply))
        while (history.length() > 8) history.remove(0)
        addAzuro(text)
    }

    private fun fail(e: Exception) {
        setBusy(false, "Ready"); val m = e.message ?: "Unknown error"
        addNote(when {
            m.startsWith("HTTP 429") -> "Rate limit reached. Wait a moment or choose another model."
            m.startsWith("HTTP 401") -> "OpenRouter rejected your API key."
            m.startsWith("HTTP 402") -> "This model needs credits on your OpenRouter account."
            m.startsWith("HTTP 404") -> "That model isn’t available right now. Pick another in Azuro."
            else -> m
        }, true)
    }

    override fun onDestroy() {
        pulseAnim?.cancel(); main.removeCallbacksAndMessages(null)
        try { if (panelAttached) wm.removeView(panel); wm.removeView(bubble); wm.removeView(drawView) } catch (_: Exception) {}
        vd?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
}
