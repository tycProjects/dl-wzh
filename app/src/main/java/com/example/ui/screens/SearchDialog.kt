package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Folder
import com.example.data.model.Note
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
    noteTagsMap: Map<String, List<String>>,
    onSelectNote: (String) -> Unit,
    onClose: () -> Unit
) {
    if (!isOpen) return

    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var query by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var selectedTagId by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var includeArchived by remember { mutableStateOf(false) }

    val folderMap = remember(folders) { folders.associateBy { it.id } }
    val tagMap = remember(tags) { tags.associateBy { it.id } }

    val filteredNotes = remember(
        notes, query, selectedFolderId, selectedTagId, favoritesOnly, includeArchived
    ) {
        val q = query.trim().lowercase()
        notes.filter { note ->
            if (note.inTrash) return@filter false
            if (!includeArchived && note.isArchived) return@filter false
            if (favoritesOnly && !note.isFavorite) return@filter false
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

            false
        }.sortedByDescending { it.updatedAt }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, colors.borderColor, RoundedCornerShape(14.dp)),
            color = colors.bgCard
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
                        onValueChange = { query = it },
                        placeholder = { Text(strings.searchPlaceholder, fontSize = 13.sp, color = colors.textMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("omni_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        singleLine = true
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

                // Filters Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgSidebar)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.FilterList, null, tint = colors.textMuted, modifier = Modifier.size(14.dp))

                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = !favoritesOnly },
                        label = { Text(strings.favorites, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.bgElevated,
                            selectedLabelColor = colors.textPrimary,
                            containerColor = colors.bgMain,
                            labelColor = colors.textSecondary
                        )
                    )

                    FilterChip(
                        selected = includeArchived,
                        onClick = { includeArchived = !includeArchived },
                        label = { Text(strings.includeArchived, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Archive, null, modifier = Modifier.size(12.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.bgElevated,
                            selectedLabelColor = colors.textPrimary,
                            containerColor = colors.bgMain,
                            labelColor = colors.textSecondary
                        )
                    )

                    folders.forEach { f ->
                        FilterChip(
                            selected = selectedFolderId == f.id,
                            onClick = { selectedFolderId = if (selectedFolderId == f.id) null else f.id },
                            label = { Text(f.name, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(12.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.bgElevated,
                                selectedLabelColor = colors.textPrimary,
                                containerColor = colors.bgMain,
                                labelColor = colors.textSecondary
                            )
                        )
                    }

                    tags.forEach { t ->
                        FilterChip(
                            selected = selectedTagId == t.id,
                            onClick = { selectedTagId = if (selectedTagId == t.id) null else t.id },
                            label = { Text("#${t.name}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.bgElevated,
                                selectedLabelColor = colors.textPrimary,
                                containerColor = colors.bgMain,
                                labelColor = colors.textSecondary
                            )
                        )
                    }
                }

                HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

                // Results count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredNotes.size} ${strings.resultsSuffix}",
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted
                    )
                }

                // Results list
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noSearchMatch,
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            val folder = note.folderId?.let { folderMap[it] }
                            val noteTagIds = noteTagsMap[note.id] ?: emptyList()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.bgElevated)
                                    .clickable {
                                        onSelectNote(note.id)
                                        onClose()
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (note.isPinned) {
                                                Icon(Icons.Default.PushPin, null, tint = colors.accent, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            if (note.isFavorite) {
                                                Icon(Icons.Default.Star, null, tint = colors.accent, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = note.title.ifBlank { strings.untitledNote },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        val snippet = note.content.lines().firstOrNull { it.isNotBlank() } ?: ""
                                        if (snippet.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = snippet,
                                                fontSize = 11.sp,
                                                color = colors.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = TextFormatUtils.formatDate(note.updatedAt),
                                                fontSize = 9.5.sp,
                                                color = colors.textMuted
                                            )
                                            if (folder != null) {
                                                Text(
                                                    text = folder.name,
                                                    fontSize = 9.5.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                            noteTagIds.take(2).forEach { tId ->
                                                val tag = tagMap[tId]
                                                if (tag != null) {
                                                    Text(
                                                        text = "#${tag.name}",
                                                        fontSize = 9.5.sp,
                                                        color = colors.textMuted
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}
