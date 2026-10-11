package dev.tajim.jarvis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.textLabStore: DataStore<Preferences> by preferencesDataStore(name = "text_lab")

/** Persists the Text Lab draft locally. Plain text, not encrypted. */
class DraftStore(context: Context) {
    private val store = context.applicationContext.textLabStore
    private val key = stringPreferencesKey("draft")

    suspend fun load(): String = runCatching { store.data.first()[key].orEmpty() }.getOrDefault("")
    suspend fun save(text: String) { store.edit { it[key] = text } }
}
