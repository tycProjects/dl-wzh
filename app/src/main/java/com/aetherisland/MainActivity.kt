package com.aetherisland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aetherisland.core.model.IslandActivity
import com.aetherisland.core.model.IslandState
import com.aetherisland.design.theme.AetherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AetherTheme { DashboardScreen() } }
    }
}

@Composable
private fun DashboardScreen() {
    var overlayEnabled by remember { mutableStateOf(false) }
    var whatsappEnabled by remember { mutableStateOf(false) }
    var glassIntensity by remember { mutableFloatStateOf(0.55f) }
    val previewState = remember { IslandState(activity = IslandActivity.MEDIA, expanded = true) }

    Scaffold(containerColor = Color(0xFF090A0D)) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("AetherIsland", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("Dynamic Island for Android", color = Color(0xFF9BA3B4))
            }
            item { IslandPreview(state = previewState) }
            item {
                SettingCard("Overlay permission", "Diperlukan untuk menampilkan island di atas aplikasi lain", overlayEnabled) { overlayEnabled = it }
            }
            item {
                SettingCard("WhatsApp notifications", "Hanya notifikasi WhatsApp yang akan diproses", whatsappEnabled) { whatsappEnabled = it }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171A21))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Liquid Glass · Adaptive", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Intensity", color = Color(0xFFADB5C4))
                        Slider(value = glassIntensity, onValueChange = { glassIntensity = it })
                        Text("Profil adaptif: efek berat hanya saat expanded atau banner aktif.", color = Color(0xFFADB5C4), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171A21))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Fondasi fitur", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        FeatureLine(Icons.Default.MusicNote, "Media Session & lyrics")
                        FeatureLine(Icons.Default.Notifications, "WhatsApp-only notification listener")
                        FeatureLine(Icons.Default.Tune, "Cutout adaptation & manual calibration")
                    }
                }
            }
            item { Text("Foundation build 0.1.0 · API 26+", color = Color(0xFF747D8D), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun IslandPreview(state: IslandState) {
    Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF11151D)), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier.padding(top = 18.dp).background(Color.Black, MaterialTheme.shapes.extraLarge).padding(horizontal = 22.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.MusicNote, null, tint = Color.White)
            Column {
                Text("Now Playing", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text("Expanded preview · OLED compact", color = Color(0xFF9BA3B4), style = MaterialTheme.typography.labelSmall)
            }
        }
        Text("Preview concept", Modifier.align(Alignment.BottomCenter).padding(12.dp), color = Color(0xFF747D8D))
    }
}

@Composable
private fun SettingCard(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171A21))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = Color(0xFFADB5C4), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun FeatureLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = Color(0xFFB9C7FF))
        Text(label, color = Color(0xFFE2E6EF), style = MaterialTheme.typography.bodyMedium)
    }
}
