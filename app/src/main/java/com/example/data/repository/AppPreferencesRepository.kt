package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nexora_prefs")

class AppPreferencesRepository(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val ACTIVE_OUTLET_KEY = stringPreferencesKey("active_outlet_id")
    private val ACTIVE_STAFF_KEY = stringPreferencesKey("active_staff_id")

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    val activeOutletId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_OUTLET_KEY]
    }

    val activeStaffId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_STAFF_KEY]
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs -> prefs[THEME_KEY] = mode.name }
    }

    suspend fun setActiveOutletId(outletId: String) {
        context.dataStore.edit { prefs -> prefs[ACTIVE_OUTLET_KEY] = outletId }
    }

    suspend fun setActiveStaffId(staffId: String) {
        context.dataStore.edit { prefs -> prefs[ACTIVE_STAFF_KEY] = staffId }
    }
}
