package com.mousetuner

import android.content.Context

/** Danh sách game: Free Fire (mặc định) + game người dùng tự thêm. */
object FreeFire {
    val APPS = listOf("Free Fire" to "com.dts.freefireth", "Free Fire MAX" to "com.dts.freefiremax")
    @Volatile private var known: List<String> = APPS.map { it.second }

    fun extra(c: Context) = Cfg.s(c, "games").split(",").filter { it.isNotBlank() }
    fun isCustom(c: Context, pkg: String) = extra(c).contains(pkg)
    fun add(c: Context, pkg: String) { Cfg.setS(c, "games", (extra(c) + pkg).distinct().joinToString(",")) }
    fun remove(c: Context, pkg: String) { Cfg.setS(c, "games", extra(c).filter { it != pkg }.joinToString(",")) }

    fun installed(c: Context): List<Pair<String, String>> {
        val pm = c.packageManager
        val l = (APPS.map { it.second } + extra(c)).distinct().filter { pm.getLaunchIntentForPackage(it) != null }.map { p ->
            (APPS.firstOrNull { it.second == p }?.first ?: try { pm.getApplicationInfo(p, 0).loadLabel(pm).toString() } catch (e: Throwable) { p }) to p
        }
        known = l.map { it.second }; return l
    }
    fun launch(c: Context, pkg: String) { c.packageManager.getLaunchIntentForPackage(pkg)?.let { c.startActivity(it) } }

    /** Game đang ở trước màn hình (null nếu không). */
    fun foregroundPkg(): String? {
        val out = ShizukuShell.exec("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'").second
        return known.firstOrNull { out.contains(it) }
    }
    fun enter(c: Context) = Engine.apply(c)
    fun leave(c: Context) = Engine.reset(c)
}
