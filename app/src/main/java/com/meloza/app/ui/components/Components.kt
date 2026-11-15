package com.meloza.app.ui.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.meloza.app.R
import com.meloza.app.data.Album
import com.meloza.app.data.Track
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/** Album art with a placeholder when the file has no embedded picture. */
@Composable
fun Artwork(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
) {
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (model == null) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp))
        }
    }
}

/** Large rounded album card for the home screen carousels. */
@Composable
fun AlbumCard(album: Album, onClick: () -> Unit, width: Dp = 168.dp) {
    val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        Modifier.width(width).pressScale(source).clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
        ),
    ) {
        Artwork(album.artworkUri, Modifier.size(width), RoundedCornerShape(32.dp))
        Spacer(Modifier.height(10.dp))
        Text(album.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            album.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Row with artwork, title, artist and favorite toggle. */
@Composable
fun TrackRow(
    track: Track,
    isFavorite: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().pressScale(source).clickable(
            interactionSource = source, indication = null, onClick = onClick,
        ).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(track.artworkUri, Modifier.size(56.dp), RoundedCornerShape(16.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${track.artist} · ${formatDuration(track.durationMs)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(R.string.action_favorite),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Bars driven by [levels] (0..1 per bar) from the audio visualizer. Paused: the bars
 * flatten and nothing animates. Without levels, a gentle idle wave runs while playing.
 */
@Composable
fun SoundWave(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 28,
    levels: FloatArray? = null,
) {
    // The phase only advances while playing, so the wave is still when paused.
    val phase by produceState(0f, playing) {
        if (!playing) return@produceState
        var start = -1L
        while (true) {
            withFrameNanos { time ->
                if (start < 0) start = time
                value = ((time - start) / 1_400_000_000f) % 1f
            }
        }
    }
    val amp by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "amp",
    )
    Canvas(modifier) {
        val count = levels?.size ?: bars
        val gap = size.width / count
        val barWidth = gap * 0.55f
        for (i in 0 until count) {
            val level = if (levels != null) levels[i].coerceIn(0f, 1f)
                else (kotlin.math.sin(i / count.toFloat() * 6.28f + phase * 6.28f) + 1f) / 2f
            val h = size.height * (0.08f + 0.92f * level * amp)
            val x = i * gap + gap / 2
            drawLineBar(x, size.height / 2 - h / 2, x, size.height / 2 + h / 2, barWidth, color)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineBar(
    x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Color,
) {
    drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = width, cap = StrokeCap.Round)
}

/** Wavy progress line. Tap or drag anywhere on it to seek. */
@Composable
fun WavySeekBar(progress: Float, color: Color, modifier: Modifier = Modifier, onSeek: (Float) -> Unit) {
    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val amp = h * 0.28f
        val path = Path()
        path.moveTo(0f, h / 2)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, h / 2 + kotlin.math.sin(x / 14f) * amp)
            x += 4f
        }
        val stroke = Stroke(width = 6f, cap = StrokeCap.Round)
        drawPath(path, color.copy(alpha = 0.25f), style = stroke)
        clipRect(0f, 0f, w * progress, h) { drawPath(path, color, style = stroke) }
        val tx = w * progress
        drawCircle(color, radius = 9f, center = Offset(tx, h / 2 + kotlin.math.sin(tx / 14f) * amp))
    }
}

/** Decorative wave for the logo-like seekbar (wavy progress line). */
@Composable
fun WavyProgress(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path()
        var x = 0f
        path.moveTo(0f, h / 2)
        while (x <= w * progress.coerceIn(0f, 1f)) {
            val y = h / 2 + kotlin.math.sin(x / 10f) * (h / 3)
            path.lineTo(x, y)
            x += 4f
        }
        drawPath(path, color, style = Stroke(width = 6f, cap = StrokeCap.Round))
    }
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
fun FavoriteToggle(isFavorite: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "fav",
    )
    IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(R.string.action_favorite),
            modifier = Modifier.size(28.dp).then(Modifier),
            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
