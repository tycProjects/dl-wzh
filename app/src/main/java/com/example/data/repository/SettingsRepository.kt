package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.playback.VideoPlaybackMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val Context.dataStore by preferencesDataStore(name = "jnmf_settings")

enum class ThemeColor(val key: String, val label: String) {
    NEON_GREEN("neon_green", "Green (Default)"),
    ELECTRIC_BLUE("electric_blue", "Blue"),
    CRIMSON_RED("crimson_red", "Red"),
    MODERN_WHITE("modern_white", "White"),
    VIBRANT_CYAN("vibrant_cyan", "Cyan"),
    DEEP_VIOLET("deep_violet", "Violet");

    companion object {
        fun fromKey(key: String?): ThemeColor {
            return entries.find { it.key == key } ?: NEON_GREEN
        }
    }
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    CEBUANO("ceb", "Cebuano (Bisaya)"),
    INDONESIAN("in", "Bahasa Indonesia (JUSSARSian)");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
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
        val VIDEO_PLAYBACK_MODE = stringPreferencesKey("video_playback_mode")
    }

    private val syncCachePrefs by lazy {
        context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns persisted [ThemeColor] immediately on first Compose frame without flash.
     */
    fun getInitialThemeColor(): ThemeColor {
        val cached = syncCachePrefs.getString(KEY_SYNC_THEME, null)
        return ThemeColor.fromKey(cached)
    }

    /**
     * Returns persisted [AppLanguage] immediately on first Compose frame without flash.
     */
    fun getInitialLanguage(): AppLanguage {
        val cached = syncCachePrefs.getString(KEY_SYNC_LANGUAGE, null)
        return AppLanguage.fromCode(cached)
    }

    fun getInitialVideoPlaybackMode(): VideoPlaybackMode {
        val cached = syncCachePrefs.getString(KEY_SYNC_VIDEO_MODE, null)
        return VideoPlaybackMode.fromKey(cached)
    }

    val themeColorFlow: Flow<ThemeColor> = context.dataStore.data
        .map { prefs ->
            val key = prefs[PreferencesKeys.THEME_COLOR] ?: getInitialThemeColor().key
            ThemeColor.fromKey(key)
        }
        .onEach { resolved ->
            syncCachePrefs.edit().putString(KEY_SYNC_THEME, resolved.key).apply()
        }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data
        .map { prefs ->
            val code = prefs[PreferencesKeys.APP_LANGUAGE] ?: getInitialLanguage().code
            AppLanguage.fromCode(code)
        }
        .onEach { resolved ->
            syncCachePrefs.edit().putString(KEY_SYNC_LANGUAGE, resolved.code).apply()
        }

    val reverseQueueFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.REVERSE_QUEUE] ?: false
    }

    val videoPlaybackModeFlow: Flow<VideoPlaybackMode> = context.dataStore.data
        .map { prefs ->
            val key = prefs[PreferencesKeys.VIDEO_PLAYBACK_MODE] ?: getInitialVideoPlaybackMode().key
            VideoPlaybackMode.fromKey(key)
        }
        .onEach { resolved ->
            syncCachePrefs.edit().putString(KEY_SYNC_VIDEO_MODE, resolved.key).apply()
        }

    suspend fun setThemeColor(theme: ThemeColor) {
        syncCachePrefs.edit().putString(KEY_SYNC_THEME, theme.key).apply()
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_COLOR] = theme.key
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        syncCachePrefs.edit().putString(KEY_SYNC_LANGUAGE, language.code).apply()
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LANGUAGE] = language.code
        }
    }

    suspend fun setReverseQueue(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REVERSE_QUEUE] = enabled
        }
    }

    suspend fun setVideoPlaybackMode(mode: VideoPlaybackMode) {
        syncCachePrefs.edit().putString(KEY_SYNC_VIDEO_MODE, mode.key).apply()
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.VIDEO_PLAYBACK_MODE] = mode.key
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

    companion object {
        private const val SYNC_CACHE_NAME = "jnmf_sync_boot_cache"
        private const val KEY_SYNC_THEME = "theme_color"
        private const val KEY_SYNC_LANGUAGE = "app_language"
        private const val KEY_SYNC_VIDEO_MODE = "video_playback_mode"

        fun getSavedLanguage(context: Context): AppLanguage {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return AppLanguage.fromCode(prefs.getString(KEY_SYNC_LANGUAGE, null))
        }

        fun getSavedThemeColor(context: Context): ThemeColor {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return ThemeColor.fromKey(prefs.getString(KEY_SYNC_THEME, null))
        }

        fun getSavedVideoPlaybackMode(context: Context): VideoPlaybackMode {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return VideoPlaybackMode.fromKey(prefs.getString(KEY_SYNC_VIDEO_MODE, null))
        }
    }
}

data class EqSavedState(
    val enabled: Boolean,
    val bass: Int,
    val virtualizer: Int,
    val preset: String
)
