// app/src/main/java/com/hyperboost/engine/ui/FreeFireApp.kt
package com.hyperboost.engine.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.hyperboost.engine.core.ColorPresets
import com.hyperboost.engine.core.FfOptions
import com.hyperboost.engine.core.FreeFireBoost
import com.hyperboost.engine.core.ShizukuManager
import com.hyperboost.engine.core.TurboModes
import androidx.compose.runtime.saveable.rememberSaveable

private val Orange = Color(0xFFFF9100)
private val Red = Color(0xFFFF2D55)
private val Green = Color(0xFF00E676)
private val Ink = Color(0xFF0A0B10)
private val PanelBg = Color(0xFF13151D)
private val Tile = Color(0xFF1C2029)
private val TextHi = Color(0xFFF1F4FA)
private val TextMid = Color(0xFF9AA5B6)
private val TextLow = Color(0xFF66707F)
private val Cut = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
private val CutS = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
private val Edge = Brush.horizontalGradient(listOf(Orange, Red.copy(alpha = 0.35f)))
private val Fire = Brush.horizontalGradient(listOf(Orange, Red))

private data class Feature(
    val title: String,
    val desc: String,
    val get: (FfOptions) -> Boolean,
    val set: (FfOptions, Boolean) -> FfOptions
)

private val FEATURES = listOf(
    Feature("Dọn RAM trước khi vào", "Tắt ứng dụng nền để giải phóng bộ nhớ", { it.cleanRam }, { o, v -> o.copy(cleanRam = v) }),
    Feature("Không làm phiền", "Chặn cuộc gọi, tin nhắn làm giật game", { it.dnd }, { o, v -> o.copy(dnd = v) }),
    Feature("Chặn thông báo nổi", "Không cho banner che màn hình khi chơi", { it.blockHeadsUp }, { o, v -> o.copy(blockHeadsUp = v) }),
    Feature("Cảm ứng nhanh", "Giảm độ trễ chạm khi bắn và di chuyển", { it.touchBoost }, { o, v -> o.copy(touchBoost = v) }),
    Feature("Wi-Fi độ trễ thấp", "Ưu tiên gói tin game, giảm ping", { it.wifiLowLatency }, { o, v -> o.copy(wifiLowLatency = v) }),
    Feature("Chống hệ thống bóp game", "Bỏ tối ưu pin, đặt game ở mức hoạt động cao", { it.keepAlive }, { o, v -> o.copy(keepAlive = v) }),
    Feature("Tắt hiệu ứng chuyển cảnh", "Giao diện hệ thống nhẹ hơn", { it.noAnimations }, { o, v -> o.copy(noAnimations = v) }),
    Feature("Giữ màn hình sáng", "Không tắt màn hình khi đang chờ trận", { it.keepScreenOn }, { o, v -> o.copy(keepScreenOn = v) }),
    Feature("Khoá độ sáng", "Tắt tự chỉnh sáng, tránh đổi sáng giữa trận", { it.lockBrightness }, { o, v -> o.copy(lockBrightness = v) }),
    Feature("Thanh Game Turbo trong game", "Mép trái màn hình: FPS, ping, màu, chụp, quay", { it.sidebar }, { o, v -> o.copy(sidebar = v) }),
    Feature("Tự bật khi mở Free Fire", "Vào game tự tăng tốc, thoát game tự khôi phục", { it.autoStart }, { o, v -> o.copy(autoStart = v) })
)

private val RES_TITLES = listOf("GỐC", "MỨC 1", "MỨC 2", "MỨC 3")
private val RES_SUB = listOf("100%", "80% · Nhẹ", "70% · Vừa", "50% · Mạnh")
private val RES_DESC = listOf(
    "Độ phân giải mặc định của game",
    "Ít mờ nhất, tăng FPS nhẹ",
    "Cân bằng giữa độ nét và FPS",
    "FPS cao nhất cho máy yếu, hình mờ hơn"
)

@Composable
fun FreeFireApp(
    viewModel: MainViewModel,
    ff: FfViewModel,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = advanced) { advanced = false }
    if (advanced) {
        DashboardScreen(viewModel, onOpenOverlaySettings, onOpenAccessibilitySettings, modifier, onBack = { advanced = false })
        return
    }

    val context = LocalContext.current
    val opts by ff.opts.collectAsState()
    val games by ff.games.collectAsState()
    val selected by ff.selected.collectAsState()
    val active by ff.active.collectAsState()
    val busy by ff.busy.collectAsState()
    val color by ff.color.collectAsState()
    val t by viewModel.telemetryState.collectAsState()
    val shizuku by viewModel.shizukuStatus.collectAsState()
    val authorized = shizuku == ShizukuManager.ShizukuStatus.AUTHORIZED
    val overlayOk = Settings.canDrawOverlays(context)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { ff.message.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B0A0E), Ink, Ink))),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF0D0E14)).navigationBarsPadding().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(Icons.Filled.PlayArrow, "CHƠI", page == 0) { page = 0 }
                NavItem(Icons.Filled.Palette, "MÀN HÌNH", page == 1) { page = 1 }
                NavItem(Icons.Filled.Settings, "TUỲ CHỌN", page == 2) { page = 2 }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("FREE FIRE", color = Orange, fontSize = 28.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 2.sp)
                        Text("TURBO  ·  LSUPRA", color = TextLow, fontSize = 11.sp, letterSpacing = 4.sp)
                    }
                    Tag(if (authorized) "SHIZUKU" else "NO ADB", if (authorized) Green else Red)
                }
            }
            if (!authorized) item { Banner("Cần cấp quyền Shizuku để tăng tốc", "CẤP QUYỀN") { viewModel.requestShizukuPermission() } }
            if (!overlayOk) item { Banner("Cấp quyền hiển thị trên ứng dụng khác để dùng thanh Game Turbo", "CẤP QUYỀN", onOpenOverlaySettings) }

            when (page) {
                0 -> {
                    item {
                        GamePanel {
                            if (games.isEmpty()) {
                                Text("Chưa tìm thấy Free Fire hoặc Free Fire MAX", color = TextHi, fontWeight = FontWeight.Bold)
                                Text("Cài game rồi mở lại ứng dụng này.", color = TextLow, fontSize = 13.sp)
                            }
                            games.forEach { g ->
                                val sel = g.pkg == selected
                                Row(
                                    Modifier.fillMaxWidth().clip(CutS)
                                        .background(if (sel) Orange.copy(alpha = 0.14f) else Tile)
                                        .clickable { ff.select(g.pkg) }.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val bmp = remember(g.pkg) { g.icon?.toBitmap(128, 128)?.asImageBitmap() }
                                    if (bmp != null) Image(bmp, g.name, Modifier.size(46.dp).clip(CutS))
                                    Text(g.name, color = TextHi, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    if (sel) Text("CHỌN", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            if (active) {
                                FireButton("ĐANG BOOST · TẮT VÀ KHÔI PHỤC", !busy, Modifier.fillMaxWidth(), Brush.horizontalGradient(listOf(Green, Color(0xFF00B0FF)))) { ff.stopBoost() }
                            }
                            FireButton(if (busy) "ĐANG ÁP DỤNG..." else "CHƠI FREE FIRE", !busy && games.isNotEmpty() && authorized, Modifier.fillMaxWidth(), Fire) { ff.play() }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Stat("CPU", t.cpuTempC?.let { "%.0f°C".format(it) } ?: "--", Modifier.weight(1f))
                            Stat("RAM", if (t.ramTotalMb > 0) "${t.ramUsedMb * 100 / t.ramTotalMb}%" else "--", Modifier.weight(1f))
                            Stat("PIN", if (t.batteryPercent >= 0) "${t.batteryPercent}%" else "--", Modifier.weight(1f))
                            Stat("HZ", "${t.refreshRateHz}", Modifier.weight(1f))
                        }
                    }
                    item { Title("CHẾ ĐỘ HIỆU NĂNG") }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TurboModes.ALL.forEach { m -> Choice(TurboModes.label(m), opts.mode == m, Modifier.weight(1f)) { ff.update { it.copy(mode = m) } } }
                        }
                    }
                    item { Title("CÔNG CỤ NHANH") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Tool("Dọn RAM ngay", Modifier.weight(1f), !busy && authorized) { ff.cleanRam() }
                                Tool("Chụp màn hình", Modifier.weight(1f), !busy && authorized) { ff.screenshot() }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Tool("Biên dịch shader", Modifier.weight(1f), !busy && authorized) { ff.compileShaders() }
                                Tool("Dọn cache game", Modifier.weight(1f), !busy && authorized) { ff.clearCache() }
                            }
                        }
                    }
                }

                1 -> {
                    item { Title("ĐỘ PHÂN GIẢI FREE FIRE") }
                    item { Text("Hạ độ phân giải riêng cho game (giao diện hệ thống vẫn nét). Có hiệu lực khi mở lại game.", color = TextLow, fontSize = 12.sp) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..3).forEach { i ->
                                val sel = opts.resLevel == i
                                Row(
                                    Modifier.fillMaxWidth().clip(Cut).background(if (sel) Orange.copy(alpha = 0.14f) else PanelBg)
                                        .then(if (sel) Modifier.border(1.dp, Edge, Cut) else Modifier)
                                        .clickable { ff.update { it.copy(resLevel = i) } }.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(RES_TITLES[i], color = if (sel) Orange else TextHi, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 16.sp)
                                        Text(RES_DESC[i], color = TextLow, fontSize = 12.sp)
                                    }
                                    Text(RES_SUB[i], color = if (sel) Orange else TextMid, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                            FireButton("ÁP DỤNG ĐỘ PHÂN GIẢI", !busy && authorized && games.isNotEmpty(), Modifier.fillMaxWidth(), Fire) { ff.applyResolutionNow() }
                        }
                    }

                    item { Title("CHỈNH MÀU SẮC") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorPresets.ALL.chunked(3).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { (name, p) ->
                                        Choice(name, color == p, Modifier.weight(1f)) { ff.setColor(p, authorized) }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        GamePanel {
                            ColorSlider("Độ bão hòa", color.saturation, 0.5f..2f, "%.2f".format(color.saturation), { ff.setColor(color.copy(saturation = it), false) }, { ff.setColor(color, authorized) })
                            ColorSlider("Độ tương phản", color.contrast, 0.8f..1.5f, "%.2f".format(color.contrast), { ff.setColor(color.copy(contrast = it), false) }, { ff.setColor(color, authorized) })
                            ColorSlider("Độ sáng màu", color.brightness, 0.7f..1.4f, "%.2f".format(color.brightness), { ff.setColor(color.copy(brightness = it), false) }, { ff.setColor(color, authorized) })
                            ColorSlider("Nhiệt màu (lạnh ↔ ấm)", color.warmth, -1f..1f, "%+.2f".format(color.warmth), { ff.setColor(color.copy(warmth = it), false) }, { ff.setColor(color, authorized) })
                            var bright by remember { mutableStateOf(ff.brightness().toFloat()) }
                            ColorSlider("Độ sáng màn hình", bright, 1f..255f, "${bright.toInt()}", { bright = it }, { ff.setBrightness(bright.toInt()) })
                            Tool("ĐẶT LẠI MÀU GỐC", Modifier.fillMaxWidth(), authorized) { ff.resetColor() }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SwitchRow("Tự áp màu khi vào game", "Dùng bộ màu đang chọn ở trên", opts.colorOn) { v -> ff.update { it.copy(colorOn = v) } }
                            SwitchRow("Gỡ lọc màu hệ thống", "Tắt ánh sáng đêm, giảm sáng thêm, lọc mù màu khi chơi", opts.clearFilters) { v -> ff.update { it.copy(clearFilters = v) } }
                        }
                    }
                }

                else -> {
                    item { Title("TÍNH NĂNG GAME TURBO") }
                    items(FEATURES.size) { i ->
                        val f = FEATURES[i]
                        SwitchRow(f.title, f.desc, f.get(opts)) { v -> ff.update { f.set(it, v) } }
                    }
                    item {
                        OutlinedButton(onClick = { advanced = true }, modifier = Modifier.fillMaxWidth(), shape = CutS) {
                            Text("Công cụ nâng cao (HUD, tinh chỉnh, auto tap)", color = TextMid)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(CutS).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (selected) Orange else TextLow, modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) Orange else TextLow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
private fun GamePanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(Cut).background(PanelBg).border(1.dp, Edge, Cut).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun FireButton(text: String, enabled: Boolean, modifier: Modifier, brush: Brush, onClick: () -> Unit) {
    Box(
        modifier.height(54.dp).clip(Cut)
            .background(if (enabled) brush else Brush.horizontalGradient(listOf(Tile, Tile)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.Black else TextLow, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 1.sp, fontSize = 14.sp)
    }
}

@Composable
private fun Title(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Box(Modifier.width(4.dp).height(16.dp).background(Fire))
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextHi, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(CutS)
            .background(if (selected) Orange.copy(alpha = 0.2f) else Tile)
            .then(if (selected) Modifier.border(1.dp, Orange, CutS) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (selected) Orange else TextMid, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center) }
}

@Composable
private fun Tool(text: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.height(46.dp), shape = CutS) {
        Text(text, color = if (enabled) TextHi else TextLow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CutS).background(PanelBg).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextHi, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = TextLow, fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Orange, checkedThumbColor = Color.White)
        )
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, onChange: (Float) -> Unit, onDone: () -> Unit) {
    Column {
        Row {
            Text(label, color = TextMid, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(shown, color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range,
            onValueChangeFinished = onDone,
            colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Orange, inactiveTrackColor = Tile)
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(CutS).background(PanelBg).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Green, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(label, color = TextLow, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
        modifier = Modifier.clip(CutS).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun Banner(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CutS).background(Orange.copy(alpha = 0.18f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text, color = TextHi, fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action, color = Orange, fontWeight = FontWeight.Black) }
    }
}
