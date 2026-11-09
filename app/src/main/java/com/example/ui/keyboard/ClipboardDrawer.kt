package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipboardItem
import com.example.model.KeyboardSettings

@Composable
fun ClipboardDrawer(
    clips: List<ClipboardItem>,
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    fontFamily: FontFamily,
    onClipSelected: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onClearUnpinned: () -> Unit,
    onBackToQwerty: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredClips = remember(clips, searchQuery) {
        if (searchQuery.isBlank()) {
            clips
        } else {
            clips.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Header toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBackToQwerty,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.specialKeyColor,
                    contentColor = palette.specialTextColor
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ABC",
                    fontSize = 13.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Clipboard Manager",
                color = palette.textColor,
                fontSize = 14.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold
            )

            // Clear unpinned
            TextButton(
                onClick = onClearUnpinned,
                colors = ButtonDefaults.textButtonColors(contentColor = palette.accentColor),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Clear", fontSize = 12.sp, fontFamily = fontFamily)
            }
        }

        if (filteredClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = null,
                        tint = palette.textColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Clipboard is empty",
                        color = palette.textColor.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontFamily = fontFamily
                    )
                    Text(
                        text = "Copy text anywhere to see it here",
                        color = palette.textColor.copy(alpha = 0.35f),
                        fontSize = 11.sp,
                        fontFamily = fontFamily
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredClips, key = { it.id }) { item ->
                    val shape = RoundedCornerShape(10.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(
                                if (item.isPinned) palette.accentColor.copy(alpha = 0.20f) else palette.keyColor
                            )
                            .border(
                                width = 1.dp,
                                color = if (item.isPinned) palette.accentColor.copy(alpha = 0.6f) else palette.borderColor.copy(alpha = 0.3f),
                                shape = shape
                            )
                            .clickable { onClipSelected(item.text) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.text,
                            color = palette.textColor,
                            fontSize = 13.sp,
                            fontFamily = fontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Pin button
                        IconButton(
                            onClick = { onTogglePin(item.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (item.isPinned) "Unpin" else "Pin",
                                tint = if (item.isPinned) palette.accentColor else palette.textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete button
                        IconButton(
                            onClick = { onDeleteClip(item.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Delete",
                                tint = palette.textColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
