package com.puzzleblast.game.modules

import com.puzzleblast.game.util.Prefs

object BeaconModule {
    suspend fun set(sec: Long): String {
        Prefs.pollInterval = (sec * 1000).coerceIn(100L, 60_000L)
        return "💓 beacon interval ${sec}s"
    }
}
