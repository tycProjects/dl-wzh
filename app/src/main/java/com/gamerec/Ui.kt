package com.gamerec

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import java.io.File

object Ui {
    const val MATCH = -1
    const val WRAP = -2
    val BG = 0xFF0B0D17.toInt(); val CARD = 0xFF151A2E.toInt(); val LINE = 0xFF262D4D.toInt()
    val TX = 0xFFEEF0FF.toInt(); val MUT = 0xFF8E95BD.toInt(); val OK = 0xFF3DDC97.toInt(); val RED = 0xFFFF4D5E.toInt()
    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()
    fun round(c: Context, fill: Int, r: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(c, r).toFloat(); if (stroke != 0) setStroke(dp(c, 1), stroke)
    }
    fun grad(c: Context, a: Int, b: Int, r: Int) =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(a, b)).apply { cornerRadius = dp(c, r).toFloat() }
}

object Cfg {
    private lateinit var sp: SharedPreferences
    fun init(c: Context) { if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("cfg", 0) }
    private fun gi(k: String, d: Int) = sp.getInt(k, d)
    private fun pi(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    var size: Int
        get() = gi("size", 42)
        set(v) = pi("size", v)
    var alpha: Int
        get() = gi("alpha", 95)
        set(v) = pi("alpha", v)
    var color: Int
        get() = gi("color", 0)
        set(v) = pi("color", v)
    var res: Int
        get() = gi("res", 0)
        set(v) = pi("res", v)
    var fps: Int
        get() = gi("fps", 30)
        set(v) = pi("fps", v)
    var count: Int
        get() = gi("count", 3)
        set(v) = pi("count", v)
    var battAsked: Boolean
        get() = sp.getBoolean("ba", false)
        set(v) = sp.edit().putBoolean("ba", v).apply()
    var audio: Int
        get() = gi("audio", 0)
        set(v) = pi("audio", v)
    val pal = listOf(
        0xFF7C5CFF.toInt() to 0xFFFF4DA6.toInt(), 0xFF00D4FF.toInt() to 0xFF7C5CFF.toInt(),
        0xFFFF8A00.toInt() to 0xFFFF2D55.toInt(), 0xFF3DDC97.toInt() to 0xFF00B3FF.toInt(),
        0xFFA5F3FC.toInt() to 0xFF6366F1.toInt(), 0xFFFFD166.toInt() to 0xFFFF6B6B.toInt())
    fun c1() = pal[color].first
    fun c2() = pal[color].second
    fun img(c: Context): Bitmap? { val f = File(c.filesDir, "bubble.png"); return if (f.exists()) BitmapFactory.decodeFile(f.path) else null }
}
