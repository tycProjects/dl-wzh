package com.greatnice.gts1550control.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greatnice.gts1550control.MainViewModel

@Composable
fun AppRoot(vm: MainViewModel) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(vm.bt.hasAllPermissions()) }
    var deniedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.all { it }
        deniedOnce = !granted
    }
    LaunchedEffect(granted) { if (granted) vm.bt.onPermissionsGranted() }

    Surface(color = Neon.Bg, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header()
            Box(Modifier.weight(1f)) {
                if (!granted) {
                    PermissionScreen(
                        deniedOnce = deniedOnce,
                        onRequest = {
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                )
                            )
                        },
                        onOpenSettings = {
                            ctx.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", ctx.packageName, null)
                                )
                            )
                        }
                    )
                } else {
                    MainTabs(vm)
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "GREATNICE GTS-1550",
            color = Neon.Cyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            style = TextStyle(shadow = Shadow(Neon.Cyan, Offset.Zero, 24f))
        )
        Text("SMART CONTROL", color = Neon.Blue, fontSize = 12.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PermissionScreen(deniedOnce: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeonCard {
            Label("PERMISSIONS NEEDED")
            Body("This app asks for exactly two permissions, both for Bluetooth, and nothing else:", modifier = Modifier.padding(top = 8.dp))
            Body("• Nearby devices — scan: find speakers in range (declared neverForLocation, so no location access).", Neon.Dim, 13, Modifier.padding(top = 8.dp))
            Body("• Nearby devices — connect: pair, read connection state and device info.", Neon.Dim, 13, Modifier.padding(top = 4.dp))
            Body("No internet permission, no accounts, no analytics.", Neon.Dim, 13, Modifier.padding(top = 4.dp))
        }
        NeonButton("GRANT BLUETOOTH ACCESS", Modifier.fillMaxWidth(), onClick = onRequest)
        if (deniedOnce) {
            Body("Permission was denied. If Android no longer shows the dialog, enable \"Nearby devices\" in app settings.", Neon.Warn, 13)
            NeonButton("OPEN APP SETTINGS", Modifier.fillMaxWidth(), accent = Neon.Warn, onClick = onOpenSettings)
        }
    }
}

@Composable
private fun MainTabs(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("CONTROL", "DEVICES", "DIAG", "FEATURES")
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> ControlScreen(vm)
                1 -> DevicesScreen(vm)
                2 -> DiagnosticsScreen(vm)
                else -> FeaturesScreen(vm)
            }
        }
        Row(Modifier.fillMaxWidth().background(Neon.Surface)) {
            tabs.forEachIndexed { i, t ->
                Text(
                    t,
                    modifier = Modifier.weight(1f).clickable { tab = i }.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (tab == i) Neon.Cyan else Neon.Dim
                )
            }
        }
    }
}
