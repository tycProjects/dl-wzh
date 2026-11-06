// app/src/main/java/com/hyperboost/engine/core/ColorTuner.kt
package com.hyperboost.engine.core

import java.util.Locale

/** Display tuning values. 1.0 / 0.0 means "leave unchanged". */
data class ColorSettings(
    val saturation: Float = 1f,   // 0.5 .. 2.0
    val contrast: Float = 1f,     // 0.8 .. 1.5
    val brightness: Float = 1f,   // 0.7 .. 1.4 (colour gain, not backlight)
    val warmth: Float = 0f        // -1 (cool) .. 1 (warm)
) {
    val isDefault: Boolean get() = saturation == 1f && contrast == 1f && brightness == 1f && warmth == 0f
}

object ColorPresets {
    val ALL: List<Pair<String, ColorSettings>> = listOf(
        "Gốc" to ColorSettings(),
        "Sống động" to ColorSettings(1.35f, 1.05f, 1.0f, 0f),
        "Nét FPS" to ColorSettings(1.2f, 1.15f, 1.05f, -0.1f),
        "Soi bụi cỏ" to ColorSettings(1.25f, 1.2f, 1.15f, 0f),
        "Ấm" to ColorSettings(1.1f, 1.0f, 1.0f, 0.5f),
        "Lạnh" to ColorSettings(1.1f, 1.05f, 1.0f, -0.5f)
    )
}

/**
 * Whole-screen colour tuning through SurfaceFlinger (saturation + a 4x4 colour matrix for
 * contrast, brightness gain and warmth). Needs Shizuku; devices that block these transactions
 * report failure instead of pretending.
 */
object ColorTuner {

    private fun f(v: Float) = String.format(Locale.US, "%.4f", v)

    private fun ok(r: Result<ShizukuShellExecutor.ShellResult>): Boolean =
        r.getOrNull()?.let { it.exitCode == 0 && it.stdout.contains("00000000") } == true

    suspend fun apply(c: ColorSettings): Result<String> {
        if (c.isDefault) return reset()
        val sat = ok(ShizukuShellExecutor.execute("service call SurfaceFlinger 1022 f ${f(c.saturation.coerceIn(0.5f, 2f))}"))

        val w = c.warmth.coerceIn(-1f, 1f)
        val gain = c.brightness.coerceIn(0.7f, 1.4f)
        val k = c.contrast.coerceIn(0.8f, 1.5f)
        val r = (gain * (1f + 0.12f * w)).coerceAtMost(1.5f) * k
        val g = gain * k
        val b = (gain * (1f - 0.12f * w)).coerceAtMost(1.5f) * k
        val t = 0.5f * (1f - k)
        val matrix = listOf(r, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, b, 0f, t, t, t, 1f)
        val mat = ok(ShizukuShellExecutor.execute("service call SurfaceFlinger 1015 i32 1 " + matrix.joinToString(" ") { "f ${f(it)}" }))

        return if (sat || mat) Result.success("Đã áp dụng màu") else
            Result.failure(RuntimeException("Máy không cho chỉnh màu qua hệ thống"))
    }

    suspend fun reset(): Result<String> {
        val a = ok(ShizukuShellExecutor.execute("service call SurfaceFlinger 1022 f 1.0"))
        val b = ok(ShizukuShellExecutor.execute("service call SurfaceFlinger 1015 i32 0"))
        return if (a || b) Result.success("Đã trả màu gốc") else Result.failure(RuntimeException("Không đặt lại được màu"))
    }
}
