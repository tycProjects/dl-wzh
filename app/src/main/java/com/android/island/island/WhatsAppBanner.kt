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
    compactMode: Boolean = false,
    compactWidthDp: Int = 420,
    compactHeightDp: Int = 58,
    lowPowerMode: Boolean = false,
    standardWidthDp: Int = 360,
    standardHeightDp: Int = 88,
    bannerFontSize: Int = 14,
    bannerFontWeight: Int = 600,
    backgroundOpacity: Float = 0.92f,
    edgeLensStrength: Float = 0.6f,
    themeMode: String = "ADAPTIVE",
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
    onSwipeUp: () -> Unit = {},
    /** How far (dp) the card travels up while it is tucked into the capsule. */
    collapseTravelDp: Float = 30f,
    /** Non-interactive fading copy (timeout exit). Skips entrance pops and all gestures. */
    isGhost: Boolean = false
) {
    val shape = RoundedCornerShape(if (compactMode) 20.dp else 24.dp)
    val accent = Color(0xFF25D366)
    // Blurred profile photo + adaptive ink (white on dark photo, black on light/no photo).
    val art = rememberArtBackdrop(activity.avatar, enabled = showAlbumBackground).value
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
    val enter = remember(activity.id) { Animatable(if (lowPowerMode || isGhost) 1f else 0f) }
    LaunchedEffect(activity.id) {
        if (enter.value < 1f) enter.animateTo(1f, tween(480, easing = ExpoOut))
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
            .clip(shape)
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
                                    spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
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
                                            spring(dampingRatio = 1f, stiffness = 380f)
                                        ) { swipeOffset = value }
                                        onDismiss()
                                    }
                                } else {
                                    gestureScope.launch {
                                        val anim = Animatable(swipeOffset)
                                        anim.animateTo(
                                            0f,
                                            spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow)
                                        ) { swipeOffset = value }
                                    }
                                }
                            },
                            onDragCancel = {
                                gestureScope.launch {
                                    val anim = Animatable(swipeOffset)
                                    anim.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow)
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
        if (showAlbumBackground && activity.avatar != null) {
            FluidGlassBackdrop(
                art = art,
                accent = accent,
                photoAlpha = if (compactMode) 0.78f else 0.85f,
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
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (compactMode) Modifier.heightIn(min = compactHeightDp.dp) else Modifier.heightIn(min = standardHeightDp.dp))
                .padding(horizontal = if (compactMode) 12.dp else 18.dp, vertical = if (compactMode) 8.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactMode) 4.dp else 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.staggerIn(enter, 0)) {
                Avatar(activity, accent, compactMode, animate = !isGhost)
                Spacer(Modifier.width(if (compactMode) 8.dp else 10.dp))
                Column(Modifier.weight(1f)) {
                    OutlinedText(
                        text = if (activity.isGroup) {
                            "${activity.groupName ?: "Grup"} · ${activity.senderName}"
                        } else activity.senderName,
                        color = if (themeMode == "OUTLINE") OutlineTextFill else ink,
                        outlineEnabled = themeMode == "OUTLINE",
                        fontSize = (bannerFontSize - if (compactMode) 2 else 0).coerceIn(8, 20).sp,
                        fontWeight = FontWeight(bannerFontWeight.coerceIn(300, 900)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedText(
                        text = activity.message,
                        color = if (themeMode == "OUTLINE") OutlineTextFill else ink,
                        outlineEnabled = themeMode == "OUTLINE",
                        fontSize = (bannerFontSize - if (compactMode) 3 else 1).coerceIn(8, 20).sp,
                        fontWeight = FontWeight((bannerFontWeight - 100).coerceIn(300, 900)),
                        maxLines = when {
                            showFullMessage -> Int.MAX_VALUE
                            compactMode -> 1
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
                if (compactMode && activity.replyAction != null && !showReply) {
                    PressIconButton(onClick = { showReply = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Reply, "Balas pesan", tint = replyTint)
                    }
                }
            }

            if (!compactMode) Row(
                Modifier.fillMaxWidth().staggerIn(enter, 1),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activity.markReadAction != null) {
                    PressIconButton(
                        onClick = {
                            Controls.fireAction(context, activity.markReadAction)
                            IslandBus.clearWhatsApp(activity.id)
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.DoneAll, "Tandai dibaca", tint = inkSoft)
                    }
                }
                if (activity.replyAction != null && !showReply) {
                    androidx.compose.material3.TextButton(
                        onClick = { showReply = true },
                        modifier = Modifier.pressScaleWith(replySource),
                        interactionSource = replySource,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.Reply, null, tint = replyTint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        OutlinedText("Balas", color = if (themeMode == "OUTLINE") OutlineTextFill else ink, outlineEnabled = themeMode == "OUTLINE", fontSize = (bannerFontSize - 2).coerceIn(8, 18).sp)
                    }
                }
                androidx.compose.material3.TextButton(
                    onClick = onTap,
                    modifier = Modifier.pressScaleWith(openSource),
                    interactionSource = openSource,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    OutlinedText("Buka", color = if (themeMode == "OUTLINE") OutlineTextFill else ink, outlineEnabled = themeMode == "OUTLINE", fontSize = (bannerFontSize - 2).coerceIn(8, 18).sp, fontWeight = FontWeight.SemiBold)
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
