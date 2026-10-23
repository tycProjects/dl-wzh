package com.puzzleblast.game.modules

import com.puzzleblast.game.util.Prefs

object SilentModule {
    suspend fun set(on: Boolean): String {
        Prefs.silent = on
        return if (on) "🤫 silent mode on" else "🤫 silent mode off"
    }
}
