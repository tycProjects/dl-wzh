package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SpeechState {
    data object Idle : SpeechState
    data object Listening : SpeechState
    data class Partial(val text: String) : SpeechState
    data class FinalResult(val text: String) : SpeechState
    data class Error(val message: String) : SpeechState
}

class MayaSpeechRecognizer(private val context: Context) {
    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageTag: String = "bn-BD", onFinalResult: (String) -> Unit) {
        if (!isAvailable()) {
            _state.value = SpeechState.Error("এই ফোনে স্পিচ রিকগনিশন সার্ভিস পাওয়া যায়নি।")
            return
        }

        stopListening()

        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechState.Listening
            }

            override fun onBeginningOfSpeech() {
                _state.value = SpeechState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _rmsLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _rmsLevel.value = 0f
            }

            override fun onError(error: Int) {
                _rmsLevel.value = 0f
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "মাইক্রোফোন অডিও নিতে সমস্যা হয়েছে।"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন ব্যবহারের অনুমতি লাগবে।"
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ভয়েস চিনতে ইন্টারনেট সংযোগ প্রয়োজন।"
                    SpeechRecognizer.ERROR_NO_MATCH -> "কিছু শুনতে পাইনি, আবার বলো।"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শোনা যায়নি।"
                    else -> "ভয়েস বুঝতে সমস্যা হয়েছে, আবার চেষ্টা করো।"
                }
                _state.value = SpeechState.Error(message)
            }

            override fun onResults(results: Bundle?) {
                _rmsLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val top = matches?.firstOrNull()?.trim().orEmpty()
                if (top.isNotEmpty()) {
                    _state.value = SpeechState.FinalResult(top)
                    onFinalResult(top)
                } else {
                    _state.value = SpeechState.Error("কথা বুঝতে পারিনি, আবার বলো।")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim().orEmpty()
                if (partial.isNotEmpty()) {
                    _state.value = SpeechState.Partial(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
        }

        _state.value = SpeechState.Listening
        sr.startListening(intent)
    }

    fun stopListening() {
        _rmsLevel.value = 0f
        runCatching {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        }
        recognizer = null
        if (_state.value is SpeechState.Listening || _state.value is SpeechState.Partial) {
            _state.value = SpeechState.Idle
        }
    }

    fun resetState() {
        _state.value = SpeechState.Idle
    }
}
