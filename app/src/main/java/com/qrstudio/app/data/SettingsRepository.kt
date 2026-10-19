package com.qrstudio.app.data

import android.content.SharedPreferences

class SettingsRepository(private val prefs: SharedPreferences) {

    var defaultSize: Int
        get() = prefs.getInt(KEY_SIZE, 600)
        set(value) = prefs.edit().putInt(KEY_SIZE, value).apply()

    var errorCorrection: String
        get() = prefs.getString(KEY_EC, "M") ?: "M"
        set(value) = prefs.edit().putString(KEY_EC, value).apply()

    var sound: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var vibrate: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    fun reset() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_SIZE = "default_size"
        const val KEY_EC = "default_ec"
        const val KEY_SOUND = "sound"
        const val KEY_VIBRATE = "vibrate"
    }
}
