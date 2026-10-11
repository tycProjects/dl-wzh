package dev.tajim.jarvis.ui.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.tajim.jarvis.BuildConfig
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.components.GlassCard
import dev.tajim.jarvis.ui.components.JarvisScreenScaffold
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing

/** Honest Updates screen: there is no update backend, so it shows the real installed version and nothing invented. */
@Composable
fun UpdatesScreen() {
    val c = LocalJarvisColors.current
    JarvisScreenScaffold(title = stringResource(R.string.updates_title), onBack = null, showsBottomBar = true) {
        GlassCard(Modifier.fillMaxWidth().padding(top = Spacing.lg)) {
            Column(Modifier.padding(Spacing.lg)) {
                Text(
                    stringResource(R.string.updates_current_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.titleMedium, color = c.textPrimary,
                )
                Text(
                    stringResource(R.string.updates_none),
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
                )
            }
        }
    }
}
