package com.mousetuner

import android.content.Context
import android.os.Build
import kotlin.concurrent.thread

/** Áp dụng cấu hình ra hệ thống thật (qua Shizuku). Gọi trên luồng nền. */
object Engine {
    /** Android tính gain con trỏ ≈ 2^(pointer_speed/4). Ở đây đảo lại: speed = 4·log2(gain), giới hạn -7..7. */
    fun speedFor(c: Context): Int {
        var gain = if (Cfg.lin(c)) Cfg.sens(c) else 1f
        if (Cfg.cad(c)) gain *= 800f / Cfg.dpi(c)           // chuẩn hoá theo CPI, mốc 800
        val s = Math.round(4.0 * Math.log(gain.toDouble()) / Math.log(2.0)).toInt()
        return s.coerceIn(-7, 7)
    }
    fun gainOf(speed: Int) = Math.pow(2.0, speed / 4.0)

    fun accelSupport(): String =
        if (Build.VERSION.SDK_INT >= 35) "Android ${Build.VERSION.RELEASE}: có công tắc tắt gia tốc hệ thống"
        else "Android ${Build.VERSION.RELEASE}: hệ thống KHÔNG cho tắt hẳn gia tốc → dùng Vùng Linear (Raw) để có tuyến tính tuyệt đối"

    fun apply(c: Context) {
        val wantAccelOff = (Cfg.lin(c) && Cfg.noAccel(c)) || Cfg.cad(c)
        if (Cfg.lin(c) || Cfg.cad(c)) {
            ShizukuShell.setPointerSpeed(speedFor(c))
            ShizukuShell.setAccel(!wantAccelOff)
        } else { ShizukuShell.setPointerSpeed(0); ShizukuShell.setAccel(true) }

        if (Cfg.xfr(c)) {
            ShizukuShell.setHighRefresh(ShizukuShell.supportedHz(c).maxOrNull())
            ShizukuShell.setAnim("0.5")
            val hz = ShizukuShell.supportedHz(c).maxOrNull() ?: 60
            FreeFire.installed(c).forEach {
                ShizukuShell.setGameMode(it.second, "performance")
                if (Cfg.ds(c) < 0.99f) ShizukuShell.setDownscale(it.second, Cfg.ds(c), hz) else ShizukuShell.gameFps(it.second, hz)
            }
            gameCfg = true
        } else {
            ShizukuShell.setHighRefresh(null); ShizukuShell.setAnim("1.0")
            if (gameCfg) { FreeFire.installed(c).forEach { ShizukuShell.resetGame(it.second) }; gameCfg = false }
        }

        ShizukuShell.setTouchOverlays(Cfg.dbg(c))
        acts.forEach { (k, f) -> sync(c, k, f) }
    }

    // Tiện ích bật/tắt: chỉ đụng vào hệ thống khi thực sự đổi trạng thái, và chỉ trả lại thứ do app đã bật.
    @Volatile private var gameCfg = false
    private val applied = HashSet<String>()
    private val acts: Map<String, (Context, Boolean) -> Unit> = mapOf(
        "dnd" to { _: Context, on: Boolean -> ShizukuShell.setDnd(on); Unit },
        "touch" to { _: Context, on: Boolean -> ShizukuShell.setTouchTune(on); Unit },
        "nobat" to { _: Context, on: Boolean -> ShizukuShell.setBatteryPerf(on); Unit },
        "noblur" to { _: Context, on: Boolean -> ShizukuShell.setNoBlur(on); Unit },
        "wifi" to { _: Context, on: Boolean -> ShizukuShell.setWifiLowLatency(on); Unit },
        "noauto" to { c: Context, on: Boolean ->
            if (on) { Cfg.setI(c, "prev_auto", ShizukuShell.get("system", "screen_brightness_mode").toIntOrNull() ?: 1); ShizukuShell.setAutoBrightness(false) }
            else ShizukuShell.setAutoBrightness(Cfg.i(c, "prev_auto", 1) == 1)
            Unit }
    )
    private fun sync(c: Context, key: String, f: (Context, Boolean) -> Unit) {
        val want = Cfg.b(c, key); val has = applied.contains(key)
        if (want && !has) { f(c, true); applied.add(key) } else if (!want && has) { f(c, false); applied.remove(key) }
    }
    /** Hồ sơ nhanh: bật/tắt toàn bộ bộ tính năng chơi game. */
    fun profile(c: Context, on: Boolean) {
        listOf("xfr", "dbg", "fps", "dnd", "touch", "nobat", "noblur", "wifi", "lin").forEach { Cfg.setB(c, it, on) }
        if (on) Cfg.setB(c, "noaccel", true)
        applyAsync(c)
    }
    fun applyAsync(c: Context) { thread { apply(c) } }

    fun reset(c: Context) {
        ShizukuShell.reset(); ShizukuShell.setTouchOverlays(false)
        applied.toList().forEach { acts[it]?.invoke(c, false) }; applied.clear()
        if (gameCfg) { FreeFire.installed(c).forEach { ShizukuShell.resetGame(it.second) }; gameCfg = false }
    }
}
