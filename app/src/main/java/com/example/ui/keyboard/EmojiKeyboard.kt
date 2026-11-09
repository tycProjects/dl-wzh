package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

enum class EmojiCategory(val label: String, val icon: ImageVector, val emojis: List<String>) {
    SMILEYS(
        "Smileys", Icons.Filled.SentimentSatisfied,
        listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥹", "😊",
            "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙",
            "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
            "🫡", "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬",
            "😮‍💨", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕"
        )
    ),
    GESTURES(
        "Gestures", Icons.Filled.PanTool,
        listOf(
            "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌",
            "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
            "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜",
            "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳",
            "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🫀", "🫁"
        )
    ),
    HEARTS(
        "Hearts", Icons.Filled.Favorite,
        listOf(
            "❤️", "🩷", "🧡", "💛", "💚", "💙", "🩵", "💜", "🖤", "🩶",
            "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗",
            "💖", "💘", "💝", "💟", "🔥", "✨", "⭐", "🌟", "💫", "⚡",
            "💥", "💯", "💢", "🎉", "🎊", "🔮", "🪄", "👑", "💎", "💐"
        )
    ),
    NATURE(
        "Nature", Icons.Filled.Park,
        listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨",
            "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤",
            "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🪱",
            "🐛", "🦋", "🐌", "🐞", "🐜", "🪰", "🪲", "🪳", "🪴", "🌸"
        )
    ),
    FOOD(
        "Food", Icons.Filled.Fastfood,
        listOf(
            "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐",
            "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🥑", "🍕",
            "🍔", "🍟", "🌭", "🍿", "🧈", "🥞", "🧇", "🧀", "🍖", "🍗",
            "🌮", "🌯", "🍣", "🍜", "🍩", "🍪", "🎂", "🍰", "🍫", "☕"
        )
    ),
    OBJECTS(
        "Objects", Icons.Filled.Category,
        listOf(
            "💻", "📱", "⌨️", "🎧", "🎮", "🕹️", "📷", "💡", "🔦", "🔋",
            "🔌", "🚀", "✈️", "🚗", "🏎️", "🚲", "🧭", "⚓", "🪐", "🌍",
            "⚽", "🏀", "🏈", "🎾", "🏆", "🥇", "🎯", "🎸", "🎵", "🎨"
        )
    )
}

@Composable
fun EmojiKeyboard(
    palette: KeyboardPalette,
    settings: KeyboardSettings,
    fontFamily: FontFamily,
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackToQwerty: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiCategory.values().forEach { cat ->
                val isSelected = cat == selectedCategory
                IconButton(
                    onClick = { selectedCategory = cat },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) palette.accentColor.copy(alpha = 0.25f) else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = cat.label,
                        tint = if (isSelected) palette.accentColor else palette.specialTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 40.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            items(selectedCategory.emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 26.sp
                    )
                }
            }
        }

        // Bottom navigation bar for Emoji mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Return to ABC button
            Button(
                onClick = onBackToQwerty,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.specialKeyColor,
                    contentColor = palette.specialTextColor
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "ABC",
                    fontSize = 14.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = selectedCategory.label,
                color = palette.textColor.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontFamily = fontFamily
            )

            // Backspace button
            IconButton(
                onClick = onBackspace,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.specialKeyColor)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = palette.textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
