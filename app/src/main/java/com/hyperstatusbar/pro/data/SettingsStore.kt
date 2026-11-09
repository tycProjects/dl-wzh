package com.hyperstatusbar.pro.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.settingsDataStore by preferencesDataStore("hyper_statusbar_settings")

class SettingsStore(private val context: Context) {
    private val key = stringPreferencesKey("settings_json")

    val settings = context.settingsDataStore.data.map { prefs ->
        decode(prefs[key].orEmpty())
    }

    suspend fun save(value: BarSettings) {
        context.settingsDataStore.edit { it[key] = encode(value) }
    }

    private fun encode(s: BarSettings) = JSONObject().apply {
        put("leftOffsetPx", s.leftOffsetPx); put("rightOffsetPx", s.rightOffsetPx)
        put("iconSpacingDp", s.iconSpacingDp); put("barHeightDp", s.barHeightDp)
        put("opacity", s.opacity.toDouble()); put("iconScale", s.iconScale.toDouble())
        put("clockScale", s.clockScale.toDouble()); put("batteryScale", s.batteryScale.toDouble())
        put("networkScale", s.networkScale.toDouble()); put("iconStyle", s.iconStyle)
        put("colorTheme", s.colorTheme); put("activeProfile", s.activeProfile)
    }.toString()

    private fun decode(raw: String): BarSettings = runCatching {
        val j = JSONObject(raw)
        BarSettings(
            leftOffsetPx = j.optInt("leftOffsetPx", 0),
            rightOffsetPx = j.optInt("rightOffsetPx", 0),
            iconSpacingDp = j.optInt("iconSpacingDp", 6),
            barHeightDp = j.optInt("barHeightDp", 28),
            opacity = j.optDouble("opacity", 1.0).toFloat(),
            iconScale = j.optDouble("iconScale", 1.0).toFloat(),
            clockScale = j.optDouble("clockScale", 1.0).toFloat(),
            batteryScale = j.optDouble("batteryScale", 1.0).toFloat(),
            networkScale = j.optDouble("networkScale", 1.0).toFloat(),
            iconStyle = j.optString("iconStyle", "iOS"),
            colorTheme = j.optString("colorTheme", "Mono"),
            activeProfile = j.optString("activeProfile", "Daily")
        )
    }.getOrDefault(BarSettings())
}
