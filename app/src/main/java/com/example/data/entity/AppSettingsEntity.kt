package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AppLanguage
import com.example.model.AppPreferences
import com.example.model.AppUiColor
import com.example.model.EqualizerPresetType

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val bassBoostStrength: Int = 500,
    val stereoWidthStrength: Int = 600,
    val depth3DStrength: Int = 450,
    val clarityStrength: Int = 550,
    val limiterEnabled: Boolean = true,
    val twsHeadsetMode: Boolean = true,
    val eqEnabled: Boolean = true,
    val eqPreset: String = "INDO_REMIX",
    val pauseOnUnplug: Boolean = true,
    val uiColor: String = "GREEN",
    val language: String = "ENGLISH"
) {
    fun toPreferences(): AppPreferences = AppPreferences(
        bassBoostStrength = bassBoostStrength,
        stereoWidthStrength = stereoWidthStrength,
        depth3DStrength = depth3DStrength,
        clarityStrength = clarityStrength,
        limiterEnabled = limiterEnabled,
        twsHeadsetMode = twsHeadsetMode,
        eqEnabled = eqEnabled,
        eqPreset = EqualizerPresetType.fromName(eqPreset),
        pauseOnUnplug = pauseOnUnplug,
        uiColor = AppUiColor.fromId(uiColor),
        language = AppLanguage.fromCode(language)
    )

    companion object {
        fun fromPreferences(prefs: AppPreferences): AppSettingsEntity = AppSettingsEntity(
            id = 1,
            bassBoostStrength = prefs.bassBoostStrength,
            stereoWidthStrength = prefs.stereoWidthStrength,
            depth3DStrength = prefs.depth3DStrength,
            clarityStrength = prefs.clarityStrength,
            limiterEnabled = prefs.limiterEnabled,
            twsHeadsetMode = prefs.twsHeadsetMode,
            eqEnabled = prefs.eqEnabled,
            eqPreset = prefs.eqPreset.name,
            pauseOnUnplug = prefs.pauseOnUnplug,
            uiColor = prefs.uiColor.name,
            language = prefs.language.name
        )
    }
}
