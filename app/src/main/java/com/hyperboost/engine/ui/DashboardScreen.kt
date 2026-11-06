// app/src/main/java/com/hyperboost/engine/ui/DashboardScreen.kt
package com.hyperboost.engine.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import android.content.Intent
// AUTO-REPAIR: disabled -- unresolved reference 'ads' (was: import com.hyperboost.engine.ads.findActivity)
import kotlinx.coroutines.launch
import com.hyperboost.engine.core.PerformanceEngine
import com.hyperboost.engine.core.ShizukuManager
import com.hyperboost.engine.ui.components.HbCard
import com.hyperboost.engine.ui.components.HbIconBadge
import com.hyperboost.engine.ui.components.HbSwitch
import com.hyperboost.engine.ui.components.SectionHeader
import com.hyperboost.engine.ui.components.StatChip
import com.hyperboost.engine.ui.components.StatTile
import com.hyperboost.engine.ui.theme.DangerRed
import com.hyperboost.engine.ui.theme.DarkBackground
import com.hyperboost.engine.ui.theme.ElectricOrange
import com.hyperboost.engine.ui.theme.NeonCyan
import com.hyperboost.engine.ui.theme.NeonCyanVariant
import com.hyperboost.engine.ui.theme.NeonGreen
import com.hyperboost.engine.ui.theme.TextMuted
import com.hyperboost.engine.ui.theme.TextPrimary
import com.hyperboost.engine.ui.theme.TextSecondary

/** Top-level sections of the dashboard. Each tab shows a focused set of cards. */
private enum class DashTab(val label: String, val icon: ImageVector) {
    Boost("Boost", Icons.Default.Bolt),
    Monitor("Monitor", Icons.Default.Monitor),
    Tools("Tools", Icons.Default.Build),
    Games("Games", Icons.Default.SportsEsports)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val shizukuVersion by viewModel.shizukuVersion.collectAsState()
    val telemetry by viewModel.telemetryState.collectAsState()
    val games by viewModel.installedGames.collectAsState()
    val isHudRunning by viewModel.isHudRunning.collectAsState()
    val isCrosshairActive by viewModel.isCrosshairActive.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    // Easy Enable States
    val isMasterTurboActive by viewModel.isMasterTurboActive.collectAsState()
    val isTouchBoostActive by viewModel.isTouchBoostActive.collectAsState()
    val isZeroAnimationActive by viewModel.isZeroAnimationActive.collectAsState()
    val isWifiGamingActive by viewModel.isWifiGamingActive.collectAsState()
    val isVulkanActive by viewModel.isVulkanActive.collectAsState()
    val isSurfaceFlingerUncapped by viewModel.isSurfaceFlingerUncapped.collectAsState()
    val isCachedFreezerActive by viewModel.isCachedFreezerActive.collectAsState()
    val isAdaptiveBrightnessOff by viewModel.isAdaptiveBrightnessOff.collectAsState()
    val isShowRefreshRateActive by viewModel.isShowRefreshRateActive.collectAsState()
    val selectedRefreshRate by viewModel.selectedRefreshRate.collectAsState()
    val supportedRefreshRates by viewModel.supportedRefreshRates.collectAsState()
    val selectedDns by viewModel.selectedDns.collectAsState()
    val activeBoostersCount by viewModel.activeBoostersCount.collectAsState()
    val isSustainedPerfActive by viewModel.isSustainedPerfActive.collectAsState()
    val isGameDndActive by viewModel.isGameDndActive.collectAsState()
    val isPhantomProtectionActive by viewModel.isPhantomProtectionActive.collectAsState()
    val isBatteryThrottleOff by viewModel.isBatteryThrottleOff.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val adScope = rememberCoroutineScope()
    var showCrosshairDialog by remember { mutableStateOf(false) }
    var showAdbGuideDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var configuringModeGame by remember { mutableStateOf<MainViewModel.GameProfile?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val currentTab = DashTab.entries[selectedTab]
    val shizukuAuthorized = shizukuStatus == ShizukuManager.ShizukuStatus.AUTHORIZED

    LaunchedEffect(Unit) {
        viewModel.userFeedbackMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B1622), MaterialTheme.colorScheme.background)
                )
            ),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                HorizontalDivider(color = Color(0x14FFFFFF), thickness = 1.dp)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    DashTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = index == selectedTab,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = TextPrimary,
                                indicatorColor = NeonCyan.copy(alpha = 0.14f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = TextPrimary)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(NeonCyan, NeonGreen))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "HyperBoost",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.3.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "PERFORMANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.1.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    StatChip(
                        label = if (shizukuAuthorized) "SHIZUKU" else "NO ADB",
                        color = if (shizukuAuthorized) NeonGreen else DangerRed
                    )
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                tint = TextSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh status") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                modifier = Modifier.testTag("refresh_status_button"),
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.reevaluateShizuku()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Setup guide") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showAdbGuideDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextSecondary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp)
        ) {
            // Shizuku status stays visible on every tab.
            item(key = "shizuku_status") {
                ShizukuStatusBanner(
                    status = shizukuStatus,
                    version = shizukuVersion,
                    onRequestPermission = { viewModel.requestShizukuPermission() },
                    onOpenGuide = { showAdbGuideDialog = true }
                )
            }

            when (currentTab) {
                DashTab.Boost -> {
                    item(key = "master_turbo") {
                        MasterTurboHeroCard(
                            isActive = isMasterTurboActive,
                            activeCount = activeBoostersCount,
                            totalCount = MainViewModel.TOTAL_BOOSTERS,
                            isBusy = isBusy,
                            onToggle = {
                                viewModel.toggleMasterTurbo()
                            }
                        )
                    }
                    item(key = "easy_enable") {
                        EasyEnableOptimizationSection(
                            isTouchBoostActive = isTouchBoostActive,
                            onToggleTouch = { viewModel.toggleTouchBoost() },
                            isZeroAnimationActive = isZeroAnimationActive,
                            onToggleAnimations = { viewModel.toggleZeroAnimations() },
                            isWifiGamingActive = isWifiGamingActive,
                            onToggleWifi = { viewModel.toggleWifiGaming() },
                            isVulkanActive = isVulkanActive,
                            onToggleVulkan = { viewModel.toggleVulkan() },
                            isSurfaceFlingerUncapped = isSurfaceFlingerUncapped,
                            onToggleSurfaceFlinger = { viewModel.toggleSurfaceFlingerLatency() },
                            selectedRefreshRate = selectedRefreshRate,
                            supportedRefreshRates = supportedRefreshRates,
                            onSelectRefreshRate = { viewModel.setRefreshRate(it) },
                            selectedDns = selectedDns,
                            onSelectDns = { viewModel.setPrivateDns(it) },
                            onTrimCaches = { viewModel.trimCaches() }
                        )
                    }
                    item(key = "gaming_tuning") {
                        GamingTuningSection(
                            isCachedFreezerActive = isCachedFreezerActive,
                            onToggleFreezer = { viewModel.toggleCachedAppsFreezer() },
                            isAdaptiveBrightnessOff = isAdaptiveBrightnessOff,
                            onToggleAdaptiveBrightness = { viewModel.toggleAdaptiveBrightness() },
                            isShowRefreshRateActive = isShowRefreshRateActive,
                            onToggleShowRefreshRate = { viewModel.toggleShowRefreshRate() }
                        )
                    }
                    item(key = "advanced_tuning") {
                        AdvancedTuningSection(
                            isSustainedPerfActive = isSustainedPerfActive,
                            onToggleSustainedPerf = { viewModel.toggleSustainedPerformance() },
                            isGameDndActive = isGameDndActive,
                            onToggleGameDnd = { viewModel.toggleGameDnd() },
                            isPhantomProtectionActive = isPhantomProtectionActive,
                            onTogglePhantom = { viewModel.togglePhantomProtection() },
                            isBatteryThrottleOff = isBatteryThrottleOff,
                            onToggleBattery = { viewModel.toggleBatteryThrottle() },
                            onKillBackground = { viewModel.killBackgroundApps() },
                            onStorageMaintenance = { viewModel.runStorageMaintenance() },
                            onRestoreDefaults = { viewModel.restoreDefaults() }
                        )
                    }
                }

                DashTab.Monitor -> {
                    item(key = "telemetry") {
                        LiveTelemetrySection(telemetry = telemetry)
                    }
                }

                DashTab.Tools -> {
                    item(key = "more_tools") {
                        ExtraTweaksSection(viewModel = viewModel)
                    }
                    item(key = "advanced_tweaks") {
                        AdvancedTweaksSection(viewModel = viewModel)
                    }
                    item(key = "hud_controls") {
                        HudControlsCard(
                            isHudRunning = isHudRunning,
                            isCrosshairActive = isCrosshairActive,
                            isAccessibilityActive = isAccessibilityActive,
                            onToggleHud = { viewModel.toggleHudOverlay() },
                            onToggleCrosshair = { viewModel.toggleCrosshair() },
                            onConfigureCrosshair = { showCrosshairDialog = true },
                            onOpenOverlaySettings = onOpenOverlaySettings,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings
                        )
                    }
                }

                DashTab.Games -> {
                    item(key = "games") {
                        GameLauncherSection(
                            games = games,
                            onLaunchGame = { viewModel.launchGame(it) },
                            onToggleGameOpt = { viewModel.toggleGameOptimization(it) },
                            onCompileShaders = { viewModel.compileGameShaders(it) },
                            onConfigureGameMode = { configuringModeGame = it }
                        )
                    }
                }
            }
        }
    }

    if (showCrosshairDialog) {
        CrosshairConfigDialog(
            viewModel = viewModel,
            onDismiss = { showCrosshairDialog = false }
        )
    }

    if (showAdbGuideDialog) {
        AdbGuideDialog(onDismiss = { showAdbGuideDialog = false })
    }
    configuringModeGame?.let { game ->
        GameModeDialog(
            game = game,
            onDismiss = { configuringModeGame = null },
            onSelect = { mode -> viewModel.setGameMode(game.packageName, mode) }
        )
    }
}

@Composable
fun MasterTurboHeroCard(
    isActive: Boolean,
    activeCount: Int,
    totalCount: Int,
    isBusy: Boolean,
    onToggle: () -> Unit
) {
    val safeTotal = if (totalCount <= 0) 1 else totalCount
    val shownCount = if (isActive) totalCount else activeCount
    val targetProgress = if (isActive) 1f else (activeCount.toFloat() / safeTotal).coerceIn(0f, 1f)
    val progress by animateFloatAsState(targetValue = targetProgress, label = "master_boost_progress")
    val statusLabel = when {
        isBusy -> "WORKING"
        isActive -> "ENGAGED"
        else -> "STANDBY"
    }

    HbCard(
        modifier = Modifier.testTag("master_turbo_card"),
        accent = if (isActive) NeonCyan else null,
        onClick = if (isBusy) null else onToggle
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HbIconBadge(
                icon = Icons.Default.Bolt,
                active = isActive,
                tint = NeonCyan,
                size = 56.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Master Boost",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.3.sp
                        ),
                        color = TextPrimary
                    )
                    if (isActive || isBusy) {
                        StatChip(
                            label = if (isBusy) "WORKING" else "ACTIVE",
                            color = if (isBusy) NeonCyan else NeonGreen
                        )
                    }
                }
                Text(
                    text = if (isActive) "All supported subsystems are engaged" else "One tap to apply every supported booster",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            HbSwitch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                enabled = !isBusy,
                modifier = Modifier.testTag("switch_master_turbo")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                label = "Modules",
                value = "$shownCount/$totalCount",
                valueColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Status",
                value = statusLabel,
                valueColor = when (statusLabel) {
                    "ENGAGED" -> NeonGreen
                    "WORKING" -> NeonCyan
                    else -> TextSecondary
                },
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x1FFFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(NeonCyan, NeonGreen)))
            )
        }
    }
}

/**
 * Easy Enable Optimization Section: Clear, interactive toggles for all modules.
 */
@Composable
fun EasyEnableOptimizationSection(
    isTouchBoostActive: Boolean,
    onToggleTouch: () -> Unit,
    isZeroAnimationActive: Boolean,
    onToggleAnimations: () -> Unit,
    isWifiGamingActive: Boolean,
    onToggleWifi: () -> Unit,
    isVulkanActive: Boolean,
    onToggleVulkan: () -> Unit,
    isSurfaceFlingerUncapped: Boolean,
    onToggleSurfaceFlinger: () -> Unit,
    selectedRefreshRate: Int,
    supportedRefreshRates: List<Int>,
    onSelectRefreshRate: (Int) -> Unit,
    selectedDns: String?,
    onSelectDns: (String?) -> Unit,
    onTrimCaches: () -> Unit
) {
    val activeCount = listOf(
        isTouchBoostActive,
        isZeroAnimationActive,
        isWifiGamingActive,
        isVulkanActive,
        isSurfaceFlingerUncapped
    ).count { it }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Rootless boost modules",
            trailing = "$activeCount/5 active"
        )

        // 1. Touch & Latency Overclock Card
        EasyToggleCard(
            title = "Touch & Debounce Overclock",
            description = "Configures supported touch-prediction and debounce options; results vary by device",
            icon = Icons.Default.TouchApp,
            isActive = isTouchBoostActive,
            onToggle = onToggleTouch,
            tag = "toggle_touch_boost",
            status = if (isTouchBoostActive) "Verified on this device" else null
        )

        // 2. Zero Window Animations Card
        EasyToggleCard(
            title = "Zero Window Animations (0.0x)",
            description = "Eliminates UI transition latency for instantaneous app & game response",
            icon = Icons.Default.Speed,
            isActive = isZeroAnimationActive,
            onToggle = onToggleAnimations,
            tag = "toggle_zero_animations"
        )

        // 3. Wi-Fi Gaming Low-Latency Card
        EasyToggleCard(
            title = "Wi-Fi Low-Latency Lock",
            description = "Reduces power-save Wi-Fi stalls where the device supports it",
            icon = Icons.Default.Wifi,
            isActive = isWifiGamingActive,
            onToggle = onToggleWifi,
            tag = "toggle_wifi_gaming",
            status = if (isWifiGamingActive) "Verified on this device" else null
        )

        // 4. Force Vulkan GPU Engine Card
        EasyToggleCard(
            title = "Force Vulkan GPU Pipeline",
            description = "Routes system HWUI and game driver rendering through Vulkan",
            icon = Icons.Default.Layers,
            isActive = isVulkanActive,
            onToggle = onToggleVulkan,
            tag = "toggle_vulkan"
        )

        // 5. SurfaceFlinger Uncapped Latching Card
        EasyToggleCard(
            title = "SurfaceFlinger Instant Latch",
            description = "Unsignaled buffer submission eliminates compositor queue latency",
            icon = Icons.Default.Timeline,
            isActive = isSurfaceFlingerUncapped,
            onToggle = onToggleSurfaceFlinger,
            tag = "toggle_surfaceflinger"
        )

        // Refresh Rate Selector Card
        HbCard(contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(NeonCyan.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(19.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Refresh rate lock",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        "Choose a rate reported as supported by this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                StatChip(label = "${selectedRefreshRate}Hz", color = NeonGreen)
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedRefreshRates.forEach { hz ->
                    val isSelected = selectedRefreshRate == hz
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectRefreshRate(hz) },
                        label = { Text("${hz}Hz") },
                        modifier = Modifier.testTag("btn_hz_$hz"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.16f),
                            selectedLabelColor = NeonCyan,
                            labelColor = TextMuted
                        )
                    )
                }
            }
        }

        // DNS & Cache Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // DNS Selector Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        when (selectedDns) {
                            null -> onSelectDns(PerformanceEngine.DNS_CLOUDFLARE)
                            PerformanceEngine.DNS_CLOUDFLARE -> onSelectDns(PerformanceEngine.DNS_GOOGLE)
                            else -> onSelectDns(null)
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0x12FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Text("Gaming DNS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(
                        text = when (selectedDns) {
                            PerformanceEngine.DNS_CLOUDFLARE -> "Cloudflare 1.1.1.1"
                            PerformanceEngine.DNS_GOOGLE -> "Google DNS"
                            else -> "Default (Tap to Switch)"
                        },
                        fontSize = 11.sp,
                        color = if (selectedDns != null) NeonGreen else TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Cache Trim Button Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onTrimCaches() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0x12FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = ElectricOrange, modifier = Modifier.size(16.dp))
                        Text("Trim Caches", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(
                        text = "Trim app caches (up to 5 GB)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Resolution presets plus the extra gaming tuning toggles. */
@Composable
fun GamingTuningSection(
    isCachedFreezerActive: Boolean,
    onToggleFreezer: () -> Unit,
    isAdaptiveBrightnessOff: Boolean,
    onToggleAdaptiveBrightness: () -> Unit,
    isShowRefreshRateActive: Boolean,
    onToggleShowRefreshRate: () -> Unit
) {
    val activeCount = listOf(
        isCachedFreezerActive,
        isAdaptiveBrightnessOff,
        isShowRefreshRateActive
    ).count { it }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "More gaming tuning",
            trailing = "$activeCount/3 active"
        )

        EasyToggleCard(
            title = "Cached Apps Freezer",
            description = "Freezes cached apps more aggressively (Android 11+)",
            icon = Icons.Default.AcUnit,
            isActive = isCachedFreezerActive,
            onToggle = onToggleFreezer,
            tag = "toggle_cached_freezer",
            status = if (isCachedFreezerActive) "Verified on this device" else null
        )

        EasyToggleCard(
            title = "Adaptive Brightness Off",
            description = "Keeps the screen brightness fixed during a session",
            icon = Icons.Default.WbSunny,
            isActive = isAdaptiveBrightnessOff,
            onToggle = onToggleAdaptiveBrightness,
            tag = "toggle_adaptive_brightness",
            status = if (isAdaptiveBrightnessOff) "Verified on this device" else null
        )

        EasyToggleCard(
            title = "Show Refresh Rate",
            description = "Adds Android's refresh-rate overlay",
            icon = Icons.Default.Speed,
            isActive = isShowRefreshRateActive,
            onToggle = onToggleShowRefreshRate,
            tag = "toggle_show_refresh_rate",
            status = if (isShowRefreshRateActive) "Verified on this device" else null
        )
    }
}

/**
 * Advanced rootless tuning: power / thermal / background-process / storage tweaks plus a
 * one-tap "restore my original settings" panic button.
 */
@Composable
fun AdvancedTuningSection(
    isSustainedPerfActive: Boolean,
    onToggleSustainedPerf: () -> Unit,
    isGameDndActive: Boolean,
    onToggleGameDnd: () -> Unit,
    isPhantomProtectionActive: Boolean,
    onTogglePhantom: () -> Unit,
    isBatteryThrottleOff: Boolean,
    onToggleBattery: () -> Unit,
    onKillBackground: () -> Unit,
    onStorageMaintenance: () -> Unit,
    onRestoreDefaults: () -> Unit
) {
    val activeCount = listOf(
        isSustainedPerfActive,
        isGameDndActive,
        isPhantomProtectionActive,
        isBatteryThrottleOff
    ).count { it }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Advanced rootless tuning",
            trailing = "$activeCount/4 active"
        )

        EasyToggleCard(
            title = "Sustained Performance Mode",
            description = "Holds steadier CPU/GPU clocks to cut thermal throttling (Android 12+)",
            icon = Icons.Default.Bolt,
            isActive = isSustainedPerfActive,
            onToggle = onToggleSustainedPerf,
            tag = "toggle_sustained_perf"
        )

        EasyToggleCard(
            title = "Phantom Process Shield",
            description = "Stops Android 12+ from randomly killing game & emulator child processes",
            icon = Icons.Default.Shield,
            isActive = isPhantomProtectionActive,
            onToggle = onTogglePhantom,
            tag = "toggle_phantom_shield"
        )

        EasyToggleCard(
            title = "No Battery Throttling",
            description = "Turns off Battery Saver and Adaptive Battery; device behavior varies",
            icon = Icons.Default.BatteryChargingFull,
            isActive = isBatteryThrottleOff,
            onToggle = onToggleBattery,
            tag = "toggle_battery_throttle"
        )

        EasyToggleCard(
            title = "Game Do Not Disturb",
            description = "Priority-only DND: no popups or heads-up banners mid-match",
            icon = Icons.Default.NotificationsOff,
            isActive = isGameDndActive,
            onToggle = onToggleGameDnd,
            tag = "toggle_game_dnd"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Clear Background",
                subtitle = "Kill cached apps",
                icon = Icons.Default.Memory,
                tint = NeonCyan,
                onClick = onKillBackground,
                compact = true
            )
            ActionTile(
                modifier = Modifier.weight(1f),
                title = "Storage Boost",
                subtitle = "TRIM + dexopt",
                icon = Icons.Default.Storage,
                tint = ElectricOrange,
                onClick = onStorageMaintenance,
                compact = true
            )
        }

        ActionTile(
            modifier = Modifier.fillMaxWidth(),
            title = "Restore My Original Settings",
            subtitle = "Undo every tweak back to the exact values you had before",
            icon = Icons.Default.RestartAlt,
            tint = DangerRed,
            onClick = onRestoreDefaults
        )
    }
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        modifier = modifier
            .then(if (compact) Modifier.heightIn(min = 88.dp) else Modifier)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0x12FFFFFF))
    ) {
        if (compact) {
            Column(
                modifier = Modifier.padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tint.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                    }
                    Text(
                        title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
                Text(
                    subtitle,
                    fontSize = 10.5.sp,
                    color = TextMuted,
                    maxLines = 2
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tint.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Reusable Easy-Toggle Card with description, icon, and switch.
 */
@Composable
fun EasyToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isActive: Boolean,
    onToggle: () -> Unit,
    tag: String,
    status: String? = null
) {
    HbCard(
        accent = if (isActive) NeonCyan else null,
        onClick = onToggle,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HbIconBadge(
                icon = icon,
                active = isActive,
                tint = NeonCyan,
                size = 42.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
                if (status != null) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) NeonGreen else TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            HbSwitch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

@Composable
fun ShizukuStatusBanner(
    status: ShizukuManager.ShizukuStatus,
    version: Int,
    onRequestPermission: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val isAuthorized = status == ShizukuManager.ShizukuStatus.AUTHORIZED
    val isRunning = status != ShizukuManager.ShizukuStatus.NOT_RUNNING
    val accent = if (isAuthorized) NeonGreen else DangerRed
    val title = when (status) {
        ShizukuManager.ShizukuStatus.AUTHORIZED -> "Shizuku connected"
        ShizukuManager.ShizukuStatus.UNAUTHORIZED -> "Shizuku permission needed"
        ShizukuManager.ShizukuStatus.NOT_RUNNING -> "Shizuku is not running"
    }
    val subtitle = if (isAuthorized) {
        "ADB-level privileges active · API v$version"
    } else {
        "Start Shizuku and authorize HyperBoost to use system tuning"
    }

    HbCard(
        accent = accent,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HbIconBadge(
                icon = if (isAuthorized) Icons.Default.CheckCircle else Icons.Default.Warning,
                active = isAuthorized,
                tint = accent,
                size = 42.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            StatChip(
                label = if (isAuthorized) "ACTIVE" else "ACTION",
                color = accent
            )
        }

        if (!isAuthorized) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRunning) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_request_shizuku"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = DarkBackground
                        )
                    ) {
                        Text("Grant permission", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onOpenGuide,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_shizuku_guide")
                ) {
                    Text("Setup guide")
                }
            }
        }
    }
}

@Composable
fun LiveTelemetrySection(telemetry: MainViewModel.TelemetryState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Live engine telemetry",
            trailing = telemetry.fps?.let { "${it.toInt()} FPS" } ?: "no FPS data"
        )

        val fps = telemetry.fps
        val hz = telemetry.refreshRateHz
        val fpsColor = when {
            fps == null -> TextSecondary
            fps >= hz * 0.9f -> NeonGreen
            fps >= hz * 0.5f -> Color(0xFFFFD54F)
            else -> ElectricOrange
        }
        val jank = telemetry.jankPercentage
        val grey = TextSecondary

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCard(Modifier.weight(1f), 125.dp, "LIVE FPS", Icons.Default.Speed, NeonCyan) {
                Text(
                    text = fps?.let { "${it.toInt()}" } ?: "--",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = fpsColor
                )
                Text(
                    text = if (fps != null)
                        "1% low ${telemetry.onePercentLowFps?.toInt() ?: "--"} · ${hz}Hz panel"
                    else telemetry.statusNote ?: "No data",
                    fontSize = 10.sp,
                    color = grey,
                    maxLines = 2
                )
            }
            TelemetryCard(Modifier.weight(1f), 125.dp, "FRAMETIME", Icons.Default.Timeline, NeonCyan) {
                Text(
                    text = telemetry.frametimeMs?.let { "$it ms" } ?: "--",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = TextPrimary
                )
                Text(
                    text = jank?.let { "Jank: $it%" } ?: "Jank: --",
                    fontSize = 11.sp,
                    color = when {
                        jank == null -> grey
                        jank < 5f -> NeonGreen
                        else -> DangerRed
                    }
                )
                Text(
                    text = if (fps != null) "via ${telemetry.frameSource}" else "not measuring",
                    fontSize = 10.sp,
                    color = grey
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCard(Modifier.weight(1f), 105.dp, "CPU / GPU", Icons.Default.Memory, NeonCyan) {
                Text(
                    text = telemetry.cpuUsagePct?.let { "${it.toInt()}% load" } ?: "-- load",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                val cpuT = telemetry.cpuTempCelsius?.let { "${it.toInt()}°" } ?: "--"
                val gpuT = telemetry.gpuTempCelsius?.let { "${it.toInt()}°" } ?: "--"
                Text("CPU $cpuT · GPU $gpuT", fontSize = 11.sp, color = grey)
            }
            TelemetryCard(Modifier.weight(1f), 105.dp, "MEMORY", Icons.Default.Memory, NeonCyan) {
                Text(
                    text = "${telemetry.ramUsedMb} / ${telemetry.ramTotalMb} MB",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                val ramFrac = if (telemetry.ramTotalMb > 0)
                    (telemetry.ramUsedMb.toFloat() / telemetry.ramTotalMb.toFloat()).coerceIn(0f, 1f)
                else 0f
                LinearProgressIndicator(
                    progress = { ramFrac },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (ramFrac < 0.8f) NeonCyan else DangerRed,
                    trackColor = Color(0x33FFFFFF)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCard(Modifier.weight(1f), 105.dp, "BATTERY / POWER", Icons.Default.DeviceThermostat, ElectricOrange) {
                val bt = telemetry.batteryTempCelsius
                Text(
                    text = bt?.let { "$it°C" } ?: "--",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (bt == null) grey else if (bt < 40f) NeonGreen else DangerRed
                )
                val pw = telemetry.powerDrainMw
                Text(
                    text = when {
                        pw == null -> "power n/a"
                        telemetry.isCharging -> "charging +$pw mW"
                        else -> "drawing $pw mW"
                    },
                    fontSize = 10.sp,
                    color = grey
                )
            }
            TelemetryCard(Modifier.weight(1f), 105.dp, "DISPLAY", Icons.Default.Speed, NeonCyan) {
                Text(
                    text = "${hz}Hz",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = TextPrimary
                )
                Text(text = telemetry.displayResolution, fontSize = 10.sp, color = grey, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TelemetryCard(
    modifier: Modifier,
    height: androidx.compose.ui.unit.Dp,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0x12FFFFFF))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.7.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextMuted
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                }
            }
            content()
        }
    }
}

@Composable
fun HudControlsCard(
    isHudRunning: Boolean,
    isCrosshairActive: Boolean,
    isAccessibilityActive: Boolean,
    onToggleHud: () -> Unit,
    onToggleCrosshair: () -> Unit,
    onConfigureCrosshair: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    HbCard(contentPadding = PaddingValues(16.dp)) {
            SectionHeader(title = "Floating overlay & services")

            // Telemetry HUD Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Floating Telemetry HUD", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Live draggable FPS, frametime curve, and temp", fontSize = 12.sp, color = TextMuted)
                }
                HbSwitch(
                    checked = isHudRunning,
                    onCheckedChange = { onToggleHud() },
                    modifier = Modifier.testTag("switch_hud_overlay")
                )
            }

            HorizontalDivider(color = Color(0x1FFFFFFF))

            // Crosshair Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Screen Center Crosshair", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Hardware crosshair overlay for shooter games", fontSize = 12.sp, color = TextMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onConfigureCrosshair,
                        modifier = Modifier.testTag("btn_configure_crosshair")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Crosshair Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                    HbSwitch(
                        checked = isCrosshairActive,
                        onCheckedChange = { onToggleCrosshair() },
                        modifier = Modifier.testTag("switch_crosshair")
                    )
                }
            }

            HorizontalDivider(color = Color(0x1FFFFFFF))

            // Permissions helper buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenOverlaySettings,
                    modifier = Modifier.weight(1f).testTag("btn_overlay_permission")
                ) {
                    Text("Overlay Perms", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier.weight(1f).testTag("btn_accessibility_service")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isAccessibilityActive) NeonGreen else DangerRed)
                        )
                        Text("Game Detection", fontSize = 12.sp)
                    }
                }
            }
        }
}

@Composable
fun GameLauncherSection(
    games: List<MainViewModel.GameProfile>,
    onLaunchGame: (MainViewModel.GameProfile) -> Unit,
    onToggleGameOpt: (MainViewModel.GameProfile) -> Unit,
    onCompileShaders: (MainViewModel.GameProfile) -> Unit,
    onConfigureGameMode: (MainViewModel.GameProfile) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Game profiles",
            trailing = "${games.size} apps"
        )

        if (games.isEmpty()) {
            HbCard(contentPadding = PaddingValues(24.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No games detected on this device", color = TextSecondary)
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(games) { game ->
                    GameCard(
                        game = game,
                        onLaunch = { onLaunchGame(game) },
                        onToggleOpt = { onToggleGameOpt(game) },
                        onCompile = { onCompileShaders(game) },
                        onConfigureGameMode = { onConfigureGameMode(game) }
                    )
                }
            }
        }
    }
}

@Composable
fun GameCard(
    game: MainViewModel.GameProfile,
    onLaunch: () -> Unit,
    onToggleOpt: () -> Unit,
    onCompile: () -> Unit,
    onConfigureGameMode: () -> Unit
) {
    val modeLabel = game.gameMode.replaceFirstChar { it.uppercase() }
    val statusText = when {
        game.isCompiled -> "AOT shaders ready"
        game.isOptimized -> "Auto-boost ${game.targetRefreshRate}Hz"
        else -> "Standard profile"
    }
    val statusColor = when {
        game.isCompiled -> NeonGreen
        game.isOptimized -> NeonCyan
        else -> TextSecondary
    }
    Card(
        modifier = Modifier
            .width(176.dp)
            .height(218.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (game.isOptimized) {
            BorderStroke(1.dp, Brush.verticalGradient(listOf(NeonCyan.copy(alpha = 0.75f), NeonGreen.copy(alpha = 0.45f))))
        } else {
            BorderStroke(1.dp, Color(0x12FFFFFF))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (game.icon != null) {
                    val bitmap = remember(game.icon) {
                        game.icon.toBitmap(96, 96).asImageBitmap()
                    }
                    Image(
                        bitmap = bitmap,
                        contentDescription = game.appName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = TextSecondary)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onConfigureGameMode,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Game mode",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    HbSwitch(
                        checked = game.isOptimized,
                        onCheckedChange = { onToggleOpt() }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = game.appName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    color = TextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = "$statusText · $modeLabel",
                        fontSize = 10.sp,
                        color = statusColor,
                        maxLines = 1
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onCompile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, Color(0x1FFFFFFF))
                ) {
                    Text("Pre-compile AOT", fontSize = 10.sp, color = TextSecondary)
                }

                Button(
                    onClick = onLaunch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = DarkBackground
                    )
                ) {
                    Text("Launch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameModeDialog(
    game: MainViewModel.GameProfile,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val modes = listOf(
        "performance" to "Performance",
        "standard" to "Standard",
        "battery" to "Battery"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(game.appName, fontWeight = FontWeight.Bold)
                Text(
                    "Android 12+ only. Applied when this game comes to the front with Optimize on; support varies by device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    modes.forEach { (id, label) ->
                        FilterChip(
                            selected = game.gameMode == id,
                            onClick = {
                                onSelect(id)
                                onDismiss()
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CrosshairConfigDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentColorHex by viewModel.crosshairColorHex.collectAsState()
    val currentSize by viewModel.crosshairSizeDp.collectAsState()
    val currentStroke by viewModel.crosshairStrokeDp.collectAsState()

    var selectedColor by remember { mutableStateOf(currentColorHex) }
    var selectedSize by remember { mutableStateOf(currentSize) }
    var selectedStroke by remember { mutableStateOf(currentStroke) }

    val presetColors = listOf(
        0xFF00E5FF.toInt(), // Cyan
        0xFF00E676.toInt(), // Green
        0xFFFF5252.toInt(), // Red
        0xFFFFD600.toInt(), // Yellow
        0xFFFFFFFF.toInt()  // White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crosshair HUD Customizer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Color Picker Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Reticle Color", fontSize = 12.sp, color = TextMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 1.dp,
                                        color = if (selectedColor == color) TextPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }

                // Size Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Reticle Size: ${selectedSize}dp", fontSize = 12.sp, color = TextMuted)
                    Slider(
                        value = selectedSize.toFloat(),
                        onValueChange = { selectedSize = it.toInt() },
                        valueRange = 16f..48f,
                        steps = 7
                    )
                }

                // Stroke Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Stroke Thickness: ${selectedStroke}dp", fontSize = 12.sp, color = TextMuted)
                    Slider(
                        value = selectedStroke.toFloat(),
                        onValueChange = { selectedStroke = it.toInt() },
                        valueRange = 1f..6f,
                        steps = 4
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.updateCrosshairSettings(selectedColor, selectedSize, selectedStroke)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdbGuideDialog(onDismiss: () -> Unit) {
    val steps = listOf(
        "1. Install Shizuku from Google Play or from its GitHub page.",
        "2. Open Settings > About phone and tap Build number 7 times to enable Developer options.",
        "3. Open Settings > Developer options and turn on Wireless debugging. Keep Wi-Fi on.",
        "4. Open Shizuku and follow its Wireless debugging steps to pair with a pairing code.",
        "5. In Shizuku, start the service using Wireless debugging.",
        "6. Come back here, tap the refresh icon at the top, then tap Grant Permission."
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Easy Shizuku Setup") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Android 11 or newer. No computer needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonCyan
                )
                steps.forEach { step ->
                    Text(step, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    "Shizuku may need to be started again after a reboot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonGreen
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}