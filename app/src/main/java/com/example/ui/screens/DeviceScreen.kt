package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenSearchDesktop
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsAccessibility
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.StopScreenShare
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.connect.PcConnectionState
import com.example.device.DeviceTelemetry
import com.example.device.InstalledApp
import com.example.ui.components.AppIconImage
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusDotPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CosmicDeep
import com.example.ui.theme.CosmicGlassBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicVoid
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DeviceScreen(
    telemetry: DeviceTelemetry,
    installedApps: List<InstalledApp>,
    pcState: PcConnectionState,
    isScreenSharing: Boolean,
    screenFrameBytes: ByteArray?,
    listState: LazyListState = rememberLazyListState(),
    onRefresh: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: (Boolean) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onRequestWriteSettings: () -> Unit,
    onOpenSettingsShortcut: (String) -> Unit,
    onOpenAppLauncher: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onStartPcServer: () -> Unit,
    onStopPcServer: () -> Unit,
    onDisconnectPc: () -> Unit,
    onShareFileUri: (android.net.Uri) -> Unit,
    onRemoveSharedFile: (String) -> Unit,
    onStartScreenShare: () -> Unit,
    onStopScreenShare: () -> Unit,
    onAskMayaAboutScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onShareFileUri(uri)
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onShareFileUri(uri)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CosmicVoid, CosmicDeep)))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "ডিভাইস ও কানেক্ট",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("device_refresh_btn")) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "রিফ্রেশ", tint = NeonCyan)
                }
            }

            // Section Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                listOf("কন্ট্রোল ও অ্যাপ", "ফোনের তথ্য", "PC ও স্ক্রিন শেয়ার").forEachIndexed { idx, title ->
                    FilterChip(
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        label = { Text(title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonIndigo.copy(alpha = 0.35f),
                            selectedLabelColor = NeonCyan,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("device_subtab_$idx")
                    )
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedSection) {
                    0 -> {
                        // Quick Controls: Flashlight, Volume, Brightness
                        item {
                            DeviceControlsCard(
                                telemetry = telemetry,
                                onToggleFlashlight = onToggleFlashlight,
                                onVolumeChange = onVolumeChange,
                                onToggleMute = onToggleMute,
                                onBrightnessChange = onBrightnessChange,
                                onRequestWriteSettings = onRequestWriteSettings
                            )
                        }
                        // Universal App Launcher Preview
                        item {
                            InstalledAppsCard(
                                apps = installedApps,
                                onOpenAll = onOpenAppLauncher,
                                onLaunchApp = onLaunchApp
                            )
                        }
                        // System Settings Shortcuts
                        item {
                            SystemSettingsShortcutsCard(onOpenShortcut = onOpenSettingsShortcut)
                        }
                    }

                    1 -> {
                        item { BatteryAndStorageCard(telemetry) }
                        item { NetworkAndHardwareCard(telemetry) }
                    }

                    2 -> {
                        item {
                            ScreenShareCard(
                                isSharing = isScreenSharing,
                                frameBytes = screenFrameBytes,
                                onStart = onStartScreenShare,
                                onStop = onStopScreenShare,
                                onAskMayaAboutScreen = onAskMayaAboutScreen
                            )
                        }
                        item {
                            PcConnectCard(
                                pcState = pcState,
                                onStartServer = onStartPcServer,
                                onStopServer = onStopPcServer,
                                onDisconnect = onDisconnectPc,
                                onPickPhoto = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onPickDocument = {
                                    documentPickerLauncher.launch(arrayOf("*/*"))
                                },
                                onRemoveFile = onRemoveSharedFile
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceControlsCard(
    telemetry: DeviceTelemetry,
    onToggleFlashlight: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onToggleMute: (Boolean) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onRequestWriteSettings: () -> Unit
) {
    GlassCard {
        Text("স্মার্ট ফোন কন্ট্রোল", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        // Flashlight Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (telemetry.isFlashlightOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                    contentDescription = "ফ্ল্যাশলাইট",
                    tint = if (telemetry.isFlashlightOn) AmberWarning else TextSecondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("ফ্ল্যাশলাইট / টর্চ", color = TextPrimary)
                    Text(
                        text = if (!telemetry.hasFlashlight) "সাপোর্ট নেই"
                        else if (telemetry.isFlashlightOn) "চালু আছে" else "বন্ধ আছে",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Switch(
                checked = telemetry.isFlashlightOn,
                enabled = telemetry.hasFlashlight,
                onCheckedChange = { onToggleFlashlight() },
                modifier = Modifier.testTag("device_flashlight_switch")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Volume Control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (telemetry.isMediaMuted) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                    contentDescription = "ভলিউম",
                    tint = NeonCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("মিডিয়া ভলিউম (${telemetry.mediaVolumePercent}%)", color = TextPrimary)
            }
            TextButton(onClick = { onToggleMute(!telemetry.isMediaMuted) }) {
                Text(if (telemetry.isMediaMuted) "Unmute" else "Mute", color = NeonPink)
            }
        }
        Slider(
            value = telemetry.mediaVolumePercent / 100f,
            onValueChange = { onVolumeChange((it * 100).toInt()) },
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonIndigo),
            modifier = Modifier.testTag("device_volume_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Brightness Control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Brightness6, contentDescription = "ব্রাইটনেস", tint = AmberWarning)
                Spacer(modifier = Modifier.width(10.dp))
                Text("ব্রাইটনেস (${telemetry.brightnessPercent}%)", color = TextPrimary)
            }
            if (!telemetry.canWriteSystemSettings) {
                TextButton(onClick = onRequestWriteSettings) {
                    Text("অনুমতি দাও", color = AmberWarning)
                }
            }
        }
        Slider(
            value = telemetry.brightnessPercent / 100f,
            onValueChange = { onBrightnessChange((it * 100).toInt()) },
            colors = SliderDefaults.colors(thumbColor = AmberWarning, activeTrackColor = NeonPurple),
            modifier = Modifier.testTag("device_brightness_slider")
        )
    }
}

@Composable
private fun InstalledAppsCard(
    apps: List<InstalledApp>,
    onOpenAll: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit
) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Apps, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("অ্যাপ লঞ্চার (${apps.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            TextButton(onClick = onOpenAll, modifier = Modifier.testTag("device_all_apps_btn")) {
                Text("সব দেখো", color = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (apps.isEmpty()) {
            Text("ইনস্টল করা অ্যাপ খোঁজা হচ্ছে…", color = TextSecondary)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(apps.take(14), key = { it.packageName }) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onLaunchApp(app) }
                            .padding(4.dp)
                    ) {
                        AppIconImage(app.icon, app.label, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemSettingsShortcutsCard(onOpenShortcut: (String) -> Unit) {
    val shortcuts = listOf(
        Triple("wifi", "Wi-Fi", Icons.Rounded.Wifi),
        Triple("internet", "ইন্টারনেট", Icons.Rounded.Wifi),
        Triple("bluetooth", "Bluetooth", Icons.Rounded.Bluetooth),
        Triple("display", "Display", Icons.Rounded.Brightness6),
        Triple("sound", "Sound", Icons.Rounded.VolumeUp),
        Triple("battery", "Battery", Icons.Rounded.BatteryChargingFull),
        Triple("apps", "Apps", Icons.Rounded.Apps),
        Triple("location", "Location", Icons.Rounded.LocationOn),
        Triple("notifications", "Notifications", Icons.Rounded.Notifications),
        Triple("accessibility", "Accessibility", Icons.Rounded.SettingsAccessibility),
        Triple("system", "System Settings", Icons.Rounded.Settings)
    )

    GlassCard {
        Text("সিস্টেম সেটিংস শর্টকাট", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            shortcuts.forEach { (key, label, icon) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicGlassBorder, RoundedCornerShape(14.dp))
                        .clickable { onOpenShortcut(key) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("settings_shortcut_$key")
                ) {
                    Icon(icon, contentDescription = label, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun BatteryAndStorageCard(telemetry: DeviceTelemetry) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BatteryChargingFull, null, tint = EmeraldActive)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ব্যাটারি ড্যাশবোর্ড", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${telemetry.battery.levelPercent}% • ${if (telemetry.battery.isCharging) "চার্জহচ্ছে (${telemetry.battery.chargeSource})" else "ডিসচার্জিং"}",
                style = MaterialTheme.typography.titleLarge,
                color = if (telemetry.battery.levelPercent <= 20) CoralError else EmeraldActive
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { telemetry.battery.levelPercent / 100f },
                color = EmeraldActive,
                trackColor = CosmicSurface,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val temp = telemetry.battery.temperatureC?.let { "${String.format(Locale.US, "%.1f", it)}°C" } ?: "অজানা"
            Text(
                text = "স্বাস্থ্য: ${telemetry.battery.healthLabel} • তাপমাত্রা: $temp",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Storage, null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("স্টোরেজ", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "খালি ${String.format(Locale.US, "%.1f", telemetry.storage.freeGb)} GB / মোট ${String.format(Locale.US, "%.1f", telemetry.storage.totalGb)} GB",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { telemetry.storage.usedPercent / 100f },
                color = NeonPurple,
                trackColor = CosmicSurface,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ব্যবহৃত: ${String.format(Locale.US, "%.1f", telemetry.storage.usedGb)} GB (${telemetry.storage.usedPercent}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun NetworkAndHardwareCard(telemetry: DeviceTelemetry) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Wifi, null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("নেটওয়ার্ক স্ট্যাটাস", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                StatusDotPill(
                    label = if (telemetry.network.isConnected) "সংযুক্ত" else "অফলাইন",
                    dotColor = if (telemetry.network.isConnected) EmeraldActive else CoralError
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ধরন: ${telemetry.network.transportType} • ${if (telemetry.network.isMetered) "Metered" else "Unmetered"}",
                color = TextPrimary
            )
            if (telemetry.network.downstreamMbps > 0) {
                Text(
                    text = "আনুমানিক লিংক স্পিড: ${telemetry.network.downstreamMbps} Mbps",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PhoneAndroid, null, tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ডিভাইস ও র‍্যাম তথ্য", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("ব্র্যান্ড ও মডেল: ${telemetry.brand} ${telemetry.model}", color = TextPrimary)
            Text("Android ভার্সন: ${telemetry.androidVersion} (API ${telemetry.sdkInt})", color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Rounded.Memory, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RAM ফ্রি: ${String.format(Locale.US, "%.2f", telemetry.availableRamGb)} GB / ${String.format(Locale.US, "%.2f", telemetry.totalRamGb)} GB",
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ScreenShareCard(
    isSharing: Boolean,
    frameBytes: ByteArray?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onAskMayaAboutScreen: () -> Unit
) {
    GlassCard(borderColor = if (isSharing) CoralError.copy(alpha = 0.6f) else CosmicGlassBorder) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSharing) Icons.Rounded.StopScreenShare else Icons.Rounded.ScreenShare,
                    contentDescription = null,
                    tint = if (isSharing) CoralError else NeonCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("স্ক্রিন শেয়ার (MediaProjection)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = if (isSharing) "লাইভ শেয়ারিং চলছে" else "সিস্টেম অনুমতির পর চালু হবে",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSharing) CoralError else TextSecondary
                    )
                }
            }
            Button(
                onClick = if (isSharing) onStop else onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSharing) CoralError else NeonIndigo
                ),
                modifier = Modifier.testTag("device_screen_share_btn")
            ) {
                Text(if (isSharing) "বন্ধ করো" else "শুরু করো")
            }
        }

        if (isSharing) {
            Spacer(modifier = Modifier.height(10.dp))
            val previewBmp = remember(frameBytes) {
                frameBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
            }
            if (previewBmp != null) {
                Image(
                    bitmap = previewBmp,
                    contentDescription = "লাইভ স্ক্রিন প্রিভিউ",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, CosmicGlassBorder, RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onAskMayaAboutScreen,
                modifier = Modifier.fillMaxWidth().testTag("device_ask_screen_btn")
            ) {
                Icon(Icons.Rounded.ScreenSearchDesktop, null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("মায়াকে স্ক্রিন সম্পর্কে জিজ্ঞেস করো", color = TextPrimary)
            }
        }
    }
}

@Composable
private fun PcConnectCard(
    pcState: PcConnectionState,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onDisconnect: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onRemoveFile: (String) -> Unit
) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Computer, null, tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Phone ↔ PC কানেক্ট", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = if (pcState.isServerRunning) "Wi-Fi সার্ভার চালু আছে" else "একই Wi-Fi নেটওয়ার্কে পেয়ার করো",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Button(
                onClick = if (pcState.isServerRunning) onStopServer else onStartServer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pcState.isServerRunning) CoralError else NeonPurple
                ),
                modifier = Modifier.testTag("device_pc_server_btn")
            ) {
                Text(if (pcState.isServerRunning) "বন্ধ করো" else "সার্ভার চালু")
            }
        }

        if (!pcState.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(pcState.errorMessage, color = CoralError, style = MaterialTheme.typography.bodyMedium)
        }

        if (pcState.isServerRunning) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                pcState.qrBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PC Pairing QR Code",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(6.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("PC ব্রাউজারে এই ঠিকানা খোলো বা QR স্ক্যান করো:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(pcState.serverUrl, style = MaterialTheme.typography.bodyMedium, color = NeonCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("পেয়ারিং PIN: ${pcState.pairingPin}", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }

            if (pcState.connectedClientInfo != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldActive.copy(alpha = 0.15f))
                        .padding(10.dp)
                ) {
                    Text("সংযুক্ত PC: ${pcState.connectedClientInfo}", color = EmeraldActive, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Rounded.LinkOff, contentDescription = "সংযোগ বিচ্ছিন্ন", tint = CoralError)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("নিরাপদ ফাইল ট্রান্সফার (তোমার অনুমোদিত ফাইল)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickPhoto, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PhotoLibrary, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ছবি/ভিডিও", color = TextPrimary)
                }
                OutlinedButton(onClick = onPickDocument, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.AttachFile, null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ফাইল বাছো", color = TextPrimary)
                }
            }
            pcState.sharedFiles.forEach { file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Text(file.displayName, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemoveFile(file.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "মুছো", tint = CoralError, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
