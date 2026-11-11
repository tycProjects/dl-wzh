package com.logalyze.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.logalyze.app.ui.theme.Accent
import com.logalyze.app.ui.theme.ThemeMode

class SettingsState(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("logalyze_prefs", Context.MODE_PRIVATE)

    var language by mutableStateOf(LocaleHelper.current(appContext))
        private set
    var themeMode by mutableStateOf(readEnum("theme", ThemeMode.Dark))
        private set
    var accent by mutableStateOf(readEnum("accent", Accent.Cyan))
        private set

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T {
        val name = prefs.getString(key, null) ?: return default
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    fun updateLanguage(code: String) {
        language = code
        LocaleHelper.save(appContext, code)
    }

    fun updateThemeMode(value: ThemeMode) {
        themeMode = value
        prefs.edit().putString("theme", value.name).apply()
    }

    fun updateAccent(value: Accent) {
        accent = value
        prefs.edit().putString("accent", value.name).apply()
    }
}

val LocalSettings = staticCompositionLocalOf<SettingsState> { error("SettingsState not provided") }
