package com.liveisland

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Проверка обновлений. Приложение обращается к сайту только по нажатию кнопки
 * и только для чтения одного JSON-файла. Никакие данные о телефоне не отправляются.
 *
 * Ожидаемый формат release.json на сайте:
 * {
 *   "versionCode": 4,
 *   "versionName": "3.1",
 *   "apkUrl": "https://live-island-download.lovable.app/LiveIsland-v3.1.apk",
 *   "changes": ["пункт 1", "пункт 2"]
 * }
 */
object UpdateChecker {
    const val RELEASE_URL = "https://live-island-download.lovable.app/updates"

  class Info(val newer: Boolean, val versionName: String, val apkUrl: String, val changes: List<String>)

  /**
   * Данные о версии берём из страницы /updates. Они лежат внутри неё в блоке
   * <script id="release-data" type="application/json">{...}</script>.
   * Если страница сама отдаёт чистый JSON — берём его целиком.
   */
  fun extractJson(body: String): String {
    if (body.startsWith("{")) return body
    val m = Regex("""<script[^>]*id=["']release-data["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL).find(body)
      ?: throw IllegalStateException("на странице /updates нет данных о версии (блок release-data)")
    return m.groupValues[1].trim()
  }

  /** Блокирующий вызов: запускать только из фонового потока. Бросает исключение при ошибке. */
  fun fetch(currentCode: Int): Info {
    val conn = URL(RELEASE_URL).openConnection() as HttpURLConnection
    try {
      conn.connectTimeout = 8000
      conn.readTimeout = 8000
      conn.requestMethod = "GET"
      conn.setRequestProperty("Accept", "*/*")
      conn.setRequestProperty("User-Agent", "LiveIsland-Android/4.3")
      conn.instanceFollowRedirects = true
      val code0 = conn.responseCode
      if (code0 != 200) throw IllegalStateException("сервер ответил HTTP $code0 (нет файла release.json?)")
      val body = conn.inputStream.bufferedReader().use { it.readText() }.trim()
      val json = JSONObject(extractJson(body))
      val code = json.getInt("versionCode")
      val apk = json.optString("apkUrl", "")
      val newer = code > currentCode
      if (newer) require(apk.startsWith("https://")) { "Нет безопасной ссылки на APK" }
      val arr = json.optJSONArray("changes")
      val changes = (0 until (arr?.length() ?: 0)).map { arr!!.getString(it) }
      return Info(newer, json.optString("versionName", ""), apk, changes)
    } finally {
      conn.disconnect()
    }
  }
}
