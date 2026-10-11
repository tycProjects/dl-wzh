package dev.tajim.jarvis.assistant

import dev.tajim.jarvis.ai.AiException
import dev.tajim.jarvis.ai.AiService
import dev.tajim.jarvis.data.SettingsRepository
import dev.tajim.jarvis.phone.ActionResult
import dev.tajim.jarvis.phone.CommandRouter
import dev.tajim.jarvis.phone.PhoneControl
import dev.tajim.jarvis.voice.Speaker
import dev.tajim.jarvis.voice.VoiceController
import dev.tajim.jarvis.voice.VoiceMessage
import dev.tajim.jarvis.voice.WakeWordEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AssistantMode { OFF, ASLEEP, AWAKE }
enum class AssistantPhase { IDLE, LISTENING, THINKING, SPEAKING }

data class Notice(val text: String, val kind: Kind = Kind.INFO) {
    enum class Kind { INFO, MIC_PERMISSION }
}

data class AssistantState(
    val mode: AssistantMode = AssistantMode.OFF,
    val phase: AssistantPhase = AssistantPhase.IDLE,
    val lastHeard: String = "",
    val lastReply: String = "",
    val notice: Notice? = null,
) {
    val busy: Boolean get() = phase != AssistantPhase.IDLE
}

/**
 * The assistant's brain loop. Modes: OFF (not running), ASLEEP (only the wake word "Jarvis" is heard), AWAKE (listens
 * for commands). Saying "sleep" returns to ASLEEP; saying "Jarvis" wakes it again. Runs on the main-thread app scope and
 * is kept alive in the background by [AssistantService]. Each turn: listen, route to phone control or AI, speak the answer.
 */
class AssistantController(
    private val scope: CoroutineScope,
    private val settings: SettingsRepository,
    private val ai: AiService,
    private val voice: VoiceController,
    private val speaker: Speaker,
    private val wake: WakeWordEngine,
    private val phone: PhoneControl,
    private val router: CommandRouter,
) {
    private val _state = MutableStateFlow(AssistantState())
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private var loop: Job? = null
    private var oneShot: Job? = null
    private val wakeSignal = Channel<Unit>(Channel.CONFLATED)
    private var sleepRequested = false
    private var pending: ActionResult.Confirm? = null

    val isRunning: Boolean get() = loop?.isActive == true

    private fun set(transform: (AssistantState) -> AssistantState) = _state.update(transform)
    private fun phase(p: AssistantPhase) = set { it.copy(phase = p) }
    private suspend fun lang(): String? = settings.current().voiceLanguage.ifBlank { null }

    // ------------------------------------------------------------ lifecycle
    fun start() {
        if (isRunning) return
        oneShot?.cancel()
        sleepRequested = false
        set { it.copy(mode = AssistantMode.ASLEEP, phase = AssistantPhase.IDLE, notice = null) }
        loop = scope.launch {
            try {
                while (true) {
                    asleep()
                    awake()
                }
            } finally {
                voice.cancel()
                speaker.stop()
            }
        }
    }

    fun stop() {
        loop?.cancel(); loop = null
        oneShot?.cancel(); oneShot = null
        voice.cancel(); speaker.stop()
        pending = null
        _state.value = AssistantState()
    }

    /** Wake from sleep (notification button, Home button). */
    fun wakeNow() { if (_state.value.mode == AssistantMode.ASLEEP) wakeSignal.trySend(Unit) }

    /** Go to sleep from AWAKE (notification button). */
    fun sleepNow() {
        if (_state.value.mode == AssistantMode.AWAKE) { sleepRequested = true; speaker.stop(); voice.cancel() }
    }

    /** Orb / mic tap. Wakes if asleep, runs a single command if the assistant is off, stops speech/listening if busy. */
    fun tapToTalk() {
        val s = _state.value
        when {
            s.mode == AssistantMode.ASLEEP -> wakeSignal.trySend(Unit)
            s.busy -> { speaker.stop(); voice.stop() }
            s.mode == AssistantMode.OFF -> if (oneShot?.isActive != true) oneShot = scope.launch { runOnce() }
        }
    }

    fun clearNotice() = set { it.copy(notice = null) }

    fun micDenied() = set { it.copy(notice = Notice("Microphone permission is off.", Notice.Kind.MIC_PERMISSION)) }

    // ------------------------------------------------------------ phases
    private suspend fun asleep() = coroutineScope {
        sleepRequested = false
        set { it.copy(mode = AssistantMode.ASLEEP, phase = AssistantPhase.IDLE) }
        val s = settings.current()
        val detector = launch {
            if (s.wakeWordEnabled && wake.ensureReady()) {
                set { it.copy(notice = null) }
                if (wake.listenFor(setOf("jarvis")) != null) wakeSignal.trySend(Unit)
            } else {
                set { it.copy(notice = Notice(if (!s.wakeWordEnabled) "Wake word is off. Use the Wake button to wake me." else "Wake-word model not found. Copy assets/model-en-us into the app, or use the Wake button.")) }
            }
        }
        wakeSignal.receive()
        detector.cancelAndJoin()
    }

    private suspend fun awake() {
        set { it.copy(mode = AssistantMode.AWAKE) }
        reply("Yes?")
        var quiet = 0
        var failures = 0
        while (!sleepRequested) {
            phase(AssistantPhase.LISTENING)
            val r = voice.listenOnce(lang())
            phase(AssistantPhase.IDLE)
            if (sleepRequested) break
            when (r.message) {
                VoiceMessage.PERMISSION_DENIED -> { set { it.copy(notice = Notice("Microphone permission is off.", Notice.Kind.MIC_PERMISSION)) }; return }
                VoiceMessage.UNAVAILABLE -> { set { it.copy(notice = Notice("Speech recognition is not available on this phone.")) }; return }
                VoiceMessage.NETWORK, VoiceMessage.BUSY, VoiceMessage.AUDIO, VoiceMessage.GENERIC, VoiceMessage.LANGUAGE -> {
                    if (++failures >= 3) { set { it.copy(notice = Notice("Speech recognition keeps failing, so I went to sleep. Check your connection or language.")) }; return }
                    delay(1_500); continue
                }
                else -> failures = 0
            }
            val text = r.transcript
            if (text.isBlank()) {
                if (++quiet >= 3) { reply("Going to sleep. Say Jarvis to wake me."); return }
                continue
            }
            quiet = 0
            voice.clear()
            if (handle(text)) return
        }
    }

    /** One command from a tap while the assistant is off. */
    private suspend fun runOnce() {
        try {
            phase(AssistantPhase.LISTENING)
            val r = voice.listenOnce(lang())
            phase(AssistantPhase.IDLE)
            val text = r.transcript
            voice.clear()
            when {
                r.message == VoiceMessage.PERMISSION_DENIED -> set { it.copy(notice = Notice("Microphone permission is off.", Notice.Kind.MIC_PERMISSION)) }
                r.message != null && text.isBlank() -> set { it.copy(notice = Notice(describe(r.message))) }
                text.isNotBlank() -> handle(text)
            }
        } finally {
            phase(AssistantPhase.IDLE)
        }
    }

    private fun describe(m: VoiceMessage) = when (m) {
        VoiceMessage.NO_SPEECH -> "I did not catch that. Tap the orb and speak again."
        VoiceMessage.NETWORK -> "Speech recognition needs an internet connection."
        VoiceMessage.BUSY -> "The speech service is busy. Try again."
        VoiceMessage.AUDIO -> "Could not use the microphone."
        VoiceMessage.LANGUAGE -> "That language is not available for speech recognition. Change it in Settings."
        VoiceMessage.UNAVAILABLE -> "Speech recognition is not available on this phone."
        VoiceMessage.PERMISSION_DENIED -> "Microphone permission is off."
        VoiceMessage.GENERIC -> "Speech recognition failed. Try again."
    }

    // ------------------------------------------------------------ one turn; returns true to go to sleep
    private suspend fun handle(text: String): Boolean {
        set { it.copy(lastHeard = text, notice = null) }
        val n = router.normalize(text)

        pending?.let { p ->
            pending = null
            if (router.isYes(n)) {
                phase(AssistantPhase.THINKING)
                when (val res = p.run()) {
                    is ActionResult.Done -> reply(res.message)
                    is ActionResult.Failed -> reply(res.message)
                    is ActionResult.Confirm -> reply(res.prompt)
                }
            } else {
                reply("Okay, cancelled.")
            }
            return false
        }

        if (router.isSleep(n)) { reply("Going to sleep. Say Jarvis to wake me."); return true }

        val cmd = router.parse(text)
        phase(AssistantPhase.THINKING)
        if (cmd != null) {
            when (val res = phone.execute(cmd)) {
                is ActionResult.Done -> reply(res.message)
                is ActionResult.Failed -> reply(res.message)
                is ActionResult.Confirm -> { pending = res; reply(res.prompt + " Say yes to confirm.") }
            }
            return false
        }

        val result = ai.ask(text)
        result.fold(
            onSuccess = { reply(it) },
            onFailure = { e ->
                if (e is CancellationException) throw e
                reply(if (e is AiException) e.message.orEmpty() else "Something went wrong: ${e.message}")
            },
        )
        return false
    }

    private suspend fun reply(text: String) {
        set { it.copy(lastReply = text) }
        val s = settings.current()
        if (s.speakReplies) {
            phase(AssistantPhase.SPEAKING)
            speaker.speak(text, s.speechRate)
        }
        phase(AssistantPhase.IDLE)
    }
}
