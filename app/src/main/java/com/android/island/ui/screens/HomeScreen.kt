package com.android.island.ui.screens

import android.widget.Toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.android.island.core.IslandBus
import com.android.island.ai.AiSession
import com.android.island.ai.AiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import com.android.island.util.IslandDiagnostics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.core.AnimationPreset
import com.android.island.core.CutoutProfile
import com.android.island.core.CutoutType
import com.android.island.core.Elasticity
import com.android.island.core.ExpandStyle
import com.android.island.core.TapAction
import com.android.island.data.IslandSettings
import com.android.island.data.AnimationOption
import com.android.island.data.IslandThemeMode
import com.android.island.service.IslandAccessibilityService
import com.android.island.ui.components.*
import com.android.island.ui.theme.Tile
import com.android.island.ui.theme.Ui
import com.android.island.system.ShizukuState
import com.android.island.system.ShizukuStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun HomeScreen(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    hasAccessibility: Boolean,
    hasNotifListener: Boolean,
    onRequestAccessibility: () -> Unit,
    onRequestNotificationListener: () -> Unit,
    onRequestShizuku: () -> Unit,
    hasPhoneState: Boolean = false,
    onRequestPhoneState: () -> Unit = {},
    onRestartIsland: () -> Unit,
    onDemoScenario: (String) -> Unit,
    onApplyRecommended: () -> Unit = {},
    /** Called after a successful AI connection test: MainActivity restarts the app via Shizuku. */
    onConnectionVerified: () -> Unit = {}
) {
    val ready = hasAccessibility && hasNotifListener
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val maxBannerWidthDp = (screenWidthDp - 8).coerceAtLeast(180)
    val shizukuStatus by ShizukuState.status.collectAsState()
    val diagnosticLines by IslandDiagnostics.snapshot.collectAsState()
    var showDiagnostics by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf<AiDialogKind?>(null) }
    var showAiClearConfirm by remember { mutableStateOf(false) }
    // Null = service off; false = the accessibility config predates screen reading (needs re-toggle).
    val a11yCanReadScreen = remember { IslandAccessibilityService.canReadScreen() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val aiScope = androidx.compose.runtime.rememberCoroutineScope()
    var aiSetupMessage by remember { mutableStateOf("") }
    var aiSetupBusy by remember { mutableStateOf(false) }
    var aiConnectionVerified by remember { mutableStateOf(false) }
    var showAiAdvancedSettings by remember { mutableStateOf(false) }
    // After a restart the same, already tested setup is still verified: no need to test again.
    LaunchedEffect(settings.aiProvider, settings.aiBaseUrl, settings.aiModel, settings.aiApiKey) {
        if (settings.aiApiKey.isBlank() || settings.aiModel.isBlank()) return@LaunchedEffect
        val fp = com.android.island.ui.UiPrefs.aiFingerprint(
            settings.aiProvider, settings.aiBaseUrl, settings.aiModel, settings.aiApiKey.trim()
        )
        if (com.android.island.ui.UiPrefs.isAiVerified(context, fp) && !aiConnectionVerified) {
            aiConnectionVerified = true
            if (aiSetupMessage.isBlank()) aiSetupMessage = "Koneksi AI tetap tersambung setelah mulai ulang. Model: ${settings.aiModel}."
        }
    }
    var availableAiModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAiModelPicker by remember { mutableStateOf(false) }
    fun detectAiProvider(key: String): Triple<String, String, String> {
        val k = key.trim()
        return when {
            // Full ready-to-use chat-completions URL so the Base URL field is immediately usable.
            k.startsWith("gsk_", ignoreCase = true) ->
                Triple("groq", "https://api.groq.com/openai/v1/chat/completions", "")
            k.startsWith("sk-or-v1-") ->
                Triple("openai", "https://openrouter.ai/api/v1/chat/completions", "")
            k.startsWith("AIza") ->
                Triple("gemini", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "")
            else -> Triple(settings.aiProvider, settings.aiBaseUrl, settings.aiModel)
        }
    }
    fun loadAiModels(provider: String, baseUrl: String, key: String, testChat: Boolean = false) {
        val localEndpoint = isLocalAiEndpoint(baseUrl)
        if (key.isBlank() && !localEndpoint) {
            aiConnectionVerified = false
            aiSetupMessage = "Tempel API key dari penyedia AI terlebih dahulu."
            return
        }
        aiSetupBusy = true
        aiConnectionVerified = false
        aiSetupMessage = if (testChat) "Memeriksa daftar model lalu mengirim tes chat singkat…"
            else "Menghubungkan ke penyedia dan mengambil daftar model…"
        aiScope.launch {
            try {
                val models = AiClient.discoverModels(baseUrl, key.trim(), provider)
                availableAiModels = models
                if (models.isEmpty()) {
                    aiConnectionVerified = false
                    aiSetupMessage = "Server merespons, tetapi belum memberikan daftar model yang bisa digunakan. Periksa akses akun atau alamat server."
                } else {
                    val preferred = com.android.island.ai.ModelPicker.pickDefault(
                        provider, baseUrl, models,
                        AI_PROVIDER_PRESETS.firstOrNull { it.provider == provider && AiClient.canonicalBaseUrl(it.provider, it.baseUrl) == baseUrl }?.familyRegex
                    ) ?: models.first()
                    val selectedModel = settings.aiModel.takeIf {
                        settings.aiProvider == provider && settings.aiBaseUrl == baseUrl && it in models
                    } ?: preferred
                    onUpdateSettings { it.copy(aiModel = selectedModel) }
                    if (testChat) {
                        val endpoint = com.android.island.ai.AiEndpoint(
                            baseUrl = baseUrl,
                            apiKey = key.trim(),
                            model = selectedModel,
                            temperature = 0f,
                            maxTokens = 16,
                            streaming = false,
                            provider = provider
                        )
                        val answer = AiClient.chatCompletions(
                            endpoint,
                            listOf(
                                com.android.island.ai.ApiChatMessage.system("Ini hanya tes koneksi. Balas tepat satu kata: OK"),
                                com.android.island.ai.ApiChatMessage.user("Tes koneksi Android Island.")
                            ),
                            onDelta = {}
                        )
                        if (answer.isBlank()) throw IllegalStateException("Server menerima tes tetapi tidak mengirim jawaban.")
                        aiConnectionVerified = true
                        aiSetupMessage = "Berhasil! AI dapat menerima dan menjawab pesan. Model: $selectedModel."
                        com.android.island.ui.UiPrefs.saveVerifiedAi(
                            context,
                            com.android.island.ui.UiPrefs.aiFingerprint(provider, baseUrl, selectedModel, key.trim())
                        )
                        onConnectionVerified()
                    } else {
                        aiConnectionVerified = false
                        aiSetupMessage = "Ditemukan ${models.size} model. Pilih model, lalu tekan ‘Uji koneksi’ untuk memastikan chat benar-benar berjalan."
                        if (models.size > 1) showAiModelPicker = true
                    }
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                aiConnectionVerified = false
                availableAiModels = emptyList()
                aiSetupMessage = "Belum berhasil terhubung. ${error.message ?: "Periksa penyedia dan API key, lalu coba lagi."}"
            } finally {
                aiSetupBusy = false
            }
        }
    }
    fun autoConfigureAi(key: String) {
        val detected = detectAiProvider(key)
        val detectedDefaultUrl = AiClient.canonicalBaseUrl(detected.first, detected.second)
        val currentPresetUrl = AI_PROVIDER_PRESETS.firstOrNull { preset ->
            preset.id != "custom" && AiClient.canonicalBaseUrl(preset.provider, preset.baseUrl) == settings.aiBaseUrl
        }?.baseUrl ?: AI_PROVIDER_PRESETS.firstOrNull {
            it.id != "custom" && it.provider == settings.aiProvider
        }?.baseUrl
        val effectiveUrl = AiClient.baseUrlForProviderChange(
            settings.aiBaseUrl, currentPresetUrl, detected.first, detectedDefaultUrl
        )
        // A user-entered endpoint is authoritative; key detection must never overwrite it.
        // Clear the old model because model IDs are provider-specific.
        onUpdateSettings { it.copy(aiProvider = detected.first, aiBaseUrl = effectiveUrl, aiApiKey = key.trim(), aiModel = "") }
        aiConnectionVerified = false
        availableAiModels = emptyList()
        if (key.isNotBlank()) loadAiModels(detected.first, effectiveUrl, key)
        else aiSetupMessage = "API key dihapus. Masukkan key untuk memvalidasi provider."
    }
    val defaults = remember { IslandSettings() }
    // Scroll position survives the Shizuku restart: restored once the content is tall enough, saved when a scroll ends.
    val savedScroll = remember { com.android.island.ui.UiPrefs.homeScroll(context) }
    val homeScrollState = rememberScrollState()
    var scrollRestored by remember { mutableStateOf(savedScroll == 0) }
    LaunchedEffect(homeScrollState.maxValue) {
        if (!scrollRestored && homeScrollState.maxValue >= savedScroll) {
            homeScrollState.scrollTo(savedScroll)
            scrollRestored = true
        }
    }
    LaunchedEffect(scrollRestored) {
        if (scrollRestored) return@LaunchedEffect
        // Settings load a moment after the first frame; if the page never grows that tall, stop waiting.
        kotlinx.coroutines.delay(2_000)
        homeScrollState.scrollTo(savedScroll.coerceAtMost(homeScrollState.maxValue))
        scrollRestored = true
    }
    LaunchedEffect(homeScrollState) {
        snapshotFlow { homeScrollState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling && scrollRestored) {
                com.android.island.ui.UiPrefs.saveHomeScroll(context, homeScrollState.value)
            }
        }
    }
    // Cutout the running overlay detected, so "Otomatis" shows the real size instead of a guess.
    val detectedCutout by remember { IslandBus.state.map { it.cutout.type }.distinctUntilChanged() }
        .collectAsState(initial = IslandBus.state.value.cutout.type)

    CompositionLocalProvider(LocalSliderHaptics provides settings.hapticsEnabled) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .verticalScroll(homeScrollState)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Android Island",
                color = Ui.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(10.dp))
            // STAGE 70: robotic version tag (monospace chip, drawn once — no animation loop).
            Text(
                com.android.island.BuildConfig.VERSION_NAME,
                color = Tile.Teal,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Ui.Card)
                    .border(1.dp, Ui.IconBg.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Text(
            "Pesan, musik, dan asisten AI dalam satu kapsul kecil di atas layar",
            color = Ui.Sub,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        // Master toggle
        GroupCard {
            SwitchRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.Lens,
                iconColor = Tile.Blue,
                title = "Island aktif",
                subtitle = if (ready) "Overlay berjalan" else "Lengkapi izin dulu",
                checked = settings.enabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(enabled = v) } }
            )
        }

        GroupCard {
            SettingsRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.Refresh,
                iconColor = Tile.Green,
                title = "Restart Island",
                subtitle = "Muat ulang overlay jika capsule tidak muncul. Dengan Shizuku siap dan aksesibilitas sudah diizinkan, aplikasi di-force-stop lalu izin aksesibilitas disambungkan otomatis.",
                onClick = onRestartIsland
            )
        }

        GroupCard {
            SettingsRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.BugReport,
                iconColor = Tile.Orange,
                title = "Diagnostik internal",
                subtitle = "${if (settings.diagnosticsEnabled) "Log detail aktif" else "Log error selalu aktif"} · ${diagnosticLines.size} log terbaru · salin semua untuk riwayat penuh",
                onClick = { showDiagnostics = true }
            )
            SwitchRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.FiberManualRecord,
                iconColor = if (settings.diagnosticsEnabled) Tile.Red else Ui.Sub,
                title = "Perekaman log",
                subtitle = if (settings.diagnosticsEnabled) "Merekam aktivitas detail; semua error tetap dicatat" else "Mode hemat log aktif; error dan kegagalan tetap dicatat otomatis",
                checked = settings.diagnosticsEnabled,
                onCheckedChange = { enabled ->
                    IslandDiagnostics.setEnabled(enabled)
                    onUpdateSettings { it.copy(diagnosticsEnabled = enabled) }
                }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Izin · Akses sistem")
        GroupCard {
            SettingsRow(
                pos = RowPos.First,
                icon = Icons.Rounded.AccessibilityNew,
                iconColor = Tile.Purple,
                title = "Accessibility",
                subtitle = if (hasAccessibility) "Siap menggambar island" else "Wajib diaktifkan",
                onClick = onRequestAccessibility,
                trailing = { StatusPill(hasAccessibility, if (hasAccessibility) "ON" else "OFF") }
            )
            SettingsRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.Notifications,
                iconColor = Tile.Orange,
                title = "Notification Listener",
                subtitle = if (hasNotifListener) "WhatsApp & media terhubung" else "Wajib diaktifkan",
                onClick = onRequestNotificationListener,
                trailing = { StatusPill(hasNotifListener, if (hasNotifListener) "ON" else "OFF") }
            )
            SettingsRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.Call,
                iconColor = Tile.Green,
                title = "Telepon",
                subtitle = if (hasPhoneState) "Mendeteksi panggilan masuk dan berlangsung langsung dari sistem" else "Opsional · untuk panggilan tanpa notifikasi",
                onClick = onRequestPhoneState,
                trailing = { StatusPill(hasPhoneState, if (hasPhoneState) "ON" else "OFF") }
            )
            SettingsRow(
                pos = RowPos.Last,
                icon = Icons.Rounded.Shield,
                iconColor = Tile.Teal,
                title = "Shizuku",
                subtitle = when (shizukuStatus) {
                    ShizukuStatus.READY -> "Aktif · dipakai untuk membersihkan ikon status bar"
                    ShizukuStatus.NOT_RUNNING -> "Terpasang tetapi belum berjalan · ketuk untuk membuka"
                    ShizukuStatus.PERMISSION_REQUIRED -> "Izin belum diberikan · ketuk untuk mengizinkan"
                    ShizukuStatus.NOT_INSTALLED -> "Belum terpasang · opsional, ketuk untuk memasang"
                },
                onClick = onRequestShizuku,
                trailing = { StatusPill(shizukuStatus == ShizukuStatus.READY, if (shizukuStatus == ShizukuStatus.READY) "ON" else "OFF") }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kustomisasi · Warna teks, progress & tombol")
        GroupCard {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Warna teks & kontrol", color = Ui.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Mengubah warna teks, progress lagu, dan tombol kontrol. Capsule, panel, banner, serta tampilan Liquid Glass tetap seperti bawaan.",
                    color = Ui.Sub, fontSize = 11.sp, lineHeight = 15.sp
                )
                Spacer(Modifier.height(12.dp))
                ChipRow(
                    options = listOf("Adaptive", "Putih", "Hitam", "Outline"),
                    selected = when (settings.islandTheme) {
                        IslandThemeMode.ADAPTIVE -> "Adaptive"
                        IslandThemeMode.WHITE -> "Putih"
                        IslandThemeMode.BLACK -> "Hitam"
                        IslandThemeMode.OUTLINE -> "Outline"
                    },
                    onSelect = { label ->
                        onUpdateSettings {
                            it.copy(islandTheme = when (label) {
                                "Putih" -> IslandThemeMode.WHITE
                                "Hitam" -> IslandThemeMode.BLACK
                                "Outline" -> IslandThemeMode.OUTLINE
                                else -> IslandThemeMode.ADAPTIVE
                            })
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (settings.islandTheme) {
                        IslandThemeMode.ADAPTIVE -> "Memilih warna teks dan kontrol berdasarkan kontras artwork album atau foto profil yang tersedia."
                        IslandThemeMode.WHITE -> "Teks, progress, dan tombol kontrol menggunakan warna putih. Kaca dan tepinya dibuat lebih putih dan terang, seperti widget kaca referensi."
                        IslandThemeMode.BLACK -> "Teks, progress, dan tombol kontrol menggunakan warna hitam."
                        IslandThemeMode.OUTLINE -> "Teks memakai isi hitam dengan garis tepi putih; progress dan tombol tetap kontras."
                    },
                    color = Ui.Sub, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kustomisasi · Tampilan")
        GroupCard {
            SwitchRow(
                pos = RowPos.First,
                icon = Icons.Rounded.MusicNote,
                iconColor = Tile.Pink,
                title = "Lirik di expanded",
                subtitle = "Menampilkan lirik berjalan saat lagu diputar",
                checked = settings.showLyrics,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showLyrics = v) } }
            )
            Reveal(settings.showLyrics) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingSlider(
                        title = "Ukuran font lirik", valueText = "${settings.lyricFontSize} sp",
                        value = settings.lyricFontSize.toFloat(), range = 9f..22f, default = defaults.lyricFontSize.toFloat(),
                        onChange = { v -> onUpdateSettings { it.copy(lyricFontSize = v.toInt()) } }
                    )
                    SettingSlider(
                        title = "Ketebalan font lirik", valueText = "${settings.lyricFontWeight}",
                        value = settings.lyricFontWeight.toFloat(), range = 300f..900f, default = defaults.lyricFontWeight.toFloat(),
                        onChange = { v -> onUpdateSettings { it.copy(lyricFontWeight = v.toInt()) } }
                    )
                }
            }
            SwitchRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.Image,
                iconColor = Tile.Blue,
                title = "Latar album / avatar",
                subtitle = "Cover lagu & foto profil sebagai background",
                checked = settings.showAlbumBackground,
                onCheckedChange = { v -> onUpdateSettings { it.copy(showAlbumBackground = v) } }
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Reveal(settings.showAlbumBackground) {
                    SettingSlider(
                        title = "Opasitas artwork", valueText = "${(settings.backgroundOpacity * 100).toInt()}%",
                        value = settings.backgroundOpacity, range = 0f..1f, default = defaults.backgroundOpacity,
                        onChange = { v -> onUpdateSettings { it.copy(backgroundOpacity = v) } }
                    )
                }
                SwitchRow(
                    icon = Icons.Rounded.AutoAwesome,
                    iconColor = Tile.Teal,
                    title = "Glow lirik",
                    subtitle = "Cahaya lembut di sekitar teks lirik yang sedang diputar.",
                    checked = settings.lyricGlowEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(lyricGlowEnabled = v) } }
                )
                SettingSlider(
                    title = "Intensitas glow lirik", valueText = "${(settings.lyricGlowIntensity * 100).toInt()}%",
                    value = settings.lyricGlowIntensity, range = 0f..0.7f, default = defaults.lyricGlowIntensity,
                    onChange = { v -> onUpdateSettings { it.copy(lyricGlowIntensity = v) } }
                )
                SettingSlider(
                    title = "Kilau liquid (sheen)", valueText = "${(settings.sheenStrength * 100).toInt()}%",
                    value = settings.sheenStrength, range = 0f..1f, default = defaults.sheenStrength,
                    onChange = { v -> onUpdateSettings { it.copy(sheenStrength = v) } }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Sistem · Performa & baterai")
        GroupCard {
            SwitchRow(
                pos = RowPos.First,
                icon = Icons.Rounded.BatterySaver,
                iconColor = Tile.Green,
                title = "Mode hemat daya",
                subtitle = "Artwork lebih kecil, lens resolusi ringan, cache dibatasi, efek dekoratif dikurangi. Semua fitur tetap aktif. Saat Battery Saver Android menyala, jam animasi ambient otomatis melambat setengah (tidak terlihat mata).",
                checked = settings.lowPowerMode,
                onCheckedChange = { v -> onUpdateSettings { it.copy(lowPowerMode = v) } }
            )
            SwitchRow(
                pos = RowPos.Last,
                icon = Icons.Rounded.Shield,
                iconColor = Tile.Teal,
                title = "Mode grafik ringan (Low-End)",
                subtitle = "Tampilan paling sederhana: latar hitam, tanpa blur/shader. Memulai ulang aplikasi saat diganti. Pakai hanya bila Mode hemat daya masih terasa berat.",
                checked = settings.lowEndMode,
                onCheckedChange = { v -> onUpdateSettings { it.copy(lowEndMode = v) } }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kustomisasi · Capsule & posisi")
        GroupCard {
            SwitchRow(
                pos = RowPos.First,
                icon = Icons.Rounded.PhoneAndroid,
                iconColor = Tile.Blue,
                title = "Mode iPhone Notch",
                subtitle = "Overlay hitam permanen untuk layar Waterdrop / punch-hole",
                checked = settings.iphoneNotchEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(iphoneNotchEnabled = v) } }
            )
            val (shownW, shownH) = CutoutProfile.resolve(
                settings.cutoutProfile, detectedCutout, settings.iphoneNotchWidth, settings.iphoneNotchHeight
            )
            val profiles = listOf(
                CutoutProfile.AUTO, CutoutProfile.IPHONE, CutoutProfile.PUNCH_HOLE,
                CutoutProfile.WATERDROP, CutoutProfile.WIDE_NOTCH, CutoutProfile.CUSTOM
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingSlider(
                    title = "Offset X", valueText = "${settings.offsetX} dp",
                    value = settings.offsetX.toFloat(), range = -120f..120f, default = defaults.offsetX.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(offsetX = v.toInt()) } }
                )
                SettingSlider(
                    title = "Offset Y", valueText = "${settings.offsetY} dp",
                    value = settings.offsetY.toFloat(), range = 0f..48f, default = defaults.offsetY.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(offsetY = v.toInt()) } }
                )
                SwitchRow(
                    icon = Icons.Rounded.AutoAwesome,
                    iconColor = Tile.Teal,
                    title = "Deteksi cutout kamera depan",
                    subtitle = if (detectedCutout == CutoutType.NONE) "Tidak ada cutout terdeteksi; memakai ukuran manual."
                        else "Terdeteksi ${detectedCutout.name.lowercase()} · ${shownW}×${shownH} dp",
                    checked = settings.cutoutProfile == CutoutProfile.AUTO,
                    onCheckedChange = { v ->
                        onUpdateSettings { s ->
                            if (v) s.copy(cutoutProfile = CutoutProfile.AUTO)
                            else s.copy(cutoutProfile = if (s.cutoutProfile == CutoutProfile.AUTO) CutoutProfile.IPHONE else s.cutoutProfile)
                        }
                    }
                )
                Text("Ukuran capsule", color = Ui.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                // STAGE 70: manual profile controls only exist when automatic detection is off —
                // sebelumnya tombol deteksi otomatis dan chip "Otomatis" bisa saling menimpa
                // tanpa penjelasan; kini hanya satu jalur yang terlihat pada satu waktu.
                Reveal(settings.cutoutProfile != CutoutProfile.AUTO) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipRow(
                    options = profiles.map { it.label },
                    selected = settings.cutoutProfile.label,
                    onSelect = { lbl ->
                        val picked = profiles.first { it.label == lbl }
                        onUpdateSettings { s ->
                            // Pindah ke Kustom mulai dari ukuran yang sedang terlihat, bukan lompat.
                            if (picked == CutoutProfile.CUSTOM && s.cutoutProfile != CutoutProfile.CUSTOM)
                                s.copy(cutoutProfile = picked, iphoneNotchWidth = shownW, iphoneNotchHeight = shownH)
                            else s.copy(cutoutProfile = picked)
                        }
                    },
                    perRow = 3
                )
                Hint(
                    when (settings.cutoutProfile) {
                        CutoutProfile.AUTO -> "Mengikuti cutout yang terdeteksi di layar (${shownW}×${shownH} dp)."
                        CutoutProfile.CUSTOM -> "Ukuran manual dari slider di bawah."
                        else -> "Ukuran bawaan profil ${settings.cutoutProfile.label}. Geser slider untuk beralih ke Kustom."
                    }
                )
                SettingSlider(
                    title = "Lebar capsule", valueText = "$shownW dp",
                    value = shownW.toFloat(), range = 88f..360f, default = CutoutProfile.IPHONE.width.toFloat(),
                    onChange = { v ->
                        onUpdateSettings { s ->
                            s.copy(
                                iphoneNotchWidth = v.toInt(),
                                iphoneNotchHeight = if (s.cutoutProfile == CutoutProfile.CUSTOM) s.iphoneNotchHeight else shownH,
                                cutoutProfile = CutoutProfile.CUSTOM
                            )
                        }
                    }
                )
                SettingSlider(
                    title = "Tinggi capsule", valueText = "$shownH dp",
                    value = shownH.toFloat(), range = 24f..64f, default = CutoutProfile.IPHONE.height.toFloat(),
                    onChange = { v ->
                        onUpdateSettings { s ->
                            s.copy(
                                iphoneNotchWidth = if (s.cutoutProfile == CutoutProfile.CUSTOM) s.iphoneNotchWidth else shownW,
                                iphoneNotchHeight = v.toInt(),
                                cutoutProfile = CutoutProfile.CUSTOM
                            )
                        }
                    }
                )
                }
                }
            }
            Reveal(settings.iphoneNotchEnabled) {
                SwitchRow(
                    pos = RowPos.Middle,
                    icon = Icons.Rounded.VisibilityOff,
                    iconColor = Tile.Orange,
                    title = "Sembunyikan saat idle",
                    subtitle = "Notch hilang saat tidak ada aktivitas untuk mengurangi overlay aktif",
                    checked = settings.hideNotchWhenIdle,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(hideNotchWhenIdle = v) } }
                )
            }
            Reveal(!settings.iphoneNotchEnabled) {
                SwitchRow(
                    pos = RowPos.Middle,
                    icon = Icons.Rounded.CropFree,
                    iconColor = Tile.Purple,
                    title = "Capsule atas tajam",
                    subtitle = "Sudut atas rata menempel ke tepi layar (hanya saat Mode iPhone Notch mati)",
                    checked = settings.sharpNotch,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(sharpNotch = v) } }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kustomisasi · Ukuran expanded")
        GroupCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingSlider(
                    title = "Lebar expanded", valueText = "${settings.expandedWidth.coerceAtMost(screenWidthDp)} dp / maks. $screenWidthDp dp",
                    value = settings.expandedWidth.toFloat(), range = 220f..screenWidthDp.toFloat().coerceAtLeast(240f), default = defaults.expandedWidth.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(expandedWidth = v.toInt()) } }
                )
                SettingSlider(
                    title = "Tinggi minimum (pendek ↔ tinggi)",
                    valueText = if (settings.expandedMinHeight == 0) "Auto" else "${settings.expandedMinHeight} dp",
                    value = settings.expandedMinHeight.toFloat(), range = 0f..420f, default = defaults.expandedMinHeight.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(expandedMinHeight = v.toInt()) } }
                )
                SettingSlider(
                    title = "Lekukan sudut", valueText = "${settings.expandedCornerRadius} dp",
                    value = settings.expandedCornerRadius.toFloat(), range = 8f..48f, default = defaults.expandedCornerRadius.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(expandedCornerRadius = v.toInt()) } }
                )
                SettingSlider(
                    title = "Jarak dari capsule (offset Y)", valueText = "${settings.expandedOffsetY} dp",
                    value = settings.expandedOffsetY.toFloat(), range = 0f..80f, default = defaults.expandedOffsetY.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(expandedOffsetY = v.toInt()) } }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kustomisasi · Banner WhatsApp")
        GroupCard {
            SwitchRow(
                pos = RowPos.First,
                icon = Icons.Rounded.Notifications,
                iconColor = Tile.Green,
                title = "Compact banner",
                subtitle = "Banner notifikasi horizontal yang lebih pendek",
                checked = settings.compactBanner,
                onCheckedChange = { v -> onUpdateSettings { it.copy(compactBanner = v) } }
            )
            SwitchRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.Notifications,
                iconColor = Tile.Green,
                title = "Indikator WhatsApp di kiri",
                subtitle = if (settings.whatsAppIndicatorLeft) "Penanda pesan belum dibaca di sisi kiri capsule" else "Penanda pesan belum dibaca di sisi kanan capsule",
                checked = settings.whatsAppIndicatorLeft,
                onCheckedChange = { v -> onUpdateSettings { it.copy(whatsAppIndicatorLeft = v) } }
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val shownBannerW = (if (settings.compactBanner) settings.compactBannerWidth else settings.standardBannerWidth)
                    .coerceIn(180, maxBannerWidthDp)
                // Satu slider lebar untuk kedua mode banner.
                SettingSlider(
                    title = "Lebar banner", valueText = "$shownBannerW dp / maks. $maxBannerWidthDp dp",
                    value = shownBannerW.toFloat(), range = 180f..maxBannerWidthDp.toFloat().coerceAtLeast(181f), default = defaults.compactBannerWidth.toFloat().coerceAtMost(maxBannerWidthDp.toFloat()),
                    onChange = { v -> onUpdateSettings { it.copy(compactBannerWidth = v.toInt(), standardBannerWidth = v.toInt()) } }
                )
                // Satu slider tinggi: hanya untuk mode banner yang sedang aktif.
                if (settings.compactBanner) {
                    SettingSlider(
                        title = "Tinggi banner", valueText = "${settings.compactBannerHeight} dp",
                        value = settings.compactBannerHeight.toFloat(), range = 48f..104f, default = defaults.compactBannerHeight.toFloat(),
                        onChange = { v -> onUpdateSettings { it.copy(compactBannerHeight = v.toInt()) } }
                    )
                } else {
                    SettingSlider(
                        title = "Tinggi banner", valueText = "${settings.standardBannerHeight} dp",
                        value = settings.standardBannerHeight.toFloat(), range = 64f..180f, default = defaults.standardBannerHeight.toFloat(),
                        onChange = { v -> onUpdateSettings { it.copy(standardBannerHeight = v.toInt()) } }
                    )
                }
                SettingSlider(
                    title = "Ukuran teks banner", valueText = "${settings.bannerFontSize} sp",
                    value = settings.bannerFontSize.toFloat(), range = 10f..20f, default = defaults.bannerFontSize.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(bannerFontSize = v.toInt()) } }
                )
                SettingSlider(
                    title = "Ketebalan teks banner", valueText = "${settings.bannerFontWeight}",
                    value = settings.bannerFontWeight.toFloat(), range = 300f..900f, default = defaults.bannerFontWeight.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(bannerFontWeight = v.toInt()) } }
                )
                SettingSlider(
                    title = "Durasi banner", valueText = String.format("%.1f detik", settings.whatsAppBannerMs / 1000f),
                    value = settings.whatsAppBannerMs / 1000f, range = 1.5f..10f, default = defaults.whatsAppBannerMs / 1000f,
                    onChange = { v -> onUpdateSettings { it.copy(whatsAppBannerMs = (v * 2).toInt() * 500) } }
                )
                Hint("Setelah durasi habis, banner menutup otomatis. Pesan tetap tersimpan pada indikator di samping capsule.")
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Sistem · Status bar")
        GroupCard {
            SwitchRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.VisibilityOff,
                iconColor = Tile.Purple,
                title = "Bersihkan ikon status bar",
                subtitle = when (shizukuStatus) {
                    ShizukuStatus.READY -> "Sembunyikan ikon notifikasi + slot sistem via Shizuku icon_blacklist"
                    ShizukuStatus.NOT_RUNNING -> "Shizuku terpasang tetapi belum berjalan · ketuk untuk membukanya"
                    ShizukuStatus.PERMISSION_REQUIRED -> "Izin Shizuku belum diberikan · ketuk untuk mengizinkan"
                    ShizukuStatus.NOT_INSTALLED -> "Shizuku belum terpasang · ketuk untuk memasang"
                },
                checked = settings.hideStatusIcons,
                onCheckedChange = { v ->
                    if (v) onRequestShizuku()
                    onUpdateSettings { it.copy(hideStatusIcons = v) }
                }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kustomisasi · Efek liquid")
        GroupCard {
            SwitchRow(
                pos = RowPos.First,
                icon = Icons.Rounded.AutoAwesome,
                iconColor = Tile.Teal,
                title = "Liquid expansion",
                subtitle = "Konten baru tumbuh dari bentuk sebelumnya, mengikuti pegas island",
                checked = settings.liquidExpand,
                onCheckedChange = { v -> onUpdateSettings { it.copy(liquidExpand = v) } }
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingSlider(
                    title = "Refraksi tepi (edge lens)", valueText = "${(settings.edgeLensStrength * 100).toInt()}%",
                    value = settings.edgeLensStrength, range = 0f..1f, default = defaults.edgeLensStrength,
                    onChange = { v -> onUpdateSettings { it.copy(edgeLensStrength = v) } }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kustomisasi · Material glass")
        GroupCard {
            // STAGE 70: the Low-End switch moved to "Sistem · Performa & baterai" so the two
            // performance modes are explained side by side and never confused with glass looks.
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingSlider(
                    title = "Highlight tepi", valueText = "${(settings.glassConfig.edgeHighlight * 100).toInt()}%",
                    value = settings.glassConfig.edgeHighlight, range = 0f..1.5f, default = defaults.glassConfig.edgeHighlight,
                    onChange = { v -> onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(edgeHighlight = v)) } }
                )
                SettingSlider(
                    title = "Pantulan ambient", valueText = "${(settings.glassConfig.ambientReflection * 100).toInt()}%",
                    value = settings.glassConfig.ambientReflection, range = 0f..1f, default = defaults.glassConfig.ambientReflection,
                    onChange = { v -> onUpdateSettings { it.copy(glassConfig = it.glassConfig.copy(ambientReflection = v)) } }
                )
                SettingSlider(
                    title = "Kilau · Expanded", valueText = "${(settings.profileSheenExpanded * 100).toInt()}%",
                    value = settings.profileSheenExpanded, range = 0f..1.5f, default = defaults.profileSheenExpanded,
                    onChange = { v -> onUpdateSettings { it.copy(profileSheenExpanded = v) } }
                )
                SettingSlider(
                    title = "Kilau · Banner", valueText = "${(settings.profileSheenBanner * 100).toInt()}%",
                    value = settings.profileSheenBanner, range = 0f..1.5f, default = defaults.profileSheenBanner,
                    onChange = { v -> onUpdateSettings { it.copy(profileSheenBanner = v) } }
                )
                Text(
                    "Kembalikan default glass",
                    color = Ui.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable {
                            onUpdateSettings {
                                it.copy(
                                    glassConfig = defaults.glassConfig,
                                    sheenStrength = defaults.sheenStrength,
                                    glassSurfaceOpacity = defaults.glassSurfaceOpacity,
                                    backgroundOpacity = defaults.backgroundOpacity,
                                    edgeLensStrength = defaults.edgeLensStrength,
                                    profileSheenExpanded = defaults.profileSheenExpanded,
                                    profileSheenBanner = defaults.profileSheenBanner
                                )
                            }
                        }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kustomisasi · Animasi")
        GroupCard {
            Column(Modifier.padding(16.dp)) {
                Text("Profil animasi", color = Ui.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                ChipRow(
                    options = AnimationPreset.entries.map { it.label },
                    selected = AnimationPreset.entries.firstOrNull { it.matches(settings) }?.label ?: "",
                    onSelect = { lbl ->
                        onUpdateSettings { s -> AnimationPreset.entries.first { it.label == lbl }.applyTo(s) }
                    }
                )
                Hint(
                    "Rekomendasi: iOS Ringan. Membuka dan menutup seperti iOS tanpa pantulan dan tanpa blur. " +
                        "Jelly Halus untuk efek memantul, Stabil untuk paling sederhana. " +
                        "Mengubah gaya, elastisitas, atau kecepatan di bawah menjadikan profil Kustom."
                )
                Spacer(Modifier.height(12.dp))
                Text("Gaya animasi", color = Ui.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                ChipRow(
                    options = listOf("Stabil", "Mengalir", "Jelly"),
                    selected = when (settings.animationOption) {
                        AnimationOption.OPTION_1 -> "Stabil"
                        AnimationOption.OPTION_2 -> "Mengalir"
                        AnimationOption.OPTION_3 -> "Jelly"
                    },
                    onSelect = { selected ->
                        onUpdateSettings {
                            it.copy(
                                animationOption = when {
                                    selected == "Jelly" -> AnimationOption.OPTION_3
                                    selected == "Mengalir" -> AnimationOption.OPTION_2
                                    else -> AnimationOption.OPTION_1
                                }
                            )
                        }
                    }
                )
                Hint(
                    when (settings.animationOption) {
                        AnimationOption.OPTION_1 -> "Animasi stabil, dipertahankan sebagai pilihan pertama."
                        AnimationOption.OPTION_2 -> "Kurva spring lebih mengalir dengan reveal bertahap; efek dekoratif dibatasi pada mode hemat daya."
                        AnimationOption.OPTION_3 -> "Jelly: lebar membuka lebih dulu lalu tinggi menyusul dengan sedikit memantul (squash & stretch)."
                    }
                )
                Spacer(Modifier.height(12.dp))
                Text("Cara membuka island", color = Ui.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                val expandStyleOptions = listOf(
                    ExpandStyle.SPRING to "Pegas (memantul)",
                    ExpandStyle.SMOOTH to "Halus",
                    ExpandStyle.INSTANT to "Instan"
                )
                ChipRow(
                    options = expandStyleOptions.map { it.second },
                    selected = expandStyleOptions.firstOrNull { it.first == settings.expandStyle }?.second ?: "Pegas (memantul)",
                    onSelect = { label ->
                        val picked = expandStyleOptions.firstOrNull { it.second == label } ?: (ExpandStyle.SPRING to label)
                        onUpdateSettings { it.copy(expandStyle = picked.first) }
                    }
                )
                Hint(
                    when (settings.expandStyle) {
                        ExpandStyle.SPRING -> "Pegas: elastisitas dan kecepatan berlaku."
                        ExpandStyle.SMOOTH -> "Halus tanpa pantulan; elastisitas tidak dipakai."
                        ExpandStyle.INSTANT -> "Instan tanpa animasi; elastisitas dan kecepatan tidak dipakai."
                    }
                )
                // Elastisitas hanya berarti untuk pegas; kecepatan tidak berarti untuk Instant.
                Reveal(settings.expandStyle == ExpandStyle.SPRING) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Text("Kelembutan jelly (seberapa memantul)", color = Ui.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        val elasticityOptions = listOf(
                            Elasticity.NONE to "Kaku",
                            Elasticity.LOW to "Sedang",
                            Elasticity.MEDIUM to "Lunak",
                            Elasticity.HIGH to "Sangat lunak"
                        )
                        ChipRow(
                            options = elasticityOptions.map { it.second },
                            selected = elasticityOptions.firstOrNull { it.first == settings.elasticity }?.second ?: "Sangat lunak",
                            onSelect = { label ->
                                val picked = elasticityOptions.firstOrNull { it.second == label } ?: (Elasticity.HIGH to label)
                                onUpdateSettings { it.copy(elasticity = picked.first) }
                            }
                        )
                    }
                }
                Reveal(settings.expandStyle != ExpandStyle.INSTANT) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        SettingSlider(
                            title = "Kecepatan", valueText = "%.1fx".format(settings.animationSpeed),
                            value = settings.animationSpeed, range = 0.05f..2f, default = defaults.animationSpeed,
                            onChange = { v -> onUpdateSettings { it.copy(animationSpeed = v) } }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("Haptik")
                SwitchRow(
                    pos = RowPos.Single,
                    icon = Icons.Rounded.Notifications,
                    iconColor = Tile.Purple,
                    title = "Getaran halus",
                    subtitle = "Tick ringan di island dan saat menggeser slider pengaturan",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(hapticsEnabled = v) } }
                )
                Spacer(Modifier.height(14.dp))
                SectionLabel("Glass Sweep · Sapuan kaca")
                SwitchRow(
                    pos = RowPos.Single,
                    icon = Icons.Rounded.Notifications,
                    iconColor = Tile.Purple,
                    title = "Glass Sweep satu kali",
                    subtitle = "Menyapu permukaan kaca saat expand; bukan background",
                    checked = settings.glassSweepEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(glassSweepEnabled = v) } }
                )
                Reveal(settings.glassSweepEnabled) {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SettingSlider(
                            title = "Opacity sweep", valueText = "${(settings.glassSweepOpacity * 100).toInt()}%",
                            value = settings.glassSweepOpacity, range = 0f..1f, default = defaults.glassSweepOpacity,
                            onChange = { v -> onUpdateSettings { it.copy(glassSweepOpacity = v) } }
                        )
                        SettingSlider(
                            title = "Durasi lintasan sweep", valueText = "%.1fs".format(settings.glassSweepDurationMs / 1000f),
                            value = settings.glassSweepDurationMs.toFloat(), range = 500f..2600f, default = defaults.glassSweepDurationMs.toFloat(),
                            onChange = { v -> onUpdateSettings { it.copy(glassSweepDurationMs = v.toInt()) } }
                        )
                    }
                }
                // Efek lirik hanya relevan kalau lirik ditampilkan.
                Reveal(settings.showLyrics) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        SectionLabel("Lirik · Shine bergerak")
                        SwitchRow(
                            pos = RowPos.Single,
                            icon = Icons.Rounded.MusicNote,
                            iconColor = Tile.Purple,
                            title = "Efek shine lirik",
                            subtitle = "Kilau putih bergerak di atas huruf (dibatasi ±30 fps agar hemat baterai)",
                            checked = settings.lyricShineEnabled,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(lyricShineEnabled = v) } }
                        )
                        Reveal(settings.lyricShineEnabled) {
                            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SettingSlider(
                                    title = "Kecepatan shine", valueText = "%.2fx".format(settings.lyricShineSpeed),
                                    value = settings.lyricShineSpeed, range = 0.25f..2f, default = defaults.lyricShineSpeed,
                                    onChange = { v -> onUpdateSettings { it.copy(lyricShineSpeed = v) } }
                                )
                                SettingSlider(
                                    title = "Intensitas shine", valueText = "%.0f%%".format(settings.lyricShineIntensity * 100f),
                                    value = settings.lyricShineIntensity, range = 0f..0.8f, default = defaults.lyricShineIntensity,
                                    onChange = { v -> onUpdateSettings { it.copy(lyricShineIntensity = v) } }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Sistem · Preview & demo")
        GroupCard {
            val demos = listOf(
                Triple("media", "Demo pemutar media", "Compact, expanded, progress, dan lirik"),
                Triple("media_paused", "Demo media dijeda", "Tampilan saat lagu berhenti"),
                Triple("whatsapp", "Demo WhatsApp", "Banner satu pesan"),
                Triple("whatsapp_group", "Demo WhatsApp grup", "Tumpukan tiga pesan"),
                Triple("whatsapp_long", "Demo pesan panjang", "Teks yang memanjang dan terpotong"),
                Triple("call_in", "Demo panggilan masuk", "Tombol terima dan tolak"),
                Triple("call_active", "Demo panggilan berlangsung", "Durasi berjalan"),
                Triple("both", "Demo media + WhatsApp", "Uji prioritas banner di atas media"),
                Triple("stop", "Hentikan semua demo", "Bersihkan semua contoh di island")
            )
            demos.forEachIndexed { i, (key, title, sub) ->
                SettingsRow(
                    pos = when {
                        demos.size == 1 -> RowPos.Single
                        i == 0 -> RowPos.First
                        i == demos.lastIndex -> RowPos.Last
                        else -> RowPos.Middle
                    },
                    icon = Icons.Rounded.PlayArrow,
                    iconColor = Tile.Purple,
                    title = title,
                    subtitle = sub,
                    onClick = { onDemoScenario(key) }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Kontrol · Gesture")
        GroupCard {
            SwitchRow(
                pos = RowPos.First,
                icon = Icons.Rounded.Swipe,
                iconColor = Tile.Blue,
                title = "Swipe ganti lagu",
                subtitle = "Geser kiri/kanan di capsule musik untuk lagu berikut/sebelumnya",
                checked = settings.swipeTracks,
                onCheckedChange = { v -> onUpdateSettings { it.copy(swipeTracks = v) } }
            )
            SwitchRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.SwipeVertical,
                iconColor = Tile.Teal,
                title = "Swipe expand",
                subtitle = "Geser ke atas di capsule untuk membuka island",
                checked = settings.swipeExpand,
                onCheckedChange = { v -> onUpdateSettings { it.copy(swipeExpand = v) } }
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tap", color = Ui.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                ChipRow(
                    options = TapAction.entries.map { it.label },
                    selected = settings.tapAction.label,
                    perRow = 3,
                    onSelect = { lbl -> onUpdateSettings { s -> s.copy(tapAction = TapAction.entries.first { it.label == lbl }) } }
                )
                Spacer(Modifier.height(4.dp))
                Text("Double tap", color = Ui.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                ChipRow(
                    options = TapAction.entries.map { it.label },
                    selected = settings.doubleTapAction.label,
                    perRow = 3,
                    onSelect = { lbl -> onUpdateSettings { s -> s.copy(doubleTapAction = TapAction.entries.first { it.label == lbl }) } }
                )
                Spacer(Modifier.height(4.dp))
                Text("Long press", color = Ui.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                ChipRow(
                    options = TapAction.entries.map { it.label },
                    selected = settings.longPressAction.label,
                    perRow = 3,
                    onSelect = { lbl -> onUpdateSettings { s -> s.copy(longPressAction = TapAction.entries.first { it.label == lbl }) } }
                )
                Hint(
                    buildString {
                        append("Berlaku di semua tampilan capsule. ")
                        if (settings.doubleTapAction == TapAction.NOTHING) append("Double tap mati, jadi tap merespons langsung tanpa jeda. ")
                        append("Putar/Jeda dan Berikutnya hanya untuk musik; di chat atau panggilan akan membuka island.")
                    }
                )
            }
            SettingsRow(
                pos = RowPos.Last,
                icon = Icons.Rounded.PlayArrow,
                iconColor = Tile.Purple,
                title = "Coba gesture di island",
                subtitle = "Tampilkan demo musik, lalu coba tap, double tap, long press, dan swipe",
                onClick = { onDemoScenario("media") }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Sistem · Cakupan")
        GroupCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Hanya Media & WhatsApp.\nPanggilan, alarm, navigasi, dan app lain tidak ditampilkan.",
                    color = Ui.Sub,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // ---- Asisten AI --------------------------------------------------------
        Spacer(Modifier.height(18.dp))
        SectionLabel("Asisten AI")
        GroupCard {
            SwitchRow(
                pos = RowPos.First,
                icon = Icons.Rounded.AutoAwesome,
                iconColor = Tile.Purple,
                title = "Asisten AI",
                subtitle = "Tanya AI langsung dari island. Pilih penyedia dan sambungkan sekali dari sini.",
                checked = settings.aiEnabled,
                onCheckedChange = { v -> onUpdateSettings { it.copy(aiEnabled = v) } }
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                // Only the essentials are required for normal chat. Screen reading is optional.
                val isLocalAi = isLocalAiEndpoint(settings.aiBaseUrl)
                val setupReady = settings.aiProvider.isNotBlank() &&
                    (settings.aiApiKey.isNotBlank() || isLocalAi) && aiConnectionVerified
                if (!setupReady) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Tile.Purple.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Hubungkan asisten AI", color = Ui.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                settings.aiProvider.isBlank() -> "1. Pilih penyedia AI."
                                settings.aiApiKey.isBlank() && !isLocalAi -> "2. Tempel API key dari akun penyedia AI."
                                aiSetupBusy -> "Sedang memeriksa koneksi dan daftar model…"
                                else -> "3. Tekan ‘Uji koneksi’. Setelah berhasil, pilih model yang tersedia."
                            },
                            color = Ui.Sub, fontSize = 12.sp, lineHeight = 17.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Text("Asisten AI sudah terhubung.", color = Tile.Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                }
                Text("Penyedia AI", color = Ui.Sub, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                val selectedPreset = AI_PROVIDER_PRESETS.firstOrNull {
                    it.id != "custom" && AiClient.canonicalBaseUrl(it.provider, it.baseUrl) == settings.aiBaseUrl
                } ?: AI_PROVIDER_PRESETS.firstOrNull {
                    it.id != "custom" && it.provider == settings.aiProvider
                } ?: AI_PROVIDER_PRESETS.last()
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AI_PROVIDER_PRESETS.forEach { preset ->
                        ProviderChip(
                            label = preset.label,
                            selected = preset.id == selectedPreset.id,
                            onClick = {
                                onUpdateSettings {
                                    if (preset.id == "custom") it.copy(aiProvider = "openai")
                                    else {
                                        val currentPresetUrl = AI_PROVIDER_PRESETS.firstOrNull { current ->
                                            current.id != "custom" && AiClient.canonicalBaseUrl(current.provider, current.baseUrl) == it.aiBaseUrl
                                        }?.baseUrl ?: AI_PROVIDER_PRESETS.firstOrNull { candidate ->
                                            candidate.id != "custom" && candidate.provider == it.aiProvider
                                        }?.baseUrl
                                        val nextUrl = AiClient.baseUrlForProviderChange(
                                            it.aiBaseUrl, currentPresetUrl, preset.provider, preset.baseUrl
                                        )
                                        it.copy(aiProvider = preset.provider, aiBaseUrl = nextUrl, aiModel = preset.defaultModel)
                                    }
                                }
                                aiConnectionVerified = false
                                availableAiModels = emptyList()
                                aiSetupMessage = "Penyedia berubah. Uji koneksi untuk memastikan pengaturan baru berfungsi."
                            }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when (selectedPreset.id) {
                        "ollama" -> "Untuk Ollama yang berjalan di perangkat ini. Jika Ollama ada di PC, ubah alamat server di Pengaturan lanjutan."
                        "custom" -> "Untuk layanan AI lain. Alamat server dan nama model perlu diisi manual."
                        "groq" -> "Tempel API key Groq. Batas penggunaan mengikuti paket akun Groq kamu."
                        "gemini" -> "Gunakan API key dari Google AI Studio. Model akan dicari otomatis."
                        "openrouter" -> "Satu akun untuk mengakses berbagai model AI. Gunakan API key OpenRouter."
                        else -> "Endpoint bawaan sudah disiapkan. Cukup masukkan API key; model akan dimuat dari katalog provider dan bisa kamu pilih sendiri."
                    },
                    color = Ui.Sub, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
            SettingsRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.Key,
                iconColor = Tile.Orange,
                title = when (settings.aiProvider) {
                    "gemini" -> "Kunci akses Google AI Studio"
                    "groq" -> "Kunci akses Groq"
                    else -> "Kunci akses AI (API key)"
                },
                subtitle = if (settings.aiApiKey.isBlank()) {
                    if (isLocalAiEndpoint(settings.aiBaseUrl)) "Tidak diperlukan untuk server lokal" else "Belum diisi · ketuk untuk memasukkan"
                } else "Sudah diisi · ketuk untuk mengganti",
                onClick = { showAiDialog = AiDialogKind.API_KEY }
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (aiConnectionVerified) "Koneksi berhasil" else "Belum diuji",
                        color = if (aiConnectionVerified) Tile.Teal else Ui.Sub,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        aiSetupMessage.ifBlank {
                            if (settings.aiApiKey.isNotBlank() && settings.aiModel.isNotBlank())
                                "Pengaturan sudah terisi. Uji koneksi untuk memeriksa apakah layanan tersedia sekarang."
                            else "Uji koneksi sebelum mulai menggunakan AI."
                        },
                        color = if (aiSetupMessage.contains("gagal", true) || aiSetupMessage.contains("belum berhasil", true)) Tile.Orange else Ui.Sub,
                        fontSize = 11.sp, lineHeight = 15.sp
                    )
                }
                TextButton(
                    enabled = !aiSetupBusy && (settings.aiApiKey.isNotBlank() || isLocalAiEndpoint(settings.aiBaseUrl)),
                    onClick = { loadAiModels(settings.aiProvider, settings.aiBaseUrl, settings.aiApiKey, testChat = true) }
                ) { Text(if (aiSetupBusy) "Memeriksa…" else "Uji koneksi chat") }
            }
            Text(
                "Tes ini mengirim satu pesan singkat. Penyedia AI mungkin menghitung sedikit penggunaan.",
                color = Ui.Sub, fontSize = 10.sp, lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            TextButton(
                onClick = { showAiAdvancedSettings = !showAiAdvancedSettings },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(if (showAiAdvancedSettings) "Tutup pengaturan lanjutan" else "Pengaturan lanjutan")
            }
            AnimatedVisibility(
                visible = showAiAdvancedSettings,
                enter = fadeIn(tween(160)) + expandVertically(tween(160)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
            ) {
                Column {
                    SettingsRow(
                        pos = RowPos.Middle, icon = Icons.Rounded.TravelExplore, iconColor = Tile.Teal,
                        title = "Brave Web Search",
                        subtitle = if (settings.aiBraveSearchKey.isBlank()) "Belum dikonfigurasi · gunakan pencarian terbatas sebagai cadangan" else "Kunci tersimpan terenkripsi · pencarian web utama aktif",
                        onClick = { showAiDialog = AiDialogKind.BRAVE_SEARCH_KEY }
                    )
                    SettingsRow(
                        pos = RowPos.Middle, icon = Icons.Rounded.Link, iconColor = Tile.Blue,
                        title = "Alamat server (Base URL)", subtitle = settings.aiBaseUrl,
                        onClick = { showAiDialog = AiDialogKind.BASE_URL }
                    )
                    SettingsRow(
                        pos = RowPos.Middle, icon = Icons.Rounded.Key, iconColor = Tile.Orange,
                        title = "Kunci cadangan",
                        subtitle = settings.aiExtraKeys.lines().count { it.isNotBlank() }.let { n -> if (n == 0) "Belum ada" else "$n kunci tersimpan" },
                        onClick = { showAiDialog = AiDialogKind.EXTRA_KEYS }
                    )
                    SettingsRow(
                        pos = RowPos.Middle, icon = Icons.Rounded.Key, iconColor = Tile.Teal,
                        title = "Penyedia cadangan",
                        subtitle = settings.aiFallbackApis.lines().count { it.isNotBlank() }.let { n -> if (n == 0) "Belum diatur" else "$n penyedia tersimpan" },
                        onClick = { showAiDialog = AiDialogKind.FALLBACK_APIS }
                    )
                    SettingsRow(
                        pos = RowPos.Middle, icon = Icons.Rounded.Memory, iconColor = Tile.Teal,
                        title = "Model AI",
                        subtitle = if (availableAiModels.isNotEmpty()) "${settings.aiModel} · ${availableAiModels.size} tersedia" else settings.aiModel.ifBlank { "Belum dipilih" },
                        onClick = { if (availableAiModels.isNotEmpty()) showAiModelPicker = true else showAiDialog = AiDialogKind.MODEL }
                    )
                    SettingsRow(
                        pos = RowPos.Middle, icon = Icons.Rounded.Description, iconColor = Tile.Gray,
                        title = "Instruksi khusus AI",
                        subtitle = settings.aiSystemPrompt.ifBlank { "Opsional · gunakan aturan jawaban sendiri" },
                        onClick = { showAiDialog = AiDialogKind.SYSTEM_PROMPT }
                    )
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        SettingSlider(
                            title = "Temperature (kreativitas)",
                            valueText = String.format(java.util.Locale.US, "%.1f", settings.aiTemperature),
                            value = settings.aiTemperature, range = 0f..2f, default = 0.7f,
                            onChange = { v -> onUpdateSettings { it.copy(aiTemperature = v) } }
                        )
                        Spacer(Modifier.height(10.dp))
                        SettingSlider(
                            title = "Batas panjang jawaban",
                            valueText = "${settings.aiMaxTokens} kata",
                            value = settings.aiMaxTokens.toFloat(), range = 1024f..16384f, default = 4096f,
                            onChange = { v -> onUpdateSettings { it.copy(aiMaxTokens = v.toInt()) } }
                        )
                    }
                }
            }
            SwitchRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.Memory,
                iconColor = Tile.Teal,
                title = "Memori AI lokal terenkripsi",
                subtitle = "Nonaktif secara default. Saat aktif, AI hanya menyimpan hal yang Anda minta secara eksplisit dengan frasa ‘ingat …’. Data dienkripsi di perangkat. Perintah: ‘apa yang kamu ingat’ atau ‘hapus semua memori’.",
                checked = settings.aiMemoryEnabled,
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(aiMemoryEnabled = enabled) } }
            )
            SwitchRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.Visibility,
                iconColor = Tile.Purple,
                title = "Analisis layar saat diminta",
                subtitle = "AI membaca app aktif + teks layar HANYA saat Anda memintanya (chip Analisis Layar atau pertanyaan soal layar). Tidak ada pembacaan diam-diam.",
                checked = settings.aiScreenAnalysis,
                onCheckedChange = { v -> onUpdateSettings { it.copy(aiScreenAnalysis = v) } }
            )
            SwitchRow(
                pos = RowPos.Middle,
                icon = Icons.Rounded.Speed,
                iconColor = Tile.Green,
                title = "Streaming jawaban",
                subtitle = "Jawaban tampil bertahap seiring token masuk. Matikan bila endpoint tidak mendukung streaming.",
                checked = settings.aiStreaming,
                onCheckedChange = { v -> onUpdateSettings { it.copy(aiStreaming = v) } }
            )
            // STAGE 70: panel AI bisa dipanjangkan dari sini — sesuai permintaan
            // "bisa di panjangin lagi panjang panel AI nya". Isi chat yang lebih tinggi
            // dari nilai ini tetap mengikuti isi, jadi chat panjang tidak pernah terpotong.
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                SettingSlider(
                    title = "Panjang panel AI",
                    valueText = "${settings.aiPanelMinHeight} dp",
                    value = settings.aiPanelMinHeight.toFloat(), range = 320f..880f, default = 520f,
                    onChange = { v -> onUpdateSettings { it.copy(aiPanelMinHeight = v.toInt()) } }
                )
                Hint("Geser ke kanan untuk panel yang lebih tinggi (maksimal sekitar 94% tinggi layar). Panel otomatis tetap membesar bila isi chat lebih panjang.")
            }
            SettingsRow(
                pos = RowPos.Last,
                icon = Icons.Rounded.DeleteOutline,
                iconColor = Tile.Red,
                title = "Hapus riwayat chat",
                subtitle = "Bersihkan maksimal 30 pesan yang tersimpan di perangkat",
                onClick = { showAiClearConfirm = true }
            )
        }
        if (settings.aiEnabled && settings.aiScreenAnalysis && a11yCanReadScreen == false) {
            Spacer(Modifier.height(8.dp))
            GroupCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Analisis layar butuh pembacaan konten: matikan lalu nyalakan kembali Accessibility \u201CAndroid Island\u201D di Pengaturan sekali saja, agar izin baru aktif.",
                        color = Tile.Orange,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        GroupCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Cara membuka AI Island",
                    color = Ui.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "1. Ketuk kapsul island lalu ketuk ikon \u2728 AI di panel bawah.",
                    color = Ui.Sub,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Text(
                    "2. Tekan ganda tombol power \u2014 One UI: Pengaturan > Fitur lanjutan > Side button > Tekan dua kali > Buka app > pilih \u201CAI Island\u201D.",
                    color = Ui.Sub,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Hint("Agar long-press tombol power membuka AI Island: Settings > Aplikasi > Aplikasi default > Asisten digital > pilih AI Island. Pastikan Accessibility Android Island aktif.")
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Pemulihan")
        GroupCard {
            SettingsRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.RestartAlt,
                iconColor = Tile.Orange,
                title = "Kembalikan semua ke awal",
                subtitle = "Semua pengaturan dikembalikan ke bawaan, termasuk kunci AI dan model yang tersimpan (perlu diisi ulang). Izin sistem tidak berubah.",
                onClick = { showResetConfirm = true }
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            if (ready) "Siap · Android Island ${IslandAccessibilityService.isRunning.let { if (it) "· overlay ON" else "" }}"
            else "Aktifkan izin di atas",
            color = Ui.Sub.copy(0.7f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        Spacer(Modifier.height(20.dp))
    }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Kembalikan semua ke awal?") },
            text = { Text("Seluruh pengaturan (termasuk kunci AI, model, dan tampilan) dihapus dan kembali ke bawaan. Kamu perlu memasang AI lagi dari bagian Asisten AI. Izin Accessibility dan Notification Listener tidak terpengaruh.") },
            confirmButton = {
                TextButton(onClick = { showResetConfirm = false; onApplyRecommended() }) { Text("Kembalikan") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Batal") } }
        )
    }

    if (showDiagnostics) {
        AlertDialog(
            onDismissRequest = { showDiagnostics = false },
            title = { Text("Diagnostik Android Island") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        diagnosticLines.joinToString("\n").ifBlank { "Belum ada catatan. Error akan direkam otomatis; aktifkan Perekaman log untuk merekam aktivitas detail." },
                        color = Ui.Sub,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aiScope.launch {
                        val exported = IslandDiagnostics.exportTextAsync()
                        if (exported.isBlank()) {
                            Toast.makeText(context, "Log kosong. Aktifkan Perekaman log terlebih dahulu.", Toast.LENGTH_SHORT).show()
                        } else {
                            clipboard.setText(AnnotatedString(exported))
                            Toast.makeText(context, "Log berhasil disalin (${exported.length} karakter).", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Salin semua") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { IslandDiagnostics.clear() }) { Text("Hapus") }
                    TextButton(onClick = { showDiagnostics = false }) { Text("Tutup") }
                }
            }
        )
    }

    if (showAiClearConfirm) {
        AlertDialog(
            onDismissRequest = { showAiClearConfirm = false },
            title = { Text("Hapus riwayat chat AI?") },
            text = { Text("Semua percakapan yang tersimpan di perangkat (maksimal 30 pesan) akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = { showAiClearConfirm = false; AiSession.clearHistory() }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showAiClearConfirm = false }) { Text("Batal") } }
        )
    }

    if (showAiModelPicker) {
        AlertDialog(
            onDismissRequest = { showAiModelPicker = false },
            title = { Text("Pilih model AI") },
            text = {
                Column {
                    Text("Model berikut diambil langsung dari endpoint provider menggunakan API key kamu.", color = Ui.Sub, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(availableAiModels, key = { it }) { modelId ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onUpdateSettings { it.copy(aiModel = modelId) }
                                    aiConnectionVerified = false
                                    showAiModelPicker = false
                                    aiSetupMessage = "Model dipilih. Tekan ‘Uji koneksi chat’ untuk memastikan model ini berfungsi."
                                }
                            ) {
                                Text(if (settings.aiModel == modelId) "✓  $modelId" else modelId, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAiModelPicker = false }) { Text("Tutup") } },
            dismissButton = { TextButton(onClick = { loadAiModels(settings.aiProvider, settings.aiBaseUrl, settings.aiApiKey) }) { Text("Muat ulang") } }
        )
    }

    when (showAiDialog) {
        AiDialogKind.BRAVE_SEARCH_KEY -> AiTextDialog(
            title = "Brave Search API key",
            subtitle = "Opsional. Digunakan khusus untuk pencarian web AI melalui Brave LLM Context. Kunci dienkripsi di perangkat dan tidak dikirim ke penyedia model AI. Tanpa kunci ini, aplikasi memakai pencarian alternatif terbatas. API key harus berasal dari akun yang sah; jangan bagikan kunci kepada siapa pun.",
            initial = settings.aiBraveSearchKey,
            placeholder = "Tempel Brave Search API key",
            password = true,
            onDismiss = { showAiDialog = null },
            onConfirm = { value ->
                onUpdateSettings { it.copy(aiBraveSearchKey = value.trim()) }
                showAiDialog = null
            }
        )
        AiDialogKind.BASE_URL -> AiTextDialog(
            title = "Base URL",
            subtitle = "Base URL dapat kamu ubah kapan saja. URL manual akan dipertahankan dan tidak ditimpa saat model diganti atau provider dipilih ulang. Gemini OpenAI-compatible: https://generativelanguage.googleapis.com/v1beta/openai/.",
            initial = settings.aiBaseUrl,
            placeholder = "https://api.openai.com/v1/chat/completions",
            onDismiss = { showAiDialog = null },
            onConfirm = { v ->
                val canonical = AiClient.canonicalBaseUrl(settings.aiProvider, v)
                onUpdateSettings { it.copy(aiBaseUrl = canonical) }
                aiConnectionVerified = false
                if (settings.aiApiKey.isNotBlank()) loadAiModels(settings.aiProvider, canonical, settings.aiApiKey)
                showAiDialog = null
            }
        )
        AiDialogKind.API_KEY -> AiTextDialog(
            title = "API key",
            subtitle = "Tempel kunci dari layanan AI yang dipilih. Kunci disimpan aman di perangkat dan tidak ditampilkan kembali.",
            initial = settings.aiApiKey,
            placeholder = "sk-...",
            password = true,
            onDismiss = { showAiDialog = null },
            onConfirm = { v ->
                autoConfigureAi(v)
                showAiDialog = null
            }
        )
        AiDialogKind.EXTRA_KEYS -> AiTextDialog(
            title = "Kunci cadangan",
            subtitle = "Opsional. Tambahkan kunci lain dari akun penyedia yang sama, satu kunci per baris. Android Island akan mencoba kunci berikutnya jika kunci utama sedang dibatasi.",
            initial = settings.aiExtraKeys,
            placeholder = "kunci-cadangan-1",
            singleLine = false,
            onDismiss = { showAiDialog = null },
            onConfirm = { v ->
                onUpdateSettings { it.copy(aiExtraKeys = v) }
                showAiDialog = null
            }
        )
        AiDialogKind.FALLBACK_APIS -> AiTextDialog(
            title = "API cadangan lintas provider",
            subtitle = "Dipakai otomatis saat API utama terkena kuota/batas permintaan, akses ditolak, atau server bermasalah. Format setiap baris: provider|Base URL|model|API key. Provider: openai, gemini, groq, deepseek, openrouter, mistral, together, fireworks, cerebras, xai, ollama, atau custom OpenAI-compatible. Tambahkan minimal satu API cadangan yang aktif dan masih memiliki kuota. Kunci disimpan terenkripsi di perangkat.",
            initial = settings.aiFallbackApis,
            placeholder = "groq|https://api.groq.com/openai/v1|openai/gpt-oss-120b|gsk-KEY",
            singleLine = false,
            password = true,
            onDismiss = { showAiDialog = null },
            onConfirm = { v ->
                onUpdateSettings { it.copy(aiFallbackApis = v) }
                showAiDialog = null
            }
        )
        AiDialogKind.MODEL -> AiTextDialog(
            title = "Model",
            subtitle = "Nama model persis seperti di endpoint. Contoh: gpt-5.6-sol (OpenAI) · gemini-3.5-flash-lite (Gemini) · llama3.2 (Ollama)",
            initial = settings.aiModel,
            placeholder = "Pilih dari daftar model atau masukkan ID model yang valid",
            onDismiss = { showAiDialog = null },
            onConfirm = { v ->
                onUpdateSettings { it.copy(aiModel = v.trim()) }
                showAiDialog = null
            }
        )
        AiDialogKind.SYSTEM_PROMPT -> AiTextDialog(
            title = "Instruksi khusus AI",
            subtitle = "Instruksi tambahan untuk AI (kepribadian, aturan, format jawaban). Digabung dengan instruksi bawaan island.",
            initial = settings.aiSystemPrompt,
            placeholder = "Contoh: Jawab selalu singkat, maksimal 3 kalimat.",
            singleLine = false,
            onDismiss = { showAiDialog = null },
            onConfirm = { v ->
                onUpdateSettings { it.copy(aiSystemPrompt = v.trim()) }
                showAiDialog = null
            }
        )
        null -> {}
    }

}

private fun isLocalAiEndpoint(url: String): Boolean {
    val host = runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() ?: return false
    val octets = host.split('.').mapNotNull { it.toIntOrNull() }
    return host == "localhost" || host.endsWith(".local") || host == "::1" ||
        host.startsWith("127.") || host.startsWith("192.168.") || host.startsWith("10.") ||
        (octets.size == 4 && octets[0] == 172 && octets[1] in 16..31)
}

private enum class AiDialogKind { BRAVE_SEARCH_KEY, BASE_URL, API_KEY, EXTRA_KEYS, FALLBACK_APIS, MODEL, SYSTEM_PROMPT }

/** One tap fills Base URL + Model for a known provider, so only the API key is left to type. */
private data class AiProviderPreset(
    val id: String,
    val label: String,
    val provider: String,
    val baseUrl: String,
    val defaultModel: String,
    /** Family used to pick the newest model automatically; null means the user chooses the model. */
    val familyRegex: Regex? = null
)

private val AI_PROVIDER_PRESETS = listOf(
    // Defaults checked against each provider's own docs in November 2026 (OpenAI, Gemini, DeepSeek).
    // Where a name could not be confirmed the default stays empty and the live /models list decides.
    AiProviderPreset("openai", "OpenAI", "openai", "https://api.openai.com/v1/chat/completions", "", com.android.island.ai.ModelPicker.GPT_SOL),
    AiProviderPreset("gemini", "Gemini", "gemini", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions", "gemini-3.5-flash-lite", com.android.island.ai.ModelPicker.GEMINI_FLASH),
    AiProviderPreset("openrouter", "OpenRouter", "openai", "https://openrouter.ai/api/v1/chat/completions", ""),
    AiProviderPreset("groq", "Groq", "groq", "https://api.groq.com/openai/v1/chat/completions", ""),
    AiProviderPreset("deepseek", "DeepSeek", "deepseek", "https://api.deepseek.com/chat/completions", "deepseek-flash"),
    AiProviderPreset("mistral", "Mistral", "openai", "https://api.mistral.ai/v1/chat/completions", ""),
    AiProviderPreset("together", "Together AI", "openai", "https://api.together.ai/v1/chat/completions", ""),
    AiProviderPreset("fireworks", "Fireworks", "openai", "https://api.fireworks.ai/inference/v1/chat/completions", ""),
    AiProviderPreset("cerebras", "Cerebras", "openai", "https://api.cerebras.ai/v1/chat/completions", ""),
    AiProviderPreset("xai", "xAI", "openai", "https://api.x.ai/v1/chat/completions", "", Regex("""^grok-(\d+(?:\.\d+)*)$""")),
    AiProviderPreset("ollama", "Ollama lokal", "openai", "http://127.0.0.1:11434/v1/chat/completions", ""),
    AiProviderPreset("custom", "Kustom", "openai", "", "")
)

@Composable
private fun ProviderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Tile.Purple.copy(alpha = 0.22f) else Ui.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Tile.Purple else Ui.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun AiTextDialog(
    title: String,
    subtitle: String? = null,
    initial: String,
    placeholder: String,
    singleLine: Boolean = true,
    password: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (subtitle != null) {
                    Text(subtitle, color = Ui.Sub, fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text(placeholder, color = Ui.Sub, fontSize = 13.sp) },
                    singleLine = singleLine,
                    visualTransformation = if (password) PasswordVisualTransformation()
                        else androidx.compose.ui.text.input.VisualTransformation.None,
                    minLines = if (singleLine) 1 else 3,
                    maxLines = if (singleLine) 1 else 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun SectionLabel(text: String) {
    // STAGE 70: robotic section header — a neon gradient marker plus monospace caps.
    // Static draw (no animation), so section headers cost nothing while scrolling.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(Tile.Teal, Tile.Purple)))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text.uppercase(),
            color = Ui.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.1.sp
        )
    }
}

private val LocalSliderHaptics = compositionLocalOf { true }

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Ui.Sub,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = modifier.padding(top = 6.dp)
    )
}

/** Dependent controls fade/slide in and out instead of just sitting there disabled. */
@Composable
private fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
    ) { content() }
}

/** Title + live value + (when changed) a Reset chip, over one slider. */
@Composable
private fun SettingSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    default: Float,
    onChange: (Float) -> Unit
) {
    val changed = kotlin.math.abs(value - default) > (range.endInclusive - range.start) * 0.005f
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Ui.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (changed) {
                Text(
                    "Reset",
                    color = Ui.Primary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onChange(default.coerceIn(range.start, range.endInclusive)) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(valueText, color = Ui.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        AestheticSlider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range
        )
    }
}

@Composable
private fun AestheticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    var previewValue by remember(valueRange) { mutableFloatStateOf(value) }
    var dragging by remember { mutableStateOf(false) }
    var lastStep by remember { mutableIntStateOf(-1) }
    // Live writes are capped to ~11 per second while dragging (the overlay redraws on every write).
    val lastSent = remember { LongArray(1) }
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalSliderHaptics.current
    LaunchedEffect(value) {
        if (!dragging) previewValue = value
    }
    Slider(
        value = previewValue,
        onValueChange = {
            dragging = true
            previewValue = it
            // A soft tick every ~5% of travel, like a detent.
            val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)
            val step = (((it - valueRange.start) / span) * 20f).toInt()
            if (step != lastStep) {
                if (lastStep != -1 && hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastStep = step
            }
            val now = System.nanoTime()
            if (now - lastSent[0] >= 90_000_000L) {
                lastSent[0] = now
                onValueChange(it)
            }
        },
        onValueChangeFinished = {
            dragging = false
            lastStep = -1
            onValueChange(previewValue)
        },
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = Ui.Primary,
            activeTrackColor = Ui.Primary,
            inactiveTrackColor = Ui.IconBg
        ),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    perRow: Int = options.size.coerceAtLeast(1)
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.chunked(perRow).forEach { rowOptions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowOptions.forEach { opt ->
                    val sel = opt == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp)
                            .background(
                                if (sel) Ui.Primary.copy(0.16f) else Ui.IconBg,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelect(opt) }
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            opt,
                            color = if (sel) Ui.Primary else Ui.Sub,
                            fontSize = 11.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(perRow - rowOptions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
