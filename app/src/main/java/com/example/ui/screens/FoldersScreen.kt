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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings

data class FolderTreeItem(
    val folder: Folder,
    val depth: Int,
    val noteCount: Int,
    val hasChildren: Boolean
)

@Composable
fun FoldersScreen(
    folders: List<Folder>,
    tags: List<Tag>,
    notes: List<Note>,
    noteTagsMap: Map<String, List<String>>,
    onSelectFolder: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onNewFolder: (parentId: String?) -> Unit,
    onEditFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    onNewTag: () -> Unit,
    onEditTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    // Expanded state
    val expandedMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            folders.forEach { this[it.id] = true }
        }
    }

    // Build hierarchy
    val folderNoteCounts = remember(folders, notes) {
        val map = mutableMapOf<String, Int>()
        notes.forEach { n ->
            if (!n.inTrash && n.folderId != null) {
                map[n.folderId] = (map[n.folderId] ?: 0) + 1
            }
        }
        map
    }

    // Flatten tree respecting expansion
    val flattenedFolders = remember(folders, expandedMap.toMap(), folderNoteCounts) {
        val roots = folders.filter { it.parentId == null }.sortedBy { it.name }
        val result = mutableListOf<FolderTreeItem>()

        fun addNode(f: Folder, depth: Int) {
            val children = folders.filter { it.parentId == f.id }.sortedBy { it.name }
            val count = folderNoteCounts[f.id] ?: 0
            val hasChildren = children.isNotEmpty()
            result.add(FolderTreeItem(f, depth, count, hasChildren))

            if (hasChildren && (expandedMap[f.id] == true)) {
                children.forEach { child ->
                    addNode(child, depth + 1)
                }
            }
        }

        roots.forEach { addNode(it, 0) }
        result
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgMain)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSidebar)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("folders_menu_button")
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
                        text = strings.foldersAndTags,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = strings.hierarchicalOrg,
                        fontSize = 10.5.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onNewFolder(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.bgElevated,
                        contentColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("folders_new_folder_button")
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.folders, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onNewTag,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.bgElevated,
                        contentColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("folders_new_tag_button")
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.tags, fontSize = 11.sp)
                }
            }
        }

        HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Folders Tree Section
            item {
                Text(
                    text = "${strings.foldersHierarchyUpper} (${folders.size})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = colors.textMuted
                )
            }

            if (folders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.bgCard)
                            .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.noFoldersYet, fontSize = 12.sp, color = colors.textMuted)
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.bgCard)
                            .border(0.8.dp, colors.borderColor, RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        flattenedFolders.forEach { item ->
                            val f = item.folder
                            val isExpanded = expandedMap[f.id] == true
                            val folderColor = runCatching { Color(android.graphics.Color.parseColor(f.color)) }.getOrDefault(colors.accent)
                            var showMenu by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = (item.depth * 18).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSelectFolder(f.id) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (item.hasChildren) {
                                        IconButton(
                                            onClick = { expandedMap[f.id] = !isExpanded },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                                contentDescription = "Expand",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(24.dp))
                                    }

                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = folderColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = f.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.noteCount} ${strings.notesCountSuffix}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMuted
                                    )

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
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.Add, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                                },
                                                text = { Text(strings.newSubfolder, fontSize = 12.sp, color = colors.textPrimary) },
                                                onClick = {
                                                    onNewFolder(f.id)
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.Edit, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                                },
                                                text = { Text(strings.editFolder, fontSize = 12.sp, color = colors.textPrimary) },
                                                onClick = {
                                                    onEditFolder(f)
                                                    showMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.Delete, null, tint = colors.danger, modifier = Modifier.size(16.dp))
                                                },
                                                text = { Text(strings.deleteFolder, fontSize = 12.sp, color = colors.danger) },
                                                onClick = {
                                                    onDeleteFolder(f)
                                                    showMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tags Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${strings.tagsUpper} (${tags.size})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = colors.textMuted
                )
            }

            if (tags.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.bgCard)
                            .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.noTagsYet, fontSize = 12.sp, color = colors.textMuted)
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.bgCard)
                            .border(0.8.dp, colors.borderColor, RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            val count = noteTagsMap.values.count { it.contains(tag.id) }
                            val tagColor = runCatching { Color(android.graphics.Color.parseColor(tag.color)) }.getOrDefault(colors.accent)
                            var showTagMenu by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSelectTag(tag.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(tagColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "#${tag.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$count ${strings.notesCountSuffix}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMuted
                                    )

                                    Box {
                                        IconButton(
                                            onClick = { showTagMenu = true },
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
                                            expanded = showTagMenu,
                                            onDismissRequest = { showTagMenu = false },
                                            modifier = Modifier.background(colors.bgElevated)
                                        ) {
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.Edit, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                                },
                                                text = { Text(strings.editTag, fontSize = 12.sp, color = colors.textPrimary) },
                                                onClick = {
                                                    onEditTag(tag)
                                                    showTagMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.Delete, null, tint = colors.danger, modifier = Modifier.size(16.dp))
                                                },
                                                text = { Text(strings.deleteTag, fontSize = 12.sp, color = colors.danger) },
                                                onClick = {
                                                    onDeleteTag(tag)
                                                    showTagMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
