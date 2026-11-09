package com.nomango.browser

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A language the page can be translated into. */
class Lang(val code: String, val native: String, val english: String)

object Langs {
    val all: List<Lang> = listOf(
        Lang("bn", "বাংলা", "Bangla"),
        Lang("en", "English", "English"),
        Lang("hi", "हिन्दी", "Hindi"),
        Lang("ar", "العربية", "Arabic"),
        Lang("ur", "اردو", "Urdu"),
        Lang("ne", "नेपाली", "Nepali"),
        Lang("fr", "Français", "French"),
        Lang("es", "Español", "Spanish"),
        Lang("de", "Deutsch", "German"),
        Lang("pt", "Português", "Portuguese"),
        Lang("ru", "Русский", "Russian"),
        Lang("zh", "中文", "Chinese"),
        Lang("ja", "日本語", "Japanese"),
        Lang("ko", "한국어", "Korean"),
        Lang("tr", "Türkçe", "Turkish"),
        Lang("id", "Bahasa Indonesia", "Indonesian"),
        Lang("ms", "Bahasa Melayu", "Malay"),
        Lang("th", "ไทย", "Thai"),
        Lang("vi", "Tiếng Việt", "Vietnamese"),
        Lang("it", "Italiano", "Italian"),
        Lang("ta", "தமிழ்", "Tamil"),
        Lang("te", "తెలుగు", "Telugu"),
        Lang("mr", "मराठी", "Marathi"),
        Lang("gu", "ગુજરાતી", "Gujarati"),
        Lang("pa", "ਪੰਜਾਬੀ", "Punjabi"),
        Lang("fa", "فارسی", "Persian"),
        Lang("nl", "Nederlands", "Dutch"),
        Lang("pl", "Polski", "Polish"),
        Lang("uk", "Українська", "Ukrainian"),
        Lang("el", "Ελληνικά", "Greek"),
        Lang("he", "עברית", "Hebrew"),
        Lang("sw", "Kiswahili", "Swahili")
    )

    /** English name of a language code such as "es", "pt-BR" or "zh-Hans". */
    fun name(code: String): String {
        val c = code.substringBefore('-').lowercase()
        for (l in all) {
            if (l.code == c) return l.english
        }
        return if (code.isEmpty()) "Unknown" else code
    }

    fun same(a: String, b: String): Boolean =
        a.isNotEmpty() && b.isNotEmpty() && a.substringBefore('-').lowercase() == b.substringBefore('-').lowercase()

    fun ms(code: String): String = if (code == "zh") "zh-Hans" else code
    fun google(code: String): String = if (code == "zh") "zh-CN" else code
}

/** What the user set up in Settings > Translate. [provider] is ms, google or libre. */
class TrConfig(val provider: String, val key: String, val region: String, val endpoint: String) {
    fun ready(): Boolean = when (provider) {
        "google" -> key.isNotBlank()
        "libre" -> endpoint.trim().startsWith("http")
        else -> key.isNotBlank()
    }

    fun providerName(): String = when (provider) {
        "google" -> "Google Cloud Translation"
        "libre" -> "LibreTranslate"
        else -> "Microsoft Translator"
    }
}

class TrResult(val ok: Boolean, val texts: List<String>, val detected: String, val error: String)

/**
 * Talks to the translation service with the official REST API of the chosen provider.
 * Call it from a background thread only.
 */
class Translator(private val cfg: TrConfig) {

    companion object {
        const val RATE = "The translation limit was reached. Try again in a minute."
    }

    fun translate(texts: List<String>, target: String): TrResult {
        if (texts.isEmpty()) return TrResult(true, texts, "", "")
        var last = TrResult(false, emptyList(), "", "Translation failed")
        for (attempt in 0..1) {
            val r = try {
                when (cfg.provider) {
                    "google" -> googleTranslate(texts, target)
                    "libre" -> libreTranslate(texts, target)
                    else -> msTranslate(texts, target)
                }
            } catch (e: Exception) {
                TrResult(false, emptyList(), "", "Could not reach the translation service. Check your internet connection.")
            }
            if (r.ok || r.error != RATE) return r
            last = r
            try {
                Thread.sleep(1600L)
            } catch (e: InterruptedException) {
            }
        }
        return last
    }

    /** Language code of [sample], or an empty string when it cannot be found out. */
    fun detect(sample: String): String {
        if (sample.isBlank()) return ""
        return try {
            when (cfg.provider) {
                "google" -> googleDetect(sample)
                "libre" -> libreDetect(sample)
                else -> msDetect(sample)
            }
        } catch (e: Exception) {
            ""
        }
    }

    // ------------------------------------------------------------------ Microsoft Translator
    private fun msBase(): String {
        val e = cfg.endpoint.trim()
        return if (e.startsWith("http")) e.trimEnd('/') else "https://api.cognitive.microsofttranslator.com"
    }

    private fun msHeaders(): HashMap<String, String> {
        val h = HashMap<String, String>()
        h["Ocp-Apim-Subscription-Key"] = cfg.key.trim()
        if (cfg.region.isNotBlank()) h["Ocp-Apim-Subscription-Region"] = cfg.region.trim()
        return h
    }

    private fun msTranslate(texts: List<String>, target: String): TrResult {
        val arr = JSONArray()
        for (t in texts) arr.put(JSONObject().put("Text", t))
        val url = msBase() + "/translate?api-version=3.0&textType=plain&to=" + URLEncoder.encode(Langs.ms(target), "UTF-8")
        val (code, text) = post(url, arr.toString(), msHeaders())
        if (code !in 200..299) return fail(code, text)
        val res = JSONArray(text)
        val out = ArrayList<String>()
        var det = ""
        for (i in 0 until res.length()) {
            val o = res.getJSONObject(i)
            val tr = o.optJSONArray("translations")
            out.add(if (tr != null && tr.length() > 0) tr.getJSONObject(0).optString("text", texts[i]) else texts[i])
            if (det.isEmpty()) {
                val d = o.optJSONObject("detectedLanguage")
                if (d != null) det = d.optString("language", "")
            }
        }
        if (out.size != texts.size) return TrResult(false, emptyList(), "", "The service returned an unexpected answer.")
        return TrResult(true, out, det, "")
    }

    private fun msDetect(sample: String): String {
        val body = JSONArray().put(JSONObject().put("Text", sample.take(600))).toString()
        val (code, text) = post(msBase() + "/detect?api-version=3.0", body, msHeaders())
        if (code !in 200..299) return ""
        val res = JSONArray(text)
        return if (res.length() > 0) res.getJSONObject(0).optString("language", "") else ""
    }

    // ------------------------------------------------------------------ Google Cloud Translation (Basic, v2)
    private fun googleUrl(path: String): String =
        "https://translation.googleapis.com/language/translate/v2" + path + "?key=" + URLEncoder.encode(cfg.key.trim(), "UTF-8")

    private fun googleTranslate(texts: List<String>, target: String): TrResult {
        val body = JSONObject().put("q", JSONArray(texts)).put("target", Langs.google(target)).put("format", "text")
        val (code, text) = post(googleUrl(""), body.toString(), HashMap())
        if (code !in 200..299) return fail(code, text)
        val tr = JSONObject(text).getJSONObject("data").getJSONArray("translations")
        val out = ArrayList<String>()
        var det = ""
        for (i in 0 until tr.length()) {
            val o = tr.getJSONObject(i)
            out.add(unescape(o.optString("translatedText", texts[i])))
            if (det.isEmpty()) det = o.optString("detectedSourceLanguage", "")
        }
        if (out.size != texts.size) return TrResult(false, emptyList(), "", "The service returned an unexpected answer.")
        return TrResult(true, out, det, "")
    }

    private fun googleDetect(sample: String): String {
        val body = JSONObject().put("q", sample.take(600)).toString()
        val (code, text) = post(googleUrl("/detect"), body, HashMap())
        if (code !in 200..299) return ""
        val d = JSONObject(text).getJSONObject("data").getJSONArray("detections")
        if (d.length() == 0) return ""
        val first = d.getJSONArray(0)
        return if (first.length() > 0) first.getJSONObject(0).optString("language", "") else ""
    }

    // ------------------------------------------------------------------ LibreTranslate
    private fun libreBase(): String = cfg.endpoint.trim().trimEnd('/')

    private fun libreTranslate(texts: List<String>, target: String): TrResult {
        val body = JSONObject().put("q", JSONArray(texts)).put("source", "auto").put("target", target).put("format", "text")
        if (cfg.key.isNotBlank()) body.put("api_key", cfg.key.trim())
        val (code, text) = post(libreBase() + "/translate", body.toString(), HashMap())
        if (code !in 200..299) return fail(code, text)
        val o = JSONObject(text)
        val tt = o.get("translatedText")
        val out = ArrayList<String>()
        if (tt is JSONArray) {
            for (i in 0 until tt.length()) out.add(tt.optString(i, texts[i]))
        } else if (texts.size == 1) {
            out.add(tt.toString())
        }
        if (out.size != texts.size) return TrResult(false, emptyList(), "", "The service returned an unexpected answer.")
        var det = ""
        val d = o.opt("detectedLanguage")
        if (d is JSONObject) det = d.optString("language", "")
        if (d is JSONArray && d.length() > 0) det = d.optJSONObject(0)?.optString("language", "") ?: ""
        return TrResult(true, out, det, "")
    }

    private fun libreDetect(sample: String): String {
        val body = JSONObject().put("q", sample.take(600))
        if (cfg.key.isNotBlank()) body.put("api_key", cfg.key.trim())
        val (code, text) = post(libreBase() + "/detect", body.toString(), HashMap())
        if (code !in 200..299) return ""
        val a = JSONArray(text)
        return if (a.length() > 0) a.getJSONObject(0).optString("language", "") else ""
    }

    // ------------------------------------------------------------------ http
    private fun post(url: String, body: String, headers: Map<String, String>): Pair<Int, String> {
        var con: HttpURLConnection? = null
        try {
            con = URL(url).openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.connectTimeout = 10000
            con.readTimeout = 25000
            con.doOutput = true
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            con.setRequestProperty("Accept", "application/json")
            for ((k, v) in headers) con.setRequestProperty(k, v)
            val data = body.toByteArray(Charsets.UTF_8)
            con.setFixedLengthStreamingMode(data.size)
            con.outputStream.use { it.write(data) }
            val code = con.responseCode
            val stream = if (code in 200..299) con.inputStream else con.errorStream
            val text = if (stream == null) "" else stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return Pair(code, text)
        } finally {
            try {
                con?.disconnect()
            } catch (e: Exception) {
            }
        }
    }

    private fun fail(code: Int, body: String): TrResult {
        var detail = ""
        try {
            val e = JSONObject(body).optJSONObject("error")
            if (e != null) detail = e.optString("message", "")
        } catch (x: Exception) {
        }
        val d = if (detail.isNotEmpty()) " (" + detail.take(110) + ")" else ""
        val msg = when {
            code == 401 || code == 403 -> "The translation key was not accepted. Check the key (and the region for Microsoft) in Settings > Translate." + d
            code == 429 -> RATE
            code == 400 && detail.isNotEmpty() -> detail.take(150)
            else -> "Translation failed (HTTP $code)$d"
        }
        return TrResult(false, emptyList(), "", msg)
    }

    private fun unescape(s: String): String =
        s.replace("&#39;", "'").replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
}
