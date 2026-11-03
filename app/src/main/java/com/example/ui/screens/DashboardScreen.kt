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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
fun DashboardScreen(
    notes: List<Note>,
    folders: List<Folder>,
    tags: List<Tag>,
    onSelectNote: (String) -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectSection: (ViewSection) -> Unit,
    onNewNote: () -> Unit,
    onNewFolder: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    // Metrics
    val activeNotes = notes.filter { !it.inTrash && !it.isArchived }
    val totalNotes = activeNotes.size
    val totalFolders = folders.size
    val totalTags = tags.size
    val favoritesCount = activeNotes.count { it.isFavorite }
    val archiveCount = notes.count { it.isArchived && !it.inTrash }
    val trashCount = notes.count { it.inTrash }

    // Recently edited active notes (top 5)
    val recentlyEdited = remember(notes) {
        notes.filter { !it.inTrash && !it.isArchived }
            .sortedByDescending { it.updatedAt }
            .take(5)
    }

    // Pinned & Favorites (top 4)
    val pinnedOrStarred = remember(notes) {
        notes.filter { !it.inTrash && !it.isArchived && (it.isPinned || it.isFavorite) }
            .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
            .take(4)
    }

    // Top folders
    val topFolders = remember(folders, notes) {
        folders.sortedByDescending { f -> notes.count { !it.inTrash && it.folderId == f.id } }
            .take(6)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgMain)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("dashboard_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation",
                            tint = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TUMBA NOTES",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.testTag("dashboard_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textSecondary
                    )
                }
            }
        }

        // Welcome Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bgCard)
                    .border(0.8.dp, colors.borderColor, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = strings.personalKnowledgeBase,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.minimalistGraphiteNotes,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.offlineDescription,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNewNote,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("dashboard_create_note_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.textPrimary,
                                contentColor = colors.bgMain
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.createNote,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onNewFolder,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("dashboard_create_folder_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.textPrimary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.borderColor)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = colors.accent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.newFolder,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Metrics Grid (6 tiles in 2 rows of 3)
        item {
            Text(
                text = strings.databaseOverview,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        title = strings.notes.uppercase(),
                        value = "$totalNotes",
                        sub = strings.metricActive,
                        icon = Icons.Default.Description,
                        iconTint = colors.accent,
                        onClick = { onSelectSection(ViewSection.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = strings.folders.uppercase(),
                        value = "$totalFolders",
                        sub = strings.metricNested,
                        icon = Icons.Default.Folder,
                        iconTint = colors.accent,
                        onClick = { onSelectSection(ViewSection.FOLDERS) },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = strings.tags.uppercase(),
                        value = "$totalTags",
                        sub = strings.metricLabels,
                        icon = Icons.Default.Label,
                        iconTint = colors.accent,
                        onClick = { onSelectSection(ViewSection.FOLDERS) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        title = strings.favorites.uppercase(),
                        value = "$favoritesCount",
                        sub = strings.metricStarred,
                        icon = Icons.Default.Star,
                        iconTint = colors.accent,
                        onClick = { onSelectSection(ViewSection.FAVORITES) },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = strings.archive.uppercase(),
                        value = "$archiveCount",
                        sub = strings.metricPreserved,
                        icon = Icons.Default.Archive,
                        iconTint = colors.textMuted,
                        onClick = { onSelectSection(ViewSection.ARCHIVE) },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = strings.trash.uppercase(),
                        value = "$trashCount",
                        sub = strings.metricInBin,
                        icon = Icons.Default.Delete,
                        iconTint = colors.danger,
                        onClick = { onSelectSection(ViewSection.TRASH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Access Folders
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.quickAccessFolders,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = strings.viewAll,
                    fontSize = 11.sp,
                    color = colors.accent,
                    modifier = Modifier.clickable { onSelectSection(ViewSection.FOLDERS) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (topFolders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgCard)
                        .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noFoldersYet,
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunked = topFolders.chunked(3)
                    chunked.forEach { rowFolders ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowFolders.forEach { folder ->
                                val count = notes.count { !it.inTrash && it.folderId == folder.id }
                                val folderColor = runCatching { Color(android.graphics.Color.parseColor(folder.color)) }.getOrDefault(colors.accent)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.bgCard)
                                        .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
                                        .clickable { onSelectFolder(folder.id) }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = folderColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "$count",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = colors.textMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = folder.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            // Fill empty slots if less than 3
                            repeat(3 - rowFolders.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Recently Edited Notes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.recentlyEdited,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = strings.viewAll,
                    fontSize = 11.sp,
                    color = colors.accent,
                    modifier = Modifier.clickable { onSelectSection(ViewSection.RECENT) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (recentlyEdited.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgCard)
                        .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noNotesYet,
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentlyEdited.forEach { note ->
                        DashboardNoteItem(
                            note = note,
                            untitledFallback = strings.untitledNote,
                            onClick = { onSelectNote(note.id) }
                        )
                    }
                }
            }
        }

        // Pinned & Favorites Notes
        if (pinnedOrStarred.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.pinnedAndFavorites,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = strings.viewAll,
                        fontSize = 11.sp,
                        color = colors.accent,
                        modifier = Modifier.clickable { onSelectSection(ViewSection.FAVORITES) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pinnedOrStarred.forEach { note ->
                        DashboardNoteItem(
                            note = note,
                            untitledFallback = strings.untitledNote,
                            onClick = { onSelectNote(note.id) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgCard)
            .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = colors.textPrimary
            )
            Text(
                text = sub,
                fontSize = 9.sp,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun DashboardNoteItem(
    note: Note,
    untitledFallback: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgCard)
            .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
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
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (note.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = colors.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = note.title.ifBlank { untitledFallback },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = TextFormatUtils.formatDate(note.updatedAt),
                    fontSize = 10.sp,
                    color = colors.textMuted
                )
            }

            val snippet = note.content.lines().firstOrNull { it.isNotBlank() } ?: ""
            if (snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = snippet,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
