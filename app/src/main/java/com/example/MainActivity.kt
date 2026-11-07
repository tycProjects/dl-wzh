package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HistorySheet
import com.example.ui.screens.*
import com.example.ui.theme.SharpCalculatorTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.CalculatorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            var showHistorySheet by remember { mutableStateOf(false) }

            // Speech recognizer result launcher
            val speechLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val spokenResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = spokenResults?.firstOrNull()
                    if (!spokenText.isNullOrBlank()) {
                        viewModel.onSpokenEquationReceived(spokenText)
                    }
                }
            }

            // Audio permission request launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    startSpeechRecognition(context, speechLauncher)
                } else {
                    Toast.makeText(
                        context,
                        "Microphone permission is required for voice-to-text calculations",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            val triggerVoiceInput = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    startSpeechRecognition(context, speechLauncher)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            // Back handler to navigate back to Basic tab if in another tab
            BackHandler(enabled = state.currentTab != AppTab.BASIC) {
                viewModel.switchTab(AppTab.BASIC)
            }

            SharpCalculatorTheme(
                palette = state.themePalette,
                darkModePref = state.darkModePref
            ) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        CalculatorTopBar(
                            currentTab = state.currentTab,
                            onVoiceClick = triggerVoiceInput,
                            onOpenHistory = { showHistorySheet = true },
                            onOpenRoadmap = { viewModel.toggleRoadmapSheet(true) },
                            onOpenSettings = { viewModel.switchTab(AppTab.SETTINGS) },
                            onTabSelected = { viewModel.switchTab(it) }
                        )
                    },
                    bottomBar = {
                        CalculatorBottomNav(
                            currentTab = state.currentTab,
                            onTabSelected = { viewModel.switchTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = state.currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { tab ->
                            when (tab) {
                                AppTab.BASIC -> BasicCalcScreen(
                                    state = state,
                                    onKeyClick = { viewModel.onKeyClick(it) },
                                    onOpenHistory = { showHistorySheet = true },
                                    onVoiceClick = triggerVoiceInput,
                                    onClearVoiceMessage = { viewModel.clearVoiceMessage() }
                                )
                                AppTab.SCIENTIFIC -> ScientificCalcScreen(
                                    state = state,
                                    onKeyClick = { viewModel.onKeyClick(it) },
                                    onOpenHistory = { showHistorySheet = true },
                                    onVoiceClick = triggerVoiceInput,
                                    onClearVoiceMessage = { viewModel.clearVoiceMessage() }
                                )
                                AppTab.VAT -> VatCalcScreen(
                                    state = state,
                                    onSetVatMode = { viewModel.setVatMode(it) },
                                    onSetNetAmount = { viewModel.setVatAmountInput(it) },
                                    onSetGrossAmount = { viewModel.setVatGrossInput(it) },
                                    onSetVatRate = { viewModel.setVatRateInput(it) },
                                    onSelectPreset = { viewModel.selectVatPreset(it) },
                                    onAddCustomPreset = { rate, label -> viewModel.addCustomVatPreset(rate, label) },
                                    onSendToCalc = { viewModel.sendVatResultToCalc(it) }
                                )
                                AppTab.CONVERTER -> UnitConverterScreen(
                                    state = state,
                                    onSetDimension = { viewModel.setConverterDimension(it) },
                                    onSetFromUnit = { viewModel.setConverterFromUnit(it) },
                                    onSetToUnit = { viewModel.setConverterToUnit(it) },
                                    onSetInputValue = { viewModel.setConverterInputValue(it) },
                                    onSwapUnits = { viewModel.swapConverterUnits() }
                                )
                                AppTab.DISCOUNT -> DiscountTipScreen(
                                    state = state,
                                    onSetBill = { viewModel.setDiscountBill(it) },
                                    onSetDiscountPercent = { viewModel.setDiscountPercent(it) },
                                    onSetTipPercent = { viewModel.setTipPercent(it) },
                                    onSetSplitPeople = { viewModel.setSplitPeople(it) }
                                )
                                AppTab.SETTINGS -> SettingsThemeScreen(
                                    state = state,
                                    onSetTheme = { viewModel.setThemePalette(it) },
                                    onSetDarkMode = { viewModel.setDarkModePref(it) },
                                    onSetHaptic = { viewModel.setHapticEnabled(it) },
                                    onSetPrecision = { viewModel.setDecimalPrecision(it) },
                                    onOpenRoadmap = { viewModel.toggleRoadmapSheet(true) }
                                )
                            }
                        }

                        // Bottom Sheets
                        if (showHistorySheet) {
                            HistorySheet(
                                history = state.history,
                                palette = state.themePalette,
                                onSelectItem = { viewModel.onSelectHistoryItem(it) },
                                onClearHistory = { viewModel.clearHistory() },
                                onDismiss = { showHistorySheet = false }
                            )
                        }

                        if (state.showRoadmapSheet) {
                            RoadmapFeedbackSheet(
                                votes = state.roadmapVotes,
                                userVoted = state.userVotedFeatures,
                                palette = state.themePalette,
                                onVote = { viewModel.voteFeature(it) },
                                onDismiss = { viewModel.toggleRoadmapSheet(false) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startSpeechRecognition(
        context: Context,
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>
    ) {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak your equation (e.g. '50 plus 20' or 'sqrt 144')..."
                )
            }
            launcher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Speech recognition is not available on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorTopBar(
    currentTab: AppTab,
    onVoiceClick: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRoadmap: () -> Unit,
    onOpenSettings: () -> Unit,
    onTabSelected: (AppTab) -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sharp",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Calc",
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier.testTag("topbar_btn_voice")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Equation",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onOpenRoadmap,
                modifier = Modifier.testTag("topbar_btn_roadmap")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Upcoming Features & Options",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier.testTag("topbar_btn_history")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("topbar_btn_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (currentTab == AppTab.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun CalculatorBottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            Triple(AppTab.BASIC, "Basic", Icons.Default.Calculate),
            Triple(AppTab.SCIENTIFIC, "Scientific", Icons.Default.Functions),
            Triple(AppTab.VAT, "VAT / Tax", Icons.Default.Percent),
            Triple(AppTab.CONVERTER, "Converter", Icons.AutoMirrored.Filled.CompareArrows),
            Triple(AppTab.DISCOUNT, "Discount", Icons.Default.LocalOffer)
        )

        items.forEach { (tab, label, icon) ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name}")
            )
        }
    }
}
