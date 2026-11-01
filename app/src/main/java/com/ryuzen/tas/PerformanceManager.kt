package com.ryuzen.tas

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.io.File

class PerformanceManager(private val context: Context) {
    fun getRamUsage(): String {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        val used = info.totalMem - info.availMem
        return "%.2f GB".format(used / 1073741824.0)
    }

    fun getAvailableRam(): String {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return "%.2f GB".format(info.availMem / 1073741824.0)
    }

    fun getThermalStatus(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                else -> "Unknown"
            }
        }
        return "Unavailable on this Android version"
    }

    fun getCpuSummary(): String {
        val cores = Runtime.getRuntime().availableProcessors()
        val frequencies = (0 until cores).mapNotNull { core ->
            val file = File("/sys/devices/system/cpu/cpu$core/cpufreq/scaling_cur_freq")
            runCatching { file.readText().trim().toLong() / 1000 }.getOrNull()
        }
        return if (frequencies.isEmpty()) "$cores cores detected • frequency unavailable"
        else "$cores cores detected • ${frequencies.average().toInt()} MHz average (read-only)"
    }

    fun requestGameMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_GAME_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }

    fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
