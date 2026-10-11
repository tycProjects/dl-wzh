package dev.tajim.jarvis.core

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager

/** Real device signals used to decide whether animation should be limited to 30 fps. */
object PerformanceHints {
    fun shouldCapFrameRate(context: Context): Boolean {
        val app = context.applicationContext
        val lowRam = app.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
        val powerSave = app.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        return lowRam || powerSave
    }
}
