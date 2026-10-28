package com.schedule.app.util

/** توحيد النص العربي للبحث: إزالة التشكيل والتطويل، وتوحيد الألف والتاء المربوطة والياء. */
fun normalizeArabic(input: String): String {
    val sb = StringBuilder(input.length)
    for (c in input.lowercase()) {
        when {
            c in 'ً'..'ٟ' || c == 'ـ' || c == 'ٰ' -> {}
            c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ' -> sb.append('ا')
            c == 'ة' -> sb.append('ه')
            c == 'ى' || c == 'ئ' -> sb.append('ي')
            c == 'ؤ' -> sb.append('و')
            c in '٠'..'٩' -> sb.append('0' + (c - '٠'))
            else -> sb.append(c)
        }
    }
    return sb.toString().trim()
}
