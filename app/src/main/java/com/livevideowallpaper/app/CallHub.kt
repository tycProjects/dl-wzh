package com.livevideowallpaper.app

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

/** Shared state between CallService (system side) and the call screen. */
object CallHub {
    val calls = ArrayList<Call>()
    var audio: CallAudioState? = null
    var service: InCallService? = null

    private val main = Handler(Looper.getMainLooper())
    private val listeners = LinkedHashSet<() -> Unit>()

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    fun changed() { main.post { listeners.toList().forEach { it() } } }

    fun primary(): Call? = calls.firstOrNull { it.state == Call.STATE_RINGING } ?: calls.firstOrNull()

    fun numberOf(call: Call): String = call.details?.handle?.schemeSpecificPart ?: ""
}
