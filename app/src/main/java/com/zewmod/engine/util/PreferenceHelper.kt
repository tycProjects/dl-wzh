package com.zewmod.engine.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zewmod_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SELECTED_GAME    = "selected_game"
        const val KEY_SELECTED_MODE    = "selected_mode"
        const val KEY_LAUNCH_MODE      = "launch_mode"
        const val KEY_SELECTED_SERVER  = "selected_server"
        const val KEY_SESSION_TOKEN    = "session_token"
        const val KEY_LAST_FEATURES    = "last_features"
        const val KEY_FIRST_LAUNCH     = "first_launch"
        const val KEY_MENU_OPACITY     = "menu_opacity"
        const val KEY_MENU_SIZE        = "menu_size"
    }

    var selectedGame: String
        get() = prefs.getString(KEY_SELECTED_GAME, "ff") ?: "ff"
        set(v) = prefs.edit().putString(KEY_SELECTED_GAME, v).apply()

    var selectedMode: String
        get() = prefs.getString(KEY_SELECTED_MODE, "CHỌN SẴN") ?: "CHỌN SẴN"
        set(v) = prefs.edit().putString(KEY_SELECTED_MODE, v).apply()

    var launchMode: String
        get() = prefs.getString(KEY_LAUNCH_MODE, "CARD") ?: "CARD"
        set(v) = prefs.edit().putString(KEY_LAUNCH_MODE, v).apply()

    var selectedServer: String
        get() = prefs.getString(KEY_SELECTED_SERVER, "LOCAL") ?: "LOCAL"
        set(v) = prefs.edit().putString(KEY_SELECTED_SERVER, v).apply()

    var sessionToken: String
        get() = prefs.getString(KEY_SESSION_TOKEN, "local") ?: "local"
        set(v) = prefs.edit().putString(KEY_SESSION_TOKEN, v).apply()

    var lastFeatures: String
        get() = prefs.getString(KEY_LAST_FEATURES, "track,head,legit,freeze,scope") ?: "track,head,legit,freeze,scope"
        set(v) = prefs.edit().putString(KEY_LAST_FEATURES, v).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(v) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, v).apply()

    var menuOpacity: Float
        get() = prefs.getFloat(KEY_MENU_OPACITY, 0.9f)
        set(v) = prefs.edit().putFloat(KEY_MENU_OPACITY, v).apply()

    var menuSize: Float
        get() = prefs.getFloat(KEY_MENU_SIZE, 1.0f)
        set(v) = prefs.edit().putFloat(KEY_MENU_SIZE, v).apply()

    // Feature sliders
    fun getFeatureSlider(featureId: String): Float =
        prefs.getFloat("slider_$featureId", 0.5f)

    fun setFeatureSlider(featureId: String, value: Float) =
        prefs.edit().putFloat("slider_$featureId", value).apply()

    fun isFeatureEnabled(featureId: String): Boolean =
        prefs.getBoolean("feat_$featureId", false)

    fun setFeatureEnabled(featureId: String, enabled: Boolean) =
        prefs.edit().putBoolean("feat_$featureId", enabled).apply()

    fun clearAll() = prefs.edit().clear().apply()
}
