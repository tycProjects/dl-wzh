package com.ahnaf.ff

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager

object StatsReader {
    fun read(ctx: Context): String {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalMb = mi.totalMem / (1024 * 1024)
        val usedMb = totalMb - mi.availMem / (1024 * 1024)

        val bat: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (bat?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val level = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0

        val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val hz = dm.getDisplay(0)?.refreshRate ?: 0f

        return "RAM: $usedMb / $totalMb MB\n" +
            "TEMP: %.1f°C   BAT: %d%%\n".format(temp, level) +
            "DISPLAY: %.0f Hz".format(hz)
    }
}
