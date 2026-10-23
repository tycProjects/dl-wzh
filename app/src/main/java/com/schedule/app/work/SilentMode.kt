package com.schedule.app.work

import android.content.Context
import android.media.AudioManager
import com.schedule.app.data.Prefs

/** يحوّل الهاتف للاهتزاز وقت المحاضرة ويعيد الوضع السابق بعدها. */
object SilentMode {
    fun enter(ctx: Context) {
        val prefs = Prefs(ctx)
        if (!prefs.read().silentMode) return
        runCatching {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (prefs.savedRinger == -1 && am.ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
                prefs.savedRinger = am.ringerMode
                am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
        }
    }

    fun exit(ctx: Context) {
        val prefs = Prefs(ctx)
        val saved = prefs.savedRinger
        if (saved == -1) return
        runCatching { (ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode = saved }
        prefs.savedRinger = -1
    }
}
