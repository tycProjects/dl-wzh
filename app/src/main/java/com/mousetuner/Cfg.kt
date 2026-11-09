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

    fun s(c: Context, k: String, d: String = "") = sp(c).getString(k, d) ?: d
    fun setS(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()

    fun mode(c: Context) = i(c, "mode", 1)          // 0 Tiết kiệm · 1 Cân bằng · 2 MONSTER
    fun hzMode(c: Context) = i(c, "hzm", 0)         // 0 Thông minh · 1 60Hz · 2 Cao nhất
    fun xfr(c: Context) = mode(c) == 2
    fun fps(c: Context) = b(c, "fps")
    fun dbg(c: Context) = b(c, "dbg")
    fun auto(c: Context) = b(c, "auto", true)
    fun ds(c: Context) = f(c, "ds", 1f)             // độ phân giải game
    fun sat(c: Context) = f(c, "sat", 1f)           // độ rực màu
    fun angle(c: Context) = b(c, "angle")
    fun lp(c: Context) = i(c, "lp", 250)            // độ trễ nhấn giữ (ms) cho Vuốt nhạy
    fun hudAlpha(c: Context) = i(c, "hudA", 90)
    fun hudCompact(c: Context) = b(c, "hudc")
}
