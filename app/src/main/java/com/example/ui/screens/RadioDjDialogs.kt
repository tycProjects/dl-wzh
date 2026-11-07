package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RadioItemType
import com.example.data.model.RadioTimelineCalculator
import com.example.data.model.Song
import com.example.data.db.RadioScheduleEntity
import com.example.data.repository.AppLanguage
import com.example.data.repository.MediaSearchMatcher
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.RadioDjStrings

/**
 * Distinct high-contrast broadcast accent colors per [RadioItemType] so DJs can immediately
 * tell music tracks apart from radio ads, news segments, prerecorded radio programs, announcements, and jingles.
 */
@Composable
fun radioItemTypeColor(type: RadioItemType): Color = when (type) {
    RadioItemType.MUSIC -> MaterialTheme.colorScheme.primary
    RadioItemType.ADVERTISEMENT -> Color(0xFFFFAB00) // Broadcast Amber
    RadioItemType.NEWS -> Color(0xFF2979FF)          // Electric Blue for News
    RadioItemType.RADIO_PROGRAM -> Color(0xFF7C4DFF) // Deep Violet for Radio Programs
    RadioItemType.ANNOUNCEMENT -> Color(0xFF00E5FF)  // Electric Cyan
    RadioItemType.JINGLE -> Color(0xFFE040FB)        // Station ID Magenta
    RadioItemType.INSERT -> Color(0xFF26A69A)        // Studio Teal
}

fun radioItemTypeIcon(type: RadioItemType): ImageVector = when (type) {
    RadioItemType.MUSIC -> Icons.Default.Audiotrack
    RadioItemType.ADVERTISEMENT -> Icons.Default.Campaign
    RadioItemType.NEWS -> Icons.Default.Newspaper
    RadioItemType.RADIO_PROGRAM -> Icons.Default.Podcasts
    RadioItemType.ANNOUNCEMENT -> Icons.Default.RecordVoiceOver
    RadioItemType.JINGLE -> Icons.Default.AutoAwesome
    RadioItemType.INSERT -> Icons.Default.GraphicEq
}

@Composable
fun RadioItemTypeBadge(
    type: RadioItemType,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val accent = radioItemTypeColor(type)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = radioItemTypeIcon(type),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = RadioDjStrings.itemTypeBadgeShort(type, lang),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun RadioStudioModalDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit
) {
    BackHandler(onBack = onDismissRequest)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                title()
                text()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun CreateOrRenameRadioScheduleDialog(
    isRename: Boolean,
    initialName: String,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameInput by remember(initialName) { mutableStateOf(initialName) }
    val presets = remember {
        listOf(
            "JNMF Morning Radio",
            "JNMF Afternoon Mix",
            "JNMF Night Program"
        )
    }

    RadioStudioModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRename) {
                    RadioDjStrings.renameScheduleTitle(lang)
                } else {
                    RadioDjStrings.createScheduleTitle(lang)
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(RadioDjStrings.scheduleNameLabel(lang)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radio_schedule_name_input")
                )

                if (!isRename) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable { nameInput = preset }
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nameInput.trim()) },
                enabled = nameInput.trim().isNotEmpty(),
                modifier = Modifier.testTag("radio_schedule_save_confirm")
            ) {
                Text(JnmfStrings.save(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(JnmfStrings.cancel(lang))
            }
        }
    )
}

@Composable
fun ProgramStartTimeDialog(
    currentStartTimeSeconds: Int,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmSeconds: (Int) -> Unit
) {
    val normalized = RadioTimelineCalculator.normalizeStartTimeSeconds(currentStartTimeSeconds)
    val initialHour24 = normalized / 3600
    val initialMinute = (normalized % 3600) / 60
    val initialIsPm = initialHour24 >= 12
    val initialHour12 = when {
        initialHour24 == 0 -> 12
        initialHour24 > 12 -> initialHour24 - 12
        else -> initialHour24
    }

    var hour12 by remember(currentStartTimeSeconds) { mutableIntStateOf(initialHour12) }
    var minute by remember(currentStartTimeSeconds) { mutableIntStateOf(initialMinute) }
    var isPm by remember(currentStartTimeSeconds) { mutableStateOf(initialIsPm) }

    fun toSecondsOfDay(h12: Int, min: Int, pm: Boolean): Int {
        val h24 = when {
            pm && h12 < 12 -> h12 + 12
            !pm && h12 == 12 -> 0
            else -> h12
        }
        return RadioTimelineCalculator.hmsToSecondsOfDay(h24, min, 0)
    }

    val previewText = RadioTimelineCalculator.formatClockTime12hWithSeconds(
        toSecondsOfDay(hour12, minute, isPm).toLong()
    )

    val quickPresets = remember {
        listOf(
            "6:00 AM" to (6 * 3600),
            "12:00 PM" to (12 * 3600),
            "5:00 PM" to (17 * 3600),
            "8:00 PM" to (20 * 3600),
            "10:00 PM" to (22 * 3600)
        )
    }

    RadioStudioModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = RadioDjStrings.changeStartTime(lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = previewText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("radio_time_dialog_preview")
                    )
                }

                // Hour / Minute / AM-PM steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeNumberStepper(
                        label = RadioDjStrings.hourLabel(lang),
                        valueText = hour12.toString(),
                        onDecrement = { hour12 = if (hour12 <= 1) 12 else hour12 - 1 },
                        onIncrement = { hour12 = if (hour12 >= 12) 1 else hour12 + 1 },
                        decTag = "radio_hour_dec",
                        incTag = "radio_hour_inc"
                    )

                    TimeNumberStepper(
                        label = RadioDjStrings.minuteLabel(lang),
                        valueText = "%02d".format(minute),
                        onDecrement = { minute = if (minute <= 0) 55 else (minute - 5).coerceAtLeast(0) },
                        onIncrement = { minute = (minute + 5) % 60 },
                        decTag = "radio_min_dec",
                        incTag = "radio_min_inc"
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AM / PM", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { isPm = !isPm },
                            modifier = Modifier.testTag("radio_ampm_toggle")
                        ) {
                            Text(
                                text = if (isPm) "PM" else "AM",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.forEach { (label, secs) ->
                        FilterChip(
                            selected = toSecondsOfDay(hour12, minute, isPm) == secs,
                            onClick = {
                                val h24 = secs / 3600
                                val m = (secs % 3600) / 60
                                isPm = h24 >= 12
                                hour12 = when {
                                    h24 == 0 -> 12
                                    h24 > 12 -> h24 - 12
                                    else -> h24
                                }
                                minute = m
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("radio_preset_time_${label.replace(' ', '_').replace(':', '_')}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSeconds(toSecondsOfDay(hour12, minute, isPm)) },
                modifier = Modifier.testTag("radio_start_time_confirm")
            ) {
                Text(JnmfStrings.save(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(JnmfStrings.cancel(lang))
            }
        }
    )
}

@Composable
private fun TimeNumberStepper(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decTag: String,
    incTag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(40.dp)
                    .testTag(decTag),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(
                text = valueText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            OutlinedButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(40.dp)
                    .testTag(incTag),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AddOrReplaceRadioItemDialog(
    isReplaceMode: Boolean,
    initialType: RadioItemType,
    allSongs: List<Song>,
    existingSongIds: Set<Long> = emptySet(),
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onPickExternalFile: (RadioItemType) -> Unit,
    onSelectLibrarySong: (Song, RadioItemType) -> Unit,
    onPreviewSong: ((Song) -> Unit)? = null
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = remember(allSongs, searchQuery) {
        val norm = MediaSearchMatcher.normalizeForSearch(searchQuery)
        if (norm.isBlank()) {
            allSongs
        } else {
            allSongs.filter { it.normalizedSearchIndex.contains(norm) }
        }
    }

    RadioStudioModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isReplaceMode) {
                    RadioDjStrings.replaceProgramItemTitle(lang)
                } else {
                    RadioDjStrings.addProgramItemTitle(lang)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = RadioDjStrings.chooseItemTypeLabel(lang),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RadioItemType.entries.forEach { type ->
                        val accent = radioItemTypeColor(type)
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            leadingIcon = {
                                Icon(
                                    imageVector = radioItemTypeIcon(type),
                                    contentDescription = null,
                                    tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = RadioDjStrings.itemTypeLabel(type, lang),
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.2f),
                                selectedLabelColor = accent
                            ),
                            modifier = Modifier.testTag("radio_picker_type_${type.storageKey}")
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onPickExternalFile(selectedType) },
                    border = BorderStroke(1.dp, radioItemTypeColor(selectedType).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radio_pick_external_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = radioItemTypeColor(selectedType),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = RadioDjStrings.pickExternalAudioButton(lang),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = RadioDjStrings.pickFromLibraryHeader(lang),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(JnmfStrings.searchPlaceholder(lang), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radio_library_picker_search")
                )

                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = JnmfStrings.noSongsFound(lang),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .testTag("radio_library_picker_list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            val alreadyAdded = existingSongIds.contains(song.id)
                            var showItemMenu by remember { mutableStateOf(false) }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectLibrarySong(song, selectedType) }
                                    .testTag("radio_picker_song_${song.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (alreadyAdded) {
                                                Text(
                                                    text = RadioDjStrings.alreadyInSchedule(lang),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                                Text("• ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(
                                                text = "${song.artist} • ${song.formattedDuration}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Quick Add (+) Button
                                    IconButton(
                                        onClick = { onSelectLibrarySong(song, selectedType) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("radio_picker_quick_add_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = RadioDjStrings.quickAdd(lang),
                                            tint = radioItemTypeColor(selectedType),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // 3-dot category menu
                                    Box {
                                        IconButton(
                                            onClick = { showItemMenu = true },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("radio_picker_menu_${song.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showItemMenu,
                                            onDismissRequest = { showItemMenu = false }
                                        ) {
                                            RadioItemType.entries.forEach { type ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text("Add as ${RadioDjStrings.itemTypeLabel(type, lang)}", fontSize = 12.sp)
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = radioItemTypeIcon(type),
                                                            contentDescription = null,
                                                            tint = radioItemTypeColor(type),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        showItemMenu = false
                                                        onSelectLibrarySong(song, type)
                                                    }
                                                )
                                            }
                                            if (onPreviewSong != null) {
                                                DropdownMenuItem(
                                                    text = { Text("Preview Song", fontSize = 12.sp) },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    },
                                                    onClick = {
                                                        showItemMenu = false
                                                        onPreviewSong(song)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(JnmfStrings.close(lang))
            }
        }
    )
}

@Composable
fun TimeAnnouncementSettingsDialog(
    initialEnabled: Boolean,
    initialHourly: Boolean,
    initialAudioUri: String,
    initialTitle: String,
    initialDurationMs: Long,
    initialCustomMinutes: String,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onPickAudioFile: () -> Unit,
    onPreviewAudio: (Uri, String) -> Unit,
    onSave: (enabled: Boolean, hourly: Boolean, audioUri: String, title: String, durMs: Long, customMinutes: String) -> Unit
) {
    var enabled by remember { mutableStateOf(initialEnabled) }
    var hourly by remember { mutableStateOf(initialHourly) }
    var customMinutes by remember { mutableStateOf(initialCustomMinutes) }
    var audioUri by remember(initialAudioUri) { mutableStateOf(initialAudioUri) }
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var durMs by remember(initialDurationMs) { mutableStateOf(initialDurationMs) }

    RadioStudioModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = RadioDjStrings.timeAnnouncementSettings(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = RadioDjStrings.timeAnnouncement(lang),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Interrupts music at scheduled minute, broadcasts announcement, then plays next song",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        modifier = Modifier.testTag("radio_announcement_toggle")
                    )
                }

                // Hourly Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = RadioDjStrings.hourlyAnnouncement(lang),
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = hourly,
                        onCheckedChange = { hourly = it },
                        enabled = enabled,
                        modifier = Modifier.testTag("radio_announcement_hourly_toggle")
                    )
                }

                // Custom minutes text field
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it },
                    label = { Text(RadioDjStrings.customMinutes(lang), fontSize = 12.sp) },
                    placeholder = { Text("0, 15, 30, 45", fontSize = 12.sp) },
                    enabled = enabled,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radio_announcement_minutes_input")
                )

                // Audio file selection
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (title.isNotBlank()) title else "No audio file selected",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("radio_announcement_file_title")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPickAudioFile,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("radio_announcement_pick_file")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose File", fontSize = 12.sp)
                            }
                            if (audioUri.isNotBlank()) {
                                Button(
                                    onClick = { onPreviewAudio(Uri.parse(audioUri), title) },
                                    modifier = Modifier.testTag("radio_announcement_preview_btn")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(RadioDjStrings.testAnnouncement(lang), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(enabled, hourly, audioUri, title, durMs, customMinutes)
                },
                modifier = Modifier.testTag("radio_announcement_save_btn")
            ) {
                Text(JnmfStrings.save(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(JnmfStrings.cancel(lang))
            }
        }
    )
}

@Composable
fun ChangeRadioItemTypeDialog(
    currentType: RadioItemType,
    itemTitle: String,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onSelectType: (RadioItemType) -> Unit
) {
    RadioStudioModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = RadioDjStrings.changeType(lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = itemTitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                RadioItemType.entries.forEach { type ->
                    val accent = radioItemTypeColor(type)
                    val isSelected = type == currentType
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) accent else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectType(type) }
                            .testTag("radio_change_type_option_${type.storageKey}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = radioItemTypeIcon(type),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = RadioDjStrings.itemTypeLabel(type, lang),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(JnmfStrings.cancel(lang))
            }
        }
    )
}

@Composable
fun DeleteRadioScheduleDialog(
    scheduleName: String,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    RadioStudioModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = RadioDjStrings.deleteScheduleTitle(lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = RadioDjStrings.deleteScheduleConfirm(scheduleName, lang),
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("radio_delete_schedule_confirm")
            ) {
                Text(JnmfStrings.delete(lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(JnmfStrings.cancel(lang))
            }
        }
    )
}

@Composable
fun RadioPlaylistPickerDialog(
    songsToAdd: List<Song>,
    availableSchedules: List<RadioScheduleEntity>,
    onSelectSchedule: (RadioScheduleEntity) -> Unit,
    onCreateNewScheduleAndAdd: (String) -> Unit,
    onDismiss: () -> Unit,
    lang: AppLanguage = LocalAppLanguage.current
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    RadioStudioModalDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = RadioDjStrings.selectRadioPlaylist(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val summaryText = if (songsToAdd.size == 1) {
                    songsToAdd.first().title
                } else {
                    RadioDjStrings.selectedSongsCount(songsToAdd.size, lang)
                }
                Text(
                    text = summaryText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isCreatingNew) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text(RadioDjStrings.newRadioPlaylistName(lang)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("radio_picker_new_playlist_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isCreatingNew = false }) {
                                Text(JnmfStrings.cancel(lang))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPlaylistName.isNotBlank()) {
                                        onCreateNewScheduleAndAdd(newPlaylistName.trim())
                                    }
                                },
                                enabled = newPlaylistName.isNotBlank(),
                                modifier = Modifier.testTag("radio_picker_create_and_add_button")
                            ) {
                                Text(JnmfStrings.create(lang))
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("radio_picker_create_new_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(RadioDjStrings.createNewRadioPlaylist(lang))
                    }
                }

                if (availableSchedules.isEmpty() && !isCreatingNew) {
                    Text(
                        text = "No saved Radio Playlists yet. Tap above to create one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else if (availableSchedules.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableSchedules, key = { it.id }) { schedule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { onSelectSchedule(schedule) }
                                    .testTag("radio_picker_playlist_${schedule.id}")
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Podcasts,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = schedule.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val startFormatted = RadioTimelineCalculator.formatClockTimeShort(schedule.startTimeSecondsOfDay)
                                    Text(
                                        text = "Start: $startFormatted",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("radio_picker_cancel_button")
            ) {
                Text(JnmfStrings.close(lang))
            }
        }
    )
}
