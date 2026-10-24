package com.devrafa.gamebooster

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.app.ActivityManager
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset

private val Black = Color(0xFF080808)
private val Panel = Color(0xFF191919)
private val Panel2 = Color(0xFF242424)
private val Silver = Color(0xFFD9D9D9)
private val Muted = Color(0xFFA6A6A6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Silver, onPrimary = Black,
                    background = Black, onBackground = Color.White,
                    surface = Panel, onSurface = Color.White,
                    secondary = Muted
                )
            ) {
                Surface(color = Black) { BoosterApp(this) }
            }
        }
    }
}

private enum class Page(val label: String) {
    Home("Beranda"), Cursor("Cursor Lab"), Monitor("Monitor"), Tools("Tools"), Settings("Setelan")
}
private val cursorNames = listOf("Classic Dot", "Precision Cross", "Tactical Four", "Silver Target", "Pro Frame")
private val modes = listOf("Performance", "Balanced", "Battery Saver", "Light")

@Composable
private fun BoosterApp(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("drx_settings", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(Page.Home) }
    var mode by remember { mutableStateOf(prefs.getString("mode", "Balanced") ?: "Balanced") }
    var cursor by remember { mutableIntStateOf(prefs.getInt("cursor", 0)) }
    var size by remember { mutableFloatStateOf(prefs.getFloat("size", 64f)) }
    var alpha by remember { mutableFloatStateOf(prefs.getFloat("alpha", 90f)) }
    var thickness by remember { mutableFloatStateOf(prefs.getFloat("thickness", 3f)) }
    var x by remember { mutableFloatStateOf(prefs.getFloat("x", 0f)) }
    var y by remember { mutableFloatStateOf(prefs.getFloat("y", 0f)) }
    var overlayActive by remember { mutableStateOf(false) }
    var panelActive by remember { mutableStateOf(false) }
    var boostReport by remember { mutableStateOf("Belum ada pemeriksaan.") }
    var showBoostDialog by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    fun save() {
        prefs.edit().putString("mode", mode).putInt("cursor", cursor)
            .putFloat("size", size).putFloat("alpha", alpha).putFloat("thickness", thickness)
            .putFloat("x", x).putFloat("y", y).apply()
    }
    fun overlayAllowed() = Settings.canDrawOverlays(activity)
    fun requestOverlay() {
        if (!overlayAllowed()) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")))
        } else {
            activity.startService(Intent(activity, OverlayService::class.java).apply {
                action = OverlayService.ACTION_CURSOR
                putExtra("cursor", cursor)
                putExtra("size", size)
                putExtra("alpha", alpha)
                putExtra("thickness", thickness)
                putExtra("x", x)
                putExtra("y", y)
            })
            overlayActive = true
        }
    }
    fun startPanel() {
        if (!overlayAllowed()) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")))
        } else {
            activity.startService(Intent(activity, OverlayService::class.java).apply { action = OverlayService.ACTION_PANEL })
            panelActive = true
        }
    }
    fun stopOverlay() {
        activity.startService(Intent(activity, OverlayService::class.java).apply { action = OverlayService.ACTION_STOP })
        overlayActive = false
        panelActive = false
    }

    Column(Modifier.fillMaxSize().background(Black)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).background(Silver, RoundedCornerShape(13.dp)), Alignment.Center) {
                Text("DRX", color = Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("DEV RAFA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("GAME BOOSTER X", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
            }
            IconButton(onClick = { page = Page.Settings }) {
                Icon(Icons.Default.Settings, "Setelan", tint = Silver)
            }
        }
        HorizontalDivider(color = Panel2)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (page) {
                Page.Home -> HomePage(
                    activity, mode, { selected ->
                        mode = selected; save()
                    }, overlayActive, panelActive, boostReport,
                    { showBoostDialog = true }, { requestOverlay() }, { startPanel() }, { stopOverlay() },
                    { page = Page.Cursor }, { page = Page.Monitor }, { page = Page.Tools }, refresh
                )
                Page.Cursor -> CursorPage(
                    cursor, { cursor = it; save() }, size, { size = it; save() },
                    alpha, { alpha = it; save() }, thickness, { thickness = it; save() },
                    x, { x = it; save() }, y, { y = it; save() },
                    overlayActive, { requestOverlay() }, { stopOverlay() }, { x = 0f; y = 0f; save() }
                )
                Page.Monitor -> MonitorPage(activity, refresh) { refresh++ }
                Page.Tools -> ToolsPage(activity, { refresh++ })
                Page.Settings -> SettingsPage(activity, mode, { mode = it; save() }, { stopOverlay() }, { refresh++ })
            }
        }
        NavigationBar(containerColor = Color(0xFF101010), contentColor = Silver) {
            val icons = listOf(Icons.Default.Home, Icons.Default.GpsFixed, Icons.Default.Speed, Icons.Default.Build, Icons.Default.Settings)
            Page.values().forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = page == item,
                    onClick = { page = item },
                    icon = { Icon(icons[index], contentDescription = item.label) },
                    label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Black, selectedTextColor = Silver,
                        indicatorColor = Silver, unselectedIconColor = Muted, unselectedTextColor = Muted
                    )
                )
            }
        }
    }

    if (showBoostDialog) {
        AlertDialog(
            onDismissRequest = { showBoostDialog = false },
            containerColor = Panel,
            title = { Text("Pemeriksaan optimasi", color = Color.White) },
            text = { Text("Aplikasi akan memeriksa kondisi perangkat dan menampilkan informasi yang tersedia. Android tidak mengizinkan aplikasi biasa membersihkan RAM aplikasi lain secara paksa atau menjamin kenaikan FPS.", color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    boostReport = buildDeviceReport(activity, mode)
                    showBoostDialog = false
                    page = Page.Home
                    refresh++
                }) { Text("Jalankan pemeriksaan", color = Silver) }
            },
            dismissButton = { TextButton(onClick = { showBoostDialog = false }) { Text("Batal", color = Muted) } }
        )
    }
}

@Composable
private fun HomePage(
    context: Context, mode: String, setMode: (String) -> Unit,
    overlayActive: Boolean, panelActive: Boolean, report: String,
    boost: () -> Unit, cursorStart: () -> Unit, panelStart: () -> Unit, stop: () -> Unit,
    goCursor: () -> Unit, goMonitor: () -> Unit, goTools: () -> Unit, refresh: Int
) {
    val stats = remember(refresh) { readStats(context) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)) {
        item {
            Box(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(22.dp)).padding(18.dp)) {
                Column {
                    Text("READY TO PLAY?", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Game Booster", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Optimasi realistis. Kontrol penuh di tanganmu.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = boost, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Silver, contentColor = Black)) {
                        Icon(Icons.Default.Bolt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("BOOST NOW", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
        item { Text("PROFIL PERFORMA", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().background(if (mode == item) Panel2 else Panel, RoundedCornerShape(14.dp))
                            .clickable { setMode(item) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (item == "Performance") Icons.Default.Bolt else if (item == "Battery Saver") Icons.Default.BatterySaver else if (item == "Light") Icons.Default.FlashOn else Icons.Default.Balance, null, tint = Silver)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item, fontWeight = FontWeight.SemiBold)
                            Text(when(item) {
                                "Performance" -> "Pemeriksaan perangkat dan profil bermain"
                                "Balanced" -> "Seimbang antara monitor dan baterai"
                                "Battery Saver" -> "Kurangi kerja aplikasi booster"
                                else -> "UI sederhana dan pembaruan lebih jarang"
                            }, color = Muted, fontSize = 11.sp)
                        }
                        RadioButton(selected = mode == item, onClick = { setMode(item) }, colors = RadioButtonDefaults.colors(selectedColor = Silver, unselectedColor = Muted))
                    }
                }
            }
        }
        item { Text("STATUS PERANGKAT", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("RAM tersedia", stats.ram, Modifier.weight(1f))
                    StatCard("Baterai", stats.battery, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Penyimpanan", stats.storage, Modifier.weight(1f))
                    StatCard("Suhu baterai", stats.temperature, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Refresh rate", stats.refresh, Modifier.weight(1f))
                    StatCard("FPS game", "Tidak tersedia", Modifier.weight(1f))
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("OVERLAY STATUS", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                StatusLine("Cursor overlay", if (overlayActive) "Aktif" else "Nonaktif")
                StatusLine("Floating panel", if (panelActive) "Aktif" else "Nonaktif")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = cursorStart, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cursor ON", fontSize = 12.sp) }
                    OutlinedButton(onClick = panelStart, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Panel ON", fontSize = 12.sp) }
                    OutlinedButton(onClick = stop, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Stop, null) }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("LAPORAN TERAKHIR", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(report, color = Muted, fontSize = 12.sp)
            }
        }
        item {
            Text("AKSES CEPAT", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickCard("Cursor Lab", Icons.Default.GpsFixed, goCursor, Modifier.weight(1f))
                QuickCard("Monitor", Icons.Default.Speed, goMonitor, Modifier.weight(1f))
                QuickCard("Tools", Icons.Default.Build, goTools, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CursorPage(
    cursor: Int, setCursor: (Int) -> Unit,
    size: Float, setSize: (Float) -> Unit,
    alpha: Float, setAlpha: (Float) -> Unit,
    thickness: Float, setThickness: (Float) -> Unit,
    x: Float, setX: (Float) -> Unit, y: Float, setY: (Float) -> Unit,
    active: Boolean, start: () -> Unit, stop: () -> Unit, reset: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Text("CURSOR LAB", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Lima gaya overlay visual. Tidak mengubah mekanisme bidikan game.", color = Muted, fontSize = 12.sp)
        }
        items(cursorNames.indices.toList()) { i ->
            Row(
                Modifier.fillMaxWidth().background(if (cursor == i) Panel2 else Panel, RoundedCornerShape(15.dp))
                    .clickable { setCursor(i) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CursorPreview(i, size.coerceIn(30f, 90f), alpha / 100f, thickness, Modifier.size(74.dp).background(Black, RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(cursorNames[i], fontWeight = FontWeight.SemiBold)
                    Text("Varian ${i + 1} • visual overlay", color = Muted, fontSize = 11.sp)
                }
                RadioButton(selected = cursor == i, onClick = { setCursor(i) }, colors = RadioButtonDefaults.colors(selectedColor = Silver))
            }
        }
        item { SliderSetting("Ukuran", size, "20–150 dp", 20f..150f, setSize) }
        item { SliderSetting("Transparansi", alpha, "${alpha.roundToInt()}%", 10f..100f, setAlpha) }
        item { SliderSetting("Ketebalan garis", thickness, "${thickness.roundToInt()} dp", 1f..8f, setThickness) }
        item {
            Column(Modifier.background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("Posisi overlay", fontWeight = FontWeight.Bold)
                Text("X: ${x.roundToInt()} dp   Y: ${y.roundToInt()} dp", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { setX((x - 10f).coerceAtLeast(-300f)) }, modifier = Modifier.weight(1f)) { Text("←") }
                    OutlinedButton(onClick = { setX((x + 10f).coerceAtMost(300f)) }, modifier = Modifier.weight(1f)) { Text("→") }
                    OutlinedButton(onClick = { setY((y - 10f).coerceAtLeast(-500f)) }, modifier = Modifier.weight(1f)) { Text("↑") }
                    OutlinedButton(onClick = { setY((y + 10f).coerceAtMost(500f)) }, modifier = Modifier.weight(1f)) { Text("↓") }
                    OutlinedButton(onClick = reset) { Icon(Icons.Default.Refresh, null) }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("PRATINJAU", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFF101010), RoundedCornerShape(12.dp)), Alignment.Center) {
                    CursorPreview(cursor, size, alpha / 100f, thickness, Modifier.size(170.dp))
                }
                Text(if (active) "Overlay ditandai aktif selama layanan berjalan." else "Pratinjau lokal; aktifkan overlay untuk menampilkan di atas aplikasi lain.", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = start, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Silver, contentColor = Black)) { Text("Tampilkan overlay") }
                    OutlinedButton(onClick = stop, modifier = Modifier.weight(1f)) { Text("Matikan") }
                }
            }
        }
    }
}

@Composable
private fun CursorPreview(index: Int, size: Float, alpha: Float, thickness: Float, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val unit = size.coerceIn(20f, 150f) * density
        val r = unit / 2
        val stroke = thickness.coerceIn(1f, 8f) * density
        val color = Color.White.copy(alpha = alpha.coerceIn(.1f, 1f))
        when (index) {
            0 -> {
                drawCircle(color, radius = r * .55f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawCircle(color, radius = stroke * .8f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
            1 -> {
                drawLine(color, androidx.compose.ui.geometry.Offset(cx - r, cy), androidx.compose.ui.geometry.Offset(cx - r * .25f, cy), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx + r * .25f, cy), androidx.compose.ui.geometry.Offset(cx + r, cy), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx, cy - r), androidx.compose.ui.geometry.Offset(cx, cy - r * .25f), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx, cy + r * .25f), androidx.compose.ui.geometry.Offset(cx, cy + r), stroke)
                drawCircle(color, radius = stroke * .7f, center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
            2 -> {
                val d = r * .35f
                drawLine(color, androidx.compose.ui.geometry.Offset(cx - r, cy), androidx.compose.ui.geometry.Offset(cx - d, cy), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx + d, cy), androidx.compose.ui.geometry.Offset(cx + r, cy), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx, cy - r), androidx.compose.ui.geometry.Offset(cx, cy - d), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx, cy + d), androidx.compose.ui.geometry.Offset(cx, cy + r), stroke)
                drawCircle(color, radius = r * .65f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            }
            3 -> {
                drawCircle(Silver.copy(alpha = alpha), radius = r * .75f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawLine(color, androidx.compose.ui.geometry.Offset(cx - r, cy), androidx.compose.ui.geometry.Offset(cx - r * .65f, cy), stroke * 1.4f)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx + r * .65f, cy), androidx.compose.ui.geometry.Offset(cx + r, cy), stroke * 1.4f)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx, cy - r), androidx.compose.ui.geometry.Offset(cx, cy - r * .65f), stroke * 1.4f)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx, cy + r * .65f), androidx.compose.ui.geometry.Offset(cx, cy + r), stroke * 1.4f)
            }
            else -> {
                val half = r * .65f
                drawLine(color, androidx.compose.ui.geometry.Offset(cx - half, cy - half), androidx.compose.ui.geometry.Offset(cx - half, cy - half * .45f), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx - half, cy - half), androidx.compose.ui.geometry.Offset(cx - half * .45f, cy - half), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx + half, cy - half), androidx.compose.ui.geometry.Offset(cx + half, cy - half * .45f), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx + half, cy - half), androidx.compose.ui.geometry.Offset(cx + half * .45f, cy - half), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx - half, cy + half), androidx.compose.ui.geometry.Offset(cx - half, cy + half * .45f), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx - half, cy + half), androidx.compose.ui.geometry.Offset(cx - half * .45f, cy + half), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx + half, cy + half), androidx.compose.ui.Offset(cx + half, cy + half * .45f), stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(cx + half, cy + half), androidx.compose.ui.Offset(cx + half * .45f, cy + half), stroke)
            }
        }
    }
}

@Composable
private fun MonitorPage(context: Context, refresh: Int, onRefresh: () -> Unit) {
    val stats = remember(refresh) { readStats(context) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("PERFORMANCE MONITOR", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
        item { Text("Data yang bisa dibaca perangkat. FPS internal game tidak direkayasa.", color = Muted, fontSize = 12.sp) }
        item { StatCard("RAM tersedia", stats.ram, Modifier.fillMaxWidth()) }
        item { StatCard("Penyimpanan", stats.storage, Modifier.fillMaxWidth()) }
        item { StatCard("Baterai", stats.battery, Modifier.fillMaxWidth()) }
        item { StatCard("Suhu baterai", stats.temperature, Modifier.fillMaxWidth()) }
        item { StatCard("Refresh rate layar", stats.refresh, Modifier.fillMaxWidth()) }
        item { StatCard("Koneksi", stats.network, Modifier.fillMaxWidth()) }
        item { StatCard("FPS Free Fire", "Tidak tersedia melalui API aplikasi ini", Modifier.fillMaxWidth()) }
        item {
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Silver, contentColor = Black)) {
                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Perbarui data")
            }
        }
    }
}

@Composable
private fun ToolsPage(context: Context, onRefresh: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("TOOLS", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        item { Text("Shortcut dan pemeriksaan aman untuk Android.", color = Muted, fontSize = 12.sp) }
        item { ToolRow("Pengaturan layar", "Refresh rate dan kecerahan", Icons.Default.DisplaySettings) { openSetting(context, Settings.ACTION_DISPLAY_SETTINGS) } }
        item { ToolRow("Pengaturan baterai", "Periksa konsumsi daya", Icons.Default.BatteryFull) { openSetting(context, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).action ?: Settings.ACTION_SETTINGS) } }
        item { ToolRow("Penyimpanan", "Periksa ruang yang tersedia", Icons.Default.Storage) { openSetting(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS) } }
        item { ToolRow("Pengaturan aplikasi", "Kelola aplikasi secara manual", Icons.Default.Apps) { openSetting(context, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS) } }
        item { ToolRow("Pengaturan jaringan", "Wi-Fi dan koneksi", Icons.Default.Wifi) { openSetting(context, Settings.ACTION_WIRELESS_SETTINGS) } }
        item { ToolRow("Izin overlay", "Izinkan panel dan kursor melayang", Icons.Default.Layers) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } }
        item { ToolRow("Perbarui data perangkat", "Baca statistik terbaru", Icons.Default.Refresh, onRefresh) }
        item { Text("Catatan: shortcut membuka halaman pengaturan resmi Android. Aplikasi tidak mengubah setting sistem secara diam-diam.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun SettingsPage(context: Context, mode: String, setMode: (String) -> Unit, stop: () -> Unit, refresh: () -> Unit) {
    val prefs = remember { context.getSharedPreferences("drx_settings", Context.MODE_PRIVATE) }
    var compact by remember { mutableStateOf(prefs.getBoolean("compact", false)) }
    var showInfo by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("SETELAN", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text("Profil default", fontWeight = FontWeight.Bold)
                modes.forEach { m -> Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == m, onClick = { setMode(m) }, colors = RadioButtonDefaults.colors(selectedColor = Silver))
                    Text(m, modifier = Modifier.clickable { setMode(m) })
                } }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mode tampilan ringan", fontWeight = FontWeight.SemiBold)
                    Text("Simpan preferensi UI ringkas secara lokal", color = Muted, fontSize = 11.sp)
                }
                Switch(checked = compact, onCheckedChange = {
                    compact = it
                    prefs.edit().putBoolean("compact", it).apply()
                }, colors = SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Silver))
            }
        }
        item { ToolRow("Izin overlay", "Buka pengaturan izin", Icons.Default.Layers) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        } }
        item { ToolRow("Hentikan overlay", "Tutup kursor dan floating panel", Icons.Default.Stop, stop) }
        item { ToolRow("Perbarui informasi", "Muat ulang data perangkat", Icons.Default.Refresh, refresh) }
        item { ToolRow("Tentang aplikasi", "Versi 1.0.0 • DEV RAFA", Icons.Default.Info) { showInfo = true } }
        item {
            OutlinedButton(onClick = {
                prefs.edit().clear().apply()
                setMode("Balanced")
                refresh()
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Reset semua preferensi") }
        }
        item { Text("Tidak ada login. Pengaturan disimpan secara lokal di perangkat.", color = Muted, fontSize = 12.sp) }
    }
    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        containerColor = Panel,
        title = { Text("DEV RAFA GAME BOOSTER X") },
        text = { Text("Versi 1.0.0\nAndroid native • Kotlin • Jetpack Compose\nFitur FPS tergantung sumber data perangkat. Aplikasi tidak menjamin peningkatan FPS.", color = Muted) },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Tutup", color = Silver) } }
    )
}

@Composable
private fun SliderSetting(label: String, value: Float, valueLabel: String, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(valueLabel, color = Silver, fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = Silver, activeTrackColor = Silver, inactiveTrackColor = Panel2))
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(15.dp)).padding(14.dp)) {
        Text(title, color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Silver)
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun ToolRow(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Silver)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}

private data class DeviceStats(val ram: String, val battery: String, val storage: String, val temperature: String, val refresh: String, val network: String)

private fun readStats(context: Context): DeviceStats {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mem)
    val ram = if (mem.availMem > 0) "${mem.availMem / (1024 * 1024)} MB" else "Tidak tersedia"
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val battery = if (level in 0..100) "$level%" else "Tidak tersedia"
    val stat = StatFs(Environment.getDataDirectory().path)
    val free = stat.availableBytes / (1024L * 1024L * 1024L)
    val total = stat.totalBytes / (1024L * 1024L * 1024L)
    val storage = "$free / $total GB bebas/total"
    val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
    val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val temperature = if (tempTenths > 0) "${"%.1f".format(tempTenths / 10f)} °C" else "Tidak tersedia"
    val refresh = try {
        val display: Display? = if (Build.VERSION.SDK_INT >= 30) context.display else null
        display?.refreshRate?.let { "${it.roundToInt()} Hz" } ?: "Tidak tersedia"
    } catch (_: Exception) { "Tidak tersedia" }
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = if (Build.VERSION.SDK_INT >= 23) {
        val active = cm.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Seluler"
            else -> "Terhubung"
        }
    } else "Tidak tersedia"
    return DeviceStats(ram, battery, storage, temperature, refresh, net)
}

private fun buildDeviceReport(context: Context, mode: String): String {
    val s = readStats(context)
    return "Pemeriksaan selesai.\nMode: $mode\nRAM tersedia: ${s.ram}\nBaterai: ${s.battery}\nPenyimpanan: ${s.storage}\nSuhu baterai: ${s.temperature}\nKoneksi: ${s.network}\nFPS game: tidak tersedia melalui API aplikasi ini.\nTidak ada aplikasi lain yang dihentikan secara paksa."
}

private fun openSetting(context: Context, action: String) {
    try { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
