package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AutomationFeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mayaPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "maya_control_preferences"
)

class MayaPreferencesRepository(private val context: Context) {

    private object Keys {
        val TAP_AND_LONG_PRESS = booleanPreferencesKey("tap_and_long_press_enabled")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll_enabled")
        val AUTO_SWIPE = booleanPreferencesKey("auto_swipe_enabled")
        val APP_LAUNCH = booleanPreferencesKey("app_launch_enabled")
        val SEQUENCE_EXECUTION = booleanPreferencesKey("sequence_execution_enabled")
        val MAYA_AUTO_SPEAK = booleanPreferencesKey("maya_auto_speak_enabled")
    }

    val featureFlags: Flow<AutomationFeatureFlags> = context.mayaPrefsDataStore.data.map { prefs ->
        AutomationFeatureFlags(
            tapAndLongPressEnabled = prefs[Keys.TAP_AND_LONG_PRESS] ?: true,
            autoScrollEnabled = prefs[Keys.AUTO_SCROLL] ?: true,
            autoSwipeEnabled = prefs[Keys.AUTO_SWIPE] ?: true,
            appLaunchEnabled = prefs[Keys.APP_LAUNCH] ?: true,
            sequenceExecutionEnabled = prefs[Keys.SEQUENCE_EXECUTION] ?: true,
            mayaAutoSpeakEnabled = prefs[Keys.MAYA_AUTO_SPEAK] ?: true
        )
    }

    suspend fun updateFeatureFlags(transform: (AutomationFeatureFlags) -> AutomationFeatureFlags) {
        context.mayaPrefsDataStore.edit { prefs ->
            val current = AutomationFeatureFlags(
                tapAndLongPressEnabled = prefs[Keys.TAP_AND_LONG_PRESS] ?: true,
                autoScrollEnabled = prefs[Keys.AUTO_SCROLL] ?: true,
                autoSwipeEnabled = prefs[Keys.AUTO_SWIPE] ?: true,
                appLaunchEnabled = prefs[Keys.APP_LAUNCH] ?: true,
                sequenceExecutionEnabled = prefs[Keys.SEQUENCE_EXECUTION] ?: true,
                mayaAutoSpeakEnabled = prefs[Keys.MAYA_AUTO_SPEAK] ?: true
            )
            val updated = transform(current)
            prefs[Keys.TAP_AND_LONG_PRESS] = updated.tapAndLongPressEnabled
            prefs[Keys.AUTO_SCROLL] = updated.autoScrollEnabled
            prefs[Keys.AUTO_SWIPE] = updated.autoSwipeEnabled
            prefs[Keys.APP_LAUNCH] = updated.appLaunchEnabled
            prefs[Keys.SEQUENCE_EXECUTION] = updated.sequenceExecutionEnabled
            prefs[Keys.MAYA_AUTO_SPEAK] = updated.mayaAutoSpeakEnabled
        }
    }
}
