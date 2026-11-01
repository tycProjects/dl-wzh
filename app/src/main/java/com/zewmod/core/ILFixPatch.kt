package com.zewmod.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * ZewMod — IL2CPP Hot-Patch Loader
 *
 * Reverse dari assets/Assembly-CSharp-patch.bytes
 *
 * Format file: ILFix binary patch (dIFix.ILFixInterfaceBridge)
 *   - Assembly-CSharp, Version=0.86.0.518  ← COW (Call of War) build
 *   - Patch methods: ESP, Aimbot, Proxy relay, dll
 *
 * Cara kerja:
 *   1. App load .bytes dari assets atau download update dari server
 *   2. Patch di-inject ke Unity runtime via Shizuku (root-like privilege)
 *   3. Unity IL2CPP execute patch code menggantikan method original
 *
 * Classes yang di-patch (from strings dalam .bytes):
 *   COW.GamePlay.Player         ← player entity
 *   COW.GameFacade              ← game facade (akses ke engine state)
 *   COW.GamePlay.AttackableEntity ← enemy entity
 *   COW.GameSettingData         ← game settings (bisa override FOV, dll)
 *
 * Hook methods:
 *   - get_RootTransform / GetHeadTF     → ESP head tracking
 *   - CurrentLocalPlayer / IsLocalPlayer → self-identification
 *   - SetAimRotation / LookRotation      → aimbot rotation
 *   - LockedAimingCollider               → lock target bone
 *   - WorldToScreenPoint                 → 2D projection untuk box ESP
 *   - UdpClient / IPEndPoint             → proxy UDP relay
 *   - PlayerPrefs.SetInt                 → persist settings
 */
class ILFixPatch(private val context: Context) {

    companion object {
        private const val TAG = "ZewMod.ILFix"
        private const val PATCH_FILENAME = "Assembly-CSharp-patch.bytes"
        private const val PATCH_PREFS = "zewmod_patch"
    }

    /**
     * Load patch dari assets lokal.
     * Ini adalah payload utama yang sudah di-bundle di APK.
     */
    fun loadBundledPatch(): ByteArray {
        return context.assets.open(PATCH_FILENAME).use { it.readBytes() }
    }

    /**
     * Download patch terbaru dari server (via URL dari NativeGate).
     * localConfig.json: { "testCodePatch": true, "resetGuest": true }
     * — testCodePatch=true artinya patch selalu di-reload setiap launch
     * — resetGuest=true artinya clear guest session agar selalu fresh inject
     */
    fun downloadPatch(serverUrl: String): ByteArray? {
        return try {
            val conn = URL(serverUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                conn.inputStream.use { it.readBytes() }
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Patch download failed: ${e.message}")
            null
        }
    }

    /**
     * Tulis patch ke local cache agar bisa di-apply via Shizuku.
     */
    fun cachePatch(data: ByteArray): File {
        val cacheDir = context.cacheDir
        val outFile = File(cacheDir, PATCH_FILENAME)
        FileOutputStream(outFile).use { it.write(data) }
        return outFile
    }

    /**
     * Apply patch lewat Shizuku privilege escalation.
     * Shizuku expose adb-level shell tanpa root permission penuh.
     * Target path di dalam Unity game: /data/data/<game>/files/patch/
     */
    fun applyPatch(patchFile: File, gamePackage: String): Boolean {
        // Shizuku binder call → cp patch ke game data dir
        // dan trigger IL2CPP reload
        return try {
            val targetPath = "/data/data/$gamePackage/files/Assembly-CSharp-patch.bytes"
            val cmd = "cp '${patchFile.absolutePath}' '$targetPath' && chmod 644 '$targetPath'"
            ShizukuBridge.execCmd(cmd)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Patch apply failed: ${e.message}")
            false
        }
    }
}
