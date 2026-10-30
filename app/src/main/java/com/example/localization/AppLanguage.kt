package com.example.localization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class LanguageCode(val code: String, val displayName: String, val nativeName: String) {
    BN("bn", "Bengali", "বাং"),
    EN("en", "English", "EN")
}

object LanguageManager {
    // Default to Bengali as requested by the user
    var currentLanguage by mutableStateOf(LanguageCode.BN)

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == LanguageCode.BN) LanguageCode.EN else LanguageCode.BN
    }

    fun isBengali(): Boolean = currentLanguage == LanguageCode.BN

    fun get(key: String): String {
        val strings = if (isBengali()) BENGALI_STRINGS else ENGLISH_STRINGS
        return strings[key] ?: ENGLISH_STRINGS[key] ?: key
    }

    fun format(key: String, vararg args: Any): String {
        val template = get(key)
        return try {
            String.format(template, *args)
        } catch (_: Exception) {
            template
        }
    }

    private val ENGLISH_STRINGS = mapOf(
        "app_title" to "Zonex Emoji",
        "main_heading" to "Secret Message Inside Emoji",
        "main_subtitle" to "Invisibly encode your message inside a single emoji or letter. No server or internet connection required — everything happens on your device.",
        "tab_encode" to "Encode",
        "tab_decode" to "Decode",
        "step_write_msg" to "Write message",
        "step_select_carrier" to "Select carrier",
        "step_result" to "Result",
        "step_paste_encoded" to "Paste encoded text",
        "step_decoded_msg" to "Decoded message",
        "carrier_emoji" to "Emoji",
        "carrier_char" to "Letter",
        "carrier_custom" to "Custom Emoji",
        "custom_emoji_hint" to "Type or paste your own emoji here...",
        "input_hint" to "Write message here...",
        "ready_to_send" to "Ready to send",
        "byte_summary" to "%d byte message • total %d characters",
        "btn_copy_result" to "Copy Result",
        "btn_copy_message" to "Copy Message",
        "btn_clear" to "Clear",
        "btn_paste" to "Paste",
        "disclaimer_text" to "ⓘ Some apps might strip invisible characters. Verify by decoding before sending important messages.",
        "footer_offline" to "● Completely offline · No data saved or sent",
        "found_characters" to "● Found • %d characters",
        "not_found" to "No hidden message found",
        "copied_toast" to "Copied to clipboard!",
        "pasted_toast" to "Pasted from clipboard",
        "empty_input_warning" to "Please write a message to encode",
        "floating_window" to "Floating Window",
        "launch_floating" to "Open Floating Window"
    )

    private val BENGALI_STRINGS = mapOf(
        "app_title" to "Zonex Emoji",
        "main_heading" to "ইমোজির ভেতরে গোপন বার্তা",
        "main_subtitle" to "আপনার বার্তা একটি ইমোজি বা অক্ষরের ভেতরে অদৃশ্যভাবে এনকোড করুন। কোনো সার্ভার বা ইন্টারনেট সংযোগ প্রয়োজন হয় না — সবকিছু আপনার ডিভাইসেই সম্পন্ন হয়।",
        "tab_encode" to "এনকোড",
        "tab_decode" to "ডিকোড",
        "step_write_msg" to "বার্তা লিখুন",
        "step_select_carrier" to "বাহক নির্বাচন করুন",
        "step_result" to "ফলাফল",
        "step_paste_encoded" to "এনকোড করা টেক্সট পেস্ট করুন",
        "step_decoded_msg" to "ডিকোড করা বার্তা",
        "carrier_emoji" to "ইমোজি",
        "carrier_char" to "অক্ষর",
        "carrier_custom" to "নিজের মতো ইমুজি",
        "custom_emoji_hint" to "নিজের পছন্দমতো যেকোনো ইমোজি লিখুন...",
        "input_hint" to "এখানে বার্তা লিখুন...",
        "ready_to_send" to "পাঠানোর জন্য প্রস্তুত",
        "byte_summary" to "%d বাইট বার্তা • মোট %d ক্যারেক্টার",
        "btn_copy_result" to "ফলাফল কপি করুন",
        "btn_copy_message" to "বার্তা কপি করুন",
        "btn_clear" to "মুছে ফেলুন",
        "btn_paste" to "পেস্ট",
        "disclaimer_text" to "ⓘ কিছু অ্যাপ অদৃশ্য ক্যারেক্টার বাদ দিতে পারে। গুরুত্বপূর্ণ বার্তা পাঠানোর আগে ডিকোড করে যাচাই করে নিন।",
        "footer_offline" to "● সম্পূর্ণ অফলাইন · কোনো ডেটা সংরক্ষণ বা প্রেরণ করা হয় না",
        "found_characters" to "● পাওয়া গেছে • %d অক্ষর",
        "not_found" to "কোনো গোপন বার্তা পাওয়া যায়নি",
        "copied_toast" to "ক্লিপবোর্ডে কপি করা হয়েছে!",
        "pasted_toast" to "ক্লিপবোর্ড থেকে পেস্ট করা হয়েছে",
        "empty_input_warning" to "এনকোড করার জন্য একটি বার্তা লিখুন",
        "floating_window" to "ভাসমান উইন্ডো",
        "launch_floating" to "ভাসমান উইন্ডো খুলুন"
    )
}
