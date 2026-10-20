package com.qrstudio.app.qr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * جلب أيقونة الموقع (Favicon) من خدمة Google Favicons —
 * أقرب ما يكون لسلوك الويب الأصلي. يعمل عند توفر الإنترنت فقط.
 */
object FaviconFetcher {

    fun extractDomain(input: String): String? {
        var s = input.trim()
        if (s.isEmpty()) return null
        if (!s.contains("://")) s = "https://$s"
        return try {
            URI(s).host?.removePrefix("www.")
        } catch (e: Exception) {
            null
        }
    }

    fun fetch(urlOrDomain: String): Bitmap? = try {
        val domain = extractDomain(urlOrDomain) ?: return null
        val conn = URL("https://www.google.com/s2/favicons?domain=$domain&sz=128").openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doInput = true
        val bmp = BitmapFactory.decodeStream(conn.inputStream)
        conn.disconnect()
        bmp
    } catch (e: Exception) {
        null
    }
}
