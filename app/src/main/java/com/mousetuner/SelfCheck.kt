package com.mousetuner

import android.content.Context
import android.os.Build
import android.provider.Settings

/** Tự thử từng tính năng trên chính máy này: ghi → đọc lại → khôi phục. Báo trung thực cái nào chạy được. */
object SelfCheck {
    data class R(val name: String, val ok: Boolean?, val note: String)

    private fun roundTrip(ns: String, key: String, test: String): Pair<Boolean, String> {
        val orig = ShizukuShell.get(ns, key)
        ShizukuShell.exec("settings put $ns $key $test")
        val back = ShizukuShell.get(ns, key)
        if (orig == "null" || orig.isBlank()) ShizukuShell.exec("settings delete $ns $key") else ShizukuShell.exec("settings put $ns $key $orig")
        return (back == test) to "ghi $test → đọc lại $back"
    }

    fun run(c: Context): List<R> {
        val out = ArrayList<R>()
        val id = ShizukuShell.exec("id")
        out += R("Quyền Shizuku (shell)", id.first == 0 && (id.second.contains("uid=2000") || id.second.contains("uid=0")), id.second.take(40))
        if (id.first != 0) { out += R("Dừng kiểm tra", false, "Shizuku chưa sẵn sàng"); return out }
        out += R("Quyền overlay (bong bóng/HUD)", Settings.canDrawOverlays(c), "")
        roundTrip("secure", "long_press_timeout", "250").let { out += R("Vuốt nhạy (long_press_timeout)", it.first, it.second) }
        roundTrip("global", "heads_up_notifications_enabled", "0").let { out += R("Chặn thông báo nổi", it.first, it.second) }
        roundTrip("system", "min_refresh_rate", "60.0").let { out += R("Ép Hz màn hình", it.first, it.second + " (ROM có thể bỏ qua)") }
        roundTrip("global", "animator_duration_scale", "0.5").let { out += R("Giảm animation", it.first, it.second) }
        if (Build.VERSION.SDK_INT >= 31) roundTrip("global", "disable_window_blurs", "1").let { out += R("Tắt làm mờ cửa sổ", it.first, it.second) }
        else out += R("Tắt làm mờ cửa sổ", false, "Cần Android 12+")
        val svc = ShizukuShell.exec("cmd -l").second.lines().map { it.trim() }.toSet()
        out += R("Game Mode / downscale", svc.contains("game"), if (svc.contains("game")) "dịch vụ 'game' có" else "ROM không có dịch vụ game")
        out += R("Không làm phiền (DND)", svc.contains("notification"), "")
        out += R("Wi-Fi độ trễ thấp", svc.contains("wifi") && Build.VERSION.SDK_INT >= 33, if (Build.VERSION.SDK_INT >= 33) "" else "Cần Android 13+")
        out += R("Chế độ hiệu năng cố định", svc.contains("power") && Build.VERSION.SDK_INT >= 31, if (Build.VERSION.SDK_INT >= 31) "tuỳ phần cứng" else "Cần Android 12+")
        val pg = ShizukuShell.exec("ping -c 1 -W 1 1.1.1.1").second
        out += R("Đo ping (NET)", pg.contains("time="), if (pg.contains("time=")) "" else "không có mạng hoặc chặn ICMP")
        val pk = FreeFire.foregroundPkg()
        if (pk == null) out += R("Đo FPS thật", null, "Mở Free Fire rồi chạy lại để kiểm tra")
        else {
            val ly = findGameLayer(pk)
            val lat = if (ly != null) ShizukuShell.exec("dumpsys SurfaceFlinger --latency '$ly'").second.lines().size else 0
            out += R("Đo FPS thật", ly != null && lat > 5, if (ly == null) "không thấy layer game" else "layer có $lat dòng dữ liệu")
        }
        return out
    }

    fun format(r: List<R>): String {
        val ok = r.count { it.ok == true }; val bad = r.count { it.ok == false }
        return r.joinToString("\n") { (if (it.ok == true) "✔ " else if (it.ok == false) "✘ " else "— ") + it.name + (if (it.note.isNotBlank()) "\n     " + it.note else "") } +
            "\n\nĐạt $ok · Không hỗ trợ/lỗi $bad"
    }
}
