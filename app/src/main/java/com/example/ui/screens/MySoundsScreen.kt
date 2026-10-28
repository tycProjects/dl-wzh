package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppConfig
import com.example.model.UserSound
import com.example.ui.components.SoundWaveAnimation
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun MySoundsScreen(
    mySounds: List<UserSound>,
    config: AppConfig,
    currentlyPlayingId: String?,
    isPlaying: Boolean,
    onPlaySound: (String, String) -> Unit,
    onStopSound: () -> Unit,
    onAddSound: (title: String, audioUrl: String, duration: Double) -> Unit,
    onRenameSound: (soundId: String, newTitle: String) -> Unit,
    onDeleteSound: (soundId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var soundToRename by remember { mutableStateOf<UserSound?>(null) }
    var soundToDelete by remember { mutableStateOf<UserSound?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .testTag("my_sounds_screen_content"),
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
                            text = "MY SOUNDS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private storage",
                                tint = ElectricCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Private user sound vault",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (config.user_uploads_enabled) {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_custom_sound_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Sound", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Admin Upload Permission and Size Limit Info Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDark),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (config.user_uploads_enabled) Icons.Default.Upload else Icons.Default.Lock,
                            contentDescription = "Status",
                            tint = if (config.user_uploads_enabled) ElectricCyan else WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (config.user_uploads_enabled)
                                    "Uploads Enabled (Max ${config.upload_size_limit_mb} MB per file)"
                                else
                                    "Sound uploads are currently paused by Admin",
                                color = if (config.user_uploads_enabled) TextPrimary else WarningAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Your sounds are stored privately under your UID. Zero ads.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Empty state
        if (mySounds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "No Sounds",
                                tint = NeonPurple,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Custom Sounds Yet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Upload or create your own custom gaming sounds, callouts, or victory clips here.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (config.user_uploads_enabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add Your First Sound", color = CyberBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            items(mySounds, key = { it.id }) { sound ->
                val isCurrentlyPlaying = (currentlyPlayingId == sound.id && isPlaying)

                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentlyPlaying) Brush.horizontalGradient(listOf(NeonPurple, ElectricCyan))
                        else Brush.horizontalGradient(listOf(CyberCardBorder, CyberCardBorder))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("my_sound_item_${sound.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrentlyPlaying) Brush.linearGradient(listOf(NeonPink, NeonPurple))
                                    else Brush.linearGradient(listOf(CyberSurfaceVariant, CyberSurfaceVariant))
                                )
                                .clickable {
                                    if (isCurrentlyPlaying) onStopSound()
                                    else onPlaySound(sound.id, sound.audio_url)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isCurrentlyPlaying) "Stop" else "Play",
                                tint = if (isCurrentlyPlaying) Color.White else ElectricCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sound.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Private • ${sound.duration_seconds}s",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                if (isCurrentlyPlaying) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SoundWaveAnimation()
                                }
                            }
                        }

                        // Rename button
                        IconButton(
                            onClick = { soundToRename = sound },
                            modifier = Modifier.testTag("rename_button_${sound.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Delete button
                        IconButton(
                            onClick = { soundToDelete = sound },
                            modifier = Modifier.testTag("delete_button_${sound.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ErrorRed.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Sound Dialog
    if (showAddDialog) {
        var soundTitle by remember { mutableStateOf("") }
        var soundSourceType by remember { mutableStateOf("preset") } // preset or url
        var soundUrl by remember { mutableStateOf("") }
        val presetSamples = listOf(
            "Laser Blast" to "synth://laser_blast",
            "Epic Horn" to "synth://mlg_horn",
            "Gamer Rage" to "synth://rage_bell",
            "Victory Fanfare" to "synth://fanfare_win"
        )
        var selectedPresetIndex by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Sound", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "File size limit: ${config.upload_size_limit_mb} MB",
                        color = ElectricCyan,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = soundTitle,
                        onValueChange = { soundTitle = it },
                        label = { Text("Sound Title") },
                        placeholder = { Text("e.g. My Clutch Play") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_sound_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Sound Source:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { soundSourceType = "preset" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (soundSourceType == "preset") NeonPurple else CyberSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Preset FX", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { soundSourceType = "url" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (soundSourceType == "url") ElectricCyan else CyberSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Audio URL", fontSize = 11.sp, color = if (soundSourceType == "url") CyberBlack else Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (soundSourceType == "preset") {
                        Column {
                            presetSamples.forEachIndexed { index, (label, _) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPresetIndex = index }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, if (selectedPresetIndex == index) ElectricCyan else TextMuted, CircleShape)
                                            .background(if (selectedPresetIndex == index) ElectricCyan else Color.Transparent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = soundUrl,
                            onValueChange = { soundUrl = it },
                            label = { Text("MP3/WAV Audio URL") },
                            placeholder = { Text("https://example.com/audio.mp3") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = CyberCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = soundTitle.ifBlank { "Custom Sound" }
                        val finalUrl = if (soundSourceType == "preset") presetSamples[selectedPresetIndex].second else soundUrl.ifBlank { "synth://custom" }
                        onAddSound(finalTitle, finalUrl, 2.5)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.testTag("confirm_add_sound_button")
                ) {
                    Text("Save to Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberDark
        )
    }

    // Rename Dialog
    soundToRename?.let { sound ->
        var newTitle by remember { mutableStateOf(sound.title) }
        AlertDialog(
            onDismissRequest = { soundToRename = null },
            title = { Text("Rename Sound", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_sound_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRenameSound(sound.id, newTitle)
                        }
                        soundToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Update", color = CyberBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { soundToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberDark
        )
    }

    // Delete Confirmation Dialog
    soundToDelete?.let { sound ->
        AlertDialog(
            onDismissRequest = { soundToDelete = null },
            title = { Text("Delete Sound?", color = Color.White) },
            text = {
                Text("Are you sure you want to permanently delete \"${sound.title}\"? This cannot be undone.", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSound(sound.id)
                        soundToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { soundToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberDark
        )
    }
}
