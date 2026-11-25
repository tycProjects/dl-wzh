package com.livevideowallpaper.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.VideoProfile

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val call = CallHub.primary() ?: return
        when (intent.action) {
            CallNotifications.ACTION_ANSWER -> {
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
                try { context.startActivity(CallNotifications.uiIntent(context)) } catch (_: Exception) {}
            }
            CallNotifications.ACTION_DECLINE ->
                if (call.state == Call.STATE_RINGING) call.reject(false, null) else call.disconnect()
        }
    }
}
