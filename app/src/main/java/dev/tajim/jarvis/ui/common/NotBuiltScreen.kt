package dev.tajim.jarvis.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.JarvisScreenScaffold
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing

/** Honest placeholder for destinations scheduled for a later stage. Shows no fake content. */
@Composable
fun NotBuiltScreen(title: String, onBack: (() -> Unit)?, showsBottomBar: Boolean = false) {
    val c = LocalJarvisColors.current
    JarvisScreenScaffold(title = title, onBack = onBack, showsBottomBar = showsBottomBar) {
        GlassCard(Modifier.padding(top = Spacing.lg)) {
            Column(Modifier.padding(Spacing.lg)) {
                Text(stringResource(R.string.not_built_title), style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(
                    stringResource(R.string.not_built_body),
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
                )
            }
        }
    }
}
