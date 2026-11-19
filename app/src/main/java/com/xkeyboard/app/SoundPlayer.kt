package com.xkeyboard.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator
import java.io.File

class SoundPlayer(private val ctx: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var loadedPath: String = ""
    private val toneGen = runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, 45) }.getOrNull()

    fun playKey(code: Int) {
        if (!Prefs.sound(ctx)) return
        val style = Prefs.soundStyle(ctx)
        if (style == 3) {
            val path = Prefs.customSoundPath(ctx)
            if (path.isNotEmpty() && File(path).exists()) {
                playCustomFile(path)
                return
            }
        }
        when (style) {
            1 -> toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 18)
            2 -> toneGen?.startTone(ToneGenerator.TONE_CDMA_PIP, 22)
            else -> {
                val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.playSoundEffect(
                    when (code) {
                        XKeyboardView.C_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                        XKeyboardView.C_DEL -> AudioManager.FX_KEYPRESS_DELETE
                        XKeyboardView.C_ENTER -> AudioManager.FX_KEYPRESS_RETURN
                        else -> AudioManager.FX_KEYPRESS_STANDARD
                    },
                    -1f
                )
            }
        }
    }

    private fun playCustomFile(path: String) {
        runCatching {
            if (mediaPlayer == null || loadedPath != path) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                }
                loadedPath = path
            } else {
                mediaPlayer?.seekTo(0)
            }
            mediaPlayer?.start()
        }
    }

    fun release() {
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { toneGen?.release() }
    }
}
