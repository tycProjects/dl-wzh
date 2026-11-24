package com.tyce.enhancer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.layout.layout
import android.graphics.BitmapFactory
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** pop pop, gikan sa recording */


/** menu migawas sa button, wow */
@Composable
fun PopMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.TopStart,
    boxModifier: Modifier = Modifier,
    anchor: Offset = Offset.Unspecified,
    anchorSize: Dp = 44.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(enabled = visible) { onDismiss() }
    val density = LocalDensity.current

    // usa ra value, tanan sakop
    val p = remember { Animatable(0f) }
    var active by remember { mutableStateOf(false) }
    var boxPos by remember { mutableStateOf(Offset.Zero) }
    var boxW by remember { mutableStateOf(0f) }
    var card by remember { mutableStateOf(IntSize.Zero) }

    // tago pero andam, ayaw kulba
    val blurs = remember(density) {
        List(9) { i -> if (i == 0) null else with(density) { (i * 2).dp.toPx() }.let { BlurEffect(it, it) } }
    }

    LaunchedEffect(visible) {
        if (visible) {
            active = true
            p.animateTo(1f, Motion.PopSpring)
        } else if (active) {
            p.animateTo(0f, tween(Motion.MenuExit, easing = PopExitEase))
            active = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (visible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
            )
        }
        Box(
            boxModifier
                .fillMaxSize()
                .onGloballyPositioned {
                    boxPos = it.positionInRoot()
                    boxW = it.size.width.toFloat()
                },
            contentAlignment = alignment
        ) {
            Column(
                Modifier
                    // natago, walay hikap, walay drama
                    .layout { m, c ->
                        val pl = m.measure(c)
                        if (active) layout(pl.width, pl.height) { pl.place(0, 0) } else layout(0, 0) {}
                    }
                    .graphicsLayer {
                        val t = p.value
                        val w = card.width.toFloat()
                        val h = card.height.toFloat()
                        if (w > 0f && h > 0f) {
                            val d = anchorSize.toPx()
                            val left = boxPos.x + if (alignment == Alignment.TopEnd) boxW - w else 0f
                            val top = boxPos.y
                            val cx = if (anchor.isSpecified) anchor.x else left + d / 2f
                            val cy = if (anchor.isSpecified) anchor.y else top + d / 2f
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = d / w + (1f - d / w) * t
                            scaleY = d / h + (1f - d / h) * t
                            translationX = (cx - d / 2f - left) * (1f - t)
                            translationY = (cy - d / 2f - top) * (1f - t)
                            // walay clip, paspas ang anino
                            shape = MenuShape
                            clip = false
                            shadowElevation = 18.dp.toPx() * t.coerceIn(0f, 1f)
                            alpha = (t * 6f).coerceIn(0f, 1f)
                        } else {
                            alpha = 0f
                        }
                    }
                    .onSizeChanged { card = it }
                    // nagsira, ayaw pislita duha, uy
                    .pointerInput(visible) {
                        if (!visible) {
                            awaitPointerEventScope {
                                while (true) {
                                    val e = awaitPointerEvent(PointerEventPass.Initial)
                                    e.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .drawBehind {
                        val t = p.value
                        if (size.width > 0f && size.height > 0f) {
                            val d = anchorSize.toPx()
                            val sx = d / size.width + (1f - d / size.width) * t
                            val sy = d / size.height + (1f - d / size.height) * t
                            val r = 18.dp.toPx()
                            val rad = CornerRadius(
                                (r / sx).coerceAtMost(size.width / 2f),
                                (r / sy).coerceAtMost(size.height / 2f)
                            )
                            drawRoundRect(Color.White, size = size, cornerRadius = rad)
                            drawRoundRect(C.Line, size = size, cornerRadius = rad, style = Stroke(1.dp.toPx()))
                        }
                    }
                    .width(IntrinsicSize.Max)
                    .widthIn(min = 220.dp)
                    .padding(vertical = 6.dp)
            ) {
                Column(
                    Modifier.graphicsLayer {
                        val q = ((p.value - 0.3f) / 0.5f).coerceIn(0f, 1f)
                        alpha = q
                        val step = ((1f - q) * 8f).toInt().coerceIn(0, 8)
                        renderEffect = if (q >= 1f) null else blurs[step]
                    }
                ) { content() }
            }
        }
    }
}

private val MenuShape = RoundedCornerShape(18.dp)

/** dialog sa tunga, pop-pop */
@Composable
fun PopDialog(
    visible: Boolean,
    title: String,
    message: String,
    cancelText: String = "Cancel",
    confirmText: String,
    confirmColor: Color = C.Red,
    image: Int? = null,
    showCancel: Boolean = true,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    BackHandler(enabled = visible) { onCancel() }
    val shape = RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(Motion.Base)), exit = fadeOut(tween(Motion.Base))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCancel() }
            )
        }
        Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(Motion.PopSpring, initialScale = 0.7f) + fadeIn(tween(Motion.Fast)),
                exit = scaleOut(tween(Motion.PopExit, easing = PopExitEase), targetScale = Motion.PopExitScale) + fadeOut(tween(Motion.PopExit))
            ) {
                Column(
                    Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .shadow(24.dp, shape, ambientColor = Color(0x44000000), spotColor = Color(0x44000000))
                        .clip(shape)
                        .background(Color.White)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                ) {
                    if (image != null) {
                        Image(
                            painterResource(image),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp)) {
                        Text(title, style = heading(18f, FontWeight.SemiBold, C.Text))
                        Box(Modifier.height(8.dp))
                        Text(message, style = inter(15f, FontWeight.Normal, C.Muted, 1.4f))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
                    Row(Modifier.fillMaxWidth().height(52.dp)) {
                        if (showCancel) {
                            DialogButton(cancelText, C.Text, Modifier.weight(1f), onCancel)
                            Box(Modifier.width(1.dp).fillMaxHeight().background(C.Line))
                        }
                        DialogButton(confirmText, confirmColor, Modifier.weight(1f), onConfirm)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxSize().pressable(0.97f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = inter(16f, FontWeight.SemiBold, color), textAlign = TextAlign.Center)
    }
}

private val GlassShape = RoundedCornerShape(22.dp)

@Composable
fun GlassDialog(
    visible: Boolean,
    origin: Offset,
    title: String,
    message: String,
    confirmText: String,
    image: Int? = null,
    onDismiss: () -> Unit
) {
    val p = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf(origin) }
    val density = LocalDensity.current
    val context = LocalContext.current

    // decode una, para dili mag-lag
    val bmp by produceState<ImageBitmap?>(null, image) {
        value = if (image == null) null else withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(context.resources, image)?.asImageBitmap()
        }
    }

    // blur kausa ra, tipid
    val blurs = remember(density) {
        List(9) { i -> if (i == 0) null else with(density) { (i * 2).dp.toPx() }.let { BlurEffect(it, it) } }
    }

    LaunchedEffect(visible) {
        if (visible) {
            from = origin
            shown = true
            p.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 170f))
        } else if (shown) {
            p.animateTo(0f, tween(300, easing = PopExitEase))
            shown = false
        }
    }
    BackHandler(enabled = visible) { onDismiss() }
    if (!shown) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rootW = constraints.maxWidth.toFloat()
        val rootH = constraints.maxHeight.toFloat()
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = p.value.coerceIn(0f, 1f) }
                .background(Color(0x80000000))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
        )
        Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
            var box by remember { mutableStateOf(IntSize.Zero) }
            Column(
                Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .onSizeChanged { box = it }
                    .graphicsLayer {
                        // draw ra tanan, walay recompose
                        val t = p.value
                        if (box.width > 0 && box.height > 0) {
                            val d = 56.dp.toPx()
                            scaleX = d / box.width + (1f - d / box.width) * t
                            scaleY = d / box.height + (1f - d / box.height) * t
                            translationX = (from.x - rootW / 2f) * (1f - t)
                            translationY = (from.y - rootH / 2f) * (1f - t)
                            // parehas ra, walay clip, boom
                            shape = GlassShape
                            clip = false
                            shadowElevation = 24.dp.toPx() * t.coerceIn(0f, 1f)
                            alpha = (t * 6f).coerceIn(0f, 1f)
                        } else {
                            alpha = 0f
                        }
                    }
                    .drawBehind {
                        val t = p.value
                        if (size.width > 0f && size.height > 0f) {
                            val d = 56.dp.toPx()
                            val sx = d / size.width + (1f - d / size.width) * t
                            val sy = d / size.height + (1f - d / size.height) * t
                            val r = d / 2f + (22.dp.toPx() - d / 2f) * t
                            drawRoundRect(
                                Color.White,
                                size = size,
                                cornerRadius = CornerRadius(
                                    (r / sx).coerceAtMost(size.width / 2f),
                                    (r / sy).coerceAtMost(size.height / 2f)
                                )
                            )
                        }
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            ) {
                Column(
                    Modifier.graphicsLayer {
                        val q = ((p.value - 0.3f) / 0.5f).coerceIn(0f, 1f)
                        alpha = q
                        val step = ((1f - q) * 8f).toInt().coerceIn(0, 8)
                        renderEffect = if (q >= 1f) null else blurs[step]
                    }
                ) {
                    val b = bmp
                    if (image != null && b != null) {
                        Image(
                            b,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp)) {
                        Text(title, style = heading(18f, FontWeight.SemiBold, C.Text))
                        Box(Modifier.height(8.dp))
                        Text(message, style = inter(15f, FontWeight.Normal, C.Muted, 1.4f))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
                    Row(Modifier.fillMaxWidth().height(52.dp)) {
                        DialogButton(confirmText, C.Accent, Modifier.weight(1f), onDismiss)
                    }
                }
            }
        }
    }
}
