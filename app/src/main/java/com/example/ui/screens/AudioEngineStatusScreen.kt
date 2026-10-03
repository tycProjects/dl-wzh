package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioEngineStatusInfo
import com.example.ui.theme.JnmfAlertRed
import com.example.ui.theme.JnmfBorder
import com.example.ui.theme.JnmfDarkBg
import com.example.ui.theme.JnmfSurface
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfTextPrimary
import com.example.ui.theme.JnmfTextSecondary
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.JnmfWarningYellow

@Composable
fun AudioEngineStatusScreen(
    status: AudioEngineStatusInfo,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = JnmfTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JnmfDarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("audio_status_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.bright
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "AUDIO ENGINE STATUS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.bright
                )
                Text(
                    text = "Hardware Capability & Real-time DSP Diagnostics",
                    fontSize = 11.sp,
                    color = JnmfTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // PRIMARY REQUIRED STATUS CARD
        Text(
            text = "//JNMF_INTERNAL_AUDIO_PATH",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audio_engine_status_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderGlow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusItemRow(
                    label = "Playback:",
                    value = status.playback,
                    valueColor = colors.primary,
                    indicatorColor = colors.primary,
                    isLiveIndicator = true
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(10.dp))

                StatusItemRow(
                    label = "Audio Source:",
                    value = status.audioSource,
                    valueColor = colors.bright
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(10.dp))

                StatusItemRow(
                    label = "Decoder:",
                    value = status.decoder,
                    valueColor = colors.primary
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(10.dp))

                StatusItemRow(
                    label = "DSP:",
                    value = status.dsp,
                    valueColor = colors.primary
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(10.dp))

                StatusItemRow(
                    label = "Equalizer:",
                    value = status.equalizer,
                    valueColor = colors.bright
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(10.dp))

                StatusItemRow(
                    label = "Output:",
                    value = status.outputDevice,
                    valueColor = colors.bright
                )

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(JnmfBorder))
                Spacer(modifier = Modifier.height(10.dp))

                StatusItemRow(
                    label = "External Player:",
                    value = status.externalPlayer,
                    valueColor = JnmfTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // HARDWARE & OUTPUT CAPABILITIES
        Text(
            text = "//HARDWARE_AUDIO_CAPABILITIES",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusItemRow(label = "Hardware Sample Rate:", value = status.sampleRate, valueColor = JnmfTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                StatusItemRow(label = "Channel Configuration:", value = status.channelConfig, valueColor = JnmfTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                StatusItemRow(label = "Android Version:", value = "API ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.RELEASE})", valueColor = JnmfTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // INDEPENDENT AUDIO EFFECTS INTEGRITY
        Text(
            text = "//INDEPENDENT_EFFECTS_REGISTRY",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = JnmfSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JnmfBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                EffectStateRow("Sub-Bass Enhancement", if (status.bassBoostActive) "HARDWARE ACTIVE" else "JNMF INTERNAL DSP", colors.primary)
                Spacer(modifier = Modifier.height(8.dp))
                EffectStateRow("Stereo Width & 3D Spatializer", if (status.virtualizerActive) "HARDWARE ACTIVE" else "JNMF INTERNAL DSP", colors.primary)
                Spacer(modifier = Modifier.height(8.dp))
                EffectStateRow("Treble Clarity Harmonics", "JNMF DSP ACTIVE", colors.primary)
                Spacer(modifier = Modifier.height(8.dp))
                EffectStateRow("Peak Headroom Limiter", if (status.limiterActive) "ACTIVE (-0.5 dB)" else "BYPASS", if (status.limiterActive) colors.primary else JnmfWarningYellow)
                Spacer(modifier = Modifier.height(8.dp))
                EffectStateRow("TWS In-Ear Tuning Curve", if (status.twsModeActive) "ACTIVE" else "OFF", if (status.twsModeActive) colors.primary else JnmfTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Refresh Button
        Button(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("refresh_audio_status_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = JnmfDarkBg
            )
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RE-DETECT AUDIO HARDWARE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatusItemRow(
    label: String,
    value: String,
    valueColor: Color,
    indicatorColor: Color = Color.Unspecified,
    isLiveIndicator: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = JnmfTextSecondary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLiveIndicator) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (indicatorColor != Color.Unspecified) indicatorColor else valueColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun EffectStateRow(name: String, stateText: String, stateColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 12.sp, color = JnmfTextPrimary)
        Text(
            text = stateText,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = stateColor
        )
    }
}
