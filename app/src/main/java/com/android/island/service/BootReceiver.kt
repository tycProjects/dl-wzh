package com.android.island.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AccessibilityService is managed by the system once the user enables it.
 * Boot receiver kept for future sticky helpers; no overlay service start needed.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // IslandAccessibilityService reconnects automatically when enabled.
    }
}
