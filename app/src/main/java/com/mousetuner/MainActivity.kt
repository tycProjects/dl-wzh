package com.mousetuner

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import rikka.shizuku.Shizuku
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private var page = 0
    private val nav = listOf("TỔNG QUAN", "CHUỘT", "HIỆU NĂNG", "FPS · HUD", "CÔNG CỤ")
    private lateinit var info: TextView
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> runOnUiThread { buildUi() } }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try { Shizuku.addRequestPermissionResultListener(permListener) } catch (_: Throwable) {}
    }
    override fun onResume() { super.onResume(); buildUi() }
    override fun onDestroy() {
        try { Shizuku.removeRequestPermissionResultListener(permListener) } catch (_: Throwable) {}
        super.onDestroy()
    }
    private fun toast(s: String) = runOnUiThread { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }
    private fun toggle(key: String, on: Boolean) { Cfg.setB(this, key, on); Engine.applyAsync(this) }
    private fun fc(t: String, d: String, key: String, extra: View? = null) =
        feature(t, d, NeonSwitch(this, Cfg.b(this, key, key == "auto")) { toggle(key, it) }, extra)
    private fun prow(t: String, d: String, key: String): View {
        val r = row().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply { text = t; setTextColor(Color.WHITE); textSize = 14f })
        col.addView(TextView(this).apply { text = d; setTextColor(0xFF9A9AA0.toInt()); textSize = 11f })
        r.addView(col, lpw(0, -2, 1f)); r.addView(NeonSwitch(this, Cfg.b(this, key)) { toggle(key, it) }); return r
    }
    private fun showReport() {
        toast("Đang thử từng tính năng trên máy này…")
        thread { val r = SelfCheck.format(SelfCheck.run(this)); runOnUiThread {
            AlertDialog.Builder(this).setTitle("Độ sẵn sàng của máy").setMessage(r).setPositiveButton("OK", null).show() } }
    }

    // ---------------- Khung: thanh bên + nội dung ----------------
    private fun navItem(t: String, sel: Boolean, click: () -> Unit) = TextView(this).apply {
        text = t; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.08f
        setPadding(dp(14), dp(12), dp(14), dp(12)); setTextColor(if (sel) RED else 0xFFB0B0B8.toInt())
        if (sel) background = GradientDrawable().apply { setColor(0x33FF2A3D); cornerRadius = dp(12).toFloat(); setStroke(dp(1), RED) }
        setOnClickListener { click() }
    }
    private fun sidebar(): LinearLayout {
        val s = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(16), dp(14), dp(12))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFF1E0007.toInt(), 0xFF07070A.toInt()))
        }
        s.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_logo_bubble); setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF12000A.toInt()); setStroke(dp(2), RED) }
        }, lpw(dp(72), dp(72)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        s.addView(TextView(this).apply { text = "LONG SUPRA"; setTextColor(RED); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.15f; gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) }, lpw(-1, -2))
        s.addView(TextView(this).apply { text = "MOUSE TUNER · FREE FIRE"; setTextColor(0xFF9A9AA0.toInt()); textSize = 9f; letterSpacing = 0.1f; gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(14)) }, lpw(-1, -2))
        nav.forEachIndexed { i, t -> s.addView(navItem(t, i == page) { page = i; buildUi() }, lpw(-1, -2).apply { topMargin = dp(4) }) }
        s.addView(View(this), lpw(-1, 0, 1f))
        s.addView(TextView(this).apply { text = "© Long Supra\nv2.0"; setTextColor(0xFF77777D.toInt()); textSize = 10f; gravity = Gravity.CENTER }, lpw(-1, -2))
        return s
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xFF07070A.toInt()) }
        root.addView(sidebar(), lpw(dp(170), -1))
        val cards = when (page) { 0 -> pageOverview(); 1 -> pageMouse(); 2 -> pagePerf(); 3 -> pageHud(); else -> pageTools() }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cards.forEachIndexed { i, v -> (if (i % 2 == 0) left else right).put(v) }
        val cols = row().apply { setPadding(dp(10), dp(10), dp(10), dp(10)) }
        cols.addView(left, lpw(0, -2, 1f)); cols.addView(right, lpw(0, -2, 1f))
        val sv = ScrollView(this); sv.addView(cols)
        root.addView(sv, lpw(0, -1, 1f))
        setContentView(root)
    }

    // ---------------- TỔNG QUAN ----------------
    private fun pageOverview(): List<View> {
        val ready = ShizukuShell.ready(); val ov = Settings.canDrawOverlays(this)
        val inst = FreeFire.installed(this)
        val hero = gcard(); hero.put(sectionTitle("TRẠNG THÁI"))
        val c1 = row(); c1.put(chip("Shizuku", ready)); c1.put(chip("Overlay", ov)); hero.put(c1)
        val c2 = row(); c2.put(chip(if (inst.isEmpty()) "Chưa cài Free Fire" else inst.joinToString { it.first }, !inst.isEmpty()))
        c2.put(chip("${Math.round(display?.refreshRate ?: 60f)}Hz hiện tại", null)); hero.put(c2)
        val c3 = row(); c3.put(chip("Android ${Build.VERSION.RELEASE}", null))
        c3.put(chip(if (Build.VERSION.SDK_INT >= 35) "Tắt gia tốc: có" else "Tắt gia tốc: Raw pad", Build.VERSION.SDK_INT >= 35)); hero.put(c3)
        val go = row()
        go.put(gbtn("GAME ON", true) { Engine.profile(this, true); toast("Đã bật bộ tính năng chơi game"); buildUi() })
        go.put(gbtn("TẮT HẾT", false) { Engine.profile(this, false); toast("Đã tắt hết"); buildUi() })
        hero.put(go)
        hero.put(gbtn("KIỂM TRA ĐỘ SẴN SÀNG (thử từng tính năng)", true) { showReport() })

        val setup = gcard(); setup.put(sectionTitle("KÍCH HOẠT"))
        val r1 = row()
        r1.put(gbtn("1 · Shizuku", !ready) {
            try { if (Shizuku.pingBinder()) Shizuku.requestPermission(1) else toast("Hãy mở & khởi động Shizuku trước") }
            catch (e: Throwable) { toast("Chưa cài/chạy Shizuku") }
        })
        r1.put(gbtn("2 · Overlay", !ov) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) })
        setup.put(r1)
        val r2 = row()
        r2.put(gbtn("3 · Bật bong bóng", true) {
            if (!Settings.canDrawOverlays(this)) { toast("Cần cấp quyền overlay"); return@gbtn }
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
            startForegroundService(Intent(this, BubbleService::class.java))
        })
        r2.put(gbtn("Tắt bóng", false) { stopService(Intent(this, BubbleService::class.java)) })
        setup.put(r2)

        val ff = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val fr = row(); inst.forEach { (n, pk) -> fr.put(gbtn("Mở $n", true) { FreeFire.launch(this, pk) }) }
        if (inst.isNotEmpty()) ff.addView(fr)
        return listOf(hero, setup, fc("Free Fire · Tự động", "Vào game tự áp các tính năng đang bật, thoát game tự trả về mặc định.", "auto", ff))
    }

    // ---------------- CHUỘT ----------------
    private fun pageMouse(): List<View> {
        val linInfo = body("").apply { textSize = 11f; setTextColor(0xFFFFC107.toInt()) }
        fun refreshLin() { val sp = Engine.speedFor(this); linInfo.text = "Hệ thống: gain ≈ ×%.2f → pointer_speed %+d\n%s".format(Engine.gainOf(sp), sp, Engine.accelSupport()) }
        val lb = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lb.addView(slider("Mouse Sensitivity: ", 99, Cfg.progOf(Cfg.sens(this)), { "×%.1f".format(Cfg.sensOf(it)) }) { Cfg.setF(this, "sens", Cfg.sensOf(it)); refreshLin(); Engine.applyAsync(this) })
        lb.addView(slider("X-Axis (Vùng Linear): ", 99, Cfg.progOf(Cfg.sx(this)), { "×%.1f".format(Cfg.sensOf(it)) }) { Cfg.setF(this, "sx", Cfg.sensOf(it)) })
        lb.addView(slider("Y-Axis (Vùng Linear): ", 99, Cfg.progOf(Cfg.sy(this)), { "×%.1f".format(Cfg.sensOf(it)) }) { Cfg.setF(this, "sy", Cfg.sensOf(it)) })
        val na = row().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, 0) }
        na.addView(body("Disable Mouse Acceleration"), lpw(0, -2, 1f))
        na.addView(NeonSwitch(this, Cfg.noAccel(this)) { Cfg.setB(this, "noaccel", it); refreshLin(); Engine.applyAsync(this) })
        lb.addView(na); lb.addView(linInfo)
        lb.addView(body("Phạm vi: toàn hệ thống = gain + tắt gia tốc (Android 15+). X/Y riêng và Raw 1:1 chạy trong Vùng Linear. Android giới hạn pointer_speed ở ±7.").apply { textSize = 11f; setPadding(0, dp(6), 0, 0) })
        val lr = row()
        lr.put(gbtn("Vùng Linear (Raw)", true) { startActivity(Intent(this, LinearPadActivity::class.java)) })
        lr.put(gbtn("Tự kiểm tra", false) { AlertDialog.Builder(this).setTitle("Kiểm tra Linear Mouse").setMessage(LinearMouse.selfTest()).setPositiveButton("OK", null).show() })
        lb.addView(lr); refreshLin()
        val lin = feature("Linear Mouse Movement", "Chuyển động tuyến tính: out = delta × sensitivity. Không gia tốc, giữ tỉ lệ X/Y, không mất sự kiện nhỏ.",
            NeonSwitch(this, Cfg.lin(this)) { toggle("lin", it); refreshLin() }, lb)

        val cpiInfo = body("").apply { textSize = 11f; setTextColor(0xFFFFC107.toInt()) }
        fun refreshCpi() { cpiInfo.text = "CPI hiện tại: ${Cfg.dpi(this)} (mốc 800) → gain ×%.2f".format(800f / Cfg.dpi(this)) }
        val cb = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val dr = row(); listOf(400, 800, 1200, 1600, 3200).forEach { d -> dr.put(btn("$d") { Cfg.setI(this, "dpi", d); refreshCpi(); refreshLin(); Engine.applyAsync(this) }) }
        cb.addView(dr); cb.addView(cpiInfo); refreshCpi()
        val cpi = feature("Control CPI Mouse", "Chuẩn hoá tốc độ theo CPI/DPI của chuột (chuột 1600/3200 tự giảm gain) và tắt gia tốc. Chọn CPI chuột của bạn:",
            NeonSwitch(this, Cfg.cad(this)) { toggle("cad", it); refreshLin() }, cb)

        val dbg = fc("Debugging Sensitivity", "Bật lớp hiển thị toạ độ, vận tốc con trỏ và điểm chạm của hệ thống ngay trong game để quan sát độ nhạy thật.", "dbg")
        val tl = gcard(); tl.put(sectionTitle("ĐO KIỂM"))
        tl.put(body("Đo Hz chuột/cảm ứng thật, độ giật, độ lệch khi kéo thẳng, và kiểm tra gia tốc bằng thước (kéo cùng 1 quãng thật lúc chậm/nhanh).").apply { textSize = 11f })
        tl.put(gbtn("Đo Hz + thước + kéo thẳng", true) { startActivity(Intent(this, MouseTestActivity::class.java)) })
        return listOf(lin, cpi, dbg, tl)
    }

    // ---------------- HIỆU NĂNG ----------------
    private fun pagePerf(): List<View> {
        val xb = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        xb.addView(body("Chọn Hz thủ công").apply { textSize = 12f })
        val hz = row()
        ShizukuShell.supportedHz(this).forEach { h -> hz.put(btn("${h}Hz") { thread { ShizukuShell.setHighRefresh(h) }; toast("Đã ép ${h}Hz") }) }
        hz.put(btn("Auto") { thread { ShizukuShell.setHighRefresh(null) }; toast("Về tự động") })
        xb.addView(hz)
        xb.addView(slider("Độ phân giải game (Game Mode): ", 10, Math.round((Cfg.ds(this) - 0.5f) / 0.05f), { "${50 + it * 5}%" }) {
            Cfg.setF(this, "ds", 0.5f + it * 0.05f); Engine.applyAsync(this) })
        xb.addView(body("Hạ độ phân giải = tăng FPS (hiệu quả tuỳ game có hỗ trợ Game Mode). 100% = tắt.").apply { textSize = 10f })
        val xfr = fc("X Animated Frame Rate +", "Ép tần số quét cao nhất, giảm animation 0.5×, Game Mode Hiệu năng + FPS tối đa cho Free Fire.", "xfr", xb)

        val tg = gcard(); tg.put(sectionTitle("TIỆN ÍCH (tự áp khi vào game)"))
        tg.put(prow("Không làm phiền (DND)", "Chặn thông báo/cuộc gọi nổi giữa trận", "dnd"))
        tg.put(prow("Vuốt nhạy", "Nhấn giữ 250ms, nhấn đôi 200ms → nhận vuốt nhanh hơn", "touch"))
        tg.put(prow("Hiệu năng pin", "Tắt tiết kiệm pin + chế độ hiệu năng cố định (tuỳ máy)", "nobat"))
        tg.put(prow("Tắt làm mờ cửa sổ", "Giảm tải GPU (Android 12+)", "noblur"))
        tg.put(prow("Wi-Fi độ trễ thấp", "Giảm ping khi chơi qua Wi-Fi (Android 13+)", "wifi"))
        tg.put(prow("Tắt tự động độ sáng", "Màn hình không tự đổi sáng giữa trận", "noauto"))

        val ac = gcard(); ac.put(sectionTitle("HÀNH ĐỘNG NHANH"))
        val a1 = row()
        a1.put(gbtn("Dọn RAM", false) { thread { ShizukuShell.killBg() }; toast("Đã dọn tiến trình nền") })
        a1.put(gbtn("Dọn cache", false) { thread { ShizukuShell.trimCaches() }; toast("Đang dọn cache hệ thống") })
        ac.put(a1)
        FreeFire.installed(this).forEach { (n, pk) ->
            val r = row()
            r.put(gbtn("Giữ $n chạy nền", false) { thread { ShizukuShell.keepAlive(pk) }; toast("Đã whitelist $n") })
            r.put(gbtn("Biên dịch $n", true) { toast("Đang biên dịch, có thể mất vài phút…"); thread { val x = ShizukuShell.compile(pk); toast(if (x.first == 0) "Biên dịch $n xong" else "Lỗi: ${x.second.take(60)}") } })
            ac.put(r)
        }
        return listOf(xfr, tg, ac)
    }

    // ---------------- FPS · HUD ----------------
    private fun pageHud(): List<View> {
        val mon = fc("Monitoring FPS", "Thanh FPS thật ở góc trái dưới (FPS, 1% low, frametime, nhiệt độ, ping). Đo từ SurfaceFlinger. Cần bật bong bóng.", "fps")
        val st = gcard(); st.put(sectionTitle("KIỂU THANH FPS"))
        st.put(slider("Độ đậm: ", 70, Cfg.hudAlpha(this) - 30, { "${it + 30}%" }) { Cfg.setI(this, "hudA", it + 30) })
        st.put(prow("Chế độ gọn", "Chỉ hiện số FPS (như thanh nhỏ của Game Turbo)", "hudc"))
        st.put(body("Thanh dính sát góc trái dưới màn hình. Màu số FPS: xanh ≥90% Hz, vàng ≥60%, đỏ thấp hơn.").apply { textSize = 11f })
        return listOf(mon, st)
    }

    // ---------------- CÔNG CỤ ----------------
    private fun pageTools(): List<View> {
        val t = gcard(); t.put(sectionTitle("KIỂM TRA GIÁ TRỊ HỆ THỐNG"))
        info = body("(bấm để xem máy đã nhận cài đặt chưa)").apply { textSize = 11f }
        t.put(gbtn("Đọc giá trị hiện tại", false) { thread {
            val s = "pointer_speed = ${ShizukuShell.get("system", "pointer_speed")}\naccel = ${ShizukuShell.get("system", "mouse_pointer_acceleration_enabled")}\n" +
                "min_refresh = ${ShizukuShell.get("system", "min_refresh_rate")}\npeak_refresh = ${ShizukuShell.get("system", "peak_refresh_rate")}\n" +
                "anim = ${ShizukuShell.get("global", "animator_duration_scale")}\npointer_location = ${ShizukuShell.get("system", "pointer_location")}\n" +
                "lỗi lệnh gần nhất: ${ShizukuShell.lastErr.ifBlank { "không" }}"
            runOnUiThread { info.text = s } } })
        t.put(info)
        t.put(gbtn("KIỂM TRA ĐỘ SẴN SÀNG", true) { showReport() })
        val rs = gcard(); rs.put(sectionTitle("ĐẶT LẠI"))
        rs.put(gbtn("RESET TẤT CẢ VỀ MẶC ĐỊNH", true) {
            listOf("xfr", "cad", "dbg", "fps", "lin", "dnd", "touch", "nobat", "noblur", "wifi", "noauto").forEach { Cfg.setB(this, it, false) }
            thread { Engine.reset(this) }; toast("Đã reset"); buildUi()
        })
        val ab = gcard(); ab.put(sectionTitle("VỀ ỨNG DỤNG"))
        ab.put(body("Mouse Tuner v2.0 · dành riêng cho Free Fire\n© Long Supra — Mọi quyền được bảo lưu\n\nMọi tính năng chạy qua Shizuku bằng lệnh hệ thống thật. Nút 'Kiểm tra độ sẵn sàng' cho biết tính năng nào máy bạn hỗ trợ.").apply { textSize = 12f })
        return listOf(t, rs, ab)
    }
}
