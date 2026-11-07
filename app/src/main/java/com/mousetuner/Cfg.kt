package com.mousetuner

import android.content.Context

object Cfg {
    private fun sp(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    fun b(c: Context, k: String, d: Boolean = false) = sp(c).getBoolean(k, d)
    fun setB(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()
    fun f(c: Context, k: String, d: Float) = sp(c).getFloat(k, d)
    fun setF(c: Context, k: String, v: Float) = sp(c).edit().putFloat(k, v).apply()
    fun i(c: Context, k: String, d: Int) = sp(c).getInt(k, d)
    fun setI(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()

    fun xfr(c: Context) = b(c, "xfr")            // X Animated Frame Rate +
    fun cad(c: Context) = b(c, "cad")            // Control CPI Mouse
    fun dbg(c: Context) = b(c, "dbg")            // Debugging Sensitivity
    fun fps(c: Context) = b(c, "fps")            // Monitoring FPS
    fun lin(c: Context) = b(c, "lin")            // Linear Mouse Movement
    fun noAccel(c: Context) = b(c, "noaccel", true)
    fun auto(c: Context) = b(c, "auto", true)
    fun sens(c: Context) = f(c, "sens", 1f)
    fun sx(c: Context) = f(c, "sx", 1f)
    fun sy(c: Context) = f(c, "sy", 1f)
    fun dpi(c: Context) = i(c, "dpi", 800)
    fun ds(c: Context) = f(c, "ds", 1f)            // độ phân giải game (Game Mode downscale)
    fun hudAlpha(c: Context) = i(c, "hudA", 90)
    fun hudCompact(c: Context) = b(c, "hudc")

    // Thang sensitivity rộng: 0.1× .. 10× (thanh trượt 0..99)
    fun sensOf(p: Int) = 0.1f + p * 0.1f
    fun progOf(v: Float) = Math.round((v - 0.1f) / 0.1f).coerceAtLeast(0)
}
