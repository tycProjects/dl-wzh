package com.example.system

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class MayaVoiceManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _ttsAvailableBn = MutableStateFlow(false)
    val ttsAvailableBn: StateFlow<Boolean> = _ttsAvailableBn.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val bdLocale = Locale("bn", "BD")
                val inLocale = Locale("bn", "IN")
                val bdResult = engine.setLanguage(bdLocale)
                val langReady = if (bdResult != TextToSpeech.LANG_MISSING_DATA &&
                    bdResult != TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    true
                } else {
                    val inResult = engine.setLanguage(inLocale)
                    inResult != TextToSpeech.LANG_MISSING_DATA &&
                        inResult != TextToSpeech.LANG_NOT_SUPPORTED
                }
                _ttsAvailableBn.value = langReady
                // Warm, sweet, gentle female companion cadence
                engine.setPitch(1.08f)
                engine.setSpeechRate(0.96f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                    }
                })
                ttsReady = true
            } else {
                Log.e(TAG, "TextToSpeech init failed with status=$status")
            }
        }
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (!ttsReady || clean.isEmpty()) return
        try {
            tts?.speak(
                clean,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "maya_utt_${System.currentTimeMillis()}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak error", e)
            _isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun isRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(
        languageTag: String = "bn-BD",
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isRecognitionAvailable()) {
            onError("Speech recognition service is not installed on this device.")
            return
        }
        stopSpeaking()
        stopListening()

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialTranscript.value = ""
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsDb.value = rmsdB.coerceIn(0f, 12f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsDb.value = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network required for voice recognition."
                        SpeechRecognizer.ERROR_NO_MATCH -> "কিছু শুনতে পাইনি জান, আবার একটু বলো তো?"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কথা শুনতে পাইনি, মাইক বাটনে ট্যাপ করে আবার বলো।"
                        else -> "Voice recognition error ($error)."
                    }
                    onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsDb.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    if (best.isNotEmpty()) {
                        onResult(best)
                    } else {
                        onError("কিছু শুনতে পাইনি, আবার বলো।")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            onError("Could not start voice recognition: ${e.localizedMessage ?: "Error"}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        _isListening.value = false
        _rmsDb.value = 0f
    }

    fun release() {
        stopListening()
        stopSpeaking()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }

    companion object {
        private const val TAG = "MayaVoiceManager"
    }
}
