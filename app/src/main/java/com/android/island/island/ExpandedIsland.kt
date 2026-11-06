package com.android.island.island

import com.android.island.util.IslandDiagnostics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.core.Controls
import com.android.island.core.GlassConfig
import com.android.island.core.IslandActivity
import com.android.island.core.IslandBus
import kotlinx.coroutines.delay
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import kotlinx.coroutines.launch

/** OLED pure black for expanded text. */
private val OledBlack = Color(0xFF000000)
private val OledMuted = Color(0xFF000000).copy(alpha = 0.55f)

@Composable
fun ExpandedIsland(
    activity: IslandActivity,
    whatsAppItems: List<IslandActivity.WhatsApp> = emptyList(),
    glassConfig: GlassConfig,
    showLyrics: Boolean = true,
    expandedWidthDp: Int = 340,
    expandedMinHeightDp: Int = 0,
    showAlbumBackground: Boolean = true,
    expandedScale: Float = 1f,
    lowPowerMode: Boolean = false,
    lyricShineEnabled: Boolean = false,
    lyricShineSpeed: Float = 1f,
    lyricShineIntensity: Float = 0.35f,
    lyricGlowIntensity: Float = 0.14f,
    lyricGlowEnabled: Boolean = false,
    lyricFontSize: Int = 14,
    lyricFontWeight: Int = 500,
    compactWhatsAppBanner: Boolean = false,
    glassSweepEnabled: Boolean = true,
    glassSweepOpacity: Float = 0.25f,
    glassSweepDurationMs: Int = 700,
    backgroundOpacity: Float = 0.55f,
    edgeLensStrength: Float = 0.6f,
    /** Reports the measured height (dp) of the WhatsApp stack. */
    onStackHeightDp: (Int) -> Unit = {},
    bannerFontSize: Int = 14,
    themeMode: String = "ADAPTIVE",
    hapticsEnabled: Boolean = false,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val effectiveGlassConfig = if (lowPowerMode) {
        glassConfig.copy(chromaticAberration = 0f, ambientReflection = 0.28f)
    } else glassConfig
    when (activity) {
        is IslandActivity.Media -> MediaExpanded(
            media = activity,
            glassConfig = effectiveGlassConfig,
            showLyrics = showLyrics,
            // IslandRoot already applies expandedScale to the shared morph bounds.
            widthDp = expandedWidthDp.dp,
            minHeightDp = if (expandedMinHeightDp > 0) expandedMinHeightDp.dp else null,
            showAlbumBackground = showAlbumBackground,
            layoutScale = expandedScale.coerceIn(0.6f, 1.25f),
            lowPowerMode = lowPowerMode,
            lyricShineEnabled = lyricShineEnabled,
            lyricShineSpeed = lyricShineSpeed,
            lyricShineIntensity = lyricShineIntensity,
            lyricGlowIntensity = lyricGlowIntensity,
            lyricGlowEnabled = lyricGlowEnabled,
            lyricFontSize = lyricFontSize,
            lyricFontWeight = lyricFontWeight,
            glassSweepEnabled = glassSweepEnabled,
            glassSweepOpacity = glassSweepOpacity,
            glassSweepDurationMs = glassSweepDurationMs,
            backgroundOpacity = backgroundOpacity,
            edgeLensStrength = edgeLensStrength,
            themeMode = themeMode,
            hapticsEnabled = hapticsEnabled,
            onCollapse = onCollapse
        )
        is IslandActivity.Call -> CallExpanded(call = activity, widthDp = expandedWidthDp.dp)
        is IslandActivity.WhatsApp -> {
            val chats = whatsAppItems.ifEmpty { listOf(activity) }
            val density = LocalDensity.current
            // The stack sizes to its content, capped at 78% of the screen; beyond that it scrolls.
            // Its measured height is reported up so the panel and the touch window match it.
            val maxStackH = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
            Column(
                modifier = Modifier
                    .width(expandedWidthDp.dp)
                    .heightIn(max = maxStackH)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 4.dp)
                    .onSizeChanged { size ->
                        onStackHeightDp(with(density) { size.height.toDp() }.value.roundToInt())
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chats.forEach { chat ->
                    key(chat.id) {
                        WhatsAppBanner(
                            activity = chat,
                            glassConfig = effectiveGlassConfig,
                            showAlbumBackground = showAlbumBackground,
                            compactMode = compactWhatsAppBanner,
                            backgroundOpacity = backgroundOpacity,
                            edgeLensStrength = edgeLensStrength,
                            lowPowerMode = lowPowerMode,
                            compactWidthDp = expandedWidthDp,
                            standardWidthDp = expandedWidthDp,
                            bannerFontSize = bannerFontSize,
                            themeMode = themeMode,
                            onTap = {
                                // Open only the tapped chat and remove just that card.
                                val pkg = if (chat.conversationKey.contains("com.whatsapp.w4b")) "com.whatsapp.w4b" else "com.whatsapp"
                                if (Controls.openChat(context, chat.contentIntent, pkg)) IslandBus.clearWhatsApp(chat.id)
                            },
                            onDismiss = { IslandBus.clearWhatsApp(chat.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaExpanded(
    media: IslandActivity.Media,
    glassConfig: GlassConfig,
    showLyrics: Boolean,
    widthDp: Dp,
    minHeightDp: Dp?,
    showAlbumBackground: Boolean,
    layoutScale: Float,
    lowPowerMode: Boolean,
    lyricShineEnabled: Boolean,
    lyricShineSpeed: Float,
    lyricShineIntensity: Float,
    lyricGlowIntensity: Float,
    lyricGlowEnabled: Boolean,
    lyricFontSize: Int,
    lyricFontWeight: Int,
    glassSweepEnabled: Boolean,
    glassSweepOpacity: Float,
    glassSweepDurationMs: Int,
    backgroundOpacity: Float,
    edgeLensStrength: Float,
    themeMode: String,
    hapticsEnabled: Boolean,
    onCollapse: () -> Unit
) {
    val accent = media.accentColor ?: Color(0xFF5B8DEF)
    val shape = RoundedCornerShape(22.dp)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    // Drag follows finger — applied OUTSIDE clip so it is not cut off
    var swipeOffsetX by remember(media.id) { mutableFloatStateOf(0f) }
    val dismissAlpha = remember { Animatable(1f) }
    val dismissScale = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    val tick: () -> Unit = { if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val latestCollapse by rememberUpdatedState(onCollapse)
    // The same composable instance can be reused for the next track after a swipe-dismiss;
    // without this reset it would stay invisible (alpha 0 / scale .84) forever.
    LaunchedEffect(media.id) {
        dismissAlpha.snapTo(1f)
        dismissScale.snapTo(1f)
    }

    // ---- small-detail motion (all read in layer/draw phase; no recomposition) ----
    val enter = remember { Animatable(if (lowPowerMode) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (enter.value < 1f) enter.animateTo(1f, tween(560, easing = ExpoOut))
    }
    val artPop = remember { Animatable(1f) }
    LaunchedEffect(media.id) {
        if (!lowPowerMode) {
            artPop.snapTo(0.86f)
            artPop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val heartPop = remember { Animatable(1f) }
    val playBump = remember { Animatable(1f) }
    var firstBump by remember { mutableStateOf(true) }
    LaunchedEffect(media.isPlaying) {
        // Skip the very first run: opening the panel must not look like a play/pause toggle.
        if (firstBump) { firstBump = false; return@LaunchedEffect }
        playBump.snapTo(0.78f)
        playBump.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
    }
    // Apple-Music style: artwork settles smaller while paused, springs back when playing.
    val artRest = animateFloatAsState(
        targetValue = if (media.isPlaying) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "art-rest"
    )
    val prevNudge = remember { Animatable(0f) }
    val nextNudge = remember { Animatable(0f) }

    // Blurred artwork + adaptive ink colour (white on dark art, black on light art).
    val art = rememberArtBackdrop(media.artwork, enabled = showAlbumBackground).value
    val darkArt = art?.isDark == true
    val useLightInk = when (themeMode) {
        "WHITE" -> true
        "BLACK", "OUTLINE" -> false
        else -> darkArt
    }
    // Outline is a text treatment; its controls retain the built-in high-contrast white.
    val ink = if (themeMode == "OUTLINE" || useLightInk) Color.White else OledBlack
    val inkMuted = ink.copy(alpha = if (useLightInk) 0.72f else 0.55f)

    // The player's own like state wins when it reports one; the local store is the fallback
    // and is kept in sync so the heart stays right when the app exposes no rating.
    var liked by remember(media.id, media.title, media.artist, media.userLiked) {
        LikedTracks.init(context)
        val reported = media.userLiked
        if (reported != null) LikedTracks.setLiked(media.id, media.title, media.artist, reported)
        mutableStateOf(reported ?: LikedTracks.isLiked(media.id, media.title, media.artist))
    }

    // True playback-timestamp position (not interval-only refresh)
    val livePosState = remember(media.id) { mutableLongStateOf(media.position) }
    var livePos by livePosState
    LaunchedEffect(media.id, media.isPlaying, media.position) {
        // Playback callbacks/metadata updates seed paused and newly-seeked positions.
        // While actively playing, a modest 250 ms ticker keeps progress/lyrics current.
        // This composable exists only while Expanded is visible, so there is no idle ticker.
        livePos = media.position.coerceAtLeast(0L)
        if (media.isPlaying) {
            while (true) {
                com.android.island.core.ScreenState.awaitInteractive()
                val playback = Controls.mediaController?.playbackState
                if (playback != null && playback.state == android.media.session.PlaybackState.STATE_PLAYING) {
                    val elapsed = (SystemClock.elapsedRealtime() - playback.lastPositionUpdateTime).coerceAtLeast(0L)
                    livePos = (playback.position + elapsed).coerceAtMost(
                        media.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                    )
                } else {
                    livePos = media.position.coerceAtLeast(0L)
                }
                delay(250L)
            }
        }
    }

    // Binary-search friendly lyric line from exact timestamp
    // Playback position can update several times per second. Expose only the
    // current lyric string as derived state so text consumers recompose when the
    // lyric line changes, not on every playback-position tick.
    val lyricSlot by remember(media.lyrics, media.title) {
        derivedStateOf {
            val lines = media.lyrics
            if (lines.isEmpty()) LyricSlot("♪ ${media.title}", 0L, 0L, timed = false)
            else {
                var lo = 0
                var hi = lines.lastIndex
                var best = 0
                while (lo <= hi) {
                    val mid = (lo + hi) ushr 1
                    if (lines[mid].timeMs <= livePos) {
                        best = mid
                        lo = mid + 1
                    } else {
                        hi = mid - 1
                    }
                }
                val start = lines[best].timeMs
                val end = if (best < lines.lastIndex) lines[best + 1].timeMs else start + 4500L
                LyricSlot(lines[best].text.ifBlank { "♪ ${media.title}" }, start, end, timed = true)
            }
        }
    }
    val lyricText = lyricSlot.text
    // Reading head for the optional lyric glow (only runs while glow is on and lyrics are synced).
    val readingHead = rememberReadingHead(
        slot = lyricSlot,
        position = { livePosState.longValue },
        playing = media.isPlaying,
        enabled = showLyrics && lyricGlowEnabled && lyricGlowIntensity > 0.02f
    )

    // One-shot glass sweep: retriggers on a new expanded media item, never loops.
    // 0f = band fully off the left edge, 1f = band fully off the right edge, so it enters
    // and exits smoothly instead of popping in/out mid-surface.
    val glassSweep = remember { Animatable(0f) }
    LaunchedEffect(media.id, glassSweepEnabled) {
        glassSweep.snapTo(0f)
        if (glassSweepEnabled) {
            // Let the expand morph settle first so the sweep never competes with it for frames.
            delay(240L)
            glassSweep.animateTo(
                1f,
                tween(
                    durationMillis = glassSweepDurationMs.coerceIn(500, 2600),
                    easing = androidx.compose.animation.core.CubicBezierEasing(0.35f, 0f, 0.55f, 1f)
                )
            )
        }
    }

    // Outer box: no clip — so drag is never cut
    Box(
        modifier = Modifier
            .width(widthDp.coerceIn(220.dp, (screenWidthDp - 8).coerceAtLeast(220).dp))
            .then(if (minHeightDp != null) Modifier.heightIn(min = minHeightDp) else Modifier)
            .graphicsLayer {
                // Keep the whole surface inside the WRAP_CONTENT overlay window.
                // Drag feedback is a subtle compression; dismiss fades/scales instead
                // of translating beyond the window bounds (which caused edge clipping).
                val dragProgress = (kotlin.math.abs(swipeOffsetX) / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
                scaleX = dismissScale.value * (1f - dragProgress * 0.06f)
                scaleY = dismissScale.value
                alpha = dismissAlpha.value
            }
            // Notification-style swipe-to-dismiss: horizontal only, starts on the banner.
            // Vertical movement and taps remain normal UI interactions.
            .pointerInput(media.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        if (dragAmount > 0f || swipeOffsetX > 0f) {
                            change.consume()
                            swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(0f, size.width * 0.85f)
                        }
                    },
                    onDragEnd = {
                        val threshold = with(density) { 76.dp.toPx() }
                        if (swipeOffsetX >= threshold) {
                            scope.launch {
                                launch {
                                    androidx.compose.animation.core.Animatable(swipeOffsetX).animateTo(
                                        size.width.toFloat() * 1.15f,
                                        tween(180, easing = ExpoOut)
                                    ) { swipeOffsetX = value }
                                }
                                launch { dismissAlpha.animateTo(0f, tween(170, easing = ExpoOut)) }
                                launch { dismissScale.animateTo(0.84f, tween(180, easing = ExpoOut)) }
                                delay(180)
                                IslandBus.forceDismissMedia()
                            }
                        } else {
                            scope.launch {
                                androidx.compose.animation.core.Animatable(swipeOffsetX).animateTo(
                                    0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)
                                ) { swipeOffsetX = value }
                                dismissScale.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            androidx.compose.animation.core.Animatable(swipeOffsetX).animateTo(
                                0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)
                            ) { swipeOffsetX = value }
                            dismissScale.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium))
                        }
                    }
                )
            }
            // Swipe up anywhere on the card to close it (mirrors swipe-up-to-open on the capsule).
            .pointerInput(media.id) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, amount -> total += amount },
                    onDragEnd = {
                        if (total < -with(density) { 40.dp.toPx() }) { tick(); latestCollapse() }
                        total = 0f
                    },
                    onDragCancel = { total = 0f }
                )
            }
    ) {
        // Diagnostics: one line whenever the art or glass settings change (cheap; off unless recording is on).
        LaunchedEffect(art?.key, backgroundOpacity, edgeLensStrength, lowPowerMode, showAlbumBackground) {
            IslandDiagnostics.record(
                "GLASS_FX",
                "artKey=${art?.key} opacity=$backgroundOpacity lens=$edgeLensStrength lowPower=$lowPowerMode album=$showAlbumBackground lyricsOn=$showLyrics"
            )
        }
        // One drift clock shared by the photo backdrop and the edge lens. Runs only when there is art to animate.
        val driftPhase = rememberDriftPhase(animate = false)
        // Inner clipped surface. The one-shot sweep is drawn inside this rounded clip,
        // never across the transparent rectangular host/window.
        Box(
            Modifier.clip(shape).drawWithCache {
                // Built once per size. The band is a soft, slanted light streak, not a rectangle:
                //  * the profile is a gaussian-like falloff that reaches ZERO alpha at both ends, and
                //  * the brush is filled over the whole card (never over a band-sized rect), so
                //    there are no hard vertical edges anywhere - the old version drew a
                //    band-wide rectangle whose edges were still visibly opaque ("kotak-kotak").
                val alpha = glassSweepOpacity.coerceIn(0f, 1f)
                // The streak travels along the diagonal from the top-left corner to the bottom-right.
                // Its isolines are perpendicular to (1,1), so the band is drawn as a gradient along
                // that direction and clamped to transparent outside the band (no hard edges).
                val sqrt2 = kotlin.math.sqrt(2f)
                val diag = (size.width + size.height) / sqrt2
                val bandU = diag * 0.45f
                val sweepStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.10f to Color.White.copy(alpha = alpha * 0.02f),
                    0.20f to Color.White.copy(alpha = alpha * 0.10f),
                    0.30f to Color.White.copy(alpha = alpha * 0.32f),
                    0.40f to Color.White.copy(alpha = alpha * 0.66f),
                    0.50f to Color.White.copy(alpha = alpha),
                    0.60f to Color.White.copy(alpha = alpha * 0.66f),
                    0.70f to Color.White.copy(alpha = alpha * 0.32f),
                    0.80f to Color.White.copy(alpha = alpha * 0.10f),
                    0.90f to Color.White.copy(alpha = alpha * 0.02f),
                    1.00f to Color.Transparent
                )
                onDrawWithContent {
                    drawContent()
                    val progress = glassSweep.value
                    if (glassSweepEnabled && alpha > 0.01f && progress > 0f && progress < 1f) {
                        // u = (x + y) / sqrt2 is the position across the diagonal (0 at the top-left corner).
                        val u0 = -bandU / 2f + (diag + bandU) * progress
                        val aU = u0 - bandU / 2f
                        val bU = u0 + bandU / 2f
                        drawRect(
                            brush = Brush.linearGradient(
                                colorStops = sweepStops,
                                start = Offset(aU / sqrt2, aU / sqrt2),
                                end = Offset(bU / sqrt2, bU / sqrt2)
                            ),
                            size = size
                        )
                    }
                }
            }
        ) {
            if (showAlbumBackground && media.artwork != null) {
                FluidGlassBackdrop(
                    art = art,
                    accent = accent,
                    photoAlpha = 0.80f,
                    animate = false,
                    modifier = Modifier.matchParentSize(),
                    opacity = backgroundOpacity,
                    externalPhase = driftPhase
                )
            }

            Box(
                Modifier
                    .matchParentSize()
                    .liquidGlass(config = glassConfig, accent = accent, shape = shape, surfaceOpacity = backgroundOpacity, edgeLens = edgeLensStrength)
            )
            // Tight, efficient spacing — iPhone DI style
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = if (layoutScale < 0.85f) 8.dp else 10.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row: art | title + lyric (marquee) + artist
                Row(
                    Modifier.fillMaxWidth().staggerIn(enter, 0),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val artSize = if (layoutScale < 0.85f) 36.dp else 42.dp
                    if (media.artwork != null) {
                        Image(
                            bitmap = androidx.compose.runtime.remember(media.artwork) { media.artwork.asImageBitmap() },
                            contentDescription = null,
                            modifier = Modifier
                                .size(artSize)
                                .graphicsLayer { val a = artPop.value * artRest.value; scaleX = a; scaleY = a }
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { Controls.openApp(context, media.packageName) },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier
                                .size(artSize)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color.Black.copy(0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = ink, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        androidx.compose.animation.Crossfade(
                            targetState = media.title,
                            animationSpec = tween(240),
                            label = "title"
                        ) { titleText ->
                            OutlinedText(
                                titleText,
                                color = if (themeMode == "OUTLINE") OutlineTextFill else ink,
                                outlineEnabled = themeMode == "OUTLINE",
                                fontSize = if (layoutScale < 0.85f) 13.sp else 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (showLyrics) {
                            // Glow always respects intensity slider; lowPower only softens it.
                            val glowAlpha = if (lyricGlowEnabled) (lyricGlowIntensity * 1.15f).coerceIn(0f, 0.85f) else 0f
                            val shineOn = lyricShineEnabled && lyricShineIntensity > 0.02f
                            val shineIntensity = lyricShineIntensity.coerceIn(0f, 0.85f)
                            val lyricSize = lyricFontSize.coerceIn(9, 22).sp
                            val lyricWeight = FontWeight(lyricFontWeight.coerceIn(300, 900))
                            // The actual lyric remains OLED black. White glow is a glyph shadow,
                            // not a background or a rectangular compositing layer.
                            AnimatedContent(
                                targetState = lyricText,
                                transitionSpec = {
                                    (fadeIn(tween(240, delayMillis = 70)) +
                                        slideInVertically(tween(300, easing = ExpoOut)) { it / 3 }) togetherWith
                                        (fadeOut(tween(170, easing = ExpoOut)) + slideOutVertically(tween(230, easing = ExpoOut)) { -it / 4 })
                                },
                                label = "lyric"
                            ) { lyricLine ->
                            Box(Modifier.fillMaxWidth().heightIn(min = (lyricFontSize.coerceIn(9, 22) * 2.5f).dp)) {
                                OutlinedText(
                                    lyricLine,
                                    color = if (themeMode == "OUTLINE") OutlineTextFill else ink,
                                    outlineEnabled = themeMode == "OUTLINE",
                                    fontSize = lyricSize,
                                    fontWeight = lyricWeight,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Optional glow: only the words being read light up (see LyricGlow.kt).
                                if (glowAlpha > 0.02f) {
                                    ReadingGlowLine(
                                        text = lyricLine,
                                        color = if (themeMode == "OUTLINE") Color.White else ink,
                                        fontSize = lyricSize,
                                        fontWeight = lyricWeight,
                                        intensity = lyricGlowIntensity,
                                        head = if (lyricSlot.timed && lyricLine == lyricSlot.text) ({ readingHead.value }) else null,
                                        lowPower = lowPowerMode,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                // Shine is a second text-only pass. Transparent brush regions
                                // reveal the OLED-black base glyph beneath it.
                                if (shineOn) {
                                    val shinePhase by rememberShinePhase(
                                        active = true,
                                        durationMs = (2200f / lyricShineSpeed.coerceIn(0.25f, 2.5f)).toInt().coerceIn(900, 9000)
                                    )
                                    Text(
                                        lyricLine,
                                        fontSize = lyricSize,
                                        fontWeight = lyricWeight,
                                        style = androidx.compose.ui.text.TextStyle(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = shineIntensity * 0.45f),
                                                    Color.White.copy(alpha = shineIntensity),
                                                    Color.White.copy(alpha = shineIntensity * 0.45f),
                                                    Color.Transparent
                                                ),
                                                start = Offset(shinePhase, 0f),
                                                end = Offset(shinePhase + 220f, 0f)
                                            )
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            }
                        } else if (media.artist.isNotBlank()) {
                            OutlinedText(
                                media.artist,
                                color = if (themeMode == "OUTLINE") OutlineTextFill else ink,
                                outlineEnabled = themeMode == "OUTLINE",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (media.duration > 0) {
                    ProgressSection(
                        media = media,
                        livePosState = livePosState,
                        ink = ink,
                        outline = themeMode == "OUTLINE",
                        enter = enter,
                        onTick = tick
                    )
                }

                // Transport — compact row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .staggerIn(enter, 2),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PressIconButton(
                        onClick = {
                            tick()
                            val next = !liked
                            // Always perform a real, persistent in-app favorite action.
                            // Also forward the rating/custom action when the player exposes it.
                            liked = next
                            LikedTracks.setLiked(media.id, media.title, media.artist, next)
                            scope.launch {
                                heartPop.snapTo(if (next) 0.55f else 0.8f)
                                heartPop.animateTo(1f, spring(dampingRatio = if (next) 0.35f else 0.6f, stiffness = Spring.StiffnessMedium))
                            }
                            val sentToPlayer = Controls.toggleFavorite(next)
                            Toast.makeText(
                                context,
                                if (sentToPlayer) {
                                    if (next) "Ditambahkan ke favorit pemutar" else "Dihapus dari favorit pemutar"
                                } else {
                                    if (next) "Disimpan di favorit Android Island" else "Dihapus dari favorit Android Island"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        androidx.compose.animation.Crossfade(
                            targetState = liked,
                            animationSpec = tween(160),
                            label = "like"
                        ) { isLiked ->
                            Icon(
                                if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                null,
                                tint = if (isLiked) Color(0xFFE53935) else ink.copy(0.7f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { scaleX = heartPop.value; scaleY = heartPop.value }
                            )
                        }
                    }
                    PressIconButton(
                        onClick = {
                            tick()
                            Controls.prev()
                            scope.launch {
                                prevNudge.snapTo(0f)
                                prevNudge.animateTo(-7f, tween(70))
                                prevNudge.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SkipPrevious, null, tint = ink,
                            modifier = Modifier.size(24.dp).graphicsLayer { translationX = prevNudge.value * this.density }
                        )
                    }
                    // Filled glass-white play/pause disc (reference look). Icon contrast flips with the ink.
                    val playSource = remember { MutableInteractionSource() }
                    val onInk = if (ink == Color.White) Color.Black else Color.White
                    Box(
                        Modifier
                            .size(40.dp)
                            .pressScaleWith(playSource, 0.9f)
                            .clip(CircleShape)
                            .background(ink.copy(alpha = 0.92f))
                            .clickable(interactionSource = playSource, indication = null) {
                                tick()
                                Controls.playPause()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.Crossfade(
                            targetState = media.isPlaying,
                            animationSpec = tween(170),
                            label = "playpause"
                        ) { playing ->
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                null,
                                tint = onInk,
                                modifier = Modifier
                                    .size(26.dp)
                                    .graphicsLayer { scaleX = playBump.value; scaleY = playBump.value }
                            )
                        }
                    }
                    PressIconButton(
                        onClick = {
                            tick()
                            Controls.next()
                            scope.launch {
                                nextNudge.snapTo(0f)
                                nextNudge.animateTo(7f, tween(70))
                                nextNudge.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext, null, tint = ink,
                            modifier = Modifier.size(24.dp).graphicsLayer { translationX = nextNudge.value * this.density }
                        )
                    }
                    Spacer(Modifier.size(34.dp))
                }
            }
        }
    }
}

/**
 * Progress bar + times. Lives in its own composable so the 4 Hz position ticks recompose only
 * this small subtree instead of the whole expanded card. Supports tap-to-seek and
 * drag-to-scrub (track thickens, thumb grows, time follows the finger, seek on release).
 */
@Composable
private fun ProgressSection(
    media: IslandActivity.Media,
    livePosState: MutableLongState,
    ink: Color,
    outline: Boolean,
    enter: Animatable<Float, AnimationVector1D>,
    onTick: () -> Unit
) {
    var scrub by remember(media.id) { mutableFloatStateOf(-1f) }
    val scrubbing = scrub >= 0f
    val duration = media.duration.coerceAtLeast(1L)
    val shownPos = if (scrubbing) (scrub * duration).toLong() else livePosState.longValue
    val progress = (shownPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val progAnim = animateFloatAsState(
        progress,
        if (scrubbing) snap<Float>() else tween(300, easing = androidx.compose.animation.core.LinearEasing),
        label = "progress"
    )
    val thumbAnim = animateFloatAsState(
        if (scrubbing) 6f else if (media.isPlaying) 3f else 4.6f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "thumb"
    )
    val trackAnim = animateFloatAsState(
        if (scrubbing) 4f else 2.5f,
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), label = "track"
    )
    Column(
        Modifier.fillMaxWidth().staggerIn(enter, 1),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .pointerInput(media.id, media.duration) {
                    detectTapGestures { off ->
                        val p = (off.x / size.width).coerceIn(0f, 1f)
                        val target = (p * media.duration).toLong()
                        onTick()
                        Controls.seekTo(target)
                        livePosState.longValue = target
                    }
                }
                .pointerInput(media.id, media.duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { off ->
                            scrub = (off.x / size.width).coerceIn(0f, 1f)
                            onTick()
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            scrub = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val target = (scrub.coerceIn(0f, 1f) * media.duration).toLong()
                            scrub = -1f
                            onTick()
                            Controls.seekTo(target)
                            livePosState.longValue = target
                        },
                        onDragCancel = { scrub = -1f }
                    )
                }
                .drawBehind {
                    val h = trackAnim.value.dp.toPx()
                    val cy = size.height / 2f
                    val corner = CornerRadius(h / 2f)
                    drawRoundRect(ink.copy(0.14f), Offset(0f, cy - h / 2f), Size(size.width, h), corner)
                    val x = size.width * progAnim.value
                    drawRoundRect(ink.copy(0.9f), Offset(0f, cy - h / 2f), Size(x, h), corner)
                    val r = thumbAnim.value.dp.toPx()
                    drawCircle(ink, r, Offset(x.coerceIn(r, (size.width - r).coerceAtLeast(r)), cy))
                }
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedText(formatTime(shownPos), color = if (outline) OutlineTextFill else ink, outlineEnabled = outline, fontSize = 9.sp)
            OutlinedText(formatTime(media.duration), color = if (outline) OutlineTextFill else ink, outlineEnabled = outline, fontSize = 9.sp)
        }
    }
}

object LikedTracks {
    private val mem = mutableSetOf<String>()
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: android.content.Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("liked_tracks", android.content.Context.MODE_PRIVATE)
        prefs?.getStringSet("ids", emptySet())?.let { mem.addAll(it) }
    }

    private fun key(id: String, title: String, artist: String) =
        "${id.trim()}|${title.trim().lowercase()}|${artist.trim().lowercase()}"

    fun isLiked(id: String, title: String, artist: String): Boolean =
        key(id, title, artist) in mem

    fun setLiked(id: String, title: String, artist: String, liked: Boolean) {
        val k = key(id, title, artist)
        if (liked) mem.add(k) else mem.remove(k)
        prefs?.edit()?.putStringSet("ids", HashSet(mem))?.apply()
    }
}



private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
