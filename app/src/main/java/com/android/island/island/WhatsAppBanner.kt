package com.android.island.island

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.core.Controls
import com.android.island.core.GlassConfig
import com.android.island.core.IslandActivity
import com.android.island.core.IslandBus
import com.android.island.service.IslandAccessibilityService
import kotlinx.coroutines.launch

/**
 * WhatsApp banner. Only notifications carrying a RemoteInput reply action show the reply control;
 * channel/status notifications remain open-only. Tapping the card opens the originating chat.
 */
@Composable
fun WhatsAppBanner(
    activity: IslandActivity.WhatsApp,
    glassConfig: GlassConfig,
    showAlbumBackground: Boolean = true,
    /** False when a parent (the expanded stack) already draws the glass and artwork. */
    drawSurface: Boolean = true,
    compactMode: Boolean = false,
    compactWidthDp: Int = 420,
    compactHeightDp: Int = 58,
    lowPowerMode: Boolean = false,
    standardWidthDp: Int = 360,
    standardHeightDp: Int = 88,
    bannerFontSize: Int = 14,
    bannerFontWeight: Int = 600,
    backgroundOpacity: Float = 0.92f,
    edgeLensStrength: Float = 0.85f,
    themeMode: String = "ADAPTIVE",
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
    onSwipeUp: () -> Unit = {},
    /** Per-card delay used to make a notification stack unfold in a flowing sequence. */
    entryDelayMs: Long = 0L,
    /** How far (dp) the card travels up while it is tucked into the capsule. */
    collapseTravelDp: Float = 30f,
    /** Non-interactive fading copy (timeout exit). Skips entrance pops and all gestures. */
    isGhost: Boolean = false
) {
    val shape = RoundedCornerShape(if (compactMode) 20.dp else 24.dp)
    val accent = androidx.compose.runtime.remember(activity.packageName) { accentForPackage(activity.packageName) }
    // Blurred profile photo + adaptive ink (white on dark photo, black on light/no photo).
    // Each card has its own background: its avatar, or the sender app's icon when there is no avatar.
    val artContext = androidx.compose.ui.platform.LocalContext.current
    val lowEndStatic = com.android.island.core.DeviceProfile.staticGlass
    // Fallback WhatsApp icon: 128 px in every mode.
    val waIconBitmap = androidx.compose.runtime.remember(artContext, lowEndStatic, activity.packageName) {
        appIconBitmap(artContext, activity.packageName, 128)
    }
    // Low-End never routes the avatar through rememberArtBackdrop: that builds an ArtBackdrop
    // wrapper (and, outside Low-End, a cached compact copy) that this path does not need, since
    // the avatar bitmap is already bounded (waIconBitmap / activity.avatar) and shown as-is.
    val art = rememberArtBackdrop(if (lowEndStatic) null else activity.avatar ?: waIconBitmap, enabled = showAlbumBackground).value
    val darkArt = art?.isDark == true
    val useLightInk = when (themeMode) {
        "WHITE" -> true
        "BLACK" -> false
        "OUTLINE" -> false
        else -> darkArt
    }
    // OUTLINE affects glyphs only; controls use a readable white ink.
    val ink = if (themeMode == "OUTLINE" || useLightInk) Color.White else Color.Black
    val inkSoft = ink.copy(alpha = 0.85f)
    val replyTint = ink
    val enter = remember(activity.id) { Animatable(if (isGhost) 1f else 0f) }
    LaunchedEffect(activity.id, entryDelayMs) {
        if (entryDelayMs > 0L) delay(entryDelayMs.coerceAtMost(900L))
        // Durasi masuk dari referensi Dynamic Island (370 ms pada 35 %); fade selesai di awal (~120–155 ms).
        if (enter.value < 1f) enter.animateTo(1f, tween(if (lowPowerMode) 300 else DiMotion.durationMs(), easing = ExpoOut))
    }
    val badgePop = remember(activity.id) { Animatable(1f) }
    LaunchedEffect(activity.messageCount) {
        if (!lowPowerMode && activity.messageCount > 1) {
            badgePop.snapTo(0.5f)
            badgePop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
    }
    val openSource = remember { MutableInteractionSource() }
    val replySource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    var replyText by remember(activity.id) { mutableStateOf("") }
    var showReply by remember(activity.id) { mutableStateOf(false) }
    val replyCloseRequest by IslandBus.replyCloseRequests.collectAsState()
    var lastHandledReplyClose by remember(activity.id) { mutableLongStateOf(0L) }
    LaunchedEffect(replyCloseRequest) {
        if (replyCloseRequest > lastHandledReplyClose) {
            lastHandledReplyClose = replyCloseRequest
            if (showReply) showReply = false
        }
    }
    var showFullMessage by remember(activity.id) { mutableStateOf(false) }
    var messageCanExpand by remember(activity.id) { mutableStateOf(false) }
    var swipeOffset by remember(activity.id) { mutableFloatStateOf(0f) }
    val gestureScope = rememberCoroutineScope()
    val systemDismissAlpha = remember(activity.id) { Animatable(1f) }
    LaunchedEffect(activity.isSystemRemoving, showReply) {
        // Keep the reply surface alive while the user is composing, even if Android
        // reports that the source notification was removed during the interaction.
        if (activity.isSystemRemoving && !showReply) {
            systemDismissAlpha.animateTo(0f, tween(190, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            onDismiss()
        }
    }
    val focusRequester = remember(activity.id) { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Finger-tracked vertical drag (px, negative = up) and the "tuck into capsule" progress.
    // Both are only read in the graphics layer, so dragging never recomposes the card.
    val dens = LocalDensity.current.density
    var dragY by remember(activity.id) { mutableFloatStateOf(0f) }
    val tuck = remember(activity.id) { Animatable(0f) }
    val cardSource = remember { MutableInteractionSource() }
    // Stage 68 aura, STAGE 72: now a CONSTANT glow (no drift clock at all) — the user asked
    // breathing animations to be replaced with a plain glow. Zero wakeups while the banner rests.
    val auraCornerPx = (if (compactMode) 20f else 24f) * dens
    val dragState = rememberDraggableState { delta ->
        if (tuck.value == 0f) dragY = (dragY + delta).coerceIn(-300f * dens, 72f * dens)
    }

    LaunchedEffect(showReply) {
        IslandBus.replyOpen = showReply
        IslandAccessibilityService.setFocusable(showReply)
        if (showReply) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }
    DisposableEffect(activity.id) {
        onDispose {
            IslandBus.replyOpen = false
            IslandAccessibilityService.setFocusable(false)
        }
    }

    val width = if (compactMode) {
        compactWidthDp.coerceAtLeast(180).dp
    } else {
        standardWidthDp.coerceAtLeast(240).dp
    }

    Box(
        modifier = modifier
            .width(width)
            // Transform FIRST, clip second. The old order (clip -> graphicsLayer) clipped the
            // moving content against the card's ORIGINAL bounds, so the card was cut off while
            // sliding and a full copy re-appeared when the offset was reset. Now the whole
            // clipped card travels as one piece.
            .graphicsLayer {
                shadowElevation = 0f
                transformOrigin = TransformOrigin(0.5f, 0f)
                val e = tuck.value
                val w = size.width.coerceAtLeast(1f)
                val h = size.height.coerceAtLeast(1f)
                // Downward drag resists; upward follows the finger 1:1. The finger offset is
                // blended out as the tuck takes over, so there is no jump at release.
                val finger = if (dragY > 0f) dragY * 0.18f else dragY
                translationY = finger * (1f - e) - collapseTravelDp * density * e
                translationX = swipeOffset
                val fx = (kotlin.math.abs(swipeOffset) / w).coerceIn(0f, 1f)
                val up = (-dragY / (h * 1.4f)).coerceIn(0f, 1f)
                val calm = if (lowPowerMode) 0f else 1f
                // Entrance: the card itself now fades/grows in with the drop (it used to pop in
                // at full opacity and only its contents were staggered).
                val en = androidx.compose.animation.core.FastOutSlowInEasing.transform((enter.value / 0.42f).coerceIn(0f, 1f))
                val enterScale = 1f - (1f - en) * 0.07f * calm
                scaleX = (1f - fx * 0.12f * calm - up * 0.04f * calm) * (1f - (1f - tuckScaleX(e)) * calm) * enterScale
                scaleY = (1f - fx * 0.06f * calm - up * 0.05f * calm) * (1f - (1f - tuckScaleY(e)) * calm) * enterScale
                val a = systemDismissAlpha.value * (1f - fx * fx) * (1f - up * 0.12f) * tuckAlpha(e) * en
                alpha = a
                // Flatten while translucent: with per-draw alpha the glass, photo and text show
                // through each other and read as a pale ghost box during the fade.
                compositingStrategy = if (a < 0.999f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            // Constant aura drawn behind the glass surface; the card itself never clips it
            // (no outer clip on this Box) and the window lane keeps >= 8 dp of margin for it.
            .then(
                if (!lowEndStatic && !isGhost) Modifier.staticAura(
                    color = accent,
                    maxAlpha = 0.30f,
                    spreadDp = 5f,
                    cornerRadiusPx = auraCornerPx
                ) else Modifier
            )
            // No outer clip: a clip after graphicsLayer cut the stagger overshoot (jelly) at the
            // card edge. The glass and artwork below clip themselves to the same shape.
            .then(
                if (isGhost) Modifier else Modifier
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val commit = dragY < -(34f * dens) || (velocity < -1100f && dragY < -(8f * dens))
                            if (commit) {
                                val fast = velocity < -2200f
                                tuck.animateTo(
                                    1f,
                                    tween(
                                        durationMillis = if (lowPowerMode) 200 else if (fast) 340 else BANNER_TUCK_MS,
                                        easing = BannerTuckEasing
                                    )
                                )
                                dragY = 0f // invisible now (tuck = 1), so no visual jump
                                onSwipeUp()
                                // If nothing took over (state unchanged), bring the card back
                                // rather than leaving an invisible, untouchable banner.
                                delay(600)
                                tuck.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
                            } else {
                                Animatable(dragY).animateTo(
                                    0f,
                                    spring(dampingRatio = 0.72f, stiffness = 300f),
                                    initialVelocity = velocity
                                ) { dragY = value }
                            }
                        }
                    )
                    .pointerInput(activity.id) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                // Finger-tracked, both directions. snapTo avoids per-frame spring lag.
                                // Pointer movement is already delivered on the UI thread.
                                // Do not launch one coroutine per motion event: that backlog
                                // caused visible lag when battery saver and Ultra were active.
                                val limit = size.width * 0.55f
                                swipeOffset = (swipeOffset + amount).coerceIn(-limit, limit)
                            },
                            onDragEnd = {
                                if (kotlin.math.abs(swipeOffset) > size.width * 0.28f) {
                                    gestureScope.launch {
                                        val direction = if (swipeOffset < 0f) -1f else 1f
                                        val anim = Animatable(swipeOffset)
                                        anim.animateTo(
                                            direction * size.width * 1.4f,
                                            spring(dampingRatio = 1f, stiffness = 320f)
                                        ) { swipeOffset = value }
                                        onDismiss()
                                    }
                                } else {
                                    gestureScope.launch {
                                        val anim = Animatable(swipeOffset)
                                        anim.animateTo(
                                            0f,
                                            spring(dampingRatio = 0.74f, stiffness = 300f)
                                        ) { swipeOffset = value }
                                    }
                                }
                            },
                            onDragCancel = {
                                gestureScope.launch {
                                    val anim = Animatable(swipeOffset)
                                    anim.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.74f, stiffness = 300f)
                                    ) { swipeOffset = value }
                                }
                            }
                        )
                    }
                    // No indication: the default ripple painted a pale rectangle over the whole
                    // card whenever a swipe started, which is the "faint white box" on dismiss.
                    .clickable(interactionSource = cardSource, indication = null) { onTap() }
            )
    ) {
        // Same shared-clock pattern as the expanded media card: one drift clock feeds photo + rim.
        val driftPhase = rememberDriftPhase(animate = false)
        // Seluruh permukaan (latar artwork + kaca + tepi) dibakar jadi satu bitmap (BakedSurface).
        // Teks dan tombol tetap digambar langsung di atasnya.
        if (drawSurface) BakedSurface(
            keyBase = "wa|${activity.id}|$accent|$themeMode|$edgeLensStrength|$backgroundOpacity|${compactMode}|${activity.avatar?.hashCode() ?: 0}|${showAlbumBackground}|${lowEndStatic}",
            modifier = Modifier.matchParentSize()
        ) {
            if (com.android.island.core.DeviceProfile.staticGlass) {
                // Low-End WhatsApp uses a true OLED-black surface. The contact avatar remains
                // available through the small foreground Avatar() composable, never as a backdrop.
                Box(Modifier.matchParentSize().clip(shape).background(Color(0xFF000000)))
            } else {
                if (showAlbumBackground) {
                    FluidGlassBackdrop(
                        art = art,
                        accent = accent,
                        photoAlpha = if (compactMode) 0.78f else 0.8f,
                        animate = false,
                        modifier = Modifier.matchParentSize().clip(shape),
                        opacity = backgroundOpacity,
                        externalPhase = driftPhase,
                        lensStrength = edgeLensStrength,
                        lensCornerDp = if (compactMode) 20f else 24f,
                        frostStrength = glassConfig.frostStrength,
                        waveStrength = 0f
                    )
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .glassBody(GlassProfile.BANNER, glassConfig, accent, shape, edgeLensStrength, themeMode == "WHITE")
                )
                Box(Modifier.matchParentSize().glassEdge(GlassProfile.BANNER, if (compactMode) 20f else 24f))
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (compactMode) Modifier.heightIn(min = compactHeightDp.dp) else Modifier.heightIn(min = standardHeightDp.dp))
                .padding(horizontal = if (compactMode) 14.dp else 18.dp, vertical = if (compactMode) 12.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 8.dp)
        ) {
            // STAGE 70: app identity strip on the standard banner — asal pesan terlihat jelas
            // sekali lirik ("dari WhatsApp"), tanpa menambah tinggi kartu compact.
            Row(
                Modifier.fillMaxWidth().staggerIn(enter, 0),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = remember(waIconBitmap) { waIconBitmap.asImageBitmap() },
                    contentDescription = "WhatsApp",
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
                OutlinedText(
                    "WhatsApp",
                    color = ink.copy(alpha = 0.62f),
                    outlineEnabled = false,
                    fontSize = if (compactMode) 11.sp else 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.staggerIn(enter, if (compactMode) 0 else 1)) {
                Avatar(activity, accent, compact = false, animate = !isGhost)
                Spacer(Modifier.width(if (compactMode) 8.dp else 10.dp))
                Column(Modifier.weight(1f)) {
                    OutlinedText(
                        text = if (activity.isGroup) {
                            "${activity.groupName ?: "Grup"} · ${activity.senderName}"
                        } else activity.senderName,
                        color = if (themeMode == "OUTLINE") OutlineTextFill else ink,
                        outlineEnabled = themeMode == "OUTLINE",
                        fontSize = bannerFontSize.coerceIn(10, 22).sp,
                        fontWeight = FontWeight(bannerFontWeight.coerceIn(300, 900)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedText(
                        text = activity.message,
                        color = if (themeMode == "OUTLINE") OutlineTextFill else ink,
                        outlineEnabled = themeMode == "OUTLINE",
                        fontSize = (bannerFontSize - 1).coerceIn(10, 20).sp,
                        fontWeight = FontWeight((bannerFontWeight - 100).coerceIn(300, 900)),
                        maxLines = when {
                            showFullMessage -> Int.MAX_VALUE
                            compactMode -> 2
                            showReply -> 4
                            else -> 2
                        },
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { messageCanExpand = it.hasVisualOverflow }
                    )
                }
                if (messageCanExpand || showFullMessage) {
                    PressIconButton(
                        onClick = { showFullMessage = !showFullMessage },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            if (showFullMessage) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                            contentDescription = if (showFullMessage) "Ringkas pesan" else "Lihat seluruh pesan",
                            tint = inkSoft
                        )
                    }
                }
                if (activity.messageCount > 1) {
                    Box(
                        Modifier.size(22.dp).graphicsLayer { scaleX = badgePop.value; scaleY = badgePop.value }.clip(CircleShape).background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activity.messageCount.toString(), color = ink, fontSize = (bannerFontSize - 3).coerceIn(8, 17).sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().staggerIn(enter, 2),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activity.markReadAction != null) {
                    // STAGE 70: labeled chips instead of an icon-only button —
                    // setiap aksi sekarang terbaca tanpa menebak arti ikon.
                    BannerActionChip(
                        label = "Dibaca",
                        icon = Icons.Rounded.DoneAll,
                        ink = inkSoft,
                        outlineEnabled = themeMode == "OUTLINE"
                    ) {
                        Controls.fireAction(context, activity.markReadAction)
                        IslandBus.clearWhatsApp(activity.id)
                        onDismiss()
                    }
                }
                if (activity.replyAction != null && !showReply) {
                    BannerActionChip(
                        label = "Balas",
                        icon = Icons.Rounded.Reply,
                        ink = replyTint,
                        outlineEnabled = themeMode == "OUTLINE"
                    ) {
                        IslandBus.replyOpen = true
                        showReply = true
                    }
                }
                val openSource2 = openSource
                Box(
                    Modifier
                        .pressScaleWith(openSource2)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent)
                        .clickable(interactionSource = openSource2, indication = null) { onTap() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Buka",
                        color = Color.White,
                        fontSize = (bannerFontSize - 2).coerceIn(8, 18).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(
                visible = showReply && activity.replyAction != null,
                enter = fadeIn(tween(180)) + expandVertically(spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(tween(120)) + shrinkVertically(spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium))
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).animateContentSize(animationSpec = tween(180))
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (darkArt) Color.Black.copy(0.28f) else Color.White.copy(0.34f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = ink, fontSize = bannerFontSize.coerceIn(10, 20).sp),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp).focusRequester(focusRequester),
                        decorationBox = { inner ->
                            if (replyText.isEmpty()) Text("Balas pesan…", color = ink.copy(0.48f), fontSize = bannerFontSize.coerceIn(10, 20).sp)
                            inner()
                        }
                    )
                    PressIconButton(
                        onClick = {
                            val ok = Controls.sendReply(context, activity.replyAction, activity.replyRemoteInputs, replyText)
                            if (ok) {
                                replyText = ""
                                showReply = false
                                // A successful inline reply resolves the WhatsApp notification
                                // stack as a whole. Clear all mirrored WA cards; message-level
                                // fingerprints in the listener prevent the source repost from
                                // resurrecting this reply or older cards. New messages still pass.
                                IslandBus.clearWhatsApp(activity.id)
                                onDismiss()
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.Send, "Kirim balasan", tint = if (replyText.isNotBlank()) accent else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(activity: IslandActivity.WhatsApp, accent: Color, compact: Boolean, animate: Boolean = true) {
    val pop = remember(activity.id) { Animatable(if (animate) 0.7f else 1f) }
    LaunchedEffect(activity.id) {
        if (animate) pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
    }
    if (activity.avatar != null) {
        Image(
            bitmap = androidx.compose.runtime.remember(activity.avatar) { activity.avatar.asImageBitmap() },
            contentDescription = null,
            modifier = Modifier.size(if (compact) 30.dp else 42.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier.size(if (compact) 30.dp else 42.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Chat, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** STAGE 70: labeled, quiet action chip for the standard banner (Dibaca / Balas). */
@Composable
private fun BannerActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    ink: Color,
    outlineEnabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Row(
        Modifier
            .pressScaleWith(source)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (outlineEnabled) OutlineTextFill else ink, modifier = Modifier.size(14.dp))
        OutlinedText(
            label,
            color = if (outlineEnabled) OutlineTextFill else ink,
            outlineEnabled = outlineEnabled,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
