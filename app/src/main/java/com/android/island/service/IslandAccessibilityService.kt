package com.android.island.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.FrameLayout
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.island.IslandApplication
import com.android.island.core.CutoutProfile
import com.android.island.core.CutoutType
import com.android.island.core.IslandBus
import com.android.island.core.IslandState
import com.android.island.core.IslandUiState
import com.android.island.data.IslandSettings
import com.android.island.island.IslandRoot
import com.android.island.util.CutoutDetector
import com.android.island.util.IslandDiagnostics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * AccessibilityService hosts TYPE_ACCESSIBILITY_OVERLAY (required over status bar).
 * Applies live settings and creates the overlay only while notch/activity content is visible.
 */
class IslandAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var touchView: TouchForwardingView? = null
    private var touchLayoutParams: WindowManager.LayoutParams? = null
    private val touchHandler = Handler(Looper.getMainLooper())
    private var lastTouchBounds: String = ""
    // The renderer animates EXPANDED -> COMPACT for roughly 0.5-0.7 s. The separate
    // transparent touch window must not shrink to compact bounds during that morph.
    // Layout callbacks can arrive after the state callback and used to replace the
    // 250 ms state debounce with a 48 ms draw-layout debounce, moving the hit target
    // while the card was still visibly closing.
    private var lastTouchBoundsState: IslandState? = null
    private var closeMorphHoldUntilMs: Long = 0L
    private var lastSettingsFingerprint: Int = 0
    private var settingsEmissionCount = 0L
    private var lastSettingsEmissionAt = 0L
    private var lastStateFingerprint: Int = 0
    private val touchBoundsUpdate = Runnable { updateTouchWindow("settled") }
    private var overlayLayoutListener: View.OnLayoutChangeListener? = null
    // Avoid issuing identical WindowManager updates for every DataStore emission.
    private var lastAppliedWindowY: Int? = null
    /** Left edge (px) of the drawing window's lane. Centred on screen, so the capsule never moves. */
    private var drawLaneX = 0
    /** True while the drawing window is shrunk to the capsule (experimental, see compactWindowShrink). */
    private var windowShrunk = false
    private var shrinkScheduled = false
    private val shrinkHandler = Handler(Looper.getMainLooper())
    private val shrinkRunnable = Runnable { shrinkWindowToCapsule() }
    private val SHRINK_AFTER_MS = 8_000L
    /**
     * Idle RAM trim for the capsule. Window shrinking is disabled (see windowShrinkAllowed), so the trim that
     * used to follow it never ran in the most common state. This timer starts once when the island settles in
     * COMPACT and is cancelled when it leaves, so frequent media updates do not postpone it. No loop.
     */
    private var idleTrimScheduled = false
    private val idleTrimRunnable = Runnable {
        idleTrimScheduled = false
        if (IslandBus.state.value.islandState == IslandState.COMPACT) {
            com.android.island.util.NativeTrim.trimWhenIdle("capsule")
        }
    }

    private fun updateIdleTrim(compact: Boolean) {
        if (compact) {
            if (idleTrimScheduled) return
            idleTrimScheduled = true
            shrinkHandler.postDelayed(idleTrimRunnable, SHRINK_AFTER_MS)
        } else if (idleTrimScheduled) {
            idleTrimScheduled = false
            shrinkHandler.removeCallbacks(idleTrimRunnable)
        }
    }
    /**
     * Shrinking the drawing window is disabled. Growing it back reallocates the window surface, and when the
     * growth comes from a notification (not a touch) that reallocation lands inside the expand morph and
     * causes a visible stutter. Keeping the window full-height is the only way to keep the morph smooth.
     */
    private val windowShrinkAllowed = false
    // Measured once before the first overlay is created; refreshed on configuration changes.
    @Volatile private var displayWidthDp: Int = 0
    @Volatile private var displayHeightDp: Int = 0
    @Volatile private var displayDensity: Float = 1f
    @Volatile private var displayWidthPx: Int = 0
    @Volatile private var displayHeightPx: Int = 0
    private var displayListener: android.hardware.display.DisplayManager.DisplayListener? = null
    private var lastDisplaySig = ""

    // Seeded from DeviceProfile.staticGlass (set in IslandApplication.onCreate, before this
    // service or any overlay Composable exists) rather than the IslandSettings() default.
    // staticGlass is true for BOTH confirmed Low-End and UNRESOLVED (the preference isn't known
    // yet — fresh install, or upgrading from a build predating the fast flag): the safe path is
    // the same in either case. Without this, the first overlay frame — which can be created
    // before app.settingsRepository.settings (DataStore) has emitted — would carry
    // lowEndMode=false, and IslandRoot's SideEffect would push that stale default into
    // DeviceProfile, briefly re-arming premium shader/backdrop creation for a user whose saved
    // (or not-yet-known) preference was Low-End.
    private val settingsFlow = MutableStateFlow(
        IslandSettings(lowEndMode = com.android.island.core.DeviceProfile.staticGlass)
    )

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        instance = this
        IslandDiagnostics.record("SERVICE_CONNECTED", "display service connected")
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        measureDisplayProfile()
        IslandBus.setCutout(CutoutDetector.detect(this))
        lastTouchBoundsState = IslandBus.state.value.islandState
        registerDisplayListener()

        // Collect settings live — layout, glass, and enabled flag
        val app = applicationContext as? IslandApplication
        if (app != null) {
            scope.launch {
                app.settingsRepository.settings.distinctUntilChanged().collect { s ->
                    if (!s.compactWindowShrink) cancelWindowShrink("setting-off")
                    val now = SystemClock.uptimeMillis()
                    settingsEmissionCount++
                    val fingerprint = s.hashCode()
                    val delta = if (lastSettingsEmissionAt == 0L) 0L else now - lastSettingsEmissionAt
                    IslandDiagnostics.recordLazy("SETTINGS_APPLIED") { "count=$settingsEmissionCount deltaMs=$delta changed=${fingerprint != lastSettingsFingerprint} enabled=${s.enabled} hideStatusIcons=${s.hideStatusIcons} x=${s.offsetX} y=${s.offsetY} diagnostics=${s.diagnosticsEnabled}" }
                    lastSettingsEmissionAt = now
                    lastSettingsFingerprint = fingerprint
                    settingsFlow.value = s
                    applyLayout(s)
                    IslandBus.setGlassConfig(s.glassConfig)
                    // Create the Compose overlay only while it needs to be visible.
                    updateOverlayPresence(shouldShowOverlay(s, IslandBus.state.value))
                }
            }
        }

        observeVisibility()
        // First start (or restart): bind the notification listener now so music and banners work at once.
        com.android.island.service.WhatsAppNotificationListener.ensureConnected(this)
    }

    /** Measure the real display before composing any island content.
     * Real metrics include the full display bounds; dp conversion is cached until
     * Android reports a configuration/display change.
     */
    private fun measureDisplayProfile(log: Boolean = true) {
        // Real window bounds (full display, including system bars and cutout area), Android 11+ API.
        val bounds = runCatching { windowManager.currentWindowMetrics.bounds }.getOrNull()
        val widthPx = bounds?.width()?.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val heightPx = bounds?.height()?.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        displayWidthPx = widthPx
        displayHeightPx = heightPx
        displayDensity = resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        displayWidthDp = (widthPx / displayDensity).toInt().coerceAtLeast(240)
        displayHeightDp = (heightPx / displayDensity).toInt().coerceAtLeast(320)
        if (log) IslandDiagnostics.record("DISPLAY_PROFILE", "${widthPx}x${heightPx}px density=$displayDensity dp=${displayWidthDp}x${displayHeightDp}")
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::windowManager.isInitialized) onDisplayChanged("configuration")
    }

    /** Rotation, fold/unfold, density or resolution change: re-measure and re-read the cutout. */
    private fun onDisplayChanged(reason: String) {
        // DisplayListener fires for brightness and other non-geometry changes too. Re-laying out the window
        // for those cost CPU and battery on every call, so act only when size, density or rotation changed.
        measureDisplayProfile(log = false)
        val sig = "$displayWidthPx,$displayHeightPx,$displayDensity"
        if (sig == lastDisplaySig) return
        lastDisplaySig = sig
        IslandDiagnostics.record("DISPLAY_PROFILE", "${displayWidthPx}x${displayHeightPx}px density=$displayDensity dp=${displayWidthDp}x${displayHeightDp} reason=$reason")
        CutoutDetector.invalidate()
        IslandBus.setCutout(CutoutDetector.detect(this))
        layoutParams?.let { lp ->
            val (laneW, laneX) = drawLane(settingsFlow.value)
            drawLaneX = laneX
            lp.width = laneW
            lp.x = laneX
            lp.height = targetWindowHeightPx(settingsFlow.value)
            lp.y = windowYPx(settingsFlow.value)
            lastAppliedWindowY = lp.y
            try { overlayView?.let { windowManager.updateViewLayout(it, lp) } } catch (_: Exception) {}
        }
        overlayView?.requestLayout()
        lastTouchBounds = ""
        scheduleTouchWindowUpdate(reason)
    }

    private fun registerDisplayListener() {
        if (displayListener != null) return
        val dm = getSystemService(DISPLAY_SERVICE) as? android.hardware.display.DisplayManager ?: return
        val l = object : android.hardware.display.DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == android.view.Display.DEFAULT_DISPLAY) this@IslandAccessibilityService.onDisplayChanged("display")
            }
        }
        displayListener = l
        dm.registerDisplayListener(l, touchHandler)
    }

    private fun unregisterDisplayListener() {
        val l = displayListener ?: return
        (getSystemService(DISPLAY_SERVICE) as? android.hardware.display.DisplayManager)?.unregisterDisplayListener(l)
        displayListener = null
    }

    /**
     * Lane (width px, left x px) of the drawing window for these settings. It covers the widest state the
     * island shows (expanded card, banner, or capsule with the WhatsApp side room) plus a small edge, and is
     * centred on the screen. It never depends on the animation, so it changes only with settings or rotation.
     */
    private fun drawLane(s: IslandSettings): Pair<Int, Int> {
        val density = resources.displayMetrics.density
        val screenPx = displayWidthPx.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val screenWdp = displayWidthDp.takeIf { it > 0 } ?: (screenPx / density).toInt()
        val state = IslandBus.state.value
        val notch = CutoutProfile.resolve(s.cutoutProfile, state.cutout, density, s.iphoneNotchWidth, s.iphoneNotchHeight)
        val compactW = notch.first.coerceIn(88, 480)
        // Same shift and clamp as IslandRoot.
        val maxShiftDp = ((screenWdp - compactW) / 2f - 28f).coerceAtLeast(0f)
        val shiftDp = (s.offsetX + CutoutProfile.autoShiftDp(s.cutoutProfile, state.cutout, screenWdp))
            .coerceIn(-maxShiftDp, maxShiftDp)
        // 56 dp WhatsApp side room is always reserved, so the lane never changes when a chat arrives.
        val compactSceneDp = compactW + 56f + kotlin.math.abs(shiftDp) * 2f
        val expandedDp = s.expandedWidth.coerceIn(180, screenWdp.coerceAtLeast(180)).toFloat()
        val bannerDp = maxOf(s.compactBannerWidth, s.standardBannerWidth).toFloat()
        val widest = maxOf(compactSceneDp, expandedDp, bannerDp)
        val laneDp = com.android.island.core.WindowTuning.laneWidthDp(screenWdp.toFloat(), widest)
        val lanePx = kotlin.math.round(laneDp * density).toInt().coerceIn(1, screenPx)
        return lanePx to ((screenPx - lanePx) / 2).coerceAtLeast(0)
    }

    /** Window Y in px: the user's calibration plus the shift that centres the capsule on a punch hole below the top edge. */
    private fun windowYPx(s: IslandSettings): Int {
        val density = resources.displayMetrics.density
        val state = IslandBus.state.value
        val capsuleH = CutoutProfile.resolve(s.cutoutProfile, state.cutout, density, s.iphoneNotchWidth, s.iphoneNotchHeight).second
        val auto = CutoutProfile.autoShiftYDp(s.cutoutProfile, state.cutout, density, capsuleH)
        return ((s.offsetY + auto) * density).toInt()
    }

    /** Fixed draw height (see WindowTuning): enough for the largest media card or banner. */
    private fun drawWindowFixedHeightPx(): Int {
        val d = resources.displayMetrics.density
        val screenPx = displayHeightPx.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val s = settingsFlow.value
        val capsuleH = CutoutProfile.resolve(
            s.cutoutProfile, IslandBus.state.value.cutout, d, s.iphoneNotchWidth, s.iphoneNotchHeight
        ).second.coerceIn(24, 64).toFloat()
        val panelOpenNow = IslandBus.state.value.current is com.android.island.core.IslandActivity.Ai
        val card = maxOf(
            (if (s.expandedMinHeight > 0) s.expandedMinHeight else 118).toFloat(),
            com.android.island.island.mediaCardMinHeightDp(s.showLyrics, s.lyricFontSize).toFloat(),
            // STAGE 73 (RAM): AI chat card reserve ONLY while the panel is open. The old code
            // reserved the measured panel height (or 420 dp) whenever AI was enabled — closed
            // panel included — so after the first panel use the draw window stayed ~1750-2200 px
            // tall forever. Its four graphics buffers (width x height x 4 bytes each) were the
            // dominant chunk of GL/RSS memory while the screen is on (the "lama-lama RAM naik
            // lagi" dumpsys: Graphics 90 MB). While open the shared CAP is reserved, so the
            // panel can never outgrow the window and the height changes exactly once per
            // open/close, never while the panel measures itself. The closed-state budget is the
            // same one WindowTuning builds for IslandRoot, so both sides always agree.
            com.android.island.core.WindowTuning.aiPanelWindowReserveDp(
                screenHeightDp = screenPx / d,
                capsuleHDp = capsuleH,
                offsetYDp = s.expandedOffsetY.coerceAtLeast(0).toFloat(),
                aiEnabled = s.aiEnabled,
                panelOpen = panelOpenNow
            )
        )
        // STAGE 68: the required height now comes from the ONE shared budget in WindowTuning
        // (IslandRoot rebuilds the exact same number), so the jelly crest reserve and the
        // banner/quick-reply expansion are guaranteed room and nothing can be cut flat at the
        // bottom edge in any state. On tall surfaces the window simply reaches the screen cap
        // and IslandRoot clamps the jelly amplitude to the room that is actually there.
        // STAGE 74: while the AI panel is open the jelly crest reserve is clamped inside
        // WindowTuning, so the window no longer grows to the full display height.
        val jellyActive = s.animationOption == com.android.island.data.AnimationOption.OPTION_3 &&
            s.expandStyle != com.android.island.core.ExpandStyle.INSTANT
        val required = com.android.island.core.WindowTuning.drawWindowRequiredHeightDp(
            capsuleHDp = capsuleH,
            offsetYDp = s.expandedOffsetY.coerceAtLeast(0).toFloat(),
            cardDp = card,
            compactBannerHDp = s.compactBannerHeight.toFloat(),
            standardBannerHDp = s.standardBannerHeight.toFloat(),
            jellyActive = jellyActive,
            elasticity = s.elasticity,
            aiPanelOpen = panelOpenNow
        )
        val heightDp = com.android.island.core.WindowTuning.windowHeightDp(screenPx / d, required)
        return (heightDp * d).toInt().coerceAtLeast(1)
    }

    /** Height the drawing window should have right now: full, or capsule-only while it is shrunk. */
    private fun targetWindowHeightPx(s: IslandSettings): Int {
        // The flag is checked too: turning the feature off must restore the full height at once.
        if (!windowShrunk || !s.compactWindowShrink) return drawWindowFixedHeightPx()
        val d = resources.displayMetrics.density
        val capsuleH = CutoutProfile.resolve(
            s.cutoutProfile, IslandBus.state.value.cutout, d, s.iphoneNotchWidth, s.iphoneNotchHeight
        ).second.coerceIn(24, 64).toFloat()
        // Must cover the touch bounds: updateTouchWindow uses compact height = notch height + 28 dp. A smaller
        // window made taps in the lower band reach a view that could not draw there, so they were lost.
        val touchH = CutoutProfile.resolve(
            s.cutoutProfile, IslandBus.state.value.cutout, d, s.iphoneNotchWidth, s.iphoneNotchHeight
        ).second + 28f
        val drawH = capsuleH + 16f
        return (maxOf(drawH, touchH) * d).toInt().coerceIn(1, drawWindowFixedHeightPx())
    }

    /** Idle capsule: shrink after a quiet period. Starts the timer once, so frequent state updates do not postpone it. */
    private fun scheduleWindowShrink() {
        if (!windowShrinkAllowed || !settingsFlow.value.compactWindowShrink || windowShrunk || shrinkScheduled) return
        shrinkScheduled = true
        shrinkHandler.postDelayed(shrinkRunnable, SHRINK_AFTER_MS)
    }

    private fun shrinkWindowToCapsule() {
        shrinkScheduled = false
        if (overlayView == null || windowShrunk) return
        if (IslandBus.state.value.islandState != IslandState.COMPACT) return
        windowShrunk = true
        IslandDiagnostics.record("WINDOW_SHRINK", "to=capsule")
        applyLayout(settingsFlow.value)
        // The island is quiet again: let the renderer drop its caches (see NativeTrim).
        shrinkHandler.postDelayed({ com.android.island.util.NativeTrim.trimWhenIdle("capsule") }, 1_500L)
    }

    /** Full height again before anything expands. Called on touch-down and on any non-capsule state. */
    private fun cancelWindowShrink(reason: String) {
        shrinkHandler.removeCallbacks(shrinkRunnable)
        shrinkScheduled = false
        if (!windowShrunk) return
        windowShrunk = false
        IslandDiagnostics.record("WINDOW_GROW", "reason=$reason")
        applyLayout(settingsFlow.value)
    }

    private fun createOverlay() {
        if (overlayView != null) return
        IslandDiagnostics.record("WINDOW_CREATE_BEGIN", "architecture=two-window drawWidth=MATCH_PARENT touchWindow=transparent-forwarder gravity=TOP|START")
        val s = settingsFlow.value
        // WRAP_CONTENT width + FLAG_NOT_TOUCH_MODAL:
        // - touches outside the island pass through → quick panel / status bar tetap bisa di-swipe
        // - FLAG_NOT_FOCUSABLE default: tidak mencuri fokus keyboard kecuali saat quick-reply
        // Created once at full height and never resized while in use. Runtime height changes
        // caused surface reallocation on every tap (the stutter in the 1.6.0 log).
        val (laneW, laneX) = drawLane(s)
        drawLaneX = laneX
        val params = WindowManager.LayoutParams(
            laneW,
            drawWindowFixedHeightPx(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            baseFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Keep the overlay window itself centered in every state. Horizontal
            // calibration is applied inside the Compose scene with reserved side room,
            // so WRAP_CONTENT width changes cannot recenter/drag the capsule.
            x = laneX
            y = windowYPx(s)
            // ALWAYS draws into the cutout area on every edge/orientation (punch hole, notch, waterdrop).
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            // Never let a ROM push the overlay below the status bar: no system-bar insets are fitted.
            fitInsetsTypes = 0
            title = "DynamicIsland"
        }
        layoutParams = params

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            // Explicitly transparent: when a system notification is dismissed and the
            // Compose content/window resizes, never expose the default View background.
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@IslandAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@IslandAccessibilityService)
            setContent { OverlayContent() }
            clipChildren = false
            clipToPadding = false
            clipBounds = null
            // Drawing window is permanently MATCH_PARENT width. Touch is handled by a
            // second transparent window and forwarded here with screen-coordinate offsets.
            val boundsListener = View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                val lp = this@IslandAccessibilityService.layoutParams ?: return@OnLayoutChangeListener
                // The WindowManager anchor is deliberately invariant. State-specific
                // horizontal placement belongs to the Compose content, not this window.
                scheduleTouchWindowUpdate("draw-layout")
                if (lp.x != drawLaneX) {
                    lp.x = drawLaneX
                    try { windowManager.updateViewLayout(v, lp) } catch (_: Exception) {}
                }
            }
            overlayLayoutListener = boundsListener
            addOnLayoutChangeListener(boundsListener)
            // Touches are delivered by TouchForwardingView; this listener is retained
            // only for outside-touch notifications where the platform sends them.
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    val st = IslandBus.state.value.islandState
                    // Never auto-close BANNER (WhatsApp reply needs to stay open).
                    // Only collapse pure media EXPANDED when not typing a reply.
                    // The keyboard and other windows are outside the island: they must not close an
                    // open reply or an AI session while the composer/file picker window is open.
                    // STAGE 74: waiting for the model (aiTyping) alone no longer pins the panel
                    // open — an outside tap collapses it so the system is never blocked by an
                    // invisible or unattended panel. The chat itself lives in AiSession and is
                    // kept; the answer lands in history and is visible when the panel reopens.
                    if (st == IslandState.EXPANDED && !IslandBus.replyOpen && !IslandBus.externalAiActive && !com.android.island.ai.AiSession.agentBusy) {
                        IslandBus.collapse()
                    }
                    false
                } else {
                    false
                }
            }
        }
        overlayView = view
        try {
            windowManager.addView(view, params)
            IslandDiagnostics.recordLazy("DRAW_WINDOW_CREATED") { "x=${params.x} y=${params.y} w=${params.width} h=${params.height} flags=${params.flags}" }
            createTouchWindow()
            scheduleTouchWindowUpdate("created")
        } catch (e: Exception) {
            IslandDiagnostics.record("WINDOW_CREATE_FAILED", "${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    /** Whether the current gesture is owned by the island. Decided once on ACTION_DOWN, so a gesture never flips midway. */
    private var gestureOwned = false

    /** Transparent independent input window. It forwards touch events into the fixed-width renderer. */
    private fun createTouchWindow() {
        if (touchView != null) return
        val touch = TouchForwardingView(this) { event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                val st = IslandBus.state.value.islandState
                // STAGE 74: only the reply editor and the external AI windows (composer, file
                // picker) guard the outside-tap collapse. A model that is merely generating no
                // longer keeps the big expanded surface above the system UI while the user is
                // clearly interacting with something else.
                if (st == IslandState.EXPANDED && !IslandBus.replyOpen && !IslandBus.externalAiActive && !com.android.island.ai.AiSession.agentBusy) {
                    IslandBus.collapse()
                }
                return@TouchForwardingView false
            }
            if (overlayHidden) return@TouchForwardingView false
            if (event.actionMasked == MotionEvent.ACTION_DOWN) cancelWindowShrink("touch")
            val target = overlayView ?: return@TouchForwardingView false
            val copy = MotionEvent.obtain(event)
            val lp = touchLayoutParams
            // Touch-local -> draw-local: add the touch window origin, remove the draw window origin.
            val drawLp = layoutParams
            copy.offsetLocation(((lp?.x ?: 0) - (drawLp?.x ?: 0)).toFloat(), ((lp?.y ?: 0) - (drawLp?.y ?: 0)).toFloat())
            val handled = target.dispatchTouchEvent(copy)
            copy.recycle()
            // Only own the gesture when Compose actually consumed the DOWN. Returning false
            // on unhandled taps lets FLAG_NOT_TOUCH_MODAL pass them to the app underneath
            // (home screen icons, status bar) instead of swallowing them.
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> gestureOwned = handled
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val result = gestureOwned
                    gestureOwned = false
                    return@TouchForwardingView result
                }
            }
            val result = gestureOwned
            result
        }
        touchView = touch
        val params = WindowManager.LayoutParams(1, 1,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply {
            // A touch window recreated while the island is hidden must stay untouchable too.
            if (overlayHidden) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            gravity = Gravity.TOP or Gravity.START
            title = "AndroidIslandTouch"
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
        }
        touchLayoutParams = params
        try {
            windowManager.addView(touch, params)
            IslandDiagnostics.record("TOUCH_WINDOW_CREATED", "transparent=true forwarding=true")
        } catch (e: Exception) {
            touchView = null
            touchLayoutParams = null
            IslandDiagnostics.record("TOUCH_WINDOW_CREATE_FAILED", "${e.javaClass.simpleName}:${e.message}")
        }
    }

    private fun scheduleTouchWindowUpdate(reason: String) {
        val now = SystemClock.uptimeMillis()
        val currentState = IslandBus.state.value.islandState
        val previousState = lastTouchBoundsState

        if (previousState != currentState) {
            // Only the closing direction needs to retain the larger expanded hit area.
            // On opening, the expanded bounds can be installed promptly so controls
            // become touchable as soon as they appear.
            if (previousState == IslandState.EXPANDED && currentState != IslandState.EXPANDED) {
                closeMorphHoldUntilMs = now + 760L
            } else if (currentState == IslandState.EXPANDED) {
                closeMorphHoldUntilMs = 0L
            }
            lastTouchBoundsState = currentState
        }

        touchHandler.removeCallbacks(touchBoundsUpdate)
        val baseDelayMs = if (reason == "state" || reason == "visibility") 250L else 48L
        // A later DRAW_LAYOUT/DISPLAY callback must not cancel the close hold with its
        // shorter debounce. Keep the expanded hit rectangle until the morph has settled.
        val holdRemainingMs = (closeMorphHoldUntilMs - now).coerceAtLeast(0L)
        val delayMs = maxOf(baseDelayMs, holdRemainingMs)
        touchHandler.postDelayed(touchBoundsUpdate, delayMs)
    }

    /** Same height rule IslandRoot uses for the expanded card, so touch bounds cover every control. */
    private fun expandedPanelHeightDp(
        settings: com.android.island.data.IslandSettings,
        state: com.android.island.core.IslandUiState
    ): Int {
        val base = settings.expandedMinHeight.coerceAtLeast(118)
        val mediaMin = if (state.current is com.android.island.core.IslandActivity.Media) {
            com.android.island.island.mediaCardMinHeightDp(settings.showLyrics, settings.lyricFontSize)
        } else 0
        val aiMin = if (state.current is com.android.island.core.IslandActivity.Ai) {
            // Prefer measured height; fall back to a modest estimate so the touch window
            // does not cover the whole screen before the first layout pass.
            if (state.aiPanelHeightDp > 0) state.aiPanelHeightDp else 320
        } else 0
        return maxOf(base, mediaMin, aiMin)
    }

    private fun updateTouchWindow(reason: String) {
        val target = touchView ?: return
        val lp = touchLayoutParams ?: return
        val settings = settingsFlow.value
        val density = resources.displayMetrics.density
        val screenW = displayWidthDp.takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels / density).toInt()
        val state = IslandBus.state.value
        val expanded = state.islandState == IslandState.EXPANDED
        val banner = state.islandState == IslandState.BANNER
        // Touch bounds must match the drawn notch, so use the same profile resolution as IslandRoot.
        val notchSize = CutoutProfile.resolve(
            settings.cutoutProfile,
            state.cutout,
            density,
            settings.iphoneNotchWidth,
            settings.iphoneNotchHeight
        )
        val compactW = notchSize.first.coerceIn(88, 480)
        val compactH = notchSize.second
        val expandedW = settings.expandedWidth.coerceIn(180, screenW)
        // Hit bounds mirror the *visible content* for the active state. In particular,
        // non-compact banners use their own width/height settings; previously the touch
        // window used compactBannerHeight even when the rendered card was standard-sized.
        val bannerWidth = if (settings.compactBanner) settings.compactBannerWidth else settings.standardBannerWidth
        val bannerHeight = if (settings.compactBanner) settings.compactBannerHeight else settings.standardBannerHeight
        val bannerW = bannerWidth.coerceIn(180, (screenW - 8).coerceAtLeast(180))
        val widthDp = when {
            expanded && state.current is com.android.island.core.IslandActivity.WhatsApp ->
                maxOf(compactW, bannerW)
            expanded && state.current is com.android.island.core.IslandActivity.Ai ->
                // Match IslandRoot adaptive AI width — but never claim the full display so
                // taps on the home screen / dock outside the card still reach the system.
                (screenW - 20).coerceIn(300, (screenW - 8).coerceAtLeast(300))
            expanded -> maxOf(compactW, expandedW)
            banner -> maxOf(compactW + 8, bannerW)
            else -> compactW + if (state.whatsAppItems.isNotEmpty()) 56 else 0
        }
        val heightDp = when {
            expanded && state.current is com.android.island.core.IslandActivity.Ai ->
                // STAGE 72: cover the panel from the window's top edge — capsule height, user Y
                // offset, then the measured panel — capped by the ONE shared aiPanelCapDp chain.
                // Previously this branch measured the bare panel height with a hard 0.78 screen
                // cap, so a stretched panel put its chip row below the touchable surface and taps
                // fell through to the system ("chip tidak bisa menerima sentuhan").
                (compactH + settings.expandedOffsetY.coerceAtLeast(0) +
                    expandedPanelHeightDp(settings, state) + 8)
                    .coerceAtMost(
                        (com.android.island.core.WindowTuning.aiPanelCapDp(
                            resources.displayMetrics.heightPixels / density,
                            compactH.toFloat(),
                            settings.expandedOffsetY.toFloat()
                        ) + compactH + settings.expandedOffsetY.coerceAtLeast(0) + 8).toInt()
                    )
            expanded -> compactH + expandedPanelHeightDp(settings, state) + settings.expandedOffsetY.coerceAtLeast(0) + 4
            banner -> compactH + 8 + maxOf(bannerHeight, state.bannerMeasuredDp) + 28
            else -> compactH + 28
        }
        val w = (widthDp * density).toInt().coerceAtLeast(1)
        val h = (heightDp * density).toInt().coerceAtLeast(1)
        // Same shift and clamp as IslandRoot (user X offset + automatic move onto an off-centre punch hole).
        val maxShiftDp = ((screenW - compactW) / 2f - 28f).coerceAtLeast(0f)
        val shiftDp = (settings.offsetX + CutoutProfile.autoShiftDp(settings.cutoutProfile, state.cutout, screenW))
            .coerceIn(-maxShiftDp, maxShiftDp)
        val horizontalShift = if (expanded || banner) 0 else (shiftDp * density).toInt()
        val screenPx = displayWidthPx.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val x = (((screenPx - w) / 2) + horizontalShift).coerceIn(0, (screenPx - w).coerceAtLeast(0))
        val y = windowYPx(settings)
        val fingerprint = "$x,$y,$w,$h,$expanded,$banner"
        if (fingerprint == lastTouchBounds) return
        lp.width = w; lp.height = h; lp.x = x; lp.y = y
        try {
            if (target.isAttachedToWindow) windowManager.updateViewLayout(target, lp)
            lastTouchBounds = fingerprint
        } catch (e: Exception) {
            IslandDiagnostics.record("TOUCH_BOUNDS_FAILED", "${e.javaClass.simpleName}:${e.message} bounds=$fingerprint")
        }
    }

    private class TouchForwardingView(context: android.content.Context, private val forward: (MotionEvent) -> Boolean) : FrameLayout(context) {
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean = forward(ev)
    }

    /** True while an AI window (typing card, file picker) is on top and the island is invisible. */
    @Volatile private var overlayHidden = false

    /**
     * Hiding the overlay used to only set alpha=0 on the draw window. The separate touch window
     * kept its full panel-sized rectangle and still forwarded taps into the invisible panel, so
     * it sat above the typing card and swallowed every tap there. While hidden, the touch window
     * is made untouchable so every tap reaches the window underneath.
     */
    private fun applyOverlayHidden(hidden: Boolean) {
        overlayHidden = hidden
        overlayView?.alpha = if (hidden) 0f else 1f
        val view = touchView ?: return
        val lp = touchLayoutParams ?: return
        val notTouchable = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val newFlags = if (hidden) lp.flags or notTouchable else lp.flags and notTouchable.inv()
        if (newFlags == lp.flags) return
        lp.flags = newFlags
        try {
            if (view.isAttachedToWindow) windowManager.updateViewLayout(view, lp)
        } catch (_: Exception) {
        }
    }

    private fun baseFlags(focusable: Boolean = false): Int {
        // NOT_TOUCH_MODAL + WATCH_OUTSIDE_TOUCH: touches outside island go to system
        // (quick panel / status bar stay usable). Reference uses the same pattern.
        var flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            // Lets the draw window reach the very top edge of the display on every ROM, so the
            // notch is flush with the screen top. Part of baseFlags so focus changes keep it.
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (!focusable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        return flags
    }

    /**
     * Allow soft-keyboard when user opens quick-reply field.
     * Call with true when reply UI is shown, false when dismissed.
     */
    /**
     * Overlay IME is intentionally not used for AI typing (see [com.android.island.ai.AiComposerActivity]).
     * This only toggles focus flags for legacy reply fields if needed; the draw window stays
     * FLAG_NOT_TOUCHABLE so it never blocks the system UI or shifts when the keyboard opens.
     */
    fun setOverlayFocusable(focusable: Boolean) {
        val lp = layoutParams ?: return
        val view = overlayView ?: return
        val notTouchable = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        // Always keep NOT_TOUCHABLE: touches stay on the small touch-forwarder window.
        // Never apply SOFT_INPUT_ADJUST_RESIZE here — it shifted the island to the left.
        lp.flags = if (focusable) {
            baseFlags(focusable = true) or notTouchable
        } else {
            baseFlags(focusable = false) or notTouchable
        }
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        try {
            windowManager.updateViewLayout(view, lp)
            IslandDiagnostics.recordLazy("WINDOW_FOCUS_MODE") {
                "focusable=$focusable flags=${lp.flags} softInput=${lp.softInputMode}"
            }
            if (focusable) {
                view.isFocusable = true
                view.isFocusableInTouchMode = true
            } else {
                view.clearFocus()
                view.isFocusable = false
                view.isFocusableInTouchMode = false
            }
        } catch (_: Exception) {
        }
    }

    /** STAGE 73: keep the tall window briefly after the AI panel closes (collapse morph is still painting). */
    private val PANEL_CLOSE_GRACE_MS = 1200L
    private var lastPanelOpen = false
    private var panelCloseGraceUntilMs = 0L

    private fun applyLayout(s: IslandSettings) {
        layoutParams?.let { lp ->
            // Capsule anchor Y is controlled only by the global notch offset. The
            // expanded card's independent offset is applied inside IslandRoot.
            val desiredY = windowYPx(s)
            // STAGE 73 (RAM): the window now shrinks as soon as the AI panel closes. The collapse
            // morph (spring + jelly crest) keeps painting the old, taller panel for a moment —
            // shrinking the window to the closed-state budget at the same instant would slice the
            // morphing panel flat at the window bottom (the "terpotong" class of bug). For a short
            // grace window after the close the previous height is kept, then the window settles
            // into the smaller closed-state budget. Opening grows immediately: a bigger window
            // with transparent room never clips anything.
            val panelOpen = IslandBus.state.value.current is com.android.island.core.IslandActivity.Ai
            if (lastPanelOpen && !panelOpen) {
                panelCloseGraceUntilMs = android.os.SystemClock.uptimeMillis() + PANEL_CLOSE_GRACE_MS
            }
            lastPanelOpen = panelOpen
            val closedBudgetH = targetWindowHeightPx(s)
            val desiredH = if (android.os.SystemClock.uptimeMillis() < panelCloseGraceUntilMs) {
                maxOf(closedBudgetH, lp.height)
            } else closedBudgetH
            val (laneW, laneX) = drawLane(s)
            drawLaneX = laneX
            val changed = lp.x != laneX || lp.width != laneW || lp.y != desiredY || lastAppliedWindowY != desiredY || lp.height != desiredH
            if (!changed) return
            lp.x = laneX
            lp.width = laneW
            lp.y = desiredY
            lp.height = desiredH
            try {
                overlayView?.let { windowManager.updateViewLayout(it, lp) }
                lastAppliedWindowY = desiredY
            } catch (e: Exception) {
                IslandDiagnostics.record("WINDOW_LAYOUT_FAILED", "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    @Composable
    private fun OverlayContent() {
        val uiState by IslandBus.state.collectAsState()
        val settings by settingsFlow.collectAsState()
        if (!settings.enabled) return
        // Position is controlled once by WindowManager: auto mode is flush to the display top,
        // manual mode uses the calibrated offsetY. Avoid applying the offset twice in Compose.
        val topPad = 0
        IslandRoot(
            state = uiState.copy(glassConfig = settings.glassConfig),
            settings = settings,
            topOffsetDp = topPad,
            onExpand = { IslandBus.expand() },
            onCollapse = { IslandBus.collapse() },
            onOutsideTap = { IslandBus.collapse() },
            measuredScreenWidthDp = displayWidthDp.takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
        )
    }

    private fun disposeOverlay() {
        windowShrunk = false
        shrinkScheduled = false
        shrinkHandler.removeCallbacks(shrinkRunnable)
        val view = overlayView ?: return
        IslandDiagnostics.record("WINDOW_DISPOSE", "view=${view.width}x${view.height} attached=${view.isAttachedToWindow}")
        overlayLayoutListener?.let { view.removeOnLayoutChangeListener(it) }
        overlayLayoutListener = null
        view.setOnTouchListener(null)
        view.setOnClickListener(null)
        // Always try to remove, and remove immediately. Skipping the call when isAttachedToWindow is false
        // left the window registered with WindowManager after the system had detached it, so one window
        // (surfaces, GPU buffers, ViewRootImpl) leaked per screen-off/on cycle. A view that is not in the
        // manager throws, which the catch absorbs.
        try {
            if (::windowManager.isInitialized) windowManager.removeViewImmediate(view)
        } catch (_: Exception) {}
        view.disposeComposition()
        overlayView = null
        layoutParams = null
        touchHandler.removeCallbacks(touchBoundsUpdate)
        touchView?.let { touch ->
            try { windowManager.removeViewImmediate(touch) } catch (_: Exception) {}
        }
        touchView = null
        touchLayoutParams = null
        lastTouchBounds = ""
    }

    private fun updateOverlayPresence(visible: Boolean) {
        if (visible) {
            // After a long idle with the screen off, the system can detach the overlay window. The reference
            // stays set, so the old code kept a dead ComposeView (it disposes its composition when detached)
            // and never drew again until the service restarted. A detached view is rebuilt here instead.
            if (overlayView?.isAttachedToWindow == false || touchView?.isAttachedToWindow == false) {
                IslandDiagnostics.record("RENDERER_RESTART", "reason=detached")
                disposeOverlay()
            }
            if (overlayView == null) {
                IslandDiagnostics.record("RENDERER_START", "reason=visible")
                createOverlay()
            }
            val view = overlayView ?: return
            if (view.visibility != View.VISIBLE) view.visibility = View.VISIBLE
            view.requestLayout()
            if (touchView == null) createTouchWindow()
            scheduleTouchWindowUpdate("visibility")
        } else {
            // Dispose the ComposeView and its composition while idle, rather than
            // keeping a hidden overlay tree alive in memory.
            IslandDiagnostics.record("RENDERER_STOP", "reason=hidden compositionDisposed=true")
            disposeOverlay()
            shrinkHandler.postDelayed({ com.android.island.util.NativeTrim.trimWhenIdle("hidden") }, 1_500L)
        }
    }

    private fun shouldShowOverlay(settings: IslandSettings, state: IslandUiState): Boolean {
        com.android.island.core.ScreenState.refresh(this)
        if (!settings.enabled) return false
        // Screen off: nothing is visible, so the window (its surfaces and GPU buffers) is released. It is
        // built again when the screen comes back on, from the same state, so the content is unchanged.
        if (!com.android.island.core.ScreenState.interactive.value) return false
        // Lock screen: only a call (phone or WhatsApp) stays visible. No idle capsule, no other banner.
        if (com.android.island.core.ScreenState.locked.value) {
            return state.islandState != IslandState.HIDDEN &&
                state.current is com.android.island.core.IslandActivity.Call
        }
        val hasActiveContent = state.islandState != IslandState.HIDDEN && state.current != null
        val showIdleNotch = settings.iphoneNotchEnabled && !settings.hideNotchWhenIdle &&
            state.islandState == IslandState.HIDDEN
        return hasActiveContent || showIdleNotch
    }

    private fun observeVisibility() {
        // Screen back on: check the overlay now, so a window lost during idle is rebuilt before the next notification.
        scope.launch {
            com.android.island.core.ScreenState.interactive.collect {
                // Both directions: off releases the window, on rebuilds it when there is content to show.
                updateOverlayPresence(shouldShowOverlay(settingsFlow.value, IslandBus.state.value))
            }
        }
        // Re-evaluate as soon as the lock screen appears or goes away.
        scope.launch {
            com.android.island.core.ScreenState.locked.collect {
                IslandDiagnostics.record("KEYGUARD", "locked=$it")
                val state = IslandBus.state.value
                updateOverlayPresence(shouldShowOverlay(settingsFlow.value, state))
                if (!it) {
                    applyLayout(settingsFlow.value)
                    scheduleTouchWindowUpdate("keyguard")
                }
            }
        }
        scope.launch {
            IslandBus.state.collect { state ->
                if (state.islandState == IslandState.COMPACT) scheduleWindowShrink() else cancelWindowShrink("state=${state.islandState}")
                updateIdleTrim(state.islandState == IslandState.COMPACT)
                IslandDiagnostics.recordLazy("ISLAND_STATE") { "state=${state.islandState} current=${state.current?.javaClass?.simpleName} waCount=${state.whatsAppItems.size}" }
                // The fingerprint (a hash over the whole state, lyrics included) is only needed for the log.
                val fingerprint = if (IslandDiagnostics.isEnabled()) 31 * state.hashCode() + settingsFlow.value.hashCode() else lastStateFingerprint
                if (fingerprint != lastStateFingerprint) {
                    lastStateFingerprint = fingerprint
                }
                updateOverlayPresence(shouldShowOverlay(settingsFlow.value, state))
                applyLayout(settingsFlow.value)
                scheduleTouchWindowUpdate("state")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()
        // Cheap foreground tracking for the AI screen analysis (three strings, no parsing).
        com.android.island.ai.ScreenReader.onWindowStateChanged(pkg, event.className)
        // While the AI is acting, its own taps and app switches fire these events: they are not the
        // user reading the chat, so the banners must stay (clearing them here made banners vanish mid-run).
        // Recents/launcher took over: an AI card left open here keeps its touch window sized to
        // the card, which swallows taps on the apps underneath while nothing is drawn. Close it.
        // Only the Recents screen counts. Every other systemui window (heads-up notification, volume
        // bar, screenshot, edge panel...) fires this same event and used to close the AI panel on
        // its own while nobody touched anything.
        if (pkg == "com.android.systemui" &&
            event.className?.toString()?.contains("recents", ignoreCase = true) == true &&
            com.android.island.core.IslandBus.state.value.current is com.android.island.core.IslandActivity.Ai &&
            com.android.island.ai.AiSession.status.value == com.android.island.ai.AiSession.Status.IDLE
        ) {
            com.android.island.core.IslandBus.closeAi()
        }
        // Opening an app the user was notified about clears that app's cards (any app, not only WhatsApp).
        // The list check is a cheap in-memory scan, so unrelated window events cost nothing.
        if (pkg != null &&
            com.android.island.ai.AiSession.status.value == com.android.island.ai.AiSession.Status.IDLE &&
            com.android.island.core.IslandBus.state.value.whatsAppItems.any { it.packageName == pkg }
        ) {
            com.android.island.core.IslandBus.clearWhatsApp(packageName = pkg)
        }
    }
    override fun onInterrupt() {}

    override fun onDestroy() {
        IslandDiagnostics.record("SERVICE_DESTROY", "service destroyed")
        isRunning = false
        instance = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        shrinkHandler.removeCallbacks(idleTrimRunnable)
        unregisterDisplayListener()
        disposeOverlay()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile var isRunning: Boolean = false
            private set

        @Volatile
        private var instance: IslandAccessibilityService? = null

        fun setFocusable(focusable: Boolean) {
            instance?.setOverlayFocusable(focusable)
        }

        /** Hides or shows the whole overlay (AI panel and island) without destroying it. */
        fun setOverlayHidden(hidden: Boolean) {
            instance?.applyOverlayHidden(hidden)
        }

        /** Root of the foreground app window, not the overlay. */
        private fun appRoot(): android.view.accessibility.AccessibilityNodeInfo? {
            val svc = instance ?: return null
            val appWindow = runCatching {
                svc.windows?.firstOrNull {
                    it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION && it.isActive
                }
            }.getOrNull()
            return appWindow?.root ?: svc.rootInActiveWindow
        }

        /** Clicks an exact visible label; substring matches are unsafe for short labels. */
        fun clickByText(text: String): Boolean {
            val root = appRoot() ?: return false
            val wanted = text.trim().replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)
            if (wanted.isBlank()) return false
            val matches = root.findAccessibilityNodeInfosByText(text).orEmpty().filterNot { it.isEditable }
            val exact = matches.filter { node ->
                listOf(node.text, node.contentDescription).any { value ->
                    value?.toString()?.trim()?.replace(Regex("\\s+"), " ")
                        ?.equals(wanted, ignoreCase = true) == true
                }
            }
            // Never fall back to the first partial match: e.g. "Cari" must not tap
            // a TikTok suggestion labelled "Pencarian · ...".
            for (node in exact) {
                var cur: android.view.accessibility.AccessibilityNodeInfo? = node
                while (cur != null) {
                    if (cur.isClickable) {
                        return cur.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    cur = cur.parent
                }
            }
            if (exact.isNotEmpty()) return exact.firstOrNull()?.let { tapNode(it) } ?: false
            // No exact label. Results often add decoration ("Ummi Faris 💕", "Ummi Faris · 3 pesan"), so accept a
            // label that STARTS with the wanted text, but only when the wanted text is specific (4+ characters),
            // the match is a single visible node, and it is not a related-search suggestion.
            if (wanted.length < 4) return false
            val loose = matches.filter { node ->
                node.isVisibleToUser && listOf(node.text, node.contentDescription).any { value ->
                    val v = value?.toString()?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
                    v.startsWith(wanted, ignoreCase = true) && !com.android.island.ai.SearchControlMatcher.isSuggestionChip(v)
                }
            }
            val only = loose.singleOrNull() ?: return false
            var cur: android.view.accessibility.AccessibilityNodeInfo? = only
            while (cur != null) {
                if (cur.isClickable) return cur.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                cur = cur.parent
            }
            return tapNode(only)
        }

        /** Taps the centre of a node on screen, for text that is visible but not clickable itself. */
        private fun tapNode(node: android.view.accessibility.AccessibilityNodeInfo): Boolean {
            val svc = instance ?: return false
            val r = android.graphics.Rect()
            node.getBoundsInScreen(r)
            if (r.isEmpty) return false
            val path = android.graphics.Path().apply { moveTo(r.exactCenterX(), r.exactCenterY()) }
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 60)
            val gesture = android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build()
            return svc.dispatchGesture(gesture, null, null)
        }

        /** Scrolls the first scrollable container of the foreground app. */
        fun scroll(down: Boolean): Boolean {
            val root = appRoot() ?: return false
            val container = findScrollable(root)
            // Feeds such as TikTok often expose no scrollable node: fall back to a real swipe.
            if (container == null) return swipe(down)
            val action = if (down) {
                android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            return container.performAction(action) || swipe(down)
        }

        /** Full-screen directional swipe, distinct from a semantic scroll-container action. */
        fun swipe(down: Boolean): Boolean = swipeDirection(if (down) "DOWN" else "UP")

        fun swipeDirection(direction: String): Boolean {
            val svc = instance ?: return false
            val dm = svc.resources.displayMetrics
            val w = dm.widthPixels.toFloat()
            val h = dm.heightPixels.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val coords = when (direction.uppercase()) {
                "LEFT" -> floatArrayOf(w * .78f, cy, w * .22f, cy)
                "RIGHT" -> floatArrayOf(w * .22f, cy, w * .78f, cy)
                "UP" -> floatArrayOf(cx, h * .72f, cx, h * .28f)
                else -> floatArrayOf(cx, h * .28f, cx, h * .72f)
            }
            val path = android.graphics.Path().apply { moveTo(coords[0], coords[1]); lineTo(coords[2], coords[3]) }
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 260)
            return svc.dispatchGesture(android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }

        fun longPressText(text: String): Boolean {
            val svc = instance ?: return false
            val node = findVisibleTextNode(text) ?: return false
            val bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }
            if (bounds.isEmpty) return false
            val path = android.graphics.Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 750)
            return svc.dispatchGesture(android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }

        fun dragTextToText(fromText: String, toText: String): Boolean {
            val svc = instance ?: return false
            val from = findVisibleTextNode(fromText) ?: return false
            val to = findVisibleTextNode(toText) ?: return false
            val a = android.graphics.Rect().also { from.getBoundsInScreen(it) }
            val b = android.graphics.Rect().also { to.getBoundsInScreen(it) }
            if (a.isEmpty || b.isEmpty) return false
            val path = android.graphics.Path().apply {
                moveTo(a.exactCenterX(), a.exactCenterY())
                lineTo(b.exactCenterX(), b.exactCenterY())
            }
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 500)
            return svc.dispatchGesture(android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }

        /** Two-finger pinch. Only invoke after the foreground content is known to support zoom. */
        fun pinch(zoomIn: Boolean): Boolean {
            val svc = instance ?: return false
            val dm = svc.resources.displayMetrics
            val cx = dm.widthPixels / 2f
            val cy = dm.heightPixels / 2f
            val spread = minOf(dm.widthPixels, dm.heightPixels) * .16f
            val inner = spread * .35f
            val leftStart = if (zoomIn) cx - inner else cx - spread
            val rightStart = if (zoomIn) cx + inner else cx + spread
            val leftEnd = if (zoomIn) cx - spread else cx - inner
            val rightEnd = if (zoomIn) cx + spread else cx + inner
            val builder = android.accessibilityservice.GestureDescription.Builder()
            listOf(leftStart to leftEnd, rightStart to rightEnd).forEach { (startX, endX) ->
                val path = android.graphics.Path().apply { moveTo(startX, cy); lineTo(endX, cy) }
                builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 350))
            }
            return svc.dispatchGesture(builder.build(), null, null)
        }

        private fun findVisibleTextNode(text: String): android.view.accessibility.AccessibilityNodeInfo? {
            val root = appRoot() ?: return null
            return root.findAccessibilityNodeInfosByText(text).orEmpty().firstOrNull { node ->
                if (!node.isVisibleToUser) return@firstOrNull false
                val bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }
                !bounds.isEmpty && (node.text?.toString()?.contains(text, true) == true ||
                    node.contentDescription?.toString()?.contains(text, true) == true)
            }
        }

        private fun findScrollable(node: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
            // Prefer the largest visible scrollable region: it is usually the feed/list, not
            // a tiny nested control. Continue walking so nested candidates can be compared.
            var best: android.view.accessibility.AccessibilityNodeInfo? = null
            var bestArea = -1L
            fun visit(current: android.view.accessibility.AccessibilityNodeInfo, depth: Int) {
                if (depth > 24) return
                if (current.isScrollable && current.isVisibleToUser) {
                    val r = android.graphics.Rect().also(current::getBoundsInScreen)
                    val area = if (r.isEmpty) 0L else r.width().toLong() * r.height().toLong()
                    if (area > bestArea) { best = current; bestArea = area }
                }
                for (i in 0 until current.childCount) current.getChild(i)?.let { visit(it, depth + 1) }
            }
            visit(node, 0)
            return best
        }

        fun goBack(): Boolean =
            instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK) == true

        /** Home, recents, notifications, quick settings, lock, screenshot, power menu. */
        fun performSystemKey(key: String): Boolean {
            val svc = instance ?: return false
            val action = when (key) {
                "HOME" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
                "RECENTS" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
                "NOTIFICATIONS" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                "QUICK_SETTINGS" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
                "LOCK" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                "SCREENSHOT" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                "POWER" -> android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
                else -> return false
            }
            return svc.performGlobalAction(action)
        }

        /** Text and descriptions visible in the foreground app. */
        fun visibleTexts(): List<String> {
            val root = appRoot() ?: return emptyList()
            val out = LinkedHashSet<String>()
            collectTexts(root, out, 0)
            return out.toList()
        }

        private fun collectTexts(node: android.view.accessibility.AccessibilityNodeInfo, out: MutableSet<String>, depth: Int) {
            if (depth > 40 || out.size >= 150) return
            if (node.isVisibleToUser) {
                node.text?.toString()?.trim()?.takeIf { it.length in 2..200 }?.let { out.add(it) }
                node.contentDescription?.toString()?.trim()?.takeIf { it.length in 2..200 }?.let { out.add(it) }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { collectTexts(it, out, depth + 1) }
            }
        }

        private fun findEditable(node: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
            if (node.isEditable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findEditable(child)?.let { return it }
            }
            return null
        }

        /** Presses the keyboard's enter/search key in the focused field (Android 11+). */
        fun pressEnter(): Boolean {
            if (android.os.Build.VERSION.SDK_INT < 30) return false
            val root = appRoot() ?: return false
            val field = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
                ?: findEditable(root) ?: return false
            return field.performAction(
                android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
            )
        }

        /** Taps raw screen coordinates. Needs canPerformGestures in the service config. */
        fun tapAt(x: Float, y: Float): Boolean {
            val svc = instance ?: return false
            val dm = svc.resources.displayMetrics
            if (x < 0f || y < 0f || x > dm.widthPixels || y > dm.heightPixels) return false
            val path = android.graphics.Path().apply { moveTo(x, y) }
            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 60)
            return svc.dispatchGesture(android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }

        /** Sets text in the focused editable field, search boxes included. Never a password field. */
        fun typeIntoFocused(text: String): Boolean {
            val root = appRoot() ?: return false
            val target = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            if (!target.isEditable || target.isPassword) return false
            val args = android.os.Bundle().apply {
                putCharSequence(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            return target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        fun screenSize(): Pair<Int, Int>? {
            val dm = instance?.resources?.displayMetrics ?: return null
            return dm.widthPixels to dm.heightPixels
        }

        /**
         * Centre of the keyboard's action key (magnifier / Go / Enter) for a finger-like tap through Shizuku.
         * Uses the key's own bounds when the keyboard exposes it; otherwise the bottom-right corner of the
         * keyboard window, where every common keyboard puts that key.
         */
        fun imeActionKeyCenter(): Pair<Int, Int>? {
            val svc = instance ?: return null
            val win = runCatching {
                svc.windows?.firstOrNull { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            }.getOrNull() ?: return null
            val bounds = android.graphics.Rect().also { win.getBoundsInScreen(it) }
            if (bounds.isEmpty) return null
            val imeRoot = runCatching { win.root }.getOrNull()
            if (imeRoot != null) {
                val all = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
                collectAll(imeRoot, all, 0)
                val key = all.firstOrNull { n ->
                    n.isVisibleToUser && listOf(n.text, n.contentDescription).any {
                        it?.toString()?.trim()?.lowercase(java.util.Locale.ROOT) in IME_ACTION_LABELS
                    }
                }
                if (key != null) {
                    val r = android.graphics.Rect().also { key.getBoundsInScreen(it) }
                    if (!r.isEmpty) return r.centerX() to r.centerY()
                }
            }
            return (bounds.right - bounds.width() * 8 / 100) to (bounds.bottom - bounds.height() * 11 / 100)
        }

        private val IME_ACTION_LABELS = setOf("search", "cari", "telusuri", "go", "enter", "return", "done", "selesai")

        /**
         * Taps the keyboard's own action key (magnifier / Go / Enter) through the input-method window.
         * This is the route for apps that ignore ACTION_IME_ENTER. Returns false when the keyboard
         * window is not exposed or has no such key.
         */
        fun tapImeActionKey(): Boolean {
            val svc = instance ?: return false
            val imeRoot = runCatching {
                svc.windows?.firstOrNull {
                    it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }?.root
            }.getOrNull() ?: return false
            val all = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            collectAll(imeRoot, all, 0)
            val key = all.firstOrNull { n ->
                n.isVisibleToUser && listOf(n.text, n.contentDescription).any {
                    it?.toString()?.trim()?.lowercase(java.util.Locale.ROOT) in IME_ACTION_LABELS
                }
            } ?: return false
            var cur: android.view.accessibility.AccessibilityNodeInfo? = key
            while (cur != null) {
                if (cur.isClickable && cur.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return true
                cur = cur.parent
            }
            return tapNode(key)
        }

        private val SEARCH_WORDS = listOf("cari", "search", "telusuri")

        /** True when the focused input is a search field (by hint, view id, class or label). */
        fun focusedIsSearchField(): Boolean {
            val root = appRoot() ?: return false
            val field = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
            if (field.isPassword) return false
            val hint = field.hintText?.toString().orEmpty()
            val id = field.viewIdResourceName.orEmpty()
            val cls = field.className?.toString().orEmpty()
            return SEARCH_WORDS.any { w ->
                hint.contains(w, true) || id.contains(w, true) || cls.contains(w, true)
            } || looksLikeSearch(field)
        }

        private fun topOf(node: android.view.accessibility.AccessibilityNodeInfo): Int =
            android.graphics.Rect().also { node.getBoundsInScreen(it) }.top

        /** Right screen edge of a node; used by tapSearchButton to prefer the top-right magnifier. */
        private fun rightOf(node: android.view.accessibility.AccessibilityNodeInfo): Int =
            android.graphics.Rect().also { node.getBoundsInScreen(it) }.right

        /** True when a node's label, description, hint or view id names a search control or field. */
        private fun looksLikeSearch(node: android.view.accessibility.AccessibilityNodeInfo): Boolean =
            com.android.island.ai.SearchControlMatcher.isSearchControl(
                text = node.text?.toString().orEmpty(),
                description = node.contentDescription?.toString().orEmpty(),
                hint = node.hintText?.toString().orEmpty(),
                viewId = node.viewIdResourceName.orEmpty()
            )

        private fun isImageLike(node: android.view.accessibility.AccessibilityNodeInfo): Boolean =
            node.className?.toString()?.contains("Image", ignoreCase = true) == true

        private fun labelOf(node: android.view.accessibility.AccessibilityNodeInfo): String =
            node.text?.toString()?.trim().orEmpty().ifBlank { node.contentDescription?.toString()?.trim().orEmpty() }

        private fun hasClickableAncestor(node: android.view.accessibility.AccessibilityNodeInfo): Boolean {
            var current: android.view.accessibility.AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) return true
                current = current.parent
            }
            return false
        }

        private fun collectAll(
            node: android.view.accessibility.AccessibilityNodeInfo,
            out: MutableList<android.view.accessibility.AccessibilityNodeInfo>,
            depth: Int
        ) {
            if (depth > 40 || out.size > 500) return
            out += node
            for (i in 0 until node.childCount) node.getChild(i)?.let { collectAll(it, out, depth + 1) }
        }

        /**
         * Taps the search control of the foreground app. Apps often expose it as an icon whose label is
         * "Search Button" rather than visible text, so labels, descriptions and ids are all checked.
         * The top part of the screen is searched first, so result cards further down are never tapped.
         * [allowBottomBar] also accepts a bottom navigation tab (Instagram, Play Store). When an icon exposes
         * no click action, the centre of the icon is tapped with a gesture instead of giving up.
         */
        fun tapSearchButton(allowBottomBar: Boolean = false): Boolean {
            val svc = instance ?: return false
            val root = appRoot() ?: return false
            val h = svc.resources.displayMetrics.heightPixels
            val topLimit = h * 38 / 100
            val bottomStart = h * 82 / 100
            val all = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            collectAll(root, all, 0)
            val candidates = all.filter { n ->
                val y = topOf(n)
                n.isVisibleToUser && !n.isEditable && looksLikeSearch(n) &&
                    (y < topLimit || (allowBottomBar && y >= bottomStart)) &&
                    (hasClickableAncestor(n) || isImageLike(n))
            }.sortedWith(compareBy(
                { if (topOf(it) < topLimit) 0 else 1 },
                // STAGE 72: within the top band, real search entry points sit at the RIGHT edge of
                // the app bar (TikTok, YouTube, WhatsApp, Telegram, Facebook). A "Cari" label on
                // the LEFT of a suggestions/results page must never win over the top-right icon.
                { if (topOf(it) < topLimit) -rightOf(it) else 0 },
                { if (it.viewIdResourceName?.contains("search", true) == true) 0 else 1 },
                { if (hasClickableAncestor(it)) 0 else 1 }
            ))
            for (node in candidates) {
                var cur: android.view.accessibility.AccessibilityNodeInfo? = node
                while (cur != null) {
                    if (cur.isClickable && cur.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return true
                    cur = cur.parent
                }
                if (tapNode(node)) return true
            }
            return false
        }

        /** What the foreground app currently offers for searching; read-only, never taps. */
        data class SearchProbe(
            val hasField: Boolean,
            val hasControl: Boolean,
            val hasHomeTab: Boolean,
            val keyboardOpen: Boolean
        )

        fun probeSearch(): SearchProbe? {
            val svc = instance ?: return null
            val root = appRoot() ?: return null
            val h = svc.resources.displayMetrics.heightPixels
            val topLimit = h * 38 / 100
            val bottomStart = h * 82 / 100
            val all = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            collectAll(root, all, 0)
            val visible = all.filter { it.isVisibleToUser }
            val fields = visible.filter { it.isEditable && !it.isPassword }
            // A focused field only counts as "the search field" when it is search-like or sits in the top area;
            // the chat message box (bottom) must never be mistaken for it.
            val hasField = fields.any { looksLikeSearch(it) || (it.isFocused && topOf(it) < topLimit) }
            val hasControl = visible.any { n ->
                val y = topOf(n)
                !n.isEditable && looksLikeSearch(n) && (y < topLimit || y >= bottomStart) &&
                    (hasClickableAncestor(n) || isImageLike(n))
            }
            val hasHome = visible.any { n ->
                val y = topOf(n)
                !n.isEditable && com.android.island.ai.SearchControlMatcher.isHomeTab(labelOf(n)) &&
                    (y < h * 20 / 100 || y > h * 75 / 100) && hasClickableAncestor(n)
            }
            return SearchProbe(hasField, hasControl, hasHome, keyboardOpen = fields.any { it.isFocused })
        }

        /** Taps the app's "Beranda" / "Home" / "Chat" tab, the way back to its main page. */
        fun tapHomeTab(): Boolean {
            val svc = instance ?: return false
            val root = appRoot() ?: return false
            val h = svc.resources.displayMetrics.heightPixels
            val all = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            collectAll(root, all, 0)
            val tabs = all.filter { n ->
                val y = topOf(n)
                n.isVisibleToUser && !n.isEditable &&
                    com.android.island.ai.SearchControlMatcher.isHomeTab(labelOf(n)) &&
                    (y < h * 20 / 100 || y > h * 75 / 100)
            }
            for (node in tabs) {
                var cur: android.view.accessibility.AccessibilityNodeInfo? = node
                while (cur != null) {
                    if (cur.isClickable && cur.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return true
                    cur = cur.parent
                }
                if (tapNode(node)) return true
            }
            return false
        }

        /**
         * Types into a search field. A focused field or one labelled as search is preferred. The topmost
         * field is used only when [allowAny] is set, which the caller does after tapping the search control.
         * Password fields are never used.
         */
        fun typeIntoSearch(text: String, allowAny: Boolean = false): Boolean {
            val root = appRoot() ?: return false
            val fields = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            collectEditable(root, fields)
            val usable = fields.filter { !it.isPassword }
            // Keep the conditional fallback grouped so the final Elvis operator applies
            // to the entire nullable candidate chain on every Kotlin compiler version.
            // A focused field only qualifies when it is search-like or in the top area, so a chat message box
            // that happens to hold focus never receives the search query.
            val topLimit = (instance?.resources?.displayMetrics?.heightPixels ?: 0) * 38 / 100
            val target: android.view.accessibility.AccessibilityNodeInfo =
                usable.firstOrNull { it.isFocused && (looksLikeSearch(it) || topOf(it) < topLimit) }
                    ?: usable.firstOrNull { looksLikeSearch(it) }
                    ?: (if (allowAny) usable.minByOrNull { topOf(it) } else null)
                    ?: return false
            target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS)
            val args = android.os.Bundle().apply {
                putCharSequence(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            return target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        /**
         * Opens the first actual content card after a search. Do not tap the first text node:
         * TikTok and other feeds expose related-search chips ("Pencarian · ...") before videos,
         * which previously made the agent open a suggestion instead of the first result.
         * Candidates must be clickable content-sized regions; tiny chips and navigation controls
         * are excluded. The uppermost content row wins, with the largest card in that row preferred.
         */
        fun tapFirstResult(): Boolean {
            val svc = instance ?: return false
            val root = appRoot() ?: return false
            val metrics = svc.resources.displayMetrics
            val screenH = metrics.heightPixels
            val screenW = metrics.widthPixels
            val all = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            collectAll(root, all, 0)
            data class Candidate(
                val node: android.view.accessibility.AccessibilityNodeInfo,
                val bounds: android.graphics.Rect,
                val label: String,
                val area: Long
            )
            val candidates = all.mapNotNull { node ->
                if (!node.isVisibleToUser || !node.isClickable || node.isEditable) return@mapNotNull null
                val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
                if (bounds.isEmpty || bounds.top < screenH * 24 / 100 || bounds.bottom > screenH * 90 / 100) return@mapNotNull null
                val label = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
                    .joinToString(" ").trim()
                val lower = label.lowercase(java.util.Locale.ROOT)
                val isSearchChrome = lower in setOf("cari", "search", "pencarian", "telusuri", "beranda", "home", "profil", "profile") ||
                    lower.startsWith("pencarian ·") || lower.startsWith("search ·") ||
                    lower.startsWith("cari ·") || lower.contains("related search")
                val area = bounds.width().toLong() * bounds.height().toLong()
                val minArea = (screenW.toLong() * screenH.toLong() * 0.012).toLong()
                val maxArea = (screenW.toLong() * screenH.toLong() * 0.62).toLong()
                if (isSearchChrome || area < minArea || area > maxArea) return@mapNotNull null
                Candidate(node, bounds, label, area)
            }
            if (candidates.isEmpty()) return false
            val firstRowTop = candidates.minOf { it.bounds.top }
            val rowTolerance = (screenH * 0.12f).toInt()
            val firstRow = candidates.filter { it.bounds.top <= firstRowTop + rowTolerance }
            val ordered = firstRow.sortedWith(
                compareByDescending<Candidate> { it.area }
                    .thenBy { it.bounds.left }
            ) + candidates.filterNot { it in firstRow }.sortedBy { it.bounds.top }
            for (candidate in ordered) {
                if (candidate.node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return true
                // Some apps expose a non-clickable text child inside the clickable result card.
                var parent = candidate.node.parent
                var depth = 0
                while (parent != null && depth++ < 5) {
                    val r = android.graphics.Rect().also(parent::getBoundsInScreen)
                    val parentArea = r.width().toLong() * r.height().toLong()
                    if (parent.isClickable && parentArea <= screenW.toLong() * screenH.toLong() * 0.62 &&
                        parent.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return true
                    parent = parent.parent
                }
            }
            return false
        }

        private fun collectTextTargets(
            node: android.view.accessibility.AccessibilityNodeInfo,
            out: MutableList<android.view.accessibility.AccessibilityNodeInfo>
        ) {
            if (node.isVisibleToUser && (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank())) out += node
            for (i in 0 until node.childCount) node.getChild(i)?.let { collectTextTargets(it, out) }
        }

        /** True when the focused input in the foreground app is a password field. */
        fun focusedIsPassword(): Boolean {
            val root = appRoot() ?: return false
            return root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)?.isPassword == true
        }

        /**
         * Types into the message box of the foreground app. Search bars are skipped, so a search query
         * never overwrites the chat text. Returns false when no suitable field is on screen.
         */
        fun typeText(text: String): Boolean {
            val root = appRoot() ?: return false
            val fields = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            collectEditable(root, fields)
            val infos = fields.map { node ->
                val r = android.graphics.Rect()
                node.getBoundsInScreen(r)
                com.android.island.ai.InputPicker.Field(
                    top = r.top,
                    hint = (node.hintText ?: node.contentDescription ?: "").toString(),
                    focused = node.isFocused,
                    password = node.isPassword
                )
            }
            val idx = com.android.island.ai.InputPicker.pick(infos) ?: return false
            val target = fields[idx]
            target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS)
            val args = android.os.Bundle().apply {
                putCharSequence(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            return target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        private fun collectEditable(
            node: android.view.accessibility.AccessibilityNodeInfo,
            out: MutableList<android.view.accessibility.AccessibilityNodeInfo>
        ) {
            if (node.isEditable && node.isVisibleToUser) out += node
            for (i in 0 until node.childCount) node.getChild(i)?.let { collectEditable(it, out) }
        }

        /**
         * On-demand screen capture for the AI assistant (null when the service is not running).
         * The accessibility tree is walked ONLY at this moment — never continuously.
         */
        /**
         * Reads the foreground app while temporarily hiding Android Island's conversation
         * panel/island overlay. This prevents the assistant UI from covering or becoming the
         * apparent screen content during agent observation. The overlay is always restored.
         */
        suspend fun captureScreenContext(): com.android.island.ai.ScreenReader.CaptureResult? {
            val svc = instance ?: return null
            return withOverlayHidden(svc) {
                kotlinx.coroutines.delay(HIDE_BEFORE_CAPTURE_MS)
                com.android.island.ai.ScreenReader.capture(svc, svc.applicationContext)
            }
        }

        /** Captures only the underlying foreground app, never the visible AI conversation panel. */
        suspend fun captureScreenImageJpeg(): ByteArray? {
            val svc = instance ?: return null
            return withOverlayHidden(svc) {
                kotlinx.coroutines.delay(HIDE_BEFORE_CAPTURE_MS)
                com.android.island.ai.ScreenReader.captureScreenshotJpeg(svc)
            }
        }

        private suspend fun <T> withOverlayHidden(
            svc: IslandAccessibilityService,
            block: suspend () -> T
        ): T {
            val view = svc.overlayView
            val previousAlpha = view?.alpha ?: 1f
            val previousVisibility = view?.visibility ?: android.view.View.VISIBLE
            // Hide visually AND from normal accessibility traversal while observing the target app.
            // The short commit delay prevents a stale frame of the AI panel entering a screenshot.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                view?.alpha = 0f
                view?.visibility = android.view.View.INVISIBLE
            }
            try {
                return block()
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                    if (svc.overlayView === view) {
                        view?.visibility = previousVisibility
                        view?.alpha = previousAlpha
                    }
                }
            }
        }
        private const val HIDE_BEFORE_CAPTURE_MS = 220L

        /**
         * Null when the service is not running; true when the running service was granted
         * window-content access (fresh config); false when it still runs with the pre-AI
         * config and needs a manual off/on toggle in system Settings.
         */
        fun canReadScreen(): Boolean? {
            val svc = instance ?: return null
            return runCatching { svc.serviceInfo?.canRetrieveWindowContent }.getOrNull()
        }
    }
}
