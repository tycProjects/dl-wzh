package com.nexus.launcher.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Process

object GameUtils {

    fun launchGame(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    fun clearMemory(context: Context) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getRunningAppProcesses()?.forEach { process ->
                if (process.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND
                    && process.pid != Process.myPid()
                ) {
                    try {
                        Process.killProcess(process.pid)
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    fun isAppInForeground(context: Context, packageName: String): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return try {
            @Suppress("DEPRECATION")
            val tasks = am.getRunningTasks(1)
            if (tasks.isNullOrEmpty()) false
            else tasks[0].topActivity?.packageName == packageName
        } catch (_: Exception) {
            false
        }
    }
}
