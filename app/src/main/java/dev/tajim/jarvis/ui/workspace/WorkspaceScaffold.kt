package dev.tajim.jarvis.ui.workspace

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.components.HoloIconButton
import dev.tajim.jarvis.ui.components.JarvisBackground
import dev.tajim.jarvis.ui.navigation.Workspace
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing

/**
 * The full-screen workspace frame. It owns the entire screen: there is no Home, bottom bar, Orb or
 * connector in composition while a workspace is open, so nothing Home-related can receive touches.
 * Back and Close both pop the back stack; Android system Back does the same via the NavHost.
 */
@Composable
fun WorkspaceScaffold(
    workspace: Workspace,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalJarvisColors.current
    JarvisBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HoloIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), onClose)
                val tile = RoundedCornerShape(12.dp)
                Box(
                    Modifier
                        .padding(start = Spacing.md)
                        .size(44.dp)
                        .background(c.glassFill, tile)
                        .border(1.dp, Brush.linearGradient(listOf(c.borderStart, c.borderEnd)), tile),
                    contentAlignment = Alignment.Center,
                ) { Icon(workspace.icon, contentDescription = null, tint = c.cyan) }
                Column(Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                    Text(stringResource(workspace.title), style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                    Text(stringResource(workspace.subtitle), style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                }
                HoloIconButton(Icons.Outlined.Close, stringResource(R.string.workspace_close), onClose)
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.lg), content = content)
        }
    }
}
