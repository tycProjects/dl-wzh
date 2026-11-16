package com.supbrother.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SupBrotherScreen() }
    }
}

@Composable
private fun SupBrotherScreen() {
    var version by remember { mutableStateOf("Latest release (not installed)") }
    var memory by remember { mutableFloatStateOf(3f) }
    var performance by remember { mutableStateOf(true) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("SupBrother", style = MaterialTheme.typography.headlineLarge)
                Text("Minecraft Java Edition launcher concept", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                Text("Game version", style = MaterialTheme.typography.titleMedium)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(version) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Latest release (not installed)", "1.20.1 (not installed)").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = {
                                version = it
                                expanded = false
                            })
                        }
                    }
                }
                Text("Memory allocation: ${memory.toInt()} GB")
                Slider(value = memory, onValueChange = { memory = it }, valueRange = 1f..8f, steps = 6)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Performance preference", Modifier.weight(1f))
                    Switch(checked = performance, onCheckedChange = { performance = it })
                }
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text("Install / Play — engine not integrated")
                }
                Text(
                    "This is a UI starter project only. It does not contain Minecraft, a Java runtime, " +
                    "Microsoft sign-in, game downloads, rendering libraries, or a working launch engine.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Next: integrate a properly licensed Android Java launcher core, then build and test on a device.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
