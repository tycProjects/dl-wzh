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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings

val FOLDER_COLORS = listOf(
    "#B7B9BE", // Light Graphite
    "#8E9096", // Medium Graphite
    "#63666E", // Deep Slate
    "#4A4D53", // Charcoal
    "#D4AF37", // Muted Gold
    "#93C5FD", // Ice Blue
    "#A7F3D0", // Sage
    "#FCA5A5"  // Dusty Rose
)

val FOLDER_ICON_MAP = mapOf(
    "folder" to Icons.Default.Folder,
    "book-open" to Icons.Default.Book,
    "code" to Icons.Default.Code,
    "lightbulb" to Icons.Default.Lightbulb,
    "briefcase" to Icons.Default.Work,
    "layers" to Icons.Default.Layers,
    "bookmark" to Icons.Default.Bookmark,
    "archive" to Icons.Default.Archive,
    "terminal" to Icons.Default.Terminal,
    "globe" to Icons.Default.Public,
    "file-text" to Icons.Default.Description,
    "heart" to Icons.Default.Favorite
)

@Composable
fun FolderDialog(
    isOpen: Boolean,
    folderToEdit: Folder?,
    parentFolderId: String?,
    existingFolders: List<Folder>,
    onSave: (id: String?, name: String, parentId: String?, color: String, icon: String, description: String) -> Unit,
    onClose: () -> Unit
) {
    if (!isOpen) return

    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var name by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var color by remember { mutableStateOf("#8E9096") }
    var icon by remember { mutableStateOf("folder") }
    var description by remember { mutableStateOf("") }
    var showParentDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(folderToEdit, parentFolderId, isOpen) {
        if (folderToEdit != null) {
            name = folderToEdit.name
            selectedParentId = folderToEdit.parentId
            color = folderToEdit.color
            icon = folderToEdit.icon
            description = folderToEdit.description
        } else {
            name = ""
            selectedParentId = parentFolderId
            color = "#8E9096"
            icon = "folder"
            description = ""
        }
    }

    val parentCandidates = remember(existingFolders, folderToEdit) {
        existingFolders.filter { f ->
            if (folderToEdit == null) true
            else f.id != folderToEdit.id && f.parentId != folderToEdit.id
        }
    }

    val parentFolder = parentCandidates.find { it.id == selectedParentId }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = if (folderToEdit != null) strings.editFolderTitle else strings.createFolderTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Input
                Column {
                    Text(strings.folderName, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Daily Journal, Web Dev", fontSize = 12.sp, color = colors.textMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_name_input"),
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
                }

                // Parent Folder Selector
                Column {
                    Text(strings.parentFolderOptional, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.bgElevated)
                                .border(0.8.dp, colors.borderColor, RoundedCornerShape(8.dp))
                                .clickable { showParentDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = parentFolder?.name ?: strings.rootFolderOption,
                                fontSize = 12.sp,
                                color = colors.textPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showParentDropdown,
                            onDismissRequest = { showParentDropdown = false },
                            modifier = Modifier.background(colors.bgElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.rootFolderOption, fontSize = 12.sp, color = colors.textPrimary) },
                                onClick = {
                                    selectedParentId = null
                                    showParentDropdown = false
                                }
                            )
                            parentCandidates.forEach { f ->
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Folder, null, tint = colors.accent, modifier = Modifier.size(16.dp)) },
                                    text = { Text(f.name, fontSize = 12.sp, color = colors.textPrimary) },
                                    onClick = {
                                        selectedParentId = f.id
                                        showParentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Description Input
                Column {
                    Text(strings.descriptionOptional, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Short note on folder purpose", fontSize = 12.sp, color = colors.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
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
                }

                // Color accent picker
                Column {
                    Text(strings.colorAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FOLDER_COLORS.forEach { cHex ->
                            val c = runCatching { Color(android.graphics.Color.parseColor(cHex)) }.getOrDefault(colors.accent)
                            val isSelected = color.equals(cHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (isSelected) 2.dp else 0.dp, if (isSelected) colors.textPrimary else Color.Transparent, CircleShape)
                                    .clickable { color = cHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = colors.bgMain, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Icon picker
                Column {
                    Text(strings.icon, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FOLDER_ICON_MAP.forEach { (key, iconVec) ->
                            val isSelected = icon == key
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) colors.bgElevated else colors.bgMain)
                                    .border(if (isSelected) 1.dp else 0.8.dp, if (isSelected) colors.accent else colors.borderColor, RoundedCornerShape(6.dp))
                                    .clickable { icon = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconVec, null, tint = if (isSelected) colors.textPrimary else colors.textMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(folderToEdit?.id, name.trim(), selectedParentId, color, icon, description.trim())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.bgMain),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_folder_confirm_button")
            ) {
                Text(if (folderToEdit != null) strings.updateFolderButton else strings.createFolderButton, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(strings.cancel, color = colors.textSecondary, fontSize = 12.sp)
            }
        },
        containerColor = colors.bgCard
    )
}
