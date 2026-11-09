package com.mousetuner

import android.content.Context
import kotlin.concurrent.thread

/**
 * Áp cấu hình Game Space ra hệ thống thật (qua Shizuku). Gọi trên luồng nền.
 * Nguyên tắc: MỌI tính năng chỉ có tác dụng khi công tắc của nó đang BẬT. Tắt công tắc → trả về mặc định.
 * Không tính năng nào tự bật; chỉ áp khi giá trị thay đổi so với lần trước.
 */
object Engine {
    private val BUFF_LP = intArrayOf(400, 280, 200, 130)   // nhấn giữ (ms)
    private val BUFF_MP = intArrayOf(300, 240, 180, 140)   // nhấn đôi (ms)

    // trạng thái ĐÃ áp lên hệ thống (mặc định = chưa đụng gì)
    @Volatile private var aMode = 1
    @Volatile private var aHz = 0
    @Volatile private var aDs = 1f
    @Volatile private var aSat = 1f
    @Volatile private var aAngle = false
    @Volatile private var aDbg = false
    @Volatile private var aBat = false
    @Volatile private var appliedBuff = 0
    @Volatile private var cleaned = false
    private val applied = HashSet<String>()

    private val acts: Map<String, (Context, Boolean) -> Unit> = mapOf(
        "dnd" to { _: Context, on: Boolean -> ShizukuShell.setDnd(on); Unit },
        "touch" to { c: Context, on: Boolean -> ShizukuShell.setLongPress(if (on) Cfg.lp(c) else BUFF_LP[appliedBuff]); ShizukuShell.setMultiPress(if (on) 200 else BUFF_MP[appliedBuff]); Unit },
        "noblur" to { _: Context, on: Boolean -> ShizukuShell.setNoBlur(on); Unit },
        "wifi" to { _: Context, on: Boolean -> ShizukuShell.setWifiLowLatency(on); Unit },
        "edge" to { _: Context, on: Boolean -> ShizukuShell.setEdgeGuard(on || appliedBuff >= 2); Unit },
        "rotlock" to { _: Context, on: Boolean -> ShizukuShell.setRotLock(on); Unit },
        "stayon" to { _: Context, on: Boolean -> ShizukuShell.setStayOn(on); Unit },
        "hitouch" to { _: Context, on: Boolean -> ShizukuShell.setHighTouch(on || appliedBuff >= 2); Unit },
        "ramclean" to { _: Context, on: Boolean -> if (on) ShizukuShell.killBg(); Unit },
        "headsup" to { _: Context, on: Boolean -> ShizukuShell.setHeadsUp(on); Unit },
        "bgdata" to { _: Context, on: Boolean -> ShizukuShell.setBgData(on); Unit },
        "noauto" to { c: Context, on: Boolean ->
            if (on) { Cfg.setI(c, "prev_auto", ShizukuShell.get("system", "screen_brightness_mode").trim().toIntOrNull() ?: 1); ShizukuShell.setAutoBrightness(false) }
            else ShizukuShell.setAutoBrightness(Cfg.i(c, "prev_auto", 1) == 1)
            Unit }
    )
    private fun sync(c: Context, key: String, f: (Context, Boolean) -> Unit) {
        val want = Cfg.b(c, key); val has = applied.contains(key)
        if (want && !has) { f(c, true); applied.add(key) } else if (!want && has) { f(c, false); applied.remove(key) }
    }

    fun apply(c: Context) {
        // dọn dấu vết của bản cũ (lần đầu): tắt hiện điểm chạm / không làm phiền nếu người dùng đang để TẮT
        if (!cleaned) { cleaned = true
            if (!Cfg.b(c, "clean6")) { Cfg.setB(c, "clean6", true)
                if (!Cfg.dbg(c)) ShizukuShell.setTouchOverlays(false)
                if (!Cfg.b(c, "dnd")) ShizukuShell.setDnd(false) } }

        val mode = Cfg.mode(c); val hzm = Cfg.hzMode(c); val ds = Cfg.ds(c)
        val apps = FreeFire.installed(c).map { it.second }
        if (mode != aMode || hzm != aHz || ds != aDs) {
            val maxHz = ShizukuShell.supportedHz(c).maxOrNull() ?: 60
            val hz: Int? = when (hzm) { 1 -> 60; 2 -> maxHz; else -> when (mode) { 2 -> maxHz; 0 -> 60; else -> null } }
            if (mode == 1 && hzm == 0 && ds >= 0.99f) {           // về mặc định hoàn toàn
                ShizukuShell.setHighRefresh(null); ShizukuShell.setAnim("1.0"); apps.forEach { ShizukuShell.resetGame(it) }
                if (aMode == 0) ShizukuShell.exec("settings put global low_power 0")
            } else {
                ShizukuShell.setHighRefresh(hz); ShizukuShell.setAnim(if (mode == 2) "0.5" else "1.0")
                apps.forEach {
                    ShizukuShell.setGameMode(it, if (mode == 2) "performance" else if (mode == 0) "battery" else "standard")
                    if (ds < 0.99f) ShizukuShell.setDownscale(it, ds, hz ?: maxHz) else if (mode == 2) ShizukuShell.gameFps(it, maxHz)
                }
                if (mode == 0) ShizukuShell.exec("settings put global low_power 1") else if (aMode == 0) ShizukuShell.exec("settings put global low_power 0")
            }
            aMode = mode; aHz = hzm; aDs = ds
        }
        val bat = mode == 2 || Cfg.b(c, "nobat")
        if (bat != aBat) { ShizukuShell.setBatteryPerf(bat); aBat = bat }
        if (Cfg.angle(c) != aAngle) { ShizukuShell.setAngle(if (Cfg.angle(c)) apps else emptyList()); aAngle = Cfg.angle(c) }
        if (Cfg.sat(c) != aSat) { ShizukuShell.setSaturation(Cfg.sat(c)); aSat = Cfg.sat(c) }
        if (Cfg.dbg(c) != aDbg) { ShizukuShell.setTouchOverlays(Cfg.dbg(c)); aDbg = Cfg.dbg(c) }
        acts.forEach { (k, f) -> sync(c, k, f) }
        buff(c)
    }

    /** Buff nhạy: 0 Tắt · 1 Nhẹ · 2 Mạnh · 3 MAX. */
    private fun buff(c: Context) {
        val lv = Cfg.i(c, "buff", 0).coerceIn(0, 3)
        if (lv == appliedBuff) return
        appliedBuff = lv
        ShizukuShell.setLongPress(if (Cfg.b(c, "touch")) Cfg.lp(c) else BUFF_LP[lv]); ShizukuShell.setMultiPress(if (Cfg.b(c, "touch")) 200 else BUFF_MP[lv])
        ShizukuShell.setHighTouch(lv >= 2 || Cfg.b(c, "hitouch"))
        ShizukuShell.setEdgeGuard(lv >= 2 || Cfg.b(c, "edge"))
        if (lv == 3) ShizukuShell.setAnim("0.5") else if (Cfg.mode(c) != 2) ShizukuShell.setAnim("1.0")
    }

    private val ALL = listOf("dbg", "fps", "dnd", "touch", "noblur", "wifi", "headsup", "edge", "ramclean", "nobat", "rotlock", "stayon", "hitouch", "bgdata", "noauto", "angle")

    /** GAME ON = MONSTER + Buff Mạnh + vài tiện ích an toàn. KHÔNG tự bật hiện điểm chạm / không làm phiền / thanh FPS. TẮT HẾT = tắt tất cả. */
    fun profile(c: Context, on: Boolean) {
        if (on) { listOf("touch", "noblur", "wifi", "headsup", "edge", "ramclean").forEach { Cfg.setB(c, it, true) }; Cfg.setI(c, "mode", 2); Cfg.setI(c, "buff", 2) }
        else { ALL.forEach { Cfg.setB(c, it, false) }; Cfg.setI(c, "mode", 1); Cfg.setI(c, "buff", 0); Cfg.setI(c, "hzm", 0); Cfg.setF(c, "ds", 1f); Cfg.setF(c, "sat", 1f) }
        applyAsync(c)
    }
    fun applyAsync(c: Context) { thread { apply(c) } }

    /** Trả mọi thứ về mặc định hệ thống (không đổi công tắc người dùng). */
    fun reset(c: Context) {
        ShizukuShell.reset(); ShizukuShell.setTouchOverlays(false)
        ShizukuShell.setLongPress(400); ShizukuShell.setMultiPress(300); ShizukuShell.setHighTouch(false); ShizukuShell.setEdgeGuard(false)
        applied.toList().forEach { acts[it]?.invoke(c, false) }; applied.clear()
        FreeFire.installed(c).forEach { ShizukuShell.resetGame(it.second) }
        aMode = 1; aHz = 0; aDs = 1f; aSat = 1f; aAngle = false; aDbg = false; aBat = false; appliedBuff = 0
    }
}
