package com.xkeyboard.app

import android.content.Context
import android.content.res.Configuration

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("xkb", Context.MODE_PRIVATE)

    fun int(c: Context, k: String, d: Int) = sp(c).getInt(k, d)
    fun bool(c: Context, k: String, d: Boolean) = sp(c).getBoolean(k, d)
    fun str(c: Context, k: String, d: String = "") = sp(c).getString(k, d) ?: d

    fun putInt(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()
    fun putBool(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()
    fun putStr(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()

    fun haptic(c: Context) = bool(c, "haptic", true)
    fun sound(c: Context) = bool(c, "sound", false)
    fun soundStyle(c: Context) = int(c, "soundStyle", 0) // 0=System, 1=Soft Pop, 2=Mechanical, 3=Custom Audio
    fun customSoundPath(c: Context) = str(c, "customSoundPath", "")
    fun wallpaperPath(c: Context) = str(c, "wallpaperPath", "")
    fun wallpaperDim(c: Context) = int(c, "wallpaperDim", 45).coerceIn(0, 85)
    fun numRow(c: Context) = bool(c, "numRow", false)
    fun autoCap(c: Context) = bool(c, "autoCap", true)
    fun suggest(c: Context) = bool(c, "suggest", true)
    fun heightPercent(c: Context) = int(c, "heightPercent", 100).coerceIn(72, 140)
    fun keyRadius(c: Context) = int(c, "keyRadius", 10).coerceIn(4, 22)
    fun theme(c: Context) = int(c, "theme", 0)
    fun lang(c: Context) = str(c, "lang", "en") // "en" or "bn"
}

data class Theme(
    val name: String,
    val bg: Int,
    val key: Int,
    val special: Int,
    val pressed: Int,
    val text: Int,
    val hint: Int,
    val accent: Int,
    val onAccent: Int,
    val shadow: Int,
    val isGlass: Boolean = false,
    val glassBorder: Int = 0x55FFFFFF,
    val lightweightGlass: Boolean = false
)

object Themes {
    val all = listOf(
        Theme("Midnight", 0xFF16181D.toInt(), 0xFF2A2D35.toInt(), 0xFF20232A.toInt(), 0xFF454A57.toInt(),
            0xFFEDEFF4.toInt(), 0x88EDEFF4.toInt(), 0xFF7C5CFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF0D0E11.toInt()),
        Theme("Light", 0xFFE9ECF1.toInt(), 0xFFFFFFFF.toInt(), 0xFFD3D8E0.toInt(), 0xFFC2C8D3.toInt(),
            0xFF1B1E24.toInt(), 0x881B1E24.toInt(), 0xFF4F46E5.toInt(), 0xFFFFFFFF.toInt(), 0xFFB7BDC8.toInt()),
        Theme("AMOLED", 0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF0F0F0F.toInt(), 0xFF333333.toInt(),
            0xFFFFFFFF.toInt(), 0x88FFFFFF.toInt(), 0xFF00E676.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
        Theme("Ocean", 0xFF0B2A3A.toInt(), 0xFF14465E.toInt(), 0xFF0F3548.toInt(), 0xFF1F6A8C.toInt(),
            0xFFE6F6FF.toInt(), 0x88E6F6FF.toInt(), 0xFF22D3EE.toInt(), 0xFF05222E.toInt(), 0xFF061A25.toInt()),
        Theme("Rose", 0xFF2B1620.toInt(), 0xFF442536.toInt(), 0xFF361C2A.toInt(), 0xFF63364D.toInt(),
            0xFFFFE8F1.toInt(), 0x88FFE8F1.toInt(), 0xFFFF5C8A.toInt(), 0xFFFFFFFF.toInt(), 0xFF1B0C13.toInt()),
        Theme(
            name = "Liquid Glass",
            bg = 0xCC0F1426.toInt(),
            key = 0x3DFFFFFF.toInt(),
            special = 0x26818CF8.toInt(),
            pressed = 0x70FFFFFF.toInt(),
            text = 0xFFF8FAFC.toInt(),
            hint = 0xA8E2E8F0.toInt(),
            accent = 0xFF38BDF8.toInt(),
            onAccent = 0xFF04131C.toInt(),
            shadow = 0x45000000.toInt(),
            isGlass = true,
            glassBorder = 0x88FFFFFF.toInt(),
            lightweightGlass = false
        ),
        Theme(
            name = "Lightweight Liquid Glass",
            bg = 0xE6131722.toInt(),
            key = 0x29FFFFFF.toInt(),
            special = 0x1CFFFFFF.toInt(),
            pressed = 0x4DFFFFFF.toInt(),
            text = 0xFFF1F5F9.toInt(),
            hint = 0x88CBD5E1.toInt(),
            accent = 0xFFA78BFA.toInt(),
            onAccent = 0xFF140B2E.toInt(),
            shadow = 0x00000000,
            isGlass = true,
            glassBorder = 0x4DFFFFFF.toInt(),
            lightweightGlass = true
        ),
        Theme("Cyber Neon", 0xFF090B10.toInt(), 0xFF141824.toInt(), 0xFF10131D.toInt(), 0xFF242B40.toInt(),
            0xFFE2E8F0.toInt(), 0x8894A3B8.toInt(), 0xFFF43F5E.toInt(), 0xFFFFFFFF.toInt(), 0xFF030406.toInt()),
        Theme("Emerald Forest", 0xFF0D211A.toInt(), 0xFF17382C.toInt(), 0xFF122C23.toInt(), 0xFF225241.toInt(),
            0xFFECFDF5.toInt(), 0x88A7F3D0.toInt(), 0xFF10B981.toInt(), 0xFF042016.toInt(), 0xFF07130F.toInt())
    )

    val names = listOf("Auto (follow system)") + all.map { it.name }

    fun resolve(c: Context): Theme {
        val idx = Prefs.theme(c)
        if (idx in 1..all.size) return all[idx - 1]
        val night = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (night) all[0] else all[1]
    }
}
