package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DeviceTelemetrySnapshot
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.theme.GlassBase
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MayaRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DeviceDashboardScreen(
    telemetry: DeviceTelemetrySnapshot,
    onRefresh: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onRefresh() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Battery Hero Card
        item(key = "battery_card") {
            BatteryTelemetryCard(
                telemetry = telemetry,
                onRefresh = onRefresh
            )
        }

        // 2. Storage & RAM Diagnostics
        item(key = "storage_ram_card") {
            StorageAndRamCard(telemetry = telemetry)
        }

        // 3. Network & Connectivity Card
        item(key = "network_card") {
            NetworkTelemetryCard(
                telemetry = telemetry,
                onOpenWifiSettings = onOpenWifiSettings
            )
        }

        // 4. Device Hardware & OS Info
        item(key = "device_specs_card") {
            DeviceSpecsCard(telemetry = telemetry)
        }

        // 5. Permission & Capability Status Center
        item(key = "permissions_card") {
            PermissionsAuditCard(
                telemetry = telemetry,
                onGrantAccessibility = onOpenAccessibilitySettings,
                onGrantWriteSettings = onOpenWriteSettings,
                onRequestAudio = {
                    audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    }
}

@Composable
private fun BatteryTelemetryCard(
    telemetry: DeviceTelemetrySnapshot,
    onRefresh: () -> Unit
) {
    val pct = telemetry.batteryPercent.coerceIn(0, 100)
    val gaugeColor = when {
        telemetry.isCharging -> StatusEmerald
        pct <= 20 -> StatusCrimson
        pct <= 45 -> StatusAmber
        else -> NeonCyan
    }
    val sweep by animateFloatAsState(
        targetValue = (pct / 100f) * 360f,
        animationSpec = tween(700),
        label = "battery_sweep"
    )

    GlassCard(glowColor = gaugeColor, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Battery Diagnostics",
            subtitle = "${telemetry.batteryStatusText} • ${telemetry.chargingSourceText}",
            icon = if (telemetry.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
            tint = gaugeColor,
            trailing = {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh device telemetry",
                        tint = NeonCyan
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp)
            ) {
                Canvas(modifier = Modifier.size(88.dp)) {
                    drawArc(
                        color = GlassBase,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = gaugeColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (telemetry.isCharging) "CHG" else "BAT",
                        style = MaterialTheme.typography.labelSmall,
                        color = gaugeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetricRow(
                    label = "Temperature",
                    value = telemetry.batteryTemperatureC?.let {
                        String.format(Locale.US, "%.1f °C", it)
                    } ?: "Unavailable"
                )
                TelemetryMetricRow(
                    label = "Health",
                    value = telemetry.batteryHealthText
                )
                TelemetryMetricRow(
                    label = "Voltage",
                    value = if (telemetry.batteryVoltageMv > 0) "${telemetry.batteryVoltageMv} mV" else "N/A"
                )
            }
        }
    }
}

@Composable
private fun StorageAndRamCard(telemetry: DeviceTelemetrySnapshot) {
    val storageFraction = if (telemetry.totalStorageBytes > 0L) {
        (telemetry.usedStorageBytes.toFloat() / telemetry.totalStorageBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val ramFraction = if (telemetry.totalRamBytes > 0L) {
        (telemetry.usedRamBytes.toFloat() / telemetry.totalRamBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    GlassCard(glowColor = NeuralViolet, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Storage & Memory",
            subtitle = "Real-time internal flash & system RAM",
            icon = Icons.Filled.Storage,
            tint = NeuralViolet
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Internal Storage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Internal Storage",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = "${formatBytes(telemetry.usedStorageBytes)} / ${formatBytes(telemetry.totalStorageBytes)}",
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { storageFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = NeonCyan,
            trackColor = GlassBase
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Available: ${formatBytes(telemetry.availableStorageBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // RAM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = null,
                    tint = MayaRose,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "System RAM",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
            }
            Text(
                text = "${formatBytes(telemetry.usedRamBytes)} / ${formatBytes(telemetry.totalRamBytes)}",
                style = MaterialTheme.typography.labelMedium,
                color = MayaRose
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ramFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MayaRose,
            trackColor = GlassBase
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Available RAM: ${formatBytes(telemetry.availableRamBytes)}" +
                if (telemetry.isLowRam) " • Low Memory Pressure" else "",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun NetworkTelemetryCard(
    telemetry: DeviceTelemetrySnapshot,
    onOpenWifiSettings: () -> Unit
) {
    val accent = if (telemetry.isInternetValidated) StatusEmerald else StatusAmber
    GlassCard(glowColor = accent, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Network & Connectivity",
            subtitle = telemetry.networkTypeText,
            icon = if (telemetry.isWifiConnected) Icons.Filled.Wifi else Icons.Filled.NetworkCell,
            tint = accent,
            trailing = {
                StatusPill(
                    text = if (telemetry.isInternetValidated) "Online" else "No Internet",
                    color = accent
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryMetricRow(
                label = "Wi-Fi Status",
                value = if (telemetry.isWifiConnected) {
                    buildString {
                        append("Connected")
                        telemetry.wifiSsid?.let { append(" ($it)") }
                        telemetry.wifiLinkSpeedMbps?.let { append(" • ${it} Mbps") }
                    }
                } else "Disconnected"
            )
            TelemetryMetricRow(
                label = "Mobile Network",
                value = if (telemetry.isMobileConnected) "Active" else "Inactive"
            )
            TelemetryMetricRow(
                label = "Active Transport",
                value = telemetry.networkTypeText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = GlassBase,
            border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenWifiSettings)
        ) {
            Text(
                text = "Open Wi-Fi & Network Settings",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun DeviceSpecsCard(telemetry: DeviceTelemetrySnapshot) {
    GlassCard(glowColor = NeonCyan, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Device Identity & OS",
            subtitle = "${telemetry.deviceBrand} ${telemetry.deviceModel}",
            icon = Icons.Filled.PhoneAndroid,
            tint = NeonCyan
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryMetricRow("Brand", telemetry.deviceBrand)
            TelemetryMetricRow("Model", telemetry.deviceModel)
            TelemetryMetricRow("Manufacturer", telemetry.deviceManufacturer)
            TelemetryMetricRow("Android Version", "Android ${telemetry.androidVersion} (API ${telemetry.sdkInt})")
            if (telemetry.securityPatch.isNotBlank()) {
                TelemetryMetricRow("Security Patch", telemetry.securityPatch)
            }
        }
    }
}

@Composable
private fun PermissionsAuditCard(
    telemetry: DeviceTelemetrySnapshot,
    onGrantAccessibility: () -> Unit,
    onGrantWriteSettings: () -> Unit,
    onRequestAudio: () -> Unit
) {
    GlassCard(glowColor = StatusAmber, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Permissions & Hardware Status",
            subtitle = "Real-time capability audit",
            icon = Icons.Filled.Security,
            tint = StatusAmber
        )

        Spacer(modifier = Modifier.height(10.dp))

        PermissionStatusRow(
            name = "Accessibility Service",
            granted = telemetry.isAccessibilityEnabled,
            actionLabel = "Enable",
            onAction = onGrantAccessibility
        )
        PermissionStatusRow(
            name = "Modify System Settings",
            granted = telemetry.canWriteSettings,
            actionLabel = "Grant",
            onAction = onGrantWriteSettings
        )
        PermissionStatusRow(
            name = "Microphone (Voice Input)",
            granted = telemetry.hasRecordAudioPermission,
            actionLabel = "Allow",
            onAction = onRequestAudio
        )
        PermissionStatusRow(
            name = "Camera Flash Hardware",
            granted = telemetry.flashlightAvailable,
            actionLabel = null,
            onAction = {}
        )
    }
}

@Composable
private fun TelemetryMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun PermissionStatusRow(
    name: String,
    granted: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(
                text = if (granted) "Granted" else "Required",
                color = if (granted) StatusEmerald else StatusAmber
            )
            if (!granted && actionLabel != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = NeonCyan)
                }
            }
        }
    }
}
