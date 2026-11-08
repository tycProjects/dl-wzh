package com.dyland.app
import android.Manifest
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val B1 = Color(0xFF3F5BFF); private val B2 = Color(0xFFB05CFF)
private val grad = Brush.linearGradient(listOf(B1, B2))

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = B1)) { Home(tick.intValue) } }
    }
    override fun onResume() { super.onResume(); tick.intValue++ }
}

@Composable fun Home(t: Int) {
    val c = LocalContext.current
    var run by remember { mutableStateOf(IslandService.running) }
    val overlay = remember(t) { Settings.canDrawOverlays(c) }
    val notif = remember(t) { NotificationManagerCompat.getEnabledListenerPackages(c).contains(c.packageName) }
    var top by remember { mutableFloatStateOf(Prefs.top(c).toFloat()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF7F8FF), Color(0xFFE6E9FF))))
            .statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color.Black), Alignment.CenterEnd) {
                Box(Modifier.padding(end = 6.dp).size(9.dp).background(grad, CircleShape))
            }
            Spacer(Modifier.width(10.dp))
            Text("Dyland", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14142B))
        }
        Hero()
        Perm(Icons.Rounded.Layers, "Hiển thị trên ứng dụng", overlay) {
            c.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${c.packageName}")))
        }
        Perm(Icons.Rounded.NotificationsActive, "Truy cập thông báo", notif) {
            c.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
        val demos = listOf(
            Triple(Icons.Rounded.Notifications, "Thông báo", android.R.drawable.ic_dialog_email),
            Triple(Icons.Rounded.Call, "Cuộc gọi", android.R.drawable.ic_menu_call),
            Triple(Icons.Rounded.MusicNote, "Nhạc", android.R.drawable.ic_media_play),
            Triple(Icons.Rounded.Bolt, "Đang sạc", android.R.drawable.ic_lock_idle_charging))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            demos.forEach { (ico, name, res) ->
                Box(Modifier.size(66.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(.8f)).clickable {
                    Bus.post(Ev(name, "Dyland • Liquid Island", c.getDrawable(res), tint = true))
                }, Alignment.Center) { Icon(ico, null, tint = B1, modifier = Modifier.size(28.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(.75f)).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VerticalAlignTop, null, tint = B1)
            Slider(top, { top = it; Prefs.setTop(c, it.toInt()) }, Modifier.weight(1f).padding(start = 10.dp), valueRange = 0f..48f)
        }
        Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(if (run) Brush.linearGradient(listOf(Color(0xFF14142B), Color(0xFF14142B))) else grad)
            .clickable {
                if (!overlay) { c.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${c.packageName}"))); return@clickable }
                val i = Intent(c, IslandService::class.java)
                if (run) c.stopService(i) else {
                    if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                    ContextCompat.startForegroundService(c, i)
                }
                run = !run
            }, Alignment.Center) {
            Icon(if (run) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable fun Perm(icon: ImageVector, label: String, ok: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(.8f)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(grad, CircleShape), Alignment.Center) { Icon(icon, null, tint = Color.White) }
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Color(0xFF14142B))
        Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight, null, tint = if (ok) Color(0xFF2BBE7A) else Color.Gray)
    }
}

@Composable fun Hero() {
    var s by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1500); s = 1; delay(2600); s = 2; delay(3400); s = 0 } }
    val sp = spring<Dp>(0.55f, 380f)
    val w by animateDpAsState(listOf(124.dp, 250.dp, 300.dp)[s], sp)
    val h by animateDpAsState(listOf(34.dp, 42.dp, 132.dp)[s], sp)
    val r by animateDpAsState(listOf(17.dp, 21.dp, 34.dp)[s], sp)
    Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(32.dp))
        .background(Brush.linearGradient(listOf(Color.White, Color(0xFFDDE6FF), Color(0xFFD3C2FF)))), Alignment.TopCenter) {
        Box(Modifier.padding(top = 14.dp).size(w, h).clip(RoundedCornerShape(r)).background(Color.Black)) {
            if (s == 0) Box(Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(11.dp).background(Brush.radialGradient(listOf(B2, B1)), CircleShape))
            if (s == 1) Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(grad, CircleShape), Alignment.Center) { Icon(Icons.Rounded.Notifications, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(10.dp)); Text("Dyland", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f)); Icon(Icons.Rounded.Bolt, null, tint = Color(0xFFB9C6FF), modifier = Modifier.size(18.dp))
            }
            if (s == 2) Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(grad, CircleShape), Alignment.Center) { Icon(Icons.Rounded.Notifications, null, tint = Color.White) }
                    Spacer(Modifier.width(12.dp))
                    Column { Text("Dyland", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text("Liquid Island", color = Color.White.copy(.65f), fontSize = 12.sp) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(Icons.Rounded.Call, Icons.Rounded.MusicNote, Icons.Rounded.Bolt, Icons.Rounded.Close).forEach {
                        Box(Modifier.size(width = 56.dp, height = 30.dp).background(Color.White.copy(.15f), CircleShape), Alignment.Center) { Icon(it, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
                    }
                }
            }
        }
    }
}
