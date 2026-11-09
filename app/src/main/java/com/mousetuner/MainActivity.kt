package com.mousetuner

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import android.view.ViewGroup
import rikka.shizuku.Shizuku
import kotlin.concurrent.thread

/** Game Space — bản cảm ứng thuần (chơi bằng tay), dành cho Free Fire / Free Fire MAX. */
class MainActivity : Activity() {
    private var page = 0
    private var skipPerm = false
    private val ui2 = android.os.Handler(android.os.Looper.getMainLooper())
    private val nav = listOf("Bảng hiệu suất" to "⚡", "Đồ họa" to "🎨", "Công cụ trò chơi" to "🛠", "Game Center" to "👑")
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> runOnUiThread { buildUi() } }

    override fun onCreate(b: Bundle?) { super.onCreate(b); Theme.load(this); try { Shizuku.addRequestPermissionResultListener(permListener) } catch (_: Throwable) {} }
    override fun onResume() {
        super.onResume(); buildUi()
        if (ShizukuShell.ready()) Engine.applyAsync(this)   // chỉ áp những gì đang BẬT; tắt thì trả về mặc định
        if (Settings.canDrawOverlays(this)) try { startForegroundService(Intent(this, BubbleService::class.java)) } catch (_: Throwable) {}
    }
    override fun onDestroy() { try { Shizuku.removeRequestPermissionResultListener(permListener) } catch (_: Throwable) {}; super.onDestroy() }

    private fun toast(s: String) = runOnUiThread { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }
    private fun toggle(key: String, on: Boolean) { Cfg.setB(this, key, on); Engine.applyAsync(this) }
    private fun fc(t: String, d: String, key: String, extra: View? = null) = feature(t, d, NeonSwitch(this, Cfg.b(this, key, key == "auto")) { toggle(key, it) }, extra)
    private fun prow(t: String, d: String, key: String): View {
        val r = row().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply { text = t; setTextColor(Color.WHITE); textSize = 14f })
        r.addView(col, lpw(0, -2, 1f)); r.addView(NeonSwitch(this, Cfg.b(this, key)) { toggle(key, it) }); return r
    }
    private fun vbox() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun showReport() {
        toast("Đang thử từng tính năng trên máy này…")
        thread { val r = SelfCheck.format(SelfCheck.run(this)); runOnUiThread { AlertDialog.Builder(this).setTitle("Độ sẵn sàng của máy").setMessage(r).setPositiveButton("OK", null).show() } }
    }

    // ---------- Khung chính: thanh bên + bảng menu ----------
    private fun rail(): LinearLayout {
        val s = vbox().apply { setPadding(dp(8), dp(12), dp(8), dp(10)); background = glass(24, Theme.rail) }
        val nv = vbox()
        nv.addView(ImageView(this).apply { setImageResource(R.drawable.ic_logo_bubble) }, lpw(dp(44), dp(44)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(4) })
        nav.forEachIndexed { i, t -> nv.addView(navItem(t.second, t.first, i == page) { page = i; buildUi() }, lpw(-1, -2).apply { topMargin = dp(4) }) }
        nv.addView(View(this).apply { setBackgroundColor(0x33FFFFFF) }, lpw(dp(40), 1).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(10); bottomMargin = dp(8) })
        FreeFire.installed(this).forEach { (_, pk) ->
            val ic = try { packageManager.getApplicationIcon(pk) } catch (e: Throwable) { null }
            if (ic != null) nv.addView(ImageView(this).apply { setImageDrawable(ic); setOnClickListener { FreeFire.launch(this@MainActivity, pk) } }, lpw(dp(46), dp(46)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(6) })
        }
        nv.addView(TextView(this).apply { text = "＋"; gravity = Gravity.CENTER; textSize = 22f; setTextColor(Color.WHITE); background = glass(23, if (Theme.gold) 0x33F7D858 else 0x33FF2A3D); setOnClickListener { addGameDialog() } }, lpw(dp(46), dp(46)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8) })
        s.addView(ScrollView(this).apply { addView(nv) }, lpw(-1, 0, 1f)); return s
    }

    // ---------- Quyền cần thiết ----------
    private data class Perm(val name: String, val desc: String, val ok: Boolean, val act: () -> Unit)
    private fun perms(): List<Perm> = listOf(
        Perm("Hiện trên ứng dụng khác", "Để menu vuốt từ cạnh trái hiện trong game", Settings.canDrawOverlays(this)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
        Perm("Thay đổi cài đặt hệ thống", "Để chỉnh độ sáng, màn hình, hiệu năng", Settings.System.canWrite(this)) { startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))) },
        Perm("Tắt tiết kiệm pin cho app", "Để Game Space không bị hệ thống tắt khi chơi", (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
            try { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))) } catch (_: Throwable) { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } },
        Perm("Thông báo", "Để dịch vụ chạy nền ổn định", Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) { if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2) },
        Perm("Shizuku", "Để áp tính năng thật vào hệ thống (khởi động Shizuku trước)", ShizukuShell.ready()) {
            try { if (Shizuku.pingBinder()) Shizuku.requestPermission(1) else { toast("Hãy mở & khởi động Shizuku trước"); packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let { startActivity(it) } } } catch (e: Throwable) { toast("Chưa cài/chạy Shizuku") } }
    )
    private fun pagePerm(): List<View> {
        val ps = perms(); val c = gcard(); c.put(sectionTitle("CẤP QUYỀN ĐỂ BẮT ĐẦU"))
        c.put(body("Game Space cần các quyền dưới đây để mọi tính năng hoạt động. Bấm “Cấp” ở từng mục.").apply { textSize = 12f; setTextColor(0xFFC9A0A6.toInt()) })
        ps.forEach { p ->
            val r = row().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
            val col = vbox(); col.addView(TextView(this).apply { text = (if (p.ok) "✔ " else "○ ") + p.name; setTextColor(if (p.ok) 0xFF3DDC84.toInt() else Color.WHITE); textSize = 14f; typeface = Typeface.DEFAULT_BOLD })
            col.addView(TextView(this).apply { text = p.desc; setTextColor(0xFF9A9AA0.toInt()); textSize = 11f })
            r.addView(col, lpw(0, -2, 1f))
            r.addView(gbtn(if (p.ok) "Đã cấp" else "Cấp", !p.ok) { if (!p.ok) p.act() }, lpw(dp(90), -2))
            c.addView(r)
        }
        val go = row()
        go.put(gbtn("Cấp mục tiếp theo", true) { ps.firstOrNull { !it.ok }?.act() ?: run { page = 0; buildUi() } })
        go.put(gbtn(if (ps.all { it.ok }) "VÀO APP" else "Bỏ qua", false) { skipPerm = true; page = 0; buildUi() })
        c.put(go); return listOf(c)
    }

    private fun buildUi() {
        val needPerm = !skipPerm && perms().any { !it.ok }
        val pageNow = if (needPerm) -1 else page
        val frame = FrameLayout(this); frame.addView(WallView(this), FrameLayout.LayoutParams(-1, -1))
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(10), dp(8), dp(10), dp(8)) }
        if (!needPerm) root.addView(RgbFrame(this, 24).apply { addView(rail(), FrameLayout.LayoutParams(-1, -1)) }, lpw(dp(116), -1).apply { rightMargin = dp(10) })
        val content = vbox()
        when (pageNow) {
            -1 -> pagePerm().forEach { content.put(it) }
            0 -> { val pp = PerfPanel(this); pp.onChange = { buildUi() }; content.addView(pp.view)
                   content.addView(fc("Free Fire · Tự động", "Vào game tự áp cấu hình, thoát game tự trả về mặc định.", "auto"), lpw(-1, -2).apply { topMargin = dp(12) }) }
            else -> {
                when (pageNow) {
                    1 -> pageGfx().forEach { content.put(it) }
                    2 -> content.addView(toolsView(4) { msg -> toast(msg) })
                    else -> { val cards = pageCenter(); val l = vbox(); val r = vbox(); cards.forEachIndexed { i, v -> (if (i % 2 == 0) l else r).put(v) }
                        val cols = row(); cols.addView(l, lpw(0, -2, 1f)); cols.addView(r, lpw(0, -2, 1f)); content.addView(cols) }
                }
            }
        }
        val panel = vbox().apply { setPadding(dp(16), dp(10), dp(16), dp(12)); background = glass(28, Theme.panel) }
        panel.addView(ScrollView(this).apply { addView(content) }, lpw(-1, 0, 1f))
        root.addView(RgbFrame(this, 28).apply { addView(panel, FrameLayout.LayoutParams(-1, -1)) }, lpw(0, -1, 1f)); frame.addView(root, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
    }

    // ---------- ĐỒ HỌA (có ảnh minh họa) ----------
    private fun pageGfx(): List<View> {
        val ds = Cfg.ds(this)
        val q = gcard(); q.put(sectionTitle("RENDERING GRAPHICS"))
        q.put(thumbRow(listOf("Gốc", "Low Texture", "Low Pixel"), listOf(0, 1, 2), listOf(1f, 1f, 1f), if (ds >= 0.99f) 0 else if (ds >= 0.8f) 1 else 2) { Cfg.setF(this, "ds", floatArrayOf(1f, 0.85f, 0.65f)[it]); Engine.applyAsync(this) })
        q.put(slider("Độ phân giải: ", 10, Math.round((ds - 0.5f) / 0.05f).coerceIn(0, 10), { "${50 + it * 5}%" }) { Cfg.setF(this, "ds", 0.5f + it * 0.05f); Engine.applyAsync(this) })
        q.put(feature("ANGLE", "", NeonSwitch(this, Cfg.angle(this)) { Cfg.setB(this, "angle", it); Engine.applyAsync(this) }))
        val sats = floatArrayOf(1f, 1.35f, 1.7f)
        val col = gcard(); col.put(sectionTitle("MÀU GAME"))
        col.put(thumbRow(listOf("Gốc", "Sống động", "Siêu FPS"), listOf(0, 0, 0), sats.toList(), sats.indexOfFirst { it == Cfg.sat(this) }.coerceAtLeast(0)) { Cfg.setF(this, "sat", sats[it]); Engine.applyAsync(this) })
        col.put(slider("Rực màu: ", 20, Math.round((Cfg.sat(this) - 0.5f) / 0.1f).coerceIn(0, 20), { "%.1f".format(0.5f + it * 0.1f) }) { Cfg.setF(this, "sat", 0.5f + it * 0.1f); Engine.applyAsync(this) })
        return listOf(q, col)
    }

    // ---------- CẢM ỨNG (chơi bằng tay) ----------
    private fun pageTouch(): List<View> {
        val lb = vbox()
        lb.addView(slider("Độ trễ nhấn giữ: ", 35, Cfg.lp(this) / 10 - 15, { "${(it + 15) * 10}ms" }) { Cfg.setI(this, "lp", (it + 15) * 10); thread { ShizukuShell.setLongPress(Cfg.lp(this)) } })
        val sw = fc("Vuốt nhạy", "Nhận nhấn giữ & nhấn đôi nhanh hơn → vuốt, kéo tâm, bắn mượt hơn.", "touch", lb)
        val aim = gcard(); aim.put(sectionTitle("BUFF NHẠY"))
        aim.put(body("Tăng độ nhạy màn hình khi chơi: rút ngắn thời gian nhận nhấn giữ/nhấn đôi, bật cảm ứng cao và thu hẹp vùng vuốt cạnh. Kéo tâm, bắn và vuốt phản hồi nhanh hơn.").apply { textSize = 11f })
        aim.put(seg(listOf("Tắt", "Nhẹ", "Mạnh", "MAX"), Cfg.i(this, "buff", 0)) { Cfg.setI(this, "buff", it); Engine.applyAsync(this); ui2.postDelayed({ buildUi() }, 1200) })
        val stv = body("Đang đọc thông số…").apply { textSize = 11f }; aim.put(stv)
        if (ShizukuShell.ready()) thread { val s = ShizukuShell.touchStatus().joinToString("\n") { "• ${it.first}: ${it.second}" }; runOnUiThread { stv.text = s } } else stv.text = "Cần bật Shizuku để áp và xem thông số thật."
        aim.put(prow("Chống vuốt nhầm cạnh", "Thu hẹp vùng vuốt-quay-lại ở mép khi kéo tâm sát cạnh (tuỳ ROM)", "edge"))
        aim.put(prow("Khoá xoay màn hình", "Giữ màn hình ngang, không bị xoay giữa trận", "rotlock"))
        aim.put(prow("Độ nhạy chạm cao", "Tăng độ nhạy cảm ứng (Samsung/một số ROM)", "hitouch"))
        val g = gcard(); g.put(sectionTitle("CHƠI BẰNG TAY"))
        g.put(prow("Chặn thông báo nổi", "Không bị banner che màn hình giữa trận", "headsup"))
        g.put(prow("Không làm phiền (DND)", "Chặn cuộc gọi/thông báo", "dnd"))
        g.put(prow("Hiện điểm chạm", "Xem vị trí ngón tay để chỉnh nút", "dbg"))
        g.put(prow("Dọn RAM khi vào game", "Giải phóng bộ nhớ ngay khi mở game", "ramclean"))
        g.put(prow("Giữ sáng khi sạc", "Màn hình không tắt lúc vừa sạc vừa chơi", "stayon"))
        return listOf(sw, aim, g)
    }

    // ---------- CÔNG CỤ ----------
    private fun pageTools(): List<View> {
        val tg = gcard(); tg.put(sectionTitle("TIỆN ÍCH (tự áp khi vào game)"))
        tg.put(prow("Hiệu năng pin", "Tắt tiết kiệm pin, hiệu năng cố định", "nobat"))
        tg.put(prow("Wi-Fi độ trễ thấp", "Giảm ping qua Wi-Fi (Android 13+)", "wifi"))
        tg.put(prow("Tắt tự động độ sáng", "Màn hình không tự đổi sáng", "noauto"))
        tg.put(prow("Chặn dữ liệu nền", "Dành mạng cho game", "bgdata"))
        val ac = gcard(); ac.put(sectionTitle("HÀNH ĐỘNG NHANH"))
        val a1 = row(); a1.put(gbtn("Dọn RAM", false) { thread { ShizukuShell.killBg() }; toast("Đã dọn tiến trình nền") }); a1.put(gbtn("Dọn cache", false) { thread { ShizukuShell.trimCaches() }; toast("Đang dọn cache") }); ac.put(a1)
        ac.put(gbtn("Chụp màn hình (Pictures)", true) { thread { val r = ShizukuShell.screenshot(); toast(if (r.first == 0) "Đã lưu ảnh" else "Lỗi chụp") } })
        FreeFire.installed(this).forEach { (n, pk) ->
            val r = row(); r.put(gbtn("Giữ $n nền", false) { thread { ShizukuShell.keepAlive(pk) }; toast("Đã whitelist $n") })
            r.put(gbtn("Biên dịch $n", true) { toast("Đang biên dịch…"); thread { val x = ShizukuShell.compile(pk); toast(if (x.first == 0) "Xong" else "Lỗi: ${x.second.take(60)}") } }); ac.put(r)
        }
        val mon = fc("Monitoring FPS", "Thanh FPS thật (FPS, 1% low, frametime, nhiệt độ). Cần bật bong bóng.", "fps", vbox().apply {
            addView(slider("Độ đậm: ", 70, Cfg.hudAlpha(this@MainActivity) - 30, { "${it + 30}%" }) { Cfg.setI(this@MainActivity, "hudA", it + 30) }) })
        return listOf(tg, ac, mon)
    }

    // ---------- GAME CENTER ----------
    private fun addGameDialog() {
        val pm = packageManager; val have = FreeFire.installed(this).map { it.second }
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }.filter { it.second != packageName && it.second !in have }.distinctBy { it.second }.sortedBy { it.first.lowercase() }
        if (apps.isEmpty()) { toast("Không còn ứng dụng nào để thêm"); return }
        AlertDialog.Builder(this).setTitle("Thêm game vào Game Space").setItems(apps.map { it.first }.toTypedArray()) { _, w -> FreeFire.add(this, apps[w].second); toast("Đã thêm ${apps[w].first}"); buildUi() }.show()
    }
    private fun pageCenter(): List<View> {
        val gc = gcard(); gc.put(sectionTitle("GAME CỦA TÔI"))
        val inst = FreeFire.installed(this)
        if (inst.isEmpty()) gc.put(body("Chưa có game nào. Bấm “Thêm game”."))
        inst.forEach { (n, pk) ->
            val r = row().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(4), 0, dp(4)) }
            val ic = try { packageManager.getApplicationIcon(pk) } catch (e: Throwable) { null }
            if (ic != null) r.addView(ImageView(this).apply { setImageDrawable(ic) }, lpw(dp(40), dp(40)))
            r.addView(TextView(this).apply { text = n; setTextColor(Color.WHITE); textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setPadding(dp(10), 0, 0, 0) }, lpw(0, -2, 1f))
            r.addView(gbtn("▶ Chơi", true) { FreeFire.launch(this, pk) })
            if (FreeFire.isCustom(this, pk)) r.addView(gbtn("✕", false) { FreeFire.remove(this, pk); buildUi() }, lpw(dp(48), -2))
            gc.addView(r)
        }
        gc.put(gbtn("＋  THÊM GAME", true) { addGameDialog() })
        val st = gcard(); st.put(sectionTitle("TRẠNG THÁI"))
        val ready = ShizukuShell.ready(); val ov = Settings.canDrawOverlays(this)
        val c1 = row(); c1.put(chip("Shizuku", ready)); c1.put(chip("Menu vuốt trái", ov)); st.put(c1)
        val c2 = row(); c2.put(chip("Ghi cài đặt", Settings.System.canWrite(this))); c2.put(chip("${Math.round(display?.refreshRate ?: 60f)}Hz", null)); st.put(c2)
        st.put(gbtn("Cấp lại quyền", false) { skipPerm = false; buildUi() })
        st.put(gbtn("KIỂM TRA ĐỘ SẴN SÀNG", true) { showReport() })
        val th = gcard(); th.put(sectionTitle("MÀU GIAO DIỆN"))
        th.put(seg(listOf("Vàng (y chang mẫu)", "Đỏ"), if (Theme.gold) 0 else 1) { Cfg.setB(this, "golddash", it == 0); Theme.load(this); buildUi() })
        th.put(feature("Hiệu ứng RGB", "Viền khung, đồng hồ và tàn lửa nền đổi màu cầu vồng liên tục.", NeonSwitch(this, Theme.rgb) { Cfg.setB(this, "rgb", it); Theme.load(this); buildUi() }))
        val rs = gcard(); rs.put(sectionTitle("ĐẶT LẠI"))
        rs.put(gbtn("RESET TẤT CẢ VỀ MẶC ĐỊNH", false) {
            listOf("fps", "dbg", "dnd", "touch", "nobat", "noblur", "wifi", "noauto", "headsup", "bgdata", "angle", "edge", "rotlock", "stayon", "hitouch", "ramclean").forEach { Cfg.setB(this, it, false) }
            Cfg.setI(this, "mode", 1); Cfg.setI(this, "hzm", 0); Cfg.setI(this, "buff", 0); Cfg.setF(this, "ds", 1f); Cfg.setF(this, "sat", 1f)
            thread { Engine.reset(this) }; toast("Đã reset"); buildUi() })
        return listOf(gc, st, th, rs)
    }
}
