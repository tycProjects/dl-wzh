package com.johan.jugb185.util

import android.os.Build

object DeviceCheck {
    // OnePlus & OPPO exclusive gate
    private val ALLOWED_BRANDS = setOf(
        "oneplus", "oppo", "realme", "one_plus"
    )
    private val ALLOWED_MANUFACTURERS = setOf(
        "oneplus", "oppo", "realme"
    )

    // Model → Max Panel Hz mapping (OnePlus 16 is god tier)
    val MODEL_MAX_HZ = mapOf(
        // OnePlus 16 series - 185Hz beast
        "CPH2751" to 185, // OP16 global
        "PYB110" to 185,  // OP16 China
        "CPH2747" to 165, // OP15 global
        "CPH2749" to 165, // OP15 NA
        "PLK110" to 165,  // OP15 China
        "CPH2767" to 165, // OP15R
        "CPH2793" to 165, // Nord 6
        // OPPO flagships that share oplusscreenmode
        "CPH2581" to 120, // Find X7 (may unlock 144)
        "PJH110" to 120,
        "CPH2449" to 120  // Reno
    )

    fun isSupportedDevice(): Boolean {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val product = Build.PRODUCT.lowercase()
        return brand in ALLOWED_BRANDS || manufacturer in ALLOWED_MANUFACTURERS || product.contains("oneplus") || product.contains("oppo")
    }

    fun getMaxHzForDevice(): Int {
        val model = Build.MODEL.uppercase()
        // Direct map
        MODEL_MAX_HZ[model]?.let { return it }
        // Fallback: OnePlus 16 family → 185, else 165
        return when {
            Build.MODEL.contains("CPH27", ignoreCase = true) -> 185 // OnePlus 16 guess future
            isOnePlus16() -> 185
            isSupportedDevice() -> 165 // Unknown OnePlus/OPPO defaults to 165
            else -> 120
        }
    }

    fun isOnePlus16(): Boolean {
        return Build.MODEL.equals("CPH2751", true) || Build.MODEL.equals("PYB110", true) ||
               Build.PRODUCT.contains("OP5D03", true) // OP16 codename
    }

    fun getDeviceLabel(): String = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})"
    fun getBrand(): String = Build.BRAND
    fun getModel(): String = Build.MODEL
}
