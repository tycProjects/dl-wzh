package com.mousetuner

import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager
import rikka.shizuku.Shizuku

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

    fun get(ns: String, key: String) = exec("settings get $ns $key").second

    fun setPointerSpeed(v: Int) = exec("settings put system pointer_speed $v")
    // Các khoá dưới đây tuỳ phiên bản Android/hãng; máy không hỗ trợ thì lệnh vô hại.
    fun setAccel(on: Boolean) = exec("settings put system mouse_pointer_acceleration_enabled ${if (on) 1 else 0}")
    fun setSwapButtons(on: Boolean) = exec("settings put system mouse_swap_primary_button ${if (on) 1 else 0}")
    fun setReverseScroll(on: Boolean) = exec("settings put system mouse_reverse_vertical_scrolling ${if (on) 1 else 0}")
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

    fun applyPreset(speed: Int, anim: String) { setPointerSpeed(speed); setAccel(false); setAnim(anim) }
    fun reset() {
        setPointerSpeed(0); setAccel(true); setHighRefresh(null); setAnim("1.0"); setSwapButtons(false); setReverseScroll(false); touchBoost(false)
    }

    /** Các mức Hz mà màn hình máy thực sự hỗ trợ. */
    @Suppress("DEPRECATION")
    fun supportedHz(ctx: Context): List<Int> = try {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.supportedModes.map { Math.round(it.refreshRate) }.distinct().sorted()
    } catch (e: Throwable) { listOf(60, 90, 120) }
}
