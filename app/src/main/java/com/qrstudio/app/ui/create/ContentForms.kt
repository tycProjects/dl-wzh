package com.qrstudio.app.ui.create

import androidx.compose.ui.text.input.KeyboardType

data class FieldSpec(
    val key: String,
    val label: String,
    val placeholder: String = "",
    val keyboard: KeyboardType = KeyboardType.Text,
    val singleLine: Boolean = true,
    val options: List<Pair<String, String>>? = null,
    val defaultValue: String = "",
)

object ContentForms {

    val socialPlatforms: List<Pair<String, String>> = listOf(
        "instagram" to "إنستجرام",
        "x" to "إكس (تويتر)",
        "facebook" to "فيسبوك",
        "tiktok" to "تيك توك",
        "snapchat" to "سناب شات",
        "youtube" to "يوتيوب",
        "linkedin" to "لينكدإن",
        "telegram" to "تيليجرام",
    )

    val wifiEncryptions: List<Pair<String, String>> = listOf(
        "WPA" to "WPA/WPA2",
        "WEP" to "WEP",
        "Open" to "مفتوح (بدون كلمة سر)",
    )

    fun fieldsFor(type: ContentType): List<FieldSpec> = when (type) {
        ContentType.URL -> listOf(
            FieldSpec("url", "الرابط", "https://example.com", KeyboardType.Uri),
        )
        ContentType.SOCIAL -> listOf(
            FieldSpec("platform", "المنصة", options = socialPlatforms, defaultValue = "instagram"),
            FieldSpec("username", "اسم المستخدم", "username"),
        )
        ContentType.WIFI -> listOf(
            FieldSpec("ssid", "اسم الشبكة (SSID)", "MyNetwork"),
            FieldSpec("password", "كلمة السر", "••••••••", KeyboardType.Password),
            FieldSpec("encryption", "نوع التشفير", options = wifiEncryptions, defaultValue = "WPA"),
        )
        // AUTO-REPAIR: disabled -- unresolved reference 'WHATSAPP' (was: ContentType.WHATSAPP -> listOf()
        ContentType.CONTACT -> listOf(
            FieldSpec("name", "الاسم", "اسم جهة الاتصال"),
            FieldSpec("phone", "رقم الهاتف", "+9665xxxxxxxx", KeyboardType.Phone),
        )
        ContentType.EMAIL -> listOf(
            FieldSpec("to", "البريد الإلكتروني", "name@example.com", KeyboardType.Email),
            FieldSpec("subject", "الموضوع"),
            FieldSpec("body", "النص", singleLine = false),
        )
        ContentType.PHONE -> listOf(
            FieldSpec("number", "رقم الهاتف", "+9665xxxxxxxx", KeyboardType.Phone),
        )
        ContentType.SMS -> listOf(
            FieldSpec("to", "رقم المستلم", "+9665xxxxxxxx", KeyboardType.Phone),
            FieldSpec("body", "نص الرسالة", singleLine = false),
        )
        ContentType.EVENT -> listOf(
            FieldSpec("title", "عنوان الفعالية"),
            FieldSpec("location", "المكان"),
            FieldSpec("start", "البداية", "2026-12-31 18:00"),
            FieldSpec("end", "النهاية", "2026-12-31 21:00"),
        )
        ContentType.GEO -> listOf(
            FieldSpec("lat", "خط العرض", "24.7136", KeyboardType.Decimal),
            FieldSpec("lng", "خط الطول", "46.6753", KeyboardType.Decimal),
            FieldSpec("label", "اسم الموقع (اختياري)"),
        )
        ContentType.TEXT -> listOf(
            FieldSpec("text", "النص", singleLine = false),
        )
    }
}
