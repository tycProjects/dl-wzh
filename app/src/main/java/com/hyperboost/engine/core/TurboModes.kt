// app/src/main/java/com/hyperboost/engine/core/TurboModes.kt
package com.hyperboost.engine.core

/**
 * The three Game Turbo performance modes. Shared by the app and the in-game sidebar so both
 * apply exactly the same set of tweaks.
 */
object TurboModes {
    const val PERFORMANCE = "performance"
    const val STANDARD = "standard"
    const val BATTERY = "battery"

    val ALL = listOf(PERFORMANCE, STANDARD, BATTERY)

    fun label(mode: String): String = when (mode) {
        PERFORMANCE -> "Hiệu năng"
        BATTERY -> "Tiết kiệm"
        else -> "Cân bằng"
    }

    /** Returns the list of tweaks the device rejected (empty = everything applied). */
    suspend fun apply(engine: PerformanceEngine, supportedRates: List<Int>, mode: String): List<String> {
        val failures = mutableListOf<String>()
        suspend fun step(label: String, block: suspend () -> Result<String>) {
            block().onFailure { failures += "$label: ${it.message}" }
        }
        when (mode) {
            PERFORMANCE -> {
                supportedRates.maxOrNull()?.let { hz -> step("Tần số quét") { engine.lockRefreshRate(hz.toFloat()) } }
                step("Hiệu năng ổn định") { engine.setSustainedPerformance(true) }
                step("Không giới hạn pin") { engine.setBatteryThrottlingOff(true) }
                step("Cảm ứng nhanh") { engine.setTouchLatencyBoost(true) }
            }
            BATTERY -> {
                val hz = supportedRates.filter { it <= 60 }.maxOrNull() ?: supportedRates.minOrNull()
                hz?.let { step("Tần số quét") { engine.lockRefreshRate(it.toFloat()) } }
                step("Hiệu năng ổn định") { engine.setSustainedPerformance(false) }
                step("Không giới hạn pin") { engine.setBatteryThrottlingOff(false) }
                step("Cảm ứng nhanh") { engine.setTouchLatencyBoost(false) }
            }
            else -> {
                step("Tần số quét") { engine.resetRefreshRate() }
                step("Hiệu năng ổn định") { engine.setSustainedPerformance(false) }
                step("Không giới hạn pin") { engine.setBatteryThrottlingOff(false) }
            }
        }
        return failures
    }
}
