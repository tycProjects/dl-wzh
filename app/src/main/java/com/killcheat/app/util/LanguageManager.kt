package com.killcheat.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {

    // All supported locales: code → display name (in that language)
    val LANGUAGES = linkedMapOf(
        "en"    to "English",
        "vi"    to "Tiếng Việt",
        "zh"    to "中文",
        "ar"    to "العربية",
        "ru"    to "Русский",
        "th"    to "ภาษาไทย",
        "id"    to "Bahasa Indonesia",
        "es"    to "Español",
        "pt"    to "Português",
        "tr"    to "Türkçe",
        "hi"    to "हिन्दी"
    )

    private const val PREF_KEY = "app_language"
    private const val PREF_FILE = "killcheat_prefs"

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(PREF_KEY, Locale.getDefault().language) ?: "en"
    }

    fun saveLanguage(context: Context, code: String) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, code).apply()
    }

    fun applyLanguage(context: Context, code: String): Context {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyAndRestart(context: Context, code: String) {
        saveLanguage(context, code)
    }
}
