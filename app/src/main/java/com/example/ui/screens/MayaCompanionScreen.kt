package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.AutomationFeatureFlags
import com.example.domain.ChatMessage
import com.example.domain.MayaEmotion
import com.example.ui.AiProcessingState
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.VoiceWaveVisualizer
import com.example.ui.components.accentColor
import com.example.ui.theme.GlassBase
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassElevated
import com.example.ui.theme.MayaRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MayaCompanionScreen(
    messages: List<ChatMessage>,
    aiState: AiProcessingState,
    currentEmotion: MayaEmotion,
    featureFlags: AutomationFeatureFlags,
    isSpeaking: Boolean,
    isListening: Boolean,
    partialTranscript: String,
    rmsDb: Float,
    voiceLanguageTag: String,
    onSendMessage: (String) -> Unit,
    onStartVoiceListening: () -> Unit,
    onStopVoiceListening: () -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onToggleVoiceLanguage: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onClearChat: () -> Unit,
    onDismissAiError: () -> Unit,
    onReportVoiceError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var commandText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartVoiceListening()
        } else {
            onReportVoiceError("Microphone permission is needed so Maya can hear your voice.")
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val onMicClick: () -> Unit = {
        if (isListening) {
            onStopVoiceListening()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onStartVoiceListening()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        val isWideLayout = maxWidth >= 650.dp

        if (isWideLayout) {
            // Canonical Two-Pane Glassmorphism Layout for Tablets / Foldables
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MayaAvatarStageCard(
                    emotion = currentEmotion,
                    isSpeaking = isSpeaking,
                    isListening = isListening,
                    rmsDb = rmsDb,
                    autoSpeakEnabled = featureFlags.mayaAutoSpeakEnabled,
                    voiceLanguageTag = voiceLanguageTag,
                    isCompactHero = false,
                    onToggleAutoSpeak = onToggleAutoSpeak,
                    onToggleVoiceLanguage = onToggleVoiceLanguage,
                    onStopSpeaking = onStopSpeaking,
                    onClearChat = onClearChat,
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight()
                ) {
                    ConversationAndInputColumn(
                        messages = messages,
                        aiState = aiState,
                        currentEmotion = currentEmotion,
                        isListening = isListening,
                        partialTranscript = partialTranscript,
                        rmsDb = rmsDb,
                        commandText = commandText,
                        onCommandTextChange = { commandText = it },
                        listState = listState,
                        onSendMessage = { text ->
                            onSendMessage(text)
                            commandText = ""
                        },
                        onQuickPrompt = onSendMessage,
                        onMicClick = onMicClick,
                        onStopVoiceListening = onStopVoiceListening,
                        onSpeakMessage = onSpeakMessage,
                        onDismissAiError = onDismissAiError,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Vertical Mobile-First Glassmorphism Layout
            Column(modifier = Modifier.fillMaxSize()) {
                MayaAvatarStageCard(
                    emotion = currentEmotion,
                    isSpeaking = isSpeaking,
                    isListening = isListening,
                    rmsDb = rmsDb,
                    autoSpeakEnabled = featureFlags.mayaAutoSpeakEnabled,
                    voiceLanguageTag = voiceLanguageTag,
                    isCompactHero = true,
                    onToggleAutoSpeak = onToggleAutoSpeak,
                    onToggleVoiceLanguage = onToggleVoiceLanguage,
                    onStopSpeaking = onStopSpeaking,
                    onClearChat = onClearChat,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                ConversationAndInputColumn(
                    messages = messages,
                    aiState = aiState,
                    currentEmotion = currentEmotion,
                    isListening = isListening,
                    partialTranscript = partialTranscript,
                    rmsDb = rmsDb,
                    commandText = commandText,
                    onCommandTextChange = { commandText = it },
                    listState = listState,
                    onSendMessage = { text ->
                        onSendMessage(text)
                        commandText = ""
                    },
                    onQuickPrompt = onSendMessage,
                    onMicClick = onMicClick,
                    onStopVoiceListening = onStopVoiceListening,
                    onSpeakMessage = onSpeakMessage,
                    onDismissAiError = onDismissAiError,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConversationAndInputColumn(
    messages: List<ChatMessage>,
    aiState: AiProcessingState,
    currentEmotion: MayaEmotion,
    isListening: Boolean,
    partialTranscript: String,
    rmsDb: Float,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSendMessage: (String) -> Unit,
    onQuickPrompt: (String) -> Unit,
    onMicClick: () -> Unit,
    onStopVoiceListening: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onDismissAiError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AnimatedVisibility(visible = aiState is AiProcessingState.Error) {
            val errMsg = (aiState as? AiProcessingState.Error)?.message.orEmpty()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = StatusCrimson.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, StatusCrimson.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = StatusCrimson
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelLarge,
                        color = StatusCrimson,
                        modifier = Modifier
                            .clickable(onClick = onDismissAiError)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Frosted Glass Conversation Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(messages, key = { "${it.id}_${it.timestampMillis}" }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onSpeakClick = { onSpeakMessage(msg.text) }
                )
            }

            if (aiState is AiProcessingState.Thinking) {
                item(key = "maya_thinking") {
                    MayaThinkingIndicator(emotionColor = currentEmotion.accentColor())
                }
            }
        }

        // Live Voice Listening Glass Strip
        AnimatedVisibility(visible = isListening) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                glowColor = MayaRose
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (partialTranscript.isBlank()) {
                                "মায়া শুনছে… কথা বলো জান"
                            } else partialTranscript,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        VoiceWaveVisualizer(
                            isActive = true,
                            intensity = rmsDb.coerceAtLeast(3f),
                            color = MayaRose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onStopVoiceListening,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StatusCrimson.copy(alpha = 0.22f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop listening",
                            tint = StatusCrimson
                        )
                    }
                }
            }
        }

        // Quick Voice & Text Command Chips
        val quickPrompts = listOf(
            "একটা মিষ্টি গল্প বলো" to "গল্প বলো",
            "Open YouTube" to "YouTube",
            "Turn on flashlight" to "ফ্ল্যাশলাইট",
            "Increase volume" to "ভলিউম +",
            "Set brightness to 50%" to "ব্রাইটনেস ৫০%",
            "Show battery information" to "ব্যাটারি",
            "Check internet connection" to "ইন্টারনেট",
            "Scroll down" to "স্ক্রল ডাউন"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { (prompt, chipLabel) ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GlassBase.copy(alpha = 0.65f),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(NeonCyan.copy(alpha = 0.45f), MayaRose.copy(alpha = 0.35f))
                        )
                    ),
                    modifier = Modifier.clickable { onQuickPrompt(prompt) }
                ) {
                    Text(
                        text = chipLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Dedicated Frosted Glass Command Input Dock
        MayaCommandInputDock(
            commandText = commandText,
            onCommandTextChange = onCommandTextChange,
            isListening = isListening,
            isThinking = aiState is AiProcessingState.Thinking,
            onMicClick = onMicClick,
            onSendClick = {
                if (commandText.isNotBlank()) {
                    onSendMessage(commandText)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )
    }
}

@Composable
private fun MayaAvatarStageCard(
    emotion: MayaEmotion,
    isSpeaking: Boolean,
    isListening: Boolean,
    rmsDb: Float,
    autoSpeakEnabled: Boolean,
    voiceLanguageTag: String,
    isCompactHero: Boolean,
    onToggleAutoSpeak: () -> Unit,
    onToggleVoiceLanguage: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = emotion.accentColor()
    val transition = rememberInfiniteTransition(label = "avatar_aura")
    val auraScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSpeaking || isListening) 1.14f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )
    val ringRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )

    val stageShape = RoundedCornerShape(28.dp)
    val glassBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.25f),
            accent.copy(alpha = 0.55f),
            NeonCyan.copy(alpha = 0.35f),
            NeuralViolet.copy(alpha = 0.35f)
        )
    )

    Box(
        modifier = modifier
            .clip(stageShape)
            .border(1.2.dp, glassBorderBrush, stageShape)
            .testTag("maya_avatar_stage")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_maya_aura_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.45f)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            GlassElevated.copy(alpha = 0.52f),
                            ObsidianDeep.copy(alpha = 0.80f)
                        )
                    )
                )
        )

        if (isCompactHero) {
            // Mobile Portrait: Dedicated Visual Avatar Stage with luminous concentric halo + info & controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MayaGlowingPortrait(
                    avatarSize = 78.dp,
                    accent = accent,
                    auraScale = auraScale,
                    ringRotation = ringRotation,
                    isActive = isSpeaking || isListening
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "মায়া • Maya",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MayaRose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "তোমার স্মার্টফোন সঙ্গী",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(
                            text = when {
                                isSpeaking -> "কথা বলছে"
                                isListening -> "শুনছে"
                                else -> emotion.labelBn
                            },
                            color = accent
                        )
                        if (isSpeaking || isListening) {
                            VoiceWaveVisualizer(
                                isActive = true,
                                intensity = if (isListening) rmsDb.coerceAtLeast(3f) else 7f,
                                color = accent,
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(18.dp)
                            )
                        }
                    }
                }

                StageUtilityActions(
                    autoSpeakEnabled = autoSpeakEnabled,
                    isSpeaking = isSpeaking,
                    voiceLanguageTag = voiceLanguageTag,
                    onToggleAutoSpeak = onToggleAutoSpeak,
                    onToggleVoiceLanguage = onToggleVoiceLanguage,
                    onStopSpeaking = onStopSpeaking,
                    onClearChat = onClearChat,
                    vertical = true
                )
            }
        } else {
            // Wide / Tablet Supporting Pane: Tall Centered Avatar Sanctuary
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = when {
                            isSpeaking -> "কথা বলছে"
                            isListening -> "শুনছে"
                            else -> emotion.labelBn
                        },
                        color = accent
                    )
                    StageUtilityActions(
                        autoSpeakEnabled = autoSpeakEnabled,
                        isSpeaking = isSpeaking,
                        voiceLanguageTag = voiceLanguageTag,
                        onToggleAutoSpeak = onToggleAutoSpeak,
                        onToggleVoiceLanguage = onToggleVoiceLanguage,
                        onStopSpeaking = onStopSpeaking,
                        onClearChat = onClearChat,
                        vertical = false
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MayaGlowingPortrait(
                        avatarSize = 136.dp,
                        accent = accent,
                        auraScale = auraScale,
                        ringRotation = ringRotation,
                        isActive = isSpeaking || isListening
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "মায়া • Maya",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MayaRose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "তোমার আদরের বাংলা এআই সঙ্গী",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                VoiceWaveVisualizer(
                    isActive = isSpeaking || isListening,
                    intensity = if (isListening) rmsDb.coerceAtLeast(3f) else if (isSpeaking) 7f else 2f,
                    color = accent,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(28.dp)
                )
            }
        }
    }
}

@Composable
private fun MayaGlowingPortrait(
    avatarSize: androidx.compose.ui.unit.Dp,
    accent: Color,
    auraScale: Float,
    ringRotation: Float,
    isActive: Boolean
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(avatarSize + 18.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(avatarSize + 14.dp)
                .scale(auraScale)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isActive) 0.42f else 0.24f),
                        Color.Transparent
                    )
                )
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        accent.copy(alpha = 0.85f),
                        NeonCyan.copy(alpha = 0.45f),
                        Color.Transparent,
                        MayaRose.copy(alpha = 0.75f)
                    )
                ),
                startAngle = ringRotation,
                sweepAngle = 290f,
                useCenter = false,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        Image(
            painter = painterResource(id = R.drawable.img_maya_avatar),
            contentDescription = "Maya AI Companion Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .border(2.dp, accent.copy(alpha = 0.9f), CircleShape)
        )
    }
}

@Composable
private fun StageUtilityActions(
    autoSpeakEnabled: Boolean,
    isSpeaking: Boolean,
    voiceLanguageTag: String,
    onToggleAutoSpeak: () -> Unit,
    onToggleVoiceLanguage: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClearChat: () -> Unit,
    vertical: Boolean
) {
    val content: @Composable () -> Unit = {
        IconButton(
            onClick = onToggleVoiceLanguage,
            modifier = Modifier.size(40.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = "Switch Voice Language",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (voiceLanguageTag == "bn-BD") "BN" else "EN",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
        }
        IconButton(
            onClick = { if (isSpeaking) onStopSpeaking() else onToggleAutoSpeak() },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (autoSpeakEnabled) {
                    Icons.AutoMirrored.Filled.VolumeUp
                } else {
                    Icons.AutoMirrored.Filled.VolumeOff
                },
                contentDescription = "Toggle Maya Voice Reply",
                tint = if (autoSpeakEnabled) MayaRose else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(
            onClick = onClearChat,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = "Clear Chat History",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (vertical) {
        Row(verticalAlignment = Alignment.CenterVertically) { content() }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

@Composable
private fun MayaCommandInputDock(
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    isListening: Boolean,
    isThinking: Boolean,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockShape = RoundedCornerShape(28.dp)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.24f),
            NeonCyan.copy(alpha = 0.55f),
            MayaRose.copy(alpha = 0.45f)
        )
    )
    val bgBrush = Brush.horizontalGradient(
        colors = listOf(
            GlassElevated.copy(alpha = 0.78f),
            GlassBase.copy(alpha = 0.72f)
        )
    )

    Row(
        modifier = modifier
            .clip(dockShape)
            .background(bgBrush)
            .border(1.dp, borderBrush, dockShape)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("command_input_dock"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VoiceOrbButton(
            isListening = isListening,
            onClick = onMicClick
        )

        TextField(
            value = commandText,
            onValueChange = onCommandTextChange,
            modifier = Modifier
                .weight(1f)
                .testTag("command_input"),
            placeholder = {
                Text(
                    text = "মায়াকে বলো বা কমান্ড লিখো…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeonCyan
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendClick() })
        )

        IconButton(
            onClick = onSendClick,
            enabled = commandText.isNotBlank() && !isThinking,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (commandText.isNotBlank() && !isThinking) {
                            listOf(NeonCyan, MayaRose)
                        } else {
                            listOf(GlassBorder.copy(alpha = 0.4f), GlassBorder.copy(alpha = 0.3f))
                        }
                    )
                )
                .testTag("send_command_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send command to Maya",
                tint = if (commandText.isNotBlank() && !isThinking) ObsidianDeep else TextMuted
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onSpeakClick: () -> Unit
) {
    val isMaya = message.isFromMaya
    val accent = message.emotion.accentColor()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMaya) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isMaya) 6.dp else 22.dp,
                bottomEnd = if (isMaya) 22.dp else 6.dp
            ),
            color = if (isMaya) {
                GlassElevated.copy(alpha = 0.72f)
            } else {
                NeonCyan.copy(alpha = 0.16f)
            },
            border = BorderStroke(
                1.dp,
                if (isMaya) accent.copy(alpha = 0.38f) else NeonCyan.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                if (!message.actionSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val ok = message.actionSuccess == true
                    val chipColor = if (ok) StatusEmerald else StatusCrimson
                    Surface(
                        color = chipColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = chipColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.actionSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = chipColor
                            )
                        }
                    }
                }

                if (isMaya) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onSpeakClick)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = "Listen to Maya",
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "শুনুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MayaThinkingIndicator(emotionColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GlassBase.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, emotionColor.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = emotionColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MayaRose,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "মায়া ভাবছে…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun VoiceOrbButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mic_orb")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = if (isListening) {
                        listOf(StatusCrimson, MayaRose)
                    } else {
                        listOf(MayaRose, NeonCyan)
                    }
                )
            )
            .clickable(onClick = onClick)
            .testTag("voice_command_button")
    ) {
        Icon(
            imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = if (isListening) "Stop voice command" else "Start voice command",
            tint = ObsidianDeep,
            modifier = Modifier.size(22.dp)
        )
    }
}
