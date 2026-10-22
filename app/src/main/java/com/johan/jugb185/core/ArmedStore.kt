package com.johan.jugb185.core

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jugb_armed")

class ArmedStore(private val ctx: Context) {
    companion object {
        private val ARMED_MAP = stringPreferencesKey("armed_map") // "pkg:vendorId,pkg:vendorId"
        private val GLOBAL_HZ = intPreferencesKey("global_hz")
        private val TURBO_MODE = booleanPreferencesKey("turbo_mode")
        private val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        private val THERMAL_GUARD = booleanPreferencesKey("thermal_guard")
    }

    // Armed = package -> vendorId (e.g. com.pubg -> 7 for 185Hz)
    val armedFlow: Flow<Map<String, Int>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[ARMED_MAP] ?: ""
        if (raw.isBlank()) emptyMap()
        else raw.split(";").filter { it.contains(":") }.associate {
            val (pkg, id) = it.split(":")
            pkg to id.toInt()
        }
    }

    val globalHzFlow: Flow<Int> = ctx.dataStore.data.map { it[GLOBAL_HZ] ?: 185 }

    suspend fun setArmed(pkg: String, vendorId: Int) {
        ctx.dataStore.edit { prefs ->
            val current = (prefs[ARMED_MAP] ?: "").split(";").filter { it.isNotBlank() }.toMutableList()
            current.removeAll { it.startsWith("$pkg:") }
            if (vendorId != RateLock.RefreshRate.DISARM_ID) {
                current.add("$pkg:$vendorId")
            }
            prefs[ARMED_MAP] = current.joinToString(";")
        }
    }

    suspend fun clearAll() {
        ctx.dataStore.edit { it[ARMED_MAP] = "" }
    }

    suspend fun armAll(packages: List<String>, hz: Int) {
        val vid = RateLock.RefreshRate.fromHz(hz)?.vendorId ?: 7
        ctx.dataStore.edit { prefs ->
            prefs[ARMED_MAP] = packages.joinToString(";") { "$it:$vid" }
            prefs[GLOBAL_HZ] = hz
        }
    }

    suspend fun setGlobalHz(hz: Int) {
        ctx.dataStore.edit { it[GLOBAL_HZ] = hz }
    }

    suspend fun setTurbo(enabled: Boolean) = ctx.dataStore.edit { it[TURBO_MODE] = enabled }
    suspend fun setBatterySaver(enabled: Boolean) = ctx.dataStore.edit { it[BATTERY_SAVER] = enabled }
}
