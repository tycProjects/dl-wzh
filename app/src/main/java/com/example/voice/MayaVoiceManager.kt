package com.example.voice

import android.content.Context
import com.example.data.MayaSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class VoiceEngineStatus {
    FALLBACK_TTS_READY,
    ELEVENLABS_CONNECTED,
    ELEVENLABS_ERROR
}

class MayaVoiceManager(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val elevenLabs = ElevenLabsClient(context.applicationContext)
    private val fallbackTts = BengaliTtsFallback(context.applicationContext)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude: StateFlow<Float> = _speechAmplitude.asStateFlow()

    private val _voiceStatus = MutableStateFlow(VoiceEngineStatus.FALLBACK_TTS_READY)
    val voiceStatus: StateFlow<VoiceEngineStatus> = _voiceStatus.asStateFlow()

    private val _voiceStatusDetail = MutableStateFlow("বাংলা নারী ভয়েস প্রস্তুত (Device TTS)")
    val voiceStatusDetail: StateFlow<String> = _voiceStatusDetail.asStateFlow()

    private val _availableVoices = MutableStateFlow(ElevenLabsClient.PRESET_FEMALE_VOICES)
    val availableVoices: StateFlow<List<ElevenLabsVoiceOption>> = _availableVoices.asStateFlow()

    private var speechQueue = Channel<SpeechRequest>(Channel.UNLIMITED)
    private var workerJob: Job? = null
    private var amplitudeAnimatorJob: Job? = null

    private data class SpeechRequest(
        val text: String,
        val settings: MayaSettings,
        val elevenLabsKey: String
    )

    init {
        startWorker()
    }

    private fun startWorker() {
        workerJob?.cancel()
        workerJob = scope.launch {
            for (req in speechQueue) {
                if (!isActive) break
                speakSingleChunk(req)
            }
        }
    }

    private suspend fun speakSingleChunk(req: SpeechRequest) {
        _isSpeaking.value = true
        startAmplitudePulse()

        try {
            val canUseElevenLabs = req.settings.useElevenLabs && req.elevenLabsKey.isNotBlank()
            if (canUseElevenLabs) {
                val result = elevenLabs.synthesizeAndPlay(
                    text = req.text,
                    apiKey = req.elevenLabsKey,
                    voiceId = req.settings.elevenLabsVoiceId,
                    modelId = req.settings.elevenLabsModel,
                    speed = req.settings.voiceSpeed,
                    volume = req.settings.voiceVolume
                ) { amp ->
                    _speechAmplitude.value = amp
                }
                if (result.isSuccess) {
                    _voiceStatus.value = VoiceEngineStatus.ELEVENLABS_CONNECTED
                    _voiceStatusDetail.value = "ElevenLabs (${req.settings.elevenLabsVoiceName})"
                } else {
                    _voiceStatus.value = VoiceEngineStatus.ELEVENLABS_ERROR
                    _voiceStatusDetail.value =
                        result.exceptionOrNull()?.localizedMessage ?: "ElevenLabs ত্রুটি"
                    if (req.settings.fallbackToAndroidTts) {
                        fallbackTts.speak(
                            text = req.text,
                            speed = req.settings.voiceSpeed,
                            volume = req.settings.voiceVolume
                        ) { amp -> _speechAmplitude.value = amp }
                    }
                }
            } else if (req.settings.fallbackToAndroidTts) {
                _voiceStatus.value = VoiceEngineStatus.FALLBACK_TTS_READY
                _voiceStatusDetail.value = "বাংলা নারী ভয়েস সক্রিয় (Device TTS)"
                fallbackTts.speak(
                    text = req.text,
                    speed = req.settings.voiceSpeed,
                    volume = req.settings.voiceVolume
                ) { amp -> _speechAmplitude.value = amp }
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            if (speechQueue.isEmpty) {
                stopAmplitudePulse()
                _isSpeaking.value = false
                _speechAmplitude.value = 0f
            }
        }
    }

    private fun startAmplitudePulse() {
        amplitudeAnimatorJob?.cancel()
        amplitudeAnimatorJob = scope.launch {
            while (isActive && _isSpeaking.value) {
                _speechAmplitude.value = 0.35f + Random.nextFloat() * 0.65f
                delay(95L)
            }
        }
    }

    private fun stopAmplitudePulse() {
        amplitudeAnimatorJob?.cancel()
        _speechAmplitude.value = 0f
    }

    fun enqueueSpeech(text: String, settings: MayaSettings, elevenLabsKey: String) {
        if (text.isBlank()) return
        speechQueue.trySend(SpeechRequest(text, settings, elevenLabsKey))
    }

    fun speakImmediately(text: String, settings: MayaSettings, elevenLabsKey: String) {
        stopSpeaking()
        enqueueSpeech(text, settings, elevenLabsKey)
    }

    fun stopSpeaking() {
        while (speechQueue.tryReceive().isSuccess) {
            // drain queued chunks
        }
        elevenLabs.stop()
        fallbackTts.stop()
        stopAmplitudePulse()
        _isSpeaking.value = false
        _speechAmplitude.value = 0f
        startWorker()
    }

    suspend fun connectAndLoadVoices(apiKey: String): Result<List<ElevenLabsVoiceOption>> {
        val result = elevenLabs.fetchVoices(apiKey)
        if (result.isSuccess) {
            val voices = result.getOrDefault(ElevenLabsClient.PRESET_FEMALE_VOICES)
            _availableVoices.value = voices
            _voiceStatus.value = VoiceEngineStatus.ELEVENLABS_CONNECTED
            _voiceStatusDetail.value = "ElevenLabs সংযুক্ত (${voices.size}টি নারী কণ্ঠ)"
        } else {
            _voiceStatus.value = VoiceEngineStatus.ELEVENLABS_ERROR
            _voiceStatusDetail.value =
                result.exceptionOrNull()?.localizedMessage ?: "সংযোগ ব্যর্থ"
        }
        return result
    }

    fun disconnectElevenLabs() {
        _voiceStatus.value = VoiceEngineStatus.FALLBACK_TTS_READY
        _voiceStatusDetail.value = "বাংলা নারী ভয়েস প্রস্তুত (Device TTS)"
    }

    fun shutdown() {
        stopSpeaking()
        workerJob?.cancel()
        fallbackTts.shutdown()
    }
}
