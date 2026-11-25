package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MayaAiRepository
import com.example.data.local.MayaDatabase
import com.example.data.local.MayaPreferencesRepository
import com.example.data.local.mapToDomainMessages
import com.example.data.local.mapToDomainSequences
import com.example.data.local.toEntity
import com.example.domain.ActionResult
import com.example.domain.AppDestination
import com.example.domain.AudioStreamType
import com.example.domain.AutomationFeatureFlags
import com.example.domain.AutomationSequence
import com.example.domain.AutomationStep
import com.example.domain.AutomationStepType
import com.example.domain.ChatMessage
import com.example.domain.DeviceTelemetrySnapshot
import com.example.domain.InstalledAppInfo
import com.example.domain.MayaCommandAction
import com.example.domain.MayaEmotion
import com.example.domain.PermissionRequirement
import com.example.service.MayaAccessibilityService
import com.example.system.DeviceControlManager
import com.example.system.DeviceTelemetryManager
import com.example.system.MayaVoiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

sealed interface AiProcessingState {
    data object Idle : AiProcessingState
    data object Thinking : AiProcessingState
    data class Error(val message: String) : AiProcessingState
}

data class StatusBannerState(
    val message: String,
    val isError: Boolean = false,
    val permissionRequirement: PermissionRequirement? = null
)

data class AutomationExecutionState(
    val isRunning: Boolean = false,
    val sequenceName: String? = null,
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val currentStepLabel: String = ""
)

class MayaViewModel(application: Application) : AndroidViewModel(application) {

    private val controlManager = DeviceControlManager(application)
    private val telemetryManager = DeviceTelemetryManager(application, controlManager)
    val voiceManager = MayaVoiceManager(application)
    private val aiRepository = MayaAiRepository()
    private val database = MayaDatabase.getInstance(application)
    private val prefsRepository = MayaPreferencesRepository(application)

    private val _selectedDestination = MutableStateFlow(AppDestination.MAYA)
    val selectedDestination: StateFlow<AppDestination> = _selectedDestination.asStateFlow()

    private val _currentEmotion = MutableStateFlow(MayaEmotion.AFFECTIONATE)
    val currentEmotion: StateFlow<MayaEmotion> = _currentEmotion.asStateFlow()

    private val _aiState = MutableStateFlow<AiProcessingState>(AiProcessingState.Idle)
    val aiState: StateFlow<AiProcessingState> = _aiState.asStateFlow()

    private val _statusBanner = MutableStateFlow<StatusBannerState?>(null)
    val statusBanner: StateFlow<StatusBannerState?> = _statusBanner.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _appsLoading = MutableStateFlow(true)
    val appsLoading: StateFlow<Boolean> = _appsLoading.asStateFlow()

    private val _automationState = MutableStateFlow(AutomationExecutionState())
    val automationState: StateFlow<AutomationExecutionState> = _automationState.asStateFlow()

    private val _voiceLanguageTag = MutableStateFlow("bn-BD")
    val voiceLanguageTag: StateFlow<String> = _voiceLanguageTag.asStateFlow()

    val telemetry: StateFlow<DeviceTelemetrySnapshot> = combine(
        telemetryManager.snapshot,
        controlManager.isFlashlightOn,
        MayaAccessibilityService.isServiceConnected
    ) { snap, torchOn, accConnected ->
        snap.copy(
            isFlashlightOn = torchOn,
            isAccessibilityEnabled = accConnected ||
                MayaAccessibilityService.isAccessibilitySettingEnabled(getApplication())
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        telemetryManager.snapshot.value
    )

    val featureFlags: StateFlow<AutomationFeatureFlags> = prefsRepository.featureFlags
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AutomationFeatureFlags()
        )

    val chatMessages: StateFlow<List<ChatMessage>> = database.chatMessageDao()
        .observeAll()
        .mapToDomainMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val automationSequences: StateFlow<List<AutomationSequence>> = database.automationSequenceDao()
        .observeAll()
        .mapToDomainSequences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var automationJob: Job? = null

    init {
        telemetryManager.startObserving()
        refreshInstalledApps()
        seedInitialDataIfNeeded()
        startTelemetryHeartbeat()
    }

    private fun startTelemetryHeartbeat() {
        viewModelScope.launch {
            while (true) {
                delay(4_000L)
                telemetryManager.refresh()
            }
        }
    }

    private fun seedInitialDataIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val seqDao = database.automationSequenceDao()
            if (seqDao.count() == 0) {
                seqDao.insert(
                    AutomationSequence(
                        name = "Auto Scroll Feed",
                        description = "Scrolls down twice with brief reading pauses",
                        steps = listOf(
                            AutomationStep(AutomationStepType.SCROLL_DOWN),
                            AutomationStep(AutomationStepType.WAIT, "1400"),
                            AutomationStep(AutomationStepType.SCROLL_DOWN),
                            AutomationStep(AutomationStepType.WAIT, "1400")
                        )
                    ).toEntity()
                )
                seqDao.insert(
                    AutomationSequence(
                        name = "Swipe Gallery Tour",
                        description = "Swipes left twice then returns",
                        steps = listOf(
                            AutomationStep(AutomationStepType.SWIPE_LEFT),
                            AutomationStep(AutomationStepType.WAIT, "1200"),
                            AutomationStep(AutomationStepType.SWIPE_LEFT),
                            AutomationStep(AutomationStepType.WAIT, "1200"),
                            AutomationStep(AutomationStepType.SWIPE_RIGHT)
                        )
                    ).toEntity()
                )
                seqDao.insert(
                    AutomationSequence(
                        name = "Open YouTube & Scroll",
                        description = "Launches YouTube and scrolls the feed",
                        steps = listOf(
                            AutomationStep(AutomationStepType.OPEN_APP, "YouTube"),
                            AutomationStep(AutomationStepType.WAIT, "2200"),
                            AutomationStep(AutomationStepType.SCROLL_DOWN)
                        )
                    ).toEntity()
                )
            }

            val chatDao = database.chatMessageDao()
            // Add a warm initial greeting only when database has no messages yet
            val existing = chatMessages.value
            if (existing.isEmpty()) {
                delay(150)
                if (chatMessages.value.isEmpty()) {
                    chatDao.insert(
                        ChatMessage(
                            isFromMaya = true,
                            text = "হ্যালো জান! আমি মায়া, তোমার স্মার্টফোন সঙ্গী। আজ কেমন আছো তুমি? বলো, তোমার ফোনের কোন কাজটা করে দেবো, নাকি একটু গল্প করবে আমার সাথে?",
                            emotion = MayaEmotion.AFFECTIONATE
                        ).toEntity()
                    )
                }
            }
        }
    }

    fun refreshAllState() {
        telemetryManager.refresh()
        refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _appsLoading.value = true
            _installedApps.value = controlManager.loadLaunchableApps()
            _appsLoading.value = false
        }
    }

    fun selectDestination(destination: AppDestination) {
        _selectedDestination.value = destination
        telemetryManager.refresh()
    }

    fun dismissStatusBanner() {
        _statusBanner.value = null
    }

    fun clearAiError() {
        if (_aiState.value is AiProcessingState.Error) {
            _aiState.value = AiProcessingState.Idle
        }
    }

    fun showVoiceError(message: String) {
        _statusBanner.value = StatusBannerState(
            message = message,
            isError = true
        )
    }

    fun toggleVoiceLanguage() {
        _voiceLanguageTag.value = if (_voiceLanguageTag.value == "bn-BD") "en-US" else "bn-BD"
    }

    fun sendUserCommand(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val userMsg = ChatMessage(
                isFromMaya = false,
                text = trimmed
            )
            withContext(Dispatchers.IO) {
                database.chatMessageDao().insert(userMsg.toEntity())
            }

            _aiState.value = AiProcessingState.Thinking
            telemetryManager.refresh()

            try {
                val parsed = aiRepository.processUserMessage(
                    userInput = trimmed,
                    recentHistory = chatMessages.value,
                    telemetry = telemetry.value,
                    installedAppNames = _installedApps.value.map { it.label },
                    sequenceNames = automationSequences.value.map { it.name }
                )

                _currentEmotion.value = parsed.emotion
                val executionResult = executeAiAction(
                    action = parsed.action,
                    targetAppName = parsed.targetAppName,
                    brightnessPercent = parsed.brightnessPercent,
                    tapX = parsed.tapX,
                    tapY = parsed.tapY,
                    sequenceName = parsed.sequenceName
                )

                val actionNote = if (parsed.action != MayaCommandAction.NONE) {
                    executionResult?.message
                } else null

                val mayaReply = ChatMessage(
                    isFromMaya = true,
                    text = parsed.replyBn,
                    emotion = parsed.emotion,
                    actionSummary = actionNote,
                    actionSuccess = executionResult?.success
                )

                withContext(Dispatchers.IO) {
                    database.chatMessageDao().insert(mayaReply.toEntity())
                }

                _aiState.value = AiProcessingState.Idle

                if (featureFlags.value.mayaAutoSpeakEnabled) {
                    voiceManager.speak(parsed.replyBn)
                }

                executionResult?.let { res ->
                    _statusBanner.value = StatusBannerState(
                        message = res.message,
                        isError = !res.success,
                        permissionRequirement = res.permissionType
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Maya AI timed out", e)
                _aiState.value = AiProcessingState.Error(
                    "Request timed out after 60 seconds. Please check your connection and try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Maya AI failed", e)
                _aiState.value = AiProcessingState.Error(
                    e.localizedMessage ?: "Failed to connect to Gemini AI Logic."
                )
            }
        }
    }

    private suspend fun executeAiAction(
        action: MayaCommandAction,
        targetAppName: String?,
        brightnessPercent: Int?,
        tapX: Int?,
        tapY: Int?,
        sequenceName: String?
    ): ActionResult? {
        return when (action) {
            MayaCommandAction.NONE -> null
            MayaCommandAction.OPEN_APP -> launchApp(targetAppName.orEmpty())
            MayaCommandAction.GO_HOME -> performGlobalHome()
            MayaCommandAction.OPEN_RECENTS -> performGlobalRecents()
            MayaCommandAction.OPEN_BACK -> performGlobalBack()
            MayaCommandAction.OPEN_QUICK_SETTINGS -> controlManager.openQuickSettingsPanel()
            MayaCommandAction.OPEN_NOTIFICATIONS -> controlManager.openNotificationsPanel()
            MayaCommandAction.OPEN_WIFI_SETTINGS -> controlManager.openWifiSettings()
            MayaCommandAction.OPEN_BLUETOOTH_SETTINGS -> controlManager.openBluetoothSettings()
            MayaCommandAction.OPEN_SYSTEM_SETTINGS -> controlManager.openSystemSettings()
            MayaCommandAction.FLASHLIGHT_ON -> controlManager.setFlashlight(true).also { telemetryManager.refresh() }
            MayaCommandAction.FLASHLIGHT_OFF -> controlManager.setFlashlight(false).also { telemetryManager.refresh() }
            MayaCommandAction.FLASHLIGHT_TOGGLE -> controlManager.toggleFlashlight().also { telemetryManager.refresh() }
            MayaCommandAction.VOLUME_UP -> adjustVolume(AudioStreamType.MEDIA, true)
            MayaCommandAction.VOLUME_DOWN -> adjustVolume(AudioStreamType.MEDIA, false)
            MayaCommandAction.VOLUME_MUTE -> setStreamMuted(AudioStreamType.MEDIA, true)
            MayaCommandAction.VOLUME_UNMUTE -> setStreamMuted(AudioStreamType.MEDIA, false)
            MayaCommandAction.BRIGHTNESS_UP -> adjustBrightness(true)
            MayaCommandAction.BRIGHTNESS_DOWN -> adjustBrightness(false)
            MayaCommandAction.BRIGHTNESS_SET -> setBrightness(brightnessPercent ?: 50)
            MayaCommandAction.SCROLL_UP -> performScroll(down = false)
            MayaCommandAction.SCROLL_DOWN -> performScroll(down = true)
            MayaCommandAction.SWIPE_LEFT -> performSwipe(left = true)
            MayaCommandAction.SWIPE_RIGHT -> performSwipe(left = false)
            MayaCommandAction.TAP_COORDINATE -> performTap(
                (tapX ?: 540).toFloat(),
                (tapY ?: 960).toFloat()
            )
            MayaCommandAction.LONG_PRESS_COORDINATE -> performLongPress(
                (tapX ?: 540).toFloat(),
                (tapY ?: 960).toFloat()
            )
            MayaCommandAction.SHOW_BATTERY_INFO -> {
                _selectedDestination.value = AppDestination.DEVICE
                telemetryManager.refresh()
                val snap = telemetry.value
                ActionResult(true, "Battery ${snap.batteryPercent}% (${snap.batteryStatusText})")
            }
            MayaCommandAction.SHOW_STORAGE_INFO -> {
                _selectedDestination.value = AppDestination.DEVICE
                telemetryManager.refresh()
                ActionResult(true, "Showing live Storage diagnostics")
            }
            MayaCommandAction.SHOW_NETWORK_INFO -> {
                _selectedDestination.value = AppDestination.DEVICE
                telemetryManager.refresh()
                val snap = telemetry.value
                ActionResult(true, "Network: ${snap.networkTypeText}")
            }
            MayaCommandAction.SHOW_DEVICE_INFO -> {
                _selectedDestination.value = AppDestination.DEVICE
                telemetryManager.refresh()
                ActionResult(true, "Showing live Device Dashboard")
            }
            MayaCommandAction.RUN_AUTOMATION_SEQUENCE -> {
                val match = automationSequences.value.firstOrNull {
                    sequenceName != null && it.name.contains(sequenceName, ignoreCase = true)
                } ?: automationSequences.value.firstOrNull()
                if (match != null) {
                    runAutomationSequence(match)
                    ActionResult(true, "Started sequence: ${match.name}")
                } else {
                    ActionResult(false, "No automation sequence found.")
                }
            }
            MayaCommandAction.STOP_AUTOMATION -> {
                emergencyStopAutomation()
                ActionResult(true, "Automation stopped")
            }
        }
    }

    // --- Direct UI Action Methods ---

    fun toggleFlashlight() {
        val res = controlManager.toggleFlashlight()
        telemetryManager.refresh()
        reportAction(res)
    }

    fun setFlashlight(enabled: Boolean) {
        val res = controlManager.setFlashlight(enabled)
        telemetryManager.refresh()
        reportAction(res)
    }

    fun adjustVolume(stream: AudioStreamType, raise: Boolean): ActionResult {
        val res = controlManager.adjustVolume(stream, raise)
        telemetryManager.refresh()
        reportAction(res)
        return res
    }

    fun setStreamVolume(stream: AudioStreamType, level: Int) {
        val res = controlManager.setStreamVolume(stream, level)
        telemetryManager.refresh()
        reportAction(res)
    }

    fun setStreamMuted(stream: AudioStreamType, mute: Boolean): ActionResult {
        val res = controlManager.setStreamMuted(stream, mute)
        telemetryManager.refresh()
        reportAction(res)
        return res
    }

    fun setBrightness(percent: Int): ActionResult {
        val res = controlManager.setBrightnessPercent(percent)
        telemetryManager.refresh()
        reportAction(res)
        return res
    }

    fun adjustBrightness(increase: Boolean): ActionResult {
        val res = controlManager.adjustBrightness(increase)
        telemetryManager.refresh()
        reportAction(res)
        return res
    }

    fun launchApp(query: String): ActionResult {
        if (!featureFlags.value.appLaunchEnabled) {
            return ActionResult(
                false,
                "App Launch automation is disabled in Automate settings."
            ).also { reportAction(it) }
        }
        val res = controlManager.launchAppByNameOrPackage(query, _installedApps.value)
        reportAction(res)
        return res
    }

    fun launchAppDirect(app: InstalledAppInfo) {
        val res = controlManager.launchAppByNameOrPackage(app.packageName, _installedApps.value)
        reportAction(res)
    }

    fun openQuickSettings() = reportAction(controlManager.openQuickSettingsPanel())
    fun openNotifications() = reportAction(controlManager.openNotificationsPanel())
    fun openWifiSettings() = reportAction(controlManager.openWifiSettings())
    fun openInternetPanel() = reportAction(controlManager.openInternetPanel())
    fun openBluetoothSettings() = reportAction(controlManager.openBluetoothSettings())
    fun openVolumePanel() = reportAction(controlManager.openVolumePanel())
    fun openDisplaySettings() = reportAction(controlManager.openDisplaySettings())
    fun openBatterySettings() = reportAction(controlManager.openBatterySettings())
    fun openSystemSettings() = reportAction(controlManager.openSystemSettings())
    fun openAccessibilitySettings() = reportAction(controlManager.openAccessibilitySettings())
    fun openWriteSettingsPermission() = reportAction(controlManager.openWriteSettingsPermission())

    fun performGlobalHome(): ActionResult {
        val res = if (MayaAccessibilityService.instance == null) {
            ActionResult(
                false,
                "Enable Maya Accessibility Service to navigate Home.",
                requiresPermission = true,
                permissionType = PermissionRequirement.ACCESSIBILITY
            )
        } else {
            val ok = MayaAccessibilityService.performHome()
            ActionResult(ok, if (ok) "Returned to Home screen" else "Home navigation failed")
        }
        reportAction(res)
        return res
    }

    fun performGlobalRecents(): ActionResult {
        val res = if (MayaAccessibilityService.instance == null) {
            ActionResult(
                false,
                "Enable Maya Accessibility Service to open Recent Apps.",
                requiresPermission = true,
                permissionType = PermissionRequirement.ACCESSIBILITY
            )
        } else {
            val ok = MayaAccessibilityService.performRecents()
            ActionResult(ok, if (ok) "Opened Recent Apps switcher" else "Recents failed")
        }
        reportAction(res)
        return res
    }

    fun performGlobalBack(): ActionResult {
        val res = if (MayaAccessibilityService.instance == null) {
            ActionResult(
                false,
                "Enable Maya Accessibility Service to perform Back navigation.",
                requiresPermission = true,
                permissionType = PermissionRequirement.ACCESSIBILITY
            )
        } else {
            val ok = MayaAccessibilityService.performBack()
            ActionResult(ok, if (ok) "Performed Back navigation" else "Back action failed")
        }
        reportAction(res)
        return res
    }

    suspend fun performTap(x: Float, y: Float): ActionResult {
        if (!featureFlags.value.tapAndLongPressEnabled) {
            return ActionResult(false, "Tap & Long Press automation is currently disabled.")
                .also { reportAction(it) }
        }
        val service = MayaAccessibilityService.instance ?: return ActionResult(
            false,
            "Enable Maya Accessibility Service to perform screen taps.",
            requiresPermission = true,
            permissionType = PermissionRequirement.ACCESSIBILITY
        ).also { reportAction(it) }

        MayaAccessibilityService.resetEmergencyStop()
        val ok = suspendCancellableCoroutine { cont ->
            service.tapAt(x, y) { success ->
                if (cont.isActive) cont.resume(success)
            }
        }
        val res = ActionResult(
            ok,
            if (ok) "Tapped screen at (${x.toInt()}, ${y.toInt()})" else "Tap gesture cancelled"
        )
        reportAction(res)
        return res
    }

    suspend fun performLongPress(x: Float, y: Float): ActionResult {
        if (!featureFlags.value.tapAndLongPressEnabled) {
            return ActionResult(false, "Tap & Long Press automation is currently disabled.")
                .also { reportAction(it) }
        }
        val service = MayaAccessibilityService.instance ?: return ActionResult(
            false,
            "Enable Maya Accessibility Service to perform long press.",
            requiresPermission = true,
            permissionType = PermissionRequirement.ACCESSIBILITY
        ).also { reportAction(it) }

        MayaAccessibilityService.resetEmergencyStop()
        val ok = suspendCancellableCoroutine { cont ->
            service.longPressAt(x, y) { success ->
                if (cont.isActive) cont.resume(success)
            }
        }
        val res = ActionResult(
            ok,
            if (ok) "Long pressed at (${x.toInt()}, ${y.toInt()})" else "Long press cancelled"
        )
        reportAction(res)
        return res
    }

    suspend fun performScroll(down: Boolean): ActionResult {
        if (!featureFlags.value.autoScrollEnabled) {
            return ActionResult(false, "Auto Scroll feature is disabled in Automate settings.")
                .also { reportAction(it) }
        }
        val service = MayaAccessibilityService.instance ?: return ActionResult(
            false,
            "Enable Maya Accessibility Service to auto-scroll.",
            requiresPermission = true,
            permissionType = PermissionRequirement.ACCESSIBILITY
        ).also { reportAction(it) }

        MayaAccessibilityService.resetEmergencyStop()
        val ok = suspendCancellableCoroutine { cont ->
            service.scroll(down) { success ->
                if (cont.isActive) cont.resume(success)
            }
        }
        val dir = if (down) "down" else "up"
        val res = ActionResult(ok, if (ok) "Scrolled $dir" else "Scroll gesture cancelled")
        reportAction(res)
        return res
    }

    suspend fun performSwipe(left: Boolean): ActionResult {
        if (!featureFlags.value.autoSwipeEnabled) {
            return ActionResult(false, "Auto Swipe feature is disabled in Automate settings.")
                .also { reportAction(it) }
        }
        val service = MayaAccessibilityService.instance ?: return ActionResult(
            false,
            "Enable Maya Accessibility Service to auto-swipe.",
            requiresPermission = true,
            permissionType = PermissionRequirement.ACCESSIBILITY
        ).also { reportAction(it) }

        MayaAccessibilityService.resetEmergencyStop()
        val ok = suspendCancellableCoroutine { cont ->
            service.swipe(left) { success ->
                if (cont.isActive) cont.resume(success)
            }
        }
        val dir = if (left) "left" else "right"
        val res = ActionResult(ok, if (ok) "Swiped $dir" else "Swipe gesture cancelled")
        reportAction(res)
        return res
    }

    fun triggerManualTap(x: Float, y: Float) {
        viewModelScope.launch { performTap(x, y) }
    }

    fun triggerManualLongPress(x: Float, y: Float) {
        viewModelScope.launch { performLongPress(x, y) }
    }

    fun triggerManualScroll(down: Boolean) {
        viewModelScope.launch { performScroll(down) }
    }

    fun triggerManualSwipe(left: Boolean) {
        viewModelScope.launch { performSwipe(left) }
    }

    fun updateFeatureFlag(transform: (AutomationFeatureFlags) -> AutomationFeatureFlags) {
        viewModelScope.launch {
            prefsRepository.updateFeatureFlags(transform)
        }
    }

    fun saveAutomationSequence(sequence: AutomationSequence) {
        viewModelScope.launch(Dispatchers.IO) {
            database.automationSequenceDao().insert(sequence.toEntity())
            _statusBanner.value = StatusBannerState("Saved sequence \"${sequence.name}\"")
        }
    }

    fun deleteAutomationSequence(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.automationSequenceDao().deleteById(id)
            _statusBanner.value = StatusBannerState("Sequence deleted")
        }
    }

    fun runAutomationSequence(sequence: AutomationSequence) {
        if (!featureFlags.value.sequenceExecutionEnabled) {
            reportAction(
                ActionResult(false, "Sequence Execution is disabled in Automate toggles.")
            )
            return
        }
        if (sequence.steps.isEmpty()) {
            reportAction(ActionResult(false, "This sequence has no steps."))
            return
        }

        val requiresAcc = sequence.steps.any {
            it.type in listOf(
                AutomationStepType.TAP,
                AutomationStepType.LONG_PRESS,
                AutomationStepType.SCROLL_DOWN,
                AutomationStepType.SCROLL_UP,
                AutomationStepType.SWIPE_LEFT,
                AutomationStepType.SWIPE_RIGHT,
                AutomationStepType.GO_HOME,
                AutomationStepType.RECENTS
            )
        }
        if (requiresAcc && MayaAccessibilityService.instance == null) {
            reportAction(
                ActionResult(
                    false,
                    "Enable Maya Accessibility Service to run screen automation sequences.",
                    requiresPermission = true,
                    permissionType = PermissionRequirement.ACCESSIBILITY
                )
            )
            return
        }

        automationJob?.cancel()
        MayaAccessibilityService.resetEmergencyStop()

        automationJob = viewModelScope.launch {
            _automationState.value = AutomationExecutionState(
                isRunning = true,
                sequenceName = sequence.name,
                currentStepIndex = 0,
                totalSteps = sequence.steps.size,
                currentStepLabel = "Starting..."
            )
            try {
                for ((index, step) in sequence.steps.withIndex()) {
                    if (MayaAccessibilityService.emergencyStopped.get()) break
                    _automationState.value = _automationState.value.copy(
                        currentStepIndex = index + 1,
                        currentStepLabel = step.type.label
                    )
                    val res = when (step.type) {
                        AutomationStepType.TAP -> performTap(
                            step.param1.toFloatOrNull() ?: 540f,
                            step.param2.toFloatOrNull() ?: 960f
                        )
                        AutomationStepType.LONG_PRESS -> performLongPress(
                            step.param1.toFloatOrNull() ?: 540f,
                            step.param2.toFloatOrNull() ?: 960f
                        )
                        AutomationStepType.SCROLL_DOWN -> performScroll(down = true)
                        AutomationStepType.SCROLL_UP -> performScroll(down = false)
                        AutomationStepType.SWIPE_LEFT -> performSwipe(left = true)
                        AutomationStepType.SWIPE_RIGHT -> performSwipe(left = false)
                        AutomationStepType.OPEN_APP -> launchApp(step.param1)
                        AutomationStepType.GO_HOME -> performGlobalHome()
                        AutomationStepType.RECENTS -> performGlobalRecents()
                        AutomationStepType.WAIT -> {
                            val ms = (step.param1.toLongOrNull() ?: 1000L).coerceIn(100L, 15_000L)
                            delay(ms)
                            ActionResult(true, "Waited ${ms}ms")
                        }
                    }
                    if (!res.success) {
                        reportAction(res)
                        break
                    }
                    delay(450L)
                }
                if (!MayaAccessibilityService.emergencyStopped.get()) {
                    _statusBanner.value = StatusBannerState("Completed sequence \"${sequence.name}\"")
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _automationState.value = AutomationExecutionState()
            }
        }
    }

    fun emergencyStopAutomation() {
        MayaAccessibilityService.triggerEmergencyStop()
        automationJob?.cancel()
        automationJob = null
        _automationState.value = AutomationExecutionState()
        voiceManager.stopListening()
        voiceManager.stopSpeaking()
        _statusBanner.value = StatusBannerState(
            message = "Emergency Stop activated — all automation halted.",
            isError = true
        )
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.chatMessageDao().clearAll()
            database.chatMessageDao().insert(
                ChatMessage(
                    isFromMaya = true,
                    text = "নতুন করে শুরু করলাম জান! বলো, এখন তোমার জন্য কী করতে পারি?",
                    emotion = MayaEmotion.HAPPY
                ).toEntity()
            )
        }
    }

    private fun reportAction(result: ActionResult) {
        _statusBanner.value = StatusBannerState(
            message = result.message,
            isError = !result.success,
            permissionRequirement = result.permissionType
        )
    }

    override fun onCleared() {
        super.onCleared()
        telemetryManager.stopObserving()
        controlManager.release()
        voiceManager.release()
    }

    companion object {
        private const val TAG = "MayaViewModel"
    }
}
