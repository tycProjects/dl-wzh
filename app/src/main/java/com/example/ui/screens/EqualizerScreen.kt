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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playback.EqualizerManager

@Composable
fun EqualizerScreen(
    equalizerManager: EqualizerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eqState by equalizerManager.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button_equalizer")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Equalizer & Audio DSP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Indo Remix Sound Engine",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Master Toggle
                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = { equalizerManager.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("master_eq_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Session Connection Indicator (Root-cause diagnostic)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (eqState.audioSessionId > 0) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (eqState.audioSessionId > 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Session Status",
                        tint = if (eqState.audioSessionId > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (eqState.audioSessionId > 0)
                                "Audio Pipeline Attached: Session #${eqState.audioSessionId}"
                            else
                                "Player Session Idle: Waiting for playback",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (eqState.isHardwareSupported)
                                "Processing directly on JNMF AudioTrack (Redmi 10C compatible)"
                            else
                                "Software DSP active (hardware limiter engaged)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Selection Horizontal Row
            Text(
                text = "SOUND PRESETS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eqState.availablePresets.take(3).forEach { preset ->
                    PresetChip(
                        name = preset,
                        isSelected = eqState.currentPreset == preset,
                        onClick = { equalizerManager.applyPreset(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eqState.availablePresets.drop(3).forEach { preset ->
                    PresetChip(
                        name = preset,
                        isSelected = eqState.currentPreset == preset,
                        onClick = { equalizerManager.applyPreset(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Frequency Sliders
            Text(
                text = "FREQUENCY BANDS (dB)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    eqState.bands.forEach { band ->
                        val min = band.minLevelMilliBel.toFloat()
                        val max = band.maxLevelMilliBel.toFloat()
                        val current = band.currentLevelMilliBel.toFloat()
                        val db = band.currentLevelMilliBel / 100

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = band.displayFrequency,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(62.dp)
                            )

                            Slider(
                                value = current,
                                onValueChange = { newLevel ->
                                    equalizerManager.setBandLevel(band.index, newLevel.toInt().toShort())
                                },
                                valueRange = min..max,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                enabled = eqState.isEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_band_${band.index}")
                            )

                            Text(
                                text = "${if (db > 0) "+$db" else "$db"} dB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (db > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(50.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Acoustic Enhancements (Bass Horeg, Virtualizer 3D, Clarity)
            Text(
                text = "REMIX ACOUSTIC ENHANCEMENT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Bass Horeg Slider
            EffectControlCard(
                title = "Bass Horeg / Sub-Bass Boost",
                subtitle = "Optimized for Indo Remix low-end punch",
                value = eqState.bassStrength,
                maxValue = 1000,
                isEnabled = eqState.isEnabled,
                onValueChange = { equalizerManager.setBassStrength(it) },
                testTag = "bass_slider"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stereo Width / Virtualizer 3D
            EffectControlCard(
                title = "Stereo Width / Spatial 3D",
                subtitle = "Broadens soundstage for headphones & TWS earbuds",
                value = eqState.virtualizerStrength,
                maxValue = 1000,
                isEnabled = eqState.isEnabled,
                onValueChange = { equalizerManager.setVirtualizerStrength(it) },
                testTag = "virtualizer_slider"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Clarity Enhancer
            EffectControlCard(
                title = "Vocal & Treble Clarity",
                subtitle = "Enhances vocals and synth leads without clipping",
                value = eqState.clarityStrength,
                maxValue = 1000,
                isEnabled = eqState.isEnabled,
                onValueChange = { equalizerManager.setClarityStrength(it) },
                testTag = "clarity_slider"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PresetChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun EffectControlCard(
    title: String,
    subtitle: String,
    value: Int,
    maxValue: Int,
    isEnabled: Boolean,
    onValueChange: (Int) -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(value / 10)}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..maxValue.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            )
        }
    }
}
