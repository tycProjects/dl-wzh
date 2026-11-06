package com.android.island.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import android.os.SystemClock
import com.android.island.util.IslandDiagnostics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import com.android.island.core.Controls
import com.android.island.core.IslandActivity
import com.android.island.core.IslandBus
import com.android.island.core.IslandState
import com.android.island.core.CutoutProfile
import com.android.island.core.CutoutType
import com.android.island.core.IslandUiState
import com.android.island.core.TapAction
import com.android.island.data.IslandSettings
import com.android.island.data.AnimationOption
import kotlin.math.roundToInt
import kotlin.math.abs

/**
 * Layered island renderer. The capsule anchor, expanded panel, and notification
 * banner have separate positioning/animation channels. Expanded progress controls
 * only the capsule visibility and panel reveal; it never changes capsule geometry.
 */
@Composable
fun IslandRoot(
    state: IslandUiState,
    settings: IslandSettings,
    topOffsetDp: Int = 0,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onOutsideTap: () -> Unit,
    measuredScreenWidthDp: Int = 0
) {
    val islandState = state.islandState
    val context = LocalContext.current
    val reducedMotion = settings.lowPowerMode
    // One gesture dispatcher for every state (compact, banner, expanded layer), so the Tap /
    // Double tap / Long press settings always apply. "Expand" means the right thing for the
    // context (banner media, WhatsApp stack, or the normal expand). Play/pause and next only make
    // sense for music, so they fall back to "expand" on chat/call capsules.
    val runGesture: (TapAction, IslandActivity, Boolean) -> Unit = { action, activity, inBanner ->
        val effective = if (activity !is IslandActivity.Media &&
            (action == TapAction.PLAY_PAUSE || action == TapAction.NEXT_TRACK)) TapAction.EXPAND else action
        if (effective == TapAction.EXPAND) {
            when {
                activity is IslandActivity.Media && state.pendingMedia != null -> IslandBus.expandMediaFromBanner()
                activity is IslandActivity.WhatsApp && inBanner -> IslandBus.expandWhatsAppStack()
                else -> onExpand()
            }
        } else {
            handleTap(effective, activity, onExpand, context)
        }
    }
    val hasDoubleTap = settings.doubleTapAction != TapAction.NOTHING
    val hasLongPress = settings.longPressAction != TapAction.NOTHING
    val current = state.current
    val pendingMedia = state.pendingMedia
    // Respect the detected cutout type instead of treating every detected cutout as
    // a waterdrop. Manual sharp-notch mode remains an explicit override.
    val useWaterDropShape = !settings.iphoneNotchEnabled && (
        settings.sharpNotch || state.cutout.type == CutoutType.WATERDROP
        )

    // Target progress: 0 compact/banner, 1 expanded
    val targetProgress = when (islandState) {
        IslandState.EXPANDED -> 1f
        else -> 0f
    }

    val progress = remember { Animatable(0f) }
    // Diagnostic samples are intentionally throttled: enough to identify geometry jumps
    // without turning the in-app recorder into a source of animation jank.
    LaunchedEffect(progress, islandState) {
        // Recorder off (default): do not even collect the per-frame progress flow.
        if (!IslandDiagnostics.isEnabled()) return@LaunchedEffect
        var lastSample = 0L
        // Start from the current animation value; -1f produced a false delta=2
        // every time this collector restarted during a close transition.
        var previous = progress.value
        snapshotFlow { progress.value }.collect { value ->
            val now = SystemClock.uptimeMillis()
            if (now - lastSample >= 100L || kotlin.math.abs(value - previous) >= 0.20f) {
                IslandDiagnostics.record("MORPH_SAMPLE", "state=$islandState progress=${"%.3f".format(value)} delta=${"%.3f".format(value - previous)}")
                lastSample = now
                previous = value
            }
        }
    }
    LaunchedEffect(islandState, current?.javaClass?.simpleName) {
        IslandDiagnostics.record("COMPOSE_STATE", "state=$islandState activity=${current?.javaClass?.simpleName} screen=${measuredScreenWidthDp}dp topOffset=${topOffsetDp}dp")
    }
    // Banner has its own entrance channel; it never participates in the capsule morph.
    val bannerOffsetY = remember { Animatable(-34f) }
    // Resolve the current WhatsApp notification identity before effects use it.
    val bannerKey = (current as? IslandActivity.WhatsApp)?.id
    // Auto-expire only the visible banner. The queued notifications remain in IslandBus,
    // and the capsule geometry is untouched; the side indicator becomes the entry point.
    LaunchedEffect(islandState, bannerKey, settings.whatsAppBannerDuration) {
        if (islandState == IslandState.BANNER && current is IslandActivity.WhatsApp) {
            val timeoutMs = settings.whatsAppBannerDuration.coerceIn(3, 60) * 1000L
            // Quick-reply is an active interaction: pause expiry while the input is open.
            // Once the user closes/sends the reply, grant the banner a fresh timeout.
            while (IslandBus.state.value.islandState == IslandState.BANNER &&
                (IslandBus.state.value.current as? IslandActivity.WhatsApp)?.id == bannerKey) {
                while (IslandBus.replyOpen &&
                    IslandBus.state.value.islandState == IslandState.BANNER &&
                    (IslandBus.state.value.current as? IslandActivity.WhatsApp)?.id == bannerKey) {
                    delay(100L)
                }
                if (IslandBus.state.value.islandState != IslandState.BANNER ||
                    (IslandBus.state.value.current as? IslandActivity.WhatsApp)?.id != bannerKey) break
                delay(timeoutMs)
                if (IslandBus.replyOpen) continue
                if (IslandBus.state.value.islandState == IslandState.BANNER &&
                    (IslandBus.state.value.current as? IslandActivity.WhatsApp)?.id == bannerKey) {
                    IslandBus.collapseWhatsAppBanner()
                }
                break
            }
        }
    }

    // ---- Banner exit ----
    // The BANNER branch is removed the moment the state flips, so a banner that merely times out
    // (collapses to the side indicator) used to vanish in one frame. We keep a lightweight copy
    // for ~220 ms and fade/lift it. Dismissed or opened chats are NOT replayed: their own
    // swipe/open motion already ran, and a ghost at the centre would look like a glitch.
    val bannerHold = remember { BannerHold() }
    if (islandState == IslandState.BANNER && current is IslandActivity.WhatsApp) bannerHold.last = current
    var exitingBanner by remember { mutableStateOf<IslandActivity.WhatsApp?>(null) }
    val bannerExit = remember { Animatable(0f) }
    LaunchedEffect(islandState) {
        if (islandState == IslandState.BANNER) { exitingBanner = null; bannerHold.skipExit = false; return@LaunchedEffect }
        val last = bannerHold.last ?: return@LaunchedEffect
        bannerHold.last = null
        // A swipe-up already played the identical tuck animation on the live banner. Replaying it
        // here put a second, paler copy back at full opacity = the "faint white box" on dismiss.
        val alreadyTucked = bannerHold.skipExit
        bannerHold.skipExit = false
        val stillQueued = IslandBus.state.value.whatsAppItems.any { it.id == last.id }
        if (islandState == IslandState.COMPACT && stillQueued && !reducedMotion && !alreadyTucked) {
            try {
                exitingBanner = last
                bannerExit.snapTo(0f)
                // Same curve + endpoints as the swipe-up tuck (see IslandMotion.kt).
                bannerExit.animateTo(1f, tween(BANNER_TUCK_MS, easing = BannerTuckEasing))
            } finally {
                exitingBanner = null
            }
        }
    }

    // Banner entrance only when entering BANNER (or switching WA id).
    // Do NOT depend on media metadata updates — that caused stutter.
    LaunchedEffect(islandState, bannerKey, settings.animationSpeed, settings.expandStyle, settings.elasticity) {
        if (islandState == IslandState.BANNER) {
            bannerOffsetY.snapTo(-26f)
            bannerOffsetY.animateTo(0f, islandSpring(settings, false))
        } else {
            // Instant reset when leaving banner — no reverse animation that fights morph.
            bannerOffsetY.snapTo(0f)
        }
    }
    // Keep the latest motion preferences without restarting an in-flight transition.
    // Re-keying this effect on every settings change cancelled and recreated the
    // animation coroutine, which could discard velocity and produce a tiny hitch.
    val latestSettings by rememberUpdatedState(settings)
    // One transition owner only. Animatable retargets from its current value when
    // the destination changes, preserving a continuous transition under rapid taps.
    LaunchedEffect(targetProgress) {
        if (abs(progress.value - targetProgress) > 0.001f) {
            val spec = if (targetProgress == 0f) islandCloseSpring(latestSettings.copy(animationSpeed = latestSettings.animationSpeed * 0.8f))
            else islandSpring(latestSettings.copy(animationSpeed = latestSettings.animationSpeed * 0.8f), false)
            progress.animateTo(targetProgress, spec)
        }
        // Spring overshoot can leave a tiny residue; land exactly on the target.
        if (progress.value != targetProgress) progress.snapTo(targetProgress)
    }

    // Appearance scale: pop-in when leaving HIDDEN
    val appear = remember { Animatable(0f) }
    val hasActiveContent = islandState != IslandState.HIDDEN && current != null
    val showIdleNotch = settings.iphoneNotchEnabled && !settings.hideNotchWhenIdle && islandState == IslandState.HIDDEN
    val isVisible = hasActiveContent || showIdleNotch
    LaunchedEffect(isVisible) {
        if (isVisible) {
            appear.snapTo(0.88f)
            appear.animateTo(1f, islandSpring(settings, false))
        } else {
            appear.snapTo(0f)
        }
    }

    if (!isVisible) return

    // Keep the physical capsule anchor fixed for every state. The previous code
    // translated the entire root by expandedOffsetY, moving the capsule together
    // with the expanding card. Expanded content now grows from the same top anchor.
    // progress is animated every frame. Reading it here would recompose this whole tree on
    // every frame of the morph (the main cause of choppy open/close). Composition only sees
    // the three boolean thresholds below; the per-frame values are read in graphicsLayer.
    val panelShown by remember { derivedStateOf { progress.value > 0.001f } }
    val hostWide by remember { derivedStateOf { progress.value > 0.01f } }
    // A notification banner must never animate the capsule itself. Read the
    // animated appearance value in the layer phase (below), not during composition;
    // reading Animatable.value here invalidated the entire island tree on every frame.
    val isBannerState = islandState == IslandState.BANNER

    // --- Size morph (lerp compact ↔ expanded) ---
    val density = context.resources.displayMetrics.density
    val screenW = (measuredScreenWidthDp.takeIf { it > 0 }
        ?: (context.resources.displayMetrics.widthPixels / density).roundToInt()).coerceAtLeast(240)
    // Notch size for the chosen cutout profile (AUTO follows what the screen reports).
    val notchSize = CutoutProfile.resolve(
        settings.cutoutProfile,
        state.cutout.type,
        settings.iphoneNotchWidth,
        settings.iphoneNotchHeight
    )
    val compactW = notchSize.first.dp.coerceIn(88.dp, 480.dp)
    // Wider expanded target — limited only by screen margins
    val expandedW = settings.expandedWidth
        .dp
        .coerceIn(180.dp, screenW.coerceAtLeast(180).dp)
    val compactH = notchSize.second.dp.coerceIn(24.dp, 64.dp)
    // Keep the capsule and its external 20dp WhatsApp bubble inside the display bounds.
    val maxHorizontalOffset = ((screenW.dp - compactW) / 2f - 28.dp).coerceAtLeast(0.dp)
    val horizontalOffset = settings.offsetX.dp.coerceIn(-maxHorizontalOffset, maxHorizontalOffset)
    val configuredExpandedH = if (settings.expandedMinHeight > 0) {
        settings.expandedMinHeight.dp
    } else 118.dp
    // Give a multi-chat stack enough room to reveal its cards; cap the scroll surface
    // so a burst of messages never creates an unbounded overlay window.
    // Lyrics may wrap to two lines; reserve the extra line so the card grows instead of squashing.
    val lyricExtraH = if (settings.showLyrics && current is IslandActivity.Media)
        (settings.lyricFontSize.coerceIn(9, 22) * 1.25f).dp else 0.dp
    val stackMode = current is IslandActivity.WhatsApp && state.whatsAppItems.size > 1
    // The stack measures its own content (see ExpandedIsland). Until the first layout we use an
    // estimate. It never exceeds 78% of the screen; beyond that the list scrolls.
    val waStackCapDp = (LocalConfiguration.current.screenHeightDp * 0.78f).dp
    val expandedH = if (stackMode) {
        val measured = if (state.whatsAppStackHeightDp > 0) state.whatsAppStackHeightDp
            else state.whatsAppItems.size * 112 + 16
        maxOf(configuredExpandedH, measured.dp).coerceAtMost(waStackCapDp)
    } else configuredExpandedH + lyricExtraH
    LaunchedEffect(islandState, compactW, compactH, expandedW, expandedH, horizontalOffset, settings.expandedOffsetY) {
        IslandDiagnostics.record(
            "ISLAND_GEOMETRY",
            "state=$islandState compact=${compactW.value}x${compactH.value}dp expanded=${expandedW.value}x${expandedH.value}dp xOffset=${horizontalOffset.value}dp expandedOffsetY=${settings.expandedOffsetY}dp"
        )
    }

    // Keep the overlay window tightly bounded to interactive content. A full-width
    // transparent accessibility overlay can intercept taps intended for other apps.
    // The window remains gravity-centered; changing its width does not move the island
    // anchor, while the banner's own entrance stays on its independent Y animation.
    val sideAllowance = if (current is IslandActivity.WhatsApp && state.whatsAppItems.isNotEmpty()) 56.dp else 0.dp
    val compactSceneWidth = (compactW + sideAllowance + abs(horizontalOffset.value).dp * 2f)
        .coerceAtMost(screenW.dp)
    val bannerContentWidth = maxOf(settings.compactBannerWidth, settings.standardBannerWidth)
        .coerceIn(180, (screenW - 8).coerceAtLeast(180)).dp + 8.dp
    // Keep the measured overlay width stable while a WhatsApp item moves between
    // compact/banner/expanded. WindowManager re-centers WRAP_CONTENT windows on
    // every width change; that was the source of the capsule's visible side-jump.
    // A WhatsApp host must not change width while the state morphs. WindowManager
    // centers WRAP_CONTENT windows again whenever width changes, which visibly drags
    // the capsule sideways on expand/collapse. Reserve the largest required width
    // (banner OR expanded card) for the entire WhatsApp lifecycle.
    val whatsappHostWidth = if (current is IslandActivity.WhatsApp) {
        maxOf(compactSceneWidth, bannerContentWidth, expandedW)
    } else compactSceneWidth
    // Width changes only once at the expanded-state boundary, never per animation
    // frame. Center gravity preserves the physical center while avoiding a wide
    // touchable window during ordinary compact playback.
    // Keep the drawing/touch host at one width for the whole activity lifecycle.
    // A WRAP_CONTENT window that changes width at the EXPANDED boundary can be
    // re-laid out by WindowManager between frames. Center gravity is not enough to
    // prevent the one-frame stale-surface jump on several OEM builds.
    // Keep the compact touch window narrow so swipes starting elsewhere on the
    // status bar can reach Quick Settings. Expand the host only while the card or
    // wide WhatsApp banner is actually visible; center gravity preserves the notch anchor.
    // Reserve the expanded width throughout the entire morph, including closing.
    // The previous state-only switch shrank 600dp -> 244dp immediately on
    // COMPACT, before progress reached zero; centered WRAP_CONTENT windows can
    // then recenter and visibly drag the capsule. Release width after morph end.
    val expandedHostWidth = maxOf(compactSceneWidth, expandedW).coerceAtMost(screenW.dp)
    val sceneWidthDp = when {
        islandState == IslandState.BANNER -> maxOf(compactSceneWidth, bannerContentWidth).coerceAtMost(screenW.dp)
        islandState == IslandState.EXPANDED || hostWide -> expandedHostWidth
        else -> compactSceneWidth
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = topOffsetDp.dp)
            .graphicsLayer {
                clip = false
                // Keep frame-by-frame animation reads in the graphics layer. This
                // schedules layer updates instead of recomposing and remeasuring all
                // descendants (media artwork, lyrics, notification cards) every frame.
                val appearScale = if (isBannerState) 1f else appear.value
                scaleX = appearScale
                scaleY = appearScale
                alpha = appearScale.coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.TopCenter
    ) {
        when (islandState) {
            IslandState.HIDDEN -> {
                if (settings.iphoneNotchEnabled && !settings.hideNotchWhenIdle) {
                    Box(
                        Modifier
                            .width(compactW)
                            .height(compactH)
                            .clip(iPhoneNotchShape())
                            .background(androidx.compose.ui.graphics.Color.Black)
                    )
                }
            }
            IslandState.BANNER -> {
                val availableBannerWidth = screenW.coerceAtLeast(188)
                val waTopGap = compactH + 8.dp
                // Capsule layer — absolute position, never moves when banner enters.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = horizontalOffset)
                        .width(compactW)
                        .height(compactH),
                    contentAlignment = Alignment.Center
                ) {
                    if (pendingMedia == null) {
                        (current as? IslandActivity.WhatsApp)?.let { wa ->
                            CompactIsland(
                                activity = wa.copy(messageCount = state.whatsAppItems.size.coerceAtLeast(1)),
                                waterDrop = useWaterDropShape,
                                iPhoneNotch = settings.iphoneNotchEnabled,
                                widthDp = compactW,
                                heightDp = compactH,
                                swipeTracks = false,
                                swipeExpand = true,
                                onTap = { runGesture(settings.tapAction, wa, true) },
                                onDoubleTap = { runGesture(settings.doubleTapAction, wa, true) },
                                onLongPress = { runGesture(settings.longPressAction, wa, true) },
                                onSwipeUp = { if (pendingMedia != null) IslandBus.expandMediaFromBanner() else IslandBus.expandWhatsAppStack() },
                                onHorizontalDismiss = { IslandBus.clearWhatsApp() },
                                allowHorizontalGesture = false,
                                allowSwipeDismiss = false,
                                themeMode = settings.islandTheme.name,
                                hapticsEnabled = settings.hapticsEnabled,
                                hasDoubleTap = hasDoubleTap,
                                hasLongPress = hasLongPress
                            )
                        }
                    }
                    pendingMedia?.let { media ->
                        CompactIsland(
                            activity = media,
                            waterDrop = useWaterDropShape,
                            iPhoneNotch = settings.iphoneNotchEnabled,
                            widthDp = compactW,
                            heightDp = compactH,
                            swipeTracks = settings.swipeTracks,
                            swipeExpand = settings.swipeExpand,
                            onTap = { runGesture(settings.tapAction, media, true) },
                            onDoubleTap = { runGesture(settings.doubleTapAction, media, true) },
                            onLongPress = { runGesture(settings.longPressAction, media, true) },
                            onSwipeUp = { if (settings.swipeExpand) IslandBus.expandMediaFromBanner() },
                            onHorizontalDismiss = { IslandBus.forceDismissMedia() },
                            allowHorizontalGesture = true,
                            allowSwipeDismiss = false,
                            themeMode = settings.islandTheme.name,
                            hapticsEnabled = settings.hapticsEnabled,
                            hasDoubleTap = hasDoubleTap,
                            hasLongPress = hasLongPress
                        )
                    }
                }
                // Unread WhatsApp marker — independent of capsule size, left by default.
                if (state.whatsAppItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(
                                x = horizontalOffset + if (settings.whatsAppIndicatorLeft) {
                                    -(compactW / 2 + 18.dp)
                                } else {
                                    compactW / 2 + 18.dp
                                },
                                y = (compactH - 20.dp) / 2
                            )
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { IslandBus.expandWhatsAppStack() }
                            .background(androidx.compose.ui.graphics.Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        WhatsAppGlyph()
                    }
                }
                (current as? IslandActivity.Call)?.let { call ->
                    CallBanner(
                        call = call,
                        glassConfig = state.glassConfig,
                        showAlbumBackground = settings.showAlbumBackground,
                        compactMode = settings.compactBanner,
                        compactWidthDp = settings.compactBannerWidth.coerceIn(180, 900),
                        compactHeightDp = settings.compactBannerHeight,
                        standardWidthDp = settings.standardBannerWidth.coerceIn(200, 900),
                        standardHeightDp = settings.standardBannerHeight,
                        bannerFontSize = settings.bannerFontSize,
                        bannerFontWeight = settings.bannerFontWeight,
                        backgroundOpacity = settings.backgroundOpacity,
                        edgeLensStrength = settings.edgeLensStrength,
                        lowPowerMode = reducedMotion,
                        onOpen = { handleTap(TapAction.OPEN_APP, call, {}, context) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = compactH + 8.dp)
                    )
                }
                // Banner layer — screen-centered, independent Y entrance, wide travel room.
                (current as? IslandActivity.WhatsApp)?.let { wa ->
                    WhatsAppBanner(
                        activity = wa,
                        glassConfig = state.glassConfig,
                        showAlbumBackground = settings.showAlbumBackground,
                        compactMode = settings.compactBanner,
                        backgroundOpacity = settings.backgroundOpacity,
                        edgeLensStrength = settings.edgeLensStrength,
                        themeMode = settings.islandTheme.name,
                        // Allow banner almost full-width (8dp margin each side).
                        // Slider max 560 is honored when the display is wide enough.
                        compactWidthDp = settings.compactBannerWidth.coerceIn(
                            180,
                            (availableBannerWidth - 8).coerceAtLeast(180)
                        ),
                        compactHeightDp = settings.compactBannerHeight,
                        standardHeightDp = settings.standardBannerHeight,
                        bannerFontSize = settings.bannerFontSize,
                        bannerFontWeight = settings.bannerFontWeight,
                        lowPowerMode = reducedMotion,
                        standardWidthDp = settings.standardBannerWidth.coerceIn(
                            200,
                            (availableBannerWidth - 8).coerceAtLeast(200)
                        ),
                        onTap = {
                            // Open only the tapped chat, and remove just that card once it has opened.
                            val pkg = if (wa.conversationKey.contains("com.whatsapp.w4b")) "com.whatsapp.w4b" else "com.whatsapp"
                            if (com.android.island.core.Controls.openChat(context, wa.contentIntent, pkg)) {
                                IslandBus.clearWhatsApp(wa.id)
                            }
                        },
                        onDismiss = { IslandBus.clearWhatsApp(wa.id) },
                        onSwipeUp = {
                            bannerHold.skipExit = true
                            IslandBus.collapseWhatsAppBanner()
                        },
                        collapseTravelDp = compactH.value / 2f + 8f,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = waTopGap)
                            .graphicsLayer {
                                translationY = bannerOffsetY.value * density
                                clip = false
                            }
                    )
                }
            }
            else -> {
                // COMPACT or EXPANDED (and mid-morph)
                current?.let { act ->
                    if (islandState == IslandState.COMPACT && !panelShown) {
                        // Compact window stays close to the capsule to preserve touch pass-through.
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(x = horizontalOffset)
                                .width(compactW)
                                .height(compactH),
                            contentAlignment = Alignment.Center
                        ) {
                            val capsuleActivity = pendingMedia ?: act
                            CompactIsland(
                                activity = capsuleActivity,
                                waterDrop = useWaterDropShape,
                                iPhoneNotch = settings.iphoneNotchEnabled,
                                widthDp = compactW,
                                heightDp = compactH,
                                // Horizontal swipe is enabled only for the media capsule.
                                // It sends next/previous commands without translating the capsule.
                                swipeTracks = settings.swipeTracks && capsuleActivity is IslandActivity.Media,
                                swipeExpand = settings.swipeExpand,
                                onTap = { runGesture(settings.tapAction, capsuleActivity, false) },
                                onDoubleTap = { runGesture(settings.doubleTapAction, capsuleActivity, false) },
                                onLongPress = { runGesture(settings.longPressAction, capsuleActivity, false) },
                                onSwipeUp = {
                                    if (settings.swipeExpand) {
                                        if (pendingMedia != null) IslandBus.expandMediaFromBanner() else onExpand()
                                    } else onCollapse()
                                },
                                onHorizontalDismiss = {},
                                allowHorizontalGesture = capsuleActivity is IslandActivity.Media,
                                allowSwipeDismiss = false,
                                themeMode = settings.islandTheme.name,
                                hapticsEnabled = settings.hapticsEnabled,
                                hasDoubleTap = hasDoubleTap,
                                hasLongPress = hasLongPress
                            )
                        }
                        // Keep the unread marker visible in COMPACT when WhatsApp is queued.
                        if (state.whatsAppItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(
                                        x = horizontalOffset + if (settings.whatsAppIndicatorLeft) {
                                            -(compactW / 2 + 18.dp)
                                        } else {
                                            compactW / 2 + 18.dp
                                        },
                                        y = (compactH - 20.dp) / 2
                                    )
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { IslandBus.expandWhatsAppStack() }
                                    .background(androidx.compose.ui.graphics.Color(0xFF25D366)),
                                contentAlignment = Alignment.Center
                            ) {
                                WhatsAppGlyph()
                            }
                        }
                    } else {
                        // EXPANDED is a separate layer, not a resize of the capsule.
                        // `progress` is its own open/close controller: the capsule fades
                        // out at its fixed notch anchor while the card appears below it.
                        // On collapse, the same controller reverses, restoring the capsule
                        // at exactly the same X/Y instead of animating it toward an edge.
                        val expandedOffset = settings.expandedOffsetY.coerceIn(-compactH.value.toInt(), 240).dp
                        val panelTop = compactH + expandedOffset
                        // Jelly overshoot grows the panel past its resting height. The overlay window wraps its
                        // content, so reserve a little room below or the stretch is cut flat (which is why
                        // the old jelly looked stiff).
                        val jellySlack = if (settings.animationOption == AnimationOption.OPTION_3 && !reducedMotion) JELLY_SLACK_DP.dp else 0.dp
                        val stageHeight = (panelTop.coerceAtLeast(compactH) + (if (stackMode) waStackCapDp else expandedH) + jellySlack)
                            .coerceAtLeast(compactH)
                        Box(
                            modifier = Modifier
                                .width(sceneWidthDp)
                                .height(stageHeight),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Capsule remains at the original anchor for the whole
                            // transition, but is fully hidden once expansion completes.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(x = horizontalOffset)
                                    .width(compactW)
                                    .height(compactH)
                                    .graphicsLayer {
                                        // Capsule fades out during the first 40% of the opening and
                                        // fades back in during the last 40% of the closing, with a
                                        // tiny scale so it feels like it melts into / out of the panel.
                                        val t = (1f - progress.value / 0.4f).coerceIn(0f, 1f)
                                        alpha = t
                                        val sc = 0.9f + 0.1f * t
                                        scaleX = sc
                                        scaleY = sc
                                        transformOrigin = TransformOrigin(0.5f, 0f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CompactIsland(
                                    activity = act,
                                    waterDrop = useWaterDropShape,
                                    iPhoneNotch = settings.iphoneNotchEnabled,
                                    widthDp = compactW,
                                    heightDp = compactH,
                                    swipeTracks = settings.swipeTracks,
                                    swipeExpand = settings.swipeExpand,
                                    onTap = { runGesture(settings.tapAction, act, false) },
                                    onDoubleTap = { runGesture(settings.doubleTapAction, act, false) },
                                    onLongPress = { runGesture(settings.longPressAction, act, false) },
                                    onSwipeUp = { if (settings.swipeExpand) onExpand() else onCollapse() },
                                    onHorizontalDismiss = { IslandBus.forceDismissMedia() },
                                    allowHorizontalGesture = true,
                                    allowSwipeDismiss = false,
                                    themeMode = settings.islandTheme.name,
                                    hapticsEnabled = settings.hapticsEnabled,
                                    hasDoubleTap = hasDoubleTap,
                                    hasLongPress = hasLongPress
                                )
                            }
                            if (panelShown) {
                                val jellyOption = settings.animationOption == AnimationOption.OPTION_3
                                val opening = islandState == IslandState.EXPANDED
                                val flowOption = settings.animationOption == AnimationOption.OPTION_2 || jellyOption
                                val startCorner = compactH.value / 2f
                                val endCorner = settings.expandedCornerRadius.toFloat()
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = panelTop)
                                        .width(expandedW)
                                        .then(if (stackMode) Modifier.heightIn(min = expandedH, max = waStackCapDp) else Modifier.height(expandedH))
                                        .graphicsLayer {
                                            // Everything that changes per frame lives here (layer phase):
                                            // no recomposition, no re-measure, GPU-only transform.
                                            val reveal = progress.value.coerceIn(0f, 1f)
                                            val cornerDp = if (flowOption) startCorner + (endCorner - startCorner) * reveal else endCorner
                                            shape = RoundedCornerShape(androidx.compose.foundation.shape.CornerSize(cornerDp * density))
                                            clip = true
                                            alpha = (reveal * (if (jellyOption) 1.8f else 2.2f)).coerceIn(0f, 1f)
                                            transformOrigin = TransformOrigin(0.5f, 0f)
                                            if (jellyOption) {
                                                // Jelly: same capsule morph as the others, but the spring overshoot is kept.
                                                // Overshoot past the full size stretches taller and squashes a little wider.
                                                val rx = (compactW.value / expandedW.value.coerceAtLeast(1f)).coerceIn(0.2f, 1f)
                                                val ry = (compactH.value / expandedH.value.coerceAtLeast(1f)).coerceIn(0.1f, 1f)
                                                val pr = progress.value.coerceIn(-0.05f, 1.25f)
                                                val squash = (pr - 1f).coerceAtLeast(0f) * 0.55f
                                                scaleX = (rx + (1f - rx) * pr - squash).coerceIn(0.3f, 1.12f)
                                                scaleY = ry + (1f - ry) * pr
                                                translationY = -panelTop.toPx() * (1f - pr)
                                            } else {
                                                // iOS-style: the glass grows out of the capsule (same size, same top edge) and
                                                // folds back into it. The corner radius and the content already follow 'reveal'.
                                                val rx = (compactW.value / expandedW.value.coerceAtLeast(1f)).coerceIn(0.2f, 1f)
                                                val ry = (compactH.value / expandedH.value.coerceAtLeast(1f)).coerceIn(0.1f, 1f)
                                                scaleX = rx + (1f - rx) * reveal
                                                scaleY = ry + (1f - ry) * reveal
                                                translationY = -panelTop.toPx() * (1f - reveal)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Content layer: fades/settles in after the glass has opened and
                                    // fades out first on close. Read in the layer phase only.
                                    Box(
                                        Modifier.graphicsLayer {
                                            val t = ((progress.value - 0.30f) / 0.55f).coerceIn(0f, 1f)
                                            alpha = t
                                            val sc = 0.94f + 0.06f * t
                                            scaleX = sc
                                            scaleY = sc
                                            translationY = (1f - t) * -6f * density
                                            transformOrigin = TransformOrigin(0.5f, 0f)
                                        }
                                    ) {
                                    LiquidReveal(
                                        state = act,
                                        contentKey = { it.id },
                                        enabled = settings.liquidExpand,
                                        blurDp = if (settings.fxBlur) 14f else 0f,
                                        shapeSpec = islandSpring(settings, false),
                                        fadeSpec = androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 1200f)
                                    ) { a ->
                                    ExpandedIsland(
                                        activity = a,
                                        whatsAppItems = state.whatsAppItems,
                                        glassConfig = state.glassConfig,
                                        showLyrics = settings.showLyrics,
                                        expandedWidthDp = expandedW.value.roundToInt(),
                                        expandedMinHeightDp = expandedH.value.roundToInt(),
                                        showAlbumBackground = settings.showAlbumBackground,
                                        lowPowerMode = reducedMotion,
                                        lyricShineEnabled = settings.lyricShineEnabled,
                                        lyricShineSpeed = settings.lyricShineSpeed,
                                        lyricShineIntensity = settings.lyricShineIntensity,
                                        lyricGlowIntensity = settings.lyricGlowIntensity,
                                        lyricGlowEnabled = settings.lyricGlowEnabled,
                                        lyricFontSize = settings.lyricFontSize,
                                        lyricFontWeight = settings.lyricFontWeight,
                                        compactWhatsAppBanner = settings.compactBanner,
                                        glassSweepEnabled = settings.glassSweepEnabled,
                                        glassSweepOpacity = settings.glassSweepOpacity,
                                        glassSweepDurationMs = settings.glassSweepDurationMs,
                                        backgroundOpacity = settings.backgroundOpacity,
                                        edgeLensStrength = settings.edgeLensStrength,
                                        bannerFontSize = settings.bannerFontSize,
                                        themeMode = settings.islandTheme.name,
                                        onStackHeightDp = { IslandBus.setWhatsAppStackHeight(it) },
                                        hapticsEnabled = settings.hapticsEnabled,
                                        onCollapse = onCollapse
                                    )
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Fading copy of the banner that just timed out (see "Banner exit" above). Non-interactive.
        exitingBanner?.let { wa ->
            val exitW = screenW.coerceAtLeast(188)
            WhatsAppBanner(
                activity = wa,
                glassConfig = state.glassConfig,
                showAlbumBackground = settings.showAlbumBackground,
                compactMode = settings.compactBanner,
                backgroundOpacity = settings.backgroundOpacity,
                edgeLensStrength = settings.edgeLensStrength,
                themeMode = settings.islandTheme.name,
                compactWidthDp = settings.compactBannerWidth.coerceIn(180, (exitW - 8).coerceAtLeast(180)),
                compactHeightDp = settings.compactBannerHeight,
                standardHeightDp = settings.standardBannerHeight,
                bannerFontSize = settings.bannerFontSize,
                bannerFontWeight = settings.bannerFontWeight,
                lowPowerMode = true,
                standardWidthDp = settings.standardBannerWidth.coerceIn(200, (exitW - 8).coerceAtLeast(200)),
                onTap = {},
                onDismiss = {},
                isGhost = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = compactH + 8.dp)
                    .graphicsLayer {
                        // Same tuck as the swipe-up path: glide up into the capsule while shrinking.
                        val e = bannerExit.value
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        translationY = -(compactH.value / 2f + 8f) * density * e
                        scaleX = tuckScaleX(e)
                        scaleY = tuckScaleY(e)
                        val a = tuckAlpha(e)
                        alpha = a
                        compositingStrategy = if (a < 0.999f) androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                        else androidx.compose.ui.graphics.CompositingStrategy.Auto
                    }
            )
        }
    }
}
private const val JELLY_SLACK_DP = 18f

private fun handleTap(action: TapAction, activity: IslandActivity, onExpand: () -> Unit, context: android.content.Context) {
    when (action) {
        TapAction.EXPAND -> onExpand()
        TapAction.PLAY_PAUSE -> com.android.island.core.Controls.playPause()
        TapAction.NEXT_TRACK -> com.android.island.core.Controls.next()
        TapAction.OPEN_APP -> when (activity) {
            is IslandActivity.Media -> if (activity.packageName.isNotBlank()) com.android.island.core.Controls.openApp(context, activity.packageName) else onExpand()
            is IslandActivity.Call -> Controls.fireAction(context, activity.contentIntent)
            is IslandActivity.WhatsApp -> {
                val openedByNotification = com.android.island.core.Controls.fireAction(context, activity.contentIntent)
                val opened = if (openedByNotification) true else com.android.island.core.Controls.openApp(
                    context,
                    if (activity.conversationKey.contains("com.whatsapp.w4b")) "com.whatsapp.w4b" else "com.whatsapp"
                )
                if (opened) IslandBus.clearWhatsApp()
            }
        }
        TapAction.NOTHING -> {}
    }
}


@Composable
private fun WhatsAppGlyph() {
    Canvas(Modifier.size(13.dp)) {
        val unit = size.minDimension / 24f
        val white = androidx.compose.ui.graphics.Color.White
        val bubble = Path().apply {
            moveTo(12f * unit, 2.2f * unit)
            cubicTo(6.7f * unit, 2.2f * unit, 2.4f * unit, 6.4f * unit, 2.4f * unit, 11.7f * unit)
            cubicTo(2.4f * unit, 13.6f * unit, 3f * unit, 15.3f * unit, 4.1f * unit, 16.8f * unit)
            lineTo(3.1f * unit, 21.2f * unit)
            lineTo(7.5f * unit, 20.1f * unit)
            cubicTo(8.9f * unit, 20.9f * unit, 10.4f * unit, 21.3f * unit, 12f * unit, 21.3f * unit)
            cubicTo(17.3f * unit, 21.3f * unit, 21.6f * unit, 17f * unit, 21.6f * unit, 11.7f * unit)
            cubicTo(21.6f * unit, 6.4f * unit, 17.3f * unit, 2.2f * unit, 12f * unit, 2.2f * unit)
            close()
        }
        drawPath(bubble, white, style = Stroke(width = 1.9f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val handset = Path().apply {
            moveTo(8.2f * unit, 7.2f * unit)
            cubicTo(7.7f * unit, 7.4f * unit, 7.2f * unit, 8.1f * unit, 7.5f * unit, 8.9f * unit)
            cubicTo(8.5f * unit, 11.7f * unit, 10.5f * unit, 13.8f * unit, 13.3f * unit, 15.2f * unit)
            cubicTo(14.2f * unit, 15.7f * unit, 15.1f * unit, 15.8f * unit, 15.8f * unit, 15.1f * unit)
            lineTo(16.8f * unit, 13.9f * unit)
            cubicTo(17.1f * unit, 13.5f * unit, 16.9f * unit, 13f * unit, 16.4f * unit, 12.8f * unit)
            lineTo(14.7f * unit, 12f * unit)
            cubicTo(14.2f * unit, 11.8f * unit, 13.8f * unit, 12f * unit, 13.5f * unit, 12.4f * unit)
            lineTo(13.1f * unit, 12.9f * unit)
            cubicTo(11.9f * unit, 12.2f * unit, 11f * unit, 11.3f * unit, 10.3f * unit, 10.1f * unit)
            lineTo(10.8f * unit, 9.6f * unit)
            cubicTo(11.1f * unit, 9.3f * unit, 11.2f * unit, 8.8f * unit, 11f * unit, 8.4f * unit)
            lineTo(10.2f * unit, 7.4f * unit)
            cubicTo(9.9f * unit, 7f * unit, 9f * unit, 6.9f * unit, 8.2f * unit, 7.2f * unit)
            close()
        }
        drawPath(handset, white)
    }
}


/** Plain (non-observable) holder for the last banner shown, used to render its exit fade. */
private class BannerHold {
    var last: IslandActivity.WhatsApp? = null
    /** Set by the live banner when it has already animated itself away (swipe-up). */
    var skipExit: Boolean = false
}
