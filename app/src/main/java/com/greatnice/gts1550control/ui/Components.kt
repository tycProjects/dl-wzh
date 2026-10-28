package com.greatnice.gts1550control.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Neon {
    val Bg = Color(0xFF05070D)
    val Surface = Color(0xFF0B1220)
    val Surface2 = Color(0xFF101A2E)
    val Cyan = Color(0xFF00E5FF)
    val Blue = Color(0xFF2979FF)
    val Text = Color(0xFFE6F1FF)
    val Dim = Color(0xFF7F93B2)
    val Ok = Color(0xFF00E676)
    val Warn = Color(0xFFFFB300)
    val Bad = Color(0xFFFF5252)
}

@Composable
fun GtsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon.Cyan,
            onPrimary = Neon.Bg,
            secondary = Neon.Blue,
            background = Neon.Bg,
            surface = Neon.Surface,
            onSurface = Neon.Text,
            onBackground = Neon.Text
        ),
        content = content
    )
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    accent: Color = Neon.Cyan,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Neon.Surface)
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun Label(text: String, color: Color = Neon.Cyan) =
    Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

@Composable
fun Body(text: String, color: Color = Neon.Text, size: Int = 14, modifier: Modifier = Modifier) =
    Text(text, color = color, fontSize = size.sp, lineHeight = (size + 6).sp, modifier = modifier)

@Composable
fun Chip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Neon.Cyan,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) accent else Neon.Dim.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accent,
            disabledContentColor = Neon.Dim
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}
