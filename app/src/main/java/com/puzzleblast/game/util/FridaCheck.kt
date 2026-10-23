package com.puzzleblast.game.util

import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

object FridaCheck {
    fun detected(): Boolean {
        try {
            File("/proc/self/maps").forEachLine { line ->
                if (line.contains("frida", true) || line.contains("gadget", true) || line.contains("xposed", true)) return true
            }
        } catch (_: Exception) {}
        try {
            Socket().use { s ->
                s.connect(InetSocketAddress("127.0.0.1", 27042), 200)
                return true
            }
        } catch (_: Exception) {}
        try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            return true
        } catch (_: Exception) {}
        return false
    }
}
