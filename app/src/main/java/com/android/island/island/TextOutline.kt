package com.android.island.island

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/** Island text: black fill with a white outline, readable on any photo or app behind it. */
val OutlineTextFill: Color = Color.Black
val OutlineTextStroke: Color = Color.White

/**
 * Two stacked text layers: a white stroke drawn first, then the black fill on top, so only the
 * outer edge of each glyph shows white. Text-only, no offscreen layer, so the cost stays small.
 */
@Composable
fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = OutlineTextFill,
    outlineColor: Color = OutlineTextStroke,
    outlineWidth: Dp = 2.4f.dp,
    style: TextStyle = TextStyle.Default,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    outlineEnabled: Boolean = true
) {
    if (!outlineEnabled) {
        Text(
            text = text, modifier = modifier, color = color, style = style, fontSize = fontSize,
            fontWeight = fontWeight, maxLines = maxLines, overflow = overflow, onTextLayout = onTextLayout
        )
        return
    }
    val density = LocalDensity.current
    val strokePx = with(density) { outlineWidth.toPx() }
    val outlineStyle = style.merge(
        TextStyle(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round))
    )
    Box(modifier) {
        Text(
            text = text,
            color = outlineColor,
            style = outlineStyle,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow
        )
        Text(
            text = text,
            color = color,
            style = style,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = onTextLayout
        )
    }
}
