package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ZMapsAccent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zmaps_settings")

class PreferencesRepository(private val context: Context) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val placeListType = Types.newParameterizedType(List::class.java, ZPlace::class.java)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)

    private val placeListAdapter = moshi.adapter<List<ZPlace>>(placeListType)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    companion object {
        val KEY_ACCENT = stringPreferencesKey("accent_color")
        val KEY_LANGUAGE = stringPreferencesKey("language_code")
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches_json")
        val KEY_BOOKMARKS = stringPreferencesKey("bookmarks_json")
    }

    val accentFlow: Flow<ZMapsAccent> = context.dataStore.data.map { preferences ->
        val id = preferences[KEY_ACCENT] ?: ZMapsAccent.GREEN.id
        ZMapsAccent.fromId(id)
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        val code = preferences[KEY_LANGUAGE] ?: AppLanguage.ENGLISH.code
        AppLanguage.fromCode(code)
    }

    val recentSearchesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_RECENT_SEARCHES]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                stringListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val bookmarksFlow: Flow<List<ZPlace>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_BOOKMARKS]
        if (json.isNullOrBlank()) {
            defaultBookmarks()
        } else {
            try {
                placeListAdapter.fromJson(json) ?: defaultBookmarks()
            } catch (e: Exception) {
                defaultBookmarks()
            }
        }
    }

    suspend fun setAccent(accent: ZMapsAccent) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCENT] = accent.id
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { preferences ->
            val json = preferences[KEY_RECENT_SEARCHES]
            val existing = if (json.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    stringListAdapter.fromJson(json) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val updated = (listOf(query.trim()) + existing.filterNot { it.equals(query.trim(), ignoreCase = true) })
                .take(15)
            preferences[KEY_RECENT_SEARCHES] = stringListAdapter.toJson(updated)
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_RECENT_SEARCHES)
        }
    }

    suspend fun toggleBookmark(place: ZPlace) {
        context.dataStore.edit { preferences ->
            val json = preferences[KEY_BOOKMARKS]
            val existing = if (json.isNullOrBlank()) {
                defaultBookmarks()
            } else {
                try {
                    placeListAdapter.fromJson(json) ?: defaultBookmarks()
                } catch (e: Exception) {
                    defaultBookmarks()
                }
            }

            val exists = existing.any { it.id == place.id || (it.latitude == place.latitude && it.longitude == place.longitude) }
            val updated = if (exists) {
                existing.filterNot { it.id == place.id || (it.latitude == place.latitude && it.longitude == place.longitude) }
            } else {
                listOf(place.copy(isBookmarked = true)) + existing
            }
            preferences[KEY_BOOKMARKS] = placeListAdapter.toJson(updated)
        }
    }

    private fun defaultBookmarks(): List<ZPlace> {
        return listOf(
            ZPlace(
                id = "def_1",
                name = "Cyber District HQ",
                address = "101 Neon Way, Neo Tech City",
                latitude = 14.5547,
                longitude = 121.0244,
                category = "headquarters",
                isBookmarked = true
            ),
            ZPlace(
                id = "def_2",
                name = "Metropolis Transit Center",
                address = "Grand Terminal Ave",
                latitude = 14.5839,
                longitude = 121.0583,
                category = "transit",
                isBookmarked = true
            ),
            ZPlace(
                id = "def_3",
                name = "Apex Charging & Fuel Hub",
                address = "Highway Express Sector 4",
                latitude = 14.5350,
                longitude = 121.0120,
                category = "charging",
                isBookmarked = true
            )
        )
    }
}
