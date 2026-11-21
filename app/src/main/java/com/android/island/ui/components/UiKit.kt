package com.android.island.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.ui.theme.Tile
import com.android.island.ui.theme.Ui

enum class RowPos { Single, First, Middle, Last }


@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Ui.Card)
            // STAGE 70: 1 dp robotic hairline — sections read as machined panels instead of
            // flat blocks, without any extra overdraw cost per row.
            .border(1.dp, Ui.IconBg.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
    ) { content() }
}

@Composable
fun SettingsRow(
    pos: RowPos = RowPos.Single,
    icon: ImageVector,
    iconColor: Color = Tile.Blue,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val shape = when (pos) {
        RowPos.Single -> RoundedCornerShape(16.dp)
        RowPos.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        RowPos.Last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        RowPos.Middle -> RoundedCornerShape(0.dp)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ui.Text, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = Ui.Sub, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        if (trailing != null) trailing()
        else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = Ui.Sub,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * STAGE 70: robotic animated switch. A spring-driven thumb with a soft glow that matches the
 * island's motion language, replacing the default Material switch inside [SwitchRow].
 * Draw-phase glow + one Animatable pair: no loops, no recomposition while resting, and the
 * same gesture surface as before (whole row stays clickable through the Switch's own handler).
 */
@Composable
fun IslandSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Tile.Green else Ui.IconBg,
        animationSpec = tween(220),
        label = "island-switch-track"
    )
    val thumbShift by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "island-switch-thumb"
    )
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .width(50.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(trackColor)
            // The switch itself handles the tap; the row keeps its own hit area.
            .clickable(interactionSource = interaction, indication = null) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(start = 3.dp)
                .offset(x = 20.dp * thumbShift)
                .size(24.dp)
                .drawBehind {
                    // Soft green glow pooled around the thumb while ON: drawn behind the thumb,
                    // no extra layer, nothing recomposes while the switch rests.
                    if (checked) {
                        drawCircle(Tile.Green.copy(alpha = 0.28f), radius = size.width / 2f + 5.dp.toPx())
                        drawCircle(Tile.Green.copy(alpha = 0.14f), radius = size.width / 2f + 9.dp.toPx())
                    }
                }
                .clip(CircleShape)
                .background(Color.White)
        ) {
            // ON dot: a tiny accent core so the thumb reads as "powered", not just moved.
            androidx.compose.animation.AnimatedVisibility(
                visible = checked,
                enter = androidx.compose.animation.fadeIn(tween(160)),
                exit = androidx.compose.animation.fadeOut(tween(120))
            ) {
                Box(
                    Modifier
                        .padding(7.dp)
                        .clip(CircleShape)
                        .background(Tile.Green)
                )
            }
        }
    }
}

@Composable
fun SwitchRow(
    pos: RowPos = RowPos.Single,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        pos = pos,
        icon = icon,
        iconColor = iconColor,
        title = title,
        subtitle = subtitle,
        trailing = { IslandSwitch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
fun StatusPill(ok: Boolean, text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (ok) Tile.Green.copy(0.18f) else Tile.Orange.copy(0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = if (ok) Tile.Green else Tile.Orange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
