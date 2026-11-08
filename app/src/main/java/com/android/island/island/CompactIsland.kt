package com.android.island.island

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.CornerRadius
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.core.Controls
import com.android.island.core.IslandActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/**
 * Compact Island — AMOLED pure black.
 * Gestures:
 * - Tap / double / long as configured
 * - Vertical swipe up → expand (if enabled)
 * - Horizontal swipe → next/prev track (if swipeTracks) OR dismiss when large enough
 */
@Composable
fun CompactIsland(
    activity: IslandActivity,
    waterDrop: Boolean = true,
    iPhoneNotch: Boolean = false,
    widthDp: Dp = 112.dp,
    heightDp: Dp = 34.dp,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    swipeTracks: Boolean = true,
    swipeExpand: Boolean = true,
    onHorizontalDismiss: () -> Unit = {},
    allowHorizontalGesture: Boolean = true,
    allowSwipeDismiss: Boolean = true,
    themeMode: String = "ADAPTIVE",
    hapticsEnabled: Boolean = false,
    /** When double tap / long press are set to "none", the tap fires instantly (no double-tap wait). */
    hasDoubleTap: Boolean = true,
    hasLongPress: Boolean = true
) {
    val shape = if (iPhoneNotch) iPhoneNotchShape() else waterDropShape(topFlat = waterDrop)
    // Foreground theme never changes the compact capsule's AMOLED-black surface.
    // Compact capsule keeps its original built-in white indicator in every text theme.
    val capsuleInk = Color.White
    val capsuleBg = Color.Black
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestOnTap by rememberUpdatedState(onTap)
    val latestOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val latestOnLongPress by rememberUpdatedState(onLongPress)
    val latestOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val latestOnHorizontalDismiss by rememberUpdatedState(onHorizontalDismiss)
    val haptic = LocalHapticFeedback.current
    val tick: () -> Unit = { if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    // Content pulse: the capsule briefly "breathes" when a new track / chat arrives. It only
    // scales the capsule's own layer (anchor stays fixed, hit-box does not change).
    val pulse = remember { Animatable(1f) }
    var firstPulse by remember { mutableStateOf(true) }
    LaunchedEffect(activity.id) {
        if (firstPulse) { firstPulse = false; return@LaunchedEffect }
        pulse.snapTo(0.93f)
        pulse.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }

    // Touch feedback: the capsule dips a little under the finger and springs back (layer only;
    // the hit-box never changes). The swipe nudge below moves only the content INSIDE the capsule.
    val press = remember { Animatable(1f) }
    var nudgeX by remember { mutableFloatStateOf(0f) }

    var vDrag by remember { mutableFloatStateOf(0f) }
    var hOffset by remember { mutableFloatStateOf(0f) }
    var hDrag by remember { mutableFloatStateOf(0f) }
    var dismissAlpha by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = Modifier
            .height(heightDp)
            .width(widthDp.coerceIn(88.dp, 480.dp))
            .graphicsLayer {
                translationX = hOffset
                alpha = dismissAlpha
                scaleX = pulse.value * press.value
                scaleY = pulse.value * press.value
            }
            .clip(shape)
            .background(capsuleBg)
            .pointerInput(activity.id, hasDoubleTap, hasLongPress) {
                val doubleTapHandler: (androidx.compose.ui.geometry.Offset) -> Unit = { tick(); latestOnDoubleTap() }
                val longPressHandler: (androidx.compose.ui.geometry.Offset) -> Unit = { tick(); latestOnLongPress() }
                detectTapGestures(
                    onPress = {
                        // Spring (not a 90 ms tween) so releasing mid-press keeps its velocity instead of restarting from rest.
                        scope.launch { press.animateTo(0.95f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessHigh)) }
                        try {
                            tryAwaitRelease()
                        } finally {
                            scope.launch { press.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 520f)) }
                        }
                    },
                    onTap = { tick(); latestOnTap() },
                    onDoubleTap = if (hasDoubleTap) doubleTapHandler else null,
                    onLongPress = if (hasLongPress) longPressHandler else null
                )
            }
            .pointerInput(activity.id, swipeExpand, allowSwipeDismiss) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        when {
                            vDrag < -28f && swipeExpand -> latestOnSwipeUp()
                            allowSwipeDismiss && vDrag > 36f -> latestOnHorizontalDismiss() // optional swipe-down dismiss
                        }
                        vDrag = 0f
                    },
                    onDragCancel = { vDrag = 0f },
                    onVerticalDrag = { _, amount -> vDrag += amount }
                )
            }
            .then(if (allowHorizontalGesture) Modifier.pointerInput(swipeTracks, allowSwipeDismiss, activity.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        // Track navigation is a gesture only: never translate the capsule itself.
                        // The icons inside follow the finger a few dp (rubber band) and spring back.
                        hDrag += amount
                        val maxPx = with(density) { 6.dp.toPx() }
                        nudgeX = (hDrag * 0.28f).coerceIn(-maxPx, maxPx)
                    },
                    onDragEnd = {
                        val threshold = with(density) { 42.dp.toPx() }
                        if (swipeTracks && abs(hDrag) >= threshold) {
                            if (hDrag < 0f) Controls.next() else Controls.prev()
                        } else if (allowSwipeDismiss && abs(hDrag) >= with(density) { 64.dp.toPx() }) {
                            latestOnHorizontalDismiss()
                        }
                        hDrag = 0f
                        scope.launch {
                            Animatable(nudgeX).animateTo(
                                0f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium)
                            ) { nudgeX = value }
                        }
                    },
                    onDragCancel = {
                        hDrag = 0f
                        scope.launch {
                            Animatable(nudgeX).animateTo(
                                0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
                            ) { nudgeX = value }
                        }
                    }
                )
            } else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxSize().graphicsLayer { translationX = nudgeX },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        when (activity) {
            is IslandActivity.Media -> {
                if (activity.artwork != null) {
                    Image(
                        bitmap = androidx.compose.runtime.remember(activity.artwork) { activity.artwork.asImageBitmap() },
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = capsuleInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
                // One indicator for both states: the bars dance while playing and ease down into
                // three resting pills when paused (no cross-fade to a different shape).
                Box(
                    Modifier.size(width = 18.dp, height = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MiniWaveform(playing = activity.isPlaying, color = activity.accentColor ?: capsuleInk)
                }
            }
            is IslandActivity.Call -> {
                if (activity.avatar != null) {
                    Image(
                        bitmap = androidx.compose.runtime.remember(activity.avatar) { activity.avatar.asImageBitmap() },
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Elapsed time ticks once a second for an active call (ringing shows the name).
                val elapsed = androidx.compose.runtime.produceState(initialValue = "0:00", activity.timestamp) {
                    while (true) {
                        com.android.island.core.ScreenState.awaitInteractive()
                        val secs = ((System.currentTimeMillis() - activity.timestamp) / 1000L).coerceAtLeast(0L)
                        value = "%d:%02d".format(secs / 60L, secs % 60L)
                        kotlinx.coroutines.delay(1000L)
                    }
                }
                androidx.compose.material3.Text(
                    text = if (activity.isIncoming) activity.callerName else elapsed.value,
                    color = capsuleInk,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 6.dp)
                )
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34C759)))
            }
            is IslandActivity.WhatsApp -> {
                if (activity.avatar != null) {
                    Image(
                        bitmap = androidx.compose.runtime.remember(activity.avatar) { activity.avatar.asImageBitmap() },
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (activity.messageCount > 1) {
                    val badgePop = remember { Animatable(1f) }
                    LaunchedEffect(activity.messageCount) {
                        badgePop.snapTo(0.6f)
                        badgePop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                    }
                    Box(
                        Modifier
                            .size(16.dp)
                            .graphicsLayer { scaleX = badgePop.value; scaleY = badgePop.value }
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(
                            text = activity.messageCount.coerceAtMost(99).toString(),
                            color = capsuleInk,
                            fontSize = 9.sp
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366).copy(alpha = 0.8f))
                    )
                }
            }
        }
      }
    }
}

@Composable
private fun MiniWaveform(playing: Boolean, color: Color) {
    var phase by remember { mutableFloatStateOf(0f) }
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var attached by remember(view) { mutableStateOf(view.isAttachedToWindow) }
    var lifecycleStarted by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(view) {
        val attachListener = object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) { attached = true }
            override fun onViewDetachedFromWindow(v: android.view.View) { attached = false }
        }
        view.addOnAttachStateChangeListener(attachListener)
        attached = view.isAttachedToWindow
        onDispose { view.removeOnAttachStateChangeListener(attachListener) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> lifecycleStarted = true
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_DESTROY -> lifecycleStarted = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val animate = attached && lifecycleStarted && playing
    // Amplitude eases in/out so play <-> pause is a smooth swell, never a jump.
    val amp = animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "mini-amp"
    )
    LaunchedEffect(animate) {
        if (animate) {
            var last = android.os.SystemClock.uptimeMillis()
            while (true) {
                com.android.island.core.ScreenState.awaitInteractive()
                val now = android.os.SystemClock.uptimeMillis()
                val dt = (now - last).coerceIn(0L, 100L)
                last = now
                phase += dt * 0.0046f   // ~30 steps/s with a time-based phase: smooth and steady
                delay(com.android.island.core.DeviceProfile.tickMs)
            }
        }
    }
    Canvas(modifier = Modifier.size(width = 18.dp, height = 12.dp)) {
        val a = amp.value
        val n = 3
        val barW = size.width / (n * 2.2f)
        val tint = color.copy(alpha = color.alpha * (0.55f + 0.45f * a))
        for (i in 0 until n) {
            val wave = (sin(phase + i * 1.1f) + 1f) / 2f
            val h = size.height * (0.30f + a * wave * 0.70f)
            drawRoundRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(
                    i * (barW * 2.1f),
                    (size.height - h) / 2f
                ),
                size = androidx.compose.ui.geometry.Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }
    }
}
