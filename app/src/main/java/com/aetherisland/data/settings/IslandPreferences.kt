package com.aetherisland.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aether_preferences")

data class IslandSettings(
    val autoDetectCutout: Boolean = true,
    val horizontalOffsetDp: Int = 0,
    val verticalOffsetDp: Int = 0,
    val widthAdjustmentDp: Int = 0,
    val heightAdjustmentDp: Int = 0,
    val glassIntensity: Float = 0.55f,
    val reducedMotion: Boolean = false
)

class IslandPreferences(private val context: Context) {
    private object Keys {
        val autoDetect = booleanPreferencesKey("auto_detect_cutout")
        val horizontal = intPreferencesKey("horizontal_offset_dp")
        val vertical = intPreferencesKey("vertical_offset_dp")
        val width = intPreferencesKey("width_adjustment_dp")
        val height = intPreferencesKey("height_adjustment_dp")
        val glass = floatPreferencesKey("glass_intensity")
        val reducedMotion = booleanPreferencesKey("reduced_motion")
    }

    val settings = context.dataStore.data.map { p ->
        IslandSettings(
            autoDetectCutout = p[Keys.autoDetect] ?: true,
            horizontalOffsetDp = p[Keys.horizontal] ?: 0,
            verticalOffsetDp = p[Keys.vertical] ?: 0,
            widthAdjustmentDp = p[Keys.width] ?: 0,
            heightAdjustmentDp = p[Keys.height] ?: 0,
            glassIntensity = p[Keys.glass] ?: 0.55f,
            reducedMotion = p[Keys.reducedMotion] ?: false
        )
    }

    suspend fun setGlassIntensity(value: Float) {
        context.dataStore.edit { it[Keys.glass] = value.coerceIn(0f, 1f) }
    }
}
