package com.mousetuner

import android.content.Context
import kotlin.concurrent.thread

/** Áp cấu hình Game Space ra hệ thống thật (qua Shizuku). Gọi trên luồng nền. */
object Engine {
    @Volatile private var gameCfg = false
    @Volatile private var visual = false
    @Volatile private var appliedBuff = 0
    private val BUFF_LP = intArrayOf(400, 280, 200, 130)   // nhấn giữ (ms)
    private val BUFF_MP = intArrayOf(300, 240, 180, 140)   // nhấn đôi (ms)

    /** Buff nhạy: 0 Tắt · 1 Nhẹ · 2 Mạnh · 3 MAX — rút ngắn thời gian nhận chạm, bật cảm ứng cao (nếu ROM có), thu hẹp vùng vuốt cạnh. */
    private fun buff(c: Context) {
        val lv = Cfg.i(c, "buff", 0).coerceIn(0, 3)
        if (lv == appliedBuff) return
        ShizukuShell.setLongPress(BUFF_LP[lv]); ShizukuShell.setMultiPress(BUFF_MP[lv])
        ShizukuShell.setHighTouch(lv >= 2)
        if (lv >= 2) ShizukuShell.setEdgeGuard(true) else if (!Cfg.b(c, "edge")) ShizukuShell.setEdgeGuard(false)
        if (lv == 3) ShizukuShell.setAnim("0.5")
        appliedBuff = lv
    }
    private val applied = HashSet<String>()

    fun apply(c: Context) {
        val mode = Cfg.mode(c)
        val maxHz = ShizukuShell.supportedHz(c).maxOrNull() ?: 60
        val hz: Int? = when (Cfg.hzMode(c)) { 1 -> 60; 2 -> maxHz; else -> when (mode) { 2 -> maxHz; 0 -> 60; else -> null } }
        ShizukuShell.setHighRefresh(hz)
        ShizukuShell.setAnim(if (mode == 2) "0.5" else "1.0")
        val apps = FreeFire.installed(c).map { it.second }
        val wantDs = Cfg.ds(c) < 0.99f
        if (mode != 1 || wantDs) {
            apps.forEach {
                ShizukuShell.setGameMode(it, if (mode == 2) "performance" else if (mode == 0) "battery" else "standard")
                if (wantDs) ShizukuShell.setDownscale(it, Cfg.ds(c), hz ?: maxHz) else if (mode == 2) ShizukuShell.gameFps(it, maxHz)
            }
            gameCfg = true
        } else if (gameCfg) { apps.forEach { ShizukuShell.resetGame(it) }; gameCfg = false }

        ShizukuShell.setAngle(if (Cfg.angle(c)) apps else emptyList())
        if (Cfg.sat(c) != 1f || visual) { ShizukuShell.setSaturation(Cfg.sat(c)); visual = Cfg.sat(c) != 1f }
        ShizukuShell.setTouchOverlays(Cfg.dbg(c))
        ShizukuShell.setBatteryPerf(mode == 2 || Cfg.b(c, "nobat")).also { if (mode == 0) ShizukuShell.exec("settings put global low_power 1") }
        acts.forEach { (k, f) -> sync(c, k, f) }
        buff(c)
    }

    private val acts: Map<String, (Context, Boolean) -> Unit> = mapOf(
        "dnd" to { _: Context, on: Boolean -> ShizukuShell.setDnd(on); Unit },
        "touch" to { c: Context, on: Boolean -> ShizukuShell.setLongPress(if (on) Cfg.lp(c) else 400); ShizukuShell.setMultiPress(if (on) 200 else 300); Unit },
        "noblur" to { _: Context, on: Boolean -> ShizukuShell.setNoBlur(on); Unit },
        "wifi" to { _: Context, on: Boolean -> ShizukuShell.setWifiLowLatency(on); Unit },
        "edge" to { _: Context, on: Boolean -> ShizukuShell.setEdgeGuard(on); Unit },
        "rotlock" to { _: Context, on: Boolean -> ShizukuShell.setRotLock(on); Unit },
        "stayon" to { _: Context, on: Boolean -> ShizukuShell.setStayOn(on); Unit },
        "hitouch" to { _: Context, on: Boolean -> ShizukuShell.setHighTouch(on); Unit },
        "ramclean" to { _: Context, on: Boolean -> if (on) ShizukuShell.killBg(); Unit },
        "headsup" to { _: Context, on: Boolean -> ShizukuShell.setHeadsUp(on); Unit },
        "bgdata" to { _: Context, on: Boolean -> ShizukuShell.setBgData(on); Unit },
        "noauto" to { c: Context, on: Boolean ->
            if (on) { Cfg.setI(c, "prev_auto", ShizukuShell.get("system", "screen_brightness_mode").toIntOrNull() ?: 1); ShizukuShell.setAutoBrightness(false) }
            else ShizukuShell.setAutoBrightness(Cfg.i(c, "prev_auto", 1) == 1)
            Unit }
    )
    private fun sync(c: Context, key: String, f: (Context, Boolean) -> Unit) {
        val want = Cfg.b(c, key); val has = applied.contains(key)
        if (want && !has) { f(c, true); applied.add(key) } else if (!want && has) { f(c, false); applied.remove(key) }
    }

    /** GAME ON = MONSTER + toàn bộ tiện ích chơi game; TẮT HẾT = về Cân bằng. */
    fun profile(c: Context, on: Boolean) {
        listOf("dbg", "fps", "dnd", "touch", "noblur", "wifi", "headsup", "edge", "ramclean").forEach { Cfg.setB(c, it, on) }
        Cfg.setI(c, "mode", if (on) 2 else 1); Cfg.setI(c, "buff", if (on) 2 else 0); if (!on) { Cfg.setF(c, "ds", 1f); Cfg.setF(c, "sat", 1f); Cfg.setB(c, "angle", false); Cfg.setI(c, "hzm", 0) }
        applyAsync(c)
    }
    fun applyAsync(c: Context) { thread { apply(c) } }

    fun reset(c: Context) {
        ShizukuShell.reset(); ShizukuShell.setTouchOverlays(false); visual = false
        ShizukuShell.setLongPress(400); ShizukuShell.setMultiPress(300); ShizukuShell.setHighTouch(false); appliedBuff = 0
        applied.toList().forEach { acts[it]?.invoke(c, false) }; applied.clear()
        if (gameCfg) { FreeFire.installed(c).forEach { ShizukuShell.resetGame(it.second) }; gameCfg = false }
    }
}
