package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.BioscienceAiService
import com.example.ui.components.LiquidBackgroundCanvas
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidCyanLight
import com.example.ui.theme.LiquidMagenta
import com.example.ui.theme.LiquidTeal
import com.example.ui.theme.LiquidViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun BioscienceAiScreen() {
    val aiService = remember { BioscienceAiService() }
    val scope = rememberCoroutineScope()

    var userPrompt by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    val presetPrompts = listOf(
        "Simulate osmotic shock & hemolysis in human red blood cells",
        "How does the ATP Synthase rotor mechanism convert electrochemical proton gradients into chemical bonds?",
        "Explain CRISPR-Cas9 target DNA cleavage and gRNA pairing",
        "Thermodynamics of lipid bilayer phase transitions: liquid-ordered vs liquid-disordered states"
    )

    fun startInference(prompt: String) {
        if (prompt.isBlank() || isStreaming) return
        userPrompt = prompt
        aiResponse = ""
        isStreaming = true

        activeJob?.cancel()
        activeJob = scope.launch {
            try {
                aiService.streamScientificInquiry(prompt).collect { chunk ->
                    aiResponse += chunk
                }
            } finally {
                isStreaming = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackgroundCanvas(
            primaryColor = LiquidCyan,
            secondaryColor = LiquidViolet,
            tertiaryColor = LiquidTeal
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            // Header
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                testTag = "ai_header_card"
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI Research Engine",
                            tint = LiquidCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "GEMINI 3.8 FLASH INTELLIGENCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiquidCyanLight,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Bioscience Research Lab",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Query advanced cellular mechanisms, thermodynamic fluid equations, genetic mutations, and molecular biology backed by Gemini 3.8 Flash.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Quick Preset Prompts
            Text(
                text = "SCIENTIFIC EXPERIMENT PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LiquidTeal,
                letterSpacing = 1.8.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetPrompts.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .clickable(enabled = !isStreaming) {
                                startInference(prompt)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Preset Query",
                                tint = LiquidCyanLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = prompt,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Query Input Card
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                testTag = "ai_query_input_card"
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ENTER BIOSCIENCE QUERY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidViolet,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userPrompt,
                        onValueChange = { userPrompt = it },
                        placeholder = {
                            Text(
                                text = "Ask about cellular biophysics, osmosis, enzymes...",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_text_field"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LiquidCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = LiquidCyan
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (isStreaming) {
                            LiquidGlassButton(
                                text = "Cancel Generation",
                                onClick = {
                                    activeJob?.cancel()
                                    isStreaming = false
                                },
                                primaryColor = Color(0xFFEF4444),
                                secondaryColor = Color(0xFFDC2626),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Generation",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                testTag = "stop_generation_button"
                            )
                        } else {
                            LiquidGlassButton(
                                text = "Run Scientific Analysis",
                                onClick = { startInference(userPrompt) },
                                primaryColor = LiquidCyan,
                                secondaryColor = LiquidViolet,
                                enabled = userPrompt.isNotBlank(),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send Query",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                testTag = "send_query_button"
                            )
                        }
                    }
                }
            }

            // Streaming Result Card
            if (isStreaming || aiResponse.isNotEmpty()) {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(26.dp),
                    hasShimmer = isStreaming,
                    testTag = "ai_result_card"
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ANALYSIS OUTPUT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiquidCyanLight,
                                letterSpacing = 1.4.sp
                            )

                            if (isStreaming) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = LiquidCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Synthesizing...",
                                        fontSize = 11.sp,
                                        color = LiquidCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = aiResponse.ifEmpty { "Initializing Gemini 3.8 Flash inference engine..." },
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}
