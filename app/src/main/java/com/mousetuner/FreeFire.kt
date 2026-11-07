package com.mousetuner

import android.content.Context

/** Hồ sơ dành riêng cho Free Fire / Free Fire MAX. */
object FreeFire {
    val APPS = listOf("Free Fire" to "com.dts.freefireth", "Free Fire MAX" to "com.dts.freefiremax")

    fun installed(c: Context) = APPS.filter { c.packageManager.getLaunchIntentForPackage(it.second) != null }
    fun launch(c: Context, pkg: String) { c.packageManager.getLaunchIntentForPackage(pkg)?.let { c.startActivity(it) } }

    /** Package Free Fire đang ở trước màn hình (null nếu không). */
    fun foregroundPkg(): String? {
        val out = ShizukuShell.exec("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'").second
        return APPS.map { it.second }.firstOrNull { out.contains(it) }
    }
    fun enter(c: Context) = Engine.apply(c)
    fun leave(c: Context) = Engine.reset(c)
}
