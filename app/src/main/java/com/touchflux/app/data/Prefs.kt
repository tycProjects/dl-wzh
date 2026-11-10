package com.touchflux.app.data

import android.content.Context
import android.content.SharedPreferences
import com.touchflux.app.fx.EffectType
import com.touchflux.app.fx.FxConfig
import com.touchflux.app.fx.Palette

/** Persists [FxConfig]. All surfaces (app, wallpaper, everywhere-mode) share one instance of the file. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("touchflux", Context.MODE_PRIVATE)

    fun config(): FxConfig {
        val d = FxConfig.DEFAULT
        return FxConfig(
            effect = EffectType.values().getOrElse(sp.getInt(K_EFFECT, d.effect.ordinal)) { d.effect },
            palette = Palette.values().getOrElse(sp.getInt(K_PALETTE, d.palette.ordinal)) { d.palette },
            size = sp.getFloat(K_SIZE, d.size),
            duration = sp.getFloat(K_DURATION, d.duration),
            intensity = sp.getFloat(K_INTENSITY, d.intensity),
            haptics = sp.getBoolean(K_HAPTICS, d.haptics),
            trails = sp.getBoolean(K_TRAILS, d.trails),
            geometry = sp.getBoolean(K_GEOMETRY, d.geometry)
        )
    }

    fun save(c: FxConfig) {
        sp.edit()
            .putInt(K_EFFECT, c.effect.ordinal)
            .putInt(K_PALETTE, c.palette.ordinal)
            .putFloat(K_SIZE, c.size)
            .putFloat(K_DURATION, c.duration)
            .putFloat(K_INTENSITY, c.intensity)
            .putBoolean(K_HAPTICS, c.haptics)
            .putBoolean(K_TRAILS, c.trails)
            .putBoolean(K_GEOMETRY, c.geometry)
            .apply()
    }

    private companion object {
        const val K_EFFECT = "effect"
        const val K_PALETTE = "palette"
        const val K_SIZE = "size"
        const val K_DURATION = "duration"
        const val K_INTENSITY = "intensity"
        const val K_HAPTICS = "haptics"
        const val K_TRAILS = "trails"
        const val K_GEOMETRY = "geometry"
    }
}
