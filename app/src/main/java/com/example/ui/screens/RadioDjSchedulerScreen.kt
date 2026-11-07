package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.playback.RadioBroadcastAlarmReceiver
import com.example.playback.RadioPreviewController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.RadioScheduleEntity
import com.example.data.db.RadioScheduleItemEntity
import com.example.data.model.RadioItemType
import com.example.data.model.RadioScheduledItem
import com.example.data.model.RadioTimelineCalculator
import com.example.data.model.RadioTimelineEntry
import com.example.data.model.RadioTimelineSummary
import com.example.data.model.Song
import com.example.data.repository.AppLanguage
import com.example.data.repository.RadioScheduleRepository
import com.example.data.repository.SaveResult
import com.example.playback.MusicController
import com.example.playback.RadioPlaybackController
import com.example.playback.RadioPlaybackState
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.RadioDjStrings
import kotlinx.coroutines.launch

@Composable
fun RadioDjSchedulerScreen(
    schedules: List<RadioScheduleEntity>,
    allScheduleItems: List<RadioScheduleItemEntity>,
    allSongs: List<Song>,
    hasScannedLibrary: Boolean,
    selectedScheduleId: Long?,
    onSelectedScheduleIdChange: (Long?) -> Unit,
    radioRepo: RadioScheduleRepository,
    language: AppLanguage,
    onSaveResultFeedback: (SaveResult<*>, String?) -> Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val radioState by RadioPlaybackController.state.collectAsState()
    val musicState by MusicController.state.collectAsState()
    val previewState by RadioPreviewController.state.collectAsState()
    val djModeEnabled = musicState.djModeEnabled
    val djTransitionSeconds = musicState.djTransitionSeconds

    DisposableEffect(Unit) {
        onDispose { RadioPreviewController.stop() }
    }

    // Ensure a valid selected schedule whenever schedules list updates
    val activeSchedule: RadioScheduleEntity? = remember(schedules, selectedScheduleId) {
        if (schedules.isEmpty()) {
            null
        } else {
            schedules.find { it.id == selectedScheduleId } ?: schedules.first()
        }
    }

    LaunchedEffect(
        activeSchedule?.id,
        activeSchedule?.timeAnnouncementEnabled,
        activeSchedule?.hourlyAnnouncementEnabled,
        activeSchedule?.customAnnouncementMinutes,
        activeSchedule?.timeAnnouncementAudioUri,
        activeSchedule?.timeAnnouncementTitle
    ) {
        val s = activeSchedule ?: return@LaunchedEffect
        val uri = if (s.timeAnnouncementAudioUri.isNotBlank()) android.net.Uri.parse(s.timeAnnouncementAudioUri) else null
        RadioPlaybackController.configureTimeAnnouncement(
            enabled = s.timeAnnouncementEnabled,
            hourly = s.hourlyAnnouncementEnabled,
            customMinutes = s.customAnnouncementMinutes,
            audioUri = uri,
            title = s.timeAnnouncementTitle
        )
    }

    LaunchedEffect(activeSchedule?.id) {
        if (activeSchedule != null && activeSchedule.id != selectedScheduleId) {
            onSelectedScheduleIdChange(activeSchedule.id)
        } else if (activeSchedule == null && selectedScheduleId != null && schedules.isEmpty()) {
            onSelectedScheduleIdChange(null)
        }
    }

    val songsById = remember(allSongs) { allSongs.associateBy { it.id } }
    var scannedUnavailableIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val activeRawItems = remember(allScheduleItems, activeSchedule?.id) {
        val targetId = activeSchedule?.id ?: return@remember emptyList()
        allScheduleItems.filter { it.scheduleId == targetId }
    }

    val combinedUnavailableIds = remember(scannedUnavailableIds, radioState.unavailableItemIds) {
        if (scannedUnavailableIds.isEmpty()) {
            radioState.unavailableItemIds
        } else if (radioState.unavailableItemIds.isEmpty()) {
            scannedUnavailableIds
        } else {
            scannedUnavailableIds + radioState.unavailableItemIds
        }
    }

    val resolvedItems = remember(
        activeRawItems,
        songsById,
        hasScannedLibrary,
        combinedUnavailableIds
    ) {
        radioRepo.resolveItemsForUi(
            entities = activeRawItems,
            songsById = songsById,
            hasScannedLibrary = hasScannedLibrary,
            knownUnavailableItemIds = combinedUnavailableIds
        )
    }

    // Background IO pass to detect inaccessible external SAF/content URIs without main-thread I/O
    LaunchedEffect(activeSchedule?.id, activeRawItems.size) {
        if (resolvedItems.isNotEmpty()) {
            scannedUnavailableIds = radioRepo.scanUnavailableItemIds(resolvedItems)
        } else {
            scannedUnavailableIds = emptySet()
        }
    }

    // Keep live RadioPlaybackController queue synchronized if active schedule is edited
    LaunchedEffect(activeSchedule?.id, activeSchedule?.name, resolvedItems) {
        if (activeSchedule != null) {
            RadioPlaybackController.syncActiveScheduleEdits(
                scheduleId = activeSchedule.id,
                scheduleName = activeSchedule.name,
                updatedItems = resolvedItems
            )
        }
    }

    val timelineSummary: RadioTimelineSummary? = remember(
        activeSchedule?.startTimeSecondsOfDay,
        resolvedItems,
        djModeEnabled,
        djTransitionSeconds
    ) {
        val sched = activeSchedule ?: return@remember null
        RadioTimelineCalculator.computeTimeline(
            startTimeSecondsOfDay = sched.startTimeSecondsOfDay,
            items = resolvedItems,
            djModeEnabled = djModeEnabled,
            djTransitionSeconds = djTransitionSeconds
        )
    }

    // Dialog states
    var showCreateScheduleDialog by remember { mutableStateOf(false) }
    var showRenameScheduleDialog by remember { mutableStateOf(false) }
    var showDeleteScheduleDialog by remember { mutableStateOf(false) }
    var showStartTimeDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToReplace by remember { mutableStateOf<RadioScheduledItem?>(null) }
    var itemForTypeChange by remember { mutableStateOf<RadioScheduledItem?>(null) }

    // Pending SAF external audio selection context (Add vs Replace + chosen RadioItemType)
    var pendingExternalType by remember { mutableStateOf(RadioItemType.ADVERTISEMENT) }
    var pendingExternalReplaceItemId by remember { mutableStateOf<Long?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    val openAnnouncementFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { pickedUri ->
        val sched = activeSchedule
        if (pickedUri != null && sched != null) {
            val info = RadioScheduleRepository.inspectExternalAudioUri(context, pickedUri)
            scope.launch {
                val res = radioRepo.updateAnnouncementSettings(
                    scheduleId = sched.id,
                    enabled = sched.timeAnnouncementEnabled,
                    hourly = sched.hourlyAnnouncementEnabled,
                    audioUri = pickedUri.toString(),
                    title = info.title,
                    durMs = info.durationMs,
                    customMinutes = sched.customAnnouncementMinutes
                )
                RadioPlaybackController.configureTimeAnnouncement(
                    enabled = sched.timeAnnouncementEnabled,
                    hourly = sched.hourlyAnnouncementEnabled,
                    customMinutes = sched.customAnnouncementMinutes,
                    audioUri = pickedUri,
                    title = info.title
                )
                onSaveResultFeedback(res, null)
            }
        }
    }

    val openAudioDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { pickedUri ->
        val sched = activeSchedule
        if (pickedUri != null && sched != null) {
            val replaceId = pendingExternalReplaceItemId
            val chosenType = pendingExternalType
            pendingExternalReplaceItemId = null
            scope.launch {
                val res = if (replaceId != null) {
                    radioRepo.replaceItemWithExternalUri(
                        itemId = replaceId,
                        uri = pickedUri,
                        itemType = chosenType
                    )
                } else {
                    radioRepo.addExternalUriItem(
                        scheduleId = sched.id,
                        uri = pickedUri,
                        itemType = chosenType
                    )
                }
                onSaveResultFeedback(res, null)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("radio_dj_scheduler_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Section Header
        item(key = "radio_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = RadioDjStrings.screenTitle(language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = RadioDjStrings.screenSubtitle(language),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = { showCreateScheduleDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("radio_new_schedule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(RadioDjStrings.newSchedule(language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Saved Radio Programs Selector & Management Bar
        item(key = "radio_saved_schedules") {
            SavedSchedulesSelectorCard(
                schedules = schedules,
                activeSchedule = activeSchedule,
                language = language,
                onSelectSchedule = { id -> onSelectedScheduleIdChange(id) },
                onRenameClick = { showRenameScheduleDialog = true },
                onDuplicateClick = {
                    val sched = activeSchedule ?: return@SavedSchedulesSelectorCard
                    scope.launch {
                        val res = radioRepo.duplicateSchedule(sched.id)
                        if (res is SaveResult.Success) {
                            onSelectedScheduleIdChange(res.data)
                        } else {
                            onSaveResultFeedback(res, null)
                        }
                    }
                },
                onDeleteClick = { showDeleteScheduleDialog = true }
            )
        }

        if (activeSchedule != null && timelineSummary != null) {
            // 3. Live Radio Broadcast Control Deck
            item(key = "radio_broadcast_deck") {
                RadioBroadcastDeckCard(
                    activeSchedule = activeSchedule,
                    resolvedItems = resolvedItems,
                    radioState = radioState,
                    language = language,
                    onStartRadio = {
                        RadioPlaybackController.startRadioProgram(
                            scheduleId = activeSchedule.id,
                            scheduleName = activeSchedule.name,
                            items = resolvedItems,
                            startIndex = 0
                        )
                    }
                )
            }

            // 4. Program Start Time & Estimated Broadcast Timeline Summary
            item(key = "radio_timeline_summary") {
                TimelineClockSummaryCard(
                    summary = timelineSummary,
                    schedule = activeSchedule,
                    language = language,
                    onEditStartTime = { showStartTimeDialog = true },
                    onToggleAutoSchedule = { enabled ->
                        scope.launch {
                            val res = radioRepo.updateAutoSchedule(activeSchedule.id, enabled)
                            if (enabled) {
                                RadioBroadcastAlarmReceiver.scheduleAutoPlayAlarm(
                                    context,
                                    activeSchedule.id,
                                    activeSchedule.startTimeSecondsOfDay
                                )
                            } else {
                                RadioBroadcastAlarmReceiver.cancelAutoPlayAlarm(context, activeSchedule.id)
                            }
                            onSaveResultFeedback(res, null)
                        }
                    },
                    onOpenAnnouncementSettings = {
                        showAnnouncementDialog = true
                    }
                )
            }

            // 5. Timeline Section Header + Add Item Action
            item(key = "radio_timeline_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = RadioDjStrings.timelineSectionHeader(timelineSummary.entries.size, language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val res = radioRepo.saveScheduleExplicitly(activeSchedule.id)
                                    onSaveResultFeedback(res, RadioDjStrings.manualSaveSuccess(language))
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("radio_manual_save_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(RadioDjStrings.manualSaveButton(language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showAddItemDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("radio_add_item_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(RadioDjStrings.addProgramItem(language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 6. Program Timeline Slots
            if (timelineSummary.entries.isEmpty()) {
                item(key = "radio_empty_timeline") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("radio_empty_timeline_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = RadioDjStrings.emptyTimelineHint(language),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = timelineSummary.entries,
                    key = { _, entry -> entry.item.id }
                ) { index, entry ->
                    val isCurrentProgramItem = !radioState.isPreviewMode &&
                        radioState.activeScheduleId == activeSchedule.id &&
                        radioState.currentItem?.id == entry.item.id
                    val isPreviewingThisItem = previewState.previewItemId == entry.item.id && previewState.isPlaying

                    RadioTimelineSlotCard(
                        entry = entry,
                        index = index,
                        totalCount = timelineSummary.entries.size,
                        isCurrentProgramItem = isCurrentProgramItem,
                        isProgramPlaying = isCurrentProgramItem && radioState.isPlaying,
                        isPreviewingThisItem = isPreviewingThisItem,
                        language = language,
                        onPlayFromSlot = {
                            RadioPlaybackController.startRadioProgram(
                                scheduleId = activeSchedule.id,
                                scheduleName = activeSchedule.name,
                                items = resolvedItems,
                                startIndex = index
                            )
                        },
                        onTogglePreview = {
                            if (isPreviewingThisItem) {
                                RadioPreviewController.stop()
                            } else {
                                RadioPreviewController.previewItem(
                                    context = context,
                                    itemId = entry.item.id,
                                    uri = entry.item.contentUri,
                                    title = entry.item.title,
                                    gainPercent = entry.item.gainPercent
                                )
                            }
                        },
                        onDecreaseGain = {
                            val newGain = (entry.item.gainPercent - 5).coerceIn(50, 150)
                            scope.launch {
                                val res = radioRepo.updateItemGain(entry.item.id, newGain)
                                RadioPlaybackController.updateCurrentItemGainIfActive(entry.item.id, newGain)
                                if (previewState.previewItemId == entry.item.id) {
                                    RadioPreviewController.setGain(newGain)
                                }
                                onSaveResultFeedback(res, null)
                            }
                        },
                        onIncreaseGain = {
                            val newGain = (entry.item.gainPercent + 5).coerceIn(50, 150)
                            scope.launch {
                                val res = radioRepo.updateItemGain(entry.item.id, newGain)
                                RadioPlaybackController.updateCurrentItemGainIfActive(entry.item.id, newGain)
                                if (previewState.previewItemId == entry.item.id) {
                                    RadioPreviewController.setGain(newGain)
                                }
                                onSaveResultFeedback(res, null)
                            }
                        },
                        onMoveUp = {
                            if (index > 0) {
                                scope.launch {
                                    val res = radioRepo.moveItem(activeSchedule.id, index, index - 1)
                                    onSaveResultFeedback(res, null)
                                }
                            }
                        },
                        onMoveDown = {
                            if (index < timelineSummary.entries.lastIndex) {
                                scope.launch {
                                    val res = radioRepo.moveItem(activeSchedule.id, index, index + 1)
                                    onSaveResultFeedback(res, null)
                                }
                            }
                        },
                        onMoveToIndex = { toIndex ->
                            if (toIndex in timelineSummary.entries.indices && toIndex != index) {
                                scope.launch {
                                    val res = radioRepo.moveItem(activeSchedule.id, index, toIndex)
                                    onSaveResultFeedback(res, null)
                                }
                            }
                        },
                        onChangeType = { itemForTypeChange = entry.item },
                        onReplace = { itemToReplace = entry.item },
                        onRemove = {
                            scope.launch {
                                val res = radioRepo.removeItem(activeSchedule.id, entry.item.id)
                                onSaveResultFeedback(res, null)
                            }
                        }
                    )
                }
            }
        }
    }

    // --- Dialogs ---
    if (showCreateScheduleDialog) {
        CreateOrRenameRadioScheduleDialog(
            isRename = false,
            initialName = "",
            lang = language,
            onDismiss = { showCreateScheduleDialog = false },
            onConfirm = { newName ->
                scope.launch {
                    val res = radioRepo.createSchedule(newName)
                    if (res is SaveResult.Success) {
                        onSelectedScheduleIdChange(res.data)
                        showCreateScheduleDialog = false
                    } else {
                        onSaveResultFeedback(res, RadioDjStrings.scheduleAlreadyExists(language))
                    }
                }
            }
        )
    }

    if (showRenameScheduleDialog && activeSchedule != null) {
        CreateOrRenameRadioScheduleDialog(
            isRename = true,
            initialName = activeSchedule.name,
            lang = language,
            onDismiss = { showRenameScheduleDialog = false },
            onConfirm = { updatedName ->
                scope.launch {
                    val res = radioRepo.renameSchedule(activeSchedule.id, updatedName)
                    if (onSaveResultFeedback(res, RadioDjStrings.scheduleAlreadyExists(language))) {
                        showRenameScheduleDialog = false
                    }
                }
            }
        )
    }

    if (showDeleteScheduleDialog && activeSchedule != null) {
        DeleteRadioScheduleDialog(
            scheduleName = activeSchedule.name,
            lang = language,
            onDismiss = { showDeleteScheduleDialog = false },
            onConfirmDelete = {
                val deletingId = activeSchedule.id
                showDeleteScheduleDialog = false
                if (radioState.activeScheduleId == deletingId) {
                    RadioPlaybackController.stop()
                }
                scope.launch {
                    val res = radioRepo.deleteSchedule(deletingId)
                    onSaveResultFeedback(res, null)
                }
            }
        )
    }

    if (showStartTimeDialog && activeSchedule != null) {
        ProgramStartTimeDialog(
            currentStartTimeSeconds = activeSchedule.startTimeSecondsOfDay,
            lang = language,
            onDismiss = { showStartTimeDialog = false },
            onConfirmSeconds = { newSeconds ->
                showStartTimeDialog = false
                scope.launch {
                    val res = radioRepo.updateScheduleStartTime(activeSchedule.id, newSeconds)
                    onSaveResultFeedback(res, null)
                }
            }
        )
    }

    val existingScheduledSongIds = remember(resolvedItems) {
        resolvedItems.mapNotNull { if (it.songId > 0L) it.songId else null }.toSet()
    }

    if (showAddItemDialog && activeSchedule != null) {
        AddOrReplaceRadioItemDialog(
            isReplaceMode = false,
            initialType = RadioItemType.MUSIC,
            allSongs = allSongs,
            existingSongIds = existingScheduledSongIds,
            lang = language,
            onDismiss = { showAddItemDialog = false },
            onPickExternalFile = { chosenType ->
                pendingExternalType = chosenType
                pendingExternalReplaceItemId = null
                showAddItemDialog = false
                openAudioDocumentLauncher.launch(arrayOf("audio/*", "video/*"))
            },
            onSelectLibrarySong = { song, chosenType ->
                showAddItemDialog = false
                scope.launch {
                    val res = radioRepo.addSongItem(activeSchedule.id, song, chosenType)
                    onSaveResultFeedback(res, null)
                }
            },
            onPreviewSong = { song ->
                RadioPreviewController.previewItem(
                    context = context,
                    itemId = song.id,
                    uri = song.contentUri,
                    title = song.title,
                    gainPercent = 100
                )
            }
        )
    }

    val replacingTarget = itemToReplace
    if (replacingTarget != null && activeSchedule != null) {
        AddOrReplaceRadioItemDialog(
            isReplaceMode = true,
            initialType = replacingTarget.itemType,
            allSongs = allSongs,
            existingSongIds = existingScheduledSongIds,
            lang = language,
            onDismiss = { itemToReplace = null },
            onPickExternalFile = { chosenType ->
                pendingExternalType = chosenType
                pendingExternalReplaceItemId = replacingTarget.id
                itemToReplace = null
                openAudioDocumentLauncher.launch(arrayOf("audio/*", "video/*"))
            },
            onSelectLibrarySong = { song, chosenType ->
                val targetId = replacingTarget.id
                itemToReplace = null
                scope.launch {
                    val res = radioRepo.replaceItemWithSong(targetId, song, chosenType)
                    onSaveResultFeedback(res, null)
                }
            },
            onPreviewSong = { song ->
                RadioPreviewController.previewItem(
                    context = context,
                    itemId = song.id,
                    uri = song.contentUri,
                    title = song.title,
                    gainPercent = 100
                )
            }
        )
    }

    if (showAnnouncementDialog && activeSchedule != null) {
        TimeAnnouncementSettingsDialog(
            initialEnabled = activeSchedule.timeAnnouncementEnabled,
            initialHourly = activeSchedule.hourlyAnnouncementEnabled,
            initialAudioUri = activeSchedule.timeAnnouncementAudioUri,
            initialTitle = activeSchedule.timeAnnouncementTitle,
            initialDurationMs = activeSchedule.timeAnnouncementDurationMs,
            initialCustomMinutes = activeSchedule.customAnnouncementMinutes,
            lang = language,
            onDismiss = { showAnnouncementDialog = false },
            onPickAudioFile = {
                openAnnouncementFileLauncher.launch(arrayOf("audio/*", "video/*"))
            },
            onPreviewAudio = { uri, title ->
                RadioPreviewController.previewItem(
                    context = context,
                    itemId = -999L,
                    uri = uri,
                    title = title,
                    gainPercent = 100
                )
            },
            onSave = { enabled, hourly, audioUri, title, durMs, customMinutes ->
                showAnnouncementDialog = false
                scope.launch {
                    val res = radioRepo.updateAnnouncementSettings(
                        scheduleId = activeSchedule.id,
                        enabled = enabled,
                        hourly = hourly,
                        audioUri = audioUri,
                        title = title,
                        durMs = durMs,
                        customMinutes = customMinutes
                    )
                    RadioPlaybackController.configureTimeAnnouncement(
                        enabled = enabled,
                        hourly = hourly,
                        customMinutes = customMinutes,
                        audioUri = if (audioUri.isNotBlank()) android.net.Uri.parse(audioUri) else null,
                        title = title
                    )
                    onSaveResultFeedback(res, null)
                }
            }
        )
    }

    val typeChangeTarget = itemForTypeChange
    if (typeChangeTarget != null) {
        ChangeRadioItemTypeDialog(
            currentType = typeChangeTarget.itemType,
            itemTitle = typeChangeTarget.title,
            lang = language,
            onDismiss = { itemForTypeChange = null },
            onSelectType = { newType ->
                val targetId = typeChangeTarget.id
                itemForTypeChange = null
                scope.launch {
                    val res = radioRepo.updateItemType(targetId, newType)
                    onSaveResultFeedback(res, null)
                }
            }
        )
    }
    }
}

@Composable
private fun SavedSchedulesSelectorCard(
    schedules: List<RadioScheduleEntity>,
    activeSchedule: RadioScheduleEntity?,
    language: AppLanguage,
    onSelectSchedule: (Long) -> Unit,
    onRenameClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = RadioDjStrings.savedSchedulesHeader(language),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (schedules.isEmpty()) {
                Text(
                    text = RadioDjStrings.emptySchedulesHint(language),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("radio_empty_schedules_hint")
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    schedules.forEach { sched ->
                        val isSelected = sched.id == activeSchedule?.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            ),
                            modifier = Modifier
                                .clickable { onSelectSchedule(sched.id) }
                                .testTag("radio_schedule_chip_${sched.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sched.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = RadioTimelineCalculator.formatClockTimeShort(sched.startTimeSecondsOfDay),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (activeSchedule != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onRenameClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("radio_rename_schedule_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(JnmfStrings.rename(language), fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onDuplicateClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("radio_duplicate_schedule_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(RadioDjStrings.duplicateSchedule(language), fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onDeleteClick,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("radio_delete_schedule_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                JnmfStrings.delete(language),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioBroadcastDeckCard(
    activeSchedule: RadioScheduleEntity,
    resolvedItems: List<RadioScheduledItem>,
    radioState: RadioPlaybackState,
    language: AppLanguage,
    onStartRadio: () -> Unit
) {
    val isThisScheduleOnDeck = !radioState.isPreviewMode && radioState.activeScheduleId == activeSchedule.id
    val currentItem = radioState.currentItem
    val upcomingItem = radioState.upcomingItem

    val statusText = when {
        radioState.isPreviewMode -> RadioDjStrings.statusPreviewing(language)
        radioState.isPlaying -> RadioDjStrings.statusOnAir(language)
        radioState.isProgramCompleted && isThisScheduleOnDeck -> RadioDjStrings.statusCompleted(language)
        currentItem != null -> RadioDjStrings.statusPaused(language)
        else -> RadioDjStrings.statusStandby(language)
    }

    val statusColor = when {
        radioState.isPreviewMode -> Color(0xFF00E5FF)
        radioState.isPlaying -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("radio_broadcast_deck_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.16f))
                        .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("radio_status_badge")
                    )
                }

                Button(
                    onClick = onStartRadio,
                    enabled = resolvedItems.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("radio_start_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(RadioDjStrings.startRadio(language), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (currentItem != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = RadioDjStrings.currentItemHeader(language),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        RadioItemTypeBadge(type = currentItem.itemType, lang = language)
                    }

                    Text(
                        text = currentItem.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("radio_now_playing_title")
                    )

                    val dur = radioState.durationMs.coerceAtLeast(1L)
                    val pos = radioState.currentPositionMs.coerceIn(0L, dur)
                    Slider(
                        value = pos.toFloat(),
                        onValueChange = { RadioPlaybackController.seekTo(it.toLong()) },
                        valueRange = 0f..dur.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = RadioTimelineCalculator.formatDurationCompact(pos),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = RadioTimelineCalculator.formatDurationCompact(radioState.durationMs),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Upcoming Item ("Up Next") preview
                if (!radioState.isPreviewMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${RadioDjStrings.upcomingItemHeader(language)}: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (upcomingItem != null) {
                            Text(
                                text = upcomingItem.title,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("radio_upcoming_title")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            RadioItemTypeBadge(type = upcomingItem.itemType, lang = language)
                        } else {
                            Text(
                                text = RadioDjStrings.endOfProgramLabel(language),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("radio_upcoming_title")
                            )
                        }
                    }
                }
            }

            // Transport Controls Row: Previous, Play/Pause, Next, Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { RadioPlaybackController.previous() },
                    enabled = currentItem != null,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("radio_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = JnmfStrings.previous(language),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Button(
                    onClick = {
                        if (currentItem == null && resolvedItems.isNotEmpty()) {
                            onStartRadio()
                        } else {
                            RadioPlaybackController.togglePlayPause()
                        }
                    },
                    enabled = currentItem != null || resolvedItems.isNotEmpty(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("radio_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (radioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (radioState.isPlaying) {
                            JnmfStrings.pause(language)
                        } else {
                            JnmfStrings.play(language)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = { RadioPlaybackController.next() },
                    enabled = currentItem != null,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("radio_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = JnmfStrings.next(language),
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = { RadioPlaybackController.stop() },
                    enabled = currentItem != null || radioState.isPlaying,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("radio_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = RadioDjStrings.stopRadio(language),
                        tint = if (currentItem != null || radioState.isPlaying) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Non-crashing feedback banner when missing/corrupted files are skipped
            val feedbackMsg = radioState.errorMessage ?: radioState.statusNotice
            if (!feedbackMsg.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFAB00).copy(alpha = 0.14f))
                        .border(1.dp, Color(0xFFFFAB00).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .testTag("radio_feedback_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFAB00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feedbackMsg,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineClockSummaryCard(
    summary: RadioTimelineSummary,
    schedule: RadioScheduleEntity,
    language: AppLanguage,
    onEditStartTime: () -> Unit,
    onToggleAutoSchedule: (Boolean) -> Unit,
    onOpenAnnouncementSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = RadioDjStrings.programStartTimeLabel(language),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.formattedProgramStartTime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("radio_program_start_display")
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = RadioDjStrings.totalDurationLabel(language),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.formattedTotalDuration,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("radio_total_duration_display")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = RadioDjStrings.estimatedFinishLabel(language),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.formattedEstimatedFinishTime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("radio_estimated_finish_display")
                    )
                }
            }

            // Start Time Edit and Announcement Settings Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditStartTime,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("radio_start_time_button")
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(RadioDjStrings.changeStartTime(language), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenAnnouncementSettings,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("radio_announcement_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (schedule.timeAnnouncementEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(RadioDjStrings.edit(language), fontSize = 11.sp)
                }
            }

            // Compact Auto-Schedule Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (schedule.isAutoScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (schedule.isAutoScheduled) {
                            "Auto-Start: ${RadioTimelineCalculator.formatClockTimeShort(schedule.startTimeSecondsOfDay)}"
                        } else {
                            RadioDjStrings.autoSchedule(language)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (schedule.isAutoScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = schedule.isAutoScheduled,
                    onCheckedChange = onToggleAutoSchedule,
                    modifier = Modifier.testTag("radio_auto_schedule_toggle")
                )
            }

            if (summary.djModeEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("radio_dj_mode_timeline_indicator"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = RadioDjStrings.djCrossfadeTimelineActive(
                            seconds = summary.djTransitionSeconds,
                            overlapStr = RadioTimelineCalculator.formatDurationCompact(summary.totalOverlapMs),
                            lang = language
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = RadioDjStrings.estimatedDisclaimer(language),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("radio_estimated_disclaimer")
                )
            }
        }
    }
}

@Composable
private fun RadioTimelineSlotCard(
    entry: RadioTimelineEntry,
    index: Int,
    totalCount: Int,
    isCurrentProgramItem: Boolean,
    isProgramPlaying: Boolean,
    isPreviewingThisItem: Boolean,
    language: AppLanguage,
    onPlayFromSlot: () -> Unit,
    onTogglePreview: () -> Unit,
    onDecreaseGain: () -> Unit,
    onIncreaseGain: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToIndex: ((Int) -> Unit)? = null,
    onChangeType: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    val item = entry.item
    val accent = radioItemTypeColor(item.itemType)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentProgramItem) {
                accent.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isCurrentProgramItem) 1.5.dp else 1.dp,
            color = if (isCurrentProgramItem) accent else accent.copy(alpha = 0.28f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isAvailable, onClick = onPlayFromSlot)
            .testTag("radio_timeline_item_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Row: Drag Handle, Slot #, Clock Start - Est. End, Duration, and Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = RadioDjStrings.dragToReorder(language),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("radio_item_drag_handle_$index")
                            .pointerInput(index, totalCount) {
                                var accumulatedY = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { accumulatedY = 0f },
                                    onDragEnd = {
                                        val slotHeightPx = 90.dp.toPx()
                                        val deltaSteps = (accumulatedY / slotHeightPx).toInt()
                                        if (deltaSteps != 0) {
                                            val target = (index + deltaSteps).coerceIn(0, totalCount - 1)
                                            if (target != index) {
                                                onMoveToIndex?.invoke(target)
                                            }
                                        }
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedY += dragAmount
                                    }
                                )
                            }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${entry.slotNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = RadioDjStrings.estimatedSlotRange(
                            start = entry.formattedStartTime,
                            end = entry.formattedEndTime,
                            lang = language
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("radio_item_clock_$index")
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                RadioItemTypeBadge(
                    type = item.itemType,
                    lang = language,
                    modifier = Modifier.testTag("radio_item_badge_$index")
                )
            }

            // Middle Row: Title, Subtitle, Missing Badge, Audio Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!item.isAvailable) {
                            Icons.Default.Warning
                        } else if (isProgramPlaying) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            radioItemTypeIcon(item.itemType)
                        },
                        contentDescription = null,
                        tint = if (!item.isAvailable) MaterialTheme.colorScheme.error else accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isAvailable) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("radio_item_title_$index")
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!item.isAvailable) {
                            Text(
                                text = JnmfStrings.missingFileBadge(language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("radio_item_missing_$index")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "${RadioDjStrings.itemTypeLabel(item.itemType, language)} • ${item.formattedDuration}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Sound Level / Gain Adjustment Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = RadioDjStrings.gainLevel(language),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDecreaseGain,
                        enabled = item.gainPercent > 50,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("radio_item_gain_minus_$index")
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                    }

                    Text(
                        text = "${item.gainPercent}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (item.gainPercent != 100) accent else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .testTag("radio_item_gain_value_$index")
                    )

                    IconButton(
                        onClick = onIncreaseGain,
                        enabled = item.gainPercent < 150,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("radio_item_gain_plus_$index")
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
                    }
                }
            }

            // Action Controls Row: Preview, Move Up, Move Down, Type, Replace, Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTogglePreview,
                    enabled = item.isAvailable,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("radio_item_preview_$index")
                ) {
                    Icon(
                        imageVector = if (isPreviewingThisItem) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPreviewingThisItem) {
                            RadioDjStrings.stopPreview(language)
                        } else {
                            RadioDjStrings.previewItem(language)
                        },
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("radio_item_move_up_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = JnmfStrings.moveUp(language),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("radio_item_move_down_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = JnmfStrings.moveDown(language),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onChangeType,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("radio_item_type_$index")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = RadioDjStrings.changeType(language),
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onReplace,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("radio_item_replace_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = RadioDjStrings.replaceItem(language),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("radio_item_remove_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = RadioDjStrings.removeItem(language),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
