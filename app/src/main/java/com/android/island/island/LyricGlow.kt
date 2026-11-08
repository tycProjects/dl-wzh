package com.android.island.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.offset
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

/**
 * The lyric line that is playing right now plus the time window it occupies.
 * [timed] is false when there are no synced lyrics (the line is just the song title).
 */
data class LyricSlot(val text: String, val startMs: Long, val endMs: Long, val timed: Boolean)

/**
 * 0..1 "reading head" across the active lyric line.
 *
 * The player only reports its position ~4x per second, so the head glides linearly toward where
 * it will be at the next report instead of jumping. A seek (large mismatch) snaps instead of
 * gliding. Paused playback freezes the head. Nothing here recomposes: consumers read
 * [Animatable.value] in the draw phase.
 */
@Composable
fun rememberReadingHead(
    slot: LyricSlot,
    position: () -> Long,
    playing: Boolean,
    enabled: Boolean
): Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val head = remember { Animatable(0f) }
    LaunchedEffect(slot.startMs, slot.endMs, slot.timed, playing, enabled) {
        if (!enabled || !slot.timed) { head.snapTo(0f); return@LaunchedEffect }
        // Singing rarely fills the whole gap to the next line; finish a little early so the
        // glow reaches the end of the text before the line is replaced.
        val span = ((slot.endMs - slot.startMs).coerceAtLeast(1400L) * 0.86f).coerceAtLeast(1f)
        fun at(pos: Long) = ((pos - slot.startMs) / span).coerceIn(0f, 1f)
        head.snapTo(at(position()))
        if (!playing) return@LaunchedEffect
        snapshotFlow { position() }.collectLatest { pos ->
            if (abs(head.value - at(pos)) > 0.10f) head.snapTo(at(pos))
            head.animateTo(at(pos + 260L), tween(260, easing = LinearEasing))
        }
    }
    return head
}

/**
 * Soft halo for the lyric that is being read.
 *
 * The halo is a blurred copy of the glyphs (two radii for a bloom) and is masked by a moving
 * window, bright at the reading head and fading out behind it (a comet trail) and just ahead of
 * it, so only the words being sung glow. Without a [head] (no synced lyrics) the whole line gets
 * a faint, even halo instead.
 *
 * Blur radius scales with the font size (the old fixed 28-100 px shadow was several times the
 * height of a 14 sp line and smeared into a pale box). The layer is padded so the halo is never
 * cut flat at the text bounds, while the reported size stays equal to the plain text so layout
 * is identical to the base lyric underneath.
 */
@Composable
fun ReadingGlowLine(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    intensity: Float,
    head: (() -> Float)?,
    lowPower: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val fontPx = with(density) { fontSize.toPx() }
    val padPx = (fontPx * 1.6f).toInt()
    val padDp = with(density) { padPx.toDp() }
    val a = (0.55f + intensity * 1.6f).coerceIn(0.55f, 1f)
    val tightR = fontPx * 0.38f
    val wideR = fontPx * 1.05f
    val glowColor = Color.White
    val layoutRef = remember { arrayOfNulls<TextLayoutResult>(1) }

    Box(
        modifier = modifier
            // Report the size of the bare text, but measure/draw with room around it for the halo.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.offset(horizontal = padPx * 2))
                val w = (placeable.width - padPx * 2).coerceAtLeast(0)
                val h = (placeable.height - padPx * 2).coerceAtLeast(0)
                layout(w, h) { placeable.place(-padPx, -padPx) }
            }
    ) {
        Box(
            // Layer and mask come BEFORE the padding, so the offscreen buffer includes the
            // padded margin and the halo is not cut at the text bounds.
            Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    val lay = layoutRef[0] ?: return@drawWithContent
                    drawContent()
                    val hd = head?.invoke()
                    if (hd == null) {
                        // No timing: even, faint halo.
                        drawRect(Color.White.copy(alpha = 0.8f), size = size, blendMode = BlendMode.DstIn)
                        return@drawWithContent
                    }
                    val n = lay.lineCount
                    var total = 0f
                    for (i in 0 until n) total += lay.getLineRight(i) - lay.getLineLeft(i)
                    if (total <= 1f) return@drawWithContent
                    // Only the word being sung glows: its span is measured in characters.
                    val (ws, we) = wordWindow(text, hd)
                    var cum = 0f
                    for (i in 0 until n) {
                        val left = lay.getLineLeft(i)
                        val lw = lay.getLineRight(i) - left
                        val topEdge = if (i == 0) 0f else padPx + (lay.getLineBottom(i - 1) + lay.getLineTop(i)) / 2f
                        val botEdge = if (i == n - 1) size.height else padPx + (lay.getLineBottom(i) + lay.getLineTop(i + 1)) / 2f
                        // Word span in this line's local coordinates (negative or past the line
                        // end means the word is on another line, so the mask stays transparent).
                        val xs = padPx + left + ws * total - cum
                        val xe = padPx + left + we * total - cum
                        val mask = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.18f to Color.White.copy(alpha = 0.55f),
                                0.50f to Color.White,
                                0.82f to Color.White.copy(alpha = 0.55f),
                                1.00f to Color.Transparent
                            ),
                            startX = xs,
                            endX = xe
                        )
                        drawRect(
                            brush = mask,
                            topLeft = Offset(0f, topEdge),
                            size = Size(size.width, (botEdge - topEdge).coerceAtLeast(0f)),
                            blendMode = BlendMode.DstIn
                        )
                        cum += lw
                    }
                }
                .padding(padDp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                if (!lowPower) {
                    Text(
                        text = text,
                        color = color,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        style = TextStyle(shadow = Shadow(glowColor.copy(alpha = a * 0.8f), Offset.Zero, wideR)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = text,
                    color = color,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    style = TextStyle(shadow = Shadow(glowColor.copy(alpha = a), Offset.Zero, tightR)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layoutRef[0] = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Start and end (0..1 of the line) of the word at reading position [f].
 * Words are weighted by character count (plus the following space), which tracks the rendered
 * width closely enough that the glow steps from word to word instead of sliding per letter.
 */
internal fun wordWindow(text: String, f: Float): Pair<Float, Float> {
    val words = text.split(' ').filter { it.isNotEmpty() }
    if (words.size <= 1) return 0f to 1f
    val lens = words.map { it.length + 1f }
    val total = lens.sum()
    val p = f.coerceIn(0f, 1f) * total
    var acc = 0f
    for (i in words.indices) {
        val end = acc + lens[i]
        if (p < end || i == words.lastIndex) {
            return (acc / total) to ((end - 1f) / total)
        }
        acc = end
    }
    return 0f to 1f
}
