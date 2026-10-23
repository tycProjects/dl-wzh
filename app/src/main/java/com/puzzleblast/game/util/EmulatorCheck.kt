package com.puzzleblast.game.util

import android.os.Build
import java.io.File

object EmulatorCheck {
    fun detected(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        if (fp.contains("generic") || fp.contains("emulator") || fp.contains("sdk_gphone")) return true
        if (Build.MODEL.contains("sdk", true) || Build.MODEL.contains("emulator", true) || Build.MODEL.contains("Android SDK built for x86", true)) return true
        if (Build.MANUFACTURER.contains("Genymotion", true)) return true
        if (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) return true
        if ("google_sdk" == Build.PRODUCT || "sdk_google" == Build.PRODUCT) return true
        if (File("/dev/goldfish_pipe").exists() || File("/dev/qemu_pipe").exists()) return true
        if (File("/system/lib/libc_malloc_debug_qemu.so").exists()) return true
        try {
            val f = File("/proc/cpuinfo")
            if (f.exists() && f.readText().contains("goldfish", true)) return true
        } catch (_: Exception) {}
        return false
    }
}
