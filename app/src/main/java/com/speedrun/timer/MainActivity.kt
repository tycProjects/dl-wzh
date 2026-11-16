package com.speedrun.timer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFeature
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            var overlayEnabled by remember { mutableStateOf(false) }
            val prefs = remember { getSharedPreferences("appearance", MODE_PRIVATE) }
            var accent by remember { mutableStateOf("Roxo") }
            var appTheme by remember { mutableStateOf(prefs.getString("app_theme", "Sistema") ?: "Sistema") }
            var timerStyle by remember { mutableStateOf(prefs.getString("timer_style", "Padrão") ?: "Padrão") }
            SpeedrunTheme(accent, appTheme) {
                SpeedrunApp(
                    overlayEnabled = overlayEnabled,
                    onOverlayChange = { enabled ->
                        if (enabled) {
                            if (!Settings.canDrawOverlays(this)) {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                )
                                overlayEnabled = false
                            } else {
                                val serviceIntent = Intent(this, OverlayTimerService::class.java).setAction(OverlayTimerService.ACTION_START)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(serviceIntent)
                                } else {
                                    startService(serviceIntent)
                                }
                                overlayEnabled = true
                            }
                        } else {
                            stopService(Intent(this, OverlayTimerService::class.java))
                            overlayEnabled = false
                        }
                    },
                    accent = accent,
                    onAccentChange = { accent = it },
                    appTheme = appTheme,
                    onAppThemeChange = { appTheme = it; prefs.edit().putString("app_theme", it).apply() },
                    timerStyle = timerStyle,
                    onTimerStyleChange = {
                        timerStyle = it
                        prefs.edit().putString("timer_style", it).apply()
                        if (overlayEnabled) {
                            val refresh = Intent(this, OverlayTimerService::class.java).setAction(OverlayTimerService.ACTION_REFRESH_STYLE)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(refresh) else startService(refresh)
                        }
                    },
                    onOverlayCommand = { action ->
                        val command = Intent(this, OverlayTimerService::class.java).setAction(action)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(command)
                        else startService(command)
                    }
                )
            }
        }
    }
}

@Composable
fun SpeedrunApp(
    overlayEnabled: Boolean,
    onOverlayChange: (Boolean) -> Unit,
    accent: String,
    onAccentChange: (String) -> Unit,
    appTheme: String,
    onAppThemeChange: (String) -> Unit,
    timerStyle: String,
    onTimerStyleChange: (String) -> Unit,
    onOverlayCommand: (String) -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var timerOn by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(timerOn) {
        while (timerOn) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(25)
        }
    }

    if (settingsOpen) {
        SettingsScreen(
            overlayEnabled = overlayEnabled,
            onOverlayChange = onOverlayChange,
            accent = accent,
            onAccentChange = onAccentChange,
            appTheme = appTheme,
            onAppThemeChange = onAppThemeChange,
            timerStyle = timerStyle,
            onTimerStyleChange = onTimerStyleChange,
            onBack = { settingsOpen = false }
        )
    } else {
        HomeScreen(
            timerOn = timerOn,
            onTimerChange = { enabled ->
                if (enabled && !timerOn) {
                    startedAt = System.currentTimeMillis() - elapsed
                    timerOn = true
                    onOverlayChange(true)
                } else if (!enabled && timerOn) {
                    elapsed = System.currentTimeMillis() - startedAt
                    timerOn = false
                    onOverlayChange(false)
                }
            },
            onStop = {
                if (timerOn) {
                    elapsed = System.currentTimeMillis() - startedAt
                    timerOn = false
                    if (overlayEnabled) onOverlayCommand(OverlayTimerService.ACTION_STOP)
                }
            },
            onReset = {
                timerOn = false
                elapsed = 0L
                startedAt = 0L
                if (overlayEnabled) onOverlayCommand(OverlayTimerService.ACTION_RESET)
            },
            onSettings = { settingsOpen = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    timerOn: Boolean,
    onTimerChange: (Boolean) -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speedrun Timer", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Timer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = timerOn, onCheckedChange = onTimerChange)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Stop Timer", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Reset Timer", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Configurações e customização")
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    overlayEnabled: Boolean,
    onOverlayChange: (Boolean) -> Unit,
    accent: String,
    onAccentChange: (String) -> Unit,
    appTheme: String,
    onAppThemeChange: (String) -> Unit,
    timerStyle: String,
    onTimerStyleChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações e customização") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Sobreposição", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Timer flutuante", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Exibir o cronômetro sobre outros aplicativos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = overlayEnabled, onCheckedChange = onOverlayChange)
                }
            }
            Text("Temas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Escolha a aparência do aplicativo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("Sistema", "Escuro", "Branco").forEach { option ->
                Surface(onClick = { onAppThemeChange(option) }, shape = RoundedCornerShape(18.dp), color = if (appTheme == option) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(option, modifier = Modifier.weight(1f))
                        RadioButton(selected = appTheme == option, onClick = { onAppThemeChange(option) })
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Personalização do timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Escolha o visual da sobreposição.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("Padrão" to "Visual atual, com fundo arredondado.", "Texto" to "Somente os números e os ícones, sem fundo.", "Liquid Glass" to "Efeito de vidro translúcido; timer ativo em azul.").forEach { (option, description) ->
                Surface(onClick = { onTimerStyleChange(option) }, shape = RoundedCornerShape(18.dp), color = if (timerStyle == option) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(option, style = MaterialTheme.typography.titleSmall)
                            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = timerStyle == option, onClick = { onTimerStyleChange(option) })
                    }
                }
            }
            Text("Cor do aplicativo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf("Roxo", "Azul", "Verde").forEach { option ->
                Surface(
                    onClick = { onAccentChange(option) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (accent == option) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(option, modifier = Modifier.weight(1f))
                        RadioButton(selected = accent == option, onClick = { onAccentChange(option) })
                    }
                }
            }
            Text(
                "A sobreposição exige a permissão “Exibir sobre outros apps” do Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            KeyboardShortcutsSettings(overlayEnabled = overlayEnabled)
        }
    }
}


@Composable
fun KeyboardShortcutsSettings(overlayEnabled: Boolean) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("keyboard_shortcuts", android.content.Context.MODE_PRIVATE) }
    val actions = listOf(
        "pause" to "Pausar / continuar timer",
        "reset" to "Zerar timer",
        "home" to "Voltar ao aplicativo",
        "close" to "Fechar sobreposição"
    )
    var keys by remember {
        mutableStateOf(actions.associate { (id, _) -> id to prefs.getInt("key_$id", KeyEvent.KEYCODE_UNKNOWN) })
    }
    var capturing by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Keyboard, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Configurações de teclado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Configure atalhos para um teclado Bluetooth. Toque em uma função e pressione a tecla desejada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        actions.forEach { (id, label) ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        val code = keys[id] ?: KeyEvent.KEYCODE_UNKNOWN
                        Text(
                            if (code == KeyEvent.KEYCODE_UNKNOWN) "Nenhuma tecla definida" else KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { capturing = id }) { Text("Definir") }
                    IconButton(onClick = {
                        prefs.edit().remove("key_$id").apply()
                        keys = keys.toMutableMap().also { it[id] = KeyEvent.KEYCODE_UNKNOWN }
                        if (overlayEnabled) context.startService(Intent(context, OverlayTimerService::class.java).setAction(OverlayTimerService.ACTION_REFRESH_KEYS))
                    }) { Icon(Icons.Default.Delete, contentDescription = "Remover atalho") }
                }
            }
        }
        if (capturing != null) {
            Dialog(onDismissRequest = { capturing = null }) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp)
                            .focusRequester(focusRequester)
                            .focusable()
                            .onPreviewKeyEvent { event ->
                                val native = event.nativeKeyEvent
                                if (native.action == KeyEvent.ACTION_DOWN && native.keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                                    val id = capturing
                                    if (id != null) {
                                        val editor = prefs.edit().putInt("key_$id", native.keyCode)
                                        actions.filter { it.first != id }.forEach { (otherId, _) ->
                                            if (prefs.getInt("key_$otherId", KeyEvent.KEYCODE_UNKNOWN) == native.keyCode) {
                                                editor.remove("key_$otherId")
                                            }
                                        }
                                        editor.apply()
                                        keys = actions.associate { (actionId, _) ->
                                            actionId to prefs.getInt("key_$actionId", KeyEvent.KEYCODE_UNKNOWN)
                                        }
                                        if (overlayEnabled) context.startService(Intent(context, OverlayTimerService::class.java).setAction(OverlayTimerService.ACTION_REFRESH_KEYS))
                                    }
                                    capturing = null
                                    true
                                } else false
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Pressione uma tecla", style = MaterialTheme.typography.titleLarge)
                        Text("Aguardando a tecla do teclado Bluetooth…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { capturing = null }) { Text("Cancelar") }
                    }
                }
            }
            LaunchedEffect(capturing) { if (capturing != null) focusRequester.requestFocus() }
        }
        Text(
            "Para receber atalhos enquanto outro aplicativo está aberto, a sobreposição precisa manter o foco do teclado. Isso pode impedir a digitação nesse outro aplicativo enquanto os atalhos estiverem ativos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0)
    val minutes = safe / 60000
    val seconds = (safe / 1000) % 60
    val centis = (safe % 1000) / 10
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
}

@Composable
fun SpeedrunTheme(accent: String, appTheme: String, content: @Composable () -> Unit) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (appTheme) { "Escuro" -> true; "Branco" -> false; else -> systemDark }
    val primary = when (accent) { "Azul" -> Color(0xFF386A9B); "Verde" -> Color(0xFF496B50); else -> Color(0xFF6750A4) }
    val scheme = if (dark) darkColorScheme(primary = primary, secondary = primary, background = Color(0xFF121116), surface = Color(0xFF121116))
    else lightColorScheme(primary = primary, onPrimary = Color.White, secondary = primary, background = Color(0xFFF8F6FC), surface = Color(0xFFF8F6FC))
    MaterialTheme(colorScheme = scheme, content = content)
}
