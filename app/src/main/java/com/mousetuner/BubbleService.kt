package com.mousetuner

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.*
import android.widget.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubble: ImageView
    private var panel: View? = null
    private lateinit var bubbleLp: WindowManager.LayoutParams
    private lateinit var fps: FpsMonitor
    private val ses = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var active = false
    private var curPkg: String? = null
    private var tickN = 0
    private var tab = 0

    private fun lp(w: Int, h: Int) = WindowManager.LayoutParams(w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("tuner", "Mouse Tuner", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "tuner").setContentTitle("Long Supra · Mouse Tuner đang chạy")
            .setSmallIcon(android.R.drawable.ic_menu_manage).build())
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        fps = FpsMonitor(this)
        buildBubble()
        // Một tác vụ định kỳ duy nhất; chỉ làm việc khi Shizuku sẵn sàng.
        ses.scheduleWithFixedDelay({ try { loop() } catch (_: Throwable) {} }, 0, 700, TimeUnit.MILLISECONDS)
    }

    private fun loop() {
        if (!ShizukuShell.ready()) return
        if (tickN++ % 3 == 0) {                          // kiểm tra game ở foreground ~2s/lần
            curPkg = FreeFire.foregroundPkg()
            if (Cfg.auto(this)) {
                if (curPkg != null && !active) { FreeFire.enter(this); active = true; mark() }
                else if (curPkg == null && active) { FreeFire.leave(this); active = false; mark() }
            }
        }
        val p = curPkg
        if (Cfg.fps(this) && p != null) fps.tick(p) else fps.hide()
    }
    private fun mark() = bubble.post { (bubble.background as GradientDrawable).setStroke(dp(2), if (active) 0xFF3DDC84.toInt() else RED) }

    // ---------------- Bong bóng: kéo thả, tự hít vào cạnh, mờ khi nhàn rỗi ----------------
    private fun screenW() = wm.currentWindowMetrics.bounds.width()
    private val dim = Runnable { bubble.animate().alpha(0.55f).setDuration(300).start() }

    private fun buildBubble() {
        bubble = ImageView(this).apply {
            setImageResource(R.drawable.ic_logo_bubble); setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xEE12000A.toInt()); setStroke(dp(2), RED) }
            elevation = dp(6).toFloat()
        }
        bubbleLp = lp(dp(52), dp(52)).apply { x = 0; y = dp(120) }
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { bubble.removeCallbacks(dim); bubble.animate().alpha(1f).setDuration(100).start()
                    sx = bubbleLp.x; sy = bubbleLp.y; tx = e.rawX; ty = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true
                    bubbleLp.x = sx + dx; bubbleLp.y = sy + dy; wm.updateViewLayout(bubble, bubbleLp)
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel() else snap()
                    bubble.postDelayed(dim, 3000)
                }
            }
            true
        }
        wm.addView(bubble, bubbleLp)
        bubble.postDelayed(dim, 3000)
    }

    private fun snap() {
        val target = if (bubbleLp.x + dp(26) < screenW() / 2) 0 else screenW() - dp(52)
        ValueAnimator.ofInt(bubbleLp.x, target).apply {
            duration = 180
            addUpdateListener { bubbleLp.x = it.animatedValue as Int; try { wm.updateViewLayout(bubble, bubbleLp) } catch (_: Throwable) {} }
            start()
        }
    }

    // ---------------- Menu tối ưu: 3 tab CHƠI · CHUỘT · HỆ THỐNG ----------------
    private fun trow(label: String, on: Boolean, cb: (Boolean) -> Unit): LinearLayout {
        val r = row().apply { gravity = Gravity.CENTER_VERTICAL }
        r.addView(body(label).apply { textSize = 13f }, lpw(0, -2, 1f)); r.addView(NeonSwitch(this, on, cb)); return r
    }
    private fun tg(key: String, on: Boolean) { Cfg.setB(this, key, on); Engine.applyAsync(this) }
    private fun bg(f: () -> Unit) { Thread { f() }.start() }

    private fun tabPlay(): View {
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val q = row()
        q.put(btn("GAME ON", true) { Engine.profile(this, true); refreshPanel() })
        q.put(btn("TẮT HẾT") { Engine.profile(this, false); refreshPanel() })
        l.addView(q)
        l.addView(trow("X Animated Frame Rate +", Cfg.xfr(this)) { tg("xfr", it) })
        l.addView(trow("Monitoring FPS", Cfg.fps(this)) { Cfg.setB(this, "fps", it) })
        l.addView(trow("Debugging Sensitivity", Cfg.dbg(this)) { tg("dbg", it) })
        l.addView(trow("Vuốt nhạy", Cfg.b(this, "touch")) { tg("touch", it) })
        l.addView(trow("Không làm phiền (DND)", Cfg.b(this, "dnd")) { tg("dnd", it) })
        l.addView(trow("Hiệu năng pin", Cfg.b(this, "nobat")) { tg("nobat", it) })
        l.addView(trow("Tắt làm mờ cửa sổ", Cfg.b(this, "noblur")) { tg("noblur", it) })
        l.addView(trow("Wi-Fi độ trễ thấp", Cfg.b(this, "wifi")) { tg("wifi", it) })
        l.addView(trow("Tắt tự động độ sáng", Cfg.b(this, "noauto")) { tg("noauto", it) })
        return l
    }

    private fun tabMouse(): View {
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val info = body("").apply { textSize = 11f; setTextColor(0xFFFFC107.toInt()) }
        fun refresh() { val sp = Engine.speedFor(this); info.text = "Gain ≈ ×%.2f → pointer_speed %+d | CPI %d".format(Engine.gainOf(sp), sp, Cfg.dpi(this)) }
        l.addView(trow("Linear Mouse Movement", Cfg.lin(this)) { tg("lin", it); refresh() })
        l.addView(slider("Sensitivity: ", 99, Cfg.progOf(Cfg.sens(this)), { "×%.1f".format(Cfg.sensOf(it)) }) {
            Cfg.setF(this, "sens", Cfg.sensOf(it)); refresh(); Engine.applyAsync(this) })
        l.addView(trow("Disable Mouse Acceleration", Cfg.noAccel(this)) { Cfg.setB(this, "noaccel", it); refresh(); Engine.applyAsync(this) })
        l.addView(trow("Control CPI Mouse", Cfg.cad(this)) { tg("cad", it); refresh() })
        val dr = row()
        listOf(400, 800, 1200, 1600, 3200).forEach { d -> dr.put(btn("$d") { Cfg.setI(this, "dpi", d); refresh(); Engine.applyAsync(this) }) }
        l.addView(dr); l.addView(info); refresh()
        l.addView(body(Engine.accelSupport()).apply { textSize = 10f })
        val tools = row()
        tools.put(btn("Vùng Linear (Raw)", true) { startActivity(Intent(this, LinearPadActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) })
        tools.put(btn("Đo Hz + thước") { startActivity(Intent(this, MouseTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) })
        l.addView(tools)
        return l
    }

    private fun tabSystem(): View {
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        l.addView(body("Hz màn hình").apply { textSize = 12f })
        val hz = row()
        ShizukuShell.supportedHz(this).forEach { h -> hz.put(btn("$h") { bg { ShizukuShell.setHighRefresh(h) } }) }
        hz.put(btn("Auto") { bg { ShizukuShell.setHighRefresh(null) } })
        l.addView(hz)
        val a = row()
        a.put(btn("Dọn RAM") { bg { ShizukuShell.killBg() } })
        a.put(btn("Dọn cache") { bg { ShizukuShell.trimCaches() } })
        l.addView(a)
        FreeFire.installed(this).forEach { (n, pk) ->
            val r = row()
            r.put(btn("Mở $n", true) { FreeFire.launch(this, pk) })
            r.put(btn("Giữ nền") { bg { ShizukuShell.keepAlive(pk) } })
            l.addView(r)
        }
        l.addView(btn("RESET TẤT CẢ", true) { Engine.profile(this, false); bg { Engine.reset(this) }; refreshPanel() }
            .also { it.layoutParams = lpw(-1, -2).apply { topMargin = dp(6) } })
        return l
    }

    private fun buildPanel(): View {
        val p = gcard().apply { background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xF21E0007.toInt(), 0xF207070A.toInt())).apply { cornerRadius = dp(20).toFloat(); setStroke(dp(1), RED) } }
        val hd = row().apply { gravity = Gravity.CENTER_VERTICAL }
        hd.addView(ImageView(this).apply { setImageResource(R.drawable.ic_logo_bubble) }, lpw(dp(32), dp(32)))
        val tc = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), 0, 0, 0) }
        tc.addView(TextView(this).apply { text = "LONG SUPRA"; setTextColor(RED); textSize = 14f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.12f })
        tc.addView(body((if (active) "● ĐANG CHƠI  " else "○ chờ game  ") + (if (ShizukuShell.ready()) "Shizuku ✔" else "Shizuku ✘")).apply { textSize = 10f })
        hd.addView(tc, lpw(0, -2, 1f))
        if (active && Cfg.fps(this)) hd.addView(TextView(this).apply { text = "${fps.lastFps.toInt()} FPS"; setTextColor(0xFF3DDC84.toInt()); textSize = 14f; typeface = Typeface.MONOSPACE })
        p.addView(hd)
        val tabs = row()
        listOf("CHƠI", "CHUỘT", "HỆ THỐNG").forEachIndexed { i, t -> tabs.put(btn(t, i == tab) { tab = i; refreshPanel() }) }
        p.addView(tabs)
        val sv = ScrollView(this); sv.addView(when (tab) { 0 -> tabPlay(); 1 -> tabMouse(); else -> tabSystem() })
        p.addView(sv, lpw(-1, dp(220)))
        p.addView(TextView(this).apply { text = "© Long Supra"; setTextColor(0xFF77777D.toInt()); textSize = 10f; gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) })
        return p
    }

    private fun panelLp(): WindowManager.LayoutParams {
        val w = dp(320); val onRight = bubbleLp.x + dp(26) > screenW() / 2
        return lp(w, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            x = if (onRight) Math.max(0, bubbleLp.x - w - dp(8)) else bubbleLp.x + dp(60); y = Math.max(0, bubbleLp.y - dp(20))
        }
    }
    private fun refreshPanel() { panel?.let { try { wm.removeView(it) } catch (_: Throwable) {} }; val v = buildPanel(); panel = v; wm.addView(v, panelLp()) }
    private fun togglePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Throwable) {}; panel = null; return }
        refreshPanel()
    }

    override fun onDestroy() {
        ses.shutdownNow(); fps.hide()
        if (active) Thread { FreeFire.leave(this) }.start()
        try { wm.removeView(bubble) } catch (_: Throwable) {}
        panel?.let { try { wm.removeView(it) } catch (_: Throwable) {} }
        super.onDestroy()
    }
}
