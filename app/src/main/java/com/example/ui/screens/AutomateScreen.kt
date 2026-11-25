package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.AutomationFeatureFlags
import com.example.domain.AutomationSequence
import com.example.domain.AutomationStep
import com.example.domain.AutomationStepType
import com.example.domain.DeviceTelemetrySnapshot
import com.example.ui.AutomationExecutionState
import com.example.ui.components.EmergencyStopButton
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.GlassBase
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassElevated
import com.example.ui.theme.MayaRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AutomateScreen(
    telemetry: DeviceTelemetrySnapshot,
    featureFlags: AutomationFeatureFlags,
    automationState: AutomationExecutionState,
    sequences: List<AutomationSequence>,
    onOpenAccessibilitySettings: () -> Unit,
    onEmergencyStop: () -> Unit,
    onUpdateFlag: ((AutomationFeatureFlags) -> AutomationFeatureFlags) -> Unit,
    onPerformHome: () -> Unit,
    onPerformRecents: () -> Unit,
    onPerformBack: () -> Unit,
    onOpenQuickSettings: () -> Unit,
    onScroll: (Boolean) -> Unit,
    onSwipe: (Boolean) -> Unit,
    onTap: (Float, Float) -> Unit,
    onLongPress: (Float, Float) -> Unit,
    onRunSequence: (AutomationSequence) -> Unit,
    onSaveSequence: (AutomationSequence) -> Unit,
    onDeleteSequence: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var tapXText by rememberSaveable { mutableStateOf("540") }
    var tapYText by rememberSaveable { mutableStateOf("960") }
    var showNewSequenceDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Accessibility Permission & Emergency Stop Hero Card
        item(key = "acc_and_stop") {
            GlassCard(
                glowColor = if (automationState.isRunning) StatusCrimson else NeonCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeader(
                    title = "Accessibility & Safety Control",
                    subtitle = if (telemetry.isAccessibilityEnabled) {
                        "Maya Smart Control Service is active"
                    } else {
                        "User authorization required for gestures & navigation"
                    },
                    icon = Icons.Filled.AccessibilityNew,
                    tint = if (telemetry.isAccessibilityEnabled) StatusEmerald else StatusAmber,
                    trailing = {
                        StatusPill(
                            text = if (telemetry.isAccessibilityEnabled) "Active" else "Disabled",
                            color = if (telemetry.isAccessibilityEnabled) StatusEmerald else StatusAmber
                        )
                    }
                )

                if (!telemetry.isAccessibilityEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enable_accessibility_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusAmber.copy(alpha = 0.22f),
                            contentColor = StatusAmber
                        ),
                        border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.5f))
                    ) {
                        Text("Open Accessibility Settings")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (automationState.isRunning) {
                    Text(
                        text = "Running: ${automationState.sequenceName} (${automationState.currentStepIndex}/${automationState.totalSteps}) • ${automationState.currentStepLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (automationState.totalSteps > 0) {
                                automationState.currentStepIndex.toFloat() / automationState.totalSteps.toFloat()
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = NeonCyan,
                        trackColor = GlassBase
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                EmergencyStopButton(
                    isActive = automationState.isRunning,
                    onStopClick = onEmergencyStop,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Screen Navigation & Gesture Pad
        item(key = "gesture_pad") {
            GlassCard(glowColor = NeuralViolet, modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Screen & Navigation Control",
                    subtitle = "Global navigation, scroll, swipe & coordinate touch",
                    icon = Icons.Filled.TouchApp,
                    tint = NeuralViolet
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionTileButton(
                        label = "Back",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onPerformBack,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTileButton(
                        label = "Home",
                        icon = Icons.Filled.Home,
                        onClick = onPerformHome,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTileButton(
                        label = "Recents",
                        icon = Icons.Filled.Layers,
                        onClick = onPerformRecents,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTileButton(
                        label = "Quick",
                        icon = Icons.Filled.Tune,
                        onClick = onOpenQuickSettings,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionTileButton(
                        label = "Scroll Up",
                        icon = Icons.Filled.KeyboardArrowUp,
                        onClick = { onScroll(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionTileButton(
                        label = "Scroll Down",
                        icon = Icons.Filled.KeyboardArrowDown,
                        onClick = { onScroll(true) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionTileButton(
                        label = "Swipe Left",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        onClick = { onSwipe(true) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionTileButton(
                        label = "Swipe Right",
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        onClick = { onSwipe(false) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tapXText,
                        onValueChange = { tapXText = it.filter(Char::isDigit) },
                        label = { Text("X (px)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tapYText,
                        onValueChange = { tapYText = it.filter(Char::isDigit) },
                        label = { Text("Y (px)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onTap(
                                tapXText.toFloatOrNull() ?: 540f,
                                tapYText.toFloatOrNull() ?: 960f
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_tap_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Tap (X, Y)")
                    }
                    OutlinedButton(
                        onClick = {
                            onLongPress(
                                tapXText.toFloatOrNull() ?: 540f,
                                tapYText.toFloatOrNull() ?: 960f
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_long_press_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Long Press")
                    }
                }
            }
        }

        // 3. Individual Automation Feature Toggles
        item(key = "feature_toggles") {
            GlassCard(glowColor = StatusEmerald, modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Automation Feature Toggles",
                    subtitle = "Enable or disable individual capabilities",
                    icon = Icons.Filled.Tune,
                    tint = StatusEmerald
                )
                Spacer(modifier = Modifier.height(8.dp))

                FeatureToggleRow(
                    title = "Tap & Long Press Gestures",
                    checked = featureFlags.tapAndLongPressEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateFlag { it.copy(tapAndLongPressEnabled = enabled) }
                    }
                )
                FeatureToggleRow(
                    title = "Auto Scroll (Up / Down)",
                    checked = featureFlags.autoScrollEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateFlag { it.copy(autoScrollEnabled = enabled) }
                    }
                )
                FeatureToggleRow(
                    title = "Auto Swipe (Left / Right)",
                    checked = featureFlags.autoSwipeEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateFlag { it.copy(autoSwipeEnabled = enabled) }
                    }
                )
                FeatureToggleRow(
                    title = "Voice & AI App Launching",
                    checked = featureFlags.appLaunchEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateFlag { it.copy(appLaunchEnabled = enabled) }
                    }
                )
                FeatureToggleRow(
                    title = "Sequence Execution Engine",
                    checked = featureFlags.sequenceExecutionEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateFlag { it.copy(sequenceExecutionEnabled = enabled) }
                    }
                )
            }
        }

        // 4. Configurable Automation Sequences Header
        item(key = "sequences_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Automation Sequences",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                FilledTonalButton(
                    onClick = { showNewSequenceDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("new_sequence_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Sequence")
                }
            }
        }

        items(sequences, key = { it.id }) { sequence ->
            GlassCard(glowColor = MayaRose, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sequence.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = sequence.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onRunSequence(sequence) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Run ${sequence.name}",
                                tint = StatusEmerald
                            )
                        }
                        IconButton(
                            onClick = { onDeleteSequence(sequence.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete ${sequence.name}",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sequence.steps.forEachIndexed { idx, step ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GlassBase,
                            border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.5f))
                        ) {
                            val label = if (step.param1.isNotBlank()) {
                                "${idx + 1}. ${step.type.label} (${step.param1})"
                            } else {
                                "${idx + 1}. ${step.type.label}"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewSequenceDialog) {
        NewSequenceBuilderDialog(
            onDismiss = { showNewSequenceDialog = false },
            onSave = { seq ->
                onSaveSequence(seq)
                showNewSequenceDialog = false
            }
        )
    }
}

@Composable
private fun ActionTileButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = GlassBase.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeuralViolet,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianDeep,
                checkedTrackColor = StatusEmerald,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = GlassBase
            )
        )
    }
}

@Composable
private fun NewSequenceBuilderDialog(
    onDismiss: () -> Unit,
    onSave: (AutomationSequence) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val steps = remember { mutableStateListOf<AutomationStep>() }
    var selectedType by rememberSaveable { mutableStateOf(AutomationStepType.SCROLL_DOWN) }
    var param1 by rememberSaveable { mutableStateOf("") }
    var param2 by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassElevated,
        title = {
            Text(
                text = "Create Automation Sequence",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Sequence Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Add Step",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AutomationStepType.entries.forEach { type ->
                        val active = type == selectedType
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) NeonCyan.copy(alpha = 0.2f) else GlassBase,
                            border = BorderStroke(
                                1.dp,
                                if (active) NeonCyan else GlassBorder
                            ),
                            modifier = Modifier.clickable {
                                selectedType = type
                                param1 = when (type) {
                                    AutomationStepType.TAP, AutomationStepType.LONG_PRESS -> "540"
                                    AutomationStepType.WAIT -> "1200"
                                    AutomationStepType.OPEN_APP -> "YouTube"
                                    else -> ""
                                }
                                param2 = when (type) {
                                    AutomationStepType.TAP, AutomationStepType.LONG_PRESS -> "960"
                                    else -> ""
                                }
                            }
                        ) {
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) NeonCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (selectedType == AutomationStepType.TAP ||
                    selectedType == AutomationStepType.LONG_PRESS
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = param1,
                            onValueChange = { param1 = it.filter(Char::isDigit) },
                            label = { Text("X") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = param2,
                            onValueChange = { param2 = it.filter(Char::isDigit) },
                            label = { Text("Y") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else if (selectedType == AutomationStepType.OPEN_APP ||
                    selectedType == AutomationStepType.WAIT
                ) {
                    OutlinedTextField(
                        value = param1,
                        onValueChange = { param1 = it },
                        label = {
                            Text(
                                if (selectedType == AutomationStepType.OPEN_APP) "App Name"
                                else "Duration (ms)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                FilledTonalButton(
                    onClick = {
                        steps.add(AutomationStep(selectedType, param1, param2))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Step (${steps.size} added)")
                }

                if (steps.isNotEmpty()) {
                    Text(
                        text = steps.mapIndexed { i, s -> "${i + 1}. ${s.type.label}" }.joinToString(" → "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = name.trim().ifEmpty { "Custom Routine" }
                    val finalDesc = description.trim().ifEmpty { "${steps.size} automated steps" }
                    onSave(AutomationSequence(name = finalName, description = finalDesc, steps = steps.toList()))
                },
                enabled = steps.isNotEmpty()
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
