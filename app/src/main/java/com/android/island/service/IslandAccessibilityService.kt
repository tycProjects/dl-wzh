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
    private var lastSettingsFingerprint: Int = 0
    private var settingsEmissionCount = 0L
    private var lastSettingsEmissionAt = 0L
    private var lastStateFingerprint: Int = 0
    private val touchBoundsUpdate = Runnable { updateTouchWindow("settled") }
    private var overlayLayoutListener: View.OnLayoutChangeListener? = null
    // Avoid issuing identical WindowManager updates for every DataStore emission.
    private var lastAppliedWindowY: Int? = null
    // Measured once before the first overlay is created; refreshed on configuration changes.
    @Volatile private var displayWidthDp: Int = 0
    @Volatile private var displayHeightDp: Int = 0
    private var lastStackHeightSeen = -1
    @Volatile private var displayDensity: Float = 1f

    private val settingsFlow = MutableStateFlow(IslandSettings())

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

        // Collect settings live — layout, glass, and enabled flag
        val app = applicationContext as? IslandApplication
        if (app != null) {
            scope.launch {
                app.settingsRepository.settings.distinctUntilChanged().collect { s ->
                    val now = SystemClock.uptimeMillis()
                    settingsEmissionCount++
                    val fingerprint = s.hashCode()
                    val delta = if (lastSettingsEmissionAt == 0L) 0L else now - lastSettingsEmissionAt
                    IslandDiagnostics.record("SETTINGS_APPLIED", "count=$settingsEmissionCount deltaMs=$delta changed=${fingerprint != lastSettingsFingerprint} enabled=${s.enabled} hideStatusIcons=${s.hideStatusIcons} x=${s.offsetX} y=${s.offsetY} diagnostics=${s.diagnosticsEnabled}")
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
    }

    /** Measure the real display before composing any island content.
     * Real metrics include the full display bounds; dp conversion is cached until
     * Android reports a configuration/display change.
     */
    private fun measureDisplayProfile() {
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        displayDensity = metrics.density.takeIf { it > 0f } ?: resources.displayMetrics.density
        displayWidthDp = (metrics.widthPixels / displayDensity).toInt().coerceAtLeast(240)
        displayHeightDp = (metrics.heightPixels / displayDensity).toInt().coerceAtLeast(320)
        IslandDiagnostics.record("DISPLAY_PROFILE", "${metrics.widthPixels}x${metrics.heightPixels}px density=$displayDensity dp=${displayWidthDp}x${displayHeightDp}")
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::windowManager.isInitialized) {
            measureDisplayProfile()
            overlayView?.requestLayout()
            scheduleTouchWindowUpdate("configuration")
            layoutParams?.let { lp ->
                lp.x = 0
                try { overlayView?.let { windowManager.updateViewLayout(it, lp) } } catch (_: Exception) {}
            }
        }
    }

    private fun createOverlay() {
        if (overlayView != null) return
        IslandDiagnostics.record("WINDOW_CREATE_BEGIN", "architecture=two-window drawWidth=MATCH_PARENT touchWindow=transparent-forwarder gravity=TOP|START")
        val s = settingsFlow.value
        // WRAP_CONTENT width + FLAG_NOT_TOUCH_MODAL:
        // - touches outside the island pass through → quick panel / status bar tetap bisa di-swipe
        // - FLAG_NOT_FOCUSABLE default: tidak mencuri fokus keyboard kecuali saat quick-reply
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            baseFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Keep the overlay window itself centered in every state. Horizontal
            // calibration is applied inside the Compose scene with reserved side room,
            // so WRAP_CONTENT width changes cannot recenter/drag the capsule.
            x = 0
            y = (s.offsetY * resources.displayMetrics.density).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
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
                IslandDiagnostics.record("DRAW_LAYOUT", "view=${v.width}x${v.height} bounds=[$left,$top,$right,$bottom] old=[$oldLeft,$oldTop,$oldRight,$oldBottom] window=${lp.width}x${lp.height} x=${lp.x} y=${lp.y}")
                scheduleTouchWindowUpdate("draw-layout")
                if (lp.x != 0) {
                    lp.x = 0
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
                    IslandDiagnostics.record("OUTSIDE_TOUCH", "state=$st replyOpen=${IslandBus.replyOpen} action=${event.action}")
                    if (st == IslandState.BANNER && IslandBus.replyOpen) {
                        IslandBus.requestCloseReply()
                    } else if (st == IslandState.EXPANDED && !IslandBus.replyOpen) {
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
            IslandDiagnostics.record("DRAW_WINDOW_CREATED", "x=${params.x} y=${params.y} w=${params.width} h=${params.height} flags=${params.flags}")
            createTouchWindow()
            scheduleTouchWindowUpdate("created")
        } catch (e: Exception) {
            IslandDiagnostics.record("WINDOW_CREATE_FAILED", "${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    /** Transparent independent input window. It forwards touch events into the fixed-width renderer. */
    private fun createTouchWindow() {
        if (touchView != null) return
        val touch = TouchForwardingView(this) { event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                val st = IslandBus.state.value.islandState
                IslandDiagnostics.record("OUTSIDE_TOUCH", "window=touch state=$st replyOpen=${IslandBus.replyOpen}")
                if (st == IslandState.BANNER && IslandBus.replyOpen) {
                    IslandBus.requestCloseReply()
                } else if (st == IslandState.EXPANDED && !IslandBus.replyOpen) {
                    IslandBus.collapse()
                }
                return@TouchForwardingView false
            }
            val target = overlayView ?: return@TouchForwardingView false
            val copy = MotionEvent.obtain(event)
            val lp = touchLayoutParams
            copy.offsetLocation((lp?.x ?: 0).toFloat(), (lp?.y ?: 0).toFloat())
            val handled = target.dispatchTouchEvent(copy)
            copy.recycle()
            IslandDiagnostics.record("TOUCH_FORWARD", "action=${event.actionMasked} x=${event.x.toInt()} y=${event.y.toInt()} forwarded=$handled")
            handled
        }
        touchView = touch
        val params = WindowManager.LayoutParams(1, 1,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "AndroidIslandTouch"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
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
        touchHandler.removeCallbacks(touchBoundsUpdate)
        val delayMs = if (reason == "state" || reason == "visibility") 250L else 48L
        IslandDiagnostics.record("TOUCH_BOUNDS_DEBOUNCE", "reason=$reason delayMs=$delayMs policy=do-not-move-during-morph")
        touchHandler.postDelayed(touchBoundsUpdate, delayMs)
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
            state.cutout.type,
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
            expanded -> maxOf(compactW, expandedW)
            banner -> maxOf(compactW + 8, bannerW)
            else -> compactW + if (state.whatsAppItems.isNotEmpty()) 56 else 0
        }
        val heightDp = when {
            expanded && state.whatsAppItems.size > 1 && state.current is com.android.island.core.IslandActivity.WhatsApp ->
                (compactH + state.whatsAppStackHeightDp.coerceAtLeast(120) + settings.expandedOffsetY.coerceAtLeast(0) + 8)
                    .coerceAtMost((resources.displayMetrics.heightPixels / density * 0.8f).toInt())
            expanded -> compactH + settings.expandedMinHeight.coerceAtLeast(118) + settings.expandedOffsetY.coerceAtLeast(0)
            banner -> compactH + 8 + bannerHeight + 28
            else -> compactH + 28
        }
        val w = (widthDp * density).toInt().coerceAtLeast(1)
        val h = (heightDp * density).toInt().coerceAtLeast(1)
        val horizontalShift = if (expanded || banner) 0 else (settings.offsetX * density).toInt()
        val x = (((resources.displayMetrics.widthPixels - w) / 2) + horizontalShift).coerceIn(0, (resources.displayMetrics.widthPixels - w).coerceAtLeast(0))
        val y = (settings.offsetY * density).toInt()
        val fingerprint = "$x,$y,$w,$h,$expanded,$banner"
        if (fingerprint == lastTouchBounds) return
        lp.width = w; lp.height = h; lp.x = x; lp.y = y
        try {
            if (target.isAttachedToWindow) windowManager.updateViewLayout(target, lp)
            lastTouchBounds = fingerprint
            IslandDiagnostics.record("TOUCH_BOUNDS_UPDATED", "reason=$reason bounds=$fingerprint state=${state.islandState}")
        } catch (e: Exception) {
            IslandDiagnostics.record("TOUCH_BOUNDS_FAILED", "${e.javaClass.simpleName}:${e.message} bounds=$fingerprint")
        }
    }

    private class TouchForwardingView(context: android.content.Context, private val forward: (MotionEvent) -> Boolean) : FrameLayout(context) {
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean = forward(ev)
    }

    private fun baseFlags(focusable: Boolean = false): Int {
        // NOT_TOUCH_MODAL + WATCH_OUTSIDE_TOUCH: touches outside island go to system
        // (quick panel / status bar stay usable). Reference uses the same pattern.
        var flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
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
    fun setOverlayFocusable(focusable: Boolean) {
        val lp = layoutParams ?: return
        val view = overlayView ?: return
        lp.flags = baseFlags(focusable = focusable) or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        if (focusable) {
            lp.softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        } else {
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
        }
        try {
            windowManager.updateViewLayout(view, lp)
            IslandDiagnostics.record("WINDOW_FOCUS_MODE", "focusable=$focusable flags=${lp.flags} softInput=${lp.softInputMode}")
            if (focusable) {
                view.isFocusable = true
                view.isFocusableInTouchMode = true
                view.requestFocus()
            } else {
                view.clearFocus()
                view.isFocusable = false
                view.isFocusableInTouchMode = false
            }
        } catch (_: Exception) {
        }
    }

    private fun applyLayout(s: IslandSettings) {
        layoutParams?.let { lp ->
            // Capsule anchor Y is controlled only by the global notch offset. The
            // expanded card's independent offset is applied inside IslandRoot.
            val desiredY = (s.offsetY * resources.displayMetrics.density).toInt()
            val changed = lp.x != 0 || lp.y != desiredY || lastAppliedWindowY != desiredY
            if (!changed) return
            lp.x = 0
            lp.y = desiredY
            try {
                overlayView?.let { windowManager.updateViewLayout(it, lp) }
                lastAppliedWindowY = desiredY
                IslandDiagnostics.record("WINDOW_LAYOUT_UPDATE", "x=${lp.x} y=${lp.y} w=${lp.width} h=${lp.height} offsetY=${s.offsetY}")
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
        val view = overlayView ?: return
        IslandDiagnostics.record("WINDOW_DISPOSE", "view=${view.width}x${view.height} attached=${view.isAttachedToWindow}")
        overlayLayoutListener?.let { view.removeOnLayoutChangeListener(it) }
        overlayLayoutListener = null
        view.setOnTouchListener(null)
        view.setOnClickListener(null)
        try {
            if (::windowManager.isInitialized && view.isAttachedToWindow) windowManager.removeView(view)
        } catch (_: Exception) {}
        view.disposeComposition()
        overlayView = null
        layoutParams = null
        touchHandler.removeCallbacks(touchBoundsUpdate)
        touchView?.let { touch ->
            try { if (touch.isAttachedToWindow) windowManager.removeView(touch) } catch (_: Exception) {}
        }
        touchView = null
        touchLayoutParams = null
        lastTouchBounds = ""
    }

    private fun updateOverlayPresence(visible: Boolean) {
        if (visible) {
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
        }
    }

    private fun shouldShowOverlay(settings: IslandSettings, state: IslandUiState): Boolean {
        if (!settings.enabled) return false
        val hasActiveContent = state.islandState != IslandState.HIDDEN && state.current != null
        val showIdleNotch = settings.iphoneNotchEnabled && !settings.hideNotchWhenIdle &&
            state.islandState == IslandState.HIDDEN
        return hasActiveContent || showIdleNotch
    }

    private fun observeVisibility() {
        scope.launch {
            IslandBus.state.collect { state ->
                IslandDiagnostics.record("ISLAND_STATE", "state=${state.islandState} current=${state.current?.javaClass?.simpleName} waCount=${state.whatsAppItems.size}")
                val fingerprint = 31 * state.hashCode() + settingsFlow.value.hashCode()
                if (fingerprint != lastStateFingerprint) {
                    IslandDiagnostics.record("STATE_OBSERVER", "state=${state.islandState} current=${state.current?.javaClass?.simpleName} changed=true")
                    lastStateFingerprint = fingerprint
                }
                updateOverlayPresence(shouldShowOverlay(settingsFlow.value, state))
                applyLayout(settingsFlow.value)
                // The stack reports its measured height on layout; keep the touch window in step.
                if (state.whatsAppStackHeightDp != lastStackHeightSeen) {
                    lastStackHeightSeen = state.whatsAppStackHeightDp
                    scheduleTouchWindowUpdate("stack")
                }
                scheduleTouchWindowUpdate("state")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        IslandDiagnostics.record("SERVICE_DESTROY", "service destroyed")
        isRunning = false
        instance = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
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
    }
}
