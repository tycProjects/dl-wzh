package dev.tajim.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.tajim.jarvis.ui.theme.LocalJarvisColors

/** Static gradient + faint circuit grid + two corner glows. Drawn once: no per-frame cost. */
@Composable
fun JarvisBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = LocalJarvisColors.current
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.backgroundTop, c.backgroundBottom)))
            .drawBehind {
                val step = 44.dp.toPx()
                var x = 0f
                while (x < size.width) { drawLine(c.gridLine, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                var y = 0f
                while (y < size.height) { drawLine(c.gridLine, Offset(0f, y), Offset(size.width, y), 1f); y += step }
                val r = size.maxDimension * 0.55f
                drawCircle(
                    Brush.radialGradient(listOf(c.violet.copy(alpha = if (c.isDark) 0.22f else 0.14f), Color.Transparent), Offset(size.width, 0f), r),
                    r, Offset(size.width, 0f),
                )
                drawCircle(
                    Brush.radialGradient(listOf(c.cyan.copy(alpha = if (c.isDark) 0.18f else 0.14f), Color.Transparent), Offset(0f, size.height), r),
                    r, Offset(0f, size.height),
                )
            },
        content = content,
    )
}

