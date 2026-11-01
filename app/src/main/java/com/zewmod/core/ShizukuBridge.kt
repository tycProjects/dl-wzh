package com.zewmod.core

import android.util.Log

/**
 * ZewMod — ShizukuBridge (compatibility shim)
 *
 * Shizuku telah digantikan sepenuhnya oleh WirelessAdbBridge.
 * File ini dipertahankan agar tidak ada import error di ILFixPatch.kt
 * dan file lain yang masih referensi ShizukuBridge.
 *
 * Semua call di-delegate ke WirelessAdbBridge.
 */
object ShizukuBridge {

    private const val TAG = "ZewMod.Shizuku"

    // Delegate ke WirelessAdbBridge
    val isConnected: Boolean
        get() = WirelessAdbBridge.isConnected

    fun init() {
        Log.i(TAG, "ShizukuBridge.init() — delegating to WirelessAdbBridge")
        // WirelessAdbBridge.autoConnect() dipanggil dari ModService/MainActivity
    }

    fun destroy() {
        Log.i(TAG, "ShizukuBridge.destroy() — delegating to WirelessAdbBridge")
        WirelessAdbBridge.disconnect()
    }

    fun requestPermission(requestCode: Int = 1001) {
        // Tidak diperlukan untuk Wireless ADB
        Log.i(TAG, "requestPermission() — no-op, Wireless ADB tidak butuh permission ini")
    }

    fun hasPermission(): Boolean {
        return WirelessAdbBridge.isConnected
    }

    fun execCmd(cmd: String): String {
        return WirelessAdbBridge.execCmd(cmd)
    }

    fun injectPatchToGame(patchBytes: ByteArray, gamePackage: String): Boolean {
        return WirelessAdbBridge.injectPatchToGame(patchBytes, gamePackage)
    }
}
