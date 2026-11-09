package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeyboardSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("sv_keyboard_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<KeyboardSettings> = _settingsFlow.asStateFlow()

    fun getSettings(): KeyboardSettings = _settingsFlow.value

    private fun loadSettings(): KeyboardSettings {
        val borderStyleName = prefs.getString("key_border_style", KeyBorderStyle.LIQUID_GLASS.name)
            ?: KeyBorderStyle.LIQUID_GLASS.name
        val themePresetName = prefs.getString("theme_preset", KeyboardThemePreset.LIQUID_GLASS.name)
            ?: KeyboardThemePreset.LIQUID_GLASS.name
        val fontName = prefs.getString("font_family", KeyboardFontFamily.POPPINS.name)
            ?: KeyboardFontFamily.POPPINS.name
        val iconStyleName = prefs.getString("icon_style", KeyboardIconStyle.MODERN_FILLED.name)
            ?: KeyboardIconStyle.MODERN_FILLED.name
        val wallpaperName = prefs.getString("wallpaper", KeyboardWallpaper.AURORA_GLASS.name)
            ?: KeyboardWallpaper.AURORA_GLASS.name
        val keyHeightName = prefs.getString("key_height", KeyHeight.NORMAL.name)
            ?: KeyHeight.NORMAL.name

        return KeyboardSettings(
            keyBorderStyle = runCatching { KeyBorderStyle.valueOf(borderStyleName) }.getOrDefault(KeyBorderStyle.LIQUID_GLASS),
            themePreset = runCatching { KeyboardThemePreset.valueOf(themePresetName) }.getOrDefault(KeyboardThemePreset.LIQUID_GLASS),
            fontFamily = runCatching { KeyboardFontFamily.valueOf(fontName) }.getOrDefault(KeyboardFontFamily.POPPINS),
            iconStyle = runCatching { KeyboardIconStyle.valueOf(iconStyleName) }.getOrDefault(KeyboardIconStyle.MODERN_FILLED),
            wallpaper = runCatching { KeyboardWallpaper.valueOf(wallpaperName) }.getOrDefault(KeyboardWallpaper.AURORA_GLASS),
            hapticEnabled = prefs.getBoolean("haptic_enabled", true),
            hapticStrength = prefs.getInt("haptic_strength", 30),
            soundEnabled = prefs.getBoolean("sound_enabled", false),
            soundVolume = prefs.getFloat("sound_volume", 0.5f),
            autoCorrectEnabled = prefs.getBoolean("auto_correct_enabled", true),
            suggestionsEnabled = prefs.getBoolean("suggestions_enabled", true),
            autoCapitalization = prefs.getBoolean("auto_capitalization", true),
            doubleSpacePeriod = prefs.getBoolean("double_space_period", true),
            numberRowEnabled = prefs.getBoolean("number_row_enabled", false),
            keyHeight = runCatching { KeyHeight.valueOf(keyHeightName) }.getOrDefault(KeyHeight.NORMAL),
            glassOpacity = prefs.getFloat("glass_opacity", 0.35f)
        )
    }

    fun updateSettings(transform: (KeyboardSettings) -> KeyboardSettings) {
        val updated = transform(_settingsFlow.value)
        _settingsFlow.value = updated
        prefs.edit().apply {
            putString("key_border_style", updated.keyBorderStyle.name)
            putString("theme_preset", updated.themePreset.name)
            putString("font_family", updated.fontFamily.name)
            putString("icon_style", updated.iconStyle.name)
            putString("wallpaper", updated.wallpaper.name)
            putBoolean("haptic_enabled", updated.hapticEnabled)
            putInt("haptic_strength", updated.hapticStrength)
            putBoolean("sound_enabled", updated.soundEnabled)
            putFloat("sound_volume", updated.soundVolume)
            putBoolean("auto_correct_enabled", updated.autoCorrectEnabled)
            putBoolean("suggestions_enabled", updated.suggestionsEnabled)
            putBoolean("auto_capitalization", updated.autoCapitalization)
            putBoolean("double_space_period", updated.doubleSpacePeriod)
            putBoolean("number_row_enabled", updated.numberRowEnabled)
            putString("key_height", updated.keyHeight.name)
            putFloat("glass_opacity", updated.glassOpacity)
            apply()
        }
    }

    companion object {
        @Volatile
        private var instance: KeyboardSettingsManager? = null

        fun getInstance(context: Context): KeyboardSettingsManager {
            return instance ?: synchronized(this) {
                instance ?: KeyboardSettingsManager(context).also { instance = it }
            }
        }
    }
}
