// app/src/main/java/com/hyperboost/engine/core/FreeFireBoost.kt
package com.hyperboost.engine.core

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.hyperboost.engine.telemetry.TelemetryHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The only two games this build targets. */
object FreeFire {
    data class Variant(val pkg: String, val name: String, val icon: Drawable?)

    val PACKAGES = mapOf(
        "com.dts.freefireth" to "Free Fire",
        "com.dts.freefiremax" to "Free Fire MAX"
    )

    fun isFreeFire(pkg: String?): Boolean = pkg != null && pkg in PACKAGES

    fun installed(pm: PackageManager): List<Variant> = PACKAGES.mapNotNull { (pkg, name) ->
        try {
            val info = pm.getApplicationInfo(pkg, 0)
            Variant(pkg, name, pm.getApplicationIcon(info))
        } catch (_: Exception) { null }
    }
}

/** Everything the user can switch on for a Free Fire session. Persisted. */
data class FfOptions(
    val mode: String = TurboModes.PERFORMANCE,
    val cleanRam: Boolean = true,
    val dnd: Boolean = true,
    val blockHeadsUp: Boolean = true,
    val touchBoost: Boolean = true,
    val wifiLowLatency: Boolean = true,
    val keepAlive: Boolean = true,
    val noAnimations: Boolean = true,
    val keepScreenOn: Boolean = true,
    val lockBrightness: Boolean = false,
    val resLevel: Int = 0,
    val colorOn: Boolean = true,
    val clearFilters: Boolean = true,
    val sidebar: Boolean = true,
    val autoStart: Boolean = false
)

/**
 * Free Fire session tuning over Shizuku. Every setting it changes is snapshotted first and put
 * back by [restore], so leaving the game returns the phone to how the user had it.
 */
class FreeFireBoost(context: Context) {

    companion object {
        private const val UNSET = "\u0000unset"
        /** Free Fire render scale per level: Gốc, Mức 1, Mức 2, Mức 3. */
        val RES_PERCENT = listOf(100, 80, 70, 50)
        private val _recording = MutableStateFlow(false)
        val recording: StateFlow<Boolean> = _recording.asStateFlow()
    }

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("ff_turbo", Context.MODE_PRIVATE)
    private val snaps = app.getSharedPreferences("ff_snap", Context.MODE_PRIVATE)
    private val engine = PerformanceEngine(app)

    // ---- options -------------------------------------------------------------------------------

    fun loadOptions(): FfOptions {
        val d = FfOptions()
        return FfOptions(
            mode = prefs.getString("mode", d.mode)?.takeIf { it in TurboModes.ALL } ?: d.mode,
            cleanRam = prefs.getBoolean("cleanRam", d.cleanRam),
            dnd = prefs.getBoolean("dnd", d.dnd),
            blockHeadsUp = prefs.getBoolean("blockHeadsUp", d.blockHeadsUp),
            touchBoost = prefs.getBoolean("touchBoost", d.touchBoost),
            wifiLowLatency = prefs.getBoolean("wifi", d.wifiLowLatency),
            keepAlive = prefs.getBoolean("keepAlive", d.keepAlive),
            noAnimations = prefs.getBoolean("noAnim", d.noAnimations),
            keepScreenOn = prefs.getBoolean("keepScreen", d.keepScreenOn),
            lockBrightness = prefs.getBoolean("lockBright", d.lockBrightness),
            resLevel = prefs.getInt("resLevel", d.resLevel).coerceIn(0, 3),
            colorOn = prefs.getBoolean("colorOn", d.colorOn),
            clearFilters = prefs.getBoolean("clearFilters", d.clearFilters),
            sidebar = prefs.getBoolean("sidebar", d.sidebar),
            autoStart = prefs.getBoolean("autoStart", d.autoStart)
        )
    }

    fun saveOptions(o: FfOptions) {
        if (o.resLevel != prefs.getInt("resLevel", 0)) prefs.edit().remove("resPercent").apply()
        prefs.edit()
            .putString("mode", o.mode).putBoolean("cleanRam", o.cleanRam).putBoolean("dnd", o.dnd)
            .putBoolean("blockHeadsUp", o.blockHeadsUp).putBoolean("touchBoost", o.touchBoost)
            .putBoolean("wifi", o.wifiLowLatency).putBoolean("keepAlive", o.keepAlive)
            .putBoolean("noAnim", o.noAnimations).putBoolean("keepScreen", o.keepScreenOn)
            .putBoolean("lockBright", o.lockBrightness).putInt("resLevel", o.resLevel).putBoolean("colorOn", o.colorOn).putBoolean("clearFilters", o.clearFilters)
            .putBoolean("sidebar", o.sidebar).putBoolean("autoStart", o.autoStart).apply()
    }

    fun isActive(): Boolean = prefs.getBoolean("active", false)

    // ---- shell helpers -------------------------------------------------------------------------

    private suspend fun sh(cmd: String, timeout: Long = ShizukuShellExecutor.DEFAULT_TIMEOUT_MS) =
        ShizukuShellExecutor.execute(cmd, timeout)

    private suspend fun out(cmd: String): String? =
        sh(cmd).getOrNull()?.takeIf { it.exitCode == 0 }?.stdout?.trim()

    private suspend fun put(ns: String, key: String, value: String): Boolean {
        val id = "$ns/$key"
        if (!snaps.contains(id)) {
            val v = out("settings get $ns $key")
            snaps.edit().putString(id, if (v.isNullOrEmpty() || v == "null") UNSET else v).apply()
        }
        return sh("settings put $ns $key ${ShizukuShellExecutor.quote(value)}").getOrNull()?.exitCode == 0
    }

    private suspend fun restoreSettings(fails: MutableList<String>) {
        for ((id, v) in snaps.all.toMap()) {
            val saved = v as? String ?: continue
            val ns = id.substringBefore('/')
            val key = id.substringAfter('/')
            val cmd = if (saved == UNSET) "settings delete $ns $key"
            else "settings put $ns $key ${ShizukuShellExecutor.quote(saved)}"
            if (sh(cmd).getOrNull()?.exitCode == 0) snaps.edit().remove(id).apply() else fails += "Khôi phục $key"
        }
    }

    // ---- apply / restore -----------------------------------------------------------------------

    /** Applies every enabled tweak. Returns the tweaks this device rejected. */
    suspend fun apply(pkg: String, o: FfOptions): List<String> = withContext(Dispatchers.IO) {
        val fails = mutableListOf<String>()
        fun note(label: String, r: Result<*>) { r.onFailure { fails += label } }
        val rates = TelemetryHub.get(app).supportedRefreshRates()

        if (o.cleanRam) note("Dọn RAM", engine.killBackgroundApps())
        TurboModes.apply(engine, rates, o.mode).forEach { fails += it }
        note("Game Mode", engine.setAndroidGameMode(pkg, o.mode))
        if (o.keepAlive) {
            note("Chống ngủ ứng dụng", engine.whitelistAppFromDoze(pkg))
            sh("am set-standby-bucket $pkg active")
            if (!put("global", "adaptive_battery_management_enabled", "0")) fails += "Pin thích ứng"
        }
        if (o.dnd) note("Không làm phiền", engine.setGameDnd(true))
        if (o.blockHeadsUp && !put("global", "heads_up_notifications_enabled", "0")) fails += "Chặn thông báo nổi"
        if (o.touchBoost) note("Cảm ứng nhanh", engine.setTouchLatencyBoost(true))
        if (o.wifiLowLatency) note("Wi-Fi độ trễ thấp", engine.setWifiGamingOptimization(true))
        if (o.noAnimations) note("Tắt hiệu ứng", engine.setWindowAnimationScale(0f))
        if (o.keepScreenOn && !put("system", "screen_off_timeout", "1800000")) fails += "Giữ màn hình sáng"
        if (o.lockBrightness && !put("system", "screen_brightness_mode", "0")) fails += "Khoá độ sáng"
        loadResPercent().let { pct -> if (pct < 100) applyResolutionPercent(pkg, pct).onFailure { fails += "Giảm độ phân giải" } }
        if (o.clearFilters) {
            listOf("night_display_activated", "reduce_bright_colors_activated", "accessibility_display_daltonizer_enabled")
                .forEach { put("secure", it, "0") }
        }
        if (o.colorOn) applyColor(loadColor()).onFailure { fails += "Chỉnh màu" }
        prefs.edit().putBoolean("active", true).putString("activePkg", pkg).apply()
        fails
    }

    private suspend fun setResolutionWm(percent: Int): Boolean {
        val line = out("wm size") ?: return false
        val m = Regex("Physical size: (\\d+)x(\\d+)").find(line) ?: return false
        fun scaled(v: Int) = (v * percent / 100) / 2 * 2
        val w = scaled(m.groupValues[1].toInt())
        val h = scaled(m.groupValues[2].toInt())
        return sh("wm size ${w}x$h").getOrNull()?.exitCode == 0
    }

    /** Puts the phone back the way it was before [apply]. */
    suspend fun restore(): List<String> = withContext(Dispatchers.IO) {
        val o = loadOptions()
        val fails = mutableListOf<String>()
        fun note(label: String, r: Result<*>) { r.onFailure { fails += label } }
        if (_recording.value) stopRecording()
        if (prefs.getString("resMethod", null) != null) {
            resetResolution(prefs.getString("activePkg", "") ?: "").onFailure { fails += "Khôi phục độ phân giải" }
        }
        if (prefs.getBoolean("colorApplied", false)) {
            ColorTuner.reset().onFailure { fails += "Khôi phục màu" }
            prefs.edit().putBoolean("colorApplied", false).apply()
        }
        if (o.dnd) note("Không làm phiền", engine.setGameDnd(false))
        if (o.touchBoost) note("Cảm ứng", engine.setTouchLatencyBoost(false))
        if (o.wifiLowLatency) note("Wi-Fi", engine.setWifiGamingOptimization(false))
        if (o.noAnimations) note("Hiệu ứng", engine.restoreWindowAnimationScale())
        restoreSettings(fails)
        TurboModes.apply(engine, TelemetryHub.get(app).supportedRefreshRates(), TurboModes.STANDARD).forEach { fails += it }
        prefs.edit().putBoolean("active", false).apply()
        fails
    }

    // ---- resolution (per-game downscale, ROG-style) --------------------------------------------

    /**
     * Level 1-3 lowers only Free Fire's render resolution with Android's built-in game
     * downscale (`cmd game set --downscale`), so the system UI stays sharp. It takes effect the
     * next time the game starts. Devices without it fall back to a whole-screen size change.
     */
    suspend fun applyResolution(pkg: String, level: Int): Result<String> =
        applyResolutionPercent(pkg, RES_PERCENT[level.coerceIn(0, 3)])

    /** Saved render scale in percent (50..100). Falls back to the level's preset. */
    fun loadResPercent(): Int =
        prefs.getInt("resPercent", RES_PERCENT[prefs.getInt("resLevel", 0).coerceIn(0, 3)]).coerceIn(50, 100)

    /** Free percentage (50..100, steps of 5 in the sidebar). 100 puts the original resolution back. */
    suspend fun applyResolutionPercent(pkg: String, percentIn: Int): Result<String> = withContext(Dispatchers.IO) {
        val percent = percentIn.coerceIn(50, 100)
        prefs.edit().putInt("resPercent", percent).apply()
        if (percent == 100) return@withContext resetResolution(pkg)
        val ratio = String.format(Locale.US, "%.2f", percent / 100f)
        val r = sh("cmd game set --mode 2 --downscale $ratio $pkg").getOrNull()
        val text = ((r?.stdout ?: "") + " " + (r?.stderr ?: "")).lowercase()
        val ok = r != null && r.exitCode == 0 && listOf("error", "exception", "unknown", "usage", "not supported").none { it in text }
        if (ok) {
            prefs.edit().putString("resMethod", "game").putString("activePkg", pkg).apply()
            return@withContext Result.success("Đã đặt $percent% cho game, có hiệu lực khi mở lại game")
        }
        if (setResolutionWm(percent)) {
            prefs.edit().putString("resMethod", "wm").putString("activePkg", pkg).apply()
            Result.success("Máy không hỗ trợ giảm riêng game, đã giảm toàn màn hình $percent%")
        } else Result.failure(RuntimeException("Máy không hỗ trợ giảm độ phân giải"))
    }

    suspend fun resetResolution(pkg: String): Result<String> = withContext(Dispatchers.IO) {
        if (prefs.getString("resMethod", null) == "wm") sh("wm size reset")
        if (pkg.isNotEmpty()) sh("cmd game reset $pkg")
        prefs.edit().remove("resMethod").apply()
        Result.success("Đã trả độ phân giải gốc")
    }

    // ---- colour / display ------------------------------------------------------------------------

    fun loadColor(): ColorSettings = ColorSettings(
        prefs.getFloat("c_sat", 1.2f), prefs.getFloat("c_con", 1.15f),
        prefs.getFloat("c_bri", 1.05f), prefs.getFloat("c_warm", -0.1f)
    )

    fun saveColor(c: ColorSettings) {
        prefs.edit().putFloat("c_sat", c.saturation).putFloat("c_con", c.contrast)
            .putFloat("c_bri", c.brightness).putFloat("c_warm", c.warmth).apply()
    }

    suspend fun applyColor(c: ColorSettings): Result<String> = withContext(Dispatchers.IO) {
        saveColor(c)
        val r = ColorTuner.apply(c)
        if (r.isSuccess) prefs.edit().putBoolean("colorApplied", !c.isDefault).apply()
        r
    }

    suspend fun resetColor(): Result<String> = withContext(Dispatchers.IO) {
        saveColor(ColorSettings())
        val r = ColorTuner.reset()
        prefs.edit().putBoolean("colorApplied", false).apply()
        r
    }

    fun currentBrightness(): Int =
        android.provider.Settings.System.getInt(app.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, 128)

    /** Backlight level 1-255 (switches auto-brightness off). Restored when the session ends. */
    suspend fun setBrightness(v: Int): Boolean = withContext(Dispatchers.IO) {
        put("system", "screen_brightness_mode", "0") && put("system", "screen_brightness", v.coerceIn(1, 255).toString())
    }

    suspend fun setHeadsUp(block: Boolean): Boolean =
        withContext(Dispatchers.IO) { put("global", "heads_up_notifications_enabled", if (block) "0" else "1") }

    suspend fun keepAlive(pkg: String): Result<String> = withContext(Dispatchers.IO) {
        val r = engine.whitelistAppFromDoze(pkg)
        sh("am set-standby-bucket $pkg active")
        r
    }

    // ---- one-shot tools ------------------------------------------------------------------------

    suspend fun compileShaders(pkg: String): Result<String> = engine.compileAppSpeedProfile(pkg)

    suspend fun cleanRam(): Result<String> = engine.killBackgroundApps()

    suspend fun clearCache(pkg: String): Result<String> = withContext(Dispatchers.IO) {
        val a = sh("pm clear --cache-only $pkg").getOrNull()
        if (a != null && a.exitCode == 0 && a.stdout.contains("Success", true)) return@withContext Result.success("Đã dọn cache")
        val b = sh("rm -rf /sdcard/Android/data/$pkg/cache/*").getOrNull()
        if (b != null && b.exitCode == 0) Result.success("Đã dọn cache")
        else Result.failure(RuntimeException("Máy không cho dọn cache"))
    }

    private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    suspend fun screenshot(): Result<String> = withContext(Dispatchers.IO) {
        val path = "/sdcard/Pictures/LSUPRA/FF_${stamp()}.png"
        val r = sh("mkdir -p /sdcard/Pictures/LSUPRA && screencap -p $path").getOrNull()
        if (r != null && r.exitCode == 0) {
            sh("am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$path")
            Result.success("Đã lưu: Pictures/LSUPRA")
        } else Result.failure(RuntimeException("Không chụp được màn hình"))
    }

    suspend fun startRecording(): Result<String> = withContext(Dispatchers.IO) {
        val path = "/sdcard/Movies/LSUPRA/FF_${stamp()}.mp4"
        val r = sh("mkdir -p /sdcard/Movies/LSUPRA; setsid nohup screenrecord --time-limit 180 --bit-rate 8000000 $path >/dev/null 2>&1 &").getOrNull()
        if (r != null && r.exitCode == 0) {
            _recording.value = true
            CoroutineScope(Dispatchers.IO).launch { delay(182_000); _recording.value = false }
            Result.success("Đang quay (tối đa 3 phút)")
        } else Result.failure(RuntimeException("Không quay được màn hình"))
    }

    suspend fun stopRecording(): Result<String> = withContext(Dispatchers.IO) {
        sh("pkill -INT screenrecord")
        _recording.value = false
        Result.success("Đã lưu video: Movies/LSUPRA")
    }

    /** Round-trip time in ms to a public DNS, or null when it cannot be measured. */
    suspend fun ping(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val p = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "2", "1.1.1.1").redirectErrorStream(true).start()
            val text = p.inputStream.bufferedReader().readText()
            p.waitFor()
            Regex("time=([0-9.]+)").find(text)?.groupValues?.get(1)?.toFloat()?.toInt()
        }.getOrNull()
    }
}
