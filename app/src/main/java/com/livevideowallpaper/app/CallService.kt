package com.livevideowallpaper.app

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

/** The system binds this when the app is the default phone app. It receives every call. */
class CallService : InCallService() {

    private val cb = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            refresh()
            CallHub.changed()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            CallHub.changed()
        }
    }

    override fun onCreate() {
        super.onCreate()
        CallHub.service = this
        CallNotifications.ensureChannel(this)
    }

    override fun onDestroy() {
        CallHub.service = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        call.registerCallback(cb)
        CallHub.calls.add(call)
        refresh()
        CallHub.changed()
        launchUi()
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(cb)
        CallHub.calls.remove(call)
        refresh()
        CallHub.changed()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        CallHub.audio = audioState
        CallHub.changed()
    }

    private fun refresh() {
        val c = CallHub.primary()
        if (c == null) CallNotifications.cancel(this) else CallNotifications.show(this, c)
    }

    /** Direct launch works when the app is allowed to; the full-screen notification is the backup. */
    private fun launchUi() {
        try { startActivity(CallNotifications.uiIntent(this)) } catch (_: Exception) {}
    }
}
