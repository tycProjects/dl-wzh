package com.android.island.island

import com.android.island.util.IslandDiagnostics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
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
    edgeLensStrength: Float = 0.85f,
    /** Reports the measured height (dp) of the WhatsApp stack. */
    onStackHeightDp: (Int) -> Unit = {},
    bannerFontSize: Int = 14,
    themeMode: String = "ADAPTIVE",
    hapticsEnabled: Boolean = false,
    /** Corner radius (dp) of the panel clip, so the glass rim follows the real corner. */
    cornerRadiusDp: Int = 22,
    /** Open/close morph progress (0..1), read only while drawing. */
    openProgress: () -> Float = { 1f },
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
            cornerRadiusDp = cornerRadiusDp,
            openProgress = openProgress,
            onCollapse = onCollapse
        )
        is IslandActivity.Call -> CallExpanded(call = activity, widthDp = expandedWidthDp.dp)
        is IslandActivity.WhatsApp -> {
            // Newest first. At most [STACK_VISIBLE] cards are shown in full; older ones are piled
            // under the last card (like the iOS lock screen). Tapping the pile shows everything.
            val sorted = whatsAppItems.ifEmpty { listOf(activity) }.sortedByDescending { it.timestamp }
            val density = LocalDensity.current
            var showAll by remember { mutableStateOf(false) }
            LaunchedEffect(sorted.size > STACK_VISIBLE) { if (sorted.size <= STACK_VISIBLE) showAll = false }
            val visible = if (showAll) sorted else sorted.take(STACK_VISIBLE)
            val hidden = if (showAll) emptyList() else sorted.drop(STACK_VISIBLE)
            val maxStackH = GlassTuning.stackCapDp.dp
            val stackListState = androidx.compose.foundation.lazy.rememberLazyListState()
            val softSpring = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.9f, stiffness = 260f)
            val softSizeSpring = spring<androidx.compose.ui.unit.IntSize>(dampingRatio = 0.92f, stiffness = 300f)
            LazyColumn(
                state = stackListState,
                modifier = Modifier
                    .width(expandedWidthDp.dp)
                    .heightIn(max = maxStackH)
                    .animateContentSize(softSizeSpring)
                    .onSizeChanged { size ->
                        onStackHeightDp(with(density) { size.height.toDp() }.value.roundToInt())
                    },
                contentPadding = PaddingValues(
                    start = GlassTuning.stackInsetDp.dp,
                    end = GlassTuning.stackInsetDp.dp,
                    top = GlassTuning.stackInsetDp.dp,
                    bottom = GlassTuning.stackInsetDp.dp
                ),
                verticalArrangement = Arrangement.spacedBy(GlassTuning.stackGapDp.dp)
            ) {
                itemsIndexed(items = visible, key = { _, chat -> chat.id }) { index, chat ->
                    // Newest card on top; the one below slides slightly under it.
                    Box(
                        Modifier
                            .zIndex((visible.size - index).toFloat())
                            .animateItem(
                                fadeInSpec = tween(260),
                                placementSpec = softSpring,
                                fadeOutSpec = tween(200)
                            )
                    ) {
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
                            entryDelayMs = index * (if (lowPowerMode) 48L else 80L),
                            onSwipeUp = { IslandBus.collapseWhatsAppBanner() },
                            onTap = {
                                // Open only the tapped chat and remove just that card.
                                val pkg = if (chat.conversationKey.contains("com.whatsapp.w4b")) "com.whatsapp.w4b" else "com.whatsapp"
                                if (Controls.openChat(context, chat.contentIntent, pkg)) IslandBus.clearWhatsApp(chat.id)
                            },
                            onDismiss = { IslandBus.clearWhatsApp(chat.id) }
                        )
                    }
                }
                if (hidden.isNotEmpty()) {
                    item(key = "stack-pile") {
                        StackPile(
                            hidden = hidden,
                            widthDp = expandedWidthDp - (GlassTuning.stackInsetDp * 2).toInt(),
                            cornerDp = if (compactWhatsAppBanner) 20 else 24,
                            gapDp = GlassTuning.stackGapDp,
                            themeMode = themeMode,
                            modifier = Modifier.animateItem(fadeInSpec = tween(260), placementSpec = softSpring),
                            onClick = { showAll = true }
                        )
                    }
                } else if (showAll && sorted.size > STACK_VISIBLE) {
                    item(key = "stack-less") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .animateItem(fadeInSpec = tween(220), placementSpec = softSpring)
                                .clip(RoundedCornerShape(50))
                                .clickable { showAll = false; },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Tampilkan lebih sedikit",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** How many notification cards are drawn in full before older ones are piled underneath. */
private const val STACK_VISIBLE = 3
/** Height (dp) the pile adds below the last full card. */
private const val STACK_PILE_PEEK_DP = 30

/**
 * Older messages piled under the last card: two narrower, dimmer layers peek out below it, the
 * first one carrying a "+N" label. It is drawn behind the card above (negative z, pulled up by
 * the list gap) so it reads as cards stacked on top of each other, like the reference image.
 */
@Composable
private fun StackPile(
    hidden: List<IslandActivity.WhatsApp>,
    widthDp: Int,
    cornerDp: Int,
    gapDp: Float,
    themeMode: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val layers = if (hidden.size >= 2) 2 else 1
    val names = hidden.map { it.groupName ?: it.senderName }.distinct().take(2).joinToString(", ")
    val label = if (names.isBlank()) "+${hidden.size}" else "+${hidden.size} ($names)"
    val dark = themeMode == "BLACK"
    val tint = if (dark) Color.Black else Color.White
    val textColor = if (dark) Color.White else Color.White
    val pull = gapDp + 22f
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 240f)) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(STACK_PILE_PEEK_DP.dp)
            .zIndex(-1f)
            .graphicsLayer { translationY = -(pull * density) * appear.value; alpha = appear.value }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.TopCenter
    ) {
        for (i in layers downTo 1) {
            val inset = (12 * i).dp
            Box(
                Modifier
                    .padding(top = (6 * i).dp)
                    .width((widthDp.dp - inset * 2).coerceAtLeast(120.dp))
                    .height(40.dp)
                    .graphicsLayer { alpha = 1f - 0.28f * (i - 1) }
                    .clip(RoundedCornerShape(cornerDp.dp))
                    .background(tint.copy(alpha = 0.16f - 0.05f * (i - 1)))
                    .border(0.8.dp, Color.White.copy(alpha = 0.22f - 0.08f * (i - 1)), RoundedCornerShape(cornerDp.dp))
            )
        }
        Text(
            label,
            color = textColor.copy(alpha = 0.92f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(y = 24.dp).padding(horizontal = 28.dp)
        )
    }
}

// ---- Media card geometry (dp). The smallest height at which the layout below does not clip;
// IslandRoot never makes the media card shorter than this (see mediaCardMinHeightDp).
/**
 * Smallest card height (dp) for the media layout (1.4.70 arrangement): art row (title + lyric box or
 * artist), progress bar with times, and the transport row. Prevents the bottom row from being cut.
 */
fun mediaCardMinHeightDp(showLyrics: Boolean, lyricFontSize: Int): Int {
    val row = if (showLyrics) 18 + (lyricFontSize.coerceIn(9, 22) * 2.5f).toInt() else 44
    return 16 + row + 30 + 36 + 8 + 4
}

private val HeartRed = Color(0xFFFF4D5E)

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
    cornerRadiusDp: Int,
    openProgress: () -> Float,
    onCollapse: () -> Unit
) {
    val accent = media.accentColor ?: Color(0xFF5B8DEF)
    // Same radius as the panel clip in IslandRoot, so the glass rim follows the real corner.
    val shape = RoundedCornerShape(cornerRadiusDp.coerceIn(8, 48).dp)
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
    // One cascade clock for the whole card: art, title, each button, lyric line and progress bar
    // enter one after another (see staggerIn). It starts a beat after the glass begins to open,
    // so the card is seen unfolding first and the details then settle in.
    val enter = remember { Animatable(if (lowPowerMode) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (enter.value < 1f) {
            delay(70L)
            enter.animateTo(1f, tween(780, easing = androidx.compose.animation.core.LinearEasing))
        }
    }
    val artPop = remember { Animatable(1f) }
    LaunchedEffect(media.id) {
        if (!lowPowerMode) {
            artPop.snapTo(0.86f)
            artPop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val heartPop = remember { Animatable(1f) }
    val heartBurst = remember { Animatable(1f) }
    val playBump = remember { Animatable(1f) }
    val playRing = remember { Animatable(1f) }
    var firstBump by remember { mutableStateOf(true) }
    LaunchedEffect(media.isPlaying) {
        // Skip the very first run: opening the panel must not look like a play/pause toggle.
        if (firstBump) { firstBump = false; return@LaunchedEffect }
        playBump.snapTo(0.78f)
        playRing.snapTo(0f)
        launch { playRing.animateTo(1f, tween(560, easing = ExpoOut)) }
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
    // After a tap the player needs a moment to confirm; its old answer must not flip the heart back.
    val lastLocalToggleAt = remember { longArrayOf(0L) }
    val lastLocalValue = remember { booleanArrayOf(false) }
    var liked by remember(media.id, media.title, media.artist, media.userLiked) {
        LikedTracks.init(context)
        val reported = media.userLiked
        val recentlyTapped = SystemClock.elapsedRealtime() - lastLocalToggleAt[0] < 1800L
        if (recentlyTapped) {
            mutableStateOf(lastLocalValue[0])
        } else {
            if (reported != null) LikedTracks.setLiked(media.id, media.title, media.artist, reported)
            mutableStateOf(reported ?: LikedTracks.isLiked(media.id, media.title, media.artist))
        }
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
            // One read of the player's clock anchors the ticker; every tick after that is plain arithmetic.
            // (Reading playbackState every 250 ms was a binder call plus an allocation, four times a second.)
            // A seek, pause or resume changes media.position, which restarts this effect with a fresh anchor.
            val anchor = Controls.mediaController?.playbackState
                ?.takeIf { it.state == android.media.session.PlaybackState.STATE_PLAYING }
            val basePos = anchor?.position ?: media.position.coerceAtLeast(0L)
            val baseAt = anchor?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime()
            val speed = anchor?.playbackSpeed?.takeIf { it > 0f } ?: 1f
            val cap = media.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
            while (true) {
                com.android.island.core.ScreenState.awaitInteractive()
                val elapsed = (SystemClock.elapsedRealtime() - baseAt).coerceAtLeast(0L)
                livePos = (basePos + (elapsed * speed).toLong()).coerceIn(0L, cap)
                delay(com.android.island.core.DeviceProfile.positionTickMs)
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
        enabled = false // blur removed: lyric halo is disabled
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
            .shadow(
                elevation = (glassConfig.expandedShadow.coerceIn(0f, 1f) * 26f).dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.72f)
            )
            .graphicsLayer {
                // Keep the whole surface inside the WRAP_CONTENT overlay window.
                // Drag feedback is a subtle compression; dismiss fades/scales instead
                // of translating beyond the window bounds (which caused edge clipping).
                val dragProgress = (kotlin.math.abs(swipeOffsetX) / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
                scaleX = dismissScale.value * (1f - dragProgress * 0.06f)
                scaleY = dismissScale.value * (1f - dragProgress * 0.02f)
                alpha = dismissAlpha.value * (1f - dragProgress * 0.30f)
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
                                        tween(200, easing = ExpoOut)
                                    ) { swipeOffsetX = value }
                                }
                                launch { dismissAlpha.animateTo(0f, tween(190, easing = ExpoOut)) }
                                launch { dismissScale.animateTo(0.84f, tween(200, easing = ExpoOut)) }
                                delay(200)
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
        // never across the transparent rectangular host/window. It fills the whole card, so the
        // glass covers the full panel height even when the content is shorter than the panel.
        Box(
            Modifier.fillMaxSize().clip(shape).drawWithCache {
                // Built once per size. The band is a soft, slanted light streak, not a rectangle:
                //  * the profile is a gaussian-like falloff that reaches ZERO alpha at both ends, and
                //  * the brush is filled over the whole card (never over a band-sized rect), so
                //    there are no hard vertical edges anywhere.
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
                // ONE shader for the whole sweep: it is created here, once per size, and only translated per frame.
                // (Building a new linear-gradient shader every frame was the main cost of the old sweep.)
                val sweepBrush = Brush.linearGradient(
                    colorStops = sweepStops,
                    start = Offset.Zero,
                    end = Offset(bandU / sqrt2, bandU / sqrt2)
                )
                onDrawWithContent {
                    drawContent()
                    val progress = glassSweep.value
                    if (glassSweepEnabled && alpha > 0.01f && progress > 0f && progress < 1f) {
                        // u = (x + y) / sqrt2 is the position across the diagonal (0 at the top-left corner).
                        val u0 = -bandU / 2f + (diag + bandU) * progress
                        val t = (u0 - bandU / 2f) / sqrt2
                        translate(left = t, top = t) {
                            drawRect(brush = sweepBrush, topLeft = Offset(-t, -t), size = size)
                        }
                    }
                }
            }
        ) {
            if (showAlbumBackground && media.artwork != null) {
                FluidGlassBackdrop(
                    art = art,
                    accent = accent,
                    photoAlpha = 0.95f,
                    animate = false,
                    modifier = Modifier.matchParentSize(),
                    opacity = backgroundOpacity,
                    externalPhase = driftPhase,
                    lensStrength = edgeLensStrength,
                    lensCornerDp = cornerRadiusDp.coerceIn(8, 48).toFloat(),
                    frostStrength = glassConfig.frostStrength,
                    waveStrength = 0f
                )
            }

            Box(
                Modifier
                    .matchParentSize()
                    .liquidGlass(config = glassConfig, accent = accent, shape = shape, surfaceOpacity = GlassTuning.glassSurfaceOpacity, edgeLens = edgeLensStrength, whiteProfile = themeMode == "WHITE")
                    // Light that blooms out of the capsule while the card unfolds (gone at rest).
                    .then(if (lowPowerMode) Modifier else Modifier.openBloom(accent, openProgress))
            )
            Box(Modifier.matchParentSize().edgeSheenOverlay(GlassTuning.sheenStrength, cornerRadiusDp.coerceIn(8, 48).toFloat()))

            // Layout of 1.4.70 (art | title + lyric, progress bar with times underneath, transport row),
            // keeping every small-detail motion of 1.4.71 (per-element cascade, press halo, heart burst,
            // play/pause ring, cover cross-fade, title/lyric slide, nudges).
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 12.dp,
                        vertical = if (layoutScale < 0.85f) 7.dp else 8.dp
                    )
                    .graphicsLayer {
                        // While swiping away, the content glides a little inside the glass and
                        // fades, so the gesture has depth even though the glass itself stays put.
                        val p = (swipeOffsetX / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
                        translationX = swipeOffsetX * 0.22f
                        alpha = 1f - p * 0.55f
                    },
                // At the natural height this equals the 1.4.70 spacing; extra "Tinggi minimum" is shared evenly.
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Row: art | title + lyric (or artist)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val artSize = if (layoutScale < 0.85f) 36.dp else 42.dp
                    Box(
                        Modifier
                            .size(artSize)
                            .staggerIn(enter, 0, distanceDp = 6f, fromScale = 0.78f)
                            .graphicsLayer { val a = artPop.value * artRest.value; scaleX = a; scaleY = a }
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { Controls.openApp(context, media.packageName) }
                    ) {
                        // New cover = a soft cross-fade, never a hard swap.
                        androidx.compose.animation.Crossfade(
                            targetState = media.artwork,
                            animationSpec = tween(360),
                            label = "art"
                        ) { bmp ->
                            if (bmp != null) {
                                Image(
                                    bitmap = androidx.compose.runtime.remember(bmp) { bmp.asImageBitmap() },
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    Modifier.fillMaxSize().background(Color.Black.copy(0.06f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, null, tint = ink, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        // Title: a new track slides up into place while the old one lifts away.
                        AnimatedContent(
                            targetState = media.title,
                            modifier = Modifier.fillMaxWidth().staggerIn(enter, 1, distanceDp = 6f),
                            transitionSpec = {
                                (fadeIn(tween(300, delayMillis = 80)) +
                                    slideInVertically(tween(340, easing = ExpoOut)) { it / 2 }) togetherWith
                                    (fadeOut(tween(160)) + slideOutVertically(tween(240, easing = ExpoOut)) { -it / 2 })
                            },
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
                            AnimatedContent(
                                targetState = lyricText,
                                modifier = Modifier.fillMaxWidth().staggerIn(enter, 2, distanceDp = 6f),
                                transitionSpec = {
                                    // A new line rises in from below with a tiny grow; the old one lifts away.
                                    (fadeIn(tween(320, delayMillis = 90)) +
                                        slideInVertically(tween(380, easing = ExpoOut)) { it / 2 } +
                                        scaleIn(tween(380, easing = ExpoOut), initialScale = 0.96f)) togetherWith
                                        (fadeOut(tween(180, easing = ExpoOut)) + slideOutVertically(tween(260, easing = ExpoOut)) { -it / 3 })
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
                                    // reveal the base glyph beneath it.
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
                            androidx.compose.animation.Crossfade(
                                targetState = media.artist,
                                animationSpec = tween(260),
                                modifier = Modifier.staggerIn(enter, 2, distanceDp = 6f),
                                label = "artist"
                            ) { artistText ->
                                OutlinedText(
                                    artistText,
                                    color = if (themeMode == "OUTLINE") OutlineTextFill else ink,
                                    outlineEnabled = themeMode == "OUTLINE",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                        staggerIndex = 3,
                        onTick = tick
                    )
                }

                // Transport row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Heart: the icon swaps with a pop, and a ring + sparks burst out when it turns red.
                    PressIconButton(
                        onClick = {
                            tick()
                            val next = !liked
                            // Always perform a real, persistent in-app favorite action.
                            // Also forward the rating/custom action when the player exposes it.
                            liked = next
                            lastLocalToggleAt[0] = SystemClock.elapsedRealtime()
                            lastLocalValue[0] = next
                            LikedTracks.setLiked(media.id, media.title, media.artist, next)
                            scope.launch {
                                heartPop.snapTo(if (next) 0.55f else 0.8f)
                                heartPop.animateTo(1f, spring(dampingRatio = if (next) 0.35f else 0.6f, stiffness = Spring.StiffnessMedium))
                            }
                            if (next) {
                                scope.launch {
                                    heartBurst.snapTo(0f)
                                    heartBurst.animateTo(1f, tween(620, easing = ExpoOut))
                                }
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
                        haloColor = ink.copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(34.dp)
                            .staggerIn(enter, 4, distanceDp = 8f, fromScale = 0.5f)
                            .drawBehind {
                                val b = heartBurst.value
                                if (b > 0f && b < 1f) {
                                    val fade = 1f - b
                                    val r0 = size.minDimension * 0.30f
                                    val r = r0 + size.minDimension * 0.70f * b
                                    for (i in 0 until 6) {
                                        val ang = (i * 60f - 90f) * (PI.toFloat() / 180f)
                                        drawCircle(
                                            color = HeartRed.copy(alpha = fade * 0.9f),
                                            radius = (0.6f + 2.2f * fade) * this.density,
                                            center = Offset(center.x + cos(ang) * r, center.y + sin(ang) * r)
                                        )
                                    }
                                    drawCircle(
                                        color = HeartRed.copy(alpha = fade * 0.38f),
                                        radius = r0 + size.minDimension * 0.5f * b,
                                        center = center,
                                        style = Stroke(width = (0.6f + 1.6f * fade) * this.density)
                                    )
                                }
                            }
                    ) {
                        AnimatedContent(
                            targetState = liked,
                            transitionSpec = {
                                (fadeIn(tween(150, delayMillis = 40)) +
                                    scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), initialScale = 0.3f)) togetherWith
                                    (fadeOut(tween(100)) + scaleOut(tween(120), targetScale = 0.3f))
                            },
                            contentAlignment = Alignment.Center,
                            label = "like"
                        ) { isLiked ->
                            Icon(
                                if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                null,
                                tint = if (isLiked) HeartRed else ink.copy(0.7f),
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
                        haloColor = ink.copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(34.dp)
                            .staggerIn(enter, 5, distanceDp = 8f, fromScale = 0.5f)
                    ) {
                        Icon(
                            Icons.Rounded.SkipPrevious, null, tint = ink,
                            modifier = Modifier.size(24.dp).graphicsLayer { translationX = prevNudge.value * this.density }
                        )
                    }
                    // Filled glass-white play/pause disc. Icon contrast flips with the ink.
                    val playSource = remember { MutableInteractionSource() }
                    val onInk = if (ink == Color.White) Color.Black else Color.White
                    Box(
                        Modifier
                            .size(40.dp)
                            .staggerIn(enter, 6, distanceDp = 8f, fromScale = 0.5f)
                            .pressScaleWith(playSource, 0.9f)
                            .drawBehind {
                                // A ring pings outward each time playback toggles.
                                val r = playRing.value
                                if (r > 0f && r < 1f) {
                                    drawCircle(
                                        color = ink.copy(alpha = 0.40f * (1f - r)),
                                        radius = size.minDimension / 2f * (1f + 0.8f * r),
                                        style = Stroke(width = 1.6f * this.density)
                                    )
                                }
                            }
                            .clip(CircleShape)
                            .background(ink.copy(alpha = 0.92f))
                            .clickable(interactionSource = playSource, indication = null) {
                                tick()
                                Controls.playPause()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = media.isPlaying,
                            transitionSpec = {
                                (fadeIn(tween(180, delayMillis = 50)) +
                                    scaleIn(spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium), initialScale = 0.4f)) togetherWith
                                    (fadeOut(tween(110)) + scaleOut(tween(130), targetScale = 0.4f))
                            },
                            contentAlignment = Alignment.Center,
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
                        haloColor = ink.copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(34.dp)
                            .staggerIn(enter, 7, distanceDp = 8f, fromScale = 0.5f)
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext, null, tint = ink,
                            modifier = Modifier.size(24.dp).graphicsLayer { translationX = nextNudge.value * this.density }
                        )
                    }
                    // Repeat this song: the slot to the right of "next".
                    RepeatOneButton(
                        ink = ink,
                        accent = accent,
                        durationMs = media.duration,
                        livePosState = livePosState,
                        enter = enter,
                        onTick = tick
                    )
                }
            }
        }
    }
}

/**
 * Repeat-one button (music only), the right-hand slot of the transport row. Small details, all drawn
 * in the graphics/draw phase so nothing recomposes while they run:
 *  - cascades in with the other controls (stagger), jelly press scale and halo (PressIconButton)
 *  - on toggle: a full icon turn, a springy pop, three orbiting dots and a ring that pings outward
 *  - when on: a soft accent disc behind the icon, a dot underneath, and the icon at full strength
 *  - each time the song loops back to the start, a short ring pulse confirms the repeat
 */
@Composable
private fun RepeatOneButton(
    ink: Color,
    accent: Color,
    durationMs: Long,
    livePosState: MutableLongState,
    enter: Animatable<Float, AnimationVector1D>,
    onTick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val on by Controls.repeatOne.collectAsState()
    val spin = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val ring = remember { Animatable(1f) }
    val loopPulse = remember { Animatable(0f) }
    val disc = animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "repeat-disc"
    )
    val dot = animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "repeat-dot"
    )
    val strength = animateFloatAsState(
        targetValue = if (on) 1f else 0.55f,
        animationSpec = tween(220),
        label = "repeat-strength"
    )

    // Loop detection: only watched while repeat is on, and only reads a value that already ticks.
    LaunchedEffect(on, durationMs) {
        if (!on || durationMs <= 0L) return@LaunchedEffect
        var prev = livePosState.longValue
        snapshotFlow { livePosState.longValue }.collect { cur ->
            if (cur < prev && prev > durationMs * 0.75f && cur < durationMs * 0.25f) {
                loopPulse.snapTo(1f)
                loopPulse.animateTo(0f, tween(720, easing = ExpoOut))
            }
            prev = cur
        }
    }

    PressIconButton(
        onClick = {
            onTick()
            val next = !on
            Controls.setRepeatOne(next)
            scope.launch { spin.snapTo(0f); spin.animateTo(1f, tween(640, easing = ExpoOut)) }
            scope.launch {
                pop.snapTo(if (next) 0.55f else 0.8f)
                pop.animateTo(1f, spring(dampingRatio = if (next) 0.35f else 0.6f, stiffness = Spring.StiffnessMedium))
            }
            if (next) scope.launch { ring.snapTo(0f); ring.animateTo(1f, tween(620, easing = ExpoOut)) }
            Toast.makeText(
                context,
                if (next) "Ulangi lagu ini: aktif" else "Ulangi lagu ini: mati",
                Toast.LENGTH_SHORT
            ).show()
        },
        haloColor = ink.copy(alpha = 0.16f),
        modifier = Modifier
            .size(34.dp)
            .staggerIn(enter, 7, distanceDp = 8f, fromScale = 0.5f)
            .drawBehind {
                val d = this.density
                val half = size.minDimension / 2f
                val dv = disc.value
                if (dv > 0.01f) {
                    drawCircle(accent.copy(alpha = 0.30f * dv), radius = half * (0.62f + 0.38f * dv))
                }
                val r = ring.value
                if (r > 0f && r < 1f) {
                    drawCircle(
                        color = accent.copy(alpha = 0.55f * (1f - r)),
                        radius = half * (1f + 0.7f * r),
                        style = Stroke(width = (0.6f + 1.4f * (1f - r)) * d)
                    )
                }
                val sp = spin.value
                if (sp > 0f && sp < 1f) {
                    val fade = sin(PI.toFloat() * sp)
                    for (i in 0 until 3) {
                        val ang = (sp * 360f + i * 120f - 90f) * (PI.toFloat() / 180f)
                        drawCircle(
                            color = accent.copy(alpha = fade * 0.9f),
                            radius = (1.1f + 1.1f * fade) * d,
                            center = Offset(center.x + cos(ang) * half * 0.92f, center.y + sin(ang) * half * 0.92f)
                        )
                    }
                }
                val lp = loopPulse.value
                if (lp > 0.01f) {
                    drawCircle(
                        color = ink.copy(alpha = 0.40f * lp),
                        radius = half * (1f + 0.55f * (1f - lp)),
                        style = Stroke(width = 1.3f * d)
                    )
                }
                val dd = dot.value
                if (dd > 0.01f) {
                    drawCircle(ink.copy(alpha = 0.85f * dd), radius = 1.7f * dd * d, center = Offset(center.x, size.height - 2.6f * d))
                }
            }
    ) {
        AnimatedContent(
            targetState = on,
            transitionSpec = {
                (fadeIn(tween(150, delayMillis = 40)) +
                    scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), initialScale = 0.3f)) togetherWith
                    (fadeOut(tween(100)) + scaleOut(tween(120), targetScale = 0.3f))
            },
            contentAlignment = Alignment.Center,
            label = "repeat-icon"
        ) { isOn ->
            Icon(
                if (isOn) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                null,
                tint = ink.copy(alpha = strength.value),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        val sc = pop.value
                        scaleX = sc
                        scaleY = sc
                        rotationZ = spin.value * 360f
                    }
            )
        }
    }
}

/**
 * Progress bar + times (1.4.70 layout: bar on top, times underneath). Lives in its own composable so
 * the 4 Hz position ticks recompose only this small subtree instead of the whole expanded card.
 * Supports tap-to-seek and drag-to-scrub (track thickens, thumb grows, time follows the finger,
 * seek on release). A tap makes the thumb pulse; while playing the filled part carries a soft glow.
 */
@Composable
private fun ProgressSection(
    media: IslandActivity.Media,
    livePosState: MutableLongState,
    ink: Color,
    outline: Boolean,
    enter: Animatable<Float, AnimationVector1D>,
    staggerIndex: Int,
    onTick: () -> Unit
) {
    val scope = rememberCoroutineScope()
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
    val tapBump = remember { Animatable(0f) }
    val textColor = if (outline) OutlineTextFill else ink
    Column(
        Modifier.fillMaxWidth().staggerIn(enter, staggerIndex, distanceDp = 6f),
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
                        scope.launch {
                            tapBump.snapTo(1f)
                            tapBump.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                        }
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
                    if (media.isPlaying || scrubbing) {
                        drawRoundRect(ink.copy(0.18f), Offset(0f, cy - h), Size(x, h * 2f), CornerRadius(h))
                    }
                    drawRoundRect(ink.copy(0.9f), Offset(0f, cy - h / 2f), Size(x, h), corner)
                    val r = (thumbAnim.value + tapBump.value * 2.4f).dp.toPx()
                    drawCircle(ink, r, Offset(x.coerceIn(r, (size.width - r).coerceAtLeast(r)), cy))
                }
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedText(formatTime(shownPos), color = textColor, outlineEnabled = outline, fontSize = 9.sp)
            OutlinedText(formatTime(media.duration), color = textColor, outlineEnabled = outline, fontSize = 9.sp)
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
        if (liked) {
            mem.add(k)
            while (mem.size > 1000) mem.remove(mem.first())
        } else {
            mem.remove(k)
        }
        prefs?.edit()?.putStringSet("ids", HashSet(mem))?.apply()
    }
}



private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
