package com.droidstore.app.data

import com.droidstore.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiException(val status: Int, message: String) : Exception(message)

object Api {
    private val base = BuildConfig.API_BASE_URL.trimEnd('/')
    @Volatile var token: String? = null

    fun imageUrl(id: Int?): String? = id?.let { "$base/api/images/$it" }
    fun downloadUrl(appId: Int): String = "$base/api/apps/$appId/download"

    private fun call(
        method: String,
        path: String,
        body: ByteArray? = null,
        contentType: String? = "application/json",
        query: Map<String, String> = emptyMap(),
    ): JSONObject {
        val qs = if (query.isEmpty()) "" else "?" + query.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
        val conn = (URL(base + path + qs).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 120_000
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (contentType != null) setRequestProperty("Content-Type", contentType)
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body) }
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val json = if (text.isBlank()) JSONObject() else JSONObject(text)
        if (code !in 200..299) throw ApiException(code, json.optString("error", "HTTP $code"))
        return json
    }

    private fun jsonBody(o: JSONObject) = o.toString().toByteArray(Charsets.UTF_8)

    // ----- Аутентификация -----
    /** Старт входа через Telegram: сервер возвращает ссылку на бота и идентификатор сессии. */
    fun telegramStart(): JSONObject = call("POST", "/api/auth/telegram/start")

    /** Проверка статуса входа: pending, либо ok с токеном после подтверждения в боте. */
    fun telegramStatus(sid: String): JSONObject = call("GET", "/api/auth/telegram/status", query = mapOf("sid" to sid))

    fun me(): JSONObject = call("GET", "/api/me")
    fun logout() { runCatching { call("POST", "/api/auth/logout") } }

    // ----- Каталог -----
    fun catalog(query: String, sort: String): List<AppItem> {
        val q = mutableMapOf("sort" to sort)
        if (query.isNotBlank()) q["q"] = query
        return call("GET", "/api/apps", query = q).getJSONArray("apps").map { AppItem.from(it) }
    }

    fun appDetail(id: Int): AppItem = AppItem.from(call("GET", "/api/apps/$id").getJSONObject("app"))

    fun ratings(id: Int): Pair<RatingSummary, List<Review>> {
        val o = call("GET", "/api/apps/$id/ratings")
        return RatingSummary.from(o.getJSONObject("summary")) to o.getJSONArray("ratings").map { Review.from(it) }
    }

    fun rate(id: Int, stars: Int, comment: String): RatingSummary =
        RatingSummary.from(call("POST", "/api/apps/$id/ratings", jsonBody(JSONObject().put("stars", stars).put("comment", comment))).getJSONObject("summary"))

    fun report(id: Int, reason: String, description: String) {
        call("POST", "/api/apps/$id/reports", jsonBody(JSONObject().put("reason", reason).put("description", description)))
    }

    // ----- Студии и публикация -----
    fun myStudios(): List<Studio> =
        call("GET", "/api/studios/mine").getJSONArray("studios").map { Studio.from(it) }

    fun createStudio(name: String, description: String, email: String, website: String): Studio =
        Studio.from(call("POST", "/api/studios", jsonBody(
            JSONObject().put("name", name).put("description", description).put("contactEmail", email).put("website", website)
        )).getJSONObject("studio"))

    fun studioApps(studioId: Int): List<AppItem> =
        call("GET", "/api/studios/$studioId/apps").getJSONArray("apps").map { AppItem.from(it) }

    fun createApp(studioId: Int, form: AppForm): AppItem =
        AppItem.from(call("POST", "/api/studios/$studioId/apps", jsonBody(form.toJson())).getJSONObject("app"))

    fun updateApp(appId: Int, form: AppForm, iconId: Int?, screenshotIds: List<Int>): AppItem {
        val o = form.toJson()
        iconId?.let { o.put("iconId", it) }
        o.put("screenshotIds", JSONArray(screenshotIds))
        return AppItem.from(call("PATCH", "/api/apps/$appId", jsonBody(o)).getJSONObject("app"))
    }

    fun uploadApk(appId: Int, bytes: ByteArray, versionCode: Int, versionName: String, notes: String): JSONObject =
        call(
            "POST", "/api/apps/$appId/versions", bytes, "application/vnd.android.package-archive",
            mapOf("versionCode" to versionCode.toString(), "versionName" to versionName, "releaseNotes" to notes),
        )

    fun uploadImage(kind: String, bytes: ByteArray, mime: String): Int =
        call("POST", "/api/images", bytes, mime, mapOf("kind" to kind)).getJSONObject("image").getInt("id")

    fun submit(appId: Int): AppItem = AppItem.from(call("POST", "/api/apps/$appId/submit").getJSONObject("app"))
}
