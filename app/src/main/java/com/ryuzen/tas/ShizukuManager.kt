package com.ryuzen.tas

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuManager(private val context: Context) {
    fun isConnected(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean =
        isConnected() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun requestPermission() {
        if (!isConnected()) return
        if (!hasPermission()) Shizuku.requestPermission(1001)
    }
}
