package com.johan.jugb185.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Log.i("JUGB-Boot", "Boot received: ${intent.action}")
        // Locked-in: Start turbo immediately after boot
        try {
            WatchdogService.start(ctx)
        } catch (e: Exception) {
            Log.e("JUGB-Boot", "Failed to start watchdog", e)
        }
    }
}
