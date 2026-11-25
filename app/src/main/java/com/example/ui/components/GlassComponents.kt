package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.MayaEmotion
import com.example.domain.PermissionRequirement
import com.example.ui.StatusBannerState
import com.example.ui.theme.GlassBase
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassElevated
import com.example.ui.theme.MayaCoral
import com.example.ui.theme.MayaRose
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.sin

fun MayaEmotion.accentColor(): Color = when (this) {
    MayaEmotion.AFFECTIONATE -> MayaRose
    MayaEmotion.HAPPY -> StatusEmerald
    MayaEmotion.CARING -> NeonCyan
    MayaEmotion.PLAYFUL -> NeuralViolet
    MayaEmotion.CALM -> NeonCyan
    MayaEmotion.CONCERNED -> StatusAmber
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        val mb = bytes / (1024.0 * 1024.0)
        String.format(Locale.US, "%.0f MB", mb)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.24f),
            glowColor.copy(alpha = 0.48f),
            GlassBorder.copy(alpha = 0.42f),
            MayaRose.copy(alpha = 0.28f)
        )
    )
    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            GlassElevated.copy(alpha = 0.68f),
            GlassBase.copy(alpha = 0.56f),
            ObsidianDeep.copy(alpha = 0.72f)
        )
    )
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(clickModifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Color = NeonCyan,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tint.copy(alpha = 0.14f))
                        .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
fun EmergencyStopButton(
    isActive: Boolean,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "emergency_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Button(
        onClick = onStopClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("emergency_stop_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) StatusCrimson else StatusCrimson.copy(alpha = 0.22f),
            contentColor = if (isActive) Color.White else StatusCrimson
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = StatusCrimson.copy(alpha = if (isActive) pulseAlpha else 0.55f)
        )
    ) {
        Icon(
            imageVector = Icons.Filled.StopCircle,
            contentDescription = "Emergency Stop Automation",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isActive) "STOP AUTOMATION NOW" else "Emergency Stop",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusFeedbackBar(
    banner: StatusBannerState?,
    onDismiss: () -> Unit,
    onGrantPermission: (PermissionRequirement) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn(tween(220)) + expandVertically(),
        exit = fadeOut(tween(180)) + shrinkVertically(),
        modifier = modifier
    ) {
        banner?.let { state ->
            val accent = when {
                state.permissionRequirement != null -> StatusAmber
                state.isError -> StatusCrimson
                else -> StatusEmerald
            }
            val icon = when {
                state.permissionRequirement != null -> Icons.Filled.Security
                state.isError -> Icons.Filled.ErrorOutline
                else -> Icons.Filled.CheckCircle
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = ObsidianDeep.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.permissionRequirement != null) {
                        TextButton(
                            onClick = { onGrantPermission(state.permissionRequirement) }
                        ) {
                            Text(
                                text = "Grant",
                                color = StatusAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss status",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceWaveVisualizer(
    isActive: Boolean,
    intensity: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "voice_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 14
        val barWidth = size.width / (barCount * 2f)
        val centerY = size.height / 2f
        val normalizedIntensity = (intensity / 10f).coerceIn(0.25f, 1.0f)

        for (i in 0 until barCount) {
            val wave = if (isActive) {
                (0.3f + 0.7f * ((sin(phase + i * 0.65f) + 1f) / 2f)) * normalizedIntensity
            } else {
                0.15f
            }
            val barHeight = (size.height * wave).coerceAtLeast(4f)
            val x = i * barWidth * 2f + barWidth / 2f
            drawRoundRect(
                color = if (i % 2 == 0) color else MayaCoral.copy(alpha = 0.85f),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
