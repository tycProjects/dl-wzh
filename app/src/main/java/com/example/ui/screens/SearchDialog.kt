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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Attachment
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.NoteLink
import com.example.data.model.SearchHistoryItem
import com.example.data.model.Tag
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings
import com.example.utils.TextFormatUtils

@Composable
fun SearchDialog(
    isOpen: Boolean,
    notes: List<Note>,
    folders: List<Folder>,
    tags: List<Tag>,
    attachments: List<Attachment> = emptyList(),
    links: List<NoteLink> = emptyList(),
    searchHistory: List<SearchHistoryItem> = emptyList(),
    noteTagsMap: Map<String, List<String>>,
    onSelectNote: (String) -> Unit,
    onRecordQuery: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onClose: () -> Unit
) {
    if (!isOpen) return

    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var query by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var selectedTagId by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var pinnedOnly by remember { mutableStateOf(false) }
    var hasAttachmentsOnly by remember { mutableStateOf(false) }
    var hasLinksOnly by remember { mutableStateOf(false) }
    var includeArchived by remember { mutableStateOf(false) }

    val folderMap = remember(folders) { folders.associateBy { it.id } }
    val tagMap = remember(tags) { tags.associateBy { it.id } }

    val notesWithAttachments = remember(attachments) { attachments.map { it.noteId }.toSet() }
    val notesWithLinks = remember(links) {
        (links.map { it.sourceNoteId } + links.map { it.targetNoteId }).toSet()
    }

    val attachmentNamesByNote = remember(attachments) {
        val map = mutableMapOf<String, MutableList<String>>()
        attachments.forEach { att ->
            map.getOrPut(att.noteId) { mutableListOf() }.add(att.fileName.lowercase())
        }
        map
    }

    val filteredNotes = remember(
        notes, query, selectedFolderId, selectedTagId, favoritesOnly, pinnedOnly,
        hasAttachmentsOnly, hasLinksOnly, includeArchived
    ) {
        val q = query.trim().lowercase()
        notes.filter { note ->
            if (note.inTrash) return@filter false
            if (!includeArchived && note.isArchived) return@filter false
            if (favoritesOnly && !note.isFavorite) return@filter false
            if (pinnedOnly && !note.isPinned) return@filter false
            if (hasAttachmentsOnly && note.id !in notesWithAttachments) return@filter false
            if (hasLinksOnly && note.id !in notesWithLinks) return@filter false
            if (selectedFolderId != null && note.folderId != selectedFolderId) return@filter false
            if (selectedTagId != null && !(noteTagsMap[note.id] ?: emptyList()).contains(selectedTagId)) return@filter false

            if (q.isBlank()) return@filter true

            if (note.title.lowercase().contains(q)) return@filter true
            if (note.content.lowercase().contains(q)) return@filter true

            val folder = note.folderId?.let { folderMap[it] }
            if (folder != null && folder.name.lowercase().contains(q)) return@filter true

            val tagIds = noteTagsMap[note.id] ?: emptyList()
            for (tId in tagIds) {
                val tag = tagMap[tId]
                if (tag != null && tag.name.lowercase().contains(q)) return@filter true
            }

            // Search attachment filenames
            val fileNames = attachmentNamesByNote[note.id]
            if (fileNames != null && fileNames.any { it.contains(q) }) return@filter true

            false
        }.sortedByDescending { it.updatedAt }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
            color = colors.panelBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Input Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.length >= 3) {
                                onRecordQuery(it)
                            }
                        },
                        placeholder = {
                            Text(
                                strings.searchPlaceholder,
                                color = colors.textSecondary.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                HorizontalDivider(color = colors.border)

                // Advanced Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = !favoritesOnly },
                        label = { Text(strings.favorites, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(13.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.accent,
                            containerColor = colors.cardBackground,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = if (favoritesOnly) colors.accent else colors.border, enabled = true, selected = favoritesOnly)
                    )

                    FilterChip(
                        selected = pinnedOnly,
                        onClick = { pinnedOnly = !pinnedOnly },
                        label = { Text(strings.pinned, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(13.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.accent,
                            containerColor = colors.cardBackground,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = if (pinnedOnly) colors.accent else colors.border, enabled = true, selected = pinnedOnly)
                    )

                    FilterChip(
                        selected = hasAttachmentsOnly,
                        onClick = { hasAttachmentsOnly = !hasAttachmentsOnly },
                        label = { Text(strings.filterHasAttachments, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(13.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.accent,
                            containerColor = colors.cardBackground,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = if (hasAttachmentsOnly) colors.accent else colors.border, enabled = true, selected = hasAttachmentsOnly)
                    )

                    FilterChip(
                        selected = hasLinksOnly,
                        onClick = { hasLinksOnly = !hasLinksOnly },
                        label = { Text(strings.filterHasLinks, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(13.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.accent,
                            containerColor = colors.cardBackground,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = if (hasLinksOnly) colors.accent else colors.border, enabled = true, selected = hasLinksOnly)
                    )

                    FilterChip(
                        selected = includeArchived,
                        onClick = { includeArchived = !includeArchived },
                        label = { Text(strings.includeArchived, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(13.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.accent,
                            containerColor = colors.cardBackground,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = if (includeArchived) colors.accent else colors.border, enabled = true, selected = includeArchived)
                    )
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                // Results or Search History
                if (query.isBlank() && searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.searchHistoryTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 0.5.sp
                            )
                        )
                        TextButton(onClick = onClearHistory) {
                            Text(strings.clearHistory, fontSize = 11.sp, color = colors.accent)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(searchHistory, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { query = item.query }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.query, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    HorizontalDivider(color = colors.border)
                }

                // Results Count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filteredNotes.size} ${strings.resultsSuffix}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Results List
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noSearchMatch,
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            SearchResultCard(
                                note = note,
                                query = query,
                                folderName = note.folderId?.let { folderMap[it]?.name },
                                colors = colors,
                                onClick = {
                                    if (query.isNotBlank()) onRecordQuery(query)
                                    onSelectNote(note.id)
                                    onClose()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    note: Note,
    query: String,
    folderName: String?,
    colors: com.example.ui.theme.TumbaColors,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Highlight query in title
                Text(
                    text = highlightSubstring(note.title.ifBlank { "Untitled Note" }, query, colors.accent),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (folderName != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.panelBackground
                    ) {
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = highlightSubstring(note.content.take(160).replace("\n", " "), query, colors.accent),
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TextFormatUtils.formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary.copy(alpha = 0.6f), fontSize = 10.sp)
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

fun highlightSubstring(text: String, query: String, highlightColor: Color): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()

    return buildAnnotatedString {
        var startIdx = 0
        while (startIdx < text.length) {
            val foundIdx = lowerText.indexOf(lowerQuery, startIdx)
            if (foundIdx == -1) {
                append(text.substring(startIdx))
                break
            }
            if (foundIdx > startIdx) {
                append(text.substring(startIdx, foundIdx))
            }
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                append(text.substring(foundIdx, foundIdx + lowerQuery.length))
            }
            startIdx = foundIdx + lowerQuery.length
        }
    }
}
