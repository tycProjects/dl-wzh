package com.example.mapphim

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Gom mọi thay đổi của các "ngón tay" vào một lệnh chạm duy nhất, tránh các lệnh hủy lẫn nhau. */
class Dispatcher(private val svc: AccessibilityService) {
    private val touches = ArrayList<Touch>()
    private var busy = false

    fun add(t: Touch) { if (!touches.contains(t)) touches.add(t) }

    fun flush() {
        if (busy) return
        val sent = touches.filter { it.pending != null }
        if (sent.isEmpty()) { touches.removeAll { it.stroke == null }; return }
        val b = GestureDescription.Builder()
        sent.forEach { b.addStroke(it.build()) }
        busy = true
        val cb = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { busy = false; flush() }
            override fun onCancelled(g: GestureDescription?) { sent.forEach { it.stroke = null }; busy = false; flush() }
        }
        if (!svc.dispatchGesture(b.build(), cb, null)) { sent.forEach { it.stroke = null }; busy = false }
    }
}

/** Một "ngón tay" ảo: giữ được nét chạm liên tục và di chuyển. */
class Touch(private val disp: Dispatcher) {
    var stroke: StrokeDescription? = null
    var pending: Triple<Float, Float, Boolean>? = null
    private var cx = 0f
    private var cy = 0f
    val isDown get() = stroke != null || pending?.third == true

    fun move(x: Float, y: Float, keep: Boolean) {
        pending = Triple(x, y, keep); disp.add(this); disp.flush()
    }

    fun release() {
        val p = pending
        if (p != null) move(p.first, p.second, false) else if (stroke != null) move(cx, cy, false)
    }

    fun build(): StrokeDescription {
        val (x, y, keep) = pending!!
        pending = null
        val path = Path()
        val s = stroke
        val d = if (s == null) {
            path.moveTo(x, y); StrokeDescription(path, 0, 40, keep)
        } else {
            path.moveTo(cx, cy); path.lineTo(x, y); s.continueStroke(path, 0, 40, keep)
        }
        cx = x; cy = y
        stroke = if (keep) d else null
        return d
    }
}

class Mapper(private val svc: AccessibilityService) {
    var paused = false
    private var hasUser = false
    private var expires = 0L
    val loggedIn get() = hasUser && (expires == 0L || expires > System.currentTimeMillis() / 1000)
    private val handler = Handler(Looper.getMainLooper())
    private val dm = svc.resources.displayMetrics
    private val W = max(dm.widthPixels, dm.heightPixels).toFloat()
    private val H = min(dm.widthPixels, dm.heightPixels).toFloat()
    private val disp = Dispatcher(svc)
    private var mul = 1f
    private var items = listOf<Item>()

    inner class Item(o: JSONObject) {
        val t = o.getString("t")
        val px = o.getDouble("x").toFloat() / 100f * W
        val py = o.getDouble("y").toFloat() / 100f * H
        val s = o.getDouble("s").toFloat() / 100f * W
        val k = o.getJSONArray("k").let { a -> List(a.length()) { a.getString(it) } }
        val sens = o.optInt("sens", 50)
        val touch = Touch(disp)
        val dirs = BooleanArray(4)
        var on = false
        var lx = 0f
        var ly = 0f
        val idle = Runnable { if (on) { touch.move(lx, ly, false); on = false } }
    }

    fun reload() {
        releaseAll()
        val sp = svc.getSharedPreferences("mapphim", Context.MODE_PRIVATE)
        mul = sp.getFloat("sens_mul", 1f)
        hasUser = sp.getString("user", null) != null
        expires = sp.getLong("expires", 0L)
        val raw = sp.getString("profile", "[]")
        items = try {
            val a = JSONArray(raw)
            List(a.length()) { Item(a.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    fun releaseAll() {
        for (i in items) {
            handler.removeCallbacks(i.idle)
            i.touch.release(); i.dirs.fill(false); i.on = false
        }
    }

    /** Trả về true nếu phím này có trong bố cục (sẽ bị chặn không gửi cho app). */
    fun onKey(name: String, down: Boolean, repeat: Boolean): Boolean {
        var hit = false
        for (i in items) {
            val n = i.k.indexOf(name)
            if (n < 0 || i.t == "look") continue
            hit = true
            if (repeat) continue
            when (i.t) {
                "joy" -> { i.dirs[n] = down; joy(i) }
                "tap" -> if (down) i.touch.move(i.px, i.py, false)
                "hold", "scope" -> i.touch.move(i.px, i.py, down)
                "toggle" -> if (down) { i.on = !i.on; i.touch.move(i.px, i.py, i.on) }
            }
        }
        return hit
    }

    private fun joy(i: Item) {
        val dx = (if (i.dirs[3]) 1 else 0) - (if (i.dirs[1]) 1 else 0)
        val dy = (if (i.dirs[2]) 1 else 0) - (if (i.dirs[0]) 1 else 0)
        if (dx == 0 && dy == 0) { if (i.touch.isDown) i.touch.move(i.px, i.py, false); return }
        val r = i.s / 2f * 0.8f
        val d = if (dx != 0 && dy != 0) 0.707f else 1f
        if (!i.touch.isDown) i.touch.move(i.px, i.py, true)
        i.touch.move(i.px + dx * d * r, i.py + dy * d * r, true)
    }

    /** dx, dy: chuyển động chuột tương đối (pixel). */
    fun onMouseMove(dx: Float, dy: Float) {
        for (i in items) {
            if (i.t != "look") continue
            val k = i.sens / 50f * 1.5f * mul
            val hw = i.s / 2f
            val hh = i.s * 0.6f / 2f
            if (!i.on) { i.on = true; i.lx = i.px; i.ly = i.py; i.touch.move(i.lx, i.ly, true) }
            i.lx += dx * k; i.ly += dy * k
            val ox = i.lx.coerceIn(i.px - hw, i.px + hw)
            val oy = i.ly.coerceIn(i.py - hh, i.py + hh)
            if (ox != i.lx || oy != i.ly) {      // chạm mép vùng: nhấc tay, lần sau bắt đầu lại ở giữa
                i.touch.move(ox, oy, false); i.on = false
            } else i.touch.move(i.lx, i.ly, true)
            handler.removeCallbacks(i.idle)
            handler.postDelayed(i.idle, 250)
        }
    }
}

/** Lớp phủ trong suốt, không chặn chạm, dùng để bắt chuột (pointer capture). */
class Overlay(ctx: Context, private val m: Mapper) : View(ctx) {
    private val pressed = HashSet<Int>()
    init { isFocusable = true; isFocusableInTouchMode = true }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !m.paused) requestPointerCapture()
    }

    private fun btn(b: Int) = when (b) {
        MotionEvent.BUTTON_PRIMARY -> "Mouse0"
        MotionEvent.BUTTON_TERTIARY -> "Mouse1"
        MotionEvent.BUTTON_SECONDARY -> "Mouse2"
        else -> "Mouse$b"
    }

    override fun onCapturedPointerEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_HOVER_MOVE -> m.onMouseMove(e.x, e.y)
            MotionEvent.ACTION_BUTTON_PRESS -> if (pressed.add(e.actionButton)) m.onKey(btn(e.actionButton), true, false)
            MotionEvent.ACTION_BUTTON_RELEASE -> if (pressed.remove(e.actionButton)) m.onKey(btn(e.actionButton), false, false)
        }
        return true
    }
}

class MapService : AccessibilityService() {
    private lateinit var mapper: Mapper
    private lateinit var wm: WindowManager
    private var overlay: Overlay? = null
    private var manualPause = false
    private var inApp = false
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { applyPause(); h.postDelayed(this, 30000) }
    }
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        mapper.reload()
        applyPause()
    }

    /** Tạm dừng khi: bấm F1, chưa kích hoạt/hết hạn, hoặc đang ở trong chính app Map Phím. */
    private fun applyPause() {
        val p = manualPause || inApp || !mapper.loggedIn
        if (p != mapper.paused) setPaused(p)
    }

    override fun onServiceConnected() {
        mapper = Mapper(this)
        mapper.reload()
        getSharedPreferences("mapphim", Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefListener)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT)
        overlay = Overlay(this, mapper).also { wm.addView(it, lp) }
        applyPause()
        h.postDelayed(tick, 30000)
    }

    private fun setPaused(p: Boolean) {
        mapper.paused = p
        mapper.releaseAll()
        val v = overlay ?: return
        val lp = v.layoutParams as WindowManager.LayoutParams
        lp.flags = if (p) lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        else lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        wm.updateViewLayout(v, lp)
        if (p) v.releasePointerCapture() else v.post { v.requestFocus(); v.requestPointerCapture() }
    }

    override fun onKeyEvent(e: KeyEvent): Boolean {
        if (!mapper.loggedIn) return false
        val name = keyName(e.keyCode)
        val down = e.action == KeyEvent.ACTION_DOWN
        if (name == "F1") {
            if (down && e.repeatCount == 0) {
                manualPause = !manualPause
                applyPause()
                Toast.makeText(this, if (manualPause) "Map phím: TẠM DỪNG" else "Map phím: ĐANG BẬT", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        if (mapper.paused) return false
        return mapper.onKey(name, down, e.repeatCount > 0)
    }

    private fun keyName(c: Int): String {
        val r = KeyEvent.keyCodeToString(c).removePrefix("KEYCODE_")
        return when (r) {
            "CTRL_LEFT", "CTRL_RIGHT" -> "Ctrl"
            "SHIFT_LEFT", "SHIFT_RIGHT" -> "Shift"
            "ALT_LEFT", "ALT_RIGHT" -> "Alt"
            "DPAD_UP" -> "↔Up"; "DPAD_DOWN" -> "↔Down"
            "DPAD_LEFT" -> "↔Left"; "DPAD_RIGHT" -> "↔Right"
            "CAPS_LOCK" -> "CapsLock"
            else -> if (r.length == 1) r else r.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pk = e.packageName?.toString() ?: return
            if (pk == "com.android.systemui") return
            inApp = pk == packageName
            applyPause()
        }
    }
    override fun onInterrupt() { mapper.releaseAll() }

    override fun onDestroy() {
        h.removeCallbacks(tick)
        mapper.releaseAll()
        overlay?.let { wm.removeView(it) }
        super.onDestroy()
    }
}
