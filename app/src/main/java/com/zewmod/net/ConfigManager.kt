package com.zewmod.net

import android.content.Context
import android.util.Log
import com.zewmod.core.ILFixPatch
import com.zewmod.core.NativeGate
import com.zewmod.hack.HackState
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * ZewMod — Config Manager
 *
 * Reverse dari network logic di DEX.
 *
 * localConfig.json (bundled di assets):
 *   { "testCodePatch": true, "resetGuest": true }
 *
 *   testCodePatch = true → selalu re-download patch terbaru dari server
 *   resetGuest = true    → reset guest/anonymous session tiap launch
 *
 * Remote config format (JSONObject dari server):
 *   {
 *     "enabled": true,
 *     "patchUrl": "https://...",
 *     "version": "1.0.x",
 *     "features": { "esp": true, "aimbot": false, "proxy": true },
 *     "targetPackage": "com.target.game"
 *   }
 *
 * Server URL di-hardcode dalam libzewmod_native.so (XOR-obfuscated),
 * diekstrak via NativeGate.nativeConfigUrl().
 *
 * HTTP client: java.net.HttpURLConnection + JSONObject (org.json)
 * — Sama persis dengan classes yang ditemukan di DEX.
 */
class ConfigManager(private val context: Context) {

    companion object {
        private const val TAG = "ZewMod.Config"
        private const val PREFS_NAME = "zewmod_config"
        private const val KEY_DEVICE_ID = "device_id"
    }

    /**
     * Fetch remote config + apply ke HackState.
     * Dipanggil sekali saat service init.
     */
    suspend fun fetchAndApply() {
        val localCfg = loadLocalConfig()
        val testPatch = localCfg.optBoolean("testCodePatch", false)
        val resetGuest = localCfg.optBoolean("resetGuest", false)

        if (resetGuest) clearGuestSession()

        val configUrl = try { NativeGate.nativeConfigUrl() } catch (e: Exception) {
            Log.w(TAG, "NativeGate not available, using fallback")
            return
        }

        val remoteCfg = fetchJson(configUrl) ?: run {
            Log.w(TAG, "Remote config fetch failed, using defaults")
            return
        }

        applyConfig(remoteCfg)

        if (testPatch) {
            val patchUrl = try { NativeGate.nativePatchUrl() } catch (e: Exception) { return }
            val patcher = ILFixPatch(context)
            val newPatch = patcher.downloadPatch(patchUrl)
            if (newPatch != null) {
                patcher.cachePatch(newPatch)
                Log.i(TAG, "Fresh patch downloaded: ${newPatch.size} bytes")
            }
        }
    }

    private fun loadLocalConfig(): JSONObject {
        return try {
            val json = context.assets.open("localConfig.json").bufferedReader().readText()
            JSONObject(json)
        } catch (e: Exception) {
            Log.w(TAG, "localConfig.json read failed: ${e.message}")
            JSONObject()
        }
    }

    private fun applyConfig(cfg: JSONObject) {
        // features object
        val features = cfg.optJSONObject("features")
        features?.let {
            HackState.esp.enabled    = it.optBoolean("esp", false)
            HackState.aimbot.enabled = it.optBoolean("aimbot", false)
            HackState.proxy.enabled  = it.optBoolean("proxy", false)
        }

        // target game package
        val pkg = cfg.optString("targetPackage", "")
        if (pkg.isNotBlank()) {
            com.zewmod.core.targetGamePackage = pkg
        }

        Log.i(TAG, "Config applied — esp=${HackState.esp.enabled} " +
            "aim=${HackState.aimbot.enabled} proxy=${HackState.proxy.enabled}")
    }

    private fun fetchJson(url: String): JSONObject? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "ZewMod/1.0")

            // POST body: device info
            val body = JSONObject().apply {
                put("deviceId", getDeviceId())
                put("version", "1.0.0")
            }.toString()
            conn.outputStream.write(body.toByteArray(Charsets.UTF_8))

            if (conn.responseCode == 200) {
                val sw = StringWriter()
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val buf = CharArray(1024)
                var n: Int
                while (reader.read(buf).also { n = it } != -1) sw.write(buf, 0, n)
                conn.disconnect()
                JSONObject(sw.toString())
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchJson error: ${e.message}")
            null
        }
    }

    private fun getDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_ID, null) ?: run {
            val id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            id
        }
    }

    private fun clearGuestSession() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("guest_token")
            .remove("session_id")
            .apply()
        Log.i(TAG, "Guest session cleared")
    }
}
