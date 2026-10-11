package dev.tajim.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.MinTouch
import dev.tajim.jarvis.ui.theme.Radii
import dev.tajim.jarvis.ui.theme.Spacing

@Composable
fun HoloIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalJarvisColors.current.cyan,
) {
    val c = LocalJarvisColors.current
    Box(
        modifier
            .size(MinTouch)
            .clip(CircleShape)
            .background(c.glassFill)
            .border(1.dp, Brush.linearGradient(listOf(c.borderStart, c.borderEnd)), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
fun HoloButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = LocalJarvisColors.current
    val shape = RoundedCornerShape(Radii.chip)
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier
            .heightIn(min = MinTouch)
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.cyan.copy(alpha = 0.22f * alpha), c.violet.copy(alpha = 0.22f * alpha))))
            .border(1.dp, Brush.linearGradient(listOf(c.borderStart.copy(alpha = alpha), c.borderEnd.copy(alpha = alpha))), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.textPrimary.copy(alpha = alpha))
    }
}

enum class StatusKind { OK, WARN, ERROR, NEUTRAL }

@Composable
fun StatusDot(kind: StatusKind, modifier: Modifier = Modifier) {
    val c = LocalJarvisColors.current
    val color = when (kind) {
        StatusKind.OK -> c.success
        StatusKind.WARN -> c.warning
        StatusKind.ERROR -> c.error
        StatusKind.NEUTRAL -> c.textSecondary
    }
    Box(modifier.size(8.dp).background(color, CircleShape))
}

/** Compact label with a status dot, e.g. the connection indicator in the Home header. */
@Composable
fun StatusPill(label: String, kind: StatusKind, modifier: Modifier = Modifier) {
    val c = LocalJarvisColors.current
    Row(
        modifier.padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusDot(kind)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
    }
}
