package com.greatnice.gts1550control.media

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.view.KeyEvent

/**
 * Standard Android media controls. These act on the PHONE's active media session
 * and the PHONE's media volume. The phone/speaker pair propagates them over
 * AVRCP / A2DP if (and only if) the speaker implements those profiles.
 */
class PlaybackControls(ctx: Context) {
    private val audio = ctx.getSystemService(AudioManager::class.java)

    fun playPause() = key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun next() = key(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous() = key(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    fun pause() = key(KeyEvent.KEYCODE_MEDIA_PAUSE)

    private fun key(code: Int) {
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    val maxVolume: Int get() = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    fun volume(): Int = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    fun setVolume(v: Int) =
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, v.coerceIn(0, maxVolume), 0)

    fun isMusicActive(): Boolean = audio.isMusicActive

    /** Name of the Bluetooth A2DP output Android is currently routing media to, if any. */
    fun bluetoothOutputName(): String? =
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?.productName?.toString()
}
