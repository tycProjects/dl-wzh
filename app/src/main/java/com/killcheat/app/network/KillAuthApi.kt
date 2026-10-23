package com.killcheat.app.network

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * KillAuth REST client — calls https://killcheat.onrender.com
 * All calls are blocking; run from a background thread.
 */
object KillAuthApi {

    private const val BASE_URL = "https://killcheat.onrender.com"
    private const val TIMEOUT  = 15_000 // ms

    // ─────────────────────────────────────────────────────────
    //  POST /api/verify.php
    //  Returns full JSON response object from the server.
    // ─────────────────────────────────────────────────────────
    fun verify(
        appId    : String,
        key      : String,
        version  : String = "",
        deviceId : String = ""
    ): JSONObject {
        val payload = JSONObject().apply {
            put("app_id",    appId)
            put("key",       key)
            put("version",   version)
            put("device_id", deviceId)
        }
        return post("$BASE_URL/api/verify.php", payload)
    }

    // ─────────────────────────────────────────────────────────
    //  POST /api/check_update.php
    // ─────────────────────────────────────────────────────────
    fun checkUpdate(appId: String, version: String): JSONObject {
        val payload = JSONObject().apply {
            put("app_id",  appId)
            put("version", version)
        }
        return post("$BASE_URL/api/check_update.php", payload)
    }

    // ─────────────────────────────────────────────────────────
    //  POST /api/notifications.php
    // ─────────────────────────────────────────────────────────
    fun getNotifications(appId: String): JSONObject {
        val payload = JSONObject().apply { put("app_id", appId) }
        return post("$BASE_URL/api/notifications.php", payload)
    }

    // ─────────────────────────────────────────────────────────
    //  Internal HTTP POST helper
    // ─────────────────────────────────────────────────────────
    private fun post(urlStr: String, body: JSONObject): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod     = "POST"
            connectTimeout    = TIMEOUT
            readTimeout       = TIMEOUT
            doOutput          = true
            doInput           = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val raw = stream.bufferedReader().readText()
            return JSONObject(raw)
        } finally {
            conn.disconnect()
        }
    }
}
