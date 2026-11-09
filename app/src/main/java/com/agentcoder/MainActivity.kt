package com.agentcoder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0C0D10)
private val Panel = Color(0xFF15171C)
private val Panel2 = Color(0xFF1D2027)
private val Ink = Color(0xFFF1F2F5)
private val Muted = Color(0xFF8B909D)
private val Accent = Color(0xFFB5F36D)
private val Line = Color(0xFF292D36)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgentCoderApp() }
    }
}

data class Provider(val name: String, val modelCount: String, val color: Color, val glyph: String)

private val providers = listOf(
    Provider("OpenAI", "API compatible", Color(0xFFB8C4D8), "O"),
    Provider("Anthropic", "Claude models", Color(0xFFD7A889), "A"),
    Provider("Google", "Gemini models", Color(0xFF8CB8FF), "G"),
    Provider("DeepSeek", "DeepSeek models", Color(0xFF6CA8FF), "D"),
    Provider("OpenRouter", "Multi-provider", Color(0xFFB69AFF), "R"),
    Provider("Groq", "Fast inference", Color(0xFFFFA66B), "GQ"),
    Provider("Mistral", "Mistral models", Color(0xFFFFC477), "M"),
    Provider("Custom API", "OpenAI-compatible", Color(0xFF9EA5B5), "+")
)

@Composable
fun AgentCoderApp() {
    var selected by remember { mutableStateOf("Workspace") }
    var showAdd by remember { mutableStateOf(false) }
    var providerName by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Accent,
        onBackground = Ink, onSurface = Ink, onPrimary = Bg
    )) {
        Row(Modifier.fillMaxSize().background(Bg)) {
            NavigationRail(selected, onSelect = { selected = it })
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Header()
                Spacer(Modifier.height(18.dp))
                when (selected) {
                    "Workspace" -> WorkspaceScreen()
                    "Providers" -> ProvidersScreen(onAdd = { showAdd = true })
                    "Models" -> ModelsScreen()
                    "API Keys" -> ApiKeysScreen()
                    "Files" -> SimpleScreen("Project files", "Open a project folder to browse and edit source files.")
                    "Terminal" -> SimpleScreen("Terminal", "Build, run and debug tools will execute through the configured runtime.")
                    "Settings" -> SimpleScreen("Settings", "Appearance, agent permissions, runtime and security settings.")
                }
            }
        }
        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                title = { Text("Add custom provider", color = Ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InputField(providerName, { providerName = it }, "Provider name")
                        InputField(baseUrl, { baseUrl = it }, "Base URL")
                        InputField(apiKey, { apiKey = it }, "API key", secret = true)
                        Text("Keys are not persisted in this demo screen. Secure storage is the next implementation step.", color = Muted, fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAdd = false; providerName = ""; baseUrl = ""; apiKey = "" }) {
                        Text("Save provider", color = Accent)
                    }
                },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel", color = Muted) } },
                containerColor = Panel
            )
        }
    }
}

@Composable
fun Header() {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("AGENTCODER", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("AI CODING WORKSPACE", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
        }
        Box(Modifier.background(Color(0xFF202A1A), RoundedCornerShape(30.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("●  AGENT READY", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NavigationRail(selected: String, onSelect: (String) -> Unit) {
    val items = listOf("Workspace", "Providers", "Models", "API Keys", "Files", "Terminal", "Settings")
    Column(
        Modifier.width(76.dp).fillMaxHeight().background(Color(0xFF101216))
            .padding(top = 20.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PixelMark()
        Spacer(Modifier.height(14.dp))
        items.forEachIndexed { index, item ->
            val active = selected == item
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 7.dp)
                    .background(if (active) Panel2 else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(item) }.padding(vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelIcon(index, if (active) Accent else Muted)
                Spacer(Modifier.height(5.dp))
                Text(item.substringBefore(" "), color = if (active) Ink else Muted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
fun PixelMark() {
    Box(Modifier.size(38.dp).background(Accent, RoundedCornerShape(10.dp)), Alignment.Center) {
        Text("A", color = Bg, fontSize = 23.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PixelIcon(index: Int, tint: Color) {
    // Original pixel-inspired icon set drawn from small block glyphs.
    val glyph = listOf("▦", "⌘", "◈", "▣", "▤", ">_", "⚙").getOrElse(index) { "◇" }
    Text(glyph, color = tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        if (action != null) Text(action, color = Accent, fontSize = 12.sp, modifier = Modifier.clickable { onAction?.invoke() })
    }
}

@Composable
fun WorkspaceScreen() {
    var prompt by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Workspace", "Build, edit and debug with your AI agent.")
        CardBlock {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("No project opened", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("Choose a project folder to get started.", color = Muted, fontSize = 11.sp)
                }
                SmallAction("OPEN FOLDER")
            }
        }
        SectionTitle("Agent", "Describe a task. AgentCoder will plan the changes.")
        CardBlock {
            Text("What would you like to build?", color = Ink, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            InputField(prompt, { prompt = it }, "Ask AgentCoder to inspect, create or fix code…", multiline = true)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("＋ Attach files", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                SmallAction("RUN AGENT")
            }
        }
        SectionTitle("Quick tools")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolCard("⌘", "Code edit", Modifier.weight(1f))
            ToolCard("▤", "Build", Modifier.weight(1f))
            ToolCard(">_", "Debug", Modifier.weight(1f))
        }
    }
}

@Composable
fun ProvidersScreen(onAdd: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        SectionTitle("Providers", "Connect and manage AI services.", "+ ADD PROVIDER", onAdd)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(providers) { p ->
                CardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(p.color.copy(alpha = .14f), RoundedCornerShape(11.dp)), Alignment.Center) {
                            Text(p.glyph, color = p.color, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(p.modelCount, color = Muted, fontSize = 10.sp)
                        }
                        Text("CONFIGURE  ›", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ModelsScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        SectionTitle("Model library", "Discover models from connected providers.")
        InputField("", {}, "Search model or provider")
        listOf(
            Triple("GPT-4.1", "OpenAI · Tools · Vision", "GENERAL"),
            Triple("Claude Sonnet", "Anthropic · Reasoning · Tools", "CODING"),
            Triple("Gemini 2.5 Pro", "Google · Vision · Reasoning", "REASONING"),
            Triple("DeepSeek V3", "DeepSeek · Coding · Tools", "CODING")
        ).forEach { (name, detail, tag) ->
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(detail, color = Muted, fontSize = 10.sp)
                    }
                    Text(tag, color = Accent, fontSize = 9.sp)
                }
            }
        }
        Text("Model names shown here are sample entries. Live discovery requires provider credentials and API integration.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
fun ApiKeysScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        SectionTitle("API keys", "Multiple keys per provider. Keep credentials private.", "+ ADD KEY")
        CardBlock {
            Text("SECURITY STATUS", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Secure credential storage is planned with Android Keystore-backed encryption.", color = Ink, fontSize = 12.sp)
        }
        providers.take(4).forEach { p ->
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("No keys added", color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SimpleScreen(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(title, subtitle)
        CardBlock {
            Text("Ready for configuration", color = Ink, fontWeight = FontWeight.SemiBold)
            Text("This workspace module is scaffolded. Connect a runtime to enable execution.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp))
            .padding(14.dp), content = content
    )
}

@Composable
fun ToolCard(icon: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(13.dp)).padding(12.dp)) {
        Text(icon, color = Accent, fontSize = 19.sp)
        Spacer(Modifier.height(7.dp))
        Text(label, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SmallAction(label: String) {
    Box(Modifier.background(Accent, RoundedCornerShape(9.dp)).padding(horizontal = 11.dp, vertical = 9.dp)) {
        Text(label, color = Bg, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    multiline: Boolean = false,
    secret: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().background(Panel2, RoundedCornerShape(10.dp)).padding(12.dp),
        textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 12.sp),
        singleLine = !multiline,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, color = Muted, fontSize = 12.sp)
                inner()
            }
        }
    )
}
