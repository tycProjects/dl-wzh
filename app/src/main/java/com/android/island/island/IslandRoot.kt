package com.android.island.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import android.os.SystemClock
import com.android.island.util.IslandDiagnostics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
    // System "Remove animations" (animator scale 0) switches off the decorative cascade/bloom as well.
    val systemAnimationsOff = remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val reducedMotion = settings.lowPowerMode || systemAnimationsOff
    // Low-end mode forces the white palette (it is lighter to draw and reads well on the static glass).
    // The saved choice is not changed, so turning low-end off brings the chosen theme back.
    val islandThemeName = if (settings.lowEndMode) "WHITE" else settings.islandTheme.name
    // Entering Low-End releases Premium GPU effects and the entire secondary artwork cache.
    // WhatsApp/avatar and music art are drawn from their already-bounded activity-owned source bitmaps.
    LaunchedEffect(settings.lowEndMode) {
        // Apply the renderer state before notifying the media listener, so every decode started
        // by the listener sees the correct bitmap limit and shader policy.
        com.android.island.core.DeviceProfile.setLowEnd(settings.lowEndMode)
        com.android.island.core.DeviceProfile.applyStaticGlass(settings.lowEndMode)
        // The listener drops its Premium artwork copy or rebuilds it at the Low-End bound.
        // Old UI frames are allowed to finish before their bitmap references become collectible.
        com.android.island.service.WhatsAppNotificationListener.onLowEndModeChanged(settings.lowEndMode)
        if (settings.lowEndMode) {
            com.android.island.island.trimArtCaches()
            com.android.island.service.WhatsAppNotificationListener.onTrimMemory()
            IslandDiagnostics.record("LOW_END_CACHE_TRIM", "reason=low_end_enabled")
        } else {
            IslandDiagnostics.record("LOW_END_CACHE_TRIM", "reason=low_end_disabled")
        }
    }
    SideEffect {
        GlassTuning.sheenStrength = settings.sheenStrength
        GlassTuning.edgeHighlight = settings.glassConfig.edgeHighlight
        GlassTuning.profileSheenExpanded = settings.profileSheenExpanded
        GlassTuning.profileSheenBanner = settings.profileSheenBanner
        // Low-End is an explicit visual/resource mode: only its own toggle may replace
        // the normal convex mirror lens with the shader-free static surfaces. Low Power
        // and Android Battery Saver may reduce motion, but must not silently remove glass.
        com.android.island.core.DeviceProfile.setLowEnd(settings.lowEndMode)
        com.android.island.core.DeviceProfile.applyStaticGlass(settings.lowEndMode)
        GlassTuning.lowEnd = if (settings.lowEndMode) 1 else 0
        GlassTuning.glassSurfaceOpacity = settings.glassSurfaceOpacity
    }
    // One gesture dispatcher for every state (compact, banner, expanded layer), so the Tap /
    // Double tap / Long press settings always apply. "Expand" means the right thing for the
    // context (banner media, WhatsApp stack, or the normal expand). Play/pause and next only make
    // sense for music, so they fall back to "expand" on chat/call capsules.
    // Keep the gesture dispatcher stable across ordinary recompositions. The latest state and
    // callbacks are read through updated-state holders, so this does not capture stale closures.
    val latestIslandState by rememberUpdatedState(state)
    val latestOnExpand by rememberUpdatedState(onExpand)
    val runGesture: (TapAction, IslandActivity, Boolean) -> Unit = remember(context) {
        { action: TapAction, activity: IslandActivity, inBanner: Boolean ->
            val effective = if (activity !is IslandActivity.Media &&
                (action == TapAction.PLAY_PAUSE || action == TapAction.NEXT_TRACK)) TapAction.EXPAND else action
            if (effective == TapAction.EXPAND) {
                when {
                    activity is IslandActivity.Media && latestIslandState.pendingMedia != null -> IslandBus.expandMediaFromBanner()
                    // No stack: a chat/notification capsule opens the app that posted it.
                    activity is IslandActivity.WhatsApp -> handleTap(TapAction.OPEN_APP, activity, latestOnExpand, context)
                    else -> latestOnExpand()
                }
            } else {
                handleTap(effective, activity, latestOnExpand, context)
            }
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
    // Banner has its own entrance channel; it never participates in the capsule morph.
    val bannerOffsetY = remember { Animatable(-34f) }
    // Resolve the current WhatsApp notification identity before effects use it.
    val bannerKey = (current as? IslandActivity.WhatsApp)?.id
    // Auto-expire only the visible banner. The queued notifications remain in IslandBus,
    // and the capsule geometry is untouched; the side indicator becomes the entry point.
    LaunchedEffect(islandState, bannerKey, settings.whatsAppBannerMs) {
        if (islandState == IslandState.BANNER && current is IslandActivity.WhatsApp) {
            val timeoutMs = settings.whatsAppBannerMs.coerceIn(1500, 10000).toLong()
            // Quick-reply is an active interaction: pause expiry while the input is open.
            // Once the user closes/sends the reply, grant the banner a fresh timeout.
            while (IslandBus.state.value.islandState == IslandState.BANNER &&
                (IslandBus.state.value.current as? IslandActivity.WhatsApp)?.id == bannerKey) {
                // Event, not polling: suspend until the quick-reply editor closes.
                if (IslandBus.replyOpen) IslandBus.replyOpenState.first { !it }
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
                bannerExit.animateTo(1f, tween(
                    (BANNER_TUCK_MS / settings.animationSpeed.coerceIn(0.05f, 2f)).toInt().coerceIn(200, if (settings.animationSpeed < 0.5f) 8000 else 700),
                    easing = BannerTuckEasing))
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
            bannerOffsetY.animateTo(0f, if (reducedMotion) islandSpring(settings, false) else bannerEnterSpring())
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
        if (targetProgress == 1f && progress.value < 0.001f) {
            // Pre-roll: let the first composition, layout and the overlay window resize (60 px -> card
            // height) finish on their own frames. Two frames (~32 ms at 60 Hz) are invisible, while a
            // spring that starts on a 40 ms frame visibly skips its first third.
            withFrameNanos { }
            withFrameNanos { }
        }
        if (abs(progress.value - targetProgress) > 0.001f) {
            // Same spring for open and close (reference: one islandSpring for both directions).
            val spec = islandMorphSpring(latestSettings, opening = targetProgress == 1f)
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
    // The panel is composed as soon as the state flips to EXPANDED (still invisible: progress is 0, so its
    // alpha is 0), not one frame after the spring has started. Composing the heavy card INSIDE the
    // running spring made the first frames jump (log: progress 0.000 -> 0.309 in one step); now the
    // composition, the shader compile and the window resize all land before the animation clock starts.
    // The panel stays composed for a moment after closing, so the spring overshoot around 0 cannot remove
    // and re-add it (that was the double blink). It is released only once the close has settled.
    var panelKeep by remember { mutableStateOf(false) }
    LaunchedEffect(islandState) {
        if (islandState == IslandState.EXPANDED) {
            // Reserve the expanded host before the first morph frame.
            panelKeep = true
        } else {
            // Keep the panel and its GL surface alive until Animatable has COMPLETED
            // the close, including its final snapTo(target). A value-only threshold is
            // not sufficient for the Jelly spring: it can cross zero, briefly rebound
            // above 0.001, and make panelShown remove/re-add the whole glass subtree.
            // That teardown/recreation is the single-frame flash seen at the capsule.
            // Waiting for both idle + the exact compact endpoint also handles INSTANT
            // motion and retargeting without a guessed post-animation delay.
            snapshotFlow { progress.isRunning to progress.value }
                .first { (running, value) -> !running && value <= 0.001f }
            panelKeep = false
        }
    }
    val panelShown by remember(islandState) {
        derivedStateOf { panelKeep || islandState == IslandState.EXPANDED || progress.value.coerceIn(0f, 1f) > 0.001f }
    }
    val hostWide by remember {
        derivedStateOf { panelKeep || islandState == IslandState.EXPANDED || progress.value.coerceIn(0f, 1f) > 0.001f }
    }
    // The capsule is fully faded out once the panel is 40 % open. Not composing it then also stops its wave loop.
    val capsuleFading by remember { derivedStateOf { progress.value.coerceIn(0f, 1f) < 0.35f } }
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
        state.cutout,
        density,
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
    // Off-centre punch holes: the capsule is moved onto the real cutout, then the user's own X offset is added.
    val cutoutShiftDp = CutoutProfile.autoShiftDp(settings.cutoutProfile, state.cutout, screenW)
    val horizontalOffset = (settings.offsetX + cutoutShiftDp).dp.coerceIn(-maxHorizontalOffset, maxHorizontalOffset)
    // The media card never goes below the height its layout needs (art row, lyric line, progress
    // row); "Tinggi minimum" can only make it taller. Extra height is spread evenly inside the card.
    val mediaMinH = if (current is IslandActivity.Media) {
        mediaCardMinHeightDp(settings.showLyrics, settings.lyricFontSize).dp
    } else 0.dp
    val configuredExpandedH = maxOf(
        if (settings.expandedMinHeight > 0) settings.expandedMinHeight.dp else 118.dp,
        mediaMinH
    )
    // Lyrics may wrap to two lines; reserve the extra line so the card grows instead of squashing.
    // (The media card's own minimum height already reserves the second lyric line.)
    val lyricExtraH = 0.dp
    val stackItems = remember(state.current, state.whatsAppItems) { state.stackItems() }
    // AI chat card: content-sized. Before the first measurement a
    // 400 dp estimate is used; the cap keeps the card inside the fixed draw window.
    val aiMode = current is IslandActivity.Ai
    val aiMeasuredH = if (state.aiPanelHeightDp > 0) state.aiPanelHeightDp.dp else 400.dp
    val expandedH = if (aiMode) {
        // AI is a reading surface, so it gets its own near-full-height budget.
        // Reserve the capsule anchor, user Y offset and a small bottom safety margin.
        // STAGE 72: the cap is now the ONE shared WindowTuning.aiPanelCapDp — the same number
        // the panel itself and the touch window use, so a stretched panel can never put its
        // chip row outside the touchable surface again.
        val aiCapDp = com.android.island.core.WindowTuning.aiPanelCapDp(
            LocalConfiguration.current.screenHeightDp.toFloat(),
            compactH.value,
            settings.expandedOffsetY.toFloat()
        ).dp
        // STAGE 70: the user-set minimum height ("Tinggi minimum panel AI") guarantees the chat
        // is comfortably readable from the first frame; taller content still grows to fit.
        maxOf(configuredExpandedH, aiMeasuredH, settings.aiPanelMinHeight.coerceAtLeast(320).dp).coerceAtMost(aiCapDp)
    } else configuredExpandedH + lyricExtraH
    LaunchedEffect(islandState, compactW, compactH, expandedW, expandedH, horizontalOffset, settings.expandedOffsetY) {
        IslandDiagnostics.record(
            "ISLAND_GEOMETRY",
            "state=$islandState compact=${compactW.value}x${compactH.value}dp expanded=${expandedW.value}x${expandedH.value}dp xOffset=${horizontalOffset.value}dp expandedOffsetY=${settings.expandedOffsetY}dp"
        )
    }
    // WhatsApp expanded panel (stack and single chat): the same width as the WhatsApp banner, reaching up to
    // the screen edge (8 dp margin, like the banner). Other panels keep the expanded-width setting.
    val waBannerW = (if (settings.compactBanner) settings.compactBannerWidth else settings.standardBannerWidth)
        .coerceIn(200, (screenW - 8).coerceAtLeast(200)).dp
    // AI card uses nearly the full usable width (adaptive). Media expandedWidth is often
    // too narrow and makes the chat look flattened ("gepeng") especially when summoned
    // from the system assistant / long-press power.
    val panelW = when {
        current is IslandActivity.WhatsApp -> waBannerW
        current is IslandActivity.Ai -> (screenW - 20).coerceIn(300, (screenW - 8).coerceAtLeast(300)).dp
        else -> expandedW
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
        maxOf(compactSceneWidth, bannerContentWidth, panelW)
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
    val expandedHostWidth = maxOf(compactSceneWidth, panelW).coerceAtMost(screenW.dp)
    val sceneWidthDp = when {
        islandState == IslandState.BANNER -> maxOf(compactSceneWidth, bannerContentWidth).coerceAtMost(screenW.dp)
        islandState == IslandState.EXPANDED || hostWide -> expandedHostWidth
        else -> compactSceneWidth
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Top-aligned explicitly. The default for wrapContentHeight is vertically CENTRED, which
            // moved the capsule to the middle of the 60% drawing window while touch bounds stayed at the top.
            .wrapContentHeight(Alignment.Top)
            .padding(top = topOffsetDp.dp)
            .graphicsLayer {
                clip = false
                // Scale from the top-centre anchor (under the notch / capsule), not the screen centre.
                transformOrigin = TransformOrigin(0.5f, 0f)
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
                            // Same horizontal offset as the compact capsule, so the idle notch and the
                            // active capsule sit on exactly the same spot (no jump on activation).
                            .offset(x = horizontalOffset)
                            .width(compactW)
                            .height(compactH)
                            .clip(iPhoneNotchShape())
                            .background(androidx.compose.ui.graphics.Color.Black)
                            // STAGE 74: the idle notch keeps working as the island entry point.
                            // Tapping it summons the assistant card (the island itself has no
                            // other active content while hidden, so EXPAND would be a no-op).
                            .clickable { IslandBus.pushAi() }
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
                                activity = wa.copy(messageCount = stackItems.size.coerceAtLeast(1)),
                                waterDrop = useWaterDropShape,
                                iPhoneNotch = settings.iphoneNotchEnabled,
                                widthDp = compactW,
                                heightDp = compactH,
                                swipeTracks = false,
                                swipeExpand = true,
                                onTap = { runGesture(settings.tapAction, wa, true) },
                                onDoubleTap = { runGesture(settings.doubleTapAction, wa, true) },
                                onLongPress = { runGesture(settings.longPressAction, wa, true) },
                                onSwipeUp = { if (pendingMedia != null) IslandBus.expandMediaFromBanner() else IslandBus.collapseWhatsAppBanner() },
                                onHorizontalDismiss = { IslandBus.clearWhatsApp(wa.id) },
                                allowHorizontalGesture = false,
                                allowSwipeDismiss = false,
                                themeMode = islandThemeName,
                                hapticsEnabled = settings.hapticsEnabled,
                                hasDoubleTap = hasDoubleTap,
                                hasLongPress = hasLongPress,
                                breath = null
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
                            themeMode = islandThemeName,
                            hapticsEnabled = settings.hapticsEnabled,
                            hasDoubleTap = hasDoubleTap,
                            hasLongPress = hasLongPress,
                            breath = null
                        )
                    }
                }
                // Unread WhatsApp marker — independent of capsule size, left by default.
                if (state.whatsAppItems.isNotEmpty()) {
                    WhatsUnreadMarker(
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
                    )
                }
                // STAGE 70: banner content swap crossfade. When a call takes over a chat banner,
                // or a newer chat replaces the current one, the outgoing card fades out under the
                // incoming one instead of vanishing in a single frame (the "patah-patah" report).
                // Slots keep the last content per identity key, so the fading copy still shows its
                // own sender/message. Same-id metadata updates refresh live (slots are rewritten on
                // every recomposition) and old slots are pruned 400 ms after each swap, so no avatar
                // bitmap is ever retained.
                val bannerKeyNow = when (current) {
                    is IslandActivity.Call -> "call:${current.id}"
                    is IslandActivity.WhatsApp -> "wa:${current.id}"
                    else -> ""
                }
                val bannerSlots = remember { androidx.compose.runtime.snapshots.SnapshotStateMap<String, IslandActivity>() }
                when (current) {
                    is IslandActivity.Call -> bannerSlots[bannerKeyNow] = current
                    is IslandActivity.WhatsApp -> bannerSlots[bannerKeyNow] = current
                    else -> {}
                }
                LaunchedEffect(bannerKeyNow) {
                    delay(400)
                    bannerSlots.keys.retainAll { it == bannerKeyNow }
                }
                AnimatedContent(
                    targetState = bannerKeyNow,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(180)) },
                    label = "bannerSwap"
                ) { slotKey ->
                    Box(Modifier.fillMaxSize()) {
                        val slotActivity = bannerSlots[slotKey]
                        when (slotActivity) {
                            is IslandActivity.Call -> CallBanner(
                                call = slotActivity,
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
                                onOpen = { handleTap(TapAction.OPEN_APP, slotActivity, {}, context) },
                                themeMode = islandThemeName,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = compactH + 8.dp)
                            )
                            is IslandActivity.WhatsApp -> {
                                val wa = slotActivity
                                WhatsAppBanner(
                                    activity = wa,
                                    glassConfig = state.glassConfig,
                                    showAlbumBackground = settings.showAlbumBackground,
                                    compactMode = settings.compactBanner,
                                    backgroundOpacity = settings.backgroundOpacity,
                                    edgeLensStrength = settings.edgeLensStrength,
                                    themeMode = islandThemeName,
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
                                        if (com.android.island.core.Controls.openChat(context, wa.contentIntent, wa.packageName)) {
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
                                        .onSizeChanged { size ->
                                            IslandBus.setBannerMeasuredHeight((size.height / density).toInt())
                                        }
                                        .graphicsLayer {
                                            translationY = bannerOffsetY.value * density
                                            clip = false
                                            // Grows out of the capsule: scale from the top centre + a quick fade.
                                            // Fully inert (scale 1, alpha 1) once the spring has settled.
                                            val k = bannerEnterT(bannerOffsetY.value)
                                            val a = smooth(k, 0f, 0.55f)
                                            val sc = 0.80f + 0.20f * k.coerceAtMost(1f)
                                            transformOrigin = TransformOrigin(0.5f, 0f)
                                            scaleX = sc
                                            scaleY = sc
                                            alpha = a
                                            // Group alpha: overlapping glass layers must fade as one surface.
                                            compositingStrategy = if (a < 0.999f) androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                                            else androidx.compose.ui.graphics.CompositingStrategy.Auto
                                        }
                                )
                            }
                            else -> {}
                        }
                    }
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
                                themeMode = islandThemeName,
                                hapticsEnabled = settings.hapticsEnabled,
                                hasDoubleTap = hasDoubleTap,
                                hasLongPress = hasLongPress,
                                breath = null
                            )
                        }
                        // Keep the unread marker visible in COMPACT when WhatsApp is queued.
                        if (state.whatsAppItems.isNotEmpty()) {
                            WhatsUnreadMarker(
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
                            )
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
                        // The spring is used even in low-power mode (islandSpring is called with reducedMotion=false
                        // above), so the overshoot happens there too. The old `!reducedMotion` check removed the
                        // slack exactly when the log showed progress=1.11, cutting the bottom of the jelly flat.
                        //
                        // STAGE 68 (never cut in any state): the reserve and the render amplitude now come from
                        // ONE shared formula (WindowTuning), so this side and the window owner (service) can
                        // never disagree again:
                        //   1. The service adds jellyRoomReserveDp() to the required window height -> the window
                        //      guarantees room below the resting panel bottom.
                        //   2. Here the same inputs rebuild the required height, run it through the same
                        //      windowHeightDp() the service uses, and derive the EXACT room left under the
                        //      resting bottom. The jelly crest amplitude (prMax) is clamped to that room, so
                        //      even a screen-capped window (tall AI panel) shows a lively but fully visible
                        //      overshoot instead of a flat-cut bottom.
                        val jellyActive = settings.animationOption == AnimationOption.OPTION_3 &&
                            settings.expandStyle != com.android.island.core.ExpandStyle.INSTANT
                        val jellyOvershoot = com.android.island.core.WindowTuning.jellyOvershootFraction(settings.elasticity)
                        // Same card budget the service uses for its window (max of the resting surfaces).
                        // STAGE 73 (RAM): AI panel room only while the panel is open — the shared
                        // WindowTuning rule, identical to the service window and the stack budget
                        // above. Closed panel = media/banner budget, so the guaranteed room and
                        // the jelly clamp below are computed from the window that really exists.
                        val screenHdpNow = LocalConfiguration.current.screenHeightDp.toFloat()
                        val aiCardDp = com.android.island.core.WindowTuning.aiPanelWindowReserveDp(
                            screenHdpNow,
                            compactH.value,
                            settings.expandedOffsetY.coerceAtLeast(0).toFloat(),
                            settings.aiEnabled,
                            aiMode
                        )
                        val windowCardDp = maxOf(
                            (if (settings.expandedMinHeight > 0) settings.expandedMinHeight else 118).toFloat(),
                            mediaCardMinHeightDp(settings.showLyrics, settings.lyricFontSize).toFloat(),
                            aiCardDp
                        )
                        val requiredWindowDp = com.android.island.core.WindowTuning.drawWindowRequiredHeightDp(
                            capsuleHDp = compactH.value,
                            offsetYDp = settings.expandedOffsetY.coerceAtLeast(0).toFloat(),
                            cardDp = windowCardDp,
                            compactBannerHDp = settings.compactBannerHeight.toFloat(),
                            standardBannerHDp = settings.standardBannerHeight.toFloat(),
                            jellyActive = jellyActive,
                            elasticity = settings.elasticity,
                            aiPanelOpen = aiCardDp > 0f
                        )
                        val guaranteedWindowDp = com.android.island.core.WindowTuning.windowHeightDp(screenHdpNow, requiredWindowDp)
                        // Exact room below the resting bottom of THIS panel inside the guaranteed window.
                        val roomBelowDp = (guaranteedWindowDp - panelTop.value - expandedH.value).coerceAtLeast(0f)
                        // Growth distance capsule -> resting bottom: the distance the crest travels past it.
                        val jellyGrowDp = (panelTop.value + expandedH.value - compactH.value).coerceAtLeast(1f)
                        // Render amplitude: full jelly when the window has room (normal cards), gently reduced
                        // when the window is screen-capped (tall AI panel) — visible jelly, never a flat cut.
                        val jellyPrMax = 1f + ((roomBelowDp / jellyGrowDp).coerceIn(0f, 0.25f))
                        val jellySlack = if (jellyActive) {
                            com.android.island.core.WindowTuning.jellyRoomReserveDp(jellyGrowDp, jellyOvershoot).dp
                        } else 0.dp
                        val stageHeight = (panelTop.coerceAtLeast(compactH) + expandedH + jellySlack)
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
                                        // The black skin of the panel (below) already matches the capsule, so the
                                        // capsule can hand over within the first 22 % of the morph.
                                        // CLOSE: the capsule must already be fully opaque UNDER the panel before the
                                        // panel surface fades away (panel alpha 0.18 -> 0.045). Fading the capsule in
                                        // over the last 22 % left it at ~80 % alpha when the panel disappeared, which
                                        // read as "something left behind, then a jump" at the end of the close.
                                        val p = progress.value
                                        val t = if (islandState == IslandState.EXPANDED) {
                                            (1f - p / 0.22f).coerceIn(0f, 1f)
                                        } else {
                                            ((0.30f - p) / 0.22f).coerceIn(0f, 1f)
                                        }
                                        alpha = t
                                        val sc = 0.94f + 0.06f * t
                                        scaleX = sc
                                        scaleY = sc
                                        transformOrigin = TransformOrigin(0.5f, 0f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (capsuleFading) CompactIsland(
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
                                    themeMode = islandThemeName,
                                    hapticsEnabled = settings.hapticsEnabled,
                                    hasDoubleTap = hasDoubleTap,
                                    hasLongPress = hasLongPress,
                                    breath = null
                                )
                            }
                            if (panelShown) {
                                val jellyOption = settings.animationOption == AnimationOption.OPTION_3
                                val opening = islandState == IslandState.EXPANDED
                                val flowOption = settings.animationOption == AnimationOption.OPTION_2 || jellyOption
                                val startCorner = compactH.value / 2f
                                // Capsule bottom is blunt (~34% of its height), its top is flat.
                                val startBottom = compactH.value * 0.34f
                                val endCorner = settings.expandedCornerRadius.toFloat()
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = panelTop)
                                        .width(panelW)
                                        .height(expandedH)
                                        .graphicsLayer {
                                            // Everything that changes per frame lives here (layer phase):
                                            // no recomposition, no re-measure, GPU-only transform.
                                            val reveal = progress.value.coerceIn(0f, 1f)
                                            shape = if (flowOption) {
                                                // Top: flat capsule edge -> expanded radius. Bottom: blunt capsule -> expanded radius.
                                                // Liquid corner curve: the radius rounds up early (sqrt of progress), so the
                                                // shape never sits in a sharp mid-morph state. Same spring drives the motion.
                                                val curve = smooth(reveal, 0f, 1f)
                                                com.android.island.island.morphCapsuleShape(
                                                    topPx = (endCorner * curve) * density,
                                                    bottomPx = (startBottom + (endCorner - startBottom) * curve) * density
                                                )
                                            } else {
                                                RoundedCornerShape(androidx.compose.foundation.shape.CornerSize(endCorner * density))
                                            }
                                            clip = true
                                            // Opening: reveal the panel gently from the capsule. Closing:
                                            // fade the entire clipped surface out BEFORE the spring's tiny
                                            // negative-to-positive rebound near zero. Without this close-only
                                            // guard, the black backing can reappear for 1-2 frames as a sharp
                                            // rectangular sliver while the morph shape is almost a flat pill.
                                            // The content has its own reverse fade; this only suppresses the
                                            // residual surface flash at the very end of the close.
                                            alpha = if (opening) {
                                                smooth(reveal, 0f, 0.12f)
                                            } else {
                                                smooth(reveal, 0.045f, 0.18f)
                                            }
                                            transformOrigin = TransformOrigin(0.5f, 0f)
                                            if (jellyOption) {
                                                // Jelly: same capsule morph as the others, but the spring overshoot is kept.
                                                // Overshoot past the full size stretches taller and squashes a little wider.
                                                // prMax (computed above from the shared window reserve) caps the crest at
                                                // exactly what the drawing window can show, so the bottom is never cut.
                                                val rx = (compactW.value / panelW.value.coerceAtLeast(1f)).coerceIn(0.2f, 1f)
                                                val ry = (compactH.value / expandedH.value.coerceAtLeast(1f)).coerceIn(0.1f, 1f)
                                                val pr = progress.value.coerceIn(0f, jellyPrMax)
                                                val squash = (pr - 1f).coerceAtLeast(0f) * 0.55f
                                                // Never let the width overshoot leave the scene: that part would be cut flat.
                                                val maxSx = (sceneWidthDp.value / panelW.value.coerceAtLeast(1f)).coerceAtLeast(1f)
                                                scaleX = (rx + (1f - rx) * pr - squash).coerceIn(0.3f, maxSx)
                                                scaleY = ry + (1f - ry) * pr
                                                translationY = -panelTop.toPx() * (1f - pr)
                                            } else {
                                                // iOS-style: the glass grows out of the capsule (same size, same top edge) and
                                                // folds back into it. The corner radius and the content already follow 'reveal'.
                                                val rx = (compactW.value / panelW.value.coerceAtLeast(1f)).coerceIn(0.2f, 1f)
                                                val ry = (compactH.value / expandedH.value.coerceAtLeast(1f)).coerceIn(0.1f, 1f)
                                                scaleX = rx + (1f - rx) * reveal
                                                scaleY = ry + (1f - ry) * reveal
                                                translationY = -panelTop.toPx() * (1f - reveal)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Black skin: the panel starts as the same pure-black pill as the capsule and the
                                    // glass develops on top of it (and goes back to black on close). This replaces the old
                                    // capsule/glass cross-fade, which showed a see-through gap in the middle of the morph.
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .graphicsLayer { alpha = 1f - smooth(progress.value.coerceIn(0f, 1f), 0.15f, 0.62f) }
                                            .background(androidx.compose.ui.graphics.Color.Black)
                                    )
                                    // Content layer (glass + controls): develops over the black skin and, on close,
                                    // on close the items leave one by one (reversed stagger in ExpandedIsland).
                                    // Read in the layer phase only.
                                    Box(
                                        Modifier.graphicsLayer {
                                            val closeProgress = progress.value.coerceIn(0f, 1f)
                                            // Opening content develops with the card. On close, reverse the
                                            // choreography at the capsule end: controls/artwork shrink and
                                            // fade INTO the notch instead of remaining visible until clipped.
                                            val t = if (opening) smooth(closeProgress, 0.08f, 0.50f)
                                                else smooth(closeProgress, 0.02f, 0.30f)
                                            alpha = t
                                            val sc = if (opening) 0.94f + 0.06f * t else 0.88f + 0.12f * t
                                            scaleX = sc
                                            scaleY = sc
                                            translationY = if (opening) (1f - t) * -7f * density else (1f - t) * -3f * density
                                            transformOrigin = TransformOrigin(0.5f, 0f)
                                        }
                                    ) {
                                    LiquidReveal(
                                        state = act,
                                        contentKey = { it.id },
                                        enabled = settings.liquidExpand,
                                        blurDp = 0f,
                                        shapeSpec = islandSpring(settings, false),
                                        fadeSpec = DiMotion.fadeSpring(durationPercentFromSpeed(settings.animationSpeed))
                                    ) { a ->
                                    ExpandedIsland(
                                        enterSpec = islandSpring(settings, false),
                                        open = islandState == IslandState.EXPANDED,
                                        activity = a,
                                        glassConfig = state.glassConfig,
                                        showLyrics = settings.showLyrics,
                                        expandedWidthDp = panelW.value.roundToInt(),
                                        expandedMinHeightDp = expandedH.value.roundToInt(),
                                        // STAGE 72: the AI panel clamps itself to this same value,
                                        // so its measured height can never exceed the host window.
                                        aiPanelMaxHeightDp = expandedH.value.roundToInt(),
                                        showAlbumBackground = settings.showAlbumBackground,
                                        lowPowerMode = reducedMotion,
                                        lyricShineEnabled = settings.lyricShineEnabled,
                                        lyricShineSpeed = settings.lyricShineSpeed,
                                        lyricShineIntensity = settings.lyricShineIntensity,
                                        lyricGlowIntensity = settings.lyricGlowIntensity,
                                        lyricGlowEnabled = settings.lyricGlowEnabled,
                                        lyricFontSize = settings.lyricFontSize,
                                        lyricFontWeight = settings.lyricFontWeight,
                                        glassSweepEnabled = settings.glassSweepEnabled,
                                        glassSweepOpacity = settings.glassSweepOpacity,
                                        glassSweepDurationMs = settings.glassSweepDurationMs,
                                        backgroundOpacity = settings.backgroundOpacity,
                                        edgeLensStrength = settings.edgeLensStrength,
                                        themeMode = islandThemeName,
                                        onAiHeightDp = { IslandBus.setAiPanelHeight(it) },
                                        hapticsEnabled = settings.hapticsEnabled,
                                        cornerRadiusDp = settings.expandedCornerRadius,
                                        openProgress = { progress.value },
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
                themeMode = islandThemeName,
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

private fun handleTap(action: TapAction, activity: IslandActivity, onExpand: () -> Unit, context: android.content.Context) {
    when (action) {
        TapAction.EXPAND -> onExpand()
        TapAction.OPEN_AI -> IslandBus.pushAi()
        TapAction.PLAY_PAUSE -> com.android.island.core.Controls.playPause()
        TapAction.NEXT_TRACK -> com.android.island.core.Controls.next()
        TapAction.OPEN_APP -> when (activity) {
            is IslandActivity.Media -> if (activity.packageName.isNotBlank()) com.android.island.core.Controls.openApp(context, activity.packageName) else onExpand()
            is IslandActivity.Ai -> onExpand()
            is IslandActivity.Call -> Controls.fireAction(context, activity.contentIntent)
            is IslandActivity.WhatsApp -> {
                val openedByNotification = com.android.island.core.Controls.fireAction(context, activity.contentIntent)
                // Open the app that posted the notification, never a fixed app.
                val opened = if (openedByNotification) true else activity.packageName.isNotBlank() &&
                    com.android.island.core.Controls.openApp(context, activity.packageName)
                // Opening an app clears only that app's cards.
                if (opened) {
                    IslandBus.clearWhatsApp(packageName = activity.packageName.ifBlank { null })
                }
            }
        }
        TapAction.NOTHING -> {}
    }
}


/**
 * Side indicator for notifications that have not been seen or interacted with yet (only ones that
 * vibrate ever enter the queue). A plain bell on a flat violet-to-cyan disc: no glow, no halo, no
 * animation and no touch handling, so it can never open anything. It disappears when the
 * notification is opened or dismissed.
 */
@Composable
private fun WhatsUnreadMarker(modifier: Modifier) {
    val fill = remember {
        androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(androidx.compose.ui.graphics.Color(0xFF8B5CF6), androidx.compose.ui.graphics.Color(0xFF22D3EE))
        )
    }
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center
    ) {
        NotificationBellGlyph()
    }
}

@Composable
private fun NotificationBellGlyph() {
    // Static vector: build the paths once per size instead of on every draw.
    Box(
        Modifier
            .size(13.dp)
            .drawWithCache {
                val u = size.minDimension / 24f
                val white = androidx.compose.ui.graphics.Color.White
                val body = Path().apply {
                    moveTo(4.5f * u, 17.5f * u)
                    lineTo(6.2f * u, 15.8f * u)
                    lineTo(6.2f * u, 10.8f * u)
                    cubicTo(6.2f * u, 7.6f * u, 8.5f * u, 5.6f * u, 12f * u, 5.6f * u)
                    cubicTo(15.5f * u, 5.6f * u, 17.8f * u, 7.6f * u, 17.8f * u, 10.8f * u)
                    lineTo(17.8f * u, 15.8f * u)
                    lineTo(19.5f * u, 17.5f * u)
                    close()
                }
                onDrawBehind {
                    drawPath(body, white)
                    drawLine(white, androidx.compose.ui.geometry.Offset(12f * u, 3.4f * u),
                        androidx.compose.ui.geometry.Offset(12f * u, 5.8f * u), strokeWidth = 2f * u, cap = StrokeCap.Round)
                    drawCircle(white, radius = 1.9f * u, center = androidx.compose.ui.geometry.Offset(12f * u, 20f * u))
                }
            }
    )
}


/** Plain (non-observable) holder for the last banner shown, used to render its exit fade. */
private class BannerHold {
    var last: IslandActivity.WhatsApp? = null
    /** Set by the live banner when it has already animated itself away (swipe-up). */
    var skipExit: Boolean = false
}
