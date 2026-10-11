package dev.tajim.jarvis.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.JarvisScreenScaffold
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.MinTouch
import dev.tajim.jarvis.ui.theme.Spacing

@Composable
fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    JarvisScreenScaffold(title = title, onBack = onBack, showsBottomBar = false) {
        content()
        androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = Spacing.xl))
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text, modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm),
        style = MaterialTheme.typography.titleMedium, color = LocalJarvisColors.current.textSecondary,
    )
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md), content = content)
    }
}

@Composable
fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    val c = LocalJarvisColors.current
    GlassCard(Modifier.fillMaxWidth().padding(bottom = Spacing.sm), onClick = onClick) {
        Row(Modifier.heightIn(min = 64.dp).padding(Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = c.cyan)
        }
    }
}

@Composable
fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalJarvisColors.current
    Row(Modifier.fillMaxWidth().heightIn(min = MinTouch), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

