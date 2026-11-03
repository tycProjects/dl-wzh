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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.ui.theme.GraphiteAmber
import com.example.ui.theme.GraphiteGreen
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils

@Composable
fun NoteEditorScreen(
    note: Note?,
    folders: List<Folder>,
    tags: List<Tag>,
    noteTagIds: List<String>,
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
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

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
    var content by remember(note.id) { mutableStateOf(note.content) }
    var selectedFolderId by remember(note.id, note.folderId) { mutableStateOf(note.folderId) }

    var showFolderMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val currentFolder = folders.find { it.id == selectedFolderId }
    val assignedTags = remember(noteTagIds, tags) {
        noteTagIds.mapNotNull { tId -> tags.find { it.id == tId } }
    }

    // Auto-save debounced when title, content, or folder changes
    LaunchedEffect(title, content, selectedFolderId) {
        if (title != note.title || content != note.content || selectedFolderId != note.folderId) {
            onSaveNote(
                note.copy(
                    title = title,
                    content = content,
                    folderId = selectedFolderId
                )
            )
        }
    }

    val wordCount = remember(content) { TextFormatUtils.getWordCount(content) }
    val charCount = remember(content) { TextFormatUtils.getCharCount(content) }
    val readingTime = remember(wordCount) {
        val minutes = kotlin.math.ceil(wordCount / 200.0).toInt().coerceAtLeast(1)
        "$minutes ${strings.minRead}"
    }

    val fontFamily = remember(settings.editorFontFamily) {
        when (settings.editorFontFamily) {
            "mono" -> FontFamily.Monospace
            "serif" -> FontFamily.Serif
            else -> FontFamily.Default
        }
    }

    val fontSize = remember(settings.fontSize) {
        when (settings.fontSize) {
            "small" -> 14.sp
            "large" -> 18.sp
            else -> 16.sp
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSidebar)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }

                // Folder selector chip
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.bgElevated)
                            .clickable { showFolderMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentFolder?.name ?: strings.noFolder,
                            fontSize = 11.5.sp,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }

                    DropdownMenu(
                        expanded = showFolderMenu,
                        onDismissRequest = { showFolderMenu = false },
                        modifier = Modifier.background(colors.bgElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.noFolder, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = {
                                selectedFolderId = null
                                showFolderMenu = false
                            }
                        )
                        folders.forEach { f ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Folder,
                                        null,
                                        tint = runCatching { Color(android.graphics.Color.parseColor(f.color)) }.getOrDefault(colors.accent),
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                text = { Text(f.name, fontSize = 12.sp, color = colors.textPrimary) },
                                onClick = {
                                    selectedFolderId = f.id
                                    showFolderMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Tags chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.bgElevated)
                        .clickable { showTagDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (assignedTags.isEmpty()) strings.tags else "${strings.tags} (${assignedTags.size})",
                        fontSize = 11.5.sp,
                        color = colors.textPrimary
                    )
                }
            }

            // Right actions: Autosave indicator, Star, Pin, Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Autosave status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (saveStatus == "saving") GraphiteAmber else GraphiteGreen
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (saveStatus == "saving") strings.saving else strings.saved,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(note.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Favorite",
                        tint = if (note.isFavorite) colors.accent else colors.textMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = { onTogglePin(note.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) colors.accent else colors.textMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(colors.bgElevated)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Download, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            },
                            text = { Text(strings.exportMarkdown, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = {
                                val tagNames = assignedTags.map { it.name }
                                val md = TextFormatUtils.exportAsMarkdown(note.copy(title = title, content = content), currentFolder?.name, tagNames)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "${title.ifBlank { "note" }}.md")
                                    putExtra(Intent.EXTRA_TEXT, md)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Markdown"))
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Share, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            },
                            text = { Text(strings.shareNoteText, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = {
                                val tagNames = assignedTags.map { it.name }
                                val txt = TextFormatUtils.exportAsPlainText(note.copy(title = title, content = content), currentFolder?.name, tagNames)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, txt)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            },
                            text = { Text(strings.duplicateNote, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = {
                                onDuplicateNote(note.copy(title = title, content = content))
                                showMoreMenu = false
                            }
                        )
                        HorizontalDivider(color = colors.borderColor, thickness = 0.5.dp)
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Archive, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            },
                            text = { Text(strings.archiveNote, fontSize = 12.sp, color = colors.textPrimary) },
                            onClick = {
                                onArchiveNote(note.id)
                                showMoreMenu = false
                                onBack()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = colors.danger, modifier = Modifier.size(16.dp))
                            },
                            text = { Text(strings.moveToTrash, fontSize = 12.sp, color = colors.danger) },
                            onClick = {
                                onTrashNote(note.id)
                                showMoreMenu = false
                                onBack()
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

        // Formatting Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgCard)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Heading buttons
            ToolbarTextButton("H1", onClick = { content = appendBlock(content, "# ") })
            ToolbarTextButton("H2", onClick = { content = appendBlock(content, "## ") })
            ToolbarTextButton("H3", onClick = { content = appendBlock(content, "### ") })
            ToolbarTextButton("P", onClick = { content = appendBlock(content, "") })

            ToolbarDivider()

            // Inline styles
            ToolbarIconButton(Icons.Default.FormatBold, "Bold", onClick = { content = wrapSelection(content, "**", "**") })
            ToolbarIconButton(Icons.Default.FormatItalic, "Italic", onClick = { content = wrapSelection(content, "*", "*") })
            ToolbarIconButton(Icons.Default.FormatUnderlined, "Underline", onClick = { content = wrapSelection(content, "<u>", "</u>") })
            ToolbarIconButton(Icons.Default.FormatStrikethrough, "Strikethrough", onClick = { content = wrapSelection(content, "~~", "~~") })

            ToolbarDivider()

            // Lists
            ToolbarIconButton(Icons.Default.FormatListBulleted, "Bullet List", onClick = { content = appendBlock(content, "• ") })
            ToolbarIconButton(Icons.Default.FormatListNumbered, "Numbered List", onClick = { content = appendBlock(content, "1. ") })
            ToolbarIconButton(Icons.Default.CheckBox, "Checklist Task", onClick = { content = appendBlock(content, "[ ] ") })

            ToolbarDivider()

            // Blocks & Quotes
            ToolbarIconButton(Icons.Default.Code, "Code Block", onClick = { content = appendBlock(content, "```\n// code snippet\n```") })
            ToolbarIconButton(Icons.Default.FormatQuote, "Quote", onClick = { content = appendBlock(content, "> ") })
            ToolbarIconButton(Icons.Default.HorizontalRule, "Divider", onClick = { content = appendBlock(content, "\n---\n") })
            ToolbarIconButton(Icons.Default.TableChart, "Table Template", onClick = {
                content = appendBlock(
                    content,
                    "| Header 1 | Header 2 | Header 3 |\n| --- | --- | --- |\n| Item 1 | Details | Status |"
                )
            })
        }

        HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

        // Main Editor Area (Title + Content + Interactive Checklist toggles)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Title Input
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontFamily = fontFamily
                ),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = strings.untitledPlaceholder,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            fontFamily = fontFamily
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.borderColor.copy(alpha = 0.4f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Body text editor
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    fontSize = fontSize,
                    color = colors.textPrimary,
                    fontFamily = fontFamily,
                    lineHeight = (fontSize.value * 1.6).sp
                ),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_input"),
                decorationBox = { innerTextField ->
                    if (content.isEmpty()) {
                        Text(
                            text = strings.editorPlaceholder,
                            fontSize = fontSize,
                            color = colors.textMuted,
                            fontFamily = fontFamily,
                            lineHeight = (fontSize.value * 1.6).sp
                        )
                    }
                    innerTextField()
                }
            )

            // Interactive checklist companion (tappable directly!)
            val checklistLines = remember(content) {
                content.lines().mapIndexedNotNull { idx, line ->
                    val trimmed = line.trimStart()
                    if (trimmed.startsWith("[ ]") || trimmed.startsWith("[x]")) {
                        Triple(idx, trimmed.startsWith("[x]"), trimmed.removePrefix("[x]").removePrefix("[ ]").trim())
                    } else null
                }
            }

            if (checklistLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = colors.borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.checklistQuickToggle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                checklistLines.forEachIndexed { itemIdx, (_, isChecked, text) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                content = TextFormatUtils.toggleChecklistLine(content, itemIdx)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (isChecked) colors.accent else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = text.ifBlank { strings.taskItem },
                            fontSize = 13.sp,
                            color = if (isChecked) colors.textMuted else colors.textPrimary,
                            style = if (isChecked) TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else TextStyle()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }

        HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

        // Bottom Stats Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSidebar)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$wordCount ${strings.words}",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textMuted
                )
                Text(
                    text = "$charCount ${strings.chars}",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textMuted
                )
                Text(
                    text = readingTime,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textMuted
                )
            }

            Text(
                text = "${strings.modified}: ${TextFormatUtils.formatDate(note.updatedAt)}",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.textMuted
            )
        }
    }

    // Tag Selection Dialog
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = {
                Text(strings.assignTags, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tags.isEmpty()) {
                        Text(strings.noTagsYet, fontSize = 12.sp, color = colors.textMuted)
                    } else {
                        tags.forEach { tag ->
                            val isChecked = noteTagIds.contains(tag.id)
                            val tagColor = runCatching { Color(android.graphics.Color.parseColor(tag.color)) }.getOrDefault(colors.accent)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newIds = if (isChecked) {
                                            noteTagIds - tag.id
                                        } else {
                                            noteTagIds + tag.id
                                        }
                                        onUpdateNoteTags(note.id, newIds)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        val newIds = if (it) noteTagIds + tag.id else noteTagIds - tag.id
                                        onUpdateNoteTags(note.id, newIds)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.accent,
                                        uncheckedColor = colors.borderColor
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("#${tag.name}", fontSize = 13.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTagDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.bgMain)
                ) {
                    Text(strings.done)
                }
            },
            containerColor = colors.bgElevated
        )
    }
}

private fun appendBlock(content: String, prefix: String): String {
    return if (content.isBlank()) {
        prefix
    } else if (content.endsWith("\n")) {
        content + prefix
    } else {
        content + "\n" + prefix
    }
}

private fun wrapSelection(content: String, startWrap: String, endWrap: String): String {
    return if (content.isBlank()) {
        "$startWrap$endWrap"
    } else {
        content + " $startWrap$endWrap"
    }
}

@Composable
private fun ToolbarTextButton(text: String, onClick: () -> Unit) {
    val colors = LocalTumbaColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun ToolbarIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = LocalTumbaColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colors.textSecondary,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun ToolbarDivider() {
    val colors = LocalTumbaColors.current
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(16.dp)
            .width(0.8.dp)
            .background(colors.borderColor)
    )
}
