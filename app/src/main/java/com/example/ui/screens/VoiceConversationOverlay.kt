package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.ScreenSearchDesktop
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ai.GeminiConnectionStatus
import com.example.ui.components.MayaCharacter
import com.example.ui.components.StatusDotPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CosmicDeep
import com.example.ui.theme.CosmicGlassBorder
import com.example.ui.theme.CosmicSurfaceElevated
import com.example.ui.theme.CosmicVoid
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.VoiceEngineStatus
import com.example.viewmodel.AssistantMode

/**
 * Voice Conversation Hide Mode:
 * Hides all text chat bubbles and message lists, focusing exclusively on Maya's animated voice states.
 */
@Composable
fun VoiceConversationOverlay(
    assistantMode: AssistantMode,
    statusHeadline: String,
    liveTranscript: String,
    speechAmplitude: Float,
    speechRmsLevel: Float,
    hasMicPermission: Boolean,
    geminiStatus: GeminiConnectionStatus,
    voiceStatus: VoiceEngineStatus,
    isScreenSharing: Boolean,
    onTapListen: () -> Unit,
    onInterruptMaya: () -> Unit,
    onAskAboutScreen: () -> Unit,
    onGoToChat: () -> Unit,
    onExitVoiceMode: () -> Unit,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val apiColor = when (geminiStatus) {
        GeminiConnectionStatus.CONNECTED, GeminiConnectionStatus.READY -> EmeraldActive
        GeminiConnectionStatus.CONNECTING -> AmberWarning
        GeminiConnectionStatus.ERROR -> CoralError
    }

    val voiceColor = when (voiceStatus) {
        VoiceEngineStatus.ELEVENLABS_CONNECTED -> NeonPink
        VoiceEngineStatus.FALLBACK_TTS_READY -> NeonCyan
        VoiceEngineStatus.ELEVENLABS_ERROR -> AmberWarning
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF15193B), CosmicDeep, CosmicVoid)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Status Row: Microphone & API status (No chat messages shown!)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusDotPill(
                        label = if (hasMicPermission) "Mic প্রস্তুত" else "Mic বন্ধ",
                        dotColor = if (hasMicPermission) EmeraldActive else CoralError,
                        onClick = if (!hasMicPermission) onRequestMicPermission else null
                    )
                    StatusDotPill(
                        label = when (geminiStatus) {
                            GeminiConnectionStatus.CONNECTED, GeminiConnectionStatus.READY -> "Gemini AI"
                            GeminiConnectionStatus.CONNECTING -> "সংযুক্ত হচ্ছে"
                            GeminiConnectionStatus.ERROR -> "API ত্রুটি"
                        },
                        dotColor = apiColor
                    )
                }

                StatusDotPill(
                    label = if (voiceStatus == VoiceEngineStatus.ELEVENLABS_CONNECTED) "ElevenLabs" else "BN Voice",
                    dotColor = voiceColor
                )
            }

            // Prominent Central Maya Character & Animated State
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                MayaCharacter(
                    mode = assistantMode,
                    speechAmplitude = speechAmplitude,
                    listeningRms = speechRmsLevel,
                    size = 250.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (assistantMode) {
                        AssistantMode.LISTENING -> "মায়া শুনছে…"
                        AssistantMode.THINKING -> "মায়া ভাবছে…"
                        AssistantMode.SPEAKING -> "মায়া কথা বলছে…"
                        AssistantMode.ERROR -> statusHeadline
                        AssistantMode.IDLE -> "কথা বলতে মাইক চাপো"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                AnimatedVisibility(visible = liveTranscript.isNotBlank()) {
                    Text(
                        text = "\"$liveTranscript\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NeonCyan,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicSurfaceElevated.copy(alpha = 0.7f))
                            .border(1.dp, CosmicGlassBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Voice Controls: Interrupt, Listen, Chat, Exit Voice Mode
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (assistantMode == AssistantMode.SPEAKING || assistantMode == AssistantMode.THINKING) {
                        Button(
                            onClick = onInterruptMaya,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                            shape = CircleShape,
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("voice_interrupt_btn")
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("কথা থামাও")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Primary Voice Mic Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonPink, NeonPurple)))
                            .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .minimumInteractiveComponentSize()
                            .testTag("voice_overlay_mic_btn")
                            .clickable(onClick = {
                                if (!hasMicPermission) onRequestMicPermission()
                                else onTapListen()
                            })
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "কথা বলো",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    if (isScreenSharing) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onAskAboutScreen,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                            shape = CircleShape,
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(Icons.Rounded.ScreenSearchDesktop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("স্ক্রিন দেখো")
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onGoToChat,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_go_to_chat_btn")
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("চ্যাটে যাও", color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = onExitVoiceMode,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_exit_btn")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = CoralError, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ভয়েস বন্ধ করো", color = TextPrimary)
                    }
                }
            }
        }
    }
}
