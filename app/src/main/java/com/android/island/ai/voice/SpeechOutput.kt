package com.android.island.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lazy, process-scoped TTS. No foreground service or permanent audio resource is held. */
object SpeechOutput : TextToSpeech.OnInitListener {
    @Volatile private var engine: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile private var pendingText: String? = null

    @Synchronized fun speak(context: Context, text: String, language: Locale = Locale.getDefault()) {
        val clean = text.trim().take(6000)
        if (clean.isBlank()) return
        val current = engine
        if (current == null) {
            pendingText = clean
            engine = TextToSpeech(context.applicationContext, this)
            return
        }
        if (ready) speakNow(current, clean, language) else pendingText = clean
    }

    @Synchronized fun stop() {
        pendingText = null
        engine?.stop()
    }

    @Synchronized fun release() {
        pendingText = null
        ready = false
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    override fun onInit(status: Int) {
        val current = engine ?: return
        ready = status == TextToSpeech.SUCCESS
        val queued = pendingText
        pendingText = null
        if (ready && !queued.isNullOrBlank()) {
            runCatching { speakNow(current, queued, Locale.getDefault()) }
        } else if (!ready) {
            current.shutdown()
            engine = null
        }
    }

    private fun speakNow(tts: TextToSpeech, text: String, locale: Locale) {
        val result = runCatching { tts.setLanguage(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            runCatching { tts.setLanguage(Locale.US) }
        }
        runCatching { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "android_island_ai_reply") }
    }
}
