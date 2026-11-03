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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.data.model.ViewSection
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils

@Composable
fun NoteListScreen(
    currentSection: ViewSection,
    selectedFolder: Folder?,
    selectedTag: Tag?,
    notes: List<Note>,
    folders: List<Folder>,
    tags: List<Tag>,
    noteTagsMap: Map<String, List<String>>,
    activeNoteId: String?,
    isCompactMode: Boolean,
    onSelectNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDuplicateNote: (Note) -> Unit,
    onArchiveNote: (String) -> Unit,
    onUnarchiveNote: (String) -> Unit,
    onTrashNote: (String) -> Unit,
    onRestoreNote: (String) -> Unit,
    onPermanentDeleteNote: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("updated-desc") }
    var showSortMenu by remember { mutableStateOf(false) }

    val folderMap = remember(folders) { folders.associateBy { it.id } }
    val tagMap = remember(tags) { tags.associateBy { it.id } }

    val isTrashSection = currentSection == ViewSection.TRASH
    val isArchiveSection = currentSection == ViewSection.ARCHIVE

    // Determine Title and Subtitle
    val (title, subtitle) = remember(currentSection, selectedFolder, selectedTag, strings) {
        when {
            selectedFolder != null -> selectedFolder.name to (selectedFolder.description.ifBlank { "Folder" })
            selectedTag != null -> "#${selectedTag.name}" to strings.tags
            currentSection == ViewSection.FAVORITES -> strings.favorites to strings.metricStarred
            currentSection == ViewSection.RECENT -> strings.recentNotes to strings.metricActive
            currentSection == ViewSection.ARCHIVE -> strings.archive to strings.metricPreserved
            currentSection == ViewSection.TRASH -> strings.trash to strings.metricInBin
            else -> strings.allNotes to strings.allNotesUpper
        }
    }

    // Filter notes
    val oneWeekAgo = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val baseFilteredNotes = remember(notes, currentSection, selectedFolder, selectedTag) {
        notes.filter { note ->
            when {
                isTrashSection -> note.inTrash
                note.inTrash -> false
                isArchiveSection -> note.isArchived
                note.isArchived -> false
                selectedFolder != null -> note.folderId == selectedFolder.id
                selectedTag != null -> (noteTagsMap[note.id] ?: emptyList()).contains(selectedTag.id)
                currentSection == ViewSection.FAVORITES -> note.isFavorite
                currentSection == ViewSection.RECENT -> note.updatedAt >= oneWeekAgo
                else -> true
            }
        }
    }

    val searchFilteredNotes = remember(baseFilteredNotes, searchQuery, sortBy) {
        val q = searchQuery.trim().lowercase()
        val list = if (q.isBlank()) {
            baseFilteredNotes
        } else {
            baseFilteredNotes.filter { n ->
                n.title.lowercase().contains(q) || n.content.lowercase().contains(q)
            }
        }

        list.sortedWith { a, b ->
            if (!isTrashSection && !isArchiveSection) {
                if (a.isPinned && !b.isPinned) return@sortedWith -1
                if (!a.isPinned && b.isPinned) return@sortedWith 1
            }
            when (sortBy) {
                "updated-asc" -> a.updatedAt.compareTo(b.updatedAt)
                "created-desc" -> b.createdAt.compareTo(a.createdAt)
                "title-asc" -> a.title.compareTo(b.title, ignoreCase = true)
                else -> b.updatedAt.compareTo(a.updatedAt)
            }
        }
    }

    val (pinnedNotes, regularNotes) = remember(searchFilteredNotes, isTrashSection, isArchiveSection) {
        if (isTrashSection || isArchiveSection) {
            emptyList<Note>() to searchFilteredNotes
        } else {
            searchFilteredNotes.partition { it.isPinned }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSidebar)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("notes_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 10.5.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTrashSection && searchFilteredNotes.isNotEmpty()) {
                        Button(
                            onClick = onEmptyTrash,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dangerBg,
                                contentColor = colors.danger
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("empty_trash_button")
                        ) {
                            Text(strings.emptyTrash, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (!isTrashSection) {
                        IconButton(
                            onClick = onNewNote,
                            modifier = Modifier.testTag("notes_new_note_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.newNote,
                                tint = colors.textPrimary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

            // Search and sort bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSidebar)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.filterNotes, fontSize = 12.sp, color = colors.textMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("filter_notes_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.bgElevated,
                        unfocusedContainerColor = colors.bgElevated,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderColor,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.bgElevated)
                            .testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(colors.bgElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.sortModifiedNewest, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = { sortBy = "updated-desc"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortModifiedOldest, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = { sortBy = "updated-asc"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortCreatedNewest, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = { sortBy = "created-desc"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortTitleAz, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = { sortBy = "title-asc"; showSortMenu = false }
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

            // Notes List
            if (searchFilteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = colors.textMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.noNotesFound,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) strings.tryAnotherKeyword else strings.tapToCreateFirstNote,
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (!isTrashSection && searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNewNote,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.bgElevated,
                                    contentColor = colors.textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ ${strings.newNote}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.pinned,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textMuted,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                isCompactMode = isCompactMode,
                                isTrashSection = isTrashSection,
                                folder = note.folderId?.let { folderMap[it] },
                                tags = (noteTagsMap[note.id] ?: emptyList()).mapNotNull { tagMap[it] },
                                onSelectNote = { onSelectNote(note.id) },
                                onToggleFavorite = { onToggleFavorite(note.id) },
                                onTogglePin = { onTogglePin(note.id) },
                                onDuplicateNote = { onDuplicateNote(note) },
                                onArchiveNote = { onArchiveNote(note.id) },
                                onUnarchiveNote = { onUnarchiveNote(note.id) },
                                onTrashNote = { onTrashNote(note.id) },
                                onRestoreNote = { onRestoreNote(note.id) },
                                onPermanentDeleteNote = { onPermanentDeleteNote(note) }
                            )
                        }
                    }

                    if (regularNotes.isNotEmpty()) {
                        if (pinnedNotes.isNotEmpty()) {
                            item {
                                Text(
                                    text = strings.allNotesUpper,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textMuted,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                        }
                        items(regularNotes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                isCompactMode = isCompactMode,
                                isTrashSection = isTrashSection,
                                folder = note.folderId?.let { folderMap[it] },
                                tags = (noteTagsMap[note.id] ?: emptyList()).mapNotNull { tagMap[it] },
                                onSelectNote = { onSelectNote(note.id) },
                                onToggleFavorite = { onToggleFavorite(note.id) },
                                onTogglePin = { onTogglePin(note.id) },
                                onDuplicateNote = { onDuplicateNote(note) },
                                onArchiveNote = { onArchiveNote(note.id) },
                                onUnarchiveNote = { onUnarchiveNote(note.id) },
                                onTrashNote = { onTrashNote(note.id) },
                                onRestoreNote = { onRestoreNote(note.id) },
                                onPermanentDeleteNote = { onPermanentDeleteNote(note) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Floating Action Button
        if (!isTrashSection) {
            FloatingActionButton(
                onClick = onNewNote,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("notes_fab_new_note"),
                containerColor = colors.textPrimary,
                contentColor = colors.bgMain,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.newNote,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteCardItem(
    note: Note,
    isCompactMode: Boolean,
    isTrashSection: Boolean,
    folder: Folder?,
    tags: List<Tag>,
    onSelectNote: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onDuplicateNote: () -> Unit,
    onArchiveNote: () -> Unit,
    onUnarchiveNote: () -> Unit,
    onTrashNote: () -> Unit,
    onRestoreNote: () -> Unit,
    onPermanentDeleteNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgCard)
            .border(0.8.dp, colors.borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelectNote)
            .padding(if (isCompactMode) 10.dp else 12.dp)
    ) {
        Column {
            // Header Row: Pin, Star, Title, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = note.title.ifBlank { strings.untitledNote },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isTrashSection) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) colors.accent else colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(colors.bgElevated)
                        ) {
                            if (!isTrashSection) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(strings.duplicate, fontSize = 12.sp, color = colors.textPrimary) },
                                    onClick = { onDuplicateNote(); showMenu = false }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.PushPin, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(if (note.isPinned) strings.unpin else strings.pinToTop, fontSize = 12.sp, color = colors.textPrimary) },
                                    onClick = { onTogglePin(); showMenu = false }
                                )
                                if (note.isArchived) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.Restore, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                        },
                                        text = { Text(strings.unarchive, fontSize = 12.sp, color = colors.textPrimary) },
                                        onClick = { onUnarchiveNote(); showMenu = false }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.Archive, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                        },
                                        text = { Text(strings.archiveNote, fontSize = 12.sp, color = colors.textPrimary) },
                                        onClick = { onArchiveNote(); showMenu = false }
                                    )
                                }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, null, tint = colors.danger, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(strings.moveToTrash, fontSize = 12.sp, color = colors.danger) },
                                    onClick = { onTrashNote(); showMenu = false }
                                )
                            } else {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Restore, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(strings.restoreNote, fontSize = 12.sp, color = colors.textPrimary) },
                                    onClick = { onRestoreNote(); showMenu = false }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteForever, null, tint = colors.danger, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(strings.deleteForever, fontSize = 12.sp, color = colors.danger) },
                                    onClick = { onPermanentDeleteNote(); showMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            // Snippet content
            if (!isCompactMode) {
                val snippet = note.content.lines().firstOrNull { it.isNotBlank() } ?: ""
                if (snippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = snippet,
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info: Date, Folder Badge, Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = TextFormatUtils.formatDate(note.updatedAt),
                    fontSize = 10.sp,
                    color = colors.textMuted
                )

                if (folder != null) {
                    val fColor = runCatching { Color(android.graphics.Color.parseColor(folder.color)) }.getOrDefault(colors.accent)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.bgElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = fColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = folder.name,
                            fontSize = 10.sp,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }

                tags.take(2).forEach { tag ->
                    val tColor = runCatching { Color(android.graphics.Color.parseColor(tag.color)) }.getOrDefault(colors.accent)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.bgElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(tColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "#${tag.name}",
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                if (tags.size > 2) {
                    Text(
                        text = "+${tags.size - 2}",
                        fontSize = 9.sp,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}
