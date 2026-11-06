package com.android.island.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.android.island.core.IslandBus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import com.android.island.util.IslandDiagnostics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    onApplyRecommended: () -> Unit = {}
) {
    val ready = hasAccessibility && hasNotifListener
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val maxBannerWidthDp = (screenWidthDp - 8).coerceAtLeast(180)
    val shizukuStatus by ShizukuState.status.collectAsState()
    val diagnosticLines by IslandDiagnostics.snapshot.collectAsState()
    var showDiagnostics by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val defaults = remember { IslandSettings() }
    // Cutout the running overlay detected, so "Otomatis" shows the real size instead of a guess.
    val detectedCutout by remember { IslandBus.state.map { it.cutout.type }.distinctUntilChanged() }
        .collectAsState(initial = IslandBus.state.value.cutout.type)

    CompositionLocalProvider(LocalSliderHaptics provides settings.hapticsEnabled) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Android Island",
            color = Ui.Text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Media · WhatsApp · Kaca",
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
                subtitle = "Muat ulang overlay jika capsule tidak muncul",
                onClick = onRestartIsland
            )
        }

        GroupCard {
            SettingsRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.BugReport,
                iconColor = Tile.Orange,
                title = "Diagnostik internal",
                subtitle = "${if (settings.diagnosticsEnabled) "Perekaman aktif" else "Perekaman nonaktif"} · ${diagnosticLines.size} catatan tersimpan · ketuk untuk melihat",
                onClick = { showDiagnostics = true }
            )
            SwitchRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.FiberManualRecord,
                iconColor = if (settings.diagnosticsEnabled) Tile.Red else Ui.Sub,
                title = "Perekaman log",
                subtitle = if (settings.diagnosticsEnabled) "Merekam diagnostik lokal di memori aplikasi" else "Log baru tidak direkam; log lama tetap tersimpan sampai dihapus",
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
                        IslandThemeMode.WHITE -> "Teks, progress, dan tombol kontrol menggunakan warna putih."
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
                        title = "Opasitas latar", valueText = "${(settings.backgroundOpacity * 100).toInt()}%",
                        value = settings.backgroundOpacity, range = 0.35f..1f, default = defaults.backgroundOpacity,
                        onChange = { v -> onUpdateSettings { it.copy(backgroundOpacity = v) } }
                    )
                }
                SettingSlider(
                    title = "Lensa tepi kaca", valueText = "${(settings.edgeLensStrength * 100).toInt()}%",
                    value = settings.edgeLensStrength, range = 0f..1f, default = defaults.edgeLensStrength,
                    onChange = { v -> onUpdateSettings { it.copy(edgeLensStrength = v) } }
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("Sistem · Performa & baterai")
        GroupCard {
            SwitchRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.BatterySaver,
                iconColor = Tile.Green,
                title = "Mode hemat RAM & baterai",
                subtitle = "Kurangi efek dekoratif (entrance bertahap, kilau kaca). Gaya animasi, elastisitas, dan shine tetap sesuai pilihanmu",
                checked = settings.lowPowerMode,
                onCheckedChange = { v -> onUpdateSettings { it.copy(lowPowerMode = v) } }
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
            val profiles = listOf(CutoutProfile.AUTO, CutoutProfile.IPHONE, CutoutProfile.WATERDROP, CutoutProfile.CUSTOM)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ukuran capsule", color = Ui.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                    }
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
                    title = "Durasi banner", valueText = "${settings.whatsAppBannerDuration} detik",
                    value = settings.whatsAppBannerDuration.toFloat(), range = 3f..60f, default = defaults.whatsAppBannerDuration.toFloat(),
                    onChange = { v -> onUpdateSettings { it.copy(whatsAppBannerDuration = v.toInt()) } }
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
            // Blur hanya bekerja bersama liquid expansion dan hanya di Android 12+.
            Reveal(settings.liquidExpand && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                SwitchRow(
                    pos = RowPos.Last,
                    icon = Icons.Rounded.AutoAwesome,
                    iconColor = Tile.Purple,
                    title = "Blur saat berganti",
                    subtitle = "Mati secara bawaan agar tetap ringan",
                    checked = settings.fxBlur,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(fxBlur = v) } }
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
                    options = listOf("Opsi 1 · Stabil", "Opsi 2 · Flow", "Opsi 3 · Jelly"),
                    selected = when (settings.animationOption) {
                        AnimationOption.OPTION_1 -> "Opsi 1 · Stabil"
                        AnimationOption.OPTION_2 -> "Opsi 2 · Flow"
                        AnimationOption.OPTION_3 -> "Opsi 3 · Jelly"
                    },
                    onSelect = { selected ->
                        onUpdateSettings {
                            it.copy(
                                animationOption = when {
                                    selected.startsWith("Opsi 3") -> AnimationOption.OPTION_3
                                    selected.startsWith("Opsi 2") -> AnimationOption.OPTION_2
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
                Text("Style morph", color = Ui.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                ChipRow(
                    options = ExpandStyle.entries.map { it.name },
                    selected = settings.expandStyle.name,
                    onSelect = { name ->
                        onUpdateSettings { it.copy(expandStyle = ExpandStyle.valueOf(name)) }
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
                        Text("Elasticity", color = Ui.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        ChipRow(
                            options = Elasticity.entries.map { it.name },
                            selected = settings.elasticity.name,
                            onSelect = { name ->
                                onUpdateSettings { it.copy(elasticity = Elasticity.valueOf(name)) }
                            }
                        )
                    }
                }
                Reveal(settings.expandStyle != ExpandStyle.INSTANT) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        SettingSlider(
                            title = "Kecepatan", valueText = "%.1fx".format(settings.animationSpeed),
                            value = settings.animationSpeed, range = 0.5f..2f, default = defaults.animationSpeed,
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
                        SectionLabel("Lirik · Glow huruf")
                        SwitchRow(
                            pos = RowPos.Single,
                            icon = Icons.Rounded.AutoAwesome,
                            iconColor = Tile.Purple,
                            title = "Glow lirik yang sedang dibaca",
                            subtitle = "Opsional. Cahaya lembut mengikuti kata yang sedang dinyanyikan",
                            checked = settings.lyricGlowEnabled,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(lyricGlowEnabled = v) } }
                        )
                        Reveal(settings.lyricGlowEnabled) {
                            Column(Modifier.padding(top = 8.dp)) {
                                SettingSlider(
                                    title = "Intensitas glow", valueText = "${(settings.lyricGlowIntensity * 100).toInt()}%",
                                    value = settings.lyricGlowIntensity, range = 0f..0.7f, default = defaults.lyricGlowIntensity,
                                    onChange = { v -> onUpdateSettings { it.copy(lyricGlowIntensity = v) } }
                                )
                            }
                        }
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

        Spacer(Modifier.height(18.dp))
        SectionLabel("Rekomendasi")
        GroupCard {
            SettingsRow(
                pos = RowPos.Single,
                icon = Icons.Rounded.AutoAwesome,
                iconColor = Tile.Purple,
                title = "Terapkan rekomendasi",
                subtitle = "Opsional. Ganti semua pengaturan dengan nilai yang kami sarankan. Izin sistem tidak berubah.",
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
            title = { Text("Terapkan rekomendasi?") },
            text = { Text("Semua pengaturan akan diganti dengan nilai rekomendasi. Izin Accessibility dan Notification Listener tidak terpengaruh.") },
            confirmButton = {
                TextButton(onClick = { showResetConfirm = false; onApplyRecommended() }) { Text("Terapkan") }
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
                        diagnosticLines.takeLast(500).joinToString("\n").ifBlank { "Belum ada catatan. Aktifkan Island dan lakukan interaksi untuk merekam." },
                        color = Ui.Sub,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { clipboard.setText(AnnotatedString(IslandDiagnostics.exportText())) }) { Text("Salin semua") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { IslandDiagnostics.clear() }) { Text("Hapus") }
                    TextButton(onClick = { showDiagnostics = false }) { Text("Tutup") }
                }
            }
        )
    }

}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Ui.Sub,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
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
            onValueChange(it)
        },
        onValueChangeFinished = {
            dragging = false
            lastStep = -1
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
