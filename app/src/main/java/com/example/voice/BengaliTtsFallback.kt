package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class BengaliTtsFallback(context: Context) {
    companion object {
        private const val TAG = "BengaliTtsFallback"
    }

    private var tts: TextToSpeech? = null
    @Volatile
    private var isReady = false
    @Volatile
    private var activeCont: kotlin.coroutines.Continuation<Unit>? = null
    @Volatile
    private var activeAmplitudeCallback: ((Float) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureBengaliFemaleVoice()
                isReady = true
                Log.d(TAG, "Android Bengali TTS initialized")
            } else {
                Log.e(TAG, "Android TTS init failed with status $status")
            }
        }
    }

    private fun configureBengaliFemaleVoice() {
        val engine = tts ?: return
        val bnBd = Locale("bn", "BD")
        val bnIn = Locale("bn", "IN")
        val res = engine.setLanguage(bnBd)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(bnIn)
        }
        // Prefer female Bengali voice if available on device
        runCatching {
            val femaleVoice = engine.voices?.firstOrNull { v ->
                v.locale.language == "bn" && (
                    v.name.contains("female", ignoreCase = true) ||
                        v.name.contains("bnd", ignoreCase = true) ||
                        v.name.contains("bn-bd-x-ban", ignoreCase = true) ||
                        v.name.contains("sfb", ignoreCase = true)
                    )
            } ?: engine.voices?.firstOrNull { it.locale.language == "bn" }
            if (femaleVoice != null) {
                engine.voice = femaleVoice
            }
        }
        engine.setPitch(1.08f)

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                activeAmplitudeCallback?.invoke(0.68f)
            }

            override fun onDone(utteranceId: String?) {
                activeAmplitudeCallback?.invoke(0f)
                val cont = activeCont
                activeCont = null
                cont?.resume(Unit)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                activeAmplitudeCallback?.invoke(0f)
                val cont = activeCont
                activeCont = null
                cont?.resume(Unit)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                activeAmplitudeCallback?.invoke(0f)
                val cont = activeCont
                activeCont = null
                cont?.resume(Unit)
            }
        })
    }

    suspend fun speak(
        text: String,
        speed: Float,
        volume: Float,
        onAmplitudeTick: (Float) -> Unit
    ) {
        val cleanText = text.replace(Regex("[*#`_]"), "").trim()
        val engine = tts
        if (!isReady || engine == null || cleanText.isBlank()) {
            onAmplitudeTick(0f)
            return
        }

        suspendCancellableCoroutine { cont ->
            activeCont = cont
            activeAmplitudeCallback = onAmplitudeTick
            engine.setSpeechRate(speed.coerceIn(0.75f, 1.6f))
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            }
            val utteranceId = UUID.randomUUID().toString()

            cont.invokeOnCancellation {
                onAmplitudeTick(0f)
                activeCont = null
                runCatching { engine.stop() }
            }

            val result = engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                onAmplitudeTick(0f)
                activeCont = null
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    fun stop() {
        activeAmplitudeCallback?.invoke(0f)
        runCatching { tts?.stop() }
        val cont = activeCont
        activeCont = null
        cont?.resume(Unit)
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
    }
}
