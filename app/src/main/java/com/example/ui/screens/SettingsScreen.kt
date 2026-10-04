package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.AppPreferences
import com.example.model.AppUiColor
import com.example.ui.components.JnmfHeaderTitle
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.JnmfAlertRed
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfSurfaceVariant
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.JnmfWarningYellow

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    totalSongCount: Int,
    kickedSongCount: Int,
    possibleDuplicateCount: Int,
    totalPlaylistCount: Int,
    onUpdatePreferences: (AppPreferences) -> Unit,
    onSelectUiColor: (AppUiColor) -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onScanLibrary: () -> Unit,
    onReloadDemoTracks: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAudioEngineStatus: () -> Unit,
    onOpenAudioTest: () -> Unit,
    onOpenKickedSongs: () -> Unit,
    onOpenPossibleDuplicates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val strings = LocalAppStrings.current
    val colors = JnmfTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        JnmfHeaderTitle(subtitle = strings.settingsTitle)

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 1: UI COLOR / ACCENT COLOR
        SettingsSectionHeader(title = strings.uiColorSectionTitle, accentColor = colors.primary)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ui_color_settings_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderGlow)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsIcon(Icons.Default.Palette, colors.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.uiColorSectionTitle,
                            fontWeight = FontWeight.Bold,
                            color = colors.bright,
                            fontSize = 14.sp
                        )
                        Text(
                            text = strings.uiColorSectionDesc,
                            fontSize = 11.sp,
                            color = JnmfTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Grid of 6 Available UI Colors
                val uiColorChoices = listOf(
                    Triple(AppUiColor.GREEN, strings.colorGreen, true),
                    Triple(AppUiColor.BLUE, strings.colorBlue, false),
                    Triple(AppUiColor.RED, strings.colorRed, false),
                    Triple(AppUiColor.WHITE, strings.colorWhite, false),
                    Triple(AppUiColor.CYAN, strings.colorCyan, false),
                    Triple(AppUiColor.VIOLET, strings.colorViolet, false)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in uiColorChoices.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val first = uiColorChoices[i]
                            val second = uiColorChoices.getOrNull(i + 1)

                            UiColorCard(
                                color = first.first,
                                label = first.second,
                                isDefault = first.third,
                                isSelected = preferences.uiColor == first.first,
                                onSelect = {
                                    onSelectUiColor(first.first)
                                    onUpdatePreferences(preferences.copy(uiColor = first.first))
                                },
                                modifier = Modifier.weight(1f)
                            )

                            if (second != null) {
                                UiColorCard(
                                    color = second.first,
                                    label = second.second,
                                    isDefault = second.third,
                                    isSelected = preferences.uiColor == second.first,
                                    onSelect = {
                                        onSelectUiColor(second.first)
                                        onUpdatePreferences(preferences.copy(uiColor = second.first))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 2: LANGUAGE SELECTION
        SettingsSectionHeader(title = strings.languageSectionTitle, accentColor = colors.primary)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("language_settings_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderGlow)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsIcon(Icons.Default.Language, colors.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.languageSectionTitle,
                            fontWeight = FontWeight.Bold,
                            color = colors.bright,
                            fontSize = 14.sp
                        )
                        Text(
                            text = strings.languageSectionDesc,
                            fontSize = 11.sp,
                            color = JnmfTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val languageChoices = listOf(
                    Triple(AppLanguage.ENGLISH, strings.langEnglish, "English (Global)"),
                    Triple(AppLanguage.CEBUANO, strings.langCebuano, "Bisaya / Sinugboanon"),
                    Triple(AppLanguage.INDONESIAN, strings.langIndonesian, "Bahasa Indonesia")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languageChoices.forEach { (lang, name, sub) ->
                        val isSelected = preferences.language == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.primary.copy(alpha = 0.14f) else JnmfSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.primary else JnmfBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSelectLanguage(lang)
                                    onUpdatePreferences(preferences.copy(language = lang))
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("lang_choice_${lang.name.lowercase()}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) colors.bright else JnmfTextPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = sub,
                                    fontSize = 11.sp,
                                    color = if (isSelected) colors.primary else JnmfTextSecondary
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = JnmfDarkBg,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: AUDIO ENGINE & DIAGNOSTICS
        SettingsSectionHeader(title = strings.audioEngineSectionTitle, accentColor = colors.primary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Audio Engine Status Row
                SettingsNavRow(
                    icon = Icons.Default.Tune,
                    iconTint = colors.bright,
                    title = strings.audioEngineStatusBtn,
                    subtitle = strings.audioEngineStatusDesc,
                    badge = strings.activeStatus,
                    badgeColor = colors.primary,
                    onClick = onOpenAudioEngineStatus,
                    testTag = "nav_to_audio_engine_status"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(8.dp))

                // Audio Tests Row
                SettingsNavRow(
                    icon = Icons.Default.SpatialAudio,
                    iconTint = colors.primary,
                    title = strings.audioTestsBtn,
                    subtitle = strings.audioTestsDesc,
                    badge = "DIAGNOSTICS",
                    badgeColor = colors.bright,
                    onClick = onOpenAudioTest,
                    testTag = "nav_to_audio_test"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(8.dp))

                // Equalizer Direct Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenEqualizer)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIcon(Icons.Default.Equalizer, colors.bright)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                strings.masterEqualizer,
                                fontWeight = FontWeight.Bold,
                                color = JnmfTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                strings.eqHardwareDspActive,
                                fontSize = 11.sp,
                                color = JnmfTextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 4: LIBRARY & SAFETY MANAGEMENT
        SettingsSectionHeader(title = strings.libraryManagementSectionTitle, accentColor = colors.primary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Kicked Songs Navigation Row
                SettingsNavRow(
                    icon = Icons.Default.Block,
                    iconTint = JnmfWarningYellow,
                    title = strings.kickedSongsReviewBtn,
                    subtitle = "$kickedSongCount ${strings.kickedTracksCount}",
                    badge = if (kickedSongCount > 0) "$kickedSongCount" else null,
                    badgeColor = JnmfWarningYellow,
                    onClick = onOpenKickedSongs,
                    testTag = "nav_to_kicked_songs"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(8.dp))

                // Possible Duplicates Navigation Row
                SettingsNavRow(
                    icon = Icons.Default.CopyAll,
                    iconTint = colors.bright,
                    title = strings.duplicateDetectorBtn,
                    subtitle = "$possibleDuplicateCount ${strings.duplicatesTracksCount}",
                    badge = if (possibleDuplicateCount > 0) "$possibleDuplicateCount" else null,
                    badgeColor = JnmfAlertRed,
                    onClick = onOpenPossibleDuplicates,
                    testTag = "nav_to_possible_duplicates"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(8.dp))

                // Scan Device Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onScanLibrary)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIcon(Icons.Default.Storage, colors.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                strings.rescanStorageBtn,
                                fontWeight = FontWeight.SemiBold,
                                color = JnmfTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                strings.noSongsFoundDesc,
                                fontSize = 11.sp,
                                color = JnmfTextSecondary
                            )
                        }
                    }

                    Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(8.dp))

                // Reload Demo Tracks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReloadDemoTracks)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIcon(Icons.Default.Tune, colors.bright)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Reload Indo Remix Demo Tracks",
                                fontWeight = FontWeight.SemiBold,
                                color = JnmfTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                "Regenerates Jedag Jedug, Breakbeat, and Funk demo files",
                                fontSize = 11.sp,
                                color = JnmfTextSecondary
                            )
                        }
                    }

                    Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 5: AUDIO DSP HARDWARE SLIDERS
        SettingsSectionHeader(title = "AUDIO DSP CONTROLS", accentColor = colors.primary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // TWS Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIcon(Icons.Default.Headphones, colors.bright)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.twsModeLabel,
                                fontWeight = FontWeight.Bold,
                                color = JnmfTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = strings.twsModeDesc,
                                fontSize = 11.sp,
                                color = JnmfTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = preferences.twsHeadsetMode,
                        onCheckedChange = { onUpdatePreferences(preferences.copy(twsHeadsetMode = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JnmfDarkBg,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = JnmfTextSecondary,
                            uncheckedTrackColor = JnmfSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(10.dp))

                // Limiter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIcon(Icons.Default.VolumeUp, colors.bright)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.limiterLabel,
                                fontWeight = FontWeight.Bold,
                                color = JnmfTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = strings.limiterDesc,
                                fontSize = 11.sp,
                                color = JnmfTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = preferences.limiterEnabled,
                        onCheckedChange = { onUpdatePreferences(preferences.copy(limiterEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JnmfDarkBg,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = JnmfTextSecondary,
                            uncheckedTrackColor = JnmfSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bass Boost Slider
                Text(
                    text = "${strings.bassBoostLabel}: ${preferences.bassBoostStrength / 10}%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright
                )
                Slider(
                    value = preferences.bassBoostStrength.toFloat(),
                    onValueChange = { onUpdatePreferences(preferences.copy(bassBoostStrength = it.toInt())) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.bright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = JnmfSurfaceVariant
                    )
                )

                // Stereo Width Slider
                Text(
                    text = "${strings.stereoWidthLabel}: ${preferences.stereoWidthStrength / 10}%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright
                )
                Slider(
                    value = preferences.stereoWidthStrength.toFloat(),
                    onValueChange = { onUpdatePreferences(preferences.copy(stereoWidthStrength = it.toInt())) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.bright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = JnmfSurfaceVariant
                    )
                )

                // 3D Depth Slider
                Text(
                    text = "${strings.depth3DLabel}: ${preferences.depth3DStrength / 10}%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright
                )
                Slider(
                    value = preferences.depth3DStrength.toFloat(),
                    onValueChange = { onUpdatePreferences(preferences.copy(depth3DStrength = it.toInt())) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.bright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = JnmfSurfaceVariant
                    )
                )

                // Clarity Slider
                Text(
                    text = "${strings.clarityLabel}: ${preferences.clarityStrength / 10}%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright
                )
                Slider(
                    value = preferences.clarityStrength.toFloat(),
                    onValueChange = { onUpdatePreferences(preferences.copy(clarityStrength = it.toInt())) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.bright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = JnmfSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 6: OFFLINE INTEGRITY
        SettingsSectionHeader(title = strings.offlineIntegrityTitle, accentColor = colors.primary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsIcon(Icons.Default.Security, colors.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.offlineSafeBadge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = strings.offlineIntegrityDesc,
                            fontSize = 11.sp,
                            color = JnmfTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.totalActiveTracks, fontSize = 12.sp, color = JnmfTextSecondary)
                    Text("$totalSongCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.bright)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.kickedTracksCount, fontSize = 12.sp, color = JnmfTextSecondary)
                    Text("$kickedSongCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JnmfWarningYellow)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.totalPlaylists, fontSize = 12.sp, color = JnmfTextSecondary)
                    Text("$totalPlaylistCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JnmfTextPrimary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.engineVersionLabel, fontSize = 12.sp, color = JnmfTextSecondary)
                    Text("v2.2 (Indo Remix Cyber)", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = colors.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 7: CREDITS (Informational, exact format specified by user)
        SettingsSectionHeader(title = strings.creditsSectionTitle, accentColor = colors.primary)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("credits_section_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Compiled by
                Text(
                    text = strings.creditsCompiledBy,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JnmfTextSecondary
                )
                Text(
                    text = "JUSSAR GOLDEN",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.bright
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(12.dp))

                // Created by
                Text(
                    text = strings.creditsCreatedBy,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JnmfTextSecondary
                )
                Text(
                    text = "ChatGPT — Advisor",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = JnmfTextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(12.dp))

                // App Developer
                Text(
                    text = strings.creditsAppDeveloper,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JnmfTextSecondary
                )
                Text(
                    text = "Google AI Studio",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun UiColorCard(
    color: AppUiColor,
    label: String,
    isDefault: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) color.primary.copy(alpha = 0.16f) else JnmfSurfaceElevated)
            .border(
                1.5.dp,
                if (isSelected) color.primary else JnmfBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("ui_color_choice_${color.id}"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color circle swatch
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color.primary)
                        .border(1.dp, color.secondary, CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = color.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) color.secondary else JnmfTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isDefault) {
                        Text(
                            text = "DEFAULT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = color.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = color.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String?,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIcon(icon, iconTint)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = JnmfTextPrimary,
                        fontSize = 14.sp
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .border(0.5.dp, badgeColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 7.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = JnmfTextSecondary,
                    lineHeight = 14.sp
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, accentColor: Color) {
    Text(
        text = "//$title",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        modifier = Modifier.padding(bottom = 6.dp, top = 2.dp)
    )
}

@Composable
private fun SettingsIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(JnmfSurfaceElevated)
            .border(1.dp, JnmfBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
