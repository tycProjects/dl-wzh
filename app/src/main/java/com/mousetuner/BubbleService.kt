package com.mousetuner

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Menu Game Space: vuốt từ mép trái màn hình khi đang trong game để mở ngăn kéo. */
class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private var edge: View? = null
    private var drawer: FrameLayout? = null
    private var holder: LinearLayout? = null
    private lateinit var fps: FpsMonitor
    private val ses = Executors.newSingleThreadScheduledExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var active = false
    @Volatile private var edgeOn = false
    private var curPkg: String? = null
    private var tickN = 0
    private var tab = 0
    private val UI_SCALE = 0.65f   // thu nhỏ menu trong game còn 65% (nhỏ lại 35%)

    private fun lp(w: Int, h: Int) = WindowManager.LayoutParams(w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("tuner", "Game Space", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "tuner").setContentTitle("Game Space đang chạy · vuốt mép trái trong game để mở menu")
            .setSmallIcon(android.R.drawable.ic_menu_manage).build())
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Theme.load(this); fps = FpsMonitor(this); FreeFire.installed(this)
        ses.scheduleWithFixedDelay({ try { loop() } catch (_: Throwable) {} }, 0, 700, TimeUnit.MILLISECONDS)
    }

    private fun loop() {
        if (!ShizukuShell.ready()) return
        if (tickN++ % 3 == 0) {
            curPkg = FreeFire.foregroundPkg()
            val inGame = curPkg != null
            if (inGame) Cfg.setF(this, "playsec", Cfg.f(this, "playsec", 0f) + 2.1f)
            if (inGame != edgeOn) { edgeOn = inGame; main.post { if (inGame) showEdge() else { hideEdge(); closeDrawer() } } }
            if (Cfg.auto(this)) {
                if (inGame && !active) { FreeFire.enter(this); active = true }
                else if (!inGame && active) { FreeFire.leave(this); active = false }
            }
        }
        val p = curPkg
        if (p != null) fps.tick(p) else fps.hide()
    }

    // ---------------- Mép trái: vuốt sang phải để mở menu ----------------
    private fun showEdge() {
        if (edge != null || !android.provider.Settings.canDrawOverlays(this)) return
        val f = FrameLayout(this)
        f.addView(View(this).apply { background = rounded(0xCCFF2A3D.toInt(), dp(2).toFloat()) }, FrameLayout.LayoutParams(dp(4), dp(64), Gravity.CENTER_VERTICAL or Gravity.START))
        var sx = 0f; var opened = false
        f.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; opened = false }
                MotionEvent.ACTION_MOVE -> if (!opened && e.rawX - sx > dp(36)) { opened = true; openDrawer() }
            }
            true
        }
        wm.addView(f, lp(dp(14), WindowManager.LayoutParams.MATCH_PARENT)); edge = f
    }
    private fun hideEdge() { edge?.let { try { wm.removeView(it) } catch (_: Throwable) {} }; edge = null }

    private fun openDrawer() {
        if (drawer != null) return
        val root = FrameLayout(this)
        root.addView(View(this).apply { setBackgroundColor(0x99000000.toInt()); setOnClickListener { closeDrawer() } }, FrameLayout.LayoutParams(-1, -1))
        val h = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        h.pivotX = 0f; h.pivotY = 0f; h.scaleX = UI_SCALE; h.scaleY = UI_SCALE
        root.addView(h, FrameLayout.LayoutParams(drawerW(), (resources.displayMetrics.heightPixels / UI_SCALE).toInt(), Gravity.START or Gravity.TOP))
        h.addView(buildPanel(), lpw(-1, -1))
        wm.addView(root, lp(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)); drawer = root; holder = h
        h.translationX = -drawerW() * UI_SCALE; h.animate().translationX(0f).setDuration(200).start()
    }
    private fun closeDrawer() {
        val d = drawer ?: return; val h = holder; drawer = null; holder = null
        h?.animate()?.translationX(-drawerW() * UI_SCALE)?.setDuration(160)?.withEndAction { try { wm.removeView(d) } catch (_: Throwable) {} }?.start() ?: try { wm.removeView(d) } catch (_: Throwable) {}
    }
    private fun refreshPanel() { holder?.let { it.removeAllViews(); it.addView(buildPanel(), lpw(-1, -1)) } }

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
        l.addView(seg(listOf("PIN", "CÂN BẰNG", "MONSTER"), Cfg.mode(this)) { Cfg.setI(this, "mode", it); Engine.applyAsync(this) })
        l.addView(trow("Monitoring FPS", Cfg.fps(this)) { Cfg.setB(this, "fps", it) })
                l.addView(body("BUFF NHẠY").apply { setTextColor(Theme.acc); textSize = 12f; typeface = Typeface.DEFAULT_BOLD })
        l.addView(seg(listOf("Tắt", "Nhẹ", "Mạnh", "MAX"), Cfg.i(this, "buff", 0)) { Cfg.setI(this, "buff", it); Engine.applyAsync(this) })
        l.addView(trow("Vuốt nhạy", Cfg.b(this, "touch")) { tg("touch", it) })
        l.addView(trow("Không làm phiền (DND)", Cfg.b(this, "dnd")) { tg("dnd", it) })
                l.addView(trow("Tắt làm mờ cửa sổ", Cfg.b(this, "noblur")) { tg("noblur", it) })
        l.addView(trow("Wi-Fi độ trễ thấp", Cfg.b(this, "wifi")) { tg("wifi", it) })
        l.addView(trow("Tắt tự động độ sáng", Cfg.b(this, "noauto")) { tg("noauto", it) })
        return l
    }

    private fun tabTouch(): View {
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        l.addView(trow("Vuốt nhạy", Cfg.b(this, "touch")) { tg("touch", it) })
        l.addView(slider("Nhấn giữ: ", 35, Cfg.lp(this) / 10 - 15, { "${(it + 15) * 10}ms" }) { Cfg.setI(this, "lp", (it + 15) * 10); tg("touch", Cfg.b(this, "touch")) })
        l.addView(trow("Chặn thông báo nổi", Cfg.b(this, "headsup")) { tg("headsup", it) })
        l.addView(trow("Hiện điểm chạm", Cfg.dbg(this)) { tg("dbg", it) })
        l.addView(trow("Chống vuốt nhầm cạnh", Cfg.b(this, "edge")) { tg("edge", it) })
        l.addView(trow("Khoá xoay màn hình", Cfg.b(this, "rotlock")) { tg("rotlock", it) })
        l.addView(trow("Giữ sáng khi sạc", Cfg.b(this, "stayon")) { tg("stayon", it) })
        l.addView(body("Chế độ cảm ứng: chơi bằng tay, không cần chuột/phím.").apply { textSize = 10f })
        val r = row()
        r.put(btn("Chụp màn hình", true) { bg { ShizukuShell.screenshot() } })
        r.put(btn("Dọn RAM") { bg { ShizukuShell.killBg() } })
        l.addView(r)
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

    private fun drawerW() = Math.min(dp(660), (resources.displayMetrics.widthPixels * 0.9f).toInt())
    private fun buildPanel(): View {
        val w = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        val rail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(6), dp(10), dp(6), dp(8)); background = glass(22, Theme.rail) }
        listOf("Hiệu suất" to "⚡", "Công cụ" to "🛠").forEachIndexed { i, t -> rail.addView(navItem(t.second, t.first, i == tab) { tab = i; refreshPanel() }, lpw(-1, -2).apply { topMargin = dp(4) }) }
        rail.addView(View(this), lpw(-1, 0, 1f)); rail.addView(navItem("✕", "Đóng", false) { closeDrawer() }, lpw(-1, -2))
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(10)); background = glass(26, Theme.panel) }
        val sv = ScrollView(this)
        sv.addView(if (tab == 0) PerfPanel(this, true).also { it.onChange = { refreshPanel() } }.view
                   else toolsView(3) { msg -> main.post { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } })
        panel.addView(sv, lpw(-1, 0, 1f))
        w.addView(RgbFrame(this, 22).apply { addView(rail, FrameLayout.LayoutParams(-1, -1)) }, lpw(dp(84), -1).apply { rightMargin = dp(8) }); w.addView(RgbFrame(this, 26).apply { addView(panel, FrameLayout.LayoutParams(-1, -1)) }, lpw(0, -1, 1f))
        return w
    }

    override fun onDestroy() {
        ses.shutdownNow(); fps.hide()
        if (active) Thread { FreeFire.leave(this) }.start()
        hideEdge(); drawer?.let { try { wm.removeView(it) } catch (_: Throwable) {} }
        super.onDestroy()
    }
}
