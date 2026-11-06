// app/src/main/java/com/hyperboost/engine/services/GameSidebarService.kt
package com.hyperboost.engine.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hyperboost.engine.MainActivity
import com.hyperboost.engine.core.ColorPresets
import com.hyperboost.engine.core.ColorSettings
import com.hyperboost.engine.core.FreeFireBoost
import com.hyperboost.engine.core.FreqReader
import com.hyperboost.engine.core.PerformanceEngine
import com.hyperboost.engine.core.TurboModes
import com.hyperboost.engine.telemetry.TelemetryHub
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Game Turbo 2.0: a glowing edge handle. Swipe it inward (or tap) and a full-height landscape
 * panel slides in: live gauges, MONSTER/Balanced/Saver modes, refresh rate, per-game render
 * resolution, game colour and tools, without leaving the game.
 */
class GameSidebarService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "GameSidebarService"
        const val CHANNEL_ID = "lsupra_game_turbo_channel"
        const val NOTIFICATION_ID = 9101
        const val ACTION_STOP = "com.lsupra.gameturbo.STOP_SIDEBAR"
        const val EXTRA_GAME_PACKAGE = "game_package"
        const val EXTRA_GAME_NAME = "game_name"

        /** How long the game may be out of front before the sidebar closes itself. */
        private const val AWAY_GRACE_MS = 4_000L

        fun start(ctx: Context, pkg: String, name: String) {
            if (!android.provider.Settings.canDrawOverlays(ctx)) return
            val i = Intent(ctx, GameSidebarService::class.java)
                .putExtra(EXTRA_GAME_PACKAGE, pkg).putExtra(EXTRA_GAME_NAME, name)
            androidx.core.content.ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, GameSidebarService::class.java).setAction(ACTION_STOP))
        }

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val telemetryHub by lazy { TelemetryHub.get(applicationContext) }
    private val engine by lazy { PerformanceEngine(applicationContext) }
    private val boost by lazy { FreeFireBoost(applicationContext) }

    private var gamePackage: String? = null
    private var gameName: String = "Game"
    private var watchJob: Job? = null
    private var params: WindowManager.LayoutParams? = null
    private var handleY = 260
    private var panelOpen by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        telemetryHub.acquire("sidebar", 1000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        gamePackage = intent?.getStringExtra(EXTRA_GAME_PACKAGE) ?: gamePackage
        gameName = intent?.getStringExtra(EXTRA_GAME_NAME) ?: gameName
        startForeground(NOTIFICATION_ID, buildNotification())
        _isRunning.value = true
        showSidebar()
        startWatchingForeground()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return START_NOT_STICKY
    }

    /** Closes the sidebar once the game has been out of front for a few seconds. */
    private fun startWatchingForeground() {
        watchJob?.cancel()
        val game = gamePackage ?: return
        watchJob = scope.launch {
            var awaySince = 0L
            telemetryHub.foregroundPackage.collect { fg ->
                if (fg == null || fg == game || fg == packageName) {
                    awaySince = 0L
                } else {
                    if (awaySince == 0L) awaySince = System.currentTimeMillis()
                    launch {
                        delay(AWAY_GRACE_MS)
                        val still = telemetryHub.foregroundPackage.value
                        if (still != null && still != game && still != packageName) stopSelf()
                    }
                }
            }
        }
    }

    private fun showSidebar() {
        if (composeView != null) return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = handleY
        }
        params = p

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@GameSidebarService)
            setViewTreeViewModelStoreOwner(this@GameSidebarService)
            setViewTreeSavedStateRegistryOwner(this@GameSidebarService)
            setContent {
                TurboRoot(
                    gameName = gameName,
                    open = panelOpen,
                    onOpenChange = { setPanelOpen(it) },
                    onDragY = { dy ->
                        handleY = (handleY + dy.toInt()).coerceAtLeast(0)
                        p.y = handleY
                        try { windowManager?.updateViewLayout(this@apply, p) } catch (_: Exception) {}
                    },
                    onClose = { stopSelf() }
                )
            }
        }
        try {
            windowManager?.addView(view, p)
            composeView = view
        } catch (e: Exception) {
            Log.e(TAG, "Could not add sidebar window", e)
            stopSelf()
        }
    }

    /** Small window while closed (only the handle catches touches), full screen while open. */
    private fun setPanelOpen(open: Boolean) {
        val p = params ?: return
        val v = composeView ?: return
        panelOpen = open
        if (open) {
            p.width = WindowManager.LayoutParams.MATCH_PARENT
            p.height = WindowManager.LayoutParams.MATCH_PARENT
            p.x = 0; p.y = 0
        } else {
            p.width = WindowManager.LayoutParams.WRAP_CONTENT
            p.height = WindowManager.LayoutParams.WRAP_CONTENT
            p.x = 0; p.y = handleY
        }
        try { windowManager?.updateViewLayout(v, p) } catch (_: Exception) {}
    }

    // ---------------------------------------------------------------------------------------
    // UI: handle + landscape panel
    // ---------------------------------------------------------------------------------------

    @Composable
    private fun TurboRoot(
        gameName: String,
        open: Boolean,
        onOpenChange: (Boolean) -> Unit,
        onDragY: (Float) -> Unit,
        onClose: () -> Unit
    ) {
        if (!open) {
            EdgeHandle(onOpen = { onOpenChange(true) }, onDragY = onDragY)
        } else {
            TurboPanel(gameName = gameName, onDismiss = { onOpenChange(false) }, onStop = onClose)
        }
    }

    /** Thin neon strip on the screen edge. Swipe it inward or tap it to open the panel. */
    @Composable
    private fun EdgeHandle(onOpen: () -> Unit, onDragY: (Float) -> Unit) {
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.45f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulseA"
        )
        Box(
            modifier = Modifier
                .width(26.dp).height(112.dp)
                .pointerInput(Unit) {
                    var total = 0f
                    var opened = false
                    detectDragGestures(
                        onDragStart = { total = 0f; opened = false },
                        onDrag = { change, drag ->
                            change.consume()
                            total += drag.x
                            if (!opened && total > 32.dp.toPx()) { opened = true; onOpen() }
                            else if (!opened) onDragY(drag.y)
                        }
                    )
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpen() },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier.width(5.dp).height(64.dp)
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(Brush.verticalGradient(listOf(cyan, Color(0xFFFFFFFF), hot)))
                    .alpha(pulse)
            )
        }
    }

    @Composable
    private fun TurboPanel(gameName: String, onDismiss: () -> Unit, onStop: () -> Unit) {
        var shown by remember { mutableStateOf(false) }
        var tab by remember { mutableStateOf(0) }
        val t by telemetryHub.telemetry.collectAsState()
        val prefs = remember { getSharedPreferences("hyperboost_games", Context.MODE_PRIVATE) }
        var mode by remember { mutableStateOf(prefs.getString("turbo/mode", TurboModes.PERFORMANCE) ?: TurboModes.PERFORMANCE) }
        var hz by remember { mutableStateOf<Int?>(null) } // null = tự động
        var dnd by remember { mutableStateOf(false) }
        var touch by remember { mutableStateOf(false) }
        var wifi by remember { mutableStateOf(false) }
        var banner by remember { mutableStateOf(boost.loadOptions().blockHeadsUp) }
        var resPct by remember { mutableStateOf(boost.loadResPercent().toFloat()) }
        var freq by remember { mutableStateOf(FreqReader.Freq()) }
        var col by remember { mutableStateOf(boost.loadColor()) }
        var bright by remember { mutableStateOf(boost.currentBrightness().toFloat()) }
        var status by remember { mutableStateOf<String?>(null) }
        var ping by remember { mutableStateOf<Int?>(null) }
        val rec by FreeFireBoost.recording.collectAsState()
        val crosshair by OverlayHudService.isCrosshairActive.collectAsState()
        val hud by OverlayHudService.isHudRunning.collectAsState()
        val rates = remember { telemetryHub.supportedRefreshRates() }
        val rowScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            shown = true
            val s = withContext(Dispatchers.IO) { engine.readState().getOrNull() }
            if (s != null) { dnd = s.gameDnd; touch = s.touchBoost; wifi = s.wifiGaming }
            hz = withContext(Dispatchers.IO) { engine.readRefreshLock() }
        }
        LaunchedEffect(Unit) { while (true) { ping = boost.ping(); delay(3000) } }
        LaunchedEffect(tab) { while (tab == 0) { freq = FreqReader.read(); delay(1500) } }

        fun close() { rowScope.launch { shown = false; delay(220); onDismiss() } }

        fun exec(label: String, block: suspend () -> Result<String>, onOk: (String) -> Unit = {}) {
            scope.launch {
                val r = withContext(Dispatchers.IO) { block() }
                r.fold({ status = null; onOk(it) }, { status = "$label: ${it.message ?: "maykhonghotro"}" })
            }
        }

        Box(
            Modifier.fillMaxSize().background(Color(0x66000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() }
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val panelW = minOf(maxWidth - 16.dp, 600.dp)
                AnimatedVisibility(
                    visible = shown,
                    enter = slideInHorizontally(tween(260)) { -it } + fadeIn(tween(200)),
                    exit = slideOutHorizontally(tween(200)) { -it } + fadeOut(tween(160))
                ) {
                    val shape = RoundedCornerShape(22.dp)
                    Row(
                        Modifier.padding(8.dp).width(panelW).fillMaxHeight().clip(shape)
                            .background(Brush.verticalGradient(listOf(Color(0xF2141824), Color(0xF20A0C12))))
                            .border(1.dp, Brush.linearGradient(listOf(cyan.copy(alpha = 0.8f), Color(0x22FFFFFF), hot.copy(alpha = 0.8f))), shape)
                            .pointerInput(Unit) { detectTapGestures { } }
                    ) {
                        // ---- rail ----
                        Column(
                            Modifier.width(78.dp).fillMaxHeight().background(Color(0x33000000)).padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RailItem(Icons.Rounded.Speed, "Hiệu suất", tab == 0) { tab = 0 }
                            RailItem(Icons.Rounded.Memory, "Đồ họa", tab == 1) { tab = 1 }
                            RailItem(Icons.Rounded.Palette, "Màu game", tab == 2) { tab = 2 }
                            RailItem(Icons.Rounded.Build, "Công cụ", tab == 3) { tab = 3 }
                            Spacer(Modifier.weight(1f))
                            RailItem(Icons.Rounded.Close, "Thu gọn", false) { close() }
                        }

                        // ---- content ----
                        Column(
                            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("LSUPRA TURBO", color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                                    Text(gameName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                Text("Tắt Turbo", color = Color(0xFF8A94A6), fontSize = 11.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onStop() }.padding(8.dp))
                            }

                            when (tab) {
                                0 -> {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        val maxHz = (rates.maxOrNull() ?: 60).toFloat()
                                        val cpuMax = freq.cpuMaxGhz ?: 3.2f
                                        val gpuMax = freq.gpuMaxMhz ?: 900f
                                        ArcGauge((t.fps ?: 0f) / maxHz, t.fps?.let { "%.0f".format(it) } ?: "--", "FPS", listOf(cyan, green), 80.dp)
                                        ArcGauge((freq.cpuGhz ?: 0f) / cpuMax, freq.cpuGhz?.let { "%.2f".format(it) } ?: "--", "CPU GHz", listOf(Color(0xFFFFD60A), hot), 80.dp)
                                        ArcGauge((freq.gpuMhz ?: 0f) / gpuMax, freq.gpuMhz?.let { "%.0f".format(it) } ?: "--", "GPU MHz", listOf(cyan, Color(0xFFB388FF)), 80.dp)
                                        ArcGauge((t.cpuTempC ?: 0f) / 90f, t.cpuTempC?.let { "%.0f°".format(it) } ?: "--", "NHIỆT", listOf(green, hot), 80.dp)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MiniStat(ping?.let { "${it}ms" } ?: "--", "PING", Modifier.weight(1f))
                                        MiniStat(if (t.ramTotalMb > 0) "${t.ramUsedMb * 100 / t.ramTotalMb}%" else "--", "RAM", Modifier.weight(1f))
                                        MiniStat(if (t.batteryPercent >= 0) "${t.batteryPercent}%" else "--", "PIN", Modifier.weight(1f))
                                    }

                                    Label("CHẾ ĐỘ")
                                    ModeSelector(mode) { m ->
                                        mode = m
                                        prefs.edit().putString("turbo/mode", m).apply()
                                        scope.launch {
                                            val fails = withContext(Dispatchers.IO) { TurboModes.apply(engine, rates, m) }
                                            status = if (fails.size >= 3) "Máy từ chối phần lớn tinh chỉnh" else null
                                        }
                                    }

                                    Label("TỐC ĐỘ LÀM MỚI")
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        NeonChip("Tự động", hz == null, Modifier.weight(1f)) {
                                            exec("Tần số quét", { engine.resetRefreshRate() }) { hz = null }
                                        }
                                        rates.forEach { r ->
                                            NeonChip("$r", hz == r, Modifier.weight(1f)) {
                                                exec("Tần số quét", { engine.lockRefreshRate(r.toFloat()) }) { hz = r }
                                            }
                                        }
                                    }

                                    GlowSlider("Độ sáng màn hình", bright, 1f..255f, "${(bright / 2.55f).toInt()}%", { bright = it }) {
                                        scope.launch { boost.setBrightness(bright.toInt()) }
                                    }
                                }

                                1 -> {
                                    Label("ĐỘ PHÂN GIẢI RENDER · CHỈ GAME")
                                    Text(
                                        "Giảm độ phân giải vẽ của riêng game (như Graphics rendering trên ROG Phone), giao diện hệ thống vẫn nét. Có hiệu lực khi mở lại game.",
                                        color = Color(0xFF8A94A6), fontSize = 11.sp, lineHeight = 15.sp
                                    )
                                    fun setRes(p: Int) {
                                        val pkg = gamePackage ?: return
                                        resPct = p.toFloat()
                                        exec("Độ phân giải", { boost.applyResolutionPercent(pkg, p) }) { status = it }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(100, 80, 70, 50).forEach { p ->
                                            NeonChip(if (p == 100) "Gốc" else "$p%", resPct.toInt() == p, Modifier.weight(1f).height(44.dp)) { setRes(p) }
                                        }
                                    }
                                    GlowSlider("Tinh chỉnh từng 5%", resPct, 50f..100f, if (resPct >= 100f) "Gốc" else "${resPct.toInt()}%", { resPct = (it / 5f).roundToInt() * 5f }, steps = 9) {
                                        setRes(resPct.toInt())
                                    }
                                    Label("TĂNG TỐC ĐỒ HỌA")
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ToolTile("Cảm ứng nhanh", touch, Modifier.weight(1f)) { exec("Cảm ứng", { engine.setTouchLatencyBoost(!touch) }) { touch = !touch } }
                                        ToolTile("Chống bóp game", false, Modifier.weight(1f)) {
                                            val pkg = gamePackage
                                            if (pkg != null) exec("Chống bóp", { boost.keepAlive(pkg) }) { status = "Game được giữ hoạt động cao" }
                                        }
                                    }
                                }

                                2 -> {
                                    Label("MÀU CHỈ ÁP TRONG LÚC CHƠI · THOÁT GAME TỰ TRẢ MÀU GỐC")
                                    ColorPresets.ALL.chunked(3).forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            row.forEach { (name, p) ->
                                                NeonChip(name, col == p, Modifier.weight(1f)) {
                                                    exec("Chỉnh màu", { boost.applyColor(p) }) { col = p; status = null }
                                                }
                                            }
                                        }
                                    }
                                    GlowSlider("Bão hòa", col.saturation, 0.5f..2f, "%.2f".format(col.saturation), { col = col.copy(saturation = it) }) { scope.launch { boost.applyColor(col) } }
                                    GlowSlider("Tương phản", col.contrast, 0.8f..1.5f, "%.2f".format(col.contrast), { col = col.copy(contrast = it) }) { scope.launch { boost.applyColor(col) } }
                                    GlowSlider("Độ sáng màu", col.brightness, 0.7f..1.4f, "%.2f".format(col.brightness), { col = col.copy(brightness = it) }) { scope.launch { boost.applyColor(col) } }
                                    GlowSlider("Nhiệt màu (lạnh ↔ ấm)", col.warmth, -1f..1f, "%+.2f".format(col.warmth), { col = col.copy(warmth = it) }) { scope.launch { boost.applyColor(col) } }
                                    NeonChip("Đặt lại màu gốc", false, Modifier.fillMaxWidth()) {
                                        exec("Màu", { boost.resetColor() }) { col = ColorSettings(); status = "Đã trả màu gốc" }
                                    }
                                }

                                else -> {
                                    Label("CÔNG CỤ")
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ToolTile("Không làm phiền", dnd, Modifier.weight(1f)) { exec("DND", { engine.setGameDnd(!dnd) }) { dnd = !dnd } }
                                        ToolTile("Wi-Fi tốc độ", wifi, Modifier.weight(1f)) { exec("Wi-Fi", { engine.setWifiGamingOptimization(!wifi) }) { wifi = !wifi } }
                                        ToolTile("Chặn banner", banner, Modifier.weight(1f)) {
                                            exec("Banner", { if (boost.setHeadsUp(!banner)) Result.success("") else Result.failure(RuntimeException("máy không hỗ trợ")) }) { banner = !banner }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ToolTile("Tâm ngắm", crosshair, Modifier.weight(1f)) {
                                            startService(Intent(this@GameSidebarService, OverlayHudService::class.java).setAction(OverlayHudService.ACTION_TOGGLE_CROSSHAIR))
                                        }
                                        ToolTile("HUD FPS", hud, Modifier.weight(1f)) {
                                            val a = if (hud) OverlayHudService.ACTION_STOP_HUD else OverlayHudService.ACTION_START_HUD
                                            startService(Intent(this@GameSidebarService, OverlayHudService::class.java).setAction(a))
                                        }
                                        ToolTile("Dọn RAM", false, Modifier.weight(1f)) { exec("Dọn RAM", { engine.killBackgroundApps() }) { status = "Đã dọn bộ nhớ nền" } }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ToolTile("Chụp ảnh", false, Modifier.weight(1f)) { exec("Chụp ảnh", { boost.screenshot() }) { status = "Đã lưu ảnh: Pictures/LSUPRA" } }
                                        ToolTile(if (rec) "Dừng quay" else "Quay màn hình", rec, Modifier.weight(1f)) {
                                            if (rec) exec("Quay", { boost.stopRecording() }) { status = "Đã lưu video: Movies/LSUPRA" }
                                            else exec("Quay", { boost.startRecording() }) { status = "Đang quay (tối đa 3 phút)" }
                                        }
                                        ToolTile("Mở Turbo", false, Modifier.weight(1f)) {
                                            startActivity(Intent(this@GameSidebarService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        }
                                    }
                                }
                            }

                            status?.let { Text(it, color = Color(0xFFFFB74D), fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }

    private val cyan = Color(0xFF00E5FF)
    private val hot = Color(0xFFFF2D55)
    private val green = Color(0xFF00E676)
    private val fire = Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF2D55)))

    @Composable
    private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(14.dp))
                .background(if (selected) Color(0x2200E5FF) else Color.Transparent)
                .then(if (selected) Modifier.border(1.dp, cyan.copy(alpha = 0.7f), RoundedCornerShape(14.dp)) else Modifier)
                .clickable(onClick = onClick).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = if (selected) cyan else Color(0xFF8A94A6), modifier = Modifier.size(24.dp))
            Text(label, color = if (selected) Color.White else Color(0xFF8A94A6), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }

    @Composable
    private fun ModeSelector(mode: String, onPick: (String) -> Unit) {
        val order = listOf(TurboModes.BATTERY, TurboModes.STANDARD, TurboModes.PERFORMANCE)
        Row(
            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A1F2B)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            order.forEach { m ->
                val sel = mode == m
                val monster = m == TurboModes.PERFORMANCE
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                        .then(if (sel) Modifier.background(if (monster) fire else Brush.horizontalGradient(listOf(Color(0xFF2B3345), Color(0xFF2B3345)))) else Modifier)
                        .clickable { onPick(m) },
                    contentAlignment = Alignment.Center
                ) {
                    if (monster) Text("MONSTER", color = if (sel) Color(0xFF1A1000) else Color(0xFFFFB300), fontSize = 14.sp,
                        fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 1.sp)
                    else Text(TurboModes.label(m), color = if (sel) Color.White else Color(0xFF8A94A6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun ArcGauge(value: Float, big: String, label: String, colors: List<Color>, size: Dp = 92.dp) {
        val anim by animateFloatAsState(value.coerceIn(0f, 1f), tween(600), label = "gauge")
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 9.dp.toPx()
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(Color(0x22FFFFFF), 150f, 240f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (anim > 0.01f) {
                    drawArc(
                        Brush.linearGradient(colors, Offset(0f, this.size.height), Offset(this.size.width, 0f)),
                        150f, 240f * anim, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(big, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(label, color = Color(0xFF8A94A6), fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
    }

    @Composable
    private fun MiniStat(value: String, label: String, modifier: Modifier) {
        Row(
            modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1F2B)).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color(0xFF8A94A6), fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Text(value, color = green, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }

    @Composable
    private fun Label(text: String) =
        Text(text, color = Color(0xFF8A94A6), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

    @Composable
    private fun NeonChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier.heightIn(min = 38.dp).clip(shape)
                .background(if (selected) Color(0x2200E5FF) else Color(0xFF1A1F2B))
                .then(if (selected) Modifier.border(1.dp, cyan, shape) else Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = if (selected) cyan else Color(0xFFB7C0CE), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }

    @Composable
    private fun ToolTile(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier.height(48.dp).clip(shape)
                .background(if (on) Color(0x3300E676) else Color(0xFF1A1F2B))
                .then(if (on) Modifier.border(1.dp, green.copy(alpha = 0.8f), shape) else Modifier)
                .clickable(onClick = onClick).padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = if (on) green else Color(0xFFD5DEEA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }

    @Composable
    private fun GlowSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, onChange: (Float) -> Unit, steps: Int = 0, onDone: () -> Unit) {
        Column {
            Row {
                Text(label, color = Color(0xFFB7C0CE), fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(shown, color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, steps = steps,
                onValueChangeFinished = onDone, modifier = Modifier.height(28.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = cyan, inactiveTrackColor = Color(0xFF2A303C))
            )
        }
    }

    // ---------------------------------------------------------------------------------------
    // Notification / teardown
    // ---------------------------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Game Turbo", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, GameSidebarService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Game Turbo đang bật")
            .setContentText(gameName)
            .setContentIntent(open)
            .addAction(0, "Tắt", stop)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        watchJob?.cancel()
        scope.cancel()
        composeView?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        composeView = null
        telemetryHub.release("sidebar")
        _isRunning.value = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

