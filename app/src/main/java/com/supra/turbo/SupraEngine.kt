package com.supra.turbo

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import rikka.shizuku.Shizuku
import java.util.Locale
import java.util.concurrent.Executors

/** 4 mức hạ độ phân giải (tỉ lệ render của game). */
enum class ResLevel(val label: String, val pct: Int) {
    CAN_BANG("Cân bằng", 85), MUOT("Mượt", 70), SIEU_MUOT("Siêu mượt", 60), CUC_NHE("Cực nhẹ", 50)
}

/** Tối ưu hình ảnh: tăng tương phản + độ rực (không phải làm nét thật). */
enum class Sharp(val label: String, val contrast: Float, val sat: Float) {
    NHE("Nhẹ", 1.05f, 1.10f), VUA("Vừa", 1.10f, 1.20f), MANH("Mạnh", 1.15f, 1.30f)
}

object SupraEngine {

    val FREE_FIRE = listOf("com.dts.freefireth", "com.dts.freefiremax")
    val FPS_OPTIONS = listOf(45, 60, 90)

    data class Out(val code: Int, val out: String) { val ok get() = code == 0 }

    /** Mọi lệnh chạy tuần tự trên 1 luồng, không bao giờ chồng nhau. */
    private val exec = Executors.newSingleThreadExecutor()
    fun async(block: () -> Unit) { exec.execute { try { block() } catch (_: Throwable) {} } }

    fun sp(c: Context) = c.getSharedPreferences("supra", Context.MODE_PRIVATE)

    // ───────── Quyền ─────────
    fun hasShizuku(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) { false }

    fun usageGranted(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        else @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun notifGranted(c: Context) = Build.VERSION.SDK_INT < 33 ||
        c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun allGranted(c: Context) = hasShizuku() && usageGranted(c) && notifGranted(c)

    // ───────── Shell ─────────
    fun run(cmd: String): Out = try {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
        m.isAccessible = true
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        Out(p.waitFor(), (out + err).trim())
    } catch (t: Throwable) { Out(-1, t.message ?: "Shizuku error") }

    private fun bad(o: Out) = !o.ok || o.out.contains("unknown command", true) ||
        o.out.contains("can't find service", true) || o.out.contains("exception", true) ||
        o.out.contains("error", true)

    private fun f(x: Float) = String.format(Locale.US, "%.3f", x)

    fun installed(c: Context): List<String> = FREE_FIRE.filter {
        try { c.packageManager.getPackageInfo(it, 0); true } catch (_: Exception) { false }
    }

    // ───────── 1) Hạ độ phân giải + khóa FPS RIÊNG Free Fire (Game Mode, Android 13+) ─────────
    // Không đổi kích thước màn hình hệ thống → cảm ứng/độ nhạy không bị lệch. Có hiệu lực ở lần mở game kế tiếp.
    fun applyGame(c: Context): Boolean {
        val s = sp(c); val pk = installed(c)
        if (pk.isEmpty()) return false
        val on = s.getBoolean("enabled", false)
        val pct = ResLevel.values()[s.getInt("res", 1)].pct
        val fps = if (s.getBoolean("fpsLock", true)) " --fps ${s.getInt("fps", 60)}" else ""
        val cmd = pk.joinToString("; ") {
            if (!on) "cmd game reset $it; cmd game mode standard $it"
            else "cmd game set --mode 2 --downscale ${pct / 100f}$fps $it; cmd game mode performance $it"
        }
        val ok = !bad(run(cmd))
        s.edit().putInt("support", if (ok) 1 else 0).apply()
        return ok
    }

    // ───────── 2) Phiên chơi: dọn nền, tắt hiệu ứng, tắt overlay debug, tăng nét ─────────
    fun sessionStart(c: Context) {
        val s = sp(c)
        if (s.getBoolean("sessionActive", false)) return
        val prev = run("echo \"\$(settings get global window_animation_scale)|" +
            "\$(settings get global transition_animation_scale)|\$(settings get global animator_duration_scale)\"").out
        s.edit().putString("prevAnim", prev).putBoolean("sessionActive", true).apply()
        if (s.getBoolean("session", true)) {
            run("am kill-all; settings put global window_animation_scale 0; " +
                "settings put global transition_animation_scale 0; settings put global animator_duration_scale 0; " +
                "settings put system show_touches 0; settings put system pointer_location 0")
        }
        if (s.getBoolean("sharp", false)) {
            val lv = Sharp.values()[s.getInt("sharpLv", 1)]
            val k = lv.contrast; val t = 0.5f * (1 - k)
            val r = run("service call SurfaceFlinger 1015 i32 1 f ${f(k)} f 0 f 0 f 0 f 0 f ${f(k)} f 0 f 0 " +
                "f 0 f 0 f ${f(k)} f 0 f ${f(t)} f ${f(t)} f ${f(t)} f 1; " +
                "service call SurfaceFlinger 1022 f ${f(lv.sat)}")
            val ok = !bad(r)
            s.edit().putInt("sharpOk", if (ok) 1 else 0).putBoolean("sharpApplied", ok).apply()
        }
    }

    fun sessionEnd(c: Context) {
        val s = sp(c)
        val a = (s.getString("prevAnim", "") ?: "").split("|").map { if (it.isBlank() || it == "null") "1.0" else it }
        if (a.size == 3 && s.getBoolean("session", true))
            run("settings put global window_animation_scale ${a[0]}; settings put global transition_animation_scale ${a[1]}; " +
                "settings put global animator_duration_scale ${a[2]}")
        if (s.getBoolean("sharpApplied", false)) resetSharp()
        s.edit().putBoolean("sessionActive", false).putBoolean("sharpApplied", false).apply()
    }

    private fun resetSharp() { run("service call SurfaceFlinger 1015 i32 0; service call SurfaceFlinger 1022 f 1.0") }

    fun restoreAll(c: Context) {
        val s = sp(c)
        installed(c).forEach { run("cmd game reset $it; cmd game mode standard $it") }
        s.edit().putBoolean("sessionActive", true).apply()   // ép sessionEnd chạy
        sessionEnd(c); resetSharp()
        s.edit().putBoolean("enabled", false).remove("support").apply()
    }
}
