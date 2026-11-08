package com.kian.samsungfartomizer

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import android.content.IntentFilter
import android.provider.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

private val SamsungBlue = Color(0xFF3E91FF)
private val Bg = Color(0xFF0B0F14)
private val CardBg = Color(0xFF1B2129)
private val Muted = Color(0xFF8A94A0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(primary = SamsungBlue, background = Bg, surface = CardBg)
            ) { Screen() }
        }
    }
}

@Composable
fun Screen() {
    val scope = rememberCoroutineScope()
    var up by remember { mutableStateOf(Sh.up()) }
    var ok by remember { mutableStateOf(Sh.granted()) }
    var log by remember { mutableStateOf("Ready.") }
    var phantomOff by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    var pkg by remember { mutableStateOf<String?>(null) }
    var pkgLabel by remember { mutableStateOf("Choose an app") }
    var showPicker by remember { mutableStateOf(false) }
    var revertSecs by remember { mutableStateOf(0) }
    val apps = remember {
        val pm = ctx.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
    }
    fun ram(): String {
        val mi = ActivityManager.MemoryInfo()
        (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        return "RAM free: ${mi.availMem shr 20} MB of ${mi.totalMem shr 20} MB"
    }
    var ramText by remember { mutableStateOf(ram()) }

    var tab by remember { mutableStateOf(0) }
    var pickGame by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf("") }
    var bRam by remember { mutableStateOf(true) }
    var bKill by remember { mutableStateOf(false) }
    var bDnd by remember { mutableStateOf(false) }
    var bHz by remember { mutableStateOf(false) }
    val prefs = remember { ctx.getSharedPreferences("games", Context.MODE_PRIVATE) }
    var games by remember { mutableStateOf(prefs.getStringSet("list", emptySet())!!.toList()) }
    var gosOff by remember { mutableStateOf(prefs.getBoolean("gos", false)) }
    val presets = remember {
        mutableStateMapOf<String, String>().apply {
            games.forEach { put(it, prefs.getString("p:$it", "High") ?: "High") }
        }
    }
    fun label(id: String) = apps.firstOrNull { it.second == id }?.first ?: id
    fun addGame(id: String) {
        if (id !in games) {
            games = games + id
            presets[id] = "High"
            prefs.edit().putStringSet("list", games.toSet()).apply()
        }
    }
    fun launchGame(id: String) {
        scope.launch {
            val steps = mutableListOf(presetCmd(id, presets[id] ?: "High"), "am set-standby-bucket $id active")
            if (bKill) {
                val keep = setOf(
                    id, ctx.packageName, "moe.shizuku.privileged.api",
                    ctx.packageManager.resolveActivity(
                        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                        PackageManager.MATCH_DEFAULT_ONLY
                    )?.activityInfo?.packageName,
                    Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')
                )
                Sh.run("pm list packages -3").text.lines()
                    .map { it.removePrefix("package:").trim() }
                    .filter { it.isNotEmpty() && it !in keep }
                    .forEach { steps += "am force-stop $it" }
            }
            if (bRam) steps += "am kill-all"
            if (bDnd) steps += "cmd notification set_dnd priority"
            if (bHz) steps += "settings put system min_refresh_rate 120.0; settings put system peak_refresh_rate 120.0"
            val r = Sh.run(steps.joinToString("; "))
            log = if (r.code == 0) "Launching ${label(id)} (${presets[id] ?: "High"})" else "Launch boost: ${r.text}"
            ctx.packageManager.getLaunchIntentForPackage(id)?.let {
                ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ramText = ram()
        }
    }
    LaunchedEffect(tab) {
        while (tab == 1) {
            val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val t = (i?.getIntExtra("temperature", 0) ?: 0) / 10.0
            stats = "Battery ${i?.getIntExtra("level", 0) ?: 0}%  |  $t C"
            ramText = ram()
            delay(2000)
        }
    }

    fun applyScale(pct: Int) {
        scope.launch {
            if (pct == 100) {
                val r = Sh.run("wm size reset; wm density reset")
                log = if (r.code == 0) "Resolution reset" else "Reset failed: ${r.text}"
                return@launch
            }
            val s = Regex("Physical size: (\\d+)x(\\d+)").find(Sh.run("wm size").text)
            val d = Regex("Physical density: (\\d+)").find(Sh.run("wm density").text)
            if (s == null || d == null) { log = "Couldn't read screen size"; return@launch }
            val w = s.groupValues[1].toInt() * pct / 100 / 2 * 2
            val h = s.groupValues[2].toInt() * pct / 100 / 2 * 2
            val dn = d.groupValues[1].toInt() * pct / 100
            Sh.run("wm size ${w}x$h; wm density $dn")
            log = "Set ${w}x$h at $dn dpi"
            revertSecs = 10
            while (revertSecs > 0) { delay(1000); if (revertSecs > 0) revertSecs-- }
            if (revertSecs == 0) { Sh.run("wm size reset; wm density reset"); log = "Reverted to original resolution" }
            else { revertSecs = 0; log = "Resolution kept: ${w}x$h" }
        }
    }

    DisposableEffect(Unit) {
        val onBinder = Shizuku.OnBinderReceivedListener { up = true; ok = Sh.granted() }
        val onDead = Shizuku.OnBinderDeadListener { up = false; ok = false }
        val onPerm = Shizuku.OnRequestPermissionResultListener { _, result ->
            ok = result == PackageManager.PERMISSION_GRANTED
        }
        Shizuku.addBinderReceivedListenerSticky(onBinder)
        Shizuku.addBinderDeadListener(onDead)
        Shizuku.addRequestPermissionResultListener(onPerm)
        onDispose {
            Shizuku.removeBinderReceivedListener(onBinder)
            Shizuku.removeBinderDeadListener(onDead)
            Shizuku.removeRequestPermissionResultListener(onPerm)
        }
    }

    fun exec(label: String, cmd: String) {
        scope.launch {
            log = "Running: $label..."
            val r = Sh.run(cmd)
            log = if (r.code == 0) "$label: done" + (if (r.text.isNotEmpty()) "\n${r.text}" else "")
            else "$label: failed (code ${r.code})\n${r.text}"
            ramText = ram()
        }
    }

    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Fartomizer", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Phone & Game Tuner  |  by Kian", color = SamsungBlue, fontSize = 16.sp)

        // Status
        Section("Shizuku") {
            val status = when {
                !up -> "Not running. Install Shizuku and start it (Wireless debugging), then come back."
                !ok -> "Running, but this app has no permission yet."
                else -> "Connected and permission granted."
            }
            Text(status, color = Muted, fontSize = 13.sp)
            if (up && !ok) {
                Button(
                    onClick = {
                        try {
                            if (Shizuku.isPreV11()) log = "Your Shizuku is too old, update it."
                            else Shizuku.requestPermission(100)
                        } catch (t: Throwable) { log = t.message ?: "Error" }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue),
                    shape = RoundedCornerShape(20.dp)
                ) { Text("Grant permission") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipBtn("Phone", tab == 0) { tab = 0 }
            ChipBtn("Games", tab == 1) { tab = 1 }
        }

        if (tab == 0) {
        // Refresh rate
        Section("Refresh rate") {
            Text("Locks the screen refresh range. Samsung's own Motion smoothness setting can still override it on some models.", color = Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120).forEach { hz ->
                    OutlinedButton(
                        onClick = {
                            exec(
                                "$hz Hz",
                                "settings put system min_refresh_rate $hz.0; settings put system peak_refresh_rate $hz.0"
                            )
                        },
                        enabled = ok,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("$hz") }
                }
            }
            OutlinedButton(
                onClick = {
                    exec("Refresh rate reset", "settings delete system min_refresh_rate; settings delete system peak_refresh_rate")
                },
                enabled = ok,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Back to default") }
        }

        // Phantom process killer
        Section("Background apps") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Disable phantom process limit", color = Color.White)
                    Text("Stops Android from killing background child processes (Android 12+).", color = Muted, fontSize = 12.sp)
                }
                Switch(
                    checked = phantomOff,
                    enabled = ok,
                    onCheckedChange = { on ->
                        phantomOff = on
                        if (on) exec(
                            "Phantom limit off",
                            "device_config set_sync_disabled_for_tests persistent; " +
                                "device_config put activity_manager max_phantom_processes 2147483647; " +
                                "settings put global settings_enable_monitor_phantom_procs false"
                        ) else exec(
                            "Phantom limit restored",
                            "device_config delete activity_manager max_phantom_processes; " +
                                "settings put global settings_enable_monitor_phantom_procs true; " +
                                "device_config set_sync_disabled_for_tests none"
                        )
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = SamsungBlue)
                )
            }
        }

        // Animations
        Section("Animation speed") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Off" to "0", "0.5x" to "0.5", "1x" to "1").forEach { (label, v) ->
                    OutlinedButton(
                        onClick = {
                            exec(
                                "Animations $label",
                                "settings put global window_animation_scale $v; " +
                                    "settings put global transition_animation_scale $v; " +
                                    "settings put global animator_duration_scale $v"
                            )
                        },
                        enabled = ok,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text(label) }
                }
            }
        }

        // Clean RAM
        Section("Memory") {
            Text(ramText, color = Muted, fontSize = 12.sp)
            Button(
                onClick = { exec("Clear background apps", "am kill-all") },
                enabled = ok,
                colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Clear background apps", fontWeight = FontWeight.SemiBold) }
        }

        Section("Resolution") {
            Text("Lower resolution can raise FPS in games. Size and density are scaled together, and it auto-reverts in 10 s unless you tap Keep.", color = Muted, fontSize = 12.sp)
            if (revertSecs > 0) {
                Text("Reverting in $revertSecs s...", color = Color.White)
                Button(
                    onClick = { revertSecs = -1 },
                    colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Keep this resolution") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 90, 80, 70).forEach { pct ->
                    OutlinedButton(
                        onClick = { applyScale(pct) },
                        enabled = ok && revertSecs <= 0,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("$pct%") }
                }
            }
        }

        Section("App tools") {
            OutlinedButton(
                onClick = { pickGame = false; showPicker = true },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(pkgLabel) }
            val p = pkg
            val on = ok && p != null
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tool("Game mode", on) { exec("Game mode: performance", "cmd game mode performance $p") }
                Tool("Reset mode", on) { exec("Game mode reset", "cmd game reset $p") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tool("Optimize", on) { exec("Optimize $pkgLabel", "cmd package compile -m speed-profile -f $p") }
                Tool("Force stop", on) { exec("Force stop $pkgLabel", "am force-stop $p") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tool("Keep alive", on) { exec("Keep alive", "cmd deviceidle whitelist +$p") }
                Tool("Undo alive", on) { exec("Keep alive undone", "cmd deviceidle whitelist -$p") }
            }
            OutlinedButton(
                onClick = { exec("Optimize all apps", "cmd package bg-dexopt-job") },
                enabled = ok,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Optimize all apps (dexopt)") }
        }

        Section("Private DNS") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tool("Off", ok) { exec("DNS off", "settings put global private_dns_mode off") }
                Tool("Auto", ok) { exec("DNS auto", "settings put global private_dns_mode opportunistic") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tool("Google", ok) { exec("DNS Google", "settings put global private_dns_mode hostname; settings put global private_dns_specifier dns.google") }
                Tool("Cloudflare", ok) { exec("DNS Cloudflare", "settings put global private_dns_mode hostname; settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com") }
            }
        }

        }

        if (tab == 1) {
            Section("Live stats") {
                Text(stats, color = Color.White)
                Text(ramText, color = Muted, fontSize = 12.sp)
            }
            Section("Launch boost") {
                Text("Applied when you tap Launch on a game. End session undoes them.", color = Muted, fontSize = 12.sp)
                BoostRow("Clear background apps", bRam) { bRam = it }
                BoostRow("Force-stop other apps (may close VPN, music)", bKill) { bKill = it }
                BoostRow("Do Not Disturb", bDnd) { bDnd = it }
                BoostRow("Max refresh rate (120 Hz)", bHz) { bHz = it }
                OutlinedButton(
                    onClick = { exec("End session", "cmd notification set_dnd off; settings delete system min_refresh_rate; settings delete system peak_refresh_rate") },
                    enabled = ok,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("End session (restore)") }
            }
            Section("Samsung") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Disable Game Optimizing Service", color = Color.White)
                        Text("Stops Samsung's GOS from capping apps. GOS exists to limit heat, so expect a warmer phone. Turn off to restore.", color = Muted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = gosOff,
                        enabled = ok,
                        onCheckedChange = { on ->
                            gosOff = on
                            prefs.edit().putBoolean("gos", on).apply()
                            if (on) exec("GOS disabled", "pm disable-user --user 0 com.samsung.android.game.gos")
                            else exec("GOS restored", "pm enable com.samsung.android.game.gos")
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = SamsungBlue)
                    )
                }
            }
            Button(
                onClick = { pickGame = true; showPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("+ Add game", fontWeight = FontWeight.SemiBold) }
            if (games.isEmpty()) Text("No games yet. Add one to set its graphics preset and launch it with boosts.", color = Muted, fontSize = 13.sp)
            games.sortedBy { label(it).lowercase() }.forEach { id ->
                Section(label(id)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("High", "Medium", "Low", "Ultra Low").forEach { name ->
                            ChipBtn(name, presets[id] == name) {
                                presets[id] = name
                                prefs.edit().putString("p:$id", name).apply()
                                if (ok) exec("${label(id)}: $name graphics", presetCmd(id, name))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tool("Launch", ok) { launchGame(id) }
                        Tool("Optimize", ok) { exec("Optimize ${label(id)}", "cmd package compile -m speed-profile -f $id") }
                        Tool("Remove", true) {
                            games = games - id
                            presets.remove(id)
                            prefs.edit().putStringSet("list", games.toSet()).apply()
                            if (ok) exec("Reset ${label(id)}", "cmd game reset $id")
                        }
                    }
                }
            }
        }

        if (showPicker) AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = { TextButton(onClick = { showPicker = false }) { Text("Close") } },
            title = { Text("Choose app") },
            text = {
                LazyColumn {
                    items(apps) { (label, id) ->
                        Text(
                            label,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { if (pickGame) addGame(id) else { pkg = id; pkgLabel = label }; showPicker = false }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        )

        Section("Output") { Text(log, color = Muted, fontSize = 12.sp) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            content()
        }
    }
}

@Composable
private fun RowScope.Tool(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.weight(1f)
    ) { Text(text, fontSize = 12.sp, maxLines = 1) }
}

private fun presetCmd(id: String, preset: String) = when (preset) {
    "High" -> "cmd game reset $id; cmd game mode standard $id"
    "Medium" -> "cmd game set --mode performance --downscale 0.8 $id"
    "Low" -> "cmd game set --mode performance --downscale 0.6 $id"
    else -> "cmd game set --mode performance --downscale 0.4 $id"
}

@Composable
private fun RowScope.ChipBtn(text: String, selected: Boolean, onClick: () -> Unit) {
    val m = Modifier.weight(1f)
    if (selected) Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = m
    ) { Text(text, fontSize = 11.sp, maxLines = 1) }
    else OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = m
    ) { Text(text, fontSize = 11.sp, maxLines = 1) }
}

@Composable
private fun BoostRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = SamsungBlue))
    }
}
