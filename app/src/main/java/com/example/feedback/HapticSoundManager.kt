package com.example.feedback

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.model.KeyboardSettings

class HapticSoundManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun triggerFeedback(settings: KeyboardSettings, view: View? = null, isHeavy: Boolean = false) {
        if (settings.hapticEnabled) {
            triggerHaptic(settings.hapticStrength, view, isHeavy)
        }
        if (settings.soundEnabled) {
            triggerSound(settings.soundVolume)
        }
    }

    private fun triggerHaptic(strengthMs: Int, view: View?, isHeavy: Boolean) {
        // Fast path: use View haptic feedback if provided
        if (view != null) {
            val constant = if (isHeavy) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            if (view.performHapticFeedback(constant)) {
                return
            }
        }

        // Direct vibrator fallback with strength adjustment
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                val duration = if (isHeavy) (strengthMs * 1.5).toLong().coerceIn(20, 100) else strengthMs.toLong().coerceIn(5, 75)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitude = if (isHeavy) VibrationEffect.DEFAULT_AMPLITUDE else (strengthMs * 3).coerceIn(40, 255)
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            }
        } catch (_: Exception) {
            // Ignore security or hardware limitations gracefully
        }
    }

    private fun triggerSound(volume: Float) {
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, volume.coerceIn(0.1f, 1.0f))
        } catch (_: Exception) {
            // Ignore sound play failures
        }
    }

    companion object {
        @Volatile
        private var instance: HapticSoundManager? = null

        fun getInstance(context: Context): HapticSoundManager {
            return instance ?: synchronized(this) {
                instance ?: HapticSoundManager(context).also { instance = it }
            }
        }
    }
}
