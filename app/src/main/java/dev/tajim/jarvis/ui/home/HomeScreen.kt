package dev.tajim.jarvis.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tajim.jarvis.R
import dev.tajim.jarvis.assistant.AssistantMode
import dev.tajim.jarvis.assistant.AssistantPhase
import dev.tajim.jarvis.assistant.Notice
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.HoloButton
import dev.tajim.jarvis.ui.components.HoloIconButton
import dev.tajim.jarvis.ui.components.JarvisBackground
import dev.tajim.jarvis.ui.components.StatusDot
import dev.tajim.jarvis.ui.components.StatusKind
import dev.tajim.jarvis.ui.components.tabContentInsets
import dev.tajim.jarvis.ui.navigation.Workspace
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing
import dev.tajim.jarvis.voice.MicPermission
import java.util.Calendar

private val LeftRail = listOf(Workspace.WEB_LAB, Workspace.CODE_LAB, Workspace.FILE_LAB, Workspace.MEDIA_LAB)
private val RightRail = listOf(Workspace.AI_LAB, Workspace.TOOLS, Workspace.SYSTEM, Workspace.MORE)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMicClick: () -> Unit,
    onOpenWorkspace: (Workspace) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenWeatherSettings: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenLanguage: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val orbActive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val c = LocalJarvisColors.current

    // Real weather: fetched when Home appears or resumes (throttled inside the view model).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshWeather() }
    LaunchedEffect(state.settings.useDeviceLocation, state.settings.weatherCity) { viewModel.refreshWeather(force = true) }

    JarvisBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(tabContentInsets())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
        ) {
            HomeHeader(state.settings.displayName, onOpenSettings, onOpenAbout, onOpenMemory)

            Row(
                Modifier.fillMaxWidth().padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LauncherRail(LeftRail, onOpenWorkspace)
                FanOrbCluster(
                    orbState = state.orbState,
                    orbActive = orbActive,
                    onOrbTap = onMicClick,
                    onOpen = onOpenWorkspace,
                    modifier = Modifier.weight(1f),
                )
                LauncherRail(RightRail, onOpenWorkspace)
            }

            StatusCapsule(state, onMicClick)
            Waveform(level = state.micLevel, active = state.assistant.phase == AssistantPhase.LISTENING)

            state.assistant.notice?.let { NoticeCard(it, onDismiss = viewModel::clearNotice, onAllowMic = onMicClick) }

            WeatherCard(state.weather, onClick = {
                if (state.weather is WeatherUi.NeedsLocation || state.weather is WeatherUi.Error) onOpenWeatherSettings() else viewModel.refreshWeather(force = true)
            })

            val micOk = MicPermission.isGranted(androidx.compose.ui.platform.LocalContext.current)
            Row(Modifier.fillMaxWidth().padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                InfoChip(Icons.Outlined.Mic, "Voice", if (state.assistant.mode == AssistantMode.OFF) (if (micOk) "Ready" else "Mic off") else if (state.assistant.mode == AssistantMode.AWAKE) "Awake" else "Sleeping",
                    if (micOk) StatusKind.OK else StatusKind.WARN, Modifier.weight(1f), onOpenSettings)
                InfoChip(Icons.Outlined.Layers, "Memories", if (state.memoryCount == 0) "None saved" else "${state.memoryCount} saved",
                    StatusKind.NEUTRAL, Modifier.weight(1f), onOpenMemory)
                InfoChip(Icons.Outlined.Language, "Language", when (state.settings.voiceLanguage) { "bn-BD" -> "Bengali"; "en-US" -> "English"; else -> "Device" },
                    StatusKind.NEUTRAL, Modifier.weight(1f), onOpenLanguage)
            }
            Text(
                aiCaption(state),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = Spacing.lg),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun aiCaption(s: HomeUiState): String {
    val net = if (s.isOnline) "Online" else "Offline"
    val ai = when {
        !s.settings.hasApiKey -> "AI: not configured"
        s.settings.aiModel.isBlank() -> "AI: choose a model"
        else -> "AI: ${s.settings.aiModel}"
    }
    return "$net  •  $ai"
}

@Composable
private fun HomeHeader(name: String, onOpenSettings: () -> Unit, onOpenAbout: () -> Unit, onOpenMemory: () -> Unit) {
    val c = LocalJarvisColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greet = when { hour < 12 -> "Good morning"; hour < 18 -> "Good afternoon"; else -> "Good evening" }
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Box {
            HoloIconButton(Icons.Outlined.Menu, stringResource(R.string.menu_open), { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Settings") }, onClick = { menuOpen = false; onOpenSettings() })
                DropdownMenuItem(text = { Text("Memory Bank") }, onClick = { menuOpen = false; onOpenMemory() })
                DropdownMenuItem(text = { Text("About JARVIS") }, onClick = { menuOpen = false; onOpenAbout() })
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Jarvis",
                style = TextStyle(
                    fontSize = 36.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp,
                    brush = Brush.linearGradient(listOf(c.cyan, c.blue, c.violet)),
                ),
            )
            Text(
                if (name.isBlank()) greet else "$greet, $name",
                style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        HoloIconButton(Icons.Outlined.Settings, stringResource(R.string.home_open_settings), onOpenSettings)
    }
}

@Composable
private fun LauncherRail(items: List<Workspace>, onOpen: (Workspace) -> Unit) {
    Column(Modifier.width(46.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { ws ->
            val c = LocalJarvisColors.current
            GlassCard(Modifier.fillMaxWidth().height(60.dp), onClick = { onOpen(ws) }) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(ws.icon, contentDescription = null, tint = c.cyan, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(ws.title).uppercase(),
                        fontSize = 8.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2,
                        color = c.textPrimary,
                    )
                }
            }
        }
    }
}

/** Capsule under the Orb. Every line reflects the assistant's real mode and phase. */
@Composable
private fun StatusCapsule(state: HomeUiState, onTap: () -> Unit) {
    val c = LocalJarvisColors.current
    val a = state.assistant
    val title: String
    val sub: String
    when {
        a.phase == AssistantPhase.LISTENING -> { title = "LISTENING…"; sub = "Speak now" }
        a.phase == AssistantPhase.THINKING -> { title = "THINKING…"; sub = if (a.lastHeard.isNotBlank()) "Processing: ${a.lastHeard}" else "Processing request…" }
        a.phase == AssistantPhase.SPEAKING -> { title = "SPEAKING…"; sub = a.lastReply.ifBlank { "…" } }
        a.mode == AssistantMode.ASLEEP -> { title = "SLEEPING"; sub = "Say “Jarvis” to wake me" }
        a.mode == AssistantMode.AWAKE -> { title = "AWAKE"; sub = "Say “sleep” to rest" }
        else -> { title = "READY"; sub = "Tap the orb to talk" }
    }
    val working = a.phase != AssistantPhase.IDLE
    GlassCard(Modifier.fillMaxWidth().padding(top = Spacing.md), onClick = onTap) {
        Row(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (working) CircularProgressIndicator(Modifier.size(28.dp), color = c.cyan, strokeWidth = 3.dp)
                else Icon(if (a.mode == AssistantMode.ASLEEP) Icons.Outlined.Bedtime else Icons.Outlined.Mic, null, tint = c.cyan)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Bars follow the real microphone level while listening and rest as a flat line otherwise. */
@Composable
private fun Waveform(level: Float, active: Boolean) {
    val c = LocalJarvisColors.current
    var history by remember { mutableStateOf(List(32) { 0f }) }
    LaunchedEffect(level, active) { history = history.drop(1) + (if (active) level else 0f) }
    Canvas(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = Spacing.xl)) {
        val n = history.size
        val gap = 3.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        history.forEachIndexed { i, v ->
            val h = (2.dp.toPx() + v * (size.height - 2.dp.toPx())).coerceAtMost(size.height)
            drawRoundRect(
                Brush.verticalGradient(listOf(c.cyan, c.violet)),
                topLeft = Offset(i * (bw + gap), (size.height - h) / 2f),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f),
            )
        }
    }
}

@Composable
private fun NoticeCard(notice: Notice, onDismiss: () -> Unit, onAllowMic: () -> Unit) {
    val c = LocalJarvisColors.current
    GlassCard(Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(notice.text, style = MaterialTheme.typography.bodyMedium, color = c.warning)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (notice.kind == Notice.Kind.MIC_PERMISSION) HoloButton("Allow microphone", onAllowMic)
                HoloButton("Dismiss", onDismiss)
            }
        }
    }
}

@Composable
private fun WeatherCard(w: WeatherUi, onClick: () -> Unit) {
    val c = LocalJarvisColors.current
    GlassCard(Modifier.fillMaxWidth().padding(top = Spacing.md), onClick = onClick) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            when (w) {
                is WeatherUi.Ready -> {
                    val x = w.weather
                    Icon(Icons.Outlined.WbSunny, null, tint = c.warning, modifier = Modifier.size(34.dp))
                    Text("${x.tempC}°C", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium), color = c.textPrimary)
                    Column(Modifier.weight(1f)) {
                        Text("${x.description} • ${x.place}", style = MaterialTheme.typography.titleMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Feels ${x.feelsC}° • H ${x.highC}° L ${x.lowC}° • ${x.humidity}% humidity", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, maxLines = 2)
                    }
                }
                WeatherUi.Loading -> {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Loading weather…", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                }
                WeatherUi.NeedsLocation -> {
                    Icon(Icons.Outlined.WbSunny, null, tint = c.textSecondary, modifier = Modifier.size(28.dp))
                    Text("Tap to set your location for live weather", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                }
                is WeatherUi.Error -> {
                    StatusDot(StatusKind.ERROR)
                    Text(w.message + " Tap to change location.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, title: String, value: String, kind: StatusKind, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalJarvisColors.current
    GlassCard(modifier, onClick = onClick) {
        Row(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = c.cyan, modifier = Modifier.size(20.dp))
            Column {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary, maxLines = 1)
                Text(value, fontSize = 11.sp, color = if (kind == StatusKind.OK) c.success else if (kind == StatusKind.WARN) c.warning else c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
