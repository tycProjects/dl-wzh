package com.johan.jugb185.core

/**
 * JUGB 185 — Rate mapping + Idle intelligence
 * Better than 165 Armer: smarter LTPO, battery-aware
 */
object RateLock {

    enum class RefreshRate(val hz: Int, val vendorId: Int, val label: String, val desc: String) {
        R60(60, 1, "60 Hz", "Battery Saver"),
        R90(90, 2, "90 Hz", "Balanced"),
        R120(120, 3, "120 Hz", "Smooth"),
        R144(144, 4, "144 Hz", "Ultra"),
        R165(165, 5, "165 Hz", "Extreme"),
        R185(185, 7, "185 Hz", "GOD MODE");

        companion object {
            // Vendor ID 0 = DISARM (remove override)
            const val DISARM_ID = 0
            const val DISARM_HZ = 0

            fun fromHz(hz: Int): RefreshRate? = entries.find { it.hz == hz }
            fun fromVendorId(id: Int): RefreshRate? = entries.find { it.vendorId == id }

            // OnePlus 16 can do 185, others cap at 165 (but we offer 185 and run at 165)
            fun availableRates(maxPanelHz: Int): List<RefreshRate> {
                return when {
                    maxPanelHz >= 185 -> entries // All
                    maxPanelHz >= 165 -> entries.filter { it != R185 } + R185 // Show 185 as "runs at 165"
                    else -> entries.filter { it.hz <= maxPanelHz }
                }
            }
        }
    }

    // Idle behavior — JUGB is smarter than Armer:
    // We park to 120Hz for 1Hz idle, but 165/185 stay at 55Hz (can't improve)
    data class IdleInfo(val stillHz: Int, val restoreMs: Int, val note: String)

    fun idleFor(rate: RefreshRate): IdleInfo = when (rate) {
        RefreshRate.R60, RefreshRate.R90, RefreshRate.R120 -> IdleInfo(1, 80, "LTPO 1Hz — max battery")
        RefreshRate.R144 -> IdleInfo(1, 90, "Parks at 120Hz → 1Hz idle")
        RefreshRate.R165 -> IdleInfo(55, 50, "55Hz floor — OnePlus LTPO limit")
        RefreshRate.R185 -> IdleInfo(55, 50, "55Hz floor — panel limit (OP16)")
    }

    // Presets — NEW vs Armer (100x better)
    enum class Preset(val name: String, val hz: Int, val icon: String) {
        GAMING("Gaming Beast", 185, "🎮"),
        ESPORTS("E-Sports", 165, "⚡"),
        VIDEO("Video Smooth", 60, "🎬"),
        SOCIAL("Social Scroll", 120, "📱"),
        BATTERY("Ultra Battery", 60, "🔋")
    }

    fun presetForPackage(pkg: String): Preset {
        val p = pkg.lowercase()
        return when {
            p.contains("game") || p.contains("pubg") || p.contains("codm") || p.contains("mlbb") ||
            p.contains("genshin") || p.contains("mobilelegends") || p.contains("freefire") -> Preset.GAMING
            p.contains("youtube") || p.contains("netflix") || p.contains("video") -> Preset.VIDEO
            p.contains("instagram") || p.contains("tiktok") || p.contains("facebook") -> Preset.SOCIAL
            else -> Preset.GAMING // Default to beast mode
        }
    }
}
