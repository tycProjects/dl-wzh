package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.AppDestination
import com.example.domain.PermissionRequirement
import com.example.security.configureAppCheck
import com.example.ui.MayaViewModel
import com.example.ui.components.StatusFeedbackBar
import com.example.ui.components.StatusPill
import com.example.ui.screens.AutomateScreen
import com.example.ui.screens.ControlsScreen
import com.example.ui.screens.DeviceDashboardScreen
import com.example.ui.screens.MayaCompanionScreen
import com.example.ui.theme.GlassBase
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MayaRose
import com.example.ui.theme.MayaTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MayaTheme {
                MayaSmartControlApp()
            }
        }
    }
}

@Composable
fun MayaSmartControlApp(
    viewModel: MayaViewModel = viewModel()
) {
    val selectedDestination by viewModel.selectedDestination.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val featureFlags by viewModel.featureFlags.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val currentEmotion by viewModel.currentEmotion.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val appsLoading by viewModel.appsLoading.collectAsStateWithLifecycle()
    val automationState by viewModel.automationState.collectAsStateWithLifecycle()
    val sequences by viewModel.automationSequences.collectAsStateWithLifecycle()
    val voiceLanguageTag by viewModel.voiceLanguageTag.collectAsStateWithLifecycle()

    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val isListening by viewModel.voiceManager.isListening.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.voiceManager.partialTranscript.collectAsStateWithLifecycle()
    val rmsDb by viewModel.voiceManager.rmsDb.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAllState()
    }

    // BackHandler on secondary tabs returns smoothly to Maya Home
    BackHandler(enabled = selectedDestination != AppDestination.MAYA) {
        viewModel.selectDestination(AppDestination.MAYA)
    }

    val atmosphericBackground = Brush.radialGradient(
        colors = listOf(
            Color(0xFF102242),
            ObsidianDeep,
            Color(0xFF04060D)
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(atmosphericBackground)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                MayaTopTelemetryBar(
                    batteryPercent = telemetry.batteryPercent,
                    isOnline = telemetry.isInternetValidated,
                    isAutomationRunning = automationState.isRunning,
                    onEmergencyStop = { viewModel.emergencyStopAutomation() }
                )
            },
            bottomBar = {
                if (!useRail) {
                    MayaBottomNavBar(
                        selected = selectedDestination,
                        onSelect = viewModel::selectDestination
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    MayaNavRail(
                        selected = selectedDestination,
                        onSelect = viewModel::selectDestination
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusFeedbackBar(
                        banner = statusBanner,
                        onDismiss = viewModel::dismissStatusBanner,
                        onGrantPermission = { req ->
                            when (req) {
                                PermissionRequirement.ACCESSIBILITY -> viewModel.openAccessibilitySettings()
                                PermissionRequirement.WRITE_SETTINGS -> viewModel.openWriteSettingsPermission()
                                PermissionRequirement.RECORD_AUDIO -> viewModel.selectDestination(AppDestination.DEVICE)
                            }
                        },
                        modifier = Modifier.widthIn(max = 1040.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 1040.dp)
                            .fillMaxWidth()
                    ) {
                        when (selectedDestination) {
                            AppDestination.MAYA -> MayaCompanionScreen(
                                messages = chatMessages,
                                aiState = aiState,
                                currentEmotion = currentEmotion,
                                featureFlags = featureFlags,
                                isSpeaking = isSpeaking,
                                isListening = isListening,
                                partialTranscript = partialTranscript,
                                rmsDb = rmsDb,
                                voiceLanguageTag = voiceLanguageTag,
                                onSendMessage = viewModel::sendUserCommand,
                                onStartVoiceListening = {
                                    viewModel.voiceManager.startListening(
                                        languageTag = voiceLanguageTag,
                                        onResult = { spoken -> viewModel.sendUserCommand(spoken) },
                                        onError = { err -> viewModel.showVoiceError(err) }
                                    )
                                },
                                onStopVoiceListening = { viewModel.voiceManager.stopListening() },
                                onToggleAutoSpeak = {
                                    viewModel.updateFeatureFlag {
                                        it.copy(mayaAutoSpeakEnabled = !it.mayaAutoSpeakEnabled)
                                    }
                                },
                                onToggleVoiceLanguage = viewModel::toggleVoiceLanguage,
                                onSpeakMessage = { viewModel.voiceManager.speak(it) },
                                onStopSpeaking = { viewModel.voiceManager.stopSpeaking() },
                                onClearChat = viewModel::clearChatHistory,
                                onDismissAiError = viewModel::clearAiError,
                                onReportVoiceError = { viewModel.showVoiceError(it) }
                            )

                            AppDestination.CONTROLS -> ControlsScreen(
                                telemetry = telemetry,
                                installedApps = installedApps,
                                appsLoading = appsLoading,
                                onToggleFlashlight = viewModel::toggleFlashlight,
                                onSetBrightness = { viewModel.setBrightness(it) },
                                onAdjustBrightness = { viewModel.adjustBrightness(it) },
                                onRequestWriteSettings = viewModel::openWriteSettingsPermission,
                                onAdjustVolume = { s, r -> viewModel.adjustVolume(s, r) },
                                onSetStreamVolume = viewModel::setStreamVolume,
                                onToggleMuteStream = { s, m -> viewModel.setStreamMuted(s, m) },
                                onOpenQuickSettings = viewModel::openQuickSettings,
                                onOpenNotifications = viewModel::openNotifications,
                                onOpenWifi = viewModel::openWifiSettings,
                                onOpenInternet = viewModel::openInternetPanel,
                                onOpenBluetooth = viewModel::openBluetoothSettings,
                                onOpenVolumePanel = viewModel::openVolumePanel,
                                onOpenDisplay = viewModel::openDisplaySettings,
                                onOpenBattery = viewModel::openBatterySettings,
                                onOpenSystemSettings = viewModel::openSystemSettings,
                                onLaunchApp = viewModel::launchAppDirect,
                                onRefreshApps = viewModel::refreshInstalledApps
                            )

                            AppDestination.AUTOMATE -> AutomateScreen(
                                telemetry = telemetry,
                                featureFlags = featureFlags,
                                automationState = automationState,
                                sequences = sequences,
                                onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                                onEmergencyStop = viewModel::emergencyStopAutomation,
                                onUpdateFlag = viewModel::updateFeatureFlag,
                                onPerformHome = { viewModel.performGlobalHome() },
                                onPerformRecents = { viewModel.performGlobalRecents() },
                                onPerformBack = { viewModel.performGlobalBack() },
                                onOpenQuickSettings = viewModel::openQuickSettings,
                                onScroll = viewModel::triggerManualScroll,
                                onSwipe = viewModel::triggerManualSwipe,
                                onTap = viewModel::triggerManualTap,
                                onLongPress = viewModel::triggerManualLongPress,
                                onRunSequence = viewModel::runAutomationSequence,
                                onSaveSequence = viewModel::saveAutomationSequence,
                                onDeleteSequence = viewModel::deleteAutomationSequence
                            )

                            AppDestination.DEVICE -> DeviceDashboardScreen(
                                telemetry = telemetry,
                                onRefresh = viewModel::refreshAllState,
                                onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                                onOpenWriteSettings = viewModel::openWriteSettingsPermission,
                                onOpenWifiSettings = viewModel::openWifiSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MayaTopTelemetryBar(
    batteryPercent: Int,
    isOnline: Boolean,
    isAutomationRunning: Boolean,
    onEmergencyStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "MAYA CORE",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Smart Smartphone Control",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(
                text = "$batteryPercent%",
                color = if (batteryPercent > 20) StatusEmerald else StatusCrimson
            )
            StatusPill(
                text = if (isOnline) "NET" else "OFF",
                color = if (isOnline) NeonCyan else MayaRose
            )
            IconButton(
                onClick = onEmergencyStop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isAutomationRunning) StatusCrimson else StatusCrimson.copy(alpha = 0.16f)
                    )
                    .border(1.dp, StatusCrimson.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .testTag("top_emergency_stop")
            ) {
                Icon(
                    imageVector = Icons.Filled.StopCircle,
                    contentDescription = "Emergency Stop Automation",
                    tint = if (isAutomationRunning) Color.White else StatusCrimson,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MayaBottomNavBar(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit
) {
    Surface(
        color = ObsidianSurface.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            AppDestination.entries.forEach { dest ->
                val isSelected = dest == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(dest) },
                    modifier = Modifier.testTag("nav_${dest.route}"),
                    icon = {
                        Icon(
                            imageVector = destinationIcon(dest, isSelected),
                            contentDescription = dest.label
                        )
                    },
                    label = {
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        indicatorColor = GlassBase,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun MayaNavRail(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit
) {
    NavigationRail(
        containerColor = ObsidianSurface.copy(alpha = 0.85f),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        AppDestination.entries.forEach { dest ->
            val isSelected = dest == selected
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSelect(dest) },
                modifier = Modifier.testTag("rail_${dest.route}"),
                icon = {
                    Icon(
                        imageVector = destinationIcon(dest, isSelected),
                        contentDescription = dest.label
                    )
                },
                label = { Text(dest.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    indicatorColor = GlassBase,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun destinationIcon(dest: AppDestination, selected: Boolean) = when (dest) {
    AppDestination.MAYA -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    AppDestination.CONTROLS -> if (selected) Icons.Filled.Tune else Icons.Outlined.Tune
    AppDestination.AUTOMATE -> if (selected) Icons.Filled.AutoFixHigh else Icons.Outlined.AutoFixHigh
    AppDestination.DEVICE -> if (selected) Icons.Filled.Devices else Icons.Outlined.Devices
}
