@file:OptIn(ExperimentalMaterial3Api::class)
package com.lapu.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF080D17)
private val Panel = Color(0xFF111D30)
private val Blue = Color(0xFF168BFF)
private val Muted = Color(0xFF9FB2CC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LapuTheme {
                LapuHome(
                    onJava = { showInfo("Java Edition needs a compatible PojavLauncher runtime installed and integrated.") },
                    onBedrock = { showInfo("Bedrock Edition must be installed separately from an official source. This launcher does not include or install the game.") }
                )
            }
        }
    }

    private fun showInfo(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun LapuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Blue,
            secondary = Color(0xFF59B4FF),
            background = Night,
            surface = Panel
        ),
        content = content
    )
}

@Composable
private fun LapuHome(onJava: () -> Unit, onBedrock: () -> Unit) {
    var selectedVersion by remember { mutableStateOf("1.20.1") }
    var selectedTab by remember { mutableStateOf("Home") }

    Scaffold(
        containerColor = Night,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Home", "Mods", "Settings").forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(Icons.Default.Gamepad, contentDescription = null) },
                        label = { Text(tab) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("LAPU", color = Blue, fontSize = 34.sp)
                Text("LAUNCHER  •  MOBILE", color = Muted, fontSize = 12.sp)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Your Minecraft hub", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text("Choose an edition to continue", color = Muted)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = onJava, modifier = Modifier.fillMaxWidth()) {
                            Text("JAVA EDITION")
                        }
                        OutlinedButton(onClick = onBedrock, modifier = Modifier.fillMaxWidth()) {
                            Text("BEDROCK EDITION")
                        }
                    }
                }
            }
            item {
                Text("JAVA PROFILE", color = Blue, style = MaterialTheme.typography.titleMedium)
                Text("Select a profile version", color = Muted)
                Spacer(Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedVersion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Minecraft version") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("1.21.4", "1.21.1", "1.20.1", "1.19.4", "1.18.2").forEach { version ->
                            DropdownMenuItem(
                                text = { Text(version) },
                                onClick = { selectedVersion = version; expanded = false }
                            )
                        }
                    }
                }
                Text("Version selection is a UI profile only until a Java runtime/installer is integrated.", color = Muted, fontSize = 12.sp)
            }
            item {
                Text("TOOLS", color = Blue, style = MaterialTheme.typography.titleMedium)
                ToolTile(Icons.Default.Speed, "FPS Booster", "Performance tips and device settings")
                ToolTile(Icons.Default.Extension, "Mod Manager", "Manage compatible mod files")
                ToolTile(Icons.Default.Palette, "Shaders & Resource Packs", "Organize your packs")
                ToolTile(Icons.Default.Gamepad, "Custom Controls", "Control layout settings")
                ToolTile(Icons.Default.Memory, "Runtime & Storage", "Runtime integration status")
            }
            item {
                Text("Lapu Launcher is an independent community project. Minecraft is not included.", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ToolTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, color = Color.White)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
        }
    }
}
