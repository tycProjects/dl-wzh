// app/src/main/java/com/hyperboost/engine/ui/components/Components.kt
package com.hyperboost.engine.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperboost.engine.ui.theme.DarkBackground
import com.hyperboost.engine.ui.theme.DarkOutline
import com.hyperboost.engine.ui.theme.DarkSurfaceHighlight
import com.hyperboost.engine.ui.theme.NeonCyan
import com.hyperboost.engine.ui.theme.NeonGreen
import com.hyperboost.engine.ui.theme.TextMuted
import com.hyperboost.engine.ui.theme.TextSecondary

/**
 * Standard dashboard card. Pass [accent] to tint the border and background
 * (for "active" states), and [onClick] to make the whole card tappable.
 */
@Composable
fun HbCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val surface = MaterialTheme.colorScheme.surface
    val container = if (accent != null) accent.copy(alpha = 0.07f).compositeOver(surface) else surface
    val colors = CardDefaults.cardColors(containerColor = container)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    val border = if (accent != null) {
        BorderStroke(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.72f), NeonGreen.copy(alpha = 0.32f))))
    } else {
        BorderStroke(1.dp, Color(0x12FFFFFF))
    }
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) { body() }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) { body() }
    }
}

/** Small accent bar + uppercase label used above every dashboard group. */
@Composable
fun SectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(NeonCyan, NeonGreen)))
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.35.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextSecondary
            )
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

/** Themed switch used everywhere so active controls share one accent language. */
@Composable
fun HbSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = DarkBackground,
            checkedTrackColor = NeonCyan,
            checkedBorderColor = NeonCyan,
            uncheckedThumbColor = TextSecondary,
            uncheckedTrackColor = DarkSurfaceHighlight,
            uncheckedBorderColor = DarkOutline,
            disabledCheckedThumbColor = TextMuted,
            disabledUncheckedThumbColor = TextMuted
        )
    )
}

/** Rounded icon tile; the gradient version marks an active module. */
@Composable
fun HbIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color = NeonCyan,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .then(
                if (active) {
                    Modifier.background(Brush.linearGradient(listOf(tint, NeonGreen)))
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(13.dp))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) DarkBackground else tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/** Small pill with a colored dot, e.g. "SHIZUKU" or "ENGAGED". */
@Composable
fun StatChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            ),
            color = color
        )
    }
}

/** Label + monospaced value block for key numbers. */
@Composable
fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Color(0x0DFFFFFF), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = valueColor
        )
    }
}
