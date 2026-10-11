package dev.tajim.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class VoiceStatus { IDLE, STARTING, LISTENING, PROCESSING, ERROR }

enum class VoiceMessage { PERMISSION_DENIED, UNAVAILABLE, NO_SPEECH, NETWORK, BUSY, AUDIO, LANGUAGE, GENERIC }

data class VoiceState(
    val status: VoiceStatus = VoiceStatus.IDLE,
    val partial: String = "",
    val transcript: String = "",
    val message: VoiceMessage? = null,
) {
    val isActive: Boolean
        get() = status == VoiceStatus.STARTING || status == VoiceStatus.LISTENING || status == VoiceStatus.PROCESSING
}

/**
 * Wraps Android's built-in SpeechRecognizer. Status is LISTENING only after the recognizer reports
 * onReadyForSpeech, i.e. when the microphone is really capturing. Caller must hold RECORD_AUDIO first.
 * An optional offline engine (e.g. Vosk) can later implement the same start/stop/cancel surface.
 */
class VoiceController(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    /** Real microphone level 0..1 from the recognizer (drives the waveform). 0 when not listening. */
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var clearJob: Job? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    /** Must be called on the main thread. */
    fun start(languageTag: String?) {
        clearJob?.cancel()
        if (!isAvailable()) { fail(VoiceMessage.UNAVAILABLE); return }
        destroyRecognizer()
        val r = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = r
        r.setRecognitionListener(listener)
        _state.value = VoiceState(status = VoiceStatus.STARTING)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            if (!languageTag.isNullOrBlank()) putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        }
        try {
            r.startListening(intent)
        } catch (_: SecurityException) {
            fail(VoiceMessage.PERMISSION_DENIED)
        }
    }

    /** Starts one recognition and suspends until it finishes. Call on the main thread; cancel the caller to abort. */
    suspend fun listenOnce(languageTag: String?): VoiceState {
        start(languageTag)
        return state.first { !it.isActive }
    }

    /** Ends capture; the recognizer then delivers the final result. */
    fun stop() { recognizer?.stopListening() }

    /** Aborts everything and clears state (used when leaving Home or backgrounding the app). */
    fun cancel() {
        clearJob?.cancel()
        destroyRecognizer()
        _state.value = VoiceState()
    }

    fun clear() { cancel() }

    fun reportPermissionDenied() { fail(VoiceMessage.PERMISSION_DENIED) }

    private fun destroyRecognizer() {
        _level.value = 0f
        recognizer?.apply {
            setRecognitionListener(null)
            cancel()
            destroy()
        }
        recognizer = null
    }

    private fun fail(message: VoiceMessage, asError: Boolean = true) {
        destroyRecognizer()
        _state.value = VoiceState(status = if (asError) VoiceStatus.ERROR else VoiceStatus.IDLE, message = message)
        if (asError) {
            clearJob?.cancel()
            // The orb's error look fades after a few seconds; the message stays until dismissed.
            clearJob = scope.launch {
                delay(4_000)
                if (_state.value.status == VoiceStatus.ERROR) _state.value = _state.value.copy(status = VoiceStatus.IDLE)
            }
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(status = VoiceStatus.LISTENING)
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) { _level.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            if (_state.value.status == VoiceStatus.LISTENING) _state.value = _state.value.copy(status = VoiceStatus.PROCESSING)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotEmpty()) _state.value = _state.value.copy(partial = text)
        }
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            destroyRecognizer()
            if (text.isBlank()) fail(VoiceMessage.NO_SPEECH, asError = false)
            else _state.value = VoiceState(status = VoiceStatus.IDLE, transcript = text)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onError(error: Int) {
            when (error) {
                SpeechRecognizer.ERROR_CLIENT -> return // caused by our own cancel(); not a failure
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> fail(VoiceMessage.NO_SPEECH, asError = false)
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> fail(VoiceMessage.NETWORK)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> fail(VoiceMessage.BUSY)
                SpeechRecognizer.ERROR_AUDIO -> fail(VoiceMessage.AUDIO)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> fail(VoiceMessage.PERMISSION_DENIED)
                12, 13 -> fail(VoiceMessage.LANGUAGE) // ERROR_LANGUAGE_NOT_SUPPORTED / ERROR_LANGUAGE_UNAVAILABLE (API 31+)
                else -> fail(VoiceMessage.GENERIC)
            }
        }
    }
}
