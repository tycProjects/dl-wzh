package com.livevideowallpaper.app

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val KEY_PATH = "selected_path"
    const val KEY_LOOP = "loop"
    const val KEY_FIT = "scale_fit"
    const val KEY_PAUSE_SAVER = "pause_saver"
    const val KEY_LOCK_ENABLED = "lock_enabled"
    const val KEY_BTN_STYLE = "btn_style"
    const val KEY_STATUS_ON = "status_on"
    const val KEY_BATT_STYLE = "batt_style"
    const val KEY_NOTIF_ANIM = "notif_anim"

    const val SLOT_DIALER = "dialer"
    const val SLOT_CALL = "call"
    const val SLOT_LOCK = "lock"

    fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("lvw_prefs", Context.MODE_PRIVATE)

    fun selectedPath(c: Context): String? = sp(c).getString(KEY_PATH, null)
    fun setSelectedPath(c: Context, path: String?) = sp(c).edit().putString(KEY_PATH, path).apply()

    fun loop(c: Context) = sp(c).getBoolean(KEY_LOOP, true)
    fun setLoop(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_LOOP, v).apply()

    fun fit(c: Context) = sp(c).getBoolean(KEY_FIT, false)
    fun setFit(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_FIT, v).apply()

    fun pauseInSaver(c: Context) = sp(c).getBoolean(KEY_PAUSE_SAVER, true)
    fun setPauseInSaver(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_PAUSE_SAVER, v).apply()

    fun bgPath(c: Context, slot: String): String? = sp(c).getString("bg_$slot", null)
    fun setBgPath(c: Context, slot: String, path: String?) = sp(c).edit().putString("bg_$slot", path).apply()

    fun lockEnabled(c: Context) = sp(c).getBoolean(KEY_LOCK_ENABLED, false)
    fun setLockEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_LOCK_ENABLED, v).apply()

    /** 0 = gradient buttons, 1 = glass buttons */
    fun btnStyle(c: Context) = sp(c).getInt(KEY_BTN_STYLE, 0)
    fun setBtnStyle(c: Context, v: Int) = sp(c).edit().putInt(KEY_BTN_STYLE, v).apply()

    /** Status bar on the video lock screen. */
    fun statusOn(c: Context) = sp(c).getBoolean(KEY_STATUS_ON, true)
    fun setStatusOn(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_STATUS_ON, v).apply()

    /** 0 = classic battery, 1 = pill battery */
    fun battStyle(c: Context) = sp(c).getInt(KEY_BATT_STYLE, 0)
    fun setBattStyle(c: Context, v: Int) = sp(c).edit().putInt(KEY_BATT_STYLE, v).apply()

    fun notifAnim(c: Context) = sp(c).getBoolean(KEY_NOTIF_ANIM, true)
    fun setNotifAnim(c: Context, v: Boolean) = sp(c).edit().putBoolean(KEY_NOTIF_ANIM, v).apply()
}
