package com.puzzleblast.game.util

import java.io.File

object RootCheck {
    private val suPaths = listOf(
        "/system/bin/su", "/sbin/su", "/system/xbin/su", "/data/adb/magisk",
        "/system/su", "/system/bin/.ext/.su", "/system/usr/we-need-root/su-backup",
        "/data/local/xbin/su", "/data/local/bin/su", "/system/app/Superuser.apk",
        "/sbin/.magisk", "/sbin/.core"
    )

    fun detected(): Boolean = suPaths.any { File(it).exists() } ||
        System.getenv("PATH")?.split(":")?.any { File(it, "su").exists() } == true
}
