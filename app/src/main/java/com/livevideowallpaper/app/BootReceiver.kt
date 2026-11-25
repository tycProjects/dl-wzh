package com.livevideowallpaper.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.lockEnabled(context) && Settings.canDrawOverlays(context)) {
            try { LockVideoService.start(context) } catch (_: Exception) {}
        }
    }
}
