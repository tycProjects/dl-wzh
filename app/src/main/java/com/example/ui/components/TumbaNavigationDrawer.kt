package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.data.model.ViewSection
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings

@Composable
fun TumbaNavigationDrawer(
    currentSection: ViewSection,
    selectedFolderId: String?,
    selectedTagId: String?,
    notes: List<Note>,
    folders: List<Folder>,
    tags: List<Tag>,
    noteTagsMap: Map<String, List<String>>,
    onSelectSection: (ViewSection) -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onNewNote: () -> Unit,
    onNewFolder: () -> Unit,
    onNewTag: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    // Calculate section counts
    val activeNotes = notes.filter { !it.inTrash && !it.isArchived }
    val allCount = activeNotes.size
    val favoritesCount = activeNotes.count { it.isFavorite }
    val oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val recentCount = activeNotes.count { it.updatedAt >= oneWeekAgo }
    val archiveCount = notes.count { it.isArchived && !it.inTrash }
    val trashCount = notes.count { it.inTrash }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(colors.bgSidebar)
            .statusBarsPadding()
    ) {
        // App Branding Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    onSelectSection(ViewSection.ALL)
                    onCloseDrawer()
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "TUMBA NOTES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Graphite Local-First",
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

        // Action Buttons: New Note & Quick Search
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onNewNote()
                    onCloseDrawer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("drawer_new_note_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.textPrimary,
                    contentColor = colors.bgMain
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.newNote,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.newNote,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.bgElevated)
                    .clickable {
                        onCloseDrawer()
                        onOpenSearch()
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.quickSearch,
                    color = colors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

        // Scrollable sections
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            // Main views
            DrawerNavItem(
                label = strings.allNotes,
                icon = Icons.Default.Description,
                count = allCount,
                isSelected = currentSection == ViewSection.ALL && selectedFolderId == null && selectedTagId == null,
                onClick = {
                    onSelectSection(ViewSection.ALL)
                    onCloseDrawer()
                }
            )
            DrawerNavItem(
                label = strings.folders,
                icon = Icons.Default.Folder,
                count = folders.size,
                isSelected = currentSection == ViewSection.FOLDERS && selectedFolderId == null && selectedTagId == null,
                onClick = {
                    onSelectSection(ViewSection.FOLDERS)
                    onCloseDrawer()
                }
            )
            DrawerNavItem(
                label = strings.favorites,
                icon = Icons.Default.Star,
                count = favoritesCount,
                isSelected = currentSection == ViewSection.FAVORITES && selectedFolderId == null && selectedTagId == null,
                iconTint = colors.accent,
                onClick = {
                    onSelectSection(ViewSection.FAVORITES)
                    onCloseDrawer()
                }
            )
            DrawerNavItem(
                label = strings.recentNotes,
                icon = Icons.Default.History,
                count = recentCount,
                isSelected = currentSection == ViewSection.RECENT && selectedFolderId == null && selectedTagId == null,
                onClick = {
                    onSelectSection(ViewSection.RECENT)
                    onCloseDrawer()
                }
            )
            DrawerNavItem(
                label = strings.archive,
                icon = Icons.Default.Archive,
                count = archiveCount,
                isSelected = currentSection == ViewSection.ARCHIVE && selectedFolderId == null && selectedTagId == null,
                onClick = {
                    onSelectSection(ViewSection.ARCHIVE)
                    onCloseDrawer()
                }
            )
            DrawerNavItem(
                label = strings.trash,
                icon = Icons.Default.Delete,
                count = trashCount,
                isSelected = currentSection == ViewSection.TRASH && selectedFolderId == null && selectedTagId == null,
                iconTint = colors.danger,
                onClick = {
                    onSelectSection(ViewSection.TRASH)
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                label = strings.calendar,
                icon = Icons.Default.Today,
                count = null,
                isSelected = currentSection == ViewSection.CALENDAR && selectedFolderId == null && selectedTagId == null,
                onClick = {
                    onSelectSection(ViewSection.CALENDAR)
                    onCloseDrawer()
                }
            )
            DrawerNavItem(
                label = strings.graph,
                icon = Icons.Default.Share,
                count = null,
                isSelected = currentSection == ViewSection.GRAPH && selectedFolderId == null && selectedTagId == null,
                onClick = {
                    onSelectSection(ViewSection.GRAPH)
                    onCloseDrawer()
                }
            )
            DrawerNavItem(
                label = strings.templates,
                icon = Icons.Default.Description,
                count = null,
                isSelected = currentSection == ViewSection.TEMPLATES && selectedFolderId == null && selectedTagId == null,
                onClick = {
                    onSelectSection(ViewSection.TEMPLATES)
                    onCloseDrawer()
                }
            )
            DrawerNavItem(
                label = strings.analytics,
                icon = Icons.Default.BarChart,
                count = null,
                isSelected = currentSection == ViewSection.ANALYTICS && selectedFolderId == null && selectedTagId == null,
                onClick = {
                    onSelectSection(ViewSection.ANALYTICS)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Folders header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.folders.uppercase()} (${folders.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )
                IconButton(
                    onClick = onNewFolder,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = strings.newFolder,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Folders list
            folders.take(8).forEach { folder ->
                val count = notes.count { !it.inTrash && it.folderId == folder.id }
                DrawerNavItem(
                    label = folder.name,
                    icon = Icons.Default.Folder,
                    count = count,
                    isSelected = selectedFolderId == folder.id,
                    iconTint = runCatching { Color(android.graphics.Color.parseColor(folder.color)) }.getOrDefault(colors.textSecondary),
                    onClick = {
                        onSelectFolder(folder.id)
                        onCloseDrawer()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Tags header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.tags.uppercase()} (${tags.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )
                IconButton(
                    onClick = onNewTag,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.newTag,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Tags list
            tags.take(6).forEach { tag ->
                val tagColor = runCatching { Color(android.graphics.Color.parseColor(tag.color)) }.getOrDefault(colors.accent)
                val count = noteTagsMap.values.count { it.contains(tag.id) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedTagId == tag.id) colors.bgElevated else Color.Transparent)
                        .clickable {
                            onSelectTag(tag.id)
                            onCloseDrawer()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(tagColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "#${tag.name}",
                            fontSize = 12.sp,
                            color = if (selectedTagId == tag.id) colors.textPrimary else colors.textSecondary
                        )
                    }
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted
                    )
                }
            }
        }

        HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

        // Footer Settings Item
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onCloseDrawer()
                    onOpenSettings()
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = strings.settings,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.settingsAndData,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            Text(
                text = "v1.0",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null
) {
    val colors = LocalTumbaColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) colors.bgElevated else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint ?: if (isSelected) colors.textPrimary else colors.textMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) colors.textPrimary else colors.textSecondary
            )
        }
        if (count != null) {
            Text(
                text = "$count",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.textMuted
            )
        }
    }
}
