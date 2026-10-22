package com.johan.jugb185

import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.johan.jugb185.core.ArmedStore
import com.johan.jugb185.core.RateLock
import com.johan.jugb185.core.VendorStub
import com.johan.jugb185.service.WatchdogService
import com.johan.jugb185.ui.theme.JugbTheme
import com.johan.jugb185.util.DeviceCheck
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JugbTheme { JugbApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JugbApp() {
    val ctx = LocalContext.current
    val store = remember { ArmedStore(ctx) }
    val scope = rememberCoroutineScope()
    var selectedHz by remember { mutableStateOf(185) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var showUnsupported by remember { mutableStateOf(false) }

    // Device gate — OnePlus & OPPO only
    val maxHz = DeviceCheck.getMaxHzForDevice()
    val isSupported = DeviceCheck.isSupportedDevice()

    LaunchedEffect(Unit) {
        if (!isSupported) showUnsupported = true else {
            // Auto-start turbo on open
            WatchdogService.start(ctx)
        }
    }

    // Mock app list for preview (real app queries PackageManager)
    val allApps = remember {
        listOf(
            AppInfo("PUBG Mobile", "com.tencent.ig", true, "🎮"),
            AppInfo("Call of Duty Mobile", "com.activision.callofduty.shooter", true, "🎯"),
            AppInfo("Mobile Legends", "com.mobile.legends", true, "⚔️"),
            AppInfo("Genshin Impact", "com.miHoYo.GenshinImpact", true, "🌟"),
            AppInfo("Free Fire MAX", "com.dts.freefiremax", true, "🔥"),
            AppInfo("YouTube", "com.google.android.youtube", false, "▶️"),
            AppInfo("Instagram", "com.instagram.android", false, "📸"),
            AppInfo("TikTok", "com.zhiliaoapp.musically", false, "🎵"),
            AppInfo("Chrome", "com.android.chrome", false, "🌐"),
            AppInfo("System UI", "com.android.systemui", false, "⚙️"),
        )
    }

    var armedMap by remember { mutableStateOf(mapOf<String, Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("JUGB 185", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF00E5FF))
                        Text("Johan Ultimate Gaming Booster", fontSize = 10.sp, color = Color(0xFFFF6D00))
                    }
                },
                actions = {
                    IconButton(onClick = { ctx.startActivity(Intent(ctx, com.johan.jugb185.ui.SettingsActivity::class.java)) }) {
                        Icon(Icons.Default.Settings, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E14))
            )
        },
        containerColor = Color(0xFF0A0E14)
    ) { pad ->

        if (showUnsupported) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Card(Modifier.padding(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2533))) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️ UNSUPPORTED DEVICE", color = Color(0xFFFFAB00), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("JUGB 185 is exclusive to OnePlus & OPPO.\nYour: ${DeviceCheck.getDeviceLabel()}", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            return@Scaffold
        }

        Column(Modifier.padding(pad).fillMaxSize().background(Color(0xFF0A0E14)).padding(12.dp)) {

            // Device banner — OnePlus 16 God Mode
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📱", fontSize = 20.sp)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(DeviceCheck.getModel() + " • ${maxHz}Hz PANEL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (maxHz==185) "GOD MODE UNLOCKED ⚡" else "EXTREME MODE • 165Hz", color = Color(0xFF00E676), fontSize = 10.sp)
                    }
                    Badge(containerColor = Color(0xFF00E5FF)) { Text("TURBO ON", color = Color.Black, fontSize = 10.sp, modifier = Modifier.padding(4.dp)) }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Rate selector — JUGB improvement: presets + Hz chips
            Text("SELECT POWER LEVEL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60,90,120,144,165,185).forEach { hz ->
                    val enabled = hz <= maxHz || hz==185 // show 185 even if caps at 165
                    val sel = selectedHz == hz
                    FilterChip(
                        selected = sel,
                        onClick = { selectedHz = hz; scope.launch { store.setGlobalHz(hz) } },
                        label = { Text("${hz}Hz", fontSize = 11.sp, fontWeight = if(sel) FontWeight.Bold else FontWeight.Normal) },
                        enabled = enabled,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if(hz==185) Color(0xFFFF6D00) else Color(0xFF00E5FF),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Arm All bar — JUGB signature
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { scope.launch { store.armAll(allApps.map { it.pkg }, selectedHz)
                        WatchdogService.start(ctx) } },
                    Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("⚡ ARM ALL • ${selectedHz}Hz", fontWeight = FontWeight.Black, fontSize = 12.sp) }

                OutlinedButton(
                    onClick = { scope.launch { store.clearAll() } },
                    Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("DISARM ALL", fontSize = 12.sp) }
            }

            Spacer(Modifier.height(10.dp))

            // Search + Filters
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("Search 300+ apps…", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All","Armed ⚡","Games 🎮","User","System").forEach { f ->
                    FilterChip(selected = filter==f, onClick = { filter=f }, label = { Text(f, fontSize = 10.sp) })
                }
            }

            Spacer(Modifier.height(8.dp))

            // App list
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val filtered = allApps.filter {
                    (search.isBlank() || it.name.contains(search, true) || it.pkg.contains(search, true)) &&
                    when(filter) {
                        "Armed ⚡" -> armedMap.containsKey(it.pkg)
                        "Games 🎮" -> it.isGame
                        else -> true
                    }
                }
                items(filtered) { app ->
                    val armedHz = armedMap[app.pkg]?.let { vid -> RateLock.RefreshRate.fromVendorId(vid)?.hz }
                    AppRow(app, armedHz, selectedHz) { pkg, hz ->
                        val vid = RateLock.RefreshRate.fromHz(hz)?.vendorId ?: 7
                        scope.launch { store.setArmed(pkg, vid); armedMap = armedMap + (pkg to vid)
                            // instant vote
                            VendorStub().apply { init(); request(pkg, vid) }
                        }
                    }
                }
            }
        }
    }
}

data class AppInfo(val name: String, val pkg: String, val isGame: Boolean, val icon: String)

@Composable
fun AppRow(app: AppInfo, armedHz: Int?, selectedHz: Int, onArm: (String, Int)->Unit) {
    val isArmed = armedHz != null
    Card(
        Modifier.fillMaxWidth().clickable { onArm(app.pkg, if(isArmed) 0 else selectedHz) },
        colors = CardDefaults.cardColors(containerColor = if(isArmed) Color(0xFF1B2A1E) else Color(0xFF1C2533)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(app.icon, fontSize = 22.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF0A0E14)).padding(8.dp))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(app.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(app.pkg, color = Color.Gray, fontSize = 10.sp)
                if (isArmed) {
                    Spacer(Modifier.height(2.dp))
                    Text("⚡ LOCKED @ ${armedHz}Hz • ${RateLock.idleFor(RateLock.RefreshRate.fromHz(armedHz)!!).note}", color = Color(0xFF00E676), fontSize = 9.sp)
                }
            }
            Switch(checked = isArmed, onCheckedChange = { onArm(app.pkg, if(it) selectedHz else 0) },
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF00E5FF), checkedThumbColor = Color.Black))
        }
    }
}
