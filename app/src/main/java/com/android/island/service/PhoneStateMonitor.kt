package com.android.island.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.android.island.core.IslandActivity
import com.android.island.core.IslandBus

/**
 * Detects calls straight from the telephony state (READ_PHONE_STATE), so a ringing or active call
 * shows up even when the dialer posts no notification. Calls that already come in through a call
 * notification are left to the notification path, so the same call is never shown twice.
 */
object PhoneStateMonitor {
    private const val PHONE_CALL_ID = "phone-state-call"
    private var receiver: BroadcastReceiver? = null

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    /** Registers the receiver once permission is granted. Cheap to call repeatedly. */
    @Synchronized
    fun ensureRegistered(context: Context) {
        if (receiver != null || !hasPermission(context)) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
                val notificationCallActive =
                    (IslandBus.state.value.current as? IslandActivity.Call)?.let { it.id != PHONE_CALL_ID } == true
                when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                    TelephonyManager.EXTRA_STATE_RINGING -> if (!notificationCallActive) {
                        IslandBus.pushCall(
                            IslandActivity.Call(
                                id = PHONE_CALL_ID,
                                callerName = "Panggilan masuk",
                                subtitle = "Panggilan masuk",
                                isIncoming = true
                            )
                        )
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> if (!notificationCallActive) {
                        IslandBus.pushCall(
                            IslandActivity.Call(
                                id = PHONE_CALL_ID,
                                callerName = "Panggilan",
                                isIncoming = false
                            )
                        )
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> IslandBus.clearCall(PHONE_CALL_ID)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            r,
            IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        receiver = r
    }

    @Synchronized
    fun unregister(context: Context) {
        receiver?.let { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
        receiver = null
    }
}
