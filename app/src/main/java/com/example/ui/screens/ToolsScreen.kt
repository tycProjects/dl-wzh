package com.example.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.GeminiBrain
import com.example.ai.GeminiConnectionStatus
import com.example.data.CommandAliasEntity
import com.example.data.MayaSettings
import com.example.device.AuditedPermission
import com.example.device.InstalledApp
import com.example.device.PermissionActionType
import com.example.service.BackgroundModeState
import com.example.ui.components.AppIconImage
import com.example.ui.components.GlassCard
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
import com.example.voice.ElevenLabsVoiceOption
import com.example.voice.VoiceEngineStatus
import com.example.viewmodel.AutomationState
import java.util.Locale

@Composable
fun ToolsScreen(
    settings: MayaSettings,
    geminiStatus: GeminiConnectionStatus,
    hasCustomGeminiKey: Boolean,
    voiceStatus: VoiceEngineStatus,
    voiceStatusDetail: String,
    hasElevenLabsKey: Boolean,
    elevenLabsVoices: List<ElevenLabsVoiceOption>,
    backgroundState: BackgroundModeState,
    batterySaverActive: Boolean,
    auditedPermissions: List<AuditedPermission>,
    aliases: List<CommandAliasEntity>,
    installedApps: List<InstalledApp>,
    automationState: AutomationState,
    listState: LazyListState = rememberLazyListState(),
    onTestGemini: () -> Unit,
    onSaveCustomGeminiKey: (String) -> Unit,
    onDisconnectCustomGeminiKey: () -> Unit,
    onConnectElevenLabs: (String) -> Unit,
    onDisconnectElevenLabs: () -> Unit,
    onTestVoice: () -> Unit,
    onUpdateSettings: ((MayaSettings) -> MayaSettings) -> Unit,
    onStartBackground: () -> Unit,
    onPauseBackground: () -> Unit,
    onResumeBackground: () -> Unit,
    onStopBackground: () -> Unit,
    onPermissionAction: (PermissionActionType) -> Unit,
    onAddAlias: (String, InstalledApp) -> Unit,
    onToggleAlias: (CommandAliasEntity, Boolean) -> Unit,
    onDeleteAlias: (Long) -> Unit,
    onRunAutomationSample: (String) -> Unit,
    onStopAutomation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddAliasDialog by remember { mutableStateOf(false) }

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
            Text(
                text = "টুলস ও সেটিংস",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                listOf("AI ও ভয়েস", "পারমিশন ও ব্যাকগ্রাউন্ড", "অ্যালায়াস ও অ্যাডভান্সড").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonIndigo.copy(alpha = 0.35f),
                            selectedLabelColor = NeonCyan,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("tools_subtab_$idx")
                    )
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            GeminiConfigCard(
                                settings = settings,
                                status = geminiStatus,
                                hasCustomKey = hasCustomGeminiKey,
                                onSelectModel = { model -> onUpdateSettings { it.copy(geminiModel = model) } },
                                onTest = onTestGemini,
                                onSaveCustomKey = onSaveCustomGeminiKey,
                                onDisconnectCustomKey = onDisconnectCustomGeminiKey
                            )
                        }
                        item {
                            ElevenLabsConfigCard(
                                settings = settings,
                                voiceStatus = voiceStatus,
                                voiceStatusDetail = voiceStatusDetail,
                                hasKey = hasElevenLabsKey,
                                voices = elevenLabsVoices,
                                onConnect = onConnectElevenLabs,
                                onDisconnect = onDisconnectElevenLabs,
                                onSelectVoice = { v ->
                                    onUpdateSettings { it.copy(elevenLabsVoiceId = v.voiceId, elevenLabsVoiceName = v.name) }
                                },
                                onToggleFallbackTts = { enabled ->
                                    onUpdateSettings { it.copy(fallbackToAndroidTts = enabled) }
                                },
                                onTestVoice = onTestVoice
                            )
                        }
                    }

                    1 -> {
                        item {
                            BackgroundAssistantCard(
                                state = backgroundState,
                                batterySaver = batterySaverActive,
                                onStart = onStartBackground,
                                onPause = onPauseBackground,
                                onResume = onResumeBackground,
                                onStop = onStopBackground
                            )
                        }
                        item {
                            PermissionCenterCard(
                                permissions = auditedPermissions,
                                onAction = onPermissionAction
                            )
                        }
                    }

                    2 -> {
                        item {
                            CommandAliasesCard(
                                aliases = aliases,
                                onOpenAdd = { showAddAliasDialog = true },
                                onToggle = onToggleAlias,
                                onDelete = onDeleteAlias
                            )
                        }
                        item {
                            AutomationCard(
                                automationState = automationState,
                                onRun = onRunAutomationSample,
                                onStop = onStopAutomation
                            )
                        }
                        item {
                            AdvancedSettingsCard(
                                settings = settings,
                                onUpdateSettings = onUpdateSettings
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddAliasDialog) {
        AddAliasDialog(
            apps = installedApps,
            onConfirm = { phrase, app ->
                onAddAlias(phrase, app)
                showAddAliasDialog = false
            },
            onDismiss = { showAddAliasDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiConfigCard(
    settings: MayaSettings,
    status: GeminiConnectionStatus,
    hasCustomKey: Boolean,
    onSelectModel: (String) -> Unit,
    onTest: () -> Unit,
    onSaveCustomKey: (String) -> Unit,
    onDisconnectCustomKey: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini AI ইঞ্জিন", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            StatusDotPill(
                label = when (status) {
                    GeminiConnectionStatus.CONNECTED -> "সংযুক্ত"
                    GeminiConnectionStatus.READY -> if (hasCustomKey && settings.useCustomGeminiKey) "Custom Key" else "Firebase AI প্রস্তুত"
                    GeminiConnectionStatus.CONNECTING -> "যাচাই হচ্ছে…"
                    GeminiConnectionStatus.ERROR -> "ত্রুটি"
                },
                dotColor = when (status) {
                    GeminiConnectionStatus.CONNECTED, GeminiConnectionStatus.READY -> EmeraldActive
                    GeminiConnectionStatus.CONNECTING -> AmberWarning
                    GeminiConnectionStatus.ERROR -> CoralError
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("মডেল নির্বাচন:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GeminiBrain.SUPPORTED_MODELS.forEach { (id, label) ->
                FilterChip(
                    selected = settings.geminiModel == id,
                    onClick = { onSelectModel(id) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonIndigo.copy(alpha = 0.35f),
                        selectedLabelColor = NeonCyan,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Firebase AI Logic ডিফল্টভাবে কোনো API Key ছাড়াই চলে। চাইলে নিচে নিজস্ব Gemini API Key যোগ করতে পারো:",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            placeholder = { Text(if (hasCustomKey) "নতুন Key লিখো (বর্তমানে সংরক্ষিত)" else "ঐচ্ছিক Gemini API Key", color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CosmicGlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth().testTag("tools_gemini_key_input")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (apiKeyInput.isNotBlank()) {
                        onSaveCustomKey(apiKeyInput)
                        apiKeyInput = ""
                    } else {
                        onTest()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                modifier = Modifier.weight(1f).testTag("tools_gemini_connect_btn")
            ) {
                Text(if (apiKeyInput.isNotBlank()) "Key সংযুক্ত করো" else "সংযোগ টেস্ট করো")
            }
            if (hasCustomKey) {
                OutlinedButton(onClick = onDisconnectCustomKey) {
                    Text("বিচ্ছিন্ন", color = CoralError)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ElevenLabsConfigCard(
    settings: MayaSettings,
    voiceStatus: VoiceEngineStatus,
    voiceStatusDetail: String,
    hasKey: Boolean,
    voices: List<ElevenLabsVoiceOption>,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onSelectVoice: (ElevenLabsVoiceOption) -> Unit,
    onToggleFallbackTts: (Boolean) -> Unit,
    onTestVoice: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.RecordVoiceOver, null, tint = NeonPink)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ElevenLabs নারী ভয়েস", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            StatusDotPill(
                label = if (voiceStatus == VoiceEngineStatus.ELEVENLABS_CONNECTED) "সংযুক্ত" else "TTS Fallback",
                dotColor = when (voiceStatus) {
                    VoiceEngineStatus.ELEVENLABS_CONNECTED -> EmeraldActive
                    VoiceEngineStatus.FALLBACK_TTS_READY -> NeonCyan
                    VoiceEngineStatus.ELEVENLABS_ERROR -> CoralError
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(voiceStatusDetail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            placeholder = { Text(if (hasKey) "ElevenLabs Key সংরক্ষিত আছে" else "ElevenLabs API Key দাও", color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPink,
                unfocusedBorderColor = CosmicGlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth().testTag("tools_elevenlabs_key_input")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (keyInput.isNotBlank()) {
                        onConnect(keyInput)
                        keyInput = ""
                    } else {
                        onTestVoice()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                modifier = Modifier.weight(1f).testTag("tools_elevenlabs_connect_btn")
            ) {
                Text(if (keyInput.isNotBlank()) "কানেক্ট করো" else "ভয়েস শোনো")
            }
            if (hasKey) {
                OutlinedButton(onClick = onDisconnect) {
                    Text("বিচ্ছিন্ন", color = CoralError)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("নারী কণ্ঠস্বর বাছাই:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            voices.take(6).forEach { v ->
                FilterChip(
                    selected = settings.elevenLabsVoiceId == v.voiceId,
                    onClick = { onSelectVoice(v) },
                    label = { Text(v.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPink.copy(alpha = 0.25f),
                        selectedLabelColor = NeonPink,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ElevenLabs না থাকলে Android বাংলা TTS ব্যবহার করো", color = TextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = settings.fallbackToAndroidTts, onCheckedChange = onToggleFallbackTts)
        }
    }
}

@Composable
private fun BackgroundAssistantCard(
    state: BackgroundModeState,
    batterySaver: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GraphicEq, null, tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = if (batterySaver) "ব্যাটারি সেভার সক্রিয় (সাশ্রয়ী মোড)" else "স্বচ্ছ ফোরগ্রাউন্ড নোটিফিকেশন সহ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            StatusDotPill(
                label = when (state) {
                    BackgroundModeState.ACTIVE_LISTENING -> "চালু"
                    BackgroundModeState.PAUSED -> "বিরতিতে"
                    BackgroundModeState.STOPPED -> "বন্ধ"
                },
                dotColor = when (state) {
                    BackgroundModeState.ACTIVE_LISTENING -> EmeraldActive
                    BackgroundModeState.PAUSED -> AmberWarning
                    BackgroundModeState.STOPPED -> TextSecondary
                }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            when (state) {
                BackgroundModeState.STOPPED -> {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        modifier = Modifier.fillMaxWidth().testTag("bg_start_btn")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ব্যাকগ্রাউন্ড অ্যাসিস্ট্যান্ট চালু করো")
                    }
                }
                BackgroundModeState.ACTIVE_LISTENING -> {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).testTag("bg_pause_btn")) {
                        Icon(Icons.Rounded.Pause, null, tint = AmberWarning)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বিরতি", color = TextPrimary)
                    }
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                        modifier = Modifier.weight(1f).testTag("bg_stop_btn")
                    ) {
                        Icon(Icons.Rounded.Stop, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বন্ধ করো")
                    }
                }
                BackgroundModeState.PAUSED -> {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive),
                        modifier = Modifier.weight(1f).testTag("bg_resume_btn")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("আবার চালু")
                    }
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Stop, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("বন্ধ করো")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCenterCard(
    permissions: List<AuditedPermission>,
    onAction: (PermissionActionType) -> Unit
) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Security, null, tint = EmeraldActive)
            Spacer(modifier = Modifier.width(8.dp))
            Text("পারমিশন সেন্টার (রিয়েল-টাইম)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            permissions.forEach { perm ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicGlassBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(perm.title, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                        StatusDotPill(
                            label = perm.statusLabel,
                            dotColor = if (perm.isGranted) EmeraldActive else AmberWarning
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(perm.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { onAction(perm.actionType) },
                        modifier = Modifier.align(Alignment.End).testTag("perm_action_${perm.id}")
                    ) {
                        Text(if (perm.isGranted) "সেটিংস দেখো" else "অনুমতি দাও / সেটিংস", color = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandAliasesCard(
    aliases: List<CommandAliasEntity>,
    onOpenAdd: () -> Unit,
    onToggle: (CommandAliasEntity, Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("কাস্টম ভয়েস কমান্ড (Aliases)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            IconButton(onClick = onOpenAdd, modifier = Modifier.testTag("add_alias_btn")) {
                Icon(Icons.Rounded.Add, contentDescription = "নতুন অ্যালায়াস", tint = NeonCyan)
            }
        }
        Text(
            text = "যেমন: \"ভিডিও দেখাও\" → YouTube অথবা \"কাজের অ্যাপ খোলো\" → পছন্দের অ্যাপ",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (aliases.isEmpty()) {
            Text("এখনো কোনো কাস্টম কমান্ড যোগ করা হয়নি। '+' চেপে যোগ করো।", color = TextSecondary)
        } else {
            aliases.forEach { alias ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("\"${alias.triggerPhrase}\"", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                        Text("→ ${alias.targetAppLabel}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Switch(checked = alias.enabled, onCheckedChange = { onToggle(alias, it) })
                    IconButton(onClick = { onDelete(alias.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "মুছো", tint = CoralError)
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationCard(
    automationState: AutomationState,
    onRun: (String) -> Unit,
    onStop: () -> Unit
) {
    var sequenceInput by remember { mutableStateOf("ব্যাটারি চেক করো তারপর স্টোরেজ দেখাও") }

    GlassCard {
        Text("নিরাপদ অটোমেশন সিকোয়েন্স", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\"তারপর\" দিয়ে একাধিক ধাপ একসাথে চালাও (যেকোনো সময় Stop Automation চাপতে পারবে)।",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sequenceInput,
            onValueChange = { sequenceInput = it },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPurple,
                unfocusedBorderColor = CosmicGlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onRun(sequenceInput) },
                enabled = !automationState.isRunning && sequenceInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                modifier = Modifier.weight(1f).testTag("run_automation_btn")
            ) {
                Text("অটোমেশন চালাও")
            }
            if (automationState.isRunning) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                    modifier = Modifier.testTag("stop_automation_btn")
                ) {
                    Text("Stop Automation")
                }
            }
        }
        if (automationState.statusText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(automationState.statusText, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedSettingsCard(
    settings: MayaSettings,
    onUpdateSettings: ((MayaSettings) -> MayaSettings) -> Unit
) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Settings, null, tint = NeonCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("অ্যাডভান্সড সেটিংস", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(10.dp))

        Text("ভাষা (Language):", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("bn-BD" to "বাংলা (ডিফল্ট)", "banglish" to "Banglish", "en" to "English").forEach { (code, label) ->
                FilterChip(
                    selected = settings.languageMode == code,
                    onClick = { onUpdateSettings { it.copy(languageMode = code) } },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("কথা বলার গতি (${String.format(Locale.US, "%.2f", settings.voiceSpeed)}x)", color = TextPrimary)
        Slider(
            value = settings.voiceSpeed,
            valueRange = 0.75f..1.5f,
            onValueChange = { v -> onUpdateSettings { it.copy(voiceSpeed = v) } },
            colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPurple)
        )

        Text("ভয়েস ভলিউম (${(settings.voiceVolume * 100).toInt()}%)", color = TextPrimary)
        Slider(
            value = settings.voiceVolume,
            valueRange = 0f..1f,
            onValueChange = { v -> onUpdateSettings { it.copy(voiceVolume = v) } },
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonIndigo)
        )

        SettingSwitchRow("দ্রুত লোকাল কমান্ড ইঞ্জিন (Low Latency)", settings.fastLocalCommands) { v ->
            onUpdateSettings { it.copy(fastLocalCommands = v) }
        }
        SettingSwitchRow("স্ট্রিমিং ভয়েস (Streaming TTS)", settings.streamingTts) { v ->
            onUpdateSettings { it.copy(streamingTts = v) }
        }
        SettingSwitchRow("উত্তর শেষে আবার শোনা (Auto-Listen)", settings.autoListenAfterReply) { v ->
            onUpdateSettings { it.copy(autoListenAfterReply = v) }
        }
        SettingSwitchRow("সংবেদনশীল কাজে অনুমতি চাওয়া (Safety)", settings.confirmSensitiveActions) { v ->
            onUpdateSettings { it.copy(confirmSensitiveActions = v) }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AddAliasDialog(
    apps: List<InstalledApp>,
    onConfirm: (String, InstalledApp) -> Unit,
    onDismiss: () -> Unit
) {
    var phrase by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf(apps.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicSurfaceElevated,
        title = { Text("নতুন কমান্ড অ্যালায়াস", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    placeholder = { Text("ভয়েস কমান্ড লিখো (যেমন: ভিডিও দেখাও)", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("কোন অ্যাপ খুলবে বাছো:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(apps.size) { idx ->
                        val app = apps[idx]
                        val isSel = selectedApp?.packageName == app.packageName
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) NeonIndigo.copy(alpha = 0.35f) else CosmicSurface)
                                .clickable { selectedApp = app }
                                .padding(8.dp)
                        ) {
                            AppIconImage(app.icon, app.label, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(app.label, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedApp?.let { onConfirm(phrase, it) } },
                enabled = phrase.isNotBlank() && selectedApp != null
            ) {
                Text("সংরক্ষণ করো")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল", color = TextSecondary)
            }
        }
    )
}
