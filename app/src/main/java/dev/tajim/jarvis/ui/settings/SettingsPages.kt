package dev.tajim.jarvis.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tajim.jarvis.assistant.AssistantMode
import dev.tajim.jarvis.assistant.AssistantService
import dev.tajim.jarvis.data.Persona
import dev.tajim.jarvis.data.ThemeMode
import dev.tajim.jarvis.phone.JarvisAccessibilityService
import dev.tajim.jarvis.ui.components.HoloButton
import dev.tajim.jarvis.ui.components.StatusDot
import dev.tajim.jarvis.ui.components.StatusKind
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.MinTouch
import dev.tajim.jarvis.ui.theme.Spacing
import dev.tajim.jarvis.voice.MicPermission

// ------------------------------------------------------------------ Voice & hands-free
@Composable
fun HandsFreePage(vm: SettingsViewModel, onBack: () -> Unit, onOpenPermissions: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val a by vm.assistant.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val c = LocalJarvisColors.current
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { vm.setAssistantEnabled(true); AssistantService.start(ctx) }
    }
    SettingsPage("Voice & Hands-free", onBack) {
        SectionLabel("Background assistant")
        SectionCard {
            ToggleRow(
                "Run JARVIS in the background",
                "Keeps a notification on while it runs. It starts asleep and wakes when you say “Jarvis”.",
                checked = s.assistantEnabled,
            ) { on ->
                if (on) {
                    if (MicPermission.isGranted(ctx)) { vm.setAssistantEnabled(true); AssistantService.start(ctx) }
                    else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else { vm.setAssistantEnabled(false); AssistantService.stop(ctx) }
            }
            Text(
                "Status: " + when (a.mode) { AssistantMode.OFF -> "off"; AssistantMode.ASLEEP -> "sleeping, waiting for “Jarvis”"; AssistantMode.AWAKE -> "awake, listening for commands" },
                style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
            )
            a.notice?.let { Text(it.text, style = MaterialTheme.typography.bodyMedium, color = c.warning) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                HoloButton("Wake now", vm::wakeNow, enabled = a.mode == AssistantMode.ASLEEP)
                HoloButton("Sleep now", vm::sleepNow, enabled = a.mode == AssistantMode.AWAKE)
            }
        }

        SectionLabel("Wake word and sleep word")
        SectionCard {
            ToggleRow("Wake word “Jarvis”", "Heard offline on this phone. Audio is not stored or sent. Always-on listening uses battery.", s.wakeWordEnabled, vm::setWakeWordEnabled)
            val bundled = remember { vm.wakeWordModelBundled() }
            Text(
                if (bundled) "Wake-word model: found." else "Wake-word model: missing. Copy assets/model-en-us into the app (see README), then rebuild. Until then use Wake now or tap the orb.",
                style = MaterialTheme.typography.bodyMedium, color = if (bundled) c.success else c.warning,
            )
            Text("Sleep word: say “sleep”, “go to sleep” or “ঘুমাও” while awake. Three silent listens in a row also put it to sleep.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }

        SectionLabel("What you can say")
        SectionCard {
            Text(
                "“Open WhatsApp” • “Go home” • “Go back” • “Lock the phone” • “Unlock” • “Scroll down” • “Volume up” • “Flashlight on” • " +
                    "“Call Rahim” • “Message Rahim saying I am late” • “Search for pizza” • “What’s the weather” • “What time is it” • anything else goes to the AI.",
                style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
            )
            Text("Phone control and background use need extra permissions.", style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            HoloButton("Open permissions", onOpenPermissions)
            Text("Unlock: JARVIS can wake the screen and swipe away a plain lock screen. It cannot and will not enter your PIN, pattern or password.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
    }
}

// ------------------------------------------------------------------ Language
@Composable
fun LanguagePage(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = LocalJarvisColors.current
    SettingsPage("Conversation Language", onBack) {
        SectionLabel("Speech recognition language")
        SectionCard {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("" to "Device default", "en-US" to "English", "bn-BD" to "Bengali").forEach { (tag, label) ->
                    FilterChip(selected = s.voiceLanguage == tag, onClick = { vm.setVoiceLanguage(tag) }, label = { Text(label) }, modifier = Modifier.heightIn(min = MinTouch))
                }
            }
            Text(
                "Bengali recognition needs internet or an installed Bengali language pack. AI answers follow the language you speak or type. The wake word “Jarvis” is always English.",
                style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
            )
        }
    }
}

// ------------------------------------------------------------------ Persona & voice
@Composable
fun PersonaPage(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = LocalJarvisColors.current
    SettingsPage("AI Persona & Voice", onBack) {
        SectionLabel("Persona")
        SectionCard {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Persona.entries.forEach { p ->
                    FilterChip(selected = s.persona == p, onClick = { vm.setPersona(p) }, label = { Text(p.label) }, modifier = Modifier.heightIn(min = MinTouch))
                }
            }
            var draft by remember(s.customInstructions) { mutableStateOf(s.customInstructions) }
            OutlinedTextField(draft, { draft = it.take(600) }, Modifier.fillMaxWidth(), label = { Text("Custom response instructions") }, minLines = 2)
            HoloButton("Save instructions", { vm.setCustomInstructions(draft) }, enabled = draft != s.customInstructions)
            Text("A persona only changes tone. JARVIS always says it is an AI.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        SectionLabel("Speech output")
        SectionCard {
            ToggleRow("Speak replies aloud", "Uses your phone's text-to-speech voice.", s.speakReplies, vm::setSpeakReplies)
            var rate by remember(s.speechRate) { mutableFloatStateOf(s.speechRate) }
            Text("Speech rate ×${"%.1f".format(rate)}", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Slider(rate, { rate = it }, valueRange = 0.5f..2f, onValueChangeFinished = { vm.setSpeechRate(rate) })
            HoloButton("Test voice", vm::testVoice)
            Text("Change the voice itself in Android Settings, Text-to-speech.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
    }
}

// ------------------------------------------------------------------ AI provider
@Composable
fun AiProviderPage(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = LocalJarvisColors.current
    SettingsPage("System & Data Engine", onBack) {
        SectionLabel("Provider: Gemini")
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusDot(if (s.hasApiKey) StatusKind.OK else StatusKind.WARN)
                Text(if (s.hasApiKey) "API key saved (encrypted on this phone)" else "No API key saved", style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            }
            var key by remember { mutableStateOf("") }
            OutlinedTextField(
                key, { key = it }, Modifier.fillMaxWidth(), label = { Text("Gemini API key") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                HoloButton("Save key", { vm.saveApiKey(key); key = "" }, enabled = key.isNotBlank())
                HoloButton("Remove key", vm::removeApiKey, enabled = s.hasApiKey)
            }
            Text(
                "The key stays on this phone and is sent only to Google's Gemini API. It is never shown again, logged or exported. Anyone with the key can use your quota, so keep it private.",
                style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
            )
        }
        SectionLabel("Model")
        SectionCard {
            Text("Selected: " + s.aiModel.ifBlank { "none" }, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            HoloButton("Load models for my key", vm::loadModels, enabled = s.hasApiKey)
            when (val m = vm.models) {
                ModelsUi.Idle -> {}
                ModelsUi.Loading -> Text("Loading…", color = c.textSecondary)
                is ModelsUi.Error -> Text(m.message, color = c.error, style = MaterialTheme.typography.bodyMedium)
                is ModelsUi.Ready -> Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                    m.models.forEach { name ->
                        Text(
                            (if (name == s.aiModel) "✓  " else "    ") + name,
                            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch).clickable { vm.selectModel(name) }.padding(vertical = 12.dp),
                            color = if (name == s.aiModel) c.cyan else c.textPrimary, style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Text("The list comes live from Google, so only models your key can really use appear.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        SectionLabel("Connection")
        SectionCard {
            HoloButton("Test connection", vm::testConnection, enabled = s.hasApiKey && s.aiModel.isNotBlank())
            vm.testResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (it.startsWith("Connected")) c.success else c.textSecondary) }
        }
    }
}

// ------------------------------------------------------------------ Weather & location
@Composable
fun WeatherPage(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = LocalJarvisColors.current
    val ctx = LocalContext.current
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.setUseDeviceLocation(true) }
    SettingsPage("Weather & Location", onBack) {
        SectionLabel("Where is the weather for?")
        SectionCard {
            ToggleRow("Use my device location", "Approximate location, read only when Home opens. Needs Location permission.", s.useDeviceLocation) { on ->
                if (!on) vm.setUseDeviceLocation(false)
                else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) vm.setUseDeviceLocation(true)
                else locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (!s.useDeviceLocation) Text("Using: " + s.weatherCity.ifBlank { "no city chosen" }, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
        SectionLabel("Or choose a city")
        SectionCard {
            var q by remember { mutableStateOf("") }
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("City name") }, singleLine = true)
            HoloButton("Search", { vm.searchCity(q) }, enabled = q.isNotBlank())
            when (val r = vm.cities) {
                CitiesUi.Idle -> {}
                CitiesUi.Loading -> Text("Searching…", color = c.textSecondary)
                is CitiesUi.Error -> Text(r.message, color = c.error, style = MaterialTheme.typography.bodyMedium)
                is CitiesUi.Results -> r.places.forEach { p ->
                    Text(
                        p.name + if (p.detail.isNotBlank()) ", ${p.detail}" else "",
                        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch).clickable { vm.pickCity(p) }.padding(vertical = 12.dp),
                        color = c.cyan, style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Text("Weather and city search come from Open-Meteo (no account needed).", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
    }
}

// ------------------------------------------------------------------ Permissions
@Composable
fun PermissionsPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalJarvisColors.current
    val a11y by JarvisAccessibilityService.connected.collectAsStateWithLifecycle()
    fun ignoringBattery() = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    var battery by remember { mutableStateOf(ignoringBattery()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { battery = ignoringBattery() }

    SettingsPage("Permissions", onBack) {
        Text("Nothing is requested until you tap Allow. You can revoke any of these in Android settings.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        SectionLabel("Voice and background")
        SectionCard {
            PermissionRow("Microphone", "Listening for you and the wake word.", Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) PermissionRow("Notifications", "Shows the “JARVIS is running” notification.", Manifest.permission.POST_NOTIFICATIONS)
            StatusLine("Run without battery limits", "Stops Android from putting JARVIS to sleep.", battery)
            if (!battery) HoloButton("Allow in background", {
                ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName)))
            })
        }
        SectionLabel("Phone control")
        SectionCard {
            StatusLine("Accessibility service", "Lets JARVIS press, scroll, go Home/Back, lock and type when you ask. Android only lets you switch it on yourself.", a11y)
            HoloButton(if (a11y) "Manage in Accessibility settings" else "Open Accessibility settings", { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
            if (!a11y) Text("Find JARVIS in the list, switch it on and confirm.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            PermissionRow("Contacts", "Finds the person when you say “call Rahim”.", Manifest.permission.READ_CONTACTS)
            PermissionRow("Phone calls", "Places a call, only after you say yes.", Manifest.permission.CALL_PHONE)
        }
        SectionLabel("Weather")
        SectionCard { PermissionRow("Location", "Approximate location for live weather.", Manifest.permission.ACCESS_COARSE_LOCATION) }
    }
}

@Composable
private fun StatusLine(title: String, why: String, ok: Boolean) {
    val c = LocalJarvisColors.current
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatusDot(if (ok) StatusKind.OK else StatusKind.WARN, Modifier.padding(top = 6.dp))
        Column(Modifier.weight(1f)) {
            Text(title + if (ok) " — on" else " — off", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Text(why, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
    }
}

@Composable
private fun PermissionRow(title: String, why: String, permission: String) {
    val ctx = LocalContext.current
    fun has() = ContextCompat.checkSelfPermission(ctx, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(has()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = has() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    StatusLine(title, why, granted)
    if (!granted) HoloButton("Allow $title", { launcher.launch(permission) })
}

// ------------------------------------------------------------------ Appearance
@Composable
fun AppearancePage(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    SettingsPage("Appearance", onBack) {
        SectionLabel("Theme")
        SectionCard {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(s.themeMode == ThemeMode.DARK, { vm.setThemeMode(ThemeMode.DARK) }, { Text("Dark holographic") }, Modifier.heightIn(min = MinTouch))
                FilterChip(s.themeMode == ThemeMode.LIGHT, { vm.setThemeMode(ThemeMode.LIGHT) }, { Text("Light holographic") }, Modifier.heightIn(min = MinTouch))
            }
            ToggleRow("Reduce motion", "Stops orb animation and screen transitions", s.reducedMotion, vm::setReducedMotion)
            var intensity by remember(s.animationIntensity) { mutableFloatStateOf(s.animationIntensity) }
            Text("Animation intensity", style = MaterialTheme.typography.titleMedium, color = LocalJarvisColors.current.textPrimary)
            Slider(intensity, { intensity = it }, valueRange = 0.25f..1f, enabled = !s.reducedMotion, onValueChangeFinished = { vm.setAnimationIntensity(intensity) })
        }
    }
}
