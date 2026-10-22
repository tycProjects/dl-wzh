package com.johan.jugb185.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.johan.jugb185.ui.theme.JugbTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JugbTheme {
                Scaffold(containerColor = Color(0xFF0A0E14)) { pad ->
                    Column(Modifier.padding(pad).padding(16.dp)) {
                        Text("JUGB Command Center", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("OnePlus 16 • 185Hz God Mode • Turbo 2.5s", color = Color(0xFF00E676))
                        Spacer(Modifier.height(16.dp))
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Live Engine Log (logcat)")
                                Text("I/JUGB185: VendorStub tx=12 via cached", color = Color.Gray)
                                Text("D/JUGB-Watchdog: vote com.tencent.ig ->7 ok=true", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
