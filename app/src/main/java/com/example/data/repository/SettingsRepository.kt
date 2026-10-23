package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jnmf_settings")

enum class ThemeColor(val key: String, val label: String) {
    NEON_GREEN("neon_green", "Neon Green (Default)"),
    ELECTRIC_BLUE("electric_blue", "Electric Blue"),
    CRIMSON_RED("crimson_red", "Crimson Red"),
    MODERN_WHITE("modern_white", "Modern White"),
    VIBRANT_CYAN("vibrant_cyan", "Vibrant Cyan"),
    DEEP_VIOLET("deep_violet", "Deep Violet")
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    CEBUANO("ceb", "Cebuano / Bisaya"),
    INDONESIAN("in", "Bhs Jussarsian (Indonesian)")
}

class SettingsRepository(private val context: Context) {
    private object PreferencesKeys {
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val REVERSE_QUEUE = booleanPreferencesKey("reverse_queue")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode") // 0 = off, 1 = all, 2 = one
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_BASS = intPreferencesKey("eq_bass")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
    }

    val themeColorFlow: Flow<ThemeColor> = context.dataStore.data.map { prefs ->
        val key = prefs[PreferencesKeys.THEME_COLOR] ?: ThemeColor.NEON_GREEN.key
        ThemeColor.values().find { it.key == key } ?: ThemeColor.NEON_GREEN
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val code = prefs[PreferencesKeys.APP_LANGUAGE] ?: AppLanguage.ENGLISH.code
        AppLanguage.values().find { it.code == code } ?: AppLanguage.ENGLISH
    }

    val reverseQueueFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.REVERSE_QUEUE] ?: false
    }

    suspend fun setThemeColor(theme: ThemeColor) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_COLOR] = theme.key
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LANGUAGE] = language.code
        }
    }

    suspend fun setReverseQueue(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REVERSE_QUEUE] = enabled
        }
    }

    suspend fun saveEqSettings(enabled: Boolean, bass: Int, virtualizer: Int, preset: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_ENABLED] = enabled
            prefs[PreferencesKeys.EQ_BASS] = bass
            prefs[PreferencesKeys.EQ_VIRTUALIZER] = virtualizer
            prefs[PreferencesKeys.EQ_PRESET] = preset
        }
    }

    val eqSettingsFlow = context.dataStore.data.map { prefs ->
        EqSavedState(
            enabled = prefs[PreferencesKeys.EQ_ENABLED] ?: true,
            bass = prefs[PreferencesKeys.EQ_BASS] ?: 300,
            virtualizer = prefs[PreferencesKeys.EQ_VIRTUALIZER] ?: 200,
            preset = prefs[PreferencesKeys.EQ_PRESET] ?: "Indo Fullbeat"
        )
    }
}

data class EqSavedState(
    val enabled: Boolean,
    val bass: Int,
    val virtualizer: Int,
    val preset: String
)
