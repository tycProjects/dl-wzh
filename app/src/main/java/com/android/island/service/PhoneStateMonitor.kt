package com.android.island.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.android.island.core.IslandActivity
import com.android.island.core.IslandBus

/**
 * Detects calls straight from the telephony state (READ_PHONE_STATE), so a ringing or active call
 * shows up even when the dialer posts no notification. Calls that already come in through a call
 * notification are left to the notification path, so the same call is never shown twice.
 *
 * Uses TelephonyCallback (the supported API on Android 12+). The old ACTION_PHONE_STATE_CHANGED
 * broadcast is deprecated and is delivered once per SIM on many MediaTek / Unisoc dual-SIM devices.
 */
object PhoneStateMonitor {
    private const val PHONE_CALL_ID = "phone-state-call"
    private var callback: TelephonyCallback? = null

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    private class CallStateCallback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) = handle(state)
    }

    private fun handle(state: Int) {
        val notificationCallActive =
            (IslandBus.state.value.current as? IslandActivity.Call)?.let { it.id != PHONE_CALL_ID } == true
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> if (!notificationCallActive) {
                IslandBus.pushCall(
                    IslandActivity.Call(
                        id = PHONE_CALL_ID,
                        callerName = "Panggilan masuk",
                        subtitle = "Panggilan masuk",
                        isIncoming = true
                    )
                )
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> if (!notificationCallActive) {
                IslandBus.pushCall(
                    IslandActivity.Call(
                        id = PHONE_CALL_ID,
                        callerName = "Panggilan",
                        isIncoming = false
                    )
                )
            }
            TelephonyManager.CALL_STATE_IDLE -> IslandBus.clearCall(PHONE_CALL_ID)
        }
    }

    /** Registers the callback once permission is granted. Cheap to call repeatedly. */
    @Synchronized
    fun ensureRegistered(context: Context) {
        if (callback != null || !hasPermission(context)) return
        val tm = context.getSystemService(TelephonyManager::class.java) ?: return
        val cb = CallStateCallback()
        val ok = runCatching {
            tm.registerTelephonyCallback(ContextCompat.getMainExecutor(context), cb)
        }.isSuccess
        if (ok) callback = cb
    }

    @Synchronized
    fun unregister(context: Context) {
        val cb = callback ?: return
        runCatching { context.getSystemService(TelephonyManager::class.java)?.unregisterTelephonyCallback(cb) }
        callback = null
    }
}
