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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.TumbaWysiwygEditor
import com.example.ui.components.rememberTumbaWysiwygState
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils
import java.io.File

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
    var noteColor by remember(note.id, note.color) { mutableStateOf(note.color) }
    var selectedFolderId by remember(note.id, note.folderId) { mutableStateOf(note.folderId) }

    // Pure WYSIWYG editor state holder
    val wysiwygState = rememberTumbaWysiwygState()

    // Dialog & popover states
    var showColorPicker by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRevisionsDialog by remember { mutableStateOf(false) }
    var showAttachmentsSheet by remember { mutableStateOf(false) }

    val primaryTextColorInt = remember(colors.textPrimary) {
        val c = colors.textPrimary
        android.graphics.Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }
    val accentColorInt = remember(colors.accent) {
        val c = colors.accent
        android.graphics.Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }
    val mutedColorInt = remember(colors.textMuted) {
        val c = colors.textMuted
        android.graphics.Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }
    val codeBgColorInt = android.graphics.Color.parseColor("#26282C")

    fun currentNoteState(): Note {
        val currentContent = wysiwygState.getContent().ifBlank { note.content }
        return note.copy(
            title = title,
            content = currentContent,
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

    val currentFolder = folders.find { it.id == selectedFolderId }
    val assignedTags = remember(noteTagIds, tags) {
        noteTagIds.mapNotNull { tId -> tags.find { it.id == tId } }
    }

    val noteSurfaceColor = remember(noteColor) {
        runCatching { Color(android.graphics.Color.parseColor(noteColor)) }
            .getOrDefault(colors.bgCard)
    }

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
                // Back Button (guarantees immediate save & exit)
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
                                    val currentText = wysiwygState.getContent()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "$title\n\n$currentText")
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

            // Title Input Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { newTitle ->
                        title = newTitle
                        onSaveNote(note.copy(title = newTitle, content = wysiwygState.getContent(), folderId = selectedFolderId, color = noteColor))
                    },
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = strings.untitledPlaceholder,
                                style = TextStyle(
                                    color = colors.textMuted.copy(alpha = 0.6f),
                                    fontSize = 22.sp,
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
            }

            HorizontalDivider(color = colors.borderColor.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Pure WYSIWYG Editor Body: Directly shows bold, headers, checkboxes, lists & quotes!
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val fontSizeSp = when (settings.fontSize) {
                    "small" -> 14.5f
                    "large" -> 18.5f
                    else -> 16f
                }

                TumbaWysiwygEditor(
                    state = wysiwygState,
                    initialContent = note.content,
                    fontSize = fontSizeSp,
                    fontFamilyStyle = settings.editorFontFamily,
                    placeholderText = strings.editorPlaceholder,
                    onContentChanged = { updatedContent ->
                        onSaveNote(note.copy(title = title, content = updatedContent, folderId = selectedFolderId, color = noteColor))
                    },
                    onLinkTapped = { targetTitle ->
                        onOpenLinkedNote(targetTitle)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Compact WYSIWYG Formatting Toolbar Docked at Bottom
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
                IconButton(onClick = { wysiwygState.undo() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { wysiwygState.redo() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                Box(modifier = Modifier.width(1.dp).height(18.dp).background(colors.borderColor))

                // Bold (Instant visual bold)
                IconButton(onClick = { wysiwygState.toggleBold() }, modifier = Modifier.size(34.dp).testTag("toolbar_bold_button")) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                // Italic (Instant visual italic)
                IconButton(onClick = { wysiwygState.toggleItalic() }, modifier = Modifier.size(34.dp).testTag("toolbar_italic_button")) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                // Strikethrough
                IconButton(onClick = { wysiwygState.toggleStrikethrough() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strike", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                Box(modifier = Modifier.width(1.dp).height(18.dp).background(colors.borderColor))

                // Heading 1 (Instant visual large heading)
                EditorToolbarButton(label = "H1") { wysiwygState.applyHeading(1) }

                // Heading 2 (Instant visual medium heading)
                EditorToolbarButton(label = "H2") { wysiwygState.applyHeading(2) }

                Box(modifier = Modifier.width(1.dp).height(18.dp).background(colors.borderColor))

                // Interactive Checklist [ ] / ☐
                IconButton(onClick = { wysiwygState.toggleChecklist(mutedColorInt) }, modifier = Modifier.size(34.dp).testTag("toolbar_checklist_button")) {
                    Icon(Icons.Default.CheckBox, contentDescription = "Checklist", tint = colors.accent, modifier = Modifier.size(17.dp))
                }

                // Bullet list •
                IconButton(onClick = { wysiwygState.insertBulletList(accentColorInt) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bullet List", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                // Numbered list 1.
                IconButton(onClick = { wysiwygState.insertNumberedList() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                // Blockquote (Instant visual quote border)
                IconButton(onClick = { wysiwygState.insertQuote(accentColorInt) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                // Inline / block Code
                IconButton(onClick = { wysiwygState.insertCodeBlock(codeBgColorInt) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Code, contentDescription = "Code", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                // Divider line
                IconButton(onClick = { wysiwygState.insertDivider() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Divider", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                }

                // Attachment
                IconButton(onClick = onAddAttachment, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.AttachFile, contentDescription = strings.addAttachment, tint = colors.accent, modifier = Modifier.size(17.dp))
                }

                // Link / Internal Note Link
                IconButton(onClick = { showLinkDialog = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Link, contentDescription = strings.internalLink, tint = colors.accent, modifier = Modifier.size(17.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Autosave status
                Text(
                    text = if (saveStatus == "saving") strings.saving else strings.saved,
                    fontSize = 10.sp,
                    color = if (saveStatus == "saving") colors.accent else colors.textMuted
                )
            }

            // Sub-footer: words, chars, modified
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
                    text = "${TextFormatUtils.getWordCount(note.content)} ${strings.words}   ${TextFormatUtils.getCharCount(note.content)} ${strings.chars}   ${TextFormatUtils.getReadingTime(TextFormatUtils.getWordCount(note.content))}",
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

    // Link Dialog (Allows linking to existing note or URL)
    if (showLinkDialog) {
        var linkUrl by remember { mutableStateOf("") }
        var linkText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(strings.internalLink, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.linkToNotePrompt, fontSize = 11.5.sp, color = colors.textSecondary)

                    // Quick list of other notes to link to
                    if (allNotes.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().height(140.dp).verticalScroll(rememberScrollState())) {
                            allNotes.filter { it.id != note.id }.take(8).forEach { otherNote ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val noteTitle = otherNote.title.ifBlank { "Untitled Note" }
                                            wysiwygState.insertLink(noteTitle, "note://$noteTitle", accentColorInt)
                                            showLinkDialog = false
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(otherNote.title.ifBlank { "Untitled" }, color = colors.textPrimary, fontSize = 12.5.sp)
                                }
                            }
                        }
                        HorizontalDivider(color = colors.borderColor)
                    }

                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        placeholder = { Text("Link Text (optional)", color = colors.textMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.bgElevated,
                            unfocusedContainerColor = colors.bgElevated,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderColor,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        placeholder = { Text("https://example.com", color = colors.textMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.bgElevated,
                            unfocusedContainerColor = colors.bgElevated,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderColor,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUrl = linkUrl.trim()
                        val finalTitle = linkText.trim().ifBlank { finalUrl }
                        if (finalUrl.isNotBlank()) {
                            wysiwygState.insertLink(finalTitle, finalUrl, accentColorInt)
                        }
                        showLinkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.bgMain)
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text(strings.cancel, color = colors.textSecondary)
                }
            },
            containerColor = colors.bgElevated
        )
    }

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
