package com.android.island.island

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.core.Controls
import com.android.island.core.GlassConfig
import com.android.island.core.IslandActivity

private val CallBannerGreen = Color(0xFF34C759)
private val CallBannerRed = Color(0xFFE53935)

/**
 * Incoming / active call shown as a banner, styled like the WhatsApp banner: liquid glass with the
 * contact photo as a faint backdrop, and the same compact/standard size and text settings.
 * Buttons respond to presses (scale) and fire the notification's own PendingIntents.
 */
@Composable
fun CallBanner(
    call: IslandActivity.Call,
    glassConfig: GlassConfig,
    showAlbumBackground: Boolean,
    compactMode: Boolean,
    compactWidthDp: Int,
    compactHeightDp: Int,
    standardWidthDp: Int,
    standardHeightDp: Int,
    bannerFontSize: Int,
    bannerFontWeight: Int,
    backgroundOpacity: Float,
    edgeLensStrength: Float,
    lowPowerMode: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(if (compactMode) 20.dp else 24.dp)
    val accent = CallBannerGreen
    val art = rememberArtBackdrop(call.avatar, enabled = showAlbumBackground).value
    val driftPhase = rememberDriftPhase(animate = false)
    val width: Dp = (if (compactMode) compactWidthDp else standardWidthDp).dp
    val minHeight: Dp = (if (compactMode) compactHeightDp else standardHeightDp).dp
    val ink = Color.White
    val buttonSize: Dp = if (compactMode) 34.dp else 42.dp
    val status = if (call.isIncoming) call.subtitle.ifBlank { "Panggilan masuk" } else "Panggilan berlangsung"

    Box(modifier = modifier.width(width).clip(shape).clickable(onClick = onOpen)) {
        if (showAlbumBackground && call.avatar != null) {
            FluidGlassBackdrop(
                art = art,
                accent = accent,
                photoAlpha = if (compactMode) 0.5f else 0.6f,
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
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(horizontal = if (compactMode) 12.dp else 16.dp, vertical = if (compactMode) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatar = call.avatar
            if (avatar != null) {
                Image(
                    bitmap = remember(avatar) { avatar.asImageBitmap() },
                    contentDescription = null,
                    modifier = Modifier.size(if (compactMode) 30.dp else 40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(if (compactMode) 30.dp else 40.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(if (compactMode) 8.dp else 10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = call.callerName,
                    color = ink,
                    fontSize = bannerFontSize.coerceIn(8, 20).sp,
                    fontWeight = FontWeight(bannerFontWeight.coerceIn(300, 900)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = status,
                    color = ink.copy(alpha = 0.72f),
                    fontSize = (bannerFontSize - if (compactMode) 3 else 1).coerceIn(8, 18).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (call.isIncoming && call.answerAction != null) {
                    CallBannerButton(CallBannerGreen, Icons.Filled.Call, buttonSize) {
                        Controls.fireAction(context, call.answerAction)
                    }
                }
                CallBannerButton(CallBannerRed, Icons.Filled.CallEnd, buttonSize) {
                    Controls.fireAction(context, call.declineAction ?: call.contentIntent)
                }
            }
        }
    }
}

@Composable
private fun CallBannerButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: Dp,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.84f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "callBtn"
    )
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}
