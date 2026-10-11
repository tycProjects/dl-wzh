package dev.tajim.jarvis.ui.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.navigation.Workspace
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing

/** Dedicated full-screen destination for workspaces whose tools are scheduled for a later stage. */
@Composable
fun WorkspacePlaceholder(workspace: Workspace, onClose: () -> Unit) {
    val c = LocalJarvisColors.current
    WorkspaceScaffold(workspace, onClose) {
        GlassCard(Modifier.fillMaxWidth().padding(top = Spacing.md)) {
            Column(Modifier.padding(Spacing.lg)) {
                Text(
                    stringResource(R.string.workspace_not_built_title, stringResource(workspace.title)),
                    style = MaterialTheme.typography.titleMedium, color = c.textPrimary,
                )
                Text(
                    stringResource(R.string.workspace_not_built_body),
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
                )
            }
        }
    }
}
