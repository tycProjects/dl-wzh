package com.logalyze.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {
    private const val PREFS = "logalyze_prefs"
    private const val KEY = "lang"

    private fun saved(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    /** "ar" or "en". Uses the saved choice, otherwise the device language. */
    fun current(context: Context): String =
        saved(context) ?: if (Locale.getDefault().language == "ar") "ar" else "en"

    fun save(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, lang).apply()
    }

    /** Used at startup so system-level strings follow the saved language. */
    fun wrap(context: Context): Context {
        val lang = saved(context) ?: return context
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    private class LocalizedContext(base: Context, private val res: Resources) : ContextWrapper(base) {
        override fun getResources(): Resources = res
    }

    /**
     * A context whose resources use [lang]. It still wraps the activity,
     * so file pickers and back handling keep working. Lets the app switch
     * language live, without restarting the activity.
     */
    fun contextFor(base: Context, lang: String): Context {
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return LocalizedContext(base, base.createConfigurationContext(config).resources)
    }
}
