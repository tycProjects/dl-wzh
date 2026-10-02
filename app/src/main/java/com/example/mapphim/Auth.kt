package com.example.mapphim

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Kích hoạt bằng key qua máy chủ của bạn (API_BASE trong app/build.gradle.kts).
 * API_BASE trống = chế độ thử, không cần key.
 */
object Auth {
    fun online() = BuildConfig.API_BASE.isNotBlank()
    private fun main(c: Context) = c.getSharedPreferences("mapphim", Context.MODE_PRIVATE)
    private fun deviceId(c: Context) = Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: ""

    fun useTrial(c: Context) {
        if (main(c).getString("user", null) == null) main(c).edit().putString("user", "trial").apply()
    }

    fun activate(c: Context, key: String, done: (String?) -> Unit) {
        io(done) {
            val body = JSONObject().put("key", key).put("device_id", deviceId(c))
            val (code, j) = post("/license/activate", body, null)
            val token = j?.optString("token").orEmpty()
            if (code in 200..299 && token.isNotEmpty()) {
                main(c).edit().putString("user", "license").putString("token", token).putString("license_key", key)
                    .putLong("expires", j!!.optLong("expires_at", 0L)).apply()
                null
            } else j?.optString("error")?.takeIf { it.isNotEmpty() } ?: "Yêu cầu thất bại (mã $code)."
        }
    }

    /** Mất mạng: giữ nguyên phiên. Máy chủ trả 401/403: done(false). */
    fun verify(c: Context, done: (Boolean) -> Unit) {
        val t = main(c).getString("token", null)
        if (!online() || t == null) { done(true); return }
        Thread {
            val ok = try { post("/auth/verify", JSONObject(), t).first.let { it != 401 && it != 403 } } catch (e: Exception) { true }
            Handler(Looper.getMainLooper()).post { done(ok) }
        }.start()
    }

    fun user(c: Context): String? {
        val p = main(c)
        val u = p.getString("user", null) ?: return null
        val e = p.getLong("expires", 0L)
        return if (e > 0 && e < System.currentTimeMillis() / 1000) null else u
    }

    fun logout(c: Context) { main(c).edit().remove("user").remove("token").remove("expires").remove("license_key").apply() }

    private fun io(done: (String?) -> Unit, work: () -> String?) {
        Thread {
            val r = try { work() }
            catch (e: IOException) { "Không kết nối được máy chủ." }
            catch (e: Exception) { "Lỗi: ${e.message}" }
            Handler(Looper.getMainLooper()).post { done(r) }
        }.start()
    }

    private fun post(path: String, body: JSONObject, token: String?): Pair<Int, JSONObject?> {
        val conn = URL(BuildConfig.API_BASE.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val txt = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return code to (try { JSONObject(txt) } catch (e: Exception) { null })
        } finally { conn.disconnect() }
    }
}
