package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiBrain
import com.example.ai.GeminiConnectionStatus
import com.example.ai.StreamChunk
import com.example.connect.PcCompanionServer
import com.example.connect.PcConnectionState
import com.example.data.ChatMessageEntity
import com.example.data.ChatSessionEntity
import com.example.data.CommandAliasEntity
import com.example.data.MayaDatabase
import com.example.data.MayaPreferences
import com.example.data.MayaRepository
import com.example.data.MayaSettings
import com.example.device.AppLaunchOutcome
import com.example.device.AppLauncherManager
import com.example.device.AuditedPermission
import com.example.device.DeviceControlManager
import com.example.device.DeviceTelemetry
import com.example.device.InstalledApp
import com.example.device.LocalCommandEngine
import com.example.device.LocalIntentAction
import com.example.device.PermissionAuditor
import com.example.service.BackgroundAssistantRegistry
import com.example.service.BackgroundModeState
import com.example.service.MayaBackgroundService
import com.example.service.MayaScreenShareService
import com.example.service.ScreenShareStateHolder
import com.example.voice.ElevenLabsVoiceOption
import com.example.voice.MayaSpeechRecognizer
import com.example.voice.MayaVoiceManager
import com.example.voice.SpeechState
import com.example.voice.VoiceEngineStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class MayaTab {
    HOME,
    DEVICE,
    CHAT,
    TOOLS
}

enum class AssistantMode {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

sealed interface UiNavigationEffect {
    data class ScrollScreen(val direction: String) : UiNavigationEffect
    data object RequestMediaProjection : UiNavigationEffect
    data class ToastMessage(val message: String) : UiNavigationEffect
}

data class PendingConfirmation(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

data class AmbiguousAppRequest(
    val query: String,
    val candidates: List<InstalledApp>
)

data class AutomationState(
    val isRunning: Boolean = false,
    val statusText: String = "",
    val steps: List<String> = emptyList(),
    val currentStepIndex: Int = 0
)

class MayaViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MayaViewModel"
    }

    private val appContext = application.applicationContext
    private val database = MayaDatabase.getInstance(appContext)
    private val repository = MayaRepository(database)
    val preferences = MayaPreferences(appContext)

    val geminiBrain = GeminiBrain()
    val voiceManager = MayaVoiceManager(appContext)
    val speechRecognizer = MayaSpeechRecognizer(appContext)
    val appLauncher = AppLauncherManager(appContext)
    val deviceControl = DeviceControlManager(appContext)
    val permissionAuditor = PermissionAuditor(appContext)

    val pcServer = PcCompanionServer(
        context = appContext,
        deviceControl = deviceControl,
        onRemoteCommand = { cmd ->
            viewModelScope.launch {
                submitCommand(cmd, speakReply = true, hostActivity = null)
            }
        }
    )

    val settings: StateFlow<MayaSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, MayaSettings())

    val hasCustomGeminiKey: StateFlow<Boolean> = preferences.hasCustomGeminiKey
    val hasElevenLabsKey: StateFlow<Boolean> = preferences.hasElevenLabsKey

    val sessions: StateFlow<List<ChatSessionEntity>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aliases: StateFlow<List<CommandAliasEntity>> = repository.aliases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _activeTab = MutableStateFlow(MayaTab.HOME)
    val activeTab: StateFlow<MayaTab> = _activeTab.asStateFlow()

    private val _isVoiceHideMode = MutableStateFlow(false)
    val isVoiceHideMode: StateFlow<Boolean> = _isVoiceHideMode.asStateFlow()

    private val _assistantMode = MutableStateFlow(AssistantMode.IDLE)
    val assistantMode: StateFlow<AssistantMode> = _assistantMode.asStateFlow()

    private val _statusHeadline = MutableStateFlow("আমি প্রস্তুত, কিছু বলো…")
    val statusHeadline: StateFlow<String> = _statusHeadline.asStateFlow()

    private val _lastMayaReply = MutableStateFlow("হ্যালো! আমি মায়া। তোমাকে আজ কীভাবে সাহায্য করতে পারি?")
    val lastMayaReply: StateFlow<String> = _lastMayaReply.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _telemetry = MutableStateFlow(deviceControl.collectTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _showAppLauncherSheet = MutableStateFlow(false)
    val showAppLauncherSheet: StateFlow<Boolean> = _showAppLauncherSheet.asStateFlow()

    private val _auditedPermissions = MutableStateFlow<List<AuditedPermission>>(emptyList())
    val auditedPermissions: StateFlow<List<AuditedPermission>> = _auditedPermissions.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _ambiguousAppRequest = MutableStateFlow<AmbiguousAppRequest?>(null)
    val ambiguousAppRequest: StateFlow<AmbiguousAppRequest?> = _ambiguousAppRequest.asStateFlow()

    private val _automationState = MutableStateFlow(AutomationState())
    val automationState: StateFlow<AutomationState> = _automationState.asStateFlow()

    private val _navEffects = MutableSharedFlow<UiNavigationEffect>(extraBufferCapacity = 8)
    val navEffects: SharedFlow<UiNavigationEffect> = _navEffects.asSharedFlow()

    val geminiStatus: StateFlow<GeminiConnectionStatus> = geminiBrain.status
    val voiceStatus: StateFlow<VoiceEngineStatus> = voiceManager.voiceStatus
    val voiceStatusDetail: StateFlow<String> = voiceManager.voiceStatusDetail
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val speechAmplitude: StateFlow<Float> = voiceManager.speechAmplitude
    val availableElevenLabsVoices: StateFlow<List<ElevenLabsVoiceOption>> = voiceManager.availableVoices
    val speechRmsLevel: StateFlow<Float> = speechRecognizer.rmsLevel
    val backgroundState: StateFlow<BackgroundModeState> = BackgroundAssistantRegistry.state
    val batterySaverActive: StateFlow<Boolean> = BackgroundAssistantRegistry.batterySaverActive
    val isScreenSharing: StateFlow<Boolean> = ScreenShareStateHolder.isSharing
    val screenShareFrameBytes: StateFlow<ByteArray?> = ScreenShareStateHolder.latestFrameBytes
    val pcConnectionState: StateFlow<PcConnectionState> = pcServer.state

    private var activeGenerationJob: Job? = null
    private var activeAutomationJob: Job? = null
    private var sessionObserverJob: Job? = null
    private var lastActivityRef: Activity? = null

    init {
        viewModelScope.launch {
            ensureActiveSession()
            refreshInstalledApps()
            refreshTelemetryAndPermissions()
        }

        // Observe speech recognition states
        viewModelScope.launch {
            speechRecognizer.state.collect { state ->
                when (state) {
                    is SpeechState.Listening -> {
                        _assistantMode.value = AssistantMode.LISTENING
                        _statusHeadline.value = "আমি শুনছি…"
                    }
                    is SpeechState.Partial -> {
                        _assistantMode.value = AssistantMode.LISTENING
                        _liveTranscript.value = state.text
                    }
                    is SpeechState.FinalResult -> {
                        _liveTranscript.value = state.text
                    }
                    is SpeechState.Error -> {
                        _assistantMode.value = AssistantMode.ERROR
                        _statusHeadline.value = state.message
                    }
                    is SpeechState.Idle -> {
                        if (_assistantMode.value == AssistantMode.LISTENING) {
                            _assistantMode.value = AssistantMode.IDLE
                            _statusHeadline.value = "আমি প্রস্তুত"
                        }
                    }
                }
            }
        }

        // Observe voice speaking state for Maya visual mode
        viewModelScope.launch {
            combine(voiceManager.isSpeaking, _assistantMode) { speaking, mode ->
                speaking to mode
            }.collect { (speaking, mode) ->
                if (speaking) {
                    _assistantMode.value = AssistantMode.SPEAKING
                    _statusHeadline.value = "মায়া কথা বলছে…"
                } else if (mode == AssistantMode.SPEAKING) {
                    _assistantMode.value = AssistantMode.IDLE
                    _statusHeadline.value = "আমি প্রস্তুত, কিছু বলো…"
                    if (_isVoiceHideMode.value && settings.value.autoListenAfterReply &&
                        permissionAuditor.hasMicrophonePermission()
                    ) {
                        delay(250L)
                        startVoiceListening(lastActivityRef)
                    }
                }
            }
        }
    }

    private suspend fun ensureActiveSession(): Long {
        val existingId = _currentSessionId.value
        if (existingId != null) return existingId
        val firstId = repository.createSession("মায়ার সাথে কথা")
        selectSession(firstId)
        return firstId
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        sessionObserverJob?.cancel()
        sessionObserverJob = viewModelScope.launch {
            repository.observeMessages(sessionId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val newId = repository.createSession("নতুন কথোপকথন")
            selectSession(newId)
            _lastMayaReply.value = "নতুন চ্যাট শুরু হয়েছে। কী জানতে চাও বলো?"
        }
    }

    fun clearCurrentChat() {
        viewModelScope.launch {
            val id = _currentSessionId.value ?: return@launch
            repository.clearMessages(id)
            _lastMayaReply.value = "চ্যাট পরিষ্কার করা হয়েছে।"
        }
    }

    fun selectTab(tab: MayaTab) {
        _activeTab.value = tab
        if (tab == MayaTab.DEVICE || tab == MayaTab.TOOLS) {
            refreshTelemetryAndPermissions()
        }
    }

    fun openVoiceHideMode(activity: Activity? = null) {
        lastActivityRef = activity
        _isVoiceHideMode.value = true
        if (permissionAuditor.hasMicrophonePermission()) {
            startVoiceListening(activity)
        } else {
            _statusHeadline.value = "ভয়েস ব্যবহারের জন্য মাইক্রোফোন অনুমতি দাও"
        }
    }

    fun exitVoiceHideMode(goToChat: Boolean = false) {
        speechRecognizer.stopListening()
        _isVoiceHideMode.value = false
        if (goToChat) {
            _activeTab.value = MayaTab.CHAT
        }
    }

    fun startVoiceListening(activity: Activity? = null) {
        lastActivityRef = activity
        if (!permissionAuditor.hasMicrophonePermission()) {
            _assistantMode.value = AssistantMode.ERROR
            _statusHeadline.value = "মাইক্রোফোন ব্যবহারের অনুমতি লাগবে।"
            return
        }
        // Interrupt Maya immediately when user wants to speak
        interruptMaya()
        _liveTranscript.value = ""
        val lang = when (settings.value.languageMode) {
            "en" -> "en-US"
            else -> "bn-BD"
        }
        speechRecognizer.startListening(lang) { recognizedText ->
            submitCommand(recognizedText, speakReply = true, hostActivity = activity)
        }
    }

    fun stopVoiceListening() {
        speechRecognizer.stopListening()
        _assistantMode.value = AssistantMode.IDLE
        _statusHeadline.value = "আমি প্রস্তুত"
    }

    fun interruptMaya() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        voiceManager.stopSpeaking()
        _streamingText.value = ""
        if (_assistantMode.value == AssistantMode.THINKING || _assistantMode.value == AssistantMode.SPEAKING) {
            _assistantMode.value = AssistantMode.IDLE
            _statusHeadline.value = "থামিয়ে দিয়েছি, বলো…"
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _streamingText.value = ""
        _assistantMode.value = AssistantMode.IDLE
        _statusHeadline.value = "উত্তর তৈরি থামানো হয়েছে"
    }

    fun submitCommand(
        rawText: String,
        speakReply: Boolean = true,
        hostActivity: Activity? = null,
        includeScreenFrame: Boolean = false
    ) {
        val input = rawText.trim()
        if (input.isEmpty()) return
        lastActivityRef = hostActivity

        interruptMaya()

        viewModelScope.launch {
            val sessionId = ensureActiveSession()
            repository.addMessage(sessionId, role = "user", text = input, sourceTag = "user")
            if (_messages.value.size <= 1) {
                repository.renameSession(sessionId, input)
            }

            val currentSettings = settings.value
            val currentAliases = repository.getEnabledAliases()

            if (currentSettings.fastLocalCommands && !includeScreenFrame) {
                val localIntent = LocalCommandEngine.parse(input, currentAliases)
                if (localIntent !is LocalIntentAction.FallbackToGemini) {
                    executeLocalIntent(localIntent, sessionId, speakReply, hostActivity)
                    return@launch
                }
            }

            // Otherwise route to Gemini AI Brain with streaming
            runGeminiStreaming(
                prompt = input,
                sessionId = sessionId,
                speakReply = speakReply,
                includeScreenFrame = includeScreenFrame
            )
        }
    }

    private suspend fun executeLocalIntent(
        intent: LocalIntentAction,
        sessionId: Long,
        speakReply: Boolean,
        hostActivity: Activity?
    ) {
        when (intent) {
            is LocalIntentAction.SensitiveConfirmationRequired -> {
                _pendingConfirmation.value = PendingConfirmation(
                    title = "নিরাপত্তা নিশ্চিতকরণ",
                    message = intent.actionSummaryBn,
                    onConfirm = {
                        viewModelScope.launch {
                            runGeminiStreaming(
                                prompt = intent.rawText,
                                sessionId = sessionId,
                                speakReply = speakReply,
                                includeScreenFrame = false
                            )
                        }
                    }
                )
                val msg = "এটি একটি সংবেদনশীল কাজ—অনুমোদন দিলে আমি এগিয়ে যাব।"
                recordAndSpeakLocalReply(sessionId, msg, speakReply)
            }

            is LocalIntentAction.AutomationSequence -> {
                executeAutomationSequence(intent.steps, sessionId, speakReply, hostActivity)
            }

            is LocalIntentAction.LaunchApp -> {
                val outcome = if (!intent.explicitPackage.isNullOrBlank()) {
                    appLauncher.launchPackage(intent.explicitPackage)
                } else {
                    appLauncher.findAndLaunch(intent.query)
                }
                val reply = when (outcome) {
                    is AppLaunchOutcome.Launched -> "${outcome.app.label} চালু করেছি।"
                    is AppLaunchOutcome.Ambiguous -> {
                        _ambiguousAppRequest.value = AmbiguousAppRequest(outcome.query, outcome.candidates)
                        "একই নামের একাধিক অ্যাপ আছে, কোনটা খুলবো বেছে নাও।"
                    }
                    is AppLaunchOutcome.NotInstalled -> "অ্যাপটি তোমার ফোনে ইনস্টল করা নেই।"
                    is AppLaunchOutcome.Failed -> "${outcome.appLabel} চালু করা যায়নি: ${outcome.reason}"
                }
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.Flashlight -> {
                val res = if (intent.turnOn == null) {
                    deviceControl.toggleFlashlight()
                } else {
                    deviceControl.setFlashlight(intent.turnOn)
                }
                refreshTelemetryAndPermissions()
                val reply = res.fold(
                    onSuccess = { on -> if (on) "ফ্ল্যাশলাইট চালু করেছি।" else "ফ্ল্যাশলাইট বন্ধ করেছি।" },
                    onFailure = { err -> err.localizedMessage ?: "ফ্ল্যাশলাইট এই ফোনে সাপোর্ট করছে না।" }
                )
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.VolumeChange -> {
                val reply = when {
                    intent.mute == true -> {
                        deviceControl.setMediaMuted(true)
                        "সাউন্ড মিউট করেছি।"
                    }
                    intent.mute == false -> {
                        deviceControl.setMediaMuted(false)
                        "সাউন্ড আনমিউট করেছি।"
                    }
                    intent.targetPercent != null -> {
                        val actual = deviceControl.setMediaVolumePercent(intent.targetPercent)
                        "ভলিউম $actual পার্সেন্ট করেছি।"
                    }
                    intent.deltaPercent != null && intent.deltaPercent != 0 -> {
                        val actual = deviceControl.adjustMediaVolume(intent.deltaPercent)
                        if (intent.deltaPercent > 0) "ভলিউম বাড়িয়ে $actual% করেছি।"
                        else "ভলিউম কমিয়ে $actual% করেছি।"
                    }
                    else -> "বর্তমান ভলিউম ${deviceControl.getMediaVolumePercent()}%।"
                }
                refreshTelemetryAndPermissions()
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.BrightnessChange -> {
                val current = deviceControl.getBrightnessPercent()
                val target = when {
                    intent.targetPercent != null -> intent.targetPercent
                    intent.deltaPercent != null && intent.deltaPercent != 0 -> current + intent.deltaPercent
                    else -> current
                }
                val reply = if (intent.deltaPercent == 0 && intent.targetPercent == null) {
                    "বর্তমান ব্রাইটনেস $current%।"
                } else {
                    val res = deviceControl.setBrightnessPercent(target, hostActivity)
                    res.fold(
                        onSuccess = { pct -> "ব্রাইটনেস $pct% করেছি।" },
                        onFailure = {
                            deviceControl.openWriteSettingsPermission()
                            "সিস্টেম ব্রাইটনেস বদলাতে সেটিংস থেকে Modify System Settings অনুমতি চালু করো।"
                        }
                    )
                }
                refreshTelemetryAndPermissions()
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.QueryBattery -> {
                val b = deviceControl.getBatteryStatus()
                refreshTelemetryAndPermissions()
                val tempPart = b.temperatureC?.let { " তাপমাত্রা ${String.format(Locale.US, "%.1f", it)}°C।" } ?: ""
                val chargePart = if (b.isCharging) "ফোন চার্জ হচ্ছে (${b.chargeSource})।" else "চার্জার লাগানো নেই।"
                val reply = "তোমার ব্যাটারি এখন ${b.levelPercent}%। $chargePart$tempPart"
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.QueryStorage -> {
                val s = deviceControl.getStorageStatus()
                refreshTelemetryAndPermissions()
                val reply = "মোট স্টোরেজ ${String.format(Locale.US, "%.1f", s.totalGb)} GB, " +
                    "খালি আছে ${String.format(Locale.US, "%.1f", s.freeGb)} GB (${100 - s.usedPercent}% ফ্রি)।"
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.QueryNetwork -> {
                val n = deviceControl.getNetworkStatus()
                refreshTelemetryAndPermissions()
                val reply = if (n.isConnected) {
                    "হ্যাঁ, ইন্টারনেট সংযোগ আছে (${n.transportType})।"
                } else {
                    "ইন্টারনেট সংযোগ পাচ্ছি না।"
                }
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.QueryDeviceInfo -> {
                val t = deviceControl.collectTelemetry()
                refreshTelemetryAndPermissions()
                val reply = "ডিভাইস: ${t.brand} ${t.model}, Android ${t.androidVersion}, " +
                    "RAM ফ্রি ${String.format(Locale.US, "%.1f", t.availableRamGb)}/${String.format(Locale.US, "%.1f", t.totalRamGb)} GB।"
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.OpenSettings -> {
                val ok = deviceControl.openSettingsShortcut(intent.target)
                val reply = if (ok) "${intent.labelBn} সেটিংস খুলে দিয়েছি।"
                else "এই সেটিংস প্যানেলটি খোলা যায়নি।"
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.InAppNavigation -> {
                val reply = when (intent.destination) {
                    "home" -> {
                        _isVoiceHideMode.value = false
                        _activeTab.value = MayaTab.HOME
                        "হোম স্ক্রিনে চলে এসেছি।"
                    }
                    "device" -> {
                        _isVoiceHideMode.value = false
                        _activeTab.value = MayaTab.DEVICE
                        "ডিভাইস কন্ট্রোল পেজে যাচ্ছি।"
                    }
                    "chat" -> {
                        _isVoiceHideMode.value = false
                        _activeTab.value = MayaTab.CHAT
                        "চ্যাট পেজে নিয়ে যাচ্ছি।"
                    }
                    "tools" -> {
                        _isVoiceHideMode.value = false
                        _activeTab.value = MayaTab.TOOLS
                        "টুলস ও সেটিংস খুলেছি।"
                    }
                    "back" -> {
                        if (_isVoiceHideMode.value) {
                            _isVoiceHideMode.value = false
                        } else if (_activeTab.value != MayaTab.HOME) {
                            _activeTab.value = MayaTab.HOME
                        }
                        "আগের পেজে ফিরে এসেছি।"
                    }
                    "scroll" -> {
                        _navEffects.tryEmit(UiNavigationEffect.ScrollScreen(intent.scrollDirection ?: "down"))
                        if (intent.scrollDirection == "up") "উপরে স্ক্রল করেছি।" else "নিচে স্ক্রল করেছি।"
                    }
                    else -> "নেভিগেশন সম্পন্ন হয়েছে।"
                }
                recordAndSpeakLocalReply(sessionId, reply, speakReply)
            }

            is LocalIntentAction.StartScreenShare -> {
                _navEffects.tryEmit(UiNavigationEffect.RequestMediaProjection)
                recordAndSpeakLocalReply(sessionId, "স্ক্রিন শেয়ার শুরু করার অনুমতি দাও।", speakReply)
            }

            is LocalIntentAction.StopScreenShare -> {
                stopScreenShare()
                recordAndSpeakLocalReply(sessionId, "স্ক্রিন শেয়ার বন্ধ করেছি।", speakReply)
            }

            is LocalIntentAction.FallbackToGemini -> {
                // Already handled above
            }
        }
    }

    private fun executeAutomationSequence(
        steps: List<LocalIntentAction>,
        sessionId: Long,
        speakReply: Boolean,
        hostActivity: Activity?
    ) {
        activeAutomationJob?.cancel()
        activeAutomationJob = viewModelScope.launch {
            val stepDescriptions = steps.mapIndexed { idx, act ->
                "ধাপ ${idx + 1}: ${describeAction(act)}"
            }
            _automationState.value = AutomationState(
                isRunning = true,
                statusText = "অটোমেশন চলছে…",
                steps = stepDescriptions,
                currentStepIndex = 0
            )
            try {
                for ((idx, step) in steps.withIndex()) {
                    _automationState.value = _automationState.value.copy(
                        currentStepIndex = idx,
                        statusText = "চলছে: ${stepDescriptions[idx]}"
                    )
                    executeLocalIntent(step, sessionId, speakReply = false, hostActivity = hostActivity)
                    delay(700L)
                }
                _automationState.value = AutomationState(isRunning = false, statusText = "সব ধাপ সম্পন্ন হয়েছে")
                recordAndSpeakLocalReply(sessionId, "তোমার বলা সব ধাপ সম্পন্ন করেছি।", speakReply)
            } catch (e: CancellationException) {
                _automationState.value = AutomationState(isRunning = false, statusText = "অটোমেশন বন্ধ করা হয়েছে")
                throw e
            }
        }
    }

    fun stopAutomation() {
        activeAutomationJob?.cancel()
        activeAutomationJob = null
        _automationState.value = AutomationState(isRunning = false, statusText = "ব্যবহারকারী দ্বারা থামানো হয়েছে")
    }

    private fun describeAction(action: LocalIntentAction): String = when (action) {
        is LocalIntentAction.LaunchApp -> "${action.query} খোলা"
        is LocalIntentAction.Flashlight -> if (action.turnOn == true) "ফ্ল্যাশলাইট চালু" else "ফ্ল্যাশলাইট বন্ধ"
        is LocalIntentAction.VolumeChange -> "ভলিউম পরিবর্তন"
        is LocalIntentAction.BrightnessChange -> "ব্রাইটনেস পরিবর্তন"
        is LocalIntentAction.QueryBattery -> "ব্যাটারি চেক"
        is LocalIntentAction.QueryStorage -> "স্টোরেজ চেক"
        is LocalIntentAction.QueryNetwork -> "নেটওয়ার্ক চেক"
        is LocalIntentAction.OpenSettings -> "${action.labelBn} সেটিংস"
        is LocalIntentAction.InAppNavigation -> "স্ক্রিন নেভিগেশন (${action.destination})"
        else -> "কমান্ড"
    }

    private suspend fun recordAndSpeakLocalReply(sessionId: Long, reply: String, speakReply: Boolean) {
        _lastMayaReply.value = reply
        _assistantMode.value = AssistantMode.IDLE
        _statusHeadline.value = reply
        repository.addMessage(sessionId, role = "maya", text = reply, sourceTag = "local")
        if (speakReply) {
            voiceManager.speakImmediately(
                text = reply,
                settings = settings.value,
                elevenLabsKey = preferences.getElevenLabsApiKey()
            )
        }
    }

    private fun runGeminiStreaming(
        prompt: String,
        sessionId: Long,
        speakReply: Boolean,
        includeScreenFrame: Boolean
    ) {
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            _assistantMode.value = AssistantMode.THINKING
            _statusHeadline.value = "মায়া ভাবছে…"
            _streamingText.value = ""

            val currentSettings = settings.value
            val customKey = if (currentSettings.useCustomGeminiKey) {
                preferences.getCustomGeminiApiKey().takeIf { it.isNotBlank() }
            } else null

            val history = repository.getMessagesSnapshot(sessionId).dropLast(1)
            val screenBmp = if (includeScreenFrame) ScreenShareStateHolder.latestBitmap else null
            val deviceSummary = deviceControl.buildContextSummary()

            val sentenceBuffer = StringBuilder()
            val messageId = repository.addMessage(sessionId, role = "maya", text = "…", sourceTag = "gemini")

            try {
                geminiBrain.streamResponse(
                    userMessage = prompt,
                    history = history,
                    deviceContextSummary = deviceSummary,
                    modelName = currentSettings.geminiModel,
                    languageMode = currentSettings.languageMode,
                    customApiKey = customKey,
                    screenBitmap = screenBmp
                ).collect { chunk ->
                    when (chunk) {
                        is StreamChunk.Delta -> {
                            _streamingText.value = chunk.accumulatedText
                            _lastMayaReply.value = chunk.accumulatedText
                            repository.updateMessageText(messageId, chunk.accumulatedText)

                            if (speakReply && currentSettings.streamingTts) {
                                sentenceBuffer.append(chunk.deltaText)
                                val bufStr = sentenceBuffer.toString()
                                val boundaryIdx = bufStr.indexOfAny(charArrayOf('।', '.', '!', '?', '\n'))
                                if (boundaryIdx >= 8) {
                                    val toSpeak = bufStr.substring(0, boundaryIdx + 1).trim()
                                    val rest = bufStr.substring(boundaryIdx + 1)
                                    sentenceBuffer.clear()
                                    sentenceBuffer.append(rest)
                                    voiceManager.enqueueSpeech(
                                        text = toSpeak,
                                        settings = currentSettings,
                                        elevenLabsKey = preferences.getElevenLabsApiKey()
                                    )
                                }
                            }
                        }
                        is StreamChunk.Completed -> {
                            _streamingText.value = ""
                            _lastMayaReply.value = chunk.fullText
                            repository.updateMessageText(messageId, chunk.fullText)

                            if (speakReply) {
                                val remaining = if (currentSettings.streamingTts) {
                                    sentenceBuffer.toString().trim()
                                } else {
                                    chunk.fullText
                                }
                                if (remaining.isNotEmpty()) {
                                    voiceManager.enqueueSpeech(
                                        text = remaining,
                                        settings = currentSettings,
                                        elevenLabsKey = preferences.getElevenLabsApiKey()
                                    )
                                }
                            } else {
                                _assistantMode.value = AssistantMode.IDLE
                                _statusHeadline.value = "আমি প্রস্তুত, কিছু বলো…"
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini streaming failed", e)
                val errorText = e.localizedMessage ?: "ইন্টারনেট সংযোগ পাচ্ছি না।"
                _streamingText.value = ""
                _lastMayaReply.value = errorText
                _assistantMode.value = AssistantMode.ERROR
                _statusHeadline.value = errorText
                repository.updateMessageText(messageId, errorText)
                if (speakReply) {
                    voiceManager.speakImmediately(
                        text = errorText,
                        settings = currentSettings,
                        elevenLabsKey = preferences.getElevenLabsApiKey()
                    )
                }
            }
        }
    }

    fun speakMessage(text: String) {
        voiceManager.speakImmediately(
            text = text,
            settings = settings.value,
            elevenLabsKey = preferences.getElevenLabsApiKey()
        )
    }

    fun launchInstalledApp(app: InstalledApp) {
        _ambiguousAppRequest.value = null
        val outcome = appLauncher.launchPackage(app)
        val msg = when (outcome) {
            is AppLaunchOutcome.Launched -> "${outcome.app.label} চালু করেছি।"
            is AppLaunchOutcome.Failed -> "${outcome.appLabel} খোলা যায়নি: ${outcome.reason}"
            else -> "অ্যাপটি তোমার ফোনে ইনস্টল করা নেই।"
        }
        _navEffects.tryEmit(UiNavigationEffect.ToastMessage(msg))
    }

    fun dismissAmbiguousAppDialog() {
        _ambiguousAppRequest.value = null
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }

    fun confirmPendingAction() {
        val action = _pendingConfirmation.value?.onConfirm
        _pendingConfirmation.value = null
        action?.invoke()
    }

    fun toggleAppLauncherSheet(show: Boolean) {
        _showAppLauncherSheet.value = show
        if (show) refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = appLauncher.getLaunchableApps(forceRefresh = true)
        }
    }

    fun refreshTelemetryAndPermissions() {
        _telemetry.value = deviceControl.collectTelemetry()
        _auditedPermissions.value = permissionAuditor.auditAll(
            isScreenSharingActive = ScreenShareStateHolder.isSharing.value,
            isBackgroundServiceActive = BackgroundAssistantRegistry.state.value != BackgroundModeState.STOPPED
        )
    }

    fun toggleFlashlight() {
        val res = deviceControl.toggleFlashlight()
        refreshTelemetryAndPermissions()
        res.onFailure {
            _navEffects.tryEmit(UiNavigationEffect.ToastMessage(it.localizedMessage ?: "ফ্ল্যাশলাইট সাপোর্ট নেই"))
        }
    }

    fun setVolumePercent(percent: Int) {
        deviceControl.setMediaVolumePercent(percent)
        refreshTelemetryAndPermissions()
    }

    fun setBrightnessPercent(percent: Int, activity: Activity?) {
        val res = deviceControl.setBrightnessPercent(percent, activity)
        refreshTelemetryAndPermissions()
        if (res.isFailure) {
            _navEffects.tryEmit(UiNavigationEffect.ToastMessage("সিস্টেম ব্রাইটনেস পারমিশন দিলে পুরো ফোনে কাজ করবে"))
        }
    }

    fun startBackgroundAssistant() {
        if (!permissionAuditor.hasMicrophonePermission()) {
            _navEffects.tryEmit(UiNavigationEffect.ToastMessage("ব্যাকগ্রাউন্ড ভয়েস অ্যাসিস্ট্যান্টের জন্য মাইক্রোফোন অনুমতি দাও"))
            return
        }
        MayaBackgroundService.start(appContext)
        refreshTelemetryAndPermissions()
    }

    fun pauseBackgroundAssistant() {
        MayaBackgroundService.pause(appContext)
    }

    fun resumeBackgroundAssistant() {
        MayaBackgroundService.resume(appContext)
    }

    fun stopBackgroundAssistant() {
        MayaBackgroundService.stop(appContext)
        refreshTelemetryAndPermissions()
    }

    fun requestScreenShare() {
        _navEffects.tryEmit(UiNavigationEffect.RequestMediaProjection)
    }

    fun stopScreenShare() {
        MayaScreenShareService.stop(appContext)
        refreshTelemetryAndPermissions()
    }

    fun askMayaAboutScreen(question: String = "আমার ফোনের স্ক্রিনে এখন কী দেখা যাচ্ছে সংক্ষেপে বাংলায় বলো।") {
        if (!ScreenShareStateHolder.isSharing.value || ScreenShareStateHolder.latestBitmap == null) {
            _navEffects.tryEmit(UiNavigationEffect.ToastMessage("আগে স্ক্রিন শেয়ার চালু করো"))
            return
        }
        submitCommand(question, speakReply = true, includeScreenFrame = true)
    }

    fun startPcServer() = pcServer.startServer()
    fun stopPcServer() = pcServer.stopServer()
    fun disconnectPcClient() = pcServer.disconnectClient()
    fun addSharedFileToPc(uri: Uri) = pcServer.addApprovedSharedFile(uri)
    fun removeSharedFile(id: String) = pcServer.removeSharedFile(id)

    fun saveCustomGeminiKey(key: String, enable: Boolean) {
        preferences.saveCustomGeminiApiKey(key)
        viewModelScope.launch {
            preferences.updateSettings { it.copy(useCustomGeminiKey = enable && key.isNotBlank()) }
        }
    }

    fun disconnectCustomGeminiKey() {
        preferences.saveCustomGeminiApiKey("")
        viewModelScope.launch {
            preferences.updateSettings { it.copy(useCustomGeminiKey = false) }
        }
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            val s = settings.value
            val key = if (s.useCustomGeminiKey) preferences.getCustomGeminiApiKey() else null
            val res = geminiBrain.testConnection(s.geminiModel, key)
            res.fold(
                onSuccess = { reply ->
                    _navEffects.tryEmit(UiNavigationEffect.ToastMessage("Gemini সংযুক্ত: $reply"))
                },
                onFailure = { err ->
                    _navEffects.tryEmit(UiNavigationEffect.ToastMessage(err.localizedMessage ?: "সংযোগ ব্যর্থ"))
                }
            )
        }
    }

    fun connectElevenLabs(apiKey: String) {
        preferences.saveElevenLabsApiKey(apiKey)
        viewModelScope.launch {
            val res = voiceManager.connectAndLoadVoices(apiKey)
            res.fold(
                onSuccess = { voices ->
                    preferences.updateSettings { s ->
                        val firstVoice = voices.firstOrNull()
                        s.copy(
                            useElevenLabs = true,
                            elevenLabsVoiceId = firstVoice?.voiceId ?: s.elevenLabsVoiceId,
                            elevenLabsVoiceName = firstVoice?.name ?: s.elevenLabsVoiceName
                        )
                    }
                    _navEffects.tryEmit(UiNavigationEffect.ToastMessage("ElevenLabs নারী কণ্ঠ সংযুক্ত হয়েছে"))
                },
                onFailure = { err ->
                    _navEffects.tryEmit(UiNavigationEffect.ToastMessage(err.localizedMessage ?: "ElevenLabs সংযোগ ব্যর্থ"))
                }
            )
        }
    }

    fun disconnectElevenLabs() {
        preferences.saveElevenLabsApiKey("")
        voiceManager.disconnectElevenLabs()
        viewModelScope.launch {
            preferences.updateSettings { it.copy(useElevenLabs = false) }
        }
    }

    fun updateSettings(transform: (MayaSettings) -> MayaSettings) {
        viewModelScope.launch {
            preferences.updateSettings(transform)
        }
    }

    fun addOrUpdateAlias(id: Long = 0, trigger: String, app: InstalledApp) {
        if (trigger.isBlank()) return
        viewModelScope.launch {
            repository.saveAlias(
                CommandAliasEntity(
                    id = id,
                    triggerPhrase = trigger.trim(),
                    targetPackageName = app.packageName,
                    targetAppLabel = app.label,
                    enabled = true
                )
            )
        }
    }

    fun toggleAliasEnabled(alias: CommandAliasEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAlias(alias.copy(enabled = enabled))
        }
    }

    fun deleteAlias(id: Long) {
        viewModelScope.launch {
            repository.deleteAlias(id)
        }
    }

    override fun onCleared() {
        pcServer.stopServer()
        voiceManager.shutdown()
        speechRecognizer.stopListening()
        super.onCleared()
    }
}
