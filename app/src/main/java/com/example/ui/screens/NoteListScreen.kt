package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.data.model.ViewSection
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils
import java.io.File

// Google Keep style graphite tones
val NOTE_GRAPHITE_COLORS = listOf(
    "#202124", // Default Dark Graphite
    "#262A33", // Slate Blue Tint
    "#232F2A", // Forest Pine
    "#33242A", // Muted Wine
    "#332B21", // Warm Bronze
    "#292535", // Deep Iris
    "#203033", // Dark Teal
    "#2A2B2E"  // Charcoal Titanium
)

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
    isGridView: Boolean,
    profileNickname: String,
    profileAvatarPath: String?,
    onSelectNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onUpdateNoteColor: (noteId: String, color: String) -> Unit,
    onMoveNoteToFolder: (noteId: String, folderId: String?) -> Unit,
    onDuplicateNote: (Note) -> Unit,
    onArchiveNote: (String) -> Unit,
    onUnarchiveNote: (String) -> Unit,
    onTrashNote: (String) -> Unit,
    onRestoreNote: (String) -> Unit,
    onPermanentDeleteNote: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
    onToggleGridView: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("updated-desc") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Dialog state for moving note to folder
    var noteToMoveFolder by remember { mutableStateOf<Note?>(null) }

    val folderMap = remember(folders) { folders.associateBy { it.id } }
    val tagMap = remember(tags) { tags.associateBy { it.id } }

    val isTrashSection = currentSection == ViewSection.TRASH
    val isArchiveSection = currentSection == ViewSection.ARCHIVE

    // Title and Subtitle based on active view
    val (title, subtitle) = remember(currentSection, selectedFolder, selectedTag, strings) {
        when {
            selectedFolder != null -> selectedFolder.name to (selectedFolder.description.ifBlank { strings.folders })
            selectedTag != null -> "#${selectedTag.name}" to strings.tags
            currentSection == ViewSection.FAVORITES -> strings.favorites to strings.metricStarred
            currentSection == ViewSection.RECENT -> strings.recentNotes to strings.metricActive
            currentSection == ViewSection.ARCHIVE -> strings.archive to strings.metricPreserved
            currentSection == ViewSection.TRASH -> strings.trash to strings.metricInBin
            else -> "TUMBA NOTES" to strings.allNotesUpper
        }
    }

    // Base filtering
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

    // Search and sort
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
            // Top Bar: Navigation Drawer, Logo, User Avatar, Search, Grid/List toggle, New Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSidebar)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("notes_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
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
                        if (subtitle.isNotBlank() && title != "TUMBA NOTES") {
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Search icon
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("top_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Grid / List View Toggle
                    IconButton(
                        onClick = onToggleGridView,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("toggle_grid_view_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = if (isGridView) strings.listView else strings.gridView,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // User Avatar Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.bgElevated)
                            .border(1.dp, colors.accent.copy(alpha = 0.6f), CircleShape)
                            .clickable(onClick = onOpenProfile)
                            .testTag("user_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarFile = profileAvatarPath?.let { File(it) }
                        if (avatarFile != null && avatarFile.exists()) {
                            AsyncImage(
                                model = avatarFile,
                                contentDescription = "Profile Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            val initial = profileNickname.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                            Text(
                                text = initial,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

            // Search Filter & Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSidebar)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.bgElevated)
                            .testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(17.dp)
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

            // Content: Empty State OR Grid / List of Notes
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
                            tint = colors.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.noNotesFound,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) strings.tryAnotherKeyword else strings.tapToCreateFirstNote,
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (!isTrashSection && searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNewNote,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.textPrimary,
                                    contentColor = colors.bgMain
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("create_first_note_button")
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.createNote, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (isGridView) {
                // 2-Column Staggered Grid (Google Keep Style)
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(11.dp)
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
                            NoteCard(
                                note = note,
                                isCompactMode = isCompactMode,
                                isTrashSection = isTrashSection,
                                folder = note.folderId?.let { folderMap[it] },
                                tags = (noteTagsMap[note.id] ?: emptyList()).mapNotNull { tagMap[it] },
                                onSelectNote = { onSelectNote(note.id) },
                                onToggleFavorite = { onToggleFavorite(note.id) },
                                onTogglePin = { onTogglePin(note.id) },
                                onSelectColor = { c -> onUpdateNoteColor(note.id, c) },
                                onOpenMoveFolder = { noteToMoveFolder = note },
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
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = strings.allNotesUpper,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textMuted,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                        items(regularNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                isCompactMode = isCompactMode,
                                isTrashSection = isTrashSection,
                                folder = note.folderId?.let { folderMap[it] },
                                tags = (noteTagsMap[note.id] ?: emptyList()).mapNotNull { tagMap[it] },
                                onSelectNote = { onSelectNote(note.id) },
                                onToggleFavorite = { onToggleFavorite(note.id) },
                                onTogglePin = { onTogglePin(note.id) },
                                onSelectColor = { c -> onUpdateNoteColor(note.id, c) },
                                onOpenMoveFolder = { noteToMoveFolder = note },
                                onDuplicateNote = { onDuplicateNote(note) },
                                onArchiveNote = { onArchiveNote(note.id) },
                                onUnarchiveNote = { onUnarchiveNote(note.id) },
                                onTrashNote = { onTrashNote(note.id) },
                                onRestoreNote = { onRestoreNote(note.id) },
                                onPermanentDeleteNote = { onPermanentDeleteNote(note) }
                            )
                        }
                    }
                }
            } else {
                // 1-Column List View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(11.dp)
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
                            NoteCard(
                                note = note,
                                isCompactMode = isCompactMode,
                                isTrashSection = isTrashSection,
                                folder = note.folderId?.let { folderMap[it] },
                                tags = (noteTagsMap[note.id] ?: emptyList()).mapNotNull { tagMap[it] },
                                onSelectNote = { onSelectNote(note.id) },
                                onToggleFavorite = { onToggleFavorite(note.id) },
                                onTogglePin = { onTogglePin(note.id) },
                                onSelectColor = { c -> onUpdateNoteColor(note.id, c) },
                                onOpenMoveFolder = { noteToMoveFolder = note },
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
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                        items(regularNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                isCompactMode = isCompactMode,
                                isTrashSection = isTrashSection,
                                folder = note.folderId?.let { folderMap[it] },
                                tags = (noteTagsMap[note.id] ?: emptyList()).mapNotNull { tagMap[it] },
                                onSelectNote = { onSelectNote(note.id) },
                                onToggleFavorite = { onToggleFavorite(note.id) },
                                onTogglePin = { onTogglePin(note.id) },
                                onSelectColor = { c -> onUpdateNoteColor(note.id, c) },
                                onOpenMoveFolder = { noteToMoveFolder = note },
                                onDuplicateNote = { onDuplicateNote(note) },
                                onArchiveNote = { onArchiveNote(note.id) },
                                onUnarchiveNote = { onUnarchiveNote(note.id) },
                                onTrashNote = { onTrashNote(note.id) },
                                onRestoreNote = { onRestoreNote(note.id) },
                                onPermanentDeleteNote = { onPermanentDeleteNote(note) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button (New Note)
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
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // Move to Folder Dialog
    MoveFolderDialog(
        isOpen = noteToMoveFolder != null,
        currentFolderId = noteToMoveFolder?.folderId,
        folders = folders,
        onSelectFolder = { fId ->
            noteToMoveFolder?.let { n ->
                onMoveNoteToFolder(n.id, fId)
            }
            noteToMoveFolder = null
        },
        onClose = { noteToMoveFolder = null }
    )
}

@Composable
private fun NoteCard(
    note: Note,
    isCompactMode: Boolean,
    isTrashSection: Boolean,
    folder: Folder?,
    tags: List<Tag>,
    onSelectNote: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onSelectColor: (String) -> Unit,
    onOpenMoveFolder: () -> Unit,
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
    var showColorMenu by remember { mutableStateOf(false) }

    val noteBgColor = remember(note.color) {
        runCatching { Color(android.graphics.Color.parseColor(note.color)) }
            .getOrDefault(colors.bgCard)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(noteBgColor)
            .border(0.8.dp, colors.borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelectNote)
            .padding(12.dp)
    ) {
        Column {
            // Header Row: Pin, Star, Title, Options
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
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) colors.accent else colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = colors.textMuted,
                                modifier = Modifier.size(15.dp)
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
                                        Icon(Icons.Default.Palette, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(strings.noteColor, fontSize = 12.sp, color = colors.textPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showColorMenu = true
                                    }
                                )

                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.DriveFileMove, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(strings.moveToFolder, fontSize = 12.sp, color = colors.textPrimary) },
                                    onClick = {
                                        showMenu = false
                                        onOpenMoveFolder()
                                    }
                                )

                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.PushPin, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(if (note.isPinned) strings.unpin else strings.pinToTop, fontSize = 12.sp, color = colors.textPrimary) },
                                    onClick = { onTogglePin(); showMenu = false }
                                )

                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    },
                                    text = { Text(strings.duplicate, fontSize = 12.sp, color = colors.textPrimary) },
                                    onClick = { onDuplicateNote(); showMenu = false }
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

                        // Inline Color Selection Dropdown
                        DropdownMenu(
                            expanded = showColorMenu,
                            onDismissRequest = { showColorMenu = false },
                            modifier = Modifier.background(colors.bgElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NOTE_GRAPHITE_COLORS.forEach { hex ->
                                    val c = Color(android.graphics.Color.parseColor(hex))
                                    val isCurrent = note.color.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(if (isCurrent) 2.dp else 1.dp, if (isCurrent) colors.textPrimary else colors.borderColor, CircleShape)
                                            .clickable {
                                                onSelectColor(hex)
                                                showColorMenu = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrent) {
                                            Icon(Icons.Default.Check, null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Snippet content
            if (!isCompactMode) {
                val snippet = note.content.lines().firstOrNull { it.isNotBlank() } ?: ""
                if (snippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = snippet,
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info: Date, Folder Badge, Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TextFormatUtils.formatDate(note.updatedAt),
                    fontSize = 10.sp,
                    color = colors.textMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (folder != null) {
                        val fColor = runCatching { Color(android.graphics.Color.parseColor(folder.color)) }
                            .getOrDefault(colors.accent)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.bgElevated.copy(alpha = 0.8f))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = fColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = folder.name,
                                fontSize = 9.5.sp,
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    tags.take(1).forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.bgElevated.copy(alpha = 0.8f))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${tag.name}",
                                fontSize = 9.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
