package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyboardSettings

@Composable
fun TextEditPad(
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    fontFamily: FontFamily,
    onAction: (KeyAction) -> Unit,
    onBackToQwerty: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Top row: Back to ABC & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                text = "Precision Cursor & Text Pad",
                color = palette.textColor,
                fontSize = 13.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center: D-Pad Navigation & Quick Selection Actions
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick action buttons: Select All, Copy, Cut, Paste
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextActionBtn(
                        label = "Select All",
                        icon = Icons.Filled.SelectAll,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(KeyAction.SelectAll) }
                    )
                    TextActionBtn(
                        label = "Copy",
                        icon = Icons.Filled.ContentCopy,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(KeyAction.CopyText) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextActionBtn(
                        label = "Cut",
                        icon = Icons.Filled.ContentCut,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(KeyAction.CutText) }
                    )
                    TextActionBtn(
                        label = "Paste",
                        icon = Icons.Filled.ContentPaste,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(KeyAction.PasteText) }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // D-Pad Navigation Cross
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surfaceColor)
                    .border(1.dp, palette.borderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Up arrow
                DPadArrow(
                    icon = Icons.Filled.KeyboardArrowUp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
                    palette = palette,
                    onClick = { onAction(KeyAction.CursorUp) }
                )
                // Down arrow
                DPadArrow(
                    icon = Icons.Filled.KeyboardArrowDown,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                    palette = palette,
                    onClick = { onAction(KeyAction.CursorDown) }
                )
                // Left arrow
                DPadArrow(
                    icon = Icons.Filled.KeyboardArrowLeft,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
                    palette = palette,
                    onClick = { onAction(KeyAction.CursorLeft) }
                )
                // Right arrow
                DPadArrow(
                    icon = Icons.Filled.KeyboardArrowRight,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                    palette = palette,
                    onClick = { onAction(KeyAction.CursorRight) }
                )
                // Center dot / indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.accentColor.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun TextActionBtn(
    label: String,
    icon: ImageVector,
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(palette.keyColor)
            .border(1.dp, palette.borderColor.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = palette.accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = palette.textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DPadArrow(
    icon: ImageVector,
    palette: KeyboardPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(palette.keyColor)
            .border(1.dp, palette.borderColor.copy(alpha = 0.3f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
