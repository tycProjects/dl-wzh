package dev.tajim.jarvis.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.Spacing

/** Insets for screens that sit above the bottom bar: the bar itself handles the bottom edge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun tabContentInsets(): WindowInsets =
    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

/**
 * Standard secondary-screen frame: background, safe-area insets, optional back control and a title.
 * [showsBottomBar] = true for tab screens (the bottom bar owns the bottom inset).
 */
@Composable
fun JarvisScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    showsBottomBar: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalJarvisColors.current
    val insets = if (showsBottomBar) tabContentInsets() else WindowInsets.safeDrawing
    JarvisBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(insets)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    HoloIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), onBack)
                }
                Text(
                    title,
                    modifier = Modifier.padding(start = if (onBack != null) Spacing.md else 0.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                )
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg),
                content = content,
            )
        }
    }
}

