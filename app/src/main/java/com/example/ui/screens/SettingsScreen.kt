package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AiProvider
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNavyCard
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberNavySurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var customGeminiKeyInput by remember(settings.customGeminiKey) { mutableStateOf(settings.customGeminiKey) }
    var customGrokKeyInput by remember(settings.customGrokKey) { mutableStateOf(settings.customGrokKey) }

    val geminiModels = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-flash-latest")
    val grokModels = listOf("grok-2-latest", "grok-2-vision-latest", "grok-beta")

    val languages = listOf(
        "en-US" to "English (US)",
        "bn-BD" to "বাংলা (Bengali - Bangladesh)",
        "hi-IN" to "हिन्दी (Hindi - India)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberNavyDark)
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arafat_logo),
                contentDescription = "ARAFAT AI Logo",
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, NeonCyan, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ARAFAT AI SETTINGS",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyanBright
                )
                Text(
                    text = "AI Providers • Speech • Privacy • Developer Info",
                    fontSize = 11.sp,
                    color = Color(0xFF7E93AC)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 1. AI Provider Selection ---
        SectionHeader("1. ACTIVE AI PROVIDER")
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                AiProvider.values().forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateSettings { it.copy(primaryProvider = provider) }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.primaryProvider == provider,
                            onClick = {
                                viewModel.updateSettings { it.copy(primaryProvider = provider) }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = provider.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when (provider) {
                                    AiProvider.AUTO -> "Tries Gemini, automatically falls back to Grok if available."
                                    AiProvider.GEMINI -> "Official Google multimodal AI with Bengali & coding excellence."
                                    AiProvider.GROK -> "Official xAI Grok API model."
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF8A9FB8)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. Gemini Configuration ---
        SectionHeader("2. GEMINI AI CONFIGURATION")
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Gemini Model Selection:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    geminiModels.forEach { m ->
                        val isSel = settings.geminiModel == m
                        OutlinedButton(
                            onClick = {
                                viewModel.updateSettings { it.copy(geminiModel = m) }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSel) ElectricBlue else Color.Transparent,
                                contentColor = if (isSel) Color.White else Color(0xFFBACCDD)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) NeonCyan else CyberBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(m.replace("gemini-", "").replace("-preview", ""), fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customGeminiKeyInput,
                    onValueChange = { customGeminiKeyInput = it },
                    label = { Text("Custom Gemini API Key (Optional)") },
                    placeholder = { Text("Paste key if not in AI Studio secrets...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("settings_gemini_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateSettings { it.copy(customGeminiKey = customGeminiKeyInput.trim()) }
                        Toast.makeText(context, "Gemini Key Saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberNavyDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Gemini Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. Grok AI Configuration ---
        SectionHeader("3. GROK AI (xAI) CONFIGURATION")
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Grok Model Selection:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    grokModels.forEach { gm ->
                        val isSel = settings.grokModel == gm
                        OutlinedButton(
                            onClick = {
                                viewModel.updateSettings { it.copy(grokModel = gm) }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSel) NeonPurple else Color.Transparent,
                                contentColor = if (isSel) Color.White else Color(0xFFBACCDD)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) NeonCyan else CyberBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(gm, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customGrokKeyInput,
                    onValueChange = { customGrokKeyInput = it },
                    label = { Text("xAI API Key (Bearer Token)") },
                    placeholder = { Text("xai-...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("settings_grok_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateSettings { it.copy(customGrokKey = customGrokKeyInput.trim()) }
                        Toast.makeText(context, "Grok Key Saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Grok Key", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. Voice & Speech Language ---
        SectionHeader("4. VOICE & SPEECH SYNTHESIS")
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Speech Recognition Language:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateSettings { it.copy(voiceLanguage = code) }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.voiceLanguage == code,
                            onClick = {
                                viewModel.updateSettings { it.copy(voiceLanguage = code) }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(name, fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Text-to-Speech (TTS)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Read assistant responses aloud",
                            fontSize = 11.sp,
                            color = Color(0xFF8A9FB8)
                        )
                    }
                    Switch(
                        checked = settings.ttsEnabled,
                        onCheckedChange = { isEnabled ->
                            viewModel.updateSettings { it.copy(ttsEnabled = isEnabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = ElectricBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.speechAndTtsManager.speak(
                            "Hello, I am ARAFAT AI, created by ARAFAT MOJMUNDAR. Speech synthesis is fully operational.",
                            settings.voiceLanguage
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Test Audio", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test TTS Speech Output", color = NeonCyan, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. Privacy & Data Controls ---
        SectionHeader("5. PRIVACY & LOCAL STORAGE")
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Local Device Security:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "ARAFAT AI stores conversation and code strictly on your local device. No telemetry or secret keys are exposed.",
                    fontSize = 11.sp,
                    color = Color(0xFFBACCDD),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.clearChatHistory()
                            Toast.makeText(context, "Chat history deleted", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.8f), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Chat", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.updateSettings { it.copy(customGeminiKey = "", customGrokKey = "") }
                            customGeminiKeyInput = ""
                            customGrokKeyInput = ""
                            Toast.makeText(context, "Custom keys wiped", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263750), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Wipe Keys", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 6. About Developer & App Information ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavySurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_arafat_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NeonCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "ARAFAT AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyanBright
                    )
                    Text(
                        text = "Your Personal AI Assistant",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Developer: ARAFAT MOJMUNDAR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlueGlow
                    )
                    Text(
                        text = "Version 1.0.0 • Native Android Kotlin",
                        fontSize = 10.sp,
                        color = Color(0xFF7E93AC)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF6B809E),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
