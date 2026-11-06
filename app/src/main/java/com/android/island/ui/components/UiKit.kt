package com.android.island.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.ui.theme.Tile
import com.android.island.ui.theme.Ui

enum class RowPos { Single, First, Middle, Last }

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(title, color = Ui.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Ui.Sub, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Ui.Card)
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
                Text(subtitle, color = Ui.Sub, fontSize = 13.sp, lineHeight = 17.sp)
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
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Tile.Green,
                    checkedThumbColor = Color.White
                )
            )
        }
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

@Composable
fun HeroCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ui.Primary.copy(alpha = 0.12f))
            .clickable(onClick = onAction)
            .padding(20.dp)
    ) {
        Text(title, color = Ui.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = Ui.Sub, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Ui.Primary)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(actionLabel, color = Ui.OnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
