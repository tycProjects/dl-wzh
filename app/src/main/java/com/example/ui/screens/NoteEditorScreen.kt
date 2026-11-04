package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppSettings
import com.example.data.model.Attachment
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.data.model.NoteRevision
import com.example.data.model.Tag
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils
import java.io.File
import kotlin.math.max
import kotlin.math.min

@Composable
fun NoteEditorScreen(
    note: Note?,
    folders: List<Folder>,
    tags: List<Tag>,
    allNotes: List<Note> = emptyList(),
    noteTagIds: List<String>,
    attachments: List<Attachment> = emptyList(),
    revisions: List<NoteRevision> = emptyList(),
    incomingLinks: List<NoteLink> = emptyList(),
    outgoingLinks: List<NoteLink> = emptyList(),
    settings: AppSettings,
    saveStatus: String,
    onBack: () -> Unit,
    onSaveNote: (Note) -> Unit,
    onUpdateNoteTags: (String, List<String>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDuplicateNote: (Note) -> Unit,
    onArchiveNote: (String) -> Unit,
    onTrashNote: (String) -> Unit,
    onRestoreRevision: (NoteRevision) -> Unit = {},
    onAddAttachment: () -> Unit = {},
    onDeleteAttachment: (Attachment) -> Unit = {},
    onOpenLinkedNote: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    if (note == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.bgMain),
            contentAlignment = Alignment.Center
        ) {
            Text(strings.noNoteSelected, color = colors.textSecondary)
        }
        return
    }

    var title by remember(note.id) { mutableStateOf(note.title) }
    var contentValue by remember(note.id) { mutableStateOf(TextFieldValue(note.content)) }
    var noteColor by remember(note.id, note.color) { mutableStateOf(note.color) }
    var selectedFolderId by remember(note.id, note.folderId) { mutableStateOf(note.folderId) }

    // Undo / Redo history
    val undoStack = remember(note.id) { mutableListOf<TextFieldValue>() }
    val redoStack = remember(note.id) { mutableListOf<TextFieldValue>() }

    // Dialog & popover states
    var isReadingMode by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRevisionsDialog by remember { mutableStateOf(false) }
    var showAttachmentsSheet by remember { mutableStateOf(false) }
    var showLinkAutocomplete by remember { mutableStateOf(false) }

    fun currentNoteState(): Note {
        return note.copy(
            title = title,
            content = contentValue.text,
            folderId = selectedFolderId,
            color = noteColor
        )
    }

    // Flush save immediately before exiting
    fun saveAndExit() {
        onSaveNote(currentNoteState())
        onBack()
    }

    BackHandler {
        saveAndExit()
    }

    // Also flush on dispose
    DisposableEffect(note.id) {
        onDispose {
            onSaveNote(currentNoteState())
        }
    }

    fun notifyContentChange(newVal: TextFieldValue, addToUndo: Boolean = true) {
        if (addToUndo && newVal.text != contentValue.text) {
            undoStack.add(contentValue)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }
        contentValue = newVal

        // Link autocomplete detect
        val textBefore = newVal.text.take(newVal.selection.start)
        val currentLine = textBefore.substringAfterLast('\n', textBefore)
        showLinkAutocomplete = currentLine.endsWith("[[") && !currentLine.endsWith("]]")

        onSaveNote(note.copy(title = title, content = newVal.text, folderId = selectedFolderId, color = noteColor))
    }

    fun applyUndo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(contentValue)
            contentValue = prev
            onSaveNote(note.copy(title = title, content = prev.text, folderId = selectedFolderId, color = noteColor))
        }
    }

    fun applyRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(contentValue)
            contentValue = next
            onSaveNote(note.copy(title = title, content = next.text, folderId = selectedFolderId, color = noteColor))
        }
    }

    fun insertTextAtCursor(insert: String, cursorOffset: Int = insert.length) {
        val currentText = contentValue.text
        val selStart = min(contentValue.selection.start, contentValue.selection.end)
        val selEnd = max(contentValue.selection.start, contentValue.selection.end)
        val newText = currentText.substring(0, selStart) + insert + currentText.substring(selEnd)
        val newCursor = selStart + cursorOffset
        notifyContentChange(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun wrapSelection(prefix: String, suffix: String = prefix) {
        val currentText = contentValue.text
        val selStart = min(contentValue.selection.start, contentValue.selection.end)
        val selEnd = max(contentValue.selection.start, contentValue.selection.end)
        if (selStart == selEnd) {
            insertTextAtCursor(prefix + suffix, prefix.length)
        } else {
            val selected = currentText.substring(selStart, selEnd)
            val replacement = prefix + selected + suffix
            val newText = currentText.substring(0, selStart) + replacement + currentText.substring(selEnd)
            notifyContentChange(TextFieldValue(newText, TextRange(selStart + replacement.length)))
        }
    }

    fun insertAtLineStart(prefix: String) {
        val currentText = contentValue.text
        val selStart = contentValue.selection.start
        val lineStart = currentText.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val newText = currentText.substring(0, lineStart) + prefix + currentText.substring(lineStart)
        notifyContentChange(TextFieldValue(newText, TextRange(selStart + prefix.length)))
    }

    val currentFolder = folders.find { it.id == selectedFolderId }
    val assignedTags = remember(noteTagIds, tags) {
        noteTagIds.mapNotNull { tId -> tags.find { it.id == tId } }
    }

    val noteSurfaceColor = remember(noteColor) {
        runCatching { Color(android.graphics.Color.parseColor(noteColor)) }
            .getOrDefault(colors.bgCard)
    }

    val wordCount = remember(contentValue.text) { TextFormatUtils.getWordCount(contentValue.text) }
    val charCount = remember(contentValue.text) { TextFormatUtils.getCharCount(contentValue.text) }
    val readingTime = remember(wordCount) { TextFormatUtils.getReadingTime(wordCount) }

    val selectionColors = TextSelectionColors(
        handleColor = colors.accent,
        backgroundColor = colors.accent.copy(alpha = 0.35f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(noteSurfaceColor)
                .imePadding()
        ) {
            // Minimalist Google Keep Style Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button (saves immediately)
                IconButton(
                    onClick = { saveAndExit() },
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Pin Button
                    IconButton(
                        onClick = { onTogglePin(note.id) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Color Picker Button
                    Box {
                        IconButton(
                            onClick = { showColorPicker = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("editor_color_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = strings.noteColor,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Color selection dropdown
                        DropdownMenu(
                            expanded = showColorPicker,
                            onDismissRequest = { showColorPicker = false },
                            modifier = Modifier.background(colors.bgElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NOTE_GRAPHITE_COLORS.forEach { hex ->
                                    val c = Color(android.graphics.Color.parseColor(hex))
                                    val isSelected = noteColor.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) colors.textPrimary else colors.borderColor,
                                                CircleShape
                                            )
                                            .clickable {
                                                noteColor = hex
                                                onSaveNote(note.copy(color = hex))
                                                showColorPicker = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Favorite Star
                    IconButton(
                        onClick = { onToggleFavorite(note.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Reading / Edit Mode
                    IconButton(
                        onClick = { isReadingMode = !isReadingMode },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isReadingMode) Icons.Default.Description else Icons.Default.MenuBook,
                            contentDescription = if (isReadingMode) strings.editMode else strings.readingMode,
                            tint = if (isReadingMode) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(colors.bgElevated)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.History, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                },
                                text = { Text(strings.historyRevisions, fontSize = 12.sp, color = colors.textPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    showRevisionsDialog = true
                                }
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.AttachFile, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                },
                                text = { Text("${strings.attachments} (${attachments.size})", fontSize = 12.sp, color = colors.textPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    showAttachmentsSheet = true
                                }
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.Share, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                                },
                                text = { Text(strings.shareNoteText, fontSize = 12.sp, color = colors.textPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "${title}\n\n${contentValue.text}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                                }
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                },
                                text = { Text(strings.duplicateNote, fontSize = 12.sp, color = colors.textPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    onDuplicateNote(currentNoteState())
                                }
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.Archive, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                },
                                text = { Text(strings.archiveNote, fontSize = 12.sp, color = colors.textPrimary) },
                                onClick = {
                                    showMoreMenu = false
                                    onArchiveNote(note.id)
                                }
                            )

                            HorizontalDivider(color = colors.borderColor)

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = colors.danger, modifier = Modifier.size(16.dp))
                                },
                                text = { Text(strings.moveToTrash, fontSize = 12.sp, color = colors.danger) },
                                onClick = {
                                    showMoreMenu = false
                                    onTrashNote(note.id)
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.borderColor.copy(alpha = 0.4f), thickness = 0.5.dp)

            // Sub-header Badges: Folder selector & Tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Folder Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.bgElevated.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.borderColor),
                    modifier = Modifier.clickable { showFolderMenu = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentFolder?.name ?: strings.noFolder,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Tags chips
                assignedTags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.bgElevated.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.borderColor)
                    ) {
                        Text(
                            text = "#${tag.name}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Add / Edit Tag Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.bgElevated.copy(alpha = 0.8f),
                    modifier = Modifier.clickable { showTagDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = strings.assignTags,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accent,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Image Attachments Carousel at Top of Note (if any images attached)
            val imageAttachments = remember(attachments) {
                attachments.filter { it.mimeType.startsWith("image/") }
            }
            if (imageAttachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(imageAttachments, key = { it.id }) { att ->
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 75.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
                                .clickable { showAttachmentsSheet = true }
                        ) {
                            AsyncImage(
                                model = File(att.localPath),
                                contentDescription = att.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Main Editor Writing Area (Expands to fill screen, smoothly scrollable!)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isReadingMode) {
                    // Rendered Reading View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = title.ifBlank { strings.untitledPlaceholder },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = colors.borderColor.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        RenderedMarkdownContent(
                            content = contentValue.text,
                            colors = colors,
                            onToggleChecklist = { lineIdx ->
                                val toggled = TextFormatUtils.toggleChecklistLine(contentValue.text, lineIdx)
                                notifyContentChange(TextFieldValue(toggled, TextRange(toggled.length)))
                            },
                            onLinkClick = { targetTitle ->
                                onOpenLinkedNote(targetTitle)
                            }
                        )

                        if (outgoingLinks.isNotEmpty() || incomingLinks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = colors.borderColor)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.internalLinks.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val allLinkedIds = (outgoingLinks.map { it.targetNoteId } + incomingLinks.map { it.sourceNoteId }).distinct()
                            allLinkedIds.forEach { linkedId ->
                                val targetNote = allNotes.find { it.id == linkedId }
                                if (targetNote != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.bgElevated,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { onOpenLinkedNote(targetNote.title) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(targetNote.title, color = colors.textPrimary, fontSize = 12.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Active Editing Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        // Title Input
                        BasicTextField(
                            value = title,
                            onValueChange = { newTitle ->
                                title = newTitle
                                onSaveNote(note.copy(title = newTitle, content = contentValue.text, folderId = selectedFolderId, color = noteColor))
                            },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        text = strings.untitledPlaceholder,
                                        style = TextStyle(
                                            color = colors.textMuted.copy(alpha = 0.6f),
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_title_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Content Body
                        val bodyFontSize = when (settings.fontSize) {
                            "small" -> 14.sp
                            "large" -> 18.sp
                            else -> 16.sp
                        }
                        val bodyFontFamily = when (settings.editorFontFamily) {
                            "serif" -> FontFamily.Serif
                            "mono" -> FontFamily.Monospace
                            else -> FontFamily.SansSerif
                        }

                        BasicTextField(
                            value = contentValue,
                            onValueChange = { newVal ->
                                notifyContentChange(newVal)
                            },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = bodyFontSize,
                                fontFamily = bodyFontFamily,
                                lineHeight = (bodyFontSize.value * 1.5).sp
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            decorationBox = { innerTextField ->
                                if (contentValue.text.isEmpty()) {
                                    Text(
                                        text = strings.editorPlaceholder,
                                        style = TextStyle(
                                            color = colors.textMuted.copy(alpha = 0.5f),
                                            fontSize = bodyFontSize,
                                            lineHeight = (bodyFontSize.value * 1.5).sp
                                        )
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .testTag("editor_content_input")
                        )

                        Spacer(modifier = Modifier.height(120.dp))
                    }

                    // Autocomplete Link Popup
                    if (showLinkAutocomplete) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 54.dp)
                        ) {
                            LinkAutocompletePalette(
                                notes = allNotes.filter { it.id != note.id },
                                colors = colors,
                                onSelect = { noteTitle ->
                                    showLinkAutocomplete = false
                                    insertTextAtCursor("$noteTitle]]")
                                }
                            )
                        }
                    }
                }
            }

            // Compact Google Keep Style Bottom Formatting Bar
            if (!isReadingMode) {
                HorizontalDivider(color = colors.borderColor.copy(alpha = 0.6f), thickness = 0.8.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgSidebar)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo & Redo
                    IconButton(onClick = { applyUndo() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = { applyRedo() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }

                    Box(modifier = Modifier.width(1.dp).height(18.dp).background(colors.borderColor))

                    // Checklist [ ]
                    IconButton(onClick = { insertAtLineStart("[ ] ") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.CheckBox, contentDescription = "Checklist", tint = colors.accent, modifier = Modifier.size(17.dp))
                    }

                    // Bullet list •
                    IconButton(onClick = { insertAtLineStart("• ") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }

                    // Numbered list 1.
                    IconButton(onClick = { insertAtLineStart("1. ") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }

                    // Headings
                    EditorToolbarButton(label = "H1") { insertAtLineStart("# ") }
                    EditorToolbarButton(label = "H2") { insertAtLineStart("## ") }

                    // Bold
                    IconButton(onClick = { wrapSelection("**") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }

                    // Italic
                    IconButton(onClick = { wrapSelection("*") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }

                    // Quote
                    IconButton(onClick = { insertAtLineStart("> ") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }

                    // Code
                    IconButton(onClick = { wrapSelection("`") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Code, contentDescription = "Code", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }

                    // Divider
                    IconButton(onClick = { insertTextAtCursor("\n---\n") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.HorizontalRule, contentDescription = "Divider", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                    }

                    // Add image / attachment
                    IconButton(onClick = onAddAttachment, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.AttachFile, contentDescription = strings.addAttachment, tint = colors.accent, modifier = Modifier.size(17.dp))
                    }

                    // Internal Link [[
                    IconButton(onClick = { insertTextAtCursor("[[") }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Link, contentDescription = strings.internalLink, tint = colors.accent, modifier = Modifier.size(17.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Autosave Indicator
                    Text(
                        text = if (saveStatus == "saving") strings.saving else strings.saved,
                        fontSize = 10.sp,
                        color = if (saveStatus == "saving") colors.accent else colors.textMuted
                    )
                }

                // Sub-footer stats: words, chars, modified
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgSidebar)
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount ${strings.words}   $charCount ${strings.chars}   $readingTime",
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = "${strings.modified} ${TextFormatUtils.formatDate(note.updatedAt)}",
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }
            }
        }
    }

    // Revisions Dialog
    RevisionHistoryDialog(
        isOpen = showRevisionsDialog,
        note = note,
        revisions = revisions,
        onDismiss = { showRevisionsDialog = false },
        onRestoreRevision = onRestoreRevision
    )

    // Attachments Sheet
    AttachmentsSheet(
        isOpen = showAttachmentsSheet,
        attachments = attachments,
        onDismiss = { showAttachmentsSheet = false },
        onAddAttachment = onAddAttachment,
        onDeleteAttachment = onDeleteAttachment
    )

    // Folder Picker Dialog
    if (showFolderMenu) {
        AlertDialog(
            onDismissRequest = { showFolderMenu = false },
            title = { Text(strings.folders, color = colors.textPrimary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.noFolder,
                        color = colors.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFolderId = null
                                onSaveNote(note.copy(folderId = null))
                                showFolderMenu = false
                            }
                            .padding(vertical = 10.dp)
                    )
                    folders.forEach { f ->
                        Text(
                            text = f.name,
                            color = colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFolderId = f.id
                                    onSaveNote(note.copy(folderId = f.id))
                                    showFolderMenu = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFolderMenu = false }) {
                    Text(strings.cancel, color = colors.textSecondary)
                }
            },
            containerColor = colors.bgElevated
        )
    }

    // Tag assignment dialog
    if (showTagDialog) {
        var currentTags by remember { mutableStateOf(noteTagIds.toSet()) }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text(strings.assignTags, color = colors.textPrimary) },
            text = {
                if (tags.isEmpty()) {
                    Text(strings.noTagsYet, color = colors.textSecondary)
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        tags.forEach { t ->
                            val isChecked = currentTags.contains(t.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentTags = if (isChecked) currentTags - t.id else currentTags + t.id
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { chk ->
                                        currentTags = if (chk) currentTags + t.id else currentTags - t.id
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.accent,
                                        uncheckedColor = colors.textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("#${t.name}", color = colors.textPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNoteTags(note.id, currentTags.toList())
                        showTagDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.bgMain)
                ) {
                    Text(strings.done)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text(strings.cancel, color = colors.textSecondary)
                }
            },
            containerColor = colors.bgElevated
        )
    }
}

@Composable
fun EditorToolbarButton(
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalTumbaColors.current
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = colors.bgElevated,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun LinkAutocompletePalette(
    notes: List<Note>,
    colors: com.example.ui.theme.TumbaColors,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(colors.accent.copy(alpha = 0.5f))
        )
    ) {
        Column(modifier = Modifier.padding(6.dp).width(220.dp)) {
            Text(
                text = "LINK TO NOTE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
            notes.take(6).forEach { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(note.title) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textPrimary),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RenderedMarkdownContent(
    content: String,
    colors: com.example.ui.theme.TumbaColors,
    onToggleChecklist: (Int) -> Unit,
    onLinkClick: (String) -> Unit
) {
    val lines = content.lines()
    var checklistCounter = 0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# "),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }
                trimmed.startsWith("[ ] ") || trimmed.startsWith("[x] ") -> {
                    val isChecked = trimmed.startsWith("[x] ")
                    val text = if (isChecked) trimmed.removePrefix("[x] ") else trimmed.removePrefix("[ ] ")
                    val currentIdx = checklistCounter++
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleChecklist(currentIdx) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (isChecked) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isChecked) colors.textSecondary else colors.textPrimary,
                                textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        )
                    }
                }
                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.bgElevated, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(20.dp).background(colors.accent))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trimmed.removePrefix("> "),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = colors.textSecondary
                            )
                        )
                    }
                }
                trimmed.startsWith("```") -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.bgElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = colors.accent
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                trimmed == "---" -> {
                    HorizontalDivider(color = colors.borderColor, modifier = Modifier.padding(vertical = 6.dp))
                }
                else -> {
                    RenderParagraphWithLinks(text = line, colors = colors, onLinkClick = onLinkClick)
                }
            }
        }
    }
}

@Composable
fun RenderParagraphWithLinks(
    text: String,
    colors: com.example.ui.theme.TumbaColors,
    onLinkClick: (String) -> Unit
) {
    if (text.isBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        return
    }

    val parts = mutableListOf<Pair<String, Boolean>>()
    val regex = Regex("\\[\\[([^\\]]+)\\]\\]")
    var lastIndex = 0

    regex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            parts.add(Pair(text.substring(lastIndex, match.range.first), false))
        }
        parts.add(Pair(match.groupValues[1], true))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        parts.add(Pair(text.substring(lastIndex), false))
    }

    if (parts.any { it.second }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            parts.forEach { (part, isLink) ->
                if (isLink) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.accent.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clickable { onLinkClick(part) }
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = part,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.accent,
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Text(
                        text = part,
                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary)
                    )
                }
            }
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.textPrimary,
                lineHeight = 22.sp
            )
        )
    }
}
