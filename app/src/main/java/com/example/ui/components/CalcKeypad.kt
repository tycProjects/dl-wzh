package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ThemePalette

enum class KeyType {
    NUMBER,
    OPERATOR,
    ACTION,      // AC, C, ⌫
    EQUALS,      // =
    TAX,         // TAX+, TAX-
    SCIENTIFIC,  // sin, cos, ln, etc.
    MEMORY       // MC, MR, M+, M-
}

@Composable
fun CalcButton(
    text: String,
    type: KeyType,
    modifier: Modifier = Modifier,
    palette: ThemePalette,
    isDark: Boolean,
    testTag: String = "btn_$text",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseBgColor = when (type) {
        KeyType.NUMBER -> if (isDark) palette.darkNumberKey else palette.lightNumberKey
        KeyType.OPERATOR -> if (isDark) palette.darkOperatorKey else palette.lightOperatorKey
        KeyType.ACTION -> if (isDark) Color(0xFF2B1D24) else Color(0xFFFFE4E6)
        KeyType.EQUALS -> palette.primary
        KeyType.TAX -> if (isDark) Color(0xFF1E281F) else Color(0xFFDCFCE7)
        KeyType.SCIENTIFIC -> if (isDark) palette.darkSurfaceVariant else palette.lightSurfaceVariant
        KeyType.MEMORY -> if (isDark) Color(0xFF161C26) else Color(0xFFE2E8F0)
    }

    val textColor = when (type) {
        KeyType.EQUALS -> if (isDark) Color.Black else Color.White
        KeyType.ACTION -> if (isDark) Color(0xFFFF6B81) else Color(0xFFE11D48)
        KeyType.OPERATOR -> palette.primary
        KeyType.TAX -> if (isDark) palette.accent else Color(0xFF15803D)
        KeyType.SCIENTIFIC -> palette.secondary
        KeyType.MEMORY -> MaterialTheme.colorScheme.onSurfaceVariant
        KeyType.NUMBER -> MaterialTheme.colorScheme.onSurface
    }

    val animatedBg by animateColorAsState(
        targetValue = if (isPressed) {
            when (type) {
                KeyType.EQUALS -> palette.secondary
                else -> baseBgColor.copy(alpha = 0.7f)
            }
        } else {
            baseBgColor
        },
        label = "button_press_bg"
    )

    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = when {
                type == KeyType.EQUALS -> 26.sp
                type == KeyType.NUMBER -> 22.sp
                type == KeyType.OPERATOR -> 24.sp
                type == KeyType.TAX || type == KeyType.MEMORY -> 13.sp
                type == KeyType.SCIENTIFIC -> 14.sp
                text.length > 3 -> 14.sp
                else -> 18.sp
            },
            fontWeight = when (type) {
                KeyType.EQUALS, KeyType.OPERATOR, KeyType.TAX -> FontWeight.Bold
                KeyType.NUMBER -> FontWeight.SemiBold
                else -> FontWeight.Medium
            },
            textAlign = TextAlign.Center
        )
    }
}
