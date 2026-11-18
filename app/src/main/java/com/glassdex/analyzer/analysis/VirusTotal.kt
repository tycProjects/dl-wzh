package com.glassdex.analyzer.analysis

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks up a file's SHA-256 on VirusTotal (hash only, the file itself is never uploaded).
 * Needs a free API key from virustotal.com, entered in Settings.
 */
object VirusTotal {

    fun lookup(sha256: String, apiKey: String?): VtResult {
        if (apiKey.isNullOrBlank()) return VtResult("not-configured", message = "Add a free VirusTotal API key in Settings for a 70+ engine check.")
        return try {
            val conn = URL("https://www.virustotal.com/api/v3/files/$sha256").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("x-apikey", apiKey.trim())
            when (val code = conn.responseCode) {
                200 -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val stats = JSONObject(body).getJSONObject("data").getJSONObject("attributes").getJSONObject("last_analysis_stats")
                    VtResult(
                        "found",
                        malicious = stats.optInt("malicious"),
                        suspicious = stats.optInt("suspicious"),
                        harmless = stats.optInt("harmless"),
                        undetected = stats.optInt("undetected"),
                        permalink = "https://www.virustotal.com/gui/file/$sha256"
                    )
                }
                404 -> VtResult("unknown", message = "VirusTotal has never seen this exact file.", permalink = "https://www.virustotal.com/gui/file/$sha256")
                401, 403 -> VtResult("error", message = "VirusTotal rejected the API key.")
                429 -> VtResult("error", message = "VirusTotal rate limit reached. Try again in a minute.")
                else -> VtResult("error", message = "VirusTotal returned HTTP $code.")
            }
        } catch (e: Throwable) {
            VtResult("error", message = "Could not reach VirusTotal (${e.javaClass.simpleName}). Offline?")
        }
    }
}
