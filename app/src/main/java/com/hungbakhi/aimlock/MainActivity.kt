package com.hungbakhi.aimlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF08090D)
private val SurfaceDark = Color(0xFF171820)
private val Accent = Color(0xFFFF263D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Background, surface = SurfaceDark)) { AimLockHome() } }
    }
}

@Composable
private fun AimLockHome() {
    var enabled by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("HBK", color = Accent, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text("HƯNG BÁ KHI", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("AIMLOCK CONTROL · UI DEMO", color = Color.LightGray, fontSize = 12.sp)
            Spacer(Modifier.height(32.dp))
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("AimLock", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Công tắc giao diện thử nghiệm", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(if (enabled) "TRẠNG THÁI: ON" else "TRẠNG THÁI: OFF", color = if (enabled) Color(0xFF4ADE80) else Color.LightGray, fontWeight = FontWeight.Bold)
                            Text("Chỉ thay đổi trạng thái hiển thị trong app", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Accent))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Lưu ý: Đây là bản UI demo. Công tắc không điều khiển hay can thiệp vào Free Fire hoặc ứng dụng khác.", color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
