package dev.tajim.jarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/** Android TextToSpeech. Picks a Bengali voice when the text is Bengali and one is installed. */
class Speaker(context: Context) {
    private val app = context.applicationContext
    private var tts: TextToSpeech? = null
    private val ready = CompletableDeferred<Boolean>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()
    private var counter = 0

    private fun ensure() {
        if (tts != null) return
        tts = TextToSpeech(app) { status -> ready.complete(status == TextToSpeech.SUCCESS) }.also { engine ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _speaking.value = true }
                override fun onDone(utteranceId: String?) { finish(utteranceId) }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { finish(utteranceId) }
                override fun onStop(utteranceId: String?, interrupted: Boolean) { finish(utteranceId) }
            })
        }
    }

    private fun finish(id: String?) {
        id?.let { pending.remove(it)?.complete(Unit) }
        if (pending.isEmpty()) _speaking.value = false
    }

    /** Speaks [text] and suspends until it finishes (or [maxMillis] passes). Returns false if TTS is unavailable. */
    suspend fun speak(text: String, rate: Float, maxMillis: Long = 60_000): Boolean {
        val clean = text.replace(Regex("[*#`_>]"), "").replace(Regex("\\s+"), " ").trim().take(1500)
        if (clean.isEmpty()) return true
        ensure()
        if (ready.await().not()) return false
        val engine = tts ?: return false
        val bengali = clean.any { it in '\u0980'..'\u09FF' }
        val locale = if (bengali) Locale("bn", "BD") else Locale.getDefault()
        val avail = engine.isLanguageAvailable(locale)
        if (avail >= TextToSpeech.LANG_AVAILABLE) engine.language = locale
        engine.setSpeechRate(rate)
        val id = "u" + (counter++)
        val done = CompletableDeferred<Unit>()
        pending[id] = done
        if (engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            pending.remove(id)
            return false
        }
        withTimeoutOrNull(maxMillis) { done.await() }
        pending.remove(id)
        if (pending.isEmpty()) _speaking.value = false
        return true
    }

    fun stop() {
        tts?.stop()
        pending.values.forEach { it.complete(Unit) }
        pending.clear()
        _speaking.value = false
    }
}
