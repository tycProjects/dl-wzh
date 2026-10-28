package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Sound
import com.example.ui.components.SoundWaveAnimation
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class ChannelState(
    val id: Int,
    val name: String,
    val sound: Sound,
    val volume: Float = 0.8f,
    val isMuted: Boolean = false,
    val isPlaying: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerScreen(
    availableSounds: List<Sound>,
    masterVolume: Float,
    channelPlayingMap: Map<Int, Boolean>,
    onSetMasterVolume: (Float) -> Unit,
    onPlayChannel: (channelId: Int, audioUrl: String, volume: Float) -> Unit,
    onStopChannel: (channelId: Int) -> Unit,
    onSetChannelVolume: (channelId: Int, volume: Float) -> Unit,
    onToggleChannelMute: (channelId: Int) -> Unit,
    isChannelMuted: (channelId: Int) -> Boolean,
    onStopAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 4 Gaming Mixer Channels
    val channelSounds = remember(availableSounds) {
        val list = if (availableSounds.isNotEmpty()) availableSounds else listOf(
            Sound("ch_s1", "Bass Drum", audio_url = "synth://drum"),
            Sound("ch_s2", "Lead Synth", audio_url = "synth://lead"),
            Sound("ch_s3", "Crowd Cheering", audio_url = "synth://cheer"),
            Sound("ch_s4", "Laser FX", audio_url = "synth://laser")
        )
        mutableStateMapOf(
            1 to list.getOrElse(0) { list.first() },
            2 to list.getOrElse(1) { list.first() },
            3 to list.getOrElse(2) { list.first() },
            4 to list.getOrElse(3) { list.first() }
        )
    }

    val channelVolumes = remember {
        mutableStateMapOf(1 to 0.85f, 2 to 0.75f, 3 to 0.90f, 4 to 0.80f)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .testTag("mixer_screen_content"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AUDIO MIXER",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Multi-track simultaneous playback • No Ads",
                            color = ElectricCyan,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onStopAll,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("mixer_stop_all_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop All", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Master Volume Deck Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDark),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(NeonPurple, ElectricCyan))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Master Deck",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MASTER VOLUME",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "${(masterVolume * 100).toInt()}%",
                                color = ElectricCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = masterVolume,
                            onValueChange = onSetMasterVolume,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = CyberSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("master_volume_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "INDEPENDENT CHANNELS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Mixer Channel Items
        items(listOf(1, 2, 3, 4)) { channelId ->
            val sound = channelSounds[channelId] ?: Sound("fallback", "Audio Track $channelId")
            val isPlaying = channelPlayingMap[channelId] == true
            val volume = channelVolumes[channelId] ?: 0.8f
            val isMuted = isChannelMuted(channelId)
            var expandedDropdown by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isPlaying) Brush.horizontalGradient(listOf(ElectricCyan, NeonPurple))
                    else Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("mixer_channel_$channelId")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (channelId) {
                                            1 -> NeonPurple
                                            2 -> ElectricBlue
                                            3 -> ElectricCyan
                                            else -> NeonPink
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CH$channelId",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = sound.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (isPlaying) {
                            SoundWaveAnimation()
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sound Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = sound.title,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Sound FX", color = TextSecondary, fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.background(CyberDark)
                        ) {
                            availableSounds.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.title, color = TextPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        channelSounds[channelId] = option
                                        expandedDropdown = false
                                        if (isPlaying) {
                                            onPlayChannel(channelId, option.audio_url, volume)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Channel Controls: Play/Stop, Mute, Volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Stop button
                        IconButton(
                            onClick = {
                                if (isPlaying) onStopChannel(channelId)
                                else onPlayChannel(channelId, sound.audio_url, volume)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) NeonPink else NeonPurple)
                                .testTag("channel_${channelId}_play_toggle")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Mute button
                        IconButton(
                            onClick = { onToggleChannelMute(channelId) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) ErrorRed.copy(alpha = 0.2f) else CyberSurfaceVariant)
                                .testTag("channel_${channelId}_mute_toggle")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) ErrorRed else ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Volume Slider
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isMuted) "MUTED" else "Vol: ${(volume * 100).toInt()}%",
                                    color = if (isMuted) ErrorRed else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Slider(
                                value = volume,
                                onValueChange = { newVol ->
                                    channelVolumes[channelId] = newVol
                                    onSetChannelVolume(channelId, newVol)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricBlue,
                                    inactiveTrackColor = CyberSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
