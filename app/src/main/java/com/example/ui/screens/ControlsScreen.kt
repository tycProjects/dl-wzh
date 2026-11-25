package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AudioStreamType
import com.example.domain.DeviceTelemetrySnapshot
import com.example.domain.InstalledAppInfo
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.GlassBase
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MayaRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ControlsScreen(
    telemetry: DeviceTelemetrySnapshot,
    installedApps: List<InstalledAppInfo>,
    appsLoading: Boolean,
    onToggleFlashlight: () -> Unit,
    onSetBrightness: (Int) -> Unit,
    onAdjustBrightness: (Boolean) -> Unit,
    onRequestWriteSettings: () -> Unit,
    onAdjustVolume: (AudioStreamType, Boolean) -> Unit,
    onSetStreamVolume: (AudioStreamType, Int) -> Unit,
    onToggleMuteStream: (AudioStreamType, Boolean) -> Unit,
    onOpenQuickSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenInternet: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenVolumePanel: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onRefreshApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStream by rememberSaveable { mutableStateOf(AudioStreamType.MEDIA) }
    var appQuery by rememberSaveable { mutableStateOf("") }

    val filteredApps = remember(installedApps, appQuery) {
        if (appQuery.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(appQuery, ignoreCase = true) ||
                it.packageName.contains(appQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Flashlight Hardware Card
        item(key = "flashlight_card") {
            FlashlightControlCard(
                available = telemetry.flashlightAvailable,
                isOn = telemetry.isFlashlightOn,
                onToggle = onToggleFlashlight
            )
        }

        // 2. Brightness Control Card
        item(key = "brightness_card") {
            BrightnessControlCard(
                brightnessPercent = telemetry.brightnessPercent,
                isAuto = telemetry.isAutoBrightness,
                canWriteSettings = telemetry.canWriteSettings,
                onSetBrightness = onSetBrightness,
                onAdjustBrightness = onAdjustBrightness,
                onGrantPermission = onRequestWriteSettings
            )
        }

        // 3. Volume Stream Control Card
        item(key = "volume_card") {
            val streamState = telemetry.streamVolumes.firstOrNull { it.streamType == selectedStream }
            VolumeControlCard(
                selectedStream = selectedStream,
                currentLevel = streamState?.current ?: 0,
                maxLevel = streamState?.max ?: 15,
                isMuted = streamState?.isMuted ?: false,
                onSelectStream = { selectedStream = it },
                onAdjustVolume = { raise -> onAdjustVolume(selectedStream, raise) },
                onSetVolume = { level -> onSetStreamVolume(selectedStream, level) },
                onToggleMute = { mute -> onToggleMuteStream(selectedStream, mute) }
            )
        }

        // 4. Quick Settings & System Shortcuts
        item(key = "quick_settings_card") {
            QuickSettingsGridCard(
                isAccessibilityEnabled = telemetry.isAccessibilityEnabled,
                onOpenQuickSettings = onOpenQuickSettings,
                onOpenNotifications = onOpenNotifications,
                onOpenWifi = onOpenWifi,
                onOpenInternet = onOpenInternet,
                onOpenBluetooth = onOpenBluetooth,
                onOpenVolumePanel = onOpenVolumePanel,
                onOpenDisplay = onOpenDisplay,
                onOpenBattery = onOpenBattery,
                onOpenSystemSettings = onOpenSystemSettings
            )
        }

        // 5. Installed Apps Launcher
        item(key = "app_launcher_card") {
            AppLauncherCard(
                apps = filteredApps,
                totalCount = installedApps.size,
                loading = appsLoading,
                query = appQuery,
                onQueryChange = { appQuery = it },
                onLaunchApp = onLaunchApp,
                onRefresh = onRefreshApps
            )
        }
    }
}

@Composable
private fun FlashlightControlCard(
    available: Boolean,
    isOn: Boolean,
    onToggle: () -> Unit
) {
    val accent = if (isOn) NeonCyan else TextSecondary
    GlassCard(
        glowColor = if (isOn) NeonCyan else GlassBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isOn) NeonCyan.copy(alpha = 0.22f)
                            else GlassBase
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                        contentDescription = "Flashlight status",
                        tint = accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Flashlight Control",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    StatusPill(
                        text = when {
                            !available -> "No Flash Hardware"
                            isOn -> "TORCH ON"
                            else -> "TORCH OFF"
                        },
                        color = when {
                            !available -> StatusAmber
                            isOn -> StatusEmerald
                            else -> TextSecondary
                        }
                    )
                }
            }

            Switch(
                checked = isOn,
                onCheckedChange = { onToggle() },
                enabled = available,
                modifier = Modifier.testTag("flashlight_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianDeep,
                    checkedTrackColor = NeonCyan,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = GlassBase
                )
            )
        }
    }
}

@Composable
private fun BrightnessControlCard(
    brightnessPercent: Int,
    isAuto: Boolean,
    canWriteSettings: Boolean,
    onSetBrightness: (Int) -> Unit,
    onAdjustBrightness: (Boolean) -> Unit,
    onGrantPermission: () -> Unit
) {
    var sliderVal by remember(brightnessPercent) { mutableFloatStateOf(brightnessPercent.toFloat()) }

    GlassCard(glowColor = StatusAmber, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Screen Brightness",
            subtitle = if (isAuto) "Adaptive mode active • $brightnessPercent%" else "Manual • $brightnessPercent%",
            icon = Icons.Filled.Brightness6,
            tint = StatusAmber,
            trailing = {
                StatusPill(
                    text = if (canWriteSettings) "Writable" else "Permission Needed",
                    color = if (canWriteSettings) StatusEmerald else StatusAmber
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onAdjustBrightness(false) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("brightness_down_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.BrightnessLow,
                    contentDescription = "Decrease Brightness",
                    tint = TextSecondary
                )
            }

            Slider(
                value = sliderVal,
                onValueChange = { sliderVal = it },
                onValueChangeFinished = { onSetBrightness(sliderVal.toInt()) },
                valueRange = 1f..100f,
                modifier = Modifier
                    .weight(1f)
                    .testTag("brightness_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = StatusAmber,
                    activeTrackColor = StatusAmber,
                    inactiveTrackColor = GlassBase
                )
            )

            IconButton(
                onClick = { onAdjustBrightness(true) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("brightness_up_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.BrightnessHigh,
                    contentDescription = "Increase Brightness",
                    tint = StatusAmber
                )
            }
        }

        if (!canWriteSettings) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onGrantPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusAmber.copy(alpha = 0.2f),
                    contentColor = StatusAmber
                ),
                border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.45f))
            ) {
                Text("Grant Modify System Settings")
            }
        }
    }
}

@Composable
private fun VolumeControlCard(
    selectedStream: AudioStreamType,
    currentLevel: Int,
    maxLevel: Int,
    isMuted: Boolean,
    onSelectStream: (AudioStreamType) -> Unit,
    onAdjustVolume: (Boolean) -> Unit,
    onSetVolume: (Int) -> Unit,
    onToggleMute: (Boolean) -> Unit
) {
    var sliderVal by remember(selectedStream, currentLevel) {
        mutableFloatStateOf(currentLevel.toFloat())
    }

    GlassCard(glowColor = NeuralViolet, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Audio & Volume Streams",
            subtitle = "${selectedStream.label} (${selectedStream.labelBn}) • $currentLevel / $maxLevel",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            tint = NeuralViolet,
            trailing = {
                FilledTonalButton(
                    onClick = { onToggleMute(!isMuted) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("volume_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isMuted) "Unmute" else "Mute")
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioStreamType.entries.forEach { stream ->
                val active = stream == selectedStream
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) NeuralViolet.copy(alpha = 0.22f) else GlassBase,
                    border = BorderStroke(
                        1.dp,
                        if (active) NeuralViolet else GlassBorder.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.clickable { onSelectStream(stream) }
                ) {
                    Text(
                        text = stream.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) NeuralViolet else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onAdjustVolume(false) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("volume_down_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = "Decrease volume",
                    tint = TextSecondary
                )
            }

            Slider(
                value = sliderVal,
                onValueChange = { sliderVal = it },
                onValueChangeFinished = { onSetVolume(sliderVal.toInt()) },
                valueRange = 0f..maxLevel.coerceAtLeast(1).toFloat(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("volume_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = NeuralViolet,
                    activeTrackColor = NeuralViolet,
                    inactiveTrackColor = GlassBase
                )
            )

            IconButton(
                onClick = { onAdjustVolume(true) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("volume_up_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Increase volume",
                    tint = NeuralViolet
                )
            }
        }
    }
}

@Composable
private fun QuickSettingsGridCard(
    isAccessibilityEnabled: Boolean,
    onOpenQuickSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenInternet: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenVolumePanel: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    GlassCard(glowColor = NeonCyan, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Quick Settings & System Panels",
            subtitle = if (isAccessibilityEnabled) {
                "Direct system shade & Android settings shortcuts"
            } else {
                "Settings shortcuts active • Enable Accessibility for shade"
            },
            icon = Icons.Filled.Tune,
            tint = NeonCyan
        )

        Spacer(modifier = Modifier.height(12.dp))

        val tiles = listOf(
            Triple("Quick Settings", Icons.Filled.Tune, onOpenQuickSettings),
            Triple("Notifications", Icons.Filled.Notifications, onOpenNotifications),
            Triple("Wi-Fi", Icons.Filled.Wifi, onOpenWifi),
            Triple("Internet", Icons.Filled.Public, onOpenInternet),
            Triple("Bluetooth", Icons.Filled.Bluetooth, onOpenBluetooth),
            Triple("Sound Panel", Icons.AutoMirrored.Filled.VolumeUp, onOpenVolumePanel),
            Triple("Display", Icons.Filled.Brightness6, onOpenDisplay),
            Triple("Battery", Icons.Filled.BatteryChargingFull, onOpenBattery),
            Triple("All Settings", Icons.Filled.Settings, onOpenSystemSettings)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (label, icon, action) ->
                        QuickSettingTile(
                            label = label,
                            icon = icon,
                            onClick = action,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSettingTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = GlassBase.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeonCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppLauncherCard(
    apps: List<InstalledAppInfo>,
    totalCount: Int,
    loading: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (InstalledAppInfo) -> Unit,
    onRefresh: () -> Unit
) {
    GlassCard(glowColor = MayaRose, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Installed App Launcher",
            subtitle = "$totalCount launchable apps detected",
            icon = Icons.Filled.Apps,
            tint = MayaRose,
            trailing = {
                Text(
                    text = "Refresh",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan,
                    modifier = Modifier
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_input"),
            placeholder = {
                Text(
                    text = "Search installed apps (e.g. YouTube, Chrome)…",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = TextSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MayaRose,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassBase,
                unfocusedContainerColor = GlassBase,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MayaRose)
            }
        } else if (apps.isEmpty()) {
            Text(
                text = "No matching launchable applications found.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps.take(40), key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier
                            .width(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onLaunchApp(app) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = "Launch ${app.label}",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MayaRose.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Apps,
                                    contentDescription = app.label,
                                    tint = MayaRose
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
