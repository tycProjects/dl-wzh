package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EqualizerBandState
import com.example.model.EqualizerCompatibilityReport
import com.example.model.EqualizerPresetType
import com.example.ui.components.JnmfHeaderTitle
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfBorderGlow
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfSurfaceVariant
import com.example.ui.theme.JnmfTextMuted
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import java.util.Locale

@Composable
fun EqualizerScreen(
    bands: List<EqualizerBandState>,
    isEqualizerEnabled: Boolean,
    currentPreset: EqualizerPresetType,
    bassBoostStrength: Int,
    isBassBoostEnabled: Boolean,
    stereoWidthStrength: Int,
    depth3DStrength: Int,
    clarityStrength: Int,
    limiterEnabled: Boolean,
    twsHeadsetMode: Boolean,
    bassGainDb: Float = 0f,
    midGainDb: Float = 0f,
    trebleGainDb: Float = 0f,
    compatibilityReport: EqualizerCompatibilityReport? = null,
    onRunStrictCompatibilityCheck: () -> Unit = {},
    onToggleEqualizer: (Boolean) -> Unit,
    onSetBandLevel: (Short, Short) -> Unit,
    onSetBassGain: (Float) -> Unit = {},
    onSetMidGain: (Float) -> Unit = {},
    onSetTrebleGain: (Float) -> Unit = {},
    onSelectPreset: (EqualizerPresetType) -> Unit,
    onSetBassBoostStrength: (Int) -> Unit,
    onToggleBassBoost: (Boolean) -> Unit,
    onSetStereoWidthStrength: (Int) -> Unit,
    onSetDepth3DStrength: (Int) -> Unit,
    onSetClarityStrength: (Int) -> Unit,
    onSetLimiterEnabled: (Boolean) -> Unit,
    onSetTwsHeadsetMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val colors = JnmfTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        JnmfHeaderTitle(subtitle = "DSP & EQUALIZER")

        Spacer(modifier = Modifier.height(10.dp))

        // Master DSP Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderGlow)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary.copy(alpha = 0.15f))
                            .border(1.dp, colors.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = colors.bright,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MASTER EQUALIZER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.bright,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isEqualizerEnabled) "Hardware AudioFX + JNMF DSP Active" else "Equalizer Bypassed",
                            fontSize = 11.sp,
                            color = if (isEqualizerEnabled) colors.primary else JnmfTextMuted
                        )
                    }
                }

                Switch(
                    checked = isEqualizerEnabled,
                    onCheckedChange = onToggleEqualizer,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JnmfDarkBg,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = JnmfTextSecondary,
                        uncheckedTrackColor = JnmfSurfaceVariant
                    ),
                    modifier = Modifier.testTag("eq_master_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Strict Compatibility Status Card (Ensures EQ strictly processes JNMF playback)
        if (compatibilityReport != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("eq_compatibility_card"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = JnmfSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderGlow)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STRICT AUDIT: ${compatibilityReport.status}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.bright
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primary.copy(alpha = 0.15f))
                                .border(0.5.dp, colors.primary, RoundedCornerShape(6.dp))
                                .clickable { onRunStrictCompatibilityCheck() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "RE-VERIFY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mode: ${compatibilityReport.mode} • Range: [${compatibilityReport.minLevelMb / 100}dB .. +${compatibilityReport.maxLevelMb / 100}dB]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JnmfTextSecondary
                    )
                    Text(
                        text = "Target: JNMF Internal Player Stream • Third-Party Players: NOT USED",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Chips Row
        Text(
            text = "//INDO REMIX PRESETS",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EqualizerPresetType.entries.forEach { preset ->
                val isSelected = currentPreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.primary.copy(alpha = 0.2f) else JnmfSurface)
                        .border(
                            1.dp,
                            if (isSelected) colors.primary else JnmfBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = isEqualizerEnabled) { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = preset.label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.bright else JnmfTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3-BAND SHELF EQUALIZER (Low Shelf / Peaking / High Shelf)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "//3-BAND SHELF EQUALIZER",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = isEqualizerEnabled) {
                        onSetBassGain(0f)
                        onSetMidGain(0f)
                        onSetTrebleGain(0f)
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset to 0 dB",
                    tint = JnmfTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RESET 0 dB",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = JnmfTextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("three_band_shelf_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderGlow)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // 1. Bass Filter (Low frequencies up to 250Hz, lowshelf)
                ThreeBandSliderRow(
                    title = "Bass",
                    filterType = "Low Shelf",
                    freqDesc = "Sub-bass & punch up to 250 Hz",
                    valueDb = bassGainDb,
                    enabled = isEqualizerEnabled,
                    testTag = "slider_bass_lowshelf",
                    onValueChange = onSetBassGain
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(8.dp))

                // 2. Mids Filter (Human speech/instruments centered around 1000Hz, peaking)
                ThreeBandSliderRow(
                    title = "Mids",
                    filterType = "Peaking",
                    freqDesc = "Vocals & leads centered around 1000 Hz (Q=1.0)",
                    valueDb = midGainDb,
                    enabled = isEqualizerEnabled,
                    testTag = "slider_mids_peaking",
                    onValueChange = onSetMidGain
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(8.dp))

                // 3. Treble Filter (High frequencies from 4000Hz and up, highshelf)
                ThreeBandSliderRow(
                    title = "Treble",
                    filterType = "High Shelf",
                    freqDesc = "Cymbals & air from 4000 Hz and up",
                    valueDb = trebleGainDb,
                    enabled = isEqualizerEnabled,
                    testTag = "slider_treble_highshelf",
                    onValueChange = onSetTrebleGain
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed Multi-Band Frequency Sliders
        Text(
            text = "//DETAILED FREQUENCY BANDS",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (bands.isEmpty()) {
                    Text("Loading frequency bands...", color = JnmfTextMuted, fontSize = 12.sp)
                } else {
                    bands.forEach { band ->
                        BandSliderRow(
                            band = band,
                            enabled = isEqualizerEnabled,
                            onLevelChange = { newLevel ->
                                onSetBandLevel(band.bandIndex, newLevel)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Specialized Indo Remix Features Card
        Text(
            text = "//INDO REMIX ENHANCERS",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // TWS Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Headphones, contentDescription = null, tint = colors.bright, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("TWS / Headset Acoustic Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JnmfTextPrimary)
                            Text("Tuned for Bluetooth earbuds bass response", fontSize = 10.sp, color = JnmfTextSecondary)
                        }
                    }
                    Switch(
                        checked = twsHeadsetMode,
                        onCheckedChange = onSetTwsHeadsetMode,
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

                // Limiter Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Peak Limiter Protection", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = JnmfTextPrimary)
                            Text("Prevents distortion on heavy Jedag Jedug drops", fontSize = 10.sp, color = JnmfTextSecondary)
                        }
                    }
                    Switch(
                        checked = limiterEnabled,
                        onCheckedChange = onSetLimiterEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JnmfDarkBg,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = JnmfTextSecondary,
                            uncheckedTrackColor = JnmfSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bass Boost Strength
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sub-Bass Resonance Boost: ${bassBoostStrength / 10}%",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.bright
                    )
                    Switch(
                        checked = isBassBoostEnabled,
                        onCheckedChange = onToggleBassBoost,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JnmfDarkBg,
                            checkedTrackColor = colors.primary,
                            uncheckedThumbColor = JnmfTextSecondary,
                            uncheckedTrackColor = JnmfSurfaceVariant
                        )
                    )
                }

                Slider(
                    value = bassBoostStrength.toFloat(),
                    onValueChange = { onSetBassBoostStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = isBassBoostEnabled && isEqualizerEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.bright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = JnmfSurfaceVariant
                    )
                )

                // Stereo Width Strength
                Text(
                    text = "Stereo Width Expansion: ${stereoWidthStrength / 10}%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright
                )
                Slider(
                    value = stereoWidthStrength.toFloat(),
                    onValueChange = { onSetStereoWidthStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = isEqualizerEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.bright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = JnmfSurfaceVariant
                    )
                )

                // 3D Depth
                Text(
                    text = "3D Acoustic Depth: ${depth3DStrength / 10}%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright
                )
                Slider(
                    value = depth3DStrength.toFloat(),
                    onValueChange = { onSetDepth3DStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = isEqualizerEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.bright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = JnmfSurfaceVariant
                    )
                )

                // Clarity
                Text(
                    text = "High Treble Clarity Exciter: ${clarityStrength / 10}%",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.bright
                )
                Slider(
                    value = clarityStrength.toFloat(),
                    onValueChange = { onSetClarityStrength(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = isEqualizerEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.bright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = JnmfSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ThreeBandSliderRow(
    title: String,
    filterType: String,
    freqDesc: String,
    valueDb: Float,
    enabled: Boolean,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    val colors = JnmfTheme.colors
    val formattedValue = String.format(Locale.US, "%+.1f dB", valueDb)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$title: ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = JnmfTextPrimary
                    )
                    Text(
                        text = "($filterType)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.bright
                    )
                }
                Text(
                    text = freqDesc,
                    fontSize = 10.sp,
                    color = JnmfTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(JnmfSurfaceElevated)
                    .border(1.dp, colors.borderGlow, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formattedValue,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (valueDb > 0f) colors.bright else if (valueDb < 0f) JnmfTextSecondary else JnmfTextPrimary
                )
            }
        }

        Slider(
            value = valueDb,
            onValueChange = onValueChange,
            valueRange = -20f..20f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colors.bright,
                activeTrackColor = colors.primary,
                inactiveTrackColor = JnmfSurfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun BandSliderRow(
    band: EqualizerBandState,
    enabled: Boolean,
    onLevelChange: (Short) -> Unit
) {
    val colors = JnmfTheme.colors
    val gainDb = band.currentGainDb
    val formattedGain = String.format(Locale.US, "%+.1f dB", gainDb)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = band.formattedCenterFreq,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = JnmfTextPrimary
            )

            Text(
                text = formattedGain,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (gainDb > 0) colors.bright else JnmfTextSecondary
            )
        }

        Slider(
            value = band.currentLevelMilliBels.toFloat(),
            onValueChange = { onLevelChange(it.toInt().toShort()) },
            valueRange = band.minLevelMilliBels.toFloat()..band.maxLevelMilliBels.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colors.bright,
                activeTrackColor = colors.primary,
                inactiveTrackColor = JnmfSurfaceVariant
            ),
            modifier = Modifier.testTag("slider_band_${band.bandIndex}")
        )
    }
}
