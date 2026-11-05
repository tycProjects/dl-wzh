package com.nexus.launcher.utils

import android.content.Context
import android.content.pm.PackageManager
import com.nexus.launcher.model.GameApp
import org.json.JSONArray
import org.json.JSONObject

object GameRepository {

    private const val PREF_NAME = "nexus_games"
    private const val KEY_PACKAGES = "saved_packages"

    fun getSavedGames(context: Context): List<GameApp> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PACKAGES, "[]") ?: "[]"
        val array = JSONArray(json)
        val pm = context.packageManager
        val result = mutableListOf<GameApp>()

        for (i in 0 until array.length()) {
            val pkg = array.getString(i)
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val name = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                result.add(
                    GameApp(
                        packageName = pkg,
                        name = name,
                        icon = icon,
                        playTimeMinutes = (30..300).random().toLong(),
                        lastPlayed = System.currentTimeMillis()
                    )
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // App uninstalled, skip
            }
        }
        return result
    }

    fun saveGames(context: Context, packages: List<String>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        packages.forEach { array.put(it) }
        prefs.edit().putString(KEY_PACKAGES, array.toString()).apply()
    }

    fun addGame(context: Context, packageName: String) {
        val current = getSavedGames(context).map { it.packageName }.toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            saveGames(context, current)
        }
    }

    fun removeGame(context: Context, packageName: String) {
        val current = getSavedGames(context).map { it.packageName }.toMutableList()
        current.remove(packageName)
        saveGames(context, current)
    }

    fun getAllLaunchableApps(context: Context): List<GameApp> {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val list = mutableListOf<GameApp>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                list.add(
                    GameApp(
                        packageName = pkg,
                        name = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo)
                    )
                )
            } catch (_: Exception) {}
        }
        return list.sortedBy { it.name.lowercase() }
    }
}
