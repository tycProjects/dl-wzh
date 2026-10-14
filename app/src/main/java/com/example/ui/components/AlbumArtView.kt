package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.ui.theme.MatrixGreenBright
import com.example.ui.theme.MatrixGreenDark
import com.example.ui.theme.MatrixGreenPrimary
import com.example.ui.theme.MatrixOutline
import com.example.ui.theme.MatrixSurfaceElevated
import com.example.ui.theme.MatrixSurfaceVariant

@Composable
fun AlbumArtView(
    song: Song?,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    isPlaying: Boolean = false,
    isCircularVinyl: Boolean = false
) {
    val cornerRadius = if (isCircularVinyl) size / 2 else 18.dp
    val shape = if (isCircularVinyl) CircleShape else RoundedCornerShape(cornerRadius)

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .then(if (isCircularVinyl && isPlaying) Modifier.rotate(rotation) else Modifier)
            .clip(shape)
            .background(MatrixSurfaceVariant)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(MatrixGreenPrimary.copy(alpha = 0.8f), MatrixOutline)
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!song?.albumArtUri.isNullOrBlank()) {
            AsyncImage(
                model = song?.albumArtUri,
                contentDescription = "Album art for ${song?.title ?: "song"}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Matrix Procedural Cyber Vinyl Art
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
                val maxRadius = this.size.width / 2f

                // Concentric digital vinyl grooves
                val grooveSteps = 5
                for (i in 1..grooveSteps) {
                    val r = maxRadius * (i.toFloat() / (grooveSteps + 1))
                    drawCircle(
                        color = MatrixOutline.copy(alpha = 0.4f),
                        radius = r,
                        center = centerOffset,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Center glowing spindle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(MatrixGreenBright.copy(alpha = 0.5f), Color.Transparent),
                        center = centerOffset,
                        radius = maxRadius * 0.4f
                    ),
                    radius = maxRadius * 0.4f,
                    center = centerOffset
                )
            }

            // Inner core label
            Box(
                modifier = Modifier
                    .size(size * 0.42f)
                    .clip(CircleShape)
                    .background(MatrixSurfaceElevated)
                    .border(1.dp, MatrixGreenPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initial = song?.title?.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
                Text(
                    text = initial,
                    color = MatrixGreenPrimary,
                    fontSize = (size.value * 0.18f).sp,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
