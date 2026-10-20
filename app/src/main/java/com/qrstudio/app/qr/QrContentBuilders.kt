package com.qrstudio.app.qr

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * دوال صافية (Pure JVM) لبناء الصيغ النهائية لكل نوع محتوى — قابلة للاختبار الوحدوي.
 */
object QrContentBuilders {

    fun enc(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun url(link: String): String = link.trim()

    fun social(platformKey: String, username: String): String {
        val base = when (platformKey) {
            "instagram" -> "https://instagram.com/"
            "x" -> "https://x.com/"
            "facebook" -> "https://facebook.com/"
            "tiktok" -> "https://www.tiktok.com/@"
            "snapchat" -> "https://snapchat.com/add/"
            "youtube" -> "https://youtube.com/@"
            "linkedin" -> "https://linkedin.com/in/"
            "telegram" -> "https://t.me/"
            else -> "https://instagram.com/"
        }
        return base + username.trim().removePrefix("@")
    }

    private fun wifiEscape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace(":", "\\:")

    fun wifi(ssid: String, password: String, encryption: String): String {
        val type = if (encryption.equals("Open", ignoreCase = true)) "nopass" else encryption.uppercase(Locale.US)
        return "WIFI:S:" + wifiEscape(ssid) + ";T:" + type + ";P:" + wifiEscape(password) + ";;"
    }

    fun whatsapp(number: String, message: String): String {
        val digits = number.filter { it.isDigit() }
        val base = "https://wa.me/$digits"
        return if (message.isBlank()) base else base + "?text=" + enc(message)
    }

    fun contact(name: String, phone: String): String = buildString {
        append("BEGIN:VCARD\nVERSION:3.0\n")
        append("FN:").append(name).append("\n")
        if (phone.isNotBlank()) append("TEL:").append(phone).append("\n")
        append("END:VCARD")
    }

    fun email(to: String, subject: String, body: String): String {
        val base = "mailto:" + to.trim()
        val params = mutableListOf<String>()
        if (subject.isNotBlank()) params.add("subject=" + enc(subject))
        if (body.isNotBlank()) params.add("body=" + enc(body))
        return if (params.isEmpty()) base else base + "?" + params.joinToString("&")
    }

    fun phone(number: String): String = "tel:" + number.trim()

    fun sms(to: String, body: String): String {
        val base = "sms:" + to.trim()
        return if (body.isBlank()) base else base + "?body=" + enc(body)
    }

    private fun eventDate(input: String): String = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(input)?.let {
            SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).format(it)
        } ?: input
    } catch (e: Exception) {
        input
    }

    fun event(title: String, location: String, start: String, end: String): String = buildString {
        append("BEGIN:VEVENT\n")
        append("SUMMARY:").append(title).append("\n")
        if (location.isNotBlank()) append("LOCATION:").append(location).append("\n")
        if (start.isNotBlank()) append("DTSTART:").append(eventDate(start)).append("\n")
        if (end.isNotBlank()) append("DTEND:").append(eventDate(end)).append("\n")
        append("END:VEVENT")
    }

    fun geo(lat: String, lng: String, label: String): String {
        val base = "geo:" + lat.trim() + "," + lng.trim()
        return if (label.isBlank()) base else base + "?q=" + lat.trim() + "," + lng.trim() + "(" + enc(label) + ")"
    }

    fun text(value: String): String = value

    /**
     * يبني الحمولة النهائية من نوع المحتوى (اسم ثابت) وحقوله.
     * الأسماء ثوابت نصية حتى تبقى الدوال قابلة للاختبار دون واجهات Compose.
     */
    fun build(typeName: String, fields: Map<String, String>): String {
        val f = { key: String -> fields[key].orEmpty() }
        return when (typeName) {
            "URL" -> url(f("url"))
            "SOCIAL" -> social(f("platform"), f("username"))
            "WIFI" -> wifi(f("ssid"), f("password"), f("encryption"))
            "WHATSAPP" -> whatsapp(f("number"), f("message"))
            "CONTACT" -> contact(f("name"), f("phone"))
            "EMAIL" -> email(f("to"), f("subject"), f("body"))
            "PHONE" -> phone(f("number"))
            "SMS" -> sms(f("to"), f("body"))
            "EVENT" -> event(f("title"), f("location"), f("start"), f("end"))
            "GEO" -> geo(f("lat"), f("lng"), f("label"))
            "TEXT" -> text(f("text"))
            else -> ""
        }
    }
}
