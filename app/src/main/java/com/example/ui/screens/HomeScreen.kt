package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.StopScreenShare
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.GeminiConnectionStatus
import com.example.service.BackgroundModeState
import com.example.ui.components.GlassCard
import com.example.ui.components.MayaCharacter
import com.example.ui.components.StatusDotPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CosmicDeep
import com.example.ui.theme.CosmicGlassBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceElevated
import com.example.ui.theme.CosmicVoid
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AssistantMode
import com.example.viewmodel.AutomationState

@Composable
fun HomeScreen(
    assistantMode: AssistantMode,
    statusHeadline: String,
    lastMayaReply: String,
    speechAmplitude: Float,
    speechRmsLevel: Float,
    geminiStatus: GeminiConnectionStatus,
    backgroundState: BackgroundModeState,
    isScreenSharing: Boolean,
    automationState: AutomationState,
    onVoiceClick: () -> Unit,
    onSendCommand: (String) -> Unit,
    onInterrupt: () -> Unit,
    onSpeakLastReply: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onAskAboutScreen: () -> Unit,
    onOpenAppLauncher: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenDevice: () -> Unit,
    onToggleBackgroundAssistant: () -> Unit,
    onStopAutomation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    val statusDotColor = when (assistantMode) {
        AssistantMode.LISTENING -> NeonPink
        AssistantMode.THINKING -> NeonCyan
        AssistantMode.SPEAKING -> NeonPurple
        AssistantMode.ERROR -> CoralError
        AssistantMode.IDLE -> when (geminiStatus) {
            GeminiConnectionStatus.CONNECTED, GeminiConnectionStatus.READY -> EmeraldActive
            GeminiConnectionStatus.CONNECTING -> AmberWarning
            GeminiConnectionStatus.ERROR -> CoralError
        }
    }

    val statusText = when (assistantMode) {
        AssistantMode.LISTENING -> "শুনছি…"
        AssistantMode.THINKING -> "ভাবছি…"
        AssistantMode.SPEAKING -> "কথা বলছি…"
        AssistantMode.ERROR -> "সমস্যা হয়েছে"
        AssistantMode.IDLE -> if (backgroundState == BackgroundModeState.ACTIVE_LISTENING) "ব্যাকগ্রাউন্ড চালু" else "মায়া প্রস্তুত"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CosmicVoid, CosmicDeep, Color(0xFF090D1D))
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
        ) {
            // Minimal Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MAYA",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    StatusDotPill(
                        label = statusText,
                        dotColor = statusDotColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Screen Share Button
                    IconButton(
                        onClick = onToggleScreenShare,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(
                                if (isScreenSharing) CoralError.copy(alpha = 0.22f)
                                else CosmicSurfaceElevated.copy(alpha = 0.8f)
                            )
                            .border(
                                1.dp,
                                if (isScreenSharing) CoralError else CosmicGlassBorder,
                                CircleShape
                            )
                            .testTag("home_screen_share_btn")
                    ) {
                        Icon(
                            imageVector = if (isScreenSharing) Icons.Rounded.StopScreenShare else Icons.Rounded.ScreenShare,
                            contentDescription = if (isScreenSharing) "স্ক্রিন শেয়ার বন্ধ করো" else "স্ক্রিন শেয়ার",
                            tint = if (isScreenSharing) CoralError else NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Three-Dot Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(CosmicSurfaceElevated.copy(alpha = 0.8f))
                                .border(1.dp, CosmicGlassBorder, CircleShape)
                                .testTag("home_more_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "আরও অপশন",
                                tint = TextPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = CosmicSurfaceElevated
                        ) {
                            DropdownMenuItem(
                                text = { Text("ইনস্টল করা অ্যাপ লঞ্চার", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Apps, null, tint = NeonCyan) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenAppLauncher()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (backgroundState == BackgroundModeState.STOPPED)
                                            "ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট চালু করো"
                                        else "ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট বন্ধ করো",
                                        color = TextPrimary
                                    )
                                },
                                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, tint = NeonPurple) },
                                onClick = {
                                    menuExpanded = false
                                    onToggleBackgroundAssistant()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("পারমিশন ও কমান্ড শর্টকাট", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.Security, null, tint = EmeraldActive) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenTools()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ডিভাইস ও PC কানেক্ট", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Rounded.ScreenShare, null, tint = NeonCyan) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenDevice()
                                }
                            )
                        }
                    }
                }
            }

            // Live Screen Share Banner (transparent visual indicator)
            AnimatedVisibility(visible = isScreenSharing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CoralError.copy(alpha = 0.16f))
                        .border(1.dp, CoralError.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "স্ক্রিন শেয়ার চালু আছে",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Row {
                        TextButton(onClick = onAskAboutScreen) {
                            Text("স্ক্রিন দেখো", color = NeonCyan)
                        }
                        TextButton(onClick = onToggleScreenShare) {
                            Text("বন্ধ করো", color = CoralError)
                        }
                    }
                }
            }

            // Active Automation Banner
            AnimatedVisibility(visible = automationState.isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NeonIndigo.copy(alpha = 0.22f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = automationState.statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = onStopAutomation,
                        modifier = Modifier.testTag("home_stop_automation_btn")
                    ) {
                        Text("থামাও", color = CoralError)
                    }
                }
            }

            // Center Animated Maya Character & Minimal Subtitle Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                MayaCharacter(
                    mode = assistantMode,
                    speechAmplitude = speechAmplitude,
                    listeningRms = speechRmsLevel,
                    size = 225.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = statusHeadline,
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Minimal Glass Reply Card
                GlassCard(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastMayaReply,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (assistantMode == AssistantMode.SPEAKING || assistantMode == AssistantMode.THINKING) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CoralError.copy(alpha = 0.2f))
                                    .clickable(onClick = onInterrupt)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("home_interrupt_btn")
                            ) {
                                Icon(Icons.Rounded.Stop, null, tint = CoralError, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("থামো", style = MaterialTheme.typography.labelSmall, color = CoralError)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NeonIndigo.copy(alpha = 0.22f))
                                    .clickable(onClick = onSpeakLastReply)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("home_speak_reply_btn")
                            ) {
                                Icon(Icons.Rounded.VolumeUp, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("শোনো", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                            }
                        }
                    }
                }
            }

            // Minimal Text & Voice Command Dock (above Bottom Bar)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    placeholder = {
                        Text("মায়াকে কিছু বলো বা লিখো…", color = TextSecondary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commandText.isNotBlank()) {
                                onSendCommand(commandText)
                                commandText = ""
                            }
                        }
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onVoiceClick,
                                modifier = Modifier.testTag("home_inline_voice_btn")
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = "ভয়েস ইনপুট", tint = NeonPink)
                            }
                            if (commandText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onSendCommand(commandText)
                                        commandText = ""
                                    },
                                    modifier = Modifier.testTag("home_send_command_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "পাঠাও", tint = NeonCyan)
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = CosmicGlassBorder,
                        focusedContainerColor = CosmicSurface.copy(alpha = 0.9f),
                        unfocusedContainerColor = CosmicSurface.copy(alpha = 0.85f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_command_input")
                )
            }
        }
    }
}
