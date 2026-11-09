package com.mousetuner

import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager
import rikka.shizuku.Shizuku

data class Sys(val cpu: Float, val ghz: Float, val gpu: Float, val mhz: Int)

object ShizukuShell {
    @Volatile var lastErr = ""

    fun ready(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) { false }

    /** Chạy lệnh shell với quyền Shizuku (shell/adb), trả về (exitCode, output). */
    fun exec(cmd: String): Pair<Int, String> = try {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
        m.isAccessible = true
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        val code = p.waitFor()
        if (code != 0) lastErr = "${cmd.take(48)} → ${out.trim().take(70)}"
        code to out.trim()
    } catch (e: Throwable) { -1 to (e.message ?: "error") }

    private var lastTot = 0L; private var lastIdle = 0L
    /** Một lệnh duy nhất lấy: %CPU (từ /proc/stat), GHz CPU, %GPU (Adreno), MHz GPU. Giá trị -1 nếu máy không cho đọc. */
    fun sysStats(): Sys {
        val o = exec("head -1 /proc/stat; echo ==B; cat /sys/class/kgsl/kgsl-3d0/gpubusy 2>/dev/null; echo ==C; (cat /sys/class/kgsl/kgsl-3d0/gpuclk || cat /sys/class/devfreq/*gpu*/cur_freq || cat /sys/class/devfreq/*mali*/cur_freq) 2>/dev/null | head -1; echo ==F; cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq 2>/dev/null").second
        val parts = o.split("==B", "==C", "==F").map { it.trim() }
        var cpu = -1f
        val n = parts.getOrNull(0)?.split(Regex("\\s+"))?.drop(1)?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        if (n.size >= 5) { val tot = n.take(8).sum(); val idle = n[3] + n[4]
            if (lastTot > 0 && tot > lastTot) cpu = 100f * ((tot - lastTot) - (idle - lastIdle)) / (tot - lastTot)
            lastTot = tot; lastIdle = idle }
        val gb = parts.getOrNull(1)?.split(Regex("\\s+"))?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        val gpu = if (gb.size >= 2 && gb[1] > 0) (100f * gb[0] / gb[1]).coerceIn(0f, 100f) else -1f
        val gc = parts.getOrNull(2)?.toLongOrNull() ?: -1L
        val mhz = if (gc > 1_000_000) (gc / 1_000_000).toInt() else gc.toInt()
        val ghz = parts.getOrNull(3)?.lines()?.mapNotNull { it.trim().toLongOrNull() }?.maxOrNull()?.let { it / 1_000_000f } ?: -1f
        return Sys(cpu.coerceIn(-1f, 100f), ghz, gpu, mhz)
    }
    fun get(ns: String, key: String) = exec("settings get $ns $key").second

    fun setAnim(scale: String) = exec(
        "settings put global window_animation_scale $scale; " +
        "settings put global transition_animation_scale $scale; " +
        "settings put global animator_duration_scale $scale")
    fun setHighRefresh(hz: Int?) = if (hz != null)
        exec("settings put system min_refresh_rate $hz.0; settings put system peak_refresh_rate $hz.0")
    else exec("settings delete system min_refresh_rate; settings delete system peak_refresh_rate")

    // ---- Cảm ứng / vuốt (qua Settings, không can thiệp sâu vào InputManager) ----
    fun setLongPress(ms: Int) = exec("settings put secure long_press_timeout $ms")
    fun setMultiPress(ms: Int) = exec("settings put secure multi_press_timeout $ms")
    fun setTouchOverlays(on: Boolean) = exec(
        "settings put system show_touches ${if (on) 1 else 0}; settings put system pointer_location ${if (on) 1 else 0}")
    /** Game Mode (Android 12+): hiệu quả tuỳ game có hỗ trợ hay không. */
    fun setGameMode(pkg: String, mode: String) = exec("cmd game mode $mode $pkg")
    /** Vuốt nhạy: giảm độ trễ nhận diện nhấn giữ, tắt lớp vẽ chạm, giảm animation. */
    fun touchBoost(on: Boolean) {
        if (on) { setLongPress(250); setMultiPress(200); setTouchOverlays(false); setAnim("0.5") }
        else { setLongPress(400); setMultiPress(300); setAnim("1.0") }
    }

    // ---- Hiệu năng / tiện ích ----
    fun setDnd(on: Boolean) = exec("cmd notification set_dnd ${if (on) "priority" else "off"}")
    fun setBatteryPerf(on: Boolean) = if (on)
        exec("settings put global low_power 0; cmd power set-fixed-performance-mode-enabled true")
    else exec("cmd power set-fixed-performance-mode-enabled false")
    fun setNoBlur(on: Boolean) = exec("settings put global disable_window_blurs ${if (on) 1 else 0}")
    fun setTouchTune(on: Boolean) { if (on) { setLongPress(250); setMultiPress(200) } else { setLongPress(400); setMultiPress(300) } }
    fun setWifiLowLatency(on: Boolean) = exec("cmd wifi force-low-latency-mode ${if (on) "enabled" else "disabled"}")
    fun setAutoBrightness(on: Boolean) = exec("settings put system screen_brightness_mode ${if (on) 1 else 0}")
    fun setDownscale(pkg: String, ds: Float, hz: Int) = exec("cmd game set --mode 2 --downscale %.2f --fps $hz $pkg".format(java.util.Locale.US, ds))
    fun resetGame(pkg: String) = exec("cmd game reset $pkg")
    fun killBg() = exec("am kill-all")
    fun trimCaches() = exec("pm trim-caches 100G")
    fun compile(pkg: String) = exec("cmd package compile -m speed -f $pkg")
    fun keepAlive(pkg: String) = exec("cmd deviceidle whitelist +$pkg; cmd appops set $pkg RUN_IN_BACKGROUND allow")
    fun gameFps(pkg: String, hz: Int) = exec("cmd game set --mode 2 --fps $hz $pkg")

    fun reset() {
        setHighRefresh(null); setAnim("1.0"); touchBoost(false); setSaturation(1f); setAngle(emptyList()); setBrightnessKeep(); setEdgeGuard(false); setStayOn(false)
    }

    // ---- Game Space: đồ họa / màu / công cụ ----
    fun setSaturation(v: Float) = exec("service call SurfaceFlinger 1022 f %.2f".format(java.util.Locale.US, v))
    fun setAngle(pkgs: List<String>) = if (pkgs.isNotEmpty())
        exec("settings put global angle_gl_driver_selection_pkgs ${pkgs.joinToString(",")}; settings put global angle_gl_driver_selection_values ${pkgs.joinToString(",") { "angle" }}")
    else exec("settings delete global angle_gl_driver_selection_pkgs; settings delete global angle_gl_driver_selection_values")
    fun setBrightness(v: Int) = exec("settings put system screen_brightness_mode 0; settings put system screen_brightness $v")
    fun setBrightnessKeep() = Unit
    /** Thu hẹp vùng vuốt-quay-lại ở mép màn hình (tránh thoát game khi kéo tâm sát cạnh). Tuỳ ROM. */
    fun setEdgeGuard(on: Boolean) = if (on) exec("settings put secure back_gesture_inset_scale_left 0.3; settings put secure back_gesture_inset_scale_right 0.3")
        else exec("settings delete secure back_gesture_inset_scale_left; settings delete secure back_gesture_inset_scale_right")
    fun setRotLock(on: Boolean) = if (on) exec("settings put system accelerometer_rotation 0; settings put system user_rotation 1") else exec("settings put system accelerometer_rotation 1")
    fun setStayOn(on: Boolean) = exec("svc power stayon ${if (on) "true" else "false"}")
    /** Đọc lại các thông số cảm ứng thật đang áp trên máy. */
    fun touchStatus(): List<Pair<String, String>> = listOf(
        "Nhấn giữ" to (get("secure", "long_press_timeout").trim().let { if (it.isBlank() || it == "null") "mặc định" else "${it}ms" }),
        "Nhấn đôi" to (get("secure", "multi_press_timeout").trim().let { if (it.isBlank() || it == "null") "mặc định" else "${it}ms" }),
        "Cảm ứng cao (Samsung)" to (get("system", "high_touch_sensitivity_enable").trim().let { if (it == "1") "BẬT" else if (it == "0") "tắt" else "ROM không có" }),
        "Chống vuốt nhầm cạnh" to (get("secure", "back_gesture_inset_scale_left").trim().let { if (it.isBlank() || it == "null") "tắt" else "BẬT" }))
    fun setHighTouch(on: Boolean) = exec("settings put system high_touch_sensitivity_enable ${if (on) 1 else 0}")
    fun setHeadsUp(on: Boolean) = exec("settings put global heads_up_notifications_enabled ${if (on) 0 else 1}")
    fun setBgData(on: Boolean) = exec("cmd netpolicy set restrict-background $on")
    fun screenshot(): Pair<Int, String> = exec("screencap -p /sdcard/Pictures/GameSpace_${System.currentTimeMillis()}.png")
    fun cpuGhz(): Float = exec("cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq 2>/dev/null").second
        .lines().mapNotNull { it.trim().toLongOrNull() }.maxOrNull()?.let { it / 1_000_000f } ?: -1f
    fun gpuMhz(): Int {
        val v = exec("cat /sys/class/kgsl/kgsl-3d0/gpuclk 2>/dev/null || cat /sys/class/devfreq/*gpu*/cur_freq 2>/dev/null || cat /sys/class/devfreq/*mali*/cur_freq 2>/dev/null").second
            .lines().firstOrNull()?.trim()?.toLongOrNull() ?: return -1
        return if (v > 1_000_000) (v / 1_000_000).toInt() else v.toInt()
    }

    /** Các mức Hz mà màn hình máy thực sự hỗ trợ. */
    @Suppress("DEPRECATION")
    fun supportedHz(ctx: Context): List<Int> = try {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.supportedModes.map { Math.round(it.refreshRate) }.distinct().sorted()
    } catch (e: Throwable) { listOf(60, 90, 120) }
}
