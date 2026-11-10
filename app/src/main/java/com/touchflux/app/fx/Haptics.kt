package com.touchflux.app.fx

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object Haptics {
    @Suppress("DEPRECATION")
    fun tick(context: Context) {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 29) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            v.vibrate(VibrationEffect.createOneShot(10L, 60))
        }
    }
}
