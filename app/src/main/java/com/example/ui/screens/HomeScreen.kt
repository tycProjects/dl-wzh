package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.VoiceState
import com.example.ui.components.HudVisualizer
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNavyCard
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToCoding: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val voiceState by viewModel.speechAndTtsManager.voiceState.collectAsState()
    val recognizedText by viewModel.speechAndTtsManager.recognizedText.collectAsState()
    val voiceError by viewModel.speechAndTtsManager.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val attachedImage by viewModel.selectedImageBase64.collectAsState()

    var inputText by remember { mutableStateOf("") }

    // Audio Permission Launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImageUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberNavyDark,
                        Color(0xFF09101E),
                        CyberNavyDark
                    )
                )
            )
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // --- Top Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand & Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateToSettings() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_arafat_logo),
                    contentDescription = "ARAFAT AI Logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NeonCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ARAFAT AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyanBright,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = settings.primaryProvider.displayName,
                            fontSize = 11.sp,
                            color = Color(0xFF9FB2C8)
                        )
                    }
                }
            }

            // Quick Action Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onNavigateToChat() },
                    modifier = Modifier.testTag("chat_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = NeonCyan
                    )
                }

                IconButton(
                    onClick = { onNavigateToSettings() },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = ElectricBlueGlow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Central Animated HUD Visualizer ---
        HudVisualizer(
            voiceState = voiceState,
            size = 230.dp,
            onClick = {
                if (voiceState == VoiceState.LISTENING) {
                    viewModel.stopVoiceInput()
                } else if (voiceState == VoiceState.SPEAKING) {
                    viewModel.speechAndTtsManager.stopSpeaking()
                } else {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPerm) {
                        viewModel.startVoiceInput()
                    } else {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Status Label & Branding
        Text(
            text = "ARAFAT AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Your Personal AI Assistant",
            fontSize = 14.sp,
            color = NeonCyan,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Developed by ARAFAT MOJMUNDAR",
            fontSize = 11.sp,
            color = Color(0xFF7085A0),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // State indicator badge
        val (stateText, stateColor) = when (voiceState) {
            VoiceState.LISTENING -> "Listening to your voice..." to NeonRed
            VoiceState.THINKING -> "ARAFAT AI Thinking..." to NeonPurple
            VoiceState.SPEAKING -> "Speaking Response..." to NeonCyan
            VoiceState.IDLE -> (if (isLoading) "Synthesizing AI Response..." else "Tap Mic or HUD Core to Talk") to NeonGreen
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading || voiceState == VoiceState.THINKING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = stateColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stateColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = stateText,
                    fontSize = 12.sp,
                    color = stateColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (recognizedText.isNotBlank() && voiceState == VoiceState.LISTENING) {
            Text(
                text = "“$recognizedText”",
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (voiceError != null) {
            Text(
                text = voiceError ?: "",
                fontSize = 12.sp,
                color = NeonRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Large Central Mic Button ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, spotColor = stateColor)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (voiceState == VoiceState.LISTENING) NeonRed else ElectricBlue,
                            Color(0xFF002244)
                        )
                    )
                )
                .border(2.dp, stateColor, CircleShape)
                .clickable {
                    if (voiceState == VoiceState.LISTENING) {
                        viewModel.stopVoiceInput()
                    } else if (voiceState == VoiceState.SPEAKING) {
                        viewModel.speechAndTtsManager.stopSpeaking()
                    } else {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            viewModel.startVoiceInput()
                        } else {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
                .testTag("central_mic_button")
        ) {
            Icon(
                imageVector = when (voiceState) {
                    VoiceState.LISTENING -> Icons.Default.Stop
                    VoiceState.SPEAKING -> Icons.Default.MicOff
                    else -> Icons.Default.Mic
                },
                contentDescription = "Microphone Button",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Feature Shortcuts Row ---
        Text(
            text = "INTELLIGENT SHORTCUTS",
            fontSize = 11.sp,
            color = Color(0xFF6B809E),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ShortcutCard(
                    title = "Coding Assistant",
                    desc = "Kotlin, Python, JS",
                    icon = Icons.Default.Code,
                    color = NeonCyan,
                    onClick = onNavigateToCoding
                )
            }
            item {
                ShortcutCard(
                    title = "App Launcher",
                    desc = "YouTube, WhatsApp...",
                    icon = Icons.Default.TouchApp,
                    color = ElectricBlueGlow,
                    onClick = onNavigateToTools
                )
            }
            item {
                ShortcutCard(
                    title = "Prompt Generator",
                    desc = "ChatGPT, Gemini",
                    icon = Icons.Default.AutoAwesome,
                    color = NeonPurple,
                    onClick = onNavigateToTools
                )
            }
            item {
                ShortcutCard(
                    title = "Screen Assist",
                    desc = "Consent Analysis",
                    icon = Icons.Default.ScreenShare,
                    color = NeonGreen,
                    onClick = onNavigateToTools
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Attached Image Preview if user picked one
        AnimatedVisibility(visible = attachedImage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(CyberNavyCard, RoundedCornerShape(10.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Image attached",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Image attached (Ready for AI Vision)",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = { viewModel.clearAttachedImage() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attached image",
                        tint = NeonRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- Text Chat Input Bar ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach image button
                IconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.size(40.dp).testTag("attach_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Attach Image",
                        tint = if (attachedImage != null) NeonCyan else Color(0xFF7A8FA8)
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask ARAFAT AI in English or বাংলা...",
                            fontSize = 13.sp,
                            color = Color(0xFF6B809E)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_chat_input"),
                    singleLine = false,
                    maxLines = 3
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || attachedImage != null) {
                            val textToSend = inputText
                            inputText = ""
                            viewModel.sendMessage(textToSend)
                            onNavigateToChat()
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .testTag("home_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = CyberNavyDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ShortcutCard(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = Color(0xFF8A9FB8)
            )
        }
    }
}
