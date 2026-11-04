package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.Tag
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings

val TAG_COLORS = listOf(
    "#B7B9BE", // Light Graphite
    "#92959B", // Medium Gray
    "#6B6F7A", // Slate
    "#4A4D53", // Charcoal
    "#D4AF37", // Muted Gold
    "#93C5FD", // Ice Blue
    "#A7F3D0", // Sage
    "#FCA5A5"  // Dusty Rose
)

@Composable
fun TagDialog(
    isOpen: Boolean,
    tagToEdit: Tag?,
    onSave: (id: String?, name: String, color: String) -> Unit,
    onClose: () -> Unit
) {
    if (!isOpen) return

    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#B7B9BE") }

    LaunchedEffect(tagToEdit, isOpen) {
        if (tagToEdit != null) {
            name = tagToEdit.name
            color = tagToEdit.color
        } else {
            name = ""
            color = "#B7B9BE"
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                text = if (tagToEdit != null) strings.editTagTitle else strings.createTagTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(strings.tagName, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. priority, project-x", fontSize = 12.sp, color = colors.textMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tag_name_input"),
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

                Column {
                    Text(strings.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.textMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TAG_COLORS.forEach { cHex ->
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(tagToEdit?.id, name.trim(), color)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary, contentColor = colors.bgMain),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_tag_confirm_button")
            ) {
                Text(if (tagToEdit != null) strings.saveTagButton else strings.createTagButton, fontSize = 12.sp)
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
