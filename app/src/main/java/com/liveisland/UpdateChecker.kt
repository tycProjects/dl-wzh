package com.liveisland

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Проверка обновлений.
 * Сначала читаем страницу /updates (данные внутри блока release-data).
 * Если на ней данных нет, пробуем файл release.json на том же сайте.
 */
object UpdateChecker {
  const val PAGE_URL = "https://live-island-download.lovable.app/updates"
  const val FALLBACK_URL = "https://live-island-download.lovable.app/release.json"

  class Info(val newer: Boolean, val versionName: String, val apkUrl: String, val changes: List<String>)

  /** Достаёт JSON: либо всё тело (если это JSON), либо содержимое блока release-data */
  fun extractJson(body: String): String {
    if (body.startsWith("{")) return body
    val m = Regex("""<script[^>]*id=["']release-data["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
      .find(body)
    return m?.groupValues?.get(1)?.trim()
      ?: throw IllegalStateException("no version data on the /updates page (release-data block)")
  }

  /** Читает адрес и возвращает текст. Бросает исключение с кодом ответа при ошибке. */
  private fun read(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    try {
      conn.connectTimeout = 8000
      conn.readTimeout = 8000
      conn.requestMethod = "GET"
      conn.instanceFollowRedirects = true
      conn.setRequestProperty("Accept", "*/*")
      conn.setRequestProperty("User-Agent", "LiveIsland-Android/4.3")
      val code = conn.responseCode
      if (code != 200) throw IllegalStateException("server returned HTTP $code")
      return conn.inputStream.bufferedReader().use { it.readText() }.trim()
    } finally {
      conn.disconnect()
    }
  }

  /** Блокирующий вызов: только из фонового потока. */
  fun fetch(currentCode: Int): Info {
    val json = try {
      JSONObject(extractJson(read(PAGE_URL)))
    } catch (first: IllegalStateException) {
      // на странице данных нет — пробуем release.json; если и его нет, показываем исходную причину
      val alt = try { read(FALLBACK_URL) } catch (_: Exception) { throw first }
      JSONObject(extractJson(alt))
    }
    val code = json.getInt("versionCode")
    val apk = json.optString("apkUrl", "")
    val newer = code > currentCode
    if (newer) require(apk.startsWith("https://")) { "no secure APK link" }
    val arr = json.optJSONArray("changes")
    val changes = (0 until (arr?.length() ?: 0)).map { arr!!.getString(it) }
    return Info(newer, json.optString("versionName", ""), apk, changes)
  }
}
