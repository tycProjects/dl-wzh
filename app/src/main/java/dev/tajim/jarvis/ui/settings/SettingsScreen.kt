package dev.tajim.jarvis.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tajim.jarvis.assistant.AssistantMode
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.HoloButton
import dev.tajim.jarvis.ui.components.JarvisScreenScaffold
import dev.tajim.jarvis.ui.navigation.Routes
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing
import dev.tajim.jarvis.voice.MicPermission

/** Settings home, laid out like the reference: profile card, then rows that open each section. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigate: (String) -> Unit) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val a by viewModel.assistant.collectAsStateWithLifecycle()
    val c = LocalJarvisColors.current
    val micOk = MicPermission.isGranted(LocalContext.current)

    JarvisScreenScaffold(title = "Settings", onBack = null, showsBottomBar = true) {
        Text("System preferences & neural configuration", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)

        GlassCard(Modifier.fillMaxWidth().padding(vertical = Spacing.md)) {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Box(
                        Modifier.size(64.dp).background(c.glassFill, CircleShape).border(2.dp, Brush.linearGradient(listOf(c.cyan, c.magenta)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text((s.displayName.firstOrNull() ?: 'J').uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.textPrimary) }
                    Column {
                        Text(s.displayName.ifBlank { "Your name" }, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                        Text("JARVIS OPERATOR", style = MaterialTheme.typography.labelMedium, color = c.cyan)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Pill(if (micOk) "MIC ALLOWED" else "MIC OFF", micOk)
                    Pill(if (a.mode == AssistantMode.OFF) "ASSISTANT OFF" else "ASSISTANT ON", a.mode != AssistantMode.OFF)
                }
                var draft by remember(s.displayName) { mutableStateOf(s.displayName) }
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it.take(40) }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") }, singleLine = true,
                )
                HoloButton("Save name", { viewModel.setDisplayName(draft) }, enabled = draft.trim() != s.displayName)
            }
        }

        NavRow("Voice & Hands-free", handsFreeSummary(a.mode, s.wakeWordEnabled)) { onNavigate(Routes.S_HANDSFREE) }
        NavRow("Conversation Language", when (s.voiceLanguage) { "bn-BD" -> "Bengali"; "en-US" -> "English"; else -> "Device default" }) { onNavigate(Routes.S_LANGUAGE) }
        NavRow("AI Persona & Voice", "${s.persona.label} • speech ×${"%.1f".format(s.speechRate)}") { onNavigate(Routes.S_PERSONA) }
        NavRow("System & Data Engine", if (!s.hasApiKey) "Gemini • no API key" else "Gemini • ${s.aiModel.ifBlank { "choose a model" }}") { onNavigate(Routes.S_AI) }
        NavRow("Memory Bank", if (s.useMemories) "Used in AI replies" else "Not used in AI replies") { onNavigate(Routes.MEMORY) }
        NavRow("Weather & Location", if (s.useDeviceLocation) "Device location" else s.weatherCity.ifBlank { "Not set" }) { onNavigate(Routes.S_WEATHER) }
        NavRow("Permissions", "Microphone, phone control, notifications…") { onNavigate(Routes.S_PERMISSIONS) }
        NavRow("Appearance", if (s.themeMode == dev.tajim.jarvis.data.ThemeMode.DARK) "Dark holographic" else "Light holographic") { onNavigate(Routes.S_APPEARANCE) }
        NavRow("About JARVIS", "Version, credits, links") { onNavigate(Routes.ABOUT) }
        Box(Modifier.padding(bottom = Spacing.lg))
    }
}

private fun handsFreeSummary(mode: AssistantMode, wake: Boolean) = when (mode) {
    AssistantMode.OFF -> "Background assistant off"
    AssistantMode.ASLEEP -> if (wake) "Sleeping • say “Jarvis”" else "Sleeping"
    AssistantMode.AWAKE -> "Awake • listening"
}

@Composable
private fun Pill(text: String, good: Boolean) {
    val c = LocalJarvisColors.current
    val color = if (good) c.success else c.warning
    Text(
        text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier
            .border(1.dp, color, androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
