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
        }
    }

    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Text("SamsungFartomizer", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("by Kian", color = SamsungBlue, fontSize = 16.sp)

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
            Button(
                onClick = { exec("Clear background apps", "am kill-all") },
                enabled = ok,
                colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Clear background apps", fontWeight = FontWeight.SemiBold) }
        }

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
