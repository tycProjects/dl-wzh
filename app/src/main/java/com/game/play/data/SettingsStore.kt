package com.game.play.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class AppSettings(
    /** null = ikut pengaturan sistem */
    val darkMode: Boolean? = null,
    val notifications: Boolean = false,
    val wifiOnly: Boolean = false,
    val userNumber: Int = 0,
    val userName: String = "",
    val photoPath: String = ""
) {
    val defaultName: String get() = "user" + userNumber.coerceAtLeast(1)
    val displayName: String get() = userName.ifBlank { defaultName }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "porstore_settings")

private val KEY_DARK = intPreferencesKey("dark_mode")
private val KEY_NOTIF = booleanPreferencesKey("notifications")
private val KEY_WIFI = booleanPreferencesKey("wifi_only")
private val KEY_USER_NO = intPreferencesKey("user_number")
private val KEY_USER_NAME = stringPreferencesKey("user_name")
private val KEY_PHOTO = stringPreferencesKey("photo_path")

class SettingsStore(private val context: Context) {

    val flow: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            AppSettings(
                darkMode = p[KEY_DARK]?.let { it == 1 },
                notifications = p[KEY_NOTIF] ?: false,
                wifiOnly = p[KEY_WIFI] ?: false,
                userNumber = p[KEY_USER_NO] ?: 0,
                userName = p[KEY_USER_NAME] ?: "",
                photoPath = p[KEY_PHOTO] ?: ""
            )
        }

    /** Nomor pengguna diberikan sekali saat pertama dibuka (user1, user2, ...). */
    suspend fun ensureUser() {
        context.dataStore.edit { p -> if ((p[KEY_USER_NO] ?: 0) == 0) p[KEY_USER_NO] = 1 }
    }

    suspend fun setDark(on: Boolean) { context.dataStore.edit { it[KEY_DARK] = if (on) 1 else 0 } }
    suspend fun setNotifications(on: Boolean) { context.dataStore.edit { it[KEY_NOTIF] = on } }
    suspend fun setWifiOnly(on: Boolean) { context.dataStore.edit { it[KEY_WIFI] = on } }
    suspend fun setUserName(name: String) { context.dataStore.edit { it[KEY_USER_NAME] = name } }
    suspend fun setPhotoPath(path: String) { context.dataStore.edit { it[KEY_PHOTO] = path } }
}
