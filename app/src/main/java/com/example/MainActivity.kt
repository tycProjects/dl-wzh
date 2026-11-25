package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.device.PermissionActionType
import com.example.service.BackgroundModeState
import com.example.service.MayaBackgroundService
import com.example.service.MayaScreenShareService
import com.example.ui.components.AmbiguousAppDialog
import com.example.ui.components.AppLauncherSheet
import com.example.ui.components.MayaBottomBar
import com.example.ui.components.SensitiveConfirmationDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DeviceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VoiceConversationOverlay
import com.example.ui.theme.CosmicVoid
import com.example.ui.theme.MayaTheme
import com.example.viewmodel.AssistantMode
import com.example.viewmodel.MayaTab
import com.example.viewmodel.MayaViewModel
import com.example.viewmodel.UiNavigationEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // REQUIRED STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val startVoiceOnLaunch = intent?.getBooleanExtra(MayaBackgroundService.EXTRA_START_VOICE, false) == true

        setContent {
            MayaTheme {
                Surface(color = CosmicVoid, modifier = Modifier.fillMaxSize()) {
                    MayaRootApp(
                        hostActivity = this,
                        startVoiceOnLaunch = startVoiceOnLaunch
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MayaRootApp(
    hostActivity: Activity,
    startVoiceOnLaunch: Boolean = false,
    viewModel: MayaViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isVoiceHideMode by viewModel.isVoiceHideMode.collectAsStateWithLifecycle()
    val assistantMode by viewModel.assistantMode.collectAsStateWithLifecycle()
    val statusHeadline by viewModel.statusHeadline.collectAsStateWithLifecycle()
    val lastMayaReply by viewModel.lastMayaReply.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val speechAmplitude by viewModel.speechAmplitude.collectAsStateWithLifecycle()
    val speechRmsLevel by viewModel.speechRmsLevel.collectAsStateWithLifecycle()
    val geminiStatus by viewModel.geminiStatus.collectAsStateWithLifecycle()
    val hasCustomGeminiKey by viewModel.hasCustomGeminiKey.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val voiceStatusDetail by viewModel.voiceStatusDetail.collectAsStateWithLifecycle()
    val hasElevenLabsKey by viewModel.hasElevenLabsKey.collectAsStateWithLifecycle()
    val elevenLabsVoices by viewModel.availableElevenLabsVoices.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val showAppLauncherSheet by viewModel.showAppLauncherSheet.collectAsStateWithLifecycle()
    val auditedPermissions by viewModel.auditedPermissions.collectAsStateWithLifecycle()
    val backgroundState by viewModel.backgroundState.collectAsStateWithLifecycle()
    val batterySaverActive by viewModel.batterySaverActive.collectAsStateWithLifecycle()
    val isScreenSharing by viewModel.isScreenSharing.collectAsStateWithLifecycle()
    val screenFrameBytes by viewModel.screenShareFrameBytes.collectAsStateWithLifecycle()
    val pcState by viewModel.pcConnectionState.collectAsStateWithLifecycle()
    val aliases by viewModel.aliases.collectAsStateWithLifecycle()
    val automationState by viewModel.automationState.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val ambiguousAppRequest by viewModel.ambiguousAppRequest.collectAsStateWithLifecycle()

    val deviceListState = rememberLazyListState()
    val chatListState = rememberLazyListState()
    val toolsListState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshTelemetryAndPermissions()
        if (granted) {
            viewModel.openVoiceHideMode(hostActivity)
        } else {
            Toast.makeText(context, "ভয়েস ব্যবহারের জন্য মাইক্রোফোন অনুমতি লাগবে।", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshTelemetryAndPermissions()
        if (granted) {
            viewModel.startBackgroundAssistant()
        }
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            MayaScreenShareService.start(context, result.resultCode, result.data!!)
            viewModel.refreshTelemetryAndPermissions()
        } else {
            Toast.makeText(context, "স্ক্রিন শেয়ার অনুমতি বাতিল হয়েছে।", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchMediaProjectionConsent() {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    LaunchedEffect(startVoiceOnLaunch) {
        if (startVoiceOnLaunch) {
            viewModel.openVoiceHideMode(hostActivity)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navEffects.collect { effect ->
            when (effect) {
                is UiNavigationEffect.RequestMediaProjection -> launchMediaProjectionConsent()
                is UiNavigationEffect.ToastMessage ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is UiNavigationEffect.ScrollScreen -> {
                    val targetState = when (activeTab) {
                        MayaTab.DEVICE -> deviceListState
                        MayaTab.CHAT -> chatListState
                        MayaTab.TOOLS -> toolsListState
                        MayaTab.HOME -> null
                    }
                    targetState?.let { st ->
                        val nextIdx = if (effect.direction == "up") {
                            (st.firstVisibleItemIndex - 2).coerceAtLeast(0)
                        } else {
                            st.firstVisibleItemIndex + 2
                        }
                        st.animateScrollToItem(nextIdx)
                    }
                }
            }
        }
    }

    // Back button behavior for Voice Hide Mode & secondary tabs
    BackHandler(enabled = isVoiceHideMode || activeTab != MayaTab.HOME) {
        when {
            isVoiceHideMode -> viewModel.exitVoiceHideMode(goToChat = false)
            activeTab != MayaTab.HOME -> viewModel.selectTab(MayaTab.HOME)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (activeTab) {
            MayaTab.HOME -> HomeScreen(
                assistantMode = assistantMode,
                statusHeadline = statusHeadline,
                lastMayaReply = lastMayaReply,
                speechAmplitude = speechAmplitude,
                speechRmsLevel = speechRmsLevel,
                geminiStatus = geminiStatus,
                backgroundState = backgroundState,
                isScreenSharing = isScreenSharing,
                automationState = automationState,
                onVoiceClick = {
                    if (viewModel.permissionAuditor.hasMicrophonePermission()) {
                        viewModel.openVoiceHideMode(hostActivity)
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onSendCommand = { cmd -> viewModel.submitCommand(cmd, speakReply = true, hostActivity = hostActivity) },
                onInterrupt = { viewModel.interruptMaya() },
                onSpeakLastReply = { viewModel.speakMessage(lastMayaReply) },
                onToggleScreenShare = {
                    if (isScreenSharing) viewModel.stopScreenShare()
                    else launchMediaProjectionConsent()
                },
                onAskAboutScreen = { viewModel.askMayaAboutScreen() },
                onOpenAppLauncher = { viewModel.toggleAppLauncherSheet(true) },
                onOpenTools = { viewModel.selectTab(MayaTab.TOOLS) },
                onOpenDevice = { viewModel.selectTab(MayaTab.DEVICE) },
                onToggleBackgroundAssistant = {
                    if (backgroundState == BackgroundModeState.STOPPED) {
                        if (!viewModel.permissionAuditor.hasMicrophonePermission()) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !viewModel.permissionAuditor.hasNotificationPermission()
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.startBackgroundAssistant()
                        }
                    } else {
                        viewModel.stopBackgroundAssistant()
                    }
                },
                onStopAutomation = { viewModel.stopAutomation() }
            )

            MayaTab.DEVICE -> DeviceScreen(
                telemetry = telemetry,
                installedApps = installedApps,
                pcState = pcState,
                isScreenSharing = isScreenSharing,
                screenFrameBytes = screenFrameBytes,
                listState = deviceListState,
                onRefresh = {
                    viewModel.refreshTelemetryAndPermissions()
                    viewModel.refreshInstalledApps()
                },
                onToggleFlashlight = { viewModel.toggleFlashlight() },
                onVolumeChange = { pct -> viewModel.setVolumePercent(pct) },
                onToggleMute = { mute ->
                    viewModel.deviceControl.setMediaMuted(mute)
                    viewModel.refreshTelemetryAndPermissions()
                },
                onBrightnessChange = { pct -> viewModel.setBrightnessPercent(pct, hostActivity) },
                onRequestWriteSettings = { viewModel.deviceControl.openWriteSettingsPermission() },
                onOpenSettingsShortcut = { key -> viewModel.deviceControl.openSettingsShortcut(key) },
                onOpenAppLauncher = { viewModel.toggleAppLauncherSheet(true) },
                onLaunchApp = { app -> viewModel.launchInstalledApp(app) },
                onStartPcServer = { viewModel.startPcServer() },
                onStopPcServer = { viewModel.stopPcServer() },
                onDisconnectPc = { viewModel.disconnectPcClient() },
                onShareFileUri = { uri -> viewModel.addSharedFileToPc(uri) },
                onRemoveSharedFile = { id -> viewModel.removeSharedFile(id) },
                onStartScreenShare = { launchMediaProjectionConsent() },
                onStopScreenShare = { viewModel.stopScreenShare() },
                onAskMayaAboutScreen = { viewModel.askMayaAboutScreen() }
            )

            MayaTab.CHAT -> ChatScreen(
                sessions = sessions,
                currentSessionId = currentSessionId,
                messages = messages,
                assistantMode = assistantMode,
                listState = chatListState,
                onSelectSession = { viewModel.selectSession(it) },
                onNewChat = { viewModel.createNewChat() },
                onClearChat = { viewModel.clearCurrentChat() },
                onSendMessage = { msg -> viewModel.submitCommand(msg, speakReply = true, hostActivity = hostActivity) },
                onVoiceInputClick = {
                    if (viewModel.permissionAuditor.hasMicrophonePermission()) {
                        viewModel.openVoiceHideMode(hostActivity)
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopGeneration = { viewModel.stopGeneration() },
                onSpeakMessage = { viewModel.speakMessage(it) }
            )

            MayaTab.TOOLS -> ToolsScreen(
                settings = settings,
                geminiStatus = geminiStatus,
                hasCustomGeminiKey = hasCustomGeminiKey,
                voiceStatus = voiceStatus,
                voiceStatusDetail = voiceStatusDetail,
                hasElevenLabsKey = hasElevenLabsKey,
                elevenLabsVoices = elevenLabsVoices,
                backgroundState = backgroundState,
                batterySaverActive = batterySaverActive,
                auditedPermissions = auditedPermissions,
                aliases = aliases,
                installedApps = installedApps,
                automationState = automationState,
                listState = toolsListState,
                onTestGemini = { viewModel.testGeminiConnection() },
                onSaveCustomGeminiKey = { key -> viewModel.saveCustomGeminiKey(key, enable = true) },
                onDisconnectCustomGeminiKey = { viewModel.disconnectCustomGeminiKey() },
                onConnectElevenLabs = { key -> viewModel.connectElevenLabs(key) },
                onDisconnectElevenLabs = { viewModel.disconnectElevenLabs() },
                onTestVoice = { viewModel.speakMessage("হ্যালো! আমি মায়া, তোমার বাংলা এআই অ্যাসিস্ট্যান্ট।") },
                onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                onStartBackground = {
                    if (!viewModel.permissionAuditor.hasMicrophonePermission()) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !viewModel.permissionAuditor.hasNotificationPermission()
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.startBackgroundAssistant()
                    }
                },
                onPauseBackground = { viewModel.pauseBackgroundAssistant() },
                onResumeBackground = { viewModel.resumeBackgroundAssistant() },
                onStopBackground = { viewModel.stopBackgroundAssistant() },
                onPermissionAction = { actionType ->
                    when (actionType) {
                        PermissionActionType.RUNTIME_MIC ->
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        PermissionActionType.RUNTIME_NOTIFICATIONS ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.permissionAuditor.openAppSettings()
                            }
                        PermissionActionType.WRITE_SETTINGS_PANEL ->
                            viewModel.deviceControl.openWriteSettingsPermission()
                        PermissionActionType.MEDIA_PROJECTION_INFO ->
                            if (isScreenSharing) viewModel.stopScreenShare() else launchMediaProjectionConsent()
                        PermissionActionType.BATTERY_SETTINGS ->
                            viewModel.permissionAuditor.openBatteryOptimizationSettings()
                        PermissionActionType.ACCESSIBILITY_SETTINGS_INFO ->
                            viewModel.deviceControl.openSettingsShortcut("accessibility")
                    }
                },
                onAddAlias = { phrase, app -> viewModel.addOrUpdateAlias(trigger = phrase, app = app) },
                onToggleAlias = { alias, enabled -> viewModel.toggleAliasEnabled(alias, enabled) },
                onDeleteAlias = { id -> viewModel.deleteAlias(id) },
                onRunAutomationSample = { seq -> viewModel.submitCommand(seq, speakReply = true, hostActivity = hostActivity) },
                onStopAutomation = { viewModel.stopAutomation() }
            )
        }

        // Bottom 5-item Navigation Bar (Hidden during Voice Conversation Hide Mode)
        if (!isVoiceHideMode) {
            MayaBottomBar(
                activeTab = activeTab,
                isListening = assistantMode == AssistantMode.LISTENING,
                onSelectTab = { viewModel.selectTab(it) },
                onVoiceClick = {
                    if (viewModel.permissionAuditor.hasMicrophonePermission()) {
                        viewModel.openVoiceHideMode(hostActivity)
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Voice Conversation Hide Mode Overlay
        AnimatedVisibility(
            visible = isVoiceHideMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VoiceConversationOverlay(
                assistantMode = assistantMode,
                statusHeadline = statusHeadline,
                liveTranscript = liveTranscript,
                speechAmplitude = speechAmplitude,
                speechRmsLevel = speechRmsLevel,
                hasMicPermission = viewModel.permissionAuditor.hasMicrophonePermission(),
                geminiStatus = geminiStatus,
                voiceStatus = voiceStatus,
                isScreenSharing = isScreenSharing,
                onTapListen = { viewModel.startVoiceListening(hostActivity) },
                onInterruptMaya = { viewModel.interruptMaya() },
                onAskAboutScreen = { viewModel.askMayaAboutScreen() },
                onGoToChat = { viewModel.exitVoiceHideMode(goToChat = true) },
                onExitVoiceMode = { viewModel.exitVoiceHideMode(goToChat = false) },
                onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
        }

        // Universal Installed Apps Launcher Sheet
        if (showAppLauncherSheet) {
            AppLauncherSheet(
                apps = installedApps,
                onSelectApp = { app ->
                    viewModel.toggleAppLauncherSheet(false)
                    viewModel.launchInstalledApp(app)
                },
                onDismiss = { viewModel.toggleAppLauncherSheet(false) }
            )
        }

        // Ambiguous App Picker Dialog
        ambiguousAppRequest?.let { req ->
            AmbiguousAppDialog(
                request = req,
                onSelectApp = { app -> viewModel.launchInstalledApp(app) },
                onDismiss = { viewModel.dismissAmbiguousAppDialog() }
            )
        }

        // Sensitive Action Confirmation Dialog
        pendingConfirmation?.let { conf ->
            SensitiveConfirmationDialog(
                request = conf,
                onConfirm = { viewModel.confirmPendingAction() },
                onDismiss = { viewModel.dismissConfirmation() }
            )
        }
    }
}
